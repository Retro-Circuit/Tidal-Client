package com.tidal.builtin.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Capes owned on the Microsoft / Minecraft profile. Equipping one activates it
 * through Mojang, so every client — Tidal or vanilla — can see it.
 */
public final class AccountCapes {
    public record Cape(String id, String name, String url, boolean active) {
        public String key() {
            return "ms:" + this.id;
        }
    }

    private static final class Loaded {
        final Cape cape;
        final Identifier identifier;
        final ClientAsset.Texture asset;

        Loaded(Cape cape, Identifier identifier, ClientAsset.Texture asset) {
            this.cape = cape;
            this.identifier = identifier;
            this.asset = asset;
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final List<Cape> OWNED = new ArrayList<>();
    private static final Map<String, Loaded> LOADED = new ConcurrentHashMap<>();
    private static final Map<UUID, ClientAsset.Texture> OTHERS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> PENDING = new ConcurrentHashMap<>();
    private static volatile boolean fetching;
    private static volatile String status = "Loading Microsoft capes…";
    private static long lastOtherScan;

    private AccountCapes() {}

    public static List<Cape> owned() {
        return List.copyOf(OWNED);
    }

    public static String status() {
        return status;
    }

    public static boolean fetching() {
        return fetching;
    }

    public static void refresh() {
        if (fetching) {
            return;
        }
        fetching = true;
        status = "Loading Microsoft capes…";
        Thread.ofVirtual().name("tidal-ms-capes").start(() -> {
            try {
                String token = token();
                if (token == null || token.isBlank()) {
                    status = "Sign in with Microsoft to load account capes";
                    return;
                }
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "TidalClient/1.2")
                    .GET()
                    .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    status = "Could not load account capes";
                    return;
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                List<Cape> capes = new ArrayList<>();
                JsonArray array = root.has("capes") ? root.getAsJsonArray("capes") : new JsonArray();
                String activeId = "";
                for (JsonElement element : array) {
                    JsonObject o = element.getAsJsonObject();
                    String id = text(o, "id");
                    String name = text(o, "alias");
                    if (name.isBlank()) {
                        name = "Cape";
                    }
                    String url = text(o, "url");
                    boolean active = o.has("active") && o.get("active").getAsBoolean();
                    capes.add(new Cape(id, name.replace('_', ' '), url, active));
                    if (active) {
                        activeId = id;
                    }
                    download(id, url);
                }
                synchronized (OWNED) {
                    OWNED.clear();
                    OWNED.addAll(capes);
                }
                if (!activeId.isBlank() && (TidalMods.equippedCape.isBlank() || TidalMods.equippedCape.startsWith("ms:"))) {
                    TidalMods.equippedCape = "ms:" + activeId;
                }
                status = capes.isEmpty() ? "No Microsoft capes on this account" : capes.size() + " Microsoft capes";
            } catch (Exception ignored) {
                status = "Could not load account capes";
            } finally {
                fetching = false;
            }
        });
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        PENDING.forEach((id, bytes) -> {
            if (LOADED.containsKey(id)) {
                return;
            }
            try {
                NativeImage image = NativeImage.read(bytes);
                Identifier identifier = Identifier.fromNamespaceAndPath("tidal-builtin", "textures/ms-cape/" + id.replace("-", "") + ".png");
                DynamicTexture texture = new DynamicTexture(() -> "tidal-ms-cape-" + id, image);
                minecraft.getTextureManager().register(identifier, texture);
                Cape cape = find(id);
                if (cape != null) {
                    LOADED.put(id, new Loaded(cape, identifier, new ClientAsset.ResourceTexture(identifier, identifier)));
                }
            } catch (Exception ignored) {
            }
        });
        PENDING.keySet().removeIf(LOADED::containsKey);
        scanOthers(minecraft);
    }

    public static ClientAsset.Texture asset(String id) {
        Loaded loaded = LOADED.get(id);
        return loaded == null ? null : loaded.asset;
    }

