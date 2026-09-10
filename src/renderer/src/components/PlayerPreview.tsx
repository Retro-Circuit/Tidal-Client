import { useEffect, useRef, useState } from 'react'
import { FunctionAnimation, SkinViewer } from 'skinview3d'
import type { PlayerObject } from 'skinview3d'
import type { SkinPreview } from '../../../shared/types'

function clamp01(value: number): number {
  return Math.min(1, Math.max(0, value))
}

function smoothstep(value: number): number {
  const t = clamp01(value)
  return t * t * (3 - 2 * t)
}

function shiftPulse(cycle: number): number {
  if (cycle > 0.18) return 0
  const t = cycle / 0.18
  return smoothstep(t < 0.5 ? t * 2 : 2 - t * 2)
}

function wrapDelta(delta: number): number {
  let value = delta
  while (value > Math.PI) value -= Math.PI * 2
  while (value < -Math.PI) value += Math.PI * 2
  return value
}

function createIdle(getSpin: () => number) {
  return (player: PlayerObject, progress: number): void => {
    player.resetJoints()

    const breath = Math.sin(progress * 1.45)
    const cycle = (progress / 8.5) % 1
    const dir = Math.floor(progress / 8.5) % 2 === 0 ? 1 : -1
    const lean = shiftPulse(cycle) * dir
    const spin = getSpin()
    const wind = Math.max(-1, Math.min(1, spin * 14))

    player.position.y = breath * 0.045
    player.rotation.z = lean * 0.02 - wind * 0.045
    player.rotation.x = Math.abs(wind) * 0.02

    player.skin.body.position.y = -6 + breath * 0.12
    player.skin.body.rotation.y = lean * 0.08 - wind * 0.12
    player.skin.body.rotation.z = lean * 0.025 - wind * 0.04
    player.skin.body.rotation.x = 0.02 + breath * 0.03

    player.skin.head.position.y = breath * 0.06
    player.skin.head.rotation.y = lean * 0.14 + Math.sin(progress * 0.4) * 0.03 - wind * 0.18
    player.skin.head.rotation.x = breath * 0.025 + Math.abs(wind) * 0.04
    player.skin.head.rotation.z = -wind * 0.06

    player.skin.leftArm.rotation.z = 0.03 + lean * 0.03 - wind * 0.08
    player.skin.rightArm.rotation.z = -0.03 + lean * 0.03 - wind * 0.08
    player.skin.leftArm.rotation.x = breath * 0.03 - Math.abs(wind) * 0.12
    player.skin.rightArm.rotation.x = -breath * 0.03 - Math.abs(wind) * 0.12

    player.skin.leftLeg.rotation.x = lean * 0.04 + wind * 0.03
    player.skin.rightLeg.rotation.x = -lean * 0.035 - wind * 0.03
    player.skin.leftLeg.rotation.z = 0.02
    player.skin.rightLeg.rotation.z = -0.02

    player.cape.rotation.x = 0.14 + breath * 0.03 + Math.abs(wind) * 0.55
    player.cape.rotation.z = -wind * 0.12
  }
}

export function PlayerPreview({ uuid }: { uuid?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const wrapRef = useRef<HTMLDivElement>(null)
  const [preview, setPreview] = useState<SkinPreview | null>(null)

  useEffect(() => {
    let cancelled = false
    const load = (): void => {
      void window.tidal.getSkinPreview(uuid).then((next) => {
        if (cancelled || !next) return
        setPreview((prev) =>
          prev &&
          prev.pngBase64 === next.pngBase64 &&
          prev.capeBase64 === next.capeBase64 &&
          prev.source === next.source &&
          prev.slim === next.slim
            ? prev
            : next
        )
      })
    }
    load()
    const id = window.setInterval(load, 2500)
    return () => {
      cancelled = true
      window.clearInterval(id)
    }
  }, [uuid])

  useEffect(() => {
    const canvas = canvasRef.current
    const wrap = wrapRef.current
    if (!canvas || !wrap || !preview) return

    let lastYaw = 0
    let spin = 0
    const holder: { viewer?: SkinViewer } = {}
    const viewer = new SkinViewer({
      canvas,
      width: Math.max(200, wrap.clientWidth),
      height: Math.max(300, wrap.clientHeight),
      animation: new FunctionAnimation(
        createIdle(() => {
          const current = holder.viewer
          if (!current) return 0
          const yaw = current.controls.getAzimuthalAngle()
          const delta = wrapDelta(yaw - lastYaw)
          lastYaw = yaw
          spin += (delta - spin) * 0.28
          if (Math.abs(delta) < 0.0004) spin *= 0.9
          return spin
        })
      ),
      zoom: 0.86,
      fov: 45
    })
    holder.viewer = viewer
    viewer.renderer.setClearColor(0x1c2026, 1)
    viewer.globalLight.intensity = 2.4
    viewer.cameraLight.intensity = 0.7
    viewer.autoRotate = false
    viewer.controls.enablePan = false
    viewer.controls.enableZoom = false
    viewer.controls.enableRotate = true
    viewer.playerObject.rotation.y = Math.PI / 8
    if (viewer.animation) viewer.animation.speed = 1

    const fit = (): void => {
      viewer.setSize(Math.max(200, wrap.clientWidth), Math.max(300, wrap.clientHeight))
    }
    fit()
    const ro = new ResizeObserver(fit)
    ro.observe(wrap)

    void (async () => {
      await viewer.loadSkin(`data:image/png;base64,${preview.pngBase64}`, {
        model: preview.slim ? 'slim' : 'default'
      })
      if (preview.capeBase64) {
        try {
          await viewer.loadCape(`data:image/png;base64,${preview.capeBase64}`)
        } catch {
          /* cape is optional */
        }
      }
    })()

    return () => {
      ro.disconnect()
      viewer.dispose()
    }
  }, [preview])

  return (
    <div className="flex flex-col items-center">
      <div ref={wrapRef} className="h-[380px] w-[260px]">
        <canvas ref={canvasRef} className="block h-full w-full cursor-grab active:cursor-grabbing" />
      </div>
      <p className="mb-2 max-w-[240px] text-center text-[11px] text-mute">
        {preview?.source === 'saved'
          ? 'Tidal skin saved from your last world.'
          : 'Your Minecraft skin. Join a world to lock in a Tidal skin.'}
      </p>
    </div>
  )
}
