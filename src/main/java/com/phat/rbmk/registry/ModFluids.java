package com.phat.rbmk.registry;

import com.phat.rbmk.RbmkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Hơi nước dạng fluid, gắn tag c:steam để turbine của mod khác nhận. Không có block/xô. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RbmkMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RbmkMod.MODID);

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.rbmk.steam")
                    .density(-1000)
                    .viscosity(200)
                    .temperature(573)
                    .canConvertToSource(false)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM = FLUIDS.register("steam",
            () -> new BaseFlowingFluid.Source(properties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_STEAM = FLUIDS.register("flowing_steam",
            () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, FLOWING_STEAM);
    }

    private ModFluids() {}
}
