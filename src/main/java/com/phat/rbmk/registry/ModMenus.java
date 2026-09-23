package com.phat.rbmk.registry;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.menu.ReactorControllerMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RbmkMod.MODID);

    public static final Supplier<MenuType<ReactorControllerMenu>> REACTOR_CONTROLLER = MENUS.register("reactor_controller",
            () -> IMenuTypeExtension.create(ReactorControllerMenu::new));

    private ModMenus() {}
}
