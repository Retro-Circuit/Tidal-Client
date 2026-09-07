import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const wrapper = process.platform === 'win32' ? 'gradlew.bat' : 'gradlew'

function gradleBuild(dir) {
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
  if ((result.status ?? 1) !== 0) {
    process.exit(result.status ?? 1)
  }
}

gradleBuild(join(root, 'bundled-mod'))
gradleBuild(join(root, 'bundled-mod-121'))

const pack = spawnSync(process.execPath, [join(root, 'scripts', 'pack-builtin-releases.mjs')], {
  cwd: root,
  stdio: 'inherit'
})

process.exit(pack.status ?? 1)
