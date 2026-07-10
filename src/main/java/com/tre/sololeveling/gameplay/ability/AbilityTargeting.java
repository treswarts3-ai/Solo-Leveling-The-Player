package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.config.ModConfigs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Bounded, same-dimension targeting helpers. No helper performs a global entity scan. */
public final class AbilityTargeting {
    private static final int MAX_AREA_TARGETS = 64;

    public static Entity rayTarget(ServerPlayer player, double maximumRange) {
        double range = Math.max(0.0D, Math.min(32.0D, maximumRange));
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        HitResult blockHit = player.pick(range, 0.0F, false);
        double visibleDistanceSqr = blockHit.getType() == HitResult.Type.MISS
                ? range * range
                : Math.min(range * range, start.distanceToSqr(blockHit.getLocation()));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                entity -> isValidTarget(player, entity), visibleDistanceSqr);
        return hit == null ? null : hit.getEntity();
    }

    public static LivingEntity livingRayTarget(ServerPlayer player, double maximumRange) {
        Entity entity = rayTarget(player, maximumRange);
        return entity instanceof LivingEntity living ? living : null;
    }

    public static List<LivingEntity> nearbyLiving(ServerPlayer player, double radius, Predicate<LivingEntity> extraFilter) {
        double bounded = Math.max(0.0D, Math.min(32.0D, radius));
        AABB search = player.getBoundingBox().inflate(bounded);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                        entity -> isValidTarget(player, entity) && extraFilter.test(entity))
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(MAX_AREA_TARGETS)
                .toList();
    }

    public static List<LivingEntity> nearbyVisible(ServerPlayer player, double radius, Predicate<LivingEntity> extraFilter) {
        return nearbyLiving(player, radius, entity -> player.hasLineOfSight(entity) && extraFilter.test(entity));
    }

    public static boolean isValidTarget(ServerPlayer player, Entity entity) {
        if (entity == null || entity == player || !entity.isAlive()) return false;
        if (!entity.isPickable() && !(entity instanceof ItemEntity)) return false;
        if (entity.level() != player.level()) return false;
        if (!player.serverLevel().hasChunkAt(entity.blockPosition())) return false;
        if (isProtectedShadow(entity, player.getUUID())) return false;
        if (entity.getPersistentData().getBoolean("sl_shadow")) return false;
        if (player.isAlliedTo(entity) || entity.isAlliedTo(player)) return false;
        if (entity instanceof TamableAnimal pet && player.getUUID().equals(pet.getOwnerUUID())) return false;
        if (entity instanceof Player targetPlayer) {
            if (!ModConfigs.PVP_ABILITIES.get()) return false;
            if (targetPlayer.isSpectator() || targetPlayer.isCreative()) return false;
            if (player.getTeam() != null && player.getTeam().equals(targetPlayer.getTeam())) return false;
        }
        return true;
    }

    public static boolean isProtectedShadow(Entity entity, UUID caster) {
        if (entity == null || !entity.getPersistentData().getBoolean("sl_shadow")) return false;
        if (!entity.getPersistentData().hasUUID("sl_owner")) return true;
        return caster == null || caster.equals(entity.getPersistentData().getUUID("sl_owner"));
    }

    public static boolean inFrontCone(ServerPlayer player, Entity target, double minimumDot) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6D) return true;
        return player.getLookAngle().normalize().dot(toTarget.normalize()) >= minimumDot;
    }

    private AbilityTargeting() {
    }
}
