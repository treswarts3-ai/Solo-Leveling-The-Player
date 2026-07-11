package com.tre.sololeveling.dungeon;

import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

public final class DungeonArena {
    public static final int RADIUS = 48;
    public static final int HEIGHT = 24;

    private static final int STRUCTURE_UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final int TEMPLATE_MIN_X = -44;
    private static final int TEMPLATE_MIN_Y = -2;
    private static final int TEMPLATE_MIN_Z = -44;
    private static final int SHELL_MIN_X = -46;
    private static final int SHELL_MAX_X = 46;
    private static final int SHELL_MIN_Y = -4;
    private static final int SHELL_MAX_Y = 20;
    private static final int SHELL_MIN_Z = -46;
    private static final int SHELL_MAX_Z = 46;

    private static final Map<String, Layout> LAYOUTS = Map.of(
            "abandoned_subway", new Layout(
                    structure("abandoned_subway"), Blocks.DEEPSLATE.defaultBlockState(),
                    offset(0, 1, -38),
                    List.of(offset(-5, 1, -8), offset(35, 1, 2), offset(25, 1, 21),
                            offset(0, 1, 35), offset(-36, 1, 38)),
                    offset(0, 1, 35), offset(-36, 1, 38),
                    List.of(
                            Doorway.xPlane(29, 1, 4, -7, -3, Blocks.IRON_BARS.defaultBlockState()),
                            Doorway.zPlane(15, 1, 4, 33, 37, Blocks.IRON_BARS.defaultBlockState()),
                            Doorway.zPlane(28, 1, 5, 18, 22, Blocks.IRON_BARS.defaultBlockState()),
                            Doorway.xPlane(-24, 1, 4, 32, 36, Blocks.IRON_BARS.defaultBlockState())
                    )),
            "red_orc_outpost", new Layout(
                    structure("red_orc_outpost"), Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
                    offset(0, 1, -39),
                    List.of(offset(0, 1, -22), offset(0, 1, 14), offset(-18, 1, 33), offset(24, 1, 39)),
                    offset(-18, 1, 33), offset(24, 1, 39),
                    List.of(
                            Doorway.zPlane(-13, 1, 5, -4, 4, Blocks.IRON_BARS.defaultBlockState()),
                            Doorway.zPlane(24, 1, 5, -4, 4, Blocks.IRON_BARS.defaultBlockState()),
                            Doorway.xPlane(10, 1, 5, 32, 36, Blocks.IRON_BARS.defaultBlockState())
                    )),
            "demon_castle_foyer", new Layout(
                    structure("demon_castle_foyer"), Blocks.BLACKSTONE.defaultBlockState(),
                    offset(0, 2, -39),
                    List.of(offset(0, 1, -16), offset(33, 1, 2), offset(0, 1, 10),
                            offset(0, 1, 34), offset(-36, 1, 39)),
                    offset(0, 1, 34), offset(-36, 1, 39),
                    List.of(
                            Doorway.zPlane(-7, 1, 6, 28, 34, Blocks.RED_STAINED_GLASS.defaultBlockState()),
                            Doorway.xPlane(19, 1, 6, 12, 16, Blocks.RED_STAINED_GLASS.defaultBlockState()),
                            Doorway.zPlane(23, 1, 6, -4, 4, Blocks.RED_STAINED_GLASS.defaultBlockState()),
                            Doorway.xPlane(-28, 1, 6, 32, 36, Blocks.RED_STAINED_GLASS.defaultBlockState())
                    )),
            "cartenon_temple", new Layout(
                    structure("cartenon_temple"), Blocks.STONE.defaultBlockState(),
                    offset(0, 2, -39),
                    List.of(offset(0, 1, -8), offset(-30, 1, 13), offset(0, 1, 10),
                            offset(0, 1, 34), offset(39, 1, 40)),
                    offset(0, 1, 34), offset(39, 1, 40),
                    List.of(
                            Doorway.zPlane(5, 1, 6, -37, -31, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                            Doorway.xPlane(-18, 1, 6, 13, 17, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                            Doorway.zPlane(25, 1, 7, -4, 4, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                            Doorway.xPlane(34, 1, 6, 33, 37, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState())
                    ))
    );

    public static BlockPos originForSlot(int slot) {
        int x = slot % 40;
        int z = slot / 40;
        return new BlockPos(50000 + x * 104, -32, 50000 + z * 104);
    }

    public static AABB bounds(DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        return new AABB(origin.getX() - RADIUS, origin.getY() - 5, origin.getZ() - RADIUS,
                origin.getX() + RADIUS + 1, origin.getY() + HEIGHT, origin.getZ() + RADIUS + 1);
    }

    public static boolean build(ServerLevel level, DungeonSession session) {
        Layout layout = LAYOUTS.get(session.templateId());
        if (layout == null) return false;
        loadArenaChunks(level, session);
        restoreUndergroundVolume(level, session, layout.shell());
        BlockPos corner = session.arenaOrigin().offset(TEMPLATE_MIN_X, TEMPLATE_MIN_Y, TEMPLATE_MIN_Z);
        StructureTemplate template = level.getStructureManager().get(layout.structureId()).orElse(null);
        if (template == null || template.getSize().getX() <= 0 || template.getSize().getY() <= 0
                || template.getSize().getZ() <= 0) {
            session.setArenaBuilt(false);
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        if (!template.placeInWorld(level, corner, corner, settings, level.getRandom(), STRUCTURE_UPDATE_FLAGS)) {
            session.setArenaBuilt(false);
            return false;
        }
        for (Doorway doorway : layout.checkpoints()) doorway.close(level, session.arenaOrigin());
        session.setArenaBuilt(true);
        return true;
    }

    public static BlockPos encounterCenter(DungeonSession session) {
        Layout layout = layout(session);
        int index = session.objectiveIndex();
        BlockPos relative = index >= 0 && index < layout.objectiveCenters().size()
                ? layout.objectiveCenters().get(index) : layout.reward();
        return session.arenaOrigin().offset(relative);
    }

    public static BlockPos bossCenter(DungeonSession session) {
        return session.arenaOrigin().offset(layout(session).boss());
    }

    public static BlockPos rewardCenter(DungeonSession session) {
        return session.arenaOrigin().offset(layout(session).reward());
    }

    public static BlockPos entryPoint(DungeonSession session) {
        return session.arenaOrigin().offset(layout(session).entry());
    }

    public static int checkpointCount(DungeonSession session) {
        return layout(session).checkpoints().size();
    }

    public static void openCheckpoint(ServerLevel level, DungeonSession session, int completedObjective) {
        Layout layout = layout(session);
        if (completedObjective < 0 || completedObjective >= layout.checkpoints().size()) return;
        layout.checkpoints().get(completedObjective).open(level, session.arenaOrigin());
    }

    public static void buildRewardRoom(ServerLevel level, DungeonSession session) {
        BlockPos chestPos = rewardCenter(session);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            chest.setItem(11, new ItemStack(Items.GOLDEN_APPLE, 2));
            chest.setItem(13, new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
            chest.setItem(15, new ItemStack(Items.COOKED_BEEF, 16));
            chest.setChanged();
        }
        session.setRewardRoomCreated(true);
    }

    public static BlockPos findSafePlayerPosition(ServerLevel level, BlockPos preferred, int radius) {
        if (isSafePlayerPosition(level, preferred)) return preferred.immutable();
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos above = preferred.above(dy);
            if (isSafePlayerPosition(level, above)) return above.immutable();
        }
        for (int searchRadius = 1; searchRadius <= Math.max(1, radius); searchRadius++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    if (Math.abs(dx) != searchRadius && Math.abs(dz) != searchRadius) continue;
                    for (int dy = 2; dy >= -3; dy--) {
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

    public static void clear(ServerLevel level, DungeonSession session) {
        if (session.arenaOrigin().getY() > 0) {
            clearLegacyArena(level, session);
            return;
        }
        loadArenaChunks(level, session);
        Layout layout = LAYOUTS.get(session.templateId());
        restoreUndergroundVolume(level, session,
                layout == null ? Blocks.DEEPSLATE.defaultBlockState() : layout.shell());
        session.setArenaBuilt(false);
    }

    public static void clearLegacyArena(ServerLevel level, DungeonSession session) {
        if (session.arenaOrigin().getY() <= 0) return;
        loadArenaChunks(level, session);
        fill(level, session.arenaOrigin(), -48, 48, -2, 20, -48, 48, Blocks.AIR.defaultBlockState());
        session.setArenaBuilt(false);
    }

    public static void placeGateMarker(ServerLevel level, DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                boolean frame = x == -2 || x == 2 || y == 0 || y == 4;
                set(level, base.offset(x, y, 0), frame
                        ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : DungeonBlocks.GATE_PORTAL.get().defaultBlockState());
            }
        }
    }

    public static AABB gateTrigger(DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        return new AABB(base.getX() - 1.15D, base.getY() + 0.5D, base.getZ() - 0.75D,
                base.getX() + 2.15D, base.getY() + 4.0D, base.getZ() + 1.75D);
    }

    public static void removeGateMarker(ServerLevel level, DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = base.offset(x, y, 0);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.PURPLE_STAINED_GLASS)
                        || state.is(DungeonBlocks.GATE_PORTAL.get())) {
                    set(level, pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    public static void discardSessionEntities(ServerLevel level, DungeonSession session) {
        loadArenaChunks(level, session);
        for (Entity entity : level.getEntities((Entity)null, bounds(session), entity ->
                entity.getPersistentData().hasUUID(DungeonTypes.TAG_SESSION)
                        && session.sessionId().equals(entity.getPersistentData().getUUID(DungeonTypes.TAG_SESSION)))) {
            entity.discard();
        }
        session.trackedEntities().clear();
        session.setLiveEnemyCount(0);
    }

    private static void restoreUndergroundVolume(ServerLevel level, DungeonSession session, BlockState shell) {
        fill(level, session.arenaOrigin(), SHELL_MIN_X, SHELL_MAX_X, SHELL_MIN_Y, SHELL_MAX_Y,
                SHELL_MIN_Z, SHELL_MAX_Z, shell);
    }

    private static void fill(ServerLevel level, BlockPos origin, int minX, int maxX, int minY, int maxY,
                             int minZ, int maxZ, BlockState state) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) set(level, origin.offset(x, y, z), state);
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, STRUCTURE_UPDATE_FLAGS);
    }

    private static void loadArenaChunks(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        int minChunkX = (origin.getX() - RADIUS) >> 4;
        int maxChunkX = (origin.getX() + RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + RADIUS) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) level.getChunk(chunkX, chunkZ);
        }
    }

