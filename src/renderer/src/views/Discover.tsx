import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, Search } from 'lucide-react'
import type { AppSettings, DiscoverSearch, InstallProgress, ModpackCard, ProjectType } from '../../../shared/types'
import { ModpackCardView } from '../components/ModpackCard'
import { ProjectDetailsModal } from '../components/ProjectDetailsModal'
import { Toggle } from '../components/Toggle'

const PAGE_SIZE = 24

const TYPES: { id: ProjectType | 'all'; label: string }[] = [
  { id: 'all', label: 'All' },
  { id: 'mod', label: 'Mods' },
  { id: 'modpack', label: 'Modpacks' },
  { id: 'resourcepack', label: 'Resource packs' }
]

export function DiscoverView({
  settings,
  onSettings,
  intent
}: {
  settings: AppSettings | null
  onSettings: (patch: Partial<AppSettings>) => Promise<void>
  intent?: { projectType?: ProjectType | 'all'; gameVersion?: string; instanceId?: string }
}) {
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  const [offset, setOffset] = useState(0)
  const [projectType, setProjectType] = useState<ProjectType | 'all'>(intent?.projectType ?? 'all')
  const [packs, setPacks] = useState<ModpackCard[]>([])
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [progress, setProgress] = useState<InstallProgress | null>(null)
  const [selected, setSelected] = useState<ModpackCard | null>(null)

  useEffect(() => {
    if (intent?.projectType) setProjectType(intent.projectType)
    setOffset(0)
  }, [intent?.projectType, intent?.gameVersion, intent?.instanceId])

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 350)
    return () => clearTimeout(t)
  }, [query])

  useEffect(() => {
    setOffset(0)
  }, [debounced, projectType])

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
    const options: DiscoverSearch = {
      offset,
      projectType,
      gameVersion: intent?.gameVersion
    }
    window.tidal
      .searchModpacks(debounced, options)
      .then((result) => {
        if (cancelled) return
        setPacks(result.hits)
        setHasMore(result.hasMore)
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
  }, [debounced, sourcesKey, settings, offset, projectType, intent?.gameVersion])

  const page = Math.floor(offset / PAGE_SIZE) + 1

  return (
    <div className="animate-rise flex h-full min-h-0 flex-col gap-6">
      <div>
        <h2 className="text-3xl font-semibold tracking-tight">Discover</h2>
        <p className="mt-1 text-sm text-mute">
          {intent?.gameVersion
            ? `Showing ${intent.gameVersion} content. Click a project to install.`
            : 'Click a project to read more and download.'}
        </p>
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
        <div className="flex items-center gap-1">
          <button
            type="button"
            title="Previous page"
            disabled={offset <= 0 || loading}
            onClick={() => setOffset((value) => Math.max(0, value - PAGE_SIZE))}
            className="rounded-lg border border-line bg-panel p-2 text-mist transition hover:border-tidal/50 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronLeft size={16} />
          </button>
          <span className="min-w-10 text-center text-xs text-mute">{page}</span>
          <button
            type="button"
            title="Next page"
            disabled={!hasMore || loading}
            onClick={() => setOffset((value) => value + PAGE_SIZE)}
            className="rounded-lg border border-line bg-panel p-2 text-mist transition hover:border-tidal/50 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ChevronRight size={16} />
          </button>
        </div>
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

      <div className="flex flex-wrap gap-2">
        {TYPES.map((type) => (
          <button
            key={type.id}
            type="button"
            onClick={() => setProjectType(type.id)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${
              projectType === type.id ? 'bg-tidal text-white' : 'bg-panel text-mute hover:text-mist'
            }`}
          >
            {type.label}
          </button>
        ))}
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
