import { app } from 'electron'
import { execFile } from 'node:child_process'
import { existsSync, readdirSync, statSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'
import { promisify } from 'node:util'

const execFileAsync = promisify(execFile)

async function javaMajor(javaPath: string): Promise<number | null> {
  try {
    const { stderr, stdout } = await execFileAsync(javaPath, ['-version'])
    const text = `${stderr}\n${stdout}`
    const match = text.match(/version\s+"(\d+)(?:\.(\d+))?/)
    if (!match) return null
    const major = Number(match[1])
    return major === 1 ? Number(match[2] ?? 8) : major
  } catch {
    return null
  }
}

function walkJavaBins(root: string, depth = 0, found: string[] = []): string[] {
  if (depth > 5 || !existsSync(root)) return found
  const javaExe = process.platform === 'win32' ? 'java.exe' : 'java'
  const direct = join(root, 'bin', javaExe)
  if (existsSync(direct)) found.push(direct)
  try {
    for (const entry of readdirSync(root)) {
      const full = join(root, entry)
      try {
        if (statSync(full).isDirectory()) walkJavaBins(full, depth + 1, found)
      } catch {
        /* skip */
      }
    }
  } catch {
    /* skip */
  }
  return found
}

export async function findJava(preferred?: string): Promise<string> {
  if (preferred && existsSync(preferred)) return preferred

  const candidates = new Set<string>()
  if (process.env.JAVA_HOME) {
    const fromHome = join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
    if (existsSync(fromHome)) candidates.add(fromHome)
  }

  const searchRoots = [
    'C:\\Program Files\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Microsoft',
    'C:\\Program Files\\Zulu',
    'C:\\Program Files\\Amazon Corretto',
    join(homedir(), '.jdks'),
    join(app.getPath('userData'), 'runtime')
  ]

  for (const root of searchRoots) {
    for (const bin of walkJavaBins(root)) candidates.add(bin)
  }

  if (process.platform === 'win32') {
    try {
      const { stdout } = await execFileAsync('where', ['java'])
      for (const line of stdout.split(/\r?\n/)) {
        if (line.trim()) candidates.add(line.trim())
      }
    } catch {
      /* java not on PATH */
    }
  }

  let best: { path: string; major: number } | null = null
  for (const candidate of candidates) {
    const major = await javaMajor(candidate)
    if (!major) continue
    if (!best || major > best.major) best = { path: candidate, major }
  }

  if (!best) {
    throw new Error(
      'No Java runtime found. Install Temurin 21+ or set a Java path in Settings.'
    )
  }
  return best.path
}
