package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class TidalPanelScreen extends Screen {
    private final String heading;
    protected int panelX;
    protected int panelY;
    protected int panelW;
    protected int panelH;
    protected int bodyX;
    protected int bodyY;
    protected int bodyW;
    protected int bodyH;
    protected int bodyScroll;

    protected TidalPanelScreen(String heading) {
        super(Component.literal(heading));
        this.heading = heading;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    protected void layoutPanel() {
        this.panelW = Math.min(620, Math.max(460, this.width - 120));
        this.panelH = Math.min(this.preferredHeight(), Math.max(280, this.height - 56));
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.bodyX = this.panelX + 22;
        this.bodyY = this.panelY + 54;
        this.bodyW = this.panelW - 44;
        this.bodyH = this.panelH - 96;
        this.clampScroll();
    }

    protected int preferredHeight() {
        return 340;
    }

    protected int bodyContentHeight() {
        return this.bodyH;
    }

    protected int scrolledY() {
        return this.bodyY - this.bodyScroll;
    }

    protected int maxScroll() {
        return Math.max(0, this.bodyContentHeight() - this.bodyH);
    }

    protected void clampScroll() {
        this.bodyScroll = Math.max(0, Math.min(this.maxScroll(), this.bodyScroll));
    }

    protected String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int width = this.font.width(ellipsis);
        String cut = text;
        while (cut.length() > 1 && this.font.width(cut) + width > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ellipsis;
    }

    protected int sideW() {
        return 128;
    }

    protected int section(GuiGraphics graphics, int y, String title) {
        graphics.drawString(this.font, title.toUpperCase(), this.bodyX + 2, y + 1, Glass.MUTE, false);
        Glass.roundedFill(graphics, this.bodyX, y + 12, this.bodyX + this.bodyW, y + 13, 0, 0x14FFFFFF);
        return y + 18;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.layoutPanel();
        Glass.scrim(graphics, this.width, this.height);
        Glass.panel(graphics, this.panelX, this.panelY, this.panelW, this.panelH, 18);
        Glass.logo(graphics, this.panelX + 18, this.panelY + 14, 22);
        graphics.drawString(this.font, this.heading, this.panelX + 48, this.panelY + 14, Glass.TEXT, false);
        graphics.drawString(this.font, "Tidal Client", this.panelX + 48, this.panelY + 26, Glass.MUTE, false);
        int closeX0 = this.panelX + this.panelW - 38;
        int closeY0 = this.panelY + 14;
        Glass.fillCircle(graphics, closeX0 + 9, closeY0 + 9, 9, 0x28FFFFFF);
        graphics.drawCenteredString(this.font, "x", closeX0 + 9, closeY0 + 5, Glass.MUTE);
        Glass.roundedFill(graphics, this.panelX + 18, this.panelY + 44, this.panelX + this.panelW - 18, this.panelY + 45, 0, 0x22FFFFFF);
        graphics.enableScissor(this.bodyX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH);
        this.drawBody(graphics, mouseX, mouseY, delta);
        graphics.disableScissor();
        if (this.maxScroll() > 0) {
            Glass.scrollbar(graphics, this.bodyX + this.bodyW - 4, this.bodyY, this.bodyH, this.bodyScroll, this.maxScroll());
        }
        int backX0 = this.panelX + this.panelW / 2 - 40;
        int backY0 = this.panelY + this.panelH - 32;
        Glass.pill(graphics, backX0, backY0, 80, 20, 0x24FFFFFF);
        graphics.drawCenteredString(this.font, "Back", backX0 + 40, backY0 + 6, Glass.TEXT);
        super.render(graphics, mouseX, mouseY, delta);
    }

    protected abstract void drawBody(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    protected abstract boolean clickBody(int mouseX, int mouseY);

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.layoutPanel();
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int closeX0 = this.panelX + this.panelW - 38;
        int closeY0 = this.panelY + 14;
        int backX0 = this.panelX + this.panelW / 2 - 40;
        int backY0 = this.panelY + this.panelH - 32;
        if (in(mx, my, closeX0, closeY0, closeX0 + 18, closeY0 + 18) || in(mx, my, backX0, backY0, backX0 + 80, backY0 + 20)) {
            this.onClose();
            return true;
        }
        if (in(mx, my, this.bodyX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH) && this.clickBody(mx, my)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.layoutPanel();
        if (this.maxScroll() <= 0 || !in((int) mouseX, (int) mouseY, this.bodyX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        this.bodyScroll = Math.max(0, Math.min(this.maxScroll(), this.bodyScroll - (int) Math.round(scrollY * 16)));
        return true;
    }

    protected static boolean in(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1;
    }

    protected Font font() {
        return this.font;
    }

    protected Minecraft client() {
        return this.minecraft;
    }
}
