package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 4 orchestrator for the authored modular structure pack.
 *
 * <p>Java owns placement budgets, marker contracts, doors, validation and cleanup. Room geometry,
 * palettes, landmarks and detail live in replaceable structure templates.</p>
 */
public final class MasterDungeonBuilder {
    public static final String ID = "master";
    public static final String DISPLAY_NAME = "Abyssal Necropolis";
    public static final int SHELL_THICKNESS = 2;
    public static final int MAX_BLOCK_VISITS_PER_TICK = 16_384;
    public static final int MAX_BLOCK_CHANGES_PER_TICK = 4_096;

    public static final int MIN_X = -48;
    public static final int MAX_X = 54;
    public static final int MIN_Y = 0;
    public static final int MAX_Y = 27;
    public static final int MIN_Z = -112;
    public static final int MAX_Z = 223;

    // Phase 3's code-authored arena envelope, retained only for one-time saved-session migration.
    private static final BlockPos PHASE_3_MIN = new BlockPos(-89, -17, -105);
    private static final BlockPos PHASE_3_MAX = new BlockPos(83, 32, 159);

    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState RESTORE = Blocks.DEEPSLATE.defaultBlockState();

    private static final List<BlockPos> OBJECTIVE_CENTERS = List.of(
            marker("objective:0"), marker("objective:1"), marker("objective:2"), marker("objective:3"),
            marker("objective:4"), marker("objective:5"), marker("objective:6"), marker("boss"), marker("reward")
    );
    private static final List<BlockPos> ROUTE_CONNECTORS = List.of(
            new BlockPos(0, 1, -81), new BlockPos(0, 1, -80),
            new BlockPos(-8, 1, -54), new BlockPos(-8, 1, -53),
            new BlockPos(-1, 1, -34), new BlockPos(0, 1, -34),
            new BlockPos(8, 1, -14), new BlockPos(8, 1, -13),
            new BlockPos(0, 1, 30), new BlockPos(0, 1, 31),
            new BlockPos(-8, 1, 64), new BlockPos(-8, 1, 65),
            new BlockPos(-1, 1, 85), new BlockPos(0, 1, 85),
            new BlockPos(8, 1, 105), new BlockPos(8, 1, 106),
            new BlockPos(0, 1, 149), new BlockPos(0, 1, 150),
            new BlockPos(0, 1, 176), new BlockPos(0, 1, 177),
            new BlockPos(23, 1, 207), new BlockPos(24, 1, 207)
    );

    private static final List<Door> CHECKPOINTS = List.of(
            Door.zPlane(-16, 1, 6, 5, 11),
            Door.zPlane(28, 1, 6, -3, 3),
            Door.zPlane(62, 1, 6, -11, -5),
            Door.xPlane(-1, 1, 6, 82, 88),
            Door.zPlane(103, 1, 6, 5, 11),
            Door.zPlane(147, 1, 6, -3, 3),
            Door.zPlane(174, 1, 6, -3, 3),
            Door.xPlane(21, 1, 6, 204, 210)
    );
    private static final Door SHORTCUT_DOOR = Door.zPlane(9, 1, 6, -19, -15);

    private static final Map<String, BlockPos> REGION_POINTS = Map.ofEntries(
            Map.entry("entrance", marker("entry")),
            Map.entry("necropolis", marker("objective:0")),
            Map.entry("guardian", marker("objective:1")),
            Map.entry("catacombs", marker("objective:2")),
            Map.entry("bridge", marker("objective:3")),
            Map.entry("prison", marker("objective:4")),
            Map.entry("elite", marker("objective:5")),
            Map.entry("ritual", marker("objective:6")),
            Map.entry("arena", marker("boss")),
            Map.entry("reward", marker("reward")),
            // Compatibility aliases retained for existing operator scripts.
            Map.entry("ossuary", marker("objective:0")),
            Map.entry("foundry", marker("objective:3")),
            Map.entry("plaza", marker("objective:2")),
            Map.entry("basilica", marker("objective:6")),
            Map.entry("archive", marker("objective:5")),
            Map.entry("court", marker("objective:6"))
    );

    public static BuildJob buildJob(BlockPos origin) { return new BuildJob("build", origin, null); }
    public static BuildJob clearJob(BlockPos origin) { return new BuildJob("clear", origin, null); }
    public static BuildJob rebuildJob(BlockPos origin) { return new BuildJob("rebuild", origin, null); }
    public static BuildJob layoutUpgradeJob(BlockPos oldOrigin, BlockPos newOrigin) {
        return new BuildJob("upgrade", newOrigin, oldOrigin);
    }
    public static BuildJob legacyMigrationJob(BlockPos oldOrigin, BlockPos newOrigin) {
        return new BuildJob("migration", newOrigin, oldOrigin);
    }

