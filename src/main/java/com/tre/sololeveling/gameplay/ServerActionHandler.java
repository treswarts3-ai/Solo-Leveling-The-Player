package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.quest.QuestApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public final class ServerActionHandler {
    private static final Map<String, Integer> PRICES = Map.of(
            "healing_potion", 50,
            "mana_potion", 60,
            "greater_healing_potion", 150,
            "greater_mana_potion", 175,
            "knight_killer", 1500,
            "blessed_random_box", 750
    );

    public static void handle(ServerPlayer player, String action) {
        if (action == null || action.length() > 128) return;
        if (!allowAction(player)) return;
        if (action.equals("AWAKEN")) { HunterData.awaken(player); return; }
        if (!HunterData.isAwakened(player)) return;
        if (action.equals("OPEN_SYSTEM")) { HunterData.mutable(player).putBoolean("tutorial_opened", true); HunterData.mutable(player).putBoolean("tutorial_system_opened", true); QuestApi.onSystemOpened(player); HunterData.sync(player); return; }
        if (action.equals("TOGGLE_HUD")) { HunterData.toggleHud(player); return; }
        if (action.startsWith("ALLOCATE:")) { HunterData.allocate(player, action.substring(9), 1); return; }
        if (action.startsWith("ALLOCATE5:")) { HunterData.allocate(player, action.substring(10), 5); return; }
        if (action.startsWith("GROWTH_CHOICE:")) { ProgressionChoiceHandler.choose(player, action.substring(14)); return; }
        if (action.startsWith("ABILITY:")) {
            String skill = action.substring(8).toLowerCase(java.util.Locale.ROOT);
            int manaBefore = HunterData.getMana(player);
            long cooldownBefore = HunterData.cooldownRemaining(player, skill);
            AbilityHandler.activate(player, skill);
            if (cooldownBefore == 0L && HunterData.cooldownRemaining(player, skill) > 0L) {
                QuestApi.onAbilityUsed(player, skill);
                QuestApi.onManaSpent(player, Math.max(0, manaBefore - HunterData.getMana(player)));
                QuestHandler.onAbilityUse(player);
            }
            return;
        }
        if (action.startsWith("AUTHORITY:")) {
            String mode = action.substring(10).toLowerCase(java.util.Locale.ROOT);
            String cooldownKey = "rulers_authority_" + mode;
            int manaBefore = HunterData.getMana(player);
            long cooldownBefore = HunterData.cooldownRemaining(player, cooldownKey);
            AbilityHandler.activateAuthority(player, mode);
            if (cooldownBefore == 0L && HunterData.cooldownRemaining(player, cooldownKey) > 0L) {
                QuestApi.onAbilityUsed(player, "rulers_authority");
                QuestApi.onManaSpent(player, Math.max(0, manaBefore - HunterData.getMana(player)));
                QuestHandler.onAbilityUse(player);
            }
            return;
        }
        if (action.startsWith("EXERCISE:")) { QuestHandler.exercise(player, action.substring(9)); return; }
        if (action.equals("CLAIM_DAILY")) { QuestHandler.claimDaily(player); return; }
        if (action.equals("EXTRACT")) { ShadowHandler.extract(player); return; }
        if (action.equals("SUMMON_SHADOW")) { if (ShadowHandler.summonFirst(player)) QuestApi.onShadowSummoned(player, "any"); return; }
        if (action.equals("DISMISS_SHADOWS")) { ShadowHandler.dismissAll(player); return; }
        if (action.equals("SHADOW_MODE")) { ShadowHandler.cycleMode(player); return; }
        if (action.equals("SHADOW_EXCHANGE")) { ShadowHandler.exchange(player); return; }
        if (action.equals("TOGGLE_DOMAIN")) { AbilityHandler.activate(player, "monarch_domain"); return; }
        if (action.equals("STORE_HELD")) { HunterData.storeHeld(player); return; }
        if (action.equals("RETRIEVE_FIRST")) { HunterData.retrieveFirst(player); return; }
        if (action.startsWith("RETRIEVE_SLOT:")) {
            try { HunterData.retrieveSystemItem(player, Integer.parseInt(action.substring(14))); }
            catch (NumberFormatException ignored) { }
            return;
        }
        if (action.startsWith("BUY:")) { buy(player, action.substring(4)); }
    }

    private static boolean allowAction(ServerPlayer player) {
        net.minecraft.nbt.CompoundTag tag = HunterData.mutable(player);
        long now = player.level().getGameTime();
        long window = tag.getLong("packet_window_start");
        if (now - window >= 20L || now < window) {
            tag.putLong("packet_window_start", now);
            tag.putInt("packet_window_count", 0);
        }
        int count = tag.getInt("packet_window_count") + 1;
        tag.putInt("packet_window_count", count);
        return count <= 20;
    }

    private static void buy(ServerPlayer player, String name) {
        Integer price = PRICES.get(name);
        if (price == null) return;
        ResourceLocation id = new ResourceLocation("sololeveling", name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || !HunterData.spendGold(player, price)) {
            player.sendSystemMessage(Component.literal("[STORE] Purchase failed.").withStyle(ChatFormatting.RED));
            HunterData.sync(player);
            return;
        }
        if (!HunterData.storeSystemItem(player, new ItemStack(item))) {
            HunterData.addGold(player, price);
            player.sendSystemMessage(Component.literal("[STORE] Purchase canceled because System storage is full.").withStyle(ChatFormatting.RED));
            HunterData.sync(player);
            return;
        }
        player.sendSystemMessage(Component.literal("[STORE] Purchased " + item.getDescription().getString() + " for " + price + " gold.").withStyle(ChatFormatting.GOLD));
        HunterData.sync(player);
    }

    private ServerActionHandler() {}
}
