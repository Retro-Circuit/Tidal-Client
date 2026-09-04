import { Auth } from 'msmc'
import type { MinecraftProfile, SessionState } from '../shared/types'
import { getSession, setSession } from './store'

let accessToken: string | null = null
let profile: MinecraftProfile | null = null

function avatarFor(id: string): string {
  return `https://crafatar.com/avatars/${id}?overlay=true&size=64`
}

function applyMinecraft(mc: { mcToken: string; profile?: { id: string; name: string } }): void {
  if (!mc.profile) {
    throw new Error('Could not load the Minecraft profile for this account.')
  }
  accessToken = mc.mcToken
  profile = {
    id: mc.profile.id,
    name: mc.profile.name,
    avatar: avatarFor(mc.profile.id)
  }
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

export async function restoreSession(): Promise<SessionState> {
  const saved = getSession()
  if (!saved?.refreshToken) return getLiveSession()
  try {
    const auth = new Auth('select_account')
    const xbox = await auth.refresh(saved.refreshToken)
    const mc = await xbox.getMinecraft()
    applyMinecraft(mc)
    setSession({ refreshToken: xbox.save(), profile: { id: profile!.id, name: profile!.name } })
  } catch {
    setSession(null)
    accessToken = null
    profile = null
  }
  return getLiveSession()
}

export async function loginWithMicrosoft(): Promise<SessionState> {
  const auth = new Auth('select_account')
  const xbox = await auth.launch('electron', {
    width: 520,
    height: 700,
    resizable: false,
    title: 'Sign in to Tidal Client'
  })
  const mc = await xbox.getMinecraft()
  if (mc.isDemo()) {
    throw new Error('This Microsoft account does not own Minecraft Java Edition.')
  }
  applyMinecraft(mc)
  setSession({ refreshToken: xbox.save(), profile: { id: profile!.id, name: profile!.name } })
  return getLiveSession()
}

export function logout(): SessionState {
  accessToken = null
  profile = null
  setSession(null)
  return getLiveSession()
}
