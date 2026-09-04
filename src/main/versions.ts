import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)

const {
  installFabric,
  installQuiltVersion,
  installNeoForged,
  installForge,
  installDependencies,
  getVersionList,
  install
} = require('@xmcl/installer')

import type { ModLoaderId, VersionManifest } from '../shared/types'

const MANIFEST_URL = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'
const USER_AGENT = 'TidalClient/0.1.0 (tidal-client)'

export async function fetchVersionManifest(): Promise<VersionManifest> {
  const res = await fetch(MANIFEST_URL, { headers: { 'User-Agent': USER_AGENT } })
  if (!res.ok) throw new Error(`Could not load Minecraft versions (${res.status})`)
  const data = (await res.json()) as {
    latest: { release: string; snapshot: string }
    versions: Array<{ id: string; type: string; releaseTime: string }>
  }
  return {
    latest: data.latest,
    releases: data.versions.filter((v) => v.type === 'release'),
    snapshots: data.versions.filter((v) => v.type === 'snapshot')
  }
}

function neoForgePrefix(minecraftVersion: string): string {
  const withoutOne = minecraftVersion.startsWith('1.') ? minecraftVersion.slice(2) : minecraftVersion
  const parts = withoutOne.split('.')
  if (parts.length === 1) return `${parts[0]}.0.`
  return `${parts[0]}.${parts[1]}.`
}

async function fetchNeoForgeVersions(minecraftVersion: string): Promise<string[]> {
  const res = await fetch(
    'https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml',
    { headers: { 'User-Agent': USER_AGENT } }
  )
  if (!res.ok) return []
  const xml = await res.text()
  const versions = [...xml.matchAll(/<version>([^<]+)<\/version>/g)].map((m) => m[1])
  const prefix = neoForgePrefix(minecraftVersion)
  return versions.filter((v) => v.startsWith(prefix)).reverse()
}

export async function fetchLoaderVersions(loader: ModLoaderId, minecraftVersion: string): Promise<string[]> {
  if (loader === 'vanilla') return []
  if (loader === 'fabric') {
    const artifacts = await getLoaderArtifactListFor(minecraftVersion)
    return artifacts.map((item) => item.loader.version)
  }
  if (loader === 'quilt') {
    const artifacts = await getQuiltLoaderVersionsByMinecraft({ minecraftVersion })
    return artifacts.map((item) => item.loader.version)
  }
  if (loader === 'forge') {
    const list = await getForgeVersionList({ minecraft: minecraftVersion })
    return list.versions.map((item) => item.version)
  }
  if (loader === 'neoforge') {
    return fetchNeoForgeVersions(minecraftVersion)
  }
  return []
}
