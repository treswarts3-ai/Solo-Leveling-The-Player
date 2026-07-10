package com.tre.sololeveling.registry;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.item.FunctionalItem;
import com.tre.sololeveling.item.HunterWeaponItem;
import com.tre.sololeveling.item.HunterArmorItem;
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

    private static RegistryObject<Item> add(String name, Supplier<? extends Item> supplier) {
        RegistryObject<Item> value = ITEMS.register(name, supplier);
        ALL.add(value);
        return value;
    }

    private static RegistryObject<Item> weapon(String name, int damage, float speed, Rarity rarity, String rank, String special) {
        return add(name, () -> new HunterWeaponItem(Tiers.NETHERITE, damage, speed, new Item.Properties().durability(1200).rarity(rarity), rank, special));
    }

    private static RegistryObject<Item> armor(String name, ModArmorMaterials material, ArmorItem.Type type, Rarity rarity, String rank, String detail, String setName) {
        return add(name, () -> new HunterArmorItem(material, type, new Item.Properties().rarity(rarity), rank, detail, setName));
    }

    private static RegistryObject<Item> story(String name, String detail) {
        return add(name, () -> new FunctionalItem(new Item.Properties(), FunctionalItem.Kind.STORY, detail));
    }

    private static RegistryObject<Item> accessory(String name, String detail, String slot) {
        return add(name, () -> new FunctionalItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), FunctionalItem.Kind.ACCESSORY, detail, slot));
    }

    private static RegistryObject<Item> rune(String name, String skill) {
        return add(name, () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.RUNE, "A crystalline skill record", skill));
    }

    public static final RegistryObject<Item> KIM_SANGSHIK_STEEL_SWORD = weapon("kim_sangshiks_steel_sword", 4, -2.4F, Rarity.COMMON, "E-Rank", "Simple steel weapon");
    public static final RegistryObject<Item> KASAKAS_VENOM_FANG = weapon("kasakas_venom_fang", 6, -1.7F, Rarity.UNCOMMON, "C-Rank", "Applies venom and paralysis");
    public static final RegistryObject<Item> KNIGHT_KILLER = weapon("knight_killer", 8, -1.6F, Rarity.RARE, "B-Rank", "Deals bonus damage to armored targets");
    public static final RegistryObject<Item> BARUKAS_DAGGER = weapon("barukas_dagger", 10, -1.4F, Rarity.RARE, "A-Rank", "Grants agility while held");
    public static final RegistryObject<Item> DEMON_KINGS_DAGGER_LEFT = weapon("demon_kings_dagger_left", 12, -1.35F, Rarity.EPIC, "S-Rank", "Two as One: pair with the right dagger");
    public static final RegistryObject<Item> DEMON_KINGS_DAGGER_RIGHT = weapon("demon_kings_dagger_right", 12, -1.35F, Rarity.EPIC, "S-Rank", "Two as One: pair with the left dagger");
    public static final RegistryObject<Item> DEMON_KINGS_LONGSWORD = weapon("demon_kings_longsword", 14, -2.8F, Rarity.EPIC, "S-Rank", "Storm of White Flames");
    public static final RegistryObject<Item> KAMISHS_WRATH_LEFT = weapon("kamishs_wrath_left", 16, -1.25F, Rarity.EPIC, "National-Level", "Emissive Strength-scaling dagger");
    public static final RegistryObject<Item> KAMISHS_WRATH_RIGHT = weapon("kamishs_wrath_right", 16, -1.25F, Rarity.EPIC, "National-Level", "Emissive Strength-scaling dagger");
    public static final RegistryObject<Item> ANTARES_FANG = weapon("antares_fang", 18, -1.2F, Rarity.EPIC, "Post-100", "Administrator-only future weapon");

    public static final RegistryObject<Item> HIGH_KNIGHT_HELMET = armor("high_knight_helmet", ModArmorMaterials.HUNTER, ArmorItem.Type.HELMET, Rarity.RARE, "B-Rank", "Heavy protection and stability", "High Knight");
    public static final RegistryObject<Item> HIGH_KNIGHT_CHESTPLATE = armor("high_knight_chestplate", ModArmorMaterials.HUNTER, ArmorItem.Type.CHESTPLATE, Rarity.RARE, "B-Rank", "Heavy protection and stability", "High Knight");
    public static final RegistryObject<Item> HIGH_KNIGHT_LEGGINGS = armor("high_knight_leggings", ModArmorMaterials.HUNTER, ArmorItem.Type.LEGGINGS, Rarity.RARE, "B-Rank", "Heavy protection and stability", "High Knight");
    public static final RegistryObject<Item> HIGH_KNIGHT_BOOTS = armor("high_knight_boots", ModArmorMaterials.HUNTER, ArmorItem.Type.BOOTS, Rarity.RARE, "B-Rank", "Heavy protection and stability", "High Knight");
    public static final RegistryObject<Item> ASSASSINS_HOOD = armor("assassins_hood", ModArmorMaterials.HUNTER, ArmorItem.Type.HELMET, Rarity.RARE, "A-Rank", "Lightweight concealment gear", "Assassin");
    public static final RegistryObject<Item> ASSASSINS_JACKET = armor("assassins_jacket", ModArmorMaterials.HUNTER, ArmorItem.Type.CHESTPLATE, Rarity.RARE, "A-Rank", "Lightweight concealment gear", "Assassin");
    public static final RegistryObject<Item> ASSASSINS_TROUSERS = armor("assassins_trousers", ModArmorMaterials.HUNTER, ArmorItem.Type.LEGGINGS, Rarity.RARE, "A-Rank", "Lightweight concealment gear", "Assassin");
    public static final RegistryObject<Item> ASSASSINS_SHOES = armor("assassins_shoes", ModArmorMaterials.HUNTER, ArmorItem.Type.BOOTS, Rarity.RARE, "A-Rank", "Lightweight concealment gear", "Assassin");
    public static final RegistryObject<Item> RED_KNIGHTS_HELMET = armor("red_knights_helmet", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.HELMET, Rarity.EPIC, "S-Rank", "A crimson helm reinforced with mana", "Red Knight");
    public static final RegistryObject<Item> TRUTH_SEEKERS_TOP = armor("truth_seekers_top", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.CHESTPLATE, Rarity.RARE, "A-Rank", "Mana-conductive combat clothing", "Truth Seeker");
    public static final RegistryObject<Item> TRUTH_SEEKERS_PANTS = armor("truth_seekers_pants", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.LEGGINGS, Rarity.RARE, "A-Rank", "Mana-conductive combat clothing", "Truth Seeker");
    public static final RegistryObject<Item> TRUTH_SEEKERS_SHOES = armor("truth_seekers_shoes", ModArmorMaterials.TRUTH_SEEKER, ArmorItem.Type.BOOTS, Rarity.RARE, "A-Rank", "Mana-conductive combat clothing", "Truth Seeker");
    public static final RegistryObject<Item> SHADOW_MONARCH_COAT = armor("shadow_monarch_coat", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.CHESTPLATE, Rarity.EPIC, "Monarch", "Armor woven from condensed shadow", "Shadow Monarch");
    public static final RegistryObject<Item> SHADOW_MONARCH_TROUSERS = armor("shadow_monarch_trousers", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.LEGGINGS, Rarity.EPIC, "Monarch", "Armor woven from condensed shadow", "Shadow Monarch");
    public static final RegistryObject<Item> SHADOW_MONARCH_BOOTS = armor("shadow_monarch_boots", ModArmorMaterials.SHADOW_MONARCH, ArmorItem.Type.BOOTS, Rarity.EPIC, "Monarch", "Armor woven from condensed shadow", "Shadow Monarch");

    public static final RegistryObject<Item> HIGH_KNIGHT_GAUNTLETS = accessory("high_knight_gauntlets", "Strength and physical resistance", "hands");
    public static final RegistryObject<Item> ARCHERS_GLOVES = accessory("archers_gloves", "Sense and ranged handling", "hands");
    public static final RegistryObject<Item> HIGH_MAGICIANS_RING = accessory("high_magicians_ring", "Intelligence, mana and regeneration", "ring");
    public static final RegistryObject<Item> GATEKEEPERS_NECKLACE = accessory("gatekeepers_necklace", "Agility and Sense", "necklace");
    public static final RegistryObject<Item> DEMON_MONARCH_EARRING = accessory("demon_monarch_earring", "Demon Monarch set piece", "earring");
    public static final RegistryObject<Item> DEMON_MONARCH_NECKLACE = accessory("demon_monarch_necklace", "Demon Monarch set piece", "necklace");
    public static final RegistryObject<Item> DEMON_MONARCH_RING = accessory("demon_monarch_ring", "Demon Monarch set piece", "ring");
    public static final RegistryObject<Item> TRUTH_SEEKERS_GLOVES = accessory("truth_seekers_gloves", "Truth Seeker set piece", "hands");
    public static final RegistryObject<Item> SHADOW_MONARCH_GLOVES = accessory("shadow_monarch_gloves", "Shadow capacity and dagger handling", "hands");

    public static final RegistryObject<Item> DUNGEON_KEY = story("dungeon_key", "Opens System-marked dungeon gates");
    public static final RegistryObject<Item> DEMON_CASTLE_KEY = story("demon_castle_key", "A high-level progression key");
    public static final RegistryObject<Item> CASTLE_DOOR_KEY = story("castle_door_key", "A sealed castle access key");
    public static final RegistryObject<Item> TELEPORTATION_STONE = add("teleportation_stone", () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.TELEPORT, "Returns the player to world spawn"));
    public static final RegistryObject<Item> ESSENCE_STONE = story("essence_stone", "Condensed magical essence");
    public static final RegistryObject<Item> MANA_CRYSTAL = story("mana_crystal", "A material saturated with mana");
    public static final RegistryObject<Item> RUNE_STONE = story("rune_stone", "An unidentified skill rune");
    public static final RegistryObject<Item> SKILL_RUNE_STEALTH = rune("skill_rune_stealth", "stealth");
    public static final RegistryObject<Item> SKILL_RUNE_QUICKSILVER = rune("skill_rune_quicksilver", "quicksilver");
    public static final RegistryObject<Item> SKILL_RUNE_BLOODLUST = rune("skill_rune_bloodlust", "bloodlust");
    public static final RegistryObject<Item> SKILL_RUNE_MUTILATION = rune("skill_rune_mutilation", "mutilation");
    public static final RegistryObject<Item> SKILL_RUNE_DAGGER_RUSH = rune("skill_rune_dagger_rush", "dagger_rush");
    public static final RegistryObject<Item> ORB_OF_AVARICE = accessory("orb_of_avarice", "Amplifies magic ability power", "orb");
    public static final RegistryObject<Item> BLACK_HEART = add("black_heart", () -> new FunctionalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), FunctionalItem.Kind.BLACK_HEART, "Greatly expands mana and Shadow capacity"));
    public static final RegistryObject<Item> HOLY_WATER_OF_LIFE = add("holy_water_of_life", () -> new FunctionalItem(new Item.Properties().stacksTo(4).rarity(Rarity.EPIC), FunctionalItem.Kind.HOLY_WATER, "Restores health and removes common harmful effects"));
    public static final RegistryObject<Item> HEALING_POTION = add("healing_potion", () -> new FunctionalItem(new Item.Properties().stacksTo(16), FunctionalItem.Kind.HEALING, "Restores 4 hearts"));
    public static final RegistryObject<Item> GREATER_HEALING_POTION = add("greater_healing_potion", () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.GREATER_HEALING, "Fully restores health"));
    public static final RegistryObject<Item> MANA_POTION = add("mana_potion", () -> new FunctionalItem(new Item.Properties().stacksTo(16), FunctionalItem.Kind.MANA, "Restores 60 mana"));
    public static final RegistryObject<Item> GREATER_MANA_POTION = add("greater_mana_potion", () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.GREATER_MANA, "Fully restores mana"));
    public static final RegistryObject<Item> BLESSED_RANDOM_BOX = add("blessed_random_box", () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.RANDOM_BOX, "Contains a favorable System reward"));
    public static final RegistryObject<Item> CURSED_RANDOM_BOX = add("cursed_random_box", () -> new FunctionalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), FunctionalItem.Kind.RANDOM_BOX, "Contains a dangerous but valuable reward"));
    public static final RegistryObject<Item> KASAKA_VENOM = story("kasaka_venom", "Potent venom crafting material");
    public static final RegistryObject<Item> RAIKAN_FANG = story("raikan_fang", "A sharp magical beast material");
    public static final RegistryObject<Item> CERBERUS_FANG = story("cerberus_fang", "Unobtainable quest material; no boss is included");

    private ModItems() {}
}
