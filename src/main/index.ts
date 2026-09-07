import { app, BrowserWindow, ipcMain, Menu, nativeImage, shell } from 'electron'
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

  ipcMain.handle('auth:login', () => loginWithMicrosoft())
  ipcMain.handle('auth:logout', () => logout())
  ipcMain.handle('auth:session', () => restoreSession())

  ipcMain.handle('settings:get', () => getSettings())
  ipcMain.handle('settings:set', (_e, patch) => setSettings(patch))

  ipcMain.handle('wallet:get', () => getWallet())
  ipcMain.handle('wallet:claim-daily', () => claimDaily())
  ipcMain.handle('wallet:grant-shadow', () => grantShadowPoints())

  ipcMain.handle('discover:search', (_e, query: string, options?: DiscoverSearch) =>
    searchModpacks(query, options)
  )
  ipcMain.handle('discover:details', (_e, card: ModpackCard) => fetchProjectDetails(card))
  ipcMain.handle('minecraft:versions', () => fetchVersionManifest())
  ipcMain.handle('minecraft:loaders', (_e, loader, minecraftVersion: string) =>
    fetchLoaderVersions(loader, minecraftVersion)
  )

  ipcMain.handle('instance:list', () => getInstances())
  ipcMain.handle('instance:scan-foreign', () => scanForeignInstances())
  ipcMain.handle('instance:import-foreign', (_e, items: ForeignInstance[]) => importForeignInstances(items))
  ipcMain.handle('instance:create', async (_e, request: CreateInstanceRequest) => {
    try {
      return await createCustomInstance(request)
    } catch (error) {
      throw new Error(flattenError(error))
    }
  })
  ipcMain.handle('instance:vanilla', async (_e, version?: string) => {
    try {
      return await createVanillaInstance(version)
    } catch (error) {
      throw new Error(flattenError(error))
    }
  })
  ipcMain.handle('instance:install', async (_e, pack: ModpackCard) => {
    try {
      return await installModpack(pack)
    } catch (error) {
      throw new Error(flattenError(error))
    }
  })
  ipcMain.handle('instance:install-content', async (_e, pack: ModpackCard, instanceId: string) => {
    try {
      return await installContentToInstance(pack, instanceId)
    } catch (error) {
      throw new Error(flattenError(error))
    }
  })
  ipcMain.handle('instance:mods', (_e, instanceId: string) => listInstanceMods(instanceId))
  ipcMain.handle('instance:import-mods', (_e, instanceId: string, paths: string[]) =>
    importInstanceMods(instanceId, paths)
  )
  ipcMain.handle('instance:delete-mod', (_e, instanceId: string, fileName: string) =>
    deleteInstanceMod(instanceId, fileName)
  )
  ipcMain.handle('instance:delete', (_e, instanceId: string) => deleteInstance(instanceId))
  ipcMain.handle('instance:launch', async (_e, id: string) => {
    const instance = getInstances().find((item: GameInstance) => item.id === id)
    if (!instance) return { ok: false, error: 'Instance not found' }
    return launchInstance(instance)
  })
  ipcMain.handle('instance:stop', () => stopInstance())
  ipcMain.handle('instance:run-state', () => getRunStatus())

  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})