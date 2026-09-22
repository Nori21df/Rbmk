package com.phat.rbmk.block;

import com.phat.rbmk.block.entity.ReactorControllerBlockEntity;
import com.phat.rbmk.config.RbmkServerConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Nút AZ-5. Đặt trong lớp nắp trên. Chuột phải hoặc xung redstone = SCRAM. */
public class Az5PanelBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final MapCodec<Az5PanelBlock> CODEC = simpleCodec(Az5PanelBlock::new);

    public Az5PanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            trigger(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered) {
                trigger(level, pos, null);
            }
        }
    }

    private static void trigger(Level level, BlockPos pos, Player player) {
        ReactorControllerBlockEntity controller = findController(level, pos);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0f, 0.5f);
        if (controller == null) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.rbmk.az5_no_controller"), true);
            }
            return;
        }
        controller.triggerAz5();
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.rbmk.az5_pressed"), false);
        }
    }

    /** Tìm controller cùng lớp (nắp trên) trong bán kính cấu trúc tối đa. */
    private static ReactorControllerBlockEntity findController(Level level, BlockPos pos) {
        int r = RbmkServerConfig.MAX_RADIUS.get() + 2;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                m.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                if (level.getBlockEntity(m) instanceof ReactorControllerBlockEntity c) {
                    return c;
                }
            }
        }
        return null;
    }
}
