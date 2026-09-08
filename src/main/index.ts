import { app, BrowserWindow, dialog, ipcMain, Menu, nativeImage, shell } from 'electron'
import dns from 'node:dns'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { loginWithMicrosoft, logout, restoreSession } from './auth'
import { flattenError } from './errors'
import { fetchProjectDetails, searchModpacks } from './discover'
import { createCustomInstance, createVanillaInstance, deleteInstance, deleteInstanceMod, importForeignInstances, importInstanceMods, installContentToInstance, installModpack, listInstanceMods } from './install'
import { scanForeignInstances } from './importScan'
import { launchInstance, stopInstance, getRunStatus } from './launch'
import { claimDaily, getInstances, getSettings, getWallet, grantShadowPoints, setSettings } from './store'
import { fetchLoaderVersions, fetchVersionManifest } from './versions'
import type { CreateInstanceRequest, DiscoverSearch, ForeignInstance, GameInstance, ModpackCard } from '../shared/types'

function loadDotEnv(): void {
  const file = join(process.cwd(), '.env')
  if (!existsSync(file)) return
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const eq = trimmed.indexOf('=')
    if (eq === -1) continue
    const key = trimmed.slice(0, eq).trim()
    const value = trimmed.slice(eq + 1).trim().replace(/^['"]|['"]$/g, '')
    if (!process.env[key]) process.env[key] = value
  }
}

function resolveAppIcon(): string {
  const candidates = [
    join(process.cwd(), 'tidal.png'),
    join(__dirname, '../../tidal.png'),
    join(process.resourcesPath, 'tidal.png')
  ]
  return candidates.find((path) => existsSync(path)) ?? candidates[0]
}

const isDev = !app.isPackaged

dns.setDefaultResultOrder('ipv4first')

app.commandLine.appendSwitch('enable-gpu-rasterization')
app.commandLine.appendSwitch('enable-zero-copy')
app.commandLine.appendSwitch('disable-features', 'CalculateNativeWinOcclusion')

function createWindow(): void {
  const iconPath = resolveAppIcon()
  const icon = nativeImage.createFromPath(iconPath)
  const window = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 680,
    show: false,
    frame: false,
    backgroundColor: '#16181c',
    title: 'Tidal Client',
    icon: icon.isEmpty() ? undefined : icon,
    webPreferences: {
      preload: join(__dirname, '../preload/index.cjs'),
      sandbox: false,
      contextIsolation: true,
      nodeIntegration: false,
      spellcheck: false,
      backgroundThrottling: true
    }
  })

  window.on('ready-to-show', () => window.show())
  window.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  if (isDev && process.env['ELECTRON_RENDERER_URL']) {
    window.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    window.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(async () => {
  loadDotEnv()
  Menu.setApplicationMenu(null)
  const iconPath = resolveAppIcon()
  if (process.platform === 'win32') {
    app.setAppUserModelId('com.tidal.client')
  }
  if (process.platform === 'darwin' && existsSync(iconPath)) {
    app.dock?.setIcon(iconPath)
  }
  await restoreSession()

  ipcMain.handle('window:minimize', (event) => BrowserWindow.fromWebContents(event.sender)?.minimize())
  ipcMain.handle('window:maximize', (event) => {
    const win = BrowserWindow.fromWebContents(event.sender)
    if (!win) return
    if (win.isMaximized()) win.unmaximize()
    else win.maximize()
  })
  ipcMain.handle('window:close', (event) => BrowserWindow.fromWebContents(event.sender)?.close())

  function handle(channel: string, fn: (...args: never[]) => unknown): void {
    ipcMain.handle(channel, async (_event, ...args: unknown[]) => {
      try {
        return await (fn as (...params: unknown[]) => unknown)(...args)
      } catch (error) {
        throw new Error(flattenError(error))
      }
    })
  }

  handle('auth:login', () => loginWithMicrosoft())
  handle('auth:logout', () => logout())
  handle('auth:session', () => restoreSession())

  handle('settings:get', () => getSettings())
  handle('settings:set', (patch) => setSettings(patch as Partial<import('../shared/types').AppSettings>))

  handle('wallet:get', () => getWallet())
  handle('wallet:claim-daily', () => claimDaily())
  handle('wallet:grant-shadow', () => grantShadowPoints())

  handle('discover:search', (query, options) => searchModpacks(query as string, options as DiscoverSearch | undefined))
  handle('discover:details', (card) => fetchProjectDetails(card as ModpackCard))
  handle('minecraft:versions', () => fetchVersionManifest())
  handle('minecraft:loaders', (loader, minecraftVersion) =>
    fetchLoaderVersions(loader as import('../shared/types').ModLoaderId, minecraftVersion as string)
  )

  handle('instance:list', () => getInstances())
  handle('instance:scan-foreign', () => scanForeignInstances())
  handle('instance:import-foreign', (items) => importForeignInstances(items as ForeignInstance[]))
  handle('instance:create', (request) => createCustomInstance(request as CreateInstanceRequest))
  handle('instance:vanilla', (version) => createVanillaInstance(version as string | undefined))
  handle('instance:install', (pack) => installModpack(pack as ModpackCard))
  handle('instance:install-content', (pack, instanceId) =>
    installContentToInstance(pack as ModpackCard, instanceId as string)
  )
  handle('instance:mods', (instanceId) => listInstanceMods(instanceId as string))
  handle('instance:import-mods', (instanceId, paths) =>
    importInstanceMods(instanceId as string, paths as string[])
  )
  handle('instance:pick-jars', async () => {
    const result = await dialog.showOpenDialog({
      title: 'Add mods',
      properties: ['openFile', 'multiSelections'],
      filters: [{ name: 'Minecraft mods', extensions: ['jar'] }]
    })
    return result.canceled ? [] : result.filePaths
  })
  handle('instance:delete-mod', (instanceId, fileName) =>
    deleteInstanceMod(instanceId as string, fileName as string)
  )
  handle('instance:delete', (instanceId) => deleteInstance(instanceId as string))
  handle('instance:launch', async (id) => {
    const instance = getInstances().find((item: GameInstance) => item.id === id)
    if (!instance) return { ok: false, error: 'Instance not found' }
    try {
      return await launchInstance(instance)
    } catch (error) {
      return { ok: false, error: flattenError(error) }
    }
  })
  handle('instance:stop', () => stopInstance())
  handle('instance:run-state', () => getRunStatus())

  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})