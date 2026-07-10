package com.tre.sololeveling.network.packet;

import com.tre.sololeveling.client.ClientHunterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncHunterDataPacket(CompoundTag tag) {
    public static void encode(SyncHunterDataPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.tag); }
    public static SyncHunterDataPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new SyncHunterDataPacket(tag == null ? new CompoundTag() : tag);
    }
    public static void handle(SyncHunterDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHunterData.accept(packet.tag)));
        context.setPacketHandled(true);
    }
}
