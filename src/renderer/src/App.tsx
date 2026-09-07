import { useEffect, useState } from 'react'
import type { AppSettings, ForeignInstance, NavView, ProjectType, SessionState } from '../../shared/types'
import { CreateInstanceModal } from './components/CreateInstanceModal'
import { ImportInstancesModal } from './components/ImportInstancesModal'
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
  const [foreign, setForeign] = useState<ForeignInstance[] | null>(null)
  const [instanceTick, setInstanceTick] = useState(0)
  const [discoverIntent, setDiscoverIntent] = useState<{
    projectType?: ProjectType | 'all'
    gameVersion?: string
    instanceId?: string
  }>()

  useEffect(() => {
    void window.tidal.session().then(setSession)
    void window.tidal.getSettings().then(async (next) => {
      setSettings(next)
      if (next.importPromptDismissed) return
      const found = await window.tidal.scanForeignInstances()
      if (found.length) setForeign(found)
    })
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
        onChange={(next) => {
          if (next === 'discover' && view !== 'discover') setDiscoverIntent(undefined)
          setView(next)
        }}
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
          {view === 'discover' ? (
            <DiscoverView settings={settings} onSettings={patchSettings} intent={discoverIntent} />
          ) : null}
          {view === 'instances' ? (
            <InstancesView
              onCreateInstance={() => setCreating(true)}
              onDiscover={(intent) => {
                setDiscoverIntent(intent ?? { projectType: 'mod' })
                setView('discover')
              }}
              refreshKey={instanceTick}
            />
          ) : null}
          {view === 'settings' ? (
            <SettingsView
              settings={settings}
              onSettings={patchSettings}
              onScanLaunchers={async () => {
                const found = await window.tidal.scanForeignInstances()
                if (found.length) setForeign(found)
              }}
            />
          ) : null}
        </section>
      </main>
      {foreign ? (
        <ImportInstancesModal
          found={foreign}
          onClose={() => {
            setForeign(null)
            void patchSettings({ importPromptDismissed: true })
          }}
          onImported={() => {
            setInstanceTick((n) => n + 1)
            setView('instances')
            void patchSettings({ importPromptDismissed: true })
          }}
        />
      ) : null}
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
