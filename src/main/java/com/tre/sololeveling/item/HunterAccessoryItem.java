package com.tre.sololeveling.item;

import com.tre.sololeveling.equipment.EquipmentDefinition;
import com.tre.sololeveling.equipment.EquipmentItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class HunterAccessoryItem extends FunctionalItem implements EquipmentItem {
    private final EquipmentDefinition definition;

    public HunterAccessoryItem(Properties properties, EquipmentDefinition definition) {
        super(properties, Kind.ACCESSORY, "", definition.accessorySlot().storageKey());
        this.definition = definition;
    }

    @Override
    public EquipmentDefinition equipmentDefinition() { return definition; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        initializeEquipment(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        initializeEquipment(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        appendEquipmentTooltip(stack, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.sololeveling.accessory_use"));
    }
}
