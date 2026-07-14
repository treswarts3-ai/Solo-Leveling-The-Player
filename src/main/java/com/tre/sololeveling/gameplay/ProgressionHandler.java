package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModItems;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Main, job-change, shadow, and emergency quest progression without story bosses. */
public final class ProgressionHandler {
    public static void tick(ServerPlayer player) {
        if (!HunterData.isAwakened(player) || player.tickCount % 20 != 0) return;
        CompoundTag tag = HunterData.mutable(player);
        int stage = tag.getInt("progression_stage");
        if (stage == 0 && tag.getBoolean("tutorial_system_opened")
                && tag.getBoolean("tutorial_stat_allocated") && tag.getBoolean("daily_claimed")) {
            completeStage(player, 1, "dagger_training", "Awakening Tutorial", 350, 250);
            HunterData.storeSystemItem(player, new ItemStack(ModItems.KIM_SANGSHIK_STEEL_SWORD.get()));
        } else if (stage == 1 && tag.getInt("quest_dagger_damage") >= 500) {
            HunterData.unlockSkill(player, "advanced_dagger_techniques");
            HunterData.storeSystemItem(player, new ItemStack(ModItems.KNIGHT_KILLER.get()));
            completeStage(player, 2, "job_change", "Dagger Training", 900, 600);
        } else if (stage == 2 && HunterData.getLevel(player) >= 60
                && HunterData.getRankTier(player) >= com.tre.sololeveling.data.HunterRank.S.tier()
                && tag.getBoolean("job_change_dungeon_cleared") && tag.getInt("job_change_kills") >= 25) {
            tag.putString("job", "Necromancer");
            HunterData.unlockSkill(player, "shadow_extraction");
            HunterData.unlockSkill(player, "shadow_preservation");
            completeStage(player, 3, "shadow_mastery", "Job Change Quest", 2500, 1500);
        } else if (stage == 3 && tag.getInt("shadow_extractions") >= 3 && tag.getBoolean("shadow_summoned_once")) {
            tag.putString("job", "Shadow Monarch");
            tag.putString("title", "Monarch of Shadows");
            HunterData.unlockSkill(player, "shadow_exchange");
            HunterData.setHunterRank(player, com.tre.sololeveling.data.HunterRank.SHADOW_MONARCH.tier());
            completeStage(player, 4, "black_heart_trial", "Shadow Mastery", 5000, 3000);
        } else if (stage == 4 && HunterData.getLevel(player) >= 80 && HunterData.shadows(player).size() >= 10) {
            HunterData.storeSystemItem(player, new ItemStack(ModItems.BLACK_HEART.get()));
            HunterData.unlockSkill(player, "monarch_domain");
            completeStage(player, 5, "completed", "Black Heart Trial", 10000, 10000);
        }
        tickEmergency(player, tag);
    }

    public static void onKill(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("total_kills", tag.getInt("total_kills") + 1);
        if (tag.getInt("progression_stage") == 2) tag.putInt("job_change_kills", tag.getInt("job_change_kills") + 1);
        if (tag.getBoolean("emergency_active")) tag.putInt("emergency_kills", tag.getInt("emergency_kills") + 1);
    }

    public static void startEmergency(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!HunterData.isAwakened(player) || tag.getBoolean("emergency_active") || player.getHealth() > player.getMaxHealth() * 0.20F) return;
        tag.putBoolean("emergency_active", true);
        tag.putInt("emergency_kills", 0);
        tag.putLong("emergency_end", player.level().getGameTime() + 20L * 120L);
        player.sendSystemMessage(Component.literal("[EMERGENCY QUEST] Defeat 3 hostile enemies or survive for 120 seconds.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        HunterData.sync(player);
    }

    private static void tickEmergency(ServerPlayer player, CompoundTag tag) {
        if (!tag.getBoolean("emergency_active")) return;
        boolean kills = tag.getInt("emergency_kills") >= 3;
        boolean survived = player.level().getGameTime() >= tag.getLong("emergency_end");
        if (!kills && !survived) return;
        tag.putBoolean("emergency_active", false);
        HunterData.addXp(player, 700);
        HunterData.addGold(player, 400);
        player.heal(player.getMaxHealth());
        player.sendSystemMessage(Component.literal("[QUEST COMPLETE] Emergency Quest").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        HunterData.sync(player);
    }

    private static void completeStage(ServerPlayer player, int nextStage, String nextQuest, String completed, int xp, int gold) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("progression_stage", nextStage);
        tag.putString("active_main_quest", nextQuest);
        HunterData.addXp(player, xp);
        HunterData.addGold(player, gold);
        player.sendSystemMessage(Component.literal("[MAIN QUEST COMPLETE] " + completed).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), ModSounds.QUEST_COMPLETE.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
        HunterData.sync(player);
    }

    private ProgressionHandler() {}
}
