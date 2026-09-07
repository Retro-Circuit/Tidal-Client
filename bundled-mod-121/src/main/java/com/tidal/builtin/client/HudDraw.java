package com.tidal.builtin.client;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;

final class HudDraw {
    private HudDraw() {}

    static void scaled(GuiGraphics graphics, int x, int y, float scale, Runnable draw) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1);
        draw.run();
        pose.popPose();
    }

    static float clampScale(float scale) {
        return Math.max(0.5f, Math.min(2.5f, scale));
    }

    static int scaled(int size, float scale) {
        return Math.max(1, Math.round(size * scale));
    }
}
