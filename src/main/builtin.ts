import { copyFile, cp, mkdir, unlink } from 'node:fs/promises'
import { existsSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { app } from 'electron'
import { getWallet } from './store'
import { writeWalletPoints, writeWalletToInstance } from './wallet'

export const BUILTIN_MOD_FILE = 'tidal-builtin.jar'

export function isHiddenBuiltin(fileName: string): boolean {
  return fileName.toLowerCase().startsWith('tidal-builtin')
}

export function canLoadBuiltinJar(minecraftVersion: string): boolean {
  const version = minecraftVersion.trim()
  if (/^\d{2}\.\d/.test(version)) return true
  const match = /^1\.(\d+)(?:\.(\d+))?/.exec(version)
  if (!match) return true
  const minor = Number(match[1])
  const patch = match[2] != null ? Number(match[2]) : 0
  if (minor > 20) return true
  if (minor === 20) return patch >= 5
  return false
}

function gradleBuiltJar(): string | null {
  const libs = join(process.cwd(), 'bundled-mod', 'build', 'libs')
  if (!existsSync(libs)) return null
  const jar = readdirSync(libs).find(
    (name) =>
      name.startsWith('tidal-builtin') &&
      name.endsWith('.jar') &&
      !name.endsWith('-sources.jar') &&
      !name.includes('-dev')
  )
  return jar ? join(libs, jar) : null
}

export function resolveBuiltinMod(): string | null {
  const candidates = [
    join(process.cwd(), 'resources', 'tidal-builtin.jar'),
    gradleBuiltJar(),
    join(__dirname, '../../resources/tidal-builtin.jar'),
    join(app.getAppPath(), 'resources', 'tidal-builtin.jar'),
    join(process.resourcesPath, 'tidal-builtin.jar')
  ]
  return candidates.find((path): path is string => Boolean(path) && existsSync(path)) ?? null
}

function resolveCapesDir(): string | null {
  const candidates = [
    join(process.cwd(), 'capes'),
    join(__dirname, '../../capes'),
    join(app.getAppPath(), 'capes'),
    join(process.resourcesPath, 'capes')
  ]
  return candidates.find((path) => existsSync(join(path, 'catalog.json'))) ?? null
}

export async function injectBuiltinMod(
  instanceDir: string,
  minecraftVersion?: string
): Promise<void> {
  const modsDir = join(instanceDir, 'mods')
  await mkdir(modsDir, { recursive: true })
  const dest = join(modsDir, BUILTIN_MOD_FILE)
  const source = resolveBuiltinMod()
  if (source && (minecraftVersion == null || canLoadBuiltinJar(minecraftVersion))) {
    await copyFile(source, dest)
  } else if (existsSync(dest)) {
    await unlink(dest)
  }
  const capes = resolveCapesDir()
  if (capes) {
    await cp(capes, join(instanceDir, 'tidal-capes'), { recursive: true })
  }
  const points = getWallet().points
  writeWalletPoints(points)
  writeWalletToInstance(instanceDir, points)
}
