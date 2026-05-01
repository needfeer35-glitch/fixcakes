package com.cakeauto.client;

import com.cakeauto.CakeAutoPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CakeAutoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CakeAutoPackets.OPEN_GUI_ID, (payload, context) -> {
            context.client().execute(() -> {
                context.client().setScreen(new CakeAutoScreen(payload.autoEnabled(), payload.searchRadius()));
            });
        });
    }
}
