import { useEffect, useState } from 'react'
import type { CreateInstanceRequest, ModLoaderId, VersionManifest } from '../../../shared/types'

const LOADERS: { id: ModLoaderId; label: string }[] = [
  { id: 'vanilla', label: 'Vanilla' },
  { id: 'fabric', label: 'Fabric' },
  { id: 'forge', label: 'Forge' },
  { id: 'quilt', label: 'Quilt' },
  { id: 'neoforge', label: 'NeoForge' }
]

export function CreateInstanceModal({
  onClose,
  onCreated
}: {
  onClose: () => void
  onCreated: () => void
}) {
  const [manifest, setManifest] = useState<VersionManifest | null>(null)
  const [name, setName] = useState('')
  const [channel, setChannel] = useState<'release' | 'snapshot'>('release')
  const [minecraftVersion, setMinecraftVersion] = useState('')
  const [loader, setLoader] = useState<ModLoaderId>('vanilla')
  const [loaderVersion, setLoaderVersion] = useState('')
  const [loaderVersions, setLoaderVersions] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)

  useEffect(() => {
    void window.tidal.versionManifest().then((data) => {
      setManifest(data)
      setMinecraftVersion(data.latest.release)
    })
    return window.tidal.onInstallProgress((progress) => setStatus(progress.message))
  }, [])

  useEffect(() => {
    if (!minecraftVersion || loader === 'vanilla') {
      setLoaderVersions([])
      setLoaderVersion('')
      return
    }
    let cancelled = false
    window.tidal
      .loaderVersions(loader, minecraftVersion)
      .then((versions) => {
        if (cancelled) return
        setLoaderVersions(versions)
        setLoaderVersion(versions[0] ?? '')
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
    return () => {
      cancelled = true
    }
  }, [loader, minecraftVersion])

  const versions = channel === 'release' ? manifest?.releases ?? [] : manifest?.snapshots ?? []

  async function create(): Promise<void> {
    if (!minecraftVersion) return
    if (loader !== 'vanilla' && !loaderVersion) {
      setError('No loader version is available for this Minecraft version.')
      return
    }
    setBusy(true)
    setError(null)
    const request: CreateInstanceRequest = {
      name: name.trim() || `${LOADERS.find((item) => item.id === loader)?.label} ${minecraftVersion}`,
      minecraftVersion,
      loader,
      loaderVersion
    }
    try {
      await window.tidal.createInstance(request)
      onCreated()
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70 p-6">
      <div className="w-full max-w-lg rounded-3xl border border-line bg-ink p-6">
        <p className="text-[11px] uppercase tracking-[0.22em] text-mute">New instance</p>
        <h2 className="mt-1 text-2xl font-semibold">Create instance</h2>

        <label className="mt-5 block text-xs text-mute">
          Instance name
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="My survival world"
            className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2.5 text-sm text-white outline-none focus:border-tidal"
          />
        </label>

        <div className="mt-4 flex gap-2">
          {(['release', 'snapshot'] as const).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => {
                setChannel(item)
                const next = item === 'release' ? manifest?.latest.release : manifest?.latest.snapshot
                if (next) setMinecraftVersion(next)
              }}
              className={`rounded-full px-3 py-1 text-xs uppercase tracking-wide ${
                channel === item ? 'bg-tidal text-white' : 'bg-raised text-mute'
              }`}
            >
              {item}
            </button>
          ))}
        </div>

        <label className="mt-3 block text-xs text-mute">
          Minecraft version
          <select
            value={minecraftVersion}
            onChange={(e) => setMinecraftVersion(e.target.value)}
            className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2.5 text-sm text-white outline-none focus:border-tidal"
          >
            {versions.map((version) => (
              <option key={version.id} value={version.id}>
                {version.id}
              </option>
            ))}
          </select>
        </label>

        <label className="mt-3 block text-xs text-mute">
          Mod loader
          <select
            value={loader}
            onChange={(e) => setLoader(e.target.value as ModLoaderId)}
            className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2.5 text-sm text-white outline-none focus:border-tidal"
          >
            {LOADERS.map((item) => (
              <option key={item.id} value={item.id}>
                {item.label}
              </option>
            ))}
          </select>
        </label>

        {loader !== 'vanilla' ? (
          <label className="mt-3 block text-xs text-mute">
            Loader version
            <select
              value={loaderVersion}
              onChange={(e) => setLoaderVersion(e.target.value)}
              className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2.5 text-sm text-white outline-none focus:border-tidal"
            >
              {loaderVersions.length === 0 ? <option value="">None found</option> : null}
              {loaderVersions.map((version) => (
                <option key={version} value={version}>
                  {version}
                </option>
              ))}
            </select>
          </label>
        ) : null}

        {status && busy ? <p className="mt-3 text-sm text-mist">{status}</p> : null}
        {error ? <p className="mt-3 text-sm text-red-300">{error}</p> : null}

        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-line px-4 py-2 text-sm text-mist hover:text-white"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={busy || !minecraftVersion}
            onClick={() => void create()}
            className="rounded-xl bg-tidal px-4 py-2 text-sm font-semibold shadow-[0_0_16px_rgba(3,73,252,0.4)] hover:brightness-110 disabled:opacity-50"
          >
            {busy ? 'Creating…' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  )
}
