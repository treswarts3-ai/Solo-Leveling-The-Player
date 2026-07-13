package com.tre.sololeveling.network.packet;

import com.tre.sololeveling.client.ClientAbilityVisuals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-issued presentation event. It carries no authority over hits, movement, mana, or cooldowns. */
public record AbilityVisualPacket(int casterId, int targetId, String abilityId, String animationId,
                                  Stage stage, int durationTicks) {
    public enum Stage { STARTUP, ACTIVE, INTERRUPTED, FAILED }

    public AbilityVisualPacket {
        abilityId = abilityId == null ? "" : abilityId;
        animationId = animationId == null ? "" : animationId;
        stage = stage == null ? Stage.FAILED : stage;
        durationTicks = Math.max(0, Math.min(20 * 60, durationTicks));
    }

    public static void encode(AbilityVisualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.casterId);
        buffer.writeVarInt(packet.targetId);
        buffer.writeUtf(packet.abilityId, 64);
        buffer.writeUtf(packet.animationId, 96);
        buffer.writeEnum(packet.stage);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static AbilityVisualPacket decode(FriendlyByteBuf buffer) {
        return new AbilityVisualPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(64),
                buffer.readUtf(96), buffer.readEnum(Stage.class), buffer.readVarInt());
    }

    public static void handle(AbilityVisualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientAbilityVisuals.accept(packet)));
        context.setPacketHandled(true);
    }
}
