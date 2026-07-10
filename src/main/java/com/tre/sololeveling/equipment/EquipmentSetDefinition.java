package com.tre.sololeveling.equipment;

import java.util.List;
import java.util.Set;

public record EquipmentSetDefinition(
        String id,
        Set<String> requiredItems,
        List<StatBonus> bonuses,
        String abilityHook
) {
    public EquipmentSetDefinition {
        requiredItems = Set.copyOf(requiredItems);
        bonuses = List.copyOf(bonuses);
        abilityHook = abilityHook == null ? "" : abilityHook;
    }

    public String translationKey() { return "set.sololeveling." + id; }
}
