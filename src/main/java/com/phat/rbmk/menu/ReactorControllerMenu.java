package com.phat.rbmk.menu;

import com.phat.rbmk.block.entity.ReactorControllerBlockEntity;
import com.phat.rbmk.registry.ModBlocks;
import com.phat.rbmk.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Menu không có slot. Dữ liệu hiển thị đi qua ReactorStatusPayload, nút bấm đi qua clickMenuButton. */
public class ReactorControllerMenu extends AbstractContainerMenu {
    public static final int BTN_MINUS = 0;
    public static final int BTN_PLUS = 1;
    public static final int BTN_MODE = 2;
    public static final int BTN_AZ5 = 3;
    public static final int BTN_RESET = 4;

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    public ReactorControllerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos());
    }

    public ReactorControllerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.REACTOR_CONTROLLER.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.REACTOR_CONTROLLER.get());
    }
}
