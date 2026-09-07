import { useEffect, useState } from 'react'
import type { AppSettings, NavView, SessionState } from '../../shared/types'
import { CreateInstanceModal } from './components/CreateInstanceModal'
import { Sidebar } from './components/Sidebar'
import { TopBar } from './components/TopBar'
import { DiscoverView } from './views/Discover'
import { InstancesView } from './views/Instances'
import { SettingsView } from './views/Settings'

export default function App() {
  const [view, setView] = useState<NavView>('discover')
  const [session, setSession] = useState<SessionState>({ loggedIn: false, profile: null })
  const [loggingIn, setLoggingIn] = useState(false)
  const [loginError, setLoginError] = useState<string | null>(null)
  const [settings, setSettings] = useState<AppSettings | null>(null)
  const [creating, setCreating] = useState(false)
  const [instanceTick, setInstanceTick] = useState(0)

  useEffect(() => {
    void window.tidal.session().then(setSession)
    void window.tidal.getSettings().then(setSettings)
  }, [])

  async function patchSettings(patch: Partial<AppSettings>): Promise<void> {
    const next = await window.tidal.setSettings(patch)
    setSettings(next)
  }

  async function login(): Promise<void> {
    setLoggingIn(true)
    setLoginError(null)
    try {
      setSession(await window.tidal.login())
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : String(error))
    } finally {
      setLoggingIn(false)
    }
  }

  return (
    <div className="flex h-full bg-ink">
      <Sidebar
        view={view}
        onChange={setView}
        onCreateInstance={() => {
          setView('instances')
          setCreating(true)
        }}
      />
      <main className="relative flex min-w-0 flex-1 flex-col">
        <TopBar
          session={session}
          loggingIn={loggingIn}
          loginError={loginError}
          onLogin={() => void login()}
          onLogout={() => void window.tidal.logout().then(setSession)}
        />
        <section className="min-h-0 flex-1 overflow-hidden px-7 py-6">
          {view === 'discover' ? <DiscoverView settings={settings} onSettings={patchSettings} /> : null}
          {view === 'instances' ? (
            <InstancesView onCreateInstance={() => setCreating(true)} refreshKey={instanceTick} />
          ) : null}
          {view === 'settings' ? <SettingsView settings={settings} onSettings={patchSettings} /> : null}
        </section>
      </main>
      {creating ? (
        <CreateInstanceModal
          onClose={() => setCreating(false)}
          onCreated={() => {
            setInstanceTick((n) => n + 1)
            setView('instances')
          }}
        />
      ) : null}
    </div>
  )
}
