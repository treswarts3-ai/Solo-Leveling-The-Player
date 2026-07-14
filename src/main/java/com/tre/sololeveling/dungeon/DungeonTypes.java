package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DungeonTypes {
    public static final String DATA_NAME = "sololeveling_dungeons";
    public static final String TAG_SESSION = "sl_dungeon_session";
    public static final String TAG_ENEMY_ID = "sl_enemy_id";
    public static final String TAG_DUNGEON_ENEMY = "sl_dungeon_enemy";
    public static final String TAG_SHADOW_EXTRACTABLE = "sl_shadow_extractable";
    public static final String TAG_COLLECTION_ITEM = "sl_dungeon_collection";
    public static final String TAG_BOSS = "sl_dungeon_boss";
    public static final int MAX_LIVE_ENEMIES = 64;
    public static final int MAX_WAVE_SPAWNS = 160;

    public enum GateRank implements StringRepresentable {
        E(1, 1.0D), D(10, 1.2D), C(20, 1.5D), B(30, 2.0D), A(40, 2.8D), S(60, 4.0D);
        private final int minimumLevel;
        private final double rewardMultiplier;
        GateRank(int minimumLevel, double rewardMultiplier) { this.minimumLevel = minimumLevel; this.rewardMultiplier = rewardMultiplier; }
        public int minimumLevel() { return minimumLevel; }
        public double rewardMultiplier() { return rewardMultiplier; }
        @Override
        public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        public static GateRank parse(String value) {
            String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace("-RANK", "").replace("_RANK", "");
            try { return GateRank.valueOf(normalized); } catch (IllegalArgumentException ignored) { return E; }
        }
    }

    /** Persisted lifecycle states. Keep names stable; old CLEANUP records migrate to CLEANING. */
    public enum SessionState { WAITING, BUILDING, READY, ACTIVE, BOSS, REWARD, COMPLETED, FAILED, CLEANING, CLOSED }
    public enum ObjectiveType { WAVE, COLLECTION, ELITE, BOSS, REWARD }
    public enum EnemyKind { MELEE, ASSASSIN, TANK, RANGED, SUMMONER, ELITE }

    public record SpawnEntry(String enemyId, int count) {
        public SpawnEntry { enemyId = id(enemyId); count = Math.max(1, Math.min(32, count)); }
    }

    public record WaveDefinition(String id, List<SpawnEntry> entries, boolean collectionDrops) {
        public WaveDefinition { id = DungeonTypes.id(id); entries = List.copyOf(entries); }
        public int totalCount() { return entries.stream().mapToInt(SpawnEntry::count).sum(); }
    }

    public record ObjectiveDefinition(ObjectiveType type, String id, String encounterId, int target, int timeLimitTicks, String displayName) {
        public ObjectiveDefinition {
            id = DungeonTypes.id(id);
            encounterId = DungeonTypes.id(encounterId);
            target = Math.max(1, target);
            timeLimitTicks = Math.max(20, timeLimitTicks);
            displayName = displayName == null || displayName.isBlank() ? id : displayName;
        }
    }

    public record ItemReward(ResourceLocation itemId, int count) {
        public ItemReward { count = Math.max(1, Math.min(64, count)); }
    }

    public record RewardDefinition(int xp, int gold, List<ItemReward> items) {
        public RewardDefinition { xp = Math.max(0, xp); gold = Math.max(0, gold); items = List.copyOf(items); }
    }

    public record EnemyDefinition(String id, EnemyKind kind, double maxHealth, double attackDamage, double movementSpeed,
                                  double armor, boolean elite, boolean shadowExtractable) {
        public EnemyDefinition {
            id = DungeonTypes.id(id);
            maxHealth = Math.max(1.0D, maxHealth);
            attackDamage = Math.max(0.0D, attackDamage);
            movementSpeed = Math.max(0.05D, movementSpeed);
            armor = Math.max(0.0D, armor);
        }
    }

    public record DungeonTemplate(String id, String displayName, GateRank rank, int totalTimeTicks,
                                  List<ObjectiveDefinition> objectives, Map<String, WaveDefinition> waves, RewardDefinition reward) {
        public DungeonTemplate {
            id = DungeonTypes.id(id);
            displayName = displayName == null || displayName.isBlank() ? id : displayName;
            totalTimeTicks = Math.max(1200, totalTimeTicks);
            objectives = List.copyOf(objectives);
            waves = Map.copyOf(new LinkedHashMap<>(waves));
        }
        public ObjectiveDefinition objective(int index) { return index >= 0 && index < objectives.size() ? objectives.get(index) : null; }
    }

    public record GateDefinition(String gateId, GateRank rank, String templateId, ResourceKey<Level> dimension,
                                 BlockPos position, int minimumLevel, UUID creator) {
        public GateDefinition {
            gateId = id(gateId);
            templateId = id(templateId);
            minimumLevel = Math.max(rank.minimumLevel(), minimumLevel);
        }
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("gate_id", gateId); tag.putString("rank", rank.name()); tag.putString("template_id", templateId);
            tag.putString("dimension", dimension.location().toString()); tag.putLong("position", position.asLong());
            tag.putInt("minimum_level", minimumLevel); if (creator != null) tag.putUUID("creator", creator); return tag;
        }
        public static GateDefinition load(CompoundTag tag) {
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("dimension"));
            if (location == null) location = Level.OVERWORLD.location();
            return new GateDefinition(tag.getString("gate_id"), GateRank.parse(tag.getString("rank")), tag.getString("template_id"),
                    ResourceKey.create(Registries.DIMENSION, location), BlockPos.of(tag.getLong("position")), tag.getInt("minimum_level"),
                    tag.hasUUID("creator") ? tag.getUUID("creator") : null);
        }
    }

    public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        public CompoundTag save(UUID playerId) {
            CompoundTag tag = new CompoundTag(); tag.putUUID("player", playerId); tag.putString("dimension", dimension.location().toString());
            tag.putDouble("x", x); tag.putDouble("y", y); tag.putDouble("z", z); tag.putFloat("yaw", yaw); tag.putFloat("pitch", pitch); return tag;
        }
        public static Map.Entry<UUID, ReturnPoint> load(CompoundTag tag) {
            if (!tag.hasUUID("player")) throw new IllegalArgumentException("Missing return-point player");
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("dimension"));
            if (location == null) location = Level.OVERWORLD.location();
            double x = tag.getDouble("x");
            double y = tag.getDouble("y");
            double z = tag.getDouble("z");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 30_000_000D || Math.abs(z) > 30_000_000D || Math.abs(y) > 4096D) {
                throw new IllegalArgumentException("Invalid return-point coordinates");
            }
            return Map.entry(tag.getUUID("player"), new ReturnPoint(ResourceKey.create(Registries.DIMENSION, location),
                    x, y, z, tag.getFloat("yaw"), tag.getFloat("pitch")));
        }
    }

    public static String id(String value) {
        if (value == null) return "unknown";
        String cleaned = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    private DungeonTypes() {}
}