    public static AABB bounds(BlockPos origin) {
        return new AABB(origin.getX() + MIN_X, origin.getY() + MIN_Y, origin.getZ() + MIN_Z,
                origin.getX() + MAX_X + 1, origin.getY() + MAX_Y + 1, origin.getZ() + MAX_Z + 1);
    }

    public static BlockPos entry() { return marker("entry"); }
    public static BlockPos boss() { return marker("boss"); }
    public static BlockPos reward() { return marker("reward"); }
    public static List<BlockPos> objectiveCenters() { return OBJECTIVE_CENTERS; }
    public static int checkpointCount() { return CHECKPOINTS.size(); }

    public static void closeCheckpoints(ServerLevel level, BlockPos origin) {
        for (Door checkpoint : CHECKPOINTS) checkpoint.close(level, origin);
        SHORTCUT_DOOR.close(level, origin);
    }

    public static void openCheckpoint(ServerLevel level, BlockPos origin, int index) {
        if (index >= 0 && index < CHECKPOINTS.size()) CHECKPOINTS.get(index).open(level, origin);
        if (index >= 2) SHORTCUT_DOOR.open(level, origin);
    }

    public static BlockPos region(String raw) {
        return raw == null ? null : REGION_POINTS.get(raw.trim().toLowerCase(Locale.ROOT));
    }

    public static String location(BlockPos local) {
        if (local == null) return "outside";
        for (DungeonStructureTemplates.ModuleDefinition module : DungeonStructureTemplates.MODULES) {
            BlockPos max = module.maximum();
            if (local.getX() >= module.offset().getX() && local.getX() <= max.getX()
                    && local.getY() >= module.offset().getY() && local.getY() <= max.getY()
                    && local.getZ() >= module.offset().getZ() && local.getZ() <= max.getZ()) {
                return module.id();
            }
        }
        return "outside";
    }

    public static String info() {
        return DISPLAY_NAME + " | 12 modular structure templates | bounds 103x28x336 | "
                + "9 objectives | 3 secrets | 1 unlockable shortcut | staged 4,096 changes/tick";
    }

    public static List<String> validate(ServerLevel level, BlockPos origin) {
        List<String> errors = new ArrayList<>();
        DungeonStructureTemplates.LoadedPack pack;
        try {
            pack = DungeonStructureTemplates.load(level);
        } catch (RuntimeException exception) {
            errors.add("template pack: " + exception.getMessage());
            return errors;
        }
        for (DungeonStructureTemplates.ModuleDefinition module : DungeonStructureTemplates.MODULES) {
            if (pack.moduleLightCounts().getOrDefault(module.id(), 0) < 4) {
                errors.add("module " + module.id() + " has fewer than four authored light blocks");
            }
        }
        for (Map.Entry<String, BlockPos> marker : pack.markers().entrySet()) {
            String id = marker.getKey();
            if (id.equals("entry") || id.equals("boss") || id.equals("reward")
                    || id.startsWith("objective:") || id.startsWith("enemy:")) {
                validateStandingMarker(level, origin, marker.getValue(), errors);
            }
        }
        for (BlockPos connector : ROUTE_CONNECTORS) validateStandingMarker(level, origin, connector, errors);
        for (DungeonStructureTemplates.ModuleDefinition module : DungeonStructureTemplates.MODULES) {
            BlockPos offset = origin.offset(module.offset());
            BlockPos max = origin.offset(module.maximum());
            if (level.getBlockState(offset).isAir() || level.getBlockState(max).isAir()) {
                errors.add("open containment corner in module " + module.id());
            }
        }
        return errors;
    }

