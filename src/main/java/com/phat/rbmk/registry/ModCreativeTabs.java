package com.phat.rbmk.registry;

import com.phat.rbmk.RbmkMod;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RbmkMod.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rbmk"))
            .icon(() -> new ItemStack(ModItems.REACTOR_CONTROLLER.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.REACTOR_CONTROLLER.get());
                out.accept(ModItems.AZ5_PANEL.get());
                out.accept(ModItems.UPPER_SHIELD.get());
                out.accept(ModItems.SHIELD_CASING.get());
                out.accept(ModItems.NUCLEAR_GRAPHITE_BLOCK.get());
                out.accept(ModItems.FUEL_CHANNEL.get());
                out.accept(ModItems.CONTROL_ROD.get());
                out.accept(ModItems.WATER_CHANNEL.get());
                out.accept(ModItems.COOLANT_PORT.get());
                out.accept(ModItems.STEAM_PORT.get());
                out.accept(ModItems.ENERGY_PORT.get());
                out.accept(ModItems.FUEL_ASSEMBLY.get());
                out.accept(ModItems.SPENT_FUEL_ASSEMBLY.get());
                out.accept(ModItems.ENRICHED_PELLET.get());
                out.accept(ModItems.UO2_POWDER.get());
                out.accept(ModItems.PRESSURE_TUBE.get());
                out.accept(ModItems.ZIRCALOY_INGOT.get());
                out.accept(ModItems.NUCLEAR_GRAPHITE_BILLET.get());
                out.accept(ModItems.CORIUM.get());
            })
            .build());

    private ModCreativeTabs() {}
}
