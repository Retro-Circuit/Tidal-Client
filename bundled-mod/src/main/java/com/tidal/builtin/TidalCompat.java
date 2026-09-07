package com.tidal.builtin;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Decides whether this jar's 26.2 client mappings can run.
 * Other Minecraft versions still load the mod; mixins and UI stay off so the game does not crash.
 */
public final class TidalCompat {
    public static final boolean FEATURES = detect();

    private TidalCompat() {}

    public static String minecraftVersion() {
        try {
            return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map((mod) -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("");
        } catch (Throwable ignored) {
            return "";
        }
    }

    static boolean detect() {
        return supported(minecraftVersion());
    }

    public static boolean supported(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        String v = version.trim();
        return v.equals("26.2") || v.startsWith("26.2.") || v.startsWith("26.2-") || v.startsWith("26.2+");
    }

    public static boolean classPresent(String runtimeName) {
        try {
            Class.forName(runtimeName.replace('/', '.'), false, TidalCompat.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void invokeRuntime(String method, Object... args) {
        if (!FEATURES) {
            return;
        }
        try {
            Class<?> cls = Class.forName("com.tidal.builtin.TidalBuiltinRuntime");
            if (args.length == 0) {
                cls.getMethod(method).invoke(null);
                return;
            }
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = Object.class;
            }
            cls.getMethod(method, types).invoke(null, args);
        } catch (Throwable ignored) {
        }
    }
}
