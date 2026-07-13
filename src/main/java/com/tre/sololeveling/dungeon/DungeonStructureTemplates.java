package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Loads vanilla structure-template NBT (or its checked-in SNBT text form) for staged placement. */
public final class DungeonStructureTemplates {
    public static final String ROOT = "dungeons/master/";
    private static final Set<Block> FORBIDDEN = Set.of(Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK, Blocks.JIGSAW, Blocks.BARRIER, Blocks.BEDROCK);
    private static Object cachedResourceManager;
    private static LoadedPack cachedPack;

    public record ModuleDefinition(String id, BlockPos offset, BlockPos size) {
        public BlockPos maximum() { return offset.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1); }
    }

    public record TemplateBlock(BlockPos relativePosition, BlockState state, CompoundTag blockEntityData) {}

    public record LoadedPack(List<TemplateBlock> blocks, Map<String, BlockPos> markers,
                             int templateBlockCount, int lightCount, Map<String, Integer> moduleLightCounts) {}

    public static final List<ModuleDefinition> MODULES = List.of(
            module("00_entry", -15, 0, -112, 31, 18, 32),
            module("01_descent", -15, 0, -80, 31, 18, 27),
            module("02_outer_necropolis", -48, 0, -53, 48, 20, 40),
            module("03_guardian_hall", 0, 0, -53, 48, 20, 40),
            module("04_catacombs", -23, 0, -13, 47, 22, 44),
            module("05_collapsed_bridge", -23, 0, 31, 47, 20, 34),
            module("06_prison", -47, 0, 65, 47, 22, 41),
            module("07_elite_chamber", 0, 0, 65, 47, 22, 41),
            module("08_ritual_depths", -23, 0, 106, 47, 24, 44),
            module("09_boss_approach", -15, 0, 150, 31, 20, 27),
            module("10_boss_arena", -23, 0, 177, 47, 28, 47),
            module("11_reward_vault", 24, 0, 193, 31, 20, 31)
    );

    private static final Map<String, BlockPos> EXPECTED_MARKERS = Map.ofEntries(
            marker("entry", 0, 1, -106),
            marker("objective:0", -24, 1, -33),
            marker("objective:1", 24, 3, -33),
            marker("objective:2", 0, 1, 3),
            marker("objective:3", 0, 1, 48),
            marker("objective:4", -23, 1, 85),
            marker("objective:5", 23, 1, 85),
            marker("objective:6", 0, 3, 128),
            marker("boss", 0, 1, 200),
            marker("reward", 39, 4, 207),
            marker("door:0", 8, 1, -16),
            marker("door:1", 0, 1, 28),
            marker("door:2", -8, 1, 62),
            marker("door:3", -1, 1, 85),
            marker("door:4", 8, 1, 103),
            marker("door:5", 0, 1, 147),
            marker("door:6", 0, 1, 174),
            marker("door:7", 21, 1, 207),
            marker("secret:west_crypt", -42, 1, -22),
            marker("secret:bone_chapel", 15, 1, 18),
            marker("secret:warden_cache", 38, 1, 96),
            marker("shortcut:catacomb_loop", -17, 1, 9)
    );

    public static synchronized LoadedPack load(ServerLevel level) {
        Object resourceManager = level.getServer().getResourceManager();
        if (cachedPack != null && cachedResourceManager == resourceManager) return cachedPack;
        List<TemplateBlock> blocks = new ArrayList<>();
        Map<String, BlockPos> markers = new LinkedHashMap<>();
        Map<String, Integer> moduleLightCounts = new LinkedHashMap<>();
        int lightCount = 0;
        for (ModuleDefinition module : MODULES) {
            int moduleLightCount = 0;
            CompoundTag root = readTemplate(level, module.id());
            if (!root.getList("entities", Tag.TAG_COMPOUND).isEmpty()) {
                throw new IllegalStateException("Module " + module.id() + " must be exported without entities");
            }
            BlockPos size = readPos(root.getList("size", Tag.TAG_INT));
            if (!size.equals(module.size())) {
                throw new IllegalStateException("Module " + module.id() + " size " + size.toShortString()
                        + " does not match catalog " + module.size().toShortString());
            }
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            if (palette.isEmpty() || palette.size() > 256) {
                throw new IllegalStateException("Module " + module.id() + " has an invalid palette");
            }
            List<BlockState> states = new ArrayList<>(palette.size());
            for (int index = 0; index < palette.size(); index++) {
                BlockState state = readState(palette.getCompound(index));
                if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) {
                    states.add(state);
                    continue;
                }
                if (FORBIDDEN.contains(state.getBlock())) {
                    throw new IllegalStateException("Forbidden block in " + module.id() + ": " + state.getBlock());
                }
                states.add(state);
            }
            Set<BlockPos> occupied = new LinkedHashSet<>();
            ListTag persistedBlocks = root.getList("blocks", Tag.TAG_COMPOUND);
            for (int index = 0; index < persistedBlocks.size(); index++) {
                CompoundTag persisted = persistedBlocks.getCompound(index);
                BlockPos local = readPos(persisted.getList("pos", Tag.TAG_INT));
                if (!inside(local, size) || !occupied.add(local)) {
                    throw new IllegalStateException("Invalid or duplicate block position in " + module.id());
                }
                int stateIndex = persisted.getInt("state");
                if (stateIndex < 0 || stateIndex >= states.size()) {
                    throw new IllegalStateException("Invalid palette index in " + module.id());
                }
                BlockState state = states.get(stateIndex);
                CompoundTag nbt = persisted.contains("nbt", Tag.TAG_COMPOUND)
                        ? persisted.getCompound("nbt").copy() : null;
                BlockPos relative = module.offset().offset(local);
                if (state.is(Blocks.STRUCTURE_BLOCK)) {
                    if (nbt == null || nbt.getString("metadata").isBlank()) {
                        throw new IllegalStateException("Unlabelled structure marker in " + module.id());
                    }
                    String marker = nbt.getString("metadata").trim().toLowerCase(java.util.Locale.ROOT);
                    if (markers.putIfAbsent(marker, relative) != null) {
                        throw new IllegalStateException("Duplicate dungeon marker: " + marker);
                    }
                    continue;
                }
                if (state.is(Blocks.STRUCTURE_VOID)) continue;
                if (state.getLightEmission() > 0) {
                    lightCount++;
                    moduleLightCount++;
                }
                blocks.add(new TemplateBlock(relative, state, nbt));
            }
            moduleLightCounts.put(module.id(), moduleLightCount);
        }
        validateMarkers(markers);
        cachedResourceManager = resourceManager;
        cachedPack = new LoadedPack(List.copyOf(blocks), Map.copyOf(markers), blocks.size(), lightCount,
                Map.copyOf(moduleLightCounts));
        return cachedPack;
    }

    private static CompoundTag readTemplate(ServerLevel level, String id) {
        ResourceLocation binary = new ResourceLocation("sololeveling", "structures/" + ROOT + id + ".nbt");
        Optional<Resource> binaryResource = level.getServer().getResourceManager().getResource(binary);
        if (binaryResource.isPresent()) {
            try (InputStream stream = binaryResource.get().open()) {
                return NbtIo.readCompressed(stream);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read exported dungeon module " + id, exception);
            }
        }
        ResourceLocation text = new ResourceLocation("sololeveling", "dungeon_modules/master/" + id + ".snbt");
        Resource resource = level.getServer().getResourceManager().getResource(text)
                .orElseThrow(() -> new IllegalStateException("Missing dungeon module " + id));
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder source = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) source.append(line).append('\n');
            return TagParser.parseTag(source.toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse dungeon module " + id, exception);
        }
    }

    private static BlockState readState(CompoundTag paletteEntry) {
        ResourceLocation id = ResourceLocation.tryParse(paletteEntry.getString("Name"));
        Block block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        if (block == null) throw new IllegalStateException("Unknown block in dungeon template: " + id);
        BlockState state = block.defaultBlockState();
        if (!paletteEntry.contains("Properties", Tag.TAG_COMPOUND)) return state;
        CompoundTag values = paletteEntry.getCompound("Properties");
        for (String name : values.getAllKeys()) {
            Property<?> property = block.getStateDefinition().getProperty(name);
            if (property == null || !values.contains(name, Tag.TAG_STRING)) {
                throw new IllegalStateException("Invalid block-state property " + name + " on " + block);
            }
            state = applyProperty(state, property, values.getString(name));
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        T parsed = property.getValue(value).orElseThrow(() ->
                new IllegalStateException("Invalid value " + value + " for property " + property.getName()));
        return state.setValue(property, parsed);
    }

    private static void validateMarkers(Map<String, BlockPos> markers) {
        for (Map.Entry<String, BlockPos> expected : EXPECTED_MARKERS.entrySet()) {
            BlockPos actual = markers.get(expected.getKey());
            if (!expected.getValue().equals(actual)) {
                throw new IllegalStateException("Missing or moved dungeon marker " + expected.getKey());
            }
        }
        long secrets = markers.keySet().stream().filter(marker -> marker.startsWith("secret:")).count();
        long shortcuts = markers.keySet().stream().filter(marker -> marker.startsWith("shortcut:")).count();
        if (secrets < 2L || shortcuts < 1L) {
            throw new IllegalStateException("Dungeon requires at least two secrets and one shortcut");
        }
    }

    public static BlockPos markerPosition(String id) { return EXPECTED_MARKERS.get(id); }
    public static Set<String> expectedMarkerIds() { return EXPECTED_MARKERS.keySet(); }
    public static synchronized void clearCache() { cachedResourceManager = null; cachedPack = null; }

    private static boolean inside(BlockPos pos, BlockPos size) {
        return pos.getX() >= 0 && pos.getY() >= 0 && pos.getZ() >= 0
                && pos.getX() < size.getX() && pos.getY() < size.getY() && pos.getZ() < size.getZ();
    }

    private static BlockPos readPos(ListTag list) {
        if (list.size() != 3) throw new IllegalStateException("Expected a three-coordinate list");
        return new BlockPos(list.getInt(0), list.getInt(1), list.getInt(2));
    }

    private static ModuleDefinition module(String id, int x, int y, int z, int sx, int sy, int sz) {
        return new ModuleDefinition(id, new BlockPos(x, y, z), new BlockPos(sx, sy, sz));
    }

    private static Map.Entry<String, BlockPos> marker(String id, int x, int y, int z) {
        return Map.entry(id, new BlockPos(x, y, z));
    }

    private DungeonStructureTemplates() {}
}
