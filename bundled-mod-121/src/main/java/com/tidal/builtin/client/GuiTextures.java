package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

final class GuiTextures {
    static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/logo");
    static final ResourceLocation SETTINGS = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_settings");
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

    private static void register(Minecraft minecraft, ResourceLocation id, String classpath, boolean gearOnly) {
        try (InputStream stream = GuiTextures.class.getResourceAsStream(classpath)) {
            if (stream == null) {
                return;
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                return;
            }
            if (gearOnly) {
                image = extractGear(image);
                settingsSize = image.getWidth();
            }
            minecraft.getTextureManager().register(id, NativeImages.texture(id.getPath(), NativeImages.fromBuffered(image)));
        } catch (Exception ignored) {
        }
    }

    private static BufferedImage extractGear(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int x0 = w;
        int y0 = h;
        int x1 = 0;
        int y1 = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = source.getRGB(x, y);
                if (!isGearPixel(pixel)) {
                    source.setRGB(x, y, 0);
                    continue;
                }
                x0 = Math.min(x0, x);
                y0 = Math.min(y0, y);
                x1 = Math.max(x1, x);
                y1 = Math.max(y1, y);
            }
        }
        if (x1 < x0) {
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
        int side = Math.max(x1 - x0 + 1, y1 - y0 + 1);
        BufferedImage gear = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        int ox = (side - (x1 - x0 + 1)) / 2;
        int oy = (side - (y1 - y0 + 1)) / 2;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                gear.setRGB(ox + (x - x0), oy + (y - y0), source.getRGB(x, y));
            }
        }
        return NativeImages.scaleNearest(gear, 16, 16);
    }

    private static boolean isGearPixel(int pixel) {
        int a = pixel >>> 24;
        int r = pixel >> 16 & 0xFF;
        int g = pixel >> 8 & 0xFF;
        int b = pixel & 0xFF;
        if (a < 40) {
            return false;
        }
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        return min > 150 && max - min < 50;
    }
}
