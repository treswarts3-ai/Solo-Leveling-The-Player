package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Common damage, movement, particle, and sound operations used by abilities. */
public final class AbilityEffects {
    public static final String TAG_ABILITY_DAMAGE = "sl_ability_damage";
    public static final String TAG_GENERATED_DAMAGE = "sl_generated_damage";
    public static final String TAG_DAMAGE_DEPTH = "sl_generated_damage_depth";
    public static final String TAG_ABILITY_ID = "sl_ability_damage_id";
    public static final String TAG_LAST_ABILITY_ID = "sl_last_ability_id";
    public static final String TAG_LAST_ABILITY_OWNER = "sl_last_ability_owner";
    public static final String TAG_LAST_ABILITY_TIME = "sl_last_ability_time";

    public static boolean dealDamage(ServerPlayer player, LivingEntity target, float damage) {
        return dealAbilityDamage(player, target, damage, "unknown");
    }

    /** Deals server-authoritative ability damage while preserving player kill credit. */
    public static boolean dealAbilityDamage(ServerPlayer player, LivingEntity target, float damage, String abilityId) {
        if (!AbilityTargeting.isValidTarget(player, target) || damage <= 0.0F) return false;
        boolean backstab = HunterData.isStealthed(player) && AbilityTargeting.isBehind(player, target);
        float resolvedDamage = backstab ? damage * 1.5F : damage;
        beginGeneratedDamage(player, abilityId, true, false);
        CompoundTag targetData = target.getPersistentData();
        targetData.putString(TAG_LAST_ABILITY_ID, AbilityDefinition.normalize(abilityId));
        targetData.putUUID(TAG_LAST_ABILITY_OWNER, player.getUUID());
        targetData.putLong(TAG_LAST_ABILITY_TIME, player.level().getGameTime());
        try {
            target.invulnerableTime = 0;
            boolean dealt = target.hurt(player.damageSources().playerAttack(player), resolvedDamage);
            if (dealt && backstab) {
                particles(player.serverLevel(), target.position().add(0, target.getBbHeight() * 0.6D, 0),
                        ParticleTypes.CRIT, 18, 0.32D);
            }
            return dealt;
        } finally {
            endGeneratedDamage(player);
        }
    }

    /** Routes equipment bonus hits through the same recursion guard as abilities. */
    public static boolean dealEquipmentDamage(LivingEntity attacker, LivingEntity target, float damage, String hookId) {
        if (attacker == null || target == null || attacker == target || !target.isAlive() || damage <= 0.0F) return false;
        if (attacker instanceof ServerPlayer player && !AbilityTargeting.isValidTarget(player, target)) return false;
        beginGeneratedDamage(attacker, "equipment_" + AbilityDefinition.normalize(hookId), false, true);
        try {
            target.invulnerableTime = 0;
            return attacker instanceof ServerPlayer player
                    ? target.hurt(player.damageSources().playerAttack(player), damage)
                    : target.hurt(attacker.damageSources().mobAttack(attacker), damage);
        } finally {
            endGeneratedDamage(attacker);
        }
    }

    private static void beginGeneratedDamage(LivingEntity source, String id, boolean ability, boolean equipment) {
        CompoundTag data = source.getPersistentData();
        int depth = Math.max(0, data.getInt(TAG_DAMAGE_DEPTH));
        data.putInt(TAG_DAMAGE_DEPTH, depth + 1);
        data.putBoolean(TAG_GENERATED_DAMAGE, true);
        if (ability) data.putBoolean(TAG_ABILITY_DAMAGE, true);
        if (equipment) data.putBoolean("sl_weapon_bonus", true);
        data.putString(TAG_ABILITY_ID, AbilityDefinition.normalize(id));
    }

    private static void endGeneratedDamage(LivingEntity source) {
        CompoundTag data = source.getPersistentData();
        int remaining = Math.max(0, data.getInt(TAG_DAMAGE_DEPTH) - 1);
        if (remaining == 0) {
            data.remove(TAG_DAMAGE_DEPTH);
            data.remove(TAG_GENERATED_DAMAGE);
            data.remove(TAG_ABILITY_DAMAGE);
            data.remove("sl_weapon_bonus");
            data.remove(TAG_ABILITY_ID);
        } else {
            data.putInt(TAG_DAMAGE_DEPTH, remaining);
        }
    }

    public static boolean generatedDamage(Entity source) {
        if (source == null) return false;
        CompoundTag data = source.getPersistentData();
        return data.getBoolean(TAG_GENERATED_DAMAGE)
                || data.getBoolean(TAG_ABILITY_DAMAGE)
                || data.getBoolean("sl_weapon_bonus");
    }

    public static String currentAbilityId(Entity source) {
        return source == null ? "" : source.getPersistentData().getString(TAG_ABILITY_ID);
    }

    public static boolean recentlyDamagedByAbility(LivingEntity target, UUID owner, String abilityId, long maximumAge) {
        CompoundTag data = target.getPersistentData();
        if (!data.hasUUID(TAG_LAST_ABILITY_OWNER) || !owner.equals(data.getUUID(TAG_LAST_ABILITY_OWNER))) return false;
        if (!AbilityDefinition.normalize(abilityId).equals(data.getString(TAG_LAST_ABILITY_ID))) return false;
        return target.level().getGameTime() - data.getLong(TAG_LAST_ABILITY_TIME) <= Math.max(0L, maximumAge);
    }

