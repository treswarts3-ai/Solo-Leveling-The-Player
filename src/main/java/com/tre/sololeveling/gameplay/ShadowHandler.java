package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ShadowHandler {
    private enum ShadowMode {
        FOLLOW("Follow"), GUARD("Guard"), PASSIVE("Passive"), AGGRESSIVE("Aggressive");
        private final String display;
        ShadowMode(String display) { this.display = display; }
        static ShadowMode of(int value) { return values()[Math.floorMod(value, values().length)]; }
    }

    public static void recordImprint(ServerPlayer killer, LivingEntity victim) {
        if (!HunterData.hasSkill(killer, "shadow_extraction") || !(victim instanceof Enemy)
                || victim instanceof WitherBoss || victim instanceof EnderDragon
                || victim.getPersistentData().getBoolean("sl_shadow")) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (id == null) return;
        ListTag list = HunterData.imprints(killer);
        CompoundTag imprint = new CompoundTag();
        imprint.putString("type", id.toString());
        imprint.putDouble("x", victim.getX());
        imprint.putDouble("y", victim.getY());
        imprint.putDouble("z", victim.getZ());
        imprint.putString("dimension", victim.level().dimension().location().toString());
        imprint.putLong("expires", victim.level().getGameTime() + 20L * 60L);
        imprint.putInt("power", Math.max(1, (int)(victim.getMaxHealth() + victim.getArmorValue() * 2)));
        imprint.putInt("attempts", 0);
        list.add(imprint);
        while (list.size() > 16) list.remove(0);
        HunterData.setImprints(killer, list);
        killer.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, victim.getX(), victim.getY() + 0.5D, victim.getZ(), 35, 0.5, 0.5, 0.5, 0.1);
        HunterData.sync(killer);
    }

    public static boolean extract(ServerPlayer player) {
        if (!HunterData.hasSkill(player, "shadow_extraction") || !HunterData.cooldownReady(player, "shadow_extraction")) return false;
        ListTag imprints = HunterData.imprints(player);
        int selected = -1;
        double best = 12.0D * 12.0D;
        for (int i = 0; i < imprints.size(); i++) {
            CompoundTag imprint = imprints.getCompound(i);
            if (imprint.getLong("expires") < player.level().getGameTime()) continue;
            if (!imprint.getString("dimension").equals(player.level().dimension().location().toString())) continue;
            double distance = player.distanceToSqr(imprint.getDouble("x"), imprint.getDouble("y"), imprint.getDouble("z"));
            if (distance < best) { best = distance; selected = i; }
        }
        if (selected < 0) return false;
        ListTag shadows = HunterData.shadows(player);
        if (shadows.size() >= HunterData.getShadowCapacity(player)) {
            player.sendSystemMessage(Component.literal("[SYSTEM] Shadow storage is full.").withStyle(ChatFormatting.RED));
            return false;
        }
        CompoundTag imprint = imprints.getCompound(selected);
        int playerPower = HunterData.getLevel(player) * 3 + HunterData.getStat(player, "intelligence") * 2 + HunterData.getStat(player, "sense");
        int targetPower = imprint.getInt("power") + imprint.getInt("attempts") * 5;
        double chance = Math.max(0.15D, Math.min(0.95D, 0.55D + (playerPower - targetPower) / 200.0D));
        boolean success = player.getRandom().nextDouble() < chance;
        if (success) {
            CompoundTag shadow = new CompoundTag();
            shadow.putString("record_id", UUID.randomUUID().toString());
            shadow.putString("type", imprint.getString("type"));
            shadow.putString("name", "Shadow " + (shadows.size() + 1));
            shadow.putInt("level", Math.max(1, HunterData.getLevel(player) / 4));
            shadow.putString("rank", HunterData.getLevel(player) >= 60 ? "Elite" : "Normal");
            shadows.add(shadow);
            HunterData.setShadows(player, shadows);
            HunterData.mutable(player).putInt("shadow_extractions", HunterData.mutable(player).getInt("shadow_extractions") + 1);
            imprints.remove(selected);
            player.sendSystemMessage(Component.literal("[ARISE] Shadow extraction succeeded.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            player.level().playSound(null, player.blockPosition(), ModSounds.SHADOW.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        } else {
            imprint.putInt("attempts", imprint.getInt("attempts") + 1);
            if (imprint.getInt("attempts") >= 3) imprints.remove(selected);
            player.sendSystemMessage(Component.literal("[SYSTEM] Shadow extraction failed.").withStyle(ChatFormatting.RED));
        }
        HunterData.setImprints(player, imprints);
        HunterData.setCooldown(player, "shadow_extraction", 20);
        player.serverLevel().sendParticles(ParticleTypes.PORTAL, imprint.getDouble("x"), imprint.getDouble("y") + 0.5D, imprint.getDouble("z"), 60, 0.7, 0.8, 0.7, 0.2);
        HunterData.sync(player);
        return success;
    }

    public static boolean summonFirst(ServerPlayer player) {
        if (!HunterData.hasSkill(player, "shadow_preservation")) return false;
        ListTag stored = HunterData.shadows(player);
        if (stored.isEmpty()) return false;
        int activeLimit = Math.min(ModConfigs.ACTIVE_SHADOW_MAX.get(), HunterData.getShadowCapacity(player));
        if (activeLimit <= 0 || activeIds(player).size() >= activeLimit) {
            player.sendSystemMessage(Component.literal("[SYSTEM] Active shadow limit reached.").withStyle(ChatFormatting.RED));
            return false;
        }

        CompoundTag record = null;
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag candidate = stored.getCompound(i);
            if (candidate.getString("record_id").isBlank()) candidate.putString("record_id", UUID.randomUUID().toString());
            if (!isRecordActive(player, candidate.getString("record_id"))) { record = candidate; break; }
        }
        HunterData.setShadows(player, stored);
        if (record == null) {
            player.sendSystemMessage(Component.literal("[SYSTEM] Every stored shadow is already active.").withStyle(ChatFormatting.GRAY));
            return false;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(record.getString("type")));
        if (type == null) return false;
        Entity created = type.create(player.serverLevel());
        if (!(created instanceof Mob mob)) return false;
        int manaCost = Math.max(5, 5 + record.getInt("level"));
        if (!HunterData.spendMana(player, manaCost)) return false;

        mob.moveTo(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D, player.getYRot(), 0.0F);
        mob.getPersistentData().putBoolean("sl_shadow", true);
        mob.getPersistentData().putUUID("sl_owner", player.getUUID());
        mob.getPersistentData().putString("sl_shadow_record", record.getString("record_id"));
        mob.getPersistentData().putInt("sl_shadow_level", record.getInt("level"));
        mob.setCustomName(Component.literal(record.getString("name")).withStyle(ChatFormatting.DARK_PURPLE));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        mob.setTarget(null);
        mob.setGlowingTag(true);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60, 0, false, false));
        player.serverLevel().addFreshEntity(mob);
        HunterData.addActiveShadow(player, mob.getUUID());
        HunterData.mutable(player).putBoolean("shadow_summoned_once", true);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 60, 0.6, 0.8, 0.6, 0.2);
        player.level().playSound(null, player.blockPosition(), ModSounds.SHADOW.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        HunterData.sync(player);
        return true;
    }

    public static void dismissAll(ServerPlayer player) {
        for (UUID id : activeIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity != null) {
                if (entity.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.5D, entity.getZ(), 25, 0.4, 0.6, 0.4, 0.1);
                }
                entity.discard();
            }
        }
        HunterData.setActiveShadows(player, new ListTag());
        HunterData.sync(player);
    }

    public static boolean exchange(ServerPlayer player) {
        if (!HunterData.hasSkill(player, "shadow_exchange") || !HunterData.cooldownReady(player, "shadow_exchange")) return false;
        for (UUID id : activeIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity == null || entity.level() != player.level() || !entity.isAlive()) continue;
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
            if (!safeDestination(player.serverLevel(), player, ex, ey, ez)
                    || !safeDestination(player.serverLevel(), entity, px, py, pz)) continue;
            if (!HunterData.spendMana(player, 50)) return false;
            player.teleportTo(ex, ey, ez);
            entity.teleportTo(px, py, pz);
            player.fallDistance = 0.0F;
            entity.fallDistance = 0.0F;
            HunterData.setCooldown(player, "shadow_exchange", 20 * 45);
            player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, ex, ey + 1, ez, 80, 0.8, 1.0, 0.8, 0.3);
            HunterData.sync(player);
            return true;
        }
        player.sendSystemMessage(Component.literal("[SYSTEM] No safe active shadow destination found.").withStyle(ChatFormatting.RED));
        return false;
    }

    public static void cycleMode(ServerPlayer player) { setMode(player, HunterData.mutable(player).getInt("shadow_mode") + 1); }

    public static void setMode(ServerPlayer player, int value) {
        CompoundTag tag = HunterData.mutable(player);
        ShadowMode mode = ShadowMode.of(value);
        tag.putInt("shadow_mode", mode.ordinal());
        player.sendSystemMessage(Component.literal("[SYSTEM] Shadow command mode: " + mode.display).withStyle(ChatFormatting.DARK_PURPLE));
        HunterData.sync(player);
    }


    public static void tick(ServerPlayer owner) {
        if (owner.tickCount % 5 != 0) return;
        ShadowMode mode = ShadowMode.of(HunterData.mutable(owner).getInt("shadow_mode"));
        ListTag previous = HunterData.activeShadows(owner);
        ListTag kept = new ListTag();
        for (UUID id : activeIds(owner)) {
            Entity entity = findEntity(owner, id);
            if (!(entity instanceof Mob mob) || !entity.isAlive()) continue;
            kept.add(StringTag.valueOf(id.toString()));
            controlShadow(owner, mob, mode);
        }
        if (!kept.equals(previous)) HunterData.setActiveShadows(owner, kept);
    }

    private static void controlShadow(ServerPlayer owner, Mob mob, ShadowMode mode) {
        mob.clearFire();
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        if (mob.level() != owner.level()) return;

        LivingEntity current = mob.getTarget();
        if (current != null && !validTarget(owner, current)) mob.setTarget(null);
        LivingEntity desired = null;
        if (mode != ShadowMode.PASSIVE) {
            LivingEntity combatTarget = owner.getLastHurtMob();
            if (!validTarget(owner, combatTarget)) combatTarget = owner.getLastHurtByMob();
            if (validTarget(owner, combatTarget)) desired = combatTarget;
            if (desired == null && (mode == ShadowMode.GUARD || mode == ShadowMode.AGGRESSIVE)) {
                double range = mode == ShadowMode.AGGRESSIVE ? 18.0D : 10.0D;
                desired = nearestHostile(owner, mob, range);
            }
        }
        if (desired != null) mob.setTarget(desired);
        else if (mode == ShadowMode.PASSIVE || mode == ShadowMode.FOLLOW) mob.setTarget(null);

        double distance = mob.distanceToSqr(owner);
        if (distance > 32.0D * 32.0D) {
            teleportNearOwner(owner, mob);
        } else if (distance > (mode == ShadowMode.PASSIVE ? 4.0D * 4.0D : 7.0D * 7.0D)
                && (mob.getTarget() == null || mode != ShadowMode.AGGRESSIVE)) {
            mob.getNavigation().moveTo(owner, 1.15D);
        }
        if (HunterData.domainActive(owner)) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 1, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
            owner.serverLevel().sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 0.2D, mob.getZ(), 2, 0.3, 0.1, 0.3, 0.02);
        }
    }

    private static LivingEntity nearestHostile(ServerPlayer owner, Mob shadow, double range) {
        AABB area = shadow.getBoundingBox().inflate(range);
        return owner.serverLevel().getEntitiesOfClass(Monster.class, area,
                        candidate -> candidate.isAlive() && validTarget(owner, candidate))
                .stream().min(Comparator.comparingDouble(shadow::distanceToSqr)).orElse(null);
    }

    private static boolean validTarget(ServerPlayer owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target == owner || target.getPersistentData().getBoolean("sl_shadow")) return false;
        if (owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        if (target instanceof Player) return ModConfigs.PVP_ABILITIES.get();
        return target instanceof Enemy;
    }

    public static boolean shouldCancelAttack(LivingEntity victim, Entity attacker) {
        if (attacker == null) return false;
        UUID victimOwner = shadowOwner(victim);
        UUID attackerOwner = shadowOwner(attacker);
        if (victimOwner != null && (victimOwner.equals(attacker.getUUID()) || victimOwner.equals(attackerOwner))) return true;
        if (attackerOwner == null) return false;
        if (attackerOwner.equals(victim.getUUID()) || attackerOwner.equals(victimOwner)) return true;
        if (victim instanceof Player && !ModConfigs.PVP_ABILITIES.get()) return true;
        if (victim instanceof TamableAnimal pet && attackerOwner.equals(pet.getOwnerUUID())) return true;
        MinecraftServer server = attacker.level().getServer();
        ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(attackerOwner);
        return owner != null && (owner.isAlliedTo(victim) || victim.isAlliedTo(owner));
    }

    private static UUID shadowOwner(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        return tag.getBoolean("sl_shadow") && tag.hasUUID("sl_owner") ? tag.getUUID("sl_owner") : null;
    }

    private static boolean isRecordActive(ServerPlayer player, String recordId) {
        for (UUID id : activeIds(player)) {
            Entity active = findEntity(player, id);
            if (active != null && recordId.equals(active.getPersistentData().getString("sl_shadow_record"))) return true;
        }
        return false;
    }

    private static boolean safeDestination(ServerLevel level, Entity entity, double x, double y, double z) {
        BlockPos feet = BlockPos.containing(x, y, z);
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        AABB moved = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
        return level.noCollision(entity, moved)
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private static void teleportNearOwner(ServerPlayer owner, Mob mob) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = owner.getX() + owner.getRandom().nextInt(9) - 4;
            double y = owner.getY();
            double z = owner.getZ() + owner.getRandom().nextInt(9) - 4;
            if (safeDestination(owner.serverLevel(), mob, x, y, z)) {
                mob.teleportTo(x, y, z);
                mob.fallDistance = 0.0F;
                return;
            }
        }
    }

    public static void clearRecords(ServerPlayer player) {
        dismissAll(player);
        HunterData.setShadows(player, new ListTag());
        HunterData.sync(player);
    }

    private static List<UUID> activeIds(ServerPlayer player) {
        List<UUID> ids = new ArrayList<>();
        ListTag list = HunterData.activeShadows(player);
        for (int i = 0; i < list.size(); i++) {
            try { ids.add(UUID.fromString(list.getString(i))); } catch (IllegalArgumentException ignored) { }
        }
        return ids;
    }

    private static Entity findEntity(ServerPlayer player, UUID id) {
        if (player.getServer() == null) return null;
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    private ShadowHandler() {}
}
