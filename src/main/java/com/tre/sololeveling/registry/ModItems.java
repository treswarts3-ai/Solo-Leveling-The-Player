package com.tre.sololeveling.registry;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.equipment.EquipmentCatalog;
import com.tre.sololeveling.equipment.EquipmentDefinition;
import com.tre.sololeveling.item.EquipmentUpgradeMaterialItem;
import com.tre.sololeveling.item.FunctionalItem;
import com.tre.sololeveling.item.HunterAccessoryItem;
import com.tre.sololeveling.item.HunterArmorItem;
import com.tre.sololeveling.item.HunterWeaponItem;
import com.tre.sololeveling.item.ModArmorMaterials;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SoloLevelingMod.MODID);
    public static final List<RegistryObject<Item>> ALL = new ArrayList<>();
    public static final List<RegistryObject<Item>> EQUIPMENT = new ArrayList<>();
    public static final List<RegistryObject<Item>> CONSUMABLES = new ArrayList<>();
    public static final List<RegistryObject<Item>> MATERIALS = new ArrayList<>();

    private static RegistryObject<Item> add(String name, Supplier<? extends Item> supplier, List<RegistryObject<Item>> group) {
        RegistryObject<Item> value = ITEMS.register(name, supplier);
        ALL.add(value);
        group.add(value);
        return value;
    }

    private static RegistryObject<Item> equipment(String name, Supplier<? extends Item> supplier) {
        return add(name, supplier, EQUIPMENT);
    }

    private static RegistryObject<Item> material(String name, Supplier<? extends Item> supplier) {
        return add(name, supplier, MATERIALS);
    }

    private static RegistryObject<Item> consumable(String name, Supplier<? extends Item> supplier) {
        return add(name, supplier, CONSUMABLES);
    }

    private static RegistryObject<Item> weapon(String name, int damage, float speed) {
        EquipmentDefinition definition = EquipmentCatalog.require(name);
        return equipment(name, () -> new HunterWeaponItem(Tiers.NETHERITE, damage, speed,
                new Item.Properties().durability(1200 + definition.maxUpgrade() * 80).rarity(definition.rarity().vanilla()), definition));
    }

    private static RegistryObject<Item> armor(String name, ModArmorMaterials material, ArmorItem.Type type) {
        EquipmentDefinition definition = EquipmentCatalog.require(name);
        return equipment(name, () -> new HunterArmorItem(material, type,
                new Item.Properties().rarity(definition.rarity().vanilla()), definition));
    }

    private static RegistryObject<Item> story(String name, String detail) {
        return material(name, () -> new FunctionalItem(new Item.Properties(), FunctionalItem.Kind.STORY, detail));
    }

    private static RegistryObject<Item> accessory(String name) {
        EquipmentDefinition definition = EquipmentCatalog.require(name);
        return equipment(name, () -> new HunterAccessoryItem(
                new Item.Properties().stacksTo(1).rarity(definition.rarity().vanilla()), definition));
    }

    private static RegistryObject<Item> rune(String name, String skill) {
        return material(name, () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                FunctionalItem.Kind.RUNE, "A crystalline skill record", skill));
    }

    public static final RegistryObject<Item> KIM_SANGSHIK_STEEL_SWORD = weapon("kim_sangshiks_steel_sword", 4, -2.4F);
    public static final RegistryObject<Item> TRAINING_DAGGER = weapon("training_dagger", 3, -1.8F);
    public static final RegistryObject<Item> KASAKAS_VENOM_FANG = weapon("kasakas_venom_fang", 6, -1.7F);
    public static final RegistryObject<Item> KNIGHT_KILLER = weapon("knight_killer", 8, -1.6F);
    public static final RegistryObject<Item> BARUKAS_DAGGER = weapon("barukas_dagger", 10, -1.4F);
    public static final RegistryObject<Item> DEMON_KINGS_DAGGER_LEFT = weapon("demon_kings_dagger_left", 12, -1.35F);
    public static final RegistryObject<Item> DEMON_KINGS_DAGGER_RIGHT = weapon("demon_kings_dagger_right", 12, -1.35F);
    public static final RegistryObject<Item> DEMON_KINGS_LONGSWORD = weapon("demon_kings_longsword", 14, -2.8F);
    public static final RegistryObject<Item> MOONSHADOW_DAGGER = weapon("moonshadow_dagger", 15, -1.25F);
    public static final RegistryObject<Item> SHADOW_REAPER_DAGGER = weapon("shadow_reaper_dagger", 17, -1.2F);
    public static final RegistryObject<Item> KAMISHS_WRATH_LEFT = weapon("kamishs_wrath_left", 16, -1.25F);
    public static final RegistryObject<Item> KAMISHS_WRATH_RIGHT = weapon("kamishs_wrath_right", 16, -1.25F);
    public static final RegistryObject<Item> ANTARES_FANG = weapon("antares_fang", 18, -1.2F);

    public static final RegistryObject<Item> NOVICE_HUNTER_HOOD = armor("novice_hunter_hood", ModArmorMaterials.HUNTER, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> NOVICE_HUNTER_JACKET = armor("novice_hunter_jacket", ModArmorMaterials.HUNTER, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> NOVICE_HUNTER_LEGGINGS = armor("novice_hunter_leggings", ModArmorMaterials.HUNTER, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> NOVICE_HUNTER_BOOTS = armor("novice_hunter_boots", ModArmorMaterials.HUNTER, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> HIGH_KNIGHT_HELMET = armor("high_knight_helmet", ModArmorMaterials.HUNTER, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> HIGH_KNIGHT_CHESTPLATE = armor("high_knight_chestplate", ModArmorMaterials.HUNTER, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> HIGH_KNIGHT_LEGGINGS = armor("high_knight_leggings", ModArmorMaterials.HUNTER, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> HIGH_KNIGHT_BOOTS = armor("high_knight_boots", ModArmorMaterials.HUNTER, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> ASSASSINS_HOOD = armor("assassins_hood", ModArmorMaterials.HUNTER, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> ASSASSINS_JACKET = armor("assassins_jacket", ModArmorMaterials.HUNTER, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> ASSASSINS_TROUSERS = armor("assassins_trousers", ModArmorMaterials.HUNTER, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> ASSASSINS_SHOES = armor("assassins_shoes", ModArmorMaterials.HUNTER, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> RED_KNIGHTS_HELMET = armor("red_knights_helmet", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> TRUTH_SEEKERS_TOP = armor("truth_seekers_top", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> TRUTH_SEEKERS_PANTS = armor("truth_seekers_pants", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> TRUTH_SEEKERS_SHOES = armor("truth_seekers_shoes", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> SHADOW_MONARCH_HOOD = armor("shadow_monarch_hood", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> SHADOW_MONARCH_COAT = armor("shadow_monarch_coat", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> SHADOW_MONARCH_TROUSERS = armor("shadow_monarch_trousers", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> SHADOW_MONARCH_BOOTS = armor("shadow_monarch_boots", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.BOOTS);

    public static final RegistryObject<Item> HIGH_KNIGHT_GAUNTLETS = accessory("high_knight_gauntlets");
    public static final RegistryObject<Item> ARCHERS_GLOVES = accessory("archers_gloves");
    public static final RegistryObject<Item> HIGH_MAGICIANS_RING = accessory("high_magicians_ring");
    public static final RegistryObject<Item> GATEKEEPERS_NECKLACE = accessory("gatekeepers_necklace");
    public static final RegistryObject<Item> DEMON_MONARCH_EARRING = accessory("demon_monarch_earring");
    public static final RegistryObject<Item> DEMON_MONARCH_NECKLACE = accessory("demon_monarch_necklace");
    public static final RegistryObject<Item> DEMON_MONARCH_RING = accessory("demon_monarch_ring");
    public static final RegistryObject<Item> TRUTH_SEEKERS_GLOVES = accessory("truth_seekers_gloves");
    public static final RegistryObject<Item> SHADOW_MONARCH_GLOVES = accessory("shadow_monarch_gloves");
    public static final RegistryObject<Item> ORB_OF_AVARICE = accessory("orb_of_avarice");
    public static final RegistryObject<Item> HUNTERS_RING = accessory("hunters_ring");
    public static final RegistryObject<Item> MANA_WEAVE_NECKLACE = accessory("mana_weave_necklace");
    public static final RegistryObject<Item> SHADOW_BELT = accessory("shadow_belt");
    public static final RegistryObject<Item> MONARCH_EARRINGS = accessory("monarch_earrings");

    public static final RegistryObject<Item> DUNGEON_KEY = story("dungeon_key", "Opens System-marked dungeon gates");
    public static final RegistryObject<Item> DEMON_CASTLE_KEY = story("demon_castle_key", "A high-level progression key");
    public static final RegistryObject<Item> CASTLE_DOOR_KEY = story("castle_door_key", "A sealed castle access key");
    public static final RegistryObject<Item> TELEPORTATION_STONE = consumable("teleportation_stone", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.TELEPORT, "Returns the player to world spawn"));
    public static final RegistryObject<Item> ESSENCE_STONE = story("essence_stone", "Condensed magical essence");
    public static final RegistryObject<Item> MANA_CRYSTAL = story("mana_crystal", "A material saturated with mana");
    public static final RegistryObject<Item> RUNE_STONE = story("rune_stone", "An unidentified skill rune");
    public static final RegistryObject<Item> SKILL_RUNE_STEALTH = rune("skill_rune_stealth", "stealth");
    public static final RegistryObject<Item> SKILL_RUNE_QUICKSILVER = rune("skill_rune_quicksilver", "quicksilver");
    public static final RegistryObject<Item> SKILL_RUNE_BLOODLUST = rune("skill_rune_bloodlust", "bloodlust");
    public static final RegistryObject<Item> SKILL_RUNE_MUTILATION = rune("skill_rune_mutilation", "mutilation");
    public static final RegistryObject<Item> SKILL_RUNE_DAGGER_RUSH = rune("skill_rune_dagger_rush", "dagger_rush");
    public static final RegistryObject<Item> BLACK_HEART = material("black_heart", () -> new FunctionalItem(
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), FunctionalItem.Kind.BLACK_HEART, "Greatly expands mana and Shadow capacity"));
    public static final RegistryObject<Item> HOLY_WATER_OF_LIFE = consumable("holy_water_of_life", () -> new FunctionalItem(
            new Item.Properties().stacksTo(4).rarity(Rarity.EPIC), FunctionalItem.Kind.HOLY_WATER, "Restores health and removes common harmful effects"));
    public static final RegistryObject<Item> HEALING_POTION = consumable("healing_potion", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16), FunctionalItem.Kind.HEALING, "Restores 4 hearts"));
    public static final RegistryObject<Item> GREATER_HEALING_POTION = consumable("greater_healing_potion", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.GREATER_HEALING, "Fully restores health"));
    public static final RegistryObject<Item> MANA_POTION = consumable("mana_potion", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16), FunctionalItem.Kind.MANA, "Restores 60 mana"));
    public static final RegistryObject<Item> GREATER_MANA_POTION = consumable("greater_mana_potion", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.GREATER_MANA, "Fully restores mana"));
    public static final RegistryObject<Item> BLESSED_RANDOM_BOX = consumable("blessed_random_box", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.RANDOM_BOX, "Contains a favorable System reward"));
    public static final RegistryObject<Item> CURSED_RANDOM_BOX = consumable("cursed_random_box", () -> new FunctionalItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.RANDOM_BOX, "Contains a dangerous but valuable reward"));
    public static final RegistryObject<Item> KASAKA_VENOM = story("kasaka_venom", "Potent venom crafting material");
    public static final RegistryObject<Item> RAIKAN_FANG = story("raikan_fang", "A sharp magical beast material");
    public static final RegistryObject<Item> CERBERUS_FANG = story("cerberus_fang", "Unobtainable quest material; no boss is included");
    public static final RegistryObject<Item> ENCHANTED_STEEL = story("enchanted_steel", "Steel infused with condensed mana");
    public static final RegistryObject<Item> EQUIPMENT_FRAGMENT = story("equipment_fragment", "A fragment recovered from damaged hunter equipment");
    public static final RegistryObject<Item> QUEST_RELIC = story("quest_relic", "A relic marked by the System");
    public static final RegistryObject<Item> SHADOW_SILK = story("shadow_silk", "Thread spun from condensed shadow");
    public static final RegistryObject<Item> REINFORCEMENT_CORE = material("reinforcement_core", () -> new EquipmentUpgradeMaterialItem(
            new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1, "crafted"));
    public static final RegistryObject<Item> MONARCH_REINFORCEMENT_CORE = material("monarch_reinforcement_core", () -> new EquipmentUpgradeMaterialItem(
            new Item.Properties().stacksTo(8).rarity(Rarity.EPIC), 2, "monarch_cache"));

    private ModItems() {}
}
