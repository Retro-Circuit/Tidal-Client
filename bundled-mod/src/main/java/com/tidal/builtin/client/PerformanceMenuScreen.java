package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;

public class PerformanceMenuScreen extends TidalPanelScreen {
    public PerformanceMenuScreen() {
        super("Performance");
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
    protected int bodyContentHeight() {
        return 80 + TidalBoost.passes().size() * 38 + TidalBoost.companions().size() * 18;
    }

    @Override
    protected void drawBody(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int y = this.scrolledY();
        y = this.section(graphics, y, "Tidal Boost");
        Glass.row(graphics, this.bodyX, y, this.bodyW, 36, false);
        graphics.text(this.font, "Master", this.bodyX + 12, y + 8, Glass.TEXT, false);
        graphics.text(this.font, "Auto-tunes around Sodium, Iris, and friends", this.bodyX + 12, y + 20, Glass.MUTE, false);
        Glass.toggle(graphics, this.bodyX + this.bodyW - 42, y + 10, TidalMods.boost);
        y += 50;
        y = this.section(graphics, y, "Active passes");
        for (TidalBoost.Pass pass : TidalBoost.passes()) {
            Glass.row(graphics, this.bodyX, y, this.bodyW, 32, false);
            graphics.text(this.font, this.fit(pass.title(), this.bodyW - 24), this.bodyX + 12, y + 6, Glass.TEXT, false);
            graphics.text(this.font, this.fit(pass.hint(), this.bodyW - 24), this.bodyX + 12, y + 17, Glass.MUTE, false);
            y += 38;
        }
        y = this.section(graphics, y, "Detected");
        List<TidalBoost.Companion> companions = TidalBoost.companions();
        if (companions.isEmpty()) {
            graphics.text(this.font, "Vanilla renderer — Tidal covers particles, clouds, weather, entities, FPS.", this.bodyX + 4, y + 6, Glass.MUTE, false);
            return;
        }
        for (TidalBoost.Companion companion : companions) {
            graphics.text(this.font, companion.name() + "  ·  Tidal yields overlapping work", this.bodyX + 4, y + 2, Glass.TEXT, false);
            y += 16;
        }
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        int y = this.scrolledY() + 18;
        if (in(mouseX, mouseY, this.bodyX, y, this.bodyX + this.bodyW, y + 36)) {
            TidalMods.boost = !TidalMods.boost;
            TidalMods.save();
            return true;
        }
        return false;
    }
}
