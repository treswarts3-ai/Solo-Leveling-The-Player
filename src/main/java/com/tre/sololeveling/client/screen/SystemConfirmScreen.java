package com.tre.sololeveling.client.screen;

import com.tre.sololeveling.client.ui.SystemUi;
import com.tre.sololeveling.client.ui.component.SystemButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/** Reusable modal confirmation screen for irreversible or expensive System actions. */
public final class SystemConfirmScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private final String detail;
    private final Runnable confirmAction;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public SystemConfirmScreen(Screen parent, String heading, String detail, Runnable confirmAction) {
        super(Component.literal(heading == null ? "Confirm" : heading));
        this.parent = parent;
        this.heading = heading == null || heading.isBlank() ? "CONFIRM ACTION" : heading;
        this.detail = detail == null ? "" : detail;
        this.confirmAction = Objects.requireNonNull(confirmAction, "confirmAction");
    }

    @Override
    protected void init() {
        panelWidth = Math.min(360, Math.max(220, width - 32));
        panelHeight = 126;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int buttonWidth = (panelWidth - 30) / 2;
        addRenderableWidget(new SystemButton(left + 10, top + panelHeight - 30, buttonWidth, 20,
                Component.literal("CONFIRM"), () -> {
                    confirmAction.run();
                    if (minecraft != null) minecraft.setScreen(parent);
                }, true));
        addRenderableWidget(new SystemButton(left + 20 + buttonWidth, top + panelHeight - 30, buttonWidth, 20,
                Component.literal("CANCEL"), this::onClose));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        SystemUi.Theme.panel(graphics, left, top, panelWidth, panelHeight);
        SystemUi.Theme.icon(graphics, SystemUi.Icon.ALERT, left + 12, top + 12, 16);
        graphics.drawString(font, heading, left + 34, top + 16, SystemUi.Theme.WARNING, false);
        List<String> lines = SystemUi.wrap(font, detail, panelWidth - 24, 3);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), left + 12, top + 43 + i * 11, SystemUi.Theme.TEXT, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
