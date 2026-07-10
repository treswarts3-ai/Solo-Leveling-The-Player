package com.tre.sololeveling.item;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.equipment.EquipmentAbilityHooks;
import com.tre.sololeveling.equipment.EquipmentDefinition;
import com.tre.sololeveling.equipment.EquipmentItem;
import net.minecraft.network.chat.Component;
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

public class HunterWeaponItem extends SwordItem implements EquipmentItem {
    private final EquipmentDefinition definition;

    public HunterWeaponItem(Tier tier, int damage, float speed, Properties properties, EquipmentDefinition definition) {
        super(tier, damage, speed, properties);
        this.definition = definition;
    }

    @Override
    public EquipmentDefinition equipmentDefinition() { return definition; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        initializeEquipment(stack);
        if (!attacker.level().isClientSide) {
            EquipmentAbilityHooks.fireHit(definition.abilityHook(), stack, target, attacker);
            if (attacker instanceof ServerPlayer player) {
                HunterData.progressDaggerDamage(player, Math.max(1, (int)(4 + target.getArmorValue() * 0.25F)));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        initializeEquipment(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!held) return;
        if (definition.abilityHook().equals("frost_step")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false, true));
        } else if (definition.abilityHook().equals("moonshadow")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, true));
        } else if (definition.abilityHook().equals("shadow_reaper")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        appendEquipmentTooltip(stack, tooltip, flag);
    }
}
