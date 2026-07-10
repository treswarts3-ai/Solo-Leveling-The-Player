package com.tre.sololeveling.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.tre.sololeveling.quest.QuestTypes.Category;
import static com.tre.sololeveling.quest.QuestTypes.Objective;
import static com.tre.sololeveling.quest.QuestTypes.ObjectiveType;

public final class QuestRegistry {
    private static final String MODID = "sololeveling";
    private static final Map<ResourceLocation, QuestTypes.Definition> QUESTS = new LinkedHashMap<>();

    public static final ResourceLocation AWAKENING = id("awakening_introduction");
    public static final ResourceLocation DAILY_EXERCISE = id("daily_exercise");
    public static final ResourceLocation FIRST_COMBAT = id("first_combat");
    public static final ResourceLocation FIRST_DUNGEON = id("first_dungeon");
    public static final ResourceLocation STAT_TUTORIAL = id("stat_allocation_tutorial");
    public static final ResourceLocation SKILL_UNLOCK = id("skill_unlock");
    public static final ResourceLocation EQUIPMENT_REWARD = id("equipment_reward");
    public static final ResourceLocation SHADOW_INTRODUCTION = id("shadow_system_introduction");

    static {
        register(definition(AWAKENING, "Awakening", "Open the System and accept your new status as the Player.", Category.STORY,
                List.of(), List.of(objective("open_system", ObjectiveType.SYSTEM_OPEN, "system", 1)),
                reward(150, 100, 1), QuestTypes.Penalty.none(), false, false, true));

        register(definition(DAILY_EXERCISE, "Preparation to Become Powerful", "Complete the System's introductory exercise routine.", Category.DAILY,
                List.of(AWAKENING), List.of(
                        objective("pushups", ObjectiveType.EXERCISE, "pushup", 10),
                        objective("situps", ObjectiveType.EXERCISE, "situp", 10),
                        objective("squats", ObjectiveType.EXERCISE, "squat", 10)),
                reward(500, 250, 3), new QuestTypes.Penalty(4, 50, true), true, true, true));

        register(definition(FIRST_COMBAT, "First Combat", "Prove that you can survive against hostile enemies.", Category.STORY,
                List.of(DAILY_EXERCISE), List.of(objective("kill_enemies", ObjectiveType.KILL, "any", 5)),
                reward(350, 150, 1), QuestTypes.Penalty.none(), false, false, true));

        register(definition(FIRST_DUNGEON, "First Dungeon", "Enter and clear a System-recognized dungeon.", Category.STORY,
                List.of(FIRST_COMBAT), List.of(objective("clear_dungeon", ObjectiveType.DUNGEON_CLEAR, "any", 1)),
                reward(750, 300, 2), QuestTypes.Penalty.none(), false, false, true));

        register(definition(STAT_TUTORIAL, "Strength Through Choice", "Allocate a stat point to shape your growth.", Category.TUTORIAL,
                List.of(FIRST_DUNGEON), List.of(objective("allocate_stat", ObjectiveType.STAT_ALLOCATION, "any", 1)),
                reward(250, 100, 2), QuestTypes.Penalty.none(), false, false, true));

        register(definition(SKILL_UNLOCK, "A New Skill", "Unlock your first active skill through the System.", Category.TUTORIAL,
                List.of(STAT_TUTORIAL), List.of(objective("unlock_skill", ObjectiveType.SKILL_UNLOCK, "any", 1)),
                new QuestTypes.Reward(400, 150, 1, Map.of(), Set.of("quicksilver")), QuestTypes.Penalty.none(), false, false, true));

        register(definition(EQUIPMENT_REWARD, "Hunter Equipment Trial", "Collect essence stones to earn a weapon suitable for a hunter.", Category.STORY,
                List.of(SKILL_UNLOCK), List.of(objective("collect_essence", ObjectiveType.COLLECTION, "sololeveling:essence_stone", 3)),
                new QuestTypes.Reward(600, 250, 1, Map.of(id("knight_killer"), 1), Set.of("shadow_extraction")),
                QuestTypes.Penalty.none(), false, false, true));

        register(definition(SHADOW_INTRODUCTION, "The Shadow System", "Summon a shadow and establish your authority over it.", Category.STORY,
                List.of(EQUIPMENT_REWARD), List.of(objective("summon_shadow", ObjectiveType.SHADOW_SUMMON, "any", 1)),
                reward(1000, 500, 3), QuestTypes.Penalty.none(), false, false, true));

        ResourceLocation repeatPrerequisite = AWAKENING;
        register(definition(id("repeat_kill_enemies"), "Hunter Extermination", "Defeat hostile enemies.", Category.REPEATABLE,
                List.of(repeatPrerequisite), List.of(objective("kills", ObjectiveType.KILL, "any", 15)),
                reward(450, 180, 0), QuestTypes.Penalty.none(), true, false, false));
        register(definition(id("repeat_collect_materials"), "Material Recovery", "Collect useful magical materials.", Category.REPEATABLE,
                List.of(repeatPrerequisite), List.of(objective("materials", ObjectiveType.COLLECTION, "sololeveling:mana_crystal", 8)),
                reward(400, 200, 0), QuestTypes.Penalty.none(), true, false, false));
        register(definition(id("repeat_spend_mana"), "Mana Conditioning", "Spend mana through combat techniques.", Category.REPEATABLE,
                List.of(repeatPrerequisite), List.of(objective("mana", ObjectiveType.MANA_SPENT, "any", 250)),
                reward(500, 150, 0), QuestTypes.Penalty.none(), true, false, false));
        register(definition(id("repeat_use_abilities"), "Skill Practice", "Use active abilities in the field.", Category.REPEATABLE,
                List.of(repeatPrerequisite), List.of(objective("abilities", ObjectiveType.ABILITY_USE, "any", 12)),
                reward(500, 175, 0), QuestTypes.Penalty.none(), true, false, false));
        register(definition(id("repeat_clear_dungeon"), "Gate Suppression", "Clear a System-recognized dungeon.", Category.REPEATABLE,
                List.of(repeatPrerequisite), List.of(objective("dungeons", ObjectiveType.DUNGEON_CLEAR, "any", 1)),
                reward(900, 400, 1), QuestTypes.Penalty.none(), true, false, false));
        register(definition(id("repeat_summon_shadows"), "Shadow Muster", "Summon shadows into active service.", Category.REPEATABLE,
                List.of(SHADOW_INTRODUCTION), List.of(objective("shadows", ObjectiveType.SHADOW_SUMMON, "any", 5)),
                reward(700, 250, 0), QuestTypes.Penalty.none(), true, false, false));
    }

