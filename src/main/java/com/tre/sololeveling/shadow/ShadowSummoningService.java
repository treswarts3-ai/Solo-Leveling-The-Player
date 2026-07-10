package com.tre.sololeveling.shadow;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Spawns and dismisses stored shadows while preserving record/entity ownership invariants. */
public final class ShadowSummoningService {
    public static final String TAG_SHADOW = "sl_shadow";
    public static final String TAG_OWNER = "sl_owner";
    public static final String TAG_RECORD = "sl_shadow_record";
    public static final String TAG_LEVEL = "sl_shadow_level";
    public static final String TAG_RANK = "sl_shadow_rank";

    public static boolean summonFirst(ServerPlayer owner) {
        CompoundTag record = ShadowStorage.firstInactive(owner);
        return record != null && summon(owner, record, true);
    }

    public static boolean summonFirstValidated(ServerPlayer owner) {
        CompoundTag record = ShadowStorage.firstInactive(owner);
        return record != null && summon(owner, record, false);
    }

    public static boolean summon(ServerPlayer owner, String selector) {
        CompoundTag record = ShadowStorage.findMutable(owner, selector);
        return record != null && summon(owner, record, true);
    }

    public static int summonAll(ServerPlayer owner) {
        int summoned = 0;
        while (activeCount(owner) < activeLimit(owner)) {
            CompoundTag record = ShadowStorage.firstInactive(owner);
            if (record == null || !summon(owner, record, true)) break;
            summoned++;
        }
        return summoned;
    }

