package com.flash.server;

import com.flash.FlashConstants;
import com.flash.block.FlashBlocks;
import com.flash.data.FlashAttachments;
import com.flash.data.FlashData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Estructura: Velozador a 2 bloques del suelo + Pararrayos encima. El jugador de pie justo debajo
 * (sin importar el clima, solo Overworld) recibe el rayo, queda a 1 corazón y desbloquea la Speed Force.
 */
public final class VelozadorUnlock {
    private VelozadorUnlock() {}

    public static void check(ServerPlayer p) {
        if (!p.isAlive() || p.level().dimension() != Level.OVERWORLD) return;
        ServerLevel lv = (ServerLevel) p.level();

        BlockPos feet = p.blockPosition();
        BlockPos velozador = feet.above(2);          // 2 bloques libres debajo (pies + cabeza)
        if (!lv.getBlockState(velozador).is(FlashBlocks.VELOZADOR)) return;
        if (!(lv.getBlockState(velozador.above()).getBlock() instanceof net.minecraft.world.level.block.LightningRodBlock)) return;

        unlock(p, lv, velozador);
    }

    private static void unlock(ServerPlayer p, ServerLevel lv, BlockPos velozador) {
        // Rayo de energía dorada (visual: no incendia ni mata)
        LightningBolt bolt = (LightningBolt) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lightning_bolt"))
                .create(lv, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(Vec3.atBottomCenterOf(p.blockPosition()));
            bolt.setVisualOnly(true);
            lv.addFreshEntity(bolt);
        }
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.4, 1.0, 0.4, 0.3);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 1.0, 0.5, 0.4);

        // Queda a 1 corazón de vida, sin matarlo
        p.setHealth(Math.min(p.getHealth(), FlashConstants.UNLOCK_HEALTH));

        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        p.setAttached(FlashAttachments.FLASH_DATA, d.withUnlocked(true));   // desbloqueo PERMANENTE

        p.sendSystemMessage(Component.translatable("message.flash.unlocked"), false);
    }
}
