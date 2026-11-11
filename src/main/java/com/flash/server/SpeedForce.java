package com.flash.server;

import com.flash.FlashConstants;
import com.flash.FlashMod;
import com.flash.data.FlashData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Barra principal, supervelocidad, fatiga, atributos, curación acelerada. */
public final class SpeedForce {
    private SpeedForce() {}

    private static final Identifier SPEED_MOD = FlashMod.id("speed_force_speed");
    private static final Identifier STEP_MOD = FlashMod.id("speed_force_step");

    public static void tick(ServerPlayer p, PlayerRuntime r, FlashData d) {
        ServerLevel lv = (ServerLevel) p.level();
        long now = lv.getGameTime();
        int level = Math.max(1, Math.min(5, d.speedLevel()));

        // ---- medición de movimiento ----
        Vec3 pos = p.position();
        Vec3 mv = (r.pos == null) ? Vec3.ZERO : pos.subtract(r.pos);
        if (mv.lengthSqr() > 40 * 40 || r.tornado != null) mv = Vec3.ZERO; // teletransportes / giro del tornado
        r.prevPos = (r.pos == null) ? pos : r.pos;
        r.pos = pos;
        r.speed = Math.hypot(mv.x, mv.z);
        r.speed3 = mv.length();

        boolean running = r.superSpeed && !r.fatigued && r.speed3 >= FlashConstants.MIN_RUN_SPEED;

        // ---- salida de fatiga: >= 15 puntos y >= 3 s ----
        if (r.fatigued) {
            r.fatigueTicks++;
            if (r.energy >= FlashConstants.FATIGUE_EXIT_ENERGY && r.fatigueTicks >= FlashConstants.FATIGUE_MIN_TICKS) {
                r.fatigued = false;
            }
        }

        // ---- gasto / regeneración ----
        double costPerSec = 0;
        if (running) costPerSec += FlashConstants.SPEED_COST_PER_SEC[level];
        if (r.vibration) costPerSec += FlashConstants.VIBRATION_COST_PER_SEC;
        if (r.flashtime) costPerSec += FlashConstants.FLASHTIME_COST_PER_SEC;
        if (costPerSec > 0) r.energy -= costPerSec / 20.0;
        else r.energy += FlashConstants.ENERGY_REGEN_PER_SEC / 20.0;
        r.energy = Math.max(0, Math.min(FlashConstants.ENERGY_MAX, r.energy));

        if (r.energy <= 0 && !r.fatigued) enterFatigue(p, r);

        // ---- cooldowns ----
        if (r.vibrationCd > 0) r.vibrationCd--;
        if (r.flashCd > 0) r.flashCd--;
        if (r.rayCd > 0) r.rayCd--;
        if (r.tornadoCd > 0) r.tornadoCd--;
        if (r.dimCooldown > 0) r.dimCooldown--;

        // ---- atributos de velocidad + step assist (solo al cambiar) ----
        int desired = (r.superSpeed && !r.fatigued) ? level : 0;
        if (desired != r.appliedLevel) {
            applyModifiers(p, desired);
            r.appliedLevel = desired;
        }

        // ---- inmunidad a caídas (el daño se cancela además en ALLOW_DAMAGE) ----
        if (r.superSpeed && !r.fatigued) p.fallDistance = 0;

        // ---- agua / lava / muros: Nivel 3+ y > 15 puntos, solo en movimiento ----
        r.terrain = r.superSpeed && !r.fatigued
                && level >= FlashConstants.TERRAIN_MIN_LEVEL
                && r.energy > FlashConstants.TERRAIN_MIN_ENERGY
                && r.speed3 >= FlashConstants.MIN_RUN_SPEED;

        // ---- distancia corrida (golpe de masa) ----
        if (running) r.runDistance += r.speed; else r.runDistance = 0;

        // ---- curación acelerada ----
        if (r.superSpeed && !r.fatigued
                && p.getHealth() < p.getMaxHealth()
                && now - r.lastDamageTick >= FlashConstants.HEAL_DELAY_TICKS
                && r.energy >= FlashConstants.HEAL_COST_PER_HEART) {
            if (++r.healTimer >= 20) {                 // 1 corazón por segundo
                r.healTimer = 0;
                p.heal(2.0f);
                r.energy -= FlashConstants.HEAL_COST_PER_HEART;
                p.causeFoodExhaustion(1.5f);           // consume hambre normal
            }
        } else {
            r.healTimer = 0;
        }
    }

    public static void enterFatigue(ServerPlayer p, PlayerRuntime r) {
        r.fatigued = true;
        r.fatigueTicks = 0;
        r.superSpeed = false;
        Abilities.forceStopToggles(p, r);
        p.sendSystemMessage(Component.translatable("message.flash.fatigue"), true);
    }

    public static void applyModifiers(ServerPlayer p, int level) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MOD);
            if (level > 0) {
                speed.addTransientModifier(new AttributeModifier(SPEED_MOD,
                        FlashConstants.SPEED_BONUS[level], AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        AttributeInstance step = p.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.removeModifier(STEP_MOD);
            if (level > 0) {
                double extra = FlashConstants.STEP_HEIGHT[level] - step.getBaseValue();
                step.addTransientModifier(new AttributeModifier(STEP_MOD, extra, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }
}