    private static boolean summon(ServerPlayer owner, CompoundTag record, boolean chargeMana) {
        if (chargeMana && !HunterData.hasSkill(owner, "shadow_preservation")) return false;
        reconcile(owner);
        if (record.getBoolean("active")) return false;
        if (activeCount(owner) >= activeLimit(owner)) {
            owner.sendSystemMessage(Component.literal("[SYSTEM] Active shadow limit reached.").withStyle(ChatFormatting.RED));
            return false;
        }

        UUID recordId = ShadowStorage.recordId(record);
        ResourceLocation typeId = ResourceLocation.tryParse(record.getString("type"));
        EntityType<?> type = typeId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(typeId);
        if (recordId == null || type == null || existingForRecord(owner, recordId) != null) return false;
        Entity created = type.create(owner.serverLevel());
        if (!(created instanceof Mob mob)) return false;
        BlockPos spawn = findSpawn(owner, mob);
        if (spawn == null) return false;

        int manaCost = Math.max(5, 5 + record.getInt("level") / 2);
        if (chargeMana && !HunterData.spendMana(owner, manaCost)) return false;
        mob.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, owner.getYRot(), 0.0F);
        CompoundTag data = mob.getPersistentData();
        data.putBoolean(TAG_SHADOW, true);
        data.putUUID(TAG_OWNER, owner.getUUID());
        data.putUUID(TAG_RECORD, recordId);
        data.putInt(TAG_LEVEL, Math.max(1, record.getInt("level")));
        data.putString(TAG_RANK, ShadowStorage.rank(record).display());
        mob.setCustomName(Component.literal(record.getString("name")).withStyle(ChatFormatting.DARK_PURPLE));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        mob.setTarget(null);
        mob.setGlowingTag(true);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60, 0, false, false));
        if (!owner.serverLevel().addFreshEntity(mob)) return false;

        ShadowStorage.markActive(owner, record, mob.getUUID());
        addActiveId(owner, mob.getUUID());
        HunterData.mutable(owner).putBoolean("shadow_summoned_once", true);
        owner.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 60, 0.6D, 0.8D, 0.6D, 0.2D);
        owner.level().playSound(null, owner.blockPosition(), ModSounds.SHADOW.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        HunterData.sync(owner);
        return true;
    }

    public static boolean dismiss(ServerPlayer owner, String selector) {
        CompoundTag record = ShadowStorage.findMutable(owner, selector);
        if (record == null || !record.getBoolean("active")) return false;
        UUID entityId = ShadowStorage.activeEntityId(record);
        Entity entity = entityId == null ? null : findEntity(owner.getServer(), entityId);
        if (entity != null && isOwnedBy(entity, owner.getUUID())) dismissEntity(entity);
        ShadowStorage.markInactive(owner, record);
        removeActiveId(owner, entityId);
        HunterData.sync(owner);
        return true;
    }

    public static int dismissAll(ServerPlayer owner) {
        int dismissed = 0;
        ListTag records = ShadowStorage.normalize(owner);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            if (!record.getBoolean("active")) continue;
            UUID entityId = ShadowStorage.activeEntityId(record);
            Entity entity = entityId == null ? null : findEntity(owner.getServer(), entityId);
            if (entity != null && isOwnedBy(entity, owner.getUUID())) dismissEntity(entity);
            record.putBoolean("active", false);
            record.putString("active_entity", "");
            dismissed++;
        }
        HunterData.setActiveShadows(owner, new ListTag());
        HunterData.sync(owner);
        return dismissed;
    }

    public static void reconcile(ServerPlayer owner) {
        ListTag records = ShadowStorage.normalize(owner);
        Set<UUID> live = new HashSet<>();
        Set<UUID> seenRecords = new HashSet<>();
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            UUID recordId = ShadowStorage.recordId(record);
            UUID entityId = ShadowStorage.activeEntityId(record);
            Entity entity = entityId == null ? null : findEntity(owner.getServer(), entityId);
            boolean valid = record.getBoolean("active") && recordId != null && seenRecords.add(recordId)
                    && entity != null && entity.isAlive() && isOwnedBy(entity, owner.getUUID()) && recordId.equals(recordId(entity));
            if (valid) live.add(entityId);
            else {
                record.putBoolean("active", false);
                record.putString("active_entity", "");
            }
        }
        ListTag active = new ListTag();
        for (UUID id : live) active.add(StringTag.valueOf(id.toString()));
        HunterData.setActiveShadows(owner, active);
    }

    public static int activeLimit(ServerPlayer owner) {
        return Math.max(0, Math.min(ModConfigs.ACTIVE_SHADOW_MAX.get(), ShadowStorage.capacity(owner)));
    }
    public static int activeCount(ServerPlayer owner) {
        int count = 0;
        for (UUID ignored : activeIds(owner)) count++;
        return count;
    }

    public static Set<UUID> activeIds(ServerPlayer owner) {
        Set<UUID> ids = new HashSet<>();
        ListTag list = HunterData.activeShadows(owner);
        for (int i = 0; i < list.size(); i++) {
            try { ids.add(UUID.fromString(list.getString(i))); } catch (IllegalArgumentException ignored) { }
        }
        return ids;
    }

    public static boolean isShadow(Entity entity) { return entity.getPersistentData().getBoolean(TAG_SHADOW); }
    public static UUID ownerId(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        return isShadow(entity) && tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
    }
    public static UUID recordId(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        return isShadow(entity) && tag.hasUUID(TAG_RECORD) ? tag.getUUID(TAG_RECORD) : null;
    }
    public static boolean isOwnedBy(Entity entity, UUID owner) { return owner != null && owner.equals(ownerId(entity)); }

    public static Entity findEntity(MinecraftServer server, UUID id) {
        if (server == null || id == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    private static Entity existingForRecord(ServerPlayer owner, UUID record) {
        return owner.serverLevel().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(128.0D),
                entity -> isOwnedBy(entity, owner.getUUID()) && record.equals(recordId(entity))).stream().findFirst().orElse(null);
    }

    private static BlockPos findSpawn(ServerPlayer owner, Mob mob) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = owner.blockPosition().offset(owner.getRandom().nextInt(7) - 3, 0, owner.getRandom().nextInt(7) - 3);
            AABB moved = mob.getBoundingBox().move(pos.getX() + 0.5D - mob.getX(), pos.getY() - mob.getY(), pos.getZ() + 0.5D - mob.getZ());
            if (owner.serverLevel().getWorldBorder().isWithinBounds(pos) && owner.serverLevel().noCollision(mob, moved)
                    && !owner.serverLevel().getBlockState(pos.below()).getCollisionShape(owner.serverLevel(), pos.below()).isEmpty()) return pos;
        }
        return null;
    }

    private static void dismissEntity(Entity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.5D, entity.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.1D);
        }
        entity.discard();
    }

    private static void addActiveId(ServerPlayer owner, UUID id) {
        Set<UUID> ids = activeIds(owner);
        ids.add(id);
        ListTag list = new ListTag();
        for (UUID value : ids) list.add(StringTag.valueOf(value.toString()));
        HunterData.setActiveShadows(owner, list);
    }
    private static void removeActiveId(ServerPlayer owner, UUID id) {
        Set<UUID> ids = activeIds(owner);
        ids.remove(id);
        ListTag list = new ListTag();
        for (UUID value : ids) list.add(StringTag.valueOf(value.toString()));
        HunterData.setActiveShadows(owner, list);
    }

    private ShadowSummoningService() {}
}
