package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Central server authority for validation, mana, cooldowns, execution, and feedback. */
public final class AbilityService {
    private static final AbilityRegistry REGISTRY = createRegistry();

    private static AbilityRegistry createRegistry() {
        AbilityRegistry registry = new AbilityRegistry();
        StandardAbilities.registerAll(registry);
        return registry;
    }

    public static boolean activate(ServerPlayer player, String rawId) {
        Optional<Ability> resolved = REGISTRY.resolve(rawId);
        if (resolved.isEmpty()) {
            failure(player, "Unknown ability: " + AbilityDefinition.normalize(rawId));
            return false;
        }
        Ability ability = resolved.get();
        AbilityDefinition definition = ability.definition();

        if (!validPlayerState(player)) {
            failure(player, "Abilities cannot be used in the current player state.");
            return false;
        }
        if (!definition.unlock().isUnlocked(player)) {
            failure(player, "Ability locked: " + definition.displayName() + " (" + definition.unlock().description() + ")");
            return false;
        }
        if (!HunterData.cooldownReady(player, definition.id())) {
            double seconds = HunterData.cooldownRemaining(player, definition.id()) / 20.0D;
            failure(player, definition.displayName() + " is on cooldown for " + String.format(java.util.Locale.ROOT, "%.1f", seconds) + "s.");
            return false;
        }

        int manaCost = Math.max(0, ability.manaCost(player));
        if (HunterData.getMana(player) < manaCost || !HunterData.spendMana(player, manaCost)) {
            HunterData.sync(player);
            return false;
        }

        AbilityResult result;
        try {
            result = ability.activate(new AbilityContext(player, definition));
        } catch (RuntimeException exception) {
            if (manaCost > 0) HunterData.addMana(player, manaCost);
            failure(player, definition.displayName() + " failed safely.");
            HunterData.sync(player);
            return false;
        }

        if (result == null || !result.success()) {
            if (manaCost > 0) HunterData.addMana(player, manaCost);
            failure(player, result == null || result.feedback().isBlank() ? "No valid target or requirement was met." : result.feedback());
            HunterData.sync(player);
            return false;
        }

        int cooldown = Math.max(0, ability.cooldownTicks(player));
        HunterData.setCooldown(player, definition.id(), cooldown);
        AbilityEffects.activationBurst(player);
        if (!result.feedback().isBlank()) {
            player.sendSystemMessage(Component.literal("[" + definition.displayName().toUpperCase(java.util.Locale.ROOT) + "] " + result.feedback())
                    .withStyle(ChatFormatting.AQUA));
        }
        AbilityIntegrationHooks.notifyActivated(player, definition);
        HunterData.sync(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        for (Ability ability : REGISTRY.all()) ability.tick(player);
    }

    public static void cancel(ServerPlayer player) {
        for (Ability ability : REGISTRY.all()) ability.cancel(player);
    }

    public static Collection<String> ids() {
        return REGISTRY.ids();
    }

    public static Optional<AbilityDefinition> definition(String id) {
        return REGISTRY.resolve(id).map(Ability::definition);
    }

    public static List<AbilityCooldownView.Entry> cooldowns(ServerPlayer player) {
        return AbilityCooldownView.snapshot(player, REGISTRY);
    }

    public static AbilityRegistry registry() {
        return REGISTRY;
    }

    private static boolean validPlayerState(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && HunterData.isAwakened(player)
                && player.getServer() != null && player.serverLevel().hasChunkAt(player.blockPosition());
    }

    private static void failure(ServerPlayer player, String feedback) {
        if (player == null) return;
        player.level().playSound(null, player.blockPosition(), ModSounds.MANA_FAIL.get(), SoundSource.PLAYERS, 0.55F, 1.0F);
        player.sendSystemMessage(Component.literal("[SYSTEM] " + feedback).withStyle(ChatFormatting.RED));
    }

    private AbilityService() {
    }
}
