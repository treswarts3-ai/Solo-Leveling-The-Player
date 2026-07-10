package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.config.ModConfigs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/** Bounded, same-dimension targeting helpers. No helper performs a global entity scan. */
public final class AbilityTargeting {
    public static Entity rayTarget(ServerPlayer player, double maximumRange) {
        double range = Math.max(0.0D, maximumRange);
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                entity -> isValidTarget(player, entity), range * range);
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
                entity -> isValidTarget(player, entity) && extraFilter.test(entity));
    }

    public static boolean isValidTarget(ServerPlayer player, Entity entity) {
        if (entity == null || entity == player || !entity.isAlive() || !entity.isPickable()) return false;
        if (entity.level() != player.level()) return false;
        if (!player.serverLevel().hasChunkAt(entity.blockPosition())) return false;
        if (entity.getPersistentData().getBoolean("sl_shadow")) return false;
        if (entity instanceof Player targetPlayer) {
            if (!ModConfigs.PVP_ABILITIES.get()) return false;
            if (targetPlayer.isSpectator() || targetPlayer.isCreative()) return false;
            if (player.getTeam() != null && player.getTeam() == targetPlayer.getTeam()) return false;
        }
        return true;
    }

    public static boolean inFrontCone(ServerPlayer player, Entity target, double minimumDot) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6D) return true;
        return player.getLookAngle().normalize().dot(toTarget.normalize()) >= minimumDot;
    }

    private AbilityTargeting() {
    }
}
