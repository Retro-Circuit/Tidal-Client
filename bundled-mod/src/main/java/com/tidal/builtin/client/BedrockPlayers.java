package com.tidal.builtin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.util.UUID;

public final class BedrockPlayers {
    private BedrockPlayers() {}

    public static boolean enabled() {
        return TidalMods.bedrockDetect;
    }

    public static boolean isBedrock(PlayerInfo info) {
        return info != null && isBedrock(info.getProfile());
    }

    public static boolean isBedrock(GameProfile profile) {
        if (profile == null) {
            return false;
        }
        return isBedrock(profile.id(), profile.name());
    }

    public static boolean isBedrock(UUID uuid, String name) {
        if (name != null) {
            if (name.startsWith(".") || name.startsWith("*")) {
                return true;
            }
        }
        if (uuid == null) {
            return false;
        }
        if (uuid.version() == 0) {
            return true;
        }
        String id = uuid.toString();
        return id.startsWith("00000000-0000-0000-0009-") || id.startsWith("00000000-0000-0000-0000-");
    }

    public static boolean isBedrockName(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null || name == null || name.isBlank()) {
            return false;
        }
        PlayerInfo info = minecraft.getConnection().getPlayerInfoIgnoreCase(name);
        if (info != null) {
            return isBedrock(info);
        }
        for (PlayerInfo online : minecraft.getConnection().getOnlinePlayers()) {
            if (name.equalsIgnoreCase(online.getProfile().name())) {
                return isBedrock(online);
            }
        }
        return name.startsWith(".") || name.startsWith("*");
    }
}
