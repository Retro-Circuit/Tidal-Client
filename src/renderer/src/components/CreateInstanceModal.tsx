import { useEffect, useId, useRef, useState } from 'react'
import type { CreateInstanceRequest, ModLoaderId, VersionManifest } from '../../../shared/types'
import { FieldSelect } from './FieldSelect'

const LOADERS: { id: ModLoaderId; label: string; hint: string }[] = [
  { id: 'fabric', label: 'Fabric', hint: 'Best for most mods and the Tidal menu' },
  { id: 'vanilla', label: 'Vanilla', hint: 'Unmodded play; Tidal menu still loads' },
  { id: 'forge', label: 'Forge', hint: 'Older and Forge-only packs' },
  { id: 'neoforge', label: 'NeoForge', hint: 'Newer Forge-family mods' },
  { id: 'quilt', label: 'Quilt', hint: 'Fabric-compatible alternative' }
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
  const [loader, setLoader] = useState<ModLoaderId>('fabric')
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
        if (!cancelled) setError(err.message.replace(/^Error invoking remote method '[^']+':\s*/, ''))
      })
    return () => {
      cancelled = true
    }
  }, [loader, minecraftVersion])

  const versions = channel === 'release' ? manifest?.releases ?? [] : manifest?.snapshots ?? []
  const latest = manifest?.latest.release ?? ''

  async function createFrom(request: CreateInstanceRequest): Promise<void> {
    setBusy(true)
    setError(null)
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

  async function quick(kind: 'fabric' | 'vanilla' | 'forge'): Promise<void> {
    if (!latest) return
    if (kind === 'vanilla') {
      await createFrom({
        name: `Vanilla ${latest}`,
        minecraftVersion: latest,
        loader: 'vanilla',
        loaderVersion: ''
      })
      return
    }
    const versionsFor = await window.tidal.loaderVersions(kind, latest)
    const loaderVersionNext = versionsFor[0]
    if (!loaderVersionNext) {
      setError(`No ${kind} loader is available for Minecraft ${latest} yet.`)
      return
    }
    await createFrom({
      name: `${kind === 'fabric' ? 'Fabric' : 'Forge'} ${latest}`,
      minecraftVersion: latest,
      loader: kind,
      loaderVersion: loaderVersionNext
    })
  }

  async function create(): Promise<void> {
    if (!minecraftVersion) return
    if (loader !== 'vanilla' && !loaderVersion) {
      setError('No loader version is available for this Minecraft version.')
      return
    }
    await createFrom({
      name: name.trim() || `${LOADERS.find((item) => item.id === loader)?.label} ${minecraftVersion}`,
      minecraftVersion,
      loader,
      loaderVersion
    })
  }

  return (
    <div className="animate-fade-in no-drag fixed inset-0 z-[80] flex h-full w-full items-center justify-center bg-black/55 p-6 text-mist">
      <div
        className="animate-rise no-drag w-full max-w-lg rounded-2xl border border-line bg-ink p-6"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <p className="text-[11px] uppercase tracking-[0.22em] text-mute">New instance</p>
        <h2 className="mt-1 text-2xl font-semibold">Create instance</h2>
        <p className="mt-1 text-sm text-mute">One click for the latest game, or pick a version below.</p>

        <div className="mt-4 grid gap-2">
          <button
            type="button"
            disabled={busy || !latest}
            onClick={() => void quick('fabric')}
            className="rounded-xl border border-tidal/40 bg-tidal/15 px-4 py-3 text-left hover:bg-tidal/25 disabled:opacity-50"
          >
            <p className="text-sm font-semibold text-white">Play with mods</p>
            <p className="text-xs text-mute">Latest Minecraft + Fabric. Recommended.</p>
          </button>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              disabled={busy || !latest}
              onClick={() => void quick('vanilla')}
              className="rounded-xl border border-line bg-raised px-4 py-3 text-left text-sm hover:border-tidal/40 disabled:opacity-50"
            >
              Vanilla {latest || ''}
            </button>
            <button
              type="button"
              disabled={busy || !latest}
              onClick={() => void quick('forge')}
              className="rounded-xl border border-line bg-raised px-4 py-3 text-left text-sm hover:border-tidal/40 disabled:opacity-50"
            >
              Forge {latest || ''}
            </button>
          </div>
        </div>

        <label htmlFor={nameId} className="mt-5 block text-xs text-mute">
          Custom name
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
          placeholder="Optional — auto-named if empty"
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
          options={LOADERS.map((item) => ({ value: item.id, label: `${item.label} — ${item.hint}` }))}
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
            {busy ? 'Creating…' : 'Create custom'}
          </button>
        </div>
      </div>
    </div>
  )
}
