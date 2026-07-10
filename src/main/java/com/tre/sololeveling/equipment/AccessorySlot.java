package com.tre.sololeveling.equipment;

public enum AccessorySlot {
    NONE(""),
    RING("ring"),
    NECKLACE("necklace"),
    BELT("belt"),
    EARRING("earring"),
    HANDS("hands"),
    ORB("orb");

    private final String storageKey;

    AccessorySlot(String storageKey) {
        this.storageKey = storageKey;
    }

    public String storageKey() { return storageKey; }
    public String translationKey() { return "accessory_slot.sololeveling." + name().toLowerCase(); }
}
