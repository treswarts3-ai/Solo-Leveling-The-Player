package com.tre.sololeveling.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestTypes {
    public enum Category { STORY, TUTORIAL, DAILY, REPEATABLE }
    public enum Status { LOCKED, AVAILABLE, ACTIVE, COMPLETED, FAILED }
    public enum ObjectiveType {
        KILL,
        COLLECTION,
        ABILITY_USE,
        MANA_SPENT,
        STAT_ALLOCATION,
        DUNGEON_CLEAR,
        SHADOW_SUMMON,
        SKILL_UNLOCK,
        SYSTEM_OPEN,
        EXERCISE
    }

    public record Objective(String id, ObjectiveType type, String target, int required) {
        public Objective {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Objective id cannot be blank");
            if (type == null) throw new IllegalArgumentException("Objective type cannot be null");
            target = target == null || target.isBlank() ? "any" : target.toLowerCase();
            required = Math.max(1, required);
        }

        public boolean matches(String value) {
            if (target.equals("any")) return true;
            if (value == null) return false;
            String normalized = value.toLowerCase();
            return normalized.equals(target) || normalized.endsWith(":" + target);
        }
    }

    public record Reward(int xp, int gold, int statPoints, Map<ResourceLocation, Integer> items, Set<String> skills) {
        public Reward {
            xp = Math.max(0, xp);
            gold = Math.max(0, gold);
            statPoints = Math.max(0, statPoints);
            items = items == null ? Map.of() : Map.copyOf(items);
            skills = skills == null ? Set.of() : Set.copyOf(skills);
        }

        public static Reward of(int xp, int gold, int statPoints) {
            return new Reward(xp, gold, statPoints, Map.of(), Set.of());
        }
    }

    public record Penalty(int healthDamage, int goldLoss, boolean systemPenalty) {
        public Penalty {
            healthDamage = Math.max(0, healthDamage);
            goldLoss = Math.max(0, goldLoss);
        }

        public static Penalty none() { return new Penalty(0, 0, false); }
    }

    public record Definition(
            ResourceLocation id,
            String displayName,
            String description,
            Category category,
            List<ResourceLocation> prerequisites,
            List<Objective> objectives,
            Reward reward,
            Penalty penalty,
            boolean repeatable,
            boolean daily,
            boolean autoStart
    ) {
        public Definition {
            if (id == null) throw new IllegalArgumentException("Quest id cannot be null");
            if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Quest name cannot be blank");
            description = description == null ? "" : description;
            category = category == null ? Category.STORY : category;
            prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
            objectives = objectives == null ? List.of() : List.copyOf(objectives);
            if (objectives.isEmpty()) throw new IllegalArgumentException("Quest must have at least one objective");
            reward = reward == null ? Reward.of(0, 0, 0) : reward;
            penalty = penalty == null ? Penalty.none() : penalty;
        }
    }

    public record Snapshot(
            ResourceLocation id,
            String displayName,
            String description,
            Category category,
            Status status,
            List<ObjectiveProgress> objectives,
            int completionCount,
            boolean repeatable,
            boolean daily
    ) {}

    public record ObjectiveProgress(String id, ObjectiveType type, String target, int progress, int required) {}

    public record Notification(ResourceLocation questId, String event, String message) {}

    public record Result(boolean success, String message) {
        public static Result ok(String message) { return new Result(true, message); }
        public static Result fail(String message) { return new Result(false, message); }
    }

    private QuestTypes() {}
}
