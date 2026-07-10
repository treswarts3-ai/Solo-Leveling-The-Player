package com.tre.sololeveling.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public interface EquipmentItem {
    EquipmentDefinition equipmentDefinition();

    default void initializeEquipment(ItemStack stack) {
        EquipmentData.initialize(stack, equipmentDefinition());
    }

    default void appendEquipmentTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        EquipmentTooltips.append(stack, equipmentDefinition(), tooltip, flag);
    }
}
