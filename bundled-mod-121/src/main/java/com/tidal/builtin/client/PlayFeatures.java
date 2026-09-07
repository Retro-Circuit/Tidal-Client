package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.lang.reflect.Method;

public final class PlayFeatures {
    private PlayFeatures() {}

    public static void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (PlayMods.fullbright) {
            setOption(minecraft, "gamma", 1.0);
        }
        if (PlayMods.toggleSprint) {
            setOption(minecraft, "toggleSprint", true);
            trySprint(minecraft);
        }
        if (PlayMods.toggleSneak) {
            setOption(minecraft, "toggleCrouch", true);
        }
    }

    public static double zoomFov(double fov) {
        return MenuKeys.zooming() ? Math.max(12.0, fov * 0.32) : fov;
    }

    public static float zoomFov(float fov) {
        return (float) zoomFov((double) fov);
    }

    private static void trySprint(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || !forward(minecraft, player)) {
            return;
        }
        try {
            player.setSprinting(true);
        } catch (Throwable ignored) {
        }
    }

    private static boolean forward(Minecraft minecraft, LocalPlayer player) {
        try {
            Object options = minecraft.options;
            Object key = options.getClass().getField("keyUp").get(options);
            Object down = key.getClass().getMethod("isDown").invoke(key);
            if (Boolean.TRUE.equals(down)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object input = player.getClass().getField("input").get(player);
            Object impulse = input.getClass().getMethod("hasForwardImpulse").invoke(input);
            return Boolean.TRUE.equals(impulse);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void setOption(Minecraft minecraft, String name, Object value) {
        try {
            Object options = minecraft.options;
            Object instance = null;
            try {
                instance = options.getClass().getMethod(name).invoke(options);
            } catch (ReflectiveOperationException ignored) {
                instance = options.getClass().getField(name).get(options);
            }
            if (instance == null) {
                return;
            }
            for (Method method : instance.getClass().getMethods()) {
                if (!method.getName().equals("set") || method.getParameterCount() != 1) {
                    continue;
                }
                method.invoke(instance, value);
                return;
            }
        } catch (Throwable ignored) {
        }
    }
}
