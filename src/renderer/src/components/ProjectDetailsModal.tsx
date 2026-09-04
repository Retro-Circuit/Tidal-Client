import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { GameInstance, ModpackCard, ProjectDetails } from '../../../shared/types'

function toSafeHtml(raw: string): string {
  const looksHtml = /<\/?[a-z][\s\S]*>/i.test(raw)
  const html = looksHtml ? raw : String(marked.parse(raw, { async: false }))
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}

export function ProjectDetailsModal({
  card,
  onClose,
  onInstalled
}: {
  card: ModpackCard
  onClose: () => void
  onInstalled: (message: string) => void
}) {
  const [details, setDetails] = useState<ProjectDetails | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [instanceId, setInstanceId] = useState('')
  const [busy, setBusy] = useState(false)
  const [slide, setSlide] = useState(0)
  const [lightbox, setLightbox] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    window.tidal
      .projectDetails(card)
      .then((result) => {
        if (!cancelled) setDetails(result)
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
    void window.tidal.listInstances().then((list) => {
      if (cancelled) return
      setInstances(list)
      setInstanceId(list[0]?.id ?? '')
    })
    return () => {
      cancelled = true
    }
  }, [card])

  const html = useMemo(() => toSafeHtml(details?.bodyHtml ?? card.description), [details, card.description])
  const gallery = details?.gallery ?? []
  const current = gallery[slide]
  const isModpack = (details?.card.projectType ?? card.projectType) === 'modpack'

  async function install(): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      if (isModpack) {
        await window.tidal.installModpack(details?.card ?? card)
        onInstalled(`${card.title} is ready in My Instances`)
        onClose()
        return
      }
      if (!instanceId) {
        setError('Create or select an instance first.')
        return
      }
      await window.tidal.installContent(details?.card ?? card, instanceId)
      const name = instances.find((item) => item.id === instanceId)?.name ?? 'instance'
      onInstalled(`${card.title} installed into ${name}`)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70 p-6">
      <div className="flex max-h-[88vh] w-full max-w-5xl flex-col overflow-hidden rounded-3xl border border-line bg-ink shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
        <header className="flex items-start gap-4 border-b border-line px-6 py-5">
          <img
            src={details?.card.iconUrl || card.iconUrl}
            alt=""
            className="h-16 w-16 aspect-square rounded-md object-cover bg-raised"
          />
          <div className="min-w-0 flex-1">
            <p className="text-[11px] uppercase tracking-[0.2em] text-mute">
              {details?.card.projectType ?? card.projectType} · {card.source}
            </p>
            <h2 className="mt-1 text-2xl font-semibold">{details?.card.title ?? card.title}</h2>
            <p className="mt-1 line-clamp-2 text-sm text-mute">{details?.card.description ?? card.description}</p>
          </div>
          <button onClick={onClose} className="rounded-full p-2 text-mute hover:bg-raised hover:text-white">
            <X size={18} />
          </button>
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          {gallery.length > 0 ? (
            <div className="mb-6">
              <div className="relative overflow-hidden rounded-2xl bg-raised">
                <button
                  type="button"
                  onClick={() => setLightbox(current.url)}
                  className="block w-full"
                >
                  <img src={current.url} alt={current.title ?? ''} className="max-h-80 w-full object-contain" />
                </button>
                {gallery.length > 1 ? (
                  <>
                    <button
                      className="absolute top-1/2 left-3 -translate-y-1/2 rounded-full bg-black/55 p-2 text-white"
                      onClick={() => setSlide((n) => (n === 0 ? gallery.length - 1 : n - 1))}
                    >
                      <ChevronLeft size={18} />
                    </button>
                    <button
                      className="absolute top-1/2 right-3 -translate-y-1/2 rounded-full bg-black/55 p-2 text-white"
                      onClick={() => setSlide((n) => (n + 1) % gallery.length)}
                    >
                      <ChevronRight size={18} />
                    </button>
                  </>
                ) : null}
              </div>
              {gallery.length > 1 ? (
                <div className="mt-3 flex gap-2 overflow-x-auto">
                  {gallery.map((image, index) => (
                    <button
                      key={image.url}
                      onClick={() => setSlide(index)}
                      className={`h-14 w-14 shrink-0 overflow-hidden rounded-md border ${
                        index === slide ? 'border-tidal' : 'border-line'
                      }`}
                    >
                      <img src={image.url} alt="" className="h-full w-full aspect-square object-cover" />
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          ) : (
            <p className="mb-4 text-sm text-mute">No gallery images for this project.</p>
          )}

          {!details && !error ? <p className="text-sm text-mute">Loading description…</p> : null}
          <div className="prose-tidal" dangerouslySetInnerHTML={{ __html: html }} />
        </div>

        <footer className="flex flex-wrap items-center gap-3 border-t border-line px-6 py-4">
          {!isModpack ? (
            <select
              value={instanceId}
              onChange={(e) => setInstanceId(e.target.value)}
              className="min-w-48 flex-1 rounded-xl border border-line bg-raised px-3 py-2.5 text-sm outline-none focus:border-tidal"
            >
              {instances.length === 0 ? <option value="">No instances yet</option> : null}
              {instances.map((instance) => (
                <option key={instance.id} value={instance.id}>
                  {instance.name} ({instance.minecraftVersion})
                </option>
              ))}
            </select>
          ) : (
            <p className="flex-1 text-sm text-mute">This will create a new instance from the modpack.</p>
          )}
          {error ? <p className="text-sm text-red-300">{error}</p> : null}
          <button
            onClick={() => void install()}
            disabled={busy}
            className="rounded-xl bg-tidal px-5 py-2.5 text-sm font-semibold shadow-[0_0_18px_rgba(3,73,252,0.45)] transition hover:brightness-110 disabled:opacity-50"
          >
            {busy ? 'Installing…' : isModpack ? 'Install instance' : 'Download / Install'}
          </button>
        </footer>
      </div>

      {lightbox ? (
        <button
          type="button"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 p-8"
          onClick={() => setLightbox(null)}
        >
          <img src={lightbox} alt="" className="max-h-full max-w-full rounded-xl object-contain" />
        </button>
      ) : null}
    </div>
  )
}
