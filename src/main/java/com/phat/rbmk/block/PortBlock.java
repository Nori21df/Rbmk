package com.phat.rbmk.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.phat.rbmk.block.entity.PortBlockEntity;
import com.phat.rbmk.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Cổng giao tiếp của lò: nước vào, hơi ra, hoặc FE ra. */
public class PortBlock extends BaseEntityBlock {
    public enum PortType implements StringRepresentable {
        COOLANT("coolant"), STEAM("steam"), ENERGY("energy");

        public static final Codec<PortType> CODEC = StringRepresentable.fromEnum(PortType::values);
        private final String name;

        PortType(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }

    public static final MapCodec<PortBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            PortType.CODEC.fieldOf("port_type").forGetter(PortBlock::getPortType),
            propertiesCodec()
    ).apply(i, PortBlock::new));

    private final PortType portType;

    public PortBlock(PortType portType, Properties properties) {
        super(properties);
        this.portType = portType;
    }

    public PortType getPortType() { return portType; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PORT.get(), PortBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PortBlockEntity port) {
            player.displayClientMessage(port.describe(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public Component typeName() {
        return Component.translatable("block.rbmk." + portType.getSerializedName() + "_port");
    }
}
