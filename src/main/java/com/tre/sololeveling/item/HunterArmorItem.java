package com.tre.sololeveling.item;

import com.tre.sololeveling.equipment.EquipmentDefinition;
import com.tre.sololeveling.equipment.EquipmentItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class HunterArmorItem extends ArmorItem implements EquipmentItem {
    private final EquipmentDefinition definition;

    public HunterArmorItem(ArmorMaterial material, Type type, Properties properties, EquipmentDefinition definition) {
        super(material, type, properties);
        this.definition = definition;
    }

    @Override
    public EquipmentDefinition equipmentDefinition() { return definition; }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        initializeEquipment(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        appendEquipmentTooltip(stack, tooltip, flag);
    }
}
