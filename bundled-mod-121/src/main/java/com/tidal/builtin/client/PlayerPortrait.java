package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

final class PlayerPortrait {
    void drawHead(GuiGraphics graphics, Minecraft minecraft, int cx, int cy, int size, int mouseX, int mouseY) {
        Glass.fillCircle(graphics, cx, cy, Math.max(8, size / 2 - 2), 0xF0101218);
        ResourceLocation skin = texture(minecraft);
        if (skin == null) {
            return;
        }
        int side = Math.max(16, size / 2);
        HudBlit.sprite(graphics, skin, cx - side / 2, cy - side / 2, side, side, 8.0f, 8.0f, 8, 8, 64, 64);
        HudBlit.sprite(graphics, skin, cx - side / 2, cy - side / 2, side, side, 40.0f, 8.0f, 8, 8, 64, 64);
    }

    void drawBody(GuiGraphics graphics, Minecraft minecraft, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        ResourceLocation skin = texture(minecraft);
        if (skin == null) {
            Glass.roundedFill(graphics, x0, y0, x1, y1, 8, 0x33000000);
            return;
        }
        int w = Math.max(16, x1 - x0);
        int h = Math.max(32, y1 - y0);
        int head = Math.min(w, h / 2);
        int cx = (x0 + x1) / 2;
        HudBlit.sprite(graphics, skin, cx - head / 2, y0 + 4, head, head, 8.0f, 8.0f, 8, 8, 64, 64);
        HudBlit.sprite(graphics, skin, cx - head / 2, y0 + 4 + head, head, head, 20.0f, 20.0f, 8, 8, 64, 64);
    }

    private static ResourceLocation texture(Minecraft minecraft) {
        ResourceLocation local = LocalSkins.bodyLocation();
        if (local != null) {
            return local;
        }
        if (minecraft.player instanceof AbstractClientPlayer player) {
            PlayerSkin skin = player.getSkin();
            return skin.texture();
        }
        return null;
    }
}
