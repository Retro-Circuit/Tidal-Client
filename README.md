# Tidal Client

A dark, minimal Minecraft launcher built with Electron, React, Tailwind CSS, and TypeScript.

## What it does

- **Microsoft login** via `msmc` (official account flow in a popup window)
- **Discover** popular and searchable modpacks from Modrinth and/or CurseForge
- **Install** a modpack into a local instance (mods + overrides + Fabric/Forge/Quilt/NeoForge)
- **Launch** that instance with `@xmcl/core` and `@xmcl/installer` (assets, libraries, Java, classpath)

The old `@xmcl/minecraft-launcher-core` meta-package (last published 2019) is not used. The current XMCL stack is the modular packages above.

## Setup

```bash
cd Desktop/Coding/tidal-client
npm install
npm run dev
```

### CurseForge

1. Create an API key at [console.curseforge.com](https://console.curseforge.com/).
2. Paste it in **Settings**, or copy `.env.example` to `.env` and set `CURSEFORGE_API_KEY`.

Modrinth search works with no key.

### Java

Install a JDK 21+ (Temurin is fine) or set an explicit `java.exe` path in Settings. Tidal also scans common Windows Java folders and `PATH`.

### Microsoft login

Use **Login with Microsoft** in the top right. You need a Microsoft account that owns Minecraft Java Edition. The session refresh token is stored locally by the app (not in git).

## Layout

- `src/main` — Electron main process: auth, APIs, install, launch
- `src/preload` — Isolated IPC bridge (`window.tidal`)
- `src/renderer` — React UI
- `src/shared` — Shared TypeScript types
"# tidal-client" 
