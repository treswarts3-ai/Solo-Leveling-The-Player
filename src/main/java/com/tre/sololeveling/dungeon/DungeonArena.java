package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runtime bridge between dungeon sessions, gates, and the one master layout. */
public final class DungeonArena {
    private static final int STRUCTURE_UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final int SLOT_COLUMNS = 32;
    private static final int SLOT_X_SPACING = 224;
    private static final int SLOT_Z_SPACING = 320;
    private static final int GATE_MIN_X = -3;
    private static final int GATE_MAX_X = 3;
    private static final int GATE_MIN_Y = 0;
    private static final int GATE_MAX_Y = 6;
    private static final Map<UUID, ActiveArenaJob> ACTIVE_JOBS = new LinkedHashMap<>();

    public enum JobPurpose { BUILD, REBUILD, MIGRATION, CLEAR }

    public record JobResult(UUID sessionId, JobPurpose purpose, boolean success,
                            MasterDungeonBuilder.JobReport report, String error) {}

    public record JobStatus(JobPurpose purpose, String mode, double progress, long visitedBlocks,
                            long plannedVisits, long changedBlocks, int elapsedTicks) {}

    private record ActiveArenaJob(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                  JobPurpose purpose, MasterDungeonBuilder.BuildJob job) {}

    public static BlockPos originForSlot(int slot) {
        int x = slot % SLOT_COLUMNS;
        int z = slot / SLOT_COLUMNS;
        return new BlockPos(50_000 + x * SLOT_X_SPACING, -32, 50_000 + z * SLOT_Z_SPACING);
    }

    public static AABB bounds(DungeonSession session) {
        if (MasterDungeonBuilder.ID.equals(session.templateId())) {
            return MasterDungeonBuilder.bounds(session.arenaOrigin());
        }
        BlockPos origin = session.arenaOrigin();
        return new AABB(origin.getX() - 48, origin.getY() - 5, origin.getZ() - 48,
                origin.getX() + 49, origin.getY() + 25, origin.getZ() + 49);
    }

    public static boolean queueBuild(DungeonSession session) {
        if (!MasterDungeonBuilder.ID.equals(session.templateId()) || ACTIVE_JOBS.containsKey(session.sessionId())) {
            return false;
        }
        ACTIVE_JOBS.put(session.sessionId(), new ActiveArenaJob(session.dungeonDimension(), JobPurpose.BUILD,
                MasterDungeonBuilder.buildJob(session.arenaOrigin())));
        return true;
    }

    public static boolean queueRebuild(DungeonSession session) {
        if (!MasterDungeonBuilder.ID.equals(session.templateId()) || ACTIVE_JOBS.containsKey(session.sessionId())) {
            return false;
        }
        ACTIVE_JOBS.put(session.sessionId(), new ActiveArenaJob(session.dungeonDimension(), JobPurpose.REBUILD,
                MasterDungeonBuilder.rebuildJob(session.arenaOrigin())));
        return true;
    }

    public static boolean queueMigration(DungeonSession session, BlockPos oldOrigin) {
        if (!MasterDungeonBuilder.ID.equals(session.templateId()) || ACTIVE_JOBS.containsKey(session.sessionId())) {
            return false;
        }
        ACTIVE_JOBS.put(session.sessionId(), new ActiveArenaJob(session.dungeonDimension(), JobPurpose.MIGRATION,
                MasterDungeonBuilder.legacyMigrationJob(oldOrigin, session.arenaOrigin())));
        return true;
    }

    public static boolean queueClear(DungeonSession session) {
        if (!MasterDungeonBuilder.ID.equals(session.templateId()) || ACTIVE_JOBS.containsKey(session.sessionId())) {
            return false;
        }
        ACTIVE_JOBS.put(session.sessionId(), new ActiveArenaJob(session.dungeonDimension(), JobPurpose.CLEAR,
                MasterDungeonBuilder.clearJob(session.arenaOrigin())));
        return true;
    }

    public static boolean hasJob(UUID sessionId) {
        return ACTIVE_JOBS.containsKey(sessionId);
    }

    public static void resetJobs() {
        ACTIVE_JOBS.clear();
    }

    public static boolean cancelJob(UUID sessionId) {
        return ACTIVE_JOBS.remove(sessionId) != null;
    }

