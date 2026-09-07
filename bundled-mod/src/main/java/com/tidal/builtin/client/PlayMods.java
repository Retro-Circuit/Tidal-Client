package com.tidal.builtin.client;

import java.util.Properties;

public final class PlayMods {
    public static boolean coords;
    public static boolean netherCoords;
    public static boolean compass;
    public static boolean biome;
    public static boolean clock;
    public static boolean speedometer;
    public static boolean memory;
    public static boolean serverIp;
    public static boolean potionHud;
    public static boolean saturation;
    public static boolean dayCounter;
    public static boolean fullbright;
    public static boolean zoom;
    public static boolean toggleSprint;
    public static boolean toggleSneak;
    public static boolean hideScoreboard;
    public static boolean hideBossBar;
    public static boolean hidePumpkin;
    public static boolean noVignette;
    public static boolean chatTimestamps;

    public static int coordsX = -1;
    public static int coordsY = 8;
    public static float coordsScale = 1.0f;
    public static int netherX = -1;
    public static int netherY = 26;
    public static float netherScale = 1.0f;
    public static int compassX = -1;
    public static int compassY = 44;
    public static float compassScale = 1.0f;
    public static int biomeX = -1;
    public static int biomeY = 62;
    public static float biomeScale = 1.0f;
    public static int clockX = -1;
    public static int clockY = 80;
    public static float clockScale = 1.0f;
    public static int speedX = -1;
    public static int speedY = 98;
    public static float speedScale = 1.0f;
    public static int memoryX = -1;
    public static int memoryY = 116;
    public static float memoryScale = 1.0f;
    public static int serverX = -1;
    public static int serverY = 134;
    public static float serverScale = 1.0f;
    public static int potionX = -1;
    public static int potionY = 152;
    public static float potionScale = 1.0f;
    public static int satX = -1;
    public static int satY = 188;
    public static float satScale = 1.0f;
    public static int dayX = -1;
    public static int dayY = 206;
    public static float dayScale = 1.0f;

    private PlayMods() {}

    static void load(Properties p) {
        coords = bool(p, "coords", false);
        netherCoords = bool(p, "netherCoords", false);
        compass = bool(p, "compass", false);
        biome = bool(p, "biome", false);
        clock = bool(p, "clock", false);
        speedometer = bool(p, "speedometer", false);
        memory = bool(p, "memory", false);
        serverIp = bool(p, "serverIp", false);
        potionHud = bool(p, "potionHud", false);
        saturation = bool(p, "saturation", false);
        dayCounter = bool(p, "dayCounter", false);
        fullbright = bool(p, "fullbright", false);
        zoom = bool(p, "zoom", false);
        toggleSprint = bool(p, "toggleSprint", false);
        toggleSneak = bool(p, "toggleSneak", false);
        hideScoreboard = bool(p, "hideScoreboard", false);
        hideBossBar = bool(p, "hideBossBar", false);
        hidePumpkin = bool(p, "hidePumpkin", false);
        noVignette = bool(p, "noVignette", false);
        chatTimestamps = bool(p, "chatTimestamps", false);
        coordsX = integer(p, "coordsX", -1);
        coordsY = integer(p, "coordsY", 8);
        coordsScale = decimal(p, "coordsScale", 1.0f);
        netherX = integer(p, "netherX", -1);
        netherY = integer(p, "netherY", 26);
        netherScale = decimal(p, "netherScale", 1.0f);
        compassX = integer(p, "compassX", -1);
        compassY = integer(p, "compassY", 44);
        compassScale = decimal(p, "compassScale", 1.0f);
        biomeX = integer(p, "biomeX", -1);
        biomeY = integer(p, "biomeY", 62);
        biomeScale = decimal(p, "biomeScale", 1.0f);
        clockX = integer(p, "clockX", -1);
        clockY = integer(p, "clockY", 80);
        clockScale = decimal(p, "clockScale", 1.0f);
        speedX = integer(p, "speedX", -1);
        speedY = integer(p, "speedY", 98);
        speedScale = decimal(p, "speedScale", 1.0f);
        memoryX = integer(p, "memoryX", -1);
        memoryY = integer(p, "memoryY", 116);
        memoryScale = decimal(p, "memoryScale", 1.0f);
        serverX = integer(p, "serverX", -1);
        serverY = integer(p, "serverY", 134);
        serverScale = decimal(p, "serverScale", 1.0f);
        potionX = integer(p, "potionX", -1);
        potionY = integer(p, "potionY", 152);
        potionScale = decimal(p, "potionScale", 1.0f);
        satX = integer(p, "satX", -1);
        satY = integer(p, "satY", 188);
        satScale = decimal(p, "satScale", 1.0f);
        dayX = integer(p, "dayX", -1);
        dayY = integer(p, "dayY", 206);
        dayScale = decimal(p, "dayScale", 1.0f);
    }

