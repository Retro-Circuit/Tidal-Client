import { useEffect, useState } from 'react'
import { Play, Trash2 } from 'lucide-react'
import type { GameInstance } from '../../../shared/types'
import { InstanceDetailsModal } from '../components/InstanceDetailsModal'

export function InstancesView({
  onCreateInstance,
  refreshKey
}: {
  onCreateInstance: () => void
  refreshKey: number
}) {
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [selected, setSelected] = useState<GameInstance | null>(null)
  const [query, setQuery] = useState('')

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

  const visible = instances.filter((instance) =>
    instance.name.toLowerCase().includes(query.trim().toLowerCase())
  )

  return (
    <div className="animate-rise flex h-full min-h-0 flex-col gap-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h2 className="text-3xl font-semibold tracking-tight">My Instances</h2>
          <p className="mt-1 text-sm text-mute">Open an instance for mods, settings, and play.</p>
        </div>
        <button
          onClick={onCreateInstance}
          className="rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
        >
          Create instance
        </button>
      </div>

      <input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search instances"
        className="w-full rounded-full border border-line bg-panel px-5 py-3 text-sm outline-none transition placeholder:text-mute focus:border-tidal"
      />

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
              className="animate-row flex cursor-pointer items-center gap-4 rounded-2xl border border-line/60 bg-panel/90 px-4 py-3 transition duration-200 hover:-translate-y-0.5 hover:border-tidal/40 hover:bg-raised"
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
        />
      ) : null}
    </div>
  )
}
