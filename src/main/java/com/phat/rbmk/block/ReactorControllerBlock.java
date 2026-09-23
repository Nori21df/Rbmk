package com.phat.rbmk.block;

import com.mojang.serialization.MapCodec;
import com.phat.rbmk.block.entity.ReactorControllerBlockEntity;
import com.phat.rbmk.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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

/**
 * Não của lò. Đặt ở TÂM lớp nắp trên.
 * Redstone vào = mức công suất mong muốn (0 = tắt, 15 = 150%).
 * Chuột phải: xem trạng thái. Shift + chuột phải: reset AZ-5.
 * Comparator: nhiệt độ cao nhất so với ngưỡng vỡ kênh.
 */
public class ReactorControllerBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorControllerBlock> CODEC = simpleCodec(ReactorControllerBlock::new);

    public ReactorControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.CONTROLLER.get(), ReactorControllerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                if (controller.resetAz5()) {
                    player.displayClientMessage(Component.translatable("message.rbmk.az5_reset"), false);
                } else {
                    player.displayClientMessage(Component.translatable("message.rbmk.az5_not_latched"), true);
                }
            } else if (player instanceof ServerPlayer sp) {
                sp.openMenu(controller, pos);
                controller.sendStatusTo(sp);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity c) {
            for (var handler : new net.neoforged.neoforge.items.ItemStackHandler[]{c.getFuelInput(), c.getSpentOutput()}) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity c ? c.getComparatorOutput() : 0;
    }
}
