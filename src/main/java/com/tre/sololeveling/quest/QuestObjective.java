package com.tre.sololeveling.quest;

import java.util.Locale;
import java.util.Objects;

public record QuestObjective(String id, QuestObjectiveType type, String target, int required, String description) {
    public QuestObjective {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        id = normalize(id);
        target = target == null || target.isBlank() ? "any" : target.toLowerCase(Locale.ROOT);
        required = Math.max(1, required);
        description = description == null || description.isBlank() ? id : description;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
