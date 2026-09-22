package com.phat.rbmk.hazard;

import com.phat.rbmk.block.entity.FuelChannelBlockEntity;
import com.phat.rbmk.config.RbmkServerConfig;
import com.phat.rbmk.multiblock.ChannelType;
import com.phat.rbmk.multiblock.HexStructure;
import com.phat.rbmk.registry.ModBlocks;
import com.phat.rbmk.sim.ReactorSim;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Các sự cố: vỡ kênh và meltdown (nổ hơi + cháy graphite + corium). */
public final class ReactorHazards {
    private ReactorHazards() {}

    /** Vỡ 1 kênh nhiên liệu: cột đó thành corium, nổ nhỏ, bật nắp phía trên. */
    public static void ruptureChannel(ServerLevel level, HexStructure s, HexStructure.Cell cell) {
        for (BlockPos p : cell.blocks()) {
            if (level.getBlockEntity(p) instanceof FuelChannelBlockEntity fc) {
                fc.destroyFuel();
            }
            level.setBlockAndUpdate(p, ModBlocks.CORIUM.get().defaultBlockState());
        }
        BlockPos top = cell.blocks().get(0);
        BlockPos lidPos = top.above();
        launch(level, lidPos, level.getRandom(), 0.8);

        broadcast(level, s.center(), 64, Component.translatable("message.rbmk.rupture",
                lidPos.getX(), lidPos.getY(), lidPos.getZ()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        irradiate(level, s.center(), 16, 20 * 20);

        if (RbmkServerConfig.EXPLOSIONS_ENABLED.get()) {
            level.explode(null, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, 3.0f, false, interaction());
        }
    }

    /** Meltdown: nổ hơi, nắp Elena bay, lõi thành corium, cháy graphite, nhiễm xạ. */
    public static void meltdown(ServerLevel level, HexStructure s, ReactorSim sim) {
        RandomSource rand = level.getRandom();
        BlockPos c = s.center();
        broadcast(level, c, 128, Component.translatable("message.rbmk.meltdown",
                Math.round(sim.avgPower() * 100), Math.round(sim.maxTemp())).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        // 1) Lõi -> corium
        List<BlockPos> graphite = new ArrayList<>();
        for (HexStructure.Cell cell : s.cells()) {
            for (BlockPos p : cell.blocks()) {
                if (level.getBlockEntity(p) instanceof FuelChannelBlockEntity fc) {
                    fc.destroyFuel();
                }
                if (cell.type() == ChannelType.FUEL || rand.nextFloat() < 0.35f) {
                    level.setBlockAndUpdate(p, ModBlocks.CORIUM.get().defaultBlockState());
                } else if (cell.type() == ChannelType.GRAPHITE) {
                    graphite.add(p);
                }
            }
        }

        // 2) Nắp "Elena" bị hất tung
        for (BlockPos p : s.lid()) {
            if (rand.nextFloat() < 0.65f) {
                launch(level, p, rand, 1.0 + rand.nextDouble() * 0.8);
            }
        }

        // 3) Nhiễm xạ người chơi xung quanh
        irradiate(level, c, 48, 20 * 60);

        // 4) Nổ hơi
        if (RbmkServerConfig.EXPLOSIONS_ENABLED.get()) {
            float power = (float) Math.min(RbmkServerConfig.MAX_EXPLOSION_POWER.get(),
                    4.0 + s.radius() * 1.2 + s.height() * 0.4);
            double y = s.topY() - s.height() / 2.0;
            level.explode(null, c.getX() + 0.5, y, c.getZ() + 0.5, power, true, interaction());
        }

        // 5) Cháy graphite
        for (BlockPos p : graphite) {
            if (rand.nextFloat() < 0.3f) {
                BlockPos up = p.above();
                if (level.isEmptyBlock(up)) {
                    level.setBlockAndUpdate(up, BaseFireBlock.getState(level, up));
                }
            }
        }
    }

    private static void launch(ServerLevel level, BlockPos pos, RandomSource rand, double upward) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.UPPER_SHIELD.get()) && !state.is(ModBlocks.AZ5_PANEL.get())) {
            return;
        }
        FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, state);
        fb.setDeltaMovement((rand.nextDouble() - 0.5) * 0.6, upward, (rand.nextDouble() - 0.5) * 0.6);
        fb.hurtMarked = true;
    }

    private static void irradiate(ServerLevel level, BlockPos c, int radius, int ticks) {
        for (Player p : level.players()) {
            double d2 = p.distanceToSqr(c.getX(), c.getY(), c.getZ());
            if (d2 < (double) radius * radius) {
                double strength = 1.0 - Math.sqrt(d2) / radius;
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (ticks * strength) + 40, strength > 0.5 ? 1 : 0));
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            }
        }
    }

    private static void broadcast(ServerLevel level, BlockPos c, int radius, Component msg) {
        for (Player p : level.players()) {
            if (p.distanceToSqr(c.getX(), c.getY(), c.getZ()) < (double) radius * radius) {
                p.displayClientMessage(msg, false);
            }
        }
    }

    private static Level.ExplosionInteraction interaction() {
        return RbmkServerConfig.BLOCK_DAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
    }
}
