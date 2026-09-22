package com.phat.rbmk.registry;

import com.mojang.serialization.Codec;
import com.phat.rbmk.RbmkMod;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RbmkMod.MODID);

    /** Độ cháy nhiên liệu 0.0 (mới) → 1.0 (cạn). */
    public static final Supplier<DataComponentType<Float>> BURNUP = DATA_COMPONENTS.register("burnup",
            () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

    private ModDataComponents() {}
}
