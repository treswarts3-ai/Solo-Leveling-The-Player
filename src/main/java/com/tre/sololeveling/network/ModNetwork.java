package com.tre.sololeveling.network;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.network.packet.ActionPacket;
import com.tre.sololeveling.network.packet.SyncHunterDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SoloLevelingMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, SyncHunterDataPacket.class, SyncHunterDataPacket::encode, SyncHunterDataPacket::decode, SyncHunterDataPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private ModNetwork() {}
}
