import { resolve } from 'path'
import { defineConfig, externalizeDepsPlugin } from 'electron-vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  main: {
    resolve: {
      alias: {
        '@xmcl/unzip': resolve(__dirname, 'node_modules/@xmcl/unzip/dist/index.js'),
        '@xmcl/core': resolve(__dirname, 'node_modules/@xmcl/core/dist/index.js'),
        '@xmcl/installer': resolve(__dirname, 'node_modules/@xmcl/installer/dist/index.js'),
        '@xmcl/user': resolve(__dirname, 'node_modules/@xmcl/user/dist/index.js')
      }
    },
    build: {
      rollupOptions: {
        // Keep standard Node built-ins external, but force @xmcl to bundle
        external: ['electron', 'crypto', 'fs', 'path', 'os', 'child_process', 'stream', 'util', 'events', 'http', 'https', 'zlib']
      }
    }
    // Notice: externalizeDepsPlugin() is completely removed from main!
  },
  preload: {
    plugins: [externalizeDepsPlugin()]
  },
  renderer: {
    resolve: {
      alias: {
        '@renderer': resolve('src/renderer/src')
      }
    },
    plugins: [react()]
  }
})