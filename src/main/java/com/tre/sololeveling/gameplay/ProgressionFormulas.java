package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.config.ModConfigs;

public final class ProgressionFormulas {
    public static int xpNeeded(int level) {
        int safeLevel = Math.max(1, level);
        double raw = ModConfigs.XP_CURVE_BASE.get() * Math.pow(safeLevel, ModConfigs.XP_CURVE_EXPONENT.get())
                + ModConfigs.XP_CURVE_LINEAR.get() * Math.max(0, safeLevel - 1);
        return (int)Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(raw)));
    }

    public static double strengthDamageBonus(int strength) {
        return Math.max(0, strength - 1) * ModConfigs.STRENGTH_DAMAGE_PER_POINT.get();
    }

    public static double agilitySpeedBonus(int agility) {
        return Math.min(ModConfigs.AGILITY_SPEED_CAP.get(), Math.max(0, agility - 1) * ModConfigs.AGILITY_SPEED_PER_POINT.get());
    }

    public static double staminaHealthBonus(int stamina) {
        return Math.min(ModConfigs.STAMINA_HEALTH_CAP.get(), Math.max(0, stamina - 1) * ModConfigs.STAMINA_HEALTH_PER_POINT.get());
    }

    public static int baseMaxMana(int level, int intelligence) {
        long value = (long)ModConfigs.BASE_MANA.get()
                + (long)Math.max(1, level) * ModConfigs.MANA_PER_LEVEL.get()
                + (long)Math.max(1, intelligence) * ModConfigs.MANA_PER_INTELLIGENCE.get();
        return (int)Math.max(1L, Math.min(Integer.MAX_VALUE, value));
    }

    public static float healthRegenPerSecond(int stamina) {
        double value = ModConfigs.HEALTH_REGEN_BASE.get()
                + Math.max(0, stamina - 1) * ModConfigs.HEALTH_REGEN_PER_STAMINA.get();
        return (float)Math.max(0.0D, Math.min(ModConfigs.HEALTH_REGEN_CAP.get(), value));
    }

    public static int manaRegenPerSecond(int intelligence) {
        double value = ModConfigs.MANA_REGEN_BASE.get()
                + Math.max(0, intelligence - 1) * ModConfigs.MANA_REGEN_PER_INTELLIGENCE.get();
        return (int)Math.max(0, Math.min(Integer.MAX_VALUE, Math.floor(value)));
    }

    public static double criticalChance(int agility, int sense) {
        double chance = ModConfigs.CRITICAL_BASE_CHANCE.get()
                + Math.max(0, sense - 1) * ModConfigs.CRITICAL_CHANCE_PER_SENSE.get()
                + Math.max(0, agility - 1) * ModConfigs.CRITICAL_CHANCE_PER_AGILITY.get();
        return clamp01(Math.min(ModConfigs.CRITICAL_CHANCE_CAP.get(), chance));
    }

    public static double criticalDamageMultiplier(int strength, int sense) {
        double multiplier = ModConfigs.CRITICAL_DAMAGE_BASE.get()
                + Math.max(0, strength - 1) * ModConfigs.CRITICAL_DAMAGE_PER_STRENGTH.get()
                + Math.max(0, sense - 1) * ModConfigs.CRITICAL_DAMAGE_PER_SENSE.get();
        return Math.max(1.0D, Math.min(ModConfigs.CRITICAL_DAMAGE_CAP.get(), multiplier));
    }

    public static double evasionChance(int agility, int sense) {
        double chance = Math.max(0, agility - 1) * ModConfigs.EVASION_PER_AGILITY.get()
                + Math.max(0, sense - 1) * ModConfigs.EVASION_PER_SENSE.get();
        return clamp01(Math.min(ModConfigs.EVASION_CAP.get(), chance));
    }

    private static double clamp01(double value) { return Math.max(0.0D, Math.min(1.0D, value)); }
    private ProgressionFormulas() {}
}
