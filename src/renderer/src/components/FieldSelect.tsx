import { useEffect, useMemo, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'

export function FieldSelect({
  label,
  value,
  onChange,
  options,
  disabled,
  placeholder = 'Select'
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: { value: string; label: string }[]
  disabled?: boolean
  placeholder?: string
}) {
  const [open, setOpen] = useState(false)
  const [filter, setFilter] = useState('')
  const rootRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const current = options.find((option) => option.value === value)
  const searchable = options.length > 8
  const visible = useMemo(() => {
    const q = filter.trim().toLowerCase()
    if (!q) return options
    return options.filter((option) => option.label.toLowerCase().includes(q) || option.value.toLowerCase().includes(q))
  }, [filter, options])

  useEffect(() => {
    function close(event: MouseEvent): void {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  useEffect(() => {
    if (open && searchable) {
      setFilter('')
      window.setTimeout(() => searchRef.current?.focus(), 0)
    }
  }, [open, searchable])

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
        <span className="min-w-0 truncate">{current?.label || value || placeholder}</span>
        <ChevronDown size={14} className={`shrink-0 text-mute transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open ? (
        <div
          role="listbox"
          className="absolute z-30 mt-1 max-h-64 w-full overflow-hidden rounded-xl border border-line bg-panel shadow-[0_12px_40px_rgba(0,0,0,0.45)]"
        >
          {searchable ? (
            <input
              ref={searchRef}
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="Search…"
              className="w-full border-b border-line bg-raised px-3 py-2 text-sm text-white outline-none placeholder:text-mute"
            />
          ) : null}
          <ul className="max-h-52 overflow-y-auto py-1">
            {visible.length === 0 ? (
              <li className="px-3 py-2 text-sm text-mute">None found</li>
            ) : (
              visible.map((option) => (
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
        </div>
      ) : null}
    </div>
  )
}
