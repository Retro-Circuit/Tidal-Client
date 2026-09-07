import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'
import { app } from 'electron'
import type { ForeignInstance } from '../shared/types'

function instancesRoot(): string {
  return join(app.getPath('userData'), 'instances')
}

function appData(): string {
  return app.getPath('appData')
}

function home(): string {
  return homedir()
}

function dirs(...paths: string[]): string[] {
  return paths.filter((path) => {
    try {
      return existsSync(path) && statSync(path).isDirectory()
    } catch {
      return false
    }
  })
}

function readText(path: string): string {
  try {
    return readFileSync(path, 'utf8')
  } catch {
    return ''
  }
}

function readJson(path: string): Record<string, unknown> | null {
  try {
    return JSON.parse(readFileSync(path, 'utf8')) as Record<string, unknown>
  } catch {
    return null
  }
}

function cfgValue(text: string, key: string): string {
  const line = text.split(/\r?\n/).find((row) => row.startsWith(`${key}=`))
  return line ? line.slice(key.length + 1).trim() : ''
}

function add(
  found: ForeignInstance[],
  item: Omit<ForeignInstance, 'id'> & { id?: string }
): void {
  if (!item.path || !existsSync(item.path)) return
  const id = item.id ?? `${item.launcher}:${item.path}`
  if (found.some((row) => row.path === item.path)) return
  found.push({ ...item, id })
}

function scanPrismLike(found: ForeignInstance[], root: string, launcher: string): void {
  if (!existsSync(root)) return
  for (const name of readdirSync(root)) {
    const dir = join(root, name)
    if (!statSync(dir).isDirectory() || name.startsWith('_')) continue
    const cfg = readText(join(dir, 'instance.cfg'))
    const pack = readJson(join(dir, 'mmc-pack.json'))
    const components = Array.isArray(pack?.components) ? (pack.components as Array<Record<string, string>>) : []
    const mc = components.find((c) => c.uid === 'net.minecraft')?.version || cfgValue(cfg, 'IntendedVersion') || 'unknown'
    const fabric = components.find((c) => c.uid === 'net.fabricmc.fabric-loader')
    const quilt = components.find((c) => c.uid === 'org.quiltmc.quilt-loader')
    const forge = components.find((c) => c.uid === 'net.minecraftforge')
    const neo = components.find((c) => c.uid === 'net.neoforged')
    let loader = 'vanilla'
    let loaderVersion = ''
    if (fabric) {
      loader = 'fabric'
      loaderVersion = fabric.version ?? ''
    } else if (quilt) {
      loader = 'quilt'
      loaderVersion = quilt.version ?? ''
    } else if (neo) {
      loader = 'neoforge'
      loaderVersion = neo.version ?? ''
    } else if (forge) {
      loader = 'forge'
      loaderVersion = forge.version ?? ''
    }
    const minecraftDir = existsSync(join(dir, '.minecraft')) ? join(dir, '.minecraft') : dir
    add(found, {
      name: cfgValue(cfg, 'name') || name,
      launcher,
      path: minecraftDir,
      minecraftVersion: mc,
      loader,
      loaderVersion
    })
  }
}

function scanCurse(found: ForeignInstance[], root: string): void {
  if (!existsSync(root)) return
  for (const name of readdirSync(root)) {
    const dir = join(root, name)
    if (!statSync(dir).isDirectory()) continue
    const json = readJson(join(dir, 'minecraftinstance.json'))
    const base = (json?.baseModLoader as Record<string, string> | undefined) ?? {}
    const mc = (json?.gameVersion as string) || (json?.minecraftVersion as string) || 'unknown'
    const loaderName = (base.name || '').toLowerCase()
    let loader = 'vanilla'
    if (loaderName.includes('fabric')) loader = 'fabric'
    else if (loaderName.includes('quilt')) loader = 'quilt'
    else if (loaderName.includes('neoforge')) loader = 'neoforge'
    else if (loaderName.includes('forge')) loader = 'forge'
    add(found, {
      name: (json?.name as string) || name,
      launcher: 'CurseForge',
      path: dir,
      minecraftVersion: mc,
      loader,
      loaderVersion: base.forgeVersion || base.version || ''
    })
  }
}

