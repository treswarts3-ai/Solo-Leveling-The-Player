package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;

/** Persistent five-level milestone choices layered on top of normal Hunter growth. */
public final class ProgressionChoiceHandler {
    public static final int FIRST_CHOICE_LEVEL = 5;
    public static final int CHOICE_INTERVAL = 5;

    public static void tick(ServerPlayer player) {
        if (!HunterData.isAwakened(player) || player.tickCount % 20 != 0) return;
        CompoundTag tag = HunterData.mutable(player);
        int next = tag.contains("next_growth_choice_level")
                ? Math.max(FIRST_CHOICE_LEVEL, tag.getInt("next_growth_choice_level"))
                : FIRST_CHOICE_LEVEL;
        int pending = Math.max(0, tag.getInt("pending_growth_choices"));
        int level = HunterData.getLevel(player);
        boolean changed = false;

        while (level >= next && pending < 20) {
            pending++;
            next += CHOICE_INTERVAL;
            changed = true;
        }

        if (!changed) return;
        tag.putInt("pending_growth_choices", pending);
        tag.putInt("next_growth_choice_level", next);
        player.sendSystemMessage(Component.literal("[SYSTEM] Growth selection available. Open Stats to choose a path.")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.25F);
        HunterData.sync(player);
    }

    public static boolean choose(ServerPlayer player, String rawChoice) {
        CompoundTag tag = HunterData.mutable(player);
        int pending = Math.max(0, tag.getInt("pending_growth_choices"));
        if (pending <= 0) {
            player.sendSystemMessage(Component.literal("[SYSTEM] No growth selection is pending.").withStyle(ChatFormatting.GRAY));
            return false;
        }

        String choice = rawChoice == null ? "" : rawChoice.trim().toLowerCase(Locale.ROOT);
        String description;
        switch (choice) {
            case "vanguard" -> {
                HunterData.addStat(player, "strength", 2);
                HunterData.addStat(player, "stamina", 1);
                tag.putInt("growth_vanguard", tag.getInt("growth_vanguard") + 1);
                description = "+2 Strength, +1 Stamina";
            }
            case "phantom" -> {
                HunterData.addStat(player, "agility", 2);
                HunterData.addStat(player, "sense", 1);
                tag.putInt("growth_phantom", tag.getInt("growth_phantom") + 1);
                description = "+2 Agility, +1 Sense";
            }
            case "arcane" -> {
                HunterData.addStat(player, "intelligence", 3);
                HunterData.addMana(player, HunterData.getMaxMana(player));
                tag.putInt("growth_arcane", tag.getInt("growth_arcane") + 1);
                description = "+3 Intelligence and full Mana";
            }
            default -> {
                return false;
            }
        }

        tag.putInt("pending_growth_choices", pending - 1);
        tag.putString("last_growth_choice", choice);
        HunterData.recalculateAttributes(player);
        HunterData.sync(player);
        player.sendSystemMessage(Component.literal("[GROWTH SELECTED] " + title(choice) + " — " + description)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
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

    private static String title(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private ProgressionChoiceHandler() {}
}
