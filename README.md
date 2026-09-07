# Tidal Client

A dark, minimal Minecraft launcher built with Electron, React, Tailwind CSS, and TypeScript. (Cursor was Used for Bug fixes.)

## What it does

- **Microsoft login** via `msmc` (official account flow in a popup window)
- **Discover** popular and searchable modpacks from Modrinth and/or CurseForge
- **Install** a modpack into a local instance (mods + overrides + Fabric/Forge/Quilt/NeoForge)
- **Launch** that instance with `@xmcl/core` and `@xmcl/installer` (assets, libraries, Java, classpath)

The old `@xmcl/minecraft-launcher-core` meta-package (last published 2019) is not used. The current XMCL stack is the modular packages above.

## Setup

Needs **Node 20+**. For a Windows `.exe`, also needs the usual Visual C++ runtime on the machine that *runs* the app (Windows already has it).

```bash
npm install
npm run dev
```

### Package a Windows .exe

```bash
npm install
npm run dist
```

That compiles the launcher and writes installers to `release/`:

- `TidalClient-Setup-<version>.exe` — NSIS installer (choose install folder)
- `TidalClient-<version>-portable.exe` — single portable exe, no install

`npm run dist:full` builds the bundled Fabric mod with Gradle first (JDK 25), then the exe. `npm run dist:dir` unpacks the app into `release/win-unpacked/` for a quick local test without wrapping an installer.

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
"# congenial-telegram" 
