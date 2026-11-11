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
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Orquestador del lado servidor: tick, eventos de daño, recepción de payloads y sincronización.
 */
public final class FlashServer {
    private FlashServer() {
    }

    /**
     * true mientras el mod aplica daño propio (evita recursión del Golpe de Masa).
     */
    private static boolean internalDamage;

    public static void guarded(Runnable r) {
        boolean prev = internalDamage;
        internalDamage = true;
        try {
            r.run();
        } finally {
            internalDamage = prev;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(FlashServer::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(FlashServer::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(FlashServer::afterDamage);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PlayerRuntime.discard(handler.getPlayer().getUUID()));

        // Entregar el libro de guía por primera vez al unirse
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
            if (!d.hasReceivedGuide()) {
                p.getInventory().add(createGuideBook());
                p.setAttached(FlashAttachments.FLASH_DATA, d.withReceivedGuide(true));
            }
        });
    }

    /**
     * Crea un libro nativo escrito con la guía in-game.
     */
    public static ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<Component> pages = List.of(
                Component.empty()
                        .append(Component.literal("GUÍA SPEED FORCE\n\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED).withBold(false)))
                        .append(Component.literal("1. El Velozador\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal("Para obtener los poderes craftea el bloque principal:\n\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("- Fila 1:").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_GRAY).withBold(false)))
                        .append(Component.literal(" Blaze, Cobre, Blaze\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("- Fila 2:").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_GRAY).withBold(false)))
                        .append(Component.literal(" Glowstone, Redstone, Glowstone\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("- Fila 3:").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_GRAY).withBold(false)))
                        .append(Component.literal(" Oro, Cobre, Oro").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false))),

                Component.empty()
                        .append(Component.literal("2. El Ritual\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal("1. Coloca El Velozador a ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("2 bloques").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal(" de altura (Overworld).\n\n2. Pon un ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("Pararrayos").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal(" encima del bloque.\n\n3. Párate ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("DEBAJO").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal(" del bloque. Caerá un rayo que activará tu conexión.").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false))),

                Component.empty()
                        .append(Component.literal("3. Controles\n\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal("[R]").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED).withBold(false)))
                        .append(Component.literal(" Activar Supervelocidad.\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("[Alt+Rueda]").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED).withBold(false)))
                        .append(Component.literal(" Nivel (1 al 5).\n\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("Barra Speed Force:\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal("Regenera +5/s en reposo. Al llegar a 0 entras en ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("Fatiga").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.RED).withBold(false)))
                        .append(Component.literal(" por 3 seg.").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false))),

                Component.empty()
                        .append(Component.literal("4. Habilidades\n\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal("[V] Vibración:").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_BLUE).withBold(false)))
                        .append(Component.literal(" Intangibilidad.\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("[C] Flashtime:").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GOLD).withBold(false)))
                        .append(Component.literal(" Ralentizar área.\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("[G] Rayo:").withStyle(s -> s.withColor(ChatFormatting.GOLD).withBold(false)))
                        .append(Component.literal(" Disparo eléctrico.\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("[B] Tornado:").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_PURPLE).withBold(false)))
                        .append(Component.literal(" Levantar objetivo.").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false))),

                Component.empty()
                        .append(Component.literal("5. Brecha Dimensional\n").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal("Corre a ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
                        .append(Component.literal("Nivel 5").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(false)))
                        .append(Component.literal(" en línea recta durante 10 s para cargar la barra secundaria al 100%.\n\nSe abrirá el portal para viajar a un lugar seguro en Overworld, Nether o End.").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.BLACK).withBold(false)))
        );

        WrittenBookContent content = new WrittenBookContent(
                net.minecraft.server.network.Filterable.passThrough("Guía de la Speed Force"),
                "YesidP",
                0,
                pages.stream().map(net.minecraft.server.network.Filterable::passThrough).toList(),
                true
        );

        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }


    // ===================================================================== tick
    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive()) {
                PlayerRuntime.discard(p.getUUID());
                continue;
            }

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

    /**
     * Golpe de Masa + Robo de Velocidad. Se aplica DESPUÉS del golpe vanilla para que el daño sea aditivo.
     */
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
            default -> {
            }
        }
    }

    public static void onChooseDimension(ServerPlayer p, int index) {
        FlashData d = p.getAttachedOrCreate(FlashAttachments.FLASH_DATA);
        if (!d.unlocked()) return;
        DimensionRift.choose(p, PlayerRuntime.get(p), index);
    }
}