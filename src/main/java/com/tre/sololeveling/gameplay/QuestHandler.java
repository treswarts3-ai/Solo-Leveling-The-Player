package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.quest.QuestApi;
import com.tre.sololeveling.quest.QuestManager;
import com.tre.sololeveling.registry.ModItems;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.time.LocalDate;
import java.time.ZoneOffset;

public final class QuestHandler {
    private static final int DAILY_KILLS = 8;
    private static final int DAILY_RUN = 600;
    private static final int DAILY_ATTACKS = 20;
    private static final int DAILY_JUMPS = 15;
    private static final int DAILY_ABILITIES = 5;

    public static void tick(ServerPlayer player) {
        QuestManager.tick(player);
        CompoundTag tag = HunterData.mutable(player);
        if (!HunterData.isAwakened(player)) return;
        ProgressionHandler.tick(player);
        resetActivityDailyIfNeeded(tag);

        if (tag.getBoolean("penalty_pending") && !tag.getBoolean("penalty_active") && player.tickCount % 100 == 0) {
            tag.putBoolean("penalty_pending", false);
            sendToPenalty(player);
            return;
        }

        trackMovement(player, tag);
        checkCompletion(player);

        if (tag.getBoolean("penalty_active")) {
            long now = player.level().getGameTime();
            if (now >= tag.getLong("penalty_end")) {
                returnFromPenalty(player);
            } else if (player.tickCount % 200 == 0) {
                long nearby = player.serverLevel().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(24.0D)).stream()
                        .filter(e -> e.getPersistentData().getBoolean("sl_penalty_mob")).count();
                if (nearby < 5) spawnPenaltyMob(player);
            }
        }
    }

    private static void resetActivityDailyIfNeeded(CompoundTag tag) {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        if (today.equals(tag.getString("activity_daily_date"))) return;
        tag.putString("activity_daily_date", today);
        tag.putInt("daily_attacks", 0);
        tag.putInt("daily_jumps", 0);
        tag.putInt("daily_abilities", 0);
        tag.putBoolean("daily_was_grounded", false);
        tag.putLong("daily_last_jump", 0L);
    }

    private static void trackMovement(ServerPlayer player, CompoundTag tag) {
        double x = player.getX();
        double z = player.getZ();
        if (tag.contains("last_x") && player.isSprinting() && player.onGround()) {
            double dx = x - tag.getDouble("last_x");
            double dz = z - tag.getDouble("last_z");
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 8.0D) tag.putDouble("daily_run_precise", tag.getDouble("daily_run_precise") + distance);
            tag.putInt("daily_run", (int)Math.min(DAILY_RUN, tag.getDouble("daily_run_precise")));
        }
        tag.putDouble("last_x", x);
        tag.putDouble("last_z", z);

        boolean wasGrounded = tag.getBoolean("daily_was_grounded");
        boolean grounded = player.onGround();
        long now = player.level().getGameTime();
        if (wasGrounded && !grounded && player.getDeltaMovement().y > 0.18D && now - tag.getLong("daily_last_jump") >= 8L) {
            tag.putInt("daily_jumps", Math.min(DAILY_JUMPS, tag.getInt("daily_jumps") + 1));
            tag.putLong("daily_last_jump", now);
        }
        tag.putBoolean("daily_was_grounded", grounded);
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (!HunterData.isAwakened(player) || victim.getPersistentData().getBoolean("sl_shadow")) return;
        QuestApi.onKill(player, victim);
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("daily_kills", Math.min(DAILY_KILLS, tag.getInt("daily_kills") + 1));
        ProgressionHandler.onKill(player);
        checkCompletion(player);
    }

    public static void onAttack(ServerPlayer player) {
        if (!HunterData.isAwakened(player)) return;
        CompoundTag tag = HunterData.mutable(player);
        long now = player.level().getGameTime();
        if (now == tag.getLong("daily_last_attack_tick")) return;
        tag.putLong("daily_last_attack_tick", now);
        tag.putInt("daily_attacks", Math.min(DAILY_ATTACKS, tag.getInt("daily_attacks") + 1));
        checkCompletion(player);
    }

    public static void onAbilityUse(ServerPlayer player) {
        if (!HunterData.isAwakened(player)) return;
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("daily_abilities", Math.min(DAILY_ABILITIES, tag.getInt("daily_abilities") + 1));
        checkCompletion(player);
    }

    /** Compatibility entry point retained for old clients. Exercise buttons no longer progress the daily quest. */
    public static boolean exercise(ServerPlayer player, String type) {
        player.sendSystemMessage(Component.literal("[SYSTEM] Daily training now progresses through movement and combat.").withStyle(ChatFormatting.AQUA));
        return false;
    }

    private static void checkCompletion(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        boolean complete = tag.getInt("daily_kills") >= DAILY_KILLS
                && tag.getInt("daily_run") >= DAILY_RUN
                && tag.getInt("daily_attacks") >= DAILY_ATTACKS
                && tag.getInt("daily_jumps") >= DAILY_JUMPS
                && tag.getInt("daily_abilities") >= DAILY_ABILITIES;
        if (complete && !tag.getBoolean("daily_complete")) {
            tag.putBoolean("daily_complete", true);
            player.sendSystemMessage(Component.literal("[QUEST COMPLETE] Adaptive Hunter Training").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            player.level().playSound(null, player.blockPosition(), ModSounds.QUEST_COMPLETE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            HunterData.sync(player);
        }
    }

    public static boolean claimDaily(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!tag.getBoolean("daily_complete") || tag.getBoolean("daily_claimed")) return false;
        tag.putBoolean("daily_claimed", true);
        HunterData.addStatPoints(player, 3);
        HunterData.addGold(player, 250);
        HunterData.addXp(player, 500);
        HunterData.storeSystemItem(player, new net.minecraft.world.item.ItemStack(ModItems.BLESSED_RANDOM_BOX.get()));
        player.setHealth(player.getMaxHealth());
        HunterData.addMana(player, HunterData.getMaxMana(player));
        player.sendSystemMessage(Component.literal("[SYSTEM] Daily rewards claimed: 3 stat points, 250 gold, 500 XP, Blessed Box.").withStyle(ChatFormatting.GOLD));
        HunterData.sync(player);
        return true;
    }

    public static void sendToPenalty(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getBoolean("penalty_active")) return;
        tag.putBoolean("penalty_active", true);
        tag.putString("penalty_return_dimension", player.level().dimension().location().toString());
        tag.putDouble("penalty_return_x", player.getX());
        tag.putDouble("penalty_return_y", player.getY());
        tag.putDouble("penalty_return_z", player.getZ());
        int offset = Math.abs(player.getUUID().hashCode() % 20000);
        int baseX = 100000 + offset;
        int baseZ = 100000 + offset;
        int baseY = 120;
        buildPenaltyArena(player.serverLevel(), baseX, baseY, baseZ);
        player.teleportTo(player.serverLevel(), baseX + 0.5D, baseY + 1.0D, baseZ + 0.5D, player.getYRot(), player.getXRot());
        tag.putLong("penalty_end", player.level().getGameTime() + 20L * 60L);
        player.sendSystemMessage(Component.literal("[PENALTY QUEST] Survive for 60 seconds.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        HunterData.sync(player);
    }

    public static void returnFromPenalty(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!tag.getBoolean("penalty_active")) return;
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("penalty_return_dimension"));
        ServerLevel target = null;
        if (dimensionId != null) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimensionId);
            target = player.getServer() == null ? null : player.getServer().getLevel(key);
        }
        if (target == null && player.getServer() != null) target = player.getServer().overworld();
        if (target == null) return;
        player.teleportTo(target, tag.getDouble("penalty_return_x"), tag.getDouble("penalty_return_y"), tag.getDouble("penalty_return_z"), player.getYRot(), player.getXRot());
        tag.putBoolean("penalty_active", false);
        player.sendSystemMessage(Component.literal("[SYSTEM] Penalty Quest survived.").withStyle(ChatFormatting.AQUA));
        HunterData.sync(player);
    }

    public static void resetDaily(ServerPlayer player) {
        HunterData.resetDaily(player);
        CompoundTag tag = HunterData.mutable(player);
        tag.putString("activity_daily_date", "");
        resetActivityDailyIfNeeded(tag);
        QuestManager.resetDaily(player, true);
        HunterData.sync(player);
    }

    private static void buildPenaltyArena(ServerLevel level, int x, int y, int z) {
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 3);
                for (int dy = 1; dy <= 5; dy++) level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (int i = -13; i <= 13; i++) {
            for (int dy = 1; dy <= 5; dy++) {
                level.setBlock(new BlockPos(x - 13, y + dy, z + i), Blocks.BARRIER.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x + 13, y + dy, z + i), Blocks.BARRIER.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x + i, y + dy, z - 13), Blocks.BARRIER.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x + i, y + dy, z + 13), Blocks.BARRIER.defaultBlockState(), 3);
            }
        }
    }

    private static void spawnPenaltyMob(ServerPlayer player) {
        Husk husk = EntityType.HUSK.create(player.serverLevel());
        if (husk == null) return;
        husk.moveTo(player.getX() + player.getRandom().nextInt(17) - 8, player.getY(), player.getZ() + player.getRandom().nextInt(17) - 8, 0, 0);
        husk.getPersistentData().putBoolean("sl_penalty_mob", true);
        husk.setTarget(player);
        player.serverLevel().addFreshEntity(husk);
    }

    private QuestHandler() {}
}
