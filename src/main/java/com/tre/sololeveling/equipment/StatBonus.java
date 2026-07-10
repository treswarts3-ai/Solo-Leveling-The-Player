package com.tre.sololeveling.equipment;

public record StatBonus(EquipmentStat stat, double baseValue, double perUpgrade) {
    public double valueAt(int upgradeLevel, double upgradeMultiplier) {
        return baseValue + Math.max(0, upgradeLevel) * perUpgrade * Math.max(0.0D, upgradeMultiplier);
    }

    public static StatBonus of(EquipmentStat stat, double value) {
        return new StatBonus(stat, value, 0.0D);
    }

    public static StatBonus scaling(EquipmentStat stat, double baseValue, double perUpgrade) {
        return new StatBonus(stat, baseValue, perUpgrade);
    }
}
