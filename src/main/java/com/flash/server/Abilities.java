package com.flash.server;

import com.flash.FlashConstants;
import com.flash.common.FlashtimeField;
import com.flash.common.PlayerFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Habilidades activas: V Vibración, C Flashtime, G Rayo, B Tornado. */
public final class Abilities {
    private Abilities() {}

    private static boolean canAct(ServerPlayer p, PlayerRuntime r) {
        if (!p.isAlive() || r.fatigued) return false;
        if (r.tornado != null) return false;
        return true;
    }

    private static void msg(ServerPlayer p, String key) {
        p.sendSystemMessage(Component.translatable(key), true);
    }

    // ------------------------------------------------------------ V
    public static void toggleVibration(ServerPlayer p, PlayerRuntime r) {
        if (r.vibration) { stopVibration(p, r); return; }
        if (!canAct(p, r)) return;
        if (r.vibrationCd > 0) { msg(p, "message.flash.cooldown"); return; }
        if (r.energy < 1) { msg(p, "message.flash.no_energy"); return; }
        r.vibration = true;
    }

    public static void stopVibration(ServerPlayer p, PlayerRuntime r) {
        if (!r.vibration) return;
        r.vibration = false;
        r.vibrationCd = FlashConstants.VIBRATION_COOLDOWN;
        PlayerFlags.PHASING.remove(p.getUUID());
        if (!p.isSpectator()) p.noPhysics = false;
        ejectIfStuck(p);
    }

    /** Si el jugador queda dentro de bloques, lo expulsa al espacio libre más cercano. */
    private static void ejectIfStuck(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        AABB box = p.getBoundingBox();
        if (lv.noCollision(p, box)) return;

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        final int R = 6;
        for (int dx = -R; dx <= R; dx++) for (int dy = -R; dy <= R; dy++) for (int dz = -R; dz <= R; dz++) {
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist >= bestDist) continue;
            if (lv.noCollision(p, box.move(dx, dy, dz))) { bestDist = dist; best = p.position().add(dx, dy, dz); }
        }
        if (best == null) {                              // fallback: subir hasta hallar hueco
            for (int dy = R + 1; dy < 64; dy++) {
                if (lv.noCollision(p, box.move(0, dy, 0))) { best = p.position().add(0, dy, 0); break; }
            }
        }
        if (best != null) p.connection.teleport(best.x, best.y, best.z, p.getYRot(), p.getXRot());
    }

    // ------------------------------------------------------------ C
    public static void toggleFlashtime(ServerPlayer p, PlayerRuntime r) {
        if (r.flashtime) { stopFlashtime(p, r); return; }
        if (!canAct(p, r)) return;
        if (r.flashCd > 0) { msg(p, "message.flash.cooldown"); return; }
        if (r.energy < 1) { msg(p, "message.flash.no_energy"); return; }
        r.flashtime = true;
    }

    public static void stopFlashtime(ServerPlayer p, PlayerRuntime r) {
        if (!r.flashtime) return;
        r.flashtime = false;
        r.flashCd = FlashConstants.FLASHTIME_COOLDOWN;
        FlashtimeField.remove(p.getUUID());
    }

    /** Se llama al entrar en Fatiga. */
    public static void forceStopToggles(ServerPlayer p, PlayerRuntime r) {
        stopVibration(p, r);
        stopFlashtime(p, r);
    }

    // ------------------------------------------------------------ G
    public static void fireRay(ServerPlayer p, PlayerRuntime r) {
        if (!canAct(p, r)) return;
        ServerLevel lv = (ServerLevel) p.level();
        long now = lv.getGameTime();

        if (r.rayCd > 0) { msg(p, "message.flash.cooldown"); return; }
        if (now - r.lastRayTick > FlashConstants.RAY_COOLDOWN) r.rayBurst = 0;      // ráfaga terminada
        if (now - r.lastRayTick < FlashConstants.RAY_MIN_GAP) return;
        if (r.energy < FlashConstants.RAY_COST) { msg(p, "message.flash.no_energy"); return; }

        r.energy -= FlashConstants.RAY_COST;
        r.lastRayTick = now;
        if (++r.rayBurst >= FlashConstants.RAY_BURST) { r.rayCd = FlashConstants.RAY_COOLDOWN; r.rayBurst = 0; }

        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(FlashConstants.RAY_RANGE));

        BlockHitResult bhit = lv.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = bhit.getType() != HitResult.Type.MISS ? bhit.getLocation() : end;

        EntityHitResult ehit = ProjectileUtil.getEntityHitResult(p, eye, stop,
                p.getBoundingBox().expandTowards(stop.subtract(eye)).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable() && e != p && e instanceof LivingEntity,
                eye.distanceToSqr(stop));
        if (ehit != null) stop = ehit.getLocation();

        double len = stop.subtract(eye).length();
        for (double t = 1.0; t < len; t += 0.7) {
            Vec3 c = eye.add(look.scale(t));
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y - 0.2, c.z, 2, 0.05, 0.05, 0.05, 0.0);
            lv.sendParticles(ParticleTypes.END_ROD, c.x, c.y - 0.2, c.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 2.0f);

        if (ehit != null && ehit.getEntity() instanceof LivingEntity target) {
            FlashServer.guarded(() -> {
                target.invulnerableTime = 0;
                target.hurtServer(lv, lv.damageSources().indirectMagic(p, p), FlashConstants.RAY_DAMAGE);
                target.igniteForSeconds(3.0f);                       // incendia brevemente
            });
        }
    }

    // ------------------------------------------------------------ B
    public static void startTornado(ServerPlayer p, PlayerRuntime r) {
        if (!canAct(p, r)) return;
        if (r.tornadoCd > 0) { msg(p, "message.flash.cooldown"); return; }
        if (r.energy < FlashConstants.TORNADO_COST) { msg(p, "message.flash.no_energy"); return; }

        double range = FlashConstants.TORNADO_RANGE;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end,
                p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0),
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && e != p,
                range * range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) { msg(p, "message.flash.no_target"); return; }

        r.energy -= FlashConstants.TORNADO_COST;
        // Las habilidades de toggle se apagan para no mezclar estados durante la cinemática
        stopVibration(p, r);
        stopFlashtime(p, r);
        r.tornado = new TornadoSession(p, target);
    }

    /** Llamado cada tick desde FlashServer. */
    public static void tickTornado(ServerPlayer p, PlayerRuntime r) {
        if (r.tornado == null) return;
        if (r.tornado.tick()) {
            r.tornado.end();
            r.tornado = null;
            r.tornadoCd = FlashConstants.TORNADO_COOLDOWN;   // cooldown tras finalizar el giro
        }
    }
}
