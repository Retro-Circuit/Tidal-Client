import { BrowserWindow, app } from 'electron'
import { createWriteStream, existsSync, readdirSync, statSync } from 'node:fs'
import { mkdir, rm, unlink, writeFile, copyFile, cp } from 'node:fs/promises'
import { basename, dirname, join } from 'node:path'
import { pipeline } from 'node:stream/promises'
import { Readable } from 'node:stream'
import { randomUUID } from 'node:crypto'
import { Version } from '@xmcl/core'
import AdmZip from 'adm-zip'
import {
  getVersionList,
  install,
  installDependencies,
  installFabric,
  installForge,
  installNeoForged,
  installQuiltVersion
} from '@xmcl/installer'
import type {
  CreateInstanceRequest,
  GameInstance,
  ForeignInstance,
  InstallProgress,
  InstanceModFile,
  ModpackCard,
  ModLoaderId,
  ProjectType
} from '../shared/types'
import { curseForgeFileDownloadUrl, fetchCurseForgeLatestFile, fetchModrinthVersion } from './discover'
import { findJava } from './java'
import { injectBuiltinMod, isHiddenBuiltin } from './builtin'
import { getInstances, getSettings, removeInstance, upsertInstance } from './store'
import { fetchLoaderVersions } from './versions'

export function minecraftRoot(): string {
  return join(app.getPath('userData'), 'minecraft')
}

export function instancesRoot(): string {
  return join(app.getPath('userData'), 'instances')
}

async function prepareInstanceDir(id: string): Promise<string> {
  const instanceDir = join(instancesRoot(), id)
  await mkdir(instanceDir, { recursive: true })
  for (const folder of ['mods', 'resourcepacks', 'shaderpacks', 'config', 'saves', 'screenshots']) {
    await mkdir(join(instanceDir, folder), { recursive: true })
  }
  return instanceDir
}

function contentFolder(type: ProjectType): string {
  if (type === 'resourcepack') return 'resourcepacks'
  if (type === 'shader') return 'shaderpacks'
  return 'mods'
}

function sendProgress(payload: InstallProgress): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send('install:progress', payload)
  }
}

async function downloadFile(url: string, dest: string): Promise<void> {
  await mkdir(dirname(dest), { recursive: true })
  const res = await fetch(url, { headers: { 'User-Agent': 'TidalClient/0.1.0' } })
  if (!res.ok || !res.body) throw new Error(`Download failed: ${url}`)
  await pipeline(Readable.fromWeb(res.body as never), createWriteStream(dest))
}

interface LoaderPlan {
  minecraftVersion: string
  loader: string
  loaderVersion: string
}

async function ensureMinecraft(version: string, onProgress: (msg: string) => void): Promise<void> {
  onProgress(`Installing Minecraft ${version}`)
  const list = await getVersionList()
  const meta = list.versions.find((v) => v.id === version)
  if (!meta) throw new Error(`Minecraft ${version} was not found in the version manifest`)
  await install(meta, minecraftRoot())
}

async function installLoader(plan: LoaderPlan, onProgress: (msg: string) => void): Promise<string> {
  const root = minecraftRoot()
  await ensureMinecraft(plan.minecraftVersion, onProgress)
  const java = await findJava(getSettings().javaPath || undefined)

  const loader = plan.loader.toLowerCase()
  onProgress(`Installing ${loader} ${plan.loaderVersion}`)

  if (loader === 'fabric') {
    return await installFabric({
      minecraft: root,
      minecraftVersion: plan.minecraftVersion,
      version: plan.loaderVersion
    })
  }

  if (loader === 'quilt') {
    return await installQuiltVersion({
      minecraft: root,
      minecraftVersion: plan.minecraftVersion,
      version: plan.loaderVersion
    })
  }

  if (loader === 'neoforge') {
    return await installNeoForged('neoforge', plan.loaderVersion, root, { java })
  }

  if (loader === 'forge') {
    return await installForge(
      { mcversion: plan.minecraftVersion, version: plan.loaderVersion },
      root,
      { java }
    )
  }

  if (loader === 'minecraft' || loader === 'vanilla' || !loader) {
    onProgress('Installing Tidal menu (Fabric)')
    try {
      const fabricVersions = await fetchLoaderVersions('fabric', plan.minecraftVersion)
      const fabricVersion = fabricVersions[0]
      if (fabricVersion) {
        return await installFabric({
          minecraft: root,
          minecraftVersion: plan.minecraftVersion,
          version: fabricVersion
        })
      }
    } catch {
      // No Fabric for this version; copy the jar anyway.
    }
    return plan.minecraftVersion
  }

  throw new Error(`Unsupported loader: ${plan.loader}`)
}

