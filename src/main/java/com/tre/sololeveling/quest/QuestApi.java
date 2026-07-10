package com.tre.sololeveling.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Stable server-side integration surface for abilities, dungeons, shadows, progression, and UI systems. */
public final class QuestApi {
    private static final List<QuestNotificationListener> LISTENERS = new CopyOnWriteArrayList<>();

    public static boolean start(ServerPlayer player, String questId) { return QuestManager.start(player, questId); }
    public static boolean complete(ServerPlayer player, String questId) { return QuestManager.complete(player, questId, false); }
    public static boolean fail(ServerPlayer player, String questId, String reason) { return QuestManager.fail(player, questId, reason); }
    public static boolean reset(ServerPlayer player, String questId) { return QuestManager.reset(player, questId); }
    public static void resetDaily(ServerPlayer player) { QuestManager.resetDaily(player, true); }
    public static void clear(ServerPlayer player) { QuestManager.clear(player); }

    public static void onKill(ServerPlayer player, LivingEntity victim) { QuestManager.onKill(player, victim); }
    public static void onCollected(ServerPlayer player, ItemStack stack) { QuestManager.onCollected(player, stack); }
    public static void onAbilityUsed(ServerPlayer player, String abilityId) { QuestManager.onAbilityUsed(player, abilityId); }
    public static void onManaSpent(ServerPlayer player, int amount) { QuestManager.onManaSpent(player, amount); }
    public static void onStatAllocated(ServerPlayer player, String stat, int amount) { QuestManager.onStatAllocated(player, stat, amount); }
    public static void onDungeonCleared(ServerPlayer player, String dungeonId) { QuestManager.onDungeonCleared(player, dungeonId); }
    public static void onShadowSummoned(ServerPlayer player, String shadowId) { QuestManager.onShadowSummoned(player, shadowId); }
    public static void onSkillUnlocked(ServerPlayer player, String skillId) { QuestManager.onSkillUnlocked(player, skillId); }
    public static void onSystemOpened(ServerPlayer player) { QuestManager.onSystemOpened(player); }
    public static void onExercise(ServerPlayer player, String exercise, int amount) { QuestManager.onExercise(player, exercise, amount); }
    public static void onDistanceMoved(ServerPlayer player, int blocks) { QuestManager.onDistanceMoved(player, blocks); }

    public static void registerListener(QuestNotificationListener listener) { if (listener != null) LISTENERS.add(listener); }
    public static void unregisterListener(QuestNotificationListener listener) { LISTENERS.remove(listener); }

    static void fire(ServerPlayer player, String event, QuestDefinition definition, String detail) {
        for (QuestNotificationListener listener : LISTENERS) {
            try { listener.onQuestNotification(player, event, definition, detail); }
            catch (RuntimeException ignored) { }
        }
    }

    private QuestApi() { }
}
