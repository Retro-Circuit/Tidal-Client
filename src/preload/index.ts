import { contextBridge, ipcRenderer } from 'electron'
import type {
  AppSettings,
  CreateInstanceRequest,
  GameInstance,
  InstallProgress,
  InstanceModFile,
  InstanceRunStatus,
  ModLoaderId,
  ModpackCard,
  ProjectDetails,
  SearchResult,
  SessionState,
  VersionManifest,
  WalletState
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
  getWallet: () => ipcRenderer.invoke('wallet:get') as Promise<WalletState>,
  claimDaily: () => ipcRenderer.invoke('wallet:claim-daily') as Promise<WalletState>,
  grantShadowPoints: () => ipcRenderer.invoke('wallet:grant-shadow') as Promise<WalletState>,
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
  stopInstance: () => ipcRenderer.invoke('instance:stop') as Promise<void>,
  getRunState: () => ipcRenderer.invoke('instance:run-state') as Promise<InstanceRunStatus>,
  onRunState: (callback: (status: InstanceRunStatus) => void) => {
    const listener = (_event: unknown, payload: InstanceRunStatus): void => callback(payload)
    ipcRenderer.on('instance:run-state', listener)
    return (): void => {
      ipcRenderer.removeListener('instance:run-state', listener)
    }
  },
  getInstanceMods: (instanceId: string) =>
    ipcRenderer.invoke('instance:mods', instanceId) as Promise<InstanceModFile[]>,
  deleteInstanceMod: (instanceId: string, fileName: string) =>
    ipcRenderer.invoke('instance:delete-mod', instanceId, fileName) as Promise<void>,
  deleteInstance: (instanceId: string) => ipcRenderer.invoke('instance:delete', instanceId) as Promise<void>,
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