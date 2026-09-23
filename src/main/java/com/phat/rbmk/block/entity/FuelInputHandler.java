package com.phat.rbmk.block.entity;

import com.phat.rbmk.item.FuelAssemblyItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Ô chứa nhiên liệu mới của controller: chỉ nhận Fuel Assembly chưa cháy hết. */
public class FuelInputHandler extends ItemStackHandler {
    public static final int SLOTS = 9;
    private final Runnable onChange;

    public FuelInputHandler(Runnable onChange) {
        super(SLOTS);
        this.onChange = onChange;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof FuelAssemblyItem && FuelAssemblyItem.burnup(stack) < 1.0f;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }
}
