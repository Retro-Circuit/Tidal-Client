import StoreClass from 'electron-store'
import type { AppSettings, GameInstance } from '../shared/types'

// Handle ESM/CJS default export wrapping for electron-store v10+
const Store = (typeof StoreClass === 'function' 
  ? StoreClass 
  : (StoreClass as unknown as { default: typeof StoreClass }).default)

export interface PersistedSession {
  refreshToken: string
  profile: {
    id: string
    name: string
  }
}

interface Schema {
  settings: AppSettings
  instances: GameInstance[]
  session: PersistedSession | null
}

const defaults: AppSettings = {
  curseforgeApiKey: process.env.CURSEFORGE_API_KEY ?? '',
  modrinthEnabled: true,
  curseforgeEnabled: false,
  maxMemoryMb: 4096,
  minMemoryMb: 2048,
  javaPath: ''
}

export const store = new Store<Schema>({
  defaults: {
    settings: defaults,
    instances: [],
    session: null
  }
})

export function getSettings(): AppSettings {
  const current = store.get('settings')
  const merged = { ...defaults, ...current }
  if (!merged.curseforgeApiKey && process.env.CURSEFORGE_API_KEY) {
    merged.curseforgeApiKey = process.env.CURSEFORGE_API_KEY
  }
  return merged
}

export function setSettings(patch: Partial<AppSettings>): AppSettings {
  const next = { ...getSettings(), ...patch }
  store.set('settings', next)
  return next
}

export function getInstances(): GameInstance[] {
  return store.get('instances') ?? []
}

export function upsertInstance(instance: GameInstance): void {
  const rest = getInstances().filter((item) => item.id !== instance.id)
  store.set('instances', [instance, ...rest])
}

export function getSession(): PersistedSession | null {
  return store.get('session')
}

export function setSession(session: PersistedSession | null): void {
  store.set('session', session)
}