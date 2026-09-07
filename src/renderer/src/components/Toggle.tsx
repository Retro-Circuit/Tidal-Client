type ToggleProps = {
  label: string
  checked: boolean
  onChange: (next: boolean) => void
}

export function Toggle({ label, checked, onChange }: ToggleProps) {
  return (
    <button
      type="button"
      onClick={() => onChange(!checked)}
      className="group flex items-center gap-3 rounded-full border border-line bg-raised/80 px-3 py-1.5 transition hover:border-tidal/50"
    >
      <span className="text-xs font-medium tracking-wide text-mist">{label}</span>
      <span
        className={`relative h-5 w-9 rounded-full transition duration-200 ${
          checked ? 'bg-tidal' : 'bg-[#2c2c2c]'
        }`}
      >
        <span
          className="absolute top-0.5 h-4 w-4 rounded-full bg-white transition-all duration-200"
          style={{ left: checked ? 18 : 2 }}
        />
      </span>
    </button>
  )
}
