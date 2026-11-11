package com.flash.client.mixin;

import com.flash.common.PlayerFlags;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    /** Vibración: el cliente vanilla empuja al jugador fuera de los bloques; hay que desactivarlo. */
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void flash$noPushOut(double x, double z, CallbackInfo ci) {
        if (PlayerFlags.isPhasing((LocalPlayer) (Object) this)) ci.cancel();
    }
}
