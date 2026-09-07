import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const dir = join(root, 'bundled-mod')
const wrapper = process.platform === 'win32' ? 'gradlew.bat' : 'gradlew'
const cmd = join(dir, wrapper)

if (!existsSync(cmd)) {
  console.error(`Missing Gradle wrapper: ${cmd}`)
  process.exit(1)
}

const result = spawnSync(cmd, ['build'], {
  cwd: dir,
  stdio: 'inherit',
  shell: process.platform === 'win32'
})

process.exit(result.status ?? 1)
