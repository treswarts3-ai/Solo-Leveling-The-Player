package com.tre.sololeveling.equipment;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;

public enum EquipmentStat {
    STRENGTH, AGILITY, STAMINA, INTELLIGENCE, SENSE,
    ATTACK_DAMAGE, ATTACK_SPEED, MOVEMENT_SPEED, MAX_HEALTH,
    ARMOR, ARMOR_TOUGHNESS, KNOCKBACK_RESISTANCE, LUCK,
    MANA, SHADOW_CAPACITY;

    public String translationKey() {
        return "stat.sololeveling." + name().toLowerCase();
    }

    public boolean isPercentage() {
        return this == MOVEMENT_SPEED || this == KNOCKBACK_RESISTANCE;
    }

    @Nullable
    public Attribute vanillaAttribute() {
        return switch (this) {
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case ARMOR -> Attributes.ARMOR;
            case ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case KNOCKBACK_RESISTANCE -> Attributes.KNOCKBACK_RESISTANCE;
            case LUCK -> Attributes.LUCK;
            default -> null;
        };
    }
}
