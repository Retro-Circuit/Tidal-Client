import { useEffect, useState } from 'react'
import { Compass, Play, Trash2 } from 'lucide-react'
import type { GameInstance, InstanceModFile, InstanceRunState } from '../../../shared/types'
import { FullscreenView } from './FullscreenView'

function formatBytes(size: number): string {
  if (size >= 1_048_576) return `${(size / 1_048_576).toFixed(1)} MB`
  if (size >= 1024) return `${(size / 1024).toFixed(0)} KB`
  return `${size} B`
}

function friendlyLaunchError(error: string): string {
  const critical = error.match(/Critical injection failure: ([^\[]+)/)
  if (critical?.[1]) return critical[1].trim()
  const mixin = error.match(/Mixin apply for mod [^\n]+ failed ([^\n]+)/)
  if (mixin?.[1]) return mixin[1].trim()
  return error
    .replace(/<\/?log4j:[^>]+>/g, '')
    .replace(/<!\[CDATA\[|\]\]>/g, '')
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('at ') && !line.startsWith('...'))
    .slice(0, 6)
    .join('\n')
}

export function InstanceDetailsModal({
  instance,
  onClose,
  onChanged,
  onDiscover
}: {
  instance: GameInstance
  onClose: () => void
  onChanged: () => void
  onDiscover: () => void
}) {
  const [mods, setMods] = useState<InstanceModFile[]>([])
  const [runState, setRunState] = useState<InstanceRunState>('idle')
  const [runInstanceId, setRunInstanceId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [dragging, setDragging] = useState(false)

  const thisRun = runInstanceId === instance.id ? runState : 'idle'

  async function refreshMods(): Promise<void> {
    setMods(await window.tidal.getInstanceMods(instance.id))
  }

  useEffect(() => {
    void refreshMods()
  }, [instance.id])

  useEffect(() => {
    void window.tidal.getRunState().then((status) => {
      setRunInstanceId(status.instanceId)
      setRunState(status.state)
      if (status.error) setError(status.error)
    })
    return window.tidal.onRunState((status) => {
      setRunInstanceId(status.instanceId)
      setRunState(status.state)
      if (status.error) setError(status.error)
    })
  }, [])

  async function importDropped(files: File[]): Promise<void> {
    const jars = files.filter((file) => file.name.toLowerCase().endsWith('.jar'))
    if (!jars.length) return
    const paths = window.tidal.pathsFromDrop(jars).filter(Boolean)
    if (!paths.length) return
    setMods(await window.tidal.importInstanceMods(instance.id, paths))
    setMessage(`Imported ${paths.length} mod${paths.length === 1 ? '' : 's'}`)
  }

  async function play(): Promise<void> {
    setError(null)
    setMessage(null)
    setRunState('starting')
    setRunInstanceId(instance.id)
    const result = await window.tidal.launchInstance(instance.id)
    if (!result.ok) {
      setRunState('idle')
      setRunInstanceId(null)
      setError(result.error ?? 'Launch failed')
    }
  }

  async function stop(): Promise<void> {
    await window.tidal.stopInstance()
  }

  async function removeMod(fileName: string): Promise<void> {
    await window.tidal.deleteInstanceMod(instance.id, fileName)
    await refreshMods()
  }

  async function removeInstance(): Promise<void> {
    await window.tidal.deleteInstance(instance.id)
    onChanged()
    onClose()
  }

  return (
    <FullscreenView
      title={instance.name}
      onClose={onClose}
      actions={
        thisRun === 'running' ? (
          <button
            type="button"
            onClick={() => void stop()}
            className="rounded-lg bg-red-600 px-5 py-2 text-sm font-semibold text-white transition hover:bg-red-500"
          >
            Stop
          </button>
        ) : (
          <button
            type="button"
            onClick={() => void play()}
            disabled={thisRun === 'starting' || (runState !== 'idle' && runInstanceId !== instance.id)}
            className="flex items-center gap-2 rounded-lg bg-tidal px-5 py-2 text-sm font-semibold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {thisRun === 'starting' ? (
              '...'
            ) : (
              <>
                <Play size={14} fill="currentColor" />
                Play
              </>
            )}
          </button>
        )
      }
    >
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-8 py-8">
        <section className="flex flex-col gap-6 md:flex-row md:items-end">
          <div className="h-36 w-36 shrink-0 overflow-hidden rounded-2xl bg-panel">
            {instance.iconUrl ? (
              <img src={instance.iconUrl} alt="" className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full items-center justify-center text-lg font-bold text-tidal">MC</div>
            )}
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap gap-2">
              <span className="rounded-full bg-panel px-3 py-1 text-xs font-medium uppercase tracking-wide text-mute">
                {instance.loader}
              </span>
              <span className="rounded-full bg-panel px-3 py-1 text-xs font-medium uppercase tracking-wide text-mute">
                {instance.minecraftVersion}
              </span>
              <span className="rounded-full bg-panel px-3 py-1 text-xs font-medium uppercase tracking-wide text-mute">
                {instance.source}
              </span>
            </div>
            <h1 className="mt-3 text-4xl font-bold tracking-tight">{instance.name}</h1>
            <div className="mt-4">
              {thisRun === 'running' ? (
                <button
                  type="button"
                  onClick={() => void stop()}
                  className="rounded-lg bg-red-600 px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-red-500"
                >
                  Stop
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => void play()}
                  disabled={thisRun === 'starting' || (runState !== 'idle' && runInstanceId !== instance.id)}
                  className="flex items-center gap-2 rounded-lg bg-tidal px-6 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {thisRun === 'starting' ? (
                    'Starting…'
                  ) : (
                    <>
                      <Play size={14} fill="currentColor" />
                      Play
                    </>
                  )}
                </button>
              )}
            </div>
            <p className="mt-2 text-sm text-mute">
              {mods.length} installed mod{mods.length === 1 ? '' : 's'}
              {instance.loaderVersion ? ` · ${instance.loader} ${instance.loaderVersion}` : ''}
            </p>
            {message ? <p className="mt-3 text-sm text-mist">{message}</p> : null}
            {error ? (
              <div className="mt-4 max-h-40 overflow-auto rounded-xl border border-red-500/30 bg-red-950/40 px-4 py-3">
                <p className="text-xs font-semibold tracking-wide text-red-200 uppercase">Launch failed</p>
                <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-red-100/90">
                  {friendlyLaunchError(error)}
                </p>
              </div>
            ) : null}
          </div>
        </section>

        <section
          onDragOver={(event) => {
            event.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => {
            event.preventDefault()
            setDragging(false)
            void importDropped([...event.dataTransfer.files])
          }}
        >
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-mute">Installed mods</h3>
            <button
              type="button"
              onClick={onDiscover}
              className="flex items-center gap-1.5 text-xs font-semibold text-tidal transition hover:brightness-110"
            >
              <Compass size={12} />
              Discover
            </button>
          </div>
          {mods.length === 0 ? (
            <div
              className={`rounded-2xl border border-dashed px-4 py-16 text-center text-sm ${
                dragging ? 'border-tidal bg-tidal/10 text-mist' : 'border-line text-mute'
              }`}
            >
              No mods in this instance yet. Drop .jar files here or use Discover.
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {mods.map((mod, index) => (
                <div
                  key={mod.fileName}
                  className="animate-row flex items-center gap-3 rounded-2xl bg-panel px-4 py-3"
                  style={{ animationDelay: `${Math.min(index, 12) * 35}ms` }}
                >
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-ink text-xs font-bold text-tidal">
                    JAR
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{mod.fileName.replace(/\.jar(\.disabled)?$/, '')}</p>
                    <p className="text-xs text-mute">{formatBytes(mod.size)}</p>
                  </div>
                  <button
                    onClick={() => void removeMod(mod.fileName)}
                    className="rounded-lg p-2 text-mute transition hover:bg-raised hover:text-red-300"
                    title="Remove mod"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        <button
          onClick={() => void removeInstance()}
          className="self-start text-sm text-mute transition hover:text-red-300"
        >
          Delete this instance
        </button>
      </div>
    </FullscreenView>
  )
}
