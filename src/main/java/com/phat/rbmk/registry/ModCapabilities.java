package com.phat.rbmk.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PORT.get(),
                (be, side) -> be.getEnergyCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.PORT.get(),
                (be, side) -> be.getFluidCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CONTROLLER.get(),
                (be, side) -> be.getAutomationHandler());
    }

    private ModCapabilities() {}
}
