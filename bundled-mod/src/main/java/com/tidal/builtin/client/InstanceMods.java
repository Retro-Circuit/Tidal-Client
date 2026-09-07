package com.tidal.builtin.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import java.util.ArrayList;
import java.util.List;

final class InstanceMods {
    record Entry(String id, String name, String version, String description) {}

    private InstanceMods() {}

    static List<Entry> list() {
        List<Entry> entries = new ArrayList<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            if (container.getContainingMod().isPresent()) {
                continue;
            }
            ModMetadata meta = container.getMetadata();
            if ("builtin".equals(meta.getType())) {
                continue;
            }
            String id = meta.getId();
            if ("tidal-builtin".equals(id) || "fabricloader".equals(id) || "fabric-api".equals(id)) {
                continue;
            }
            String description = meta.getDescription() == null ? "" : meta.getDescription();
            entries.add(new Entry(id, meta.getName(), meta.getVersion().getFriendlyString(), description));
        }
        entries.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return entries;
    }
}
