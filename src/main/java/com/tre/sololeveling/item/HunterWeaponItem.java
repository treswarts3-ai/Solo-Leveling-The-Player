package com.tre.sololeveling.item;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class HunterWeaponItem extends SwordItem {
    private final String rank;
    private final String special;

    public HunterWeaponItem(Tier tier, int damage, float speed, Properties properties, String rank, String special) {
        super(tier, damage, speed, properties);
        this.rank = rank;
        this.special = special;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            String id = stack.getItem().toString();
            if (id.contains("venom_fang")) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            } else if (id.contains("knight_killer") && target.getArmorValue() > 0) {
                bonusDamage(attacker, target, Math.min(5.0F, 1.0F + target.getArmorValue() * 0.12F));
            } else if (id.contains("demon_kings_longsword")) {
                target.setSecondsOnFire(5);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                if (attacker.level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.03D);
            }
            if (attacker instanceof ServerPlayer player) {
                if (id.contains("barukas_dagger")) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                if (id.contains("kamishs_wrath")) bonusDamage(player, target, Math.min(12.0F, HunterData.getStat(player, "strength") * 0.10F));
                if (isPaired(player, "demon_kings_dagger_left", "demon_kings_dagger_right")) bonusDamage(player, target, 6.0F);
                if (isPaired(player, "kamishs_wrath_left", "kamishs_wrath_right")) bonusDamage(player, target, 8.0F);
                HunterData.progressDaggerDamage(player, Math.max(1, (int)(4 + target.getArmorValue() * 0.25F)));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!held) return;
        String id = stack.getItem().toString();
        if (id.contains("barukas_dagger")) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false, true));
    }

    private static boolean isPaired(ServerPlayer player, String left, String right) {
        String main = player.getMainHandItem().getItem().toString();
        String off = player.getOffhandItem().getItem().toString();
        return (main.contains(left) && off.contains(right)) || (main.contains(right) && off.contains(left));
    }

    private static void bonusDamage(LivingEntity attacker, LivingEntity target, float amount) {
        if (amount <= 0.0F) return;
        attacker.getPersistentData().putBoolean("sl_weapon_bonus", true);
        try { target.hurt(attacker.damageSources().mobAttack(attacker), amount); }
        finally { attacker.getPersistentData().remove("sl_weapon_bonus"); }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(rank + " Weapon").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(special).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Scales with Strength and Agility").withStyle(ChatFormatting.GRAY));
    }
}
