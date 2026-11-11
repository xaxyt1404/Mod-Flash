package com.flash.net;

import com.flash.FlashMod;
import com.flash.server.FlashServer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Todos los CustomPayload del mod + su registro. Cliente -> servidor: intenciones. Servidor -> cliente: estado. */
public final class FlashNetwork {
    private FlashNetwork() {}

    // ------------------------- C2S -------------------------
    public record ToggleSpeed() implements CustomPacketPayload {
        public static final Type<ToggleSpeed> TYPE = new Type<>(FlashMod.id("toggle_speed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSpeed> CODEC = StreamCodec.unit(new ToggleSpeed());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** delta = +1 / -1 (Alt Izq + rueda). */
    public record ChangeLevel(int delta) implements CustomPacketPayload {
        public static final Type<ChangeLevel> TYPE = new Type<>(FlashMod.id("change_level"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeLevel> CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, ChangeLevel::delta, ChangeLevel::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 0 = V Vibración, 1 = C Flashtime, 2 = G Rayo, 3 = B Tornado. */
    public record UseAbility(int ability) implements CustomPacketPayload {
        public static final int VIBRATION = 0, FLASHTIME = 1, RAY = 2, TORNADO = 3;
        public static final Type<UseAbility> TYPE = new Type<>(FlashMod.id("use_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UseAbility> CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, UseAbility::ability, UseAbility::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 0 Overworld, 1 Nether, 2 End, -1 = cerró el menú sin elegir. */
    public record ChooseDimension(int index) implements CustomPacketPayload {
        public static final Type<ChooseDimension> TYPE = new Type<>(FlashMod.id("choose_dimension"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChooseDimension> CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, ChooseDimension::index, ChooseDimension::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ------------------------- S2C -------------------------
    /** Estado completo para HUD y flags de colisión. energy10/dim10 = valor * 10 (0..1000). */
    public record SyncState(int energy10, int dim10, int level, int flags) implements CustomPacketPayload {
        public static final int UNLOCKED = 1, SUPER = 2, FATIGUE = 4, VIBRATION = 8, FLASHTIME = 16,
                TERRAIN = 32, LOCKED = 64, CINEMATIC = 128, BREACH = 256;

        public static final Type<SyncState> TYPE = new Type<>(FlashMod.id("sync_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncState> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SyncState::energy10,
                ByteBufCodecs.INT, SyncState::dim10,
                ByteBufCodecs.INT, SyncState::level,
                ByteBufCodecs.INT, SyncState::flags,
                SyncState::new);
        public boolean has(int flag) { return (flags & flag) != 0; }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenDimensionMenu() implements CustomPacketPayload {
        public static final Type<OpenDimensionMenu> TYPE = new Type<>(FlashMod.id("open_dimension_menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenDimensionMenu> CODEC =
                StreamCodec.unit(new OpenDimensionMenu());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ------------------------- Registro -------------------------
    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(ToggleSpeed.TYPE, ToggleSpeed.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChangeLevel.TYPE, ChangeLevel.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UseAbility.TYPE, UseAbility.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChooseDimension.TYPE, ChooseDimension.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncState.TYPE, SyncState.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenDimensionMenu.TYPE, OpenDimensionMenu.CODEC);

        // El servidor NUNCA confía en el cliente: solo recibe intenciones y las valida.
        ServerPlayNetworking.registerGlobalReceiver(ToggleSpeed.TYPE,
                (payload, ctx) -> FlashServer.onToggleSpeed(ctx.player()));
        ServerPlayNetworking.registerGlobalReceiver(ChangeLevel.TYPE,
                (payload, ctx) -> FlashServer.onChangeLevel(ctx.player(), payload.delta()));
        ServerPlayNetworking.registerGlobalReceiver(UseAbility.TYPE,
                (payload, ctx) -> FlashServer.onAbility(ctx.player(), payload.ability()));
        ServerPlayNetworking.registerGlobalReceiver(ChooseDimension.TYPE,
                (payload, ctx) -> FlashServer.onChooseDimension(ctx.player(), payload.index()));
    }
}
