package com.tidal.builtin.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in performance overhaul. Each pass yields to specialist mods
 * (Sodium, Iris, EntityCulling, Dynamic FPS, …) and only fills the gaps.
 */
public final class TidalBoost {
    public record Companion(String id, String name, boolean loaded) {}
    public record Pass(String title, String hint, boolean active) {}

    private static final String[][] COMPANIONS = {
        {"sodium", "Sodium"},
        {"lithium", "Lithium"},
        {"starlight", "Starlight"},
        {"phosphor", "Phosphor"},
        {"iris", "Iris"},
        {"indium", "Indium"},
        {"ferritecore", "FerriteCore"},
        {"immediatelyfast", "ImmediatelyFast"},
        {"entityculling", "EntityCulling"},
        {"moreculling", "MoreCulling"},
        {"modernfix", "ModernFix"},
        {"krypton", "Krypton"},
        {"sodium-extra", "Sodium Extra"},
        {"dynamic_fps", "Dynamic FPS"},
        {"dynamic-fps", "Dynamic FPS"}
    };

    private static int particlesThisTick;

    private TidalBoost() {}

    public static void beginTick() {
        particlesThisTick = 0;
    }

    public static boolean on() {
        return TidalMods.boost;
    }

    public static boolean sodium() {
        return loaded("sodium");
    }

    public static boolean iris() {
        return loaded("iris");
    }

    public static boolean extraParticles() {
        return loaded("sodium-extra");
    }

    public static boolean dynamicFps() {
        return loaded("dynamic_fps") || loaded("dynamic-fps");
    }

    public static boolean entityCulling() {
        return loaded("entityculling") || loaded("moreculling");
    }

    public static boolean loaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static boolean fastClouds(net.minecraft.client.CloudStatus current) {
        return on() && !sodium() && current == net.minecraft.client.CloudStatus.FANCY;
    }

    public static boolean skipWeather() {
        return on() && !sodium() && !iris();
    }

    public static boolean cullEntities() {
        return on() && !sodium() && !entityCulling();
    }

    public static boolean dropParticle() {
        if (!on() || extraParticles()) {
            return false;
        }
        particlesThisTick++;
        int cap = sodium() ? 240 : 64;
        return particlesThisTick > cap;
    }

    public static int unfocusedLimit(int current) {
        if (!on() || dynamicFps()) {
            return current;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return current;
        }
        if (!minecraft.isWindowActive()) {
            return Math.min(current, 30);
        }
        if (minecraft.gui.screen() != null && !(minecraft.gui.screen() instanceof ChatScreen)) {
            return Math.min(current, 60);
        }
        return current;
    }

    public static boolean hideEntity(Entity entity, double camX, double camY, double camZ) {
        if (!cullEntities() || entity == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && (minecraft.player == entity || entity instanceof Player)) {
            return false;
        }
        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double dist = dx * dx + dy * dy + dz * dz;
        double range = 64.0;
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            range = 28.0;
        }
        if (minecraft != null) {
            range *= Math.max(0.5, minecraft.options.entityDistanceScaling().get());
        }
        return dist > range * range;
    }

    public static List<Companion> companions() {
        List<Companion> list = new ArrayList<>();
        boolean sawDynamic = false;
        for (String[] row : COMPANIONS) {
            boolean found = loaded(row[0]);
            if ("dynamic_fps".equals(row[0]) || "dynamic-fps".equals(row[0])) {
                if (sawDynamic || !found) {
                    continue;
                }
                sawDynamic = true;
            }
            if (found) {
                list.add(new Companion(row[0], row[1], true));
            }
        }
        return list;
    }

    public static List<Pass> passes() {
        List<Pass> list = new ArrayList<>();
        list.add(new Pass("Particle budget", extraParticles() ? "Sodium Extra owns this" : (sodium() ? "Soft cap (Sodium present)" : "Hard cap at 64 extra particles"), on() && !extraParticles()));
        list.add(new Pass("Unfocused / menu FPS", dynamicFps() ? "Dynamic FPS owns this" : "30 FPS unfocused, 60 FPS in menus", on() && !dynamicFps()));
        list.add(new Pass("Clouds", sodium() ? "Sodium owns clouds" : "Fancy clouds dropped to fast", on() && !sodium()));
        list.add(new Pass("Weather mesh", sodium() || iris() ? "Renderer mod owns weather" : "Skip rain/snow mesh when far load", skipWeather()));
        list.add(new Pass("Entity distance", sodium() || entityCulling() ? "Culling mod owns entities" : "Drop far item orbs and players", cullEntities()));
        return list;
    }
}
