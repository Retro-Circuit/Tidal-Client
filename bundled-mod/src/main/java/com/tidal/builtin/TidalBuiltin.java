package com.tidal.builtin;

import net.fabricmc.api.ClientModInitializer;

public class TidalBuiltin implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TidalCompat.invokeRuntime("init");
    }

    public static void tick(Object minecraft) {
        TidalCompat.invokeRuntime("tick", minecraft);
    }
}
