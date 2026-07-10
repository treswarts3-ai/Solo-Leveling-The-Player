package com.tre.sololeveling.event;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.command.GodPowersCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID)
public final class GodPowersCommandEvents {
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        GodPowersCommand.register(event.getDispatcher());
    }

    private GodPowersCommandEvents() {
    }
}