    private static void validateStandingMarker(ServerLevel level, BlockPos origin, BlockPos relative, List<String> errors) {
        BlockPos feet = origin.offset(relative);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos standing = feet.offset(dx, 0, dz);
            if (!level.getFluidState(standing).isEmpty() || !level.getFluidState(standing.above()).isEmpty()) {
                errors.add("liquid near marker " + relative.toShortString());
                return;
            }
            if (!level.getBlockState(standing).getCollisionShape(level, standing).isEmpty()
                    || !level.getBlockState(standing.above()).getCollisionShape(level, standing.above()).isEmpty()) {
                errors.add("blocked clearance near marker " + relative.toShortString());
                return;
            }
            BlockPos floor = standing.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                errors.add("unsupported clearance near marker " + relative.toShortString());
                return;
            }
        }
    }

    public static void debugBounds(ServerLevel level, BlockPos origin) {
        int[] xs = {MIN_X, MAX_X};
        int[] ys = {MIN_Y, MAX_Y};
        int[] zs = {MIN_Z, MAX_Z};
        for (int x : xs) for (int y : ys) for (int z : zs) {
            BlockPos pos = origin.offset(x, y, z);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    8, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    public record TickReport(int visitedBlocks, int changedBlocks, boolean complete) {}

    public record JobReport(String mode, long plannedVisits, long visitedBlocks, long changedBlocks,
                            int elapsedTicks, int maxVisitedInTick, int maxChangedInTick) {
        public double progress() {
            return plannedVisits <= 0L ? 0.0D : Math.min(1.0D, visitedBlocks / (double)plannedVisits);
        }
    }

    public static final class BuildJob {
        private final String mode;
        private final BlockPos origin;
        private final BlockPos oldOrigin;
        private final List<JobOperation> operations = new ArrayList<>();
        private boolean prepared;
        private int operationIndex;
        private long plannedVisits;
        private long visited;
        private long changed;
        private int elapsedTicks;
        private int maxVisitedInTick;
        private int maxChangedInTick;

        private BuildJob(String mode, BlockPos origin, BlockPos oldOrigin) {
            this.mode = mode;
            this.origin = origin.immutable();
            this.oldOrigin = oldOrigin == null ? null : oldOrigin.immutable();
        }

        public TickReport tick(ServerLevel level) {
            if (!prepared) prepare(level);
            if (complete()) return new TickReport(0, 0, true);
            int visitedThisTick = 0;
            int changedThisTick = 0;
            while (operationIndex < operations.size() && visitedThisTick < MAX_BLOCK_VISITS_PER_TICK
                    && changedThisTick < MAX_BLOCK_CHANGES_PER_TICK) {
                OperationStep step = operations.get(operationIndex).apply(level,
                        MAX_BLOCK_VISITS_PER_TICK - visitedThisTick,
                        MAX_BLOCK_CHANGES_PER_TICK - changedThisTick);
                visitedThisTick += step.visitedBlocks();
                changedThisTick += step.changedBlocks();
                if (step.complete()) operationIndex++;
                if (step.visitedBlocks() == 0 && !step.complete()) break;
            }
            visited += visitedThisTick;
            changed += changedThisTick;
            elapsedTicks++;
            maxVisitedInTick = Math.max(maxVisitedInTick, visitedThisTick);
            maxChangedInTick = Math.max(maxChangedInTick, changedThisTick);
            return new TickReport(visitedThisTick, changedThisTick, complete());
        }

        private void prepare(ServerLevel level) {
            prepared = true;
            if (mode.equals("migration") && oldOrigin != null) {
                operations.add(new BoxOperation(oldOrigin.offset(-48, -2, -48), oldOrigin.offset(48, 20, 48), AIR));
            }
            if (mode.equals("upgrade")) {
                BlockPos upgradeOrigin = oldOrigin == null ? origin : oldOrigin;
                operations.add(new BoxOperation(upgradeOrigin.offset(PHASE_3_MIN),
                        upgradeOrigin.offset(PHASE_3_MAX), RESTORE));
            }
            if (mode.equals("clear") || mode.equals("rebuild")) appendRestoreOperations();
            if (!mode.equals("clear")) {
                for (DungeonStructureTemplates.ModuleDefinition module : DungeonStructureTemplates.MODULES) {
                    operations.add(new BoxOperation(origin.offset(module.offset()), origin.offset(module.maximum()), AIR));
                }
                DungeonStructureTemplates.LoadedPack pack = DungeonStructureTemplates.load(level);
                operations.add(new TemplateOperation(origin, pack.blocks()));
            }
            plannedVisits = operations.stream().mapToLong(JobOperation::volume).sum();
        }

        private void appendRestoreOperations() {
            for (DungeonStructureTemplates.ModuleDefinition module : DungeonStructureTemplates.MODULES) {
                operations.add(new BoxOperation(origin.offset(module.offset()), origin.offset(module.maximum()), RESTORE));
            }
        }

        public boolean complete() { return prepared && operationIndex >= operations.size(); }
        public String mode() { return mode; }
        public long plannedVisits() { return plannedVisits; }
        public long visitedBlocks() { return visited; }
        public long changedBlocks() { return changed; }
        public int elapsedTicks() { return elapsedTicks; }
        public double progress() { return plannedVisits <= 0L ? 0.0D : Math.min(1.0D, visited / (double)plannedVisits); }
        public JobReport report() {
            return new JobReport(mode, plannedVisits, visited, changed, elapsedTicks,
                    maxVisitedInTick, maxChangedInTick);
        }
    }

    private interface JobOperation {
        long volume();
        OperationStep apply(ServerLevel level, int visitBudget, int changeBudget);
    }

    private record OperationStep(int visitedBlocks, int changedBlocks, boolean complete) {}

    private static final class BoxOperation implements JobOperation {
        private final int minX, maxX, minY, maxY, minZ, maxZ;
        private final BlockState state;
        private int x, y, z;
        private boolean complete;

        private BoxOperation(BlockPos first, BlockPos second, BlockState state) {
            minX = Math.min(first.getX(), second.getX()); maxX = Math.max(first.getX(), second.getX());
            minY = Math.min(first.getY(), second.getY()); maxY = Math.max(first.getY(), second.getY());
            minZ = Math.min(first.getZ(), second.getZ()); maxZ = Math.max(first.getZ(), second.getZ());
            this.state = state;
            x = minX; y = minY; z = minZ;
        }

        public long volume() { return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1); }

        public OperationStep apply(ServerLevel level, int visitBudget, int changeBudget) {
            if (complete || visitBudget <= 0 || changeBudget <= 0) return new OperationStep(0, 0, complete);
            int visited = 0;
            int changed = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            while (!complete && visited < visitBudget && changed < changeBudget) {
                cursor.set(x, y, z);
                visited++;
                if (!level.getBlockState(cursor).equals(state)) {
                    level.setBlock(cursor, state, UPDATE_FLAGS);
                    changed++;
                }
                advance();
            }
            return new OperationStep(visited, changed, complete);
        }

        private void advance() {
            if (x < maxX) x++;
            else if (z < maxZ) { x = minX; z++; }
            else if (y < maxY) { x = minX; z = minZ; y++; }
            else complete = true;
        }
    }

    private static final class TemplateOperation implements JobOperation {
        private final BlockPos origin;
        private final List<DungeonStructureTemplates.TemplateBlock> blocks;
        private int index;

        private TemplateOperation(BlockPos origin, List<DungeonStructureTemplates.TemplateBlock> blocks) {
            this.origin = origin.immutable();
            this.blocks = blocks;
        }

        public long volume() { return blocks.size(); }

        public OperationStep apply(ServerLevel level, int visitBudget, int changeBudget) {
            int visited = 0;
            int changed = 0;
            while (index < blocks.size() && visited < visitBudget && changed < changeBudget) {
                DungeonStructureTemplates.TemplateBlock templateBlock = blocks.get(index++);
                BlockPos world = origin.offset(templateBlock.relativePosition());
                visited++;
                if (!level.getBlockState(world).equals(templateBlock.state())) {
                    level.setBlock(world, templateBlock.state(), UPDATE_FLAGS);
                    changed++;
                }
                loadBlockEntity(level, world, templateBlock.blockEntityData());
            }
            return new OperationStep(visited, changed, index >= blocks.size());
        }
    }

    private static void loadBlockEntity(ServerLevel level, BlockPos pos, CompoundTag source) {
        if (source == null) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return;
        CompoundTag data = source.copy();
        data.putInt("x", pos.getX()); data.putInt("y", pos.getY()); data.putInt("z", pos.getZ());
        blockEntity.load(data);
        blockEntity.setChanged();
    }

    private record Door(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        private static Door xPlane(int x, int minY, int maxY, int minZ, int maxZ) {
            return new Door(x, x, minY, maxY, minZ, maxZ);
        }
        private static Door zPlane(int z, int minY, int maxY, int minX, int maxX) {
            return new Door(minX, maxX, minY, maxY, z, z);
        }
        private void close(ServerLevel level, BlockPos origin) { setBox(level, origin, this, Blocks.IRON_BARS.defaultBlockState()); }
        private void open(ServerLevel level, BlockPos origin) { setBox(level, origin, this, AIR); }
    }

    private static void setBox(ServerLevel level, BlockPos origin, Door door, BlockState state) {
        for (int x = door.minX; x <= door.maxX; x++) for (int y = door.minY; y <= door.maxY; y++) {
            for (int z = door.minZ; z <= door.maxZ; z++) {
                BlockPos pos = origin.offset(x, y, z);
                if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, UPDATE_FLAGS);
            }
        }
    }

    private static BlockPos marker(String id) {
        BlockPos position = DungeonStructureTemplates.markerPosition(id);
        if (position == null) throw new IllegalStateException("Unknown master-dungeon marker " + id);
        return position;
    }

    private MasterDungeonBuilder() {}
}
