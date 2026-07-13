package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.shadow.ShadowAiService;
import com.tre.sololeveling.shadow.ShadowExtractionService;
import com.tre.sololeveling.shadow.ShadowProgressionService;
import com.tre.sololeveling.shadow.ShadowStorage;
import com.tre.sololeveling.shadow.ShadowSummoningService;
import com.tre.sololeveling.shadow.ShadowSystemApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/** Compatibility facade used by existing ability, event, command, and UI code. */
public final class ShadowHandler {
    public static void recordImprint(ServerPlayer killer, LivingEntity victim) {
        ShadowExtractionService.record(killer, victim);
    }

    public static boolean extract(ServerPlayer player) { return ShadowExtractionService.extract(player); }
    public static boolean createExtractionTarget(ServerPlayer player, EntityType<?> type) {
        return ShadowExtractionService.createTestTarget(player, type);
    }

    public static boolean summonFirst(ServerPlayer player) {
        ListTag records = ShadowStorage.normalize(player);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            if (!record.getBoolean("active") && ShadowProgressionService.restored(player, record)
                    && ShadowSummoningService.summon(player, record.getString("record_id"))) return true;
        }
        return false;
    }

    public static boolean summon(ServerPlayer player, String selector) {
        CompoundTag record = ShadowStorage.findMutable(player, selector);
        return record != null && ShadowProgressionService.restored(player, record)
                && ShadowSummoningService.summon(player, record.getString("record_id"));
    }

    public static int summonAll(ServerPlayer player) {
        int count = 0;
        ListTag records = ShadowStorage.normalize(player);
        for (int i = 0; i < records.size()
                && ShadowSummoningService.activeCount(player) < ShadowSummoningService.activeLimit(player); i++) {
            CompoundTag record = records.getCompound(i);
            if (!record.getBoolean("active") && ShadowProgressionService.restored(player, record)
                    && ShadowSummoningService.summon(player, record.getString("record_id"))) count++;
        }
        return count;
    }

    public static boolean dismiss(ServerPlayer player, String selector) {
        return ShadowSummoningService.dismiss(player, selector);
    }

    public static void dismissAll(ServerPlayer player) { ShadowSummoningService.dismissAll(player); }

    public static boolean addTestShadow(ServerPlayer player, EntityType<?> type) {
        return ShadowStorage.add(player, type, "Test Shadow " + (ShadowStorage.size(player) + 1), ShadowStorage.Rank.NORMAL, 1);
    }

    public static boolean setShadowLevel(ServerPlayer player, String selector, int level) {
        return ShadowStorage.setLevel(player, selector, level);
    }

    public static boolean setShadowRank(ServerPlayer player, String selector, String rank) {
        return ShadowStorage.setRank(player, selector, ShadowStorage.Rank.parse(rank));
    }

    public static List<ShadowStorage.Snapshot> snapshots(ServerPlayer player) {
        return ShadowStorage.snapshots(player);
    }

    public static ShadowSystemApi.UiSnapshot uiSnapshot(ServerPlayer player) {
        return ShadowSystemApi.snapshot(player);
    }

    public static boolean exchange(ServerPlayer player) {
        return exchange(player, true);
    }

    public static boolean exchangeValidated(ServerPlayer player) {
        return exchange(player, false);
    }

    public static boolean exchangeValidated(ServerPlayer player, Entity preparedTarget) {
        return exchangeWith(player, preparedTarget, false);
    }

    /** Returns the deterministic same-dimension destination used for cast preview and validation. */
    public static Entity exchangeTarget(ServerPlayer player) {
        for (UUID id : ShadowSummoningService.activeIds(player)) {
            Entity entity = ShadowSummoningService.findEntity(player.getServer(), id);
            if (entity == null || entity.level() != player.level() || !entity.isAlive()
                    || !ShadowSummoningService.isOwnedBy(entity, player.getUUID())) continue;
            if (safeDestination(player.serverLevel(), player, entity.getX(), entity.getY(), entity.getZ())
                    && safeDestination(player.serverLevel(), entity, player.getX(), player.getY(), player.getZ())) {
                return entity;
            }
        }
        return null;
    }

    private static boolean exchange(ServerPlayer player, boolean manageCost) {
        if (manageCost && (!HunterData.hasSkill(player, "shadow_exchange") || !HunterData.cooldownReady(player, "shadow_exchange"))) return false;
        return exchangeWith(player, exchangeTarget(player), manageCost);
    }

    private static boolean exchangeWith(ServerPlayer player, Entity entity, boolean manageCost) {
        if (entity != null && entity.level() == player.level() && entity.isAlive()
                && ShadowSummoningService.isOwnedBy(entity, player.getUUID())) {
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
            if (!safeDestination(player.serverLevel(), player, ex, ey, ez)
                    || !safeDestination(player.serverLevel(), entity, px, py, pz)) {
                player.sendSystemMessage(Component.literal("[SYSTEM] Shadow Exchange destination became obstructed.")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            if (manageCost && !HunterData.spendMana(player, 50)) return false;
            player.teleportTo(ex, ey, ez);
            entity.teleportTo(px, py, pz);
            player.fallDistance = 0.0F;
            entity.fallDistance = 0.0F;
            if (manageCost) HunterData.setCooldown(player, "shadow_exchange", 20 * 45);
            player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, ex, ey + 1.0D, ez,
                    80, 0.8D, 1.0D, 0.8D, 0.3D);
            HunterData.sync(player);
            return true;
        }
        player.sendSystemMessage(Component.literal("[SYSTEM] No safe active shadow destination found.").withStyle(ChatFormatting.RED));
        return false;
    }

    public static void cycleMode(ServerPlayer player) { ShadowAiService.cycleMode(player); }
    public static void setMode(ServerPlayer player, int value) { ShadowAiService.setMode(player, value); }

    public static void tick(ServerPlayer owner) {
        if (owner.tickCount % 100 == 0) ShadowExtractionService.cleanup(owner);
        ShadowAiService.tick(owner);
        ShadowProgressionService.tick(owner);
    }

    public static boolean shouldCancelAttack(LivingEntity victim, Entity attacker) {
        return ShadowAiService.shouldCancelAttack(victim, attacker);
    }

    public static void onShadowKill(ServerPlayer owner, Entity shadow, LivingEntity victim) {
        ShadowProgressionService.onShadowKill(owner, shadow, victim);
    }

    public static void onShadowDeath(ServerPlayer owner, Entity shadow) {
        ShadowProgressionService.onShadowDeath(owner, shadow);
    }

    public static void reconcile(ServerPlayer player) {
        ShadowStorage.normalize(player);
        ShadowSummoningService.reconcile(player);
        ShadowExtractionService.cleanup(player);
        HunterData.sync(player);
    }

    public static void clearRecords(ServerPlayer player) {
        ShadowSummoningService.dismissAll(player);
        ShadowStorage.clear(player);
        HunterData.setImprints(player, new ListTag());
        HunterData.sync(player);
    }

    private static boolean safeDestination(ServerLevel level, Entity entity, double x, double y, double z) {
        BlockPos feet = BlockPos.containing(x, y, z);
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        AABB moved = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
        return level.noCollision(entity, moved)
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private ShadowHandler() {}
}
