package com.tre.sololeveling.client.ui;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Client-only System presentation state. Gameplay values always originate on the server. */
public final class SystemUi {
    public static final int ACTION_SLOT_COUNT = 6;
    public static final ResourceLocation ICONS = new ResourceLocation("sololeveling", "textures/gui/system_icons.png");

    public enum NotificationKind {
        INFO, WARNING, ERROR, LEVEL_UP, RANK_ADVANCEMENT, SKILL_UNLOCK,
        QUEST_ACCEPTED, QUEST_COMPLETE, QUEST_FAILED, ITEM_OBTAINED,
        GOLD, STAT_INCREASED, DUNGEON
    }

    public enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public record Icon(int u, int v) {
        public static final Icon PLAYER = new Icon(0, 0);
        public static final Icon STATS = new Icon(16, 0);
        public static final Icon ABILITIES = new Icon(32, 0);
        public static final Icon INVENTORY = new Icon(48, 0);
        public static final Icon EQUIPMENT = new Icon(64, 0);
        public static final Icon QUESTS = new Icon(80, 0);
        public static final Icon SHOP = new Icon(96, 0);
        public static final Icon DUNGEONS = new Icon(112, 0);
        public static final Icon SHADOWS = new Icon(0, 16);
        public static final Icon SETTINGS = new Icon(16, 16);
        public static final Icon LOCKED = new Icon(32, 16);
        public static final Icon MANA = new Icon(48, 16);
        public static final Icon COOLDOWN = new Icon(64, 16);
        public static final Icon GOLD = new Icon(80, 16);
        public static final Icon SUCCESS = new Icon(96, 16);
        public static final Icon ALERT = new Icon(112, 16);
    }

    public static final class Data {
        private final CompoundTag tag;

        public Data(CompoundTag tag) {
            this.tag = tag == null ? new CompoundTag() : tag;
        }

        public CompoundTag raw() { return tag; }
        public boolean awakened() { return tag.getBoolean("awakened"); }
        public boolean hudEnabled() { return !tag.contains("hud") || tag.getBoolean("hud"); }
        public int level() { return Math.max(1, tag.getInt("level")); }
        public int xp() { return Math.max(0, tag.getInt("xp")); }
        public int gold() { return Math.max(0, tag.getInt("gold")); }
        public int mana() { return Math.max(0, tag.getInt("mana")); }
        public int statPoints() { return Math.max(0, tag.getInt("stat_points")); }
        public int stat(String name) { return Math.max(1, tag.getInt(name)); }
        public int bonus(String name) { return tag.getInt("bonus_" + name); }
        public boolean skillUnlocked(String skill) {
            if (skill == null || skill.isBlank()) return false;
            if ("dash".equals(skill)) return awakened();
            return tag.getBoolean("skill_" + skill);
        }
        public boolean dailyComplete() { return tag.getBoolean("daily_complete"); }
        public boolean dailyClaimed() { return tag.getBoolean("daily_claimed"); }
        public ListTag compounds(String key) { return tag.getList(key, Tag.TAG_COMPOUND); }
        public ListTag strings(String key) { return tag.getList(key, Tag.TAG_STRING); }
        public String text(String key, String fallback) {
            String value = tag.getString(key);
            return value == null || value.isBlank() ? fallback : value;
        }

        public int xpRequired() {
            if (tag.contains("xp_needed", Tag.TAG_INT)) return Math.max(1, tag.getInt("xp_needed"));
            if (tag.contains("xp_required", Tag.TAG_INT)) return Math.max(1, tag.getInt("xp_required"));
            return Math.max(100, (int)Math.floor(100.0D * Math.pow(level(), 1.55D)));
        }

        public int maxMana() {
            if (tag.contains("max_mana", Tag.TAG_INT)) return Math.max(1, tag.getInt("max_mana"));
            return Math.max(1, 100 + level() * 2 + stat("intelligence") * 8 + (tag.getBoolean("black_heart") ? 1000 : 0));
        }

