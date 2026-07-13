package com.tre.sololeveling.gameplay.ability;

/**
 * Shared Phase 5 contract for readable timing, presentation, interruption, and progression.
 * Values are immutable metadata; the logical server remains responsible for every outcome.
 */
public record AbilityCastProfile(
        String role,
        String animationId,
        int startupTicks,
        int activeTicks,
        int recoveryTicks,
        int failureRecoveryTicks,
        boolean interruptOnDamage,
        boolean interruptOnAttack,
        double movementTolerance,
        String progressionPath
) {
    public static final AbilityCastProfile INSTANT = new AbilityCastProfile(
            "Utility", "ability.instant", 0, 1, 0, 0,
            false, false, -1.0D, "Stat scaling");

    public AbilityCastProfile {
        role = role == null || role.isBlank() ? "Utility" : role.trim();
        animationId = animationId == null || animationId.isBlank() ? "ability.instant" : animationId.trim();
        startupTicks = Math.max(0, Math.min(100, startupTicks));
        activeTicks = Math.max(1, Math.min(20 * 60, activeTicks));
        recoveryTicks = Math.max(0, Math.min(200, recoveryTicks));
        failureRecoveryTicks = Math.max(0, Math.min(100, failureRecoveryTicks));
        movementTolerance = movementTolerance < 0.0D ? -1.0D : Math.min(8.0D, movementTolerance);
        progressionPath = progressionPath == null || progressionPath.isBlank()
                ? "Mastery improves cost and control" : progressionPath.trim();
    }

    public boolean hasReadableTiming() {
        return startupTicks > 0 && activeTicks > 0 && recoveryTicks > 0;
    }
}
