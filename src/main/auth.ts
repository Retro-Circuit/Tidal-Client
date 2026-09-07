import { Auth } from 'msmc'
import type { MinecraftProfile, SessionState } from '../shared/types'
import { getSession, setSession } from './store'

let accessToken: string | null = null
let profile: MinecraftProfile | null = null
let mcExp = 0
let authLock: Promise<unknown> | null = null

function avatarFor(id: string): string {
  return `https://crafatar.com/avatars/${id}?overlay=true&size=64`
}

function tokenExpiry(mcToken: string, fallbackExp?: number): number {
  try {
    const part = mcToken.split('.')[1] ?? ''
    const normalized = part.replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(Buffer.from(normalized, 'base64').toString()) as { exp?: number }
    if (payload.exp) return payload.exp * 1000
  } catch {
    /* use fallback */
  }
  return fallbackExp ?? Date.now() + 23 * 60 * 60 * 1000
}

function tokenStillValid(exp: number): boolean {
  return exp - 60_000 > Date.now()
}

function applyMinecraft(mc: { mcToken: string; profile?: { id: string; name: string }; exp?: number }): void {
  if (!mc.profile) {
    throw new Error('Could not load the Minecraft profile for this account.')
  }
  accessToken = mc.mcToken
  mcExp = tokenExpiry(mc.mcToken, mc.exp)
  profile = {
    id: mc.profile.id,
    name: mc.profile.name,
    avatar: avatarFor(mc.profile.id)
  }
}

function persist(refreshToken: string): void {
  if (!profile || !accessToken) return
  setSession({
    refreshToken,
    profile: { id: profile.id, name: profile.name },
    mcToken: accessToken,
    mcExp
  })
}

function clearLive(): void {
  accessToken = null
  profile = null
  mcExp = 0
}

export function getLiveSession(): SessionState {
  return {
    loggedIn: Boolean(accessToken && profile),
    profile
  }
}

export function getAccessToken(): string | null {
  return accessToken
}

function isRateLimit(error: unknown): boolean {
  if (!error || typeof error !== 'object') return false
  const response = (error as { response?: { status?: number } }).response
  return response?.status === 429
}

function describeAuthError(error: unknown): Error {
  if (isRateLimit(error)) {
    return new Error('Minecraft login is rate-limited. Wait about a minute, then try again.')
  }
  if (error instanceof Error) return error
  if (typeof error === 'string') return new Error(error)
  const ts = error && typeof error === 'object' ? (error as { ts?: string }).ts : undefined
  if (ts === 'error.auth.minecraft.login') {
    return new Error('Could not sign in to Minecraft. Try again in a moment.')
  }
  return new Error('Login failed')
}

async function sleep(ms: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

async function getMinecraftWithRetry(xbox: {
  getMinecraft: () => Promise<{ mcToken: string; profile?: { id: string; name: string }; exp?: number; isDemo?: () => boolean }>
}): Promise<{ mcToken: string; profile?: { id: string; name: string }; exp?: number; isDemo: () => boolean }> {
  let delay = 2000
  let lastError: unknown
  for (let attempt = 0; attempt < 5; attempt++) {
    try {
      const mc = await xbox.getMinecraft()
      return mc as { mcToken: string; profile?: { id: string; name: string }; exp?: number; isDemo: () => boolean }
    } catch (error) {
      lastError = error
      if (!isRateLimit(error) || attempt === 4) throw describeAuthError(error)
      await sleep(delay)
      delay *= 2
    }
  }
  throw describeAuthError(lastError)
}

async function withAuthLock<T>(fn: () => Promise<T>): Promise<T> {
  while (authLock) {
    try {
      await authLock
    } catch {
      /* previous attempt already reported */
    }
  }
  const run = fn()
  authLock = run
  try {
    return await run
  } finally {
    if (authLock === run) authLock = null
  }
}

export async function restoreSession(): Promise<SessionState> {
  return withAuthLock(async () => {
    if (accessToken && profile && tokenStillValid(mcExp)) {
      return getLiveSession()
    }

    const saved = getSession()
    if (!saved?.refreshToken) return getLiveSession()

    if (saved.mcToken && saved.profile && tokenStillValid(saved.mcExp ?? 0)) {
      accessToken = saved.mcToken
      mcExp = saved.mcExp ?? tokenExpiry(saved.mcToken)
      profile = {
        id: saved.profile.id,
        name: saved.profile.name,
        avatar: avatarFor(saved.profile.id)
      }
      return getLiveSession()
    }

    try {
      const auth = new Auth('select_account')
      const xbox = await auth.refresh(saved.refreshToken)
      const mc = await getMinecraftWithRetry(xbox)
      applyMinecraft(mc)
      persist(xbox.save())
    } catch {
      if (saved.mcToken && saved.profile && tokenStillValid(saved.mcExp ?? 0)) {
        accessToken = saved.mcToken
        mcExp = saved.mcExp ?? 0
        profile = {
          id: saved.profile.id,
          name: saved.profile.name,
          avatar: avatarFor(saved.profile.id)
        }
      }
    }
    return getLiveSession()
  })
}

export async function loginWithMicrosoft(): Promise<SessionState> {
  return withAuthLock(async () => {
    try {
      const auth = new Auth('select_account')
      const xbox = await auth.launch('electron', {
        width: 520,
        height: 700,
        resizable: false,
        title: 'Sign in to Tidal Client'
      })
      const mc = await getMinecraftWithRetry(xbox)
      if (mc.isDemo()) {
        throw new Error('This Microsoft account does not own Minecraft Java Edition.')
      }
      applyMinecraft(mc)
      persist(xbox.save())
      return getLiveSession()
    } catch (error) {
      throw describeAuthError(error)
    }
  })
}

export function logout(): SessionState {
  clearLive()
  setSession(null)
  return getLiveSession()
}
