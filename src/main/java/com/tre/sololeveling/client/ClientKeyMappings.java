package com.tre.sololeveling.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final String CATEGORY = "key.categories.sololeveling";
    public static final KeyMapping SYSTEM = key("key.sololeveling.system", GLFW.GLFW_KEY_M);
    public static final KeyMapping PRIMARY = key("key.sololeveling.primary", GLFW.GLFW_KEY_R);
    public static final KeyMapping SECONDARY = key("key.sololeveling.secondary", GLFW.GLFW_KEY_V);
    public static final KeyMapping EXTRACT = key("key.sololeveling.extract", GLFW.GLFW_KEY_G);
    public static final KeyMapping SHADOWS = key("key.sololeveling.shadows", GLFW.GLFW_KEY_B);
    public static final KeyMapping EXCHANGE = key("key.sololeveling.exchange", GLFW.GLFW_KEY_X);
    public static final KeyMapping QUICKSILVER = key("key.sololeveling.quicksilver", GLFW.GLFW_KEY_Z);
    public static final KeyMapping AUTHORITY = key("key.sololeveling.authority", GLFW.GLFW_KEY_C);
    public static final KeyMapping HUD = key("key.sololeveling.hud", GLFW.GLFW_KEY_H);
    public static final KeyMapping DODGE = key("key.sololeveling.dodge", GLFW.GLFW_KEY_LEFT_ALT);

    private static KeyMapping key(String name, int code) { return new KeyMapping(name, InputConstants.Type.KEYSYM, code, CATEGORY); }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SYSTEM); event.register(PRIMARY); event.register(SECONDARY); event.register(EXTRACT);
        event.register(SHADOWS); event.register(EXCHANGE); event.register(QUICKSILVER); event.register(AUTHORITY);
        event.register(HUD); event.register(DODGE);
    }
    private ClientKeyMappings() {}
}
