package com.tre.sololeveling.dungeon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/**
 * Small server-side role layer for dungeon mobs. It operates only on entities already tracked by a
 * dungeon session, so it never scans an entire level and does not own session or reward state.
 */
public final class DungeonCombatBehavior {
    private static final String NEXT_ACTION = "sl_role_next_action";
    private static final String EXECUTE_AT = "sl_role_execute_at";
    private static final String ACTION = "sl_role_action";
    private static final String ELITE_MODE = "sl_elite_mode";
    private static final String RECOVERY_UNTIL = "sl_role_recovery_until";

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 4L != 0L) return;
        for (DungeonSession session : DungeonSavedData.get(server).sessions().values()) {
            if (session.isTerminal() || !session.arenaBuilt()) continue;
            ServerLevel level = server.getLevel(session.dungeonDimension());
            if (level == null) continue;
            int processed = 0;
            for (UUID id : session.trackedEntities()) {
                if (processed++ >= DungeonTypes.MAX_LIVE_ENEMIES) break;
                Entity entity = level.getEntity(id);
                if (!(entity instanceof Mob mob) || !mob.isAlive() || DungeonBoss.isBoss(mob)) continue;
                DungeonTypes.EnemyDefinition definition = DungeonContent.enemy(DungeonEnemies.enemyId(mob));
                if (definition == null) continue;
                ServerPlayer target = selectTarget(server, level, session, mob);
                if (target == null) {
                    mob.setTarget(null);
                    continue;
                }
                mob.setTarget(target);
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                runRole(level, mob, target, definition, now);
            }
        }
    }

    private static ServerPlayer selectTarget(MinecraftServer server, ServerLevel level, DungeonSession session, Mob mob) {
        if (mob.getTarget() instanceof ServerPlayer current && validMember(level, session, current, mob, 36.0D)) return current;
        return session.members().stream()
                .map(id -> server.getPlayerList().getPlayer(id))
                .filter(player -> validMember(level, session, player, mob, 36.0D))
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    private static boolean validMember(ServerLevel level, DungeonSession session, ServerPlayer player, Mob mob, double range) {
        return player != null && player.isAlive() && !player.isSpectator() && player.level() == level
                && session.contains(player.getUUID()) && mob.distanceToSqr(player) <= range * range;
    }

    private static void runRole(ServerLevel level, Mob mob, ServerPlayer target,
                                DungeonTypes.EnemyDefinition definition, long now) {
        CompoundTag data = mob.getPersistentData();
        if (data.getLong(RECOVERY_UNTIL) > now) {
            mob.getNavigation().stop();
            return;
        }
        if (data.getLong(EXECUTE_AT) > 0L) {
            if (now >= data.getLong(EXECUTE_AT)) execute(level, mob, target, definition, data, now);
            return;
        }
        if (now < data.getLong(NEXT_ACTION)) return;

        double distance = Math.sqrt(mob.distanceToSqr(target));
        switch (definition.kind()) {
            case MELEE -> melee(level, mob, target, data, now, distance);
            case FAST -> fast(level, mob, target, data, now, distance);
            case TANK -> tank(level, mob, data, now, distance);
            case RANGED -> ranged(level, mob, target, data, now, distance);
            case ELITE -> elite(level, mob, target, data, now, distance);
        }
    }

    private static void melee(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        if (distance > 3.5D) {
            mob.getNavigation().moveTo(target, 1.05D);
            data.putLong(NEXT_ACTION, now + 12L);
            return;
        }
        telegraph(level, mob, data, now, "melee_heavy", 12L, ParticleTypes.CRIT, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.85F);
    }

    private static void fast(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        if (distance > 12.0D) {
            mob.getNavigation().moveTo(target, 1.35D);
            data.putLong(NEXT_ACTION, now + 10L);
            return;
        }
        if (distance < 2.2D) {
            mob.getNavigation().stop();
            data.putLong(RECOVERY_UNTIL, now + 12L);
            data.putLong(NEXT_ACTION, now + 35L);
            return;
        }
        telegraph(level, mob, data, now, "fast_lunge", 8L, ParticleTypes.CLOUD, SoundEvents.SPIDER_AMBIENT, 1.35F);
    }

    private static void tank(ServerLevel level, Mob mob, CompoundTag data, long now, double distance) {
        if (distance > 5.0D) {
            data.putLong(NEXT_ACTION, now + 14L);
            return;
        }
        telegraph(level, mob, data, now, "tank_slam", 20L, ParticleTypes.DAMAGE_INDICATOR, SoundEvents.NOTE_BLOCK_BASS.value(), 0.45F);
        ring(level, mob.position(), 4.5D, ParticleTypes.DAMAGE_INDICATOR, 28);
    }

    private static void ranged(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        if (distance < 6.0D) {
            Vec3 away = mob.position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 retreat = mob.position().add(away.normalize().scale(5.0D));
            mob.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.2D);
            data.putLong(NEXT_ACTION, now + 14L);
            return;
        }
        if (distance > 18.0D || !mob.hasLineOfSight(target)) {
            mob.getNavigation().moveTo(target, 1.0D);
            data.putLong(NEXT_ACTION, now + 12L);
            return;
        }
        mob.getNavigation().stop();
        telegraph(level, mob, data, now, "ranged_shot", 14L, ParticleTypes.ENCHANT, SoundEvents.NOTE_BLOCK_HAT.value(), 0.9F);
    }

    private static void elite(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        boolean dash = !data.getBoolean(ELITE_MODE);
        data.putBoolean(ELITE_MODE, dash);
        if (dash && distance > 3.0D && distance <= 14.0D) {
            telegraph(level, mob, data, now, "elite_dash", 10L, ParticleTypes.SOUL_FIRE_FLAME, SoundEvents.VINDICATOR_AMBIENT, 0.75F);
        } else if (distance <= 6.0D) {
            telegraph(level, mob, data, now, "elite_cleave", 18L, ParticleTypes.WITCH, SoundEvents.NOTE_BLOCK_BASS.value(), 0.65F);
            ring(level, mob.position(), 5.5D, ParticleTypes.WITCH, 32);
        } else {
            mob.getNavigation().moveTo(target, 1.15D);
            data.putLong(NEXT_ACTION, now + 12L);
        }
    }

    private static void telegraph(ServerLevel level, Mob mob, CompoundTag data, long now, String action,
                                  long delay, net.minecraft.core.particles.ParticleOptions particle,
                                  net.minecraft.sounds.SoundEvent sound, float pitch) {
        data.putString(ACTION, action);
        data.putLong(EXECUTE_AT, now + delay);
        mob.getNavigation().stop();
        level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.65D, mob.getZ(),
                14, 0.45D, 0.55D, 0.45D, 0.02D);
        level.playSound(null, mob.blockPosition(), sound, SoundSource.HOSTILE, 0.9F, pitch);
    }

    private static void execute(ServerLevel level, Mob mob, ServerPlayer target,
                                DungeonTypes.EnemyDefinition definition, CompoundTag data, long now) {
        String action = data.getString(ACTION);
        data.putString(ACTION, "");
        data.putLong(EXECUTE_AT, 0L);
        float base = (float)Math.max(2.0D, definition.attackDamage());
        switch (action) {
            case "melee_heavy" -> {
                if (mob.distanceToSqr(target) <= 3.8D * 3.8D && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 1.35F);
                    knock(target, mob.position(), 0.65D);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                        10, 0.7D, 0.35D, 0.7D, 0.01D);
                data.putLong(NEXT_ACTION, now + 70L);
            }
            case "fast_lunge" -> {
                Vec3 direction = target.position().subtract(mob.position());
                if (direction.lengthSqr() > 1.0E-6D) {
                    direction = direction.normalize();
                    mob.setDeltaMovement(direction.x * 1.15D, 0.34D, direction.z * 1.15D);
                    mob.hurtMarked = true;
                }
                if (mob.distanceToSqr(target) <= 3.0D * 3.0D && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 1.05F);
                }
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 1, false, false));
                data.putLong(RECOVERY_UNTIL, now + 16L);
                data.putLong(NEXT_ACTION, now + 55L);
            }
            case "tank_slam" -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(4.5D),
                        candidate -> candidate.isAlive() && !candidate.isSpectator() && mob.hasLineOfSight(candidate))) {
                    player.hurt(level.damageSources().mobAttack(mob), base * 1.15F);
                    knock(player, mob.position(), 1.0D);
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.85F);
                level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY(), mob.getZ(), 42,
                        2.0D, 0.25D, 2.0D, 0.08D);
                data.putLong(NEXT_ACTION, now + 100L);
            }
            case "ranged_shot" -> {
                if (mob.distanceToSqr(target) <= 20.0D * 20.0D && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 0.80F);
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                            10, 0.3D, 0.5D, 0.3D, 0.03D);
                    level.playSound(null, mob.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 0.9F, 0.85F);
                }
                data.putLong(NEXT_ACTION, now + 58L);
            }
            case "elite_dash" -> {
                Vec3 direction = target.position().subtract(mob.position());
                if (direction.lengthSqr() > 1.0E-6D) {
                    direction = direction.normalize();
                    mob.setDeltaMovement(direction.x * 1.35D, 0.22D, direction.z * 1.35D);
                    mob.hurtMarked = true;
                }
                if (mob.distanceToSqr(target) <= 3.4D * 3.4D && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 1.15F);
                }
                data.putLong(NEXT_ACTION, now + 48L);
            }
            case "elite_cleave" -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(5.5D),
                        candidate -> candidate.isAlive() && !candidate.isSpectator() && mob.hasLineOfSight(candidate))) {
                    player.hurt(level.damageSources().mobAttack(mob), base * 1.20F);
                    knock(player, mob.position(), 0.75D);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                        20, 2.0D, 0.45D, 2.0D, 0.02D);
                data.putLong(NEXT_ACTION, now + 82L);
            }
            default -> data.putLong(NEXT_ACTION, now + 20L);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius,
                             net.minecraft.core.particles.ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y + 0.12D,
                    center.z + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void knock(LivingEntity target, Vec3 source, double force) {
        Vec3 delta = target.position().subtract(source);
        if (delta.lengthSqr() < 1.0E-6D) delta = new Vec3(1.0D, 0.0D, 0.0D);
        delta = delta.normalize().scale(Math.min(1.25D, force));
        target.push(delta.x, Math.min(0.55D, force * 0.40D), delta.z);
        target.hurtMarked = true;
    }

    private DungeonCombatBehavior() {}
}
