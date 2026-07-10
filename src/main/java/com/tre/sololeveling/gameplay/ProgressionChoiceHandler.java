package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.data.HunterRank;
import com.tre.sololeveling.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Server-authoritative five-level growth, ten-level milestones, and rank evaluation state. */
public final class ProgressionChoiceHandler {
    public static final int FIRST_CHOICE_LEVEL = 5;
    public static final int CHOICE_INTERVAL = 5;
    public static final int FIRST_MILESTONE_LEVEL = 10;
    public static final int MILESTONE_INTERVAL = 10;
    public static final int D_RANK_LEVEL = 10;
    public static final int D_RANK_KILLS = 10;
    public static final int RANK_TRIAL_DURATION_TICKS = 20 * 60 * 5;
    private static final int MAX_PENDING = 20;

    public static void tick(ServerPlayer player) {
        if (!HunterData.isAwakened(player) || player.tickCount % 20 != 0) return;
        CompoundTag tag = HunterData.mutable(player);
        boolean changed = ensureRankState(player, tag);
        changed |= queueGrowthChoices(player, tag);
        changed |= queueMilestones(player, tag);
        changed |= updateRankTrial(player, tag);
        if (changed) HunterData.sync(player);
    }

    private static boolean queueGrowthChoices(ServerPlayer player, CompoundTag tag) {
        int next = tag.contains("next_growth_choice_level")
                ? Math.max(FIRST_CHOICE_LEVEL, tag.getInt("next_growth_choice_level"))
                : FIRST_CHOICE_LEVEL;
        int pending = Math.max(0, tag.getInt("pending_growth_choices"));
        int level = HunterData.getLevel(player);
        boolean changed = false;

        while (level >= next && pending < MAX_PENDING) {
            pending++;
            next += CHOICE_INTERVAL;
            changed = true;
        }

        if (!changed) return false;
        tag.putInt("pending_growth_choices", pending);
        tag.putInt("next_growth_choice_level", next);
        player.sendSystemMessage(Component.literal("[SYSTEM] Growth selection available. Open Stats to choose a path.")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.25F);
        return true;
    }

