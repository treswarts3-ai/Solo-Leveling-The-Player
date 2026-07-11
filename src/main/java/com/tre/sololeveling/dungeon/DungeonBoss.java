package com.tre.sololeveling.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DungeonBoss {
    public enum TickResult { ALIVE, MISSING }

    private static final String TAG_PHASE_TWO = "sl_boss_phase_two";
    private static final String TAG_PHASE_ADDS = "sl_boss_phase_adds";
    private static final Map<UUID, BossRuntime> RUNTIMES = new HashMap<>();

    public static LivingEntity spawn(ServerLevel level, DungeonSession session, DungeonTypes.GateRank rank) {
        if (session.bossId() != null) {
            Entity existing = level.getEntity(session.bossId());
            if (existing instanceof LivingEntity living && living.isAlive()) return living;
        }
        Ravager boss = EntityType.RAVAGER.create(level);
        if (boss == null) return null;
        BlockPos position = DungeonArena.bossCenter(session);
        boss.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 180.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.COMMAND, null, null);

        boolean subway = "abandoned_subway".equals(session.templateId());
        boolean temple = "cartenon_temple".equals(session.templateId());
        String bossName = subway ? "Subway Warden" : temple ? "Architect's Idol" : "Iron Sovereign";
        double scale = Math.max(1.0D, rank.rewardMultiplier() * 0.72D);
        double baseHealth = subway ? 180.0D : temple ? 680.0D : 520.0D;
        double baseDamage = subway ? 8.5D : temple ? 21.0D : 18.0D;
        set(boss, Attributes.MAX_HEALTH, baseHealth * scale);
        set(boss, Attributes.ATTACK_DAMAGE, baseDamage * Math.max(1.0D, scale * 0.65D));
        set(boss, Attributes.MOVEMENT_SPEED, subway ? 0.27D : temple ? 0.29D : 0.31D);
        set(boss, Attributes.ARMOR, subway ? 6.0D : temple ? 22.0D : 18.0D);
        set(boss, Attributes.KNOCKBACK_RESISTANCE, subway ? 0.65D : 0.8D);
        boss.setHealth(boss.getMaxHealth());
        boss.setCustomName(Component.literal(bossName));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        boss.getPersistentData().putBoolean(DungeonTypes.TAG_DUNGEON_ENEMY, true);
        boss.getPersistentData().putBoolean(DungeonTypes.TAG_BOSS, true);
        boss.getPersistentData().putBoolean(DungeonTypes.TAG_SHADOW_EXTRACTABLE, true);
        boss.getPersistentData().putUUID(DungeonTypes.TAG_SESSION, session.sessionId());
        boss.getPersistentData().putString(DungeonTypes.TAG_ENEMY_ID,
                subway ? "subway_warden" : temple ? "architect_idol" : "iron_sovereign");
        boss.getPersistentData().putBoolean(TAG_PHASE_TWO, false);
        boss.getPersistentData().putBoolean(TAG_PHASE_ADDS, false);
        if (!level.addFreshEntity(boss)) return null;
        session.enemySpawned(boss.getUUID());
        session.setBossId(boss.getUUID());
        RUNTIMES.put(session.sessionId(), new BossRuntime(boss, bossName));
        level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, subway ? 0.9F : 0.75F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.4D, boss.getZ(), 80, 1.5D, 1.0D, 1.5D, 0.08D);
        return boss;
    }

    public static TickResult tick(MinecraftServer server, DungeonSession session, DungeonTypes.GateRank rank) {
        if (session.bossId() == null) return TickResult.MISSING;
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) return TickResult.MISSING;
        Entity entity = level.getEntity(session.bossId());
        if (!(entity instanceof Ravager boss) || !boss.isAlive()) return TickResult.MISSING;
        String bossName = boss.getCustomName() == null ? "Dungeon Boss" : boss.getCustomName().getString();
        BossRuntime runtime = RUNTIMES.computeIfAbsent(session.sessionId(), ignored -> new BossRuntime(boss, bossName));
        runtime.tick(server, level, session, boss, rank);
        return TickResult.ALIVE;
    }

    public static boolean isBoss(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(DungeonTypes.TAG_BOSS);
    }

    public static void remove(DungeonSession session) {
        BossRuntime runtime = RUNTIMES.remove(session.sessionId());
        if (runtime != null) runtime.bar.removeAllPlayers();
    }

    public static void onDeath(DungeonSession session) {
        remove(session);
    }

    private static final class BossRuntime {
        private final ServerBossEvent bar;
        private int ticks;
        private boolean phaseTwo;
        private boolean phaseAddsSpawned;

        private BossRuntime(Ravager boss, String name) {
            bar = new ServerBossEvent(Component.literal(name), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            bar.setDarkenScreen(true);
            bar.setVisible(true);
            bar.setProgress(Math.max(0.0F, boss.getHealth() / boss.getMaxHealth()));
            phaseTwo = boss.getPersistentData().getBoolean(TAG_PHASE_TWO);
            phaseAddsSpawned = boss.getPersistentData().getBoolean(TAG_PHASE_ADDS);
        }

        private void tick(MinecraftServer server, ServerLevel level, DungeonSession session, Ravager boss, DungeonTypes.GateRank rank) {
            ticks++;
            bar.setProgress(Math.max(0.0F, boss.getHealth() / boss.getMaxHealth()));
            for (UUID member : session.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(member);
                if (player != null && player.level() == level && player.distanceToSqr(boss) < 80.0D * 80.0D) bar.addPlayer(player);
                else if (player != null) bar.removePlayer(player);
            }
            if (!phaseTwo && boss.getHealth() <= boss.getMaxHealth() * 0.5F) enterPhaseTwo(server, level, session, boss, rank);
            if (!phaseTwo) phaseOne(level, session, boss, rank);
            else phaseTwo(level, session, boss, rank);
        }

        private void enterPhaseTwo(MinecraftServer server, ServerLevel level, DungeonSession session,
                                   Ravager boss, DungeonTypes.GateRank rank) {
            phaseTwo = true;
            boss.getPersistentData().putBoolean(TAG_PHASE_TWO, true);
            boss.setGlowingTag(true);
            level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.65F);
            level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.35F);
            level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 1.0D, boss.getZ(), 12, 1.5D, 0.6D, 1.5D, 0.05D);
            level.sendParticles(ParticleTypes.SOUL, boss.getX(), boss.getY() + 1.0D, boss.getZ(), 100, 2.0D, 1.5D, 2.0D, 0.12D);
            for (UUID member : session.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(member);
                if (player != null && player.level() == level) {
                    player.sendSystemMessage(Component.literal("[BOSS] Phase Two — attacks are faster and wider.")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                }
            }
            if (!phaseAddsSpawned) {
                phaseAddsSpawned = true;
                boss.getPersistentData().putBoolean(TAG_PHASE_ADDS, true);
                BlockPos center = DungeonArena.bossCenter(session);
                String reinforcement = "cartenon_temple".equals(session.templateId())
                        ? "stone_guardian" : "steel_fang_raider";
                DungeonEnemies.spawn(level, session, reinforcement, center.offset(7, 0, 0), rank, false);
                DungeonEnemies.spawn(level, session, reinforcement, center.offset(-7, 0, 0), rank, false);
            }
        }

        private void phaseOne(ServerLevel level, DungeonSession session, Ravager boss, DungeonTypes.GateRank rank) {
            float scale = damageScale(rank);
            int cycle = ticks % 110;
            if (cycle == 78) telegraphRing(level, boss, 6.0D, ParticleTypes.WITCH);
            if (cycle == 90) cleave(level, session, boss, 6.0D, 6.0F * scale);
            if (ticks % 160 == 125) telegraphRing(level, boss, 9.0D, ParticleTypes.CRIT);
            if (ticks % 160 == 142) stomp(level, session, boss, 9.0D, 5.0F * scale, 0.9D);
        }

        private void phaseTwo(ServerLevel level, DungeonSession session, Ravager boss, DungeonTypes.GateRank rank) {
            float scale = damageScale(rank);
            int blastCycle = ticks % 88;
            if (blastCycle == 62) telegraphLine(level, boss);
            if (blastCycle == 76) sovereignBlast(level, session, boss, 7.0F * scale);
            int roarCycle = ticks % 130;
            if (roarCycle == 94) telegraphRing(level, boss, 11.0D, ParticleTypes.SOUL_FIRE_FLAME);
            if (roarCycle == 112) stomp(level, session, boss, 11.0D, 7.0F * scale, 1.15D);
        }
    }

    private static float damageScale(DungeonTypes.GateRank rank) {
        return (float)Math.max(0.75D, Math.min(2.5D, rank.rewardMultiplier() * 0.8D));
    }

    private static void cleave(ServerLevel level, DungeonSession session, Ravager boss, double radius, float damage) {
        level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.5F, 0.8F);
        Vec3 look = horizontalLook(boss);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(radius),
                player -> session.contains(player.getUUID()))) {
            Vec3 direction = player.position().subtract(boss.position());
            if (!validVictim(session, boss, player, radius)
                    || direction.lengthSqr() > 0.01D && look.dot(direction.normalize()) < 0.15D) continue;
            player.hurt(level.damageSources().mobAttack(boss), damage);
            knock(player, boss.position(), 0.8D);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX(), boss.getY() + 1.0D, boss.getZ(), 24,
                radius * 0.4D, 0.5D, radius * 0.4D, 0.01D);
    }

    private static void stomp(ServerLevel level, DungeonSession session, Ravager boss, double radius, float damage, double force) {
        level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.6F, 0.9F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(radius),
                player -> session.contains(player.getUUID()))) {
            if (!validVictim(session, boss, player, radius)) continue;
            player.hurt(level.damageSources().mobAttack(boss), damage);
            knock(player, boss.position(), force);
        }
        level.sendParticles(ParticleTypes.POOF, boss.getX(), boss.getY(), boss.getZ(), 70,
                radius * 0.45D, 0.3D, radius * 0.45D, 0.08D);
    }

    private static void sovereignBlast(ServerLevel level, DungeonSession session, Ravager boss, float damage) {
        level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.4F);
        Vec3 look = horizontalLook(boss);
        for (int i = 2; i <= 18; i++) {
            Vec3 point = boss.position().add(look.scale(i)).add(0.0D, 1.0D, 0.0D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 5, 0.35D, 0.35D, 0.35D, 0.02D);
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(18.0D),
                player -> session.contains(player.getUUID()))) {
            Vec3 toPlayer = player.position().subtract(boss.position());
            if (validVictim(session, boss, player, 18.0D)
                    && toPlayer.lengthSqr() > 0.01D && look.dot(toPlayer.normalize()) > 0.78D) {
                player.hurt(level.damageSources().mobAttack(boss), damage);
                knock(player, boss.position(), 1.2D);
            }
        }
    }

    private static boolean validVictim(DungeonSession session, Ravager boss, ServerPlayer player, double radius) {
        return session.contains(player.getUUID()) && player.isAlive() && !player.isSpectator()
                && boss.distanceToSqr(player) <= radius * radius && boss.hasLineOfSight(player);
    }

    private static Vec3 horizontalLook(Ravager boss) {
        Vec3 look = boss.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static void telegraphRing(ServerLevel level, Ravager boss, double radius,
                                      net.minecraft.core.particles.SimpleParticleType particle) {
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2.0D * i / 48.0D;
            level.sendParticles(particle, boss.getX() + Math.cos(angle) * radius,
                    boss.getY() + 0.15D, boss.getZ() + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.HOSTILE, 1.0F, 0.55F);
    }

    private static void telegraphLine(ServerLevel level, Ravager boss) {
        Vec3 look = horizontalLook(boss);
        for (int i = 2; i <= 18; i++) {
            Vec3 point = boss.position().add(look.scale(i)).add(0.0D, 0.2D, 0.0D);
            level.sendParticles(ParticleTypes.WITCH, point.x, point.y, point.z, 2, 0.1D, 0.05D, 0.1D, 0.0D);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.HOSTILE, 1.2F, 0.4F);
    }

    private static void knock(ServerPlayer player, Vec3 source, double force) {
        Vec3 delta = player.position().subtract(source);
        if (delta.lengthSqr() < 0.01D) delta = new Vec3(1.0D, 0.0D, 0.0D);
        delta = delta.normalize().scale(Math.min(1.5D, force));
        player.push(delta.x, Math.min(0.65D, force * 0.45D), delta.z);
        player.hurtMarked = true;
    }

    private static void set(Ravager boss, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = boss.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private DungeonBoss() {}
}
