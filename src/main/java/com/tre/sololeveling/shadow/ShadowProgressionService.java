package com.tre.sololeveling.shadow;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.ability.AbilityMastery;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Shadow XP, ranks, stat scaling, domain effects, and restoration state. */
public final class ShadowProgressionService {
    private static final long RESTORE_TICKS = 20L * 15L;

    public static int xpNeeded(int level) {
        int safe = Math.max(1, level);
        return Math.max(50, (int)Math.floor(50.0D * Math.pow(safe, 1.35D)));
    }

    public static boolean addXp(ServerPlayer owner, String selector, int amount) {
        if (amount <= 0) return false;
        CompoundTag record = ShadowStorage.findMutable(owner, selector);
        if (record == null) return false;
        int level = Math.max(1, record.getInt("level"));
        int oldLevel = level;
        int xp = Math.max(0, record.getInt("xp")) + amount;
        while (level < ShadowStorage.MAX_LEVEL && xp >= xpNeeded(level)) {
            xp -= xpNeeded(level);
            level++;
        }
        record.putInt("level", level);
        record.putInt("xp", xp);
        ShadowStorage.Rank oldRank = ShadowStorage.rank(record);
        ShadowStorage.Rank newRank = rankForLevel(level, oldRank);
        record.putString("rank", newRank.display());
        if (level > oldLevel) {
            owner.sendSystemMessage(Component.literal("[SHADOW] " + record.getString("name") + " reached level " + level + ".")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (newRank != oldRank) {
            owner.sendSystemMessage(Component.literal("[SHADOW] " + record.getString("name") + " advanced to " + newRank.display() + ".")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        Entity active = ShadowSummoningService.findEntity(owner.getServer(), ShadowStorage.activeEntityId(record));
        if (active instanceof Mob mob) applyStats(owner, mob, record, true);
        HunterData.sync(owner);
        return true;
    }

    public static void onShadowKill(ServerPlayer owner, Entity shadow, LivingEntity victim) {
        UUID recordId = ShadowSummoningService.recordId(shadow);
        if (recordId == null || !ShadowSummoningService.isOwnedBy(shadow, owner.getUUID())) return;
        int reward = Math.max(1, (int)Math.ceil(victim.getMaxHealth() / 4.0D + victim.getArmorValue()));
        addXp(owner, recordId.toString(), reward);
    }

    public static void onShadowDeath(ServerPlayer owner, Entity shadow) {
        UUID recordId = ShadowSummoningService.recordId(shadow);
        if (recordId == null) return;
        CompoundTag record = ShadowStorage.findMutable(owner, recordId.toString());
        if (record == null) return;
        record.putBoolean("active", false);
        record.putString("active_entity", "");
        record.putLong("restore_at", owner.level().getGameTime() + RESTORE_TICKS);
        record.putInt("defeats", Math.max(0, record.getInt("defeats")) + 1);
        HunterData.sync(owner);
    }

    public static boolean restored(ServerPlayer owner, CompoundTag record) {
        return record.getLong("restore_at") <= owner.level().getGameTime();
    }

    public static long remainingRestoreTicks(ServerPlayer owner, CompoundTag record) {
        return Math.max(0L, record.getLong("restore_at") - owner.level().getGameTime());
    }

    public static void tick(ServerPlayer owner) {
        if (owner.tickCount % 20 != 0) return;
        for (UUID entityId : ShadowSummoningService.activeIds(owner)) {
            Entity entity = ShadowSummoningService.findEntity(owner.getServer(), entityId);
            if (!(entity instanceof Mob mob) || !ShadowSummoningService.isOwnedBy(mob, owner.getUUID())) continue;
            UUID recordId = ShadowSummoningService.recordId(mob);
            CompoundTag record = recordId == null ? null : ShadowStorage.findMutable(owner, recordId.toString());
            if (record != null) applyStats(owner, mob, record, false);
        }
    }

    public static void applyStats(ServerPlayer owner, Mob mob, CompoundTag record, boolean refillHealth) {
        int level = Math.max(1, record.getInt("level"));
        ShadowStorage.Rank rank = ShadowStorage.rank(record);
        double elite = record.getBoolean("named_elite") ? 1.10D : 1.0D;
        boolean inDomain = HunterData.domainActive(owner)
                && mob.distanceToSqr(owner) <= Math.pow(AbilityMastery.domainRadius(owner), 2.0D);
        double domain = inDomain ? 1.20D : 1.0D;
        double scalar = rank.multiplier() * elite * domain;

        apply(mob.getAttribute(Attributes.MAX_HEALTH), modifierId(record, "health"), "Shadow Vitality",
                Math.min(400.0D, level * 0.45D * scalar));
        apply(mob.getAttribute(Attributes.ATTACK_DAMAGE), modifierId(record, "damage"), "Shadow Strength",
                Math.min(80.0D, level * 0.12D * scalar));
        apply(mob.getAttribute(Attributes.ARMOR), modifierId(record, "armor"), "Shadow Armor",
                Math.min(30.0D, level * 0.05D * scalar));
        apply(mob.getAttribute(Attributes.MOVEMENT_SPEED), modifierId(record, "speed"), "Shadow Speed",
                Math.min(0.12D, level * 0.00035D * scalar));

        mob.getPersistentData().putInt(ShadowSummoningService.TAG_LEVEL, level);
        mob.getPersistentData().putString(ShadowSummoningService.TAG_RANK, rank.display());
        if (refillHealth) mob.setHealth(mob.getMaxHealth());
        else if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
        if (inDomain) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 1, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
        }
    }

    public static ShadowStorage.Rank rankForLevel(int level, ShadowStorage.Rank current) {
        ShadowStorage.Rank earned;
        if (level >= 200) earned = ShadowStorage.Rank.GRAND_MARSHAL;
        else if (level >= 120) earned = ShadowStorage.Rank.MARSHAL;
        else if (level >= 80) earned = ShadowStorage.Rank.COMMANDER;
        else if (level >= 50) earned = ShadowStorage.Rank.ELITE_KNIGHT;
        else if (level >= 25) earned = ShadowStorage.Rank.KNIGHT;
        else if (level >= 10) earned = ShadowStorage.Rank.ELITE;
        else earned = ShadowStorage.Rank.NORMAL;
        return earned.ordinal() > current.ordinal() ? earned : current;
    }

    public static void addCapacity(ServerPlayer owner, int amount) {
        HunterData.addShadowCapacity(owner, Math.max(0, amount));
    }

    private static UUID modifierId(CompoundTag record, String stat) {
        return UUID.nameUUIDFromBytes(("sololeveling:shadow:" + record.getString("record_id") + ":" + stat)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static void apply(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) attribute.removeModifier(id);
        if (amount != 0.0D) attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private ShadowProgressionService() {}
}
