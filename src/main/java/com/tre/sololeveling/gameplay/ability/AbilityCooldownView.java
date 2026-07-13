package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only server cooldown snapshot for synchronization and HUD workers. */
public final class AbilityCooldownView {
    public record Entry(String id, String displayName, long remainingTicks, int totalTicks, boolean unlocked) {
        public float progress() {
            if (totalTicks <= 0 || remainingTicks <= 0) return 0.0F;
            return Math.min(1.0F, remainingTicks / (float)totalTicks);
        }

        public double remainingSeconds() {
            return remainingTicks / 20.0D;
        }
    }

    public static List<Entry> snapshot(ServerPlayer player, AbilityRegistry registry) {
        List<Entry> entries = new ArrayList<>();
        for (Ability ability : registry.all()) {
            AbilityDefinition definition = ability.definition();
            entries.add(new Entry(definition.id(), definition.displayName(),
                    HunterData.cooldownRemaining(player, definition.id()),
                    AbilityMastery.adjustCooldown(player, definition.id(), Math.max(0, ability.cooldownTicks(player))),
                    definition.unlock().isUnlocked(player)));
        }
        return Collections.unmodifiableList(entries);
    }

    private AbilityCooldownView() {
    }
}
