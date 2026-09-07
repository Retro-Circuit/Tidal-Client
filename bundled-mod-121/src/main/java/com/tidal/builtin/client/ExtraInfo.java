package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ExtraInfo {
    private ExtraInfo() {}

    static String coords(LocalPlayer player) {
        return String.format(Locale.ROOT, "%d %d %d", (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
    }

    static String nether(Minecraft minecraft, LocalPlayer player) {
        int x = (int) Math.floor(player.getX());
        int y = (int) Math.floor(player.getY());
        int z = (int) Math.floor(player.getZ());
        String dim = dimension(minecraft);
        if (dim.contains("nether")) {
            return "OW " + (x * 8) + " " + y + " " + (z * 8);
        }
        return "Nether " + (x / 8) + " " + y + " " + (z / 8);
    }

    static String compass(LocalPlayer player) {
        float yaw = player.getYRot() % 360.0f;
        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return dirs[Math.round(yaw / 45.0f) % 8] + " " + Math.round(yaw) + "°";
    }

    static String biome(Minecraft minecraft, LocalPlayer player) {
        try {
            Object level = minecraft.level;
            Object pos = player.blockPosition();
            Object biome = invoke(level, "getBiome", pos);
            Object key = invoke(biome, "unwrapKey");
            if (key instanceof Optional<?> optional && optional.isPresent()) {
                Object resource = invoke(optional.get(), "location");
                if (resource == null) {
                    resource = invoke(optional.get(), "identifier");
                }
                String path = String.valueOf(invoke(resource, "getPath"));
                return title(path.replace('_', ' '));
            }
        } catch (Throwable ignored) {
        }
        return "Biome";
    }

    static String clock(Minecraft minecraft) {
        long dayTime = dayTime(minecraft);
        long ticks = ((dayTime + 6000L) % 24000L);
        long hours = ticks / 1000L;
        long minutes = (ticks % 1000L) * 60L / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
    }

    static String speed(LocalPlayer player) {
        try {
            Object movement = player.getClass().getMethod("getDeltaMovement").invoke(player);
            double dx = ((Number) movement.getClass().getField("x").get(movement)).doubleValue();
            double dz = ((Number) movement.getClass().getField("z").get(movement)).doubleValue();
            return String.format(Locale.ROOT, "%.1f m/s", Math.sqrt(dx * dx + dz * dz) * 20.0);
        } catch (Throwable ignored) {
            return "0.0 m/s";
        }
    }

    static String memory() {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
        long max = runtime.maxMemory() / 1024L / 1024L;
        return used + " / " + max + " MB";
    }

    static String server(Minecraft minecraft) {
        try {
            Object data = invoke(minecraft, "getCurrentServer");
            if (data != null) {
                Object ip = fieldOrMethod(data, "ip", "ip");
                if (ip != null && !String.valueOf(ip).isBlank()) {
                    return String.valueOf(ip);
                }
            }
        } catch (Throwable ignored) {
        }
        return "Singleplayer";
    }

    static String saturation(LocalPlayer player) {
        try {
            Object food = invoke(player, "getFoodData");
            Object value = invoke(food, "getSaturationLevel");
            if (value instanceof Number number) {
                return String.format(Locale.ROOT, "Sat %.1f", number.floatValue());
            }
        } catch (Throwable ignored) {
        }
        return "Sat";
    }

    static String day(Minecraft minecraft) {
        return "Day " + (dayTime(minecraft) / 24000L);
    }

    static List<String> potions(LocalPlayer player) {
        List<String> lines = new ArrayList<>();
        try {
            Object effects = invoke(player, "getActiveEffects");
            if (effects instanceof Collection<?> collection) {
                for (Object inst : collection) {
                    String name = effectName(inst);
                    int amp = number(invoke(inst, "getAmplifier"), 0);
                    int dur = number(invoke(inst, "getDuration"), 0);
                    String extra = amp > 0 ? " " + (amp + 1) : "";
                    lines.add(name + extra + " " + formatTicks(dur));
                }
            }
        } catch (Throwable ignored) {
        }
        if (lines.isEmpty()) {
            lines.add("No effects");
        }
        return lines;
    }

    private static String effectName(Object inst) {
        try {
            Object holder = invoke(inst, "getEffect");
            Object effect = holder;
            try {
                effect = invoke(holder, "value");
            } catch (Throwable ignored) {
            }
            Object name = invoke(effect, "getDisplayName");
            if (name instanceof Component component) {
                return component.getString();
            }
            if (name != null) {
                return String.valueOf(invoke(name, "getString"));
            }
        } catch (Throwable ignored) {
        }
        return "Effect";
    }

    private static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        return (seconds / 60) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
    }

    private static String dimension(Minecraft minecraft) {
        try {
            Object level = minecraft.level;
            Object dim = invoke(level, "dimension");
            Object loc = invoke(dim, "location");
            return String.valueOf(loc).toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static long dayTime(Minecraft minecraft) {
        try {
            Object value = invoke(minecraft.level, "getDayTime");
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    private static String title(String text) {
        if (text == null || text.isBlank()) {
            return "Biome";
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static Object fieldOrMethod(Object target, String field, String method) {
        try {
            return target.getClass().getField(field).get(target);
        } catch (Throwable ignored) {
        }
        return invoke(target, method);
    }

    private static Object invoke(Object target, String name, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                    continue;
                }
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
