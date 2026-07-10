package com.tre.sololeveling.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Armor with System rank and set information. */
public final class HunterArmorItem extends ArmorItem {
    private final String rank;
    private final String detail;
    private final String setName;

    public HunterArmorItem(ArmorMaterial material, Type type, Properties properties, String rank, String detail, String setName) {
        super(material, type, properties);
        this.rank = rank;
        this.detail = detail;
        this.setName = setName;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(rank + " Armor").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(detail).withStyle(ChatFormatting.GRAY));
        if (!setName.isBlank()) tooltip.add(Component.literal(setName + " set piece").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
