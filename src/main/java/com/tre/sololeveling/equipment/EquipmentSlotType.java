package com.tre.sololeveling.equipment;

import net.minecraft.world.entity.EquipmentSlot;

public enum EquipmentSlotType {
    MAIN_HAND, OFF_HAND, EITHER_HAND, HEAD, CHEST, LEGS, FEET, ACCESSORY;

    public boolean accepts(EquipmentSlot slot) {
        return switch (this) {
            case MAIN_HAND -> slot == EquipmentSlot.MAINHAND;
            case OFF_HAND -> slot == EquipmentSlot.OFFHAND;
            case EITHER_HAND -> slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
            case HEAD -> slot == EquipmentSlot.HEAD;
            case CHEST -> slot == EquipmentSlot.CHEST;
            case LEGS -> slot == EquipmentSlot.LEGS;
            case FEET -> slot == EquipmentSlot.FEET;
            case ACCESSORY -> false;
        };
    }

    public String translationKey() {
        return "slot.sololeveling." + name().toLowerCase();
    }
}
