package com.phat.rbmk.registry;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.block.entity.FuelChannelBlockEntity;
import com.phat.rbmk.block.entity.PortBlockEntity;
import com.phat.rbmk.block.entity.ReactorControllerBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RbmkMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<ReactorControllerBlockEntity>> CONTROLLER = BLOCK_ENTITIES.register("reactor_controller",
            () -> BlockEntityType.Builder.of(ReactorControllerBlockEntity::new, ModBlocks.REACTOR_CONTROLLER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<FuelChannelBlockEntity>> FUEL_CHANNEL = BLOCK_ENTITIES.register("fuel_channel",
            () -> BlockEntityType.Builder.of(FuelChannelBlockEntity::new, ModBlocks.FUEL_CHANNEL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<PortBlockEntity>> PORT = BLOCK_ENTITIES.register("port",
            () -> BlockEntityType.Builder.of(PortBlockEntity::new,
                    ModBlocks.COOLANT_PORT.get(), ModBlocks.STEAM_PORT.get(), ModBlocks.ENERGY_PORT.get()).build(null));

    private ModBlockEntities() {}
}
