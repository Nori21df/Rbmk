package com.phat.rbmk.registry;

import com.mojang.serialization.MapCodec;
import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.recipe.PackModeCondition;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RbmkMod.MODID);

    public static final Supplier<MapCodec<PackModeCondition>> PACK_MODE =
            CONDITIONS.register("pack_mode", () -> PackModeCondition.CODEC);

    private ModConditions() {}
}
