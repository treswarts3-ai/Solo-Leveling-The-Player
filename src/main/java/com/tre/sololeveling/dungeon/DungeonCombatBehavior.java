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
    private static final String RETREAT_UNTIL = "sl_role_retreat_until";
    private static final String COMBO_STEP = "sl_role_combo_step";
    private static final String SUMMONS = "sl_role_summons";
    private static final String ELITE_PHASE = "sl_elite_phase";

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
                runRole(level, session, mob, target, definition, now);
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

    private static void runRole(ServerLevel level, DungeonSession session, Mob mob, ServerPlayer target,
                                DungeonTypes.EnemyDefinition definition, long now) {
        CompoundTag data = mob.getPersistentData();
        if (data.getLong(RECOVERY_UNTIL) > now) {
            mob.getNavigation().stop();
            return;
        }
        if (data.getLong(EXECUTE_AT) > 0L) {
            if (now >= data.getLong(EXECUTE_AT)) execute(level, session, mob, target, definition, data, now);
            return;
        }
        if (now < data.getLong(NEXT_ACTION)) return;

        double distance = Math.sqrt(mob.distanceToSqr(target));
        switch (definition.kind()) {
            case MELEE -> melee(level, mob, target, data, now, distance);
            case ASSASSIN -> assassin(level, mob, target, data, now, distance);
            case TANK -> tank(level, mob, data, now, distance);
            case RANGED -> ranged(level, mob, target, data, now, distance);
            case SUMMONER -> summoner(level, mob, target, data, now, distance);
            case ELITE -> elite(level, mob, target, data, now, distance);
        }
    }

    private static void melee(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        if (distance > 3.5D) {
            mob.getNavigation().moveTo(target, 1.05D);
            data.putLong(NEXT_ACTION, now + 12L);
            return;
        }
        boolean heavy = data.getInt(COMBO_STEP) >= 2;
        telegraph(level, mob, data, now, heavy ? "melee_heavy" : "melee_combo", heavy ? 16L : 7L,
                heavy ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK,
                heavy ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.PLAYER_ATTACK_SWEEP, heavy ? 0.85F : 1.15F);
    }

    private static void assassin(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        if (data.getLong(RETREAT_UNTIL) > now) {
            Vec3 away = mob.position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 retreat = mob.position().add(away.normalize().scale(7.0D));
            mob.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.45D);
            return;
        }
        if (distance > 12.0D) {
            mob.getNavigation().moveTo(target, 1.35D);
            data.putLong(NEXT_ACTION, now + 10L);
            return;
        }
        if (distance < 5.0D) {
            Vec3 radial = target.position().subtract(mob.position());
            Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
            if (tangent.lengthSqr() > 1.0E-6D) {
                Vec3 circle = target.position().add(tangent.normalize().scale(5.0D));
                mob.getNavigation().moveTo(circle.x, circle.y, circle.z, 1.25D);
            }
            data.putLong(NEXT_ACTION, now + 10L);
            return;
        }
        telegraph(level, mob, data, now, "assassin_lunge", 10L, ParticleTypes.CLOUD, SoundEvents.SPIDER_AMBIENT, 1.35F);
    }

    private static void tank(ServerLevel level, Mob mob, CompoundTag data, long now, double distance) {
        if (distance > 5.0D) {
            data.putLong(NEXT_ACTION, now + 14L);
            return;
        }
        telegraph(level, mob, data, now, "tank_slam", 20L, ParticleTypes.DAMAGE_INDICATOR, SoundEvents.NOTE_BLOCK_BASS.value(), 0.45F);
        ring(level, mob.position(), 4.5D, ParticleTypes.DAMAGE_INDICATOR, 28);
    }

    private static void summoner(ServerLevel level, Mob mob, ServerPlayer target, CompoundTag data, long now, double distance) {
        if (distance < 8.0D) {
            Vec3 away = mob.position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 retreat = mob.position().add(away.normalize().scale(6.0D));
            mob.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.1D);
            data.putLong(NEXT_ACTION, now + 12L);
            return;
        }
        if (data.getInt(SUMMONS) >= 2) {
            mob.getNavigation().moveTo(target, 0.85D);
            data.putLong(NEXT_ACTION, now + 30L);
            return;
        }
        telegraph(level, mob, data, now, "summoner_channel", 36L, ParticleTypes.SOUL,
                SoundEvents.EVOKER_PREPARE_SUMMON, 0.75F);
        ring(level, mob.position(), 3.0D, ParticleTypes.SOUL_FIRE_FLAME, 24);
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
        if (!data.getBoolean(ELITE_PHASE) && mob.getHealth() <= mob.getMaxHealth() * 0.5F) {
            data.putBoolean(ELITE_PHASE, true);
            data.putLong(RECOVERY_UNTIL, now + 28L);
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 0, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 0, false, false));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                    45, 0.8D, 0.8D, 0.8D, 0.04D);
            level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.25F);
            return;
        }
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

    private static void execute(ServerLevel level, DungeonSession session, Mob mob, ServerPlayer target,
                                DungeonTypes.EnemyDefinition definition, CompoundTag data, long now) {
        String action = data.getString(ACTION);
        data.putString(ACTION, "");
        data.putLong(EXECUTE_AT, 0L);
        float base = (float)Math.max(2.0D, definition.attackDamage());
        switch (action) {
            case "melee_combo" -> {
                if (validMember(level, session, target, mob, 3.5D) && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 0.62F);
                }
                data.putInt(COMBO_STEP, data.getInt(COMBO_STEP) + 1);
                data.putLong(NEXT_ACTION, now + 12L);
            }
            case "melee_heavy" -> {
                if (validMember(level, session, target, mob, 3.8D) && mob.hasLineOfSight(target)) {
                    boolean blocked = target.isBlocking() && facing(target, mob.position());
                    target.hurt(level.damageSources().mobAttack(mob), base * (blocked ? 0.25F : 1.35F));
                    if (!blocked) knock(target, mob.position(), 0.65D);
                    else level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.9F);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                        10, 0.7D, 0.35D, 0.7D, 0.01D);
                data.putLong(NEXT_ACTION, now + 70L);
                data.putLong(RECOVERY_UNTIL, now + 24L);
                data.putInt(COMBO_STEP, 0);
            }
            case "assassin_lunge" -> {
                Vec3 direction = target.position().subtract(mob.position());
                if (direction.lengthSqr() > 1.0E-6D) {
                    direction = direction.normalize();
                    mob.setDeltaMovement(direction.x * 1.15D, 0.34D, direction.z * 1.15D);
                    mob.hurtMarked = true;
                }
                if (validMember(level, session, target, mob, 3.0D) && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 1.05F);
                }
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 1, false, false));
                data.putLong(RECOVERY_UNTIL, now + 16L);
                data.putLong(RETREAT_UNTIL, now + 34L);
                data.putLong(NEXT_ACTION, now + 55L);
            }
            case "tank_slam" -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(4.5D),
                        candidate -> validMember(level, session, candidate, mob, 4.5D) && mob.hasLineOfSight(candidate))) {
                    player.hurt(level.damageSources().mobAttack(mob), base * 1.15F);
                    knock(player, mob.position(), 1.0D);
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.85F);
                level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY(), mob.getZ(), 42,
                        2.0D, 0.25D, 2.0D, 0.08D);
                data.putLong(NEXT_ACTION, now + 100L);
                data.putLong(RECOVERY_UNTIL, now + 36L);
            }
            case "ranged_shot" -> {
                if (validMember(level, session, target, mob, 20.0D) && mob.hasLineOfSight(target)) {
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
                if (validMember(level, session, target, mob, 3.4D) && mob.hasLineOfSight(target)) {
                    target.hurt(level.damageSources().mobAttack(mob), base * 1.15F);
                }
                data.putLong(NEXT_ACTION, now + 48L);
            }
            case "elite_cleave" -> {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(5.5D),
                        candidate -> validMember(level, session, candidate, mob, 5.5D) && mob.hasLineOfSight(candidate))) {
                    player.hurt(level.damageSources().mobAttack(mob), base * 1.20F);
                    knock(player, mob.position(), 0.75D);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                        20, 2.0D, 0.45D, 2.0D, 0.02D);
                data.putLong(NEXT_ACTION, now + 82L);
                data.putLong(RECOVERY_UNTIL, now + 22L);
            }
            case "summoner_channel" -> {
                String reinforcement = DungeonEnemies.enemyId(mob).startsWith("shadow_") ? "shadow_goblin" : "goblin_soldier";
                DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
                DungeonTypes.GateRank rank = template == null ? DungeonTypes.GateRank.E : template.rank();
                if (session.liveEnemyCount() < DungeonTypes.MAX_LIVE_ENEMIES - 1) {
                    LivingEntity first = DungeonEnemies.spawn(level, session, reinforcement, mob.blockPosition().offset(2, 0, 0), rank, false);
                    LivingEntity second = DungeonEnemies.spawn(level, session, reinforcement, mob.blockPosition().offset(-2, 0, 0), rank, false);
                    if (first != null || second != null) data.putInt(SUMMONS, data.getInt(SUMMONS) + 1);
                }
                data.putLong(RECOVERY_UNTIL, now + 28L);
                data.putLong(NEXT_ACTION, now + 120L);
            }
            default -> data.putLong(NEXT_ACTION, now + 20L);
        }
    }

    /** Cancels an interruptible channel and applies frontal defense for tank-role enemies. */
    public static float adjustIncomingDamage(LivingEntity victim, Entity attacker, float amount) {
        if (!DungeonEnemies.isDungeonEnemy(victim) || DungeonBoss.isBoss(victim)) return amount;
        DungeonTypes.EnemyDefinition definition = DungeonContent.enemy(DungeonEnemies.enemyId(victim));
        if (definition == null) return amount;
        CompoundTag data = victim.getPersistentData();
        if (definition.kind() == DungeonTypes.EnemyKind.SUMMONER
                && data.getString(ACTION).equals("summoner_channel")) {
            data.putString(ACTION, "");
            data.putLong(EXECUTE_AT, 0L);
            data.putLong(RECOVERY_UNTIL, victim.level().getGameTime() + 30L);
            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + 1.0D, victim.getZ(),
                        20, 0.35D, 0.55D, 0.35D, 0.02D);
            }
        }
        if (definition.kind() == DungeonTypes.EnemyKind.TANK && attacker != null
                && data.getLong(RECOVERY_UNTIL) <= victim.level().getGameTime()
                && facing(victim, attacker.position())) return amount * 0.35F;
        return amount;
    }

    private static boolean facing(LivingEntity entity, Vec3 point) {
        Vec3 toward = point.subtract(entity.position());
        if (toward.lengthSqr() < 1.0E-6D) return true;
        return entity.getLookAngle().dot(toward.normalize()) > 0.25D;
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
