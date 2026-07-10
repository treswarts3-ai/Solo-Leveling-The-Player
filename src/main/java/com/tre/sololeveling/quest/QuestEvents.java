package com.tre.sololeveling.quest;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.dungeon.DungeonHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        QuestCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuestManager.onLogin(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuestManager.onLogin(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) QuestManager.tick(player);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity().getPersistentData().getBoolean("sl_shadow")) return;
        QuestApi.onKill(player, event.getEntity());
    }

    @SubscribeEvent
    public static void onDungeonReward(DungeonHooks.RewardGrantedEvent event) {
        QuestApi.onDungeonCleared(event.player(), event.templateId());
    }

    private QuestEvents() {}
}
