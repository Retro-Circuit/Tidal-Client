import { useEffect, useState } from 'react'
import { List, Play, Star } from 'lucide-react'
import { PlayerPreview } from '../components/PlayerPreview'
import type { GameInstance, InstanceRunState, ModpackCard, SessionState, WalletState } from '../../../shared/types'

const PARTNERS = [
  { name: 'Hypixel', ip: 'mc.hypixel.net', blurb: 'Bedwars, Skyblock, and the biggest network' },
  { name: 'Wynncraft', ip: 'play.wynncraft.com', blurb: 'A full MMORPG inside Minecraft' },
  { name: 'CubeCraft', ip: 'play.cubecraft.net', blurb: 'Minigames for every kind of player' },
  { name: 'The Hive', ip: 'play.hivemc.com', blurb: 'Fast queues and arcade games' },
  { name: 'Minehut', ip: 'minehut.com', blurb: 'Community servers you can hop into' },
  { name: 'Origin Realms', ip: 'play.originrealms.com', blurb: 'Survival RPG with custom content' }
]

export function HomeView({
  session,
  lastInstanceId,
  onLastInstance,
  onCreateInstance,
  onOpenMods,
  onOpenInstances,
  refreshKey
}: {
  session: SessionState
  lastInstanceId: string
  onLastInstance: (id: string) => void
  onCreateInstance: () => void
  onOpenMods: () => void
  onOpenInstances: () => void
  refreshKey: number
}) {
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [picker, setPicker] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [runState, setRunState] = useState<InstanceRunState>('idle')
  const [wallet, setWallet] = useState<WalletState | null>(null)
  const [featured, setFeatured] = useState<ModpackCard | null>(null)
  const [copied, setCopied] = useState<string | null>(null)
  const [online, setOnline] = useState<Record<string, number>>({})
  const [tab, setTab] = useState<'partners' | 'instances'>('partners')

  const selected =
    instances.find((item) => item.id === lastInstanceId) ??
    [...instances].sort((a, b) => (b.lastPlayed ?? b.createdAt) - (a.lastPlayed ?? a.createdAt))[0] ??
    null

  useEffect(() => {
    void window.tidal.listInstances().then((list) => {
      setInstances([...list].sort((a, b) => (b.lastPlayed ?? b.createdAt) - (a.lastPlayed ?? a.createdAt)))
    })
    void window.tidal.getWallet().then(setWallet)
    void window.tidal.searchModpacks('', { projectType: 'modpack' }).then((result) => {
      setFeatured(result.hits[0] ?? null)
    })
  }, [refreshKey])

  useEffect(() => {
    return window.tidal.onRunState((status) => {
      setRunState(status.state)
      if (status.error) setError(status.error)
    })
  }, [])

  useEffect(() => {
    let cancelled = false
    void Promise.all(
      PARTNERS.map(async (server) => {
        try {
          const res = await fetch(`https://api.mcsrvstat.us/3/${server.ip}`)
          if (!res.ok) return [server.ip, 0] as const
          const data = (await res.json()) as { players?: { online?: number } }
          return [server.ip, data.players?.online ?? 0] as const
        } catch {
          return [server.ip, 0] as const
        }
      })
    ).then((rows) => {
      if (!cancelled) setOnline(Object.fromEntries(rows))
    })
    return () => {
      cancelled = true
    }
  }, [])

  async function play(): Promise<void> {
    if (!selected) {
      onCreateInstance()
      return
    }
    setBusy(true)
    setError(null)
    onLastInstance(selected.id)
    const result = await window.tidal.launchInstance(selected.id)
    if (!result.ok) setError(result.error ?? 'Launch failed')
    setBusy(false)
  }

  async function copyIp(ip: string): Promise<void> {
    await navigator.clipboard.writeText(ip)
    setCopied(ip)
    window.setTimeout(() => setCopied(null), 1600)
  }

  const loggedIn = Boolean(session.loggedIn && session.profile)

  return (
    <div className="flex h-full min-h-0 flex-col gap-5">
      <div className="grid min-h-0 flex-1 grid-cols-[minmax(200px,240px)_minmax(0,1fr)_minmax(200px,240px)] gap-5">
        <article className="flex flex-col rounded-2xl border border-line bg-panel p-5">
          <p className="text-[11px] uppercase tracking-[0.22em] text-mute">Daily</p>
          <p className="mt-2 text-2xl font-semibold">{wallet?.points ?? 0} points</p>
          <p className="mt-2 text-sm leading-relaxed text-mute">
            Claim points here, then equip capes in-game with Left Alt.
          </p>
          <button
            type="button"
            disabled={!wallet?.canClaim}
            onClick={() => void window.tidal.claimDaily().then(setWallet)}
            className="mt-auto rounded-lg bg-tidal px-4 py-2.5 text-sm font-semibold text-white hover:brightness-110 disabled:opacity-40"
          >
            {wallet?.canClaim ? 'Claim daily +50' : 'Already claimed'}
          </button>
        </article>

        <div className="flex min-h-0 flex-col items-center justify-center rounded-2xl border border-line bg-panel">
          {loggedIn && session.profile ? (
            <>
              <p className="rounded-full bg-ink px-3 py-1 text-xs font-medium text-mist">{session.profile.name}</p>
              <PlayerPreview uuid={session.profile.id} />
            </>
          ) : (
            <div className="flex h-[380px] w-[280px] flex-col items-center justify-center text-center">
              <p className="text-sm font-semibold">Sign in to load your skin</p>
              <p className="mt-1 max-w-[220px] text-xs text-mute">Microsoft login unlocks your Tidal skin preview.</p>
            </div>
          )}
          <div className="relative mb-6 flex items-stretch gap-2">
            <button
              type="button"
              disabled={busy || runState === 'starting'}
              onClick={() => void play()}
              className="flex min-w-[260px] items-center justify-center gap-3 rounded-lg bg-tidal px-6 py-3 text-white shadow-[0_0_16px_rgba(3,73,252,0.35)] hover:brightness-110 disabled:opacity-50"
            >
              <Play size={16} fill="currentColor" />
              <span className="text-left">
                <span className="block text-sm font-semibold">
                  {busy || runState === 'starting' ? 'Starting…' : runState === 'running' ? 'Playing' : 'Play'}
                </span>
                <span className="block text-xs text-white/80">{selected ? selected.name : 'Create an instance'}</span>
              </span>
            </button>
            <button
              type="button"
              onClick={() => setPicker((open) => !open)}
              className="flex w-11 items-center justify-center rounded-lg border border-line bg-raised text-mist hover:border-tidal/50"
              title="Choose instance"
            >
              <List size={16} />
            </button>
            {picker ? (
              <div className="absolute right-0 bottom-14 z-30 w-72 overflow-hidden rounded-xl border border-line bg-panel shadow-[0_12px_40px_rgba(0,0,0,0.45)]">
                {instances.length === 0 ? (
                  <button type="button" onClick={onCreateInstance} className="w-full px-3 py-3 text-left text-sm">
                    Create your first instance
                  </button>
                ) : (
                  instances.map((instance) => (
                    <button
                      key={instance.id}
                      type="button"
                      onClick={() => {
                        onLastInstance(instance.id)
                        setPicker(false)
                      }}
                      className={`block w-full px-3 py-2.5 text-left text-sm hover:bg-raised ${
                        selected?.id === instance.id ? 'text-white' : 'text-mist'
                      }`}
                    >
                      <span className="block truncate font-medium">{instance.name}</span>
                      <span className="text-xs text-mute">
                        {instance.minecraftVersion} · {instance.loader}
                      </span>
                    </button>
                  ))
                )}
              </div>
            ) : null}
          </div>
          {error ? <p className="mb-4 max-w-md px-4 text-center text-xs text-red-300">{error}</p> : null}
        </div>

        <article className="flex flex-col overflow-hidden rounded-2xl border border-line bg-panel">
          {featured ? (
            <button type="button" onClick={onOpenMods} className="flex h-full flex-col p-4 text-left">
              {featured.iconUrl ? (
                <img src={featured.iconUrl} alt="" className="h-28 w-full rounded-xl object-cover" />
              ) : (
                <div className="h-28 rounded-xl bg-ink" />
              )}
              <p className="mt-3 text-[11px] uppercase tracking-[0.22em] text-mute">Featured pack</p>
              <p className="mt-1 truncate text-base font-semibold">{featured.title}</p>
              <p className="mt-1 line-clamp-4 text-sm text-mute">{featured.description}</p>
            </button>
          ) : (
            <button type="button" onClick={onOpenMods} className="p-5 text-left text-sm text-mute">
              Browse mods and modpacks
            </button>
          )}
        </article>
      </div>

      <section className="shrink-0 overflow-hidden rounded-2xl border border-line bg-panel">
        <div className="flex items-center gap-3 border-b border-line px-4 py-2.5">
          <button
            type="button"
            onClick={() => setTab('partners')}
            className={`flex items-center gap-1.5 text-xs font-semibold ${tab === 'partners' ? 'text-white' : 'text-mute'}`}
          >
            <Star size={12} className={tab === 'partners' ? 'text-tidal' : ''} />
            Partner servers
          </button>
          <button
            type="button"
            onClick={() => setTab('instances')}
            className={`text-xs font-semibold ${tab === 'instances' ? 'text-white' : 'text-mute'}`}
          >
            My instances
          </button>
          <button type="button" onClick={onOpenInstances} className="ml-auto text-xs font-semibold text-tidal">
            View more
          </button>
        </div>
        {tab === 'partners' ? (
          <div className="grid grid-cols-3">
            {PARTNERS.map((server) => (
              <button
                key={server.ip}
                type="button"
                onClick={() => void copyIp(server.ip)}
                className="flex items-center gap-3 border-r border-b border-line px-4 py-3 text-left last:border-r-0 hover:bg-raised"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-ink text-xs font-bold text-tidal">
                  {server.name.slice(0, 2).toUpperCase()}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{server.name}</p>
                  <p className="truncate text-xs text-mute">{copied === server.ip ? 'Copied IP' : server.blurb}</p>
                </div>
                <p className="shrink-0 text-xs font-medium text-emerald-400">
                  {online[server.ip] ? online[server.ip].toLocaleString() : '—'}
                </p>
              </button>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-3">
            {instances.slice(0, 6).map((instance) => (
              <button
                key={instance.id}
                type="button"
                onClick={() => onLastInstance(instance.id)}
                className="flex items-center gap-3 border-r border-b border-line px-4 py-3 text-left hover:bg-raised"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-ink text-xs font-bold text-tidal">
                  {instance.iconUrl ? (
                    <img src={instance.iconUrl} alt="" className="h-full w-full object-cover" />
                  ) : (
                    'MC'
                  )}
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{instance.name}</p>
                  <p className="text-xs text-mute">
                    {instance.minecraftVersion} · {instance.loader}
                  </p>
                </div>
              </button>
            ))}
            {instances.length === 0 ? (
              <p className="col-span-3 px-4 py-6 text-sm text-mute">No instances yet. Create one and it shows up here.</p>
            ) : null}
          </div>
        )}
      </section>
    </div>
  )
}
