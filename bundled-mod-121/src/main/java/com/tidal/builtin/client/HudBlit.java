package com.tidal.builtin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Method;

final class HudBlit {
    private static Method quad;
    private static Method classic;
    private static boolean ready;

    private HudBlit() {}

    private static void init() {
        if (ready) {
            return;
        }
        ready = true;
        for (Method method : GuiGraphics.class.getMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 9 && isTexture(p[0]) && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class
                && p[5] == float.class && p[6] == float.class && p[7] == float.class && p[8] == float.class) {
                quad = method;
            } else if (p.length == 9 && isTexture(p[0]) && p[1] == int.class && p[2] == int.class && p[3] == float.class && p[5] == int.class) {
                classic = method;
            }
        }
    }

    private static boolean isTexture(Class<?> type) {
        String name = type.getName();
        return name.endsWith("ResourceLocation") || name.endsWith("Identifier") || name.contains("class_2960");
    }

    static void image(GuiGraphics graphics, ResourceLocation id, int x, int y, int size) {
        uv(graphics, id, x, y, size, size, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    static void uv(
        GuiGraphics graphics,
        ResourceLocation id,
        int x,
        int y,
        int w,
        int h,
        float u0,
        float u1,
        float v0,
        float v1
    ) {
        init();
        try {
            if (quad != null) {
                quad.invoke(graphics, id, x, y, x + w, y + h, u0, u1, v0, v1);
                return;
            }
            if (classic != null) {
                classic.invoke(graphics, id, x, y, 0.0f, 0.0f, w, h, Math.max(1, w), Math.max(1, h));
                return;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        graphics.fill(x, y, x + w, y + h, 0xFF0349FC);
    }

    static void sprite(
        GuiGraphics graphics,
        ResourceLocation id,
        int x,
        int y,
        int w,
        int h,
        float u,
        float v,
        int regionW,
        int regionH,
        int texW,
        int texH
    ) {
        uv(graphics, id, x, y, w, h, u / texW, (u + regionW) / texW, v / texH, (v + regionH) / texH);
    }
}
