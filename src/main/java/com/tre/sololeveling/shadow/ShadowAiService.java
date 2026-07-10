package com.tre.sololeveling.shadow;

import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.UUID;

/** Bounded, owner-driven AI controller for summoned vanilla-mob shadows. */
public final class ShadowAiService {
    private static final String STUCK_X = "sl_ai_x";
    private static final String STUCK_Y = "sl_ai_y";
    private static final String STUCK_Z = "sl_ai_z";
    private static final String STUCK_SINCE = "sl_ai_stuck_since";

    public enum Mode {
        FOLLOW("Follow"), GUARD("Guard"), PASSIVE("Passive"), AGGRESSIVE("Aggressive");
        private final String display;
        Mode(String display) { this.display = display; }
        public String display() { return display; }
        public static Mode of(int value) { return values()[Math.floorMod(value, values().length)]; }
    }

    public static void tick(ServerPlayer owner) {
        if (owner.tickCount % 5 != 0) return;
        if (owner.tickCount % 100 == 0) ShadowSummoningService.reconcile(owner);
        Mode mode = Mode.of(HunterData.mutable(owner).getInt("shadow_mode"));
        for (UUID id : ShadowSummoningService.activeIds(owner)) {
            Entity entity = ShadowSummoningService.findEntity(owner.getServer(), id);
            if (!(entity instanceof Mob mob) || !entity.isAlive()) continue;
            if (!ShadowSummoningService.isOwnedBy(mob, owner.getUUID())) {
                mob.discard();
                continue;
            }
            control(owner, mob, mode);
        }
    }

    public static void setMode(ServerPlayer owner, int value) {
        Mode mode = Mode.of(value);
        HunterData.mutable(owner).putInt("shadow_mode", mode.ordinal());
        owner.sendSystemMessage(Component.literal("[SYSTEM] Shadow command mode: " + mode.display()).withStyle(ChatFormatting.DARK_PURPLE));
        HunterData.sync(owner);
    }

    public static void cycleMode(ServerPlayer owner) {
        setMode(owner, HunterData.mutable(owner).getInt("shadow_mode") + 1);
    }

    private static void control(ServerPlayer owner, Mob shadow, Mode mode) {
        shadow.clearFire();
        shadow.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        if (shadow.level() != owner.level()) return;

        LivingEntity current = shadow.getTarget();
        if (!validTarget(owner, current)) shadow.setTarget(null);
        LivingEntity desired = desiredTarget(owner, shadow, mode);
        if (desired != null) shadow.setTarget(desired);
        else if (mode == Mode.PASSIVE || mode == Mode.FOLLOW) shadow.setTarget(null);

        double distance = shadow.distanceToSqr(owner);
        if (distance > 48.0D * 48.0D || stuck(owner, shadow, distance)) {
            teleportNearOwner(owner, shadow);
        } else if (distance > followDistance(mode) * followDistance(mode)
                && (shadow.getTarget() == null || mode != Mode.AGGRESSIVE)) {
            shadow.getNavigation().moveTo(owner, 1.15D);
        }
    }

    private static LivingEntity desiredTarget(ServerPlayer owner, Mob shadow, Mode mode) {
        if (mode == Mode.PASSIVE) return null;
        LivingEntity target = owner.getLastHurtMob();
        if (!validTarget(owner, target)) target = owner.getLastHurtByMob();
        if (validTarget(owner, target)) return target;
        if (mode != Mode.GUARD && mode != Mode.AGGRESSIVE) return null;
        double range = mode == Mode.AGGRESSIVE ? 18.0D : 10.0D;
        AABB area = shadow.getBoundingBox().inflate(range);
        return owner.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                        candidate -> candidate instanceof Enemy && validTarget(owner, candidate))
                .stream().min(Comparator.comparingDouble(shadow::distanceToSqr)).orElse(null);
    }

    private static boolean validTarget(ServerPlayer owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target == owner || ShadowSummoningService.isShadow(target)) return false;
        if (owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        if (target instanceof Player) return ModConfigs.PVP_ABILITIES.get();
        return target instanceof Enemy;
    }

    public static boolean shouldCancelAttack(LivingEntity victim, Entity attacker) {
        if (attacker == null) return false;
        UUID victimOwner = ShadowSummoningService.ownerId(victim);
        UUID attackerOwner = ShadowSummoningService.ownerId(attacker);
        if (victimOwner != null) {
            if (victimOwner.equals(attacker.getUUID()) || victimOwner.equals(attackerOwner)) return true;
            if (attacker instanceof Player player && victimOwner.equals(player.getUUID())) return true;
        }
        if (attackerOwner == null) return false;
        if (attackerOwner.equals(victim.getUUID()) || attackerOwner.equals(victimOwner)) return true;
        if (victim instanceof Player && !ModConfigs.PVP_ABILITIES.get()) return true;
        if (victim instanceof TamableAnimal pet && attackerOwner.equals(pet.getOwnerUUID())) return true;
        MinecraftServer server = attacker.level().getServer();
        ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(attackerOwner);
        return owner != null && (owner.isAlliedTo(victim) || victim.isAlliedTo(owner));
    }

    private static boolean stuck(ServerPlayer owner, Mob shadow, double distance) {
        CompoundTag data = shadow.getPersistentData();
        long now = owner.level().getGameTime();
        double dx = shadow.getX() - data.getDouble(STUCK_X);
        double dy = shadow.getY() - data.getDouble(STUCK_Y);
        double dz = shadow.getZ() - data.getDouble(STUCK_Z);
        if (dx * dx + dy * dy + dz * dz > 0.25D || distance < 12.0D * 12.0D || shadow.getTarget() != null) {
            data.putDouble(STUCK_X, shadow.getX());
            data.putDouble(STUCK_Y, shadow.getY());
            data.putDouble(STUCK_Z, shadow.getZ());
            data.putLong(STUCK_SINCE, now);
            return false;
        }
        if (!data.contains(STUCK_SINCE)) data.putLong(STUCK_SINCE, now);
        return now - data.getLong(STUCK_SINCE) > 40L;
    }

    private static void teleportNearOwner(ServerPlayer owner, Mob shadow) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = owner.blockPosition().offset(owner.getRandom().nextInt(9) - 4, 0, owner.getRandom().nextInt(9) - 4);
            AABB moved = shadow.getBoundingBox().move(pos.getX() + 0.5D - shadow.getX(), pos.getY() - shadow.getY(), pos.getZ() + 0.5D - shadow.getZ());
            ServerLevel level = owner.serverLevel();
            if (level.getWorldBorder().isWithinBounds(pos) && level.noCollision(shadow, moved)
                    && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                shadow.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                shadow.getNavigation().stop();
                shadow.fallDistance = 0.0F;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, shadow.getX(), shadow.getY() + 0.5D, shadow.getZ(), 18, 0.3D, 0.5D, 0.3D, 0.08D);
                CompoundTag data = shadow.getPersistentData();
                data.putLong(STUCK_SINCE, owner.level().getGameTime());
                return;
            }
        }
    }

    private static double followDistance(Mode mode) { return mode == Mode.PASSIVE ? 4.0D : 7.0D; }

    private ShadowAiService() {}
}
