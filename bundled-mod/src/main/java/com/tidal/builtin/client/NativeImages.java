package com.tidal.builtin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

final class NativeImages {
    private NativeImages() {}

    static NativeImage fromBuffered(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
        } catch (Exception ignored) {
            return new NativeImage(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()), true);
        }
    }

    static BufferedImage scaleNearest(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    static DynamicTexture texture(String name, NativeImage image) {
        return new DynamicTexture(() -> name, image);
    }
}
