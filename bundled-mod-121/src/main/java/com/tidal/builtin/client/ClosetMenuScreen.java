package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.List;

public class ClosetMenuScreen extends TidalPanelScreen {
    private static final int ROW = 42;
    private boolean capesTab;
    private int selected;
    private final PlayerPortrait portrait = new PlayerPortrait();

    public ClosetMenuScreen() {
        super("Skins");
        LocalSkins.scanLater();
        AccountCapes.refresh();
    }

    @Override
    public void onClose() {
        Capes.setPreview(null);
        LocalSkins.setPreview(null);
        super.onClose();
    }

    @Override
    protected int preferredHeight() {
        return 400;
    }

    @Override
    protected int bodyContentHeight() {
        return 28 + Math.max(1, this.rows()) * ROW;
    }

    private int rows() {
        return this.capesTab ? this.capeRows().size() : 2 + LocalSkins.list().size();
    }

    private List<CapeRow> capeRows() {
        List<CapeRow> rows = new ArrayList<>();
        rows.add(new CapeRow("", "None", "Hide cape", false));
        for (AccountCapes.Cape cape : AccountCapes.owned()) {
            rows.add(new CapeRow(cape.key(), cape.name(), cape.active() ? "Microsoft · Active" : "Microsoft account", true));
        }
        for (Capes.Item item : Capes.visibleItems()) {
            rows.add(new CapeRow(item.id, item.name, item.exclusive() ? "Tidal exclusive" : "Tidal", false));
        }
        return rows;
    }

    @Override
    protected void drawBody(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraft = this.client();
        if (minecraft != null) {
            Capes.ensure(minecraft);
            LocalSkins.ensure(minecraft);
        }
        int previewW = 140;
        int listW = Math.max(210, this.bodyW - previewW - 10);
        this.tab(graphics, this.bodyX, this.scrolledY(), 56, !this.capesTab, "Skins");
        this.tab(graphics, this.bodyX + 60, this.scrolledY(), 56, this.capesTab, "Capes");
        if (!this.capesTab) {
            this.tab(graphics, this.bodyX + listW - 64, this.scrolledY(), 64, false, LocalSkins.scanning() ? "…" : "Rescan");
        } else {
            graphics.drawString(this.font, this.fit(AccountCapes.status(), listW - 8), this.bodyX + 124, this.scrolledY() + 5, Glass.MUTE, false);
        }
        int y = this.scrolledY() + 24;
        int hover = -1;
        if (this.capesTab) {
            List<CapeRow> items = this.capeRows();
            for (int i = 0; i < items.size(); i++) {
                CapeRow item = items.get(i);
                boolean on = item.id.equals(TidalMods.equippedCape) || (item.id.isBlank() && TidalMods.equippedCape.isBlank());
                boolean hot = in(mouseX, mouseY, this.bodyX, y, this.bodyX + listW, y + ROW - 4);
                if (hot) {
                    hover = i;
                }
                Glass.row(graphics, this.bodyX, y, listW, ROW - 6, hot || this.selected == i);
                if (item.microsoft && !item.id.isBlank()) {
                    AccountCapes.blit(graphics, item.id.substring(3), this.bodyX + 8, y + 2, 18, 28);
                } else if (!item.id.isBlank()) {
                    Capes.blitIcon(graphics, item.id, this.bodyX + 8, y + 2, 18, 28);
                } else {
                    Glass.roundedFill(graphics, this.bodyX + 8, y + 6, this.bodyX + 26, y + 28, 4, 0x33FFFFFF);
                }
                graphics.drawString(this.font, this.fit(item.name, listW - 80), this.bodyX + 34, y + 8, Glass.TEXT, false);
                graphics.drawString(this.font, on ? "Equipped" : item.hint, this.bodyX + 34, y + 20, Glass.MUTE, false);
                y += ROW;
            }
            if (hover >= 0) {
                this.selected = hover;
            }
            String capeId = items.isEmpty() ? null : items.get(Math.min(this.selected, Math.max(0, items.size() - 1))).id;
            Capes.setPreview(capeId == null || capeId.isBlank() ? null : capeId);
            LocalSkins.setPreview(null);
        } else {
            boolean defHot = in(mouseX, mouseY, this.bodyX, y, this.bodyX + listW, y + ROW - 4);
            boolean defOn = TidalMods.equippedSkin.isBlank();
            Glass.row(graphics, this.bodyX, y, listW, ROW - 6, defHot || this.selected == 0);
            graphics.drawString(this.font, "Minecraft account", this.bodyX + 12, y + 8, Glass.TEXT, false);
            graphics.drawString(this.font, defOn ? "Equipped" : "Default skin", this.bodyX + 12, y + 20, Glass.MUTE, false);
            if (defHot) {
                hover = 0;
            }
            y += ROW;
            Glass.row(graphics, this.bodyX, y, listW, ROW - 6, false);
            graphics.drawString(this.font, LocalSkins.status(), this.bodyX + 12, y + 12, Glass.MUTE, false);
            y += ROW;
            List<LocalSkins.Entry> skins = LocalSkins.list();
            for (int i = 0; i < skins.size(); i++) {
                LocalSkins.Entry entry = skins.get(i);
                boolean on = entry.key().equals(TidalMods.equippedSkin);
                boolean hot = in(mouseX, mouseY, this.bodyX, y, this.bodyX + listW, y + ROW - 4);
                if (hot) {
                    hover = i + 1;
                }
                Glass.row(graphics, this.bodyX, y, listW, ROW - 6, hot || this.selected == i + 1);
                LocalSkins.blit(graphics, entry.key(), this.bodyX + 8, y + 8, 20);
                graphics.drawString(this.font, this.fit(entry.name(), listW - 80), this.bodyX + 36, y + 8, Glass.TEXT, false);
                graphics.drawString(this.font, on ? "Equipped" : entry.slim() ? "Alex" : "Steve", this.bodyX + 36, y + 20, Glass.MUTE, false);
                y += ROW;
            }
            if (hover >= 0) {
                this.selected = hover;
            }
            Capes.setPreview(null);
            if (hover < 0) {
                LocalSkins.setPreview(null);
            } else if (hover == 0) {
                LocalSkins.setPreview("");
            } else if (hover - 1 < skins.size()) {
                LocalSkins.setPreview(skins.get(hover - 1).key());
            }
        }
        int previewX = this.bodyX + listW + 8;
        this.drawPreview(graphics, mouseX, mouseY, previewX);
    }

