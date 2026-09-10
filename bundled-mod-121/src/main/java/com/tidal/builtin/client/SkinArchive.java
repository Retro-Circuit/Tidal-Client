package com.tidal.builtin.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SkinArchive {
    private static final ResourceLocation UNKNOWN_ID = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "textures/skin/unknown.png");
    private static final ResourceLocation SAVED_ID = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "textures/skin/saved.png");
    private static boolean unknownReady;
    private static long savedStamp = -1;
    private static boolean savedReady;
    private static boolean savedSlim;
    private static String capturedKey = "";
    private static int captureTries;

    private SkinArchive() {}

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        ensure(minecraft);
        if (minecraft.player != null && minecraft.level != null) {
            capture(minecraft);
        }
    }

    public static boolean menuContext(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return true;
        }
        return minecraft.screen instanceof TitleScreen;
    }

    public static PlayerSkin forMenu(PlayerSkin skin, UUID profileId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (skin == null || minecraft == null || !menuContext(minecraft) || !localUuid(minecraft, profileId)) {
            return skin;
        }
        ensure(minecraft);
        if (!savedReady) {
            return skin;
        }
        ResourceLocation body = bodyLocation();
        if (body == null) {
            return skin;
        }
        Object model = savedSlim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
        PlayerSkin next = SkinCompat.replace(skin, body, null, model);
        return next != null ? next : skin;
    }

    public static ResourceLocation bodyLocation() {
        return savedReady ? SAVED_ID : null;
    }

    public static void ensure(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        Path file = existingPng(localId(minecraft));
        if (file == null) {
            savedReady = false;
            savedStamp = -1;
            return;
        }
        try {
            long stamp = Files.getLastModifiedTime(file).toMillis();
            if (savedReady && stamp == savedStamp) {
                return;
            }
            NativeImage image;
            try (InputStream stream = Files.newInputStream(file)) {
                image = NativeImage.read(stream);
            }
            register(minecraft, SAVED_ID, "tidal-saved", image);
            savedStamp = stamp;
            savedReady = true;
            savedSlim = readSlim(file);
        } catch (Exception ignored) {
        }
    }

    private static void capture(Minecraft minecraft) {
        String uuid = localId(minecraft);
        if (uuid.isBlank()) {
            return;
        }
        Path equipped = LocalSkins.equippedFile();
        if (equipped == null) {
            return;
        }
        String key = equipped.toString();
        if (key.equals(capturedKey) && existingPng(uuid) != null) {
            return;
        }
        if (store(uuid, equipped, LocalSkins.equippedSlim(), "tidal")) {
            capturedKey = key;
            savedReady = false;
        }
    }

    private static boolean storeCurrentBody(Minecraft minecraft, String uuid) {
        try {
            ResourceLocation id = SkinCompat.bodyId(minecraft.player.getSkin());
            if (id == null) {
                return false;
            }
            String path = id.toString().toLowerCase(Locale.ROOT);
            if (path.contains("entity/player") || path.contains("alex") && path.contains("minecraft:")) {
                return false;
            }
            var texture = minecraft.getTextureManager().getTexture(id);
            NativeImage pixels = pixelsOf(texture);
            if (pixels == null || pixels.getWidth() != 64) {
                return false;
            }
            Path temp = Files.createTempFile("tidal-skin", ".png");
            pixels.writeToFile(temp);
            boolean slim = false;
            try (InputStream stream = Files.newInputStream(temp)) {
                BufferedImage image = ImageIO.read(stream);
                slim = image != null && image.getHeight() == 64 && ((image.getRGB(50, 16) >>> 24) & 0xFF) < 16;
            }
            boolean ok = store(uuid, temp, slim, "account");
            Files.deleteIfExists(temp);
            return ok;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static NativeImage pixelsOf(Object texture) {
        if (texture == null) {
            return null;
        }
        for (String name : new String[] {"getPixels", "getImage"}) {
            try {
                Object value = texture.getClass().getMethod(name).invoke(texture);
                if (value instanceof NativeImage image) {
                    return image;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean storeMojang(String uuid) {
        try {
            var connection = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "TidalClient");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            String json = new String(connection.getInputStream().readAllBytes());
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String encoded = "";
            for (var property : root.getAsJsonArray("properties")) {
                JsonObject item = property.getAsJsonObject();
                if ("textures".equals(item.get("name").getAsString())) {
                    encoded = item.get("value").getAsString();
                    break;
                }
            }
            if (encoded.isBlank()) {
                return false;
            }
            JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(encoded))).getAsJsonObject().getAsJsonObject("textures");
            if (!textures.has("SKIN")) {
                return false;
            }
            JsonObject skin = textures.getAsJsonObject("SKIN");
            String url = skin.get("url").getAsString();
            boolean slim = skin.has("metadata") && "slim".equalsIgnoreCase(skin.getAsJsonObject("metadata").get("model").getAsString());
            var download = URI.create(url).toURL().openConnection();
            download.setRequestProperty("User-Agent", "TidalClient");
            Path temp = Files.createTempFile("tidal-skin", ".png");
            try (InputStream stream = download.getInputStream()) {
                Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            boolean ok = store(uuid, temp, slim, "account");
            Files.deleteIfExists(temp);
            return ok;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean store(String uuid, Path source, boolean slim, String origin) {
        try (InputStream stream = Files.newInputStream(source)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null || image.getWidth() != 64 || (image.getHeight() != 32 && image.getHeight() != 64)) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
        String meta = "{\"slim\":" + slim + ",\"source\":\"" + origin + "\"}\n";
        boolean wrote = false;
        for (Path dir : skinDirs(uuid)) {
            try {
                Files.createDirectories(dir);
                Files.copy(source, dir.resolve("equipped.png"), StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(dir.resolve("meta.json"), meta);
                wrote = true;
            } catch (Exception ignored) {
            }
        }
        return wrote;
    }

    private static boolean readSlim(Path png) {
        Path meta = png.getParent() == null ? null : png.getParent().resolve("meta.json");
        if (meta != null && Files.isRegularFile(meta)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(meta)).getAsJsonObject();
                if (json.has("slim")) {
                    return json.get("slim").getAsBoolean();
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static Path existingPng(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        for (Path dir : skinDirs(uuid)) {
            Path file = dir.resolve("equipped.png");
            if (Files.isRegularFile(file) && tidalMeta(dir)) {
                return file;
            }
        }
        return null;
    }

    private static boolean tidalMeta(Path dir) {
        Path meta = dir.resolve("meta.json");
        if (!Files.isRegularFile(meta)) {
            return false;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(meta)).getAsJsonObject();
            return json.has("source") && "tidal".equals(json.get("source").getAsString());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<Path> skinDirs(String uuid) {
        List<Path> dirs = new ArrayList<>();
        for (Path root : appRoots()) {
            dirs.add(root.resolve("skins").resolve(uuid));
        }
        return dirs;
    }

    private static List<Path> appRoots() {
        List<Path> roots = new ArrayList<>();
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            roots.add(Path.of(appData, "tidal-client"));
            roots.add(Path.of(appData, "Tidal Client"));
        }
        String home = System.getProperty("user.home", ".");
        String xdg = System.getenv("XDG_CONFIG_HOME");
        roots.add(Path.of(xdg != null && !xdg.isBlank() ? xdg : home + "/.config", "tidal-client"));
        roots.add(Path.of(home, "Library", "Application Support", "tidal-client"));
        roots.add(Path.of(home, "Library", "Application Support", "Tidal Client"));
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path parent = gameDir.getParent();
        if (parent != null && parent.getParent() != null) {
            roots.add(parent.getParent());
        }
        return roots;
    }

    private static void register(Minecraft minecraft, ResourceLocation id, String name, NativeImage image) {
        DynamicTexture texture = NativeImages.texture(name, image);
        minecraft.getTextureManager().register(id, texture);
    }

    private static void register(Minecraft minecraft, ResourceLocation id, String name, BufferedImage image) {
        register(minecraft, id, name, NativeImages.fromBuffered(image));
    }

    static boolean localUuid(Minecraft minecraft, UUID profileId) {
        if (profileId == null) {
            return false;
        }
        UUID local = uuidOf(minecraft);
        return local != null && local.equals(profileId);
    }

    static UUID uuidOf(Minecraft minecraft) {
        try {
            Object user = minecraft.getUser();
            for (String name : new String[] {"getProfileId", "getUuid", "uuid"}) {
                try {
                    Object value = user.getClass().getMethod(name).invoke(user);
                    if (value instanceof UUID id) {
                        return id;
                    }
                } catch (Exception ignored) {
                }
            }
            try {
                Object profile = user.getClass().getMethod("getGameProfile").invoke(user);
                if (profile instanceof GameProfile game) {
                    return game.getId();
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    static UUID uuidOf(GameProfile profile) {
        return profile == null ? null : profile.getId();
    }

    private static String localId(Minecraft minecraft) {
        UUID id = uuidOf(minecraft);
        return id == null ? "" : id.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    static NativeImage unknownImage() {
        return NativeImages.fromBuffered(unknownBuffered());
    }

    static BufferedImage unknownBuffered() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int ink = 0xFF121418;
        int panel = 0xFF1C2026;
        int raised = 0xFF252A32;
        int tidal = 0xFF0349FC;
        fill(image, 8, 8, 8, 8, raised);
        fill(image, 40, 8, 8, 8, panel);
        fill(image, 20, 20, 8, 12, panel);
        fill(image, 20, 36, 8, 12, ink);
        fill(image, 44, 20, 4, 12, panel);
        fill(image, 44, 36, 4, 12, ink);
        fill(image, 36, 52, 4, 12, panel);
        fill(image, 52, 52, 4, 12, ink);
        fill(image, 4, 20, 4, 12, panel);
        fill(image, 4, 36, 4, 12, ink);
        fill(image, 20, 52, 4, 12, panel);
        fill(image, 4, 52, 4, 12, ink);
        int[][] mark = {
            {0, 0, 1, 1, 1, 0, 0, 0},
            {0, 1, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0}
        };
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (mark[y][x] == 1) {
                    image.setRGB(8 + x, 8 + y, tidal);
                    image.setRGB(40 + x, 8 + y, tidal);
                }
            }
        }
        return image;
    }

    private static void fill(BufferedImage image, int x, int y, int w, int h, int argb) {
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                image.setRGB(px, py, argb);
            }
        }
    }

}
