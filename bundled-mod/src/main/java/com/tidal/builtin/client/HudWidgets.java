package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
                (g, mc, x, y) -> StatHuds.drawCombo(g, mc, 0, 0, true))
        };
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

        void render(GuiGraphicsExtractor graphics, Minecraft minecraft) {
            HudDraw.scaled(graphics, this.x(), this.y(), this.scale(), () -> this.draw.draw(graphics, minecraft, 0, 0));
            graphics.text(minecraft.font, this.name, this.x(), this.y() - 10, Glass.MUTE, false);
        }
    }

    interface IntGet { int get(); }
    interface IntSet { void set(int value); }
    interface FloatGet { float get(); }
    interface FloatSet { void set(float value); }
    interface Draw { void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y); }
}
