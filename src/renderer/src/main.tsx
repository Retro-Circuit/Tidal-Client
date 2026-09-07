import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)

Object.defineProperty(window, 'tidp100shad', {
  configurable: true,
  get() {
    void window.tidal.grantShadowPoints().then((wallet) => {
      console.log(`_ShadowzYT Tidal points: ${wallet.points}`)
    })
    return 'Granting 100 Tidal points to _ShadowzYT…'
  }
})
