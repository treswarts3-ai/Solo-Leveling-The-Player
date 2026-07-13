package com.tre.sololeveling.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final String CATEGORY = "key.categories.sololeveling";

    public static final KeyMapping SYSTEM = key("key.sololeveling.system", GLFW.GLFW_KEY_M);
    public static final KeyMapping SHADOWS = key("key.sololeveling.shadows", GLFW.GLFW_KEY_B);
    public static final KeyMapping HUD = key("key.sololeveling.hud", GLFW.GLFW_KEY_H);

    public static final KeyMapping DASH = ability("dash", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping QUICKSILVER = ability("quicksilver", GLFW.GLFW_KEY_Z);
    public static final KeyMapping SHADOW_STEP = ability("shadow_step", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping DAGGER_MASTERY = ability("dagger_mastery", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping MUTILATION = ability("mutilation", GLFW.GLFW_KEY_R);
    public static final KeyMapping DAGGER_RUSH = ability("dagger_rush", GLFW.GLFW_KEY_V);
    public static final KeyMapping BLOODLUST = ability("bloodlust", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping AREA_SLASH = ability("area_slash", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping STEALTH = ability("stealth", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY = ability("rulers_authority", GLFW.GLFW_KEY_C);
    public static final KeyMapping RULERS_AUTHORITY_PULL = ability("rulers_authority_pull", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY_PUSH = ability("rulers_authority_push", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY_HOLD = ability("rulers_authority_hold", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY_THROW = ability("rulers_authority_throw", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY_OBJECT = ability("rulers_authority_object", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY_DASH = ability("rulers_authority_dash", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping RULERS_AUTHORITY_FLIGHT = ability("rulers_authority_flight", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping ENHANCED_SENSES = ability("enhanced_senses", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping MONARCH_DOMAIN = ability("monarch_domain", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping SHADOW_EXCHANGE = ability("shadow_exchange", GLFW.GLFW_KEY_X);
    public static final KeyMapping SHADOW_EXTRACTION = ability("shadow_extraction", GLFW.GLFW_KEY_G);
    public static final KeyMapping SHADOW_SUMMONING = ability("shadow_summoning", GLFW.GLFW_KEY_UNKNOWN);
    public static final KeyMapping DRAGONS_FEAR = ability("dragons_fear", GLFW.GLFW_KEY_UNKNOWN);

    public static final List<AbilityBinding> ABILITIES = List.of(
            bind(DASH, "dash"), bind(QUICKSILVER, "quicksilver"), bind(SHADOW_STEP, "shadow_step"),
            bind(DAGGER_MASTERY, "dagger_mastery"), bind(MUTILATION, "mutilation"), bind(DAGGER_RUSH, "dagger_rush"),
            bind(BLOODLUST, "bloodlust"), bind(AREA_SLASH, "area_slash"), bind(STEALTH, "stealth"),
            bind(RULERS_AUTHORITY, "rulers_authority"), bind(RULERS_AUTHORITY_PULL, "rulers_authority_pull"),
            bind(RULERS_AUTHORITY_PUSH, "rulers_authority_push"), bind(RULERS_AUTHORITY_HOLD, "rulers_authority_hold"),
            bind(RULERS_AUTHORITY_THROW, "rulers_authority_throw"), bind(RULERS_AUTHORITY_OBJECT, "rulers_authority_object"),
            bind(RULERS_AUTHORITY_DASH, "rulers_authority_dash"),
            bind(RULERS_AUTHORITY_FLIGHT, "rulers_authority_flight"), bind(ENHANCED_SENSES, "enhanced_senses"),
            bind(MONARCH_DOMAIN, "monarch_domain"), bind(SHADOW_EXCHANGE, "shadow_exchange"),
            bind(SHADOW_EXTRACTION, "shadow_extraction"), bind(SHADOW_SUMMONING, "shadow_summoning"),
            bind(DRAGONS_FEAR, "dragons_fear")
    );
    private static final Map<String, KeyMapping> BY_ABILITY = createIndex();

    // Compatibility aliases for existing code and saved control names.
    public static final KeyMapping PRIMARY = MUTILATION;
    public static final KeyMapping SECONDARY = DAGGER_RUSH;
    public static final KeyMapping EXTRACT = SHADOW_EXTRACTION;
    public static final KeyMapping EXCHANGE = SHADOW_EXCHANGE;
    public static final KeyMapping AUTHORITY = RULERS_AUTHORITY;
    public static final KeyMapping DODGE = DASH;

    private static KeyMapping key(String name, int code) { return new KeyMapping(name, InputConstants.Type.KEYSYM, code, CATEGORY); }
    private static KeyMapping ability(String id, int code) { return key("key.sololeveling.ability." + id, code); }
    private static AbilityBinding bind(KeyMapping mapping, String abilityId) { return new AbilityBinding(mapping, abilityId); }
    private static Map<String, KeyMapping> createIndex() {
        Map<String, KeyMapping> result = new LinkedHashMap<>();
        for (AbilityBinding binding : ABILITIES) result.put(binding.abilityId, binding.mapping);
        return Map.copyOf(result);
    }

    public static KeyMapping forAbility(String id) {
        return BY_ABILITY.get(normalize(id));
    }

    public static String keyName(String id) {
        KeyMapping mapping = forAbility(id);
        if (mapping == null || mapping.isUnbound()) return "UNBOUND";
        return mapping.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
    }

    public static boolean hasConflict(String id) {
        KeyMapping mapping = forAbility(id);
        if (mapping == null || mapping.isUnbound()) return false;
        String key = mapping.getTranslatedKeyMessage().getString();
        int matches = 0;
        for (AbilityBinding binding : ABILITIES) {
            if (!binding.mapping.isUnbound() && key.equals(binding.mapping.getTranslatedKeyMessage().getString())) matches++;
        }
        return matches > 1;
    }

    public static void resetAbilityBindings() {
        for (AbilityBinding binding : ABILITIES) binding.mapping.setKey(binding.mapping.getDefaultKey());
        KeyMapping.resetMapping();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null) minecraft.options.save();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SYSTEM);
        event.register(SHADOWS);
        event.register(HUD);
        for (AbilityBinding binding : ABILITIES) event.register(binding.mapping);
    }

    public record AbilityBinding(KeyMapping mapping, String abilityId) { }
    private ClientKeyMappings() { }
}
