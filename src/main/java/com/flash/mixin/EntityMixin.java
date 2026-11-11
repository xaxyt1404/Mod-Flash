package com.flash.mixin;

import com.flash.common.PlayerFlags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Corre en cliente Y servidor (el cliente simula su propio movimiento; el servidor lo valida).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private Vec3 collide(Vec3 movement) {
        throw new AssertionError();
    }

    @Unique
    private static final ThreadLocal<Boolean> FLASH$REENTRY = ThreadLocal.withInitial(() -> false);

    /**
     * Vibración: ignora la colisión horizontal (X/Z) pero conserva la colisión vertical (Y),
     * así el bloque bajo los pies sigue siendo sólido.
     */
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void flash$phase(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (FLASH$REENTRY.get() || !PlayerFlags.isPhasing(self)) return;
        FLASH$REENTRY.set(true);
        try {
            Vec3 verticalOnly = this.collide(new Vec3(0.0, movement.y, 0.0));
            cir.setReturnValue(new Vec3(movement.x, verticalOnly.y, movement.z));
        } finally {
            FLASH$REENTRY.set(false);
        }
    }

    /**
     * Tornado: la víctima (y el caster en cinemática) no pueden moverse por sí mismos.
     */
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void flash$lockMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((type == MoverType.SELF || type == MoverType.PLAYER) && PlayerFlags.isLocked((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    /**
     * Tornado: cámara bloqueada.
     */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void flash$lockTurn(double yRot, double xRot, CallbackInfo ci) {
        if (PlayerFlags.isLocked((Entity) (Object) this)) ci.cancel();
    }
}