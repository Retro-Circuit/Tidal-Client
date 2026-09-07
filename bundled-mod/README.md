# Tidal built-in radial menu

Fabric client mod injected into instances as `tidal-builtin.jar`.
It is hidden from the launcher mods list.

The **26.2** jar is the full menu (radial, skins, HUD, mixins). `scripts/pack-builtin-releases.mjs`
stamps a separate jar for every Mojang **release** (no snapshots) under `resources/tidal-builtin/`.
Other releases get a tiny Fabric stub so the game still starts; the mapped UI only exists for 26.2.

Hold **Left Alt** in-game for the radial on 26.2. Hover a button and release Alt to open it.

```bash
cd bundled-mod
./gradlew build
node ../scripts/pack-builtin-releases.mjs
```
