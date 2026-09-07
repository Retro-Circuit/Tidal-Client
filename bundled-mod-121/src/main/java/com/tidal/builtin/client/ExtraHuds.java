package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import java.util.List;

public final class ExtraHuds {
    private ExtraHuds() {}

    public static void extract(GuiGraphics graphics, Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || hidden(minecraft)) {
            return;
        }
        if (PlayMods.coords) {
            draw(graphics, minecraft, PlayMods.coordsX, PlayMods.coordsY, PlayMods.coordsScale, ExtraInfo.coords(player), false);
        }
        if (PlayMods.netherCoords) {
            draw(graphics, minecraft, PlayMods.netherX, PlayMods.netherY, PlayMods.netherScale, ExtraInfo.nether(minecraft, player), false);
        }
        if (PlayMods.compass) {
            draw(graphics, minecraft, PlayMods.compassX, PlayMods.compassY, PlayMods.compassScale, ExtraInfo.compass(player), false);
        }
        if (PlayMods.biome) {
            draw(graphics, minecraft, PlayMods.biomeX, PlayMods.biomeY, PlayMods.biomeScale, ExtraInfo.biome(minecraft, player), false);
        }
        if (PlayMods.clock) {
            draw(graphics, minecraft, PlayMods.clockX, PlayMods.clockY, PlayMods.clockScale, ExtraInfo.clock(minecraft), false);
        }
        if (PlayMods.speedometer) {
            draw(graphics, minecraft, PlayMods.speedX, PlayMods.speedY, PlayMods.speedScale, ExtraInfo.speed(player), false);
        }
        if (PlayMods.memory) {
            draw(graphics, minecraft, PlayMods.memoryX, PlayMods.memoryY, PlayMods.memoryScale, ExtraInfo.memory(), false);
        }
        if (PlayMods.serverIp) {
            draw(graphics, minecraft, PlayMods.serverX, PlayMods.serverY, PlayMods.serverScale, ExtraInfo.server(minecraft), false);
        }
        if (PlayMods.potionHud) {
            drawLines(graphics, minecraft, PlayMods.potionX, PlayMods.potionY, PlayMods.potionScale, ExtraInfo.potions(player), false);
        }
        if (PlayMods.saturation) {
            draw(graphics, minecraft, PlayMods.satX, PlayMods.satY, PlayMods.satScale, ExtraInfo.saturation(player), false);
        }
        if (PlayMods.dayCounter) {
            draw(graphics, minecraft, PlayMods.dayX, PlayMods.dayY, PlayMods.dayScale, ExtraInfo.day(minecraft), false);
        }
    }

    static void drawCoords(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.coords(minecraft.player), editing);
    }

    static void drawNether(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.nether(minecraft, minecraft.player), editing);
    }

    static void drawCompass(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.compass(minecraft.player), editing);
    }

    static void drawBiome(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.biome(minecraft, minecraft.player), editing);
    }

    static void drawClock(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.clock(minecraft), editing);
    }

    static void drawSpeed(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.speed(minecraft.player), editing);
    }

    static void drawMemory(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.memory(), editing);
    }

    static void drawServer(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.server(minecraft), editing);
    }

    static void drawPotions(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        drawLines(graphics, minecraft, x, y, 1.0f, ExtraInfo.potions(minecraft.player), editing);
    }

    static void drawSat(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.saturation(minecraft.player), editing);
    }

    static void drawDay(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        pill(graphics, minecraft, x, y, ExtraInfo.day(minecraft), editing);
    }

    static int width(Minecraft minecraft, String text) {
        return Math.max(48, minecraft.font.width(text) + 12);
    }

    static int potionWidth(Minecraft minecraft) {
        int w = 64;
        for (String line : ExtraInfo.potions(minecraft.player)) {
            w = Math.max(w, minecraft.font.width(line) + 12);
        }
        return w;
    }

    static int potionHeight(Minecraft minecraft) {
        return Math.max(16, ExtraInfo.potions(minecraft.player).size() * 16);
    }

    private static boolean hidden(Minecraft minecraft) {
        Screen screen = minecraft.screen;
        return screen instanceof TidalPanelScreen || screen instanceof RadialScreen || screen instanceof LayoutMenuScreen;
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, int storedX, int storedY, float scale, String text, boolean editing) {
        int w = width(minecraft, text);
        int x = storedX < 0 ? Math.max(8, minecraft.getWindow().getGuiScaledWidth() - w - 8) : storedX;
        HudDraw.scaled(graphics, x, storedY, scale, () -> pill(graphics, minecraft, 0, 0, text, editing));
    }

    private static void drawLines(GuiGraphics graphics, Minecraft minecraft, int storedX, int storedY, float scale, List<String> lines, boolean editing) {
        int w = 64;
        for (String line : lines) {
            w = Math.max(w, minecraft.font.width(line) + 12);
        }
        int x = storedX < 0 ? Math.max(8, minecraft.getWindow().getGuiScaledWidth() - w - 8) : storedX;
        int width = w;
        HudDraw.scaled(graphics, x, storedY, scale, () -> {
            int y = 0;
            for (String line : lines) {
                pill(graphics, minecraft, 0, y, line, editing, width);
                y += 16;
            }
        });
    }

    private static void pill(GuiGraphics graphics, Minecraft minecraft, int x, int y, String text, boolean editing) {
        pill(graphics, minecraft, x, y, text, editing, width(minecraft, text));
    }

    private static void pill(GuiGraphics graphics, Minecraft minecraft, int x, int y, String text, boolean editing, int w) {
        int h = 16;
        if (editing) {
            Glass.roundedFill(graphics, x - 3, y - 3, x + w + 3, y + h + 3, 8, 0x330349FC);
        }
        Glass.roundedFill(graphics, x, y, x + w, y + h, 6, 0x99000000);
        graphics.drawCenteredString(minecraft.font, text, x + w / 2, y + 4, Glass.TEXT);
    }
}
