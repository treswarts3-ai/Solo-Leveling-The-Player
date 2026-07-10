package com.tre.sololeveling.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.tre.sololeveling.equipment.EquipmentCategory.*;
import static com.tre.sololeveling.equipment.EquipmentRarity.*;
import static com.tre.sololeveling.equipment.EquipmentSlotType.*;
import static com.tre.sololeveling.equipment.EquipmentStat.*;

public final class EquipmentCatalog {
    private static final Map<String, EquipmentDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, EquipmentSetDefinition> SETS = new LinkedHashMap<>();

    static {
        weapon("kim_sangshiks_steel_sword", COMMON, SWORD, EITHER_HAND, 3, "none", "crafted",
                StatBonus.of(STRENGTH, 1), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.25));
        weapon("training_dagger", COMMON, DAGGER, EITHER_HAND, 5, "training", "crafted",
                StatBonus.of(AGILITY, 1), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.35));
        weapon("kasakas_venom_fang", UNCOMMON, DAGGER, EITHER_HAND, 7, "venom", "low_rank_dungeon",
                StatBonus.of(STRENGTH, 2), StatBonus.of(AGILITY, 2), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.45));
        weapon("knight_killer", RARE, DAGGER, EITHER_HAND, 8, "knight_killer", "mid_rank_dungeon",
                StatBonus.of(STRENGTH, 4), StatBonus.of(SENSE, 2), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.55));
        weapon("barukas_dagger", RARE, DAGGER, EITHER_HAND, 8, "frost_step", "mid_rank_dungeon",
                StatBonus.of(AGILITY, 5), StatBonus.of(SENSE, 3), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.60));
        weapon("demon_kings_dagger_left", EPIC, DAGGER, EITHER_HAND, 10, "demon_pair", "demon_castle",
                StatBonus.of(STRENGTH, 7), StatBonus.of(AGILITY, 7), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.75));
        weapon("demon_kings_dagger_right", EPIC, DAGGER, EITHER_HAND, 10, "demon_pair", "demon_castle",
                StatBonus.of(STRENGTH, 7), StatBonus.of(AGILITY, 7), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.75));
        weapon("demon_kings_longsword", EPIC, SWORD, EITHER_HAND, 10, "demon_fire", "demon_castle",
                StatBonus.of(STRENGTH, 10), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.85));
        weapon("moonshadow_dagger", EPIC, DAGGER, EITHER_HAND, 10, "moonshadow", "monarch_cache",
                StatBonus.of(AGILITY, 9), StatBonus.of(SENSE, 7), StatBonus.scaling(ATTACK_DAMAGE, 0, 0.85));
        weapon("shadow_reaper_dagger", LEGENDARY, DAGGER, EITHER_HAND, 12, "shadow_reaper", "monarch_cache",
                StatBonus.of(STRENGTH, 12), StatBonus.of(AGILITY, 10), StatBonus.scaling(ATTACK_DAMAGE, 0, 1.0));
        weapon("kamishs_wrath_left", LEGENDARY, DAGGER, EITHER_HAND, 12, "kamish_pair", "monarch_cache",
                StatBonus.of(STRENGTH, 14), StatBonus.of(AGILITY, 12), StatBonus.scaling(ATTACK_DAMAGE, 0, 1.1));
        weapon("kamishs_wrath_right", LEGENDARY, DAGGER, EITHER_HAND, 12, "kamish_pair", "monarch_cache",
                StatBonus.of(STRENGTH, 14), StatBonus.of(AGILITY, 12), StatBonus.scaling(ATTACK_DAMAGE, 0, 1.1));
        weapon("antares_fang", MONARCH, DAGGER, EITHER_HAND, 15, "antares_flame", "admin",
                StatBonus.of(STRENGTH, 20), StatBonus.of(AGILITY, 14), StatBonus.scaling(ATTACK_DAMAGE, 0, 1.25));

        armor("novice_hunter_hood", UNCOMMON, HEAD, "novice_hunter", 5, "crafted",
                StatBonus.of(STAMINA, 1), StatBonus.scaling(EquipmentStat.ARMOR, 0, 0.15));
        armor("novice_hunter_jacket", UNCOMMON, CHEST, "novice_hunter", 5, "crafted",
                StatBonus.of(STAMINA, 2), StatBonus.scaling(EquipmentStat.ARMOR, 0, 0.20));
        armor("novice_hunter_leggings", UNCOMMON, LEGS, "novice_hunter", 5, "crafted",
                StatBonus.of(AGILITY, 1), StatBonus.scaling(EquipmentStat.ARMOR, 0, 0.18));
        armor("novice_hunter_boots", UNCOMMON, FEET, "novice_hunter", 5, "crafted",
                StatBonus.of(AGILITY, 2), StatBonus.scaling(MOVEMENT_SPEED, 0, 0.001));

        armorSet("high_knight", RARE, 8, "mid_rank_dungeon",
                new String[]{"high_knight_helmet", "high_knight_chestplate", "high_knight_leggings", "high_knight_boots"},
                new EquipmentSlotType[]{HEAD, CHEST, LEGS, FEET},
                new StatBonus[]{StatBonus.of(STAMINA, 3), StatBonus.scaling(EquipmentStat.ARMOR, 0, 0.20)});
        armorSet("assassins", RARE, 8, "mid_rank_dungeon",
                new String[]{"assassins_hood", "assassins_jacket", "assassins_trousers", "assassins_shoes"},
                new EquipmentSlotType[]{HEAD, CHEST, LEGS, FEET},
                new StatBonus[]{StatBonus.of(AGILITY, 4), StatBonus.of(SENSE, 2)});
        armor("red_knights_helmet", EPIC, HEAD, "red_knight", 10, "demon_castle",
                StatBonus.of(STAMINA, 5), StatBonus.scaling(ARMOR_TOUGHNESS, 0, 0.10));
        armor("truth_seekers_top", RARE, CHEST, "truth_seeker", 8, "mid_rank_dungeon",
                StatBonus.of(INTELLIGENCE, 4), StatBonus.of(MANA, 40));
        armor("truth_seekers_pants", RARE, LEGS, "truth_seeker", 8, "mid_rank_dungeon",
                StatBonus.of(INTELLIGENCE, 3), StatBonus.of(MANA, 30));
        armor("truth_seekers_shoes", RARE, FEET, "truth_seeker", 8, "mid_rank_dungeon",
                StatBonus.of(AGILITY, 3), StatBonus.of(MANA, 20));
        armor("shadow_monarch_hood", MONARCH, HEAD, "shadow_monarch", 12, "monarch_cache",
                StatBonus.of(INTELLIGENCE, 8), StatBonus.of(SHADOW_CAPACITY, 4));
        armor("shadow_monarch_coat", MONARCH, CHEST, "shadow_monarch", 12, "monarch_cache",
                StatBonus.of(STAMINA, 8), StatBonus.of(SHADOW_CAPACITY, 5));
        armor("shadow_monarch_trousers", MONARCH, LEGS, "shadow_monarch", 12, "monarch_cache",
                StatBonus.of(AGILITY, 7), StatBonus.of(SHADOW_CAPACITY, 3));
        armor("shadow_monarch_boots", MONARCH, FEET, "shadow_monarch", 12, "monarch_cache",
                StatBonus.of(AGILITY, 6), StatBonus.of(SHADOW_CAPACITY, 3));

        accessory("high_knight_gauntlets", RARE, AccessorySlot.HANDS, "high_knight", 6, "mid_rank_dungeon",
                StatBonus.of(STRENGTH, 4), StatBonus.of(KNOCKBACK_RESISTANCE, 0.05));
        accessory("archers_gloves", RARE, AccessorySlot.HANDS, "", 6, "mid_rank_dungeon",
                StatBonus.of(SENSE, 5), StatBonus.of(AGILITY, 2));
        accessory("high_magicians_ring", RARE, AccessorySlot.RING, "", 6, "mid_rank_dungeon",
                StatBonus.of(INTELLIGENCE, 6), StatBonus.of(MANA, 150));
        accessory("gatekeepers_necklace", RARE, AccessorySlot.NECKLACE, "", 6, "mid_rank_dungeon",
                StatBonus.of(AGILITY, 5), StatBonus.of(SENSE, 3));
        accessory("demon_monarch_earring", EPIC, AccessorySlot.EARRING, "demon_monarch", 8, "demon_castle",
                StatBonus.of(STRENGTH, 5), StatBonus.of(INTELLIGENCE, 5));
        accessory("demon_monarch_necklace", EPIC, AccessorySlot.NECKLACE, "demon_monarch", 8, "demon_castle",
                StatBonus.of(AGILITY, 5), StatBonus.of(MANA, 100));
        accessory("demon_monarch_ring", EPIC, AccessorySlot.RING, "demon_monarch", 8, "demon_castle",
                StatBonus.of(STAMINA, 5), StatBonus.of(SHADOW_CAPACITY, 3));
        accessory("truth_seekers_gloves", RARE, AccessorySlot.HANDS, "truth_seeker", 6, "mid_rank_dungeon",
                StatBonus.of(INTELLIGENCE, 4), StatBonus.of(MANA, 50));
        accessory("shadow_monarch_gloves", MONARCH, AccessorySlot.HANDS, "shadow_monarch", 10, "monarch_cache",
                StatBonus.of(STRENGTH, 8), StatBonus.of(SHADOW_CAPACITY, 4));
        accessory("orb_of_avarice", EPIC, AccessorySlot.ORB, "", 8, "demon_castle",
                StatBonus.of(INTELLIGENCE, 8), StatBonus.of(LUCK, 2));
        accessory("hunters_ring", UNCOMMON, AccessorySlot.RING, "novice_hunter", 5, "crafted",
                StatBonus.of(STRENGTH, 2), StatBonus.of(STAMINA, 2));
        accessory("mana_weave_necklace", RARE, AccessorySlot.NECKLACE, "", 6, "crafted",
                StatBonus.of(INTELLIGENCE, 5), StatBonus.of(MANA, 80));
        accessory("shadow_belt", EPIC, AccessorySlot.BELT, "shadow_monarch", 8, "monarch_cache",
                StatBonus.of(STAMINA, 6), StatBonus.of(SHADOW_CAPACITY, 2), StatBonus.of(MAX_HEALTH, 4));
        accessory("monarch_earrings", EPIC, AccessorySlot.EARRING, "shadow_monarch", 8, "monarch_cache",
                StatBonus.of(AGILITY, 6), StatBonus.of(SENSE, 6), StatBonus.of(LUCK, 1));

        set("novice_hunter",
                Set.of("novice_hunter_hood", "novice_hunter_jacket", "novice_hunter_leggings", "novice_hunter_boots"),
                List.of(StatBonus.of(MAX_HEALTH, 4), StatBonus.of(EquipmentStat.ARMOR, 2)), "novice_resolve");
        set("high_knight",
                Set.of("high_knight_helmet", "high_knight_chestplate", "high_knight_leggings", "high_knight_boots", "high_knight_gauntlets"),
                List.of(StatBonus.of(STAMINA, 8)), "fortress");
        set("assassins",
                Set.of("assassins_hood", "assassins_jacket", "assassins_trousers", "assassins_shoes"),
                List.of(StatBonus.of(AGILITY, 8), StatBonus.of(SENSE, 5)), "silent_step");
        set("truth_seeker",
                Set.of("truth_seekers_top", "truth_seekers_pants", "truth_seekers_shoes", "truth_seekers_gloves"),
                List.of(StatBonus.of(INTELLIGENCE, 10), StatBonus.of(MANA, 200)), "mana_weave");
        set("demon_monarch",
                Set.of("demon_monarch_earring", "demon_monarch_necklace", "demon_monarch_ring"),
                List.of(StatBonus.of(STRENGTH, 6), StatBonus.of(INTELLIGENCE, 6)), "demon_authority");
        set("shadow_monarch",
                Set.of("shadow_monarch_hood", "shadow_monarch_coat", "shadow_monarch_trousers", "shadow_monarch_boots", "shadow_monarch_gloves", "shadow_belt", "monarch_earrings"),
                List.of(StatBonus.of(MAX_HEALTH, 10), StatBonus.of(MOVEMENT_SPEED, 0.03), StatBonus.of(SHADOW_CAPACITY, 15)), "monarch_presence");
    }

    private static void weapon(String id, EquipmentRarity rarity, EquipmentCategory category, EquipmentSlotType slot,
                               int maxUpgrade, String hook, String acquisition, StatBonus... bonuses) {
        define(new EquipmentDefinition(id, rarity, category, slot, AccessorySlot.NONE, List.of(bonuses), "", maxUpgrade, hook, acquisition));
    }

    private static void armor(String id, EquipmentRarity rarity, EquipmentSlotType slot, String setId,
                              int maxUpgrade, String acquisition, StatBonus... bonuses) {
        define(new EquipmentDefinition(id, rarity, EquipmentCategory.ARMOR, slot, AccessorySlot.NONE, List.of(bonuses), setId, maxUpgrade, "", acquisition));
    }

    private static void armorSet(String setId, EquipmentRarity rarity, int maxUpgrade, String acquisition,
                                 String[] ids, EquipmentSlotType[] slots, StatBonus[] sharedBonuses) {
        for (int i = 0; i < ids.length; i++) {
            armor(ids[i], rarity, slots[i], setId, maxUpgrade, acquisition, sharedBonuses);
        }
    }

    private static void accessory(String id, EquipmentRarity rarity, AccessorySlot slot, String setId,
                                  int maxUpgrade, String acquisition, StatBonus... bonuses) {
        define(new EquipmentDefinition(id, rarity, EquipmentCategory.ACCESSORY, EquipmentSlotType.ACCESSORY, slot, List.of(bonuses), setId, maxUpgrade, "", acquisition));
    }

    private static void set(String id, Set<String> required, List<StatBonus> bonuses, String hook) {
        SETS.put(id, new EquipmentSetDefinition(id, required, bonuses, hook));
    }

    private static void define(EquipmentDefinition definition) {
        EquipmentDefinition previous = DEFINITIONS.put(definition.id(), definition);
        if (previous != null) throw new IllegalStateException("Duplicate equipment definition: " + definition.id());
    }

    public static EquipmentDefinition require(String id) {
        EquipmentDefinition definition = DEFINITIONS.get(stripNamespace(id));
        if (definition == null) throw new IllegalArgumentException("Missing equipment definition: " + id);
        return definition;
    }

    public static Optional<EquipmentDefinition> find(String id) {
        return Optional.ofNullable(DEFINITIONS.get(stripNamespace(id)));
    }

    public static Optional<EquipmentDefinition> find(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? Optional.empty() : find(id.getPath());
    }

    public static Optional<EquipmentSetDefinition> findSet(String id) {
        return Optional.ofNullable(SETS.get(id));
    }

    public static Collection<EquipmentDefinition> definitions() { return List.copyOf(DEFINITIONS.values()); }
    public static Collection<EquipmentSetDefinition> sets() { return List.copyOf(SETS.values()); }

    private static String stripNamespace(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private EquipmentCatalog() {}
}
