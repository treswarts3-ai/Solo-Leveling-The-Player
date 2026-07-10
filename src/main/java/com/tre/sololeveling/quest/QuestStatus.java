package com.tre.sololeveling.quest;

public enum QuestStatus {
    NOT_STARTED,
    ACTIVE,
    COMPLETED,
    FAILED;

    public static QuestStatus parse(String value) {
        try {
            return value == null || value.isBlank() ? NOT_STARTED : valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return NOT_STARTED;
        }
    }
}
