package com.tre.sololeveling.quest;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface QuestPenalty {
    QuestPenalty NONE = (player, reason) -> { };

    void apply(ServerPlayer player, String reason);
}
