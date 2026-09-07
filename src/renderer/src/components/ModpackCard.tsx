import { useState } from 'react'
import type { ModpackCard } from '../../../shared/types'

function formatDownloads(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${Math.round(n / 1_000)}k`
  return String(n)
}

export function ModpackCardView({
  pack,
  index,
  onOpen
}: {
  pack: ModpackCard
  index: number
  onOpen: () => void
}) {
  const [imgError, setImgError] = useState(false)

  return (
    <article
      className="animate-row group flex w-full cursor-pointer items-center gap-4 rounded-2xl border border-line/60 bg-panel/90 px-4 py-3 transition duration-200 hover:-translate-y-0.5 hover:border-tidal/40 hover:bg-raised"
      style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}
      onClick={onOpen}
    >
      <div className="h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-ink">
        {pack.iconUrl && !imgError ? (
          <img
            src={pack.iconUrl}
            alt=""
            loading="lazy"
            decoding="async"
            className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-2xl font-bold text-tidal">
            {pack.title?.charAt(0) || 'T'}
          </div>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="truncate text-base font-semibold">{pack.title}</h3>
          <span className="rounded-full bg-ink px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-mute">
            {pack.projectType}
          </span>
        </div>
        <p className="mt-1 line-clamp-1 text-sm text-mute">{pack.description}</p>
      </div>

      <p className="hidden shrink-0 text-xs text-mute sm:block">{formatDownloads(pack.downloads)}</p>
    </article>
  )
}
