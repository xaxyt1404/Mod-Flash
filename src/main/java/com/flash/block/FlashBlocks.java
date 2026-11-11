package com.flash.block;

import com.flash.FlashMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class FlashBlocks {
    private FlashBlocks() {}

    public static final ResourceKey<Block> VELOZADOR_KEY =
            ResourceKey.create(Registries.BLOCK, FlashMod.id("velozador"));
    public static final ResourceKey<Item> VELOZADOR_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, FlashMod.id("velozador"));

    public static final Block VELOZADOR = Registry.register(BuiltInRegistries.BLOCK, VELOZADOR_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(VELOZADOR_KEY)
                    .mapColor(MapColor.GOLD)
                    .strength(3.5f, 6.0f)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops()));

    public static final Item VELOZADOR_ITEM = Registry.register(BuiltInRegistries.ITEM, VELOZADOR_ITEM_KEY,
            new BlockItem(VELOZADOR, new Item.Properties()
                    .setId(VELOZADOR_ITEM_KEY)
                    .useBlockDescriptionPrefix()));

    public static void init() {
        // Categoría Redstone del creativo. (ItemGroupEvents se renombró a CreativeModeTabEvents en 26.1)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(output -> output.accept(VELOZADOR_ITEM));
    }
}
