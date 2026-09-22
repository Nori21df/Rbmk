package com.phat.rbmk.block.entity;

import com.phat.rbmk.registry.ModBlockEntities;
import com.phat.rbmk.registry.ModDataComponents;
import com.phat.rbmk.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FuelChannelBlockEntity extends BlockEntity {
    private ItemStack fuel = ItemStack.EMPTY;
    /** Flux lần mô phỏng gần nhất (controller ghi vào), dùng để phạt khi rút nhiên liệu nóng. */
    private double lastFlux;
    /** Đánh dấu khi kênh bị phá bởi sự cố: không rơi nhiên liệu ra. */
    private boolean destroyedByAccident;

    public FuelChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_CHANNEL.get(), pos, state);
    }

    public ItemStack getFuel() { return fuel; }

    public void setFuel(ItemStack stack) {
        this.fuel = stack;
        setChanged();
    }

    public boolean hasActiveFuel() {
        return !fuel.isEmpty() && fuel.is(ModItems.FUEL_ASSEMBLY.get());
    }

    public float getBurnup() {
        return hasActiveFuel() ? fuel.getOrDefault(ModDataComponents.BURNUP.get(), 0.0f) : 1.0f;
    }

    /** Cộng độ cháy. Cạn thì đổi thành bó nhiên liệu đã cháy. */
    public void addBurnup(double amount) {
        if (!hasActiveFuel() || amount <= 0) {
            return;
        }
        float next = (float) Math.min(1.0, getBurnup() + amount);
        if (next >= 1.0f) {
            fuel = new ItemStack(ModItems.SPENT_FUEL_ASSEMBLY.get());
        } else {
            fuel.set(ModDataComponents.BURNUP.get(), next);
        }
        setChanged();
    }

    public double getLastFlux() { return lastFlux; }

    public void setLastFlux(double flux) { this.lastFlux = flux; }

    public boolean isDestroyedByAccident() { return destroyedByAccident; }

    public void destroyFuel() {
        this.destroyedByAccident = true;
        this.fuel = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fuel.isEmpty()) {
            tag.put("fuel", fuel.save(registries));
        }
        tag.putDouble("lastFlux", lastFlux);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.contains("fuel", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound("fuel"))
                : ItemStack.EMPTY;
        lastFlux = tag.getDouble("lastFlux");
    }
}
