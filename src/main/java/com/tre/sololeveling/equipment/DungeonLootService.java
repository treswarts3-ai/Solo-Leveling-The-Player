package com.tre.sololeveling.equipment;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** High-value dungeon equipment roll with deterministic duplicate conversion and no lossy drops. */
public final class DungeonLootService {
    public static void grantMasterDungeonRoll(ServerPlayer player) {
        List<EquipmentDefinition> pool = EquipmentCatalog.definitions().stream()
                .filter(definition -> definition.rarity() == EquipmentRarity.EPIC || definition.rarity() == EquipmentRarity.LEGENDARY)
                .filter(definition -> !definition.acquisitionKey().equals("admin")).toList();
        if (pool.isEmpty()) return;
        EquipmentRarity target = player.getRandom().nextFloat() < 0.24F ? EquipmentRarity.LEGENDARY : EquipmentRarity.EPIC;
        List<EquipmentDefinition> tier = pool.stream().filter(value -> value.rarity() == target).toList();
        if (tier.isEmpty()) tier = pool;
        EquipmentDefinition definition = tier.get(player.getRandom().nextInt(tier.size()));
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("sololeveling", definition.id()));
        if (item == null || item == Items.AIR) return;
        if (owns(player, item)) {
            int converted = Math.max(100, (int)Math.round(definition.sellValue() * 0.65D));
            HunterData.addGold(player, converted);
            player.sendSystemMessage(Component.literal("[DUPLICATE CONVERSION] " + definition.id() + " converted into "
                    + converted + " gold.").withStyle(ChatFormatting.GOLD));
            return;
        }
        ItemStack reward = new ItemStack(item);
        if (!player.getInventory().add(reward)) HunterData.storeSystemItem(player, reward.copy());
        player.sendSystemMessage(Component.literal("[DUNGEON EQUIPMENT] " + definition.rarity().name() + " — "
                + reward.getHoverName().getString()).withStyle(definition.rarity().color(), ChatFormatting.BOLD));
    }

    private static boolean owns(ServerPlayer player, Item item) {
        if (player.getInventory().contains(new ItemStack(item))) return true;
        ListTag stored = HunterData.mutable(player).getList("system_inventory", Tag.TAG_COMPOUND);
        for (int index = 0; index < stored.size(); index++) if (ItemStack.of(stored.getCompound(index)).is(item)) return true;
        return false;
    }

    private DungeonLootService() {}
}
