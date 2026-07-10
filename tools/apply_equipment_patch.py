from pathlib import Path
import json
import re
import sys
import textwrap

ROOT = Path(__file__).resolve().parents[1]

def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    normalized = textwrap.dedent(content).lstrip("\n").rstrip() + "\n"
    if not target.exists() or target.read_text(encoding="utf-8") != normalized:
        target.write_text(normalized, encoding="utf-8")

def framework() -> None:
    files = {}
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentRarity.java"] = r"""
        package com.tre.sololeveling.equipment;

        import net.minecraft.ChatFormatting;
        import net.minecraft.world.item.Rarity;

        public enum EquipmentRarity {
            COMMON(ChatFormatting.WHITE, Rarity.COMMON),
            UNCOMMON(ChatFormatting.GREEN, Rarity.UNCOMMON),
            RARE(ChatFormatting.AQUA, Rarity.RARE),
            EPIC(ChatFormatting.LIGHT_PURPLE, Rarity.EPIC),
            LEGENDARY(ChatFormatting.GOLD, Rarity.EPIC),
            MONARCH(ChatFormatting.DARK_PURPLE, Rarity.EPIC);

            private final ChatFormatting color;
            private final Rarity vanilla;

            EquipmentRarity(ChatFormatting color, Rarity vanilla) {
                this.color = color;
                this.vanilla = vanilla;
            }

            public ChatFormatting color() { return color; }
            public Rarity vanilla() { return vanilla; }
            public String translationKey() { return "rarity.sololeveling." + name().toLowerCase(); }
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentCategory.java"] = r"""
        package com.tre.sololeveling.equipment;

        public enum EquipmentCategory {
            DAGGER, SWORD, ARMOR, ACCESSORY;

            public String translationKey() {
                return "category.sololeveling." + name().toLowerCase();
            }
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentSlotType.java"] = r"""
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
    """
    files["src/main/java/com/tre/sololeveling/equipment/AccessorySlot.java"] = r"""
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
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentStat.java"] = r"""
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
    """
    files["src/main/java/com/tre/sololeveling/equipment/StatBonus.java"] = r"""
        package com.tre.sololeveling.equipment;

        public record StatBonus(EquipmentStat stat, double baseValue, double perUpgrade) {
            public double valueAt(int upgradeLevel, double upgradeMultiplier) {
                return baseValue + Math.max(0, upgradeLevel) * perUpgrade * Math.max(0.0D, upgradeMultiplier);
            }

            public static StatBonus of(EquipmentStat stat, double value) {
                return new StatBonus(stat, value, 0.0D);
            }

            public static StatBonus scaling(EquipmentStat stat, double baseValue, double perUpgrade) {
                return new StatBonus(stat, baseValue, perUpgrade);
            }
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentDefinition.java"] = r"""
        package com.tre.sololeveling.equipment;

        import java.util.List;
        import java.util.Objects;

        public record EquipmentDefinition(
                String id,
                EquipmentRarity rarity,
                EquipmentCategory category,
                EquipmentSlotType slot,
                AccessorySlot accessorySlot,
                List<StatBonus> bonuses,
                String setId,
                int maxUpgrade,
                String abilityHook,
                String acquisitionKey
        ) {
            public EquipmentDefinition {
                id = Objects.requireNonNull(id, "id");
                rarity = Objects.requireNonNull(rarity, "rarity");
                category = Objects.requireNonNull(category, "category");
                slot = Objects.requireNonNull(slot, "slot");
                accessorySlot = Objects.requireNonNull(accessorySlot, "accessorySlot");
                bonuses = List.copyOf(bonuses);
                setId = setId == null ? "" : setId;
                maxUpgrade = Math.max(0, maxUpgrade);
                abilityHook = abilityHook == null ? "" : abilityHook;
                acquisitionKey = acquisitionKey == null ? "unknown" : acquisitionKey;
            }

            public String itemTranslationKey() { return "item.sololeveling." + id; }
            public String abilityTranslationKey() { return "ability.sololeveling." + abilityHook; }
            public String acquisitionTranslationKey() { return "acquisition.sololeveling." + acquisitionKey; }
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentSetDefinition.java"] = r"""
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
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentCatalog.java"] = r"""
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
                        StatBonus.of(STAMINA, 1), StatBonus.scaling(ARMOR, 0, 0.15));
                armor("novice_hunter_jacket", UNCOMMON, CHEST, "novice_hunter", 5, "crafted",
                        StatBonus.of(STAMINA, 2), StatBonus.scaling(ARMOR, 0, 0.20));
                armor("novice_hunter_leggings", UNCOMMON, LEGS, "novice_hunter", 5, "crafted",
                        StatBonus.of(AGILITY, 1), StatBonus.scaling(ARMOR, 0, 0.18));
                armor("novice_hunter_boots", UNCOMMON, FEET, "novice_hunter", 5, "crafted",
                        StatBonus.of(AGILITY, 2), StatBonus.scaling(MOVEMENT_SPEED, 0, 0.001));

                armorSet("high_knight", RARE, 8, "mid_rank_dungeon",
                        new String[]{"high_knight_helmet", "high_knight_chestplate", "high_knight_leggings", "high_knight_boots"},
                        new EquipmentSlotType[]{HEAD, CHEST, LEGS, FEET},
                        new StatBonus[]{StatBonus.of(STAMINA, 3), StatBonus.scaling(ARMOR, 0, 0.20)});
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
                        List.of(StatBonus.of(MAX_HEALTH, 4), StatBonus.of(ARMOR, 2)), "novice_resolve");
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
                define(new EquipmentDefinition(id, rarity, ARMOR, slot, AccessorySlot.NONE, List.of(bonuses), setId, maxUpgrade, "", acquisition));
            }

            private static void armorSet(String setId, EquipmentRarity rarity, int maxUpgrade, String acquisition,
                                         String[] ids, EquipmentSlotType[] slots, StatBonus[] sharedBonuses) {
                for (int i = 0; i < ids.length; i++) {
                    armor(ids[i], rarity, slots[i], setId, maxUpgrade, acquisition, sharedBonuses);
                }
            }

            private static void accessory(String id, EquipmentRarity rarity, AccessorySlot slot, String setId,
                                          int maxUpgrade, String acquisition, StatBonus... bonuses) {
                define(new EquipmentDefinition(id, rarity, ACCESSORY, EquipmentSlotType.ACCESSORY, slot, List.of(bonuses), setId, maxUpgrade, "", acquisition));
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
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentData.java"] = r"""
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
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentItem.java"] = r"""
        package com.tre.sololeveling.equipment;

        import net.minecraft.network.chat.Component;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.TooltipFlag;

        import java.util.List;

        public interface EquipmentItem {
            EquipmentDefinition equipmentDefinition();

            default void initializeEquipment(ItemStack stack) {
                EquipmentData.initialize(stack, equipmentDefinition());
            }

            default void appendEquipmentTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
                EquipmentTooltips.append(stack, equipmentDefinition(), tooltip, flag);
            }
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentTooltips.java"] = r"""
        package com.tre.sololeveling.equipment;

        import net.minecraft.ChatFormatting;
        import net.minecraft.network.chat.Component;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.TooltipFlag;

        import java.util.List;
        import java.util.Locale;

        public final class EquipmentTooltips {
            public static void append(ItemStack stack, EquipmentDefinition definition, List<Component> tooltip, TooltipFlag flag) {
                int upgrade = EquipmentData.upgradeLevel(stack, definition);
                tooltip.add(Component.translatable("tooltip.sololeveling.rarity",
                        Component.translatable(definition.rarity().translationKey()).withStyle(definition.rarity().color()))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.sololeveling.category_slot",
                        Component.translatable(definition.category().translationKey()),
                        Component.translatable(definition.slot().translationKey())).withStyle(ChatFormatting.DARK_AQUA));
                if (definition.accessorySlot() != AccessorySlot.NONE) {
                    tooltip.add(Component.translatable("tooltip.sololeveling.accessory_slot",
                            Component.translatable(definition.accessorySlot().translationKey())).withStyle(ChatFormatting.GRAY));
                }
                tooltip.add(Component.translatable("tooltip.sololeveling.upgrade", upgrade, definition.maxUpgrade())
                        .withStyle(upgrade >= definition.maxUpgrade() ? ChatFormatting.GOLD : ChatFormatting.AQUA));

                double multiplier = safeUpgradeMultiplier();
                for (StatBonus bonus : definition.bonuses()) {
                    tooltip.add(statLine(bonus.stat(), bonus.valueAt(upgrade, multiplier), ChatFormatting.GREEN));
                }

                if (!definition.setId().isBlank()) {
                    tooltip.add(Component.translatable("tooltip.sololeveling.set_piece",
                            Component.translatable("set.sololeveling." + definition.setId())).withStyle(ChatFormatting.LIGHT_PURPLE));
                    EquipmentCatalog.findSet(definition.setId()).ifPresent(set -> {
                        for (StatBonus bonus : set.bonuses()) {
                            tooltip.add(Component.literal("  ").append(statLine(bonus.stat(), bonus.baseValue(), ChatFormatting.DARK_PURPLE)));
                        }
                    });
                }
                if (!definition.abilityHook().isBlank() && !definition.abilityHook().equals("none")) {
                    tooltip.add(Component.translatable("tooltip.sololeveling.ability",
                            Component.translatable(definition.abilityTranslationKey())).withStyle(ChatFormatting.DARK_PURPLE));
                }
                tooltip.add(Component.translatable("tooltip.sololeveling.acquisition",
                        Component.translatable(definition.acquisitionTranslationKey())).withStyle(ChatFormatting.DARK_GRAY));
                if (flag.isAdvanced()) {
                    tooltip.add(Component.translatable("tooltip.sololeveling.equipment_id", definition.id())
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }

            public static Component statLine(EquipmentStat stat, double value, ChatFormatting color) {
                String formatted = format(stat, value);
                return Component.translatable("tooltip.sololeveling.stat",
                        formatted, Component.translatable(stat.translationKey())).withStyle(color);
            }

            private static String format(EquipmentStat stat, double value) {
                double displayed = stat.isPercentage() ? value * 100.0D : value;
                if (Math.abs(displayed - Math.rint(displayed)) < 0.0001D) {
                    return String.format(Locale.ROOT, "%+.0f%s", displayed, stat.isPercentage() ? "%" : "");
                }
                return String.format(Locale.ROOT, "%+.1f%s", displayed, stat.isPercentage() ? "%" : "");
            }

            private static double safeUpgradeMultiplier() {
                try { return EquipmentConfig.UPGRADE_BONUS_MULTIPLIER.get(); }
                catch (IllegalStateException ignored) { return 1.0D; }
            }

            private EquipmentTooltips() {}
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentConfig.java"] = r"""
        package com.tre.sololeveling.equipment;

        import net.minecraftforge.common.ForgeConfigSpec;

        public final class EquipmentConfig {
            public static final ForgeConfigSpec SPEC;
            public static final ForgeConfigSpec.BooleanValue ENABLE_UPGRADES;
            public static final ForgeConfigSpec.BooleanValue ENABLE_SET_BONUSES;
            public static final ForgeConfigSpec.DoubleValue UPGRADE_BONUS_MULTIPLIER;
            public static final ForgeConfigSpec.IntValue EFFECT_REFRESH_TICKS;

            static {
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
                builder.push("equipment");
                ENABLE_UPGRADES = builder.comment("Allow reinforcement materials to upgrade equipment.")
                        .define("enableUpgrades", true);
                ENABLE_SET_BONUSES = builder.comment("Apply equipment set bonuses.")
                        .define("enableSetBonuses", true);
                UPGRADE_BONUS_MULTIPLIER = builder.comment("Multiplier for per-upgrade stat growth.")
                        .defineInRange("upgradeBonusMultiplier", 1.0D, 0.0D, 10.0D);
                EFFECT_REFRESH_TICKS = builder.comment("How often equipped transient modifiers are reconciled.")
                        .defineInRange("effectRefreshTicks", 20, 1, 200);
                builder.pop();
                SPEC = builder.build();
            }

            private EquipmentConfig() {}
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentEffects.java"] = r"""
        package com.tre.sololeveling.equipment;

        import com.tre.sololeveling.SoloLevelingMod;
        import com.tre.sololeveling.data.HunterData;
        import net.minecraft.resources.ResourceLocation;
        import net.minecraft.server.level.ServerPlayer;
        import net.minecraft.world.entity.ai.attributes.Attribute;
        import net.minecraft.world.entity.ai.attributes.AttributeInstance;
        import net.minecraft.world.entity.ai.attributes.AttributeModifier;
        import net.minecraft.world.entity.player.Player;
        import net.minecraft.world.item.ItemStack;
        import net.minecraftforge.event.TickEvent;
        import net.minecraftforge.eventbus.api.SubscribeEvent;
        import net.minecraftforge.fml.common.Mod;
        import net.minecraftforge.registries.ForgeRegistries;

        import java.nio.charset.StandardCharsets;
        import java.util.EnumMap;
        import java.util.HashSet;
        import java.util.Map;
        import java.util.Set;
        import java.util.UUID;

        @Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
        public final class EquipmentEffects {
            private static final Map<EquipmentStat, UUID> MODIFIER_IDS = new EnumMap<>(EquipmentStat.class);

            static {
                for (EquipmentStat stat : EquipmentStat.values()) {
                    MODIFIER_IDS.put(stat, UUID.nameUUIDFromBytes(("sololeveling:equipment:" + stat.name()).getBytes(StandardCharsets.UTF_8)));
                }
            }

            @SubscribeEvent
            public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
                if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) return;
                int interval;
                try { interval = EquipmentConfig.EFFECT_REFRESH_TICKS.get(); }
                catch (IllegalStateException ignored) { interval = 20; }
                if (player.tickCount % Math.max(1, interval) != 0) return;
                reconcile(player);
            }

            public static void reconcile(ServerPlayer player) {
                Map<EquipmentStat, Double> totals = totals(player);
                for (EquipmentStat stat : EquipmentStat.values()) {
                    Attribute attribute = stat.vanillaAttribute();
                    if (attribute == null) continue;
                    AttributeInstance instance = player.getAttribute(attribute);
                    if (instance == null) continue;
                    UUID id = MODIFIER_IDS.get(stat);
                    AttributeModifier old = instance.getModifier(id);
                    if (old != null) instance.removeModifier(old);
                    double amount = totals.getOrDefault(stat, 0.0D);
                    if (Math.abs(amount) > 0.0000001D) {
                        instance.addTransientModifier(new AttributeModifier(id, "Solo Leveling Equipment " + stat.name(),
                                amount, AttributeModifier.Operation.ADDITION));
                    }
                }
                if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            }

            public static Map<EquipmentStat, Double> totals(Player player) {
                EnumMap<EquipmentStat, Double> totals = new EnumMap<>(EquipmentStat.class);
                Set<String> equippedIds = new HashSet<>();
                Set<UUID> seenInstances = new HashSet<>();

                addStack(player.getMainHandItem(), totals, equippedIds, seenInstances);
                addStack(player.getOffhandItem(), totals, equippedIds, seenInstances);
                for (ItemStack stack : player.getInventory().armor) addStack(stack, totals, equippedIds, seenInstances);

                for (AccessorySlot slot : AccessorySlot.values()) {
                    if (slot == AccessorySlot.NONE) continue;
                    String id = HunterData.equipped(player, slot.storageKey());
                    if (id == null || id.isBlank()) continue;
                    EquipmentCatalog.find(id).ifPresent(definition -> {
                        equippedIds.add(definition.id());
                        addBonuses(totals, definition.bonuses(), 0);
                    });
                }

                boolean setBonuses;
                try { setBonuses = EquipmentConfig.ENABLE_SET_BONUSES.get(); }
                catch (IllegalStateException ignored) { setBonuses = true; }
                if (setBonuses) {
                    for (EquipmentSetDefinition set : EquipmentCatalog.sets()) {
                        if (equippedIds.containsAll(set.requiredItems())) addBonuses(totals, set.bonuses(), 0);
                    }
                }
                return Map.copyOf(totals);
            }

            private static void addStack(ItemStack stack, EnumMap<EquipmentStat, Double> totals,
                                         Set<String> equippedIds, Set<UUID> seenInstances) {
                if (stack.isEmpty()) return;
                EquipmentCatalog.find(stack).ifPresent(definition -> {
                    UUID instance = EquipmentData.instanceId(stack, definition);
                    if (!seenInstances.add(instance)) return;
                    equippedIds.add(definition.id());
                    addBonuses(totals, definition.bonuses(), EquipmentData.upgradeLevel(stack, definition));
                });
            }

            private static void addBonuses(EnumMap<EquipmentStat, Double> totals, Iterable<StatBonus> bonuses, int upgradeLevel) {
                double multiplier;
                try { multiplier = EquipmentConfig.UPGRADE_BONUS_MULTIPLIER.get(); }
                catch (IllegalStateException ignored) { multiplier = 1.0D; }
                for (StatBonus bonus : bonuses) {
                    totals.merge(bonus.stat(), bonus.valueAt(upgradeLevel, multiplier), Double::sum);
                }
            }

            public static String registryId(ItemStack stack) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                return id == null ? "" : id.toString();
            }

            private EquipmentEffects() {}
        }
    """
    files["src/main/java/com/tre/sololeveling/equipment/EquipmentAbilityHooks.java"] = r"""
        package com.tre.sololeveling.equipment;

        import com.tre.sololeveling.data.HunterData;
        import net.minecraft.core.particles.ParticleTypes;
        import net.minecraft.server.level.ServerLevel;
        import net.minecraft.server.level.ServerPlayer;
        import net.minecraft.world.effect.MobEffectInstance;
        import net.minecraft.world.effect.MobEffects;
        import net.minecraft.world.entity.LivingEntity;
        import net.minecraft.world.item.ItemStack;

        import java.util.Map;
        import java.util.concurrent.ConcurrentHashMap;

        public final class EquipmentAbilityHooks {
            @FunctionalInterface
            public interface HitHook {
                void apply(ItemStack stack, LivingEntity target, LivingEntity attacker);
            }

            private static final Map<String, HitHook> HIT_HOOKS = new ConcurrentHashMap<>();

            static {
                registerHit("venom", (stack, target, attacker) -> {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                });
                registerHit("knight_killer", (stack, target, attacker) -> {
                    if (target.getArmorValue() > 0) bonusDamage(attacker, target, Math.min(5.0F, 1.0F + target.getArmorValue() * 0.12F));
                });
                registerHit("demon_fire", (stack, target, attacker) -> {
                    target.setSecondsOnFire(5);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    particles(target, ParticleTypes.SOUL_FIRE_FLAME, 20);
                });
                registerHit("moonshadow", (stack, target, attacker) -> {
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    bonusDamage(attacker, target, 4.0F + EquipmentData.upgradeLevel(stack, EquipmentCatalog.require("moonshadow_dagger")) * 0.5F);
                    particles(target, ParticleTypes.SCULK_SOUL, 18);
                });
                registerHit("shadow_reaper", (stack, target, attacker) -> {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 0));
                    bonusDamage(attacker, target, Math.min(10.0F, 2.0F + target.getMaxHealth() * 0.06F));
                    particles(target, ParticleTypes.REVERSE_PORTAL, 24);
                });
                registerHit("demon_pair", (stack, target, attacker) -> {
                    if (attacker instanceof ServerPlayer player && paired(player, "demon_kings_dagger_left", "demon_kings_dagger_right")) {
                        bonusDamage(attacker, target, 6.0F);
                    }
                });
                registerHit("kamish_pair", (stack, target, attacker) -> {
                    if (attacker instanceof ServerPlayer player) {
                        bonusDamage(attacker, target, Math.min(12.0F, HunterData.getStat(player, "strength") * 0.10F));
                        if (paired(player, "kamishs_wrath_left", "kamishs_wrath_right")) bonusDamage(attacker, target, 8.0F);
                    }
                });
                registerHit("antares_flame", (stack, target, attacker) -> {
                    target.setSecondsOnFire(8);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                    bonusDamage(attacker, target, 12.0F);
                });
            }

            public static void registerHit(String id, HitHook hook) {
                if (id == null || id.isBlank() || hook == null) throw new IllegalArgumentException("Invalid equipment hit hook");
                HIT_HOOKS.put(id, hook);
            }

            public static void fireHit(String id, ItemStack stack, LivingEntity target, LivingEntity attacker) {
                HitHook hook = HIT_HOOKS.get(id);
                if (hook != null) hook.apply(stack, target, attacker);
            }

            private static boolean paired(ServerPlayer player, String left, String right) {
                String main = EquipmentEffects.registryId(player.getMainHandItem());
                String off = EquipmentEffects.registryId(player.getOffhandItem());
                return (main.endsWith(left) && off.endsWith(right)) || (main.endsWith(right) && off.endsWith(left));
            }

            private static void bonusDamage(LivingEntity attacker, LivingEntity target, float amount) {
                if (amount <= 0.0F) return;
                attacker.getPersistentData().putBoolean("sl_weapon_bonus", true);
                try { target.hurt(attacker.damageSources().mobAttack(attacker), amount); }
                finally { attacker.getPersistentData().remove("sl_weapon_bonus"); }
            }

            private static void particles(LivingEntity target, net.minecraft.core.particles.ParticleOptions particle, int count) {
                if (target.level() instanceof ServerLevel server) {
                    server.sendParticles(particle, target.getX(), target.getY() + 1.0D, target.getZ(), count, 0.4D, 0.6D, 0.4D, 0.03D);
                }
            }

            private EquipmentAbilityHooks() {}
        }
    """
    files["src/main/java/com/tre/sololeveling/item/HunterAccessoryItem.java"] = r"""
        package com.tre.sololeveling.item;

        import com.tre.sololeveling.equipment.EquipmentDefinition;
        import com.tre.sololeveling.equipment.EquipmentItem;
        import net.minecraft.network.chat.Component;
        import net.minecraft.world.InteractionHand;
        import net.minecraft.world.InteractionResultHolder;
        import net.minecraft.world.entity.Entity;
        import net.minecraft.world.entity.player.Player;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.TooltipFlag;
        import net.minecraft.world.level.Level;

        import javax.annotation.Nullable;
        import java.util.List;

        public final class HunterAccessoryItem extends FunctionalItem implements EquipmentItem {
            private final EquipmentDefinition definition;

            public HunterAccessoryItem(Properties properties, EquipmentDefinition definition) {
                super(properties, Kind.ACCESSORY, "", definition.accessorySlot().storageKey());
                this.definition = definition;
            }

            @Override
            public EquipmentDefinition equipmentDefinition() { return definition; }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                initializeEquipment(player.getItemInHand(hand));
                return super.use(level, player, hand);
            }

            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                initializeEquipment(stack);
                super.inventoryTick(stack, level, entity, slot, selected);
            }

            @Override
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                appendEquipmentTooltip(stack, tooltip, flag);
                tooltip.add(Component.translatable("tooltip.sololeveling.accessory_use"));
            }
        }
    """
    files["src/main/java/com/tre/sololeveling/item/EquipmentUpgradeMaterialItem.java"] = r"""
        package com.tre.sololeveling.item;

        import com.tre.sololeveling.equipment.EquipmentConfig;
        import com.tre.sololeveling.equipment.EquipmentData;
        import com.tre.sololeveling.equipment.EquipmentDefinition;
        import com.tre.sololeveling.equipment.EquipmentItem;
        import net.minecraft.ChatFormatting;
        import net.minecraft.network.chat.Component;
        import net.minecraft.server.level.ServerPlayer;
        import net.minecraft.sounds.SoundEvents;
        import net.minecraft.sounds.SoundSource;
        import net.minecraft.world.InteractionHand;
        import net.minecraft.world.InteractionResultHolder;
        import net.minecraft.world.entity.player.Player;
        import net.minecraft.world.item.Item;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.TooltipFlag;
        import net.minecraft.world.level.Level;

        import javax.annotation.Nullable;
        import java.util.List;

        public final class EquipmentUpgradeMaterialItem extends Item {
            private final int upgradeAmount;
            private final String acquisitionKey;

            public EquipmentUpgradeMaterialItem(Properties properties, int upgradeAmount, String acquisitionKey) {
                super(properties);
                this.upgradeAmount = Math.max(1, upgradeAmount);
                this.acquisitionKey = acquisitionKey;
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack catalyst = player.getItemInHand(hand);
                ItemStack target = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
                if (!(target.getItem() instanceof EquipmentItem equipmentItem)) {
                    if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.sololeveling.upgrade_no_target").withStyle(ChatFormatting.RED));
                    return InteractionResultHolder.fail(catalyst);
                }
                boolean enabled;
                try { enabled = EquipmentConfig.ENABLE_UPGRADES.get(); }
                catch (IllegalStateException ignored) { enabled = true; }
                if (!enabled) {
                    if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.sololeveling.upgrades_disabled").withStyle(ChatFormatting.RED));
                    return InteractionResultHolder.fail(catalyst);
                }
                EquipmentDefinition definition = equipmentItem.equipmentDefinition();
                int before = EquipmentData.upgradeLevel(target, definition);
                if (before >= definition.maxUpgrade()) {
                    if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.sololeveling.upgrade_max").withStyle(ChatFormatting.GOLD));
                    return InteractionResultHolder.fail(catalyst);
                }
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                    EquipmentData.upgrade(target, definition, upgradeAmount);
                    int after = EquipmentData.upgradeLevel(target, definition);
                    if (!serverPlayer.getAbilities().instabuild) catalyst.shrink(1);
                    level.playSound(null, serverPlayer.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.15F);
                    serverPlayer.sendSystemMessage(Component.translatable("message.sololeveling.upgrade_success",
                            target.getHoverName(), after, definition.maxUpgrade()).withStyle(ChatFormatting.AQUA));
                }
                return InteractionResultHolder.sidedSuccess(catalyst, level.isClientSide);
            }

            @Override
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                tooltip.add(Component.translatable("tooltip.sololeveling.upgrade_material", upgradeAmount).withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.sololeveling.upgrade_instructions").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.sololeveling.acquisition",
                        Component.translatable("acquisition.sololeveling." + acquisitionKey)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    """
    files["src/main/java/com/tre/sololeveling/item/ItemAcquisitionCatalog.java"] = r"""
        package com.tre.sololeveling.item;

        import net.minecraft.ChatFormatting;
        import net.minecraft.network.chat.Component;
        import net.minecraft.resources.ResourceLocation;
        import net.minecraft.world.item.ItemStack;
        import net.minecraftforge.registries.ForgeRegistries;

        public final class ItemAcquisitionCatalog {
            public static Component line(ItemStack stack) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                String path = id == null ? "" : id.getPath();
                return Component.translatable("tooltip.sololeveling.acquisition",
                        Component.translatable("acquisition.sololeveling." + key(path))).withStyle(ChatFormatting.DARK_GRAY);
            }

            public static String key(String id) {
                if (id.contains("potion")) return "crafted";
                if (id.contains("dungeon_key")) return "quest_reward";
                if (id.contains("castle") && id.contains("key")) return "demon_castle";
                if (id.contains("rune")) return "rune_drop";
                if (id.contains("black_heart")) return "main_quest";
                if (id.contains("random_box")) return "system_reward";
                if (id.contains("holy_water")) return "demon_castle";
                if (id.contains("teleportation")) return "system_reward";
                if (id.contains("mana_crystal") || id.contains("essence") || id.contains("venom")
                        || id.contains("fang") || id.contains("fragment") || id.contains("steel")
                        || id.contains("silk") || id.contains("relic")) return "dungeon_loot";
                return "quest_reward";
            }

            private ItemAcquisitionCatalog() {}
        }
    """
    for path, content in files.items():
        write(path, content)

def items() -> None:
    write("src/main/java/com/tre/sololeveling/item/HunterWeaponItem.java", r"""
        package com.tre.sololeveling.item;

        import com.tre.sololeveling.data.HunterData;
        import com.tre.sololeveling.equipment.EquipmentAbilityHooks;
        import com.tre.sololeveling.equipment.EquipmentDefinition;
        import com.tre.sololeveling.equipment.EquipmentItem;
        import net.minecraft.network.chat.Component;
        import net.minecraft.server.level.ServerPlayer;
        import net.minecraft.world.effect.MobEffectInstance;
        import net.minecraft.world.effect.MobEffects;
        import net.minecraft.world.entity.Entity;
        import net.minecraft.world.entity.LivingEntity;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.SwordItem;
        import net.minecraft.world.item.Tier;
        import net.minecraft.world.item.TooltipFlag;
        import net.minecraft.world.level.Level;

        import javax.annotation.Nullable;
        import java.util.List;

        public class HunterWeaponItem extends SwordItem implements EquipmentItem {
            private final EquipmentDefinition definition;

            public HunterWeaponItem(Tier tier, int damage, float speed, Properties properties, EquipmentDefinition definition) {
                super(tier, damage, speed, properties);
                this.definition = definition;
            }

            @Override
            public EquipmentDefinition equipmentDefinition() { return definition; }

            @Override
            public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                initializeEquipment(stack);
                if (!attacker.level().isClientSide) {
                    EquipmentAbilityHooks.fireHit(definition.abilityHook(), stack, target, attacker);
                    if (attacker instanceof ServerPlayer player) {
                        HunterData.progressDaggerDamage(player, Math.max(1, (int)(4 + target.getArmorValue() * 0.25F)));
                    }
                }
                return super.hurtEnemy(stack, target, attacker);
            }

            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                initializeEquipment(stack);
                super.inventoryTick(stack, level, entity, slot, selected);
                if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
                boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
                if (!held) return;
                if (definition.abilityHook().equals("frost_step")) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false, true));
                } else if (definition.abilityHook().equals("moonshadow")) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, true));
                } else if (definition.abilityHook().equals("shadow_reaper")) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
                }
            }

            @Override
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                appendEquipmentTooltip(stack, tooltip, flag);
            }
        }
    """)
    write("src/main/java/com/tre/sololeveling/item/HunterArmorItem.java", r"""
        package com.tre.sololeveling.item;

        import com.tre.sololeveling.equipment.EquipmentDefinition;
        import com.tre.sololeveling.equipment.EquipmentItem;
        import net.minecraft.network.chat.Component;
        import net.minecraft.world.entity.Entity;
        import net.minecraft.world.item.ArmorItem;
        import net.minecraft.world.item.ArmorMaterial;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.TooltipFlag;
        import net.minecraft.world.level.Level;

        import javax.annotation.Nullable;
        import java.util.List;

        public final class HunterArmorItem extends ArmorItem implements EquipmentItem {
            private final EquipmentDefinition definition;

            public HunterArmorItem(ArmorMaterial material, Type type, Properties properties, EquipmentDefinition definition) {
                super(material, type, properties);
                this.definition = definition;
            }

            @Override
            public EquipmentDefinition equipmentDefinition() { return definition; }

            @Override
            public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
                initializeEquipment(stack);
                super.inventoryTick(stack, level, entity, slot, selected);
            }

            @Override
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                appendEquipmentTooltip(stack, tooltip, flag);
            }
        }
    """)
    write("src/main/java/com/tre/sololeveling/item/FunctionalItem.java", r"""
        package com.tre.sololeveling.item;

        import com.tre.sololeveling.data.HunterData;
        import com.tre.sololeveling.registry.ModItems;
        import com.tre.sololeveling.registry.ModSounds;
        import net.minecraft.ChatFormatting;
        import net.minecraft.core.BlockPos;
        import net.minecraft.network.chat.Component;
        import net.minecraft.server.level.ServerPlayer;
        import net.minecraft.sounds.SoundSource;
        import net.minecraft.world.InteractionHand;
        import net.minecraft.world.InteractionResultHolder;
        import net.minecraft.world.effect.MobEffectInstance;
        import net.minecraft.world.effect.MobEffects;
        import net.minecraft.world.item.Item;
        import net.minecraft.world.item.ItemStack;
        import net.minecraft.world.item.TooltipFlag;
        import net.minecraft.world.level.Level;

        import javax.annotation.Nullable;
        import java.util.List;

        public class FunctionalItem extends Item {
            public enum Kind { STORY, HEALING, MANA, GREATER_HEALING, GREATER_MANA, RUNE, ACCESSORY, BLACK_HEART, HOLY_WATER, RANDOM_BOX, TELEPORT }
            private final Kind kind;
            private final String detail;
            private final String slotOrSkill;

            public FunctionalItem(Properties properties, Kind kind, String detail) {
                this(properties, kind, detail, "");
            }

            public FunctionalItem(Properties properties, Kind kind, String detail, String slotOrSkill) {
                super(properties);
                this.kind = kind;
                this.detail = detail;
                this.slotOrSkill = slotOrSkill;
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }
                boolean consumed = false;
                switch (kind) {
                    case HEALING -> { serverPlayer.heal(8.0F); consumed = true; }
                    case GREATER_HEALING -> { serverPlayer.heal(serverPlayer.getMaxHealth()); consumed = true; }
                    case MANA -> { HunterData.addMana(serverPlayer, 60); consumed = true; }
                    case GREATER_MANA -> { HunterData.addMana(serverPlayer, HunterData.getMaxMana(serverPlayer)); consumed = true; }
                    case HOLY_WATER -> {
                        serverPlayer.heal(serverPlayer.getMaxHealth());
                        serverPlayer.removeEffect(MobEffects.POISON);
                        serverPlayer.removeEffect(MobEffects.WITHER);
                        serverPlayer.removeEffect(MobEffects.WEAKNESS);
                        serverPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        consumed = true;
                    }
                    case RUNE -> {
                        if (HunterData.unlockSkill(serverPlayer, slotOrSkill)) {
                            serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] Skill unlocked: " + slotOrSkill).withStyle(ChatFormatting.AQUA));
                        } else {
                            HunterData.addGold(serverPlayer, 100);
                            HunterData.addXp(serverPlayer, 50);
                            serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] Duplicate rune converted into 100 gold and 50 XP.").withStyle(ChatFormatting.GOLD));
                        }
                        consumed = true;
                    }
                    case ACCESSORY -> {
                        HunterData.toggleAccessory(serverPlayer, slotOrSkill, stack.getItem());
                        return InteractionResultHolder.success(stack);
                    }
                    case BLACK_HEART -> {
                        HunterData.awaken(serverPlayer);
                        HunterData.setBlackHeart(serverPlayer, true);
                        HunterData.unlockSkill(serverPlayer, "monarch_domain");
                        serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] The Black Heart has awakened.").withStyle(ChatFormatting.DARK_PURPLE));
                        consumed = true;
                    }
                    case RANDOM_BOX -> {
                        boolean cursed = stack.getItem().toString().contains("cursed_random_box");
                        ItemStack reward;
                        if (cursed) {
                            int roll = level.random.nextInt(3);
                            reward = roll == 0 ? new ItemStack(ModItems.ESSENCE_STONE.get(), 12)
                                    : roll == 1 ? new ItemStack(ModItems.DEMON_CASTLE_KEY.get())
                                    : new ItemStack(ModItems.GREATER_HEALING_POTION.get(), 4);
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 45, 1));
                            serverPlayer.hurt(serverPlayer.damageSources().magic(), Math.min(10.0F, serverPlayer.getMaxHealth() * 0.25F));
                            serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] The cursed box demanded a price.").withStyle(ChatFormatting.RED));
                        } else {
                            reward = level.random.nextBoolean() ? new ItemStack(ModItems.KASAKAS_VENOM_FANG.get()) : new ItemStack(ModItems.GREATER_MANA_POTION.get(), 2);
                        }
                        HunterData.storeSystemItem(serverPlayer, reward);
                        consumed = true;
                    }
                    case TELEPORT -> {
                        BlockPos spawn = serverPlayer.serverLevel().getSharedSpawnPos();
                        serverPlayer.teleportTo(serverPlayer.serverLevel(), spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
                        consumed = true;
                    }
                    default -> { return InteractionResultHolder.pass(stack); }
                }
                if (consumed) {
                    level.playSound(null, serverPlayer.blockPosition(), ModSounds.SYSTEM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                    if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
                    HunterData.sync(serverPlayer);
                    return InteractionResultHolder.consume(stack);
                }
                return InteractionResultHolder.pass(stack);
            }

            @Override
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                if (!detail.isBlank()) tooltip.add(Component.literal(detail).withStyle(ChatFormatting.DARK_AQUA));
                if (kind == Kind.ACCESSORY) tooltip.add(Component.translatable("tooltip.sololeveling.accessory_use").withStyle(ChatFormatting.GRAY));
                if (kind == Kind.RUNE) tooltip.add(Component.translatable("tooltip.sololeveling.rune_use", slotOrSkill).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (kind == Kind.BLACK_HEART) tooltip.add(Component.translatable("tooltip.sololeveling.bound_item").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(ItemAcquisitionCatalog.line(stack));
            }
        }
    """)
    write("src/main/java/com/tre/sololeveling/registry/ModItems.java", r"""
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
    """)
    write("src/main/java/com/tre/sololeveling/SoloLevelingMod.java", r"""
        package com.tre.sololeveling;

        import com.tre.sololeveling.config.ModConfigs;
        import com.tre.sololeveling.equipment.EquipmentConfig;
        import com.tre.sololeveling.network.ModNetwork;
        import com.tre.sololeveling.registry.ModItems;
        import com.tre.sololeveling.registry.ModSounds;
        import net.minecraft.world.item.CreativeModeTabs;
        import net.minecraftforge.common.MinecraftForge;
        import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
        import net.minecraftforge.eventbus.api.IEventBus;
        import net.minecraftforge.fml.ModLoadingContext;
        import net.minecraftforge.fml.common.Mod;
        import net.minecraftforge.fml.config.ModConfig;
        import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

        @Mod(SoloLevelingMod.MODID)
        public final class SoloLevelingMod {
            public static final String MODID = "sololeveling";

            public SoloLevelingMod() {
                IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
                ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfigs.SERVER_SPEC);
                ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EquipmentConfig.SPEC, "sololeveling-equipment.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModConfigs.CLIENT_SPEC);
                ModItems.ITEMS.register(modBus);
                ModSounds.SOUNDS.register(modBus);
                modBus.addListener(this::addCreativeTabContents);
                ModNetwork.register();
                MinecraftForge.EVENT_BUS.register(new com.tre.sololeveling.event.CommonEvents());
            }

            private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
                if (event.getTabKey() == CreativeModeTabs.COMBAT) ModItems.EQUIPMENT.forEach(event::accept);
                if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) ModItems.CONSUMABLES.forEach(event::accept);
                if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) ModItems.MATERIALS.forEach(event::accept);
            }
        }
    """)

def assets() -> None:
    generator = ROOT / "tools/generate_assets.py"
    text = generator.read_text(encoding="utf-8")
    text = text.replace(
        r"for m in re.finditer(r'(?:weapon|story|accessory|rune|add)\(\"([a-z0-9_]+)\"', java):",
        r"for m in re.finditer(r'(?:weapon|armor|story|accessory|rune|material|consumable)\(\"([a-z0-9_]+)\"', java):"
    )
    text = text.replace(
        "armoritem=any(x in id for x in ['chestplate','coat','top','trousers','pants','boots','shoes','helmet','gloves','gauntlets'])",
        "armoritem=any(x in id for x in ['chestplate','coat','top','jacket','trousers','leggings','pants','boots','shoes','helmet','hood','gloves','gauntlets'])"
    )
    text = text.replace(
        "if 'helmet' in id:",
        "if 'helmet' in id or 'hood' in id:"
    )
    text = text.replace(
        "elif any(x in id for x in ['pants','trousers']):",
        "elif any(x in id for x in ['pants','trousers','leggings']):"
    )
    text = text.replace(
        "elif any(x in id for x in ['necklace','ring','earring','orb']):",
        "elif any(x in id for x in ['necklace','ring','earring','orb','belt']):"
    )
    generator.write_text(text, encoding="utf-8")

    write("tools/finalize_equipment_assets.py", r"""
        from pathlib import Path
        import json

        ROOT = Path(__file__).resolve().parents[1]
        RES = ROOT / "src/main/resources"
        LANG_PATH = RES / "assets/sololeveling/lang/en_us.json"
        lang = json.loads(LANG_PATH.read_text(encoding="utf-8"))

        exact_names = {
            "training_dagger": "Training Dagger",
            "moonshadow_dagger": "Moonshadow Dagger",
            "shadow_reaper_dagger": "Shadow Reaper Dagger",
            "novice_hunter_hood": "Novice Hunter Hood",
            "novice_hunter_jacket": "Novice Hunter Jacket",
            "novice_hunter_leggings": "Novice Hunter Leggings",
            "novice_hunter_boots": "Novice Hunter Boots",
            "shadow_monarch_hood": "Shadow Monarch Hood",
            "hunters_ring": "Hunter’s Ring",
            "mana_weave_necklace": "Mana-Weave Necklace",
            "shadow_belt": "Shadow Belt",
            "monarch_earrings": "Monarch Earrings",
            "reinforcement_core": "Reinforcement Core",
            "monarch_reinforcement_core": "Monarch Reinforcement Core",
            "equipment_fragment": "Equipment Fragment",
            "enchanted_steel": "Enchanted Steel",
            "quest_relic": "System Quest Relic",
            "shadow_silk": "Shadow Silk"
        }
        for item_id, name in exact_names.items():
            lang[f"item.sololeveling.{item_id}"] = name

        generic = {
            "tooltip.sololeveling.rarity": "Rarity: %s",
            "tooltip.sololeveling.category_slot": "%s · %s",
            "tooltip.sololeveling.accessory_slot": "Accessory slot: %s",
            "tooltip.sololeveling.upgrade": "Upgrade: +%s / +%s",
            "tooltip.sololeveling.stat": "%s %s",
            "tooltip.sololeveling.set_piece": "%s set piece",
            "tooltip.sololeveling.ability": "Item ability: %s",
            "tooltip.sololeveling.acquisition": "Acquisition: %s",
            "tooltip.sololeveling.equipment_id": "Equipment ID: %s",
            "tooltip.sololeveling.accessory_use": "Right-click to equip or unequip",
            "tooltip.sololeveling.rune_use": "Absorb to unlock %s",
            "tooltip.sololeveling.bound_item": "Bound progression item",
            "tooltip.sololeveling.upgrade_material": "Reinforces equipment by +%s",
            "tooltip.sololeveling.upgrade_instructions": "Hold equipment in the opposite hand, then use this core",
            "message.sololeveling.upgrade_no_target": "Hold an equipment item in the opposite hand.",
            "message.sololeveling.upgrades_disabled": "Equipment upgrades are disabled by server configuration.",
            "message.sololeveling.upgrade_max": "That equipment is already fully reinforced.",
            "message.sololeveling.upgrade_success": "%s reinforced to +%s / +%s."
        }
        lang.update(generic)

        for rarity, name in {
            "common": "Common", "uncommon": "Uncommon", "rare": "Rare", "epic": "Epic",
            "legendary": "Legendary", "monarch": "Monarch"
        }.items():
            lang[f"rarity.sololeveling.{rarity}"] = name

        for category, name in {"dagger": "Dagger", "sword": "Sword", "armor": "Armor", "accessory": "Accessory"}.items():
            lang[f"category.sololeveling.{category}"] = name

        for slot, name in {
            "main_hand": "Main Hand", "off_hand": "Off Hand", "either_hand": "Either Hand",
            "head": "Head", "chest": "Chest", "legs": "Legs", "feet": "Feet", "accessory": "Accessory"
        }.items():
            lang[f"slot.sololeveling.{slot}"] = name

        for slot, name in {
            "none": "None", "ring": "Ring", "necklace": "Necklace", "belt": "Belt",
            "earring": "Earrings", "hands": "Hands", "orb": "Orb"
        }.items():
            lang[f"accessory_slot.sololeveling.{slot}"] = name

        for stat, name in {
            "strength": "Strength", "agility": "Agility", "stamina": "Stamina", "intelligence": "Intelligence",
            "sense": "Sense", "attack_damage": "Attack Damage", "attack_speed": "Attack Speed",
            "movement_speed": "Movement Speed", "max_health": "Max Health", "armor": "Armor",
            "armor_toughness": "Armor Toughness", "knockback_resistance": "Knockback Resistance",
            "luck": "Luck", "mana": "Mana", "shadow_capacity": "Shadow Capacity"
        }.items():
            lang[f"stat.sololeveling.{stat}"] = name

        for set_id, name in {
            "novice_hunter": "Novice Hunter", "high_knight": "High Knight", "assassins": "Assassin",
            "truth_seeker": "Truth Seeker", "demon_monarch": "Demon Monarch", "shadow_monarch": "Shadow Monarch",
            "red_knight": "Red Knight"
        }.items():
            lang[f"set.sololeveling.{set_id}"] = name

        abilities = {
            "training": "Practice Edge", "venom": "Venom and Paralysis", "knight_killer": "Armor Breaker",
            "frost_step": "Frost Step", "demon_pair": "Two as One", "demon_fire": "White Flame",
            "moonshadow": "Moonshadow Eclipse", "shadow_reaper": "Reaper’s Mark", "kamish_pair": "Dragon Wrath",
            "antares_flame": "Flames of Destruction", "novice_resolve": "Novice Resolve",
            "fortress": "Fortress", "silent_step": "Silent Step", "mana_weave": "Mana Weave",
            "demon_authority": "Demon Authority", "monarch_presence": "Monarch’s Presence"
        }
        for key, name in abilities.items():
            lang[f"ability.sololeveling.{key}"] = name

        acquisitions = {
            "unknown": "Unknown", "crafted": "Crafting", "low_rank_dungeon": "Low-rank dungeon loot",
            "mid_rank_dungeon": "Mid-rank dungeon loot", "demon_castle": "Demon Castle loot",
            "monarch_cache": "Monarch cache loot", "admin": "Administrator or test access",
            "quest_reward": "Quest reward", "main_quest": "Main quest", "system_reward": "System reward",
            "rune_drop": "Rune drop", "dungeon_loot": "Dungeon loot"
        }
        for key, name in acquisitions.items():
            lang[f"acquisition.sololeveling.{key}"] = name

        LANG_PATH.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        recipes = RES / "data/sololeveling/recipes"
        recipes.mkdir(parents=True, exist_ok=True)

        def dump(path, data):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

        shaped = {
            "training_dagger": ([" I", "S "], {"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "minecraft:stick"}}),
            "novice_hunter_hood": (["LLL", "L L"], {"L": {"item": "minecraft:leather"}}),
            "novice_hunter_jacket": (["L L", "LLL", "LLL"], {"L": {"item": "minecraft:leather"}}),
            "novice_hunter_leggings": (["LLL", "L L", "L L"], {"L": {"item": "minecraft:leather"}}),
            "novice_hunter_boots": (["L L", "L L"], {"L": {"item": "minecraft:leather"}})
        }
        for output, (pattern, key) in shaped.items():
            dump(recipes / f"{output}.json", {
                "type": "minecraft:crafting_shaped", "pattern": pattern, "key": key,
                "result": {"item": f"sololeveling:{output}", "count": 1}
            })

        shapeless = {
            "enchanted_steel": ["minecraft:iron_ingot", "sololeveling:mana_crystal"],
            "reinforcement_core": [
                "sololeveling:equipment_fragment", "sololeveling:equipment_fragment",
                "sololeveling:equipment_fragment", "sololeveling:mana_crystal"
            ],
            "knight_killer": ["sololeveling:training_dagger", "sololeveling:enchanted_steel", "sololeveling:reinforcement_core"],
            "mana_weave_necklace": ["minecraft:string", "sololeveling:mana_crystal", "minecraft:amethyst_shard"],
            "hunters_ring": ["minecraft:iron_nugget", "minecraft:iron_nugget", "sololeveling:mana_crystal"],
            "moonshadow_dagger": ["sololeveling:barukas_dagger", "sololeveling:shadow_silk", "sololeveling:reinforcement_core"],
            "shadow_reaper_dagger": [
                "sololeveling:moonshadow_dagger", "sololeveling:enchanted_steel",
                "sololeveling:reinforcement_core", "sololeveling:reinforcement_core"
            ],
            "monarch_reinforcement_core": [
                "sololeveling:reinforcement_core", "sololeveling:reinforcement_core",
                "sololeveling:shadow_silk", "sololeveling:essence_stone"
            ]
        }
        for output, ingredients in shapeless.items():
            dump(recipes / f"{output}.json", {
                "type": "minecraft:crafting_shapeless",
                "ingredients": [{"item": item} for item in ingredients],
                "result": {"item": f"sololeveling:{output}", "count": 1}
            })

        loot_root = RES / "data/sololeveling/loot_tables/inject"
        modifier_root = RES / "data/sololeveling/loot_modifiers"
        global_root = RES / "data/forge/loot_modifiers"

        def item_entry(name, weight, minimum=1.0, maximum=1.0):
            entry = {"type": "minecraft:item", "name": f"sololeveling:{name}", "weight": weight}
            if minimum != 1.0 or maximum != 1.0:
                entry["functions"] = [{
                    "function": "minecraft:set_count",
                    "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum},
                    "add": False
                }]
            return entry

        tables = {
            "low_rank_dungeon": [
                item_entry("equipment_fragment", 20, 1.0, 3.0),
                item_entry("mana_crystal", 16, 1.0, 3.0),
                item_entry("healing_potion", 12, 1.0, 2.0),
                item_entry("mana_potion", 12, 1.0, 2.0),
                item_entry("training_dagger", 3)
            ],
            "mid_rank_dungeon": [
                item_entry("equipment_fragment", 18, 2.0, 5.0),
                item_entry("enchanted_steel", 12),
                item_entry("reinforcement_core", 8),
                item_entry("knight_killer", 2),
                item_entry("barukas_dagger", 1),
                item_entry("assassins_hood", 1)
            ],
            "monarch_cache": [
                item_entry("shadow_silk", 14, 1.0, 3.0),
                item_entry("monarch_reinforcement_core", 6),
                item_entry("moonshadow_dagger", 2),
                item_entry("shadow_reaper_dagger", 1),
                item_entry("shadow_monarch_hood", 1),
                item_entry("shadow_belt", 1),
                item_entry("monarch_earrings", 1)
            ]
        }
        for name, entries in tables.items():
            dump(loot_root / f"{name}.json", {
                "type": "minecraft:chest",
                "pools": [{
                    "rolls": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0},
                    "bonus_rolls": 0.0,
                    "entries": entries
                }]
            })

        modifier_targets = {
            "low_rank_dungeon": "minecraft:chests/simple_dungeon",
            "mid_rank_dungeon": "minecraft:chests/abandoned_mineshaft",
            "monarch_cache": "minecraft:chests/ancient_city"
        }
        for name, target in modifier_targets.items():
            dump(modifier_root / f"{name}.json", {
                "type": "forge:add_table",
                "conditions": [{"condition": "forge:loot_table_id", "loot_table_id": target}],
                "table": f"sololeveling:inject/{name}"
            })
        dump(global_root / "global_loot_modifiers.json", {
            "replace": False,
            "entries": [f"sololeveling:{name}" for name in modifier_targets]
        })

        summary = {
            "recipes": sorted(path.stem for path in recipes.glob("*.json")),
            "loot_tables": sorted(path.stem for path in loot_root.glob("*.json")),
            "loot_modifiers": sorted(path.stem for path in modifier_root.glob("*.json"))
        }
        (ROOT / "EQUIPMENT_ACQUISITION.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    """)
    write("tools/validate_equipment_assets.py", r"""
        from pathlib import Path
        import json
        import re
        import sys

        ROOT = Path(__file__).resolve().parents[1]
        RES = ROOT / "src/main/resources"
        MOD_ITEMS = (ROOT / "src/main/java/com/tre/sololeveling/registry/ModItems.java").read_text(encoding="utf-8")
        CATALOG = (ROOT / "src/main/java/com/tre/sololeveling/equipment/EquipmentCatalog.java").read_text(encoding="utf-8")

        ids = []
        for match in re.finditer(r'(?:weapon|armor|story|accessory|rune|material|consumable)\("([a-z0-9_]+)"', MOD_ITEMS):
            if match.group(1) not in ids:
                ids.append(match.group(1))

        equipment_ids = set(re.findall(r'(?:weapon|armor|accessory)\("([a-z0-9_]+)"', CATALOG))
        registry_ids = set(ids)
        errors = []

        lang_path = RES / "assets/sololeveling/lang/en_us.json"
        try:
            lang = json.loads(lang_path.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"Invalid language file: {exc}")
            lang = {}

        for item_id in ids:
            model = RES / f"assets/sololeveling/models/item/{item_id}.json"
            texture = RES / f"assets/sololeveling/textures/item/{item_id}.png"
            if not model.exists():
                errors.append(f"Missing model: {item_id}")
            else:
                try:
                    data = json.loads(model.read_text(encoding="utf-8"))
                    layer = data.get("textures", {}).get("layer0", "")
                    if layer != f"sololeveling:item/{item_id}":
                        errors.append(f"Wrong model texture reference: {item_id} -> {layer}")
                except Exception as exc:
                    errors.append(f"Invalid model JSON {item_id}: {exc}")
            if not texture.exists():
                errors.append(f"Missing texture: {item_id}")
            elif texture.read_bytes()[:8] != b"\x89PNG\r\n\x1a\n":
                errors.append(f"Invalid PNG signature: {item_id}")
            if f"item.sololeveling.{item_id}" not in lang:
                errors.append(f"Missing translation: {item_id}")

        missing_registry = sorted(equipment_ids - registry_ids)
        if missing_registry:
            errors.append("Equipment definitions missing registry entries: " + ", ".join(missing_registry))

        required_translation_prefixes = [
            "tooltip.sololeveling.rarity", "tooltip.sololeveling.upgrade",
            "tooltip.sololeveling.acquisition", "message.sololeveling.upgrade_success"
        ]
        for key in required_translation_prefixes:
            if key not in lang:
                errors.append(f"Missing framework translation: {key}")

        registered = {f"sololeveling:{item_id}" for item_id in ids}
        json_roots = [
            RES / "data/sololeveling/recipes",
            RES / "data/sololeveling/loot_tables",
            RES / "data/sololeveling/loot_modifiers",
            RES / "data/forge/loot_modifiers"
        ]

        def walk(value, source):
            if isinstance(value, dict):
                for key, child in value.items():
                    if key in {"item", "name"} and isinstance(child, str) and child.startswith("sololeveling:") and child not in registered:
                        errors.append(f"Unknown item reference {child} in {source}")
                    walk(child, source)
            elif isinstance(value, list):
                for child in value:
                    walk(child, source)

        parsed = 0
        for root in json_roots:
            if not root.exists():
                errors.append(f"Missing data directory: {root.relative_to(ROOT)}")
                continue
            for path in root.rglob("*.json"):
                try:
                    data = json.loads(path.read_text(encoding="utf-8"))
                    parsed += 1
                    walk(data, path.relative_to(ROOT))
                except Exception as exc:
                    errors.append(f"Invalid JSON {path.relative_to(ROOT)}: {exc}")

        if errors:
            print("\n".join(f"ERROR: {error}" for error in errors))
            sys.exit(1)
        print(f"validated {len(ids)} items, {len(equipment_ids)} equipment definitions and {parsed} recipe/loot JSON files")
    """)

def main() -> None:
    if len(sys.argv) != 2 or sys.argv[1] not in {"framework", "items", "assets"}:
        raise SystemExit("usage: apply_equipment_patch.py framework|items|assets")
    {"framework": framework, "items": items, "assets": assets}[sys.argv[1]]()

if __name__ == "__main__":
    main()
