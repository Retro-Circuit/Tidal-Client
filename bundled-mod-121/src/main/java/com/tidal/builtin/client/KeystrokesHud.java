package com.tidal.builtin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public final class KeystrokesHud {
    private static final ResourceLocation DEFAULT_IDLE = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/keystrokes_default");
    private static final ResourceLocation DEFAULT_DOWN = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/keystrokes_default_down");
    private static final ResourceLocation INFO_IDLE = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/keystrokes_info");
    private static final ResourceLocation INFO_DOWN = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "dynamic/keystrokes_info_down");

    private static final Key[] DEFAULT_KEYS = {
        key("up", 253, 0, 238, 238, 931, 876, 328, 35, 238, 238, 880, 888, 317, 47, 239, 239),
        key("left", 0, 253, 238, 239, 931, 876, 75, 288, 238, 239, 880, 888, 64, 298, 238, 239),
        key("down", 253, 253, 238, 239, 931, 876, 328, 288, 238, 239, 880, 888, 317, 300, 239, 239),
        key("right", 507, 253, 238, 239, 931, 876, 582, 288, 238, 239, 880, 888, 571, 300, 238, 239),
        key("jump", 0, 511, 745, 122, 931, 876, 75, 546, 745, 122, 880, 888, 64, 557, 745, 123),
        key("attack", 0, 652, 348, 123, 931, 876, 75, 687, 348, 123, 880, 888, 64, 699, 348, 123),
        key("use", 397, 652, 348, 123, 931, 876, 472, 687, 348, 123, 880, 888, 461, 699, 348, 123)
    };
    private static final int DEFAULT_W = 745;
    private static final int DEFAULT_H = 775;

    private static final Key[] INFO_KEYS = {
        key("up", 494, 0, 210, 210, 1024, 783, 552, 37, 210, 210, 1024, 726, 502, 47, 194, 193),
        key("esc", 0, 223, 117, 93, 1024, 783, 58, 260, 117, 93, 1024, 726, 50, 249, 109, 86),
        key("drop", 134, 223, 117, 93, 1024, 783, 192, 260, 117, 93, 1024, 726, 172, 249, 108, 86),
        key("left", 270, 223, 211, 211, 1024, 783, 328, 260, 211, 211, 1024, 726, 297, 250, 194, 193),
        key("down", 494, 223, 210, 211, 1024, 783, 552, 260, 210, 211, 1024, 726, 502, 251, 194, 194),
        key("right", 718, 223, 210, 211, 1024, 783, 776, 260, 210, 211, 1024, 726, 707, 251, 193, 194),
        key("swap", 0, 341, 117, 93, 1024, 783, 58, 378, 117, 93, 1024, 726, 50, 357, 108, 86),
        key("inventory", 134, 341, 117, 93, 1024, 783, 192, 378, 117, 93, 1024, 726, 172, 358, 108, 85),
        key("sneak", 0, 451, 254, 108, 1024, 783, 58, 488, 254, 108, 1024, 726, 50, 459, 233, 100),
        key("jump", 270, 451, 658, 108, 1024, 783, 328, 488, 658, 108, 1024, 726, 297, 459, 604, 100),
        key("attack", 270, 576, 308, 108, 1024, 783, 328, 613, 308, 108, 1024, 726, 297, 573, 283, 100),
        key("sprint", 0, 576, 254, 108, 1024, 783, 58, 613, 254, 108, 1024, 726, 49, 573, 234, 100),
        key("use", 621, 576, 307, 108, 1024, 783, 679, 613, 307, 108, 1024, 726, 618, 573, 283, 100)
    };
    private static final int INFO_W = 928;
    private static final int INFO_H = 684;

    private static boolean ready;

    private KeystrokesHud() {}

    static int width() {
        return TidalMods.keystrokesInfo ? 110 : 78;
    }

    static int height() {
        int srcW = TidalMods.keystrokesInfo ? INFO_W : DEFAULT_W;
        int srcH = TidalMods.keystrokesInfo ? INFO_H : DEFAULT_H;
        return Math.max(1, Math.round(width() * (srcH / (float) srcW)));
    }

    static int x() {
        return TidalMods.hudX;
    }

    static int y() {
        return TidalMods.hudY < 0 ? 8 : TidalMods.hudY;
    }

    public static void extract(GuiGraphics graphics, Minecraft minecraft) {
        if (!TidalMods.keystrokes || minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof TidalPanelScreen
            || minecraft.screen instanceof RadialScreen
            || minecraft.screen instanceof LayoutMenuScreen) {
            return;
        }
        HudDraw.scaled(graphics, x(), y(), TidalMods.hudScale, () -> draw(graphics, minecraft, 0, 0, false));
    }

    static void draw(GuiGraphics graphics, Minecraft minecraft, int originX, int originY, boolean editing) {
        ensure(minecraft);
        Key[] keys = TidalMods.keystrokesInfo ? INFO_KEYS : DEFAULT_KEYS;
        int srcW = TidalMods.keystrokesInfo ? INFO_W : DEFAULT_W;
        float scale = width() / (float) srcW;
        if (editing) {
            Glass.roundedFill(graphics, originX - 3, originY - 3, originX + width() + 3, originY + height() + 3, 8, 0x330349FC);
        }
        ResourceLocation idle = TidalMods.keystrokesInfo ? INFO_IDLE : DEFAULT_IDLE;
        ResourceLocation pressedTex = TidalMods.keystrokesInfo ? INFO_DOWN : DEFAULT_DOWN;
        for (Key key : keys) {
            boolean pressed = down(minecraft, key.bind);
            int x = originX + Math.round(key.x * scale);
            int y = originY + Math.round(key.y * scale);
            int w = Math.max(1, Math.round(key.w * scale));
            int h = Math.max(1, Math.round(key.h * scale));
            if (pressed) {
                HudBlit.uv(graphics, pressedTex, x, y, w, h, key.pu0, key.pu1, key.pv0, key.pv1);
            } else {
                HudBlit.uv(graphics, idle, x, y, w, h, key.u0, key.u1, key.v0, key.v1);
            }
        }
    }

    private static boolean down(Minecraft minecraft, String bind) {
        Options options = minecraft.options;
        KeyMapping mapping = switch (bind) {
            case "up" -> options.keyUp;
            case "left" -> options.keyLeft;
            case "down" -> options.keyDown;
            case "right" -> options.keyRight;
            case "jump" -> options.keyJump;
            case "attack" -> options.keyAttack;
            case "use" -> options.keyUse;
            case "sneak" -> options.keyShift;
            case "sprint" -> options.keySprint;
            case "drop" -> options.keyDrop;
            case "inventory" -> options.keyInventory;
            case "swap" -> options.keySwapOffhand;
            default -> null;
        };
        if (mapping != null) {
            return mapping.isDown();
        }
        return "esc".equals(bind) && MenuKeys.glfwDown(minecraft, GLFW.GLFW_KEY_ESCAPE);
    }

    private static void ensure(Minecraft minecraft) {
        if (ready || minecraft == null) {
            return;
        }
        register(minecraft, DEFAULT_IDLE, "/assets/tidal-builtin/textures/gui/keystrokes/default.png");
        register(minecraft, DEFAULT_DOWN, "/assets/tidal-builtin/textures/gui/keystrokes/default_pressed.png");
        register(minecraft, INFO_IDLE, "/assets/tidal-builtin/textures/gui/keystrokes/info.png");
        register(minecraft, INFO_DOWN, "/assets/tidal-builtin/textures/gui/keystrokes/info_pressed.png");
        ready = true;
    }

    private static void register(Minecraft minecraft, ResourceLocation id, String classpath) {
        try (InputStream stream = KeystrokesHud.class.getResourceAsStream(classpath)) {
            if (stream == null) {
                return;
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                return;
            }
            minecraft.getTextureManager().register(id, NativeImages.texture(id.getPath(), NativeImages.fromBuffered(image)));
        } catch (Exception ignored) {
        }
    }

    private static Key key(
        String bind,
        int x, int y, int w, int h,
        int atlasW, int atlasH, int ax, int ay, int aw, int ah,
        int pAtlasW, int pAtlasH, int px, int py, int pw, int ph
    ) {
        return new Key(
            bind, x, y, w, h,
            ax / (float) atlasW, (ax + aw) / (float) atlasW, ay / (float) atlasH, (ay + ah) / (float) atlasH,
            px / (float) pAtlasW, (px + pw) / (float) pAtlasW, py / (float) pAtlasH, (py + ph) / (float) pAtlasH
        );
    }

    private record Key(
        String bind, int x, int y, int w, int h,
        float u0, float u1, float v0, float v1,
        float pu0, float pu1, float pv0, float pv1
    ) {}
}
