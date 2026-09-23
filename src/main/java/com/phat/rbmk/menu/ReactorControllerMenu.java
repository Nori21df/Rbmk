package com.phat.rbmk.menu;

import com.phat.rbmk.block.entity.FuelInputHandler;
import com.phat.rbmk.block.entity.ReactorControllerBlockEntity;
import com.phat.rbmk.block.entity.SpentOutputHandler;
import com.phat.rbmk.item.FuelAssemblyItem;
import com.phat.rbmk.registry.ModBlocks;
import com.phat.rbmk.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu của controller. Slot 0–8: nạp nhiên liệu, 9–17: nhiên liệu đã cháy (chỉ lấy ra),
 * 18–53: túi đồ người chơi. Số liệu lò đi qua ReactorStatusPayload, nút bấm qua clickMenuButton.
 */
public class ReactorControllerMenu extends AbstractContainerMenu {
    public static final int BTN_MINUS = 0;
    public static final int BTN_PLUS = 1;
    public static final int BTN_MODE = 2;
    public static final int BTN_AZ5 = 3;
    public static final int BTN_RESET = 4;

    // Vị trí slot trong GUI (dùng chung với Screen)
    public static final int INV_TOP = 238;
    public static final int FUEL_X = 9;
    public static final int SPENT_X = 71;
    public static final int PLAYER_X = 175;

    private static final int FUEL_END = FuelInputHandler.SLOTS;
    private static final int SPENT_END = FUEL_END + SpentOutputHandler.SLOTS;
    private static final int PLAYER_END = SPENT_END + 36;

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    /** Client. */
    public ReactorControllerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), new FuelInputHandler(() -> {}), new SpentOutputHandler(() -> {}));
    }

    /** Server. */
    public ReactorControllerMenu(int containerId, Inventory inventory, BlockPos pos,
                                 FuelInputHandler fuel, SpentOutputHandler spent) {
        super(ModMenus.REACTOR_CONTROLLER.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);

        for (int i = 0; i < FuelInputHandler.SLOTS; i++) {
            addSlot(new SlotItemHandler(fuel, i, FUEL_X + (i % 3) * 18, INV_TOP + (i / 3) * 18));
        }
        for (int i = 0; i < SpentOutputHandler.SLOTS; i++) {
            addSlot(new OutputSlot(spent, i, SPENT_X + (i % 3) * 18, INV_TOP + (i / 3) * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_X + col * 18, INV_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_X + col * 18, INV_TOP + 58));
        }
    }

    public BlockPos getPos() { return pos; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().getBlockEntity(pos) instanceof ReactorControllerBlockEntity be) {
            be.handleButton(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SPENT_END) {
            // Kho lò -> túi đồ
            if (!moveItemStackTo(stack, SPENT_END, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FuelAssemblyItem) {
            // Túi đồ -> ô nạp nhiên liệu
            if (!moveItemStackTo(stack, 0, FUEL_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.REACTOR_CONTROLLER.get());
    }

    /** Ô chỉ để lấy ra. */
    private static final class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
