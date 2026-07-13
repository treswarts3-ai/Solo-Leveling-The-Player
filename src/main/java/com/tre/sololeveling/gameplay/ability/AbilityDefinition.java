package com.tre.sololeveling.gameplay.ability;

import java.util.Locale;

/** Immutable metadata exposed to commands, networking, configuration, and HUD code. */
public record AbilityDefinition(
        String id,
        String displayName,
        String description,
        String category,
        AbilityUnlock unlock,
        int manaCost,
        int cooldownTicks,
        double maximumRange,
        AbilityScaling scaling,
        AbilityCastProfile castProfile
) {
    public AbilityDefinition(String id, String displayName, String description, String category,
                             AbilityUnlock unlock, int manaCost, int cooldownTicks, double maximumRange,
                             AbilityScaling scaling) {
        this(id, displayName, description, category, unlock, manaCost, cooldownTicks, maximumRange,
                scaling, AbilityCastProfile.INSTANT);
    }

    public AbilityDefinition {
        id = normalize(id);
        if (id.isBlank()) throw new IllegalArgumentException("Ability id cannot be blank");
        displayName = displayName == null || displayName.isBlank() ? id.replace('_', ' ') : displayName;
        description = description == null ? "" : description;
        category = category == null || category.isBlank() ? "utility" : category.toLowerCase(Locale.ROOT);
        unlock = unlock == null ? AbilityUnlock.awakened() : unlock;
        manaCost = Math.max(0, manaCost);
        cooldownTicks = Math.max(0, cooldownTicks);
        maximumRange = Math.max(0.0D, maximumRange);
        scaling = scaling == null ? AbilityScaling.NONE : scaling;
        castProfile = castProfile == null ? AbilityCastProfile.INSTANT : castProfile;
    }

    public double cooldownSeconds() {
        return cooldownTicks / 20.0D;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
