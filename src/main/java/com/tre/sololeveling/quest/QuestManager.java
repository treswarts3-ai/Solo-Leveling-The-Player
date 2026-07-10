package com.tre.sololeveling.quest;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class QuestManager {
    public static void onLogin(ServerPlayer player) {
        initializeTrackers(player);
        resetDailyIfNeeded(player);
        refreshAvailability(player);
        autoStartEligible(player);
        sync(player);
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        boolean changed = resetDailyIfNeeded(player);
        changed |= inferManaSpending(player);
        changed |= inferStatAllocation(player);
        changed |= inferShadowSummons(player);
        changed |= inferExercise(player);
        changed |= inferSystemOpen(player);
        changed |= inferCollections(player);
        if (changed) sync(player);
    }

    public static QuestTypes.Result start(ServerPlayer player, ResourceLocation id) {
        QuestTypes.Definition definition = QuestRegistry.find(id).orElse(null);
        if (definition == null) return QuestTypes.Result.fail("Unknown quest: " + id);
        if (!prerequisitesMet(player, definition)) return QuestTypes.Result.fail("Quest prerequisites are not complete");
        QuestTypes.Status status = QuestStore.status(player, definition);
        if (status == QuestTypes.Status.ACTIVE) return QuestTypes.Result.fail("Quest is already active");
        if (status == QuestTypes.Status.COMPLETED && !definition.repeatable()) return QuestTypes.Result.fail("Quest is already complete");
        if (status == QuestTypes.Status.COMPLETED && definition.daily() && QuestStore.dailyDate(player, definition).equals(QuestStore.today())) {
            return QuestTypes.Result.fail("Daily quest is already complete for today");
        }
        QuestStore.clearProgress(player, definition);
        QuestStore.setRewardClaimed(player, definition, false);
        QuestStore.setDailyDate(player, definition, QuestStore.today());
        QuestStore.setStatus(player, definition, QuestTypes.Status.ACTIVE);
        notify(player, definition, "started", "[QUEST STARTED] " + definition.displayName());
        sync(player);
        return QuestTypes.Result.ok("Started " + definition.displayName());
    }

    public static QuestTypes.Result fail(ServerPlayer player, ResourceLocation id, String reason) {
        QuestTypes.Definition definition = QuestRegistry.find(id).orElse(null);
        if (definition == null) return QuestTypes.Result.fail("Unknown quest: " + id);
        if (QuestStore.status(player, definition) != QuestTypes.Status.ACTIVE) return QuestTypes.Result.fail("Quest is not active");
        failInternal(player, definition, reason == null || reason.isBlank() ? "Quest failed" : reason);
        sync(player);
        return QuestTypes.Result.ok("Failed " + definition.displayName());
    }

    public static QuestTypes.Result reset(ServerPlayer player, ResourceLocation id) {
        QuestTypes.Definition definition = QuestRegistry.find(id).orElse(null);
        if (definition == null) return QuestTypes.Result.fail("Unknown quest: " + id);
        resetState(player, definition);
        refreshAvailability(player);
        autoStartEligible(player);
        notify(player, definition, "reset", "[QUEST RESET] " + definition.displayName());
        sync(player);
        return QuestTypes.Result.ok("Reset " + definition.displayName());
    }

    public static QuestTypes.Result forceComplete(ServerPlayer player, ResourceLocation id) {
        QuestTypes.Definition definition = QuestRegistry.find(id).orElse(null);
        if (definition == null) return QuestTypes.Result.fail("Unknown quest: " + id);
        if (QuestStore.status(player, definition) != QuestTypes.Status.ACTIVE) {
            QuestTypes.Result started = start(player, id);
            if (!started.success()) return started;
        }
        for (QuestTypes.Objective objective : definition.objectives()) QuestStore.setProgress(player, definition, objective, objective.required());
        completeInternal(player, definition);
        sync(player);
        return QuestTypes.Result.ok("Completed " + definition.displayName());
    }

    public static QuestTypes.Result resetDaily(ServerPlayer player) {
        for (QuestTypes.Definition definition : QuestRegistry.all()) {
            if (!definition.daily()) continue;
            resetState(player, definition);
            QuestStore.setDailyDate(player, definition, QuestStore.today());
        }
        refreshAvailability(player);
        autoStartEligible(player);
        sync(player);
        return QuestTypes.Result.ok("Daily quests reset");
    }

    public static QuestTypes.Result clear(ServerPlayer player) {
        QuestStore.clear(player);
        initializeTrackers(player);
        refreshAvailability(player);
        autoStartEligible(player);
        sync(player);
        return QuestTypes.Result.ok("Quest data cleared");
    }

    public static boolean advance(ServerPlayer player, QuestTypes.ObjectiveType type, String target, int amount) {
        if (amount <= 0) return false;
        boolean changed = false;
        List<QuestTypes.Definition> completed = new ArrayList<>();
        for (QuestTypes.Definition definition : QuestRegistry.all()) {
            if (QuestStore.status(player, definition) != QuestTypes.Status.ACTIVE) continue;
            boolean questChanged = false;
            for (QuestTypes.Objective objective : definition.objectives()) {
                if (objective.type() != type || !objective.matches(target)) continue;
                int old = QuestStore.progress(player, definition, objective);
                int value = Math.min(objective.required(), old + amount);
                if (value == old) continue;
                QuestStore.setProgress(player, definition, objective, value);
                questChanged = true;
                notify(player, definition, "progress", definition.displayName() + ": " + objective.id() + " " + value + "/" + objective.required());
            }
            if (questChanged) {
                changed = true;
                if (objectivesComplete(player, definition)) completed.add(definition);
            }
        }
        for (QuestTypes.Definition definition : completed) completeInternal(player, definition);
        if (changed) sync(player);
        return changed;
    }

    public static List<QuestTypes.Snapshot> snapshots(ServerPlayer player) {
        List<QuestTypes.Snapshot> result = new ArrayList<>();
        for (QuestTypes.Definition definition : QuestRegistry.all()) {
            List<QuestTypes.ObjectiveProgress> objectives = new ArrayList<>();
            for (QuestTypes.Objective objective : definition.objectives()) {
                objectives.add(new QuestTypes.ObjectiveProgress(objective.id(), objective.type(), objective.target(),
                        QuestStore.progress(player, definition, objective), objective.required()));
            }
            result.add(new QuestTypes.Snapshot(definition.id(), definition.displayName(), definition.description(),
                    definition.category(), QuestStore.status(player, definition), List.copyOf(objectives),
                    QuestStore.completionCount(player, definition), definition.repeatable(), definition.daily()));
        }
        return List.copyOf(result);
    }

    public static QuestTypes.Snapshot snapshot(ServerPlayer player, ResourceLocation id) {
        for (QuestTypes.Snapshot snapshot : snapshots(player)) if (snapshot.id().equals(id)) return snapshot;
        return null;
    }

    public static String inspect(ServerPlayer player, ResourceLocation id) {
        QuestTypes.Snapshot snapshot = snapshot(player, id);
        if (snapshot == null) return "Unknown quest: " + id;
        StringBuilder builder = new StringBuilder(snapshot.displayName()).append(" [").append(snapshot.status()).append("]");
        for (QuestTypes.ObjectiveProgress objective : snapshot.objectives()) {
            builder.append(" | ").append(objective.id()).append('=').append(objective.progress()).append('/').append(objective.required());
        }
        return builder.toString();
    }

    private static void completeInternal(ServerPlayer player, QuestTypes.Definition definition) {
        if (QuestStore.status(player, definition) != QuestTypes.Status.ACTIVE || QuestStore.rewardClaimed(player, definition)) return;
        QuestStore.setRewardClaimed(player, definition, true);
        QuestStore.setStatus(player, definition, QuestTypes.Status.COMPLETED);
        QuestStore.incrementCompletion(player, definition);
        grantReward(player, definition.reward());
        notify(player, definition, "completed", "[QUEST COMPLETE] " + definition.displayName());
        refreshAvailability(player);
        autoStartEligible(player);
    }

    private static void failInternal(ServerPlayer player, QuestTypes.Definition definition, String reason) {
        QuestStore.setStatus(player, definition, QuestTypes.Status.FAILED);
        applyPenalty(player, definition.penalty());
        notify(player, definition, "failed", "[QUEST FAILED] " + definition.displayName() + ": " + reason);
    }

    private static void grantReward(ServerPlayer player, QuestTypes.Reward reward) {
        if (reward.xp() > 0) HunterData.addXp(player, reward.xp());
        if (reward.gold() > 0) HunterData.addGold(player, reward.gold());
        if (reward.statPoints() > 0) HunterData.addStatPoints(player, reward.statPoints());
        for (String skill : reward.skills()) {
            if (HunterData.unlockSkill(player, skill)) QuestApi.onSkillUnlocked(player, skill);
        }
        for (var entry : reward.items().entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (item == null) continue;
            ItemStack stack = new ItemStack(item, Math.max(1, entry.getValue()));
            if (!HunterData.storeSystemItem(player, stack.copy())) {
                ItemStack remaining = stack.copy();
                player.getInventory().add(remaining);
                if (!remaining.isEmpty()) player.drop(remaining, false);
            }
        }
        HunterData.sync(player);
    }

    private static void applyPenalty(ServerPlayer player, QuestTypes.Penalty penalty) {
        if (penalty.healthDamage() > 0) player.setHealth(Math.max(1.0F, player.getHealth() - penalty.healthDamage()));
        if (penalty.goldLoss() > 0) HunterData.addGold(player, -penalty.goldLoss());
        if (penalty.systemPenalty()) HunterData.mutable(player).putBoolean("penalty_pending", true);
        HunterData.sync(player);
    }

    private static boolean objectivesComplete(ServerPlayer player, QuestTypes.Definition definition) {
        for (QuestTypes.Objective objective : definition.objectives()) {
            if (QuestStore.progress(player, definition, objective) < objective.required()) return false;
        }
        return true;
    }

    private static boolean prerequisitesMet(ServerPlayer player, QuestTypes.Definition definition) {
        for (ResourceLocation prerequisite : definition.prerequisites()) {
            QuestTypes.Definition required = QuestRegistry.find(prerequisite).orElse(null);
            if (required == null || QuestStore.completionCount(player, required) <= 0) return false;
        }
        return true;
    }

    private static void refreshAvailability(ServerPlayer player) {
        for (QuestTypes.Definition definition : QuestRegistry.all()) {
            QuestTypes.Status status = QuestStore.status(player, definition);
            if (status == QuestTypes.Status.ACTIVE || status == QuestTypes.Status.COMPLETED) continue;
            QuestStore.setStatus(player, definition, prerequisitesMet(player, definition) ? QuestTypes.Status.AVAILABLE : QuestTypes.Status.LOCKED);
        }
    }

    private static void autoStartEligible(ServerPlayer player) {
        boolean started;
        do {
            started = false;
            for (QuestTypes.Definition definition : QuestRegistry.all()) {
                if (!definition.autoStart() || !prerequisitesMet(player, definition)) continue;
                QuestTypes.Status status = QuestStore.status(player, definition);
                if (status != QuestTypes.Status.AVAILABLE) continue;
                QuestStore.clearProgress(player, definition);
                QuestStore.setRewardClaimed(player, definition, false);
                QuestStore.setDailyDate(player, definition, QuestStore.today());
                QuestStore.setStatus(player, definition, QuestTypes.Status.ACTIVE);
                notify(player, definition, "started", "[QUEST STARTED] " + definition.displayName());
                started = true;
            }
        } while (started);
    }

    private static void resetState(ServerPlayer player, QuestTypes.Definition definition) {
        QuestStore.clearProgress(player, definition);
        QuestStore.setRewardClaimed(player, definition, false);
        QuestStore.setStatus(player, definition, prerequisitesMet(player, definition) ? QuestTypes.Status.AVAILABLE : QuestTypes.Status.LOCKED);
    }

    private static boolean resetDailyIfNeeded(ServerPlayer player) {
        boolean changed = false;
        String today = QuestStore.today();
        for (QuestTypes.Definition definition : QuestRegistry.all()) {
            if (!definition.daily() || today.equals(QuestStore.dailyDate(player, definition))) continue;
            if (QuestStore.status(player, definition) == QuestTypes.Status.ACTIVE && !objectivesComplete(player, definition)) {
                applyPenalty(player, definition.penalty());
                notify(player, definition, "failed", "[DAILY QUEST FAILED] " + definition.displayName());
            }
            resetState(player, definition);
            QuestStore.setDailyDate(player, definition, today);
            notify(player, definition, "daily_reset", "[DAILY RESET] " + definition.displayName());
            changed = true;
        }
        if (changed) {
            refreshAvailability(player);
            autoStartEligible(player);
        }
        return changed;
    }

    private static boolean inferManaSpending(ServerPlayer player) {
        int current = HunterData.getMana(player);
        int previous = QuestStore.tracker(player, "mana");
        QuestStore.setTracker(player, "mana", current);
        return previous > current && advance(player, QuestTypes.ObjectiveType.MANA_SPENT, "any", previous - current);
    }

    private static boolean inferStatAllocation(ServerPlayer player) {
        int total = statTotal(player);
        int points = HunterData.getStatPoints(player);
        int previousTotal = QuestStore.tracker(player, "stats");
        int previousPoints = QuestStore.tracker(player, "stat_points");
        QuestStore.setTracker(player, "stats", total);
        QuestStore.setTracker(player, "stat_points", points);
        if (previousTotal <= 0 || total <= previousTotal || previousPoints <= points) return false;
        return advance(player, QuestTypes.ObjectiveType.STAT_ALLOCATION, "any", Math.min(total - previousTotal, previousPoints - points));
    }

    private static boolean inferShadowSummons(ServerPlayer player) {
        int current = HunterData.activeShadows(player).size();
        int previous = QuestStore.tracker(player, "active_shadows");
        QuestStore.setTracker(player, "active_shadows", current);
        return current > previous && advance(player, QuestTypes.ObjectiveType.SHADOW_SUMMON, "any", current - previous);
    }

    private static boolean inferExercise(ServerPlayer player) {
        boolean changed = false;
        changed |= inferCounter(player, "exercise_pushup", HunterData.mutable(player).getInt("daily_pushups"), QuestTypes.ObjectiveType.EXERCISE, "pushup");
        changed |= inferCounter(player, "exercise_situp", HunterData.mutable(player).getInt("daily_situps"), QuestTypes.ObjectiveType.EXERCISE, "situp");
        changed |= inferCounter(player, "exercise_squat", HunterData.mutable(player).getInt("daily_squats"), QuestTypes.ObjectiveType.EXERCISE, "squat");
        return changed;
    }

    private static boolean inferSystemOpen(ServerPlayer player) {
        int current = HunterData.mutable(player).getBoolean("tutorial_system_opened") ? 1 : 0;
        int previous = QuestStore.tracker(player, "system_open");
        QuestStore.setTracker(player, "system_open", current);
        return current > previous && advance(player, QuestTypes.ObjectiveType.SYSTEM_OPEN, "system", 1);
    }

    private static boolean inferCollections(ServerPlayer player) {
        boolean changed = false;
        for (QuestTypes.Definition definition : QuestRegistry.all()) {
            if (QuestStore.status(player, definition) != QuestTypes.Status.ACTIVE) continue;
            for (QuestTypes.Objective objective : definition.objectives()) {
                if (objective.type() != QuestTypes.ObjectiveType.COLLECTION) continue;
                ResourceLocation id = ResourceLocation.tryParse(objective.target());
                Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
                if (item == null) continue;
                int count = countItem(player, item);
                int old = QuestStore.progress(player, definition, objective);
                if (count <= old) continue;
                QuestStore.setProgress(player, definition, objective, Math.min(count, objective.required()));
                changed = true;
                notify(player, definition, "progress", definition.displayName() + ": " + objective.id() + " " + Math.min(count, objective.required()) + "/" + objective.required());
                if (objectivesComplete(player, definition)) completeInternal(player, definition);
            }
        }
        return changed;
    }

    private static boolean inferCounter(ServerPlayer player, String key, int current, QuestTypes.ObjectiveType type, String target) {
        int previous = QuestStore.tracker(player, key);
        QuestStore.setTracker(player, key, current);
        return current > previous && advance(player, type, target, current - previous);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) count += stack.getCount();
        for (ItemStack stack : player.getInventory().offhand) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    private static int statTotal(ServerPlayer player) {
        return HunterData.getStat(player, "strength") + HunterData.getStat(player, "agility")
                + HunterData.getStat(player, "stamina") + HunterData.getStat(player, "intelligence")
                + HunterData.getStat(player, "sense");
    }

    private static void initializeTrackers(ServerPlayer player) {
        QuestStore.setTracker(player, "mana", HunterData.getMana(player));
        QuestStore.setTracker(player, "stats", statTotal(player));
        QuestStore.setTracker(player, "stat_points", HunterData.getStatPoints(player));
        QuestStore.setTracker(player, "active_shadows", HunterData.activeShadows(player).size());
        QuestStore.setTracker(player, "exercise_pushup", HunterData.mutable(player).getInt("daily_pushups"));
        QuestStore.setTracker(player, "exercise_situp", HunterData.mutable(player).getInt("daily_situps"));
        QuestStore.setTracker(player, "exercise_squat", HunterData.mutable(player).getInt("daily_squats"));
        QuestStore.setTracker(player, "system_open", HunterData.mutable(player).getBoolean("tutorial_system_opened") ? 1 : 0);
    }

    private static void sync(ServerPlayer player) {
        QuestStore.writeUi(player, snapshots(player));
    }

    private static void notify(ServerPlayer player, QuestTypes.Definition definition, String event, String message) {
        ChatFormatting color = event.equals("failed") ? ChatFormatting.RED : event.equals("completed") ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        player.sendSystemMessage(Component.literal(message).withStyle(color));
        QuestApi.fire(player, new QuestTypes.Notification(definition.id(), event, message));
    }

    private QuestManager() {}
}
