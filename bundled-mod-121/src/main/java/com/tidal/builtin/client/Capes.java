package com.tidal.builtin.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Catalog lives in {@code ./capes/catalog.json} (copied into the instance as {@code tidal-capes}).
 * Files use the original Minecraft cape sheet: 64x32 PNG, cape on the left, elytra on the right.
 */
public final class Capes {
    public static final class Item {
        public final String id;
        public final String name;
        public final int price;
        public final String file;
        public final String exclusive;

        Item(String id, String name, int price, String file, String exclusive) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.file = file;
            this.exclusive = exclusive == null ? "" : exclusive;
        }

        public boolean exclusive() {
            return !this.exclusive.isBlank();
        }
    }

    private static final class Loaded {
        final Item item;
        final ResourceLocation textureId;
        final DynamicTexture texture;
        final NativeImage[] frames;
        final int[] delays;
        int frame;
        int ticks;

        Loaded(Item item, ResourceLocation textureId, DynamicTexture texture, NativeImage[] frames, int[] delays) {
            this.item = item;
            this.textureId = textureId;
            this.texture = texture;
            this.frames = frames;
            this.delays = delays;
        }
    }

    private static final List<Item> ITEMS = new ArrayList<>();
    private static final List<Loaded> LOADED = new ArrayList<>();
    private static boolean ready;
    private static String previewId;

    private Capes() {}

    public static List<Item> items() {
        if (ITEMS.isEmpty()) {
            readCatalog();
        }
        return ITEMS;
    }

    public static List<Item> visibleItems() {
        String name = localName();
        List<Item> visible = new ArrayList<>();
        for (Item item : items()) {
            if (!item.exclusive() || item.exclusive.equalsIgnoreCase(name)) {
                visible.add(item);
            }
        }
        return visible;
    }

    public static boolean exclusiveOwned(String id) {
        for (Item item : items()) {
            if (item.id.equals(id)) {
                return item.exclusive() && item.exclusive.equalsIgnoreCase(localName());
            }
        }
        return false;
    }

    public static boolean isExclusive(String id) {
        for (Item item : items()) {
            if (item.id.equals(id)) {
                return item.exclusive();
            }
        }
        return false;
    }

    public static String localName() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return "";
        }
        return minecraft.getUser().getName();
    }

    public static ResourceLocation locationById(String id) {
        if (id == null || id.isBlank() || id.startsWith("ms:")) {
            return null;
        }
        ensure(Minecraft.getInstance());
        for (Loaded loaded : LOADED) {
            if (loaded.item.id.equals(id)) {
                return loaded.textureId;
            }
        }
        return null;
    }

    public static ResourceLocation equippedLocation() {
        String id = previewId != null ? previewId : TidalMods.equippedCape;
        if (id == null || id.isBlank()) {
            return null;
        }
        if (id.startsWith("ms:")) {
            return AccountCapes.location(id.substring(3));
        }
        if (previewId == null && !TidalMods.owns(id)) {
            return null;
        }
        for (Loaded loaded : LOADED) {
            if (loaded.item.id.equals(id)) {
                return loaded.textureId;
            }
        }
        return null;
    }

    public static void setPreview(String id) {
        previewId = id;
    }

    public static void blitIcon(GuiGraphics graphics, String id, int x, int y, int w, int h) {
        ResourceLocation texture = preview(id);
        if (texture == null) {
            Glass.roundedFill(graphics, x, y, x + w, y + h, 4, 0xFF0349FC);
            return;
        }
        graphics.blit(texture, x, y, w, h, 0.0f, 0.0f, 22, 17, 64, 32);
    }

    public static ResourceLocation preview(String id) {
        for (Loaded loaded : LOADED) {
            if (loaded.item.id.equals(id)) {
                return loaded.textureId;
            }
        }
        return null;
    }

    public static void tick(Minecraft minecraft) {
        ensure(minecraft);
        for (Loaded loaded : LOADED) {
            if (loaded.frames.length < 2) {
                continue;
            }
            loaded.ticks++;
            int delay = Math.max(1, loaded.delays[loaded.frame]);
            if (loaded.ticks < delay) {
                continue;
            }
            loaded.ticks = 0;
            loaded.frame = (loaded.frame + 1) % loaded.frames.length;
            copy(loaded.frames[loaded.frame], loaded.texture.getPixels());
            loaded.texture.upload();
        }
    }

    public static void ensure(Minecraft minecraft) {
        if (ready || minecraft == null) {
            return;
        }
        readCatalog();
        Path folder = folder();
        for (Item item : ITEMS) {
            NativeImage[] frames;
            int[] delays;
            Path file = folder.resolve(item.file);
            if (Files.isRegularFile(file) && item.file.toLowerCase().endsWith(".gif")) {
                frames = gifFrames(file);
                delays = gifDelays(file, frames.length);
            } else if (Files.isRegularFile(file)) {
                frames = new NativeImage[] {readStill(file)};
                delays = new int[] {1};
            } else if ("white".equals(item.id)) {
                frames = new NativeImage[] {solidWhite()};
                delays = new int[] {1};
            } else if ("blzxy".equals(item.id)) {
                frames = new NativeImage[] {exclusiveCape()};
                delays = new int[] {1};
            } else {
                continue;
            }
            ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "textures/cape/" + item.id + ".png");
            DynamicTexture texture = new DynamicTexture(copyOf(frames[0]));
            minecraft.getTextureManager().register(textureId, texture);
            LOADED.add(new Loaded(item, textureId, texture, frames, delays));
        }
        ready = true;
    }

    private static void readCatalog() {
        ITEMS.clear();
        Path catalog = folder().resolve("catalog.json");
        if (Files.isRegularFile(catalog)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(catalog)).getAsJsonObject();
                JsonArray capes = root.getAsJsonArray("capes");
                for (JsonElement element : capes) {
                    JsonObject o = element.getAsJsonObject();
                    ITEMS.add(new Item(
                        o.get("id").getAsString(),
                        o.get("name").getAsString(),
                        o.get("price").getAsInt(),
                        o.get("file").getAsString(),
                        o.has("exclusive") ? o.get("exclusive").getAsString() : ""
                    ));
                }
            } catch (Exception ignored) {
            }
        }
        if (ITEMS.isEmpty()) {
            ITEMS.add(new Item("white", "White", 100, "white.png", ""));
        }
        boolean hasExclusive = false;
        for (Item item : ITEMS) {
            if ("blzxy".equals(item.id)) {
                hasExclusive = true;
                break;
            }
        }
        if (!hasExclusive) {
            ITEMS.add(new Item("blzxy", "BLZXY", 0, "blzxy.png", "_BLZXY"));
        }
    }

    public static Path folder() {
        return FabricLoader.getInstance().getGameDir().resolve("tidal-capes");
    }

    private static NativeImage solidWhite() {
        NativeImage image = new NativeImage(64, 32, true);
        fillCapeFaces(image, 0xFFFFFFFF);
        return image;
    }

    private static NativeImage exclusiveCape() {
        NativeImage image = new NativeImage(64, 32, true);
        int blue = 0xFF0349FC;
        int white = 0xFFFFFFFF;
        fillCapeFaces(image, blue);
        for (int y = 1; y < 17; y++) {
            for (int x = 1; x < 11; x++) {
                boolean stripe = (x + y) % 5 < 2;
                int color = stripe ? white : blue;
                image.setPixelRGBA(x, y, color);
                image.setPixelRGBA(x + 10, y, color);
            }
        }
        return image;
    }

    private static void fillCapeFaces(NativeImage image, int color) {
        for (int y = 0; y < 17; y++) {
            for (int x = 0; x < 22; x++) {
                image.setPixelRGBA(x, y, color);
            }
        }
    }

    private static NativeImage readStill(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            return toOgCape(NativeImage.read(stream));
        } catch (Exception ignored) {
            return solidWhite();
        }
    }

    private static NativeImage[] gifFrames(Path file) {
        List<NativeImage> frames = new ArrayList<>();
        try (ImageInputStream stream = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return new NativeImage[] {solidWhite()};
            }
            ImageReader reader = readers.next();
            reader.setInput(stream);
            int count = reader.getNumImages(true);
            for (int i = 0; i < count; i++) {
                frames.add(fromAwt(reader.read(i)));
            }
            reader.dispose();
        } catch (Exception ignored) {
            return new NativeImage[] {solidWhite()};
        }
        return frames.isEmpty() ? new NativeImage[] {solidWhite()} : frames.toArray(NativeImage[]::new);
    }

    private static int[] gifDelays(Path file, int count) {
        int[] delays = new int[count];
        java.util.Arrays.fill(delays, 2);
        try (ImageInputStream stream = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return delays;
            }
            ImageReader reader = readers.next();
            reader.setInput(stream);
            for (int i = 0; i < count; i++) {
                IIOMetadata meta = reader.getImageMetadata(i);
                String[] names = meta.getMetadataFormatNames();
                for (String name : names) {
                    org.w3c.dom.Node tree = meta.getAsTree(name);
                    delays[i] = Math.max(1, delayFromNode(tree, delays[i]));
                }
            }
            reader.dispose();
        } catch (Exception ignored) {
        }
        return delays;
    }

    private static int delayFromNode(org.w3c.dom.Node node, int fallback) {
        if (node == null) {
            return fallback;
        }
        if ("GraphicControlExtension".equals(node.getNodeName()) && node.hasAttributes()) {
            org.w3c.dom.Node delay = node.getAttributes().getNamedItem("delayTime");
            if (delay != null) {
                try {
                    return Math.max(1, Integer.parseInt(delay.getNodeValue()) / 2);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        org.w3c.dom.NodeList kids = node.getChildNodes();
        int value = fallback;
        for (int i = 0; i < kids.getLength(); i++) {
            value = delayFromNode(kids.item(i), value);
        }
        return value;
    }

    private static NativeImage fromAwt(BufferedImage source) {
        BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        argb.getGraphics().drawImage(source, 0, 0, null);
        NativeImage image = new NativeImage(argb.getWidth(), argb.getHeight(), true);
        for (int y = 0; y < argb.getHeight(); y++) {
            for (int x = 0; x < argb.getWidth(); x++) {
                image.setPixelRGBA(x, y, argb.getRGB(x, y));
            }
        }
        return toOgCape(image);
    }

    private static NativeImage toOgCape(NativeImage source) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        if (srcW == 64 && srcH == 32) {
            return source;
        }
        NativeImage out = new NativeImage(64, 32, true);
        if (srcW == 64 && srcH >= 32) {
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    out.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
                }
            }
            source.close();
            return out;
        }
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                int sx = Math.min(srcW - 1, x * srcW / 64);
                int sy = Math.min(srcH - 1, y * srcH / 32);
                out.setPixelRGBA(x, y, source.getPixelRGBA(sx, sy));
            }
        }
        source.close();
        return out;
    }

    private static NativeImage copyOf(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        copy(source, copy);
        return copy;
    }

    private static void copy(NativeImage from, NativeImage to) {
        if (to == null) {
            return;
        }
        int w = Math.min(from.getWidth(), to.getWidth());
        int h = Math.min(from.getHeight(), to.getHeight());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                to.setPixelRGBA(x, y, from.getPixelRGBA(x, y));
            }
        }
    }
}
