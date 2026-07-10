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
}
