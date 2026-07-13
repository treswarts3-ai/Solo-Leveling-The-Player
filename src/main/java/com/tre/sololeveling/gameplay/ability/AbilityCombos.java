package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.server.level.ServerPlayer;

/** Small server-owned combo window; clients cannot choose or forge the previous ability. */
public final class AbilityCombos {
    public static void record(ServerPlayer player, String abilityId) {
        HunterData.mutable(player).putString("ability_combo_last", AbilityDefinition.normalize(abilityId));
        HunterData.mutable(player).putLong("ability_combo_tick", player.level().getGameTime());
    }

    public static boolean follows(ServerPlayer player, String abilityId, long maximumAgeTicks) {
        String expected = AbilityDefinition.normalize(abilityId);
        long age = player.level().getGameTime() - HunterData.mutable(player).getLong("ability_combo_tick");
        return expected.equals(HunterData.mutable(player).getString("ability_combo_last"))
                && age >= 0L && age <= Math.max(0L, maximumAgeTicks);
    }

    public static void clear(ServerPlayer player) {
        HunterData.mutable(player).putString("ability_combo_last", "");
        HunterData.mutable(player).putLong("ability_combo_tick", 0L);
    }

    private AbilityCombos() {}
}
