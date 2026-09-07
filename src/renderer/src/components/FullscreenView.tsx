import { useEffect, type ReactNode } from 'react'
import { ArrowLeft } from 'lucide-react'

export function FullscreenView({
  title,
  onClose,
  actions,
  children
}: {
  title: string
  onClose: () => void
  actions?: ReactNode
  children: ReactNode
}) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="animate-fade-in no-drag fixed inset-0 z-40 flex flex-col bg-ink">
      <header className="flex h-14 shrink-0 items-center gap-3 border-b border-line px-4">
        <button
          type="button"
          onClick={onClose}
          className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-mute transition hover:bg-panel hover:text-white"
        >
          <ArrowLeft size={18} />
          Back
        </button>
        <h2 className="min-w-0 flex-1 truncate text-sm font-semibold">{title}</h2>
        <div className="flex items-center gap-2">{actions}</div>
      </header>
      <div className="animate-rise min-h-0 flex-1 overflow-y-auto">{children}</div>
    </div>
  )
}
