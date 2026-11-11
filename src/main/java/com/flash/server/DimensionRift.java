package com.flash.server;

import com.flash.FlashConstants;
import com.flash.net.FlashNetwork.OpenDimensionMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Barra secundaria de Ruptura Dimensional, brecha de 5 s y teletransporte a bloque sólido seguro.
 */
public final class DimensionRift {
    private DimensionRift() {
    }

    public static void tick(ServerPlayer p, PlayerRuntime r, int level) {
        ServerLevel lv = (ServerLevel) p.level();
        Vec3 mv = (r.pos != null && r.prevPos != null) ? r.pos.subtract(r.prevPos) : Vec3.ZERO;

        if (r.breachTicks > 0 || r.menuOpen) tickBreach(p, r, lv);

        boolean eligible = r.superSpeed && !r.fatigued && level == 5
                && r.speed3 >= FlashConstants.MIN_RUN_SPEED
                && r.dimCooldown <= 0 && r.breachTicks <= 0 && !r.menuOpen && r.tornado == null;

        int bx = p.getBlockX(), by = p.getBlockY(), bz = p.getBlockZ();
        boolean gain = false;

        if (eligible) {
            if (r.dimAxis < 0 && r.dimBar <= 0) {            // ancla una nueva fila recta
                if (Math.abs(mv.x) >= Math.abs(mv.z)) {
                    r.dimAxis = 0;
                    r.dimRow = bz;
                    r.dimDir = mv.x >= 0 ? 1 : -1;
                } else {
                    r.dimAxis = 1;
                    r.dimRow = bx;
                    r.dimDir = mv.z >= 0 ? 1 : -1;
                }
                r.dimY = by;
            }
            if (r.dimAxis >= 0) {
                int rowCoord = (r.dimAxis == 0) ? bz : bx;
                double along = (r.dimAxis == 0) ? mv.x : mv.z;
                boolean sameRow = Math.abs(rowCoord - r.dimRow) <= FlashConstants.DIM_ROW_TOLERANCE;
                boolean sameY = by == r.dimY;
                boolean forward = along * r.dimDir > 0;
                gain = sameRow && sameY && forward;
            }
        }

        if (gain) r.dimBar = Math.min(100.0, r.dimBar + FlashConstants.DIM_GAIN_PER_SEC / 20.0);
        else r.dimBar = Math.max(0.0, r.dimBar - FlashConstants.DIM_LOSS_PER_SEC / 20.0);
        if (r.dimBar <= 0) r.dimAxis = -1;

        // Partículas de aviso desde el segundo 5 (50%), con intensidad creciente
        if (r.dimBar >= FlashConstants.DIM_WARN_FROM) {
            int count = 1 + (int) ((r.dimBar - FlashConstants.DIM_WARN_FROM) / 8.0);
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(),
                    count, 0.4, 0.8, 0.4, 0.15);
        }