    public static void particles(ServerLevel level, Vec3 position, ParticleOptions particle, int count, double spread) {
        int boundedCount = Math.max(0, Math.min(192, count));
        double boundedSpread = Math.max(0.0D, Math.min(8.0D, spread));
        level.sendParticles(particle, position.x, position.y, position.z, boundedCount,
                boundedSpread, boundedSpread, boundedSpread, 0.12D);
    }

    public static void ring(ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int points) {
        int boundedPoints = Math.max(8, Math.min(72, points));
        double boundedRadius = Math.max(0.25D, Math.min(24.0D, radius));
        for (int i = 0; i < boundedPoints; i++) {
            double angle = Math.PI * 2.0D * i / boundedPoints;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * boundedRadius,
                    center.y,
                    center.z + Math.sin(angle) * boundedRadius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void activationBurst(ServerPlayer player) {
        particles(player.serverLevel(), player.position().add(0, 1, 0), ParticleTypes.END_ROD, 12, 0.35D);
        play(player, ModSounds.ABILITY.get(), 0.8F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    public static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS,
                Math.max(0.0F, volume), Math.max(0.1F, pitch));
    }

    public static boolean isDagger(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = stack.getItem().toString();
        return id.contains("dagger") || id.contains("fang") || id.contains("wrath") || id.contains("knight_killer");
    }

    public static boolean hasDagger(ServerPlayer player) {
        return isDagger(player.getMainHandItem()) || isDagger(player.getOffhandItem());
    }

    public static boolean hasLineOfSight(ServerPlayer player, Entity target) {
        return player != null && target != null && player.hasLineOfSight(target);
    }

    public static boolean moveIfClear(ServerPlayer player, Vec3 destination) {
        if (!isSafeDestination(player, destination)) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        return true;
    }

    /** Samples the full server path so dash abilities cannot cross solid geometry or unloaded chunks. */
    public static boolean isClearPath(ServerPlayer player, Vec3 destination) {
        Vec3 origin = player.position();
        Vec3 delta = destination.subtract(origin);
        int samples = Math.max(1, (int)Math.ceil(delta.length() * 2.0D));
        for (int index = 1; index <= samples; index++) {
            Vec3 sample = origin.add(delta.scale(index / (double)samples));
            BlockPos block = BlockPos.containing(sample);
            if (!player.serverLevel().hasChunkAt(block)
                    || !player.serverLevel().getWorldBorder().isWithinBounds(block)) return false;
            AABB moved = player.getBoundingBox().move(sample.subtract(origin));
            if (!player.serverLevel().noCollision(player, moved)) return false;
        }
        return true;
    }

    /** Applies bounded movement only when the immediate server collision sweep is clear. */
    public static boolean setSafeClampedVelocity(ServerPlayer player, Vec3 velocity,
                                                 double maximumHorizontal, double maximumVertical) {
        Vec3 step = velocity.scale(0.5D);
        AABB moved = player.getBoundingBox().move(step);
        if (!player.serverLevel().noCollision(player, moved)) return false;
        setClampedVelocity(player, velocity, maximumHorizontal, maximumVertical);
        return true;
    }

    public static boolean isSafeDestination(ServerPlayer player, Vec3 destination) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = BlockPos.containing(destination);
        if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet)) return false;
        Vec3 delta = destination.subtract(player.position());
        AABB moved = player.getBoundingBox().move(delta);
        if (!level.noCollision(player, moved)) return false;
        BlockPos support = BlockPos.containing(destination.x, destination.y - 0.1D, destination.z);
        return !level.getBlockState(support).getCollisionShape(level, support).isEmpty();
    }

    public static Vec3 safeNearTarget(ServerPlayer player, LivingEntity target, double distance) {
        Vec3 away = target.position().subtract(player.position());
        if (away.lengthSqr() < 1.0E-6D) away = target.getLookAngle();
        away = away.normalize();
        Vec3 preferred = target.position().subtract(away.scale(Math.max(1.1D, distance)));
        Vec3[] candidates = {
                preferred,
                preferred.add(0.0D, 1.0D, 0.0D),
                target.position().add(away.z * distance, 0.0D, -away.x * distance),
                target.position().add(-away.z * distance, 0.0D, away.x * distance)
        };
        for (Vec3 candidate : candidates) if (isSafeDestination(player, candidate)) return candidate;
        return null;
    }

    public static void setClampedVelocity(Entity entity, Vec3 velocity, double maximumHorizontal, double maximumVertical) {
        double horizontalLimit = Math.max(0.0D, maximumHorizontal);
        double verticalLimit = Math.max(0.0D, maximumVertical);
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double scale = horizontal > horizontalLimit && horizontal > 1.0E-6D ? horizontalLimit / horizontal : 1.0D;
        entity.setDeltaMovement(velocity.x * scale,
                Math.max(-verticalLimit, Math.min(verticalLimit, velocity.y)),
                velocity.z * scale);
        entity.hurtMarked = true;
    }

    public static boolean isBoss(Entity entity) {
        return entity != null && (entity.getPersistentData().getBoolean("sl_dungeon_boss")
                || entity.getPersistentData().getBoolean("sl_boss")
                || entity.getType().toString().contains("wither")
                || entity.getType().toString().contains("ender_dragon"));
    }

    private AbilityEffects() {
    }
}
