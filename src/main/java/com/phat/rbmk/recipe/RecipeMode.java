package com.phat.rbmk.recipe;

import com.phat.rbmk.RbmkMod;
import com.phat.rbmk.config.RbmkCommonConfig;
import java.util.List;
import net.neoforged.fml.ModList;

public enum RecipeMode {
    ATM("atm"),
    STANDALONE("standalone");

    /** Đủ các mod này thì bật chế độ ATM và tắt recipe base. */
    public static final List<String> ATM_MODS = List.of(
            "mekanism", "mekanismgenerators", "bigreactors",
            "modern_industrialization", "oritech", "allthemodium");

    private final String id;

    RecipeMode(String id) { this.id = id; }

    public String id() { return id; }

    public static RecipeMode current() {
        String cfg = "auto";
        try {
            cfg = RbmkCommonConfig.RECIPE_MODE.get();
        } catch (IllegalStateException e) {
            RbmkMod.LOGGER.warn("RBMK common config chưa load, dùng recipeMode=auto");
        }
        return switch (cfg) {
            case "atm" -> ATM;
            case "standalone" -> STANDALONE;
            default -> ATM_MODS.stream().allMatch(ModList.get()::isLoaded) ? ATM : STANDALONE;
        };
    }
}
