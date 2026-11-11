package com.flash.client;

import com.flash.FlashMod;
import com.flash.net.FlashNetwork.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Entrypoint de cliente: teclas, HUD, GUI, cámara. Solo ENVÍA intenciones; el servidor decide. */
public class FlashClient implements ClientModInitializer {
    public static KeyMapping KEY_SPEED, KEY_VIBRATION, KEY_FLASHTIME, KEY_RAY, KEY_TORNADO;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(FlashMod.id("main"));
        KEY_SPEED     = key("key.flash.speed",     InputConstants.KEY_R, cat); // Supervelocidad
        KEY_VIBRATION = key("key.flash.vibration", InputConstants.KEY_V, cat); // Vibración
        KEY_FLASHTIME = key("key.flash.flashtime", InputConstants.KEY_C, cat); // Flashtime
        KEY_RAY       = key("key.flash.ray",       InputConstants.KEY_G, cat); // Lanzar Rayo
        KEY_TORNADO   = key("key.flash.tornado",   InputConstants.KEY_B, cat); // Tornado Dirigido


        ClientPlayNetworking.registerGlobalReceiver(SyncState.TYPE,
                (payload, ctx) -> ClientState.apply(payload, ctx.client()));
        ClientPlayNetworking.registerGlobalReceiver(OpenDimensionMenu.TYPE,
                (payload, ctx) -> ctx.client().gui.setScreen(new DimensionScreen()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());
        ClientTickEvents.END_CLIENT_TICK.register(FlashClient::tick);
        FlashHud.register();
    }

    private static KeyMapping key(String name, int glfwKey, KeyMapping.Category cat) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, glfwKey, cat));
        }

    private static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;

        while (KEY_SPEED.consumeClick())     if (ClientState.unlocked) ClientPlayNetworking.send(new ToggleSpeed());
        while (KEY_VIBRATION.consumeClick()) if (ClientState.unlocked) ClientPlayNetworking.send(new UseAbility(UseAbility.VIBRATION));
        while (KEY_FLASHTIME.consumeClick()) if (ClientState.unlocked) ClientPlayNetworking.send(new UseAbility(UseAbility.FLASHTIME));
        while (KEY_RAY.consumeClick())       if (ClientState.unlocked) ClientPlayNetworking.send(new UseAbility(UseAbility.RAY));
        while (KEY_TORNADO.consumeClick())   if (ClientState.unlocked) ClientPlayNetworking.send(new UseAbility(UseAbility.TORNADO));

        // Escalar muros verticales (Nivel 3+): el movimiento es autoritativo del cliente.
        if (ClientState.terrain && ClientState.level >= 3 && !ClientState.locked
                && p.horizontalCollision && mc.options.keyUp.isDown()) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.35 + 0.1 * ClientState.level, v.z);
            p.fallDistance = 0;
        }
    }
}
