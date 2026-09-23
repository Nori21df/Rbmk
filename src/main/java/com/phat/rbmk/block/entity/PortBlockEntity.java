package com.phat.rbmk.block.entity;

import com.phat.rbmk.block.PortBlock;
import com.phat.rbmk.registry.ModBlockEntities;
import com.phat.rbmk.registry.ModFluids;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Cổng của lò. Coolant/Steam Port có bình chứa riêng.
 * Energy Port không có kho riêng: nó là "vòi" rút FE từ kho chung (kiểu long) trong controller,
 * nên không bị giới hạn 2,1 tỉ FE của int.
 */
public class PortBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 256_000;
    public static final int MAX_STEAM_PUSH = 64_000;

    private final FluidTank tank;
    private final IEnergyStorage linkedEnergy = new LinkedEnergy();
    @Nullable private BlockPos controllerPos;

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

    /** Controller gọi mỗi lần validate cấu trúc. */
    public void linkController(BlockPos pos) {
        if (!pos.equals(controllerPos)) {
            controllerPos = pos.immutable();
            setChanged();
        }
    }

    @Nullable
    private ReactorControllerBlockEntity controller() {
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) return null;
        return level.getBlockEntity(controllerPos) instanceof ReactorControllerBlockEntity c ? c : null;
    }

    // ---------- Capabilities ----------

    @Nullable
    public IEnergyStorage getEnergyCapability() {
        return getPortType() == PortBlock.PortType.ENERGY ? linkedEnergy : null;
    }

    @Nullable
    public IFluidHandler getFluidCapability() {
        return getPortType() == PortBlock.PortType.ENERGY ? null : tank;
    }

    // ---------- Dùng bởi controller ----------

    public int getFluidAmount() { return tank.getFluidAmount(); }

    public int getWater() {
        return getPortType() == PortBlock.PortType.COOLANT ? tank.getFluidAmount() : 0;
    }

    public int drainWater(int amount) {
        if (getPortType() != PortBlock.PortType.COOLANT || amount <= 0) return 0;
        int drained = tank.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (drained > 0) setChanged();
        return drained;
    }

    public int acceptSteam(int amount) {
        if (getPortType() != PortBlock.PortType.STEAM || amount <= 0) return 0;
        int filled = tank.fill(new FluidStack(ModFluids.STEAM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) setChanged();
        return filled;
    }

    // ---------- Tick: tự đẩy hơi / FE ra xung quanh ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortBlockEntity be) {
        PortBlock.PortType type = be.getPortType();
        if (type == PortBlock.PortType.ENERGY) {
            if (be.linkedEnergy.getEnergyStored() <= 0) return;
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                BlockEntity nbe = level.getBlockEntity(np);
                if (nbe instanceof PortBlockEntity || nbe instanceof ReactorControllerBlockEntity) continue;
                IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, dir.getOpposite());
                if (target == null || !target.canReceive()) continue;
                int offer = be.linkedEnergy.extractEnergy(Integer.MAX_VALUE, true);
                if (offer <= 0) break;
                int accepted = target.receiveEnergy(offer, false);
                if (accepted > 0) be.linkedEnergy.extractEnergy(accepted, false);
            }
        } else if (type == PortBlock.PortType.STEAM && be.tank.getFluidAmount() > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                BlockEntity nbe = level.getBlockEntity(np);
                if (nbe instanceof PortBlockEntity || nbe instanceof ReactorControllerBlockEntity) continue;
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
            case ENERGY -> {
                ReactorControllerBlockEntity c = controller();
                yield c == null ? Component.translatable("message.rbmk.port_unlinked")
                        : Component.translatable("message.rbmk.port_energy", ReactorControllerBlockEntity.formatBig(c.getFeStored()),
                        ReactorControllerBlockEntity.formatBig(ReactorControllerBlockEntity.FE_CAPACITY));
            }
        };
    }

    // ---------- NBT ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        if (controllerPos != null) tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        controllerPos = NbtUtils.readBlockPos(tag, "controller").orElse(null);
    }

    /** Rút FE từ kho chung của controller. */
    private final class LinkedEnergy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int max, boolean simulate) { return 0; }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            ReactorControllerBlockEntity c = controller();
            return c == null ? 0 : c.extractFe(max, simulate);
        }

        @Override
        public int getEnergyStored() {
            ReactorControllerBlockEntity c = controller();
            return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getFeStored());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, ReactorControllerBlockEntity.FE_CAPACITY);
        }

        @Override
        public boolean canExtract() { return true; }

        @Override
        public boolean canReceive() { return false; }
    }
}
