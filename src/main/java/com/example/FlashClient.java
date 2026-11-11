package com.example;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public class FlashClient implements ClientModInitializer {

    private static KeyMapping speedKeyBinding;
    private static boolean speedEnabled = false;

    // Categoría registrada usando el Identifier del mod
    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Flash.id("abilities"));

    @Override
    public void onInitializeClient() {

        // Registrar tecla J usando InputConstants nativo (SDL3)
        speedKeyBinding = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.flash.speed",
                        InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_J,
                        KEY_CATEGORY
                )
        );

        // Detectar pulsación de la tecla en cada tick del cliente
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (speedKeyBinding.consumeClick()) {

                if (client.player == null) {
                    continue;
                }

                speedEnabled = !speedEnabled;

                String message = speedEnabled
                        ? "¡Velocidad activada!"
                        : "¡Velocidad desactivada!";

                client.player.sendSystemMessage(Component.literal(message));
            }
        });
    }
}
