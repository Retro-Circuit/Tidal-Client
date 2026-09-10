import { useEffect, useState } from 'react'
import type { AppSettings, ForeignInstance, NavView, ProjectType, SessionState } from '../../shared/types'
import { Chrome } from './components/Chrome'
import { CreateInstanceModal } from './components/CreateInstanceModal'
import { ImportInstancesModal } from './components/ImportInstancesModal'
import { CosmeticsView } from './views/Cosmetics'
import { DiscoverView } from './views/Discover'
import { HomeView } from './views/Home'
import { InstancesView } from './views/Instances'
import { SettingsView } from './views/Settings'

export default function App() {
  const [view, setView] = useState<NavView>('home')
  const [session, setSession] = useState<SessionState>({ loggedIn: false, profile: null })
  const [loggingIn, setLoggingIn] = useState(false)
  const [loginError, setLoginError] = useState<string | null>(null)
  const [settings, setSettings] = useState<AppSettings | null>(null)
  const [creating, setCreating] = useState(false)
  const [foreign, setForeign] = useState<ForeignInstance[] | null>(null)
  const [instanceTick, setInstanceTick] = useState(0)
  const [scanNote, setScanNote] = useState<string | null>(null)
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
      setLoginError(
        error instanceof Error
          ? error.message.replace(/^Error invoking remote method '[^']+':\s*/, '')
          : String(error)
      )
    } finally {
      setLoggingIn(false)
    }
  }

  return (
    <div className="flex h-full flex-col">
      <Chrome
        view={view}
        onChange={(next) => {
          if (next === 'discover' && view !== 'discover') setDiscoverIntent(undefined)
          setView(next)
        }}
        session={session}
        loggingIn={loggingIn}
        loginError={loginError}
        onLogin={() => void login()}
        onLogout={() => void window.tidal.logout().then(setSession)}
      />
      <section className="no-drag min-h-0 flex-1 overflow-hidden px-6 py-5">
        {view === 'home' ? (
          <HomeView
            session={session}
            lastInstanceId={settings?.lastInstanceId ?? ''}
            onLastInstance={(id) => void patchSettings({ lastInstanceId: id })}
            onCreateInstance={() => setCreating(true)}
            onOpenMods={() => setView('discover')}
            onOpenInstances={() => setView('instances')}
            refreshKey={instanceTick}
          />
        ) : null}
        {view === 'discover' ? (
          <DiscoverView
            settings={settings}
            onSettings={patchSettings}
            intent={discoverIntent}
            onClearIntent={() => setDiscoverIntent(undefined)}
          />
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
        {view === 'cosmetics' ? <CosmeticsView /> : null}
        {view === 'settings' ? (
          <SettingsView
            settings={settings}
            onSettings={patchSettings}
            scanNote={scanNote}
            onScanLaunchers={async () => {
              const found = await window.tidal.scanForeignInstances()
              if (found.length) {
                setScanNote(null)
                setForeign(found)
              } else {
                setScanNote('No other launcher instances were found on this PC.')
              }
            }}
          />
        ) : null}
      </section>
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
            setView('home')
          }}
        />
      ) : null}
    </div>
  )
}
