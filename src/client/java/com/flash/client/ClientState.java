package com.flash.client;

import com.flash.common.PlayerFlags;
import com.flash.net.FlashNetwork.SyncState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/** Último estado recibido del servidor para el jugador local. */
public final class ClientState {
    private ClientState() {}

    public static boolean unlocked, superSpeed, fatigued, vibration, flashtime, terrain, locked, cinematic, breach;
    public static double energy, dim;
    public static int level = 1;

    private static CameraType savedCamera;

    public static void apply(SyncState s, Minecraft mc) {
        boolean wasCinematic = cinematic;

        unlocked = s.has(SyncState.UNLOCKED);
        superSpeed = s.has(SyncState.SUPER);
        fatigued = s.has(SyncState.FATIGUE);
        vibration = s.has(SyncState.VIBRATION);
        flashtime = s.has(SyncState.FLASHTIME);
        terrain = s.has(SyncState.TERRAIN);
        locked = s.has(SyncState.LOCKED);
        cinematic = s.has(SyncState.CINEMATIC);
        breach = s.has(SyncState.BREACH);
        energy = s.energy10() / 10.0;
        dim = s.dim10() / 10.0;
        level = s.level();

        if (mc.player == null) return;

        // En un mundo de un jugador el servidor integrado ya gestiona estos sets (mismo JVM).
        if (mc.getSingleplayerServer() == null) {
            UUID id = mc.player.getUUID();
            PlayerFlags.set(PlayerFlags.PHASING, id, vibration);
            PlayerFlags.set(PlayerFlags.TERRAIN, id, terrain);
            PlayerFlags.set(PlayerFlags.LOCKED, id, locked);
        }

        // Cámara cinemática del Tornado: tercera persona mientras dura el giro
        if (cinematic && !wasCinematic) {
            savedCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!cinematic && wasCinematic && savedCamera != null) {
            mc.options.setCameraType(savedCamera);
            savedCamera = null;
        }
    }

    public static void reset() {
        unlocked = superSpeed = fatigued = vibration = flashtime = terrain = locked = cinematic = breach = false;
        energy = dim = 0;
        level = 1;
        savedCamera = null;
    }
}
