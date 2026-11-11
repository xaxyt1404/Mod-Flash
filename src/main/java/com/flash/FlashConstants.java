package com.flash;

/** Todos los valores numéricos del Documento de Diseño en un solo lugar. Tiempos en ticks (20 = 1 s). */
public final class FlashConstants {
    private FlashConstants() {}

    public static final String MOD_ID = "flash";

    // ---- Barra principal (Speed Force) ----
    public static final double ENERGY_MAX = 100.0;
    public static final double ENERGY_REGEN_PER_SEC = 5.0;
    /** Índice = nivel de velocidad (1..5): -1 .. -5 puntos/s */
    public static final double[] SPEED_COST_PER_SEC = {0, 1, 2, 3, 4, 5};
    public static final double FATIGUE_EXIT_ENERGY = 15.0;
    public static final int FATIGUE_MIN_TICKS = 60;          // 3 s
    /** Movimiento mínimo (bloques/tick) para considerar que se "corre". */
    public static final double MIN_RUN_SPEED = 0.12;

    // ---- Atributos por nivel (el documento no fija velocidades; ajusta aquí) ----
    /** ADD_MULTIPLIED_BASE sobre movement_speed. Nivel 5 = x9 la velocidad base. */
    public static final double[] SPEED_BONUS = {0, 1.0, 2.0, 3.5, 5.5, 8.0};
    /** Step Assist: 1 bloque (N1-2), 2 (N3), 3 (N4-5). Se suma +0.05 de margen. */
    public static final double[] STEP_HEIGHT = {0.6, 1.05, 1.05, 2.05, 3.05, 3.05};
    public static final int TERRAIN_MIN_LEVEL = 3;           // agua/lava/muros
    public static final double TERRAIN_MIN_ENERGY = 15.0;    // > 15 puntos

    // ---- Barra secundaria (Ruptura Dimensional) ----
    public static final double DIM_GAIN_PER_SEC = 10.0;
    public static final double DIM_LOSS_PER_SEC = 25.0;
    public static final double DIM_WARN_FROM = 50.0;         // partículas desde el seg. 5
    /** Tolerancia (en bloques) de la "fila" exacta. 0 = exactamente el mismo bloque de fila. */
    public static final int DIM_ROW_TOLERANCE = 0;
    public static final int BREACH_TICKS = 100;              // 5 s
    public static final int DIM_COOLDOWN_TICKS = 300;        // 15 s tras cerrar
    public static final double BREACH_DISTANCE = 6.0;        // a cuántos bloques frente al jugador
    public static final double BREACH_ENTER_RADIUS = 1.6;

    // ---- Golpe de masa ----
    public static final double MASS_HIT_MIN_RUN = 6.0;
    public static final double MASS_HIT_DMG_PER_BPS = 0.4;   // daño extra = bloques/seg * 0.4
    public static final float MASS_HIT_MAX_EXTRA = 30f;
    public static final int MASS_HIT_SLOW_TICKS = 60;        // 3 s
    public static final double MASS_HIT_RECHARGE = 15.0;
    public static final int MASS_HIT_RECHARGE_CD = 60;       // 3 s por enemigo

    // ---- Curación acelerada ----
    public static final int HEAL_DELAY_TICKS = 60;           // 3 s sin daño
    public static final double HEAL_COST_PER_HEART = 2.0;

    // ---- Vibración (V) ----
    public static final double VIBRATION_COST_PER_SEC = 12.0;
    public static final int VIBRATION_COOLDOWN = 100;        // 5 s

    // ---- Flashtime (C) ----
    public static final double FLASHTIME_COST_PER_SEC = 15.0;
    public static final int FLASHTIME_COOLDOWN = 200;        // 10 s
    public static final double FLASHTIME_RADIUS = 15.0;
    /** Las entidades afectadas solo ejecutan 1 de cada N ticks (20% de velocidad). */
    public static final int FLASHTIME_TICK_DIVISOR = 5;

    // ---- Rayo (G) ----
    public static final double RAY_COST = 25.0;
    public static final double RAY_RANGE = 25.0;
    public static final float RAY_DAMAGE = 8.0f;
    public static final int RAY_BURST = 4;
    public static final int RAY_COOLDOWN = 40;               // 2 s tras la ráfaga
    public static final int RAY_MIN_GAP = 3;                 // anti-spam entre disparos

    // ---- Tornado (B) ----
    public static final double TORNADO_COST = 40.0;
    public static final double TORNADO_RANGE = 12.0;
    public static final int TORNADO_DURATION = 60;           // 3 s
    public static final int TORNADO_COOLDOWN = 240;          // 12 s
    public static final double TORNADO_RADIUS = 3.0;
    public static final double TORNADO_LIFT = 4.0;
    public static final double TORNADO_REVOLUTIONS = 3.0;
    public static final float TORNADO_DAMAGE = 3.0f;
    public static final double TORNADO_EXTRA_RADIUS = 5.0;

    // ---- Velozador ----
    public static final float UNLOCK_HEALTH = 1.0f;          // 1 corazón = 2 HP
}
