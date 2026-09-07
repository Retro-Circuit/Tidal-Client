import { useEffect, useState } from 'react'
import { Compass, Play, Trash2 } from 'lucide-react'
import type { GameInstance, ProjectType } from '../../../shared/types'
import { InstanceDetailsModal } from '../components/InstanceDetailsModal'

export function InstancesView({
  onCreateInstance,
  onDiscover,
  refreshKey
}: {
  onCreateInstance: () => void
  onDiscover: (intent?: { projectType?: ProjectType | 'all'; gameVersion?: string; instanceId?: string }) => void
  refreshKey: number
}) {
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [selected, setSelected] = useState<GameInstance | null>(null)
  const [query, setQuery] = useState('')
  const [dropId, setDropId] = useState<string | null>(null)
  const [dropMessage, setDropMessage] = useState<string | null>(null)

  async function refresh(): Promise<void> {
    setInstances(await window.tidal.listInstances())
  }

  useEffect(() => {
    void refresh()
  }, [refreshKey])

  async function remove(instance: GameInstance): Promise<void> {
    const ok = window.confirm(`Delete “${instance.name}”? This cannot be undone.`)
    if (!ok) return
    await window.tidal.deleteInstance(instance.id)
    if (selected?.id === instance.id) setSelected(null)
    await refresh()
  }

  async function dropJars(instance: GameInstance, files: File[]): Promise<void> {
    const jars = files.filter((file) => file.name.toLowerCase().endsWith('.jar'))
    if (!jars.length) return
    const paths = window.tidal.pathsFromDrop(jars).filter(Boolean)
    if (!paths.length) return
    await window.tidal.importInstanceMods(instance.id, paths)
    setDropMessage(`Added ${paths.length} mod${paths.length === 1 ? '' : 's'} to ${instance.name}`)
    if (selected?.id === instance.id) setSelected({ ...instance })
    await refresh()
  }

  const visible = instances.filter((instance) =>
    instance.name.toLowerCase().includes(query.trim().toLowerCase())
  )

  return (
    <div className="animate-rise flex h-full min-h-0 flex-col gap-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h2 className="text-3xl font-semibold tracking-tight">My Instances</h2>
          <p className="mt-1 text-sm text-mute">Open an instance for mods, settings, and play. Drop .jar files onto a row to import.</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onDiscover({ projectType: 'mod' })}
            className="flex items-center gap-2 rounded-lg border border-line bg-panel px-4 py-2 text-sm font-semibold text-mist transition hover:border-tidal/50 hover:text-white"
          >
            <Compass size={14} />
            Discover
          </button>
          <button
            onClick={onCreateInstance}
            className="rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
          >
            Create instance
          </button>
        </div>
      </div>

      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search instances"
        autoComplete="off"
        spellCheck={false}
        className="relative z-10 w-full rounded-full border border-line bg-panel px-5 py-3 text-sm text-white outline-none transition placeholder:text-mute focus:border-tidal"
      />

      {dropMessage ? <p className="text-sm text-mist">{dropMessage}</p> : null}

      {visible.length === 0 ? (
        <div className="flex flex-1 items-center justify-center rounded-2xl border border-dashed border-line text-sm text-mute">
          Create an instance, or install a pack from Discover.
        </div>
      ) : (
        <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto pr-1 pb-8">
          {visible.map((instance, index) => (
            <article
              key={instance.id}
              onClick={() => setSelected(instance)}
              onDragOver={(event) => {
                event.preventDefault()
                setDropId(instance.id)
              }}
              onDragLeave={() => {
                if (dropId === instance.id) setDropId(null)
              }}
              onDrop={(event) => {
                event.preventDefault()
                event.stopPropagation()
                setDropId(null)
                void dropJars(instance, [...event.dataTransfer.files])
              }}
              className={`animate-row flex cursor-pointer items-center gap-4 rounded-2xl border bg-panel/90 px-4 py-3 transition duration-200 hover:-translate-y-0.5 hover:border-tidal/40 hover:bg-raised ${
                dropId === instance.id ? 'border-tidal bg-tidal/10' : 'border-line/60'
              }`}
              style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
            >
              <div className="h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-ink">
                {instance.iconUrl ? (
                  <img src={instance.iconUrl} alt="" className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full items-center justify-center text-sm font-bold text-tidal">MC</div>
                )}
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-base font-semibold">{instance.name}</h3>
                <p className="mt-1 text-sm text-mute">
                  {instance.minecraftVersion} · {instance.loader}
                  {instance.loaderVersion ? ` ${instance.loaderVersion}` : ''}
                </p>
              </div>
              <button
                type="button"
                title="Find mods"
                onClick={(event) => {
                  event.stopPropagation()
                  onDiscover({
                    projectType: 'mod',
                    gameVersion: instance.minecraftVersion,
                    instanceId: instance.id
                  })
                }}
                className="rounded-lg border border-line px-3 py-2 text-xs font-semibold text-mute transition hover:border-tidal/50 hover:text-white"
              >
                Discover
              </button>
              <button
                type="button"
                title="Play"
                onClick={(event) => {
                  event.stopPropagation()
                  void window.tidal.launchInstance(instance.id).then((result) => {
                    if (!result.ok) setSelected(instance)
                  })
                }}
                className="flex items-center gap-2 rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
              >
                <Play size={14} fill="currentColor" />
                Play
              </button>
              <button
                type="button"
                title="Delete instance"
                onClick={(event) => {
                  event.stopPropagation()
                  void remove(instance)
                }}
                className="rounded-lg p-2 text-mute transition hover:bg-ink hover:text-red-300"
              >
                <Trash2 size={18} strokeWidth={1.6} className="fill-none" />
              </button>
            </article>
          ))}
        </div>
      )}

      {selected ? (
        <InstanceDetailsModal
          instance={selected}
          onClose={() => setSelected(null)}
          onChanged={() => void refresh()}
          onDiscover={() =>
            onDiscover({
              projectType: 'mod',
              gameVersion: selected.minecraftVersion,
              instanceId: selected.id
            })
          }
        />
      ) : null}
    </div>
  )
}
