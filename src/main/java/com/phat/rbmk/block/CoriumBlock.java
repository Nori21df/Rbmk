package com.phat.rbmk.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Corium: lõi nóng chảy sau meltdown. Đứng lên là bỏng + nhiễm xạ. */
public class CoriumBlock extends Block {
    public static final MapCodec<CoriumBlock> CODEC = simpleCodec(CoriumBlock::new);

    public CoriumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().hotFloor(), 3.0f);
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 60));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.05, 0.0);
        }
    }
}
