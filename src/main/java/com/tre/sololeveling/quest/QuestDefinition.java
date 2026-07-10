package com.tre.sololeveling.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class QuestDefinition {
    private final ResourceLocation id;
    private final String displayName;
    private final String description;
    private final QuestCategory category;
    private final List<ResourceLocation> prerequisites;
    private final List<QuestObjective> objectives;
    private final QuestReward reward;
    private final QuestPenalty penalty;
    private final boolean repeatable;
    private final boolean daily;
    private final int storyOrder;

    private QuestDefinition(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName");
        this.description = builder.description == null ? "" : builder.description;
        this.category = Objects.requireNonNull(builder.category, "category");
        this.prerequisites = List.copyOf(builder.prerequisites);
        this.objectives = List.copyOf(builder.objectives);
        if (objectives.isEmpty()) throw new IllegalArgumentException("Quest must have at least one objective: " + id);
        this.reward = builder.reward == null ? QuestReward.NONE : builder.reward;
        this.penalty = builder.penalty == null ? QuestPenalty.NONE : builder.penalty;
        this.repeatable = builder.repeatable;
        this.daily = builder.daily;
        this.storyOrder = builder.storyOrder;
    }

    public ResourceLocation id() { return id; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public QuestCategory category() { return category; }
    public List<ResourceLocation> prerequisites() { return prerequisites; }
    public List<QuestObjective> objectives() { return objectives; }
    public QuestReward reward() { return reward; }
    public QuestPenalty penalty() { return penalty; }
    public boolean repeatable() { return repeatable; }
    public boolean daily() { return daily; }
    public int storyOrder() { return storyOrder; }

    public static Builder builder(String id, String displayName) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) throw new IllegalArgumentException("Invalid quest id: " + id);
        return new Builder(parsed, displayName);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final String displayName;
        private String description = "";
        private QuestCategory category = QuestCategory.REPEATABLE;
        private final List<ResourceLocation> prerequisites = new ArrayList<>();
        private final List<QuestObjective> objectives = new ArrayList<>();
        private QuestReward reward = QuestReward.NONE;
        private QuestPenalty penalty = QuestPenalty.NONE;
        private boolean repeatable;
        private boolean daily;
        private int storyOrder = -1;

        private Builder(ResourceLocation id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder description(String value) { description = value; return this; }
        public Builder category(QuestCategory value) { category = value; return this; }
        public Builder prerequisite(String value) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null) prerequisites.add(parsed);
            return this;
        }
        public Builder objective(String id, QuestObjectiveType type, String target, int required, String description) {
            objectives.add(new QuestObjective(id, type, target, required, description));
            return this;
        }
        public Builder reward(QuestReward value) { reward = value; return this; }
        public Builder penalty(QuestPenalty value) { penalty = value; return this; }
        public Builder repeatable() { repeatable = true; return this; }
        public Builder daily() { daily = true; repeatable = true; return this; }
        public Builder storyOrder(int value) { storyOrder = Math.max(0, value); return this; }
        public QuestDefinition build() { return new QuestDefinition(this); }
    }
}
