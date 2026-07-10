package com.tre.sololeveling.shadow;

import com.tre.sololeveling.gameplay.ShadowHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Multiplayer lifecycle and orphan cleanup that complements the shared event handler. */
@Mod.EventBusSubscriber(modid = "sololeveling", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowLifecycleEvents {
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ShadowHandler.reconcile(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ShadowHandler.reconcile(player);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof ServerPlayer owner) ShadowHandler.dismissAll(owner);

        if (ShadowSummoningService.isShadow(victim)) {
            UUID ownerId = ShadowSummoningService.ownerId(victim);
            ServerPlayer owner = ownerId == null || victim.level().getServer() == null ? null
                    : victim.level().getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) ShadowHandler.onShadowDeath(owner, victim);
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker != null && ShadowSummoningService.isShadow(attacker)) {
            UUID ownerId = ShadowSummoningService.ownerId(attacker);
            ServerPlayer owner = ownerId == null || attacker.level().getServer() == null ? null
                    : attacker.level().getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) ShadowHandler.onShadowKill(owner, attacker, victim);
        }
    }

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !ShadowSummoningService.isShadow(event.getEntity())) return;
        Entity shadow = event.getEntity();
        UUID ownerId = ShadowSummoningService.ownerId(shadow);
        UUID recordId = ShadowSummoningService.recordId(shadow);
        if (ownerId == null || recordId == null || shadow.level().getServer() == null) {
            event.setCanceled(true);
            return;
        }
        ServerPlayer owner = shadow.level().getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || ShadowStorage.findMutable(owner, recordId.toString()) == null) {
            event.setCanceled(true);
            return;
        }
        if (ShadowSummoningService.activeCount(owner) >= ShadowSummoningService.activeLimit(owner)
                && !ShadowSummoningService.activeIds(owner).contains(shadow.getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) ShadowHandler.dismissAll(player);
    }

    private ShadowLifecycleEvents() {}
}
