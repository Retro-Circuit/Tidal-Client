package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesMenuScreen extends TidalPanelScreen {
    public KeystrokesMenuScreen() {
        super("Keystrokes");
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.gui.setScreen(new ModsMenuScreen());
            return;
        }
        super.onClose();
    }

    @Override
    protected void drawBody(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Glass.row(graphics, this.bodyX, this.bodyY, this.bodyW, 28, false);
        graphics.text(this.font, this.fit("Enable keystrokes", this.bodyW - 58), this.bodyX + 12, this.bodyY + 10, Glass.TEXT, false);
        Glass.toggle(graphics, this.bodyX + this.bodyW - 42, this.bodyY + 6, TidalMods.keystrokes);
        Glass.row(graphics, this.bodyX, this.bodyY + 32, this.bodyW, 28, false);
        graphics.text(this.font, this.fit("Info mode", this.bodyW - 58), this.bodyX + 12, this.bodyY + 42, Glass.TEXT, false);
        Glass.toggle(graphics, this.bodyX + this.bodyW - 42, this.bodyY + 38, TidalMods.keystrokesInfo);
        graphics.text(this.font, "Default: WASD, space, clicks. Info adds ESC, Q, F, E, Shift, Ctrl.", this.bodyX + 4, this.bodyY + 72, Glass.MUTE, false);
        graphics.text(this.font, "Move the widget in Layout.", this.bodyX + 4, this.bodyY + 86, Glass.MUTE, false);
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        if (in(mouseX, mouseY, this.bodyX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + 28)) {
            TidalMods.keystrokes = !TidalMods.keystrokes;
            TidalMods.save();
            return true;
        }
        if (in(mouseX, mouseY, this.bodyX, this.bodyY + 32, this.bodyX + this.bodyW, this.bodyY + 60)) {
            TidalMods.keystrokesInfo = !TidalMods.keystrokesInfo;
            TidalMods.save();
            return true;
        }
        return false;
    }
}