export async function ensureBuiltinRuntime(instance: GameInstance): Promise<GameInstance> {
  const gameDir = join(instancesRoot(), instance.id)
  await injectBuiltinMod(gameDir, instance.minecraftVersion)
  if (instance.loader !== 'vanilla') return instance
  if (instance.versionId.toLowerCase().includes('fabric')) return instance
  const versionId = await installLoader(
    {
      minecraftVersion: instance.minecraftVersion,
      loader: 'vanilla',
      loaderVersion: ''
    },
    () => undefined
  )
  await finishVersion(versionId, () => undefined)
  const next = { ...instance, versionId }
  upsertInstance(next)
  return next
}

async function finishVersion(versionId: string, onProgress: (msg: string) => void): Promise<void> {
  onProgress('Downloading libraries and assets')
  const resolved = await Version.parse(minecraftRoot(), versionId)
  await installDependencies(resolved)
}

interface MrIndex {
  name: string
  dependencies: Record<string, string>
  files: Array<{
    path: string
    downloads: string[]
    env?: { client?: string }
  }>
}

function planFromMrpack(index: MrIndex): LoaderPlan {
  const minecraftVersion = index.dependencies.minecraft
  if (!minecraftVersion) throw new Error('Modpack is missing a Minecraft version')
  if (index.dependencies['fabric-loader']) {
    return { minecraftVersion, loader: 'fabric', loaderVersion: index.dependencies['fabric-loader'] }
  }
  if (index.dependencies['quilt-loader']) {
    return { minecraftVersion, loader: 'quilt', loaderVersion: index.dependencies['quilt-loader'] }
  }
  if (index.dependencies.neoforge) {
    return { minecraftVersion, loader: 'neoforge', loaderVersion: index.dependencies.neoforge }
  }
  if (index.dependencies.forge) {
    return { minecraftVersion, loader: 'forge', loaderVersion: index.dependencies.forge }
  }
  return { minecraftVersion, loader: 'vanilla', loaderVersion: '' }
}

async function installMrpack(zipPath: string, instanceDir: string, onProgress: (msg: string, n: number, t: number) => void): Promise<LoaderPlan> {
  const zip = new AdmZip(zipPath)
  const indexEntry = zip.getEntry('modrinth.index.json')
  if (!indexEntry) throw new Error('Not a valid Modrinth modpack (.mrpack)')
  const index = JSON.parse(indexEntry.getData().toString('utf8')) as MrIndex
  const files = index.files.filter((file) => file.env?.client !== 'unsupported')
  let done = 0
  for (const file of files) {
    const url = file.downloads[0]
    if (!url) continue
    onProgress(`Downloading ${file.path}`, done, files.length)
    await downloadFile(url, join(instanceDir, file.path))
    done += 1
  }
  const overridePrefixes = ['overrides/', 'client-overrides/']
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory) continue
    const name = entry.entryName.replaceAll('\\', '/')
    const prefix = overridePrefixes.find((p) => name.startsWith(p))
    if (!prefix) continue
    const rel = name.slice(prefix.length)
    if (!rel) continue
    const dest = join(instanceDir, rel)
    await mkdir(dirname(dest), { recursive: true })
    await writeFile(dest, entry.getData())
  }
  return planFromMrpack(index)
}

