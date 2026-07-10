package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private static final int STRUCTURE_UPDATE_FLAGS = Block.UPDATE_CLIENTS;

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

        // Explicitly open every room/corridor seam. The previous layout left these wall planes intact.
        doorway(level, origin, -35, 1, 3);
        doorway(level, origin, -28, 2, 4);
        doorway(level, origin, -12, 2, 4);
        doorway(level, origin, -6, 2, 4);
        doorway(level, origin, 5, 2, 4);
        doorway(level, origin, 9, 2, 4);
        doorway(level, origin, 18, 2, 4);
        doorway(level, origin, 23, 2, 5);

        // Ticket hall: booths, station marker, turnstiles, and a readable center route.
        fill(level, origin, -5, 5, 1, 1, -41, -41, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        for (int x : new int[]{-4, 4}) {
            column(level, origin.offset(x, 1, -38), 4, Blocks.POLISHED_BASALT.defaultBlockState());
            lamp(level, origin.offset(x, 5, -38));
        }
        ticketBooth(level, origin.offset(-5, 1, -40));
        ticketBooth(level, origin.offset(3, 1, -40));
        fill(level, origin, -3, 3, 4, 4, -43, -43, Blocks.CYAN_CONCRETE.defaultBlockState());
        fill(level, origin, -2, 2, 4, 4, -42, -42, Blocks.SEA_LANTERN.defaultBlockState());
        fill(level, origin, -2, 2, 1, 2, -35, -35, Blocks.IRON_BARS.defaultBlockState());
        carve(level, origin, -1, 1, 1, 3, -35, -35);

        // Platform: raised edges, recessed track bed, rails, columns, benches, rubble, and broken lights.
        fill(level, origin, -15, -3, 1, 1, -27, -13, Blocks.SMOOTH_STONE.defaultBlockState());
        fill(level, origin, 3, 15, 1, 1, -27, -13, Blocks.SMOOTH_STONE.defaultBlockState());
        fill(level, origin, -2, 2, 0, 0, -27, -13, Blocks.GRAVEL.defaultBlockState());
        fill(level, origin, -1, 1, 0, 0, -27, -13, Blocks.DEEPSLATE.defaultBlockState());
        fill(level, origin, -3, -3, 1, 1, -27, -13, Blocks.YELLOW_CONCRETE.defaultBlockState());
        fill(level, origin, 3, 3, 1, 1, -27, -13, Blocks.YELLOW_CONCRETE.defaultBlockState());
        for (int z = -26; z <= -14; z += 2) {
            set(level, origin.offset(-1, 1, z), Blocks.RAIL.defaultBlockState());
            set(level, origin.offset(1, 1, z), Blocks.RAIL.defaultBlockState());
        }
        for (int z : new int[]{-25, -19, -13}) {
            for (int x : new int[]{-12, 12}) {
                column(level, origin.offset(x, 2, z), 4, Blocks.POLISHED_BASALT.defaultBlockState());
                if (z != -19) lamp(level, origin.offset(x, 6, z));
                else set(level, origin.offset(x, 6, z), Blocks.REDSTONE_LAMP.defaultBlockState());
            }
        }
        bench(level, origin.offset(-10, 2, -18));
        bench(level, origin.offset(8, 2, -23));
        rubble(level, origin.offset(-13, 2, -15));
        rubble(level, origin.offset(11, 2, -26));
        fill(level, origin, -15, -13, 2, 4, -27, -27, accent);
        fill(level, origin, 13, 15, 2, 5, -13, -13, accent);
        fill(level, origin, -16, -16, 2, 5, -22, -20, Blocks.BLUE_STAINED_GLASS_PANE.defaultBlockState());
        set(level, origin.offset(-15, 1, -21), Blocks.CAULDRON.defaultBlockState());

        // Maintenance: pipes, machinery silhouettes, leaks, and a small elevation change.
        for (int x : new int[]{-8, 8}) {
            fill(level, origin, x, x, 1, 4, -4, 3, Blocks.COPPER_BLOCK.defaultBlockState());
            fill(level, origin, x - 1, x + 1, 1, 1, 0, 0, Blocks.CUT_COPPER.defaultBlockState());
        }
        fill(level, origin, -9, 9, 5, 5, -4, -4, Blocks.EXPOSED_COPPER.defaultBlockState());
        fill(level, origin, -5, 5, 1, 1, 3, 4, Blocks.POLISHED_ANDESITE.defaultBlockState());
        set(level, origin.offset(-7, 1, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin.offset(7, 1, 3), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(-9, 4, 2), Blocks.LIGHTNING_ROD.defaultBlockState());
        set(level, origin.offset(9, 4, -2), Blocks.LIGHTNING_ROD.defaultBlockState());
        for (int x = -9; x <= 9; x += 6) lamp(level, origin.offset(x, 5, 0));
        rubble(level, origin.offset(4, 1, -3));

        // Security room: barred checkpoint, work stations, and strong room silhouette.
        fill(level, origin, -8, 8, 1, 2, 11, 11, Blocks.IRON_BARS.defaultBlockState());
        carve(level, origin, -1, 1, 1, 3, 11, 11);
        for (int x : new int[]{-7, 7}) {
            column(level, origin.offset(x, 1, 14), 4, Blocks.POLISHED_BASALT.defaultBlockState());
            lamp(level, origin.offset(x, 5, 14));
        }
        set(level, origin.offset(-5, 1, 16), Blocks.SMITHING_TABLE.defaultBlockState());
        set(level, origin.offset(5, 1, 16), Blocks.ANVIL.defaultBlockState());
        fill(level, origin, -3, 3, 5, 5, 18, 18, Blocks.RED_CONCRETE.defaultBlockState());

        // Boss chamber: raised dais, pillars, soul lighting, side rubble, and a sealed vault wall.
        for (int x : new int[]{-13, 13}) {
            for (int z : new int[]{26, 36}) {
                column(level, origin.offset(x, 1, z), 7, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                set(level, origin.offset(x, 8, z), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }
        for (int x = -5; x <= 5; x++) {
            for (int z = 28; z <= 34; z++) {
                if (Math.abs(x) + Math.abs(z - 31) <= 7) {
                    set(level, origin.offset(x, 1, z), Blocks.GILDED_BLACKSTONE.defaultBlockState());
                }
            }
        }
        rubble(level, origin.offset(-14, 1, 31));
        rubble(level, origin.offset(12, 1, 28));
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
            case 0 -> session.arenaOrigin().offset(0, 1, -20);
            case 1 -> session.arenaOrigin().offset(0, 1, 0);
            case 2 -> session.arenaOrigin().offset(0, 1, 14);
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
            room(level, origin, -6, 6, 40, 46, 6, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());
            carve(level, origin, -2, 2, 1, 4, 39, 40);
            for (int x : new int[]{-4, 4}) lamp(level, origin.offset(x, 5, 43));
            fill(level, origin, -2, 2, 1, 1, 44, 45, Blocks.GOLD_BLOCK.defaultBlockState());
        } else {
            room(level, origin, -5, 5, 13, 21, 6, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());
            carve(level, origin, -1, 1, 1, 3, 13, 13);
        }
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

    public static BlockPos rewardCenter(DungeonSession session) {
        return "abandoned_subway".equals(session.templateId())
                ? session.arenaOrigin().offset(0, 1, 43)
                : session.arenaOrigin().offset(0, 1, 18);
    }

    public static BlockPos entryPoint(DungeonSession session) {
        return "abandoned_subway".equals(session.templateId())
                ? session.arenaOrigin().offset(0, 1, -40)
                : session.arenaOrigin().offset(0, 1, -15);
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
        BlockPos origin = session.arenaOrigin();
        if ("abandoned_subway".equals(session.templateId())) {
            clearBox(level, origin, -19, 19, -1, 12, -45, 47);
        } else {
            clearBox(level, origin, -24, 24, -1, 12, -24, 24);
        }
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

    private static void doorway(ServerLevel level, BlockPos origin, int z, int halfWidth, int height) {
        carve(level, origin, -halfWidth, halfWidth, 1, height, z, z);
    }

    private static void ticketBooth(ServerLevel level, BlockPos base) {
        fillAbsolute(level, base, 0, 2, 0, 0, 0, 2, Blocks.POLISHED_ANDESITE.defaultBlockState());
        fillAbsolute(level, base, 0, 2, 1, 2, 0, 0, Blocks.IRON_BARS.defaultBlockState());
        fillAbsolute(level, base, 0, 2, 3, 3, 0, 2, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
    }

    private static void bench(ServerLevel level, BlockPos base) {
        set(level, base, Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, base.offset(1, 0, 0), Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, base.offset(2, 0, 0), Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, base.offset(0, -1, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
        set(level, base.offset(2, -1, 0), Blocks.DARK_OAK_FENCE.defaultBlockState());
    }

    private static void rubble(ServerLevel level, BlockPos base) {
        set(level, base, Blocks.COBBLESTONE.defaultBlockState());
        set(level, base.offset(1, 0, 0), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        set(level, base.offset(0, 0, 1), Blocks.GRAVEL.defaultBlockState());
        set(level, base.offset(1, 1, 1), Blocks.COBBLESTONE_SLAB.defaultBlockState());
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

    private static void clearBox(ServerLevel level, BlockPos origin, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        fill(level, origin, minX, maxX, minY, maxY, minZ, maxZ, Blocks.AIR.defaultBlockState());
    }

    private static void fill(ServerLevel level, BlockPos origin, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) set(level, origin.offset(x, y, z), state);
            }
        }
    }

    private static void fillAbsolute(ServerLevel level, BlockPos base, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, BlockState state) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) set(level, base.offset(x, y, z), state);
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, STRUCTURE_UPDATE_FLAGS);
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
