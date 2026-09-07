package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class StatHuds {
    private StatHuds() {}

    public static void extract(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        if (hidden(minecraft)) {
            return;
        }
        if (TidalMods.cps) {
            HudDraw.scaled(graphics, TidalMods.cpsX, TidalMods.cpsY, TidalMods.cpsScale,
                () -> drawCps(graphics, minecraft, 0, 0, false));
        }
        if (TidalMods.fpsPing) {
            HudDraw.scaled(graphics, TidalMods.fpsX, TidalMods.fpsY, TidalMods.fpsScale,
                () -> drawFps(graphics, minecraft, 0, 0, false));
        }
        if (TidalMods.combo) {
            HudDraw.scaled(graphics, TidalMods.comboX, TidalMods.comboY, TidalMods.comboScale,
                () -> drawCombo(graphics, minecraft, 0, 0, false));
        }
    }

    private static boolean hidden(Minecraft minecraft) {
        Screen screen = minecraft.screen;
        return screen instanceof TidalPanelScreen || screen instanceof RadialScreen || screen instanceof LayoutMenuScreen;
    }

    static int cpsWidth(Minecraft minecraft) {
        return Math.max(48, minecraft.font.width(cpsText()) + 12);
    }

    static int fpsWidth(Minecraft minecraft) {
        return Math.max(64, minecraft.font.width(fpsText(minecraft)) + 12);
    }

    static int comboWidth(Minecraft minecraft) {
        return Math.max(48, minecraft.font.width(comboText()) + 12);
    }

    static int textHeight() {
        return 16;
    }

    static void drawCps(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, cpsWidth(minecraft), cpsText(), editing);
    }

    static void drawFps(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, fpsWidth(minecraft), fpsText(minecraft), editing);
    }

    static void drawCombo(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, comboWidth(minecraft), comboText(), editing);
    }

    private static void pill(
        GuiGraphics graphics,
        Minecraft minecraft,
        int x,
        int y,
        int w,
        String text,
        boolean editing
    ) {
        int h = textHeight();
        if (editing) {
            Glass.roundedFill(graphics, x - 3, y - 3, x + w + 3, y + h + 3, 8, 0x330349FC);
        }
        Glass.roundedFill(graphics, x, y, x + w, y + h, 6, 0x99000000);
        graphics.drawCenteredString(minecraft.font, text, x + w / 2, y + 4, Glass.TEXT);
    }

    private static String cpsText() {
        return HudRuntime.leftCps() + " | " + HudRuntime.rightCps() + " CPS";
    }

    private static String fpsText(Minecraft minecraft) {
        return minecraft.getFps() + " FPS  " + HudRuntime.ping(minecraft) + " ms";
    }

    private static String comboText() {
        return HudRuntime.combo() + " combo";
    }
}