    public static JobStatus jobStatus(UUID sessionId) {
        ActiveArenaJob active = ACTIVE_JOBS.get(sessionId);
        if (active == null) return null;
        MasterDungeonBuilder.BuildJob job = active.job();
        return new JobStatus(active.purpose(), job.mode(), job.progress(), job.visitedBlocks(),
                job.plannedVisits(), job.changedBlocks(), job.elapsedTicks());
    }

    public static List<JobResult> tickJobs(MinecraftServer server) {
        List<JobResult> completed = new ArrayList<>();
        var iterator = ACTIVE_JOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveArenaJob> entry = iterator.next();
            ActiveArenaJob active = entry.getValue();
            ServerLevel level = server.getLevel(active.dimension());
            if (level == null) {
                completed.add(new JobResult(entry.getKey(), active.purpose(), false,
                        active.job().report(), "Dungeon dimension unavailable"));
                iterator.remove();
                continue;
            }
            try {
                MasterDungeonBuilder.TickReport tick = active.job().tick(level);
                if (!tick.complete()) continue;
                completed.add(new JobResult(entry.getKey(), active.purpose(), true,
                        active.job().report(), ""));
            } catch (RuntimeException exception) {
                completed.add(new JobResult(entry.getKey(), active.purpose(), false,
                        active.job().report(), "Generation error: " + exception.getClass().getSimpleName()));
            }
            iterator.remove();
        }
        return completed;
    }

    public static BlockPos encounterCenter(DungeonSession session) {
        return session.arenaOrigin().offset(MasterDungeonBuilder.objectiveCenters().get(Math.min(Math.max(0, session.objectiveIndex()), MasterDungeonBuilder.objectiveCenters().size() - 1)));
    }

    public static BlockPos bossCenter(DungeonSession session) {
        return session.arenaOrigin().offset(MasterDungeonBuilder.boss());
    }

    public static BlockPos rewardCenter(DungeonSession session) {
        return session.arenaOrigin().offset(MasterDungeonBuilder.reward());
    }

    public static BlockPos entryPoint(DungeonSession session) {
        return session.arenaOrigin().offset(MasterDungeonBuilder.entry());
    }

    public static int checkpointCount(DungeonSession session) {
        return MasterDungeonBuilder.checkpointCount();
    }

    public static void openCheckpoint(ServerLevel level, DungeonSession session, int completedObjective) {
        MasterDungeonBuilder.openCheckpoint(level, session.arenaOrigin(), completedObjective);
    }

    public static void buildRewardRoom(ServerLevel level, DungeonSession session) {
        BlockPos chestPos = rewardCenter(session);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            chest.setItem(3, new ItemStack(Items.NETHERITE_SCRAP, 2));
            chest.setItem(5, new ItemStack(Items.DIAMOND, 6));
            chest.setItem(11, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
            chest.setItem(13, new ItemStack(Items.EXPERIENCE_BOTTLE, 16));
            chest.setItem(15, new ItemStack(Items.ECHO_SHARD, 6));
            chest.setItem(21, new ItemStack(Items.GOLD_INGOT, 24));
            chest.setItem(23, new ItemStack(Items.EMERALD, 16));
            chest.setChanged();
        }
        session.setRewardRoomCreated(true);
    }

    public static BlockPos findSafePlayerPosition(ServerLevel level, BlockPos preferred, int radius) {
        if (isSafePlayerPosition(level, preferred)) return preferred.immutable();
        for (int dy = 1; dy <= 6; dy++) {
            BlockPos above = preferred.above(dy);
            if (isSafePlayerPosition(level, above)) return above.immutable();
        }
        for (int searchRadius = 1; searchRadius <= Math.max(1, radius); searchRadius++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    if (Math.abs(dx) != searchRadius && Math.abs(dz) != searchRadius) continue;
                    for (int dy = 3; dy >= -5; dy--) {
                        BlockPos candidate = preferred.offset(dx, dy, dz);
                        if (isSafePlayerPosition(level, candidate)) return candidate.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafePlayerPosition(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        return level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }


    public static void placeGateMarker(ServerLevel level, DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        BlockState frame = GateVisuals.frame(gate.rank());
        BlockState accent = GateVisuals.accent(gate.rank());
        BlockState portal = DungeonBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GatePortalBlock.RANK, gate.rank());

        for (int x = GATE_MIN_X; x <= GATE_MAX_X; x++) {
            for (int y = GATE_MIN_Y; y <= GATE_MAX_Y; y++) {
                BlockPos pos = base.offset(x, y, 0);
                if (isGateFrameCoordinate(x, y)) {
                    set(level, pos, isGateAccentCoordinate(x, y) ? accent : frame);
                } else if (isGatePortalCoordinate(x, y)) {
                    set(level, pos, portal);
                } else if (GateVisuals.isManagedGateBlock(level.getBlockState(pos))) {
                    set(level, pos, Blocks.AIR.defaultBlockState());
                }
            }
        }

        for (int z : new int[] {-1, 1}) {
            set(level, base.offset(-3, 0, z), accent);
            set(level, base.offset(3, 0, z), accent);
            set(level, base.offset(-3, 1, z), frame);
            set(level, base.offset(3, 1, z), frame);
        }
    }

    public static AABB gateTrigger(DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        return new AABB(base.getX() - 2.35D, base.getY() + 0.5D, base.getZ() - 0.95D,
                base.getX() + 3.35D, base.getY() + 5.8D, base.getZ() + 1.95D);
    }

    public static void removeGateMarker(ServerLevel level, DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        for (int x = GATE_MIN_X; x <= GATE_MAX_X; x++) {
            for (int y = GATE_MIN_Y; y <= GATE_MAX_Y; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = base.offset(x, y, z);
                    if (GateVisuals.isManagedGateBlock(level.getBlockState(pos))) {
                        set(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    private static boolean isGateFrameCoordinate(int x, int y) {
        int absoluteX = Math.abs(x);
        if (y == 0) return absoluteX <= 3;
        if (y >= 1 && y <= 4) return absoluteX == 3;
        if (y == 5) return absoluteX >= 2 && absoluteX <= 3;
        return y == 6 && absoluteX <= 1;
    }

    private static boolean isGatePortalCoordinate(int x, int y) {
        int absoluteX = Math.abs(x);
        return (y >= 1 && y <= 4 && absoluteX <= 2)
                || (y == 5 && absoluteX <= 1);
    }

    private static boolean isGateAccentCoordinate(int x, int y) {
        int absoluteX = Math.abs(x);
        return (y == 0 && (absoluteX == 0 || absoluteX == 3))
                || (y == 4 && absoluteX == 3)
                || (y == 6 && x == 0);
    }

    public static void discardSessionEntities(ServerLevel level, DungeonSession session) {
        for (Entity entity : level.getEntities((Entity)null, bounds(session), entity ->
                entity.getPersistentData().hasUUID(DungeonTypes.TAG_SESSION)
                        && session.sessionId().equals(entity.getPersistentData().getUUID(DungeonTypes.TAG_SESSION)))) {
            entity.discard();
        }
        session.trackedEntities().clear();
        session.setLiveEnemyCount(0);
    }

    public static List<String> validate(ServerLevel level, DungeonSession session) {
        if (session.state() == DungeonTypes.SessionState.BUILDING) {
            clearObjectiveMarkerObstructions(level, session);
        }
        return MasterDungeonBuilder.validate(level, session.arenaOrigin());
    }

    /**
     * Decorations are authored after room carving, so a statue, altar, or rubble pile can
     * overlap a required encounter marker. Clear a small standing volume only during the
     * post-build validation step; later manual validation cannot erase reward containers.
     */
    private static void clearObjectiveMarkerObstructions(ServerLevel level, DungeonSession session) {
        for (BlockPos relative : MasterDungeonBuilder.objectiveCenters()) {
            BlockPos center = session.arenaOrigin().offset(relative);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        set(level, center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    public static BlockPos regionPoint(DungeonSession session, String region) {
        BlockPos relative = MasterDungeonBuilder.region(region);
        return relative == null ? null : session.arenaOrigin().offset(relative);
    }

    public static String info(DungeonSession session) {
        return MasterDungeonBuilder.info() + ", origin=" + session.arenaOrigin().toShortString();
    }

    public static void debugBounds(ServerLevel level, DungeonSession session) {
        MasterDungeonBuilder.debugBounds(level, session.arenaOrigin());
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, STRUCTURE_UPDATE_FLAGS);
    }

    private DungeonArena() {}
}
