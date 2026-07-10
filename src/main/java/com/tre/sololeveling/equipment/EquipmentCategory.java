package com.tre.sololeveling.equipment;

public enum EquipmentCategory {
    DAGGER, SWORD, ARMOR, ACCESSORY;

    public String translationKey() {
        return "category.sololeveling." + name().toLowerCase();
    }
}
