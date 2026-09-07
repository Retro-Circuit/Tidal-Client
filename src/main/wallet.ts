import { app } from 'electron'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

export const WALLET_FILE = 'tidal-wallet.json'

export function userWalletPath(): string {
  return join(app.getPath('userData'), WALLET_FILE)
}

export function instanceWalletPath(instanceDir: string): string {
  return join(instanceDir, WALLET_FILE)
}

export function readWalletPoints(): number | null {
  try {
    const file = userWalletPath()
    if (!existsSync(file)) return null
    const raw = JSON.parse(readFileSync(file, 'utf8')) as { points?: number }
    return typeof raw.points === 'number' && Number.isFinite(raw.points) ? Math.max(0, Math.floor(raw.points)) : null
  } catch {
    return null
  }
}

export function writeWalletPoints(points: number): void {
  const payload = `${JSON.stringify({ points: Math.max(0, Math.floor(points)) })}\n`
  writeFileSync(userWalletPath(), payload, 'utf8')
}

export function writeWalletToInstance(instanceDir: string, points: number): void {
  writeFileSync(instanceWalletPath(instanceDir), `${JSON.stringify({ points: Math.max(0, Math.floor(points)) })}\n`, 'utf8')
}
