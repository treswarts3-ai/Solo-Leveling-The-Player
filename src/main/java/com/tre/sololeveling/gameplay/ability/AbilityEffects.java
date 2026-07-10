package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Common damage, movement, particle, and sound operations used by abilities. */
public final class AbilityEffects {
    public static boolean dealDamage(ServerPlayer player, LivingEntity target, float damage) {
        if (!AbilityTargeting.isValidTarget(player, target) || damage <= 0.0F) return false;
        player.getPersistentData().putBoolean("sl_ability_damage", true);
        try {
            target.invulnerableTime = 0;
            return target.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            player.getPersistentData().remove("sl_ability_damage");
        }
    }

    public static void particles(ServerLevel level, Vec3 position, ParticleOptions particle, int count, double spread) {
        int boundedCount = Math.max(0, Math.min(256, count));
        double boundedSpread = Math.max(0.0D, Math.min(8.0D, spread));
        level.sendParticles(particle, position.x, position.y, position.z, boundedCount,
                boundedSpread, boundedSpread, boundedSpread, 0.12D);
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

    public static boolean moveIfClear(ServerPlayer player, Vec3 destination) {
        Vec3 delta = destination.subtract(player.position());
        if (!player.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(destination))) return false;
        if (!player.serverLevel().noCollision(player, player.getBoundingBox().move(delta))) return false;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        return true;
    }

    private AbilityEffects() {
    }
}
