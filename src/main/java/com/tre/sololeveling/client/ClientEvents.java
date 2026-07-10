package com.tre.sololeveling.client;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.client.screen.SystemScreen;
import com.tre.sololeveling.client.ui.SystemUi;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final String[] HUD_SKILLS = {"mutilation", "dagger_rush", "quicksilver", "rulers_authority"};

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        while (ClientKeyMappings.SYSTEM.consumeClick()) {
            action("OPEN_SYSTEM");
            minecraft.setScreen(new SystemScreen(SystemScreen.Tab.HOME));
        }
        while (ClientKeyMappings.SHADOWS.consumeClick()) minecraft.setScreen(new SystemScreen(SystemScreen.Tab.SHADOWS));
        for (ClientKeyMappings.AbilityBinding binding : ClientKeyMappings.ABILITIES) {
            while (binding.mapping().consumeClick()) actionForAbility(binding.abilityId());
        }
        while (ClientKeyMappings.HUD.consumeClick()) action("TOGGLE_HUD");
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.options.hideGui) return;
        SystemUi.Data data = ClientHunterData.view();
        if (!data.awakened()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        if (SystemUi.Settings.showNotifications()) renderNotifications(graphics, minecraft);
        if (!data.hudEnabled()) return;

        float scale = SystemUi.Settings.hudScale() / 100.0F;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int screenWidth = Math.max(1, Math.round(minecraft.getWindow().getGuiScaledWidth() / scale));
        int screenHeight = Math.max(1, Math.round(minecraft.getWindow().getGuiScaledHeight() / scale));
        int width = SystemUi.Settings.compactHud() ? 154 : 184;
        int baseHeight = SystemUi.Settings.compactHud() ? 52 : 60;
        int questHeight = SystemUi.Settings.showQuestTracker() ? 40 : 0;
        int cooldownCount = SystemUi.Settings.showCooldowns() ? unlockedHudSkills(data) : 0;
        int cooldownHeight = cooldownCount * 20;
        int totalHeight = baseHeight + questHeight + cooldownHeight + (questHeight > 0 ? 4 : 0) + (cooldownHeight > 0 ? 4 : 0);
        int x = leftAnchor() ? 8 : screenWidth - width - 8;
        int y = topAnchor() ? 8 : screenHeight - totalHeight - 8;

        renderVitals(graphics, minecraft, data, x, y, width, baseHeight);
        int nextY = y + baseHeight + 4;
        if (SystemUi.Settings.showQuestTracker()) {
            renderQuestTracker(graphics, minecraft, data, x, nextY, width);
            nextY += questHeight + 4;
        }
        if (SystemUi.Settings.showCooldowns()) {
            for (String skill : HUD_SKILLS) {
                if (!data.skillUnlocked(skill)) continue;
                SystemUi.Cooldowns.renderIndicator(graphics, minecraft.font, x, nextY, width, skill, SystemUi.titleCase(skill), data);
                nextY += 20;
            }
        }
        graphics.pose().popPose();
    }

    private static void renderVitals(GuiGraphics graphics, Minecraft minecraft, SystemUi.Data data,
                                     int x, int y, int width, int height) {
        SystemUi.Theme.panel(graphics, x, y, width, height);
        String header = "Lv " + data.level() + "  " + data.rank();
        graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, header, width - 12), x + 6, y + 6, SystemUi.Theme.TEXT, false);
        String gold = data.gold() + "G";
        graphics.drawString(minecraft.font, gold, x + width - minecraft.font.width(gold) - 6, y + 6, SystemUi.Theme.WARNING, false);
        int maxHealth = Math.max(1, Math.round(minecraft.player.getMaxHealth()));
        int currentHealth = Math.max(0, Math.round(minecraft.player.getHealth()));
        SystemUi.Theme.bar(graphics, minecraft.font, x + 6, y + 18, width - 12, 9,
                currentHealth / (float)maxHealth, 0xFFB92D50, "HP " + currentHealth + " / " + maxHealth);
        SystemUi.Theme.bar(graphics, minecraft.font, x + 6, y + 30, width - 12, 9,
                data.mana() / (float)Math.max(1, data.maxMana()), SystemUi.Theme.VIOLET, "MP " + data.mana() + " / " + data.maxMana());
        SystemUi.Theme.bar(graphics, minecraft.font, x + 6, y + 42, width - 12, 9,
                data.xp() / (float)Math.max(1, data.xpRequired()), SystemUi.Theme.CYAN, "XP " + data.xp() + " / " + data.xpRequired());
    }

    private static void renderQuestTracker(GuiGraphics graphics, Minecraft minecraft, SystemUi.Data data,
                                           int x, int y, int width) {
        SystemUi.Theme.inset(graphics, x, y, width, 40);
        graphics.drawString(minecraft.font, "ACTIVE QUEST", x + 6, y + 5, SystemUi.Theme.VIOLET, false);
        graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, data.activeQuestName(), width - 12), x + 6, y + 17, SystemUi.Theme.TEXT, false);
        graphics.drawString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, data.activeQuestProgress(), width - 12), x + 6, y + 28, SystemUi.Theme.TEXT_DIM, false);
    }

    private static void renderNotifications(GuiGraphics graphics, Minecraft minecraft) {
        List<SystemUi.Notification> notifications = SystemUi.Notifications.active();
        if (notifications.isEmpty()) return;
        int start = Math.max(0, notifications.size() - 3);
        int y = 10;
        for (int i = start; i < notifications.size(); i++) {
            SystemUi.Notification notification = notifications.get(i);
            if (notification.kind() == SystemUi.NotificationKind.LEVEL_UP) {
                renderLevelUp(graphics, minecraft, notification);
                continue;
            }
            int width = Math.min(270, Math.max(180, minecraft.font.width(notification.detail()) + 24));
            int x = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
            int color = notificationColor(notification.kind());
            SystemUi.Theme.panel(graphics, x, y, width, 34);
            graphics.fill(x + 2, y + 2, x + width - 2, y + 4, color);
            graphics.drawCenteredString(minecraft.font, notification.title(), x + width / 2, y + 8, color);
            graphics.drawCenteredString(minecraft.font, SystemUi.Theme.ellipsize(minecraft.font, notification.detail(), width - 16), x + width / 2, y + 20, SystemUi.Theme.TEXT);
            y += 38;
        }
    }

    private static void renderLevelUp(GuiGraphics graphics, Minecraft minecraft, SystemUi.Notification notification) {
        long age = Math.max(0L, System.currentTimeMillis() - notification.createdAt());
        float pulse = 0.75F + 0.25F * (float)Math.sin(age / 120.0D);
        int width = 230;
        int height = 48;
        int x = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() / 3 - height / 2;
        SystemUi.Theme.panel(graphics, x, y, width, height);
        int glow = Mth.clamp(Math.round(120 + 100 * pulse), 0, 255);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 5, (glow << 24) | 0x0035D9FF);
        graphics.drawCenteredString(minecraft.font, notification.title(), x + width / 2, y + 11, SystemUi.Theme.CYAN);
        graphics.drawCenteredString(minecraft.font, notification.detail(), x + width / 2, y + 27, SystemUi.Theme.TEXT);
    }

    private static int notificationColor(SystemUi.NotificationKind kind) {
        return switch (kind) {
            case FAILURE -> SystemUi.Theme.FAILURE;
            case QUEST_COMPLETE -> SystemUi.Theme.SUCCESS;
            case SKILL_UNLOCK -> SystemUi.Theme.VIOLET;
            case LEVEL_UP, INFO -> SystemUi.Theme.CYAN;
        };
    }

    private static int unlockedHudSkills(SystemUi.Data data) {
        int count = 0;
        for (String skill : HUD_SKILLS) if (data.skillUnlocked(skill)) count++;
        return count;
    }

    private static boolean leftAnchor() {
        return SystemUi.Settings.anchor() == SystemUi.HudAnchor.TOP_LEFT || SystemUi.Settings.anchor() == SystemUi.HudAnchor.BOTTOM_LEFT;
    }

    private static boolean topAnchor() {
        return SystemUi.Settings.anchor() == SystemUi.HudAnchor.TOP_LEFT || SystemUi.Settings.anchor() == SystemUi.HudAnchor.TOP_RIGHT;
    }

    private static void actionForAbility(String abilityId) {
        switch (abilityId) {
            case "shadow_extraction" -> action("EXTRACT");
            case "shadow_exchange" -> action("SHADOW_EXCHANGE");
            case "shadow_summoning" -> action("SUMMON_SHADOW");
            default -> action("ABILITY:" + abilityId);
        }
    }

    private static void action(String action) {
        ModNetwork.CHANNEL.sendToServer(new ActionPacket(action));
    }

    private ClientEvents() {}
}
