package com.tre.sololeveling.item;

import com.tre.sololeveling.equipment.EquipmentConfig;
import com.tre.sololeveling.equipment.EquipmentData;
import com.tre.sololeveling.equipment.EquipmentDefinition;
import com.tre.sololeveling.equipment.EquipmentItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class EquipmentUpgradeMaterialItem extends Item {
    private final int upgradeAmount;
    private final String acquisitionKey;

    public EquipmentUpgradeMaterialItem(Properties properties, int upgradeAmount, String acquisitionKey) {
        super(properties);
        this.upgradeAmount = Math.max(1, upgradeAmount);
        this.acquisitionKey = acquisitionKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack catalyst = player.getItemInHand(hand);
        ItemStack target = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
        if (!(target.getItem() instanceof EquipmentItem equipmentItem)) {
            if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.sololeveling.upgrade_no_target").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(catalyst);
        }
        boolean enabled;
        try { enabled = EquipmentConfig.ENABLE_UPGRADES.get(); }
        catch (IllegalStateException ignored) { enabled = true; }
        if (!enabled) {
            if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.sololeveling.upgrades_disabled").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(catalyst);
        }
        EquipmentDefinition definition = equipmentItem.equipmentDefinition();
        int before = EquipmentData.upgradeLevel(target, definition);
        if (before >= definition.maxUpgrade()) {
            if (!level.isClientSide) player.sendSystemMessage(Component.translatable("message.sololeveling.upgrade_max").withStyle(ChatFormatting.GOLD));
            return InteractionResultHolder.fail(catalyst);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int goldCost = definition.upgradeCost(before);
            if (!serverPlayer.getAbilities().instabuild && com.tre.sololeveling.data.HunterData.getGold(serverPlayer) < goldCost) {
                serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] Upgrade requires " + goldCost + " gold.")
                        .withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(catalyst);
            }
            if (!serverPlayer.getAbilities().instabuild) com.tre.sololeveling.data.HunterData.setGold(serverPlayer,
                    com.tre.sololeveling.data.HunterData.getGold(serverPlayer) - goldCost);
            EquipmentData.upgrade(target, definition, upgradeAmount);
            int after = EquipmentData.upgradeLevel(target, definition);
            if (!serverPlayer.getAbilities().instabuild) catalyst.shrink(1);
            level.playSound(null, serverPlayer.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.15F);
            serverPlayer.sendSystemMessage(Component.translatable("message.sololeveling.upgrade_success",
                    target.getHoverName(), after, definition.maxUpgrade()).withStyle(ChatFormatting.AQUA));
            com.tre.sololeveling.quest.QuestApi.onEquipmentUpgraded(serverPlayer, definition.rarity().name().toLowerCase(java.util.Locale.ROOT));
        }
        return InteractionResultHolder.sidedSuccess(catalyst, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sololeveling.upgrade_material", upgradeAmount).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.sololeveling.upgrade_instructions").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sololeveling.acquisition",
                Component.translatable("acquisition.sololeveling." + acquisitionKey)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
