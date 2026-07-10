package com.tre.sololeveling.gameplay.ability;

import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative ability implementation contract. */
public interface Ability {
    AbilityDefinition definition();

    AbilityResult activate(AbilityContext context);

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
}
