package com.tre.sololeveling.client.ui;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

/** Client-only presentation helpers. No value in this class is authoritative gameplay state. */
public final class SystemUi {
    public enum NotificationKind { INFO, FAILURE, LEVEL_UP, QUEST_COMPLETE, SKILL_UNLOCK }
    public enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

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
        public boolean skillUnlocked(String skill) { return tag.getBoolean("skill_" + skill); }
        public boolean dailyComplete() { return tag.getBoolean("daily_complete"); }
        public boolean dailyClaimed() { return tag.getBoolean("daily_claimed"); }
        public ListTag compounds(String key) { return tag.getList(key, Tag.TAG_COMPOUND); }
        public ListTag strings(String key) { return tag.getList(key, Tag.TAG_STRING); }
        public String text(String key, String fallback) {
            String value = tag.getString(key);
            return value == null || value.isBlank() ? fallback : value;
        }

        public int xpRequired() {
            if (tag.contains("xp_required", Tag.TAG_INT)) return Math.max(1, tag.getInt("xp_required"));
            // TEMPORARY_BACKEND_FALLBACK: replace when the sync snapshot exposes xp_required.
            return Math.max(100, (int)Math.floor(100.0D * Math.pow(level(), 1.55D)));
        }

        public int maxMana() {
            if (tag.contains("max_mana", Tag.TAG_INT)) return Math.max(1, tag.getInt("max_mana"));
            // TEMPORARY_BACKEND_FALLBACK: equipment bonuses require a server-provided max_mana field.
            return Math.max(1, 100 + level() * 2 + stat("intelligence") * 8 + (tag.getBoolean("black_heart") ? 1000 : 0));
        }

        public int shadowCapacity() {
            if (tag.contains("shadow_capacity", Tag.TAG_INT)) return Math.max(0, tag.getInt("shadow_capacity"));
            // TEMPORARY_BACKEND_FALLBACK: replace when shadow_capacity is included in synchronization.
            return Math.min(100, 3 + stat("intelligence") / 5 + level() / 10
                    + Math.max(0, tag.getInt("shadow_capacity_bonus")) + (tag.getBoolean("black_heart") ? 20 : 0));
        }

        public String rank() {
            String synced = tag.getString("rank");
            if (!synced.isBlank()) return synced;
            String override = tag.getString("rank_override");
            if (!override.isBlank()) return override;
            // TEMPORARY_BACKEND_FALLBACK: replace when rank is explicitly synchronized.
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

        public String activeQuestName() {
            return titleCase(text("active_main_quest", "No active quest"));
        }

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
        private final long createdAt;
        private final long expiresAt;

        private Notification(NotificationKind kind, String title, String detail, long durationMs) {
            this.kind = kind;
            this.title = title;
            this.detail = detail;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = createdAt + Math.max(1200L, durationMs);
        }

        public NotificationKind kind() { return kind; }
        public String title() { return title; }
        public String detail() { return detail; }
        public long createdAt() { return createdAt; }
        public long expiresAt() { return expiresAt; }
    }

    public static final class Notifications {
        private static final Deque<Notification> ACTIVE = new ArrayDeque<>();
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
            if (newLevel > oldLevel) push(NotificationKind.LEVEL_UP, "LEVEL UP", "Level " + oldLevel + "  →  " + newLevel, 5200L);

            if (!oldTag.getBoolean("daily_complete") && newTag.getBoolean("daily_complete")) {
                push(NotificationKind.QUEST_COMPLETE, "QUEST COMPLETE", "Daily training objectives completed", 4800L);
            }
            int oldCompleted = oldTag.getList("completed_quests", Tag.TAG_STRING).size();
            int newCompleted = newTag.getList("completed_quests", Tag.TAG_STRING).size();
            if (newCompleted > oldCompleted) {
                push(NotificationKind.QUEST_COMPLETE, "QUEST COMPLETE", "A quest was added to your completed log", 4800L);
            }
            for (String skill : HunterData.SKILLS) {
                if (!oldTag.getBoolean("skill_" + skill) && newTag.getBoolean("skill_" + skill)) {
                    push(NotificationKind.SKILL_UNLOCK, "SKILL UNLOCKED", titleCase(skill), 4600L);
                }
            }
            acceptBackendNotifications(newTag);
        }