    private void tab(GuiGraphics graphics, int x, int y, int w, boolean on, String label) {
        Glass.pill(graphics, x, y, w, 18, on ? 0x660349FC : 0x33000000);
        graphics.drawCenteredString(this.font, label, x + w / 2, y + 5, on ? Glass.TEXT : Glass.MUTE);
    }

    private void drawPreview(GuiGraphics graphics, int mouseX, int mouseY, int previewX) {
        int x0 = previewX + 4;
        int y0 = this.bodyY + 6;
        int x1 = this.bodyX + this.bodyW - 4;
        int y1 = this.bodyY + this.bodyH - 6;
        Glass.roundedFill(graphics, previewX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH, 16, 0x22000000);
        LivingEntity player = this.minecraft == null ? null : this.minecraft.player;
        graphics.disableScissor();
        graphics.enableScissor(x0, y0, x1, y1);
        if (player != null) {
            int scale = Math.max(28, Math.min(52, (y1 - y0) / 3));
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                x0,
                y0,
                x1,
                y1,
                scale,
                0.0625f,
                mouseX,
                mouseY,
                player
            );
        } else if (this.minecraft != null) {
            this.portrait.drawBody(graphics, this.minecraft, x0, y0, x1, y1, mouseX, mouseY);
        }
        graphics.disableScissor();
        graphics.enableScissor(this.bodyX, this.bodyY, this.bodyX + this.bodyW, this.bodyY + this.bodyH);
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        int previewW = 140;
        int listW = Math.max(210, this.bodyW - previewW - 10);
        int tabY = this.scrolledY();
        if (in(mouseX, mouseY, this.bodyX, tabY, this.bodyX + 56, tabY + 18)) {
            this.capesTab = false;
            this.selected = 0;
            return true;
        }
        if (in(mouseX, mouseY, this.bodyX + 60, tabY, this.bodyX + 116, tabY + 18)) {
            this.capesTab = true;
            this.selected = 0;
            return true;
        }
        if (!this.capesTab && in(mouseX, mouseY, this.bodyX + listW - 64, tabY, this.bodyX + listW, tabY + 18)) {
            LocalSkins.scanLater();
            return true;
        }
        int y = this.scrolledY() + 24;
        if (this.capesTab) {
            for (CapeRow item : this.capeRows()) {
                if (in(mouseX, mouseY, this.bodyX, y, this.bodyX + listW, y + ROW - 4)) {
                    if (item.id.isBlank()) {
                        TidalMods.equip("");
                    } else if (item.id.equals(TidalMods.equippedCape)) {
                        TidalMods.equip("");
                    } else if (TidalMods.owns(item.id) || item.microsoft) {
                        TidalMods.equip(item.id);
                    }
                    return true;
                }
                y += ROW;
            }
            return false;
        }
        if (in(mouseX, mouseY, this.bodyX, y, this.bodyX + listW, y + ROW - 4)) {
            TidalMods.equipSkin("");
            LocalSkins.setPreview("");
            return true;
        }
        y += ROW * 2;
        for (LocalSkins.Entry entry : LocalSkins.list()) {
            if (in(mouseX, mouseY, this.bodyX, y, this.bodyX + listW, y + ROW - 4)) {
                TidalMods.equipSkin(entry.key().equals(TidalMods.equippedSkin) ? "" : entry.key());
                return true;
            }
            y += ROW;
        }
        return false;
    }

    private record CapeRow(String id, String name, String hint, boolean microsoft) {}
}
