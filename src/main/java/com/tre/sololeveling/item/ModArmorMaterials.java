package com.tre.sololeveling.item;

import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;

public enum ModArmorMaterials implements ArmorMaterial {
    HUNTER("hunter", 18, 2, 5, 6, 2, 18, SoundEvents.ARMOR_EQUIP_IRON, 1.5F, 0.05F),
    TRUTH_SEEKER("truth_seeker", 26, 3, 6, 8, 3, 22, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5F, 0.08F),
    SHADOW_MONARCH("shadow_monarch", 34, 4, 7, 9, 4, 28, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.12F);

    private static final EnumMap<ArmorItem.Type, Integer> BASE = new EnumMap<>(ArmorItem.Type.class);
    static {
        BASE.put(ArmorItem.Type.BOOTS, 13);
        BASE.put(ArmorItem.Type.LEGGINGS, 15);
        BASE.put(ArmorItem.Type.CHESTPLATE, 16);
        BASE.put(ArmorItem.Type.HELMET, 11);
    }

    private final String name;
    private final int durabilityMultiplier;
    private final EnumMap<ArmorItem.Type, Integer> defenses = new EnumMap<>(ArmorItem.Type.class);
    private final int enchantment;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;

    ModArmorMaterials(String name, int durabilityMultiplier, int boots, int leggings, int chest, int helmet,
                      int enchantment, SoundEvent equipSound, float toughness, float knockbackResistance) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        defenses.put(ArmorItem.Type.BOOTS, boots);
        defenses.put(ArmorItem.Type.LEGGINGS, leggings);
        defenses.put(ArmorItem.Type.CHESTPLATE, chest);
        defenses.put(ArmorItem.Type.HELMET, helmet);
        this.enchantment = enchantment;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return BASE.get(type) * durabilityMultiplier; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defenses.get(type); }
    @Override public int getEnchantmentValue() { return enchantment; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    @Override public String getName() { return SoloLevelingMod.MODID + ":" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
