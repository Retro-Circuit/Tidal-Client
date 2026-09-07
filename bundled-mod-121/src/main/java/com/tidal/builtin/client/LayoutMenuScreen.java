package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class LayoutMenuScreen extends Screen {
    private int dragging = -1;
    private int grabX;
    private int grabY;

    public LayoutMenuScreen() {
        super(Component.literal("Layout"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Glass.scrim(graphics, this.width, this.height);
        Glass.logo(graphics, 12, 12, 20);
        graphics.drawString(this.font, "Layout", 36, 17, Glass.TEXT, false);
        graphics.drawCenteredString(this.font, "Drag anywhere. Scroll while dragging to scale.", this.width / 2, 16, Glass.MUTE);
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            for (HudWidgets.Widget widget : HudWidgets.all()) {
                widget.render(graphics, minecraft);
            }
        }
        Glass.pill(graphics, this.width / 2 - 40, this.height - 32, 80, 20, Glass.ACCENT);
        graphics.drawCenteredString(this.font, "Done", this.width / 2, this.height - 26, Glass.TEXT);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (TidalPanelScreen.in(mx, my, this.width / 2 - 40, this.height - 32, this.width / 2 + 40, this.height - 12)) {
            TidalMods.save();
            this.onClose();
            return true;
        }
        HudWidgets.Widget[] widgets = HudWidgets.all();
        for (int i = widgets.length - 1; i >= 0; i--) {
            if (widgets[i].hit(mx, my)) {
                this.dragging = i;
                this.grabX = mx - widgets[i].x();
                this.grabY = my - widgets[i].y();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.dragging < 0) {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }
        HudWidgets.Widget widget = HudWidgets.all()[this.dragging];
        int mx = (int) mouseX;
        int my = (int) mouseY;
        widget.setX(Mth.clamp(mx - this.grabX, 8 - widget.w(), this.width - 8));
        widget.setY(Mth.clamp(my - this.grabY, 8 - widget.h(), this.height - 8));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.dragging >= 0 && scrollY != 0) {
            HudWidgets.Widget widget = HudWidgets.all()[this.dragging];
            float next = widget.scale() * (scrollY > 0 ? 1.08f : 0.92f);
            widget.setScale(next);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
