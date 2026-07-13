package com.tre.sololeveling.gameplay.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Server-authoritative ability implementation contract. */
public interface Ability {
    AbilityDefinition definition();

    AbilityResult activate(AbilityContext context);

    /** Optional server-side preview used for target indicators and resolve-time revalidation. */
    default Entity selectTarget(ServerPlayer player) { return null; }

    default boolean requiresTarget(ServerPlayer player) { return false; }

    /** Side-effect-free validation before mana is reserved and startup begins. */
    default AbilityResult validateStart(AbilityContext context) { return AbilityResult.success(""); }

    default int manaCost(ServerPlayer player) {
        return definition().manaCost();
    }

    default int cooldownTicks(ServerPlayer player) {
        return definition().cooldownTicks();
    }

    default void tick(ServerPlayer player) {
    }

    default void cancel(ServerPlayer player) {
    }

    default void onInterrupted(ServerPlayer player, String reason) {
    }
}
