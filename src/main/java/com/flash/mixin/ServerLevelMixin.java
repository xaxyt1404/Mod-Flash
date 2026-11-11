package com.flash.mixin;

import com.flash.common.FlashtimeField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    /** Flashtime: mobs, proyectiles y entidades dentro de 15 bloques solo ejecutan 1 de cada N ticks. */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void flash$bulletTime(Entity entity, CallbackInfo ci) {
        if (FlashtimeField.shouldSkip((ServerLevel) (Object) this, entity)) ci.cancel();
    }
}
