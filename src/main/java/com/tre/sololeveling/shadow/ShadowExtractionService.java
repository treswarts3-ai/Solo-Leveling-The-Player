package com.tre.sololeveling.shadow;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/** Server-authoritative extraction target lifecycle and extraction resolution. */
public final class ShadowExtractionService {
    public static final double RANGE = 12.0D;
    public static final int MAX_ATTEMPTS = 3;
    public static final long TARGET_LIFETIME = 20L * 60L;
    private static final int MAX_TARGETS = 16;

    public static boolean eligible(LivingEntity entity) {
        if (!(entity instanceof Enemy) || entity instanceof WitherBoss || entity instanceof EnderDragon) return false;
        if (entity.getPersistentData().getBoolean("sl_shadow")) return false;
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) != null;
    }

    public static void record(ServerPlayer owner, LivingEntity victim) {
        if (!HunterData.hasSkill(owner, "shadow_extraction") || !eligible(victim)) return;
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (type == null) return;

        ListTag targets = cleaned(owner);
        String dimension = victim.level().dimension().location().toString();
        for (int i = targets.size() - 1; i >= 0; i--) {
            CompoundTag old = targets.getCompound(i);
            if (dimension.equals(old.getString("dimension")) && distanceSqr(old, victim.position()) < 1.0D) targets.remove(i);
        }

        CompoundTag target = new CompoundTag();
        target.putString("target_id", UUID.randomUUID().toString());
        target.putString("type", type.toString());
        target.putString("name", victim.getName().getString());
        target.putString("dimension", dimension);
        target.putDouble("x", victim.getX());
        target.putDouble("y", victim.getY());
        target.putDouble("z", victim.getZ());
        target.putLong("created", victim.level().getGameTime());
        target.putLong("expires", victim.level().getGameTime() + TARGET_LIFETIME);
        target.putInt("power", power(victim));
        target.putInt("attempts", 0);
        targets.add(target);
        while (targets.size() > MAX_TARGETS) targets.remove(0);
        HunterData.setImprints(owner, targets);
        owner.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, victim.getX(), victim.getY() + 0.5D, victim.getZ(), 35, 0.5D, 0.5D, 0.5D, 0.1D);
        HunterData.sync(owner);
    }

    public static boolean createTestTarget(ServerPlayer owner, EntityType<?> type) {
        Entity entity = type.create(owner.serverLevel());
        if (!(entity instanceof LivingEntity living) || !eligible(living)) return false;
        Vec3 look = owner.getLookAngle();
        living.moveTo(owner.getX() + look.x * 4.0D, owner.getY(), owner.getZ() + look.z * 4.0D);
        record(owner, living);
        return true;
    }

    public static boolean extract(ServerPlayer owner) {
        if (!HunterData.hasSkill(owner, "shadow_extraction") || !HunterData.cooldownReady(owner, "shadow_extraction")) return false;
        ListTag targets = cleaned(owner);
        int selected = select(owner, targets);
        if (selected < 0) {
            owner.sendSystemMessage(Component.literal("[SYSTEM] No extractable target is within range.").withStyle(ChatFormatting.RED));
            HunterData.setImprints(owner, targets);
            return false;
        }
        ListTag shadows = HunterData.shadows(owner);
        if (shadows.size() >= HunterData.getShadowCapacity(owner)) {
            owner.sendSystemMessage(Component.literal("[SYSTEM] Shadow storage is full.").withStyle(ChatFormatting.RED));
            return false;
        }

        CompoundTag target = targets.getCompound(selected);
        double chance = chance(owner, target);
        boolean success = owner.getRandom().nextDouble() < chance;
        if (success) {
            shadows.add(newRecord(owner, target, shadows.size()));
            HunterData.setShadows(owner, shadows);
            HunterData.mutable(owner).putInt("shadow_extractions", HunterData.mutable(owner).getInt("shadow_extractions") + 1);
            targets.remove(selected);
            owner.sendSystemMessage(Component.literal("[ARISE] Extraction succeeded (" + Math.round(chance * 100.0D) + "%).")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            owner.level().playSound(null, owner.blockPosition(), ModSounds.SHADOW.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
            particles(owner, target, true);
        } else {
            int attempts = target.getInt("attempts") + 1;
            target.putInt("attempts", attempts);
            if (attempts >= MAX_ATTEMPTS) targets.remove(selected);
            owner.sendSystemMessage(Component.literal("[SYSTEM] Extraction failed (" + Math.round(chance * 100.0D) + "%). Attempts "
                    + attempts + "/" + MAX_ATTEMPTS + ".").withStyle(ChatFormatting.RED));
            owner.level().playSound(null, owner.blockPosition(), ModSounds.MANA_FAIL.get(), SoundSource.PLAYERS, 0.7F, 0.75F);
            particles(owner, target, false);
        }
        HunterData.setImprints(owner, targets);
        HunterData.setCooldown(owner, "shadow_extraction", 20);
        HunterData.sync(owner);
        return success;
    }

    public static int cleanup(ServerPlayer owner) {
        int before = HunterData.imprints(owner).size();
        ListTag clean = cleaned(owner);
        HunterData.setImprints(owner, clean);
        return before - clean.size();
    }

    public static double chance(ServerPlayer owner, CompoundTag target) {
        int ownerPower = HunterData.getLevel(owner) * 3 + HunterData.getStat(owner, "intelligence") * 2 + HunterData.getStat(owner, "sense");
        double value = 0.55D + (ownerPower - Math.max(1, target.getInt("power"))) / 200.0D - target.getInt("attempts") * 0.08D;
        return Math.max(0.05D, Math.min(0.95D, value));
    }

    private static CompoundTag newRecord(ServerPlayer owner, CompoundTag target, int index) {
        CompoundTag record = new CompoundTag();
        record.putInt("record_version", 1);
        record.putString("record_id", UUID.randomUUID().toString());
        record.putString("type", target.getString("type"));
        String name = target.getString("name").trim();
        record.putString("name", name.isEmpty() ? "Shadow " + (index + 1) : name);
        record.putString("rank", target.getInt("power") >= 180 ? "Knight" : target.getInt("power") >= 100 ? "Elite" : "Normal");
        record.putInt("level", Math.max(1, HunterData.getLevel(owner) / 4));
        record.putInt("xp", 0);
        record.putBoolean("active", false);
        record.putString("active_entity", "");
        return record;
    }

    private static int select(ServerPlayer owner, ListTag targets) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle().normalize();
        String dimension = owner.level().dimension().location().toString();
        double maxDistance = RANGE * RANGE;
        int aimed = -1;
        int nearest = -1;
        double bestDot = 0.92D;
        double bestDistance = maxDistance;
        for (int i = 0; i < targets.size(); i++) {
            CompoundTag target = targets.getCompound(i);
            if (!dimension.equals(target.getString("dimension"))) continue;
            Vec3 point = new Vec3(target.getDouble("x"), target.getDouble("y") + 0.5D, target.getDouble("z"));
            double distance = eye.distanceToSqr(point);
            if (distance > maxDistance) continue;
            if (distance < bestDistance) { bestDistance = distance; nearest = i; }
            Vec3 delta = point.subtract(eye);
            if (delta.lengthSqr() < 0.0001D) return i;
            double dot = look.dot(delta.normalize());
            if (dot > bestDot) { bestDot = dot; aimed = i; }
        }
        return aimed >= 0 ? aimed : nearest;
    }

    private static ListTag cleaned(ServerPlayer owner) {
        ListTag source = HunterData.imprints(owner);
        ListTag result = new ListTag();
        long now = owner.level().getGameTime();
        for (int i = 0; i < source.size(); i++) {
            CompoundTag target = source.getCompound(i);
            if (target.getLong("expires") < now || target.getInt("attempts") >= MAX_ATTEMPTS) continue;
            if (ResourceLocation.tryParse(target.getString("type")) == null) continue;
            result.add(target.copy());
        }
        return result;
    }

    private static int power(LivingEntity entity) {
        return Math.max(1, (int)Math.round(Math.max(entity.getHealth(), entity.getMaxHealth()) + entity.getArmorValue() * 2.0D + entity.getBbHeight() * 4.0D));
    }

    private static double distanceSqr(CompoundTag target, Vec3 position) {
        double x = target.getDouble("x") - position.x;
        double y = target.getDouble("y") - position.y;
        double z = target.getDouble("z") - position.z;
        return x * x + y * y + z * z;
    }

    private static void particles(ServerPlayer owner, CompoundTag target, boolean success) {
        owner.serverLevel().sendParticles(success ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SMOKE,
                target.getDouble("x"), target.getDouble("y") + 0.5D, target.getDouble("z"), success ? 80 : 45,
                0.7D, 0.8D, 0.7D, success ? 0.22D : 0.04D);
    }

    private ShadowExtractionService() {}
}