    static void save(Properties p) {
        p.setProperty("coords", Boolean.toString(coords));
        p.setProperty("netherCoords", Boolean.toString(netherCoords));
        p.setProperty("compass", Boolean.toString(compass));
        p.setProperty("biome", Boolean.toString(biome));
        p.setProperty("clock", Boolean.toString(clock));
        p.setProperty("speedometer", Boolean.toString(speedometer));
        p.setProperty("memory", Boolean.toString(memory));
        p.setProperty("serverIp", Boolean.toString(serverIp));
        p.setProperty("potionHud", Boolean.toString(potionHud));
        p.setProperty("saturation", Boolean.toString(saturation));
        p.setProperty("dayCounter", Boolean.toString(dayCounter));
        p.setProperty("fullbright", Boolean.toString(fullbright));
        p.setProperty("zoom", Boolean.toString(zoom));
        p.setProperty("toggleSprint", Boolean.toString(toggleSprint));
        p.setProperty("toggleSneak", Boolean.toString(toggleSneak));
        p.setProperty("hideScoreboard", Boolean.toString(hideScoreboard));
        p.setProperty("hideBossBar", Boolean.toString(hideBossBar));
        p.setProperty("hidePumpkin", Boolean.toString(hidePumpkin));
        p.setProperty("noVignette", Boolean.toString(noVignette));
        p.setProperty("chatTimestamps", Boolean.toString(chatTimestamps));
        p.setProperty("coordsX", Integer.toString(coordsX));
        p.setProperty("coordsY", Integer.toString(coordsY));
        p.setProperty("coordsScale", Float.toString(coordsScale));
        p.setProperty("netherX", Integer.toString(netherX));
        p.setProperty("netherY", Integer.toString(netherY));
        p.setProperty("netherScale", Float.toString(netherScale));
        p.setProperty("compassX", Integer.toString(compassX));
        p.setProperty("compassY", Integer.toString(compassY));
        p.setProperty("compassScale", Float.toString(compassScale));
        p.setProperty("biomeX", Integer.toString(biomeX));
        p.setProperty("biomeY", Integer.toString(biomeY));
        p.setProperty("biomeScale", Float.toString(biomeScale));
        p.setProperty("clockX", Integer.toString(clockX));
        p.setProperty("clockY", Integer.toString(clockY));
        p.setProperty("clockScale", Float.toString(clockScale));
        p.setProperty("speedX", Integer.toString(speedX));
        p.setProperty("speedY", Integer.toString(speedY));
        p.setProperty("speedScale", Float.toString(speedScale));
        p.setProperty("memoryX", Integer.toString(memoryX));
        p.setProperty("memoryY", Integer.toString(memoryY));
        p.setProperty("memoryScale", Float.toString(memoryScale));
        p.setProperty("serverX", Integer.toString(serverX));
        p.setProperty("serverY", Integer.toString(serverY));
        p.setProperty("serverScale", Float.toString(serverScale));
        p.setProperty("potionX", Integer.toString(potionX));
        p.setProperty("potionY", Integer.toString(potionY));
        p.setProperty("potionScale", Float.toString(potionScale));
        p.setProperty("satX", Integer.toString(satX));
        p.setProperty("satY", Integer.toString(satY));
        p.setProperty("satScale", Float.toString(satScale));
        p.setProperty("dayX", Integer.toString(dayX));
        p.setProperty("dayY", Integer.toString(dayY));
        p.setProperty("dayScale", Float.toString(dayScale));
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