        public static synchronized void pushInfo(String title, String detail) {
            push(NotificationKind.INFO, title, detail, 3600L);
        }

        public static synchronized void pushFailure(String detail) {
            push(NotificationKind.FAILURE, "SYSTEM NOTICE", detail, 3800L);
        }

        public static synchronized List<Notification> active() {
            prune();
            return Collections.unmodifiableList(new ArrayList<>(ACTIVE));
        }

        private static void acceptBackendNotifications(CompoundTag tag) {
            int count = backendNotificationCount(tag);
            if (count <= backendNotificationCount) {
                backendNotificationCount = count;
                return;
            }
            ListTag strings = tag.getList("notifications", Tag.TAG_STRING);
            for (int i = Math.max(backendNotificationCount, 0); i < strings.size(); i++) {
                push(NotificationKind.INFO, "SYSTEM", strings.getString(i), 4200L);
            }
            ListTag compounds = tag.getList("notifications", Tag.TAG_COMPOUND);
            for (int i = Math.max(backendNotificationCount, 0); i < compounds.size(); i++) {
                CompoundTag entry = compounds.getCompound(i);
                push(NotificationKind.INFO, entry.getString("title").isBlank() ? "SYSTEM" : entry.getString("title"),
                        entry.getString("message"), 4200L);
            }
            backendNotificationCount = count;
        }

        private static int backendNotificationCount(CompoundTag tag) {
            Tag value = tag.get("notifications");
            return value instanceof ListTag list ? list.size() : 0;
        }

        private static void push(NotificationKind kind, String title, String detail, long durationMs) {
            prune();
            String cleanTitle = title == null || title.isBlank() ? "SYSTEM" : title;
            String cleanDetail = detail == null ? "" : detail;
            Notification newest = ACTIVE.peekLast();
            if (newest != null && newest.kind == kind && newest.title.equals(cleanTitle) && newest.detail.equals(cleanDetail)) return;
            ACTIVE.addLast(new Notification(kind, cleanTitle, cleanDetail, durationMs));
            while (ACTIVE.size() > 8) ACTIVE.removeFirst();
        }

        private static void prune() {
            long now = System.currentTimeMillis();
            while (!ACTIVE.isEmpty() && ACTIVE.peekFirst().expiresAt <= now) ACTIVE.removeFirst();
        }

        private Notifications() {}
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

        public static void renderIndicator(GuiGraphics graphics, Font font, int x, int y, int width,
                                           String skill, String label, Data data) {
            long remaining = remaining(data, skill);
            float progress = progress(data, skill);
            Theme.inset(graphics, x, y, width, 18);
            int color = remaining > 0L ? Theme.VIOLET : Theme.CYAN;
            graphics.fill(x + 1, y + 15, x + 1 + Math.round((width - 2) * (1.0F - progress)), y + 17, color);
            graphics.drawString(font, Theme.ellipsize(font, label, width - 45), x + 4, y + 5,
                    remaining > 0L ? Theme.TEXT_DIM : Theme.TEXT, false);
            String timer = remaining > 0L ? remainingText(data, skill) : "READY";
            graphics.drawString(font, timer, x + width - font.width(timer) - 4, y + 5,
                    remaining > 0L ? Theme.WARNING : Theme.SUCCESS, false);
        }

