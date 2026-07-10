package com.tre.sololeveling.quest;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface QuestNotificationListener {
    void onQuestNotification(ServerPlayer player, String event, QuestDefinition definition, String detail);
}
