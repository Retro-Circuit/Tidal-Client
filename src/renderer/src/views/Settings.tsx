import type { AppSettings } from '../../../shared/types'

export function SettingsView({
  settings,
  onSettings
}: {
  settings: AppSettings | null
  onSettings: (patch: Partial<AppSettings>) => Promise<void>
}) {
  if (!settings) return null

  return (
    <div className="animate-rise mx-auto flex w-full max-w-2xl flex-col gap-6">
      <div>
        <p className="text-xs uppercase tracking-[0.24em] text-mute">Preferences</p>
        <h2 className="mt-1 text-3xl font-semibold">Settings</h2>
      </div>

      <section className="rounded-2xl border border-line bg-panel p-5">
        <h3 className="text-sm font-semibold">CurseForge API key</h3>
        <p className="mt-1 text-xs text-mute">
          Stored locally in this app. Create a key at console.curseforge.com, or put it in a `.env` file as
          CURSEFORGE_API_KEY.
        </p>
        <input
          type="password"
          value={settings.curseforgeApiKey}
          onChange={(e) => void onSettings({ curseforgeApiKey: e.target.value })}
          placeholder="Paste API key"
          className="mt-3 w-full rounded-xl border border-line bg-raised px-3 py-2 text-sm outline-none focus:border-tidal"
        />
      </section>

      <section className="rounded-2xl border border-line bg-panel p-5">
        <h3 className="text-sm font-semibold">Java</h3>
        <p className="mt-1 text-xs text-mute">Leave empty to auto-detect a local JDK.</p>
        <input
          value={settings.javaPath}
          onChange={(e) => void onSettings({ javaPath: e.target.value })}
          placeholder="C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe"
          className="mt-3 w-full rounded-xl border border-line bg-raised px-3 py-2 text-sm outline-none focus:border-tidal"
        />
      </section>

      <section className="rounded-2xl border border-line bg-panel p-5">
        <h3 className="text-sm font-semibold">Memory</h3>
        <div className="mt-3 grid grid-cols-2 gap-3">
          <label className="text-xs text-mute">
            Minimum (MB)
            <input
              type="number"
              value={settings.minMemoryMb}
              onChange={(e) => void onSettings({ minMemoryMb: Number(e.target.value) })}
              className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2 text-sm text-white outline-none focus:border-tidal"
            />
          </label>
          <label className="text-xs text-mute">
            Maximum (MB)
            <input
              type="number"
              value={settings.maxMemoryMb}
              onChange={(e) => void onSettings({ maxMemoryMb: Number(e.target.value) })}
              className="mt-1 w-full rounded-xl border border-line bg-raised px-3 py-2 text-sm text-white outline-none focus:border-tidal"
            />
          </label>
        </div>
      </section>
    </div>
  )
}
