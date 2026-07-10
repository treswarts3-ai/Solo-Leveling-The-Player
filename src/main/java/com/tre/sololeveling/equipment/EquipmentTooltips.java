package com.tre.sololeveling.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public final class EquipmentTooltips {
    public static void append(ItemStack stack, EquipmentDefinition definition, List<Component> tooltip, TooltipFlag flag) {
        int upgrade = EquipmentData.upgradeLevel(stack, definition);
        tooltip.add(Component.translatable("tooltip.sololeveling.rarity",
                Component.translatable(definition.rarity().translationKey()).withStyle(definition.rarity().color()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sololeveling.category_slot",
                Component.translatable(definition.category().translationKey()),
                Component.translatable(definition.slot().translationKey())).withStyle(ChatFormatting.DARK_AQUA));
        if (definition.accessorySlot() != AccessorySlot.NONE) {
            tooltip.add(Component.translatable("tooltip.sololeveling.accessory_slot",
                    Component.translatable(definition.accessorySlot().translationKey())).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.sololeveling.upgrade", upgrade, definition.maxUpgrade())
                .withStyle(upgrade >= definition.maxUpgrade() ? ChatFormatting.GOLD : ChatFormatting.AQUA));

        double multiplier = safeUpgradeMultiplier();
        for (StatBonus bonus : definition.bonuses()) {
            tooltip.add(statLine(bonus.stat(), bonus.valueAt(upgrade, multiplier), ChatFormatting.GREEN));
        }

        if (!definition.setId().isBlank()) {
            tooltip.add(Component.translatable("tooltip.sololeveling.set_piece",
                    Component.translatable("set.sololeveling." + definition.setId())).withStyle(ChatFormatting.LIGHT_PURPLE));
            EquipmentCatalog.findSet(definition.setId()).ifPresent(set -> {
                for (StatBonus bonus : set.bonuses()) {
                    tooltip.add(Component.literal("  ").append(statLine(bonus.stat(), bonus.baseValue(), ChatFormatting.DARK_PURPLE)));
                }
            });
        }
        if (!definition.abilityHook().isBlank() && !definition.abilityHook().equals("none")) {
            tooltip.add(Component.translatable("tooltip.sololeveling.ability",
                    Component.translatable(definition.abilityTranslationKey())).withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.add(Component.translatable("tooltip.sololeveling.acquisition",
                Component.translatable(definition.acquisitionTranslationKey())).withStyle(ChatFormatting.DARK_GRAY));
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable("tooltip.sololeveling.equipment_id", definition.id())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static Component statLine(EquipmentStat stat, double value, ChatFormatting color) {
        String formatted = format(stat, value);
        return Component.translatable("tooltip.sololeveling.stat",
                formatted, Component.translatable(stat.translationKey())).withStyle(color);
    }

    private static String format(EquipmentStat stat, double value) {
        double displayed = stat.isPercentage() ? value * 100.0D : value;
        if (Math.abs(displayed - Math.rint(displayed)) < 0.0001D) {
            return String.format(Locale.ROOT, "%+.0f%s", displayed, stat.isPercentage() ? "%" : "");
        }
        return String.format(Locale.ROOT, "%+.1f%s", displayed, stat.isPercentage() ? "%" : "");
    }

    private static double safeUpgradeMultiplier() {
        try { return EquipmentConfig.UPGRADE_BONUS_MULTIPLIER.get(); }
        catch (IllegalStateException ignored) { return 1.0D; }
    }

    private EquipmentTooltips() {}
}
