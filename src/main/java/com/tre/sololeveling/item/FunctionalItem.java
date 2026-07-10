package com.tre.sololeveling.item;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModItems;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class FunctionalItem extends Item {
    public enum Kind { STORY, HEALING, MANA, GREATER_HEALING, GREATER_MANA, RUNE, ACCESSORY, BLACK_HEART, HOLY_WATER, RANDOM_BOX, TELEPORT }
    private final Kind kind;
    private final String detail;
    private final String slotOrSkill;

    public FunctionalItem(Properties properties, Kind kind, String detail) {
        this(properties, kind, detail, "");
    }

    public FunctionalItem(Properties properties, Kind kind, String detail, String slotOrSkill) {
        super(properties);
        this.kind = kind;
        this.detail = detail;
        this.slotOrSkill = slotOrSkill;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        boolean consumed = false;
        switch (kind) {
            case HEALING -> { serverPlayer.heal(8.0F); consumed = true; }
            case GREATER_HEALING -> { serverPlayer.heal(serverPlayer.getMaxHealth()); consumed = true; }
            case MANA -> { HunterData.addMana(serverPlayer, 60); consumed = true; }
            case GREATER_MANA -> { HunterData.addMana(serverPlayer, HunterData.getMaxMana(serverPlayer)); consumed = true; }
            case HOLY_WATER -> {
                serverPlayer.heal(serverPlayer.getMaxHealth());
                serverPlayer.removeEffect(MobEffects.POISON);
                serverPlayer.removeEffect(MobEffects.WITHER);
                serverPlayer.removeEffect(MobEffects.WEAKNESS);
                serverPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                consumed = true;
            }
            case RUNE -> {
                if (HunterData.unlockSkill(serverPlayer, slotOrSkill)) {
                    serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] Skill unlocked: " + slotOrSkill).withStyle(ChatFormatting.AQUA));
                } else {
                    HunterData.addGold(serverPlayer, 100);
                    HunterData.addXp(serverPlayer, 50);
                    serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] Duplicate rune converted into 100 gold and 50 XP.").withStyle(ChatFormatting.GOLD));
                }
                consumed = true;
            }
            case ACCESSORY -> {
                HunterData.toggleAccessory(serverPlayer, slotOrSkill, stack.getItem());
                return InteractionResultHolder.success(stack);
            }
            case BLACK_HEART -> {
                HunterData.awaken(serverPlayer);
                HunterData.setBlackHeart(serverPlayer, true);
                HunterData.unlockSkill(serverPlayer, "monarch_domain");
                serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] The Black Heart has awakened.").withStyle(ChatFormatting.DARK_PURPLE));
                consumed = true;
            }
            case RANDOM_BOX -> {
                boolean cursed = stack.getItem().toString().contains("cursed_random_box");
                ItemStack reward;
                if (cursed) {
                    int roll = level.random.nextInt(3);
                    reward = roll == 0 ? new ItemStack(ModItems.ESSENCE_STONE.get(), 12)
                            : roll == 1 ? new ItemStack(ModItems.DEMON_CASTLE_KEY.get())
                            : new ItemStack(ModItems.GREATER_HEALING_POTION.get(), 4);
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 45, 1));
                    serverPlayer.hurt(serverPlayer.damageSources().magic(), Math.min(10.0F, serverPlayer.getMaxHealth() * 0.25F));
                    serverPlayer.sendSystemMessage(Component.literal("[SYSTEM] The cursed box demanded a price.").withStyle(ChatFormatting.RED));
                } else {
                    reward = level.random.nextBoolean() ? new ItemStack(ModItems.KASAKAS_VENOM_FANG.get()) : new ItemStack(ModItems.GREATER_MANA_POTION.get(), 2);
                }
                HunterData.storeSystemItem(serverPlayer, reward);
                consumed = true;
            }
            case TELEPORT -> {
                BlockPos spawn = serverPlayer.serverLevel().getSharedSpawnPos();
                serverPlayer.teleportTo(serverPlayer.serverLevel(), spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
                consumed = true;
            }
            default -> { return InteractionResultHolder.pass(stack); }
        }
        if (consumed) {
            level.playSound(null, serverPlayer.blockPosition(), ModSounds.SYSTEM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
            HunterData.sync(serverPlayer);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!detail.isBlank()) tooltip.add(Component.literal(detail).withStyle(ChatFormatting.DARK_AQUA));
        if (kind == Kind.ACCESSORY) tooltip.add(Component.translatable("tooltip.sololeveling.accessory_use").withStyle(ChatFormatting.GRAY));
        if (kind == Kind.RUNE) tooltip.add(Component.translatable("tooltip.sololeveling.rune_use", slotOrSkill).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (kind == Kind.BLACK_HEART) tooltip.add(Component.translatable("tooltip.sololeveling.bound_item").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(ItemAcquisitionCatalog.line(stack));
    }
}
