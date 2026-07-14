package com.tre.sololeveling.dungeon;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Registry of combat content for the single active master dungeon. */
public final class DungeonContent {
    private static final Map<String, DungeonTypes.EnemyDefinition> ENEMIES = new LinkedHashMap<>();
    private static final Map<String, DungeonTypes.DungeonTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        enemy(new DungeonTypes.EnemyDefinition("goblin_soldier", DungeonTypes.EnemyKind.MELEE, 24, 5, 0.24, 2, false, true));
        enemy(new DungeonTypes.EnemyDefinition("steel_fang_raider", DungeonTypes.EnemyKind.ASSASSIN, 18, 4, 0.38, 0, false, true));
        enemy(new DungeonTypes.EnemyDefinition("stone_guardian", DungeonTypes.EnemyKind.TANK, 80, 9, 0.18, 12, false, true));
        enemy(new DungeonTypes.EnemyDefinition("dungeon_archer", DungeonTypes.EnemyKind.RANGED, 28, 5, 0.25, 2, false, true));
        enemy(new DungeonTypes.EnemyDefinition("orc_commander", DungeonTypes.EnemyKind.ELITE, 110, 10, 0.27, 8, true, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_goblin", DungeonTypes.EnemyKind.MELEE, 36, 7, 0.27, 4, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_raider", DungeonTypes.EnemyKind.ASSASSIN, 28, 6, 0.42, 2, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_guardian", DungeonTypes.EnemyKind.TANK, 110, 12, 0.20, 16, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_archer", DungeonTypes.EnemyKind.RANGED, 42, 8, 0.28, 4, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_commander", DungeonTypes.EnemyKind.ELITE, 190, 16, 0.31, 14, true, true));
        enemy(new DungeonTypes.EnemyDefinition("abyss_channeler", DungeonTypes.EnemyKind.SUMMONER, 38, 4, 0.22, 3, false, true));
        enemy(new DungeonTypes.EnemyDefinition("shadow_channeler", DungeonTypes.EnemyKind.SUMMONER, 58, 6, 0.24, 5, false, true));

        Map<String, DungeonTypes.WaveDefinition> waves = waves(
                wave("bridge_vanguard", false,
                        spawn("goblin_soldier", 4), spawn("steel_fang_raider", 4)),
                wave("ossuary_harvest", true,
                        spawn("shadow_goblin", 4), spawn("dungeon_archer", 4)),
                wave("plaza_siege", false,
                        spawn("shadow_goblin", 5), spawn("shadow_raider", 4), spawn("shadow_archer", 3)),
                wave("foundry_overseers", false,
                        spawn("orc_commander", 2), spawn("stone_guardian", 2), spawn("dungeon_archer", 2)),
                wave("basilica_relics", true,
                        spawn("shadow_guardian", 2), spawn("shadow_archer", 3), spawn("shadow_raider", 2),
                        spawn("abyss_channeler", 1)),
                wave("prison_wardens", false,
                        spawn("shadow_commander", 2), spawn("shadow_guardian", 2), spawn("shadow_goblin", 2)),
                wave("sunken_legion", false,
                        spawn("shadow_goblin", 5), spawn("shadow_raider", 4),
                        spawn("shadow_archer", 2), spawn("shadow_guardian", 2), spawn("shadow_channeler", 1))
        );

        template(new DungeonTypes.DungeonTemplate(
                MasterDungeonBuilder.ID, MasterDungeonBuilder.DISPLAY_NAME, DungeonTypes.GateRank.A, 20 * 60 * 38,
                List.of(
                        objective(DungeonTypes.ObjectiveType.WAVE, "break_bridge_vanguard", "bridge_vanguard", 8,
                                20 * 60 * 5, "Break the vanguard at the Broken Bridge"),
                        objective(DungeonTypes.ObjectiveType.COLLECTION, "recover_ossuary_sigils", "ossuary_harvest", 6,
                                20 * 60 * 5, "Recover six death sigils in the Ossuary Ward"),
                        objective(DungeonTypes.ObjectiveType.WAVE, "survive_buried_plaza", "plaza_siege", 12,
                                20 * 60 * 6, "Survive the siege of the Buried Kingdom"),
                        objective(DungeonTypes.ObjectiveType.ELITE, "defeat_foundry_overseers", "foundry_overseers", 6,
                                20 * 60 * 6, "Defeat the Abyssal Foundry overseers"),
                        objective(DungeonTypes.ObjectiveType.COLLECTION, "unbind_monarch_seals", "basilica_relics", 6,
                                20 * 60 * 5, "Unbind six monarch seals in the basilica"),
                        objective(DungeonTypes.ObjectiveType.ELITE, "break_prison_wardens", "prison_wardens", 6,
                                20 * 60 * 5, "Break the wardens of the Chained Prison"),
                        objective(DungeonTypes.ObjectiveType.WAVE, "clear_sunken_court", "sunken_legion", 14,
                                20 * 60 * 6, "Clear the Sunken Court"),
                        objective(DungeonTypes.ObjectiveType.BOSS, "defeat_abyssal_monarch", "abyssal_monarch", 1,
                                20 * 60 * 8, "Defeat the Abyssal Monarch"),
                        objective(DungeonTypes.ObjectiveType.REWARD, "claim_necropolis_heart", "reward_room", 1,
                                20 * 60 * 2, "Enter the Heart Vault")
                ), waves, reward(9_000, 4_000,
                        item("minecraft:netherite_scrap", 3), item("minecraft:diamond", 8),
                        item("minecraft:echo_shard", 6), item("minecraft:enchanted_golden_apple", 1))
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
    private static DungeonTypes.ObjectiveDefinition objective(DungeonTypes.ObjectiveType type, String id,
                                                               String encounter, int target, int ticks, String name) {
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
    private static DungeonTypes.ItemReward item(String id, int count) {
        return new DungeonTypes.ItemReward(new ResourceLocation(id), count);
    }
    private static DungeonTypes.RewardDefinition reward(int xp, int gold, DungeonTypes.ItemReward... items) {
        return new DungeonTypes.RewardDefinition(xp, gold, List.of(items));
    }

    private DungeonContent() {}
}
