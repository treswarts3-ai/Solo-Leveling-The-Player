package com.tre.sololeveling.equipment;

import java.util.List;
import java.util.Objects;

public record EquipmentDefinition(
        String id,
        EquipmentRarity rarity,
        EquipmentCategory category,
        EquipmentSlotType slot,
        AccessorySlot accessorySlot,
        List<StatBonus> bonuses,
        String setId,
        int maxUpgrade,
        String abilityHook,
        String acquisitionKey
) {
    public EquipmentDefinition {
        id = Objects.requireNonNull(id, "id");
        rarity = Objects.requireNonNull(rarity, "rarity");
        category = Objects.requireNonNull(category, "category");
        slot = Objects.requireNonNull(slot, "slot");
        accessorySlot = Objects.requireNonNull(accessorySlot, "accessorySlot");
        bonuses = List.copyOf(bonuses);
        setId = setId == null ? "" : setId;
        maxUpgrade = Math.max(0, maxUpgrade);
        abilityHook = abilityHook == null ? "" : abilityHook;
        acquisitionKey = acquisitionKey == null ? "unknown" : acquisitionKey;
    }

    public String itemTranslationKey() { return "item.sololeveling." + id; }
    public String abilityTranslationKey() { return "ability.sololeveling." + abilityHook; }
    public String acquisitionTranslationKey() { return "acquisition.sololeveling." + acquisitionKey; }

    /** Documented economy envelope used by tooltips, rewards, selling, and upgrade pricing. */
    public int minimumLevel() { return switch (rarity) {
        case COMMON -> 1; case UNCOMMON -> 10; case RARE -> 20; case EPIC -> 40; case LEGENDARY -> 60; case MONARCH -> 80;
    }; }
    public int maximumLevel() { return rarity == EquipmentRarity.MONARCH ? 1000 : minimumLevel() + 29; }
    public int statBudget() {
        return Math.max(1, (int)Math.round(bonuses.stream().mapToDouble(value -> Math.abs(value.baseValue())).sum()
                + rarity.ordinal() * 5.0D));
    }
    public int sellValue() { return 100 * (rarity.ordinal() + 1) * (rarity.ordinal() + 2) + statBudget() * 12; }
    public int upgradeCost(int currentLevel) {
        return Math.max(50, sellValue() / 3 + (Math.max(0, currentLevel) + 1) * (rarity.ordinal() + 1) * 75);
    }
}
