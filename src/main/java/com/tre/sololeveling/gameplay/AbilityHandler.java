package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.gameplay.ability.AbilityCooldownView;
import com.tre.sololeveling.gameplay.ability.AbilityDefinition;
import com.tre.sololeveling.gameplay.ability.AbilityService;
import com.tre.sololeveling.gameplay.ability.AbilityTargeting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Compatibility facade retained for packets, client keybinds, events, and the
 * shadow/UI workstreams. All success decisions are made by AbilityService on
 * the logical server.
 */
public final class AbilityHandler {
    public static void activate(ServerPlayer player, String rawSkill) {
        AbilityService.activate(player, rawSkill);
    }

    public static boolean activateForTest(ServerPlayer player, String rawSkill) {
        return AbilityService.activate(player, rawSkill);
    }

    public static void activateAuthority(ServerPlayer player, String rawMode) {
        String mode = rawMode == null ? "" : rawMode.toLowerCase(Locale.ROOT).trim();
        String id = mode.isBlank() ? "rulers_authority" : "rulers_authority_" + mode;
        AbilityService.activate(player, id);
    }

    public static void tick(ServerPlayer player) {
        AbilityService.tick(player);
    }

    public static void cancel(ServerPlayer player) {
        AbilityService.cancel(player);
    }

    public static LivingEntity findLivingTarget(ServerPlayer player, double range) {
        return AbilityTargeting.livingRayTarget(player, range);
    }

    public static Entity findTarget(ServerPlayer player, double range) {
        return AbilityTargeting.rayTarget(player, range);
    }

    public static Collection<String> abilityIds() {
        return AbilityService.ids();
    }

    public static Optional<AbilityDefinition> definition(String id) {
        return AbilityService.definition(id);
    }

    public static List<AbilityCooldownView.Entry> cooldownData(ServerPlayer player) {
        return AbilityService.cooldowns(player);
    }

    private AbilityHandler() {
    }
}
