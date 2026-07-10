package com.tre.sololeveling.shadow;

import com.tre.sololeveling.gameplay.ShadowHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Stable integration surface for abilities, quests, commands, and UI code. */
public final class ShadowSystemApi {
    public record UiSnapshot(int capacity, int stored, int activeLimit, int active,
                             List<ShadowStorage.Snapshot> shadows) {}

    public static UiSnapshot snapshot(ServerPlayer player) {
        List<ShadowStorage.Snapshot> shadows = ShadowStorage.snapshots(player);
        return new UiSnapshot(ShadowStorage.capacity(player), shadows.size(),
                ShadowSummoningService.activeLimit(player), ShadowSummoningService.activeCount(player), shadows);
    }

    public static void registerExtractionTarget(ServerPlayer owner, LivingEntity defeated) {
        ShadowExtractionService.record(owner, defeated);
    }

    public static boolean extract(ServerPlayer owner) { return ShadowExtractionService.extract(owner); }
    public static boolean summon(ServerPlayer owner, String shadowIdOrName) { return ShadowHandler.summon(owner, shadowIdOrName); }
    public static int summonAll(ServerPlayer owner) { return ShadowHandler.summonAll(owner); }
    public static boolean dismiss(ServerPlayer owner, String shadowIdOrName) { return ShadowSummoningService.dismiss(owner, shadowIdOrName); }
    public static int dismissAll(ServerPlayer owner) { return ShadowSummoningService.dismissAll(owner); }
    public static boolean grantShadowXp(ServerPlayer owner, String shadowIdOrName, int amount) {
        return ShadowProgressionService.addXp(owner, shadowIdOrName, amount);
    }
    public static void grantCapacity(ServerPlayer owner, int amount) { ShadowProgressionService.addCapacity(owner, amount); }

    private ShadowSystemApi() {}
}
