package com.tidal.builtin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Glass {
    static final int SCRIM = 0x72000000;
    static final int PANEL = 0xD2101218;
    static final int PANEL_EDGE = 0x28FFFFFF;
    static final int ROW = 0x5A1A1E24;
    static final int ROW_HOT = 0x6A222830;
    static final int ACCENT = 0xFF0349FC;
    static final int ON = 0xFF3DDC84;
    static final int OFF = 0xFFE05C5C;
    static final int TEXT = 0xFFF4F6F8;
    static final int MUTE = 0xFF8B93A1;
    static final int RING = 0xC4101218;
    static final int RING_HOT = 0xD00349FC;
    static final int RING_SHEEN = 0x42FFFFFF;
    static final int RING_SHEEN_HOT = 0x99FFFFFF;

    private Glass() {}

    static void scrim(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.blurBeforeThisStratum();
        graphics.nextStratum();
        graphics.fill(0, 0, width, height, SCRIM);
    }

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius) {
        roundedFill(graphics, x - 1, y - 1, x + w + 1, y + h + 1, radius + 1, PANEL_EDGE);
        roundedFill(graphics, x, y, x + w, y + h, radius, PANEL);
        roundedFill(graphics, x + 8, y + 1, x + w - 8, y + 2, 0, 0x14FFFFFF);
    }

    public static void logo(GuiGraphicsExtractor graphics, int x, int y, int size) {
        GuiTextures.ensure(net.minecraft.client.Minecraft.getInstance());
        graphics.blit(GuiTextures.LOGO, x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    static void row(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean hot) {
        roundedFill(graphics, x, y, x + w, y + h, h / 2 > 14 ? 14 : Math.max(8, h / 2), hot ? ROW_HOT : ROW);
    }

    static void roundedFill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int radius, int color) {
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

    static void pill(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        roundedFill(graphics, x, y, x + w, y + h, h / 2, color);
    }

    static void toggle(GuiGraphicsExtractor graphics, int x, int y, boolean on) {
        int w = 30;
        int h = 16;
        pill(graphics, x, y, w, h, on ? ON : OFF);
        int knob = on ? x + w - h + 1 : x + 1;
        fillCircle(graphics, knob + h / 2, y + h / 2, h / 2 - 3, 0xFFFFFFFF);
    }

    static void scrollbar(GuiGraphicsExtractor graphics, int x, int y, int h, int scroll, int maxScroll) {
        if (maxScroll <= 0 || h <= 8) {
            return;
        }
        roundedFill(graphics, x, y, x + 3, y + h, 2, 0x22000000);
        int thumb = Math.max(16, h * h / (h + maxScroll));
        int travel = h - thumb;
        int ty = y + (int) (travel * (scroll / (float) maxScroll));
        roundedFill(graphics, x, ty, x + 3, ty + thumb, 2, 0x88FFFFFF);
    }

    static void fillCircle(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int iy = -radius; iy <= radius; iy++) {
            int span = isqrt(radius * radius - iy * iy);
            graphics.fill(cx - span, cy + iy, cx + span + 1, cy + iy + 1, color);
        }
    }

    static void fillAnnulusSector(
        GuiGraphicsExtractor graphics,
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
