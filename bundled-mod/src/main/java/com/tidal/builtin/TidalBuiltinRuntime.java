package com.tidal.builtin;

import com.mojang.blaze3d.platform.InputConstants;
import com.tidal.builtin.client.AccountCapes;
import com.tidal.builtin.client.CapeShare;
import com.tidal.builtin.client.Capes;
import com.tidal.builtin.client.HudRuntime;
import com.tidal.builtin.client.LayoutMenuScreen;
import com.tidal.builtin.client.RadialScreen;
import com.tidal.builtin.client.TidalMods;
import com.tidal.builtin.client.TidalPanelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class TidalBuiltinRuntime {
    private static boolean menuHeld;

    private TidalBuiltinRuntime() {}

    public static void init() {
        TidalMods.load();
        Capes.items();
        AccountCapes.refresh();
        CapeShare.listen();
    }

    public static boolean menuDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), TidalMods.menuKey);
    }

    public static void tick(Object raw) {
        Minecraft minecraft = (Minecraft) raw;
        Capes.tick(minecraft);
        AccountCapes.tick(minecraft);
        CapeShare.tick(minecraft);
        HudRuntime.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        boolean down = menuDown(minecraft);
        boolean pressed = down && !menuHeld;
        menuHeld = down;

        Screen current = minecraft.gui.screen();
        if (current instanceof RadialScreen || current instanceof TidalPanelScreen || current instanceof LayoutMenuScreen) {
            return;
        }
        if (current != null) {
            return;
        }
        if (pressed) {
            minecraft.gui.setScreen(new RadialScreen());
        }
    }
}