        public int shadowCapacity() {
            if (tag.contains("shadow_capacity", Tag.TAG_INT)) return Math.max(0, tag.getInt("shadow_capacity"));
            return Math.min(100, 3 + stat("intelligence") / 5 + level() / 10
                    + Math.max(0, tag.getInt("shadow_capacity_bonus")) + (tag.getBoolean("black_heart") ? 20 : 0));
        }

        public String rank() {
            String synced = tag.getString("hunter_rank");
            if (!synced.isBlank()) return synced;
            synced = tag.getString("rank");
            if (!synced.isBlank()) return synced;
            String override = tag.getString("rank_override");
            if (!override.isBlank()) return override;
            int value = level();
            if (value >= 100 && tag.getBoolean("black_heart")) return "Shadow Monarch";
            if (value >= 80) return "National-Level";
            if (value >= 60) return "S-Rank";
            if (value >= 40) return "A-Rank";
            if (value >= 30) return "B-Rank";
            if (value >= 20) return "C-Rank";
            if (value >= 10) return "D-Rank";
            return "E-Rank";
        }

        public long cooldownRemaining(String skill) {
            Minecraft minecraft = Minecraft.getInstance();
            long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
            return Math.max(0L, tag.getLong("cooldown_" + skill) - now);
        }

        public String activeQuestName() { return titleCase(text("active_main_quest", "No active quest")); }

        public String activeQuestProgress() {
            return switch (Math.max(0, tag.getInt("progression_stage"))) {
                case 0 -> tutorialProgress();
                case 1 -> "Dagger damage " + Math.max(0, tag.getInt("quest_dagger_damage")) + " / 500";
                case 2 -> "Level " + level() + " / 40, kills " + Math.max(0, tag.getInt("job_change_kills")) + " / 25";
                case 3 -> "Extracted " + Math.max(0, tag.getInt("shadow_extractions")) + " / 3, summon a shadow";
                case 4 -> "Level " + level() + " / 80, stored shadows " + compounds("shadows").size() + " / 10";
                default -> "All synchronized progression completed";
            };
        }

        public String dungeonStatus() {
            if (tag.getBoolean("dungeon_active")) {
                String name = text("dungeon_name", "Dungeon run active");
                String state = tag.getString("dungeon_state");
                if (!state.equals("active")) return name + " - " + titleCase(state);
                String objective = tag.getString("dungeon_objective");
                if (!objective.isBlank()) {
                    int progress = Math.max(0, tag.getInt("dungeon_objective_progress"));
                    int target = Math.max(0, tag.getInt("dungeon_objective_target"));
                    return name + " - " + objective + (target > 0 ? " " + progress + "/" + target : "");
                }
                return name;
            }
            if (tag.getString("dungeon_state").equals("completed")) return "Dungeon cleared - recovery available";
            if (tag.getString("dungeon_state").equals("failed")) return "Dungeon failed - safe return available";
            if (tag.getBoolean("dungeon_gate_nearby")) return "Gate detected nearby";
            return "No active dungeon";
        }

        private String tutorialProgress() {
            int completed = (tag.getBoolean("tutorial_system_opened") ? 1 : 0)
                    + (tag.getBoolean("tutorial_stat_allocated") ? 1 : 0)
                    + (tag.getBoolean("daily_claimed") ? 1 : 0);
            return "Awakening tutorial " + completed + " / 3";
        }
    }

    public static final class Notification {
        private final NotificationKind kind;
        private final String title;
        private final String detail;
        private final long durationMs;
        private long startedAt;

        private Notification(NotificationKind kind, String title, String detail, long durationMs) {
            this.kind = kind;
            this.title = title;
            this.detail = detail;
            this.durationMs = Math.max(1200L, durationMs);
        }

        public NotificationKind kind() { return kind; }
        public String title() { return title; }
        public String detail() { return detail; }
        public long createdAt() { return startedAt; }
        public long expiresAt() { return startedAt <= 0L ? Long.MAX_VALUE : startedAt + durationMs; }
        public long durationMs() { return durationMs; }
    }

