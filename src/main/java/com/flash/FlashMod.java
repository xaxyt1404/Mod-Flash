package com.flash;

import com.flash.block.FlashBlocks;
import com.flash.data.FlashAttachments;
import com.flash.net.FlashNetwork;
import com.flash.server.FlashServer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint común (servidor + cliente). No toca clases de cliente. */
public class FlashMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(FlashConstants.MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FlashConstants.MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        FlashAttachments.init();
        FlashBlocks.init();
        FlashNetwork.registerCommon();
        FlashServer.init();
        LOGGER.info("Flash mod cargado.");
    }
}
