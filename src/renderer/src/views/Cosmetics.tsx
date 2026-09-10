import { useEffect, useState } from 'react'
import { Gift } from 'lucide-react'
import type { WalletState } from '../../../shared/types'

export function CosmeticsView() {
  const [wallet, setWallet] = useState<WalletState | null>(null)

  useEffect(() => {
    void window.tidal.getWallet().then(setWallet)
  }, [])

  return (
    <div className="animate-rise mx-auto flex h-full max-w-2xl flex-col justify-center gap-6">
      <div>
        <p className="text-[11px] font-bold tracking-[0.24em] text-mute uppercase">In-game</p>
        <h2 className="mt-1 text-3xl font-semibold">Cosmetics</h2>
        <p className="mt-2 text-sm text-mute">
          Capes and local skins live in the Tidal menu. Launch Minecraft, then hold Left Alt.
        </p>
      </div>
      <div className="rounded-2xl border border-line bg-panel p-6">
        <p className="text-sm text-mute">Tidal points</p>
        <p className="mt-1 text-4xl font-extrabold">{wallet?.points ?? 0}</p>
        <button
          type="button"
          disabled={!wallet?.canClaim}
          onClick={() => void window.tidal.claimDaily().then(setWallet)}
          className="mt-4 flex items-center gap-2 rounded-lg bg-tidal px-4 py-2 text-sm font-semibold disabled:opacity-40"
        >
          <Gift size={16} />
          {wallet?.canClaim ? 'Claim daily +50' : 'Come back tomorrow'}
        </button>
      </div>
    </div>
  )
}
