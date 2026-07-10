package com.tre.sololeveling.equipment;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EquipmentConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_UPGRADES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SET_BONUSES;
    public static final ForgeConfigSpec.DoubleValue UPGRADE_BONUS_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue EFFECT_REFRESH_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("equipment");
        ENABLE_UPGRADES = builder.comment("Allow reinforcement materials to upgrade equipment.")
                .define("enableUpgrades", true);
        ENABLE_SET_BONUSES = builder.comment("Apply equipment set bonuses.")
                .define("enableSetBonuses", true);
        UPGRADE_BONUS_MULTIPLIER = builder.comment("Multiplier for per-upgrade stat growth.")
                .defineInRange("upgradeBonusMultiplier", 1.0D, 0.0D, 10.0D);
        EFFECT_REFRESH_TICKS = builder.comment("How often equipped transient modifiers are reconciled.")
                .defineInRange("effectRefreshTicks", 20, 1, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private EquipmentConfig() {}
}
