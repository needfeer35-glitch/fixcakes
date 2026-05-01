package com.cakeauto;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CakeAutoMod implements ModInitializer {
    public static final String MOD_ID = "cakeauto";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("CakeAuto mod initialized!");
        CakeAutoPackets.registerServerPackets();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CakeCommand.register(dispatcher);
        });
    }
}
