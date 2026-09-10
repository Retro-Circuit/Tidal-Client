export type NavView = 'home' | 'discover' | 'instances' | 'cosmetics' | 'settings'

export type ModpackSource = 'modrinth' | 'curseforge'

export type ProjectType = 'modpack' | 'mod' | 'resourcepack' | 'shader'

export interface MinecraftProfile {
  id: string
  name: string
  avatar?: string
}

export interface SessionState {
  loggedIn: boolean
  profile: MinecraftProfile | null
}

export interface SkinPreview {
  pngBase64: string
  capeBase64?: string | null
  slim: boolean
  source: 'saved' | 'account'
}

export interface AppSettings {
  curseforgeApiKey: string
  modrinthEnabled: boolean
  curseforgeEnabled: boolean
  maxMemoryMb: number
  minMemoryMb: number
  javaPath: string
  importPromptDismissed: boolean
  lastInstanceId: string
}

export interface WalletState {
  points: number
  lastDailyClaimAt: number | null
  nextDailyAt: number
  canClaim: boolean
}

export interface ModpackCard {
  id: string
  source: ModpackSource
  projectType: ProjectType
  title: string
  description: string
  iconUrl: string
  downloads: number
  author: string
  categories: string[]
  slug?: string
  curseProjectId?: number
}

export interface GalleryImage {
  url: string
  title?: string
}

export interface ProjectDetails {
  card: ModpackCard
  bodyHtml: string
  gallery: GalleryImage[]
}

export interface ForeignInstance {
  id: string
  name: string
  launcher: string
  path: string
  minecraftVersion: string
  loader: string
  loaderVersion: string
}

export interface GameInstance {
  id: string
  name: string
  source: ModpackSource | 'vanilla' | 'custom'
  sourceId: string
  iconUrl: string
  minecraftVersion: string
  loader: string
  loaderVersion: string
  versionId: string
  createdAt: number
  lastPlayed?: number
}

export interface InstallProgress {
  instanceId: string
  phase: string
  message: string
  progress: number
  total: number
}

export interface LaunchResult {
  ok: boolean
  error?: string
}

export type InstanceRunState = 'idle' | 'starting' | 'running'

export interface InstanceRunStatus {
  instanceId: string | null
  state: InstanceRunState
  error?: string
}

export interface SearchResult {
  hits: ModpackCard[]
  offset: number
  pageSize: number
  hasMore: boolean
  error?: string
}

export interface DiscoverSearch {
  query?: string
  offset?: number
  projectType?: ProjectType | 'all'
  gameVersion?: string
}

export interface MinecraftVersionInfo {
  id: string
  type: string
  releaseTime: string
}

export interface VersionManifest {
  latest: { release: string; snapshot: string }
  releases: MinecraftVersionInfo[]
  snapshots: MinecraftVersionInfo[]
}

export type ModLoaderId = 'vanilla' | 'fabric' | 'forge' | 'quilt' | 'neoforge'

export interface CreateInstanceRequest {
  name: string
  minecraftVersion: string
  loader: ModLoaderId
  loaderVersion: string
}

export interface InstanceModFile {
  fileName: string
  size: number
}
