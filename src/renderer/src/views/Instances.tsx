import { useEffect, useState } from 'react'
import { Play } from 'lucide-react'
import type { GameInstance } from '../../../shared/types'

export function InstancesView({
  onCreateInstance,
  refreshKey
}: {
  onCreateInstance: () => void
  refreshKey: number
}) {
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [busyId, setBusyId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  async function refresh(): Promise<void> {
    setInstances(await window.tidal.listInstances())
  }

  useEffect(() => {
    void refresh()
  }, [refreshKey])

  async function launch(id: string): Promise<void> {
    setBusyId(id)
    setMessage(null)
    const result = await window.tidal.launchInstance(id)
    setBusyId(null)
    if (!result.ok) setMessage(result.error ?? 'Launch failed')
    else {
      setMessage('Minecraft is starting…')
      await refresh()
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col gap-5">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-mute">Library</p>
          <h2 className="mt-1 text-3xl font-semibold">My Instances</h2>
        </div>
        <button
          onClick={onCreateInstance}
          className="rounded-xl bg-tidal px-3 py-2 text-sm font-semibold shadow-[0_0_16px_rgba(3,73,252,0.35)] transition hover:brightness-110"
        >
          + Create instance
        </button>
      </div>

      {message ? <p className="text-sm text-mist">{message}</p> : null}

      {instances.length === 0 ? (
        <div className="flex flex-1 items-center justify-center rounded-3xl border border-dashed border-line text-sm text-mute">
          Use the + button to create an instance, or install a pack from Discover.
        </div>
      ) : (
        <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 overflow-y-auto pr-1 md:grid-cols-2 xl:grid-cols-3">
          {instances.map((instance) => (
            <article key={instance.id} className="rounded-2xl border border-line bg-panel p-4">
              <div className="flex gap-3">
                <div className="h-14 w-14 aspect-square overflow-hidden rounded-md bg-raised">
                  {instance.iconUrl ? (
                    <img src={instance.iconUrl} alt="" className="h-full w-full aspect-square object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center text-xs text-tidal">MC</div>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="truncate font-semibold">{instance.name}</h3>
                  <p className="text-xs text-mute">
                    {instance.minecraftVersion} · {instance.loader}
                    {instance.loaderVersion ? ` ${instance.loaderVersion}` : ''}
                  </p>
                </div>
              </div>
              <button
                onClick={() => launch(instance.id)}
                disabled={busyId === instance.id}
                className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-tidal py-2.5 text-sm font-semibold shadow-[0_0_18px_rgba(3,73,252,0.35)] transition hover:brightness-110 disabled:opacity-50"
              >
                <Play size={14} fill="currentColor" />
                {busyId === instance.id ? 'Launching…' : 'Play'}
              </button>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
