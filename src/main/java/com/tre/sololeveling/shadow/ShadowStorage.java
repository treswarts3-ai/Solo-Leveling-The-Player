package com.tre.sololeveling.shadow;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent shadow inventory backend and save migration boundary. */
public final class ShadowStorage {
    public static final int RECORD_VERSION = 3;
    public static final int MAX_LEVEL = 1000;
    public static final int MAX_STORED = 128;

    public enum Rank {
        NORMAL("Normal", 1.00D),
        ELITE("Elite", 1.18D),
        KNIGHT("Knight", 1.38D),
        ELITE_KNIGHT("Elite Knight", 1.62D),
        COMMANDER("Commander", 1.90D),
        MARSHAL("Marshal", 2.25D),
        GRAND_MARSHAL("Grand Marshal", 2.70D);

        private final String display;
        private final double multiplier;
        Rank(String display, double multiplier) { this.display = display; this.multiplier = multiplier; }
        public String display() { return display; }
        public double multiplier() { return multiplier; }

        public static Rank parse(String value) {
            if (value == null) return NORMAL;
            String key = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (key.equals("HIGH_KNIGHT")) key = "ELITE_KNIGHT";
            try { return valueOf(key); } catch (IllegalArgumentException ignored) { return NORMAL; }
        }
    }

    public enum Role {
        VANGUARD, ASSASSIN, GUARDIAN, RANGED, CASTER;
        public static Role parse(String value) {
            try { return valueOf(value == null ? "VANGUARD" : value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return VANGUARD; }
        }
    }

    public enum Trait {
        NONE("None"), BULWARK("Sovereign Bulwark"), EXECUTIONER("Executioner's Mark"),
        SOUL_SIPHON("Soul Siphon"), BLINK_GUARD("Blink Guard");
        private final String display;
        Trait(String display) { this.display = display; }
        public String display() { return display; }
        public static Trait parse(String value) {
            try { return valueOf(value == null ? "NONE" : value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return NONE; }
        }
    }

    public record Snapshot(UUID id, ResourceLocation type, String name, Rank rank, int level, int xp,
                           boolean active, UUID activeEntity, boolean namedElite, Role role, UUID owner,
                           Trait trait, boolean evolutionEligible) {}

    public static ListTag normalize(ServerPlayer owner) {
        ListTag source = HunterData.shadows(owner);
        ListTag normalized = new ListTag();
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < source.size(); i++) {
            CompoundTag migrated = migrate(owner, source.getCompound(i), i);
            UUID id = readUuid(migrated.getString("record_id"));
            ResourceLocation type = ResourceLocation.tryParse(migrated.getString("type"));
            if (id == null || type == null || ForgeRegistries.ENTITY_TYPES.getValue(type) == null || !ids.add(id)) continue;
            normalized.add(migrated);
        }
        HunterData.setShadows(owner, normalized);
        return normalized;
    }

    public static List<Snapshot> snapshots(ServerPlayer owner) {
        ListTag records = normalize(owner);
        List<Snapshot> result = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) result.add(snapshot(records.getCompound(i)));
        return List.copyOf(result);
    }

    public static Optional<Snapshot> inspect(ServerPlayer owner, String selector) {
        CompoundTag tag = findMutable(owner, selector);
        return tag == null ? Optional.empty() : Optional.of(snapshot(tag));
    }

    public static boolean add(ServerPlayer owner, EntityType<?> type, String name, Rank rank, int level) {
        ListTag records = normalize(owner);
        if (records.size() >= capacity(owner)) {
            owner.sendSystemMessage(Component.literal("[SYSTEM] Shadow storage is full.").withStyle(ChatFormatting.RED));
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) return false;
        CompoundTag record = defaults(owner, id, records.size());
        record.putString("name", sanitizeName(name, "Shadow " + (records.size() + 1)));
        record.putString("rank", rank.display());
        record.putInt("level", clampLevel(level));
        record.putBoolean("named_elite", !record.getString("name").startsWith("Shadow "));
        records.add(record);
        HunterData.setShadows(owner, records);
        HunterData.sync(owner);
        return true;
    }

