# Tidal built-in radial menu

Fabric client mod injected into instances as `tidal-builtin.jar`.
It is hidden from the launcher mods list.

The **26.2** jar is the full menu for 26.x. **bundled-mod-121** is the same UI compiled
for **1.21.1** and stamped onto every 1.21.x release. Other versions get a Fabric stub.

```bash
cd bundled-mod
./gradlew build
cd ../bundled-mod-121
./gradlew.bat build
node ../scripts/pack-builtin-releases.mjs
```
