package com.tre.sololeveling.client;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.client.screen.SystemScreen;
import com.tre.sololeveling.client.ui.SystemUi;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/** Adds server-validated growth, milestone, and rank controls to the System Stats tab. */
@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProgressionSystemScreenEvents {
    private static final Map<SystemScreen, Panel> PANELS = new WeakHashMap<>();

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SystemScreen screen)) return;
        long statButtons = event.getListenersList().stream()
                .filter(listener -> listener instanceof Button button && "+".equals(button.getMessage().getString()))
                .count();
        if (statButtons < 5) {
            PANELS.remove(screen);
            return;
        }

        Layout layout = Layout.forScreen(screen.width, screen.height);
        if (layout.contentHeight < 205) return;
        int gap = 4;
        int third = Math.max(42, (layout.contentWidth - gap * 2) / 3);
        int growthY = layout.contentY + 178;
        int milestoneY = layout.contentY + 211;
        int rankY = layout.contentY + 151;

        Button vanguard = button(layout.contentX, growthY, third, "VANGUARD", "GROWTH_CHOICE:vanguard");
        Button phantom = button(layout.contentX + third + gap, growthY, third, "PHANTOM", "GROWTH_CHOICE:phantom");
        Button arcane = button(layout.contentX + (third + gap) * 2, growthY,
                layout.contentWidth - (third + gap) * 2, "ARCANE", "GROWTH_CHOICE:arcane");

        Button evolution = button(layout.contentX, milestoneY, third, "EVOLUTION", "MILESTONE_CHOICE:evolution");
        Button mastery = button(layout.contentX + third + gap, milestoneY, third, "MASTERY", "MILESTONE_CHOICE:mastery");
        Button cache = button(layout.contentX + (third + gap) * 2, milestoneY,
                layout.contentWidth - (third + gap) * 2, "CACHE", "MILESTONE_CHOICE:cache");

        Button rank = button(layout.contentX, rankY, layout.contentWidth, "BEGIN E-RANK EVALUATION", "BEGIN_RANK_TRIAL");
        event.addListener(rank);
        event.addListener(vanguard);
        event.addListener(phantom);
        event.addListener(arcane);
        event.addListener(evolution);
        event.addListener(mastery);
        event.addListener(cache);
        PANELS.put(screen, new Panel(layout, rank, vanguard, phantom, arcane, evolution, mastery, cache));
    }

    @SubscribeEvent
    public static void renderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof SystemScreen screen)) return;
        Panel panel = PANELS.get(screen);
        if (panel == null) return;
        SystemUi.Data data = ClientHunterData.view();
        int growthPending = Math.max(0, data.raw().getInt("pending_growth_choices"));
        int milestonesPending = Math.max(0, data.raw().getInt("pending_major_milestones"));
        boolean trialActive = data.raw().getBoolean("rank_trial_active");
        boolean trialEligible = data.raw().getInt("rank_progress_tier") == 0 && data.level() >= 10;

        panel.vanguard.active = growthPending > 0;
        panel.phantom.active = growthPending > 0;
        panel.arcane.active = growthPending > 0;
        panel.evolution.active = milestonesPending > 0;
        panel.mastery.active = milestonesPending > 0;
        panel.cache.active = milestonesPending > 0 && data.compounds("system_inventory").size() < 108;
        panel.rank.active = trialEligible && !trialActive;

        panel.vanguard.setMessage(Component.literal("VANGUARD " + data.raw().getInt("growth_vanguard")));
        panel.phantom.setMessage(Component.literal("PHANTOM " + data.raw().getInt("growth_phantom")));
        panel.arcane.setMessage(Component.literal("ARCANE " + data.raw().getInt("growth_arcane")));
        panel.evolution.setMessage(Component.literal("EVOLVE " + data.raw().getInt("milestone_evolution")));
        panel.mastery.setMessage(Component.literal("MASTERY " + data.raw().getInt("milestone_mastery")));
        panel.cache.setMessage(Component.literal("CACHE " + data.raw().getInt("milestone_cache")));

        if (trialActive) {
            panel.rank.setMessage(Component.literal("EVALUATION ACTIVE  " + data.raw().getInt("rank_trial_kills")
                    + " / " + Math.max(1, data.raw().getInt("rank_trial_target")) + " KILLS"));
        } else if (trialEligible) {
            panel.rank.setMessage(Component.literal("BEGIN E-RANK TO D-RANK EVALUATION"));
        } else {
            panel.rank.setMessage(Component.literal("RANK EVALUATION LOCKED"));
        }
    }

    @SubscribeEvent
    public static void renderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof SystemScreen screen)) return;
        Panel panel = PANELS.get(screen);
        if (panel == null) return;
        SystemUi.Data data = ClientHunterData.view();
        int x = panel.layout.contentX;
        int growthPending = Math.max(0, data.raw().getInt("pending_growth_choices"));
        int milestonesPending = Math.max(0, data.raw().getInt("pending_major_milestones"));
        var font = Minecraft.getInstance().font;

        event.getGuiGraphics().drawString(font,
                "RANK  " + data.rank() + "  •  Level 10 unlocks the E→D evaluation",
                x, panel.rank.getY() - 10, SystemUi.Theme.CYAN, false);
        event.getGuiGraphics().drawString(font,
                "GROWTH CHOICES: " + growthPending + "  •  Vanguard +2 STR/+1 STA  •  Phantom +2 AGI/+1 SENSE  •  Arcane +3 INT",
                x, panel.vanguard.getY() - 10, growthPending > 0 ? SystemUi.Theme.WARNING : SystemUi.Theme.TEXT_DIM, false);
        event.getGuiGraphics().drawString(font,
                "MAJOR MILESTONES: " + milestonesPending + "  •  Token  •  +5 points  •  1000G + Blessed Box",
                x, panel.evolution.getY() - 10, milestonesPending > 0 ? SystemUi.Theme.WARNING : SystemUi.Theme.TEXT_DIM, false);
    }

    private static Button button(int x, int y, int width, String label, String action) {
        return Button.builder(Component.literal(label), ignored -> ModNetwork.CHANNEL.sendToServer(new ActionPacket(action)))
                .bounds(x, y, Math.max(1, width), 18)
                .build();
    }

    private record Panel(Layout layout, Button rank, Button vanguard, Button phantom, Button arcane,
                         Button evolution, Button mastery, Button cache) { }

    private record Layout(int contentX, int contentY, int contentWidth, int contentHeight) {
        private static Layout forScreen(int width, int height) {
            int panelWidth = Math.min(Math.max(220, width - 24), Math.min(520, Math.max(220, width - 8)));
            int panelHeight = Math.min(Math.max(176, height - 24), Math.min(300, Math.max(176, height - 8)));
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;
            boolean wide = panelWidth >= 410 && panelHeight >= 224;
            int contentX = left + (wide ? 108 : 12);
            int contentY = top + (wide ? 38 : 84);
            int contentWidth = panelWidth - (wide ? 120 : 24);
            int contentHeight = panelHeight - (wide ? 50 : 96);
            return new Layout(contentX, contentY, contentWidth, contentHeight);
        }
    }

    private ProgressionSystemScreenEvents() { }
}
