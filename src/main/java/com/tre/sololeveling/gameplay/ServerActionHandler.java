package com.tre.sololeveling.gameplay;

import com.mojang.logging.LogUtils;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.ability.AbilityService;
import com.tre.sololeveling.quest.QuestApi;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

public final class ServerActionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> STATS = Set.of("strength", "agility", "stamina", "intelligence", "sense");
    private static final Map<String, Integer> PRICES = Map.of(
            "healing_potion", 50,
            "mana_potion", 60,
            "greater_healing_potion", 150,
            "greater_mana_potion", 175,
            "knight_killer", 1500,
            "blessed_random_box", 750
    );

    public static void handle(ServerPlayer player, String action) {
        if (action == null || action.isBlank() || action.length() > 128) {
            CompoundTag tag = HunterData.mutable(player);
            tag.putLong("debug_packets_received", tag.getLong("debug_packets_received") + 1L);
            reject(player, "malformed action envelope");
            return;
        }
        if (!allowAction(player)) return;
        if (!player.isAlive() || player.isSpectator() || player.getServer() == null
                || !player.serverLevel().hasChunkAt(player.blockPosition())) {
            reject(player, "invalid player state");
            return;
        }
        if (action.equals("AWAKEN")) { HunterData.awaken(player); return; }
        if (!HunterData.isAwakened(player)) { reject(player, "hunter is not awakened"); return; }
        if (action.equals("OPEN_SYSTEM")) { HunterData.mutable(player).putBoolean("tutorial_opened", true); HunterData.mutable(player).putBoolean("tutorial_system_opened", true); QuestApi.onSystemOpened(player); HunterData.sync(player); return; }
        if (action.equals("TOGGLE_HUD")) { HunterData.toggleHud(player); return; }
        if (action.startsWith("ALLOCATE:")) {
            if (!STATS.contains(action.substring(9).toLowerCase(Locale.ROOT))) { reject(player, "unknown stat id"); return; }
            if (allowMutation(player, "allocate", 2L)) HunterData.allocate(player, action.substring(9), 1);
            return;
        }
        if (action.startsWith("ALLOCATE5:")) {
            if (!STATS.contains(action.substring(10).toLowerCase(Locale.ROOT))) { reject(player, "unknown stat id"); return; }
            if (allowMutation(player, "allocate", 4L)) HunterData.allocate(player, action.substring(10), 5);
            return;
        }
        if (action.startsWith("GROWTH_CHOICE:")) {
            String choice = action.substring(14);
            if (!Set.of("vanguard", "phantom", "arcane").contains(choice)) { reject(player, "unknown growth choice id"); return; }
            if (allowMutation(player, "growth_choice", 5L)) ProgressionChoiceHandler.choose(player, choice); return;
        }
        if (action.startsWith("MILESTONE_CHOICE:")) {
            String choice = action.substring(17);
            if (!Set.of("evolution", "mastery", "cache").contains(choice)) { reject(player, "unknown milestone id"); return; }
            if (allowMutation(player, "milestone_choice", 5L)) ProgressionChoiceHandler.chooseMilestone(player, choice); return;
        }
        if (action.startsWith("EVOLVE:quicksilver:")) {
            String variant = action.substring(19);
            if (!Set.of("phantom_step", "flash_execution").contains(variant)) { reject(player, "unknown evolution id"); return; }
            if (allowMutation(player, "evolve_quicksilver", 10L))
                ProgressionChoiceHandler.chooseAbilityEvolution(player, "quicksilver", variant);
            return;
        }
        if (action.equals("BEGIN_RANK_TRIAL")) {
            if (allowMutation(player, "rank_trial", 20L)) ProgressionChoiceHandler.beginRankTrial(player);
            return;
        }
        if (action.startsWith("ABILITY:")) {
            String skill = action.substring(8).toLowerCase(Locale.ROOT);
            if (AbilityService.definition(skill).isEmpty()) { reject(player, "unknown ability id"); return; }
            activateAndTrack(player, skill, skill);
            return;
        }
        if (action.startsWith("AUTHORITY:")) {
            String mode = action.substring(10).toLowerCase(Locale.ROOT).trim();
            String abilityId = mode.isBlank() ? "rulers_authority" : "rulers_authority_" + mode;
            if (AbilityService.definition(abilityId).isEmpty()) { reject(player, "unknown authority mode"); return; }
            activateAndTrack(player, abilityId, "rulers_authority");
            return;
        }
        if (action.startsWith("EXERCISE:")) { QuestHandler.exercise(player, action.substring(9)); return; }
        if (action.equals("CLAIM_DAILY")) {
            if (allowMutation(player, "claim_daily", 10L)) QuestHandler.claimDaily(player);
            return;
        }
        if (action.equals("EXTRACT")) { if (allowMutation(player, "extract", 10L)) ShadowHandler.extract(player); return; }
        if (action.equals("SUMMON_SHADOW")) { if (allowMutation(player, "summon_shadow", 5L) && ShadowHandler.summonFirst(player)) QuestApi.onShadowSummoned(player, "any"); return; }
        if (action.equals("DISMISS_SHADOWS")) { ShadowHandler.dismissAll(player); return; }
        if (action.equals("SHADOW_MODE")) { ShadowHandler.cycleMode(player); return; }
        if (action.equals("SHADOW_EXCHANGE")) { ShadowHandler.exchange(player); return; }
        if (action.equals("TOGGLE_DOMAIN")) { activateAndTrack(player, "monarch_domain", "monarch_domain"); return; }
        if (action.equals("STORE_HELD")) {
            if (allowMutation(player, "store_held", 4L)) HunterData.storeHeld(player);
            return;
        }
        if (action.equals("RETRIEVE_FIRST")) {
            if (allowMutation(player, "retrieve", 3L)) HunterData.retrieveFirst(player);
            return;
        }
        if (action.startsWith("RETRIEVE_SLOT:")) {
            if (!allowMutation(player, "retrieve", 3L)) return;
            try {
                int slot = Integer.parseInt(action.substring(14));
                if (slot < 0 || slot >= 108) { reject(player, "impossible inventory slot"); return; }
                HunterData.retrieveSystemItem(player, slot);
            } catch (NumberFormatException ignored) { reject(player, "malformed inventory slot"); }
            return;
        }
        if (action.startsWith("BUY:")) {
            String product = action.substring(4);
            if (!PRICES.containsKey(product)) { reject(player, "unknown store item id"); return; }
            if (allowMutation(player, "purchase", 8L)) buy(player, product);
            return;
        }
        reject(player, "unknown action id");
    }

    private static void activateAndTrack(ServerPlayer player, String abilityId, String questId) {
        int manaBefore = HunterData.getMana(player);
        if (!AbilityService.activate(player, abilityId)) return;
        QuestApi.onAbilityUsed(player, questId);
        QuestApi.onManaSpent(player, Math.max(0, manaBefore - HunterData.getMana(player)));
        QuestHandler.onAbilityUse(player);
    }

    /** Broad abuse ceiling for every client action packet. */
    private static boolean allowAction(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putLong("debug_packets_received", tag.getLong("debug_packets_received") + 1L);
        long now = player.level().getGameTime();
        long window = tag.getLong("packet_window_start");
        if (now - window >= 20L || now < window) {
            tag.putLong("packet_window_start", now);
            tag.putInt("packet_window_count", 0);
        }
        int count = tag.getInt("packet_window_count") + 1;
        tag.putInt("packet_window_count", count);
        boolean allowed = count <= 20;
        if (!allowed) recordRejected(player, "packet rate limit exceeded");
        return allowed;
    }

    private static void reject(ServerPlayer player, String reason) {
        if (player == null) return;
        recordRejected(player, reason);
    }

    private static void recordRejected(ServerPlayer player, String reason) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putLong("debug_packets_rejected", tag.getLong("debug_packets_rejected") + 1L);
        long now = player.level().getGameTime();
        long lastLog = tag.getLong("packet_rejection_log_tick");
        if (now < lastLog || now - lastLog >= 100L) {
            tag.putLong("packet_rejection_log_tick", now);
            LOGGER.warn("[NETWORK] Rejected client action from {}: {}", player.getUUID(), reason);
        }
    }

    /** Action-specific debounce for irreversible UI mutations. */
    private static boolean allowMutation(ServerPlayer player, String action, long cooldownTicks) {
        CompoundTag tag = HunterData.mutable(player);
        String key = "ui_action_" + action;
        long now = player.level().getGameTime();
        if (tag.contains(key, Tag.TAG_LONG)) {
            long previous = tag.getLong(key);
            if (now >= previous && now - previous < Math.max(1L, cooldownTicks)) {
                recordRejected(player, "action debounce: " + action);
                return false;
            }
        }
        tag.putLong(key, now);
        return true;
    }

    private static void buy(ServerPlayer player, String name) {
        Integer price = PRICES.get(name);
        if (price == null) return;
        ResourceLocation id = new ResourceLocation("sololeveling", name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        boolean infiniteGold = HunterData.mutable(player).getBoolean("god_powers");
        if (item == null || (!infiniteGold && !HunterData.spendGold(player, price))) {
            player.sendSystemMessage(Component.literal("[STORE] Purchase failed.").withStyle(ChatFormatting.RED));
            HunterData.sync(player);
            return;
        }
        if (!HunterData.storeSystemItem(player, new ItemStack(item))) {
            if (!infiniteGold) HunterData.addGold(player, price);
            player.sendSystemMessage(Component.literal("[STORE] Purchase canceled because System storage is full.").withStyle(ChatFormatting.RED));
            HunterData.sync(player);
            return;
        }
        String cost = infiniteGold ? "infinite gold" : price + " gold";
        player.sendSystemMessage(Component.literal("[STORE] Purchased " + item.getDescription().getString() + " with " + cost + ".").withStyle(ChatFormatting.GOLD));
        HunterData.sync(player);
    }

    private ServerActionHandler() {}
}
