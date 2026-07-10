package com.tre.sololeveling.client;

import com.tre.sololeveling.client.ui.SystemUi;
import net.minecraft.nbt.CompoundTag;

/** Thread-safe-enough client snapshot holder; all data originates from the server sync packet. */
public final class ClientHunterData {
    private static CompoundTag data = new CompoundTag();

    public static void accept(CompoundTag tag) {
        CompoundTag incoming = tag == null ? new CompoundTag() : tag.copy();
        SystemUi.Notifications.accept(data, incoming);
        data = incoming;
    }

    public static CompoundTag get() { return data; }
    public static SystemUi.Data view() { return new SystemUi.Data(data); }
    public static boolean awakened() { return data.getBoolean("awakened"); }
    private ClientHunterData() {}
}
