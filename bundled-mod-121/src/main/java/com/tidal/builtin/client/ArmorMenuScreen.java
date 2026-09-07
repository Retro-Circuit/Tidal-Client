package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ArmorMenuScreen extends TidalPanelScreen {
    private static final String[] LABELS = {
        "Enable armor HUD",
        "Show helmet",
        "Show chestplate",
        "Show leggings",
        "Show boots",
        "Show item icons",
        "Show durability %",
        "Horizontal mode",
        "Show empty slots",
        "Color by durability",
        "Show armor points",
        "Damaged pieces only",
        "Show enchant glint"
    };

    public ArmorMenuScreen() {
        super("Armor HUD");
    }

    @Override
    protected int preferredHeight() {
        return 420;
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
    protected int bodyContentHeight() {
        return LABELS.length * 24;
    }

    @Override
    protected void drawBody(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int rowY = this.scrolledY();
        int labelW = this.bodyW - 58;
        for (int i = 0; i < LABELS.length; i++) {
            boolean on = value(i);
            Glass.row(graphics, this.bodyX, rowY, this.bodyW - 8, 22, false);
            graphics.drawString(this.font, this.fit(LABELS[i], labelW), this.bodyX + 12, rowY + 7, Glass.TEXT, false);
            Glass.toggle(graphics, this.bodyX + this.bodyW - 50, rowY + 3, on);
            rowY += 24;
        }
    }

    @Override
    protected boolean clickBody(int mouseX, int mouseY) {
        int rowY = this.scrolledY();
        for (int i = 0; i < LABELS.length; i++) {
            if (in(mouseX, mouseY, this.bodyX, rowY, this.bodyX + this.bodyW, rowY + 22)) {
                flip(i);
                TidalMods.save();
                return true;
            }
            rowY += 24;
        }
        return false;
    }

    private static boolean value(int i) {
        return switch (i) {
            case 0 -> TidalMods.armor;
            case 1 -> TidalMods.armorHelmet;
            case 2 -> TidalMods.armorChest;
            case 3 -> TidalMods.armorLegs;
            case 4 -> TidalMods.armorBoots;
            case 5 -> TidalMods.armorIcons;
            case 6 -> TidalMods.armorPercent;
            case 7 -> !TidalMods.armorVertical;
            case 8 -> TidalMods.armorEmpty;
            case 9 -> TidalMods.armorColor;
            case 10 -> TidalMods.armorPoints;
            case 11 -> TidalMods.armorDamagedOnly;
            default -> TidalMods.armorEnchants;
        };
    }

    private static void flip(int i) {
        switch (i) {
            case 0 -> TidalMods.armor = !TidalMods.armor;
            case 1 -> TidalMods.armorHelmet = !TidalMods.armorHelmet;
            case 2 -> TidalMods.armorChest = !TidalMods.armorChest;
            case 3 -> TidalMods.armorLegs = !TidalMods.armorLegs;
            case 4 -> TidalMods.armorBoots = !TidalMods.armorBoots;
            case 5 -> TidalMods.armorIcons = !TidalMods.armorIcons;
            case 6 -> TidalMods.armorPercent = !TidalMods.armorPercent;
            case 7 -> TidalMods.armorVertical = !TidalMods.armorVertical;
            case 8 -> TidalMods.armorEmpty = !TidalMods.armorEmpty;
            case 9 -> TidalMods.armorColor = !TidalMods.armorColor;
            case 10 -> TidalMods.armorPoints = !TidalMods.armorPoints;
            case 11 -> TidalMods.armorDamagedOnly = !TidalMods.armorDamagedOnly;
            default -> TidalMods.armorEnchants = !TidalMods.armorEnchants;
        }
    }
}
