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
import net.minecraft.world.entity.monster.Enemy;
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
    private static final int PENALTY_BUILD_OPERATIONS = 4_290;
    private static final int PENALTY_BUILD_PER_TICK = 512;

    public static void tick(ServerPlayer player) {
        QuestManager.tick(player);
        CompoundTag tag = HunterData.mutable(player);
        if (!HunterData.isAwakened(player)) return;
        ProgressionHandler.tick(player);
        resetActivityDailyIfNeeded(player, tag);

        if (tag.getBoolean("penalty_building")) {
            tickPenaltyBuild(player, tag);
            return;
        }

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

    private static void resetActivityDailyIfNeeded(ServerPlayer player, CompoundTag tag) {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        if (today.equals(tag.getString("activity_daily_date"))) return;
        tag.putString("activity_daily_date", today);
        tag.putInt("daily_attacks", 0);
        tag.putInt("daily_jumps", 0);
        tag.putInt("daily_abilities", 0);
        tag.putDouble("daily_run_precise", 0.0D);
        tag.putBoolean("daily_was_grounded", player.onGround());
        tag.putLong("daily_last_jump", Long.MIN_VALUE / 4L);
        tag.putLong("daily_last_attack_tick", Long.MIN_VALUE / 4L);
        tag.putLong("daily_last_ability_tick", Long.MIN_VALUE / 4L);
        tag.putDouble("daily_last_x", player.getX());
        tag.putDouble("daily_last_y", player.getY());
        tag.putDouble("daily_last_z", player.getZ());
        tag.putString("daily_last_dimension", player.level().dimension().location().toString());
    }

    private static void trackMovement(ServerPlayer player, CompoundTag tag) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String dimension = player.level().dimension().location().toString();
        boolean sameDimension = dimension.equals(tag.getString("daily_last_dimension"));

        if (sameDimension && tag.contains("daily_last_x") && player.isSprinting() && player.onGround()) {
            double dx = x - tag.getDouble("daily_last_x");
            double dy = y - tag.getDouble("daily_last_y");
            double dz = z - tag.getDouble("daily_last_z");
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.01D && distance <= 1.5D && Math.abs(dy) <= 1.25D) {
                double precise = Math.min(DAILY_RUN, tag.getDouble("daily_run_precise") + distance);
                tag.putDouble("daily_run_precise", precise);
                tag.putInt("daily_run", (int)Math.min(DAILY_RUN, Math.floor(precise)));
            }
        }

        tag.putDouble("daily_last_x", x);
        tag.putDouble("daily_last_y", y);
        tag.putDouble("daily_last_z", z);
        tag.putString("daily_last_dimension", dimension);

        boolean wasGrounded = tag.getBoolean("daily_was_grounded");
        boolean grounded = player.onGround();
        long now = player.level().getGameTime();
        if (wasGrounded && !grounded && player.getDeltaMovement().y > 0.18D
                && now - tag.getLong("daily_last_jump") >= 8L) {
            tag.putInt("daily_jumps", Math.min(DAILY_JUMPS, tag.getInt("daily_jumps") + 1));
            tag.putLong("daily_last_jump", now);
        }
        tag.putBoolean("daily_was_grounded", grounded);
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (!HunterData.isAwakened(player) || victim.getPersistentData().getBoolean("sl_shadow")) return;
        QuestApi.onKill(player, victim);
        CompoundTag tag = HunterData.mutable(player);
        if (victim instanceof Enemy && !victim.getPersistentData().getBoolean("sl_penalty_mob")) {
            tag.putInt("daily_kills", Math.min(DAILY_KILLS, tag.getInt("daily_kills") + 1));
        }
        ProgressionChoiceHandler.onRankTrialKill(player, victim);
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
        long now = player.level().getGameTime();
        if (now - tag.getLong("daily_last_ability_tick") < 10L) return;
        tag.putLong("daily_last_ability_tick", now);
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
        HunterData.setMana(player, HunterData.getMaxMana(player));
        player.sendSystemMessage(Component.literal("[SYSTEM] Daily rewards claimed: 3 stat points, 250 gold, 500 XP, Blessed Box.").withStyle(ChatFormatting.GOLD));
        HunterData.sync(player);
        return true;
    }

    public static void sendToPenalty(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getBoolean("penalty_active") || tag.getBoolean("penalty_building")) return;
        tag.putBoolean("penalty_building", true);
        tag.putBoolean("penalty_active", false);
        tag.putString("penalty_return_dimension", player.level().dimension().location().toString());
        tag.putDouble("penalty_return_x", player.getX());
        tag.putDouble("penalty_return_y", player.getY());
        tag.putDouble("penalty_return_z", player.getZ());
        int offset = Math.abs(player.getUUID().hashCode() % 20000);
        int baseX = 100000 + offset;
        int baseZ = 100000 + offset;
        int baseY = 120;
        tag.putInt("penalty_arena_x", baseX);
        tag.putInt("penalty_arena_y", baseY);
        tag.putInt("penalty_arena_z", baseZ);
        tag.putInt("penalty_build_index", 0);
        tag.putString("penalty_arena_dimension", player.level().dimension().location().toString());
        player.sendSystemMessage(Component.literal("[SYSTEM] Constructing penalty arena in bounded server-tick batches...")
                .withStyle(ChatFormatting.RED));
        HunterData.sync(player);
    }

    public static void returnFromPenalty(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getBoolean("penalty_building")) {
            tag.putBoolean("penalty_building", false);
            tag.putInt("penalty_build_index", 0);
            player.sendSystemMessage(Component.literal("[SYSTEM] Penalty transfer canceled.").withStyle(ChatFormatting.GRAY));
            HunterData.sync(player);
            return;
        }
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
        resetActivityDailyIfNeeded(player, tag);
        QuestManager.resetDaily(player, true);
        HunterData.sync(player);
    }

    private static void tickPenaltyBuild(ServerPlayer player, CompoundTag tag) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("penalty_arena_dimension"));
        ServerLevel level = dimensionId == null || player.getServer() == null ? null
                : player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null) {
            tag.putBoolean("penalty_building", false);
            player.sendSystemMessage(Component.literal("[SYSTEM] Penalty arena dimension is unavailable.").withStyle(ChatFormatting.RED));
            HunterData.sync(player);
            return;
        }
        int index = Math.max(0, tag.getInt("penalty_build_index"));
        int end = Math.min(PENALTY_BUILD_OPERATIONS, index + PENALTY_BUILD_PER_TICK);
        int x = tag.getInt("penalty_arena_x"), y = tag.getInt("penalty_arena_y"), z = tag.getInt("penalty_arena_z");
        for (; index < end; index++) placePenaltyOperation(level, x, y, z, index);
        tag.putInt("penalty_build_index", index);
        if (index < PENALTY_BUILD_OPERATIONS) return;

        tag.putBoolean("penalty_building", false);
        tag.putBoolean("penalty_active", true);
        tag.putLong("penalty_end", level.getGameTime() + 20L * 60L);
        player.teleportTo(level, x + 0.5D, y + 1.0D, z + 0.5D, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("[PENALTY QUEST] Survive for 60 seconds.")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        HunterData.sync(player);
    }

    private static void placePenaltyOperation(ServerLevel level, int x, int y, int z, int index) {
        BlockPos position;
        if (index < 3_750) {
            int cell = index / 6;
            int layer = index % 6;
            int dx = cell / 25 - 12;
            int dz = cell % 25 - 12;
            position = new BlockPos(x + dx, y + layer, z + dz);
            level.setBlock(position, layer == 0 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), 2);
            return;
        }
        int wall = index - 3_750;
        int point = wall / 4;
        int side = wall % 4;
        int i = point / 5 - 13;
        int dy = point % 5 + 1;
        position = switch (side) {
            case 0 -> new BlockPos(x - 13, y + dy, z + i);
            case 1 -> new BlockPos(x + 13, y + dy, z + i);
            case 2 -> new BlockPos(x + i, y + dy, z - 13);
            default -> new BlockPos(x + i, y + dy, z + 13);
        };
        level.setBlock(position, Blocks.BARRIER.defaultBlockState(), 2);
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
