package com.tre.sololeveling.gameplay.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Trusted server context passed to ability implementations. */
public record AbilityContext(ServerPlayer player, ServerLevel level, AbilityDefinition definition,
                             Entity preparedTarget, Vec3 castOrigin, long castStartedAt) {
    public AbilityContext(ServerPlayer player, AbilityDefinition definition) {
        this(player, player.serverLevel(), definition, null, player.position(), player.level().getGameTime());
    }

    public LivingEntity livingTarget() {
        return preparedTarget instanceof LivingEntity living ? living : null;
    }
}
