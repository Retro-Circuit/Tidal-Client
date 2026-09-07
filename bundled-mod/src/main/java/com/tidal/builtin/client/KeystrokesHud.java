package com.tidal.builtin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class KeystrokesHud {
    private static final int KEY = 16;
    private static final int GAP = 3;
    static final int WIDTH = KEY * 3 + GAP * 2;
    static final int HEIGHT = KEY * 3 + GAP * 2;
    private static final float[] ANIM = new float[6];

    private KeystrokesHud() {}

    static int x() {
        return TidalMods.hudX;
    }

    static int y() {
        return TidalMods.hudY < 0 ? 8 : TidalMods.hudY;
    }

    public static void extract(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (!TidalMods.keystrokes || minecraft.player == null) {
            return;
        }
        if (minecraft.gui.screen() instanceof TidalPanelScreen
            || minecraft.gui.screen() instanceof RadialScreen
            || minecraft.gui.screen() instanceof LayoutMenuScreen) {
            return;
        }
        HudDraw.scaled(graphics, x(), y(), TidalMods.hudScale, () -> draw(graphics, minecraft, 0, 0, false));
    }

    static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, int originX, int originY, boolean editing) {
        Options options = minecraft.options;
        KeyMapping[] keys = {
            options.keyUp,
            options.keyLeft,
            options.keyDown,
            options.keyRight,
            options.keyAttack,
            options.keyUse
        };
        for (int i = 0; i < keys.length; i++) {
            float target = keys[i].isDown() ? 1.0f : 0.0f;
            ANIM[i] += (target - ANIM[i]) * (target > ANIM[i] ? 0.55f : 0.22f);
        }
        Font font = minecraft.font;
        if (editing) {
            Glass.roundedFill(graphics, originX - 3, originY - 3, originX + WIDTH + 3, originY + HEIGHT + 3, 8, 0x330349FC);
        }
        int row2 = originY + KEY + GAP;
        int row3 = originY + (KEY + GAP) * 2;
        drawKey(graphics, font, originX + KEY + GAP, originY, KEY, KEY, "W", ANIM[0]);
        drawKey(graphics, font, originX, row2, KEY, KEY, "A", ANIM[1]);
        drawKey(graphics, font, originX + KEY + GAP, row2, KEY, KEY, "S", ANIM[2]);
        drawKey(graphics, font, originX + (KEY + GAP) * 2, row2, KEY, KEY, "D", ANIM[3]);
        int half = (WIDTH - GAP) / 2;
        drawKey(graphics, font, originX, row3, half, KEY, "LMB", ANIM[4]);
        drawKey(graphics, font, originX + half + GAP, row3, WIDTH - half - GAP, KEY, "RMB", ANIM[5]);
    }

    private static void drawKey(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, String label, float press) {
        int color = press > 0.5f ? 0xFF3D6FFF : Glass.ACCENT;
        Glass.roundedFill(graphics, x, y, x + w, y + h, 5, color);
        graphics.centeredText(font, label, x + w / 2, y + (h - 8) / 2, Glass.TEXT);
    }
}
