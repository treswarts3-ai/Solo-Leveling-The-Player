package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.data.HunterRank;
import com.tre.sololeveling.dungeon.DungeonContent;
import com.tre.sololeveling.dungeon.DungeonEnemies;
import com.tre.sololeveling.dungeon.DungeonSession;
import com.tre.sololeveling.dungeon.DungeonTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Server-authoritative E-to-S evaluation ladder with a different test identity at each rank. */
public final class RankTrialService {
    private static final long RETRY_TICKS = 20L * 60L;

    private record Trial(HunterRank from, HunterRank to, int level, int seconds, String objective) {}

    private static final Trial[] TRIALS = {
            new Trial(HunterRank.E, HunterRank.D, 10, 300, "Defeat 10 hostile enemies without dying"),
            new Trial(HunterRank.D, HunterRank.C, 20, 1200, "Clear a dungeon before the evaluation timer expires"),
            new Trial(HunterRank.C, HunterRank.B, 30, 1200, "Defeat an elite and complete 3 dungeon objectives"),
            new Trial(HunterRank.B, HunterRank.A, 40, 1800, "Clear a solo raid with no more than 3 recovery events"),
            new Trial(HunterRank.A, HunterRank.S, 60, 1800, "Clear the boss evaluation with a performance score of 70")
    };

    public static void tick(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        Trial trial = trialFor(currentTier(tag));
        if (trial != null && HunterData.getLevel(player) >= trial.level() && !tag.getBoolean("rank_trial_notified_" + trial.to().name())) {
            tag.putBoolean("rank_trial_notified_" + trial.to().name(), true);
            player.sendSystemMessage(Component.literal("[SYSTEM] " + trial.from().displayName() + " to "
                    + trial.to().displayName() + " evaluation available. Use /slrank begin.")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            HunterData.sync(player);
        }
        if (tag.getBoolean("rank_trial_active") && player.level().getGameTime() >= tag.getLong("rank_trial_end")) {
            fail(player, "Evaluation timer expired");
        }
    }

    public static boolean begin(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!HunterData.isAwakened(player) || tag.getBoolean("rank_trial_active")) return false;
        Trial trial = trialFor(currentTier(tag));
        if (trial == null || HunterData.getLevel(player) < trial.level()) {
            player.sendSystemMessage(Component.literal("[SYSTEM] No rank evaluation is currently available.").withStyle(ChatFormatting.RED));
            return false;
        }
        long now = player.level().getGameTime();
        if (now < tag.getLong("rank_trial_retry_after")) return false;
        tag.putBoolean("rank_trial_active", true);
        tag.putInt("rank_trial_from", trial.from().tier());
        tag.putInt("rank_trial_target_tier", trial.to().tier());
        tag.putLong("rank_trial_started", now);
        tag.putLong("rank_trial_end", now + trial.seconds() * 20L);
        tag.putInt("rank_trial_kills", 0);
        tag.putInt("rank_trial_elites", 0);
        tag.putInt("rank_trial_objectives", 0);
        tag.putInt("rank_trial_recoveries", 0);
        tag.putInt("rank_trial_ability_success", 0);
        tag.putFloat("rank_trial_damage_taken", 0.0F);
        tag.putFloat("rank_trial_boss_damage", 0.0F);
        tag.putInt("rank_trial_attempts", tag.getInt("rank_trial_attempts") + 1);
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[RANK EVALUATION] " + trial.objective() + ".")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.7F, 1.15F);
        return true;
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        CompoundTag tag = HunterData.mutable(player);
        if (!active(tag) || !(victim instanceof Enemy) || victim.getPersistentData().getBoolean("sl_shadow")) return;
        int from = tag.getInt("rank_trial_from");
        if (from == HunterRank.E.tier()) {
            int kills = tag.getInt("rank_trial_kills") + 1;
            tag.putInt("rank_trial_kills", kills);
            if (kills >= 10) complete(player, HunterRank.D);
            else progress(player, kills + " / 10 hostiles");
        } else if (from == HunterRank.C.tier() && DungeonEnemies.isDungeonEnemy(victim)) {
            DungeonTypes.EnemyDefinition definition = DungeonContent.enemy(DungeonEnemies.enemyId(victim));
            if (definition != null && definition.elite()) {
                tag.putInt("rank_trial_elites", tag.getInt("rank_trial_elites") + 1);
                checkCB(player, tag);
            }
        }
    }

