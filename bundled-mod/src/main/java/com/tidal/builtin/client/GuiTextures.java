package com.tidal.builtin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.io.InputStream;

final class GuiTextures {
    static final Identifier LOGO = Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/logo");
    static final Identifier SETTINGS = Identifier.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_settings");
    static int settingsSize = 16;

    private static boolean ready;

    private GuiTextures() {}

    static void ensure(Minecraft minecraft) {
        if (ready || minecraft == null) {
            return;
        }
        register(minecraft, LOGO, "/assets/tidal-builtin/textures/gui/logo.png", false);
        register(minecraft, SETTINGS, "/assets/tidal-builtin/textures/gui/radial/settings.png", true);
        ready = true;
    }

    private static void register(Minecraft minecraft, Identifier id, String classpath, boolean gearOnly) {
        try (InputStream stream = GuiTextures.class.getResourceAsStream(classpath)) {
            if (stream == null) {
                return;
            }
            NativeImage image = NativeImage.read(stream);
            if (gearOnly) {
                image = extractGear(image);
                settingsSize = image.getWidth();
            }
            DynamicTexture texture = new DynamicTexture(() -> id.getPath(), image);
            minecraft.getTextureManager().register(id, texture);
        } catch (Exception ignored) {
        }
    }

    private static NativeImage extractGear(NativeImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int x0 = w;
        int y0 = h;
        int x1 = 0;
        int y1 = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!isGearPixel(source.getPixel(x, y))) {
                    source.setPixel(x, y, 0);
                    continue;
                }
                x0 = Math.min(x0, x);
                y0 = Math.min(y0, y);
                x1 = Math.max(x1, x);
                y1 = Math.max(y1, y);
            }
        }
        if (x1 < x0) {
            source.close();
            NativeImage empty = new NativeImage(16, 16, true);
            return empty;
        }
        int side = Math.max(x1 - x0 + 1, y1 - y0 + 1);
        NativeImage gear = new NativeImage(side, side, true);
        int ox = (side - (x1 - x0 + 1)) / 2;
        int oy = (side - (y1 - y0 + 1)) / 2;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                gear.setPixel(ox + (x - x0), oy + (y - y0), source.getPixel(x, y));
            }
        }
        source.close();
        return nearest(gear, 16);
    }

    private static boolean isGearPixel(int pixel) {
        int a = pixel >>> 24;
        int r = pixel & 0xFF;
        int g = pixel >> 8 & 0xFF;
        int b = pixel >> 16 & 0xFF;
        if (a < 40) {
            return false;
        }
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        return min > 150 && max - min < 50;
    }

    private static NativeImage nearest(NativeImage source, int size) {
        if (source.getWidth() == size && source.getHeight() == size) {
            return source;
        }
        NativeImage out = new NativeImage(size, size, true);
        int sw = source.getWidth();
        int sh = source.getHeight();
        for (int y = 0; y < size; y++) {
            int sy = Math.min(sh - 1, y * sh / size);
            for (int x = 0; x < size; x++) {
                int sx = Math.min(sw - 1, x * sw / size);
                out.setPixel(x, y, source.getPixel(sx, sy));
            }
        }
        source.close();
        return out;
    }
}
