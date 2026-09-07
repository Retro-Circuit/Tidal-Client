import tidalLogo from '../assets/tidal.png'
import { Minus, Square, X } from 'lucide-react'
import type { SessionState } from '../../../shared/types'

type TopBarProps = {
  session: SessionState
  loggingIn: boolean
  loginError?: string | null
  onLogin: () => void
  onLogout: () => void
}

export function TopBar({ session, loggingIn, loginError, onLogin, onLogout }: TopBarProps) {
  return (
    <header className="relative flex h-12 shrink-0 items-center justify-between border-b border-line/80 bg-ink/90 px-3">
      <div className="titlebar-drag absolute inset-y-0 left-0 right-36" />
      <div className="relative z-10 flex items-center gap-3 pl-2">
        <img src={tidalLogo} alt="" className="h-8 w-8 rounded-md object-cover" />
        <p className="text-[11px] uppercase tracking-[0.22em] text-mute">Tidal</p>
      </div>
      <div className="relative z-10 flex items-center gap-2">
        {loginError ? <p className="max-w-xs truncate text-xs text-red-300">{loginError}</p> : null}
        {session.loggedIn && session.profile ? (
          <button
            type="button"
            onClick={onLogout}
            className="flex items-center gap-2 rounded-full bg-panel py-1 pr-3 pl-1 transition hover:bg-raised"
            title="Sign out"
          >
            <img
              src={session.profile.avatar}
              alt=""
              className="h-6 w-6 rounded-full object-cover"
            />
            <span className="text-sm text-mist">{session.profile.name}</span>
          </button>
        ) : (
          <button
            type="button"
            onClick={onLogin}
            disabled={loggingIn}
            className="rounded-full bg-tidal px-3 py-1.5 text-sm font-medium transition hover:brightness-110 disabled:opacity-60"
          >
            {loggingIn ? 'Signing in…' : 'Login with Microsoft'}
          </button>
        )}
        <button
          type="button"
          onClick={() => window.tidal.minimize()}
          className="p-2 text-mute transition hover:text-white"
        >
          <Minus size={14} />
        </button>
        <button
          type="button"
          onClick={() => window.tidal.maximize()}
          className="p-2 text-mute transition hover:text-white"
        >
          <Square size={12} />
        </button>
        <button
          type="button"
          onClick={() => window.tidal.close()}
          className="p-2 text-mute transition hover:text-red-400"
        >
          <X size={14} />
        </button>
      </div>
    </header>
  )
}
