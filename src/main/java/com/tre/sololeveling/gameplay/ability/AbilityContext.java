package com.tre.sololeveling.gameplay.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Trusted server context passed to ability implementations. */
public record AbilityContext(ServerPlayer player, ServerLevel level, AbilityDefinition definition) {
    public AbilityContext(ServerPlayer player, AbilityDefinition definition) {
        this(player, player.serverLevel(), definition);
    }
}
