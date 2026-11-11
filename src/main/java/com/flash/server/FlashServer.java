package com.flash.server;

import com.flash.FlashConstants;
import com.flash.common.FlashtimeField;
import com.flash.common.PlayerFlags;
import com.flash.data.FlashAttachments;
import com.flash.data.FlashData;
import com.flash.net.FlashNetwork.SyncState;
import com.flash.net.FlashNetwork.UseAbility;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Orquestador del lado servidor: tick, eventos de daño, recepción de payloads y sincronización. */
public final class FlashServer {
    private FlashServer() {}

    /** true mientras el mod aplica daño propio (evita recursión del Golpe de Masa). */
    private static boolean internalDamage;

    public static void guarded(Runnable r) {
        boolean prev = internalDamage;
        internalDamage = true;
        try { r.run(); } finally { internalDamage = prev; }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(FlashServer::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(FlashServer::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(FlashServer::afterDamage);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PlayerRuntime.discard(handler.getPlayer().getUUID()));
    }

    // ===================================================================== tick
    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive()) { PlayerRuntime.discard(p.getUUID()); continue; }

            FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
            PlayerRuntime r = PlayerRuntime.get(p);

            if (!d.unlocked()) {
                VelozadorUnlock.check(p);
                sync(p, r, d);                                  // por si es víctima de un tornado (LOCKED)
                continue;
            }

            SpeedForce.tick(p, r, d);
            DimensionRift.tick(p, r, d.speedLevel());
            Abilities.tickTornado(p, r);

            // Flashtime: publica la posición del caster para el campo de ralentización
            if (r.flashtime) FlashtimeField.set(p.getUUID(), (ServerLevel) p.level(), p.position());

            // Flags para los mixins (colisión, agua/lava, bloqueo)
            PlayerFlags.set(PlayerFlags.PHASING, p.getUUID(), r.vibration);
            PlayerFlags.set(PlayerFlags.TERRAIN, p.getUUID(), r.terrain);
            // Truco de validación: el servidor no hace rollback al atravesar bloques con noPhysics.
            // Player.tick() lo resetea cada tick; aquí se re-activa DESPUÉS de los ticks y ANTES de los paquetes.
            if (r.vibration) p.noPhysics = true;

            sync(p, r, d);
        }
    }

    private static void sync(ServerPlayer p, PlayerRuntime r, FlashData d) {
        int flags = 0;
        if (d.unlocked()) flags |= SyncState.UNLOCKED;
        if (r.superSpeed) flags |= SyncState.SUPER;
        if (r.fatigued) flags |= SyncState.FATIGUE;
        if (r.vibration) flags |= SyncState.VIBRATION;
        if (r.flashtime) flags |= SyncState.FLASHTIME;
        if (r.terrain) flags |= SyncState.TERRAIN;
        if (PlayerFlags.LOCKED.contains(p.getUUID())) flags |= SyncState.LOCKED;
        if (r.tornado != null) flags |= SyncState.CINEMATIC;
        if (r.breachTicks > 0 || r.menuOpen) flags |= SyncState.BREACH;

        SyncState s = new SyncState((int) Math.round(r.energy * 10), (int) Math.round(r.dimBar * 10),
                d.speedLevel(), flags);
        if (!s.equals(r.lastSent)) {
            ServerPlayNetworking.send(p, s);
            r.lastSent = s;
        }
    }

    // ===================================================================== daño
    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer p)) return true;
        PlayerRuntime r = PlayerRuntime.peek(p);
        if (r == null) return true;

        if (r.superSpeed && source.is(DamageTypeTags.IS_FALL)) return false;            // inmune a caídas
        if (r.vibration && source.is(DamageTypes.IN_WALL)) return false;                // sin asfixia al phasear
        if (r.tornado != null && source.is(DamageTypeTags.IS_PROJECTILE)) return false; // inmune a proyectiles

        if (amount > 0) r.lastDamageTick = ((ServerLevel) p.level()).getGameTime();
        return true;
    }

    /** Golpe de Masa + Robo de Velocidad. Se aplica DESPUÉS del golpe vanilla para que el daño sea aditivo. */
    private static void afterDamage(LivingEntity victim, DamageSource source, float base, float taken, boolean blocked) {
        if (internalDamage || blocked || taken <= 0) return;
        if (!(source.getEntity() instanceof ServerPlayer p) || source.getDirectEntity() != p) return; // solo melee
        if (!(victim.level() instanceof ServerLevel lv)) return;

        PlayerRuntime r = PlayerRuntime.peek(p);
        if (r == null || !r.superSpeed || r.fatigued || r.runDistance < FlashConstants.MASS_HIT_MIN_RUN) return;
        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        if (!d.unlocked()) return;

        double bps = r.speed * 20.0;
        float extra = (float) Math.min(FlashConstants.MASS_HIT_MAX_EXTRA, bps * FlashConstants.MASS_HIT_DMG_PER_BPS);

        guarded(() -> {
            victim.invulnerableTime = 0;
            victim.hurtServer(lv, source, extra);                                       // daño extra por velocidad
            Vec3 toAttacker = p.position().subtract(victim.position());
            victim.knockback(1.5 + d.speedLevel() * 0.4, toAttacker.x, toAttacker.z, source, extra); // alto empuje
            victim.needsSync = true;
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    FlashConstants.MASS_HIT_SLOW_TICKS, 1), p);                         // Lentitud 3 s
        });

        // Recarga de energía con cooldown interno de 3 s por enemigo
        long now = lv.getGameTime();
        Long last = r.hitCooldowns.get(victim.getUUID());
        if (last == null || now - last >= FlashConstants.MASS_HIT_RECHARGE_CD) {
            r.energy = Math.min(FlashConstants.ENERGY_MAX, r.energy + FlashConstants.MASS_HIT_RECHARGE);
            r.hitCooldowns.put(victim.getUUID(), now);
            if (r.hitCooldowns.size() > 64) {
                r.hitCooldowns.values().removeIf(t -> now - t > FlashConstants.MASS_HIT_RECHARGE_CD);
            }
        }
    }

    // ===================================================================== receptores de red
    public static void onToggleSpeed(ServerPlayer p) {
        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        if (!d.unlocked()) return;
        PlayerRuntime r = PlayerRuntime.get(p);
        if (r.fatigued || r.tornado != null) return;                  // bloqueada en Fatiga
        if (!r.superSpeed && r.energy <= 0) return;
        r.superSpeed = !r.superSpeed;
    }

    public static void onChangeLevel(ServerPlayer p, int delta) {
        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        if (!d.unlocked() || delta == 0) return;
        p.setAttached(FlashAttachments.FLASH_DATA, d.withLevel(d.speedLevel() + (delta > 0 ? 1 : -1)));
    }

    public static void onAbility(ServerPlayer p, int ability) {
        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        if (!d.unlocked()) return;
        PlayerRuntime r = PlayerRuntime.get(p);
        switch (ability) {
            case UseAbility.VIBRATION -> Abilities.toggleVibration(p, r);
            case UseAbility.FLASHTIME -> Abilities.toggleFlashtime(p, r);
            case UseAbility.RAY -> Abilities.fireRay(p, r);
            case UseAbility.TORNADO -> Abilities.startTornado(p, r);
            default -> { }
        }
    }

    public static void onChooseDimension(ServerPlayer p, int index) {
        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        if (!d.unlocked()) return;
        DimensionRift.choose(p, PlayerRuntime.get(p), index);
    }
}
