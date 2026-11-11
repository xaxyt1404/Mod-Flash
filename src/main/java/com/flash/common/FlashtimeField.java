package com.flash.common;

import com.flash.FlashConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Campo de "tiempo bala": entidades NO jugador cerca de un caster ejecutan 1 de cada N ticks. */
public final class FlashtimeField {
    private FlashtimeField() {}

    private record Source(ResourceKey<Level> dim, Vec3 pos) {}
    private static final Map<UUID, Source> SOURCES = new ConcurrentHashMap<>();

    public static void set(UUID caster, ServerLevel level, Vec3 pos) {
        SOURCES.put(caster, new Source(level.dimension(), pos));
    }

    public static void remove(UUID caster) { SOURCES.remove(caster); }

    public static boolean shouldSkip(ServerLevel level, Entity e) {
        if (SOURCES.isEmpty()) return false;
        if (e instanceof Player || e.getControllingPassenger() instanceof Player) return false; // no afecta jugadores
        double r2 = FlashConstants.FLASHTIME_RADIUS * FlashConstants.FLASHTIME_RADIUS;
        boolean inside = false;
        for (Source s : SOURCES.values()) {
            if (s.dim() == level.dimension() && s.pos().distanceToSqr(e.position()) <= r2) { inside = true; break; }
        }
        if (!inside) return false;
        return (level.getGameTime() + e.getId()) % FlashConstants.FLASHTIME_TICK_DIVISOR != 0;
    }
}
