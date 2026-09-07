# Tidal built-in radial menu (Minecraft 1.21.x)

Same in-game UI as the 26.2 menu, compiled against **1.21.1** Mojang mappings
(`GuiGraphics`, `Screen.render`, `ResourceLocation`). The packer stamps this jar
onto every `1.21` / `1.21.*` release.

Later 1.21 patches can still miss mixins if Mojang renamed a method after 1.21.1.

```bash
cd bundled-mod-121
./gradlew.bat build
```

That writes `resources/tidal-builtin-121.jar`. Then run `node scripts/pack-builtin-releases.mjs`.