    public static ClientAsset.Texture other(UUID uuid) {
        return OTHERS.get(uuid);
    }

    public static void blit(GuiGraphicsExtractor graphics, String id, int x, int y, int w, int h) {
        Loaded loaded = LOADED.get(id);
        if (loaded == null) {
            Glass.roundedFill(graphics, x, y, x + w, y + h, 4, 0xFF0349FC);
            return;
        }
        graphics.blit(loaded.identifier, x, y, x + w, y + h, 0.0f, 22.0f / 64.0f, 0.0f, 17.0f / 32.0f);
    }

    public static boolean owns(String key) {
        if (!key.startsWith("ms:")) {
            return false;
        }
        String id = key.substring(3);
        synchronized (OWNED) {
            for (Cape cape : OWNED) {
                if (cape.id.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void activate(String id) {
        Thread.ofVirtual().name("tidal-ms-cape-equip").start(() -> {
            try {
                String token = token();
                if (token == null) {
                    return;
                }
                HttpRequest request;
                if (id == null || id.isBlank()) {
                    request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                        .timeout(Duration.ofSeconds(12))
                        .header("Authorization", "Bearer " + token)
                        .DELETE()
                        .build();
                } else {
                    request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                        .timeout(Duration.ofSeconds(12))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString("{\"capeId\":\"" + id + "\"}"))
                        .build();
                }
                HTTP.send(request, HttpResponse.BodyHandlers.discarding());
                refresh();
            } catch (Exception ignored) {
            }
        });
    }

    private static void scanOthers(Minecraft minecraft) {
        if (minecraft.level == null || System.nanoTime() - lastOtherScan < 3_000_000_000L) {
            return;
        }
        lastOtherScan = System.nanoTime();
        for (var player : minecraft.level.players()) {
            UUID uuid = player.getUUID();
            if (minecraft.player != null && uuid.equals(minecraft.player.getUUID())) {
                continue;
            }
            if (OTHERS.containsKey(uuid)) {
                continue;
            }
            Thread.ofVirtual().name("tidal-cape-other").start(() -> fetchOther(minecraft, uuid));
        }
    }

    private static void fetchOther(Minecraft minecraft, UUID uuid) {
        try {
            String id = uuid.toString().replace("-", "");
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=true")
                )
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return;
            }
            String url = capeUrl(response.body());
            if (url.isBlank()) {
                return;
            }
            byte[] png = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            minecraft.execute(() -> {
                try {
                    NativeImage image = NativeImage.read(png);
                    Identifier identifier = Identifier.fromNamespaceAndPath("tidal-builtin", "textures/other-cape/" + id + ".png");
                    DynamicTexture texture = new DynamicTexture(() -> "tidal-other-cape-" + id, image);
                    minecraft.getTextureManager().register(identifier, texture);
                    OTHERS.put(uuid, new ClientAsset.ResourceTexture(identifier, identifier));
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static String capeUrl(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray properties = root.getAsJsonArray("properties");
            for (JsonElement element : properties) {
                JsonObject property = element.getAsJsonObject();
                if (!"textures".equals(text(property, "name"))) {
                    continue;
                }
                String decoded = new String(java.util.Base64.getDecoder().decode(text(property, "value")));
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
                if (textures != null && textures.has("CAPE")) {
                    return text(textures.getAsJsonObject("CAPE"), "url");
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static void download(String id, String url) {
        if (url.isBlank() || PENDING.containsKey(id) || LOADED.containsKey(id)) {
            return;
        }
        try {
            byte[] png = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            PENDING.put(id, png);
        } catch (Exception ignored) {
        }
    }

    private static Cape find(String id) {
        synchronized (OWNED) {
            for (Cape cape : OWNED) {
                if (cape.id.equals(id)) {
                    return cape;
                }
            }
        }
        return null;
    }

    private static String token() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        return minecraft.getUser().getAccessToken();
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
}
