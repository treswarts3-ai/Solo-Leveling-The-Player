package com.tre.sololeveling.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

public enum EquipmentRarity {
    COMMON(ChatFormatting.WHITE, Rarity.COMMON),
    UNCOMMON(ChatFormatting.GREEN, Rarity.UNCOMMON),
    RARE(ChatFormatting.AQUA, Rarity.RARE),
    EPIC(ChatFormatting.LIGHT_PURPLE, Rarity.EPIC),
    LEGENDARY(ChatFormatting.GOLD, Rarity.EPIC),
    MONARCH(ChatFormatting.DARK_PURPLE, Rarity.EPIC);

    private final ChatFormatting color;
    private final Rarity vanilla;

    EquipmentRarity(ChatFormatting color, Rarity vanilla) {
        this.color = color;
        this.vanilla = vanilla;
    }

    public ChatFormatting color() { return color; }
    public Rarity vanilla() { return vanilla; }
    public String translationKey() { return "rarity.sololeveling." + name().toLowerCase(); }
}
