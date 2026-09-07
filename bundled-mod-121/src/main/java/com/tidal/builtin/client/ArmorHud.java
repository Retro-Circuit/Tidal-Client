package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmorHud {
    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    static final boolean[] SHOWS = new boolean[4];

    private ArmorHud() {}

    static int width() {
        return TidalMods.armorVertical ? 52 : 16 * shownCount() + 6;
    }

    static int height() {
        int rows = TidalMods.armorVertical ? shownCount() : 1;
        if (TidalMods.armorPoints) {
            rows += TidalMods.armorVertical ? 1 : 0;
        }
        return TidalMods.armorVertical ? rows * 18 + 4 : 22;
    }

    private static int shownCount() {
        int n = 0;
        if (TidalMods.armorHelmet) n++;
        if (TidalMods.armorChest) n++;
        if (TidalMods.armorLegs) n++;
        if (TidalMods.armorBoots) n++;
        return Math.max(1, n);
    }

    static int x(int screenW) {
        return Math.max(0, TidalMods.armorX);
    }

    static int y() {
        return TidalMods.armorY < 0 ? 8 : TidalMods.armorY;
    }

    public static void extract(GuiGraphics graphics, Minecraft minecraft) {
        if (!TidalMods.armor || minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof TidalPanelScreen
            || minecraft.screen instanceof RadialScreen
            || minecraft.screen instanceof LayoutMenuScreen) {
            return;
        }
        HudDraw.scaled(
            graphics,
            x(minecraft.getWindow().getGuiScaledWidth()),
            y(),
            TidalMods.armorScale,
            () -> draw(graphics, minecraft, 0, 0, false)
        );
    }

    static void draw(GuiGraphics graphics, Minecraft minecraft, int x, int y, boolean editing) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        SHOWS[0] = TidalMods.armorHelmet;
        SHOWS[1] = TidalMods.armorChest;
        SHOWS[2] = TidalMods.armorLegs;
        SHOWS[3] = TidalMods.armorBoots;
        if (editing) {
            Glass.roundedFill(graphics, x - 3, y - 3, x + width() + 3, y + height() + 3, 10, 0x330349FC);
        }
        int cx = x;
        int cy = y;
        for (int i = 0; i < SLOTS.length; i++) {
            if (!SHOWS[i]) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(SLOTS[i]);
            boolean empty = stack.isEmpty();
            if (empty && !TidalMods.armorEmpty) {
                continue;
            }
            if (!empty && TidalMods.armorDamagedOnly && stack.getDamageValue() == 0) {
                continue;
            }
            if (TidalMods.armorIcons) {
                if (!empty) {
                    graphics.renderItem(stack, cx, cy);
                } else {
                    Glass.roundedFill(graphics, cx, cy, cx + 16, cy + 16, 4, 0x33000000);
                }
            }
            if (!empty && TidalMods.armorPercent) {
                int max = Math.max(1, stack.getMaxDamage());
                int pct = Math.round(100.0f * (max - stack.getDamageValue()) / max);
                int color = Glass.TEXT;
                if (TidalMods.armorColor) {
                    color = pct > 50 ? Glass.ON : pct > 20 ? 0xFFE8C547 : Glass.OFF;
                }
                graphics.drawString(minecraft.font, pct + "%", cx + (TidalMods.armorIcons ? 18 : 0), cy + 4, color, false);
            }
            if (TidalMods.armorVertical) {
                cy += 18;
            } else {
                cx += 18;
            }
        }
        if (TidalMods.armorPoints) {
            graphics.drawString(minecraft.font, String.valueOf(player.getArmorValue()), x, TidalMods.armorVertical ? cy : y + 16, Glass.MUTE, false);
        }
    }
}
