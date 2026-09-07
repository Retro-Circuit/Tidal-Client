import { useEffect, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'

export function FieldSelect({
  label,
  value,
  onChange,
  options,
  disabled
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: { value: string; label: string }[]
  disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const current = options.find((option) => option.value === value)

  useEffect(() => {
    function close(event: MouseEvent): void {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  return (
    <div ref={rootRef} className="relative mt-3">
      <p className="text-xs text-mute">{label}</p>
      <button
        type="button"
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((next) => !next)}
        className="mt-1 flex w-full items-center justify-between gap-2 rounded-xl border border-line bg-raised px-3 py-2.5 text-left text-sm text-white outline-none transition hover:border-tidal/50 focus:border-tidal disabled:opacity-50"
      >
        <span className="min-w-0 truncate">{current?.label || value || 'Select'}</span>
        <ChevronDown size={14} className={`shrink-0 text-mute transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open ? (
        <ul
          role="listbox"
          className="absolute z-30 mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-line bg-panel py-1 shadow-[0_12px_40px_rgba(0,0,0,0.45)]"
        >
          {options.length === 0 ? (
            <li className="px-3 py-2 text-sm text-mute">None found</li>
          ) : (
            options.map((option) => (
              <li key={option.value} role="option" aria-selected={option.value === value}>
                <button
                  type="button"
                  onClick={() => {
                    onChange(option.value)
                    setOpen(false)
                  }}
                  className={`w-full px-3 py-2 text-left text-sm transition hover:bg-raised ${
                    option.value === value ? 'text-white' : 'text-mist'
                  }`}
                >
                  {option.label}
                </button>
              </li>
            ))
          )}
        </ul>
      ) : null}
    </div>
  )
}
