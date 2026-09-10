import { app } from 'electron'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import type { SkinPreview } from '../shared/types'

const USER_AGENT = 'TidalClient/0.1.0 (tidal-client)'

export async function getSkinPreview(uuid?: string): Promise<SkinPreview | null> {
  const id = (uuid ?? '').replace(/-/g, '').toLowerCase()
  if (id.length < 32) return null
  const capeBase64 = await fetchCape(id)
  const saved = readSaved(id)
  if (saved) return { ...saved, capeBase64 }
  const account = (await fetchMojangSkin(id)) ?? (await fetchFallbackSkin(id))
  return account ? { ...account, capeBase64 } : null
}

function readSaved(uuid: string): SkinPreview | null {
  for (const dir of skinDirs(uuid)) {
    const png = join(dir, 'equipped.png')
    if (!existsSync(png)) continue
    try {
      const meta = JSON.parse(readFileSync(join(dir, 'meta.json'), 'utf8')) as {
        slim?: boolean
        source?: string
      }
      if (meta.source !== 'tidal') continue
      return {
        pngBase64: readFileSync(png).toString('base64'),
        slim: Boolean(meta.slim),
        source: 'saved'
      }
    } catch {
      continue
    }
  }
  return null
}

async function fetchMojangSkin(uuid: string): Promise<SkinPreview | null> {
  try {
    const profile = await fetchJson(`https://sessionserver.mojang.com/session/minecraft/profile/${uuid}`) as {
      properties?: { name: string; value: string }[]
    } | null
    const encoded = profile?.properties?.find((item) => item.name === 'textures')?.value
    if (!encoded) return null
    const decoded = JSON.parse(Buffer.from(encoded, 'base64').toString('utf8')) as {
      textures?: {
        SKIN?: { url?: string; metadata?: { model?: string } }
        CAPE?: { url?: string }
      }
    }
    const skin = decoded.textures?.SKIN
    if (!skin?.url) return null
    const png = await fetchBuffer(skin.url.replace('http://', 'https://'))
    if (!png) return null
    return {
      pngBase64: png.toString('base64'),
      slim: skin.metadata?.model === 'slim',
      source: 'account'
    }
  } catch {
    return null
  }
}

async function fetchCape(uuid: string): Promise<string | null> {
  const fromProfile = await fetchMojangCape(uuid)
  if (fromProfile) return fromProfile
  for (const url of [`https://crafatar.com/capes/${uuid}`, `https://mc-heads.net/cape/${uuid}`]) {
    const png = await fetchBuffer(url)
    if (png && png.length > 200) return png.toString('base64')
  }
  return null
}

async function fetchMojangCape(uuid: string): Promise<string | null> {
  try {
    const profile = (await fetchJson(
      `https://sessionserver.mojang.com/session/minecraft/profile/${uuid}`
    )) as { properties?: { name: string; value: string }[] } | null
    const encoded = profile?.properties?.find((item) => item.name === 'textures')?.value
    if (!encoded) return null
    const decoded = JSON.parse(Buffer.from(encoded, 'base64').toString('utf8')) as {
      textures?: { CAPE?: { url?: string } }
    }
    const url = decoded.textures?.CAPE?.url
    if (!url) return null
    const png = await fetchBuffer(url.replace('http://', 'https://'))
    return png ? png.toString('base64') : null
  } catch {
    return null
  }
}
async function fetchFallbackSkin(uuid: string): Promise<SkinPreview | null> {
  for (const url of [
    `https://crafatar.com/skins/${uuid}`,
    `https://mc-heads.net/skin/${uuid}`,
    `https://minotar.net/skin/${uuid}`
  ]) {
    const png = await fetchBuffer(url)
    if (png && png.length > 200) {
      return { pngBase64: png.toString('base64'), slim: false, source: 'account' }
    }
  }
  return null
}

async function fetchJson(url: string): Promise<unknown | null> {
  const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } })
  if (!res.ok) return null
  return res.json()
}

async function fetchBuffer(url: string): Promise<Buffer | null> {
  try {
    const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } })
    if (!res.ok) return null
    return Buffer.from(await res.arrayBuffer())
  } catch {
    return null
  }
}

function skinDirs(uuid: string): string[] {
  const dirs = [join(app.getPath('userData'), 'skins', uuid)]
  const roaming = process.env.APPDATA
  if (roaming) {
    dirs.push(join(roaming, 'tidal-client', 'skins', uuid))
    dirs.push(join(roaming, 'Tidal Client', 'skins', uuid))
  }
  const home = process.env.USERPROFILE || process.env.HOME || ''
  if (home) {
    dirs.push(join(home, '.config', 'tidal-client', 'skins', uuid))
    dirs.push(join(home, 'Library', 'Application Support', 'tidal-client', 'skins', uuid))
    dirs.push(join(home, 'Library', 'Application Support', 'Tidal Client', 'skins', uuid))
  }
  return dirs
}
