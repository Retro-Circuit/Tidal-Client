package com.tidal.builtin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

final class HudDraw {
    private HudDraw() {}

    static void scaled(GuiGraphicsExtractor graphics, int x, int y, float scale, Runnable draw) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        draw.run();
        pose.popMatrix();
    }

    static float clampScale(float scale) {
        return Math.max(0.5f, Math.min(2.5f, scale));
    }

    static int scaled(int size, float scale) {
        return Math.max(1, Math.round(size * scale));
    }
}
