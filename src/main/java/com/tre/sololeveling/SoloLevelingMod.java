package com.tre.sololeveling;

import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.equipment.EquipmentConfig;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.registry.ModItems;
import com.tre.sololeveling.registry.ModSounds;
import com.tre.sololeveling.integration.IntegrationBootstrap;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SoloLevelingMod.MODID)
public final class SoloLevelingMod {
    public static final String MODID = "sololeveling";

    public SoloLevelingMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfigs.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EquipmentConfig.SPEC, "sololeveling-equipment.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModConfigs.CLIENT_SPEC);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        ModNetwork.register();
        IntegrationBootstrap.install();
        MinecraftForge.EVENT_BUS.register(new com.tre.sololeveling.event.CommonEvents());
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) ModItems.EQUIPMENT.forEach(event::accept);
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) ModItems.CONSUMABLES.forEach(event::accept);
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) ModItems.MATERIALS.forEach(event::accept);
    }
}
