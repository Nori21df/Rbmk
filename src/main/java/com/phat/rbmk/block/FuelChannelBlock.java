package com.phat.rbmk.block;

import com.mojang.serialization.MapCodec;
import com.phat.rbmk.block.entity.FuelChannelBlockEntity;
import com.phat.rbmk.item.FuelAssemblyItem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Kênh nhiên liệu: chứa 1 bó nhiên liệu. Chuột phải bằng bó nhiên liệu để nạp, tay không để rút. */
public class FuelChannelBlock extends BaseEntityBlock {
    public static final MapCodec<FuelChannelBlock> CODEC = simpleCodec(FuelChannelBlock::new);

    public FuelChannelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelChannelBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FuelAssemblyItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof FuelChannelBlockEntity channel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!channel.getFuel().isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.rbmk.channel_full"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            channel.setFuel(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.6f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FuelChannelBlockEntity channel)) {
            return InteractionResult.PASS;
        }
        ItemStack fuel = channel.getFuel();
        if (fuel.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.rbmk.channel_empty"), true);
            return InteractionResult.SUCCESS;
        }
        // Rút nhiên liệu khi lò còn đang phân hạch = ăn phóng xạ
        if (channel.getLastFlux() > 0.02) {
            int amp = channel.getLastFlux() > 0.5 ? 2 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, amp));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.hurt(level.damageSources().magic(), (float) Math.min(12.0, 2.0 + channel.getLastFlux() * 6.0));
            player.displayClientMessage(Component.translatable("message.rbmk.hot_fuel"), false);
        }
        player.getInventory().placeItemBackInInventory(fuel);
        channel.setFuel(ItemStack.EMPTY);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.6f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FuelChannelBlockEntity channel) {
            if (!channel.isDestroyedByAccident() && !channel.getFuel().isEmpty()) {
                popResource(level, pos, channel.getFuel());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
