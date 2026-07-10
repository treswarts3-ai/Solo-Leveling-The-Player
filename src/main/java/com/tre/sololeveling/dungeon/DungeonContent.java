package com.tre.sololeveling.dungeon;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DungeonContent {
    private static final Map<String, DungeonTypes.EnemyDefinition> ENEMIES = new LinkedHashMap<>();
    private static final Map<String, DungeonTypes.DungeonTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        enemy(new DungeonTypes.EnemyDefinition("goblin_soldier", DungeonTypes.EnemyKind.MELEE, 24, 5, 0.24, 2, false, true));
        enemy(new DungeonTypes.EnemyDefinition("steel_fang_raider", DungeonTypes.EnemyKind.FAST, 18, 4, 0.38, 0, false, true));
        enemy(new DungeonTypes.EnemyDefinition("stone_guardian", DungeonTypes.EnemyKind.TANK, 80, 9, 0.18, 12, false, true));
        enemy(new DungeonTypes.EnemyDefinition("dungeon_archer", DungeonTypes.EnemyKind.RANGED, 28, 5, 0.25, 2, false, true));
        enemy(new DungeonTypes.EnemyDefinition("orc_commander", DungeonTypes.EnemyKind.ELITE, 140, 13, 0.29, 10, true, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_goblin", DungeonTypes.EnemyKind.MELEE, 36, 7, 0.27, 4, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_raider", DungeonTypes.EnemyKind.FAST, 28, 6, 0.42, 2, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_guardian", DungeonTypes.EnemyKind.TANK, 110, 12, 0.20, 16, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_archer", DungeonTypes.EnemyKind.RANGED, 42, 8, 0.28, 4, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_commander", DungeonTypes.EnemyKind.ELITE, 190, 16, 0.31, 14, true, true));

        Map<String, DungeonTypes.WaveDefinition> lowWaves = waves(
                wave("low_wave_1", false, spawn("goblin_soldier", 4), spawn("steel_fang_raider", 2)),
                wave("low_collection_wave", true, spawn("goblin_soldier", 4), spawn("dungeon_archer", 2)),
                wave("low_elite", false, spawn("orc_commander", 1), spawn("goblin_soldier", 2))
        );
        template(new DungeonTypes.DungeonTemplate(
                "abandoned_subway", "Abandoned Subway", DungeonTypes.GateRank.E, 20 * 60 * 14,
                List.of(
                        objective(DungeonTypes.ObjectiveType.WAVE, "clear_platform", "low_wave_1", 6, 20 * 60 * 4, "Clear the abandoned platform"),
                        objective(DungeonTypes.ObjectiveType.COLLECTION, "recover_mana_crystals", "low_collection_wave", 4, 20 * 60 * 4, "Recover four mana crystals"),
                        objective(DungeonTypes.ObjectiveType.ELITE, "defeat_station_guard", "low_elite", 3, 20 * 60 * 3, "Defeat the station guard and escort"),
                        objective(DungeonTypes.ObjectiveType.BOSS, "defeat_subway_warden", "subway_warden", 1, 20 * 60 * 5, "Defeat the Subway Warden"),
                        objective(DungeonTypes.ObjectiveType.REWARD, "claim_low_reward", "reward_room", 1, 20 * 60, "Enter the sealed reward vault")
                ), lowWaves, reward(900, 350, item("minecraft:emerald", 5), item("minecraft:amethyst_shard", 10), item("minecraft:gold_ingot", 4))
        ));

        Map<String, DungeonTypes.WaveDefinition> midWaves = waves(
                wave("mid_wave_1", false, spawn("shadow_goblin", 6), spawn("shadow_raider", 4), spawn("shadow_archer", 3)),
                wave("mid_collection_wave", true, spawn("shadow_raider", 5), spawn("shadow_guardian", 2), spawn("shadow_archer", 3)),
                wave("mid_elite", false, spawn("shadow_commander", 1), spawn("shadow_goblin", 3))
        );
        template(new DungeonTypes.DungeonTemplate(
                "red_orc_outpost", "Red Orc Outpost", DungeonTypes.GateRank.B, 20 * 60 * 16,
                List.of(
                        objective(DungeonTypes.ObjectiveType.WAVE, "break_outer_guard", "mid_wave_1", 13, 20 * 60 * 5, "Break the outer guard"),
                        objective(DungeonTypes.ObjectiveType.COLLECTION, "recover_essence_stones", "mid_collection_wave", 8, 20 * 60 * 5, "Recover eight essence stones"),
                        objective(DungeonTypes.ObjectiveType.ELITE, "defeat_orc_commander", "mid_elite", 4, 20 * 60 * 4, "Defeat the orc commander and escort"),
                        objective(DungeonTypes.ObjectiveType.REWARD, "claim_mid_reward", "reward_room", 1, 20 * 60, "Enter the reward room")
                ), midWaves, reward(2200, 900, item("minecraft:diamond", 2), item("minecraft:emerald", 12))
        ));

        Map<String, DungeonTypes.WaveDefinition> highWaves = waves(
                wave("high_wave_1", false, spawn("shadow_goblin", 6), spawn("shadow_raider", 5), spawn("shadow_guardian", 3), spawn("shadow_archer", 4)),
                wave("high_collection_wave", true, spawn("shadow_raider", 6), spawn("shadow_guardian", 3), spawn("shadow_archer", 4)),
                wave("high_elite", false, spawn("shadow_commander", 2), spawn("shadow_guardian", 2))
        );
        template(new DungeonTypes.DungeonTemplate(
                "demon_castle_foyer", "Demon Castle Foyer", DungeonTypes.GateRank.A, 20 * 60 * 22,
                List.of(
                        objective(DungeonTypes.ObjectiveType.WAVE, "survive_castle_vanguard", "high_wave_1", 18, 20 * 60 * 6, "Survive the castle vanguard"),
                        objective(DungeonTypes.ObjectiveType.COLLECTION, "seal_demonic_cores", "high_collection_wave", 10, 20 * 60 * 5, "Seal ten demonic cores"),
                        objective(DungeonTypes.ObjectiveType.ELITE, "defeat_twin_commanders", "high_elite", 4, 20 * 60 * 5, "Defeat the twin commanders"),
                        objective(DungeonTypes.ObjectiveType.BOSS, "defeat_iron_sovereign", "iron_sovereign", 1, 20 * 60 * 6, "Defeat the Iron Sovereign"),
                        objective(DungeonTypes.ObjectiveType.REWARD, "claim_high_reward", "reward_room", 1, 20 * 60, "Enter the sovereign vault")
                ), highWaves, reward(6000, 2600, item("minecraft:netherite_scrap", 2), item("minecraft:diamond", 5), item("minecraft:echo_shard", 3))
        ));
    }

    public static DungeonTypes.DungeonTemplate template(String id) { return TEMPLATES.get(DungeonTypes.id(id)); }
    public static DungeonTypes.EnemyDefinition enemy(String id) { return ENEMIES.get(DungeonTypes.id(id)); }
    public static Set<String> templateIds() { return TEMPLATES.keySet(); }
    public static Set<String> enemyIds() { return ENEMIES.keySet(); }
    public static Map<String, DungeonTypes.DungeonTemplate> templates() { return Map.copyOf(TEMPLATES); }
    public static Map<String, DungeonTypes.EnemyDefinition> enemies() { return Map.copyOf(ENEMIES); }

    private static void template(DungeonTypes.DungeonTemplate definition) { TEMPLATES.put(definition.id(), definition); }
    private static void enemy(DungeonTypes.EnemyDefinition definition) { ENEMIES.put(definition.id(), definition); }
    private static DungeonTypes.ObjectiveDefinition objective(DungeonTypes.ObjectiveType type, String id, String encounter, int target, int ticks, String name) {
        return new DungeonTypes.ObjectiveDefinition(type, id, encounter, target, ticks, name);
    }
    private static DungeonTypes.SpawnEntry spawn(String id, int count) { return new DungeonTypes.SpawnEntry(id, count); }
    private static DungeonTypes.WaveDefinition wave(String id, boolean drops, DungeonTypes.SpawnEntry... entries) {
        return new DungeonTypes.WaveDefinition(id, List.of(entries), drops);
    }
    private static Map<String, DungeonTypes.WaveDefinition> waves(DungeonTypes.WaveDefinition... definitions) {
        Map<String, DungeonTypes.WaveDefinition> map = new LinkedHashMap<>();
        for (DungeonTypes.WaveDefinition definition : definitions) map.put(definition.id(), definition);
        return map;
    }
    private static DungeonTypes.ItemReward item(String id, int count) { return new DungeonTypes.ItemReward(new ResourceLocation(id), count); }
    private static DungeonTypes.RewardDefinition reward(int xp, int gold, DungeonTypes.ItemReward... items) {
        return new DungeonTypes.RewardDefinition(xp, gold, List.of(items));
    }

    private DungeonContent() {}
}
