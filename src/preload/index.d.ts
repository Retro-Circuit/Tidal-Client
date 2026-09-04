import type { TidalApi } from './index'

declare global {
  interface Window {
    tidal: TidalApi
  }
}

export {}
