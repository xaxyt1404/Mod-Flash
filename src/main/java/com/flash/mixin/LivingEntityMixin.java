package com.flash.mixin;

import com.flash.common.PlayerFlags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** Mismo gancho que usa el Strider: permite caminar sobre agua y lava (Nivel 3+, > 15 puntos, en movimiento). */
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void flash$walkOnFluid(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (PlayerFlags.isTerrain((LivingEntity) (Object) this)) cir.setReturnValue(true);
    }
}
