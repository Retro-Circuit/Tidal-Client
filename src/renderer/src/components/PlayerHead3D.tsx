import { useEffect, useRef, useState } from 'react'
import { Box3, Vector3 } from 'three'
import { SkinViewer } from 'skinview3d'
import type { SkinPreview } from '../../../shared/types'

export function PlayerHead3D({ uuid, size = 28 }: { uuid?: string; size?: number }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [preview, setPreview] = useState<SkinPreview | null>(null)

  useEffect(() => {
    let cancelled = false
    const load = (): void => {
      void window.tidal.getSkinPreview(uuid).then((next) => {
        if (cancelled || !next) return
        setPreview((prev) =>
          prev && prev.pngBase64 === next.pngBase64 && prev.source === next.source && prev.slim === next.slim
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
    if (!canvas || !preview) return

    const viewer = new SkinViewer({
      canvas,
      width: size,
      height: size,
      zoom: 3.55,
      fov: 32,
      enableControls: false,
      pixelRatio: 'match-device'
    })
    viewer.renderer.setClearColor(0x000000, 0)
    viewer.globalLight.intensity = 2.6
    viewer.cameraLight.intensity = 0.9
    viewer.autoRotate = false
    viewer.animation = null

    const player = viewer.playerObject
    player.skin.body.visible = false
    player.skin.leftArm.visible = false
    player.skin.rightArm.visible = false
    player.skin.leftLeg.visible = false
    player.skin.rightLeg.visible = false
    player.cape.visible = false
    player.elytra.visible = false
    player.ears.visible = false

    let cancelled = false
    void (async () => {
      await viewer.loadSkin(`data:image/png;base64,${preview.pngBase64}`, {
        model: preview.slim ? 'slim' : 'default'
      })
      if (cancelled) return
      player.skin.head.updateWorldMatrix(true, true)
      const box = new Box3().setFromObject(player.skin.head)
      const center = box.getCenter(new Vector3())
      viewer.controls.target.copy(center)
      viewer.camera.position.set(center.x + 14, center.y + 8, center.z + 22)
      viewer.camera.lookAt(center)
      viewer.controls.update()
    })()

    return () => {
      cancelled = true
      viewer.dispose()
    }
  }, [preview, size])

  return (
    <canvas
      ref={canvasRef}
      className="pointer-events-none block shrink-0"
      style={{ width: size, height: size }}
      aria-hidden
    />
  )
}