    public static final class Notifications {
        private static final Deque<Notification> QUEUE = new ArrayDeque<>();
        private static boolean initialized;
        private static int backendNotificationCount;

        public static synchronized void accept(CompoundTag previous, CompoundTag next) {
            CompoundTag oldTag = previous == null ? new CompoundTag() : previous;
            CompoundTag newTag = next == null ? new CompoundTag() : next;
            if (!initialized) {
                initialized = true;
                backendNotificationCount = backendNotificationCount(newTag);
                return;
            }
            int oldLevel = Math.max(1, oldTag.getInt("level"));
            int newLevel = Math.max(1, newTag.getInt("level"));
            if (newLevel > oldLevel) push(NotificationKind.LEVEL_UP, "LEVEL UP", "Level " + oldLevel + "  →  " + newLevel, 4800L);
            String oldRank = new Data(oldTag).rank();
            String newRank = new Data(newTag).rank();
            if (!newRank.equals(oldRank)) push(NotificationKind.RANK_ADVANCEMENT, "RANK ADVANCEMENT", oldRank + "  →  " + newRank, 5000L);
            if (!oldTag.getBoolean("daily_complete") && newTag.getBoolean("daily_complete")) {
                push(NotificationKind.QUEST_COMPLETE, "QUEST COMPLETE", "Daily training objectives completed", 4300L);
            }
            int oldCompleted = oldTag.getList("completed_quests", Tag.TAG_STRING).size();
            int newCompleted = newTag.getList("completed_quests", Tag.TAG_STRING).size();
            if (newCompleted > oldCompleted) push(NotificationKind.QUEST_COMPLETE, "QUEST COMPLETE", "Quest log updated", 4300L);
            for (String skill : HunterData.SKILLS) {
                if (!oldTag.getBoolean("skill_" + skill) && newTag.getBoolean("skill_" + skill)) {
                    push(NotificationKind.SKILL_UNLOCK, "ABILITY UNLOCKED", titleCase(skill), 4200L);
                }
            }
            String oldQuicksilverEvolution = oldTag.getString("evolution_quicksilver");
            String newQuicksilverEvolution = newTag.getString("evolution_quicksilver");
            if (oldQuicksilverEvolution.isBlank() && !newQuicksilverEvolution.isBlank()) {
                push(NotificationKind.SKILL_UNLOCK, "ABILITY EVOLVED",
                        "Quicksilver - " + titleCase(newQuicksilverEvolution), 4600L);
            }
            int goldDelta = Math.max(0, newTag.getInt("gold")) - Math.max(0, oldTag.getInt("gold"));
            if (goldDelta != 0) push(NotificationKind.GOLD, goldDelta > 0 ? "GOLD ACQUIRED" : "GOLD SPENT",
                    (goldDelta > 0 ? "+" : "") + goldDelta + " G", 3000L);
            if (newTag.getInt("stat_points") < oldTag.getInt("stat_points")) {
                push(NotificationKind.STAT_INCREASED, "STAT INCREASED", "Hunter attributes updated", 2600L);
            }
            acceptBackendNotifications(newTag);
        }

        public static synchronized Notification current() {
            prune();
            Notification current = QUEUE.peekFirst();
            if (current != null && current.startedAt <= 0L) current.startedAt = System.currentTimeMillis();
            return current;
        }

        /** Compatibility view; the renderer intentionally shows only the queue head. */
        public static synchronized List<Notification> active() {
            Notification current = current();
            return current == null ? List.of() : Collections.singletonList(current);
        }

        public static synchronized int queued() { prune(); return QUEUE.size(); }
        public static synchronized void pushInfo(String title, String detail) { push(NotificationKind.INFO, title, detail, 3300L); }
        public static synchronized void pushWarning(String detail) { push(NotificationKind.WARNING, "SYSTEM WARNING", detail, 3800L); }
        public static synchronized void pushFailure(String detail) { push(NotificationKind.ERROR, "SYSTEM ERROR", detail, 3800L); }

