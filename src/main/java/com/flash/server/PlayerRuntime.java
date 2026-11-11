package com.flash.server;

import com.flash.FlashConstants;
import com.flash.common.FlashtimeField;
import com.flash.common.PlayerFlags;
import com.flash.net.FlashNetwork.SyncState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Estado TRANSITORIO por jugador (energía, cooldowns, etc.). No se guarda en disco. */
public final class PlayerRuntime {
    private static final Map<UUID, PlayerRuntime> MAP = new ConcurrentHashMap<>();

    public final ServerPlayer owner;

    // Barra principal
    public double energy = FlashConstants.ENERGY_MAX;
    public boolean fatigued;
    public int fatigueTicks;
    public boolean superSpeed;
    public int appliedLevel;
    public boolean terrain;

    // Movimiento medido por tick
    public Vec3 pos, prevPos;
    public double speed, speed3, runDistance;

    // Curación / daño
    public long lastDamageTick = Long.MIN_VALUE / 2;
    public int healTimer;
    public final Map<UUID, Long> hitCooldowns = new HashMap<>();

    // Barra secundaria + brecha
    public double dimBar;
    public int dimAxis = -1;      // 0 = corre por X (fila = Z), 1 = corre por Z (fila = X)
    public int dimRow, dimY, dimDir;
    public int dimCooldown;
    public int breachTicks;
    public boolean menuOpen;
    public Vec3 breachCenter;
    public int breachAxis;

    // Habilidades activas
    public boolean vibration;
    public int vibrationCd;
    public boolean flashtime;
    public int flashCd;
    public int rayCd, rayBurst;
    public long lastRayTick = Long.MIN_VALUE / 2;
    public TornadoSession tornado;
    public int tornadoCd;

    public SyncState lastSent;

    private PlayerRuntime(ServerPlayer owner) { this.owner = owner; }

    public static PlayerRuntime get(ServerPlayer p) {
        PlayerRuntime r = MAP.get(p.getUUID());
        if (r == null || r.owner != p) {            // primera vez o respawn / nueva entidad
            if (r != null) discard(p.getUUID());
            r = new PlayerRuntime(p);
            MAP.put(p.getUUID(), r);
        }
        return r;
    }

    /** null si el jugador aún no tiene runtime. */
    public static PlayerRuntime peek(ServerPlayer p) {
        PlayerRuntime r = MAP.get(p.getUUID());
        return (r != null && r.owner == p) ? r : null;
    }

    public static void discard(UUID id) {
        PlayerRuntime r = MAP.remove(id);
        if (r != null && r.tornado != null) { r.tornado.end(); r.tornado = null; }
        PlayerFlags.clear(id);
        FlashtimeField.remove(id);
    }
}
