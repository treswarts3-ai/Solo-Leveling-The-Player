package com.tre.sololeveling.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemAcquisitionCatalog {
    public static Component line(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath();
        return Component.translatable("tooltip.sololeveling.acquisition",
                Component.translatable("acquisition.sololeveling." + key(path))).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static String key(String id) {
        if (id.contains("potion")) return "crafted";
        if (id.contains("dungeon_key")) return "quest_reward";
        if (id.contains("castle") && id.contains("key")) return "demon_castle";
        if (id.contains("rune")) return "rune_drop";
        if (id.contains("black_heart")) return "main_quest";
        if (id.contains("random_box")) return "system_reward";
        if (id.contains("holy_water")) return "demon_castle";
        if (id.contains("teleportation")) return "system_reward";
        if (id.contains("mana_crystal") || id.contains("essence") || id.contains("venom")
                || id.contains("fang") || id.contains("fragment") || id.contains("steel")
                || id.contains("silk") || id.contains("relic")) return "dungeon_loot";
        return "quest_reward";
    }

    private ItemAcquisitionCatalog() {}
}
