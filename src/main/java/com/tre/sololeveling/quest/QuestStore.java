package com.tre.sololeveling.quest;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.ZoneOffset;

final class QuestStore {
    private static final String ROOT = "quest_framework";
    private static final String STATES = "states";
    private static final String TRACKERS = "trackers";
    private static final String UI = "ui";

    static CompoundTag root(ServerPlayer player) {
        CompoundTag hunter = HunterData.mutable(player);
        if (!hunter.contains(ROOT, Tag.TAG_COMPOUND)) hunter.put(ROOT, new CompoundTag());
        return hunter.getCompound(ROOT);
    }

    static CompoundTag state(ServerPlayer player, QuestTypes.Definition definition) {
        CompoundTag root = root(player);
        if (!root.contains(STATES, Tag.TAG_COMPOUND)) root.put(STATES, new CompoundTag());
        CompoundTag states = root.getCompound(STATES);
        String key = definition.id().toString();
        if (!states.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag state = new CompoundTag();
            state.putString("status", QuestTypes.Status.LOCKED.name());
            state.put("progress", new CompoundTag());
            state.putInt("completion_count", 0);
            state.putBoolean("reward_claimed", false);
            state.putString("daily_date", today());
            states.put(key, state);
        }
        return states.getCompound(key);
    }

    static QuestTypes.Status status(ServerPlayer player, QuestTypes.Definition definition) {
        String value = state(player, definition).getString("status");
        try { return QuestTypes.Status.valueOf(value); }
        catch (IllegalArgumentException ignored) { return QuestTypes.Status.LOCKED; }
    }

    static void setStatus(ServerPlayer player, QuestTypes.Definition definition, QuestTypes.Status status) {
        state(player, definition).putString("status", status.name());
    }

    static int progress(ServerPlayer player, QuestTypes.Definition definition, QuestTypes.Objective objective) {
        return Math.max(0, state(player, definition).getCompound("progress").getInt(objective.id()));
    }

    static void setProgress(ServerPlayer player, QuestTypes.Definition definition, QuestTypes.Objective objective, int value) {
        CompoundTag state = state(player, definition);
        CompoundTag progress = state.getCompound("progress");
        progress.putInt(objective.id(), Math.max(0, Math.min(objective.required(), value)));
        state.put("progress", progress);
    }

    static void clearProgress(ServerPlayer player, QuestTypes.Definition definition) {
        state(player, definition).put("progress", new CompoundTag());
    }

    static int completionCount(ServerPlayer player, QuestTypes.Definition definition) {
        return Math.max(0, state(player, definition).getInt("completion_count"));
    }

    static void incrementCompletion(ServerPlayer player, QuestTypes.Definition definition) {
        CompoundTag state = state(player, definition);
        state.putInt("completion_count", completionCount(player, definition) + 1);
    }

    static boolean rewardClaimed(ServerPlayer player, QuestTypes.Definition definition) {
        return state(player, definition).getBoolean("reward_claimed");
    }

    static void setRewardClaimed(ServerPlayer player, QuestTypes.Definition definition, boolean claimed) {
        state(player, definition).putBoolean("reward_claimed", claimed);
    }

    static String dailyDate(ServerPlayer player, QuestTypes.Definition definition) {
        return state(player, definition).getString("daily_date");
    }

    static void setDailyDate(ServerPlayer player, QuestTypes.Definition definition, String date) {
        state(player, definition).putString("daily_date", date);
    }

    static int tracker(ServerPlayer player, String key) {
        CompoundTag root = root(player);
        if (!root.contains(TRACKERS, Tag.TAG_COMPOUND)) root.put(TRACKERS, new CompoundTag());
        return root.getCompound(TRACKERS).getInt(key);
    }

    static void setTracker(ServerPlayer player, String key, int value) {
        CompoundTag root = root(player);
        if (!root.contains(TRACKERS, Tag.TAG_COMPOUND)) root.put(TRACKERS, new CompoundTag());
        CompoundTag trackers = root.getCompound(TRACKERS);
        trackers.putInt(key, value);
        root.put(TRACKERS, trackers);
    }

    static void writeUi(ServerPlayer player, Iterable<QuestTypes.Snapshot> snapshots) {
        ListTag quests = new ListTag();
        for (QuestTypes.Snapshot snapshot : snapshots) {
            CompoundTag quest = new CompoundTag();
            quest.putString("id", snapshot.id().toString());
            quest.putString("name", snapshot.displayName());
            quest.putString("description", snapshot.description());
            quest.putString("category", snapshot.category().name());
            quest.putString("status", snapshot.status().name());
            quest.putInt("completion_count", snapshot.completionCount());
            quest.putBoolean("repeatable", snapshot.repeatable());
            quest.putBoolean("daily", snapshot.daily());
            ListTag objectives = new ListTag();
            for (QuestTypes.ObjectiveProgress progress : snapshot.objectives()) {
                CompoundTag objective = new CompoundTag();
                objective.putString("id", progress.id());
                objective.putString("type", progress.type().name());
                objective.putString("target", progress.target());
                objective.putInt("progress", progress.progress());
                objective.putInt("required", progress.required());
                objectives.add(objective);
            }
            quest.put("objectives", objectives);
            quests.add(quest);
        }
        root(player).put(UI, quests);
        HunterData.sync(player);
    }

    static void clear(ServerPlayer player) {
        HunterData.mutable(player).remove(ROOT);
    }

    static String today() { return LocalDate.now(ZoneOffset.UTC).toString(); }

    private QuestStore() {}
}
