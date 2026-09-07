import AdmZip from 'adm-zip'
import { copyFileSync, existsSync, mkdirSync, readdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const fullJar = join(root, 'resources', 'tidal-builtin.jar')
const full121 = join(root, 'resources', 'tidal-builtin-121.jar')
const outDir = join(root, 'resources', 'tidal-builtin')
const compiledVersion = '26.2'
const compiled121 = '1.21.1'
const MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'

function findStubJar() {
  const libs = join(root, 'bundled-mod', 'build', 'libs')
  if (!existsSync(libs)) return null
  return (
    readdirSync(libs).find(
      (name) => name.startsWith('tidal-builtin') && name.includes('-stub') && name.endsWith('.jar')
    ) ?? null
  )
}

function stamp(source, dest, minecraftVersion) {
  const zip = new AdmZip(source)
  const entry = zip.getEntry('fabric.mod.json')
  if (!entry) throw new Error(`No fabric.mod.json in ${source}`)
  const json = JSON.parse(entry.getData().toString('utf8'))
  json.depends = json.depends ?? {}
  json.depends.minecraft = minecraftVersion
  zip.updateFile('fabric.mod.json', Buffer.from(`${JSON.stringify(json, null, 2)}\n`))
  zip.writeZip(dest)
}

async function releaseIds() {
  const res = await fetch(MANIFEST, { headers: { 'User-Agent': 'TidalClient/0.1.0 (tidal-client)' } })
  if (!res.ok) throw new Error(`Version manifest failed (${res.status})`)
  const data = await res.json()
  return data.versions.filter((item) => item.type === 'release').map((item) => item.id)
}

if (!existsSync(fullJar)) {
  console.error('Missing resources/tidal-builtin.jar — build the Fabric mod first.')
  process.exit(1)
}

if (!existsSync(full121)) {
  console.error('Missing resources/tidal-builtin-121.jar — build bundled-mod-121 first.')
  process.exit(1)
}

const stubName = findStubJar()
const stubJar = stubName ? join(root, 'bundled-mod', 'build', 'libs', stubName) : null
if (!stubJar) {
  console.error('Missing stub jar. Run bundled-mod Gradle build so tidal-builtin-*-stub.jar exists.')
  process.exit(1)
}

mkdirSync(outDir, { recursive: true })

const ids = await releaseIds()
if (!ids.includes(compiledVersion)) ids.unshift(compiledVersion)

function sourceFor(id, fullJar, stubJar, full121) {
  if (id === compiledVersion || id.startsWith('26.') || /^26$/.test(id)) return fullJar
  if (id === '1.21' || id.startsWith('1.21.')) return full121
  return stubJar
}

const catalog = {
  compiledVersion,
  compiled121,
  releases: ids,
  families: { '26.x': compiledVersion, '1.21.x': compiled121 }
}
for (const id of ids) {
  const dest = join(outDir, `${id}.jar`)
  stamp(sourceFor(id, fullJar, stubJar, full121), dest, id)
}
writeFileSync(join(outDir, 'catalog.json'), `${JSON.stringify(catalog, null, 2)}\n`)
console.log(`Packed ${ids.length} release jars into ${outDir}`)
