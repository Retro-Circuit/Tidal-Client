import { useMemo, useState } from 'react'
import type { ForeignInstance } from '../../../shared/types'

export function ImportInstancesModal({
  found,
  onClose,
  onImported
}: {
  found: ForeignInstance[]
  onClose: () => void
  onImported: () => void
}) {
  const [picked, setPicked] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(found.map((item) => [item.id, true]))
  )
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const selected = useMemo(() => found.filter((item) => picked[item.id]), [found, picked])

  async function importSelected(): Promise<void> {
    if (!selected.length) {
      onClose()
      return
    }
    setBusy(true)
    setError(null)
    try {
      await window.tidal.importForeignInstances(selected)
      onImported()
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
      setBusy(false)
    }
  }

  return (
    <div className="no-drag fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-6">
      <div className="flex max-h-[80vh] w-full max-w-xl flex-col rounded-2xl border border-line bg-panel">
        <div className="border-b border-line px-5 py-4">
          <h2 className="text-lg font-semibold">Import existing instances?</h2>
          <p className="mt-1 text-sm text-mute">
            Tidal found installs from other launchers on this PC. Pick what to copy in. Worlds, mods, and configs are imported — not the whole launcher folder.
          </p>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-3">
          {found.map((item) => (
            <label key={item.id} className="mb-2 flex cursor-pointer items-start gap-3 rounded-xl bg-ink/60 px-3 py-3">
              <input
                type="checkbox"
                checked={Boolean(picked[item.id])}
                onChange={(event) => setPicked((current) => ({ ...current, [item.id]: event.target.checked }))}
                className="mt-1"
              />
              <span className="min-w-0">
                <span className="block truncate font-medium">{item.name}</span>
                <span className="block text-xs text-mute">
                  {item.launcher} · {item.minecraftVersion} · {item.loader}
                </span>
              </span>
            </label>
          ))}
        </div>
        {error ? <p className="px-5 text-sm text-red-300">{error}</p> : null}
        <div className="flex justify-end gap-2 border-t border-line px-5 py-4">
          <button
            type="button"
            disabled={busy}
            onClick={onClose}
            className="rounded-lg px-4 py-2 text-sm text-mute hover:text-white"
          >
            Not now
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={() => void importSelected()}
            className="rounded-lg bg-tidal px-4 py-2 text-sm font-semibold text-white hover:brightness-110 disabled:opacity-50"
          >
            {busy ? 'Importing…' : `Import ${selected.length}`}
          </button>
        </div>
      </div>
    </div>
  )
}
