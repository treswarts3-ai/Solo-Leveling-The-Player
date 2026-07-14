package com.tre.sololeveling.client.screen;

import com.tre.sololeveling.client.ui.SystemUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Eight-command radial palette. Every selection is validated and executed by the server. */
public final class ShadowCommandWheelScreen extends Screen {
    private static final String[] LABELS = {
            "SUMMON", "GUARD", "ATTACK", "FORMATION", "DISMISS", "RETURN", "HOLD", "FOLLOW"
    };
    private static final String[] ACTIONS = {
            "SUMMON_SHADOW", "SHADOW_GUARD", "SHADOW_ATTACK", "SHADOW_FORMATION",
            "DISMISS_SHADOWS", "SHADOW_RETURN", "SHADOW_HOLD", "SHADOW_FOLLOW"
    };

    public ShadowCommandWheelScreen() { super(Component.literal("Shadow Command Wheel")); }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int radiusX = Math.min(150, Math.max(95, width / 5));
        int radiusY = Math.min(82, Math.max(62, height / 4));
        for (int index = 0; index < LABELS.length; index++) {
            double angle = -Math.PI / 2.0D + Math.PI * 2.0D * index / LABELS.length;
            int x = cx + (int)Math.round(Math.cos(angle) * radiusX) - 45;
            int y = cy + (int)Math.round(Math.sin(angle) * radiusY) - 10;
            String action = ACTIONS[index];
            addRenderableWidget(Button.builder(Component.literal(LABELS[index]), button -> {
                SystemUi.Actions.send(action, 200L);
                onClose();
            }).bounds(x, y, 90, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2;
        int cy = height / 2;
        graphics.fill(cx - 70, cy - 25, cx + 70, cy + 25, 0xE4080712);
        graphics.fill(cx - 70, cy - 25, cx + 70, cy - 23, 0xFF8A4FFF);
        graphics.drawCenteredString(font, "SHADOW ARMY", cx, cy - 12, 0xFFE9DDFF);
        graphics.drawCenteredString(font, "Server command", cx, cy + 3, 0xFF9C8DB7);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
