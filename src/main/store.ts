import StoreClass from 'electron-store'
import type { AppSettings, GameInstance, WalletState } from '../shared/types'
import { readWalletPoints, writeWalletPoints } from './wallet'

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
  mcToken?: string
  mcExp?: number
}

interface Schema {
  settings: AppSettings
  instances: GameInstance[]
  session: PersistedSession | null
  tidalPoints: number
  lastDailyClaimAt: number | null
}

export const DAILY_REWARD = 50
export const DAILY_COOLDOWN_MS = 24 * 60 * 60 * 1000
export const SHADOW_GRANT = 100
export const SHADOW_NAME = '_ShadowzYT'

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
    session: null,
    tidalPoints: 0,
    lastDailyClaimAt: null
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

export function removeInstance(id: string): void {
  store.set(
    'instances',
    getInstances().filter((item) => item.id !== id)
  )
}

export function getSession(): PersistedSession | null {
  return store.get('session')
}

export function setSession(session: PersistedSession | null): void {
  store.set('session', session)
}

export function getWallet(): WalletState {
  const filePoints = readWalletPoints()
  const stored = store.get('tidalPoints') ?? 0
  if (filePoints == null) {
    writeWalletPoints(stored)
  }
  const points = filePoints ?? stored
  if (filePoints != null && filePoints !== stored) {
    store.set('tidalPoints', filePoints)
  }
  const lastDailyClaimAt = store.get('lastDailyClaimAt') ?? null
  const nextDailyAt = lastDailyClaimAt == null ? 0 : lastDailyClaimAt + DAILY_COOLDOWN_MS
  return {
    points,
    lastDailyClaimAt,
    nextDailyAt,
    canClaim: Date.now() >= nextDailyAt
  }
}

function setPoints(points: number): void {
  store.set('tidalPoints', points)
  writeWalletPoints(points)
}

export function claimDaily(): WalletState {
  const current = getWallet()
  if (!current.canClaim) {
    return current
  }
  const now = Date.now()
  store.set('lastDailyClaimAt', now)
  setPoints(current.points + DAILY_REWARD)
  return getWallet()
}

export function grantShadowPoints(username?: string | null): WalletState {
  const name = username?.trim() ?? getSession()?.profile.name ?? ''
  if (name.toLowerCase() !== SHADOW_NAME.toLowerCase()) {
    return getWallet()
  }
  const current = getWallet()
  setPoints(current.points + SHADOW_GRANT)
  return getWallet()
}