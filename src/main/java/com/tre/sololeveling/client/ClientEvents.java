package com.tre.sololeveling.client;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.client.screen.SystemScreen;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (ClientKeyMappings.SYSTEM.consumeClick()) { action("OPEN_SYSTEM"); mc.setScreen(new SystemScreen(SystemScreen.Tab.STATUS)); }
        while (ClientKeyMappings.SHADOWS.consumeClick()) mc.setScreen(new SystemScreen(SystemScreen.Tab.SHADOWS));
        while (ClientKeyMappings.PRIMARY.consumeClick()) action("ABILITY:mutilation");
        while (ClientKeyMappings.SECONDARY.consumeClick()) action("ABILITY:dagger_rush");
        while (ClientKeyMappings.EXTRACT.consumeClick()) action("EXTRACT");
        while (ClientKeyMappings.EXCHANGE.consumeClick()) action("SHADOW_EXCHANGE");
        while (ClientKeyMappings.QUICKSILVER.consumeClick()) action("ABILITY:quicksilver");
        while (ClientKeyMappings.AUTHORITY.consumeClick()) action("ABILITY:rulers_authority");
        while (ClientKeyMappings.DODGE.consumeClick()) action("ABILITY:quicksilver");
        while (ClientKeyMappings.HUD.consumeClick()) action("TOGGLE_HUD");
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || mc.options.hideGui) return;
        CompoundTag t = ClientHunterData.get();
        if (!t.getBoolean("awakened") || !t.getBoolean("hud")) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = 10, y = 10, w = 158;
        fillPanel(g, x, y, w, 57);
        int level = Math.max(1, t.getInt("level"));
        int xp = Math.max(0, t.getInt("xp"));
        int xpNeed = Math.max(100, (int)Math.floor(100.0D * Math.pow(level, 1.55D)));
        int mana = Math.max(0, t.getInt("mana"));
        int maxMana = 100 + level * 2 + Math.max(1, t.getInt("intelligence")) * 8 + (t.getBoolean("black_heart") ? 1000 : 0);
        int maxHp = Math.max(1, (int)mc.player.getMaxHealth());
        drawBar(g, x + 6, y + 19, w - 12, 7, mc.player.getHealth() / maxHp, 0xFFB22646, "HP " + (int)mc.player.getHealth() + "/" + maxHp);
        drawBar(g, x + 6, y + 31, w - 12, 7, mana / (float)Math.max(1,maxMana), 0xFF4826C9, "MP " + mana + "/" + maxMana);
        drawBar(g, x + 6, y + 43, w - 12, 7, xp / (float)Math.max(1,xpNeed), 0xFF1CB8D1, "XP " + xp + "/" + xpNeed);
        String displayRank=t.getString("rank_override").isBlank()?rank(level,t.getBoolean("black_heart")):t.getString("rank_override");
        g.drawString(mc.font, "Lv " + level + "  " + displayRank + "  G " + t.getInt("gold"), x + 6, y + 6, 0xFFE4FCFF, false);
    }

    private static void fillPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x+w, y+h, 0xB6091027); g.fill(x, y, x+w, y+1, 0xFF35D9FF);
        g.fill(x, y+h-1, x+w, y+h, 0xFF7049FF); g.fill(x, y, x+1, y+h, 0xFF35D9FF); g.fill(x+w-1, y, x+w, y+h, 0xFF7049FF);
    }
    private static void drawBar(GuiGraphics g, int x, int y, int w, int h, float pct, int color, String label) {
        pct = Math.max(0, Math.min(1, pct)); g.fill(x,y,x+w,y+h,0xFF10172D); g.fill(x+1,y+1,x+1+(int)((w-2)*pct),y+h-1,color);
        g.drawString(Minecraft.getInstance().font,label,x+3,y-1,0xFFFFFFFF,false);
    }
    private static String rank(int level, boolean heart) { if(level>=100&&heart)return "Shadow Monarch"; if(level>=80)return "National"; if(level>=60)return "S"; if(level>=40)return "A"; if(level>=30)return "B"; if(level>=20)return "C"; if(level>=10)return "D"; return "E"; }
    private static void action(String action) { ModNetwork.CHANNEL.sendToServer(new ActionPacket(action)); }
    private ClientEvents() {}
}
