package com.tre.sololeveling.client.ui.component;

import com.tre.sololeveling.client.ui.SystemUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;

/** A reusable System-styled button with a single press callback. */
public class SystemButton extends AbstractButton {
    private final Runnable action;
    private final boolean accent;
    private String disabledReason = "";

    public SystemButton(int x, int y, int width, int height, Component message, Runnable action) {
        this(x, y, width, height, message, action, false);
    }

    public SystemButton(int x, int y, int width, int height, Component message, Runnable action, boolean accent) {
        super(x, y, width, height, message);
        this.action = Objects.requireNonNull(action, "action");
        this.accent = accent;
    }

    public SystemButton disabledReason(String reason) {
        this.disabledReason = reason == null ? "" : reason;
        return this;
    }

    public String disabledReason() { return disabledReason; }

    @Override
    public void onPress() {
        if (active) action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float alpha = Mth.clamp(this.alpha, 0.0F, 1.0F);
        boolean hovered = isHoveredOrFocused();
        int base = !active ? 0xB4080B13 : hovered ? 0xED173455 : accent ? 0xE8183352 : 0xE80A1830;
        int border = !active ? 0xAA39465A : hovered ? SystemUi.Theme.CYAN : accent ? SystemUi.Theme.VIOLET : 0xFF27759A;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, SystemUi.Theme.alpha(base, alpha));
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, SystemUi.Theme.alpha(border, alpha));
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, SystemUi.Theme.alpha(border, alpha));
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, SystemUi.Theme.alpha(border, alpha));
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, SystemUi.Theme.alpha(border, alpha));
        if (hovered && active) graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + 3, 0x5535D9FF);
        int color = active ? SystemUi.Theme.TEXT : SystemUi.Theme.TEXT_DIM;
        String label = SystemUi.Theme.ellipsize(minecraft.font, getMessage().getString(), width - 8);
        graphics.drawCenteredString(minecraft.font, label, getX() + width / 2,
                getY() + Math.max(2, (height - 8) / 2), SystemUi.Theme.alpha(color, alpha));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
