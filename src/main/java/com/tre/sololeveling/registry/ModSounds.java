package com.tre.sololeveling.registry;

import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SoloLevelingMod.MODID);
    public static final RegistryObject<SoundEvent> SYSTEM = register("system_notification");
    public static final RegistryObject<SoundEvent> LEVEL_UP = register("level_up");
    public static final RegistryObject<SoundEvent> QUEST_COMPLETE = register("quest_complete");
    public static final RegistryObject<SoundEvent> ABILITY = register("ability");
    public static final RegistryObject<SoundEvent> SHADOW = register("shadow");
    public static final RegistryObject<SoundEvent> MANA_FAIL = register("mana_fail");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(SoloLevelingMod.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {}
}