        private Cooldowns() {}
    }

    public static final class Settings {
        private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("sololeveling-system-ui.properties");
        private static boolean showQuestTracker = true;
        private static boolean showCooldowns = true;
        private static boolean showNotifications = true;
        private static boolean compactHud;
        private static int hudScale = 100;
        private static HudAnchor anchor = HudAnchor.TOP_LEFT;

        static { load(); }

        public static boolean showQuestTracker() { return showQuestTracker; }
        public static boolean showCooldowns() { return showCooldowns; }
        public static boolean showNotifications() { return showNotifications; }
        public static boolean compactHud() { return compactHud; }
        public static int hudScale() { return hudScale; }
        public static HudAnchor anchor() { return anchor; }
        public static void toggleQuestTracker() { showQuestTracker = !showQuestTracker; save(); }
        public static void toggleCooldowns() { showCooldowns = !showCooldowns; save(); }
        public static void toggleNotifications() { showNotifications = !showNotifications; save(); }
        public static void toggleCompactHud() { compactHud = !compactHud; save(); }
        public static void cycleAnchor() { anchor = HudAnchor.values()[(anchor.ordinal() + 1) % HudAnchor.values().length]; save(); }
        public static void adjustScale(int delta) { hudScale = Mth.clamp(hudScale + delta, 75, 125); save(); }

        private static void load() {
            if (!Files.isRegularFile(FILE)) return;
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(FILE)) {
                properties.load(input);
                showQuestTracker = Boolean.parseBoolean(properties.getProperty("showQuestTracker", "true"));
                showCooldowns = Boolean.parseBoolean(properties.getProperty("showCooldowns", "true"));
                showNotifications = Boolean.parseBoolean(properties.getProperty("showNotifications", "true"));
                compactHud = Boolean.parseBoolean(properties.getProperty("compactHud", "false"));
                hudScale = Mth.clamp(Integer.parseInt(properties.getProperty("hudScale", "100")), 75, 125);
                try { anchor = HudAnchor.valueOf(properties.getProperty("anchor", HudAnchor.TOP_LEFT.name())); }
                catch (IllegalArgumentException ignored) { anchor = HudAnchor.TOP_LEFT; }
            } catch (IOException | NumberFormatException ignored) {
                // Invalid client preferences fall back to safe defaults.
            }
        }

        private static void save() {
            Properties properties = new Properties();
            properties.setProperty("showQuestTracker", Boolean.toString(showQuestTracker));
            properties.setProperty("showCooldowns", Boolean.toString(showCooldowns));
            properties.setProperty("showNotifications", Boolean.toString(showNotifications));
            properties.setProperty("compactHud", Boolean.toString(compactHud));
            properties.setProperty("hudScale", Integer.toString(hudScale));
            properties.setProperty("anchor", anchor.name());
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
        public static final int PANEL = 0xE60A1024;
        public static final int PANEL_INNER = 0xD90D1830;
        public static final int CYAN = 0xFF35D9FF;
        public static final int VIOLET = 0xFF7952FF;
        public static final int TEXT = 0xFFE8FAFF;
        public static final int TEXT_DIM = 0xFF8CA4B7;
        public static final int SUCCESS = 0xFF66F2B0;
        public static final int WARNING = 0xFFFFD36A;
        public static final int FAILURE = 0xFFFF6E82;

        public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
            graphics.fill(x, y, x + width, y + height, PANEL);
            graphics.fill(x, y, x + width, y + 2, CYAN);
            graphics.fill(x, y + height - 2, x + width, y + height, VIOLET);
            graphics.fill(x, y, x + 2, y + height, CYAN);
            graphics.fill(x + width - 2, y, x + width, y + height, VIOLET);
            graphics.fill(x + 4, y + 4, x + width - 4, y + 5, 0x5535D9FF);
        }

        public static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
            graphics.fill(x, y, x + width, y + height, 0xB8070D1B);
            graphics.fill(x, y, x + width, y + 1, 0x8835D9FF);
            graphics.fill(x, y + height - 1, x + width, y + height, 0x667952FF);
        }

        public static void bar(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                               float ratio, int color, String label) {
            float safe = Mth.clamp(ratio, 0.0F, 1.0F);
            graphics.fill(x, y, x + width, y + height, 0xFF0B1122);
            graphics.fill(x + 1, y + 1, x + 1 + Math.round((width - 2) * safe), y + height - 1, color);
            graphics.drawString(font, ellipsize(font, label, width - 6), x + 3, y + Math.max(0, (height - 8) / 2), TEXT, false);
        }

        public static String ellipsize(Font font, String value, int width) {
            if (value == null) return "";
            if (font.width(value) <= width) return value;
            return font.plainSubstrByWidth(value, Math.max(0, width - font.width("…"))) + "…";
        }

        private Theme() {}
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
