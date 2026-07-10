package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side unlock policy for an ability. A skill flag can be required, or
 * it can be combined with a minimum-level fallback for ordinary progression.
 */
public record AbilityUnlock(String skillId, int minimumLevel, boolean skillRequired) {
    public AbilityUnlock {
        skillId = skillId == null ? "" : skillId.trim().toLowerCase(java.util.Locale.ROOT);
        minimumLevel = Math.max(1, minimumLevel);
    }

    public boolean isUnlocked(ServerPlayer player) {
        if (!HunterData.isAwakened(player)) return false;
        boolean hasSkill = !skillId.isBlank() && HunterData.hasSkill(player, skillId);
        if (skillRequired) return hasSkill;
        return hasSkill || HunterData.getLevel(player) >= minimumLevel;
    }

    public String description() {
        if (skillRequired) return "Requires skill: " + skillId;
        if (skillId.isBlank()) return "Awakened, level " + minimumLevel;
        return "Skill " + skillId + " or level " + minimumLevel;
    }

    public static AbilityUnlock awakened() {
        return new AbilityUnlock("", 1, false);
    }

    public static AbilityUnlock skill(String skillId) {
        return new AbilityUnlock(skillId, Integer.MAX_VALUE, true);
    }

    public static AbilityUnlock skillOrLevel(String skillId, int minimumLevel) {
        return new AbilityUnlock(skillId, minimumLevel, false);
    }
}
