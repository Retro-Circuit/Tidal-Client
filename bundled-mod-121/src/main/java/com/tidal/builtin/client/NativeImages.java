package com.tidal.builtin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;

final class NativeImages {
    private NativeImages() {}

    static NativeImage copyOf(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        copy.copyFrom(source);
        return copy;
    }

    static void copy(NativeImage from, NativeImage to) {
        if (to == null) {
            return;
        }
        to.copyFrom(from);
    }

    static NativeImage fromArgb(int width, int height, int[] pixels) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return fromBuffered(image);
    }

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
        try {
            Constructor<DynamicTexture> ctor = DynamicTexture.class.getConstructor(NativeImage.class);
            return ctor.newInstance(image);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<DynamicTexture> ctor = DynamicTexture.class.getConstructor(Supplier.class, NativeImage.class);
            return ctor.newInstance((Supplier<String>) () -> name, image);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("DynamicTexture is unavailable", error);
        }
    }
}
