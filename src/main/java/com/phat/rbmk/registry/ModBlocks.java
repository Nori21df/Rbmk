package com.phat.rbmk.registry;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RbmkMod.MODID);

    private static BlockBehaviour.Properties metal(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness, resistance)
                .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<Block> NUCLEAR_GRAPHITE_BLOCK = BLOCKS.register("nuclear_graphite_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<Block> SHIELD_CASING = BLOCKS.register("shield_casing",
            () -> new Block(metal(8.0f, 12.0f)));

    public static final DeferredBlock<Block> UPPER_SHIELD = BLOCKS.register("upper_shield",
            () -> new Block(metal(8.0f, 12.0f)));

    public static final DeferredBlock<FuelChannelBlock> FUEL_CHANNEL = BLOCKS.register("fuel_channel",
            () -> new FuelChannelBlock(metal(5.0f, 8.0f)));

    public static final DeferredBlock<Block> CONTROL_ROD = BLOCKS.register("control_rod",
            () -> new Block(metal(5.0f, 8.0f)));

    public static final DeferredBlock<Block> WATER_CHANNEL = BLOCKS.register("water_channel",
            () -> new Block(metal(5.0f, 8.0f)));

    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER = BLOCKS.register("reactor_controller",
            () -> new ReactorControllerBlock(metal(8.0f, 12.0f)));

    public static final DeferredBlock<Az5PanelBlock> AZ5_PANEL = BLOCKS.register("az5_panel",
            () -> new Az5PanelBlock(metal(8.0f, 12.0f)));

    public static final DeferredBlock<PortBlock> COOLANT_PORT = BLOCKS.register("coolant_port",
            () -> new PortBlock(PortBlock.PortType.COOLANT, metal(8.0f, 12.0f)));

    public static final DeferredBlock<PortBlock> STEAM_PORT = BLOCKS.register("steam_port",
            () -> new PortBlock(PortBlock.PortType.STEAM, metal(8.0f, 12.0f)));

    public static final DeferredBlock<PortBlock> ENERGY_PORT = BLOCKS.register("energy_port",
            () -> new PortBlock(PortBlock.PortType.ENERGY, metal(8.0f, 12.0f)));

    public static final DeferredBlock<CoriumBlock> CORIUM = BLOCKS.register("corium",
            () -> new CoriumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(50.0f, 1200.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.BASALT).lightLevel(s -> 11)
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}
}
