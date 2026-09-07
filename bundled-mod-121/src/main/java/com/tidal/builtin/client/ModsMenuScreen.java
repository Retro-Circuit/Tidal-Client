package com.tidal.builtin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModsMenuScreen extends TidalPanelScreen {
    private static final int ROW = 36;
    private static final String[] TABS = {"PvP", "HUD", "Utility", "Visual", "Performance", "Instance"};
    private int tab;

    public ModsMenuScreen() {
        super("Mods");
    }

    @Override
    protected int preferredHeight() {
        return 420;
    }

    @Override
    protected int bodyContentHeight() {
        if (this.tab == 5) {
            int extra = InstanceMods.list().size();
            return 8 + Math.max(1, extra) * ROW;
        }
        return 8 + this.mods().size() * ROW;
    }

    @Override
    protected void drawBody(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int sideW = this.sideW();
        Glass.roundedFill(graphics, this.bodyX, this.bodyY, this.bodyX + sideW, this.bodyY + this.bodyH, 16, 0x28000000);
        Glass.roundedFill(graphics, this.bodyX + sideW + 10, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH, 16, 0x14000000);
        for (int i = 0; i < TABS.length; i++) {
            int y = this.bodyY + 10 + i * 28;
            boolean on = this.tab == i;
            if (on) {
                Glass.roundedFill(graphics, this.bodyX + 8, y, this.bodyX + sideW - 8, y + 22, 11, 0x660349FC);
            }
            graphics.drawString(this.font, TABS[i], this.bodyX + 16, y + 7, on ? Glass.TEXT : Glass.MUTE, false);
        }
        int contentX = this.bodyX + sideW + 18;
        int contentW = this.bodyW - sideW - 28;
        int y = this.bodyY + 8 - this.bodyScroll;
        if (this.tab == 5) {
            List<InstanceMods.Entry> extras = InstanceMods.list();
            if (extras.isEmpty()) {
                graphics.drawString(this.font, "No other mods in this instance.", contentX + 4, y + 10, Glass.MUTE, false);
                return;
            }
            for (InstanceMods.Entry entry : extras) {
                this.instanceRow(graphics, contentX, contentW, y, entry);
                y += ROW;
            }
            return;
        }
        int color = this.tab * 3;
        for (Mod mod : this.mods()) {
            this.modRow(graphics, contentX, contentW, y, mod, color++);
            y += ROW;
        }
    }

    private List<Mod> mods() {
        List<Mod> list = new ArrayList<>();
        switch (this.tab) {
            case 0 -> {
                list.add(mod("CPS", "Clicks per second", () -> TidalMods.cps, v -> TidalMods.cps = v, () -> new ToggleModScreen("CPS", "Enable CPS", () -> TidalMods.cps, v -> TidalMods.cps = v)));
                list.add(mod("Combo Counter", "Hits before you take damage", () -> TidalMods.combo, v -> TidalMods.combo = v, () -> new ToggleModScreen("Combo Counter", "Enable combo counter", () -> TidalMods.combo, v -> TidalMods.combo = v)));
                list.add(mod("Custom Particles", "Extra crit sparkles on hit", () -> TidalMods.customParticles, v -> TidalMods.customParticles = v, () -> new ToggleModScreen("Custom Particles", "Enable custom particles", () -> TidalMods.customParticles, v -> TidalMods.customParticles = v)));
                list.add(mod("No Hurt Cam", "Disable the damage camera shake", () -> TidalMods.noHurtCam, v -> TidalMods.noHurtCam = v, () -> new ToggleModScreen("No Hurt Cam", "Enable no hurt cam", () -> TidalMods.noHurtCam, v -> TidalMods.noHurtCam = v)));
            }
            case 1 -> {
                list.add(mod("Keystrokes", "WASD and click indicators", () -> TidalMods.keystrokes, v -> TidalMods.keystrokes = v, KeystrokesMenuScreen::new));
                list.add(mod("Armor Status", "Durability, icons, and armor value", () -> TidalMods.armor, v -> TidalMods.armor = v, ArmorMenuScreen::new));
                list.add(mod("FPS & Ping", "Frames and server latency", () -> TidalMods.fpsPing, v -> TidalMods.fpsPing = v, () -> new ToggleModScreen("FPS & Ping", "Enable FPS and ping", () -> TidalMods.fpsPing, v -> TidalMods.fpsPing = v)));
            }
            case 2 -> {
                list.add(mod("Auto GG", "Sends gg when you die or get a kill", () -> TidalMods.autoGg, v -> TidalMods.autoGg = v, () -> new ToggleModScreen("Auto GG", "Enable auto GG", () -> TidalMods.autoGg, v -> TidalMods.autoGg = v)));
                list.add(mod("Streamer Mode", "Hides names as the first four letters plus …", () -> TidalMods.streamerMode, v -> TidalMods.streamerMode = v, () -> new ToggleModScreen("Streamer Mode", "Enable streamer mode", () -> TidalMods.streamerMode, v -> TidalMods.streamerMode = v)));
                list.add(mod("Bedrock Detector", "Badge next to Geyser/Floodgate players", () -> TidalMods.bedrockDetect, v -> TidalMods.bedrockDetect = v, () -> new ToggleModScreen("Bedrock Detector", "Enable Bedrock detector", () -> TidalMods.bedrockDetect, v -> TidalMods.bedrockDetect = v)));
            }
            case 3 -> list.add(mod("Low Fire", "Lower first-person fire overlay", () -> TidalMods.lowFire, v -> TidalMods.lowFire = v, () -> new ToggleModScreen("Low Fire", "Enable low fire", () -> TidalMods.lowFire, v -> TidalMods.lowFire = v)));
            default -> list.add(mod("Tidal Boost", companionsHint(), () -> TidalMods.boost, v -> TidalMods.boost = v, PerformanceMenuScreen::new));
        }
        return list;
    }

    private void modRow(GuiGraphics graphics, int x, int w, int y, Mod mod, int color) {
        Glass.row(graphics, x, y, w, 32, false);
        this.modIcon(graphics, x + 8, y + 8, color);
        graphics.drawString(this.font, this.fit(mod.title, w - 110), x + 32, y + 6, Glass.TEXT, false);
        graphics.drawString(this.font, this.fit(mod.hint, w - 110), x + 32, y + 17, Glass.MUTE, false);
        this.gear(graphics, x + w - 70, y + 8);
        Glass.toggle(graphics, x + w - 42, y + 8, mod.get.getAsBoolean());
    }

    private void instanceRow(GuiGraphics graphics, int x, int w, int y, InstanceMods.Entry entry) {
        Glass.row(graphics, x, y, w, 32, false);
        Glass.roundedFill(graphics, x + 8, y + 8, x + 24, y + 24, 4, 0x330349FC);
        graphics.drawString(this.font, this.fit(entry.name(), w - 80), x + 32, y + 6, Glass.TEXT, false);
        graphics.drawString(this.font, this.fit(entry.version(), w - 80), x + 32, y + 17, Glass.MUTE, false);
        this.gear(graphics, x + w - 42, y + 8);
    }

    private void modIcon(GuiGraphics graphics, int x, int y, int index) {
        Glass.roundedFill(graphics, x, y, x + 16, y + 16, 4, 0x33000000);
        int color = switch (Math.floorMod(index, 8)) {
            case 0 -> Glass.ACCENT;
            case 1 -> 0xFF4EA3FF;
            case 2 -> 0xFFE8C547;
            case 3 -> 0xFF3DDC84;
            case 4 -> 0xFFFF7A59;
            case 5 -> 0xFFFF8A3D;
            case 6 -> 0xFFC47CFF;
            default -> 0xFF7AD0FF;
        };
        Glass.roundedFill(graphics, x + 4, y + 4, x + 12, y + 12, 3, color);
    }

    private void gear(GuiGraphics graphics, int x, int y) {
        Glass.fillCircle(graphics, x + 8, y + 8, 8, 0x22FFFFFF);
        Glass.fillCircle(graphics, x + 8, y + 8, 3, Glass.MUTE);
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        int sideW = this.sideW();
        for (int i = 0; i < TABS.length; i++) {
            int y = this.bodyY + 10 + i * 28;
            if (in(mouseX, mouseY, this.bodyX + 8, y, this.bodyX + sideW - 8, y + 22)) {
                this.tab = i;
                this.bodyScroll = 0;
                return true;
            }
        }
        int contentX = this.bodyX + sideW + 18;
        int contentW = this.bodyW - sideW - 28;
        int y = this.bodyY + 8 - this.bodyScroll;
        if (this.tab == 5) {
            for (InstanceMods.Entry entry : InstanceMods.list()) {
                if (in(mouseX, mouseY, contentX, y, contentX + contentW, y + 32) && this.minecraft != null) {
                    this.minecraft.setScreen(new InstanceModScreen(entry));
                    return true;
                }
                y += ROW;
            }
            return false;
        }
        for (Mod mod : this.mods()) {
            if (in(mouseX, mouseY, contentX + contentW - 42, y + 6, contentX + contentW - 10, y + 28)) {
                mod.set.accept(!mod.get.getAsBoolean());
                TidalMods.save();
                return true;
            }
            if (in(mouseX, mouseY, contentX, y, contentX + contentW, y + 32) && this.minecraft != null) {
                this.minecraft.setScreen(mod.settings.get());
                return true;
            }
            y += ROW;
        }
        return false;
    }

    private static String companionsHint() {
        List<TidalBoost.Companion> companions = TidalBoost.companions();
        if (companions.isEmpty()) {
            return "Fills gaps when Sodium and friends are missing";
        }
        return "Works with " + companions.get(0).name() + (companions.size() > 1 ? " +" + (companions.size() - 1) : "");
    }

    private static Mod mod(String title, String hint, BooleanSupplier get, Consumer<Boolean> set, Supplier<Screen> settings) {
        return new Mod(title, hint, get, set, settings);
    }

    private record Mod(String title, String hint, BooleanSupplier get, Consumer<Boolean> set, Supplier<Screen> settings) {}
}
