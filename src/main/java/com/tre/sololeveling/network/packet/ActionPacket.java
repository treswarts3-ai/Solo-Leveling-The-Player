package com.tre.sololeveling.network.packet;

import com.tre.sololeveling.gameplay.ServerActionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ActionPacket(String action) {
    public static void encode(ActionPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.action, 128); }
    public static ActionPacket decode(FriendlyByteBuf buffer) { return new ActionPacket(buffer.readUtf(128)); }
    public static void handle(ActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> ServerActionHandler.handle(player, packet.action));
        context.setPacketHandled(true);
    }
}
