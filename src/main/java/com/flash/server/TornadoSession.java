package com.flash.server;

import com.flash.FlashConstants;
import com.flash.common.PlayerFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Tornado Dirigido (3 s): el caster gira alrededor del objetivo; objetivo + enemigos cercanos
 * se bloquean (mov + cámara), se elevan 4 bloques y reciben daño. El caster ignora proyectiles.
 */
public final class TornadoSession {
    private record Victim(LivingEntity entity, double baseY, boolean wasNoAi, boolean wasNoGravity) {}

    private final ServerPlayer caster;
    private final List<Victim> victims = new ArrayList<>();
    private final LivingEntity target;
    private final double cx, cz, baseY;
    private int ticks;
    private double angle;

    public TornadoSession(ServerPlayer caster, LivingEntity target) {
        this.caster = caster;
        this.target = target;
        this.cx = target.getX();
        this.cz = target.getZ();
        this.baseY = target.getY();
        this.angle = Math.atan2(caster.getZ() - cz, caster.getX() - cx);

        ServerLevel lv = (ServerLevel) caster.level();
        List<LivingEntity> all = new ArrayList<>();
        all.add(target);
        all.addAll(lv.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(FlashConstants.TORNADO_EXTRA_RADIUS),
                e -> e != caster && e != target && e instanceof Enemy && e.isAlive()));

        for (LivingEntity e : all) {
            boolean noAi = e instanceof Mob m && m.isNoAi();
            boolean noGrav = e.isNoGravity();
            if (e instanceof Mob m) m.setNoAi(true);          // mobs: sin IA ni movimiento
            e.setNoGravity(true);
            if (e instanceof ServerPlayer sp) PlayerFlags.LOCKED.add(sp.getUUID()); // jugadores: mov + cámara
            victims.add(new Victim(e, e.getY(), noAi, noGrav));
        }
        PlayerFlags.LOCKED.add(caster.getUUID());              // cámara cinemática: el caster tampoco se mueve solo
    }

    /** @return true cuando terminó. */
    public boolean tick() {
        ServerLevel lv = (ServerLevel) caster.level();
        if (!caster.isAlive() || caster.isRemoved() || target.isRemoved() || !target.isAlive()
                || ticks >= FlashConstants.TORNADO_DURATION) {
            return true;
        }
        ticks++;
        double lift = Math.min(FlashConstants.TORNADO_LIFT, ticks * 0.4);

        // --- víctimas: elevadas y congeladas ---
        for (Victim v : victims) {
            LivingEntity e = v.entity();
            if (!e.isAlive() || e.isRemoved()) continue;
            double y = v.baseY() + lift;
            if (e instanceof ServerPlayer sp) {
                sp.connection.teleport(sp.getX(), y, sp.getZ(), sp.getYRot(), sp.getXRot());
            } else {
                e.setPos(e.getX(), y, e.getZ());
                e.setDeltaMovement(Vec3.ZERO);
            }
            e.fallDistance = 0;
        }

        // --- caster corre en círculos (3 vueltas en 3 s) ---
        angle += Math.PI * 2 * FlashConstants.TORNADO_REVOLUTIONS / FlashConstants.TORNADO_DURATION;
        double px = cx + Math.cos(angle) * FlashConstants.TORNADO_RADIUS;
        double pz = cz + Math.sin(angle) * FlashConstants.TORNADO_RADIUS;
        float yaw = (float) Math.toDegrees(angle);            // mira a lo largo de la tangente
        caster.connection.teleport(px, baseY, pz, yaw, 0f);
        caster.fallDistance = 0;

        // --- daño periódico a todos los afectados ---
        if (ticks % 10 == 0) {
            for (Victim v : victims) {
                LivingEntity e = v.entity();
                if (!e.isAlive()) continue;
                FlashServer.guarded(() -> {
                    e.invulnerableTime = 0;
                    e.hurtServer(lv, lv.damageSources().playerAttack(caster), FlashConstants.TORNADO_DAMAGE);
                });
            }
        }

        // --- vórtice visual ---
        for (int i = 0; i < 12; i++) {
            double h = (i / 12.0) * FlashConstants.TORNADO_LIFT + lift * 0.0;
            double ang = angle * 3 + h * 2.0;
            double rad = 0.5 + h * 0.4;
            lv.sendParticles(ParticleTypes.CLOUD, cx + Math.cos(ang) * rad, baseY + h, cz + Math.sin(ang) * rad,
                    1, 0, 0, 0, 0.02);
        }
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, baseY + 1.0, pz, 4, 0.2, 0.4, 0.2, 0.05);
        return false;
    }

    public void end() {
        for (Victim v : victims) {
            LivingEntity e = v.entity();
            if (e instanceof Mob m) m.setNoAi(v.wasNoAi());
            e.setNoGravity(v.wasNoGravity());
            if (e instanceof ServerPlayer sp) PlayerFlags.LOCKED.remove(sp.getUUID());
        }
        PlayerFlags.LOCKED.remove(caster.getUUID());
        victims.clear();
    }
}
