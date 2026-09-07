package com.tidal.builtin.client;

import net.minecraft.client.gui.GuiGraphics;

public final class Glass {
    static final int SCRIM = 0x99080A10;
    static final int PANEL = 0xE812141C;
    static final int PANEL_EDGE = 0x3A0349FC;
    static final int ROW = 0x66181C24;
    static final int ROW_HOT = 0x8A1E2740;
    static final int ACCENT = 0xFF0349FC;
    static final int ON = 0xFF3DDC84;
    static final int OFF = 0xFFE05C5C;
    static final int TEXT = 0xFFF7F8FA;
    static final int MUTE = 0xFF9AA3B2;
    static final int RING = 0xD012141C;
    static final int RING_HOT = 0xE00349FC;
    static final int RING_SHEEN = 0x55FFFFFF;
    static final int RING_SHEEN_HOT = 0xCCFFFFFF;

    private Glass() {}

    static void scrim(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, SCRIM);
    }

    static void panel(GuiGraphics graphics, int x, int y, int w, int h, int radius) {
        roundedFill(graphics, x - 2, y - 2, x + w + 2, y + h + 2, radius + 2, PANEL_EDGE);
        roundedFill(graphics, x, y, x + w, y + h, radius, PANEL);
        roundedFill(graphics, x, y, x + 3, y + h, 0, ACCENT);
        roundedFill(graphics, x + 12, y + 1, x + w - 12, y + 3, 0, 0x18FFFFFF);
    }

    public static void logo(GuiGraphics graphics, int x, int y, int size) {
        GuiTextures.ensure(net.minecraft.client.Minecraft.getInstance());
        graphics.blit(GuiTextures.LOGO, x, y, 0, 0, size, size, size, size);
    }

    static void row(GuiGraphics graphics, int x, int y, int w, int h, boolean hot) {
        roundedFill(graphics, x, y, x + w, y + h, h / 2 > 14 ? 14 : Math.max(8, h / 2), hot ? ROW_HOT : ROW);
    }

    static void roundedFill(GuiGraphics graphics, int x0, int y0, int x1, int y1, int radius, int color) {
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r <= 0) {
            graphics.fill(x0, y0, x1, y1, color);
            return;
        }
        for (int y = 0; y < h; y++) {
            int inset = 0;
            if (y < r) {
                inset = r - isqrt(r * r - (r - y) * (r - y));
            } else if (y >= h - r) {
                int dy = y - (h - r - 1);
                inset = r - isqrt(r * r - (r - dy) * (r - dy));
            }
            graphics.fill(x0 + inset, y0 + y, x1 - inset, y0 + y + 1, color);
        }
    }

    static void pill(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        roundedFill(graphics, x, y, x + w, y + h, h / 2, color);
    }

    static void toggle(GuiGraphics graphics, int x, int y, boolean on) {
        int w = 34;
        int h = 18;
        pill(graphics, x, y, w, h, on ? ON : OFF);
        int knob = on ? x + w - h + 1 : x + 1;
        fillCircle(graphics, knob + h / 2, y + h / 2, h / 2 - 3, 0xFFFFFFFF);
    }

    static void scrollbar(GuiGraphics graphics, int x, int y, int h, int scroll, int maxScroll) {
        if (maxScroll <= 0 || h <= 8) {
            return;
        }
        roundedFill(graphics, x, y, x + 3, y + h, 2, 0x22000000);
        int thumb = Math.max(16, h * h / (h + maxScroll));
        int travel = h - thumb;
        int ty = y + (int) (travel * (scroll / (float) maxScroll));
        roundedFill(graphics, x, ty, x + 3, ty + thumb, 2, 0x88FFFFFF);
    }

    static void fillCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int iy = -radius; iy <= radius; iy++) {
            int span = isqrt(radius * radius - iy * iy);
            graphics.fill(cx - span, cy + iy, cx + span + 1, cy + iy + 1, color);
        }
    }

    static void fillAnnulusSector(
        GuiGraphics graphics,
        int cx,
        int cy,
        int inner,
        int outer,
        float start,
        float end,
        int color
    ) {
        int innerSq = inner * inner;
        int outerSq = outer * outer;
        for (int y = -outer + 1; y <= outer - 1; y++) {
            int outerX = isqrt(outerSq - y * y);
            if (outerX <= 1) {
                continue;
            }
            int runStart = Integer.MIN_VALUE;
            for (int x = -outerX; x <= outerX; x++) {
                int dist = x * x + y * y;
                boolean inside = dist >= innerSq && RadialMath.inArc((float) Math.atan2(y, x), start, end);
                if (inside) {
                    if (runStart == Integer.MIN_VALUE) {
                        runStart = x;
                    }
                } else if (runStart != Integer.MIN_VALUE) {
                    if (x - runStart > 1) {
                        graphics.fill(cx + runStart, cy + y, cx + x, cy + y + 1, color);
                    }
                    runStart = Integer.MIN_VALUE;
                }
            }
            if (runStart != Integer.MIN_VALUE && outerX + 1 - runStart > 1) {
                graphics.fill(cx + runStart, cy + y, cx + outerX + 1, cy + y + 1, color);
            }
        }
    }

    private static int isqrt(int value) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.floor(Math.sqrt(value));
    }
}
