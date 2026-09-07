package com.tidal.builtin;

import com.tidal.builtin.client.AccountCapes;
import com.tidal.builtin.client.CapeShare;
import com.tidal.builtin.client.Capes;
import com.tidal.builtin.client.HudRuntime;
import com.tidal.builtin.client.MenuKeys;
import com.tidal.builtin.client.PlayFeatures;
import com.tidal.builtin.client.LayoutMenuScreen;
import com.tidal.builtin.client.RadialScreen;
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
        return MenuKeys.isDown(minecraft);
    }

    public static void tick(Minecraft minecraft) {
        MenuKeys.ensureInstalled(minecraft);
        Capes.tick(minecraft);
        AccountCapes.tick(minecraft);
        CapeShare.tick(minecraft);
        HudRuntime.tick(minecraft);
        PlayFeatures.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        boolean down = menuDown(minecraft);
        boolean pressed = MenuKeys.consumePress() || (down && !menuHeld);
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
