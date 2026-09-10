import { Gift, Minus, Square, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import tidalLogo from '../assets/tidal.png'
import type { NavView, SessionState, WalletState } from '../../../shared/types'
import { PlayerHead3D } from './PlayerHead3D'

const NAV: { id: NavView; label: string }[] = [
  { id: 'home', label: 'Home' },
  { id: 'discover', label: 'Mods' },
  { id: 'instances', label: 'Instances' },
  { id: 'cosmetics', label: 'Cosmetics' },
  { id: 'settings', label: 'Settings' }
]

export function Chrome({
  view,
  onChange,
  session,
  loggingIn,
  loginError,
  onLogin,
  onLogout
}: {
  view: NavView
  onChange: (view: NavView) => void
  session: SessionState
  loggingIn: boolean
  loginError?: string | null
  onLogin: () => void
  onLogout: () => void
}) {
  const [wallet, setWallet] = useState<WalletState | null>(null)

  useEffect(() => {
    const load = (): void => {
      void window.tidal.getWallet().then(setWallet)
    }
    load()
    const id = window.setInterval(load, 4000)
    return () => window.clearInterval(id)
  }, [])

  return (
    <header className="relative flex h-12 shrink-0 items-center gap-3 border-b border-line/80 bg-ink/90 px-3">
      <div className="titlebar-drag absolute inset-y-0 left-0 right-40" />
      <div className="relative z-10 flex items-center gap-2 pl-1">
        <img src={tidalLogo} alt="" className="h-8 w-8 rounded-md object-cover" />
        <p className="text-[11px] uppercase tracking-[0.22em] text-mute">Tidal</p>
      </div>
      <nav className="relative z-10 flex flex-1 items-center justify-center gap-1">
        {NAV.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => onChange(item.id)}
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
              view === item.id ? 'bg-tidal text-white' : 'text-mute hover:bg-panel hover:text-white'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>
      <div className="relative z-10 flex items-center gap-2">
        {loginError ? <p className="max-w-40 truncate text-xs text-red-300">{loginError}</p> : null}
        <div className="flex items-center gap-1.5 rounded-full bg-panel px-2.5 py-1 text-xs text-mist">
          <Gift size={12} className="text-tidal" />
          {wallet?.points ?? 0}
        </div>
        {session.loggedIn && session.profile ? (
          <button
            type="button"
            onClick={onLogout}
            title="Sign out"
            className="flex items-center gap-2 rounded-full bg-panel py-0.5 pr-3 pl-0.5 hover:bg-raised"
          >
            <PlayerHead3D uuid={session.profile.id} size={28} />
            <span className="max-w-28 truncate text-sm text-mist">{session.profile.name}</span>
          </button>
        ) : (
          <button
            type="button"
            onClick={onLogin}
            disabled={loggingIn}
            className="rounded-full bg-tidal px-3 py-1.5 text-sm font-medium hover:brightness-110 disabled:opacity-60"
          >
            {loggingIn ? 'Signing in…' : 'Login with Microsoft'}
          </button>
        )}
        <button type="button" onClick={() => window.tidal.minimize()} className="p-2 text-mute hover:text-white">
          <Minus size={14} />
        </button>
        <button type="button" onClick={() => window.tidal.maximize()} className="p-2 text-mute hover:text-white">
          <Square size={12} />
        </button>
        <button type="button" onClick={() => window.tidal.close()} className="p-2 text-mute hover:text-red-400">
          <X size={14} />
        </button>
      </div>
    </header>
  )
}