interface CfManifest {
  name: string
  minecraft: {
    version: string
    modLoaders?: { id: string; primary?: boolean }[]
  }
  files: { projectID: number; fileID: number; required?: boolean }[]
  overrides?: string
}

function planFromCurse(manifest: CfManifest): LoaderPlan {
  const minecraftVersion = manifest.minecraft.version
  const loaderId = manifest.minecraft.modLoaders?.find((l) => l.primary)?.id ?? manifest.minecraft.modLoaders?.[0]?.id ?? ''
  const [loader, ...rest] = loaderId.split('-')
  return {
    minecraftVersion,
    loader: loader || 'vanilla',
    loaderVersion: rest.join('-')
  }
}

async function installCurseZip(
  zipPath: string,
  instanceDir: string,
  apiKey: string,
  onProgress: (msg: string, n: number, t: number) => void
): Promise<LoaderPlan> {
  const zip = new AdmZip(zipPath)
  const manifestEntry = zip.getEntry('manifest.json')
  if (!manifestEntry) throw new Error('Not a valid CurseForge modpack')
  const manifest = JSON.parse(manifestEntry.getData().toString('utf8')) as CfManifest
  const files = manifest.files.filter((f) => f.required !== false)
  let done = 0
  for (const file of files) {
    onProgress(`Downloading CurseForge file ${file.fileID}`, done, files.length)
    const url = await curseForgeFileDownloadUrl(file.projectID, file.fileID, apiKey)
    const dest = join(instanceDir, 'mods', `${file.projectID}-${file.fileID}.jar`)
    await downloadFile(url, dest)
    done += 1
  }
  const overrides = manifest.overrides ?? 'overrides'
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory) continue
    const name = entry.entryName.replaceAll('\\', '/')
    if (!name.startsWith(`${overrides}/`)) continue
    const rel = name.slice(overrides.length + 1)
    const dest = join(instanceDir, rel)
    await mkdir(dirname(dest), { recursive: true })
    await writeFile(dest, entry.getData())
  }
  return planFromCurse(manifest)
}

