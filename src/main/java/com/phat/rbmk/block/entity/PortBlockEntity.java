package com.phat.rbmk.block.entity;

import com.phat.rbmk.block.PortBlock;
import com.phat.rbmk.registry.ModBlockEntities;
import com.phat.rbmk.registry.ModFluids;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class PortBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 64_000;
    public static final int ENERGY_CAPACITY = 64_000_000;
    public static final int MAX_FE_PUSH = 1_000_000;
    public static final int MAX_STEAM_PUSH = 8_000;

    private final FluidTank tank;
    private final PortEnergy energy = new PortEnergy(ENERGY_CAPACITY, MAX_FE_PUSH);

    public PortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORT.get(), pos, state);
        PortBlock.PortType type = typeOf(state);
        if (type == PortBlock.PortType.STEAM) {
            tank = new FluidTank(TANK_CAPACITY, fs -> fs.is(ModFluids.STEAM.get()));
        } else {
            tank = new FluidTank(TANK_CAPACITY, fs -> fs.is(Fluids.WATER));
        }
    }

    private static PortBlock.PortType typeOf(BlockState state) {
        return state.getBlock() instanceof PortBlock pb ? pb.getPortType() : PortBlock.PortType.COOLANT;
    }

    public PortBlock.PortType getPortType() { return typeOf(getBlockState()); }

    // ---------- Capabilities ----------

    @Nullable
    public IEnergyStorage getEnergyCapability() {
        return getPortType() == PortBlock.PortType.ENERGY ? energy : null;
    }

    @Nullable
    public IFluidHandler getFluidCapability() {
        return getPortType() == PortBlock.PortType.ENERGY ? null : tank;
    }

    // ---------- Dùng bởi controller ----------

    /** Lượng nước hiện có (chỉ Coolant Port). */
    public int getWater() {
        return getPortType() == PortBlock.PortType.COOLANT ? tank.getFluidAmount() : 0;
    }

    /** Rút nước, trả về lượng thực rút được. */
    public int drainWater(int amount) {
        if (getPortType() != PortBlock.PortType.COOLANT || amount <= 0) {
            return 0;
        }
        int drained = tank.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (drained > 0) setChanged();
        return drained;
    }

    /** Đẩy hơi vào tank, trả về lượng nhận được. */
    public int acceptSteam(int amount) {
        if (getPortType() != PortBlock.PortType.STEAM || amount <= 0) {
            return 0;
        }
        int filled = tank.fill(new FluidStack(ModFluids.STEAM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) setChanged();
        return filled;
    }

    /** Dung lượng FE còn trống (chỉ Energy Port). */
    public long energySpace() {
        return getPortType() == PortBlock.PortType.ENERGY ? (long) energy.getMaxEnergyStored() - energy.getEnergyStored() : 0;
    }

    public int acceptEnergy(long fe) {
        if (getPortType() != PortBlock.PortType.ENERGY || fe <= 0) {
            return 0;
        }
        int added = energy.addInternal((int) Math.min(Integer.MAX_VALUE, fe));
        if (added > 0) setChanged();
        return added;
    }

    // ---------- Tick: tự đẩy hơi / FE ra xung quanh ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortBlockEntity be) {
        PortBlock.PortType type = be.getPortType();
        if (type == PortBlock.PortType.ENERGY && be.energy.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (level.getBlockEntity(np) instanceof PortBlockEntity) continue;
                IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                if (target == null || !target.canReceive()) continue;
                int offer = Math.min(be.energy.getEnergyStored(), MAX_FE_PUSH);
                int accepted = target.receiveEnergy(offer, false);
                if (accepted > 0) {
                    be.energy.extractEnergy(accepted, false);
                    be.setChanged();
                }
                if (be.energy.getEnergyStored() <= 0) break;
            }
        } else if (type == PortBlock.PortType.STEAM && be.tank.getFluidAmount() > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (level.getBlockEntity(np) instanceof PortBlockEntity) continue;
                IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, np, dir.getOpposite());
                if (target == null) continue;
                FluidStack offer = be.tank.getFluid().copyWithAmount(Math.min(be.tank.getFluidAmount(), MAX_STEAM_PUSH));
                int filled = target.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    be.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    be.setChanged();
                }
                if (be.tank.getFluidAmount() <= 0) break;
            }
        }
    }

    public Component describe() {
        return switch (getPortType()) {
            case COOLANT -> Component.translatable("message.rbmk.port_coolant", tank.getFluidAmount(), TANK_CAPACITY);
            case STEAM -> Component.translatable("message.rbmk.port_steam", tank.getFluidAmount(), TANK_CAPACITY);
            case ENERGY -> Component.translatable("message.rbmk.port_energy", energy.getEnergyStored(), ENERGY_CAPACITY);
        };
    }

    // ---------- NBT ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("energy", energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        if (tag.contains("energy")) {
            energy.deserializeNBT(registries, tag.get("energy"));
        }
    }

    /** Kho FE: bên ngoài chỉ được rút, lò nạp qua addInternal. */
    private static final class PortEnergy extends EnergyStorage {
        PortEnergy(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract);
        }

        int addInternal(int amount) {
            int added = Math.min(capacity - energy, Math.max(0, amount));
            energy += added;
            return added;
        }
    }
}
