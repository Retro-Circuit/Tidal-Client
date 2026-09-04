import { app, BrowserWindow, ipcMain, nativeImage, shell } from 'electron'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { unlink } from 'node:fs/promises'
import { loginWithMicrosoft, logout, restoreSession } from './auth'
import { fetchProjectDetails, searchModpacks } from './discover'
import { createCustomInstance, createVanillaInstance, installContentToInstance, installModpack } from './install'
import { launchInstance } from './launch'
import { getInstances, getSettings, setSettings } from './store'
import { fetchLoaderVersions, fetchVersionManifest } from './versions'
import type { CreateInstanceRequest, GameInstance, ModpackCard } from '../shared/types'

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
    backgroundColor: '#121212',
    title: 'Tidal Client',
    icon: icon.isEmpty() ? undefined : icon,
    webPreferences: {
      preload: join(__dirname, '../preload/index.cjs'),
      sandbox: false,
      contextIsolation: true,
      nodeIntegration: false
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

  ipcMain.handle('discover:search', (_e, query: string) => searchModpacks(query))
  ipcMain.handle('discover:details', (_e, card: ModpackCard) => fetchProjectDetails(card))
  ipcMain.handle('minecraft:versions', () => fetchVersionManifest())
  ipcMain.handle('minecraft:loaders', (_e, loader, minecraftVersion: string) =>
    fetchLoaderVersions(loader, minecraftVersion)
  )

  ipcMain.handle('instance:list', () => getInstances())
  ipcMain.handle('instance:install', (_e, pack: ModpackCard) => installModpack(pack))
  ipcMain.handle('instance:install-content', (_e, pack: ModpackCard, instanceId: string) =>
    installContentToInstance(pack, instanceId)
  )
  ipcMain.handle('instance:create', (_e, request: CreateInstanceRequest) => createCustomInstance(request))
  ipcMain.handle('instance:vanilla', (_e, version?: string) => createVanillaInstance(version))
  
  // Instance management & mod inspection bindings
  ipcMain.handle('get-instance-mods', async (_e, instanceId: string) => {
    const settings = getSettings()
    const modsDir = join(settings.gameDir, 'instances', instanceId, 'mods')
    if (!existsSync(modsDir)) return []
    
    const files = readdirSync(modsDir)
    return files
      .filter((file) => file.endsWith('.jar'))
      .map((fileName) => ({
        fileName,
        size: statSync(join(modsDir, fileName)).size
      }))
  })

  ipcMain.handle('delete-instance-mod', async (_e, { instanceId, fileName }) => {
    const settings = getSettings()
    const modPath = join(settings.gameDir, 'instances', instanceId, 'mods', fileName)
    if (existsSync(modPath)) {
      await unlink(modPath)
    }
    return true
  })

  ipcMain.handle('instance:launch', async (_e, id: string) => {
    const instance = getInstances().find((item: GameInstance) => item.id === id)
    if (!instance) return { ok: false, error: 'Instance not found' }
    return launchInstance(instance)
  })

  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})