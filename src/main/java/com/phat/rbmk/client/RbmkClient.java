package com.phat.rbmk.client;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.registry.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = RbmkMod.MODID, dist = Dist.CLIENT)
public class RbmkClient {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    public RbmkClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(RbmkClient::registerClientExtensions);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() { return STILL; }

            @Override
            public ResourceLocation getFlowingTexture() { return FLOW; }

            @Override
            public int getTintColor() { return 0xCCE8E8E8; }
        }, ModFluids.STEAM_TYPE.get());
    }
}
