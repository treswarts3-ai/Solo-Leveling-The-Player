package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DungeonSession {
    private final UUID sessionId;
    private final String gateId;
    private final String templateId;
    private final UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Map<UUID, DungeonTypes.ReturnPoint> returnPoints = new LinkedHashMap<>();
    private final Set<UUID> trackedEntities = new LinkedHashSet<>();
    private final Set<UUID> rewardedMembers = new LinkedHashSet<>();
    private boolean rewardDistributionStarted;
    private ResourceKey<Level> dungeonDimension = Level.OVERWORLD;
    private BlockPos arenaOrigin = BlockPos.ZERO;
    private DungeonTypes.SessionState state = DungeonTypes.SessionState.WAITING;
    private int objectiveIndex;
    private int objectiveProgress;
    private int remainingTicks;
    private int objectiveTicksRemaining;
    private int liveEnemyCount;
    private int totalSpawns;
    private final int arenaSlot;
    private int arenaVersion;
    private long lastActiveGameTime;
    private long cleanupAfterGameTime;
    private boolean arenaBuilt;
    private boolean encounterSpawned;
    private boolean rewardRoomCreated;
    private boolean startRequested;
    private long generationVisitedBlocks;
    private long generationChangedBlocks;
    private int generationTicks;
    private int generationMaxVisitedPerTick;
    private int generationMaxChangedPerTick;
    private String pendingArenaJob = "";
    private BlockPos migrationOrigin = BlockPos.ZERO;
    private boolean hasMigrationOrigin;
    private UUID bossId;
    private String failureReason = "";

    public DungeonSession(UUID sessionId, String gateId, String templateId, UUID owner, Collection<UUID> members, int totalTimeTicks, int arenaSlot) {
        this.sessionId = sessionId;
        this.gateId = DungeonTypes.id(gateId);
        this.templateId = DungeonTypes.id(templateId);
        this.owner = owner;
        this.members.add(owner);
        this.members.addAll(members);
        this.remainingTicks = Math.max(20, totalTimeTicks);
        this.arenaSlot = Math.max(0, arenaSlot);
    }

    public UUID sessionId() { return sessionId; }
    public String gateId() { return gateId; }
    public String templateId() { return templateId; }
    public UUID owner() { return owner; }
    public Set<UUID> members() { return Collections.unmodifiableSet(members); }
    public Map<UUID, DungeonTypes.ReturnPoint> returnPoints() { return returnPoints; }
    public Set<UUID> trackedEntities() { return trackedEntities; }
    public ResourceKey<Level> dungeonDimension() { return dungeonDimension; }
    public BlockPos arenaOrigin() { return arenaOrigin; }
    public DungeonTypes.SessionState state() { return state; }
    public int objectiveIndex() { return objectiveIndex; }
    public int objectiveProgress() { return objectiveProgress; }
    public int remainingTicks() { return remainingTicks; }
    public int objectiveTicksRemaining() { return objectiveTicksRemaining; }
    public int liveEnemyCount() { return liveEnemyCount; }
    public int totalSpawns() { return totalSpawns; }
    public int arenaSlot() { return arenaSlot; }
    public int arenaVersion() { return arenaVersion; }
    public long lastActiveGameTime() { return lastActiveGameTime; }
    public long cleanupAfterGameTime() { return cleanupAfterGameTime; }
    public boolean arenaBuilt() { return arenaBuilt; }
    public boolean encounterSpawned() { return encounterSpawned; }
    public boolean rewardGranted() { return rewardDistributionStarted; }
    public boolean rewardGrantedTo(UUID playerId) { return rewardedMembers.contains(playerId); }
    public int rewardedMemberCount() { return rewardedMembers.size(); }
    public boolean rewardRoomCreated() { return rewardRoomCreated; }
    public boolean startRequested() { return startRequested; }
    public long generationVisitedBlocks() { return generationVisitedBlocks; }
    public long generationChangedBlocks() { return generationChangedBlocks; }
    public int generationTicks() { return generationTicks; }
    public int generationMaxVisitedPerTick() { return generationMaxVisitedPerTick; }
    public int generationMaxChangedPerTick() { return generationMaxChangedPerTick; }
    public String pendingArenaJob() { return pendingArenaJob; }
    public BlockPos migrationOrigin() { return migrationOrigin; }
    public boolean hasMigrationOrigin() { return hasMigrationOrigin; }
    public UUID bossId() { return bossId; }
    public String failureReason() { return failureReason; }
    public boolean contains(UUID playerId) { return members.contains(playerId); }
    public boolean isTerminal() { return state == DungeonTypes.SessionState.COMPLETED || state == DungeonTypes.SessionState.FAILED || state == DungeonTypes.SessionState.CLEANUP; }
    public DungeonTypes.ObjectiveDefinition currentObjective(DungeonTypes.DungeonTemplate template) { return template == null ? null : template.objective(objectiveIndex); }

    public void setDungeonLocation(ResourceKey<Level> dimension, BlockPos origin) { dungeonDimension = dimension; arenaOrigin = origin.immutable(); }
    public void setState(DungeonTypes.SessionState value) { state = value; }
    public void setObjectiveProgress(int value) { objectiveProgress = Math.max(0, value); }
    public void addObjectiveProgress(int value) { objectiveProgress = Math.max(0, objectiveProgress + value); }
    public void advanceObjective() { objectiveIndex++; objectiveProgress = 0; encounterSpawned = false; bossId = null; }
    public void setRemainingTicks(int value) { remainingTicks = Math.max(0, value); }
    public void setObjectiveTicksRemaining(int value) { objectiveTicksRemaining = Math.max(0, value); }
    public void tickTimers() { if (remainingTicks > 0) remainingTicks--; if (objectiveTicksRemaining > 0) objectiveTicksRemaining--; }
    public void setLiveEnemyCount(int value) { liveEnemyCount = Math.max(0, value); }
    public void enemySpawned(UUID id) { trackedEntities.add(id); liveEnemyCount++; totalSpawns++; }
    public void entityRemoved(UUID id) { if (trackedEntities.remove(id)) liveEnemyCount = Math.max(0, liveEnemyCount - 1); }
    public void setArenaVersion(int value) { arenaVersion = Math.max(0, value); }
    public void setLastActiveGameTime(long value) { lastActiveGameTime = value; }
    public void setCleanupAfterGameTime(long value) { cleanupAfterGameTime = value; }
    public void setArenaBuilt(boolean value) { arenaBuilt = value; }
    public void setEncounterSpawned(boolean value) { encounterSpawned = value; }
    public boolean markRewardGranted(UUID playerId) { return members.contains(playerId) && rewardedMembers.add(playerId); }
    public void setRewardGranted(boolean value) { rewardDistributionStarted = value; if (!value) rewardedMembers.clear(); }
    public void setRewardRoomCreated(boolean value) { rewardRoomCreated = value; }
    public void setStartRequested(boolean value) { startRequested = value; }
    public void setPendingArenaJob(String value) { pendingArenaJob = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT); }
    public void setMigrationOrigin(BlockPos value) {
        hasMigrationOrigin = value != null;
        migrationOrigin = value == null ? BlockPos.ZERO : value.immutable();
    }
    public void recordGeneration(MasterDungeonBuilder.JobReport report) {
        if (report == null) return;
        generationVisitedBlocks = report.visitedBlocks();
        generationChangedBlocks = report.changedBlocks();
        generationTicks = report.elapsedTicks();
        generationMaxVisitedPerTick = report.maxVisitedInTick();
        generationMaxChangedPerTick = report.maxChangedInTick();
    }
    public void setBossId(UUID value) { bossId = value; }
    public void setFailureReason(String value) { failureReason = value == null ? "" : value; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("session_id", sessionId);
        tag.putString("gate_id", gateId);
        tag.putString("template_id", templateId);
        tag.putUUID("owner", owner);
        tag.putString("state", state.name());
        tag.putString("dimension", dungeonDimension.location().toString());
        tag.putLong("arena_origin", arenaOrigin.asLong());
        tag.putInt("arena_slot", arenaSlot);
        tag.putInt("arena_version", arenaVersion);
        tag.putInt("objective_index", objectiveIndex);
        tag.putInt("objective_progress", objectiveProgress);
        tag.putInt("remaining_ticks", remainingTicks);
        tag.putInt("objective_ticks_remaining", objectiveTicksRemaining);
        tag.putInt("live_enemy_count", liveEnemyCount);
        tag.putInt("total_spawns", totalSpawns);
        tag.putLong("last_active", lastActiveGameTime);
        tag.putLong("cleanup_after", cleanupAfterGameTime);
        tag.putBoolean("arena_built", arenaBuilt);
        tag.putBoolean("encounter_spawned", encounterSpawned);
        tag.putBoolean("reward_granted", rewardDistributionStarted);
        tag.putBoolean("reward_room_created", rewardRoomCreated);
        tag.putBoolean("start_requested", startRequested);
        tag.putLong("generation_visited", generationVisitedBlocks);
        tag.putLong("generation_changed", generationChangedBlocks);
        tag.putInt("generation_ticks", generationTicks);
        tag.putInt("generation_max_visited_tick", generationMaxVisitedPerTick);
        tag.putInt("generation_max_changed_tick", generationMaxChangedPerTick);
        tag.putString("pending_arena_job", pendingArenaJob);
        tag.putBoolean("has_migration_origin", hasMigrationOrigin);
        if (hasMigrationOrigin) tag.putLong("migration_origin", migrationOrigin.asLong());
        tag.putString("failure_reason", failureReason);
        if (bossId != null) tag.putUUID("boss_id", bossId);
        ListTag memberList = new ListTag();
        members.forEach(id -> memberList.add(StringTag.valueOf(id.toString())));
        tag.put("members", memberList);
        ListTag returnList = new ListTag();
        returnPoints.forEach((id, point) -> returnList.add(point.save(id)));
        tag.put("return_points", returnList);
        ListTag entityList = new ListTag();
        trackedEntities.forEach(id -> entityList.add(StringTag.valueOf(id.toString())));
        tag.put("tracked_entities", entityList);
        ListTag rewardedList = new ListTag();
        rewardedMembers.forEach(id -> rewardedList.add(StringTag.valueOf(id.toString())));
        tag.put("rewarded_members", rewardedList);
        return tag;
    }

    public static DungeonSession load(CompoundTag tag) {
        Set<UUID> members = uuidList(tag.getList("members", Tag.TAG_STRING));
        DungeonSession session = new DungeonSession(tag.getUUID("session_id"), tag.getString("gate_id"), tag.getString("template_id"),
                tag.getUUID("owner"), members, tag.getInt("remaining_ticks"), tag.getInt("arena_slot"));
        try { session.state = DungeonTypes.SessionState.valueOf(tag.getString("state")); }
        catch (IllegalArgumentException ignored) { session.state = DungeonTypes.SessionState.FAILED; session.failureReason = "Invalid persisted state"; }
        ResourceLocation location = ResourceLocation.tryParse(tag.getString("dimension"));
        if (location == null) location = Level.OVERWORLD.location();
        session.dungeonDimension = ResourceKey.create(Registries.DIMENSION, location);
        session.arenaOrigin = BlockPos.of(tag.getLong("arena_origin"));
        session.objectiveIndex = Math.max(0, tag.getInt("objective_index"));
        session.objectiveProgress = Math.max(0, tag.getInt("objective_progress"));
        session.remainingTicks = Math.max(0, tag.getInt("remaining_ticks"));
        session.objectiveTicksRemaining = Math.max(0, tag.getInt("objective_ticks_remaining"));
        session.liveEnemyCount = Math.max(0, tag.getInt("live_enemy_count"));
        session.totalSpawns = Math.max(0, tag.getInt("total_spawns"));
        session.arenaVersion = Math.max(0, tag.getInt("arena_version"));
        session.lastActiveGameTime = tag.getLong("last_active");
        session.cleanupAfterGameTime = tag.getLong("cleanup_after");
        session.arenaBuilt = tag.getBoolean("arena_built");
        session.encounterSpawned = tag.getBoolean("encounter_spawned");
        session.rewardRoomCreated = tag.getBoolean("reward_room_created");
        session.startRequested = tag.getBoolean("start_requested");
        session.generationVisitedBlocks = Math.max(0L, tag.getLong("generation_visited"));
        session.generationChangedBlocks = Math.max(0L, tag.getLong("generation_changed"));
        session.generationTicks = Math.max(0, tag.getInt("generation_ticks"));
        session.generationMaxVisitedPerTick = Math.max(0, tag.getInt("generation_max_visited_tick"));
        session.generationMaxChangedPerTick = Math.max(0, tag.getInt("generation_max_changed_tick"));
        session.pendingArenaJob = tag.getString("pending_arena_job");
        session.hasMigrationOrigin = tag.getBoolean("has_migration_origin");
        if (session.hasMigrationOrigin) session.migrationOrigin = BlockPos.of(tag.getLong("migration_origin"));
        session.failureReason = tag.getString("failure_reason");
        if (tag.hasUUID("boss_id")) session.bossId = tag.getUUID("boss_id");
        ListTag returns = tag.getList("return_points", Tag.TAG_COMPOUND);
        for (int i = 0; i < returns.size(); i++) {
            Map.Entry<UUID, DungeonTypes.ReturnPoint> entry = DungeonTypes.ReturnPoint.load(returns.getCompound(i));
            session.returnPoints.put(entry.getKey(), entry.getValue());
        }
        session.trackedEntities.addAll(uuidList(tag.getList("tracked_entities", Tag.TAG_STRING)));
        session.rewardDistributionStarted = tag.getBoolean("reward_granted");
        if (tag.contains("rewarded_members", Tag.TAG_LIST)) {
            session.rewardedMembers.addAll(uuidList(tag.getList("rewarded_members", Tag.TAG_STRING)));
        } else if (session.rewardDistributionStarted) {
            session.rewardedMembers.addAll(session.members);
        }
        return session;
    }

    private static Set<UUID> uuidList(ListTag list) {
        Set<UUID> result = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            try { result.add(UUID.fromString(list.getString(i))); }
            catch (IllegalArgumentException ignored) {}
        }
        return result;
    }
}
