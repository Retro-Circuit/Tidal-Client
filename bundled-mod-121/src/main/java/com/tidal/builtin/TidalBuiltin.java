package com.tidal.builtin;

import com.tidal.builtin.client.AccountCapes;
import com.tidal.builtin.client.CapeShare;
import com.tidal.builtin.client.Capes;
import com.tidal.builtin.client.HudRuntime;
import com.tidal.builtin.client.MenuKeys;
import com.tidal.builtin.client.PlayFeatures;
import com.tidal.builtin.client.LayoutMenuScreen;
import com.tidal.builtin.client.RadialScreen;
import com.tidal.builtin.client.SkinArchive;
import com.tidal.builtin.client.TidalMods;
import com.tidal.builtin.client.TidalPanelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class TidalBuiltin implements ClientModInitializer {
    private static boolean menuHeld;

    @Override
    public void onInitializeClient() {
        TidalMods.load();
        Capes.items();
        AccountCapes.refresh();
        CapeShare.listen();
    }

    public static boolean menuDown(Minecraft minecraft) {
        try {
            return MenuKeys.isDown(minecraft);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void tick(Minecraft minecraft) {
        try {
            MenuKeys.ensureInstalled(minecraft);
        } catch (Throwable ignored) {
        }
        try {
            Capes.tick(minecraft);
        } catch (Throwable ignored) {
        }
        try {
            AccountCapes.tick(minecraft);
        } catch (Throwable ignored) {
        }
        try {
            CapeShare.tick(minecraft);
        } catch (Throwable ignored) {
        }
        try {
            HudRuntime.tick(minecraft);
        } catch (Throwable ignored) {
        }
        try {
            PlayFeatures.tick(minecraft);
        } catch (Throwable ignored) {
        }
        try {
            SkinArchive.tick(minecraft);
        } catch (Throwable ignored) {
        }
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        boolean down = menuDown(minecraft);
        boolean pressed = false;
        try {
            pressed = MenuKeys.consumePress() || (down && !menuHeld);
        } catch (Throwable ignored) {
            pressed = down && !menuHeld;
        }
        menuHeld = down;

        Screen current = minecraft.screen;
        if (current instanceof RadialScreen || current instanceof TidalPanelScreen || current instanceof LayoutMenuScreen) {
            return;
        }
        if (current != null) {
            return;
        }
        if (pressed) {
            minecraft.setScreen(new RadialScreen());
        }
    }
}
