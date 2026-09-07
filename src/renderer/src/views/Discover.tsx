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

  return (
    <div className="animate-rise flex h-full min-h-0 flex-col gap-6">
      <div>
        <h2 className="text-3xl font-semibold tracking-tight">Discover</h2>
        <p className="mt-1 text-sm text-mute">Click a project to read more and download.</p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <label className="relative min-w-72 flex-1">
          <Search size={16} className="absolute top-1/2 left-4 -translate-y-1/2 text-mute" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search mods, modpacks, and resource packs"
            className="w-full rounded-full border border-line bg-panel py-3 pr-4 pl-11 text-sm outline-none transition placeholder:text-mute focus:border-tidal"
          />
        </label>
        <Toggle
          label="Modrinth"
          checked={Boolean(settings?.modrinthEnabled)}
          onChange={(next) => onSettings({ modrinthEnabled: next })}
        />
        <Toggle
          label="CurseForge"
          checked={Boolean(settings?.curseforgeEnabled)}
          onChange={(next) => onSettings({ curseforgeEnabled: next })}
        />
      </div>

      {progress?.phase === 'done' ? (
        <div className="animate-fade-in rounded-xl bg-tidal/10 px-4 py-3 text-sm text-mist">{progress.message}</div>
      ) : null}

      {error ? <p className="text-sm text-red-300">{error}</p> : null}

      {loading ? (
        <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-hidden">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-[92px] animate-pulse rounded-2xl bg-panel" />
          ))}
        </div>
      ) : (
        <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto pr-1 pb-8">
          {packs.map((pack, index) => (
            <ModpackCardView key={pack.id} pack={pack} index={index} onOpen={() => setSelected(pack)} />
          ))}
          {packs.length === 0 && !error ? <p className="text-sm text-mute">No projects match this search.</p> : null}
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
