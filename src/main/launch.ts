import { BrowserWindow } from 'electron'
import { spawn, type ChildProcess } from 'node:child_process'
import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { launch, createMinecraftProcessWatcher, DEFAULT_EXTRA_JVM_ARGS } from '@xmcl/core'
import type { GameInstance, InstanceRunStatus, LaunchResult } from '../shared/types'
import { getAccessToken, getLiveSession } from './auth'
import { findJava } from './java'
import { ensureBuiltinRuntime, instancesRoot, minecraftRoot } from './install'
import { getSettings, upsertInstance } from './store'

let running: { instanceId: string; process: ChildProcess; state: 'starting' | 'running' } | null = null
let readyFallback: ReturnType<typeof setTimeout> | null = null

function broadcast(status: InstanceRunStatus): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send('instance:run-state', status)
  }
}

export function getRunStatus(): InstanceRunStatus {
  if (!running) return { instanceId: null, state: 'idle' }
  return { instanceId: running.instanceId, state: running.state }
}

function clearReadyFallback(): void {
  if (readyFallback) {
    clearTimeout(readyFallback)
    readyFallback = null
  }
}

function markStopped(error?: string): void {
  clearReadyFallback()
  running = null
  broadcast({ instanceId: null, state: 'idle', error })
}

function killProcessTree(child: ChildProcess): void {
  if (!child.pid) return
  if (process.platform === 'win32') {
    spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], {
      stdio: 'ignore',
      windowsHide: true
    })
    return
  }
  child.kill('SIGTERM')
}

export async function stopInstance(): Promise<void> {
  if (!running) return
  const child = running.process
  killProcessTree(child)
  markStopped()
}

function tail(text: string, lines = 16): string {
  return text
    .trim()
    .split(/\r?\n/)
    .filter(Boolean)
    .slice(-lines)
    .join('\n')
}

function summarizeCrash(gameDir: string, output: string): string {
  const fromLog = crashHint(output)
  if (fromLog) return fromLog
  const reports = join(gameDir, 'crash-reports')
  if (!existsSync(reports)) return ''
  const latest = readdirSync(reports)
    .filter((name) => name.endsWith('.txt'))
    .sort()
    .at(-1)
  if (!latest) return ''
  try {
    return crashHint(readFileSync(join(reports, latest), 'utf8'))
  } catch {
    return ''
  }
}

function crashHint(text: string): string {
  const mixin = text.match(/Mixin apply for mod [^\n]+/)
  if (mixin?.[0]) {
    const invalid = text.match(/InvalidInjectionException: ([^\n]+)/)
    return invalid ? `${mixin[0]}\n${invalid[1]}` : mixin[0]
  }
  const exception = text.match(/^java\.[^\n]+/m)
  const description = text.match(/^Description: ([^\n]+)/m)
  if (exception || description) {
    return [description?.[1], exception?.[0]].filter(Boolean).join('\n')
  }
  return ''
}

export async function launchInstance(instance: GameInstance): Promise<LaunchResult> {
  const session = getLiveSession()
  const token = getAccessToken()
  if (!session.loggedIn || !session.profile || !token) {
    return { ok: false, error: 'Sign in with Microsoft before launching.' }
  }
  if (running) {
    return { ok: false, error: 'Minecraft is already running.' }
  }

  try {
    const settings = getSettings()
    const javaPath = await findJava(settings.javaPath || undefined)
    const ready = await ensureBuiltinRuntime(instance)
    const gameDir = join(instancesRoot(), ready.id)
    const logFile = join(gameDir, 'logs', 'tidal-launch.log')
    mkdirSync(join(gameDir, 'logs'), { recursive: true })
    writeFileSync(logFile, `java=${javaPath}\nversion=${ready.versionId}\n`, 'utf8')

    broadcast({ instanceId: instance.id, state: 'starting' })

    const child = await launch({
      gamePath: gameDir,
      resourcePath: minecraftRoot(),
      javaPath,
      version: ready.versionId,
      accessToken: token,
      gameProfile: { id: session.profile.id, name: session.profile.name },
      userType: 'msa' as 'mojang',
      launcherName: 'Tidal Client',
      launcherBrand: 'Tidal Client',
      minMemory: settings.minMemoryMb,
      maxMemory: settings.maxMemoryMb,
      extraJVMArgs: [
        ...DEFAULT_EXTRA_JVM_ARGS.filter((flag) => !flag.startsWith('-Xmx')),
        '-XX:+ParallelRefProcEnabled',
        '-XX:MaxTenuringThreshold=1',
        '-XX:+DisableExplicitGC',
        '-XX:+AlwaysPreTouch',
        '-XX:+PerfDisableSharedMem',
        '-XX:+UseStringDeduplication',
        '-Dminecraft.launcher.brand=Tidal Client',
        '-Dminecraft.launcher.name=Tidal Client',
        '-Djava.net.preferIPv4Stack=true',
        '-Dsun.rmi.dgc.server.gcInterval=2147483646',
        '-XX:G1MixedGCCountTarget=4'
      ],
      extraExecOption: {
        detached: false,
        windowsHide: false,
        stdio: ['ignore', 'pipe', 'pipe']
      }
    })

    running = { instanceId: instance.id, process: child, state: 'starting' }

    for (const win of BrowserWindow.getAllWindows()) {
      win.webContents.setBackgroundThrottling(true)
    }

    let output = ''
    let pendingLog = ''
    const flushLog = (): void => {
      if (!pendingLog) return
      try {
        appendFileSync(logFile, pendingLog)
      } catch {
        /* ignore log write */
      }
      pendingLog = ''
    }
    const collect = (chunk: Buffer): void => {
      const text = chunk.toString()
      output += text
      if (output.length > 120_000) output = output.slice(-60_000)
      pendingLog += text
      if (pendingLog.length >= 16_384) flushLog()
    }
    child.stdout?.on('data', collect)
    child.stderr?.on('data', collect)

    const watcher = createMinecraftProcessWatcher(child)
    watcher.on('minecraft-window-ready', () => {
      if (!running || running.process !== child) return
      running.state = 'running'
      clearReadyFallback()
      broadcast({ instanceId: instance.id, state: 'running' })
    })
    watcher.on('minecraft-exit', (event) => {
      if (running?.process !== child) return
      flushLog()
      const crashed = event.code !== 0 && event.code != null
      const hint = summarizeCrash(gameDir, output)
      markStopped(
        crashed
          ? hint || tail(output) || `Minecraft exited with code ${event.code}`
          : undefined
      )
    })
    watcher.on('error', (error: unknown) => {
      if (running?.process !== child) return
      flushLog()
      markStopped(error instanceof Error ? error.message : 'Minecraft failed to start')
    })

    readyFallback = setTimeout(() => {
      if (!running || running.process !== child || running.state !== 'starting') return
      running.state = 'running'
      broadcast({ instanceId: instance.id, state: 'running' })
    }, 25_000)

    upsertInstance({ ...ready, lastPlayed: Date.now() })
    return { ok: true }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    console.error('Launch failed', error)
    markStopped(message)
    return { ok: false, error: message }
  }
}
