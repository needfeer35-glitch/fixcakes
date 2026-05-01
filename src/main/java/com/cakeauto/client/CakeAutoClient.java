package com.cakeauto.client;

import com.cakeauto.CakeAutoPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class CakeAutoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(CakeAutoPackets.OPEN_GUI_ID, CakeAutoPackets.OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CakeAutoPackets.TOGGLE_AUTO_ID, CakeAutoPackets.ToggleAutoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CakeAutoPackets.AUTO_OPEN_TABLE_ID, CakeAutoPackets.AutoOpenTablePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(CakeAutoPackets.OPEN_GUI_ID, (payload, context) -> {
            context.client().execute(() -> {
                context.client().setScreen(new CakeAutoScreen(payload.autoEnabled(), payload.searchRadius()));
            });
        });
    }
}