        private static void acceptBackendNotifications(CompoundTag tag) {
            int count = backendNotificationCount(tag);
            if (count <= backendNotificationCount) {
                backendNotificationCount = count;
                return;
            }
            Tag value = tag.get("notifications");
            if (value instanceof ListTag list) {
                for (int i = Math.max(backendNotificationCount, 0); i < list.size(); i++) {
                    Tag entry = list.get(i);
                    if (entry instanceof CompoundTag compound) {
                        String title = compound.getString("title");
                        String message = compound.getString("message");
                        push(classify(title + " " + message), title.isBlank() ? "SYSTEM" : title, message, 4000L);
                    } else {
                        String message = list.getString(i);
                        push(classify(message), "SYSTEM", message, 4000L);
                    }
                }
            }
            backendNotificationCount = count;
        }

        private static NotificationKind classify(String value) {
            String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
            if (normalized.contains("dungeon") || normalized.contains("gate")) return NotificationKind.DUNGEON;
            if (normalized.contains("quest") && normalized.contains("fail")) return NotificationKind.QUEST_FAILED;
            if (normalized.contains("quest") && normalized.contains("complete")) return NotificationKind.QUEST_COMPLETE;
            if (normalized.contains("item") || normalized.contains("obtained") || normalized.contains("purchased")) return NotificationKind.ITEM_OBTAINED;
            if (normalized.contains("error") || normalized.contains("failed") || normalized.contains("cannot")) return NotificationKind.ERROR;
            if (normalized.contains("warning")) return NotificationKind.WARNING;
            return NotificationKind.INFO;
        }

        private static int backendNotificationCount(CompoundTag tag) {
            Tag value = tag.get("notifications");
            return value instanceof ListTag list ? list.size() : 0;
        }

        private static void push(NotificationKind kind, String title, String detail, long durationMs) {
            prune();
            String cleanTitle = title == null || title.isBlank() ? "SYSTEM" : title;
            String cleanDetail = detail == null ? "" : detail;
            Notification newest = QUEUE.peekLast();
            if (newest != null && newest.kind == kind && newest.title.equals(cleanTitle) && newest.detail.equals(cleanDetail)) return;
            Notification incoming = new Notification(kind, cleanTitle, cleanDetail, durationMs);
            List<Notification> ordered = new ArrayList<>(QUEUE);
            int insertion = ordered.size();
            // Do not interrupt a notification already being displayed. Everything behind
            // it follows the documented System priority while remaining stable within a tier.
            int firstMovable = !ordered.isEmpty() && ordered.get(0).startedAt > 0L ? 1 : 0;
            for (int i = firstMovable; i < ordered.size(); i++) {
                if (priority(kind) < priority(ordered.get(i).kind)) {
                    insertion = i;
                    break;
                }
            }
            ordered.add(insertion, incoming);
            QUEUE.clear();
            QUEUE.addAll(ordered);
            while (QUEUE.size() > 12) QUEUE.removeLast();
            playFeedback(kind);
        }

        private static int priority(NotificationKind kind) {
            return switch (kind) {
                case LEVEL_UP -> 1;
                case SKILL_UNLOCK -> 2;
                case QUEST_COMPLETE -> 3;
                case RANK_ADVANCEMENT -> 4;
                case DUNGEON -> 5;
                case ITEM_OBTAINED -> 6;
                case ERROR, QUEST_FAILED -> 7;
                default -> 8;
            };
        }

