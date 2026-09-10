package com.tidal.builtin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalSkins {
    public record Entry(String key, String name, Path path, boolean slim) {}

    private static final class Loaded {
        final Entry entry;
        final Identifier identifier;
        final ClientAsset.Texture asset;

        Loaded(Entry entry, Identifier identifier, ClientAsset.Texture asset) {
            this.entry = entry;
            this.identifier = identifier;
            this.asset = asset;
        }
    }

    private static final Set<String> SKIP = Set.of(
        "node_modules", ".git", ".gradle", "libraries", "versions", "natives", "assets",
        "logs", "crash-reports", "cache", "caches", "tmp", "temp", "bin", "build",
        "saves", "worlds", "screenshots", "resourcepacks", "texturepacks", "datapacks",
        "server-resource-packs", "playerdata", "stats", "advancements", "icons"
    );

    private static volatile List<Entry> found = List.of();
    private static final Map<String, Loaded> textures = new LinkedHashMap<>();
    private static final AtomicBoolean scanning = new AtomicBoolean();
    private static volatile String status = "Scan your PC for 64×64 skins";
    private static String previewKey;

    private LocalSkins() {}

    public static List<Entry> list() {
        return found;
    }

    public static boolean scanning() {
        return scanning.get();
    }

    public static String status() {
        return status;
    }

    public static void setPreview(String key) {
        previewKey = key;
    }

    public static void scanLater() {
        if (!scanning.compareAndSet(false, true)) {
            return;
        }
        status = "Scanning…";
        Thread.ofVirtual().name("tidal-skin-scan").start(() -> {
            try {
                found = discover();
                status = found.isEmpty() ? "No skins found" : found.size() + " skins found";
            } catch (Exception ignored) {
                status = "Scan failed";
            } finally {
                scanning.set(false);
            }
        });
    }

    public static void ensure(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        for (Entry entry : found) {
            if (textures.containsKey(entry.key)) {
                continue;
            }
            try (InputStream stream = Files.newInputStream(entry.path)) {
                NativeImage image = NativeImage.read(stream);
                if (image.getWidth() != 64 || (image.getHeight() != 32 && image.getHeight() != 64)) {
                    image.close();
                    continue;
                }
                Identifier identifier = Identifier.fromNamespaceAndPath(
                    "tidal-builtin",
                    "textures/skin/" + Integer.toHexString(entry.key.hashCode()) + ".png"
                );
                DynamicTexture texture = new DynamicTexture(() -> "tidal-skin-" + entry.key.hashCode(), image);
                minecraft.getTextureManager().register(identifier, texture);
                textures.put(entry.key, new Loaded(entry, identifier, new ClientAsset.ResourceTexture(identifier, identifier)));
            } catch (Exception ignored) {
            }
        }
    }

    public static ClientAsset.Texture bodyAsset() {
        String key = previewKey != null ? previewKey : TidalMods.equippedSkin;
        if (key == null || key.isBlank()) {
            return null;
        }
        Loaded loaded = textures.get(key);
        return loaded == null ? null : loaded.asset;
    }

    public static Path equippedFile() {
        String key = TidalMods.equippedSkin;
        if (key == null || key.isBlank()) {
            return null;
        }
        Path path = Path.of(key);
        return Files.isRegularFile(path) ? path : null;
    }

    public static boolean equippedSlim() {
        String key = TidalMods.equippedSkin;
        if (key == null || key.isBlank()) {
            return false;
        }
        Loaded loaded = textures.get(key);
        if (loaded != null) {
            return loaded.entry.slim;
        }
        Path file = equippedFile();
        if (file == null) {
            return false;
        }
        try (InputStream stream = Files.newInputStream(file)) {
            BufferedImage image = ImageIO.read(stream);
            return image != null && image.getHeight() == 64 && ((image.getRGB(50, 16) >>> 24) & 0xFF) < 16;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static PlayerModelType modelOr(PlayerModelType fallback) {
        String key = previewKey != null ? previewKey : TidalMods.equippedSkin;
        if (key == null || key.isBlank()) {
            return fallback;
        }
        Loaded loaded = textures.get(key);
        if (loaded == null) {
            return fallback;
        }
        return loaded.entry.slim ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }

    public static void blit(GuiGraphicsExtractor graphics, String key, int x, int y, int size) {
        Loaded loaded = textures.get(key);
        if (loaded == null) {
            Glass.roundedFill(graphics, x, y, x + size, y + size, 4, 0x330349FC);
            return;
        }
        graphics.blit(loaded.identifier, x, y, x + size, y + size, 8.0f / 64.0f, 16.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f);
    }

    private static List<Entry> discover() {
        List<Entry> list = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        int[] visits = {0};
        for (Path root : roots()) {
            walk(root, 0, depthFor(root), list, seen, visits);
            if (list.size() >= 120) {
                break;
            }
        }
        list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return list;
    }

    private static int depthFor(Path root) {
        String name = root.getFileName() == null ? "" : root.getFileName().toString().toLowerCase(Locale.ROOT);
        if ("desktop".equals(name) || "pictures".equals(name) || "downloads".equals(name) || "skins".equals(name)) {
            return 2;
        }
        return 4;
    }

    private static void walk(Path dir, int depth, int maxDepth, List<Entry> out, Set<Path> seen, int[] visits) {
        if (depth > maxDepth || out.size() >= 120 || visits[0] > 8000 || dir == null || !Files.isDirectory(dir)) {
            return;
        }
        String folder = dir.getFileName() == null ? "" : dir.getFileName().toString().toLowerCase(Locale.ROOT);
        if (SKIP.contains(folder) || "tidal-capes".equals(folder)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                visits[0]++;
                if (out.size() >= 120 || visits[0] > 8000) {
                    return;
                }
                if (Files.isDirectory(path)) {
                    walk(path, depth + 1, maxDepth, out, seen, visits);
                    continue;
                }
                String file = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!file.endsWith(".png") || file.contains("cape") || file.equals("icon.png") || file.equals("pack.png") || file.contains("server-icon")) {
                    continue;
                }
                Path real = path.toAbsolutePath().normalize();
                if (!seen.add(real)) {
                    continue;
                }
                Entry entry = inspect(real);
                if (entry != null) {
                    out.add(entry);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static Entry inspect(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                return null;
            }
            String pathText = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (pathText.contains("/saves/") || pathText.contains("/worlds/") || pathText.contains("/screenshots/")) {
                return null;
            }
            int w = image.getWidth();
            int h = image.getHeight();
            if (w != 64 || (h != 32 && h != 64)) {
                return null;
            }
            if (!headFilled(image)) {
                return null;
            }
            if (!looksLikeSkin(image, pathText)) {
                return null;
            }
            boolean slim = h == 64 && ((image.getRGB(50, 16) >>> 24) & 0xFF) < 16;
            String raw = path.getFileName().toString();
            int dot = raw.lastIndexOf('.');
            String name = (dot > 0 ? raw.substring(0, dot) : raw).replace('_', ' ');
            return new Entry(path.toString(), name, path, slim);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean headFilled(BufferedImage image) {
        int opaque = 0;
        int h = image.getHeight();
        for (int y = 8; y < 16 && y < h; y++) {
            for (int x = 8; x < 16; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 16) {
                    opaque++;
                }
            }
        }
        return opaque >= 20;
    }

    private static boolean looksLikeSkin(BufferedImage image, String pathText) {
        if (pathText.contains("/skins/") || pathText.contains("\\skins\\") || pathText.contains("skin")) {
            return true;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int transparent = 0;
        int torso = 0;
        for (int y = 20; y < 32 && y < h; y++) {
            for (int x = 20; x < 28 && x < w; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 16) {
                    torso++;
                }
            }
        }
        if (torso < 20) {
            return false;
        }
        if (h == 32) {
            return true;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) < 8) {
                    transparent++;
                }
            }
        }
        return transparent >= 180;
    }

    private static List<Path> roots() {
        List<Path> roots = new ArrayList<>();
        Path home = Path.of(System.getProperty("user.home", "."));
        String appData = System.getenv("APPDATA");
        String local = System.getenv("LOCALAPPDATA");
        add(roots, FabricLoader.getInstance().getGameDir().resolve("skins"));
        if (appData != null) {
            Path roaming = Path.of(appData);
            add(roots, roaming.resolve(".minecraft"));
            add(roots, roaming.resolve(".minecraft").resolve("skins"));
            add(roots, roaming.resolve("tidal-client"));
            add(roots, roaming.resolve("ModrinthApp"));
            add(roots, roaming.resolve("PrismLauncher"));
            add(roots, roaming.resolve("PolyMC"));
            add(roots, roaming.resolve("MultiMC"));
            add(roots, roaming.resolve("curseforge"));
            add(roots, roaming.resolve(".feather"));
            add(roots, roaming.resolve("Badlion Client"));
            add(roots, roaming.resolve(".tlauncher"));
        }
        if (local != null) {
            add(roots, Path.of(local).resolve("ModrinthApp"));
        }
        add(roots, home.resolve(".minecraft"));
        add(roots, home.resolve(".lunarclient"));
        add(roots, home.resolve("Desktop"));
        add(roots, home.resolve("Pictures"));
        add(roots, home.resolve("Downloads"));
        add(roots, home.resolve("Documents").resolve("skins"));
        return roots;
    }

    private static void add(List<Path> roots, Path path) {
        if (path != null && Files.isDirectory(path)) {
            roots.add(path);
        }
    }
}
