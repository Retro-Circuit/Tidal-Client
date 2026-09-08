import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, Download } from 'lucide-react'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { GameInstance, ModpackCard, ProjectDetails } from '../../../shared/types'
import { FieldSelect } from './FieldSelect'
import { FullscreenView } from './FullscreenView'

function toSafeHtml(raw: string): string {
  const looksHtml = /<\/?[a-z][\s\S]*>/i.test(raw)
  const html = looksHtml ? raw : String(marked.parse(raw, { async: false }))
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}

export function ProjectDetailsModal({
  card,
  preferredInstanceId,
  onClose,
  onInstalled
}: {
  card: ModpackCard
  preferredInstanceId?: string
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
  const [progress, setProgress] = useState<string | null>(null)

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
      const sorted = [...list].sort((a, b) => (b.lastPlayed ?? b.createdAt) - (a.lastPlayed ?? a.createdAt))
      setInstances(sorted)
      setInstanceId(
        (preferredInstanceId && sorted.some((item) => item.id === preferredInstanceId)
          ? preferredInstanceId
          : sorted[0]?.id) ?? ''
      )
    })
    const stop = window.tidal.onInstallProgress((item) => setProgress(item.message))
    return () => {
      cancelled = true
      stop()
    }
  }, [card, preferredInstanceId])

  const html = useMemo(() => toSafeHtml(details?.bodyHtml ?? card.description), [details, card.description])
  const gallery = details?.gallery ?? []
  const current = gallery[slide]
  const isModpack = (details?.card.projectType ?? card.projectType) === 'modpack'
  const icon = details?.card.iconUrl || card.iconUrl
  const title = details?.card.title ?? card.title

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
      setError(
        (err instanceof Error ? err.message : String(err)).replace(/^Error invoking remote method '[^']+':\s*/, '')
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <FullscreenView
      title={title}
      onClose={onClose}
      actions={
        <button
          onClick={() => void install()}
          disabled={busy}
          className="flex items-center gap-2 rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110 disabled:opacity-50"
        >
          <Download size={15} />
          {busy ? 'Installing…' : isModpack ? 'Install modpack' : 'Add to instance'}
        </button>
      }
    >
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-8 py-8">
        <section className="flex flex-col gap-6 md:flex-row">
          <div className="h-40 w-40 shrink-0 overflow-hidden rounded-2xl bg-panel">
            {icon ? <img src={icon} alt="" className="h-full w-full object-cover" /> : null}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-xs font-medium uppercase tracking-[0.18em] text-mute">
              {details?.card.projectType ?? card.projectType} · {card.source}
            </p>
            <h1 className="mt-2 text-4xl font-bold tracking-tight">{title}</h1>
            <p className="mt-3 max-w-2xl text-base leading-relaxed text-mute">
              {details?.card.description ?? card.description}
            </p>
            {!isModpack ? (
              <div className="mt-5 max-w-md">
                <FieldSelect
                  label="Install into instance"
                  value={instanceId}
                  onChange={setInstanceId}
                  options={
                    instances.length === 0
                      ? [{ value: '', label: 'No instances yet' }]
                      : instances.map((instance) => ({
                          value: instance.id,
                          label: `${instance.name} (${instance.minecraftVersion})`
                        }))
                  }
                />
              </div>
            ) : (
              <p className="mt-5 text-sm text-mute">Download creates a new instance from this modpack.</p>
            )}
            {busy && progress ? <p className="mt-3 text-sm text-mist">{progress}</p> : null}
            {error ? <p className="mt-3 text-sm text-red-300">{error}</p> : null}
          </div>
        </section>

        {gallery.length > 0 ? (
          <section>
            <div className="relative overflow-hidden rounded-2xl bg-panel">
              <button type="button" onClick={() => current && setLightbox(current.url)} className="block w-full">
                <img src={current.url} alt={current.title ?? ''} className="max-h-[420px] w-full object-contain" />
              </button>
              {gallery.length > 1 ? (
                <>
                  <button
                    className="absolute top-1/2 left-3 -translate-y-1/2 rounded-full bg-black/55 p-2 text-white transition hover:bg-black/75"
                    onClick={() => setSlide((n) => (n === 0 ? gallery.length - 1 : n - 1))}
                  >
                    <ChevronLeft size={18} />
                  </button>
                  <button
                    className="absolute top-1/2 right-3 -translate-y-1/2 rounded-full bg-black/55 p-2 text-white transition hover:bg-black/75"
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
                    className={`h-16 w-16 shrink-0 overflow-hidden rounded-lg border transition ${
                      index === slide ? 'border-tidal' : 'border-transparent opacity-70 hover:opacity-100'
                    }`}
                  >
                    <img src={image.url} alt="" className="h-full w-full object-cover" />
                  </button>
                ))}
              </div>
            ) : null}
          </section>
        ) : null}

        <section>
          <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-mute">Description</h3>
          {!details && !error ? <p className="text-sm text-mute">Loading description…</p> : null}
          <div className="prose-tidal" dangerouslySetInnerHTML={{ __html: html }} />
        </section>
      </div>

      {lightbox ? (
        <button
          type="button"
          className="animate-fade-in fixed inset-0 z-50 flex items-center justify-center bg-black/85 p-8"
          onClick={() => setLightbox(null)}
        >
          <img src={lightbox} alt="" className="max-h-full max-w-full rounded-xl object-contain" />
        </button>
      ) : null}
    </FullscreenView>
  )
}
