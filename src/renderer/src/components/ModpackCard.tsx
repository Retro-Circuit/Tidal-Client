import { useState } from 'react'
import type { ModpackCard } from '../../../shared/types'

function formatDownloads(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`
  return String(n)
}

export function ModpackCardView({
  pack,
  installing,
  onOpen,
  onInstall
}: {
  pack: ModpackCard
  installing: boolean
  onOpen: () => void
  onInstall: () => void
}) {
  const [imgError, setImgError] = useState(false)

  return (
    <article
      className="group flex w-[400px] cursor-pointer flex-row overflow-hidden rounded-2xl border border-line bg-panel p-4 transition duration-300 hover:-translate-y-0.5 hover:border-tidal/50 hover:shadow-[0_12px_40px_rgba(3,73,252,0.12)]"
      onClick={onOpen}
    >
      <div
        className="relative shrink-0 overflow-hidden rounded-xl bg-raised flex items-center justify-center"
        style={{ width: 100, height: 100, minWidth: 100 }}
      >
        {pack.iconUrl && !imgError ? (
          <img
            src={pack.iconUrl}
            alt=""
            width={100}
            height={100}
            loading="lazy"
            decoding="async"
            className="h-full w-full object-cover object-center"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-3xl font-bold text-tidal bg-raised">
            {pack.title?.charAt(0) || "T"}
          </div>
        )}
        <span className="absolute top-2 left-2 rounded-full bg-black/60 px-2 py-0.5 text-[9px] uppercase tracking-wider text-mist backdrop-blur">
          {pack.projectType}
        </span>
      </div>

      <div className="flex flex-1 flex-col justify-between pl-4">
        <div className="flex flex-col gap-1">
          <div className="flex items-start justify-between gap-2">
            <h3 className="line-clamp-1 text-[15px] font-semibold">{pack.title}</h3>
            <span className="shrink-0 rounded-full bg-raised px-2 py-0.5 text-[9px] uppercase tracking-wider text-mute">
              {pack.source}
            </span>
          </div>
          <p className="line-clamp-2 text-xs leading-relaxed text-mute">{pack.description}</p>
        </div>

        <div className="flex items-center justify-between pt-2">
          <p className="text-[11px] text-mute">
            {formatDownloads(pack.downloads)} downloads · {pack.author}
          </p>
          <button
            onClick={(event) => {
              event.stopPropagation()
              onInstall()
            }}
            disabled={installing}
            className="rounded-lg bg-tidal px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110 disabled:opacity-50"
          >
            {installing ? 'Installing…' : 'Install'}
          </button>
        </div>
      </div>
    </article>
  )
}