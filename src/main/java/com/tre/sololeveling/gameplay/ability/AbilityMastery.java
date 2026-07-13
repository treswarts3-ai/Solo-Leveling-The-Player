package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned use mastery shared by every Phase 5 major ability. */
public final class AbilityMastery {
    private static final String ROOT = "ability_mastery";
    private static final int[] THRESHOLDS = {0, 10, 30, 75};

    public static int uses(ServerPlayer player, String abilityId) {
        return uses(HunterData.mutable(player), abilityId);
    }

    public static int uses(CompoundTag hunterData, String abilityId) {
        CompoundTag root = hunterData.contains(ROOT, Tag.TAG_COMPOUND)
                ? hunterData.getCompound(ROOT) : new CompoundTag();
        return Math.max(0, root.getInt(AbilityDefinition.normalize(abilityId)));
    }

    public static int level(ServerPlayer player, String abilityId) {
        return level(HunterData.mutable(player), abilityId);
    }

    public static int level(CompoundTag hunterData, String abilityId) {
        int uses = uses(hunterData, abilityId);
        int level = 0;
        for (int index = 1; index < THRESHOLDS.length; index++) {
            if (uses >= THRESHOLDS[index]) level = index;
        }
        return level;
    }

    public static void recordResolvedUse(ServerPlayer player, AbilityDefinition definition) {
        CompoundTag hunter = HunterData.mutable(player);
        CompoundTag root = hunter.contains(ROOT, Tag.TAG_COMPOUND)
                ? hunter.getCompound(ROOT) : new CompoundTag();
        String id = definition.id();
        int before = level(hunter, id);
        root.putInt(id, Math.min(100_000, Math.max(0, root.getInt(id)) + 1));
        hunter.put(ROOT, root);
        int after = level(hunter, id);
        if (after > before) {
            player.sendSystemMessage(Component.literal("[MASTERY] " + definition.displayName()
                    + " reached tier " + after + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static int adjustManaCost(ServerPlayer player, String abilityId, int baseCost) {
        return adjustManaCost(HunterData.mutable(player), abilityId, baseCost);
    }

    public static int adjustManaCost(CompoundTag hunterData, String abilityId, int baseCost) {
        double reduction = 0.05D * level(hunterData, abilityId);
        return Math.max(0, (int)Math.ceil(Math.max(0, baseCost) * (1.0D - reduction)));
    }

    public static int adjustCooldown(ServerPlayer player, String abilityId, int baseTicks) {
        double reduction = 0.04D * level(player, abilityId);
        return Math.max(0, (int)Math.ceil(Math.max(0, baseTicks) * (1.0D - reduction)));
    }

    public static double adjustedRange(ServerPlayer player, AbilityDefinition definition) {
        return Math.min(32.0D, definition.maximumRange()
                * (1.0D + 0.025D * level(player, definition.id())));
    }

    public static double powerMultiplier(ServerPlayer player, String abilityId) {
        return 1.0D + 0.04D * level(player, abilityId);
    }

    public static double domainRadius(ServerPlayer player) {
        return 8.0D + 2.0D * level(player, "monarch_domain");
    }

    private AbilityMastery() {}
}
