package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AbilityHandler {
    public static void activate(ServerPlayer player, String rawSkill) {
        String skill = rawSkill.toLowerCase(Locale.ROOT);
        if (!HunterData.isAwakened(player)) return;
        if (!HunterData.hasSkill(player, skill)) {
            locked(player, skill);
            return;
        }
        if (!HunterData.cooldownReady(player, skill)) {
            cooldownMessage(player, skill);
            return;
        }
        int cost = cost(skill);
        if (!HunterData.spendMana(player, cost)) return;
        boolean success = switch (skill) {
            case "stealth" -> stealth(player);
            case "bloodlust" -> bloodlust(player);
            case "quicksilver" -> quicksilver(player);
            case "mutilation" -> mutilation(player);
            case "dagger_rush" -> daggerRush(player);
            case "rulers_authority" -> authorityImpulse(player, player.isShiftKeyDown());
            case "dragons_fear" -> dragonsFear(player);
            default -> false;
        };
        finishActivation(player, skill, cost, cooldown(skill), success);
    }

    public static void activateAuthority(ServerPlayer player, String rawMode) {
        String mode = rawMode.toLowerCase(Locale.ROOT);
        if (!HunterData.isAwakened(player)) return;
        if (!HunterData.hasSkill(player, "rulers_authority")) {
            locked(player, "rulers_authority");
            return;
        }
        String cooldownKey = "rulers_authority_" + mode;
        if (!HunterData.cooldownReady(player, cooldownKey)) {
            cooldownMessage(player, cooldownKey);
            return;
        }
        int cost = authorityCost(mode);
        if (cost < 0 || !HunterData.spendMana(player, cost)) return;
        boolean success = switch (mode) {
            case "pull" -> authorityImpulse(player, true);
            case "push" -> authorityImpulse(player, false);
            case "hold" -> authorityHold(player);
            case "throw" -> authorityThrow(player);
            case "dash" -> authorityDash(player);
            case "flight" -> authorityFlight(player);
            default -> false;
        };
        finishActivation(player, cooldownKey, cost, authorityCooldown(mode), success);
    }

    public static void tick(ServerPlayer player) {
        tickAuthorityHold(player);
        tickAuthorityFlight(player);
    }

    public static void cancel(ServerPlayer player) {
        releaseHeld(player, false);
        revokeFlight(player);
    }

    private static void finishActivation(ServerPlayer player, String cooldownKey, int cost, int cooldown, boolean success) {
        if (!success) {
            HunterData.addMana(player, cost);
            player.sendSystemMessage(Component.literal("[SYSTEM] No valid target or requirement not met.").withStyle(ChatFormatting.RED));
            HunterData.sync(player);
            return;
        }
        HunterData.setCooldown(player, cooldownKey, cooldown);
        player.level().playSound(null, player.blockPosition(), ModSounds.ABILITY.get(), SoundSource.PLAYERS, 0.8F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        HunterData.sync(player);
    }

    private static boolean stealth(ServerPlayer player) {
        HunterData.beginStealth(player, 20 * 15);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 15, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 15, 0, false, false, true));
        particles(player.serverLevel(), player.position().add(0, 1, 0), ParticleTypes.PORTAL, 40, 0.6);
        return true;
    }

    private static boolean bloodlust(ServerPlayer player) {
        List<Monster> targets = player.serverLevel().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12.0D), Entity::isAlive);
        for (Monster target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            target.setTarget(null);
        }
        particles(player.serverLevel(), player.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 80, 3.0);
        return !targets.isEmpty();
    }

    private static boolean quicksilver(ServerPlayer player) {
        int amplifier = Math.min(3, 1 + HunterData.getStat(player, "agility") / 50);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, amplifier, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 10, 0, false, false, true));
        particles(player.serverLevel(), player.position().add(0, 0.5, 0), ParticleTypes.END_ROD, 30, 0.5);
        return true;
    }

    private static boolean mutilation(ServerPlayer player) {
        if (!isDagger(player.getMainHandItem().getItem().toString())) return false;
        LivingEntity target = findLivingTarget(player, 12.0D);
        if (target == null) return false;
        Vec3 look = player.getLookAngle();
        Vec3 destination = target.position().subtract(look.scale(1.6D));
        Vec3 delta = destination.subtract(player.position());
        if (player.serverLevel().noCollision(player, player.getBoundingBox().move(delta))) {
            player.teleportTo(destination.x, destination.y, destination.z);
            player.fallDistance = 0.0F;
        }
        float damage = 8.0F + HunterData.getStat(player, "strength") * 0.35F + HunterData.getStat(player, "agility") * 0.15F;
        target.invulnerableTime = 0;
        dealAbilityDamage(player, target, damage);
        particles(player.serverLevel(), target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.SWEEP_ATTACK, 12, 0.5);
        particles(player.serverLevel(), target.position().add(0, 1, 0), ParticleTypes.PORTAL, 30, 0.8);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        return true;
    }

    private static boolean daggerRush(ServerPlayer player) {
        if (!isDagger(player.getMainHandItem().getItem().toString()) && !isDagger(player.getOffhandItem().getItem().toString())) return false;
        LivingEntity target = findLivingTarget(player, 20.0D);
        if (target == null) return false;
        float damage = 10.0F + HunterData.getStat(player, "strength") * 0.25F + HunterData.getStat(player, "agility") * 0.25F;
        target.invulnerableTime = 0;
        dealAbilityDamage(player, target, damage);
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2.0D * i / 48.0D;
            level.sendParticles(ParticleTypes.PORTAL, target.getX() + Math.cos(angle) * 2.0D, target.getY() + 1.0D + Math.sin(angle * 2.0D) * 0.5D, target.getZ() + Math.sin(angle) * 2.0D, 1, 0, 0, 0, 0.2D);
        }
        return true;
    }

    private static boolean authorityImpulse(ServerPlayer player, boolean pull) {
        Entity target = findTarget(player, 22.0D);
        if (target == null) return false;
        Vec3 direction = player.getLookAngle().normalize();
        double force = Math.min(2.5D, 0.8D + HunterData.getStat(player, "strength") * 0.02D);
        if (target instanceof ItemEntity item) {
            Vec3 movement = pull ? player.getEyePosition().subtract(item.position()).normalize().scale(Math.max(0.8D, force)) : direction.scale(force);
            item.setDeltaMovement(movement);
            item.setPickUpDelay(0);
        } else if (pull) {
            Vec3 movement = player.getEyePosition().subtract(target.position()).normalize().scale(force);
            target.setDeltaMovement(movement.x, Math.max(0.15D, movement.y), movement.z);
        } else {
            target.setDeltaMovement(direction.x * force, 0.35D + direction.y * force, direction.z * force);
        }
        target.hurtMarked = true;
        particles(player.serverLevel(), target.position().add(0, target.getBbHeight() * 0.5, 0), ParticleTypes.DRAGON_BREATH, 35, 0.7);
        return true;
    }

    private static boolean authorityHold(ServerPlayer player) {
        Entity target = findTarget(player, 20.0D);
        if (target == null || target instanceof Player && !ModConfigs.PVP_ABILITIES.get()) return false;
        releaseHeld(player, false);
        CompoundTag tag = HunterData.mutable(player);
        tag.putString("authority_held", target.getUUID().toString());
        tag.putLong("authority_hold_until", player.level().getGameTime() + 20L * 6L);
        target.setNoGravity(true);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        player.sendSystemMessage(Component.literal("[RULER'S AUTHORITY] Target held. Use Throw before the hold expires.").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private static boolean authorityThrow(ServerPlayer player) {
        Entity held = heldEntity(player);
        if (held != null) {
            Vec3 force = player.getLookAngle().normalize().scale(3.0D + Math.min(2.0D, HunterData.getStat(player, "strength") * 0.015D));
            if (held instanceof LivingEntity living) dealAbilityDamage(player, living, 6.0F + HunterData.getStat(player, "strength") * 0.12F);
            releaseHeld(player, false);
            held.setDeltaMovement(force.x, force.y + 0.25D, force.z);
            held.hurtMarked = true;
            return true;
        }
        Entity target = findTarget(player, 22.0D);
        if (target == null) return false;
        Vec3 force = player.getLookAngle().normalize().scale(3.25D);
        target.setDeltaMovement(force.x, force.y + 0.3D, force.z);
        target.hurtMarked = true;
        return true;
    }

    private static boolean authorityDash(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.x * 1.8D, Math.max(0.18D, look.y * 1.2D + 0.18D), look.z * 1.8D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        particles(player.serverLevel(), player.position().add(0, 0.8D, 0), ParticleTypes.END_ROD, 24, 0.35D);
        return true;
    }

    private static boolean authorityFlight(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putLong("authority_flight_until", player.level().getGameTime() + 20L * 10L);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        player.sendSystemMessage(Component.literal("[RULER'S AUTHORITY] Controlled flight enabled for 10 seconds.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    private static void tickAuthorityHold(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getString("authority_held").isBlank()) return;
        Entity held = heldEntity(player);
        if (held == null || !held.isAlive() || player.level().getGameTime() >= tag.getLong("authority_hold_until") || !HunterData.hasSkill(player, "rulers_authority")) {
            releaseHeld(player, false);
            return;
        }
        Vec3 position = player.getEyePosition().add(player.getLookAngle().normalize().scale(3.2D));
        held.teleportTo(position.x, position.y - held.getBbHeight() * 0.45D, position.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.setNoGravity(true);
        held.fallDistance = 0.0F;
        held.hurtMarked = true;
        if (player.tickCount % 5 == 0) particles(player.serverLevel(), held.position().add(0, held.getBbHeight() * 0.5D, 0), ParticleTypes.DRAGON_BREATH, 5, 0.25D);
    }

    private static void tickAuthorityFlight(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        long until = tag.getLong("authority_flight_until");
        if (until <= 0L) return;
        if (player.level().getGameTime() >= until || !HunterData.hasSkill(player, "rulers_authority")) {
            revokeFlight(player);
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
    }

    private static Entity heldEntity(ServerPlayer player) {
        String raw = HunterData.mutable(player).getString("authority_held");
        if (raw.isBlank()) return null;
        try { return player.serverLevel().getEntity(UUID.fromString(raw)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static void releaseHeld(ServerPlayer player, boolean discardVelocity) {
        Entity held = heldEntity(player);
        if (held != null) {
            held.setNoGravity(false);
            held.fallDistance = 0.0F;
            if (discardVelocity) held.setDeltaMovement(Vec3.ZERO);
            held.hurtMarked = true;
        }
        CompoundTag tag = HunterData.mutable(player);
        tag.putString("authority_held", "");
        tag.putLong("authority_hold_until", 0L);
    }

    private static void revokeFlight(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getLong("authority_flight_until") <= 0L) return;
        tag.putLong("authority_flight_until", 0L);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
    }

    private static boolean dragonsFear(ServerPlayer player) {
        List<Monster> targets = player.serverLevel().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(18.0D), Entity::isAlive);
        for (Mob target : targets) {
            target.setTarget(null);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            Vec3 away = target.position().subtract(player.position()).normalize().scale(1.5D);
            target.setDeltaMovement(away.x, 0.2D, away.z);
        }
        particles(player.serverLevel(), player.position().add(0, 1, 0), ParticleTypes.SONIC_BOOM, 6, 2.0);
        particles(player.serverLevel(), player.position().add(0, 1, 0), ParticleTypes.DRAGON_BREATH, 120, 5.0);
        return !targets.isEmpty();
    }

    public static LivingEntity findLivingTarget(ServerPlayer player, double range) {
        Entity target = findTarget(player, range);
        return target instanceof LivingEntity living ? living : null;
    }

    public static Entity findTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
                entity -> entity.isPickable() && entity != player && !entity.getPersistentData().getBoolean("sl_shadow")
                        && (!(entity instanceof Player) || ModConfigs.PVP_ABILITIES.get()), range * range);
        return hit == null ? null : hit.getEntity();
    }

    private static void dealAbilityDamage(ServerPlayer player, LivingEntity target, float damage) {
        player.getPersistentData().putBoolean("sl_ability_damage", true);
        try { target.hurt(player.damageSources().playerAttack(player), damage); }
        finally { player.getPersistentData().remove("sl_ability_damage"); }
    }

    private static void particles(ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.12D);
    }

    private static void locked(ServerPlayer player, String skill) {
        player.sendSystemMessage(Component.literal("[SYSTEM] Skill locked: " + display(skill)).withStyle(ChatFormatting.RED));
    }

    private static void cooldownMessage(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.literal("[SYSTEM] Cooldown: " + (HunterData.cooldownRemaining(player, key) / 20.0F) + "s").withStyle(ChatFormatting.GRAY));
    }

    private static boolean isDagger(String id) { return id.contains("dagger") || id.contains("fang") || id.contains("wrath"); }
    private static String display(String skill) { return skill.replace('_', ' '); }
    private static int cost(String skill) { return switch (skill) { case "stealth" -> 25; case "bloodlust" -> 40; case "quicksilver" -> 20; case "mutilation" -> 45; case "dagger_rush" -> 80; case "rulers_authority" -> 35; case "dragons_fear" -> 120; default -> 0; }; }
    private static int cooldown(String skill) { return switch (skill) { case "stealth" -> 20 * 20; case "bloodlust" -> 20 * 25; case "quicksilver" -> 20 * 12; case "mutilation" -> 20 * 8; case "dagger_rush" -> 20 * 20; case "rulers_authority" -> 20 * 5; case "dragons_fear" -> 20 * 45; default -> 20; }; }
    private static int authorityCost(String mode) { return switch (mode) { case "pull", "push" -> 35; case "hold" -> 45; case "throw" -> 30; case "dash" -> 25; case "flight" -> 80; default -> -1; }; }
    private static int authorityCooldown(String mode) { return switch (mode) { case "pull", "push" -> 20 * 4; case "hold" -> 20 * 8; case "throw" -> 20 * 4; case "dash" -> 20 * 5; case "flight" -> 20 * 30; default -> 20; }; }
    private AbilityHandler() {}
}
