package com.tidal.builtin.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class TidalMods {
    public static boolean keystrokes = false;
    public static int hudX = 12;
    public static int hudY = 12;
    public static float hudScale = 1.0f;
    public static boolean armor = false;
    public static int armorX = 12;
    public static int armorY = 78;
    public static float armorScale = 1.0f;
    public static boolean armorHelmet = true;
    public static boolean armorChest = true;
    public static boolean armorLegs = true;
    public static boolean armorBoots = true;
    public static boolean armorIcons = true;
    public static boolean armorPercent = true;
    public static boolean armorVertical = true;
    public static boolean armorEmpty = false;
    public static boolean armorColor = true;
    public static boolean armorPoints = true;
    public static boolean armorDamagedOnly = false;
    public static boolean armorEnchants = false;
    public static int menuKey = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
    public static boolean cps = false;
    public static int cpsX = 12;
    public static int cpsY = 200;
    public static float cpsScale = 1.0f;
    public static boolean fpsPing = false;
    public static int fpsX = 12;
    public static int fpsY = 224;
    public static float fpsScale = 1.0f;
    public static boolean combo = false;
    public static int comboX = 12;
    public static int comboY = 248;
    public static float comboScale = 1.0f;
    public static boolean lowFire = false;
    public static boolean noHurtCam = false;
    public static boolean customParticles = false;
    public static boolean autoGg = false;
    public static boolean streamerMode = false;
    public static boolean bedrockDetect = false;
    public static boolean boost = true;
    public static boolean boostParticles = true;
    public static boolean boostUnfocused = true;
    public static boolean boostClouds = true;
    public static int points = 0;
    public static String equippedCape = "";
    public static String equippedSkin = "";
    public static final Set<String> ownedCapes = new LinkedHashSet<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tidal-builtin.properties");
    public static final String SHADOW_NAME = "_ShadowzYT";
    public static final String SHADOW_CODE = "tidp100shad";

    private TidalMods() {}

    public static void load() {
        if (Files.isRegularFile(FILE)) {
            try {
                Properties p = new Properties();
                p.load(Files.newInputStream(FILE));
                int schema = integer(p, "schema", 0);
                hudX = integer(p, "hudX", 12);
                hudY = integer(p, "hudY", 12);
                hudScale = decimal(p, "hudScale", 1.0f);
                if (hudY < 0) {
                    hudY = 12;
                }
                armorX = integer(p, "armorX", 12);
                armorY = integer(p, "armorY", 78);
                armorScale = decimal(p, "armorScale", 1.0f);
                armorHelmet = bool(p, "armorHelmet", true);
                armorChest = bool(p, "armorChest", true);
                armorLegs = bool(p, "armorLegs", true);
                armorBoots = bool(p, "armorBoots", true);
                armorIcons = bool(p, "armorIcons", true);
                armorPercent = bool(p, "armorPercent", true);
                armorVertical = bool(p, "armorVertical", true);
                armorEmpty = bool(p, "armorEmpty", false);
                armorColor = bool(p, "armorColor", true);
                armorPoints = bool(p, "armorPoints", true);
                armorDamagedOnly = bool(p, "armorDamagedOnly", false);
                armorEnchants = bool(p, "armorEnchants", false);
                menuKey = integer(p, "menuKey", org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT);
                cps = bool(p, "cps", false);
                cpsX = integer(p, "cpsX", 12);
                cpsY = integer(p, "cpsY", 200);
                cpsScale = decimal(p, "cpsScale", 1.0f);
                fpsPing = bool(p, "fpsPing", false);
                fpsX = integer(p, "fpsX", 12);
                fpsY = integer(p, "fpsY", 224);
                fpsScale = decimal(p, "fpsScale", 1.0f);
                combo = bool(p, "combo", false);
                comboX = integer(p, "comboX", 12);
                comboY = integer(p, "comboY", 248);
                comboScale = decimal(p, "comboScale", 1.0f);
                lowFire = bool(p, "lowFire", false);
                noHurtCam = bool(p, "noHurtCam", false);
                customParticles = bool(p, "customParticles", false);
                autoGg = bool(p, "autoGg", false);
                streamerMode = bool(p, "streamerMode", false);
                bedrockDetect = bool(p, "bedrockDetect", false);
                boost = bool(p, "boost", true);
                boostParticles = bool(p, "boostParticles", true);
                boostUnfocused = bool(p, "boostUnfocused", true);
                boostClouds = bool(p, "boostClouds", true);
                PlayMods.load(p);
                equippedCape = p.getProperty("equippedCape", "");
                equippedSkin = p.getProperty("equippedSkin", "");
                ownedCapes.clear();
                String owned = p.getProperty("ownedCapes", "");
                if (!owned.isBlank()) {
                    ownedCapes.addAll(Arrays.asList(owned.split(",")));
                }
                if (schema < 2) {
                    keystrokes = false;
                    armor = false;
                } else {
                    keystrokes = bool(p, "keystrokes", false);
                    armor = bool(p, "armor", false);
                }
                if (schema < 3) {
                    armorX = 12;
                    armorY = 78;
                }
                if (schema < 4) {
                    if (armorX < 0) {
                        armorX = 12;
                    }
                    if (hudX == 8 && hudY == 8) {
                        hudX = 12;
                        hudY = 12;
                    }
                    if (armorX == hudX && Math.abs(armorY - hudY) < 16) {
                        armorY = hudY + 66;
                    }
                    hudScale = 1.0f;
                    armorScale = 1.0f;
                }
            } catch (Exception ignored) {
            }
        }
        loadWallet();
    }

    public static void loadWallet() {
        for (Path file : walletFiles()) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.has("points")) {
                    points = Math.max(0, json.get("points").getAsInt());
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty("schema", "4");
            p.setProperty("keystrokes", Boolean.toString(keystrokes));
            p.setProperty("hudX", Integer.toString(hudX));
            p.setProperty("hudY", Integer.toString(hudY));
            p.setProperty("hudScale", Float.toString(hudScale));
            p.setProperty("armor", Boolean.toString(armor));
            p.setProperty("armorX", Integer.toString(armorX));
            p.setProperty("armorY", Integer.toString(armorY));
            p.setProperty("armorScale", Float.toString(armorScale));
            p.setProperty("armorHelmet", Boolean.toString(armorHelmet));
            p.setProperty("armorChest", Boolean.toString(armorChest));
            p.setProperty("armorLegs", Boolean.toString(armorLegs));
            p.setProperty("armorBoots", Boolean.toString(armorBoots));
            p.setProperty("armorIcons", Boolean.toString(armorIcons));
            p.setProperty("armorPercent", Boolean.toString(armorPercent));
            p.setProperty("armorVertical", Boolean.toString(armorVertical));
            p.setProperty("armorEmpty", Boolean.toString(armorEmpty));
            p.setProperty("armorColor", Boolean.toString(armorColor));
            p.setProperty("armorPoints", Boolean.toString(armorPoints));
            p.setProperty("armorDamagedOnly", Boolean.toString(armorDamagedOnly));
            p.setProperty("armorEnchants", Boolean.toString(armorEnchants));
            p.setProperty("menuKey", Integer.toString(menuKey));
            p.setProperty("cps", Boolean.toString(cps));
            p.setProperty("cpsX", Integer.toString(cpsX));
            p.setProperty("cpsY", Integer.toString(cpsY));
            p.setProperty("cpsScale", Float.toString(cpsScale));
            p.setProperty("fpsPing", Boolean.toString(fpsPing));
            p.setProperty("fpsX", Integer.toString(fpsX));
            p.setProperty("fpsY", Integer.toString(fpsY));
            p.setProperty("fpsScale", Float.toString(fpsScale));
            p.setProperty("combo", Boolean.toString(combo));
            p.setProperty("comboX", Integer.toString(comboX));
            p.setProperty("comboY", Integer.toString(comboY));
            p.setProperty("comboScale", Float.toString(comboScale));
            p.setProperty("lowFire", Boolean.toString(lowFire));
            p.setProperty("noHurtCam", Boolean.toString(noHurtCam));
            p.setProperty("customParticles", Boolean.toString(customParticles));
            p.setProperty("autoGg", Boolean.toString(autoGg));
            p.setProperty("streamerMode", Boolean.toString(streamerMode));
            p.setProperty("bedrockDetect", Boolean.toString(bedrockDetect));
            p.setProperty("boost", Boolean.toString(boost));
            p.setProperty("boostParticles", Boolean.toString(boostParticles));
            p.setProperty("boostUnfocused", Boolean.toString(boostUnfocused));
            p.setProperty("boostClouds", Boolean.toString(boostClouds));
            PlayMods.save(p);
            p.setProperty("equippedCape", equippedCape);
            p.setProperty("equippedSkin", equippedSkin);
            p.setProperty("ownedCapes", String.join(",", ownedCapes.stream().filter(id -> !Capes.isExclusive(id)).toList()));
            p.store(Files.newOutputStream(FILE), "Tidal Client");
        } catch (Exception ignored) {
        }
        saveWallet();
    }

    public static boolean grantShadowPoints() {
        if (!SHADOW_NAME.equalsIgnoreCase(Capes.localName())) {
            return false;
        }
        points += 100;
        save();
        return true;
    }

    public static String menuKeyName() {
        int key = menuKey;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) {
            return "Left Alt";
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT) {
            return "Right Alt";
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) {
            return "Left Ctrl";
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) {
            return "Right Ctrl";
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) {
            return "Left Shift";
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return "Right Shift";
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
            return "Space";
        }
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isBlank()) {
            return name.toUpperCase();
        }
        return "Key " + key;
    }

    public static boolean owns(String id) {
        if (id != null && id.startsWith("ms:")) {
            return AccountCapes.owns(id);
        }
        if (Capes.isExclusive(id)) {
            return Capes.exclusiveOwned(id);
        }
        return true;
    }

    public static void equipSkin(String key) {
        equippedSkin = key == null ? "" : key;
        save();
    }

    public static boolean buy(String id, int price) {
        if (owns(id)) {
            return true;
        }
        if (Capes.isExclusive(id) || points < price) {
            return false;
        }
        points -= price;
        ownedCapes.add(id);
        save();
        return true;
    }

    public static void equip(String id) {
        if (id == null) {
            id = "";
        }
        if (id.startsWith("ms:")) {
            equippedCape = AccountCapes.owns(id) ? id : "";
            AccountCapes.activate(equippedCape.isBlank() ? "" : equippedCape.substring(3));
        } else if (id.isBlank()) {
            equippedCape = "";
            AccountCapes.activate("");
        } else {
            equippedCape = owns(id) ? id : "";
        }
        CapeShare.publish(equippedCape.startsWith("ms:") ? "" : equippedCape);
        save();
    }

    private static void saveWallet() {
        String json = "{\"points\":" + points + "}\n";
        for (Path file : walletFiles()) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            } catch (Exception ignored) {
            }
        }
    }

    private static Path[] walletFiles() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path parent = gameDir.getParent();
        Path shared = parent != null && parent.getParent() != null
            ? parent.getParent().resolve("tidal-wallet.json")
            : gameDir.resolve("tidal-wallet.json");
        return new Path[] {shared, gameDir.resolve("tidal-wallet.json")};
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(fallback)));
    }

    private static int integer(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float decimal(Properties p, String key, float fallback) {
        try {
            return Float.parseFloat(p.getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
