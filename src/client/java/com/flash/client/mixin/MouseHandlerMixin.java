package com.flash.client.mixin;

import com.flash.client.ClientState;
import com.flash.net.FlashNetwork.ChangeLevel;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /** Alt Izquierdo + rueda: sube/baja el nivel de velocidad (1..5) en lugar de cambiar de ranura. */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void flash$altScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || !ClientState.unlocked) return;
        if (!InputConstants.isKeyDown(InputConstants.KEY_LALT)) return;
        if (yOffset != 0) ClientPlayNetworking.send(new ChangeLevel(yOffset > 0 ? 1 : -1));
        ci.cancel();
    }
}