export async function installModpack(pack: ModpackCard): Promise<GameInstance> {
  const id = randomUUID()
  const instanceDir = await prepareInstanceDir(id)
  await mkdir(minecraftRoot(), { recursive: true })

  const report = (phase: string, message: string, progress = 0, total = 0): void => {
    sendProgress({ instanceId: id, phase, message, progress, total })
  }

  report('prepare', `Installing ${pack.title}`)
  const tempDir = join(app.getPath('temp'), `tidal-${id}`)
  await mkdir(tempDir, { recursive: true })

  try {
    let plan: LoaderPlan
    if (pack.source === 'modrinth') {
      const projectId = pack.id.replace(/^modrinth:/, '')
      const version = await fetchModrinthVersion(pack.slug ?? projectId)
      const file = version.files.find((f) => f.primary) ?? version.files[0]
      if (!file) throw new Error('No Modrinth file to download')
      const zipPath = join(tempDir, file.filename)
      report('download', 'Downloading modpack archive')
      await downloadFile(file.url, zipPath)
      plan = await installMrpack(zipPath, instanceDir, (message, n, t) => report('mods', message, n, t))
    } else {
      const settings = getSettings()
      if (!settings.curseforgeApiKey) throw new Error('A CurseForge API key is required to install this pack')
      const projectId = pack.curseProjectId ?? Number(pack.id.replace(/^curseforge:/, ''))
      const file = await fetchCurseForgeLatestFile(projectId, settings.curseforgeApiKey)
      const zipPath = join(tempDir, file.fileName)
      report('download', 'Downloading modpack archive')
      await downloadFile(file.downloadUrl, zipPath)
      plan = await installCurseZip(zipPath, instanceDir, settings.curseforgeApiKey, (message, n, t) =>
        report('mods', message, n, t)
      )
    }

    const versionId = await installLoader(plan, (message) => report('loader', message))
    await finishVersion(versionId, (message) => report('assets', message))
    await injectBuiltinMod(instanceDir, plan.minecraftVersion)

    const instance: GameInstance = {
      id,
      name: pack.title,
      source: pack.source,
      sourceId: pack.id,
      iconUrl: pack.iconUrl,
      minecraftVersion: plan.minecraftVersion,
      loader: plan.loader,
      loaderVersion: plan.loaderVersion,
      versionId,
      createdAt: Date.now()
    }
    upsertInstance(instance)
    report('done', 'Install complete', 1, 1)
    return instance
  } catch (error) {
    await rm(instanceDir, { recursive: true, force: true })
    throw error
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
}

export async function createCustomInstance(request: CreateInstanceRequest): Promise<GameInstance> {
  const id = randomUUID()
  const instanceDir = await prepareInstanceDir(id)
  const report = (message: string): void => {
    sendProgress({ instanceId: id, phase: 'create', message, progress: 0, total: 0 })
  }
  try {
    const versionId = await installLoader(
      {
        minecraftVersion: request.minecraftVersion,
        loader: request.loader,
        loaderVersion: request.loaderVersion
      },
      report
    )
    await finishVersion(versionId, report)
    await injectBuiltinMod(instanceDir, request.minecraftVersion)
    const instance: GameInstance = {
      id,
      name: request.name.trim() || `${request.loader} ${request.minecraftVersion}`,
      source: request.loader === 'vanilla' ? 'vanilla' : 'custom',
      sourceId: `${request.loader}:${request.minecraftVersion}`,
      iconUrl: '',
      minecraftVersion: request.minecraftVersion,
      loader: request.loader,
      loaderVersion: request.loaderVersion,
      versionId,
      createdAt: Date.now()
    }
    upsertInstance(instance)
    sendProgress({ instanceId: id, phase: 'done', message: 'Instance ready', progress: 1, total: 1 })
    return instance
  } catch (error) {
    await rm(instanceDir, { recursive: true, force: true })
    throw error
  }
}

export async function createVanillaInstance(version = '1.21.4'): Promise<GameInstance> {
  return createCustomInstance({
    name: `Vanilla ${version}`,
    minecraftVersion: version,
    loader: 'vanilla',
    loaderVersion: ''
  })
}

export async function installContentToInstance(pack: ModpackCard, instanceId: string): Promise<GameInstance> {
  const instance = getInstances().find((item) => item.id === instanceId)
  if (!instance) throw new Error('Select an instance first.')
  if (pack.projectType === 'modpack') {
    return installModpack(pack)
  }

  const destDir = join(instancesRoot(), instanceId, contentFolder(pack.projectType))
  await mkdir(destDir, { recursive: true })
  sendProgress({
    instanceId,
    phase: 'content',
    message: `Installing ${pack.title} into ${instance.name}`,
    progress: 0,
    total: 1
  })

  if (pack.source === 'modrinth') {
    const projectId = pack.slug ?? pack.id.replace(/^modrinth:/, '')
    const version = await fetchModrinthVersion(projectId)
    const file = version.files.find((item) => item.primary) ?? version.files[0]
    if (!file) throw new Error('No file available for this project')
    await downloadFile(file.url, join(destDir, file.filename))
  } else {
    const settings = getSettings()
    if (!settings.curseforgeApiKey) throw new Error('A CurseForge API key is required')
    const projectId = pack.curseProjectId ?? Number(pack.id.replace(/^curseforge:/, ''))
    const file = await fetchCurseForgeLatestFile(projectId, settings.curseforgeApiKey)
    await downloadFile(file.downloadUrl, join(destDir, file.fileName))
  }

  sendProgress({
    instanceId,
    phase: 'done',
    message: `${pack.title} installed into ${instance.name}`,
    progress: 1,
    total: 1
  })
  return instance
}

export async function importForeignInstances(items: ForeignInstance[]): Promise<GameInstance[]> {
  const created: GameInstance[] = []
  for (const item of items) {
    const id = randomUUID()
    const instanceDir = await prepareInstanceDir(id)
    sendProgress({ instanceId: id, phase: 'import', message: `Importing ${item.name}`, progress: 0, total: 1 })
    try {
      const folders = ['mods', 'resourcepacks', 'shaderpacks', 'config', 'saves', 'screenshots', 'datapacks']
      for (const folder of folders) {
        const src = join(item.path, folder)
        if (existsSync(src)) await cp(src, join(instanceDir, folder), { recursive: true })
      }
      for (const file of ['options.txt', 'optionsof.txt', 'servers.dat']) {
        const src = join(item.path, file)
        if (existsSync(src)) await copyFile(src, join(instanceDir, file))
      }
      let loaderVersion = item.loaderVersion
      const loader = (['fabric', 'quilt', 'forge', 'neoforge', 'vanilla'].includes(item.loader)
        ? item.loader
        : 'vanilla') as ModLoaderId
      if (loader !== 'vanilla' && !loaderVersion) {
        const versions = await fetchLoaderVersions(loader, item.minecraftVersion)
        loaderVersion = versions[0] ?? ''
      }
      let versionId = item.minecraftVersion
      try {
        versionId = await installLoader(
          { minecraftVersion: item.minecraftVersion, loader, loaderVersion },
          (message) => sendProgress({ instanceId: id, phase: 'import', message, progress: 0, total: 1 })
        )
        await finishVersion(versionId, (message) =>
          sendProgress({ instanceId: id, phase: 'import', message, progress: 0, total: 1 })
        )
      } catch {
        versionId = item.minecraftVersion
      }
      await injectBuiltinMod(instanceDir, item.minecraftVersion)
      const instance: GameInstance = {
        id,
        name: item.name,
        source: 'custom',
        sourceId: item.id,
        iconUrl: '',
        minecraftVersion: item.minecraftVersion,
        loader,
        loaderVersion,
        versionId,
        createdAt: Date.now()
      }
      upsertInstance(instance)
      created.push(instance)
    } catch (error) {
      await rm(instanceDir, { recursive: true, force: true })
      throw error
    }
  }
  return created
}

export function listInstanceMods(instanceId: string): InstanceModFile[] {
  const modsDir = join(instancesRoot(), instanceId, 'mods')
  if (!existsSync(modsDir)) return []
  return readdirSync(modsDir)
    .filter((fileName) => {
      const lower = fileName.toLowerCase()
      return (lower.endsWith('.jar') || lower.endsWith('.jar.disabled')) && !isHiddenBuiltin(fileName)
    })
    .map((fileName) => ({
      fileName,
      size: statSync(join(modsDir, fileName)).size
    }))
    .sort((a, b) => a.fileName.localeCompare(b.fileName))
}

export async function importInstanceMods(instanceId: string, paths: string[]): Promise<InstanceModFile[]> {
  const modsDir = join(instancesRoot(), instanceId, 'mods')
  await mkdir(modsDir, { recursive: true })
  for (const src of paths) {
    const lower = src.toLowerCase()
    if (!lower.endsWith('.jar')) continue
    const fileName = basename(src).replace(/[/\\]/g, '')
    if (!fileName || isHiddenBuiltin(fileName)) continue
    await copyFile(src, join(modsDir, fileName))
  }
  return listInstanceMods(instanceId)
}

export async function deleteInstanceMod(instanceId: string, fileName: string): Promise<void> {
  const safeName = fileName.replace(/[/\\]/g, '')
  const modPath = join(instancesRoot(), instanceId, 'mods', safeName)
  if (existsSync(modPath)) await unlink(modPath)
}

export async function deleteInstance(instanceId: string): Promise<void> {
  await rm(join(instancesRoot(), instanceId), { recursive: true, force: true })
  removeInstance(instanceId)
}
