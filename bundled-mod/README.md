# Tidal built-in radial menu

Fabric client mod injected into Fabric/Quilt instances as `tidal-builtin.jar`.
It is hidden from the launcher mods list.

The jar accepts any Minecraft version so instances are not blocked by Fabric’s version check.
Menus, HUD, and mixins only run on **26.2** (the mappings this tree is compiled against). Other Java 21+ Fabric/Quilt versions load a no-op so the game still starts.

Hold **Left Alt** in-game for the radial (26.2). Hover a button and release Alt to open it. Release with no hover to close.

```bash
cd bundled-mod
./gradlew build
```
