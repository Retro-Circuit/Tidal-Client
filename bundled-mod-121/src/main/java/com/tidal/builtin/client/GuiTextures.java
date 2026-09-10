package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

final class GuiTextures {
    static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/logo");
    static final ResourceLocation[] RADIAL = {
        ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_settings"),
        ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_skins"),
        ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_mods"),
        ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/radial_layout")
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

    static void blitRadial(GuiGraphics graphics, int id, int cx, int cy, int box, boolean hot) {
        if (id < 0 || id >= RADIAL.length) {
            return;
        }
        int tw = Math.max(1, radialW[id]);
        int th = Math.max(1, radialH[id]);
        float scale = (hot ? 1.12f : 1.0f) * box / (float) Math.max(tw, th);
        int w = Math.max(12, Math.round(tw * scale));
        int h = Math.max(12, Math.round(th * scale));
        HudBlit.uv(graphics, RADIAL[id], cx - w / 2, cy - h / 2, w, h, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    private static void register(Minecraft minecraft, ResourceLocation id, String classpath, boolean knockBlack) {
        try (InputStream stream = GuiTextures.class.getResourceAsStream(classpath)) {
            if (stream == null) {
                return;
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                return;
            }
            if (knockBlack) {
                image = knockBlack(image);
                for (int i = 0; i < RADIAL.length; i++) {
                    if (RADIAL[i].equals(id)) {
                        radialW[i] = image.getWidth();
                        radialH[i] = image.getHeight();
                    }
                }
            }
            minecraft.getTextureManager().register(id, NativeImages.texture(id.getPath(), NativeImages.fromBuffered(image)));
        } catch (Exception ignored) {
        }
    }

    private static BufferedImage knockBlack(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = source.getRGB(x, y);
                int a = pixel >>> 24;
                int r = pixel >> 16 & 0xFF;
                int g = pixel >> 8 & 0xFF;
                int b = pixel & 0xFF;
                if (a < 16 || r + g + b < 48) {
                    out.setRGB(x, y, 0);
                } else {
                    out.setRGB(x, y, pixel);
                }
            }
        }
        return out;
    }
}
