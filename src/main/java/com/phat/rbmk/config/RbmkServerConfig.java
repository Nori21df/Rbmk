package com.phat.rbmk.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** SERVER config: nằm trong serverconfig của từng world. */
public final class RbmkServerConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ---- Hazards ----
    public static final ModConfigSpec.BooleanValue EXPLOSIONS_ENABLED;
    public static final ModConfigSpec.BooleanValue STEAM_VENT;
    public static final ModConfigSpec.BooleanValue AUTO_PROTECTION;
    public static final ModConfigSpec.BooleanValue BLOCK_DAMAGE;
    public static final ModConfigSpec.BooleanValue AUTO_SCRAM_ON_UNLOAD;
    public static final ModConfigSpec.DoubleValue MAX_EXPLOSION_POWER;
    public static final ModConfigSpec.DoubleValue RUPTURE_TEMP;
    public static final ModConfigSpec.DoubleValue MELTDOWN_TEMP;
    public static final ModConfigSpec.DoubleValue CRIT_POWER_MULT;
    public static final ModConfigSpec.DoubleValue ORM_WARNING;

    // ---- Structure ----
    public static final ModConfigSpec.IntValue MAX_RADIUS;
    public static final ModConfigSpec.IntValue MAX_HEIGHT;
    public static final ModConfigSpec.IntValue SIM_INTERVAL;

    // ---- Balance ----
    public static final ModConfigSpec.DoubleValue HEAT_PER_MB;
    public static final ModConfigSpec.DoubleValue FE_PER_MB;
    public static final ModConfigSpec.DoubleValue BURN_SECONDS;

    static {
        B.push("hazards");
        EXPLOSIONS_ENABLED = B.comment("Cho phép nổ khi vỡ kênh / meltdown").define("explosionsEnabled", true);
        BLOCK_DAMAGE = B.comment("Vụ nổ có phá block không (false = chỉ sát thương)").define("blockDamage", true);
        STEAM_VENT = B.comment("Hơi không thoát được qua port thì tự xả ra ngoài (mất năng lượng nhưng lò vẫn được làm mát).",
                "false = hơi bị nghẽn, lò mất làm mát (khó hơn)").define("steamVent", true);
        AUTO_PROTECTION = B.comment("Bảo vệ tự động: tự bấm AZ-5 khi thiếu nước, nghẽn hơi hoặc kênh quá nóng.",
                "Chernobyl xảy ra khi người vận hành tắt hệ thống này.").define("autoProtection", true);
        AUTO_SCRAM_ON_UNLOAD = B.comment("Tự SCRAM an toàn khi chunk bị unload").define("autoScramOnUnload", true);
        MAX_EXPLOSION_POWER = B.comment("Sức nổ tối đa của meltdown (TNT = 4)").defineInRange("maxExplosionPower", 14.0, 0.0, 64.0);
        RUPTURE_TEMP = B.comment("Nhiệt độ vỡ kênh (°C)").defineInRange("ruptureTemp", 950.0, 400.0, 5000.0);
        MELTDOWN_TEMP = B.comment("Nhiệt độ meltdown toàn lò (°C)").defineInRange("meltdownTemp", 1500.0, 500.0, 10000.0);
        CRIT_POWER_MULT = B.comment("Công suất trung bình vượt N lần danh định -> nổ hơi").defineInRange("criticalPowerMultiplier", 5.0, 1.5, 100.0);
        ORM_WARNING = B.comment("Rod rút quá mức này (0..1, 1 = cắm hết) khi lò chạy -> cảnh báo ORM").defineInRange("ormWarningInsertion", 0.15, 0.0, 1.0);
        B.pop();

        B.push("structure");
        MAX_RADIUS = B.comment("Bán kính lõi lục giác tối đa (không tính tường)").defineInRange("maxRadius", 7, 2, 12);
        MAX_HEIGHT = B.comment("Chiều cao lõi tối đa").defineInRange("maxHeight", 10, 2, 16);
        SIM_INTERVAL = B.comment("Số tick giữa mỗi bước mô phỏng").defineInRange("simInterval", 5, 1, 20);
        B.pop();

        B.push("balance");
        HEAT_PER_MB = B.comment("Nhiệt (HU) cần để tạo 1 mB hơi. Mỗi khối nhiên liệu sinh 1000 HU/s ở công suất danh định")
                .defineInRange("heatPerMb", 2.5, 0.1, 1000.0);
        FE_PER_MB = B.comment("FE mà Energy Port tạo ra từ 1 mB hơi").defineInRange("fePerMbSteam", 20.0, 0.0, 10000.0);
        BURN_SECONDS = B.comment("Số giây chạy ở 100% để 1 bó nhiên liệu cháy hết").defineInRange("fuelBurnSeconds", 7200.0, 60.0, 1.0e7);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private RbmkServerConfig() {}
}
