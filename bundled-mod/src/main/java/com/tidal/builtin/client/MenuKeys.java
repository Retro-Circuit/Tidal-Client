package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

public final class MenuKeys {
    private static GLFWKeyCallback hook;
    private static boolean held;
    private static boolean pressEdge;
    private static boolean zoomHeld;

    private MenuKeys() {}

    public static void ensureInstalled(Minecraft minecraft) {
        if (hook != null || minecraft == null) {
            return;
        }
        long handle = windowHandle(minecraft);
        if (handle == 0L) {
            return;
        }
        GLFWKeyCallback[] prior = new GLFWKeyCallback[1];
        hook = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                onKey(key, action);
                if (action == GLFW.GLFW_PRESS && isMenuKey(key)) {
                    tryOpen(minecraft);
                }
                if (prior[0] != null) {
                    prior[0].invoke(window, key, scancode, action, mods);
                }
            }
        };
        prior[0] = GLFW.glfwSetKeyCallback(handle, hook);
    }

    public static void onKey(int key, int action) {
        if (key == GLFW.GLFW_KEY_C) {
            zoomHeld = action != GLFW.GLFW_RELEASE;
        }
        if (!isMenuKey(key)) {
            return;
        }
        if (action == GLFW.GLFW_PRESS) {
            held = true;
            pressEdge = true;
        } else if (action == GLFW.GLFW_RELEASE) {
            held = false;
        }
    }

    public static boolean isDown(Minecraft minecraft) {
        if (held) {
            return true;
        }
        return glfwDown(minecraft, TidalMods.menuKey) || altPairDown(minecraft);
    }

    public static boolean consumePress() {
        boolean edge = pressEdge;
        pressEdge = false;
        return edge;
    }

    public static boolean zooming() {
        return PlayMods.zoom && zoomHeld;
    }

    public static void tryOpen(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        Screen current = minecraft.gui.screen();
        if (current != null) {
            return;
        }
        minecraft.gui.setScreen(new RadialScreen());
    }

    static boolean isMenuKey(int key) {
        int bound = TidalMods.menuKey;
        if (key == bound) {
            return true;
        }
        return isAlt(bound) && isAlt(key);
    }

    private static boolean isAlt(int key) {
        return key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static boolean altPairDown(Minecraft minecraft) {
        return isAlt(TidalMods.menuKey)
            && (glfwDown(minecraft, GLFW.GLFW_KEY_LEFT_ALT) || glfwDown(minecraft, GLFW.GLFW_KEY_RIGHT_ALT));
    }

    static boolean glfwDown(Minecraft minecraft, int key) {
        try {
            long handle = windowHandle(minecraft);
            return handle != 0L && GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static long windowHandle(Minecraft minecraft) {
        try {
            Object window = minecraft.getWindow();
            if (window instanceof Number number) {
                return number.longValue();
            }
            try {
                Object handle = window.getClass().getMethod("handle").invoke(window);
                if (handle instanceof Number number) {
                    return number.longValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
            try {
                Object handle = window.getClass().getMethod("getWindow").invoke(window);
                if (handle instanceof Number number) {
                    return number.longValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }
}