        private static void playFeedback(NotificationKind kind) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            float pitch = switch (kind) {
                case ERROR, QUEST_FAILED -> 0.65F;
                case LEVEL_UP, RANK_ADVANCEMENT -> 1.45F;
                case SKILL_UNLOCK, QUEST_COMPLETE -> 1.2F;
                default -> 1.0F;
            };
            minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.35F, pitch);
        }

        private static void prune() {
            long now = System.currentTimeMillis();
            while (!QUEUE.isEmpty()) {
                Notification current = QUEUE.peekFirst();
                if (current.startedAt <= 0L || current.startedAt + current.durationMs > now) break;
                QUEUE.removeFirst();
            }
        }

        private Notifications() {}
    }

    public static final class Actions {
        private static final Map<String, Long> LAST_SENT = new HashMap<>();

        public static boolean send(String action) { return send(action, 160L); }

        public static synchronized boolean send(String action, long minimumIntervalMs) {
            if (action == null || action.isBlank() || action.length() > 128) return false;
            long now = System.currentTimeMillis();
            long previous = LAST_SENT.getOrDefault(action, Long.MIN_VALUE / 4L);
            if (now - previous < Math.max(80L, minimumIntervalMs)) return false;
            LAST_SENT.put(action, now);
            ModNetwork.CHANNEL.sendToServer(new ActionPacket(action));
            return true;
        }

        public static synchronized boolean coolingDown(String action, long intervalMs) {
            return System.currentTimeMillis() - LAST_SENT.getOrDefault(action, Long.MIN_VALUE / 4L) < intervalMs;
        }

        private Actions() {}
    }

    public static final class Cooldowns {
        private static final Map<String, Long> OBSERVED_MAX = new HashMap<>();

        public static long remaining(Data data, String skill) {
            long remaining = data.cooldownRemaining(skill);
            if (remaining > 0L) OBSERVED_MAX.merge(skill, remaining, Math::max);
            else OBSERVED_MAX.remove(skill);
            return remaining;
        }

        public static float progress(Data data, String skill) {
            long remaining = remaining(data, skill);
            long maximum = Math.max(1L, OBSERVED_MAX.getOrDefault(skill, remaining));
            return Mth.clamp(remaining / (float)maximum, 0.0F, 1.0F);
        }

        public static String remainingText(Data data, String skill) {
            long ticks = remaining(data, skill);
            if (ticks <= 0L) return "READY";
            return String.format(Locale.ROOT, "%.1fs", ticks / 20.0D);
        }

        private Cooldowns() {}
    }

    public static final class Settings {
        private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("sololeveling-system-ui.properties");
        private static final String[] DEFAULT_SLOTS = {"dash", "mutilation", "dagger_rush", "quicksilver", "rulers_authority", "shadow_extraction"};
        private static boolean showVitals = true;
        private static boolean showGold = true;
        private static boolean showQuestTracker = true;
        private static boolean showAbilityBar = true;
        private static boolean showAbilityNames = true;
        private static boolean showNotifications = true;
        private static boolean compactHud;
        private static boolean minimalHud;
        private static int hudScale = 100;
        private static int hudOpacity = 90;
        private static int offsetX;
        private static int offsetY;
        private static HudAnchor anchor = HudAnchor.TOP_LEFT;
        private static String lastTab = "HOME";
        private static final String[] abilitySlots = DEFAULT_SLOTS.clone();

        static { load(); }

        public static boolean showVitals() { return showVitals; }
        public static boolean showGold() { return showGold; }
        public static boolean showQuestTracker() { return showQuestTracker && !minimalHud; }
        public static boolean showAbilityBar() { return showAbilityBar; }
        public static boolean showAbilityNames() { return showAbilityNames && !minimalHud; }
        public static boolean showCooldowns() { return showAbilityBar; }
        public static boolean showNotifications() { return showNotifications; }
        public static boolean compactHud() { return compactHud; }
        public static boolean minimalHud() { return minimalHud; }
        public static int hudScale() { return hudScale; }
        public static int hudOpacity() { return hudOpacity; }
        public static int offsetX() { return offsetX; }
        public static int offsetY() { return offsetY; }
        public static HudAnchor anchor() { return anchor; }
        public static String lastTab() { return lastTab; }
        public static String abilitySlot(int index) { return index >= 0 && index < abilitySlots.length ? abilitySlots[index] : ""; }
        public static List<String> abilitySlots() { return List.of(abilitySlots.clone()); }

        public static void toggleVitals() { showVitals = !showVitals; save(); }
        public static void toggleGold() { showGold = !showGold; save(); }
        public static void toggleQuestTracker() { showQuestTracker = !showQuestTracker; save(); }
        public static void toggleAbilityBar() { showAbilityBar = !showAbilityBar; save(); }
        public static void toggleAbilityNames() { showAbilityNames = !showAbilityNames; save(); }
        public static void toggleCooldowns() { toggleAbilityBar(); }
        public static void toggleNotifications() { showNotifications = !showNotifications; save(); }
        public static void toggleCompactHud() { compactHud = !compactHud; save(); }
        public static void toggleMinimalHud() { minimalHud = !minimalHud; save(); }
        public static void cycleAnchor() { anchor = HudAnchor.values()[(anchor.ordinal() + 1) % HudAnchor.values().length]; save(); }
        public static void adjustScale(int delta) { hudScale = Mth.clamp(hudScale + delta, 60, 150); save(); }
        public static void adjustOpacity(int delta) { hudOpacity = Mth.clamp(hudOpacity + delta, 35, 100); save(); }
        public static void adjustOffset(int dx, int dy) {
            offsetX = Mth.clamp(offsetX + dx, -240, 240);
            offsetY = Mth.clamp(offsetY + dy, -160, 160);
            save();
        }
        public static void setLastTab(String value) { if (value != null && !value.isBlank()) { lastTab = value; save(); } }
        public static void assignAbility(int slot, String abilityId) {
            if (slot < 0 || slot >= abilitySlots.length) return;
            String normalized = abilityId == null ? "" : abilityId.toLowerCase(Locale.ROOT).replace(' ', '_');
            for (int i = 0; i < abilitySlots.length; i++) if (i != slot && normalized.equals(abilitySlots[i])) abilitySlots[i] = "";
            abilitySlots[slot] = normalized;
            save();
        }
        public static void clearAbility(int slot) { assignAbility(slot, ""); }
        public static void resetDefaults() {
            showVitals = true; showGold = true; showQuestTracker = true; showAbilityBar = true;
            showAbilityNames = true; showNotifications = true; compactHud = false; minimalHud = false;
            hudScale = 100; hudOpacity = 90; offsetX = 0; offsetY = 0; anchor = HudAnchor.TOP_LEFT;
            System.arraycopy(DEFAULT_SLOTS, 0, abilitySlots, 0, abilitySlots.length);
            save();
        }

        private static void load() {
            if (!Files.isRegularFile(FILE)) return;
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(FILE)) {
                properties.load(input);
                showVitals = bool(properties, "showVitals", true);
                showGold = bool(properties, "showGold", true);
                showQuestTracker = bool(properties, "showQuestTracker", true);
                showAbilityBar = bool(properties, "showAbilityBar", bool(properties, "showCooldowns", true));
                showAbilityNames = bool(properties, "showAbilityNames", true);
                showNotifications = bool(properties, "showNotifications", true);
                compactHud = bool(properties, "compactHud", false);
                minimalHud = bool(properties, "minimalHud", false);
                hudScale = integer(properties, "hudScale", 100, 60, 150);
                hudOpacity = integer(properties, "hudOpacity", 90, 35, 100);
                offsetX = integer(properties, "offsetX", 0, -240, 240);
                offsetY = integer(properties, "offsetY", 0, -160, 160);
                lastTab = properties.getProperty("lastTab", "HOME");
                try { anchor = HudAnchor.valueOf(properties.getProperty("anchor", HudAnchor.TOP_LEFT.name())); }
                catch (IllegalArgumentException ignored) { anchor = HudAnchor.TOP_LEFT; }
                for (int i = 0; i < abilitySlots.length; i++) abilitySlots[i] = properties.getProperty("abilitySlot." + i, DEFAULT_SLOTS[i]);
            } catch (IOException | RuntimeException ignored) {
                resetDefaults();
            }
        }

        private static boolean bool(Properties properties, String key, boolean fallback) {
            String value = properties.getProperty(key);
            return value == null ? fallback : Boolean.parseBoolean(value);
        }

        private static int integer(Properties properties, String key, int fallback, int minimum, int maximum) {
            try { return Mth.clamp(Integer.parseInt(properties.getProperty(key, Integer.toString(fallback))), minimum, maximum); }
            catch (NumberFormatException ignored) { return fallback; }
        }

        private static void save() {
            Properties properties = new Properties();
            properties.setProperty("showVitals", Boolean.toString(showVitals));
            properties.setProperty("showGold", Boolean.toString(showGold));
            properties.setProperty("showQuestTracker", Boolean.toString(showQuestTracker));
            properties.setProperty("showAbilityBar", Boolean.toString(showAbilityBar));
            properties.setProperty("showAbilityNames", Boolean.toString(showAbilityNames));
            properties.setProperty("showNotifications", Boolean.toString(showNotifications));
            properties.setProperty("compactHud", Boolean.toString(compactHud));
            properties.setProperty("minimalHud", Boolean.toString(minimalHud));
            properties.setProperty("hudScale", Integer.toString(hudScale));
            properties.setProperty("hudOpacity", Integer.toString(hudOpacity));
            properties.setProperty("offsetX", Integer.toString(offsetX));
            properties.setProperty("offsetY", Integer.toString(offsetY));
            properties.setProperty("anchor", anchor.name());
            properties.setProperty("lastTab", lastTab);
            for (int i = 0; i < abilitySlots.length; i++) properties.setProperty("abilitySlot." + i, abilitySlots[i]);
            try {
                Files.createDirectories(FILE.getParent());
                try (OutputStream output = Files.newOutputStream(FILE)) {
                    properties.store(output, "Solo Leveling System UI client preferences");
                }
            } catch (IOException ignored) {
                Notifications.pushFailure("Could not save HUD preferences");
            }
        }

        private Settings() {}
    }

    public static final class Theme {
        public static final int BACKDROP = 0xB0050812;
        public static final int PANEL = 0xF20A1024;
        public static final int PANEL_INNER = 0xE60D1830;
        public static final int PANEL_ALT = 0xE20A1428;
        public static final int CYAN = 0xFF35D9FF;
        public static final int CYAN_DARK = 0xFF167EAE;
        public static final int VIOLET = 0xFF7952FF;
        public static final int TEXT = 0xFFE8FAFF;
        public static final int TEXT_DIM = 0xFF8CA4B7;
        public static final int SUCCESS = 0xFF66F2B0;
        public static final int WARNING = 0xFFFFD36A;
        public static final int FAILURE = 0xFFFF6E82;

        public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
            panel(graphics, x, y, width, height, 1.0F);
        }

        public static void panel(GuiGraphics graphics, int x, int y, int width, int height, float alpha) {
            if (width <= 0 || height <= 0) return;
            graphics.fill(x, y, x + width, y + height, alpha(PANEL, alpha));
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, alpha(PANEL_INNER, alpha));
            graphics.fill(x, y, x + width, y + 1, alpha(CYAN, alpha));
            graphics.fill(x, y + height - 1, x + width, y + height, alpha(VIOLET, alpha));
            graphics.fill(x, y, x + 1, y + height, alpha(CYAN, alpha));
            graphics.fill(x + width - 1, y, x + width, y + height, alpha(VIOLET, alpha));
            corner(graphics, x, y, 1, 1, alpha);
            corner(graphics, x + width, y, -1, 1, alpha);
            corner(graphics, x, y + height, 1, -1, alpha);
            corner(graphics, x + width, y + height, -1, -1, alpha);
        }

        private static void corner(GuiGraphics graphics, int x, int y, int dx, int dy, float alpha) {
            graphics.fill(x, y, x + dx * 5, y + dy, alpha(CYAN, alpha));
            graphics.fill(x, y, x + dx, y + dy * 5, alpha(VIOLET, alpha));
        }

        public static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
            inset(graphics, x, y, width, height, 1.0F);
        }

        public static void inset(GuiGraphics graphics, int x, int y, int width, int height, float alpha) {
            graphics.fill(x, y, x + width, y + height, alpha(0xD9070D1B, alpha));
            graphics.fill(x, y, x + width, y + 1, alpha(0xAA35D9FF, alpha));
            graphics.fill(x, y + height - 1, x + width, y + height, alpha(0x887952FF, alpha));
        }

        public static void section(GuiGraphics graphics, Font font, String title, int x, int y, int width) {
            graphics.drawString(font, title.toUpperCase(Locale.ROOT), x, y, CYAN, false);
            int lineX = x + Math.min(width - 4, font.width(title) + 8);
            graphics.fill(lineX, y + 4, x + width, y + 5, 0x6635D9FF);
        }

        public static void bar(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                               float ratio, int color, String label) {
            float safe = Mth.clamp(ratio, 0.0F, 1.0F);
            graphics.fill(x, y, x + width, y + height, 0xE60B1122);
            graphics.fill(x + 1, y + 1, x + 1 + Math.round((width - 2) * safe), y + height - 1, color);
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0x44FFFFFF);
            graphics.drawString(font, ellipsize(font, label, width - 6), x + 3, y + Math.max(0, (height - 8) / 2), TEXT, false);
        }

        public static void slot(GuiGraphics graphics, int x, int y, int size, boolean selected, boolean enabled) {
            int fill = enabled ? 0xE60A1428 : 0xE6080B12;
            graphics.fill(x, y, x + size, y + size, fill);
            int border = selected ? CYAN : enabled ? 0xAA365A86 : 0xAA39465A;
            graphics.fill(x, y, x + size, y + 1, border);
            graphics.fill(x, y + size - 1, x + size, y + size, border);
            graphics.fill(x, y, x + 1, y + size, border);
            graphics.fill(x + size - 1, y, x + size, y + size, border);
        }

        public static void icon(GuiGraphics graphics, Icon icon, int x, int y, int size) {
            if (icon == null) return;
            graphics.blit(ICONS, x, y, size, size, icon.u, icon.v, 16, 16, 128, 128);
        }

        public static int alpha(int color, float multiplier) {
            int source = color >>> 24;
            int result = Mth.clamp(Math.round(source * Mth.clamp(multiplier, 0.0F, 1.0F)), 0, 255);
            return (color & 0x00FFFFFF) | (result << 24);
        }

        public static String ellipsize(Font font, String value, int width) {
            if (value == null || width <= 0) return "";
            if (font.width(value) <= width) return value;
            return font.plainSubstrByWidth(value, Math.max(0, width - font.width("…"))) + "…";
        }

        private Theme() {}
    }

    public static List<String> wrap(Font font, String text, int width, int maximumLines) {
        if (text == null || text.isBlank() || width <= 0 || maximumLines <= 0) return List.of();
        List<String> result = new ArrayList<>();
        String remaining = text.trim();
        while (!remaining.isEmpty() && result.size() < maximumLines) {
            String line = font.plainSubstrByWidth(remaining, width);
            if (line.isEmpty()) break;
            int split = line.length();
            if (split < remaining.length()) {
                int space = line.lastIndexOf(' ');
                if (space > 0) split = space;
            }
            String value = remaining.substring(0, split).trim();
            result.add(value);
            remaining = remaining.substring(Math.min(remaining.length(), split)).trim();
        }
        if (!remaining.isEmpty() && !result.isEmpty()) {
            int last = result.size() - 1;
            result.set(last, Theme.ellipsize(font, result.get(last) + "…", width));
        }
        return result;
    }

    public static String titleCase(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        String[] parts = value.replace(':', ' ').replace('_', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private SystemUi() {}
}
