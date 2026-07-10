package com.tre.sololeveling.event;

import com.tre.sololeveling.command.SoloLevelingCommands;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.QuestHandler;
import com.tre.sololeveling.gameplay.AbilityHandler;
import com.tre.sololeveling.gameplay.PassiveHandler;
import com.tre.sololeveling.gameplay.ProgressionHandler;
import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.gameplay.ShadowHandler;
import com.tre.sololeveling.quest.QuestApi;
import com.tre.sololeveling.quest.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.event.TickEvent;

public final class CommonEvents {
    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) { SoloLevelingCommands.register(event.getDispatcher()); }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER || !(event.player instanceof ServerPlayer player)) return;
        HunterData.tick(player);
        AbilityHandler.tick(player);
        PassiveHandler.tick(player);
        QuestHandler.tick(player);
        ShadowHandler.tick(player);
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { QuestManager.onLogin(player); HunterData.sync(player); }
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { QuestManager.onLogin(player); HunterData.sync(player); }
    }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PassiveHandler.breakStealth(player);
            AbilityHandler.cancel(player);
            ShadowHandler.dismissAll(player);
            HunterData.sync(player);
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PassiveHandler.breakStealth(player);
            AbilityHandler.cancel(player);
            ShadowHandler.dismissAll(player);
        }
    }

    @SubscribeEvent
    public void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        if (!event.isWasDeath() || ModConfigs.PRESERVE_ON_DEATH.get()) HunterData.copy(event.getOriginal(), event.getEntity());
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.getPersistentData().getBoolean("sl_shadow")) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player && victim != player) {
            double attack = victim.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 2.0D : victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
            int xp = Math.max(5, (int)(victim.getMaxHealth() * 2.0D + victim.getArmorValue() * 3.0D + attack * 4.0D));
            if (victim.getPersistentData().getBoolean("sl_penalty_mob")) xp = 0;
            if (xp > 0) HunterData.addXp(player, (int)Math.max(1, Math.round(xp * ModConfigs.XP_MULTIPLIER.get())));
            QuestHandler.onKill(player, victim);
            ShadowHandler.recordImprint(player, victim);
        }
    }



    @SubscribeEvent
    public void itemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuestApi.onCollected(player, event.getItem().getItem());
    }

    @SubscribeEvent
    public void attack(LivingAttackEvent event) {
        if (ShadowHandler.shouldCancelAttack(event.getEntity(), event.getSource().getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && HunterData.isAwakened(victim)) {
            HunterData.recordCombat(victim);
            PassiveHandler.breakStealth(victim);
            if (HunterData.hasSkill(victim, "tenacity") && victim.getHealth() <= victim.getMaxHealth() * 0.30F) {
                event.setAmount(event.getAmount() * 0.50F);
            }
            ProgressionHandler.startEmergency(victim);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && HunterData.isAwakened(attacker)) {
            HunterData.recordCombat(attacker);
            PassiveHandler.breakStealth(attacker);
            String held = attacker.getMainHandItem().getItem().toString();
            boolean dagger = held.contains("dagger") || held.contains("fang") || held.contains("wrath");
            if (dagger && HunterData.hasSkill(attacker, "advanced_dagger_techniques")
                    && !attacker.getPersistentData().getBoolean("sl_ability_damage")
                    && !attacker.getPersistentData().getBoolean("sl_weapon_bonus")) {
                event.setAmount(event.getAmount() * 1.33F);
            }
        }
    }

    @SubscribeEvent
    public void drops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("sl_shadow")) event.setCanceled(true);
    }

    @SubscribeEvent
    public void experience(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("sl_shadow")) event.setDroppedExperience(0);
    }
}
