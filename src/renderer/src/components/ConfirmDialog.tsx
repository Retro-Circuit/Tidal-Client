export function ConfirmDialog({
  title,
  body,
  confirmLabel = 'Confirm',
  danger,
  onCancel,
  onConfirm
}: {
  title: string
  body: string
  confirmLabel?: string
  danger?: boolean
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <div className="no-drag fixed inset-0 z-[90] flex items-center justify-center bg-black/60 p-6">
      <div className="w-full max-w-md rounded-2xl border border-line bg-ink p-5">
        <h3 className="text-lg font-semibold">{title}</h3>
        <p className="mt-2 text-sm leading-relaxed text-mute">{body}</p>
        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-xl border border-line px-4 py-2 text-sm text-mist hover:text-white"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className={`rounded-xl px-4 py-2 text-sm font-semibold text-white ${
              danger ? 'bg-red-600 hover:bg-red-500' : 'bg-tidal hover:brightness-110'
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