function scanModrinth(found: ForeignInstance[], root: string): void {
  if (!existsSync(root)) return
  for (const name of readdirSync(root)) {
    const dir = join(root, name)
    if (!statSync(dir).isDirectory()) continue
    const json =
      readJson(join(dir, 'profile.json')) ??
      readJson(join(dir, 'modrinth.json')) ??
      readJson(join(dir, 'config.json'))
    const meta = json
    if (!meta && !existsSync(join(dir, 'mods')) && !existsSync(join(dir, 'saves'))) continue
    const mc =
      (meta?.game_version as string) ||
      (meta?.gameVersion as string) ||
      (meta?.minecraft_version as string) ||
      'unknown'
    const loaderRaw = String(meta?.loader ?? meta?.mod_loader ?? 'vanilla').toLowerCase()
    const loader = ['fabric', 'quilt', 'forge', 'neoforge'].includes(loaderRaw) ? loaderRaw : 'vanilla'
    const path = existsSync(join(dir, 'minecraft')) ? join(dir, 'minecraft') : dir
    add(found, {
      name: (meta?.name as string) || name,
      launcher: 'Modrinth',
      path,
      minecraftVersion: mc,
      loader,
      loaderVersion: String(meta?.loader_version ?? meta?.loaderVersion ?? '')
    })
  }
}

function scanVanilla(found: ForeignInstance[]): void {
  const dir = join(appData(), '.minecraft')
  if (!existsSync(dir)) return
  const versions = join(dir, 'versions')
  let version = 'unknown'
  if (existsSync(versions)) {
    const ids = readdirSync(versions).filter((name) => existsSync(join(versions, name, `${name}.json`)))
    version = ids.sort().at(-1) ?? 'unknown'
  }
  add(found, {
    name: 'Vanilla .minecraft',
    launcher: 'Minecraft Launcher',
    path: dir,
    minecraftVersion: version,
    loader: existsSync(join(dir, 'mods')) ? 'fabric' : 'vanilla',
    loaderVersion: ''
  })
}

export function scanForeignInstances(): ForeignInstance[] {
  const found: ForeignInstance[] = []
  const tidal = instancesRoot()
  const prismRoots = dirs(
    join(appData(), 'PrismLauncher', 'instances'),
    join(appData(), 'PolyMC', 'instances'),
    join(appData(), 'MultiMC', 'instances'),
    join(home(), 'Documents', 'PrismLauncher', 'instances')
  )
  for (const root of prismRoots) {
    const launcher = root.toLowerCase().includes('polymc')
      ? 'PolyMC'
      : root.toLowerCase().includes('multimc')
        ? 'MultiMC'
        : 'Prism'
    scanPrismLike(found, root, launcher)
  }
  for (const root of dirs(
    join(home(), 'curseforge', 'minecraft', 'Instances'),
    join(appData(), 'curseforge', 'minecraft', 'Instances'),
    join(home(), 'Documents', 'curseforge', 'minecraft', 'Instances')
  )) {
    scanCurse(found, root)
  }
  for (const root of dirs(
    join(appData(), 'ModrinthApp', 'profiles'),
    join(appData(), 'com.modrinth.Theseus', 'profiles'),
    join(appData(), 'ModrinthApp', 'app', 'profiles')
  )) {
    scanModrinth(found, root)
  }
  for (const root of dirs(
    join(appData(), 'gdlauncher_next', 'data', 'instances'),
    join(appData(), 'gdlauncher', 'instances'),
    join(appData(), 'ATLauncher', 'instances')
  )) {
    const launcher = root.toLowerCase().includes('atlauncher') ? 'ATLauncher' : 'GDLauncher'
    if (launcher === 'ATLauncher') {
      for (const name of readdirSync(root)) {
        const dir = join(root, name)
        if (!statSync(dir).isDirectory()) continue
        const json = readJson(join(dir, 'instance.json'))
        add(found, {
          name: (json?.name as string) || name,
          launcher,
          path: dir,
          minecraftVersion: String(json?.minecraftVersion ?? json?.id ?? 'unknown'),
          loader: String(json?.loader ?? 'vanilla'),
          loaderVersion: String(json?.loaderVersion ?? '')
        })
      }
    } else {
      scanPrismLike(found, root, launcher)
    }
  }
  scanVanilla(found)
  return found.filter((item) => !item.path.startsWith(tidal))
}
