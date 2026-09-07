import { useEffect, useId, useRef, useState } from 'react'
import type { CreateInstanceRequest, ModLoaderId, VersionManifest } from '../../../shared/types'
import { FieldSelect } from './FieldSelect'

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
  const nameRef = useRef<HTMLInputElement>(null)
  const nameId = useId()

  useEffect(() => {
    const focus = (): void => nameRef.current?.focus()
    const id = window.setTimeout(focus, 50)
    void window.tidal.versionManifest().then((data) => {
      setManifest(data)
      setMinecraftVersion(data.latest.release)
    })
    const stop = window.tidal.onInstallProgress((progress) => setStatus(progress.message))
    return () => {
      window.clearTimeout(id)
      stop()
    }
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
      const raw = err instanceof Error ? err.message : String(err)
      setError(raw.replace(/^Error invoking remote method '[^']+':\s*/, '') || 'Could not create the instance.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="animate-fade-in no-drag fixed inset-0 z-[80] flex h-full w-full items-center justify-center bg-black/55 p-6 text-mist">
      <div
        className="animate-rise no-drag w-full max-w-lg rounded-2xl border border-line bg-ink p-6"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <p className="text-[11px] uppercase tracking-[0.22em] text-mute">New instance</p>
        <h2 className="mt-1 text-2xl font-semibold">Create instance</h2>

        <label htmlFor={nameId} className="mt-5 block text-xs text-mute">
          Instance name
        </label>
        <input
          id={nameId}
          ref={nameRef}
          autoFocus
          type="text"
          name="instanceName"
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="My survival world"
          className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2.5 text-sm text-white outline-none placeholder:text-mute focus:border-tidal"
        />

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

        <FieldSelect
          label="Minecraft version"
          value={minecraftVersion}
          onChange={setMinecraftVersion}
          options={versions.map((version) => ({ value: version.id, label: version.id }))}
        />

        <FieldSelect
          label="Mod loader"
          value={loader}
          onChange={(next) => setLoader(next as ModLoaderId)}
          options={LOADERS.map((item) => ({ value: item.id, label: item.label }))}
        />

        {loader !== 'vanilla' ? (
          <FieldSelect
            label="Loader version"
            value={loaderVersion}
            onChange={setLoaderVersion}
            options={loaderVersions.map((version) => ({ value: version, label: version }))}
          />
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
