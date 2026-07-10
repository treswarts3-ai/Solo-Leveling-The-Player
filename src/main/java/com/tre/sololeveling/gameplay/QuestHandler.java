package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
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

public final class QuestHandler {
    private static final int DAILY_KILLS = 10;
    private static final int DAILY_RUN = 1000;
    private static final int DAILY_EXERCISE = 30;

    public static void tick(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (!HunterData.isAwakened(player)) return;
        ProgressionHandler.tick(player);
        tickExercise(player, tag);
        if (tag.getBoolean("penalty_pending") && !tag.getBoolean("penalty_active") && player.tickCount % 100 == 0) {
            tag.putBoolean("penalty_pending", false);
            sendToPenalty(player);
            return;
        }

        double x = player.getX();
        double z = player.getZ();
        if (tag.contains("last_x") && player.isSprinting() && player.onGround()) {
            double dx = x - tag.getDouble("last_x");
            double dz = z - tag.getDouble("last_z");
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 8.0D) tag.putDouble("daily_run_precise", tag.getDouble("daily_run_precise") + distance);
            tag.putInt("daily_run", (int)tag.getDouble("daily_run_precise"));
        }
        tag.putDouble("last_x", x);
        tag.putDouble("last_z", z);
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

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (!HunterData.isAwakened(player) || victim.getPersistentData().getBoolean("sl_shadow")) return;
        CompoundTag tag = HunterData.mutable(player);
        tag.putInt("daily_kills", tag.getInt("daily_kills") + 1);
        ProgressionHandler.onKill(player);
        checkCompletion(player);
    }

    public static boolean exercise(ServerPlayer player, String type) {
        CompoundTag tag = HunterData.mutable(player);
        long now = player.level().getGameTime();
        if (tag.getBoolean("exercise_active") || !player.onGround()
                || player.getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                || now - tag.getLong("daily_last_exercise") < 10L) return false;
        String key = exerciseKey(type);
        if (key.isEmpty() || tag.getInt(key) >= DAILY_EXERCISE) return false;
        tag.putBoolean("exercise_active", true);
        tag.putString("exercise_type", type);
        tag.putLong("exercise_start", now);
        tag.putLong("exercise_finish", now + 24L);
        tag.putDouble("exercise_x", player.getX());
        tag.putDouble("exercise_y", player.getY());
        tag.putDouble("exercise_z", player.getZ());
        tag.putLong("exercise_combat_mark", tag.getLong("last_combat"));
        player.setPose(type.equals("squat") ? net.minecraft.world.entity.Pose.CROUCHING : net.minecraft.world.entity.Pose.SWIMMING);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        HunterData.sync(player);
        return true;
    }

    private static void tickExercise(ServerPlayer player, CompoundTag tag) {
        if (!tag.getBoolean("exercise_active")) return;
        double moved = player.distanceToSqr(tag.getDouble("exercise_x"), tag.getDouble("exercise_y"), tag.getDouble("exercise_z"));
        boolean interrupted = moved > 0.16D || !player.onGround()
                || player.getDeltaMovement().horizontalDistanceSqr() > 0.01D
                || tag.getLong("last_combat") != tag.getLong("exercise_combat_mark");
        if (interrupted) {
            tag.putBoolean("exercise_active", false);
            player.setPose(net.minecraft.world.entity.Pose.STANDING);
            player.sendSystemMessage(Component.literal("[SYSTEM] Exercise interrupted.").withStyle(ChatFormatting.GRAY));
            HunterData.sync(player);
            return;
        }
        if (player.level().getGameTime() < tag.getLong("exercise_finish")) return;
        String key = exerciseKey(tag.getString("exercise_type"));
        if (!key.isEmpty()) tag.putInt(key, Math.min(DAILY_EXERCISE, tag.getInt(key) + 1));
        tag.putBoolean("exercise_active", false);
        tag.putLong("daily_last_exercise", player.level().getGameTime());
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        checkCompletion(player);
        HunterData.sync(player);
    }

    private static String exerciseKey(String type) {
        return switch (type) {
            case "pushup" -> "daily_pushups";
            case "situp" -> "daily_situps";
            case "squat" -> "daily_squats";
            default -> "";
        };
    }

    private static void checkCompletion(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        boolean complete = tag.getInt("daily_kills") >= DAILY_KILLS
                && tag.getInt("daily_run") >= DAILY_RUN
                && tag.getInt("daily_pushups") >= DAILY_EXERCISE
                && tag.getInt("daily_situps") >= DAILY_EXERCISE
                && tag.getInt("daily_squats") >= DAILY_EXERCISE;
        if (complete && !tag.getBoolean("daily_complete")) {
            tag.putBoolean("daily_complete", true);
            player.sendSystemMessage(Component.literal("[QUEST COMPLETE] Preparation to Become Powerful").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
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

    public static void resetDaily(ServerPlayer player) { HunterData.resetDaily(player); }

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
