package com.phat.rbmk;

import com.mojang.logging.LogUtils;
import com.phat.rbmk.config.RbmkCommonConfig;
import com.phat.rbmk.config.RbmkServerConfig;
import com.phat.rbmk.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(RbmkMod.MODID)
public class RbmkMod {
    public static final String MODID = "rbmk";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RbmkMod(IEventBus modBus, ModContainer container) {
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModConditions.CONDITIONS.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(ModCapabilities::register);
        modBus.addListener(com.phat.rbmk.network.ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, RbmkCommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, RbmkServerConfig.SPEC);
    }
}
