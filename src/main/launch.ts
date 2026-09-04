import { launch } from '@xmcl/core'
import { join } from 'node:path'
import type { GameInstance, LaunchResult } from '../shared/types'
import { getAccessToken, getLiveSession } from './auth'
import { findJava } from './java'
import { instancesRoot, minecraftRoot } from './install'
import { getSettings, upsertInstance } from './store'

export async function launchInstance(instance: GameInstance): Promise<LaunchResult> {
  const session = getLiveSession()
  const token = getAccessToken()
  if (!session.loggedIn || !session.profile || !token) {
    return { ok: false, error: 'Sign in with Microsoft before launching.' }
  }

  try {
    const settings = getSettings()
    const javaPath = await findJava(settings.javaPath || undefined)
    const gameDir = join(instancesRoot(), instance.id)

    await launch({
      gamePath: gameDir,
      resourcePath: minecraftRoot(),
      javaPath,
      version: instance.versionId,
      accessToken: token,
      gameProfile: { id: session.profile.id, name: session.profile.name },
      userType: 'mojang',
      launcherName: 'TidalClient',
      launcherBrand: 'tidal',
      minMemory: settings.minMemoryMb,
      maxMemory: settings.maxMemoryMb,
      extraExecOption: { detached: true, stdio: 'ignore' }
    })

    upsertInstance({ ...instance, lastPlayed: Date.now() })
    return { ok: true }
  } catch (error) {
    return { ok: false, error: error instanceof Error ? error.message : String(error) }
  }
}