    public static QuestTypes.Definition register(QuestTypes.Definition definition) {
        QuestTypes.Definition previous = QUESTS.putIfAbsent(definition.id(), definition);
        if (previous != null) throw new IllegalStateException("Duplicate quest id: " + definition.id());
        return definition;
    }

    public static Optional<QuestTypes.Definition> find(ResourceLocation id) { return Optional.ofNullable(QUESTS.get(id)); }
    public static Collection<QuestTypes.Definition> all() { return List.copyOf(QUESTS.values()); }
    public static Set<ResourceLocation> ids() { return Set.copyOf(QUESTS.keySet()); }

    public static ResourceLocation id(String path) { return new ResourceLocation(MODID, path); }
    public static ResourceLocation parse(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        return parsed == null ? id(value) : parsed;
    }

    private static Objective objective(String id, ObjectiveType type, String target, int required) {
        return new Objective(id, type, target, required);
    }

    private static QuestTypes.Reward reward(int xp, int gold, int points) {
        return QuestTypes.Reward.of(xp, gold, points);
    }

    private static QuestTypes.Definition definition(ResourceLocation id, String name, String description, Category category,
                                                     List<ResourceLocation> prerequisites, List<Objective> objectives,
                                                     QuestTypes.Reward reward, QuestTypes.Penalty penalty,
                                                     boolean repeatable, boolean daily, boolean autoStart) {
        return new QuestTypes.Definition(id, name, description, category, prerequisites, objectives, reward, penalty,
                repeatable, daily, autoStart);
    }

    private QuestRegistry() {}
}