        if (r.dimBar >= 100.0) {                              // 10 s exactos -> abrir brecha
            Vec3 dir = (r.dimAxis == 0) ? new Vec3(r.dimDir, 0, 0) : new Vec3(0, 0, r.dimDir);
            r.breachAxis = r.dimAxis;
            r.breachCenter = p.position().add(dir.scale(FlashConstants.BREACH_DISTANCE)).add(0, 1.0, 0);
            r.breachTicks = FlashConstants.BREACH_TICKS;
            r.menuOpen = false;
            r.dimBar = 0;                                     // la barra secundaria se vacía
            r.dimAxis = -1;
        }
    }

    private static void tickBreach(ServerPlayer p, PlayerRuntime r, ServerLevel lv) {
        if (r.breachCenter == null) {
            closeBreach(r);
            return;
        }
        Vec3 c = r.breachCenter;

        // Elipse vertical de partículas perpendicular a la dirección de carrera
        Vec3 side = (r.breachAxis == 0) ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
        long t = lv.getGameTime();
        for (int i = 0; i < 14; i++) {
            double a = (Math.PI * 2) * (i / 14.0) + t * 0.15;
            Vec3 pt = c.add(side.scale(Math.cos(a) * 1.1)).add(0, Math.sin(a) * 1.9, 0);
            lv.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0.0);
        }
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 3, 0.5, 0.9, 0.5, 0.05);

        // ¿Cruzó la brecha? Se mide contra el SEGMENTO recorrido este tick (a Nivel 5 se recorren ~2 bloques/tick)
        if (!r.menuOpen && r.prevPos != null && r.pos != null) {
            Vec3 a = r.prevPos.add(0, 1.0, 0), b = r.pos.add(0, 1.0, 0);
            if (distPointSegment(c, a, b) <= FlashConstants.BREACH_ENTER_RADIUS) {
                r.menuOpen = true;
                ServerPlayNetworking.send(p, new OpenDimensionMenu());
            }
        }

        if (!r.menuOpen) {                                    // con el menú abierto, la brecha espera
            if (--r.breachTicks <= 0) closeBreach(r);
        }
    }

    private static double distPointSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    public static void closeBreach(PlayerRuntime r) {
        r.breachTicks = 0;
        r.menuOpen = false;
        r.breachCenter = null;
        r.dimCooldown = FlashConstants.DIM_COOLDOWN_TICKS;    // 15 s tras cerrar el portal
    }

    // ------------------------------------------------------------------ elección
    public static void choose(ServerPlayer p, PlayerRuntime r, int index) {
        if (!r.menuOpen) return;

        // Si canceló (index < 0), cerramos la brecha inmediatamente para evitar el bucle de menú
        if (index < 0) {
            closeBreach(r);
            return;
        }

        ResourceKey<Level> targetKey = switch (index) {
            case 0 -> Level.OVERWORLD;
            case 1 -> Level.NETHER;
            case 2 -> Level.END;
            default -> null;
        };
        if (targetKey == null) return;

        ServerLevel from = (ServerLevel) p.level();
        ServerLevel to = from.getServer().getLevel(targetKey);
        if (to == null) return;
        if (from.dimension() == targetKey) {
            p.sendSystemMessage(Component.translatable("message.flash.same_dimension"), true);
            return;
        }

        double x = p.getX(), z = p.getZ();
        if (from.dimension() == Level.OVERWORLD && targetKey == Level.NETHER) {
            x /= 8.0;
            z /= 8.0;
        } else if (from.dimension() == Level.NETHER && targetKey == Level.OVERWORLD) {
            x *= 8.0;
            z *= 8.0;
        }
        BlockPos clamped = to.getWorldBorder().clampToBounds(x, 64, z);

        Vec3 dest;
        if (targetKey == Level.END) dest = buildEndPlatform(to);
        else dest = findSafe(to, clamped.getX(), clamped.getZ(), targetKey == Level.NETHER).orElse(null);

        if (dest == null) {
            p.sendSystemMessage(Component.translatable("message.flash.rift_fail"), true);
            return;
        }

        p.teleport(new TeleportTransition(to, dest, Vec3.ZERO, p.getYRot(), p.getXRot(),
                TeleportTransition.DO_NOTHING));
        r.pos = null;                                         // evita un pico de velocidad medida
        closeBreach(r);
    }

    // ------------------------------------------------------------------ destino seguro
    private static Optional<Vec3> findSafe(ServerLevel lv, int cx, int cz, boolean nether) {
        for (int ring = 0; ring <= 32; ring += 2) {
            for (int dx = -ring; dx <= ring; dx += 2) {
                for (int dz = -ring; dz <= ring; dz += 2) {
                    if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    int x = cx + dx, z = cz + dz;
                    lv.getChunk(x >> 4, z >> 4);               // fuerza carga/generación
                    Optional<Vec3> spot = nether ? scanNether(lv, x, z) : scanSurface(lv, x, z);
                    if (spot.isPresent()) return spot;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Vec3> scanSurface(ServerLevel lv, int x, int z) {
        int top = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return isSafeStand(lv, new BlockPos(x, top, z)) ? Optional.of(Vec3.atBottomCenterOf(new BlockPos(x, top, z)))
                : Optional.empty();
    }

    private static Optional<Vec3> scanNether(ServerLevel lv, int x, int z) {
        for (int y = 120; y > 32; y--) {                       // por debajo del techo de bedrock
            BlockPos feet = new BlockPos(x, y, z);
            if (isSafeStand(lv, feet)) return Optional.of(Vec3.atBottomCenterOf(feet));
        }
        return Optional.empty();
    }

    /**
     * Suelo sólido, sin líquido ni fuego, y 2 bloques libres encima.
     */
    private static boolean isSafeStand(ServerLevel lv, BlockPos feet) {
        BlockPos floor = feet.below();
        BlockState fs = lv.getBlockState(floor);
        if (fs.getCollisionShape(lv, floor).isEmpty() || !fs.getFluidState().isEmpty()
                || fs.is(Blocks.MAGMA_BLOCK) || fs.is(BlockTags.FIRE) || fs.is(Blocks.CACTUS)) return false;
        return isFree(lv, feet) && isFree(lv, feet.above());
    }

    private static boolean isFree(ServerLevel lv, BlockPos pos) {
        BlockState s = lv.getBlockState(pos);
        return s.getCollisionShape(lv, pos).isEmpty() && s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE);
    }

    /**
     * Plataforma de obsidiana 5x5 como la del portal vanilla (100, 49, 0).
     */
    private static Vec3 buildEndPlatform(ServerLevel end) {
        BlockPos center = new BlockPos(100, 49, 0);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                end.setBlockAndUpdate(center.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
                for (int dy = 0; dy <= 2; dy++)
                    end.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        return Vec3.atBottomCenterOf(center);
    }
}
