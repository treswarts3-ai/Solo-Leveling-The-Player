package com.tre.sololeveling.quest;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestRegistry {
    private static final Map<ResourceLocation, QuestDefinition> QUESTS = new LinkedHashMap<>();

    static {
        registerStarterQuests();
        registerRepeatableQuests();
    }

    public static QuestDefinition register(QuestDefinition definition) {
        QuestDefinition previous = QUESTS.putIfAbsent(definition.id(), definition);
        if (previous != null) throw new IllegalStateException("Duplicate quest id: " + definition.id());
        return definition;
    }

    public static QuestDefinition get(ResourceLocation id) { return QUESTS.get(id); }
    public static QuestDefinition get(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? null : get(parsed);
    }
    public static Collection<QuestDefinition> values() { return Collections.unmodifiableCollection(QUESTS.values()); }
    public static String[] ids() { return QUESTS.keySet().stream().map(ResourceLocation::toString).toArray(String[]::new); }

    private static void registerStarterQuests() {
        register(QuestDefinition.builder("sololeveling:awakening_introduction", "Awakening")
                .description("Open the System interface and accept your new status as the Player.")
                .category(QuestCategory.TUTORIAL).storyOrder(0)
                .objective("open_system", QuestObjectiveType.SYSTEM_OPEN, "system", 1, "Open the System interface")
                .reward(QuestReward.builder().xp(100).gold(50).build())
                .build());

        register(QuestDefinition.builder("sololeveling:daily_exercise", "Preparation to Become Powerful")
                .description("Complete a shortened daily training routine.")
                .category(QuestCategory.DAILY).storyOrder(1).daily()
                .prerequisite("sololeveling:awakening_introduction")
                .objective("pushups", QuestObjectiveType.EXERCISE, "pushup", 5, "Complete 5 push-ups")
                .objective("situps", QuestObjectiveType.EXERCISE, "situp", 5, "Complete 5 sit-ups")
                .objective("squats", QuestObjectiveType.EXERCISE, "squat", 5, "Complete 5 squats")
                .objective("sprint", QuestObjectiveType.DISTANCE, "sprint", 100, "Sprint 100 blocks")
                .reward(QuestReward.builder().xp(250).gold(150).statPoints(1).build())
                .penalty((player, reason) -> HunterData.setGold(player, Math.max(0, HunterData.getGold(player) - 50)))
                .build());

        register(QuestDefinition.builder("sololeveling:first_combat", "First Combat")
                .description("Prove that the System has changed you by defeating hostile enemies.")
                .category(QuestCategory.STORY).storyOrder(2)
                .prerequisite("sololeveling:daily_exercise")
                .objective("hostile_kills", QuestObjectiveType.KILL, "hostile", 3, "Defeat 3 hostile enemies")
                .reward(QuestReward.builder().xp(350).gold(250).build())
                .build());

        register(QuestDefinition.builder("sololeveling:first_dungeon", "First Dungeon")
                .description("Enter a gate and clear your first dungeon.")
                .category(QuestCategory.STORY).storyOrder(3)
                .prerequisite("sololeveling:first_combat")
                .objective("dungeon_clear", QuestObjectiveType.DUNGEON_CLEAR, "any", 1, "Clear any dungeon")
                .reward(QuestReward.builder().xp(600).gold(400).build())
                .build());

        register(QuestDefinition.builder("sololeveling:stat_allocation_tutorial", "Strength Through Choice")
                .description("Allocate a stat point through the System.")
                .category(QuestCategory.TUTORIAL).storyOrder(4)
                .prerequisite("sololeveling:first_dungeon")
                .objective("allocate_stat", QuestObjectiveType.STAT_ALLOCATION, "any", 1, "Allocate 1 stat point")
                .reward(QuestReward.builder().xp(250).statPoints(2).skill("bloodlust").build())
                .build());

        register(QuestDefinition.builder("sololeveling:skill_unlock", "A New Skill")
                .description("Confirm that Bloodlust has been unlocked by the System.")
                .category(QuestCategory.TUTORIAL).storyOrder(5)
                .prerequisite("sololeveling:stat_allocation_tutorial")
                .objective("unlock_bloodlust", QuestObjectiveType.SKILL_UNLOCK, "bloodlust", 1, "Unlock Bloodlust")
                .reward(QuestReward.builder().xp(300).gold(200).build())
                .build());

        register(QuestDefinition.builder("sololeveling:equipment_reward", "Hunter Equipment Trial")
                .description("Defeat enemies while the System evaluates your combat readiness.")
                .category(QuestCategory.STORY).storyOrder(6)
                .prerequisite("sololeveling:skill_unlock")
                .objective("trial_kills", QuestObjectiveType.KILL, "hostile", 5, "Defeat 5 hostile enemies")
                .reward(QuestReward.builder().xp(500).gold(300)
                        .item("sololeveling:knight_killer", 1).skill("shadow_extraction").build())
                .build());

        register(QuestDefinition.builder("sololeveling:shadow_system_introduction", "The Shadow System")
                .description("Summon a preserved shadow for the first time.")
                .category(QuestCategory.STORY).storyOrder(7)
                .prerequisite("sololeveling:equipment_reward")
                .objective("summon_shadow", QuestObjectiveType.SHADOW_SUMMON, "any", 1, "Summon a shadow")
                .reward(QuestReward.builder().xp(800).gold(500).statPoints(2).build())
                .build());
    }

    private static void registerRepeatableQuests() {
        register(QuestDefinition.builder("sololeveling:repeat_kill_enemies", "Enemy Suppression")
                .description("Defeat hostile enemies for a repeatable System bounty.")
                .category(QuestCategory.REPEATABLE).repeatable()
                .prerequisite("sololeveling:first_combat")
                .objective("kills", QuestObjectiveType.KILL, "hostile", 20, "Defeat 20 hostile enemies")
                .reward(QuestReward.builder().xp(650).gold(450).build()).build());

        register(QuestDefinition.builder("sololeveling:repeat_collect_materials", "Material Acquisition")
                .description("Collect useful materials for the System.")
                .category(QuestCategory.REPEATABLE).repeatable()
                .prerequisite("sololeveling:first_combat")
                .objective("iron", QuestObjectiveType.COLLECTION, "minecraft:iron_ingot", 16, "Hold 16 iron ingots")
                .reward(QuestReward.builder().xp(350).gold(300).build()).build());

        register(QuestDefinition.builder("sololeveling:repeat_spend_mana", "Mana Conditioning")
                .description("Spend mana by using System abilities.")
                .category(QuestCategory.REPEATABLE).repeatable()
                .prerequisite("sololeveling:skill_unlock")
                .objective("mana", QuestObjectiveType.MANA_SPENT, "any", 250, "Spend 250 mana")
                .reward(QuestReward.builder().xp(500).gold(250).build()).build());

        register(QuestDefinition.builder("sololeveling:repeat_use_abilities", "Ability Practice")
                .description("Use unlocked abilities in combat or training.")
                .category(QuestCategory.REPEATABLE).repeatable()
                .prerequisite("sololeveling:skill_unlock")
                .objective("abilities", QuestObjectiveType.ABILITY_USE, "any", 10, "Use abilities 10 times")
                .reward(QuestReward.builder().xp(550).gold(300).build()).build());

        register(QuestDefinition.builder("sololeveling:repeat_clear_dungeon", "Gate Closure")
                .description("Clear a dungeon for a repeatable System contract.")
                .category(QuestCategory.REPEATABLE).repeatable()
                .prerequisite("sololeveling:first_dungeon")
                .objective("clear", QuestObjectiveType.DUNGEON_CLEAR, "any", 1, "Clear a dungeon")
                .reward(QuestReward.builder().xp(900).gold(700).build()).build());

        register(QuestDefinition.builder("sololeveling:repeat_summon_shadows", "Shadow Muster")
                .description("Summon shadows to demonstrate command readiness.")
                .category(QuestCategory.REPEATABLE).repeatable()
                .prerequisite("sololeveling:shadow_system_introduction")
                .objective("summons", QuestObjectiveType.SHADOW_SUMMON, "any", 3, "Summon 3 shadows")
                .reward(QuestReward.builder().xp(750).gold(500).build()).build());
    }

    private QuestRegistry() { }
}
