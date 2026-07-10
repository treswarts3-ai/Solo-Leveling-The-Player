package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
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

public final class DungeonArena {
    public static final int RADIUS = 46;
    public static final int HEIGHT = 14;

    public static BlockPos originForSlot(int slot) {
        int x = slot % 40;
        int z = slot / 40;
        return new BlockPos(50000 + x * 104, 76, 50000 + z * 104);
    }

    public static AABB bounds(DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        return new AABB(origin.getX() - RADIUS - 2, origin.getY() - 2, origin.getZ() - RADIUS - 2,
                origin.getX() + RADIUS + 3, origin.getY() + HEIGHT + 5, origin.getZ() + RADIUS + 3);
    }

    public static void build(ServerLevel level, DungeonSession session) {
        clear(level, session);
        if ("abandoned_subway".equals(session.templateId())) buildAbandonedSubway(level, session);
        else buildFallbackDungeon(level, session);
        session.setArenaBuilt(true);
    }

    private static void buildAbandonedSubway(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState accent = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState ceiling = Blocks.DEEPSLATE_TILES.defaultBlockState();

        room(level, origin, -6, 6, -43, -35, 6, wall, floor, ceiling);
        tunnel(level, origin, -3, 3, -34, -29, 5, wall, floor, ceiling);
        room(level, origin, -16, 16, -28, -12, 7, wall, floor, ceiling);
        tunnel(level, origin, -3, 3, -11, -7, 5, wall, floor, ceiling);
        room(level, origin, -11, 11, -6, 5, 6, wall, floor, ceiling);
        tunnel(level, origin, -3, 3, 6, 8, 5, wall, floor, ceiling);
        room(level, origin, -10, 10, 9, 18, 7, wall, floor, ceiling);
        tunnel(level, origin, -3, 3, 19, 22, 6, wall, floor, ceiling);
        room(level, origin, -17, 17, 23, 39, 10, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());

        // Ticket hall and entrance details.
        fill(level, origin, -5, 5, 1, 1, -41, -41, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        for (int x : new int[]{-4, 4}) {
            column(level, origin.offset(x, 1, -38), 4, Blocks.POLISHED_BASALT.defaultBlockState());
            lamp(level, origin.offset(x, 5, -38));
        }
        fill(level, origin, -2, 2, 1, 2, -35, -35, Blocks.IRON_BARS.defaultBlockState());
        carve(level, origin, -1, 1, 1, 3, -35, -35);

        // Subway platform, tracks, benches, columns, and damaged sections.
        fill(level, origin, -15, 15, 1, 1, -26, -14, Blocks.SMOOTH_STONE.defaultBlockState());
        fill(level, origin, -2, 2, 1, 1, -27, -13, Blocks.GRAVEL.defaultBlockState());
        fill(level, origin, -1, 1, 1, 1, -27, -13, Blocks.DEEPSLATE.defaultBlockState());
        for (int z = -26; z <= -14; z += 2) {
            set(level, origin.offset(-1, 2, z), Blocks.RAIL.defaultBlockState());
            set(level, origin.offset(1, 2, z), Blocks.RAIL.defaultBlockState());
        }
        for (int z : new int[]{-25, -19, -13}) {
            for (int x : new int[]{-12, 12}) {
                column(level, origin.offset(x, 2, z), 4, Blocks.POLISHED_BASALT.defaultBlockState());
                lamp(level, origin.offset(x, 6, z));
            }
        }
        bench(level, origin.offset(-10, 2, -18));
        bench(level, origin.offset(8, 2, -23));
        fill(level, origin, -15, -13, 2, 4, -27, -27, accent);
        fill(level, origin, 13, 15, 2, 5, -13, -13, accent);

        // Maintenance room machinery and pipes.
        for (int x : new int[]{-8, 8}) {
            fill(level, origin, x, x, 2, 4, -4, 3, Blocks.COPPER_BLOCK.defaultBlockState());
            fill(level, origin, x - 1, x + 1, 2, 2, 0, 0, Blocks.CUT_COPPER.defaultBlockState());
        }
        fill(level, origin, -9, 9, 5, 5, -4, -4, Blocks.EXPOSED_COPPER.defaultBlockState());
        set(level, origin.offset(-7, 2, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin.offset(7, 2, 3), Blocks.BARREL.defaultBlockState());
        for (int x = -9; x <= 9; x += 6) lamp(level, origin.offset(x, 6, 0));

        // Security room before the boss chamber.
        fill(level, origin, -8, 8, 2, 2, 11, 11, Blocks.IRON_BARS.defaultBlockState());
        for (int x : new int[]{-7, 7}) {
            column(level, origin.offset(x, 2, 14), 4, Blocks.POLISHED_BASALT.defaultBlockState());
            lamp(level, origin.offset(x, 6, 14));
        }
        set(level, origin.offset(-5, 2, 16), Blocks.SMITHING_TABLE.defaultBlockState());
        set(level, origin.offset(5, 2, 16), Blocks.ANVIL.defaultBlockState());

        // Boss chamber: raised dais, pillars, soul lighting, and readable circular center.
        for (int x : new int[]{-13, 13}) {
            for (int z : new int[]{26, 36}) {
                column(level, origin.offset(x, 2, z), 7, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                set(level, origin.offset(x, 8, z), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }
        for (int x = -5; x <= 5; x++) {
            for (int z = 28; z <= 34; z++) {
                if (Math.abs(x) + Math.abs(z - 31) <= 7) set(level, origin.offset(x, 1, z), Blocks.GILDED_BLACKSTONE.defaultBlockState());
            }
        }
        fill(level, origin, -6, 6, 1, 1, 37, 38, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        fill(level, origin, -5, 5, 2, 6, 39, 39, Blocks.IRON_BARS.defaultBlockState());

        closeCheckpoint(level, session, 0);
        closeCheckpoint(level, session, 1);
        closeCheckpoint(level, session, 2);
    }

    private static void buildFallbackDungeon(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        room(level, origin, -22, 22, -22, 22, 9, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());
        for (int x : new int[]{-18, 18}) {
            for (int z : new int[]{-18, 18}) {
                column(level, origin.offset(x, 1, z), 6, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                lamp(level, origin.offset(x, 7, z));
            }
        }
    }

    public static BlockPos encounterCenter(DungeonSession session) {
        if (!"abandoned_subway".equals(session.templateId())) return session.arenaOrigin().offset(0, 1, 0);
        return switch (session.objectiveIndex()) {
            case 0 -> session.arenaOrigin().offset(0, 2, -20);
            case 1 -> session.arenaOrigin().offset(0, 2, 0);
            case 2 -> session.arenaOrigin().offset(0, 2, 14);
            case 3 -> bossCenter(session);
            default -> rewardCenter(session);
        };
    }

    public static BlockPos bossCenter(DungeonSession session) {
        return "abandoned_subway".equals(session.templateId())
                ? session.arenaOrigin().offset(0, 2, 31)
                : session.arenaOrigin().offset(0, 1, 10);
    }

    public static void openCheckpoint(ServerLevel level, DungeonSession session, int completedObjective) {
        if (!"abandoned_subway".equals(session.templateId())) return;
        BlockPos origin = session.arenaOrigin();
        int z = switch (completedObjective) {
            case 0 -> -10;
            case 1 -> 7;
            case 2 -> 20;
            default -> Integer.MIN_VALUE;
        };
        if (z == Integer.MIN_VALUE) return;
        carve(level, origin, -2, 2, 1, 4, z, z);
        fill(level, origin, -3, -3, 1, 5, z, z, Blocks.POLISHED_BASALT.defaultBlockState());
        fill(level, origin, 3, 3, 1, 5, z, z, Blocks.POLISHED_BASALT.defaultBlockState());
    }

    private static void closeCheckpoint(ServerLevel level, DungeonSession session, int objective) {
        if (!"abandoned_subway".equals(session.templateId())) return;
        int z = switch (objective) {
            case 0 -> -10;
            case 1 -> 7;
            case 2 -> 20;
            default -> 0;
        };
        fill(level, session.arenaOrigin(), -2, 2, 1, 4, z, z, Blocks.IRON_BARS.defaultBlockState());
    }

    public static void buildRewardRoom(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        if ("abandoned_subway".equals(session.templateId())) {
            room(level, origin, -6, 6, 40, 45, 6, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());
            carve(level, origin, -2, 2, 1, 4, 39, 40);
            for (int x : new int[]{-4, 4}) lamp(level, origin.offset(x, 5, 43));
        } else {
            room(level, origin, -5, 5, 13, 21, 6, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());
            carve(level, origin, -1, 1, 1, 3, 13, 13);
        }
        BlockPos chestPos = rewardCenter(session);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            chest.setItem(11, new ItemStack(Items.GOLDEN_APPLE, 2));
            chest.setItem(13, new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
            chest.setItem(15, new ItemStack(Items.COOKED_BEEF, 16));
            chest.setChanged();
        }
        session.setRewardRoomCreated(true);
    }

    public static BlockPos rewardCenter(DungeonSession session) {
        return "abandoned_subway".equals(session.templateId())
                ? session.arenaOrigin().offset(0, 2, 43)
                : session.arenaOrigin().offset(0, 1, 18);
    }

    public static BlockPos entryPoint(DungeonSession session) {
        return "abandoned_subway".equals(session.templateId())
                ? session.arenaOrigin().offset(0, 2, -40)
                : session.arenaOrigin().offset(0, 1, -15);
    }

    public static void clear(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                for (int y = -1; y <= HEIGHT + 3; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void placeGateMarker(ServerLevel level, DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                boolean frame = x == -2 || x == 2 || y == 0 || y == 4;
                level.setBlock(base.offset(x, y, 0), frame ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
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
                if (level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN) || level.getBlockState(pos).is(Blocks.PURPLE_STAINED_GLASS)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
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

    private static void room(ServerLevel level, BlockPos origin, int minX, int maxX, int minZ, int maxZ, int height,
                             BlockState wall, BlockState floor, BlockState ceiling) {
        fill(level, origin, minX, maxX, 0, 0, minZ, maxZ, floor);
        fill(level, origin, minX, maxX, height, height, minZ, maxZ, ceiling);
        fill(level, origin, minX, minX, 1, height - 1, minZ, maxZ, wall);
        fill(level, origin, maxX, maxX, 1, height - 1, minZ, maxZ, wall);
        fill(level, origin, minX, maxX, 1, height - 1, minZ, minZ, wall);
        fill(level, origin, minX, maxX, 1, height - 1, maxZ, maxZ, wall);
        carve(level, origin, minX + 1, maxX - 1, 1, height - 1, minZ + 1, maxZ - 1);
    }

    private static void tunnel(ServerLevel level, BlockPos origin, int minX, int maxX, int minZ, int maxZ, int height,
                               BlockState wall, BlockState floor, BlockState ceiling) {
        room(level, origin, minX, maxX, minZ, maxZ, height, wall, floor, ceiling);
        carve(level, origin, minX + 1, maxX - 1, 1, Math.min(4, height - 1), minZ, minZ);
        carve(level, origin, minX + 1, maxX - 1, 1, Math.min(4, height - 1), maxZ, maxZ);
    }

    private static void bench(ServerLevel level, BlockPos base) {
        set(level, base, Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, base.offset(1, 0, 0), Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, base.offset(2, 0, 0), Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, base.offset(0, -1, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
        set(level, base.offset(2, -1, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
    }

    private static void lamp(ServerLevel level, BlockPos pos) {
        set(level, pos, Blocks.SEA_LANTERN.defaultBlockState());
    }

    private static void column(ServerLevel level, BlockPos base, int height, BlockState state) {
        for (int y = 0; y < height; y++) set(level, base.offset(0, y, 0), state);
    }

    private static void carve(ServerLevel level, BlockPos origin, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        fill(level, origin, minX, maxX, minY, maxY, minZ, maxZ, Blocks.AIR.defaultBlockState());
    }

    private static void fill(ServerLevel level, BlockPos origin, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) set(level, origin.offset(x, y, z), state);
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static void loadArenaChunks(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        int minChunkX = (origin.getX() - RADIUS - 2) >> 4;
        int maxChunkX = (origin.getX() + RADIUS + 2) >> 4;
        int minChunkZ = (origin.getZ() - RADIUS - 2) >> 4;
        int maxChunkZ = (origin.getZ() + RADIUS + 2) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) level.getChunk(chunkX, chunkZ);
        }
    }

    private DungeonArena() {}
}
