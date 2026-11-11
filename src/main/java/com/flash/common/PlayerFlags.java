package com.flash.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags de estado consultados por los mixins (que corren en cliente Y servidor).
 * El servidor los gestiona; el cliente los rellena desde SyncState para el jugador local.
 */
public final class PlayerFlags {
    private PlayerFlags() {}

    public static final Set<UUID> PHASING = ConcurrentHashMap.newKeySet();     // Vibración
    public static final Set<UUID> TERRAIN = ConcurrentHashMap.newKeySet();     // agua/lava/muros
    public static final Set<UUID> LOCKED = ConcurrentHashMap.newKeySet();      // sin mover ni mirar

    public static void set(Set<UUID> set, UUID id, boolean value) {
        if (value) set.add(id); else set.remove(id);
    }

    public static boolean isPhasing(Entity e) { return e instanceof Player && PHASING.contains(e.getUUID()); }
    public static boolean isTerrain(Entity e) { return e instanceof Player && TERRAIN.contains(e.getUUID()); }
    public static boolean isLocked(Entity e) { return e instanceof Player && LOCKED.contains(e.getUUID()); }

    public static void clear(UUID id) {
        PHASING.remove(id);
        TERRAIN.remove(id);
        LOCKED.remove(id);
    }
}
