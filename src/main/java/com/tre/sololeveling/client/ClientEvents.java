package com.tre.sololeveling.client;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.client.screen.SystemScreen;
import com.tre.sololeveling.client.ui.SystemUi;
import com.tre.sololeveling.gameplay.ability.AbilityDefinition;
import com.tre.sololeveling.gameplay.ability.AbilityMastery;
import com.tre.sololeveling.gameplay.ability.AbilityService;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final Map<String, Long> LAST_COOLDOWN = new HashMap<>();
    private static final Map<String, Long> ACTIVE_FLASH_UNTIL = new HashMap<>();

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ClientAbilityVisuals.tick();

        if (ClientKeyMappings.SYSTEM.consumeClick()) {
            if (minecraft.screen instanceof SystemScreen) minecraft.setScreen(null);
            else if (minecraft.screen == null) {
                SystemUi.Actions.send("OPEN_SYSTEM", 500L);
                minecraft.setScreen(new SystemScreen(null));
            }
        }
        if (ClientKeyMappings.SHADOWS.consumeClick()) {
            if (minecraft.screen == null || minecraft.screen instanceof SystemScreen) {
                SystemUi.Actions.send("OPEN_SYSTEM", 500L);
                minecraft.setScreen(new SystemScreen(SystemScreen.Tab.SHADOWS));
            }
        }
        if (minecraft.screen == null && minecraft.player.isAlive() && !minecraft.player.isSpectator()) {
            for (ClientKeyMappings.AbilityBinding binding : ClientKeyMappings.ABILITIES) {
                if (binding.mapping().consumeClick()) activateAbility(binding.abilityId());
            }
            if (ClientKeyMappings.HUD.consumeClick()) SystemUi.Actions.send("TOGGLE_HUD", 350L);
        }
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.options.hideGui) return;
        GuiGraphics graphics = event.getGuiGraphics();
        if (SystemUi.Settings.showNotifications()) renderNotification(graphics, minecraft);

        SystemUi.Data data = ClientHunterData.view();
        if (data.raw().getBoolean("debug_overlay")) renderDeveloperOverlay(graphics, minecraft, data);
        if (!data.awakened() || !data.hudEnabled()) return;
        float scale = SystemUi.Settings.hudScale() / 100.0F;
        float opacity = SystemUi.Settings.hudOpacity() / 100.0F;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int screenWidth = Math.max(1, Math.round(minecraft.getWindow().getGuiScaledWidth() / scale));
        int screenHeight = Math.max(1, Math.round(minecraft.getWindow().getGuiScaledHeight() / scale));

        if (SystemUi.Settings.showVitals()) renderStatusCluster(graphics, minecraft, data, screenWidth, screenHeight, opacity);
        if (SystemUi.Settings.showAbilityBar()) renderAbilityBar(graphics, minecraft, data, screenWidth, screenHeight, opacity);
        graphics.pose().popPose();
    }

    private static void renderDeveloperOverlay(GuiGraphics graphics, Minecraft minecraft, SystemUi.Data data) {
        var tag = data.raw();
        java.util.List<String> lines = java.util.List.of(
                "SL DEBUG | tick " + String.format(Locale.ROOT, "%.2f/%.2fms", tag.getDouble("debug_tick_ms"), tag.getDouble("debug_tick_rolling_ms")),
                "Player Lv" + data.level() + " " + data.rank() + " MP " + data.mana() + "/" + data.maxMana() + " G " + data.gold(),
                "Stats " + tag.getString("debug_stats"),
                "Active " + tag.getString("debug_active_abilities"),
                "Cooldowns " + tag.getString("debug_cooldowns"),
                "Dungeon " + tag.getString("debug_session_id") + " " + tag.getString("dungeon_state")
                        + " | " + tag.getString("dungeon_objective"),
                "Location " + tag.getString("debug_dungeon_location") + " | entities " + tag.getInt("debug_dungeon_entities"),
                "Shadows " + tag.getInt("debug_active_shadows") + " active / " + tag.getInt("debug_stored_shadows") + " stored",
                "Packets " + tag.getLong("debug_packets_received") + " received / " + tag.getLong("debug_packets_rejected") + " rejected",
                "Generation " + tag.getString("debug_generation"),
                "World " + tag.getString("debug_dimension") + " @ " + tag.getString("debug_position")
                        + " | nearby " + tag.getInt("debug_nearby_entities")
        );
        int width = 0;
        for (String line : lines) width = Math.max(width, minecraft.font.width(line));
        int x = Math.max(4, minecraft.getWindow().getGuiScaledWidth() - width - 12);
        int y = 6;
        graphics.fill(x - 4, y - 3, x + width + 5, y + lines.size() * 10 + 2, 0xCC030711);
        graphics.fill(x - 4, y - 3, x - 2, y + lines.size() * 10 + 2, SystemUi.Theme.CYAN);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(minecraft.font, lines.get(i), x, y + i * 10,
                    i == 0 ? SystemUi.Theme.CYAN : SystemUi.Theme.TEXT, false);
        }
    }

    private static void renderStatusCluster(GuiGraphics graphics, Minecraft minecraft, SystemUi.Data data,
                                            int screenWidth, int screenHeight, float opacity) {
        int width = SystemUi.Settings.compactHud() || SystemUi.Settings.minimalHud() ? 158 : 194;
        int vitalsHeight = SystemUi.Settings.minimalHud() ? 36 : 62;
        int questHeight = SystemUi.Settings.showQuestTracker() ? 43 : 0;
        int totalHeight = vitalsHeight + (questHeight > 0 ? questHeight + 4 : 0);
        int safeBottom = 56;
        int x = isLeft() ? 8 : screenWidth - width - 8;
        int y = isTop() ? 8 : screenHeight - totalHeight - safeBottom;
        x = Mth.clamp(x + SystemUi.Settings.offsetX(), 2, Math.max(2, screenWidth - width - 2));
        y = Mth.clamp(y + SystemUi.Settings.offsetY(), 2, Math.max(2, screenHeight - totalHeight - 2));

        SystemUi.Theme.panel(graphics, x, y, width, vitalsHeight, opacity);
        String header = "LV " + data.level() + "  •  " + data.rank();
        graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, header, width - 58), x + 7, y + 7,
                SystemUi.Theme.alpha(SystemUi.Theme.TEXT, opacity), false);
        if (SystemUi.Settings.showGold()) {
            String gold = data.gold() + "G";
            graphics.drawString(minecraft.font, gold, x + width - minecraft.font.width(gold) - 7, y + 7,
                    SystemUi.Theme.alpha(SystemUi.Theme.WARNING, opacity), false);
        }
        if (SystemUi.Settings.minimalHud()) {
            SystemUi.Theme.bar(graphics, minecraft.font, x + 7, y + 20, width - 14, 10,
                    data.mana() / (float)Math.max(1, data.maxMana()), SystemUi.Theme.VIOLET,
                    "MP " + data.mana() + " / " + data.maxMana());
        } else {
            int maxHealth = Math.max(1, Math.round(minecraft.player.getMaxHealth()));
            int currentHealth = Math.max(0, Math.round(minecraft.player.getHealth()));
            SystemUi.Theme.bar(graphics, minecraft.font, x + 7, y + 19, width - 14, 10,
                    currentHealth / (float)maxHealth, 0xFFB92D50, "HP " + currentHealth + " / " + maxHealth);
            SystemUi.Theme.bar(graphics, minecraft.font, x + 7, y + 32, width - 14, 10,
                    data.mana() / (float)Math.max(1, data.maxMana()), SystemUi.Theme.VIOLET,
                    "MP " + data.mana() + " / " + data.maxMana());
            SystemUi.Theme.bar(graphics, minecraft.font, x + 7, y + 45, width - 14, 10,
                    data.xp() / (float)Math.max(1, data.xpRequired()), SystemUi.Theme.CYAN,
                    "XP " + data.xp() + " / " + data.xpRequired());
        }

        if (questHeight > 0) {
            int questY = y + vitalsHeight + 4;
            SystemUi.Theme.inset(graphics, x, questY, width, questHeight, opacity);
            SystemUi.Theme.icon(graphics, SystemUi.Icon.QUESTS, x + 6, questY + 5, 16);
            graphics.drawString(minecraft.font, "ACTIVE QUEST", x + 27, questY + 8,
                    SystemUi.Theme.alpha(SystemUi.Theme.VIOLET, opacity), false);
            graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, data.activeQuestName(), width - 13), x + 7, questY + 23,
                    SystemUi.Theme.alpha(SystemUi.Theme.TEXT, opacity), false);
            graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, data.activeQuestProgress(), width - 13), x + 7, questY + 34,
                    SystemUi.Theme.alpha(SystemUi.Theme.TEXT_DIM, opacity), false);
        }
    }

    private static void renderAbilityBar(GuiGraphics graphics, Minecraft minecraft, SystemUi.Data data,
                                         int screenWidth, int screenHeight, float opacity) {
        int slotSize = SystemUi.Settings.compactHud() ? 26 : 32;
        int gap = 3;
        int namesHeight = SystemUi.Settings.showAbilityNames() ? 11 : 0;
        int width = SystemUi.ACTION_SLOT_COUNT * slotSize + (SystemUi.ACTION_SLOT_COUNT - 1) * gap + 8;
        int height = slotSize + namesHeight + 8;
        int x = (screenWidth - width) / 2 + SystemUi.Settings.offsetX();
        int y = screenHeight - height - 60 + SystemUi.Settings.offsetY();
        x = Mth.clamp(x, 2, Math.max(2, screenWidth - width - 2));
        y = Mth.clamp(y, 2, Math.max(2, screenHeight - height - 2));
        SystemUi.Theme.panel(graphics, x, y, width, height, opacity);

        long nowMs = System.currentTimeMillis();
        for (int slot = 0; slot < SystemUi.ACTION_SLOT_COUNT; slot++) {
            String abilityId = SystemUi.Settings.abilitySlot(slot);
            int slotX = x + 4 + slot * (slotSize + gap);
            int slotY = y + 4;
            AbilityDefinition definition = AbilityService.definition(abilityId).orElse(null);
            boolean unlocked = definition != null && isUnlocked(data, definition);
            int manaCost = definition == null ? 0 : AbilityMastery.adjustManaCost(
                    ClientHunterData.get(), abilityId, definition.manaCost());
            boolean manaReady = definition != null && data.mana() >= manaCost;
            long remaining = definition == null ? 0L : SystemUi.Cooldowns.remaining(data, abilityId);
            long previous = LAST_COOLDOWN.getOrDefault(abilityId, 0L);
            if (remaining > previous && previous <= 0L) ACTIVE_FLASH_UNTIL.put(abilityId, nowMs + 450L);
            LAST_COOLDOWN.put(abilityId, remaining);
            boolean flash = ACTIVE_FLASH_UNTIL.getOrDefault(abilityId, 0L) > nowMs;

            SystemUi.Theme.slot(graphics, slotX, slotY, slotSize, flash, unlocked && manaReady);
            if (definition != null) {
                SystemUi.Icon icon = abilityIcon(abilityId);
                int iconSize = Math.min(20, slotSize - 8);
                SystemUi.Theme.icon(graphics, icon, slotX + (slotSize - iconSize) / 2, slotY + (slotSize - iconSize) / 2, iconSize);
            }
            if (!unlocked) {
                graphics.fill(slotX + 1, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, 0xAA05070D);
                SystemUi.Theme.icon(graphics, SystemUi.Icon.LOCKED, slotX + (slotSize - 16) / 2, slotY + (slotSize - 16) / 2, 16);
            } else if (!manaReady) {
                graphics.fill(slotX + 1, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, 0x665C1027);
            }
            if (remaining > 0L) {
                float progress = SystemUi.Cooldowns.progress(data, abilityId);
                int overlayHeight = Math.round((slotSize - 2) * progress);
                graphics.fill(slotX + 1, slotY + 1, slotX + slotSize - 1, slotY + 1 + overlayHeight, 0xAA03050A);
                String timer = remaining >= 20L ? Integer.toString((int)Math.ceil(remaining / 20.0D)) : String.format(Locale.ROOT, "%.1f", remaining / 20.0D);
                graphics.drawCenteredString(minecraft.font, timer, slotX + slotSize / 2, slotY + slotSize / 2 - 4, SystemUi.Theme.TEXT);
            }
            String key = definition == null ? "-" : ClientKeyMappings.keyName(abilityId);
            graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, key, slotSize - 4), slotX + 2, slotY + 2,
                    ClientKeyMappings.hasConflict(abilityId) ? SystemUi.Theme.FAILURE : SystemUi.Theme.TEXT, false);
            if (SystemUi.Settings.showAbilityNames()) {
                String name = definition == null ? "EMPTY" : definition.displayName();
                graphics.drawCenteredString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, name, slotSize), slotX + slotSize / 2,
                        slotY + slotSize + 1, SystemUi.Theme.alpha(SystemUi.Theme.TEXT_DIM, opacity));
            }
        }
    }

    private static void renderNotification(GuiGraphics graphics, Minecraft minecraft) {
        SystemUi.Notification notification = SystemUi.Notifications.current();
        if (notification == null) return;
        long now = System.currentTimeMillis();
        long age = Math.max(0L, now - notification.createdAt());
        long remaining = Math.max(0L, notification.expiresAt() - now);
        float enter = Mth.clamp(age / 220.0F, 0.0F, 1.0F);
        float leave = Mth.clamp(remaining / 260.0F, 0.0F, 1.0F);
        float alpha = Math.min(enter, leave);
        int width = Math.min(320, Math.max(210, minecraft.font.width(notification.detail()) + 34));
        int height = notification.kind() == SystemUi.NotificationKind.LEVEL_UP || notification.kind() == SystemUi.NotificationKind.RANK_ADVANCEMENT ? 52 : 40;
        int targetX = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int x = targetX + Math.round((1.0F - enter) * 28.0F);
        int y = Math.max(10, minecraft.getWindow().getGuiScaledHeight() / 9);
        int color = notificationColor(notification.kind());
        SystemUi.Theme.panel(graphics, x, y, width, height, alpha);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 4, SystemUi.Theme.alpha(color, alpha));
        SystemUi.Theme.icon(graphics, notificationIcon(notification.kind()), x + 10, y + 11, 16);
        graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, notification.title(), width - 42), x + 32, y + 11,
                SystemUi.Theme.alpha(color, alpha), false);
        graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, notification.detail(), width - 42), x + 32, y + 24,
                SystemUi.Theme.alpha(SystemUi.Theme.TEXT, alpha), false);
        if (height > 40) {
            float pulse = 0.55F + 0.45F * (float)Math.sin(age / 110.0D);
            graphics.fill(x + 8, y + height - 8, x + 8 + Math.round((width - 16) * pulse), y + height - 6,
                    SystemUi.Theme.alpha(color, alpha));
        }
        if (SystemUi.Notifications.queued() > 1) {
            String queued = "+" + (SystemUi.Notifications.queued() - 1);
            graphics.drawString(minecraft.font, queued, x + width - minecraft.font.width(queued) - 7, y + 11,
                    SystemUi.Theme.alpha(SystemUi.Theme.TEXT_DIM, alpha), false);
        }
    }

    private static void activateAbility(String abilityId) {
        SystemUi.Data data = ClientHunterData.view();
        AbilityDefinition definition = AbilityService.definition(abilityId).orElse(null);
        if (definition == null) {
            SystemUi.Notifications.pushFailure("Unknown ability binding: " + abilityId);
            return;
        }
        if (!isUnlocked(data, definition)) {
            SystemUi.Notifications.pushFailure(definition.displayName() + " is locked: " + definition.unlock().description());
            return;
        }
        if (ClientKeyMappings.hasConflict(abilityId)) {
            SystemUi.Notifications.pushWarning("Key conflict detected for " + definition.displayName());
            return;
        }
        if (data.cooldownRemaining(abilityId) > 0L) return;
        int manaCost = AbilityMastery.adjustManaCost(ClientHunterData.get(), abilityId, definition.manaCost());
        if (data.mana() < manaCost) {
            SystemUi.Notifications.pushFailure(definition.displayName() + " requires " + manaCost + " mana");
            return;
        }
        SystemUi.Actions.send(actionForAbility(abilityId), 140L);
    }

    private static boolean isUnlocked(SystemUi.Data data, AbilityDefinition definition) {
        if (!data.awakened()) return false;
        String skill = definition.unlock().skillId();
        boolean hasSkill = !skill.isBlank() && data.skillUnlocked(skill);
        return definition.unlock().skillRequired() ? hasSkill : hasSkill || data.level() >= definition.unlock().minimumLevel();
    }

    private static String actionForAbility(String abilityId) {
        return switch (abilityId) {
            case "shadow_extraction" -> "EXTRACT";
            case "shadow_exchange" -> "SHADOW_EXCHANGE";
            case "shadow_summoning" -> "SUMMON_SHADOW";
            default -> "ABILITY:" + abilityId;
        };
    }

    private static SystemUi.Icon abilityIcon(String abilityId) {
        int index = Math.floorMod(abilityId == null ? 0 : abilityId.hashCode(), 32);
        return new SystemUi.Icon((index % 8) * 16, 32 + (index / 8) * 16);
    }

    private static int notificationColor(SystemUi.NotificationKind kind) {
        return switch (kind) {
            case ERROR, QUEST_FAILED -> SystemUi.Theme.FAILURE;
            case WARNING -> SystemUi.Theme.WARNING;
            case QUEST_COMPLETE, ITEM_OBTAINED, STAT_INCREASED -> SystemUi.Theme.SUCCESS;
            case SKILL_UNLOCK, RANK_ADVANCEMENT -> SystemUi.Theme.VIOLET;
            case LEVEL_UP, DUNGEON, QUEST_ACCEPTED, GOLD, INFO -> SystemUi.Theme.CYAN;
        };
    }

    private static SystemUi.Icon notificationIcon(SystemUi.NotificationKind kind) {
        return switch (kind) {
            case ERROR, WARNING, QUEST_FAILED -> SystemUi.Icon.ALERT;
            case QUEST_COMPLETE, ITEM_OBTAINED, STAT_INCREASED -> SystemUi.Icon.SUCCESS;
            case SKILL_UNLOCK -> SystemUi.Icon.ABILITIES;
            case DUNGEON -> SystemUi.Icon.DUNGEONS;
            case GOLD -> SystemUi.Icon.GOLD;
            case LEVEL_UP, RANK_ADVANCEMENT, QUEST_ACCEPTED, INFO -> SystemUi.Icon.PLAYER;
        };
    }

    private static boolean isLeft() {
        return SystemUi.Settings.anchor() == SystemUi.HudAnchor.TOP_LEFT || SystemUi.Settings.anchor() == SystemUi.HudAnchor.BOTTOM_LEFT;
    }

    private static boolean isTop() {
        return SystemUi.Settings.anchor() == SystemUi.HudAnchor.TOP_LEFT || SystemUi.Settings.anchor() == SystemUi.HudAnchor.TOP_RIGHT;
    }

    private ClientEvents() { }
}
