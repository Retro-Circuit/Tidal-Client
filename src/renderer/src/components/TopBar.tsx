import tidalLogo from '../assets/tidal.png'
import { Minus, Square, X } from 'lucide-react'
import type { SessionState } from '../../../shared/types'

type TopBarProps = {
  session: SessionState
  loggingIn: boolean
  onLogin: () => void
  onLogout: () => void
}

export function TopBar({ session, loggingIn, onLogin, onLogout }: TopBarProps) {
  return (
    <header className="drag-region flex h-14 items-center justify-between border-b border-line px-5">
      <div className="flex items-center gap-2">
        <img src={tidalLogo} alt="" className="h-6 w-6 aspect-square rounded-md object-cover" />
        <p className="text-xs uppercase tracking-[0.28em] text-mute">Tidal</p>
      </div>
      <div className="no-drag flex items-center gap-3">
        {session.loggedIn && session.profile ? (
          <button
            onClick={onLogout}
            className="flex items-center gap-2 rounded-full border border-line bg-raised py-1 pr-3 pl-1 transition hover:border-tidal/60"
            title="Sign out"
          >
            <img
              src={session.profile.avatar}
              alt=""
              className="h-7 w-7 aspect-square rounded-full object-cover ring-2 ring-tidal"
            />
            <span className="text-sm text-mist">{session.profile.name}</span>
          </button>
        ) : (
          <button
            onClick={onLogin}
            disabled={loggingIn}
            className="rounded-full bg-tidal px-4 py-1.5 text-sm font-medium shadow-[0_0_18px_rgba(3,73,252,0.5)] transition hover:brightness-110 disabled:opacity-60"
          >
            {loggingIn ? 'Signing in…' : 'Login with Microsoft'}
          </button>
        )}
        <div className="ml-2 flex items-center">
          <button onClick={() => window.tidal.minimize()} className="p-2 text-mute hover:text-white">
            <Minus size={14} />
          </button>
          <button onClick={() => window.tidal.maximize()} className="p-2 text-mute hover:text-white">
            <Square size={12} />
          </button>
          <button onClick={() => window.tidal.close()} className="p-2 text-mute hover:text-red-400">
            <X size={14} />
          </button>
        </div>
      </div>
    </header>
  )
}
