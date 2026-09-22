package com.phat.rbmk.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Condition "rbmk:pack_mode": recipe chỉ load khi chế độ hiện tại khớp. */
public record PackModeCondition(String mode) implements ICondition {
    public static final MapCodec<PackModeCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("mode").forGetter(PackModeCondition::mode)
    ).apply(i, PackModeCondition::new));

    @Override
    public boolean test(IContext context) {
        return RecipeMode.current().id().equals(mode);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
