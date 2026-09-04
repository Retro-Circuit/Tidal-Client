import { contextBridge, ipcRenderer } from 'electron'
import type {
  AppSettings,
  CreateInstanceRequest,
  GameInstance,
  InstallProgress,
  ModLoaderId,
  ModpackCard,
  ProjectDetails,
  SearchResult,
  SessionState,
  VersionManifest
} from '../shared/types'

const api = {
  minimize: () => ipcRenderer.invoke('window:minimize'),
  maximize: () => ipcRenderer.invoke('window:maximize'),
  close: () => ipcRenderer.invoke('window:close'),
  login: () => ipcRenderer.invoke('auth:login') as Promise<SessionState>,
  logout: () => ipcRenderer.invoke('auth:logout') as Promise<SessionState>,
  session: () => ipcRenderer.invoke('auth:session') as Promise<SessionState>,
  getSettings: () => ipcRenderer.invoke('settings:get') as Promise<AppSettings>,
  setSettings: (patch: Partial<AppSettings>) =>
    ipcRenderer.invoke('settings:set', patch) as Promise<AppSettings>,
  searchModpacks: (query: string) => ipcRenderer.invoke('discover:search', query) as Promise<SearchResult>,
  projectDetails: (card: ModpackCard) =>
    ipcRenderer.invoke('discover:details', card) as Promise<ProjectDetails>,
  versionManifest: () => ipcRenderer.invoke('minecraft:versions') as Promise<VersionManifest>,
  loaderVersions: (loader: ModLoaderId, minecraftVersion: string) =>
    ipcRenderer.invoke('minecraft:loaders', loader, minecraftVersion) as Promise<string[]>,
  listInstances: () => ipcRenderer.invoke('instance:list') as Promise<GameInstance[]>,
  installModpack: (pack: ModpackCard) => ipcRenderer.invoke('instance:install', pack) as Promise<GameInstance>,
  installContent: (pack: ModpackCard, instanceId: string) =>
    ipcRenderer.invoke('instance:install-content', pack, instanceId) as Promise<GameInstance>,
  createInstance: (request: CreateInstanceRequest) =>
    ipcRenderer.invoke('instance:create', request) as Promise<GameInstance>,
  createVanilla: (version?: string) => ipcRenderer.invoke('instance:vanilla', version) as Promise<GameInstance>,
  launchInstance: (id: string) =>
    ipcRenderer.invoke('instance:launch', id) as Promise<{ ok: boolean; error?: string }>,
  getInstanceMods: (instanceId: string) =>
    ipcRenderer.invoke('get-instance-mods', instanceId) as Promise<{ fileName: string; size: number }[]>,
  deleteInstanceMod: (payload: { instanceId: string; fileName: string }) =>
    ipcRenderer.invoke('delete-instance-mod', payload) as Promise<boolean>,
  onInstallProgress: (callback: (progress: InstallProgress) => void) => {
    const listener = (_event: unknown, payload: InstallProgress): void => callback(payload)
    ipcRenderer.on('install:progress', listener)
    return (): void => {
      ipcRenderer.removeListener('install:progress', listener)
    }
  }
}

contextBridge.exposeInMainWorld('tidal', api)

export type TidalApi = typeof api