    public static void onDungeonObjective(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!active(tag) || tag.getInt("rank_trial_from") != HunterRank.C.tier()) return;
        tag.putInt("rank_trial_objectives", tag.getInt("rank_trial_objectives") + 1);
        checkCB(player, tag);
    }

    public static void onDungeonClear(ServerPlayer player, DungeonSession session) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getInt("progression_stage") == 2 && currentTier(tag) >= HunterRank.S.tier()) {
            tag.putBoolean("job_change_dungeon_cleared", true);
        }
        if (!active(tag)) return;
        int from = tag.getInt("rank_trial_from");
        if (from == HunterRank.D.tier()) complete(player, HunterRank.C);
        else if (from == HunterRank.B.tier()) {
            if (session.members().size() == 1 && tag.getInt("rank_trial_recoveries") <= 3) complete(player, HunterRank.A);
            else fail(player, "Solo-raid or limited-recovery condition failed");
        } else if (from == HunterRank.A.tier()) {
            int score = score(tag, player);
            tag.putInt("rank_trial_score", score);
            if (score >= 70) complete(player, HunterRank.S);
            else fail(player, "Performance score " + score + " / 70");
        }
    }

    public static void onDamageTaken(ServerPlayer player, float amount) {
        CompoundTag tag = HunterData.mutable(player);
        if (active(tag)) tag.putFloat("rank_trial_damage_taken", tag.getFloat("rank_trial_damage_taken") + Math.max(0.0F, amount));
    }

    public static void onBossDamage(ServerPlayer player, LivingEntity victim, float amount) {
        CompoundTag tag = HunterData.mutable(player);
        if (active(tag) && victim.getPersistentData().getBoolean(DungeonTypes.TAG_BOSS)) {
            tag.putFloat("rank_trial_boss_damage", tag.getFloat("rank_trial_boss_damage") + Math.max(0.0F, amount));
        }
    }

    public static void onRecovery(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (active(tag) && tag.getInt("rank_trial_from") == HunterRank.B.tier()) {
            long now = player.level().getGameTime();
            if (now - tag.getLong("rank_trial_last_recovery") < 100L) return;
            tag.putLong("rank_trial_last_recovery", now);
            tag.putInt("rank_trial_recoveries", tag.getInt("rank_trial_recoveries") + 1);
        }
    }

    public static void onAbilitySuccess(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (active(tag)) tag.putInt("rank_trial_ability_success", tag.getInt("rank_trial_ability_success") + 1);
    }

    public static void onDeath(ServerPlayer player) { if (active(HunterData.mutable(player))) fail(player, "Hunter defeated"); }

    public static boolean permitsEvaluationEntry(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        return active(tag) && tag.getInt("rank_trial_from") >= HunterRank.D.tier();
    }

    public static String status(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!active(tag)) {
            Trial next = trialFor(currentTier(tag));
            return next == null ? "Rank path complete: " + HunterRank.byTier(currentTier(tag)).displayName()
                    : "Current rank: " + next.from().displayName() + " | Next: " + next.to().displayName();
        }
        long seconds = Math.max(0L, (tag.getLong("rank_trial_end") - player.level().getGameTime() + 19L) / 20L);
        return HunterRank.byTier(tag.getInt("rank_trial_from")).displayName() + " evaluation active | " + seconds + "s remaining";
    }

    public static void reset(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putBoolean("rank_trial_active", false);
        tag.putLong("rank_trial_end", 0L);
        tag.putLong("rank_trial_retry_after", 0L);
        HunterData.sync(player);
    }

    private static void checkCB(ServerPlayer player, CompoundTag tag) {
        if (tag.getInt("rank_trial_elites") >= 1 && tag.getInt("rank_trial_objectives") >= 3) complete(player, HunterRank.B);
        else progress(player, tag.getInt("rank_trial_elites") + " / 1 elite, " + tag.getInt("rank_trial_objectives") + " / 3 objectives");
    }

    private static int score(CompoundTag tag, ServerPlayer player) {
        int boss = Math.min(45, Math.round(tag.getFloat("rank_trial_boss_damage") / Math.max(1.0F, player.getMaxHealth())));
        int ability = Math.min(25, tag.getInt("rank_trial_ability_success") * 2);
        int survival = Math.max(0, 30 - Math.round(tag.getFloat("rank_trial_damage_taken") / Math.max(1.0F, player.getMaxHealth()) * 10.0F));
        return Math.max(0, Math.min(100, boss + ability + survival));
    }

    private static void complete(ServerPlayer player, HunterRank rank) {
        CompoundTag tag = HunterData.mutable(player);
        if (!active(tag)) return;
        tag.putBoolean("rank_trial_active", false);
        tag.putLong("rank_trial_end", 0L);
        tag.putInt("rank_progress_tier", rank.tier());
        HunterData.setHunterRank(player, rank.tier());
        String rewardKey = "rank_reward_" + rank.name().toLowerCase();
        if (!tag.getBoolean(rewardKey)) {
            tag.putBoolean(rewardKey, true);
            HunterData.addStatPoints(player, 4 + rank.tier());
            HunterData.addGold(player, 750 * (rank.tier() + 1));
            HunterData.addXp(player, 600 * (rank.tier() + 1));
        }
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[RANK UP] Recognized as " + rank.displayName() + ".")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void fail(ServerPlayer player, String reason) {
        CompoundTag tag = HunterData.mutable(player);
        if (!active(tag)) return;
        tag.putBoolean("rank_trial_active", false);
        tag.putLong("rank_trial_end", 0L);
        tag.putLong("rank_trial_retry_after", player.level().getGameTime() + RETRY_TICKS);
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[RANK EVALUATION FAILED] " + reason + ".")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    private static void progress(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal("[RANK EVALUATION] " + text).withStyle(ChatFormatting.AQUA), true);
        HunterData.sync(player);
    }

    private static int currentTier(CompoundTag tag) {
        int progress = tag.getInt("rank_progress_tier");
        int override = tag.getInt("rank_override_tier");
        return Math.max(HunterRank.E.tier(), override >= HunterRank.E.tier() ? override : progress);
    }

    private static Trial trialFor(int tier) {
        for (Trial trial : TRIALS) if (trial.from().tier() == tier) return trial;
        return null;
    }

    private static boolean active(CompoundTag tag) { return tag.getBoolean("rank_trial_active"); }
    private RankTrialService() {}
}
