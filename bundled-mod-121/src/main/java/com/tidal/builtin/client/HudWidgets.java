package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class HudWidgets {
    private HudWidgets() {}

    static Widget[] all() {
        return new Widget[] {
            new Widget("Keystrokes", () -> TidalMods.hudX, () -> TidalMods.hudY, () -> TidalMods.hudScale,
                (x) -> TidalMods.hudX = x, (y) -> TidalMods.hudY = y, (s) -> TidalMods.hudScale = s,
                KeystrokesHud.WIDTH, KeystrokesHud.HEIGHT,
                (g, mc, x, y) -> KeystrokesHud.draw(g, mc, 0, 0, true)),
            new Widget("Armor", () -> TidalMods.armorX, () -> TidalMods.armorY, () -> TidalMods.armorScale,
                (x) -> TidalMods.armorX = x, (y) -> TidalMods.armorY = y, (s) -> TidalMods.armorScale = s,
                ArmorHud.width(), ArmorHud.height(),
                (g, mc, x, y) -> ArmorHud.draw(g, mc, 0, 0, true)),
            new Widget("CPS", () -> TidalMods.cpsX, () -> TidalMods.cpsY, () -> TidalMods.cpsScale,
                (x) -> TidalMods.cpsX = x, (y) -> TidalMods.cpsY = y, (s) -> TidalMods.cpsScale = s,
                StatHuds.cpsWidth(Minecraft.getInstance()), StatHuds.textHeight(),
                (g, mc, x, y) -> StatHuds.drawCps(g, mc, 0, 0, true)),
            new Widget("FPS / Ping", () -> TidalMods.fpsX, () -> TidalMods.fpsY, () -> TidalMods.fpsScale,
                (x) -> TidalMods.fpsX = x, (y) -> TidalMods.fpsY = y, (s) -> TidalMods.fpsScale = s,
                StatHuds.fpsWidth(Minecraft.getInstance()), StatHuds.textHeight(),
                (g, mc, x, y) -> StatHuds.drawFps(g, mc, 0, 0, true)),
            new Widget("Combo", () -> TidalMods.comboX, () -> TidalMods.comboY, () -> TidalMods.comboScale,
                (x) -> TidalMods.comboX = x, (y) -> TidalMods.comboY = y, (s) -> TidalMods.comboScale = s,
                StatHuds.comboWidth(Minecraft.getInstance()), StatHuds.textHeight(),
                (g, mc, x, y) -> StatHuds.drawCombo(g, mc, 0, 0, true)),
            extra("Coords", () -> PlayMods.coordsX, () -> PlayMods.coordsY, () -> PlayMods.coordsScale,
                (x) -> PlayMods.coordsX = x, (y) -> PlayMods.coordsY = y, (s) -> PlayMods.coordsScale = s,
                (g, mc, x, y) -> ExtraHuds.drawCoords(g, mc, 0, 0, true)),
            extra("Nether", () -> PlayMods.netherX, () -> PlayMods.netherY, () -> PlayMods.netherScale,
                (x) -> PlayMods.netherX = x, (y) -> PlayMods.netherY = y, (s) -> PlayMods.netherScale = s,
                (g, mc, x, y) -> ExtraHuds.drawNether(g, mc, 0, 0, true)),
            extra("Compass", () -> PlayMods.compassX, () -> PlayMods.compassY, () -> PlayMods.compassScale,
                (x) -> PlayMods.compassX = x, (y) -> PlayMods.compassY = y, (s) -> PlayMods.compassScale = s,
                (g, mc, x, y) -> ExtraHuds.drawCompass(g, mc, 0, 0, true)),
            extra("Biome", () -> PlayMods.biomeX, () -> PlayMods.biomeY, () -> PlayMods.biomeScale,
                (x) -> PlayMods.biomeX = x, (y) -> PlayMods.biomeY = y, (s) -> PlayMods.biomeScale = s,
                (g, mc, x, y) -> ExtraHuds.drawBiome(g, mc, 0, 0, true)),
            extra("Clock", () -> PlayMods.clockX, () -> PlayMods.clockY, () -> PlayMods.clockScale,
                (x) -> PlayMods.clockX = x, (y) -> PlayMods.clockY = y, (s) -> PlayMods.clockScale = s,
                (g, mc, x, y) -> ExtraHuds.drawClock(g, mc, 0, 0, true)),
            extra("Speed", () -> PlayMods.speedX, () -> PlayMods.speedY, () -> PlayMods.speedScale,
                (x) -> PlayMods.speedX = x, (y) -> PlayMods.speedY = y, (s) -> PlayMods.speedScale = s,
                (g, mc, x, y) -> ExtraHuds.drawSpeed(g, mc, 0, 0, true)),
            extra("Memory", () -> PlayMods.memoryX, () -> PlayMods.memoryY, () -> PlayMods.memoryScale,
                (x) -> PlayMods.memoryX = x, (y) -> PlayMods.memoryY = y, (s) -> PlayMods.memoryScale = s,
                (g, mc, x, y) -> ExtraHuds.drawMemory(g, mc, 0, 0, true)),
            extra("Server", () -> PlayMods.serverX, () -> PlayMods.serverY, () -> PlayMods.serverScale,
                (x) -> PlayMods.serverX = x, (y) -> PlayMods.serverY = y, (s) -> PlayMods.serverScale = s,
                (g, mc, x, y) -> ExtraHuds.drawServer(g, mc, 0, 0, true)),
            new Widget("Potions", () -> PlayMods.potionX < 0 ? 8 : PlayMods.potionX, () -> PlayMods.potionY, () -> PlayMods.potionScale,
                (x) -> PlayMods.potionX = x, (y) -> PlayMods.potionY = y, (s) -> PlayMods.potionScale = s,
                ExtraHuds.potionWidth(Minecraft.getInstance()), ExtraHuds.potionHeight(Minecraft.getInstance()),
                (g, mc, x, y) -> ExtraHuds.drawPotions(g, mc, 0, 0, true)),
            extra("Saturation", () -> PlayMods.satX, () -> PlayMods.satY, () -> PlayMods.satScale,
                (x) -> PlayMods.satX = x, (y) -> PlayMods.satY = y, (s) -> PlayMods.satScale = s,
                (g, mc, x, y) -> ExtraHuds.drawSat(g, mc, 0, 0, true)),
            extra("Day", () -> PlayMods.dayX, () -> PlayMods.dayY, () -> PlayMods.dayScale,
                (x) -> PlayMods.dayX = x, (y) -> PlayMods.dayY = y, (s) -> PlayMods.dayScale = s,
                (g, mc, x, y) -> ExtraHuds.drawDay(g, mc, 0, 0, true))
        };
    }

    private static Widget extra(String name, IntGet xGet, IntGet yGet, FloatGet scaleGet, IntSet xSet, IntSet ySet, FloatSet scaleSet, Draw draw) {
        Minecraft minecraft = Minecraft.getInstance();
        return new Widget(name,
            () -> xGet.get() < 0 ? Math.max(8, minecraft.getWindow().getGuiScaledWidth() - 80) : xGet.get(),
            yGet, scaleGet, xSet, ySet, scaleSet, 80, StatHuds.textHeight(), draw);
    }

    static final class Widget {
        final String name;
        private final IntGet xGet;
        private final IntGet yGet;
        private final FloatGet scaleGet;
        private final IntSet xSet;
        private final IntSet ySet;
        private final FloatSet scaleSet;
        final int baseW;
        final int baseH;
        final Draw draw;

        Widget(String name, IntGet xGet, IntGet yGet, FloatGet scaleGet, IntSet xSet, IntSet ySet, FloatSet scaleSet, int baseW, int baseH, Draw draw) {
            this.name = name;
            this.xGet = xGet;
            this.yGet = yGet;
            this.scaleGet = scaleGet;
            this.xSet = xSet;
            this.ySet = ySet;
            this.scaleSet = scaleSet;
            this.baseW = baseW;
            this.baseH = baseH;
            this.draw = draw;
        }

        int x() {
            return this.xGet.get();
        }

        int y() {
            return this.yGet.get();
        }

        float scale() {
            return this.scaleGet.get();
        }

        void setX(int x) {
            this.xSet.set(x);
        }

        void setY(int y) {
            this.ySet.set(y);
        }

        void setScale(float scale) {
            this.scaleSet.set(HudDraw.clampScale(scale));
        }

        int w() {
            return HudDraw.scaled(this.baseW, this.scale());
        }

        int h() {
            return HudDraw.scaled(this.baseH, this.scale());
        }

        boolean hit(int mx, int my) {
            return TidalPanelScreen.in(mx, my, this.x(), this.y(), this.x() + this.w(), this.y() + this.h());
        }

        void render(GuiGraphics graphics, Minecraft minecraft) {
            HudDraw.scaled(graphics, this.x(), this.y(), this.scale(), () -> this.draw.draw(graphics, minecraft, 0, 0));
            graphics.drawString(minecraft.font, this.name, this.x(), this.y() - 10, Glass.MUTE, false);
        }
    }

    interface IntGet { int get(); }
    interface IntSet { void set(int value); }
    interface FloatGet { float get(); }
    interface FloatSet { void set(float value); }
    interface Draw { void draw(GuiGraphics graphics, Minecraft minecraft, int x, int y); }
}
