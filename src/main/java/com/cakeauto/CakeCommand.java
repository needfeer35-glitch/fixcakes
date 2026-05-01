package com.cakeauto;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CakeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("tort")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    if (source.getEntity() instanceof ServerPlayerEntity player) {
                        boolean autoEnabled = CakeAutoState.isAutoEnabled(player);
                        int radius = CakeAutoState.getSearchRadius(player);
                        ServerPlayNetworking.send(player,
                                new CakeAutoPackets.OpenGuiPayload(autoEnabled, radius));
                    }
                    return 1;
                })
        );
    }
}
