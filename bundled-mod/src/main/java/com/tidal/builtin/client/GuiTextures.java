package com.tidal.builtin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.io.InputStream;

final class GuiTextures {
    static final Identifier LOGO = Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/logo");
    static final Identifier[] RADIAL = {
        Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_settings"),
        Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_skins"),
        Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_mods"),
        Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_layout")
    };
    private static final String[] RADIAL_FILES = {
        "/assets/tidal-builtin/textures/gui/radial/settings.png",
        "/assets/tidal-builtin/textures/gui/radial/skins.png",
        "/assets/tidal-builtin/textures/gui/radial/mods.png",
        "/assets/tidal-builtin/textures/gui/radial/layout.png"
    };
    static final int[] radialW = {16, 16, 16, 16};
    static final int[] radialH = {16, 16, 16, 16};

    private static boolean ready;

    private GuiTextures() {}

    static void ensure(Minecraft minecraft) {
        if (ready || minecraft == null) {
            return;
        }
        register(minecraft, LOGO, "/assets/tidal-builtin/textures/gui/logo.png", false);
        for (int i = 0; i < RADIAL.length; i++) {
            register(minecraft, RADIAL[i], RADIAL_FILES[i], true);
        }
        ready = true;
    }

    static void blitRadial(GuiGraphicsExtractor graphics, int id, int cx, int cy, int box, boolean hot) {
        if (id < 0 || id >= RADIAL.length) {
            return;
        }
        int tw = Math.max(1, radialW[id]);
        int th = Math.max(1, radialH[id]);
        float scale = (hot ? 1.12f : 1.0f) * box / (float) Math.max(tw, th);
        int w = Math.max(12, Math.round(tw * scale));
        int h = Math.max(12, Math.round(th * scale));
        graphics.blit(RADIAL[id], cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    private static void register(Minecraft minecraft, Identifier id, String classpath, boolean knockBlack) {
        try (InputStream stream = GuiTextures.class.getResourceAsStream(classpath)) {
            if (stream == null) {
                return;
            }
            NativeImage image = NativeImage.read(stream);
            if (knockBlack) {
                knockBlack(image);
                for (int i = 0; i < RADIAL.length; i++) {
                    if (RADIAL[i].equals(id)) {
                        radialW[i] = image.getWidth();
                        radialH[i] = image.getHeight();
                    }
                }
            }
            DynamicTexture texture = new DynamicTexture(() -> id.getPath(), image);
            minecraft.getTextureManager().register(id, texture);
        } catch (Exception ignored) {
        }
    }

    private static void knockBlack(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = image.getPixel(x, y);
                int a = pixel >>> 24;
                int r = pixel & 0xFF;
                int g = pixel >> 8 & 0xFF;
                int b = pixel >> 16 & 0xFF;
                if (a < 16 || r + g + b < 48) {
                    image.setPixel(x, y, 0);
                }
            }
        }
    }
}
