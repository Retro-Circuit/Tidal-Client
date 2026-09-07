package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ToggleModScreen extends TidalPanelScreen {
    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ToggleModScreen(String title, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(title);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreen(new ModsMenuScreen());
            return;
        }
        super.onClose();
    }

    @Override
    protected void drawBody(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean on = this.getter.getAsBoolean();
        Glass.row(graphics, this.bodyX, this.bodyY, this.bodyW, 28, false);
        graphics.drawString(this.font, this.fit(this.label, this.bodyW - 58), this.bodyX + 12, this.bodyY + 10, Glass.TEXT, false);
        Glass.toggle(graphics, this.bodyX + this.bodyW - 42, this.bodyY + 6, on);
        graphics.drawString(this.font, "HUD modules can be moved and scaled in Layout.", this.bodyX + 4, this.bodyY + 40, Glass.MUTE, false);
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        if (in(mouseX, mouseY, this.bodyX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + 28)) {
            this.setter.accept(!this.getter.getAsBoolean());
            TidalMods.save();
            return true;
        }
        return false;
    }
}
