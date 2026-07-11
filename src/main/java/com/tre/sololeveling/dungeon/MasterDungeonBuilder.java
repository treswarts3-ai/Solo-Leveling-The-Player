package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic handcrafted geometry for the mod's single active dungeon.
 *
 * <p>The layout is intentionally data-driven but not procedural: every room,
 * graph edge, landmark, encounter floor and shortcut is fixed and reviewed.
 * Runtime variation is limited to combat and rewards.</p>
 */
public final class MasterDungeonBuilder {
    public static final String ID = "master";
    public static final String DISPLAY_NAME = "Abyssal Necropolis";
    public static final int SHELL_THICKNESS = 5;

    public static final int MIN_X = -89;
    public static final int MAX_X = 83;
    public static final int MIN_Y = -17;
    public static final int MAX_Y = 32;
    public static final int MIN_Z = -105;
    public static final int MAX_Z = 159;

    public static final int WALKABLE_FLOOR_AREA = 20_774;
    public static final int CRITICAL_PATH_DISTANCE = 848;
    public static final int OLD_LARGEST_FLOOR_AREA = 4_343;
    public static final int OLD_LARGEST_MAIN_PATH = 184;
    public static final int MEANINGFUL_SPACES = 43;
    public static final int MAJOR_ROOMS = 13;
    public static final int REGION_COUNT = 6;
    public static final int VERTICAL_LEVEL_COUNT = 21;
    public static final int LOOP_COUNT = 6;
    public static final int SHORTCUT_COUNT = 3;
    public static final int SECRET_COUNT = 7;
    public static final int MAX_BLOCK_VISITS_PER_TICK = 16_384;
    public static final int MAX_BLOCK_CHANGES_PER_TICK = 4_096;

    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState RESTORE = Blocks.DEEPSLATE.defaultBlockState();

