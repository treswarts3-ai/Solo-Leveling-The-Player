package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DungeonArena {
    public static final int RADIUS = 22;
    public static final int HEIGHT = 8;

    public static BlockPos originForSlot(int slot) {
        int x = slot % 48;
        int z = slot / 48;
        return new BlockPos(50000 + x * 64, 80, 50000 + z * 64);
    }

    public static AABB bounds(DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        return new AABB(origin.getX() - RADIUS - 2, origin.getY(), origin.getZ() - RADIUS - 2,
                origin.getX() + RADIUS + 3, origin.getY() + HEIGHT + 5, origin.getZ() + RADIUS + 3);
    }

    public static void build(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        clear(level, session);
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) level.setBlock(origin.offset(x, 0, z), floor, 3);
        }
        for (int y = 1; y <= HEIGHT; y++) {
            for (int i = -RADIUS; i <= RADIUS; i++) {
                level.setBlock(origin.offset(i, y, -RADIUS), wall, 3);
                level.setBlock(origin.offset(i, y, RADIUS), wall, 3);
                level.setBlock(origin.offset(-RADIUS, y, i), wall, 3);
                level.setBlock(origin.offset(RADIUS, y, i), wall, 3);
            }
        }
        for (int x : new int[]{-RADIUS + 1, RADIUS - 1}) {
            for (int z : new int[]{-RADIUS + 1, RADIUS - 1}) level.setBlock(origin.offset(x, 1, z), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 1, -RADIUS + 1), Blocks.LODESTONE.defaultBlockState(), 3);
        session.setArenaBuilt(true);
    }

    public static void buildRewardRoom(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        BlockState wall = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState floor = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        int minX = -5, maxX = 5, minZ = 13, maxZ = 21;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(origin.offset(x, 0, z), floor, 3);
                level.setBlock(origin.offset(x, 6, z), wall, 3);
            }
        }
        for (int y = 1; y <= 5; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!(y <= 3 && x >= -1 && x <= 1)) level.setBlock(origin.offset(x, y, minZ), wall, 3);
                level.setBlock(origin.offset(x, y, maxZ), wall, 3);
            }
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(origin.offset(minX, y, z), wall, 3);
                level.setBlock(origin.offset(maxX, y, z), wall, 3);
            }
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
        for (int x : new int[]{-4, 4}) level.setBlock(origin.offset(x, 2, 19), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        session.setRewardRoomCreated(true);
    }

    public static BlockPos rewardCenter(DungeonSession session) { return session.arenaOrigin().offset(0, 1, 18); }
    public static BlockPos entryPoint(DungeonSession session) { return session.arenaOrigin().offset(0, 1, -15); }

    public static void clear(ServerLevel level, DungeonSession session) {
        BlockPos origin = session.arenaOrigin();
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                for (int y = 0; y <= HEIGHT + 3; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void placeGateMarker(ServerLevel level, DungeonTypes.GateDefinition gate) {
        BlockPos base = gate.position();
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                boolean frame = x == -2 || x == 2 || y == 0 || y == 4;
                level.setBlock(base.offset(x, y, 0), frame ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), 3);
            }
        }
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
        for (Entity entity : level.getEntities((Entity)null, bounds(session), entity ->
                entity.getPersistentData().hasUUID(DungeonTypes.TAG_SESSION)
                        && session.sessionId().equals(entity.getPersistentData().getUUID(DungeonTypes.TAG_SESSION)))) {
            entity.discard();
        }
        session.trackedEntities().clear();
        session.setLiveEnemyCount(0);
    }

    private DungeonArena() {}
}
