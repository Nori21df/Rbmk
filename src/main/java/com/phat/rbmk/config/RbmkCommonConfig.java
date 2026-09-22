package com.phat.rbmk.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** COMMON config: được load trước datapack, nên dùng được trong recipe condition. */
public final class RbmkCommonConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> RECIPE_MODE = B
            .comment("Chế độ recipe: auto = tự phát hiện (đủ 6 mod ATM -> atm, thiếu -> standalone), atm, standalone",
                     "Recipe mode: auto | atm | standalone")
            .defineInList("recipeMode", "auto", List.of("auto", "atm", "standalone"));

    public static final ModConfigSpec SPEC = B.build();

    private RbmkCommonConfig() {}
}
