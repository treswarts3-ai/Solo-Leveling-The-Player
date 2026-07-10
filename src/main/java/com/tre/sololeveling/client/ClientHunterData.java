package com.tre.sololeveling.client;

import net.minecraft.nbt.CompoundTag;

public final class ClientHunterData {
    private static CompoundTag data = new CompoundTag();
    public static void accept(CompoundTag tag) { data = tag.copy(); }
    public static CompoundTag get() { return data; }
    public static boolean awakened() { return data.getBoolean("awakened"); }
    private ClientHunterData() {}
}
