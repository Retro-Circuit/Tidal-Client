import type { ReactNode } from 'react'
import tidalLogo from '../assets/tidal.png'
import { Compass, FolderKanban, Plus, Settings } from 'lucide-react'
import type { NavView } from '../../../shared/types'

export function Sidebar({
  view,
  onChange,
  onCreateInstance
}: {
  view: NavView
  onChange: (view: NavView) => void
  onCreateInstance: () => void
}) {
  return (
    <aside className="flex w-60 shrink-0 flex-col border-r border-line bg-panel/90 px-4 py-5">
      <div className="mb-8 flex items-center gap-3 px-2">
        <img
          src={tidalLogo}
          alt="Tidal Client"
          className="h-10 w-10 aspect-square rounded-xl object-cover shadow-[0_0_24px_rgba(3,73,252,0.45)]"
        />
        <div>
          <p className="text-[11px] uppercase tracking-[0.22em] text-mute">Launcher</p>
          <h1 className="text-lg font-semibold leading-none">Tidal Client</h1>
        </div>
      </div>

      <nav className="flex flex-col gap-1">
        <NavButton
          active={view === 'discover'}
          icon={<Compass size={18} />}
          label="Discover"
          onClick={() => onChange('discover')}
        />
        <div className="flex items-center gap-1">
          <NavButton
            className="flex-1"
            active={view === 'instances'}
            icon={<FolderKanban size={18} />}
            label="My Instances"
            onClick={() => onChange('instances')}
          />
          <button
            type="button"
            title="Create instance"
            onClick={onCreateInstance}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-tidal/40 bg-tidal text-lg font-semibold text-white shadow-[0_0_16px_rgba(3,73,252,0.45)] transition hover:brightness-110"
          >
            <Plus size={18} strokeWidth={2.5} />
          </button>
        </div>
        <NavButton
          active={view === 'settings'}
          icon={<Settings size={18} />}
          label="Settings"
          onClick={() => onChange('settings')}
        />
      </nav>

      <div className="mt-auto rounded-2xl border border-line bg-raised p-4">
        <p className="text-xs font-medium text-white">Play anywhere</p>
        <p className="mt-1 text-[11px] leading-relaxed text-mute">
          Discover, install, and launch Minecraft instances from one dark, quiet surface.
        </p>
      </div>
    </aside>
  )
}

function NavButton({
  active,
  icon,
  label,
  onClick,
  className = ''
}: {
  active: boolean
  icon: ReactNode
  label: string
  onClick: () => void
  className?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`${className} flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition ${
        active
          ? 'bg-tidal text-white shadow-[0_0_18px_rgba(3,73,252,0.45)]'
          : 'text-mist hover:bg-raised hover:text-white'
      }`}
    >
      {icon}
      {label}
    </button>
  )
}
