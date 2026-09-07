import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import tidalLogo from '../assets/tidal.png'
import { Compass, FolderKanban, Gift, Plus, Settings } from 'lucide-react'
import type { NavView, WalletState } from '../../../shared/types'

function remainingLabel(nextDailyAt: number): string {
  const ms = Math.max(0, nextDailyAt - Date.now())
  const totalSeconds = Math.ceil(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function Sidebar({
  view,
  onChange,
  onCreateInstance
}: {
  view: NavView
  onChange: (view: NavView) => void
  onCreateInstance: () => void
}) {
  const [wallet, setWallet] = useState<WalletState | null>(null)
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const load = (): void => {
      void window.tidal.getWallet().then(setWallet)
    }
    load()
    const id = window.setInterval(load, 1500)
    return () => window.clearInterval(id)
  }, [])

  useEffect(() => {
    if (!wallet || wallet.canClaim) return
    const id = window.setInterval(() => {
      const t = Date.now()
      setNow(t)
      if (t >= wallet.nextDailyAt) {
        void window.tidal.getWallet().then(setWallet)
      }
    }, 1000)
    return () => window.clearInterval(id)
  }, [wallet])

  async function claimDaily(): Promise<void> {
    setWallet(await window.tidal.claimDaily())
  }

  const canClaim = Boolean(wallet?.canClaim)
  const points = wallet?.points ?? 0

  return (
    <aside className="no-drag flex w-[232px] shrink-0 flex-col border-r border-line/80 bg-ink px-3 py-5">
      <div className="mb-8 flex items-center gap-3 px-2">
        <img src={tidalLogo} alt="Tidal Client" className="h-9 w-9 rounded-lg object-cover" />
        <div>
          <p className="text-[10px] uppercase tracking-[0.2em] text-mute">Launcher</p>
          <h1 className="text-sm font-semibold">Tidal Client</h1>
        </div>
      </div>

      <nav className="flex flex-col gap-1">
        <NavButton
          active={view === 'discover'}
          icon={<Compass size={17} />}
          label="Discover"
          onClick={() => onChange('discover')}
        />
        <div className="flex items-center gap-1">
          <NavButton
            className="flex-1"
            active={view === 'instances'}
            icon={<FolderKanban size={17} />}
            label="Instances"
            onClick={() => onChange('instances')}
          />
          <button
            type="button"
            title="Create instance"
            onClick={onCreateInstance}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-tidal text-white transition hover:brightness-110"
          >
            <Plus size={16} strokeWidth={2.5} />
          </button>
        </div>
        <NavButton
          active={view === 'settings'}
          icon={<Settings size={17} />}
          label="Settings"
          onClick={() => onChange('settings')}
        />
      </nav>

      <div className="mt-auto px-1 pt-4">
        <p className="mb-2 px-2 text-xs text-mute">
          <span className="font-semibold text-mist">{points}</span> Tidal points
        </p>
        <button
          type="button"
          disabled={!canClaim}
          onClick={() => void claimDaily()}
          className={`flex w-full items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
            canClaim
              ? 'bg-tidal text-white hover:brightness-110'
              : 'cursor-not-allowed bg-panel text-mute'
          }`}
        >
          <Gift size={16} />
          {canClaim ? 'Daily +50' : remainingLabel(wallet?.nextDailyAt ?? now)}
        </button>
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
      className={`${className} flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition duration-200 ${
        active ? 'bg-tidal text-white shadow-[0_0_0_1px_rgba(61,111,255,0.35)]' : 'text-mute hover:bg-panel hover:text-white'
      }`}
    >
      {icon}
      {label}
    </button>
  )
}
