package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

public class SettingsMenuScreen extends TidalPanelScreen {
    private static final String[] SIDE = {"General", "Video", "Audio", "Keybinds", "Performance"};
    private int side;
    private boolean listening;

    public SettingsMenuScreen() {
        super("Settings");
    }

    @Override
    protected void drawBody(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int sideW = this.sideW();
        Glass.roundedFill(graphics, this.bodyX, this.bodyY, this.bodyX + sideW, this.bodyY + this.bodyH, 16, 0x28000000);
        Glass.roundedFill(graphics, this.bodyX + sideW + 10, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH, 16, 0x14000000);
        for (int i = 0; i < SIDE.length; i++) {
            int y = this.bodyY + 12 + i * 28;
            boolean on = this.side == i;
            if (on) {
                Glass.roundedFill(graphics, this.bodyX + 8, y, this.bodyX + sideW - 8, y + 22, 11, 0x660349FC);
            }
            graphics.drawString(this.font, SIDE[i], this.bodyX + 16, y + 7, on ? Glass.TEXT : Glass.MUTE, false);
        }
        Minecraft minecraft = this.client();
        if (minecraft == null) {
            return;
        }
        int contentX = this.bodyX + sideW + 20;
        int contentW = this.bodyW - sideW - 36;
        int rowY = this.bodyY + 12;
        int labelW = contentW - 54;
        if (this.side == 4) {
            this.drawPerformance(graphics, contentX, contentW, rowY);
            return;
        }
        if (this.side == 3) {
            boolean hot = in(mouseX, mouseY, contentX, rowY, contentX + contentW, rowY + 28);
            Glass.row(graphics, contentX, rowY, contentW, 28, hot || this.listening);
            graphics.drawString(this.font, this.fit("Tidal menu", labelW), contentX + 12, rowY + 10, Glass.TEXT, false);
            String bind = this.listening ? "Press a key..." : TidalMods.menuKeyName();
            graphics.drawString(this.font, this.fit(bind, 80), contentX + contentW - Math.min(80, this.font.width(bind)) - 12, rowY + 10, this.listening ? Glass.ACCENT : Glass.MUTE, false);
            return;
        }
        Options options = minecraft.options;
        String[] labels = this.labels();
        for (int i = 0; i < labels.length; i++) {
            boolean on = this.value(options, i);
            boolean hot = in(mouseX, mouseY, contentX, rowY, contentX + contentW, rowY + 28);
            Glass.row(graphics, contentX, rowY, contentW, 28, hot);
            graphics.drawString(this.font, this.fit(labels[i], labelW), contentX + 12, rowY + 10, Glass.TEXT, false);
            Glass.toggle(graphics, contentX + contentW - 42, rowY + 6, on);
            rowY += 32;
        }
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        int sideW = this.sideW();
        for (int i = 0; i < SIDE.length; i++) {
            int y = this.bodyY + 12 + i * 28;
            if (in(mouseX, mouseY, this.bodyX + 8, y, this.bodyX + sideW - 8, y + 22)) {
                this.side = i;
                this.listening = false;
                return true;
            }
        }
        Minecraft minecraft = this.client();
        if (minecraft == null) {
            return false;
        }
        int contentX = this.bodyX + sideW + 20;
        int contentW = this.bodyW - sideW - 36;
        int rowY = this.bodyY + 12;
        if (this.side == 4) {
            return this.clickPerformance(mouseX, mouseY, contentX, contentW, rowY);
        }
        if (this.side == 3) {
            if (in(mouseX, mouseY, contentX, rowY, contentX + contentW, rowY + 28)) {
                this.listening = true;
                return true;
            }
            this.listening = false;
            return false;
        }
        for (int i = 0; i < this.labels().length; i++) {
            if (in(mouseX, mouseY, contentX, rowY, contentX + contentW, rowY + 28)) {
                this.toggle(minecraft, i);
                return true;
            }
            rowY += 32;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (this.listening) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                TidalMods.menuKey = key;
                TidalMods.save();
            }
            this.listening = false;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void drawPerformance(GuiGraphics graphics, int x, int w, int y) {
        Glass.row(graphics, x, y, w, 32, false);
        graphics.drawString(this.font, "Tidal Boost", x + 12, y + 6, Glass.TEXT, false);
        graphics.drawString(this.font, "Auto-tunes around detected performance mods", x + 12, y + 17, Glass.MUTE, false);
        Glass.toggle(graphics, x + w - 42, y + 8, TidalMods.boost);
        y += 40;
        for (TidalBoost.Pass pass : TidalBoost.passes()) {
            graphics.drawString(this.font, this.fit((pass.active() ? "● " : "○ ") + pass.title(), w - 8), x + 4, y, Glass.TEXT, false);
            y += 12;
            graphics.drawString(this.font, this.fit(pass.hint(), w - 8), x + 4, y, Glass.MUTE, false);
            y += 16;
        }
        graphics.drawString(this.font, "DETECTED", x + 4, y + 4, Glass.MUTE, false);
        y += 18;
        var companions = TidalBoost.companions();
        if (companions.isEmpty()) {
            graphics.drawString(this.font, "None — Tidal covers the gaps.", x + 4, y + 6, Glass.MUTE, false);
            return;
        }
        for (TidalBoost.Companion companion : companions) {
            graphics.drawString(this.font, companion.name() + "  ·  active", x + 4, y + 2, Glass.TEXT, false);
            y += 16;
        }
    }

    private boolean clickPerformance(int mouseX, int mouseY, int x, int w, int y) {
        if (in(mouseX, mouseY, x, y, x + w, y + 32)) {
            TidalMods.boost = !TidalMods.boost;
            TidalMods.save();
            return true;
        }
        return false;
    }

    private String[] labels() {
        return switch (this.side) {
            case 1 -> new String[] {"VSync", "Smooth lighting", "Entity shadows"};
            case 2 -> new String[] {"Music", "Sounds"};
            default -> new String[] {"Fullscreen", "View bobbing", "Show subtitles"};
        };
    }

    private boolean value(Options options, int index) {
        return switch (this.side) {
            case 1 -> switch (index) {
                case 0 -> options.enableVsync().get();
                case 1 -> options.ambientOcclusion().get();
                default -> options.entityShadows().get();
            };
            case 2 -> switch (index) {
                case 0 -> options.getSoundSourceOptionInstance(SoundSource.MUSIC).get() > 0.01;
                default -> options.getSoundSourceOptionInstance(SoundSource.MASTER).get() > 0.01;
            };
            default -> switch (index) {
                case 0 -> options.fullscreen().get();
                case 1 -> options.bobView().get();
                default -> options.showSubtitles().get();
            };
        };
    }

    private void toggle(Minecraft minecraft, int index) {
        Options options = minecraft.options;
        switch (this.side) {
            case 1 -> {
                if (index == 0) {
                    flip(options.enableVsync());
                } else if (index == 1) {
                    flip(options.ambientOcclusion());
                } else {
                    flip(options.entityShadows());
                }
            }
            case 2 -> {
                SoundSource source = index == 0 ? SoundSource.MUSIC : SoundSource.MASTER;
                OptionInstance<Double> option = options.getSoundSourceOptionInstance(source);
                option.set(option.get() > 0.01 ? 0.0 : 1.0);
            }
            default -> {
                if (index == 0) {
                    boolean next = !options.fullscreen().get();
                    options.fullscreen().set(next);
                    if (minecraft.getWindow().isFullscreen() != next) {
                        minecraft.getWindow().toggleFullScreen();
                    }
                } else if (index == 1) {
                    flip(options.bobView());
                } else {
                    flip(options.showSubtitles());
                }
            }
        }
        options.save();
    }

    private static void flip(OptionInstance<Boolean> option) {
        option.set(!option.get());
    }
}