    private static final Theme GATE = new Theme(
            Blocks.DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
            Blocks.CHISELED_DEEPSLATE.defaultBlockState(), Blocks.CYAN_CONCRETE.defaultBlockState(),
            Blocks.SEA_LANTERN.defaultBlockState());
    private static final Theme OSSUARY = new Theme(
            Blocks.DEEPSLATE.defaultBlockState(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BONE_BLOCK.defaultBlockState(),
            Blocks.SOUL_LANTERN.defaultBlockState());
    private static final Theme FOUNDRY = new Theme(
            Blocks.DEEPSLATE.defaultBlockState(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_BASALT.defaultBlockState(), Blocks.BASALT.defaultBlockState(),
            Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState(),
            Blocks.SHROOMLIGHT.defaultBlockState());
    private static final Theme KINGDOM = new Theme(
            Blocks.DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
            Blocks.CHISELED_DEEPSLATE.defaultBlockState(), Blocks.GILDED_BLACKSTONE.defaultBlockState(),
            Blocks.SOUL_LANTERN.defaultBlockState());
    private static final Theme CATACOMB = new Theme(
            Blocks.DEEPSLATE.defaultBlockState(), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
            Blocks.TUFF.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.POLISHED_BASALT.defaultBlockState(), Blocks.SCULK.defaultBlockState(),
            Blocks.SOUL_LANTERN.defaultBlockState());
    private static final Theme SEPULCHER = new Theme(
            Blocks.OBSIDIAN.defaultBlockState(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState(),
            Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), Blocks.GILDED_BLACKSTONE.defaultBlockState(),
            Blocks.SEA_LANTERN.defaultBlockState());

    private static final List<Room> ROOMS = List.of(
            room("entry_vestibule", "Gate Descent", -10, 10, 14, -100, -90, 9, GATE, false, false),
            room("gate_hall", "Gate Descent", -16, 16, 14, -89, -78, 11, GATE, true, false),
            room("watcher_gallery", "Gate Descent", -22, 22, 14, -77, -62, 13, GATE, true, false),
            room("bridge_antechamber", "Gate Descent", -12, 12, 13, -61, -53, 10, GATE, false, false),
            room("broken_bridge", "Gate Descent", -32, 32, 12, -52, -39, 15, GATE, true, false),
            room("west_overlook", "Gate Descent", -45, -33, 12, -49, -40, 9, GATE, false, true),
            room("east_overlook", "Gate Descent", 33, 45, 12, -49, -40, 9, GATE, false, true),
            room("split_rotunda", "Gate Descent", -15, 15, 10, -38, -27, 12, GATE, true, false),

            room("west_descent", "Ossuary Ward", -34, -14, 9, -34, -26, 9, OSSUARY, false, false),
            room("ossuary_nave", "Ossuary Ward", -70, -31, 8, -25, 2, 13, OSSUARY, true, false),
            room("bone_chapel", "Ossuary Ward", -84, -71, 8, -21, -6, 11, OSSUARY, false, true),
            room("mortuary_store", "Ossuary Ward", -66, -48, 8, 3, 14, 9, OSSUARY, false, false),
            room("west_loop_gallery", "Ossuary Ward", -47, -29, 7, 5, 19, 9, OSSUARY, false, false),
            room("west_shortcut", "Ossuary Ward", -30, -18, 6, 12, 22, 8, OSSUARY, false, false),

            room("east_descent", "Abyssal Foundry", 14, 34, 9, -34, -26, 9, FOUNDRY, false, false),
            room("forge_approach", "Abyssal Foundry", 31, 48, 8, -25, -15, 9, FOUNDRY, false, false),
            room("abyssal_foundry", "Abyssal Foundry", 31, 78, 6, -14, 14, 15, FOUNDRY, true, false),
            room("slag_control", "Abyssal Foundry", 59, 76, 6, 13, 25, 10, FOUNDRY, false, false),
            room("furnace_secret", "Abyssal Foundry", 44, 58, 5, 14, 27, 10, FOUNDRY, false, true),
            room("east_loop_gallery", "Abyssal Foundry", 29, 47, 5, 13, 27, 9, FOUNDRY, false, false),
            room("east_shortcut", "Abyssal Foundry", 18, 30, 5, 17, 24, 8, FOUNDRY, false, false),

            room("central_spine", "Buried Kingdom", -7, 7, 7, -26, -7, 10, KINGDOM, false, false),
            room("central_plaza", "Buried Kingdom", -34, 34, 4, -6, 27, 16, KINGDOM, true, false),
            room("monument_ring", "Buried Kingdom", -13, 13, 5, 4, 19, 12, KINGDOM, false, false),
            room("ruined_market", "Buried Kingdom", -56, -31, 4, 3, 24, 10, KINGDOM, false, false),
            room("guard_barracks", "Buried Kingdom", 31, 56, 4, 3, 24, 10, KINGDOM, false, false),
            room("basilica_approach", "Buried Kingdom", -14, 14, 3, 26, 34, 11, KINGDOM, false, false),
            room("monarch_basilica", "Buried Kingdom", -25, 25, 2, 35, 57, 18, KINGDOM, true, false),
            room("sealed_reliquary", "Buried Kingdom", -40, -26, 2, 42, 55, 11, KINGDOM, false, true),
            room("broken_belfry", "Buried Kingdom", 26, 41, 6, 42, 56, 15, KINGDOM, false, true),

            room("prison_descent", "Lower Catacombs", -45, -24, 0, 51, 63, 10, CATACOMB, false, false),
            room("chained_prison", "Lower Catacombs", -75, -36, -4, 58, 84, 16, CATACOMB, true, false),
            room("execution_pit", "Lower Catacombs", -70, -49, -5, 85, 97, 12, CATACOMB, false, false),
            room("archive_descent", "Lower Catacombs", 24, 45, -1, 51, 63, 10, CATACOMB, false, false),
            room("drowned_archive", "Lower Catacombs", 36, 75, -6, 58, 86, 17, CATACOMB, true, false),
            room("forbidden_stacks", "Lower Catacombs", 56, 73, -6, 87, 99, 11, CATACOMB, false, true),
            room("sunken_court", "Lower Catacombs", -32, 32, -8, 66, 96, 22, CATACOMB, true, false),
            room("west_court_loop", "Lower Catacombs", -42, -29, -5, 76, 91, 10, CATACOMB, false, false),
            room("east_court_loop", "Lower Catacombs", 29, 42, -6, 76, 91, 10, CATACOMB, false, false),

            room("final_approach", "Monarch's Sepulcher", -12, 12, -9, 97, 105, 13, SEPULCHER, false, false),
            room("throne_passage", "Monarch's Sepulcher", -18, 18, -10, 106, 114, 15, SEPULCHER, false, false),
            room("final_arena", "Monarch's Sepulcher", -44, 44, -12, 115, 154, 32, SEPULCHER, true, false),
            room("reward_vault", "Monarch's Sepulcher", 42, 66, -10, 125, 146, 18, SEPULCHER, true, false)
    );

    private static final Map<String, Room> BY_ID = indexRooms();

    private static final List<Link> LINKS = List.of(
            link("entry_vestibule", "gate_hall", false, Corridor.GRAND),
            link("gate_hall", "watcher_gallery", false, Corridor.GRAND),
            link("watcher_gallery", "bridge_antechamber", false, Corridor.GRAND),
            link("bridge_antechamber", "broken_bridge", false, Corridor.GRAND),
            link("broken_bridge", "west_overlook", true, Corridor.RUINED),
            link("broken_bridge", "east_overlook", true, Corridor.RUINED),
            link("broken_bridge", "split_rotunda", false, Corridor.GRAND),
            link("split_rotunda", "west_descent", true, Corridor.RUINED),
            link("west_descent", "ossuary_nave", true, Corridor.RUINED),
            link("ossuary_nave", "bone_chapel", true, Corridor.SERVICE),
            link("ossuary_nave", "mortuary_store", false, Corridor.SERVICE),
            link("mortuary_store", "west_loop_gallery", true, Corridor.SERVICE),
            link("west_loop_gallery", "west_shortcut", true, Corridor.SERVICE),
            link("west_shortcut", "central_plaza", true, Corridor.GRAND),
            link("split_rotunda", "east_descent", true, Corridor.RUINED),
            link("east_descent", "forge_approach", true, Corridor.RUINED),
            link("forge_approach", "abyssal_foundry", false, Corridor.GRAND),
            link("abyssal_foundry", "slag_control", false, Corridor.SERVICE),
            link("abyssal_foundry", "furnace_secret", true, Corridor.SERVICE),
            link("slag_control", "east_loop_gallery", true, Corridor.SERVICE),
            link("furnace_secret", "east_loop_gallery", true, Corridor.SERVICE),
            link("east_loop_gallery", "east_shortcut", true, Corridor.SERVICE),
            link("east_shortcut", "central_plaza", true, Corridor.GRAND),
            link("split_rotunda", "central_spine", false, Corridor.GRAND),
            link("central_spine", "central_plaza", false, Corridor.GRAND),
            link("central_plaza", "monument_ring", false, Corridor.GRAND),
            link("central_plaza", "ruined_market", true, Corridor.RUINED),
            link("central_plaza", "guard_barracks", true, Corridor.RUINED),
            link("central_plaza", "basilica_approach", false, Corridor.GRAND),
            link("basilica_approach", "monarch_basilica", false, Corridor.GRAND),
            link("monarch_basilica", "sealed_reliquary", true, Corridor.SERVICE),
            link("monarch_basilica", "broken_belfry", true, Corridor.RUINED),
            link("monarch_basilica", "prison_descent", true, Corridor.RUINED),
            link("monarch_basilica", "archive_descent", true, Corridor.RUINED),
            link("prison_descent", "chained_prison", true, Corridor.RUINED),
            link("chained_prison", "execution_pit", false, Corridor.SERVICE),
            link("chained_prison", "west_court_loop", true, Corridor.SERVICE),
            link("execution_pit", "west_court_loop", true, Corridor.SERVICE),
            link("west_court_loop", "sunken_court", true, Corridor.RUINED),
            link("archive_descent", "drowned_archive", true, Corridor.RUINED),
            link("drowned_archive", "forbidden_stacks", false, Corridor.SERVICE),
            link("drowned_archive", "east_court_loop", true, Corridor.SERVICE),
            link("forbidden_stacks", "east_court_loop", true, Corridor.SERVICE),
            link("east_court_loop", "sunken_court", true, Corridor.RUINED),
            link("sunken_court", "final_approach", false, Corridor.GRAND),
            link("final_approach", "throne_passage", false, Corridor.GRAND),
            link("throne_passage", "final_arena", false, Corridor.GRAND),
            link("final_arena", "reward_vault", true, Corridor.GRAND)
    );

    private static final List<BlockPos> OBJECTIVE_CENTERS = List.of(
            pos(0, 13, -45),
            pos(-50, 9, -12),
            pos(0, 5, 11),
            pos(52, 7, 0),
            pos(0, 3, 48),
            pos(-55, -3, 70),
            pos(0, -7, 82),
            pos(0, -11, 134),
            pos(54, -9, 136)
    );

    private static final List<Door> CHECKPOINTS = List.of(
            Door.xPlane(-16, 10, 15, -34, -28),
            Door.xPlane(-25, 6, 11, 14, 20),
            Door.xPlane(24, 5, 10, 18, 24),
            Door.zPlane(25, 3, 10, -5, 5),
            Door.xPlane(-25, 0, 7, 53, 61),
            Door.xPlane(-31, -6, 1, 78, 88),
            Door.zPlane(97, -9, 0, -5, 5),
            Door.xPlane(41, -10, -3, 130, 140)
    );

    private static final Map<String, BlockPos> REGION_POINTS = Map.ofEntries(
            Map.entry("entrance", pos(0, 15, -95)),
            Map.entry("bridge", pos(0, 13, -45)),
            Map.entry("ossuary", pos(-50, 9, -12)),
            Map.entry("foundry", pos(52, 7, 0)),
            Map.entry("plaza", pos(0, 5, 11)),
            Map.entry("basilica", pos(0, 3, 48)),
            Map.entry("prison", pos(-55, -3, 70)),
            Map.entry("archive", pos(55, -5, 70)),
            Map.entry("court", pos(0, -7, 82)),
            Map.entry("arena", pos(0, -11, 134)),
            Map.entry("reward", pos(54, -9, 136))
    );

    public static BuildJob buildJob(BlockPos origin) {
        Editor editor = new Editor(origin);
        for (Room room : ROOMS) editor.room(room);
        for (Link link : LINKS) editor.link(link);
        decorate(editor);
        return new BuildJob("build", editor.operations);
    }

    public static BuildJob clearJob(BlockPos origin) {
        Editor editor = new Editor(origin);
        appendClearOperations(editor);
        return new BuildJob("clear", editor.operations);
    }

    public static BuildJob rebuildJob(BlockPos origin) {
        Editor editor = new Editor(origin);
        appendClearOperations(editor);
        for (Room room : ROOMS) editor.room(room);
        for (Link link : LINKS) editor.link(link);
        decorate(editor);
        return new BuildJob("rebuild", editor.operations);
    }

    public static BuildJob legacyMigrationJob(BlockPos oldOrigin, BlockPos newOrigin) {
        Editor editor = new Editor(oldOrigin);
        editor.fill(-48, 48, -2, 20, -48, 48, AIR);
        editor = new Editor(newOrigin, editor.operations);
        for (Room room : ROOMS) editor.room(room);
        for (Link link : LINKS) editor.link(link);
        decorate(editor);
        return new BuildJob("migration", editor.operations);
    }

    private static void appendClearOperations(Editor editor) {
        for (Room room : ROOMS) {
            editor.fill(room.minX - SHELL_THICKNESS, room.maxX + SHELL_THICKNESS,
                    room.floorY - SHELL_THICKNESS, room.ceilingY() + SHELL_THICKNESS,
                    room.minZ - SHELL_THICKNESS, room.maxZ + SHELL_THICKNESS, RESTORE);
        }
        for (Link link : LINKS) editor.clearLinkEnvelope(link);
    }

    public static AABB bounds(BlockPos origin) {
        return new AABB(origin.getX() + MIN_X, origin.getY() + MIN_Y, origin.getZ() + MIN_Z,
                origin.getX() + MAX_X + 1, origin.getY() + MAX_Y + 1, origin.getZ() + MAX_Z + 1);
    }

    public static BlockPos entry() { return pos(0, 15, -95); }
    public static BlockPos boss() { return pos(0, -11, 134); }
    public static BlockPos reward() { return pos(54, -9, 136); }
    public static List<BlockPos> objectiveCenters() { return OBJECTIVE_CENTERS; }
    public static int checkpointCount() { return CHECKPOINTS.size(); }

    public static void closeCheckpoints(ServerLevel level, BlockPos origin) {
        for (Door checkpoint : CHECKPOINTS) checkpoint.close(level, origin);
    }

    public static void openCheckpoint(ServerLevel level, BlockPos origin, int index) {
        if (index >= 0 && index < CHECKPOINTS.size()) CHECKPOINTS.get(index).open(level, origin);
    }

    public static BlockPos region(String raw) {
        if (raw == null) return null;
        return REGION_POINTS.get(raw.trim().toLowerCase(Locale.ROOT));
    }

    public static String info() {
        return DISPLAY_NAME + " | bounds 173x50x265 | 43 spaces | 13 major rooms | 6 regions | "
                + "20,774 walkable blocks | 848-block critical path | 6 loops | 3 shortcuts | 7 secrets";
    }

    public static List<String> validate(ServerLevel level, BlockPos origin) {
        List<String> errors = new ArrayList<>();
        for (BlockPos relative : OBJECTIVE_CENTERS) {
            BlockPos feet = origin.offset(relative);
            if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
                errors.add("liquid at marker " + relative.toShortString());
            }
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                errors.add("blocked marker " + relative.toShortString());
            }
            BlockPos floor = feet.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                errors.add("unsupported marker " + relative.toShortString());
            }
        }
        return errors;
    }

    public static void debugBounds(ServerLevel level, BlockPos origin) {
        int[] xs = {MIN_X, MAX_X};
        int[] ys = {MIN_Y, MAX_Y};
        int[] zs = {MIN_Z, MAX_Z};
        for (int x : xs) for (int y : ys) for (int z : zs) {
            BlockPos p = origin.offset(x, y, z);
            level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                    8, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    private static void decorate(Editor e) {
        decorateEntry(e);
        decorateBridge(e);
        decorateOssuary(e);
        decorateFoundry(e);
        decorateKingdom(e);
        decorateCatacombs(e);
        decorateFinale(e);
    }

    private static void decorateEntry(Editor e) {
        e.ribbedHall(-14, 14, 15, -88, -79, 4, GATE.trim);
        for (int x : new int[] {-12, 12}) {
            e.pillar(x, -83, 15, 23, Blocks.POLISHED_BASALT.defaultBlockState());
            e.set(x, 24, -83, GATE.light);
        }
        e.fill(-7, 7, 15, 15, -76, -64, Blocks.SMOOTH_STONE.defaultBlockState());
        for (int z = -75; z <= -64; z += 4) {
            e.pillar(-18, z, 15, 24, GATE.trim);
            e.pillar(18, z, 15, 24, GATE.trim);
            e.set(-18, 25, z, GATE.light);
            e.set(18, 25, z, GATE.light);
        }
    }

    private static void decorateBridge(Editor e) {
        e.fill(-28, 28, 12, 12, -48, -43, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        e.fill(-7, 7, 11, 11, -47, -44, Blocks.SCULK.defaultBlockState());
        for (int x = -26; x <= 26; x += 8) {
            e.pillar(x, -50, 13, 23, GATE.trim);
            e.pillar(x, -41, 13, 23, GATE.trim);
        }
        e.fill(-4, 4, 13, 17, -39, -39, GATE.accent);
        e.rubble(-26, 13, -45, GATE.wall);
        e.rubble(23, 13, -47, GATE.wall);
        e.statue(-10, -31, 11, GATE);
        e.statue(10, -31, 11, GATE);
    }

    private static void decorateOssuary(Editor e) {
        for (int x = -66; x <= -34; x += 8) {
            e.pillar(x, -22, 9, 18, OSSUARY.trim);
            e.pillar(x, -1, 9, 18, OSSUARY.trim);
            e.set(x, 19, -22, OSSUARY.light);
            e.set(x, 19, -1, OSSUARY.light);
        }
        for (int z = -18; z <= -4; z += 5) {
            e.fill(-68, -65, 9, 11, z, z + 2, Blocks.BONE_BLOCK.defaultBlockState());
            e.fill(-36, -33, 9, 11, z, z + 2, Blocks.BONE_BLOCK.defaultBlockState());
        }
        e.fill(-57, -44, 9, 9, -15, -8, Blocks.SOUL_SAND.defaultBlockState());
        e.fill(-53, -48, 10, 11, -12, -10, Blocks.GILDED_BLACKSTONE.defaultBlockState());
        e.fill(-82, -73, 9, 9, -18, -9, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        e.fill(-80, -75, 10, 12, -15, -12, Blocks.GOLD_BLOCK.defaultBlockState());
        e.rubble(-62, 9, 8, OSSUARY.wall);
    }

    private static void decorateFoundry(Editor e) {
        e.fill(35, 74, 6, 6, -10, -6, Blocks.MAGMA_BLOCK.defaultBlockState());
        e.fill(35, 74, 7, 7, -10, -6, Blocks.ORANGE_STAINED_GLASS.defaultBlockState());
        for (int x = 36; x <= 72; x += 9) {
            e.pillar(x, 10, 7, 18, Blocks.POLISHED_BASALT.defaultBlockState());
            e.set(x, 19, 10, FOUNDRY.light);
        }
        for (int x : new int[] {38, 48, 58, 68}) {
            e.fill(x, x + 3, 7, 11, -1, 3, Blocks.COPPER_BLOCK.defaultBlockState());
            e.fill(x - 1, x + 4, 12, 12, -2, 4, Blocks.CUT_COPPER.defaultBlockState());
        }
        for (int x : new int[] {37, 45, 63, 71}) e.set(x, 7, 8, Blocks.ANVIL.defaultBlockState());
        e.fill(62, 73, 7, 7, 16, 22, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        e.fill(64, 71, 8, 10, 18, 20, Blocks.RED_NETHER_BRICKS.defaultBlockState());
        e.rubble(34, 7, 9, FOUNDRY.wall);
        e.rubble(72, 7, -12, FOUNDRY.wall);
    }

    private static void decorateKingdom(Editor e) {
        for (int x = -28; x <= 28; x += 14) {
            e.pillar(x, -3, 5, 17, KINGDOM.trim);
            e.pillar(x, 24, 5, 17, KINGDOM.trim);
            e.set(x, 18, -3, KINGDOM.light);
            e.set(x, 18, 24, KINGDOM.light);
        }
        for (int x = -10; x <= 10; x++) {
            for (int z = 7; z <= 17; z++) {
                if (Math.abs(x) + Math.abs(z - 12) <= 11) e.set(x, 4, z, KINGDOM.accent);
            }
        }
        e.statue(0, 12, 5, KINGDOM);
        e.fill(-53, -34, 5, 5, 7, 10, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        e.fill(34, 53, 5, 5, 17, 20, Blocks.POLISHED_ANDESITE.defaultBlockState());
        for (int x = -20; x <= 20; x += 10) {
            e.pillar(x, 38, 3, 17, KINGDOM.trim);
            e.pillar(x, 54, 3, 17, KINGDOM.trim);
            e.set(x, 18, 38, KINGDOM.light);
            e.set(x, 18, 54, KINGDOM.light);
        }
        e.fill(-12, 12, 3, 4, 52, 56, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        e.fill(-7, 7, 5, 6, 55, 56, Blocks.GILDED_BLACKSTONE.defaultBlockState());
    }

    private static void decorateCatacombs(Editor e) {
        for (int z = 62; z <= 80; z += 6) {
            e.fill(-72, -69, -3, 6, z, z + 3, Blocks.IRON_BARS.defaultBlockState());
            e.fill(-43, -40, -3, 6, z, z + 3, Blocks.IRON_BARS.defaultBlockState());
        }
        e.fill(-63, -45, -5, -5, 88, 94, Blocks.SOUL_SOIL.defaultBlockState());
        e.fill(-59, -49, -4, -3, 90, 92, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int x = 40; x <= 70; x += 6) {
            e.fill(x, x + 2, -5, 5, 62, 64, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            e.fill(x, x + 2, -5, 5, 80, 82, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        e.fill(58, 71, -5, -5, 90, 96, Blocks.SCULK.defaultBlockState());
        for (int x = -28; x <= 28; x += 14) {
            e.pillar(x, 70, -7, 9, CATACOMB.trim);
            e.pillar(x, 92, -7, 9, CATACOMB.trim);
            e.set(x, 10, 70, CATACOMB.light);
            e.set(x, 10, 92, CATACOMB.light);
        }
        e.fill(-10, 10, -8, -8, 76, 88, Blocks.SCULK.defaultBlockState());
        e.fill(-6, 6, -7, -6, 80, 84, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
    }

    private static void decorateFinale(Editor e) {
        e.ribbedHall(-16, 16, -9, 98, 104, 4, SEPULCHER.trim);
        e.ribbedHall(-16, 16, -10, 107, 113, 4, SEPULCHER.trim);
        for (int x = -38; x <= 38; x += 12) {
            e.pillar(x, 119, -11, 15, SEPULCHER.trim);
            e.pillar(x, 150, -11, 15, SEPULCHER.trim);
            e.set(x, 16, 119, SEPULCHER.light);
            e.set(x, 16, 150, SEPULCHER.light);
        }
        for (int z = 123; z <= 147; z += 8) {
            e.pillar(-40, z, -11, 18, Blocks.OBSIDIAN.defaultBlockState());
            e.pillar(40, z, -11, 18, Blocks.OBSIDIAN.defaultBlockState());
        }
        for (int x = -13; x <= 13; x++) {
            for (int z = 126; z <= 144; z++) {
                if (Math.abs(x) + Math.abs(z - 135) <= 17) e.set(x, -12, z, SEPULCHER.accent);
            }
        }
        e.fill(-12, 12, -11, -10, 146, 151, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        e.fill(-7, 7, -9, -7, 149, 152, Blocks.GOLD_BLOCK.defaultBlockState());
        e.statue(0, 150, -6, SEPULCHER);
        e.fill(44, 64, -10, -10, 128, 143, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        e.fill(45, 45, -9, 4, 129, 142, Blocks.GOLD_BLOCK.defaultBlockState());
        e.fill(63, 63, -9, 4, 129, 142, Blocks.GOLD_BLOCK.defaultBlockState());
        e.fill(50, 58, -9, -8, 134, 138, Blocks.GOLD_BLOCK.defaultBlockState());
    }

    private static Map<String, Room> indexRooms() {
        Map<String, Room> result = new LinkedHashMap<>();
        for (Room room : ROOMS) result.put(room.id, room);
        return Map.copyOf(result);
    }

    private static Room room(String id, String region, int minX, int maxX, int floorY, int minZ, int maxZ,
                             int height, Theme theme, boolean major, boolean secret) {
        return new Room(id, region, minX, maxX, floorY, minZ, maxZ, height, theme, major, secret);
    }

    private static Link link(String from, String to, boolean xFirst, Corridor corridor) {
        return new Link(from, to, xFirst, corridor);
    }

    private static BlockPos pos(int x, int y, int z) { return new BlockPos(x, y, z); }

    public record TickReport(int visitedBlocks, int changedBlocks, boolean complete) {}

    public record JobReport(String mode, long plannedVisits, long visitedBlocks, long changedBlocks,
                            int elapsedTicks, int maxVisitedInTick, int maxChangedInTick) {
        public double progress() {
            return plannedVisits <= 0L ? 1.0D : Math.min(1.0D, visitedBlocks / (double)plannedVisits);
        }
    }

    public static final class BuildJob {
        private final String mode;
        private final List<BoxOperation> operations;
        private final long plannedVisits;
        private int operationIndex;
        private long visited;
        private long changed;
        private int elapsedTicks;
        private int maxVisitedInTick;
        private int maxChangedInTick;

        private BuildJob(String mode, List<BoxOperation> operations) {
            this.mode = mode;
            this.operations = List.copyOf(operations);
            this.plannedVisits = this.operations.stream().mapToLong(BoxOperation::volume).sum();
        }

        public TickReport tick(ServerLevel level) {
            if (complete()) return new TickReport(0, 0, true);
            int visitedThisTick = 0;
            int changedThisTick = 0;
            while (operationIndex < operations.size()
                    && visitedThisTick < MAX_BLOCK_VISITS_PER_TICK
                    && changedThisTick < MAX_BLOCK_CHANGES_PER_TICK) {
                BoxOperation operation = operations.get(operationIndex);
                OperationStep step = operation.apply(level,
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

        public boolean complete() { return operationIndex >= operations.size(); }
        public String mode() { return mode; }
        public long plannedVisits() { return plannedVisits; }
        public long visitedBlocks() { return visited; }
        public long changedBlocks() { return changed; }
        public int elapsedTicks() { return elapsedTicks; }
        public double progress() { return plannedVisits <= 0L ? 1.0D : Math.min(1.0D, visited / (double)plannedVisits); }
        public JobReport report() {
            return new JobReport(mode, plannedVisits, visited, changed, elapsedTicks,
                    maxVisitedInTick, maxChangedInTick);
        }
    }

    private record OperationStep(int visitedBlocks, int changedBlocks, boolean complete) {}

    private static final class BoxOperation {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final BlockState state;
        private int x;
        private int y;
        private int z;
        private boolean complete;

        private BoxOperation(BlockPos origin, int minX, int maxX, int minY, int maxY,
                             int minZ, int maxZ, BlockState state) {
            this.minX = origin.getX() + Math.min(minX, maxX);
            this.maxX = origin.getX() + Math.max(minX, maxX);
            this.minY = origin.getY() + Math.min(minY, maxY);
            this.maxY = origin.getY() + Math.max(minY, maxY);
            this.minZ = origin.getZ() + Math.min(minZ, maxZ);
            this.maxZ = origin.getZ() + Math.max(minZ, maxZ);
            this.state = state;
            this.x = this.minX;
            this.y = this.minY;
            this.z = this.minZ;
        }

        private long volume() {
            return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        private OperationStep apply(ServerLevel level, int visitBudget, int changeBudget) {
            if (complete || visitBudget <= 0 || changeBudget <= 0) {
                return new OperationStep(0, 0, complete);
            }
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
            if (x < maxX) {
                x++;
            } else if (z < maxZ) {
                x = minX;
                z++;
            } else if (y < maxY) {
                x = minX;
                z = minZ;
                y++;
            } else {
                complete = true;
            }
        }
    }

    private enum Corridor { GRAND, RUINED, SERVICE }

    private record Theme(BlockState shell, BlockState wall, BlockState floor, BlockState ceiling,
                         BlockState trim, BlockState accent, BlockState light) {}

    private record Room(String id, String region, int minX, int maxX, int floorY, int minZ, int maxZ,
                        int height, Theme theme, boolean major, boolean secret) {
        private int ceilingY() { return floorY + height; }
        private int centerX() { return (minX + maxX) / 2; }
        private int centerZ() { return (minZ + maxZ) / 2; }
        private boolean contains(int x, int z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }
    }

    private record Link(String from, String to, boolean xFirst, Corridor corridor) {}

    private record Door(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        private static Door xPlane(int x, int minY, int maxY, int minZ, int maxZ) {
            return new Door(x, x, minY, maxY, minZ, maxZ);
        }
        private static Door zPlane(int z, int minY, int maxY, int minX, int maxX) {
            return new Door(minX, maxX, minY, maxY, z, z);
        }
        private void close(ServerLevel level, BlockPos origin) {
            setBox(level, origin, minX, maxX, minY, maxY, minZ, maxZ, Blocks.IRON_BARS.defaultBlockState());
        }
        private void open(ServerLevel level, BlockPos origin) {
            setBox(level, origin, minX, maxX, minY, maxY, minZ, maxZ, AIR);
        }
    }

    private static final class Editor {
        private final BlockPos origin;
        private final List<BoxOperation> operations;

        private Editor(BlockPos origin) {
            this(origin, new ArrayList<>());
        }

        private Editor(BlockPos origin, List<BoxOperation> operations) {
            this.origin = origin.immutable();
            this.operations = operations;
        }

        private void room(Room room) {
            int s = SHELL_THICKNESS;
            fill(room.minX - s, room.maxX + s, room.floorY - s, room.floorY - 1,
                    room.minZ - s, room.maxZ + s, room.theme.shell);
            fill(room.minX - s, room.maxX + s, room.ceilingY() + 1, room.ceilingY() + s,
                    room.minZ - s, room.maxZ + s, room.theme.shell);
            fill(room.minX - s, room.minX - 1, room.floorY, room.ceilingY(),
                    room.minZ - s, room.maxZ + s, room.theme.shell);
            fill(room.maxX + 1, room.maxX + s, room.floorY, room.ceilingY(),
                    room.minZ - s, room.maxZ + s, room.theme.shell);
            fill(room.minX, room.maxX, room.floorY, room.ceilingY(),
                    room.minZ - s, room.minZ - 1, room.theme.shell);
            fill(room.minX, room.maxX, room.floorY, room.ceilingY(),
                    room.maxZ + 1, room.maxZ + s, room.theme.shell);

            fill(room.minX, room.maxX, room.floorY, room.floorY, room.minZ, room.maxZ, room.theme.floor);
            fill(room.minX, room.maxX, room.ceilingY(), room.ceilingY(), room.minZ, room.maxZ, room.theme.ceiling);
            fill(room.minX, room.minX, room.floorY + 1, room.ceilingY() - 1, room.minZ, room.maxZ, room.theme.wall);
            fill(room.maxX, room.maxX, room.floorY + 1, room.ceilingY() - 1, room.minZ, room.maxZ, room.theme.wall);
            fill(room.minX, room.maxX, room.floorY + 1, room.ceilingY() - 1, room.minZ, room.minZ, room.theme.wall);
            fill(room.minX, room.maxX, room.floorY + 1, room.ceilingY() - 1, room.maxZ, room.maxZ, room.theme.wall);
            fill(room.minX + 1, room.maxX - 1, room.floorY + 1, room.ceilingY() - 1,
                    room.minZ + 1, room.maxZ - 1, AIR);

            for (int x = room.minX + 2; x <= room.maxX - 2; x += 7) {
                set(x, room.floorY + 1, room.minZ, room.theme.trim);
                set(x, room.floorY + 1, room.maxZ, room.theme.trim);
            }
            for (int z = room.minZ + 2; z <= room.maxZ - 2; z += 7) {
                set(room.minX, room.floorY + 1, z, room.theme.trim);
                set(room.maxX, room.floorY + 1, z, room.theme.trim);
            }
        }

        private void link(Link link) {
            Room from = BY_ID.get(link.from);
            Room to = BY_ID.get(link.to);
            if (from == null || to == null) return;
            List<int[]> path = path(from, to, link.xFirst);
            int total = Math.max(1, path.size() - 1);
            for (int i = 0; i < path.size(); i++) {
                int[] point = path.get(i);
                int floorY = (int)Math.round(from.floorY + (to.floorY - from.floorY) * (i / (double)total));
                boolean insideEndpoint = from.contains(point[0], point[1]) || to.contains(point[0], point[1]);
                if (insideEndpoint) aisle(point[0], floorY, point[1], link.corridor);
                else tunnelCell(point[0], floorY, point[1], link.corridor, i);
            }
        }

        private List<int[]> path(Room from, Room to, boolean xFirst) {
            List<int[]> points = new ArrayList<>();
            int x = from.centerX();
            int z = from.centerZ();
            points.add(new int[] {x, z});
            if (xFirst) {
                while (x != to.centerX()) { x += Integer.signum(to.centerX() - x); points.add(new int[] {x, z}); }
                while (z != to.centerZ()) { z += Integer.signum(to.centerZ() - z); points.add(new int[] {x, z}); }
            } else {
                while (z != to.centerZ()) { z += Integer.signum(to.centerZ() - z); points.add(new int[] {x, z}); }
                while (x != to.centerX()) { x += Integer.signum(to.centerX() - x); points.add(new int[] {x, z}); }
            }
            return points;
        }

        private void aisle(int x, int floorY, int z, Corridor corridor) {
            int radius = corridor == Corridor.GRAND ? 3 : 2;
            BlockState floor = corridor == Corridor.SERVICE ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            fill(x - radius, x + radius, floorY, floorY, z - radius, z + radius, floor);
            fill(x - radius, x + radius, floorY + 1, floorY + 6, z - radius, z + radius, AIR);
        }

        private void tunnelCell(int x, int floorY, int z, Corridor corridor, int index) {
            int radius = corridor == Corridor.GRAND ? 3 : 2;
            BlockState wall = corridor == Corridor.SERVICE ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                    : corridor == Corridor.RUINED ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            BlockState floor = corridor == Corridor.SERVICE ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            for (int dy = -SHELL_THICKNESS; dy <= 0; dy++) {
                fill(x - radius - SHELL_THICKNESS, x + radius + SHELL_THICKNESS, floorY + dy, floorY + dy,
                        z - radius - SHELL_THICKNESS, z + radius + SHELL_THICKNESS, RESTORE);
            }
            fill(x - radius, x + radius, floorY, floorY, z - radius, z + radius, floor);
            fill(x - radius, x + radius, floorY + 1, floorY + 6, z - radius, z + radius, AIR);
            fill(x - radius - SHELL_THICKNESS, x - radius - 1, floorY + 1, floorY + 10,
                    z - radius - SHELL_THICKNESS, z + radius + SHELL_THICKNESS, RESTORE);
            fill(x + radius + 1, x + radius + SHELL_THICKNESS, floorY + 1, floorY + 10,
                    z - radius - SHELL_THICKNESS, z + radius + SHELL_THICKNESS, RESTORE);
            fill(x - radius, x + radius, floorY + 7, floorY + 10,
                    z - radius - SHELL_THICKNESS, z + radius + SHELL_THICKNESS, RESTORE);
            if (index % 9 == 0) {
                set(x - radius, floorY + 3, z, wall);
                set(x + radius, floorY + 3, z, wall);
                set(x, floorY + 6, z, corridor == Corridor.GRAND ? Blocks.SOUL_LANTERN.defaultBlockState() : wall);
            }
        }

        private void clearLinkEnvelope(Link link) {
            Room from = BY_ID.get(link.from);
            Room to = BY_ID.get(link.to);
            if (from == null || to == null) return;
            List<int[]> path = path(from, to, link.xFirst);
            int total = Math.max(1, path.size() - 1);
            int radius = link.corridor == Corridor.GRAND ? 8 : 7;
            for (int i = 0; i < path.size(); i++) {
                int[] point = path.get(i);
                int floorY = (int)Math.round(from.floorY + (to.floorY - from.floorY) * (i / (double)total));
                fill(point[0] - radius, point[0] + radius, floorY - 5, floorY + 10,
                        point[1] - radius, point[1] + radius, RESTORE);
            }
        }

        private void ribbedHall(int minX, int maxX, int floorY, int minZ, int maxZ, int spacing, BlockState state) {
            for (int z = minZ; z <= maxZ; z += spacing) {
                pillar(minX, z, floorY + 1, floorY + 7, state);
                pillar(maxX, z, floorY + 1, floorY + 7, state);
                fill(minX, maxX, floorY + 8, floorY + 8, z, z, state);
            }
        }

        private void statue(int x, int z, int baseY, Theme theme) {
            fill(x - 2, x + 2, baseY, baseY, z - 2, z + 2, theme.trim);
            fill(x - 1, x + 1, baseY + 1, baseY + 5, z - 1, z + 1, theme.wall);
            fill(x - 3, x + 3, baseY + 4, baseY + 5, z, z, theme.wall);
            fill(x - 1, x + 1, baseY + 6, baseY + 8, z - 1, z + 1, theme.trim);
            set(x, baseY + 7, z - 2, theme.light);
        }

        private void rubble(int x, int y, int z, BlockState state) {
            set(x, y, z, state);
            set(x + 1, y, z, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
            set(x, y, z + 1, Blocks.GRAVEL.defaultBlockState());
            set(x + 1, y + 1, z + 1, Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState());
        }

        private void pillar(int x, int z, int minY, int maxY, BlockState state) {
            fill(x, x, minY, maxY, z, z, state);
        }

        private void fill(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, BlockState state) {
            operations.add(new BoxOperation(origin, minX, maxX, minY, maxY, minZ, maxZ, state));
        }

        private void set(int x, int y, int z, BlockState state) {
            fill(x, x, y, y, z, z, state);
        }
    }

    private static void setBox(ServerLevel level, BlockPos origin, int minX, int maxX, int minY, int maxY,
                               int minZ, int maxZ, BlockState state) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos position = origin.offset(x, y, z);
                    if (!level.getBlockState(position).equals(state)) level.setBlock(position, state, UPDATE_FLAGS);
                }
            }
        }
    }

    private MasterDungeonBuilder() {}
}
