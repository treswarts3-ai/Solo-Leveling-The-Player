package com.tre.sololeveling.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfigs {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue MAX_HUNTER_LEVEL;
    public static final ForgeConfigSpec.BooleanValue PRESERVE_ON_DEATH;
    public static final ForgeConfigSpec.IntValue ACTIVE_SHADOW_MAX;
    public static final ForgeConfigSpec.BooleanValue PVP_ABILITIES;
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUD_SCALE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE;

    static {
        ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
        server.push("progression");
        XP_MULTIPLIER = server.comment("Multiplier applied to hunter XP rewards.").defineInRange("xpMultiplier", 1.0D, 0.0D, 100.0D);
        MAX_HUNTER_LEVEL = server.defineInRange("maximumHunterLevel", 100, 1, 10000);
        PRESERVE_ON_DEATH = server.define("preserveProgressionOnDeath", true);
        server.pop().push("combat");
        PVP_ABILITIES = server.define("pvpAbilityEffects", false);
        ACTIVE_SHADOW_MAX = server.defineInRange("activeShadowMaximum", 12, 0, 50);
        server.pop();
        SERVER_SPEC = server.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("interface");
        HUD_ENABLED = client.define("hudEnabled", true);
        HUD_SCALE = client.defineInRange("hudScale", 1.0D, 0.5D, 2.0D);
        PARTICLE_DENSITY = client.defineInRange("particleDensity", 1.0D, 0.0D, 1.0D);
        CAMERA_SHAKE = client.define("cameraShake", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }
    private ModConfigs() {}
}
