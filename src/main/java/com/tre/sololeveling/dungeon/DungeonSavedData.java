package com.tre.sololeveling.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class DungeonSavedData extends SavedData {
    private static final int MAX_ARENA_SLOTS = 4096;

    private final Map<String, DungeonTypes.GateDefinition> gates = new LinkedHashMap<>();
    private final Map<UUID, DungeonSession> sessions = new LinkedHashMap<>();
    private long nextSessionSequence = 1L;
    private int nextArenaSlot;

    public static DungeonSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DungeonSavedData::load, DungeonSavedData::new, DungeonTypes.DATA_NAME);
    }

    public Map<String, DungeonTypes.GateDefinition> gates() { return gates; }
    public Map<UUID, DungeonSession> sessions() { return sessions; }

    public UUID nextSessionId() {
        UUID candidate;
        do {
            long sequence = nextSessionSequence++;
            candidate = UUID.nameUUIDFromBytes(("sl-dungeon:" + sequence).getBytes(StandardCharsets.UTF_8));
        } while (sessions.containsKey(candidate));
        setDirty();
        return candidate;
    }

    public int allocateArenaSlot() {
        for (int attempt = 0; attempt < MAX_ARENA_SLOTS; attempt++) {
            int candidate = Math.floorMod(nextArenaSlot++, MAX_ARENA_SLOTS);
            nextArenaSlot = Math.floorMod(nextArenaSlot, MAX_ARENA_SLOTS);
            boolean occupied = sessions.values().stream().anyMatch(session -> session.arenaSlot() == candidate);
            if (!occupied) {
                setDirty();
                return candidate;
            }
        }
        return -1;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("next_session_sequence", nextSessionSequence);
        tag.putInt("next_arena_slot", nextArenaSlot);
        ListTag gateList = new ListTag();
        gates.values().forEach(gate -> gateList.add(gate.save()));
        tag.put("gates", gateList);
        ListTag sessionList = new ListTag();
        sessions.values().forEach(session -> sessionList.add(session.save()));
        tag.put("sessions", sessionList);
        return tag;
    }

    public static DungeonSavedData load(CompoundTag tag) {
        DungeonSavedData data = new DungeonSavedData();
        data.nextSessionSequence = Math.max(1L, tag.getLong("next_session_sequence"));
        data.nextArenaSlot = Math.floorMod(tag.getInt("next_arena_slot"), MAX_ARENA_SLOTS);
        ListTag gates = tag.getList("gates", Tag.TAG_COMPOUND);
        for (int i = 0; i < gates.size(); i++) {
            DungeonTypes.GateDefinition gate = DungeonTypes.GateDefinition.load(gates.getCompound(i));
            data.gates.put(gate.gateId(), gate);
        }
        ListTag sessions = tag.getList("sessions", Tag.TAG_COMPOUND);
        for (int i = 0; i < sessions.size(); i++) {
            DungeonSession session = DungeonSession.load(sessions.getCompound(i));
            data.sessions.put(session.sessionId(), session);
        }
        return data;
    }
}
