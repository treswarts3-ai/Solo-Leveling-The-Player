package com.tre.sololeveling.equipment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class EquipmentData {
    public static final String ROOT = "SoloLevelingEquipment";
    public static final int DATA_VERSION = 1;

    public static CompoundTag initialize(ItemStack stack, EquipmentDefinition definition) {
        CompoundTag data = stack.getOrCreateTagElement(ROOT);
        String storedId = data.getString("equipment_id");
        if (storedId.isBlank()) {
            data.putString("equipment_id", definition.id());
        } else if (!storedId.equals(definition.id())) {
            data.putString("equipment_id", definition.id());
            data.putInt("upgrade_level", 0);
        }
        data.putInt("data_version", DATA_VERSION);
        if (!data.hasUUID("instance_id")) data.putUUID("instance_id", UUID.randomUUID());
        int level = Math.max(0, Math.min(definition.maxUpgrade(), data.getInt("upgrade_level")));
        data.putInt("upgrade_level", level);
        return data;
    }

    public static int upgradeLevel(ItemStack stack, EquipmentDefinition definition) {
        return initialize(stack, definition).getInt("upgrade_level");
    }

    public static UUID instanceId(ItemStack stack, EquipmentDefinition definition) {
        return initialize(stack, definition).getUUID("instance_id");
    }

    public static boolean setUpgradeLevel(ItemStack stack, EquipmentDefinition definition, int level) {
        CompoundTag data = initialize(stack, definition);
        int previous = data.getInt("upgrade_level");
        int next = Math.max(0, Math.min(definition.maxUpgrade(), level));
        data.putInt("upgrade_level", next);
        return next != previous;
    }

    public static boolean upgrade(ItemStack stack, EquipmentDefinition definition, int amount) {
        if (amount <= 0) return false;
        return setUpgradeLevel(stack, definition, upgradeLevel(stack, definition) + amount);
    }

    public static boolean hasValidData(ItemStack stack, EquipmentDefinition definition) {
        if (!stack.hasTag()) return false;
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT, Tag.TAG_COMPOUND)) return false;
        CompoundTag data = root.getCompound(ROOT);
        return data.getInt("data_version") == DATA_VERSION
                && definition.id().equals(data.getString("equipment_id"))
                && data.hasUUID("instance_id")
                && data.getInt("upgrade_level") >= 0
                && data.getInt("upgrade_level") <= definition.maxUpgrade();
    }

    private EquipmentData() {}
}
