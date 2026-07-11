package com.tre.sololeveling.client.ui.component;

import com.tre.sololeveling.client.ui.SystemUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Navigation button with a persistent selected state and icon. */
public final class SystemTabButton extends SystemButton {
    private final SystemUi.Icon icon;
    private boolean selected;

    public SystemTabButton(int x, int y, int width, int height, Component message, SystemUi.Icon icon, Runnable action) {
        super(x, y, width, height, message, action);
        this.icon = icon;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.active = !selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!selected) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        } else {
            int x = getX();
            int y = getY();
            graphics.fill(x, y, x + width, y + height, 0xF0152C4B);
            graphics.fill(x, y, x + 3, y + height, SystemUi.Theme.CYAN);
            graphics.fill(x + 3, y, x + width, y + 1, 0xAA7952FF);
            String label = SystemUi.Theme.ellipsize(Minecraft.getInstance().font, getMessage().getString(), width - 29);
            graphics.drawString(Minecraft.getInstance().font, label, x + 24, y + Math.max(2, (height - 8) / 2), SystemUi.Theme.TEXT, false);
        }
        if (icon != null) SystemUi.Theme.icon(graphics, icon, getX() + 5, getY() + Math.max(1, (height - 16) / 2), 16);
    }
}