    private static boolean queueMilestones(ServerPlayer player, CompoundTag tag) {
        int next = tag.contains("next_major_milestone_level")
                ? Math.max(FIRST_MILESTONE_LEVEL, tag.getInt("next_major_milestone_level"))
                : FIRST_MILESTONE_LEVEL;
        int pending = Math.max(0, tag.getInt("pending_major_milestones"));
        int level = HunterData.getLevel(player);
        boolean changed = false;

        while (level >= next && pending < MAX_PENDING) {
            pending++;
            next += MILESTONE_INTERVAL;
            changed = true;
        }

        if (!changed) return false;
        tag.putInt("pending_major_milestones", pending);
        tag.putInt("next_major_milestone_level", next);
        player.sendSystemMessage(Component.literal("[SYSTEM] Major milestone reward available. Choose an evolution, mastery, or cache reward.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.15F);
        return true;
    }

    public static boolean choose(ServerPlayer player, String rawChoice) {
        CompoundTag tag = HunterData.mutable(player);
        int pending = Math.max(0, tag.getInt("pending_growth_choices"));
        if (pending <= 0) {
            player.sendSystemMessage(Component.literal("[SYSTEM] No growth selection is pending.").withStyle(ChatFormatting.GRAY));
            return false;
        }

        String choice = normalize(rawChoice);
        if (!choice.equals("vanguard") && !choice.equals("phantom") && !choice.equals("arcane")) return false;
        tag.putInt("pending_growth_choices", pending - 1);
        tag.putString("last_growth_choice", choice);

        String description;
        switch (choice) {
            case "vanguard" -> {
                tag.putInt("growth_vanguard", tag.getInt("growth_vanguard") + 1);
                HunterData.addStat(player, "strength", 2);
                HunterData.addStat(player, "stamina", 1);
                description = "+2 Strength, +1 Stamina";
            }
            case "phantom" -> {
                tag.putInt("growth_phantom", tag.getInt("growth_phantom") + 1);
                HunterData.addStat(player, "agility", 2);
                HunterData.addStat(player, "sense", 1);
                description = "+2 Agility, +1 Sense";
            }
            default -> {
                tag.putInt("growth_arcane", tag.getInt("growth_arcane") + 1);
                HunterData.addStat(player, "intelligence", 3);
                HunterData.setMana(player, HunterData.getMaxMana(player));
                description = "+3 Intelligence and full Mana";
            }
        }

        HunterData.recalculateAttributes(player);
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[GROWTH SELECTED] " + title(choice) + " — " + description)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public static boolean chooseMilestone(ServerPlayer player, String rawChoice) {
        CompoundTag tag = HunterData.mutable(player);
        int pending = Math.max(0, tag.getInt("pending_major_milestones"));
        if (pending <= 0) {
            player.sendSystemMessage(Component.literal("[SYSTEM] No major milestone reward is pending.").withStyle(ChatFormatting.GRAY));
            return false;
        }

        String choice = normalize(rawChoice);
        if (!choice.equals("evolution") && !choice.equals("mastery") && !choice.equals("cache")) return false;
        if (choice.equals("cache") && HunterData.systemInventorySize(player) >= 108) {
            player.sendSystemMessage(Component.literal("[SYSTEM] Clear one System inventory slot before selecting the reward cache.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        tag.putInt("pending_major_milestones", pending - 1);
        tag.putString("last_major_milestone_choice", choice);
        String description;
        switch (choice) {
            case "evolution" -> {
                tag.putInt("skill_evolution_tokens", tag.getInt("skill_evolution_tokens") + 1);
                tag.putInt("milestone_evolution", tag.getInt("milestone_evolution") + 1);
                description = "+1 Skill Evolution Token";
            }
            case "mastery" -> {
                tag.putInt("milestone_mastery", tag.getInt("milestone_mastery") + 1);
                HunterData.addStatPoints(player, 5);
                description = "+5 Stat Points";
            }
            default -> {
                tag.putInt("milestone_cache", tag.getInt("milestone_cache") + 1);
                HunterData.addGold(player, 1000);
                HunterData.storeSystemItem(player, new ItemStack(ModItems.BLESSED_RANDOM_BOX.get()));
                description = "+1000 Gold and a Blessed Box";
            }
        }

        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[MAJOR MILESTONE] " + title(choice) + " — " + description)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean ensureRankState(ServerPlayer player, CompoundTag tag) {
        boolean changed = false;
        if (!tag.getBoolean("rank_progress_initialized")) {
            int override = tag.getInt("rank_override_tier");
            int migratedTier = override >= HunterRank.E.tier()
                    ? override
                    : HunterRank.automatic(HunterData.getLevel(player), tag.getBoolean("black_heart")).tier();
            HunterRank rank = HunterRank.byTier(migratedTier);
            tag.putInt("rank_progress_tier", rank.tier());
            tag.putInt("rank_override_tier", rank.tier());
            tag.putString("rank_override", rank.displayName());
            tag.putBoolean("rank_progress_initialized", true);
            changed = true;
        }

        int storedTier = Math.max(HunterRank.E.tier(), Math.min(HunterRank.SHADOW_MONARCH.tier(), tag.getInt("rank_progress_tier")));
        int override = tag.getInt("rank_override_tier");
        if (override >= HunterRank.E.tier() && override <= HunterRank.SHADOW_MONARCH.tier() && override != storedTier) {
            storedTier = override;
            tag.putInt("rank_progress_tier", storedTier);
            changed = true;
        } else if (override < HunterRank.E.tier()) {
            HunterRank rank = HunterRank.byTier(storedTier);
            tag.putInt("rank_override_tier", storedTier);
            tag.putString("rank_override", rank.displayName());
            changed = true;
        }
        return changed;
    }

    private static boolean updateRankTrial(ServerPlayer player, CompoundTag tag) {
        int tier = tag.getInt("rank_progress_tier");
        boolean eligible = tier == HunterRank.E.tier() && HunterData.getLevel(player) >= D_RANK_LEVEL;
        boolean changed = false;
        if (eligible && !tag.getBoolean("rank_trial_eligible_notified")) {
            tag.putBoolean("rank_trial_eligible_notified", true);
            player.sendSystemMessage(Component.literal("[SYSTEM] E-to-D Rank Evaluation available. Use the Stats screen or /slrank begin.")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            changed = true;
        }
        if (tag.getBoolean("rank_trial_active") && player.level().getGameTime() >= tag.getLong("rank_trial_end")) {
            failRankTrial(player, "Time expired");
            return false;
        }
        return changed;
    }

    public static boolean beginRankTrial(ServerPlayer player) {
        if (!HunterData.isAwakened(player)) return false;
        CompoundTag tag = HunterData.mutable(player);
        ensureRankState(player, tag);
        long now = player.level().getGameTime();
        if (tag.getBoolean("rank_trial_active")) {
            player.sendSystemMessage(Component.literal("[SYSTEM] A rank evaluation is already active.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (tag.getInt("rank_progress_tier") != HunterRank.E.tier() || HunterData.getLevel(player) < D_RANK_LEVEL) {
            player.sendSystemMessage(Component.literal("[SYSTEM] E-to-D evaluation requires E-Rank and level " + D_RANK_LEVEL + ".")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (now < tag.getLong("rank_trial_retry_after")) {
            long seconds = Math.max(1L, (tag.getLong("rank_trial_retry_after") - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("[SYSTEM] Evaluation retry available in " + seconds + "s.").withStyle(ChatFormatting.RED));
            return false;
        }

        tag.putBoolean("rank_trial_active", true);
        tag.putInt("rank_trial_kills", 0);
        tag.putInt("rank_trial_target", D_RANK_KILLS);
        tag.putLong("rank_trial_end", now + RANK_TRIAL_DURATION_TICKS);
        tag.putInt("rank_trial_attempts", tag.getInt("rank_trial_attempts") + 1);
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[RANK EVALUATION] Defeat " + D_RANK_KILLS
                + " hostile enemies within 5 minutes without dying.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
        return true;
    }

    public static void onRankTrialKill(ServerPlayer player, LivingEntity victim) {
        CompoundTag tag = HunterData.mutable(player);
        if (!tag.getBoolean("rank_trial_active") || !(victim instanceof Enemy)) return;
        if (victim.getPersistentData().getBoolean("sl_shadow") || victim.getPersistentData().getBoolean("sl_penalty_mob")) return;
        if (victim.getPersistentData().getBoolean("sl_rank_trial_counted")) return;
        victim.getPersistentData().putBoolean("sl_rank_trial_counted", true);

        int target = Math.max(1, tag.getInt("rank_trial_target"));
        int kills = Math.min(target, tag.getInt("rank_trial_kills") + 1);
        tag.putInt("rank_trial_kills", kills);
        if (kills >= target) completeRankTrial(player);
        else {
            player.displayClientMessage(Component.literal("[RANK EVALUATION] " + kills + " / " + target)
                    .withStyle(ChatFormatting.AQUA), true);
            HunterData.sync(player);
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (HunterData.mutable(player).getBoolean("rank_trial_active")) failRankTrial(player, "Hunter defeated");
    }

    private static void completeRankTrial(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!tag.getBoolean("rank_trial_active")) return;
        tag.putBoolean("rank_trial_active", false);
        tag.putInt("rank_trial_kills", tag.getInt("rank_trial_target"));
        tag.putLong("rank_trial_end", 0L);
        tag.putInt("rank_progress_tier", HunterRank.D.tier());
        tag.putInt("rank_override_tier", HunterRank.D.tier());
        tag.putString("rank_override", HunterRank.D.displayName());

        if (!tag.getBoolean("rank_reward_d_granted")) {
            tag.putBoolean("rank_reward_d_granted", true);
            HunterData.addStatPoints(player, 5);
            HunterData.addGold(player, 1000);
            HunterData.addXp(player, 750);
        }

        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[RANK UP] Evaluation passed. Hunter rank advanced to D-Rank.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void failRankTrial(ServerPlayer player, String reason) {
        CompoundTag tag = HunterData.mutable(player);
        if (!tag.getBoolean("rank_trial_active")) return;
        tag.putBoolean("rank_trial_active", false);
        tag.putLong("rank_trial_end", 0L);
        tag.putLong("rank_trial_retry_after", player.level().getGameTime() + 20L * 60L);
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[RANK EVALUATION FAILED] " + reason + ". Retry available in 60 seconds.")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    public static String rankStatus(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        ensureRankState(player, tag);
        if (tag.getBoolean("rank_trial_active")) {
            long seconds = Math.max(0L, (tag.getLong("rank_trial_end") - player.level().getGameTime() + 19L) / 20L);
            return "E-to-D evaluation active: " + tag.getInt("rank_trial_kills") + " / "
                    + Math.max(1, tag.getInt("rank_trial_target")) + " kills, " + seconds + "s remaining";
        }
        if (tag.getInt("rank_progress_tier") == HunterRank.E.tier() && HunterData.getLevel(player) >= D_RANK_LEVEL) {
            return "E-to-D evaluation available";
        }
        return "Current rank: " + HunterRank.byTier(tag.getInt("rank_progress_tier")).displayName();
    }

    public static void reset(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("pending_growth_choices", 0);
        tag.putInt("next_growth_choice_level", FIRST_CHOICE_LEVEL);
        tag.putInt("growth_vanguard", 0);
        tag.putInt("growth_phantom", 0);
        tag.putInt("growth_arcane", 0);
        tag.remove("last_growth_choice");
        HunterData.sync(player);
    }

    public static void resetMilestones(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("pending_major_milestones", 0);
        tag.putInt("next_major_milestone_level", FIRST_MILESTONE_LEVEL);
        tag.remove("last_major_milestone_choice");
        HunterData.sync(player);
    }

    public static void resetRankTrial(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putBoolean("rank_trial_active", false);
        tag.putInt("rank_trial_kills", 0);
        tag.putLong("rank_trial_end", 0L);
        tag.putLong("rank_trial_retry_after", 0L);
        HunterData.sync(player);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String title(String value) {
        return value.isBlank() ? "" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private ProgressionChoiceHandler() {}
}
