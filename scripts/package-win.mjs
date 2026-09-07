import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const args = new Set(process.argv.slice(2))
const env = {
  ...process.env,
  CSC_IDENTITY_AUTO_DISCOVERY: 'false'
}

function run(command, commandArgs) {
  const result = spawnSync(command, commandArgs, {
    cwd: root,
    stdio: 'inherit',
    env,
    shell: process.platform === 'win32'
  })
  if ((result.status ?? 1) !== 0) {
    process.exit(result.status ?? 1)
  }
}

if (args.has('--mod')) {
  run(process.execPath, [join(root, 'scripts', 'build-mod.mjs')])
} else if (!existsSync(join(root, 'resources', 'tidal-builtin.jar'))) {
  console.warn('resources/tidal-builtin.jar is missing. Run npm run dist:full to build the Fabric mod first.')
}

run('npx', ['electron-vite', 'build'])

const builderArgs = ['electron-builder', '--win', '--x64']
if (args.has('--dir')) {
  builderArgs.push('--dir')
}
run('npx', builderArgs)

const release = join(root, 'release')
console.log(`\nWindows build written to:\n  ${release}`)
console.log('  Installer: TidalClient-Setup-<version>.exe')
console.log('  Portable:  TidalClient-<version>-portable.exe')
