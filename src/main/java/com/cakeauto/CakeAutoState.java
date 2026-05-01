package com.cakeauto;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CakeAutoState {
    private static final Map<UUID, Boolean> autoEnabled = new HashMap<>();
    private static final Map<UUID, Integer> searchRadius = new HashMap<>();

    public static boolean isAutoEnabled(ServerPlayerEntity player) {
        return autoEnabled.getOrDefault(player.getUuid(), false);
    }

    public static void setAutoEnabled(ServerPlayerEntity player, boolean enabled) {
        autoEnabled.put(player.getUuid(), enabled);
    }

    public static int getSearchRadius(ServerPlayerEntity player) {
        return searchRadius.getOrDefault(player.getUuid(), 5);
    }

    public static void setSearchRadius(ServerPlayerEntity player, int radius) {
        searchRadius.put(player.getUuid(), radius);
    }
}