    public static boolean remove(ServerPlayer owner, String selector) {
        ListTag records = normalize(owner);
        int index = findIndex(records, selector);
        if (index < 0 || records.getCompound(index).getBoolean("active")) return false;
        records.remove(index);
        HunterData.setShadows(owner, records);
        HunterData.sync(owner);
        return true;
    }

    public static void clear(ServerPlayer owner) {
        HunterData.setShadows(owner, new ListTag());
        HunterData.sync(owner);
    }

    public static int capacity(ServerPlayer owner) { return Math.max(0, Math.min(MAX_STORED, HunterData.getShadowCapacity(owner))); }
    public static int size(ServerPlayer owner) { return normalize(owner).size(); }
    public static int activeCount(ServerPlayer owner) {
        int active = 0;
        ListTag records = normalize(owner);
        for (int i = 0; i < records.size(); i++) if (records.getCompound(i).getBoolean("active")) active++;
        return active;
    }

    public static CompoundTag findMutable(ServerPlayer owner, String selector) {
        ListTag records = normalize(owner);
        int index = findIndex(records, selector);
        return index < 0 ? null : records.getCompound(index);
    }

    public static CompoundTag firstInactive(ServerPlayer owner) {
        ListTag records = normalize(owner);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            if (!record.getBoolean("active")) return record;
        }
        return null;
    }

    public static boolean setLevel(ServerPlayer owner, String selector, int level) {
        CompoundTag record = findMutable(owner, selector);
        if (record == null) return false;
        record.putInt("level", clampLevel(level));
        record.putInt("xp", Math.max(0, record.getInt("xp")));
        HunterData.sync(owner);
        return true;
    }

    public static boolean setRank(ServerPlayer owner, String selector, Rank rank) {
        CompoundTag record = findMutable(owner, selector);
        if (record == null) return false;
        record.putString("rank", rank.display());
        HunterData.sync(owner);
        return true;
    }

    public static void markActive(ServerPlayer owner, CompoundTag record, UUID entityId) {
        record.putBoolean("active", true);
        record.putString("active_entity", entityId.toString());
        record.putString("owner_id", owner.getUUID().toString());
        HunterData.sync(owner);
    }

    public static void markInactive(ServerPlayer owner, CompoundTag record) {
        record.putBoolean("active", false);
        record.putString("active_entity", "");
        HunterData.sync(owner);
    }

    public static void clearActiveState(ServerPlayer owner) {
        ListTag records = normalize(owner);
        for (int i = 0; i < records.size(); i++) {
            records.getCompound(i).putBoolean("active", false);
            records.getCompound(i).putString("active_entity", "");
        }
        HunterData.setShadows(owner, records);
        HunterData.setActiveShadows(owner, new ListTag());
        HunterData.sync(owner);
    }

    public static UUID recordId(CompoundTag record) { return readUuid(record.getString("record_id")); }
    public static UUID activeEntityId(CompoundTag record) { return readUuid(record.getString("active_entity")); }
    public static Rank rank(CompoundTag record) { return Rank.parse(record.getString("rank")); }
    public static Role role(CompoundTag record) { return Role.parse(record.getString("role")); }
    public static Trait trait(CompoundTag record) { return Trait.parse(record.getString("trait")); }

    private static CompoundTag migrate(ServerPlayer owner, CompoundTag source, int index) {
        CompoundTag tag = source.copy();
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("type"));
        if (type == null) type = new ResourceLocation("minecraft", "zombie");
        UUID recordId = readUuid(tag.getString("record_id"));
        if (recordId == null) recordId = UUID.randomUUID();
        tag.putInt("record_version", RECORD_VERSION);
        tag.putString("record_id", recordId.toString());
        tag.putString("owner_id", owner.getUUID().toString());
        tag.putString("type", type.toString());
        tag.putString("name", sanitizeName(tag.getString("name"), "Shadow " + (index + 1)));
        tag.putString("rank", rank(tag).display());
        tag.putInt("level", clampLevel(tag.getInt("level")));
        tag.putInt("xp", Math.max(0, tag.getInt("xp")));
        if (!tag.contains("active", Tag.TAG_BYTE)) tag.putBoolean("active", false);
        if (!tag.contains("active_entity", Tag.TAG_STRING)) tag.putString("active_entity", "");
        if (!tag.contains("named_elite", Tag.TAG_BYTE)) tag.putBoolean("named_elite", !tag.getString("name").startsWith("Shadow "));
        if (!tag.contains("role", Tag.TAG_STRING)) tag.putString("role", inferRole(type).name());
        if (!tag.contains("trait", Tag.TAG_STRING)) tag.putString("trait",
                tag.getBoolean("named_elite") ? namedTrait(recordId).name() : Trait.NONE.name());
        tag.putBoolean("evolution_eligible", tag.getInt("level") >= 50 && rank(tag).ordinal() < Rank.COMMANDER.ordinal());
        return tag;
    }

    private static CompoundTag defaults(ServerPlayer owner, ResourceLocation type, int index) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("record_version", RECORD_VERSION);
        tag.putString("record_id", UUID.randomUUID().toString());
        tag.putString("owner_id", owner.getUUID().toString());
        tag.putString("type", type.toString());
        tag.putString("name", "Shadow " + (index + 1));
        tag.putString("rank", Rank.NORMAL.display());
        tag.putInt("level", 1);
        tag.putInt("xp", 0);
        tag.putBoolean("active", false);
        tag.putString("active_entity", "");
        tag.putBoolean("named_elite", false);
        tag.putString("role", inferRole(type).name());
        tag.putString("trait", Trait.NONE.name());
        tag.putBoolean("evolution_eligible", false);
        return tag;
    }

    private static Snapshot snapshot(CompoundTag tag) {
        UUID id = readUuid(tag.getString("record_id"));
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("type"));
        return new Snapshot(id, type, tag.getString("name"), rank(tag), tag.getInt("level"), tag.getInt("xp"),
                tag.getBoolean("active"), activeEntityId(tag), tag.getBoolean("named_elite"), role(tag),
                readUuid(tag.getString("owner_id")), trait(tag), tag.getBoolean("evolution_eligible"));
    }

    private static Role inferRole(ResourceLocation type) {
        String path = type == null ? "" : type.getPath();
        if (path.contains("skeleton") || path.contains("pillager")) return Role.RANGED;
        if (path.contains("evoker") || path.contains("witch") || path.contains("blaze")) return Role.CASTER;
        if (path.contains("ravager") || path.contains("golem")) return Role.GUARDIAN;
        if (path.contains("spider") || path.contains("vex")) return Role.ASSASSIN;
        return Role.VANGUARD;
    }

    private static Trait namedTrait(UUID id) {
        Trait[] options = {Trait.BULWARK, Trait.EXECUTIONER, Trait.SOUL_SIPHON, Trait.BLINK_GUARD};
        return options[Math.floorMod(id.hashCode(), options.length)];
    }

    private static int findIndex(ListTag records, String selector) {
        if (selector == null || selector.isBlank()) return -1;
        try {
            int index = Integer.parseInt(selector);
            if (index >= 0 && index < records.size()) return index;
            if (index > 0 && index <= records.size()) return index - 1;
        } catch (NumberFormatException ignored) { }
        String key = selector.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            if (record.getString("record_id").toLowerCase(Locale.ROOT).startsWith(key)
                    || record.getString("name").toLowerCase(Locale.ROOT).equals(key)) return i;
        }
        return -1;
    }

    private static int clampLevel(int value) { return Math.max(1, Math.min(MAX_LEVEL, value)); }
    private static String sanitizeName(String value, String fallback) {
        String clean = value == null ? "" : value.replaceAll("[\\p{Cntrl}§]", "").trim();
        if (clean.isEmpty()) clean = fallback;
        return clean.length() <= 64 ? clean : clean.substring(0, 64);
    }
    private static UUID readUuid(String value) {
        try { return value == null || value.isBlank() ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private ShadowStorage() {}
}
