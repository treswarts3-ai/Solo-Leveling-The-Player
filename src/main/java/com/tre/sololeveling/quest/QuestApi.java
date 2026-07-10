package com.tre.sololeveling.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class QuestApi {
    private static final List<BiConsumer<ServerPlayer, QuestTypes.Notification>> LISTENERS = new CopyOnWriteArrayList<>();

    public static QuestTypes.Result start(ServerPlayer player, String questId) { return QuestManager.start(player, QuestRegistry.parse(questId)); }
    public static QuestTypes.Result complete(ServerPlayer player, String questId) { return QuestManager.forceComplete(player, QuestRegistry.parse(questId)); }
    public static QuestTypes.Result fail(ServerPlayer player, String questId, String reason) { return QuestManager.fail(player, QuestRegistry.parse(questId), reason); }
    public static QuestTypes.Result reset(ServerPlayer player, String questId) { return QuestManager.reset(player, QuestRegistry.parse(questId)); }
    public static QuestTypes.Result resetDaily(ServerPlayer player) { return QuestManager.resetDaily(player); }
    public static QuestTypes.Result clear(ServerPlayer player) { return QuestManager.clear(player); }
    public static List<QuestTypes.Snapshot> snapshots(ServerPlayer player) { return QuestManager.snapshots(player); }

    public static boolean onKill(ServerPlayer player, LivingEntity victim) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        return QuestManager.advance(player, QuestTypes.ObjectiveType.KILL, id == null ? "any" : id.toString(), 1);
    }

    public static boolean onCollected(ServerPlayer player, ResourceLocation itemId, int amount) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.COLLECTION, itemId == null ? "any" : itemId.toString(), amount);
    }

    public static boolean onAbilityUsed(ServerPlayer player, String abilityId) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.ABILITY_USE, abilityId, 1);
    }

    public static boolean onManaSpent(ServerPlayer player, int amount) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.MANA_SPENT, "any", amount);
    }

    public static boolean onStatAllocated(ServerPlayer player, String stat, int amount) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.STAT_ALLOCATION, stat, amount);
    }

    public static boolean onDungeonCleared(ServerPlayer player, String dungeonId) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.DUNGEON_CLEAR, dungeonId, 1);
    }

    public static boolean onShadowSummoned(ServerPlayer player, String shadowId, int amount) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.SHADOW_SUMMON, shadowId, amount);
    }

    public static boolean onSkillUnlocked(ServerPlayer player, String skillId) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.SKILL_UNLOCK, skillId, 1);
    }

    public static boolean onSystemOpened(ServerPlayer player) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.SYSTEM_OPEN, "system", 1);
    }

    public static boolean onExercise(ServerPlayer player, String exercise, int amount) {
        return QuestManager.advance(player, QuestTypes.ObjectiveType.EXERCISE, exercise, amount);
    }

    public static void registerListener(BiConsumer<ServerPlayer, QuestTypes.Notification> listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void unregisterListener(BiConsumer<ServerPlayer, QuestTypes.Notification> listener) {
        LISTENERS.remove(listener);
    }

    static void fire(ServerPlayer player, QuestTypes.Notification notification) {
        for (BiConsumer<ServerPlayer, QuestTypes.Notification> listener : LISTENERS) {
            try { listener.accept(player, notification); }
            catch (RuntimeException ignored) {}
        }
    }

    private QuestApi() {}
}
