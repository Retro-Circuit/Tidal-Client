import { useEffect, useState } from 'react'
import { Compass, FolderPlus, Play, Plus, Trash2 } from 'lucide-react'
import type { GameInstance, InstanceRunState, ProjectType } from '../../../shared/types'
import { ConfirmDialog } from '../components/ConfirmDialog'
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
  const [pendingDelete, setPendingDelete] = useState<GameInstance | null>(null)
  const [runId, setRunId] = useState<string | null>(null)
  const [runState, setRunState] = useState<InstanceRunState>('idle')
  const [launchError, setLaunchError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  async function refresh(): Promise<void> {
    try {
      const list = await window.tidal.listInstances()
      setInstances([...list].sort((a, b) => (b.lastPlayed ?? b.createdAt) - (a.lastPlayed ?? a.createdAt)))
      setListError(null)
    } catch (error) {
      const raw = error instanceof Error ? error.message : String(error)
      setListError(raw.replace(/^Error invoking remote method '[^']+':\s*/, '') || 'Could not load instances.')
    }
  }

  useEffect(() => {
    void refresh()
  }, [refreshKey])

  useEffect(() => {
    void window.tidal.getRunState().then((status) => {
      setRunId(status.instanceId)
      setRunState(status.state)
    })
    return window.tidal.onRunState((status) => {
      setRunId(status.instanceId)
      setRunState(status.state)
      if (status.error) setLaunchError(status.error)
    })
  }, [])

  async function remove(instance: GameInstance): Promise<void> {
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

  async function pickJars(instance: GameInstance): Promise<void> {
    const paths = await window.tidal.pickInstanceJars()
    if (!paths.length) return
    await window.tidal.importInstanceMods(instance.id, paths)
    setDropMessage(`Added ${paths.length} mod${paths.length === 1 ? '' : 's'} to ${instance.name}`)
    await refresh()
  }

  async function play(instance: GameInstance): Promise<void> {
    setLaunchError(null)
    const result = await window.tidal.launchInstance(instance.id)
    if (!result.ok) {
      setLaunchError(result.error ?? 'Launch failed')
      setSelected(instance)
    }
  }

  const visible = instances.filter((instance) =>
    instance.name.toLowerCase().includes(query.trim().toLowerCase())
  )

  return (
    <div className="animate-rise flex h-full min-h-0 flex-col gap-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h2 className="text-3xl font-semibold tracking-tight">My Instances</h2>
          <p className="mt-1 text-sm text-mute">
            Play, add mods from Discover, or drop .jar files onto a row.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onDiscover({ projectType: 'mod' })}
            className="flex items-center gap-2 rounded-lg border border-line bg-panel px-4 py-2 text-sm font-semibold text-mist transition hover:border-tidal/50 hover:text-white"
          >
            <Compass size={14} />
            Browse mods
          </button>
          <button
            onClick={onCreateInstance}
            className="flex items-center gap-2 rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
          >
            <Plus size={14} />
            Create instance
          </button>
        </div>
      </div>

      {instances.length > 0 ? (
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search instances"
          autoComplete="off"
          spellCheck={false}
          className="relative z-10 w-full rounded-full border border-line bg-panel px-5 py-3 text-sm text-white outline-none transition placeholder:text-mute focus:border-tidal"
        />
      ) : null}

      {listError ? <p className="text-sm text-red-300">{listError}</p> : null}
      {launchError && !selected ? <p className="text-sm text-red-300">{launchError}</p> : null}

      {visible.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-4 rounded-2xl border border-dashed border-line text-sm text-mute">
          <p>{instances.length === 0 ? 'No instances yet.' : 'No instances match that search.'}</p>
          {instances.length === 0 ? (
            <div className="flex gap-2">
              <button
                type="button"
                onClick={onCreateInstance}
                className="rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white hover:brightness-110"
              >
                Create instance
              </button>
              <button
                type="button"
                onClick={() => onDiscover({ projectType: 'modpack' })}
                className="rounded-lg border border-line px-4 py-2 text-sm font-semibold text-mist hover:text-white"
              >
                Install a modpack
              </button>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto pr-1 pb-8">
          {visible.map((instance, index) => {
            const running = runId === instance.id && runState !== 'idle'
            return (
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
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-base font-semibold">{instance.name}</h3>
                    {running ? (
                      <span className="rounded-full bg-tidal/20 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-tidal">
                        {runState === 'starting' ? 'Starting' : 'Playing'}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-1 text-sm text-mute">
                    {instance.minecraftVersion} · {instance.loader}
                    {instance.loaderVersion ? ` ${instance.loaderVersion}` : ''}
                  </p>
                </div>
                <button
                  type="button"
                  title="Add mods from files"
                  onClick={(event) => {
                    event.stopPropagation()
                    void pickJars(instance)
                  }}
                  className="rounded-lg border border-line px-3 py-2 text-xs font-semibold text-mute transition hover:border-tidal/50 hover:text-white"
                >
                  <FolderPlus size={14} />
                </button>
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
                  Mods
                </button>
                <button
                  type="button"
                  title="Play"
                  onClick={(event) => {
                    event.stopPropagation()
                    void play(instance)
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
                    setPendingDelete(instance)
                  }}
                  className="rounded-lg p-2 text-mute transition hover:bg-ink hover:text-red-300"
                >
                  <Trash2 size={18} strokeWidth={1.6} className="fill-none" />
                </button>
              </article>
            )
          })}
        </div>
      )}

      {pendingDelete ? (
        <ConfirmDialog
          title={`Delete ${pendingDelete.name}?`}
          body="This removes the instance and its mods, worlds, and configs from Tidal. It cannot be undone."
          confirmLabel="Delete"
          danger
          onCancel={() => setPendingDelete(null)}
          onConfirm={() => {
            const target = pendingDelete
            setPendingDelete(null)
            void remove(target)
          }}
        />
      ) : null}

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
