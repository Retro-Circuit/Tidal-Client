package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class InstanceModScreen extends TidalPanelScreen {
    private final InstanceMods.Entry entry;

    public InstanceModScreen(InstanceMods.Entry entry) {
        super(entry.name());
        this.entry = entry;
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
        graphics.text(this.font, this.entry.id() + "  " + this.entry.version(), this.bodyX, this.bodyY, Glass.MUTE, false);
        String description = this.entry.description().isBlank() ? "No description." : this.entry.description();
        int y = this.bodyY + 16;
        for (String line : wrap(description, 42)) {
            if (y > this.bodyY + this.bodyH - 12) {
                break;
            }
            graphics.text(this.font, line, this.bodyX, y, Glass.TEXT, false);
            y += 12;
        }
        graphics.text(this.font, "Settings for this mod are in the mod itself.", this.bodyX, this.bodyY + this.bodyH - 14, Glass.MUTE, false);
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        return false;
    }

    private static java.util.List<String> wrap(String text, int width) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String remaining = text.replace('\n', ' ');
        while (!remaining.isEmpty()) {
            if (remaining.length() <= width) {
                lines.add(remaining);
                break;
            }
            int cut = remaining.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width;
            }
            lines.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        return lines;
    }
}