    private static ResourceLocation structure(String id) {
        return new ResourceLocation(SoloLevelingMod.MODID, "dungeons/" + id);
    }

    private static BlockPos offset(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private static Layout layout(DungeonSession session) {
        Layout layout = LAYOUTS.get(session.templateId());
        if (layout == null) throw new IllegalStateException("No dungeon layout for " + session.templateId());
        return layout;
    }

    private record Layout(ResourceLocation structureId, BlockState shell, BlockPos entry,
                          List<BlockPos> objectiveCenters, BlockPos boss, BlockPos reward,
                          List<Doorway> checkpoints) {}

    private record Doorway(int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                           BlockState barrier) {
        private static Doorway xPlane(int x, int minY, int maxY, int minZ, int maxZ, BlockState barrier) {
            return new Doorway(x, x, minY, maxY, minZ, maxZ, barrier);
        }

        private static Doorway zPlane(int z, int minY, int maxY, int minX, int maxX, BlockState barrier) {
            return new Doorway(minX, maxX, minY, maxY, z, z, barrier);
        }

        private void close(ServerLevel level, BlockPos origin) {
            fill(level, origin, minX, maxX, minY, maxY, minZ, maxZ, barrier);
        }

        private void open(ServerLevel level, BlockPos origin) {
            fill(level, origin, minX, maxX, minY, maxY, minZ, maxZ, Blocks.AIR.defaultBlockState());
        }
    }

    private DungeonArena() {}
}
