package com.tre.sololeveling.dungeon;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.quest.QuestApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Proximity discovery for authored secret and shortcut DATA markers; no world-wide scans. */
public final class DungeonDiscoveryService {
    private static final String[] MARKERS = {"secret:west_crypt", "secret:bone_chapel", "secret:warden_cache", "shortcut:catacomb_loop"};

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        DungeonSession session = DungeonRuntime.findSession(player.server, player.getUUID());
        if (session == null || player.level().dimension() != session.dungeonDimension() || !session.arenaBuilt()) return;
        for (String marker : MARKERS) {
            BlockPos relative = DungeonStructureTemplates.markerPosition(marker);
            if (relative == null) continue;
            BlockPos world = session.arenaOrigin().offset(relative);
            if (player.blockPosition().distSqr(world) > 16.0D) continue;
            String key = "dungeon_discovery_" + session.sessionId() + "_" + marker.replace(':', '_');
            if (HunterData.mutable(player).getBoolean(key)) continue;
            HunterData.mutable(player).putBoolean(key, true);
            HunterData.addGold(player, marker.startsWith("shortcut:") ? 250 : 150);
            QuestApi.onSecretDiscovered(player, marker);
            player.sendSystemMessage(Component.literal("[SYSTEM] Discovered " + marker.substring(marker.indexOf(':') + 1)
                    .replace('_', ' ') + ".").withStyle(ChatFormatting.AQUA));
            HunterData.sync(player);
        }
    }

    private DungeonDiscoveryService() {}
}
