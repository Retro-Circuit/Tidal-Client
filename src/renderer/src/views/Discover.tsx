import { useEffect, useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import type { AppSettings, InstallProgress, ModpackCard } from '../../../shared/types'
import { ModpackCardView } from '../components/ModpackCard'
import { ProjectDetailsModal } from '../components/ProjectDetailsModal'
import { Toggle } from '../components/Toggle'

export function DiscoverView({
  settings,
  onSettings
}: {
  settings: AppSettings | null
  onSettings: (patch: Partial<AppSettings>) => Promise<void>
}) {
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  const [packs, setPacks] = useState<ModpackCard[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [installingId, setInstallingId] = useState<string | null>(null)
  const [progress, setProgress] = useState<InstallProgress | null>(null)
  const [selected, setSelected] = useState<ModpackCard | null>(null)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 350)
    return () => clearTimeout(t)
  }, [query])

  useEffect(() => {
    return window.tidal.onInstallProgress(setProgress)
  }, [])

  const sourcesKey = useMemo(
    () => `${settings?.modrinthEnabled}:${settings?.curseforgeEnabled}:${settings?.curseforgeApiKey}`,
    [settings]
  )

  useEffect(() => {
    if (!settings) return
    let cancelled = false
    setLoading(true)
    window.tidal
      .searchModpacks(debounced)
      .then((result) => {
        if (cancelled) return
        setPacks(result.hits)
        setError(result.error ?? null)
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [debounced, sourcesKey, settings])

  async function install(pack: ModpackCard): Promise<void> {
    if (pack.projectType !== 'modpack') {
      setSelected(pack)
      return
    }
    setInstallingId(pack.id)
    setProgress({ instanceId: pack.id, phase: 'start', message: `Installing ${pack.title}`, progress: 0, total: 0 })
    try {
      await window.tidal.installModpack(pack)
      setProgress({
        instanceId: pack.id,
        phase: 'done',
        message: `${pack.title} is ready in My Instances`,
        progress: 1,
        total: 1
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setInstallingId(null)
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col gap-5">
      <div>
        <p className="text-xs uppercase tracking-[0.24em] text-mute">Discover</p>
        <h2 className="mt-1 text-3xl font-semibold">Find your next world</h2>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <label className="relative min-w-72 flex-1">
          <Search size={16} className="absolute top-1/2 left-4 -translate-y-1/2 text-tidal" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search mods, modpacks, and resource packs"
            className="w-full rounded-2xl border-2 border-tidal bg-raised py-3 pr-4 pl-11 text-sm outline-none placeholder:text-mute focus:shadow-[0_0_0_4px_rgba(3,73,252,0.18)]"
          />
        </label>
        <Toggle
          label="Modrinth Support"
          checked={Boolean(settings?.modrinthEnabled)}
          onChange={(next) => onSettings({ modrinthEnabled: next })}
        />
        <Toggle
          label="CurseForge Support"
          checked={Boolean(settings?.curseforgeEnabled)}
          onChange={(next) => onSettings({ curseforgeEnabled: next })}
        />
      </div>

      {progress && installingId ? (
        <div className="rounded-xl border border-tidal/40 bg-tidal/10 px-4 py-3 text-sm text-mist">
          <span className="font-medium text-white">{progress.phase}</span>
          <span className="mx-2 text-mute">·</span>
          {progress.message}
        </div>
      ) : null}

      {error ? <p className="text-sm text-red-300">{error}</p> : null}

      {loading ? (
        <div className="grid min-h-0 flex-1 auto-rows-max grid-cols-[repeat(auto-fill,400px)] justify-start gap-4 overflow-y-auto pr-1 pb-8">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="h-[400px] w-[400px] animate-pulse rounded-md bg-raised" />
          ))}
        </div>
      ) : (
        <div className="grid min-h-0 flex-1 auto-rows-max grid-cols-[repeat(auto-fill,400px)] justify-start gap-4 overflow-y-auto pr-1 pb-8">
          {packs.map((pack) => (
            <ModpackCardView
              key={pack.id}
              pack={pack}
              installing={installingId === pack.id}
              onOpen={() => setSelected(pack)}
              onInstall={() => void install(pack)}
            />
          ))}
          {packs.length === 0 && !error ? (
            <p className="col-span-full text-sm text-mute">No projects match this search.</p>
          ) : null}
        </div>
      )}

      {selected ? (
        <ProjectDetailsModal
          card={selected}
          onClose={() => setSelected(null)}
          onInstalled={(message) =>
            setProgress({ instanceId: selected.id, phase: 'done', message, progress: 1, total: 1 })
          }
        />
      ) : null}
    </div>
  )
}
