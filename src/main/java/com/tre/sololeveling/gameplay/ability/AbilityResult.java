package com.tre.sololeveling.gameplay.ability;

/** Result returned by an ability after all shared server validation succeeds. */
public record AbilityResult(boolean success, String feedback) {
    public AbilityResult {
        feedback = feedback == null ? "" : feedback;
    }

    public static AbilityResult success(String feedback) {
        return new AbilityResult(true, feedback);
    }

    public static AbilityResult failure(String feedback) {
        return new AbilityResult(false, feedback);
    }
}
