package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.AbilityVisualPacket;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Central server authority for validation, cast timing, mana, cooldowns, execution, and feedback. */
public final class AbilityService {
    private enum CastPhase { STARTUP, RECOVERY }

    private static final class ActiveCast {
        private final Ability ability;
        private final UUID targetId;
        private final int targetEntityId;
        private final Vec3 origin;
        private final long startedAt;
        private final long phaseEndsAt;
        private final int reservedMana;
        private final CastPhase phase;

        private ActiveCast(Ability ability, Entity target, Vec3 origin, long startedAt, long phaseEndsAt,
                           int reservedMana, CastPhase phase) {
            this.ability = ability;
            this.targetId = target == null ? null : target.getUUID();
            this.targetEntityId = target == null ? -1 : target.getId();
            this.origin = origin;
            this.startedAt = startedAt;
            this.phaseEndsAt = phaseEndsAt;
            this.reservedMana = reservedMana;
            this.phase = phase;
        }
    }

    private static final AbilityRegistry REGISTRY = createRegistry();
    private static final Map<UUID, ActiveCast> ACTIVE_CASTS = new LinkedHashMap<>();

    private static AbilityRegistry createRegistry() {
        AbilityRegistry registry = new AbilityRegistry();
        StandardAbilities.registerAll(registry);
        registry.validatePhaseFiveQuality();
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
        ActiveCast existing = ACTIVE_CASTS.get(player.getUUID());
        if (existing != null) {
            failure(player, existing.phase == CastPhase.STARTUP
                    ? "Another ability is already starting." : "Ability recovery is still active.");
            return false;
        }
        if (!HunterData.cooldownReady(player, definition.id())) {
            double seconds = HunterData.cooldownRemaining(player, definition.id()) / 20.0D;
            failure(player, definition.displayName() + " is on cooldown for "
                    + String.format(java.util.Locale.ROOT, "%.1f", seconds) + "s.");
            return false;
        }

        Entity target = ability.selectTarget(player);
        if (ability.requiresTarget(player) && target == null) {
            failure(player, "No valid target is available for " + definition.displayName() + ".");
            return false;
        }
        long now = player.level().getGameTime();
        AbilityContext context = new AbilityContext(player, player.serverLevel(), definition,
                target, player.position(), now);
        AbilityResult validation = ability.validateStart(context);
        if (validation == null || !validation.success()) {
            failure(player, validation == null || validation.feedback().isBlank()
                    ? "The ability cannot start from this position." : validation.feedback());
            return false;
        }

        int manaCost = AbilityMastery.adjustManaCost(player, definition.id(), Math.max(0, ability.manaCost(player)));
        if (HunterData.getMana(player) < manaCost || !HunterData.spendMana(player, manaCost)) {
            HunterData.sync(player);
            return false;
        }

        AbilityCastProfile profile = definition.castProfile();
        if (profile.startupTicks() <= 0) return resolve(player, ability, target, context.castOrigin(), now, manaCost);

        ActiveCast cast = new ActiveCast(ability, target, context.castOrigin(), now,
                now + profile.startupTicks(), manaCost, CastPhase.STARTUP);
        ACTIVE_CASTS.put(player.getUUID(), cast);
        writeCastState(player, definition.id(), "startup", cast.phaseEndsAt);
        ModNetwork.sendAbilityVisual(player, target, definition, AbilityVisualPacket.Stage.STARTUP,
                profile.startupTicks());
        player.displayClientMessage(Component.literal(definition.displayName() + " • startup "
                + String.format(java.util.Locale.ROOT, "%.2fs", profile.startupTicks() / 20.0D))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        HunterData.sync(player);
        return true;
    }

    private static boolean resolve(ServerPlayer player, Ability ability, Entity target, Vec3 castOrigin,
                                   long startedAt, int reservedMana) {
        AbilityDefinition definition = ability.definition();
        AbilityCastProfile profile = definition.castProfile();
        AbilityResult result;
        try {
            if (ability.requiresTarget(player) && !targetStillAvailable(player, target)) {
                result = AbilityResult.failure("The prepared target became invalid.");
            } else {
                AbilityContext resolutionContext = new AbilityContext(player, player.serverLevel(), definition,
                        target, castOrigin, startedAt);
                AbilityResult revalidation = ability.validateStart(resolutionContext);
                result = revalidation == null || !revalidation.success()
                        ? revalidation : ability.activate(resolutionContext);
            }
        } catch (RuntimeException exception) {
            result = AbilityResult.failure(definition.displayName() + " failed safely.");
        }

        if (result == null || !result.success()) {
            if (reservedMana > 0) HunterData.addMana(player, reservedMana);
            String message = result == null || result.feedback().isBlank()
                    ? "No valid target or requirement was met." : result.feedback();
            failure(player, message);
            ModNetwork.sendAbilityVisual(player, target, definition, AbilityVisualPacket.Stage.FAILED,
                    profile.failureRecoveryTicks());
            beginRecovery(player, ability, target, profile.failureRecoveryTicks());
            HunterData.sync(player);
            return false;
        }

        if (HunterData.isStealthed(player) && !"stealth".equals(definition.id())) {
            HunterData.endStealth(player);
            AbilityEffects.particles(player.serverLevel(), player.position().add(0, 1, 0),
                    ParticleTypes.SMOKE, 16, 0.4D);
            player.displayClientMessage(Component.literal("[STEALTH BROKEN: ABILITY USED]")
                    .withStyle(ChatFormatting.GRAY), true);
        }

        int cooldown = AbilityMastery.adjustCooldown(player, definition.id(), Math.max(0, ability.cooldownTicks(player)));
        HunterData.setCooldown(player, definition.id(), cooldown);
        AbilityMastery.recordResolvedUse(player, definition);
        AbilityEffects.activationBurst(player);
        ModNetwork.sendAbilityVisual(player, target, definition, AbilityVisualPacket.Stage.ACTIVE,
                profile.activeTicks());
        if (!result.feedback().isBlank()) {
            player.sendSystemMessage(Component.literal("[" + definition.displayName().toUpperCase(java.util.Locale.ROOT)
                    + "] " + result.feedback()).withStyle(ChatFormatting.AQUA));
        }
        if (cooldown > 0) {
            player.displayClientMessage(Component.literal(definition.displayName() + " • "
                    + String.format(java.util.Locale.ROOT, "%.1fs cooldown", cooldown / 20.0D))
                    .withStyle(ChatFormatting.DARK_AQUA), true);
        }
        AbilityIntegrationHooks.notifyResolved(player, definition, reservedMana);
        AbilityCombos.record(player, definition.id());
        beginRecovery(player, ability, target, profile.recoveryTicks());
        HunterData.sync(player);
        return true;
    }

    private static void beginRecovery(ServerPlayer player, Ability ability, Entity target, int ticks) {
        if (ticks <= 0) {
            ACTIVE_CASTS.remove(player.getUUID());
            clearCastState(player);
            return;
        }
        long now = player.level().getGameTime();
        ActiveCast recovery = new ActiveCast(ability, target, player.position(), now,
                now + ticks, 0, CastPhase.RECOVERY);
        ACTIVE_CASTS.put(player.getUUID(), recovery);
        writeCastState(player, ability.definition().id(), "recovery", recovery.phaseEndsAt);
    }

    public static void tick(ServerPlayer player) {
        ActiveCast cast = ACTIVE_CASTS.get(player.getUUID());
        if (cast != null) {
            long now = player.level().getGameTime();
            if (cast.phase == CastPhase.STARTUP) {
                AbilityCastProfile profile = cast.ability.definition().castProfile();
                if (!validPlayerState(player)) {
                    interruptStartup(player, "invalid state");
                } else if (profile.movementTolerance() >= 0.0D
                        && player.position().distanceToSqr(cast.origin)
                        > profile.movementTolerance() * profile.movementTolerance()) {
                    interruptStartup(player, "movement");
                } else if (now >= cast.phaseEndsAt) {
                    ACTIVE_CASTS.remove(player.getUUID());
                    Entity target = cast.targetId == null ? null : player.serverLevel().getEntity(cast.targetId);
                    resolve(player, cast.ability, target, cast.origin, cast.startedAt, cast.reservedMana);
                }
            } else if (now >= cast.phaseEndsAt) {
                ACTIVE_CASTS.remove(player.getUUID());
                clearCastState(player);
                HunterData.sync(player);
            }
        }
        for (Ability ability : REGISTRY.all()) ability.tick(player);
    }

    public static void onDamaged(ServerPlayer player) {
        ActiveCast cast = ACTIVE_CASTS.get(player.getUUID());
        if (cast != null && cast.phase == CastPhase.STARTUP
                && cast.ability.definition().castProfile().interruptOnDamage()) {
            interruptStartup(player, "damage");
        }
        for (Ability ability : REGISTRY.all()) ability.onInterrupted(player, "damage");
    }

    public static void onAttack(ServerPlayer player) {
        ActiveCast cast = ACTIVE_CASTS.get(player.getUUID());
        if (cast != null && cast.phase == CastPhase.STARTUP
                && cast.ability.definition().castProfile().interruptOnAttack()) {
            interruptStartup(player, "attack");
        }
    }

    private static void interruptStartup(ServerPlayer player, String reason) {
        ActiveCast cast = ACTIVE_CASTS.remove(player.getUUID());
        if (cast == null || cast.phase != CastPhase.STARTUP) return;
        if (cast.reservedMana > 0) HunterData.addMana(player, cast.reservedMana);
        Entity target = cast.targetId == null ? null : player.serverLevel().getEntity(cast.targetId);
        ModNetwork.sendAbilityVisual(player, target, cast.ability.definition(),
                AbilityVisualPacket.Stage.INTERRUPTED, 0);
        clearCastState(player);
        player.displayClientMessage(Component.literal(cast.ability.definition().displayName()
                + " interrupted by " + reason + ".").withStyle(ChatFormatting.RED), true);
        HunterData.sync(player);
    }

    public static void cancel(ServerPlayer player) {
        ActiveCast cast = ACTIVE_CASTS.remove(player.getUUID());
        if (cast != null && cast.phase == CastPhase.STARTUP && cast.reservedMana > 0) {
            HunterData.addMana(player, cast.reservedMana);
        }
        clearCastState(player);
        AbilityCombos.clear(player);
        for (Ability ability : REGISTRY.all()) ability.cancel(player);
        if (HunterData.isStealthed(player)) HunterData.endStealth(player);
    }

    public static Collection<String> ids() { return REGISTRY.ids(); }
    public static Optional<AbilityDefinition> definition(String id) { return REGISTRY.resolve(id).map(Ability::definition); }
    public static List<AbilityCooldownView.Entry> cooldowns(ServerPlayer player) { return AbilityCooldownView.snapshot(player, REGISTRY); }
    public static AbilityRegistry registry() { return REGISTRY; }

    private static boolean targetStillAvailable(ServerPlayer player, Entity target) {
        return target != null && target.isAlive() && target.level() == player.level()
                && player.serverLevel().hasChunkAt(target.blockPosition());
    }

    private static boolean validPlayerState(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && HunterData.isAwakened(player)
                && player.getServer() != null && player.serverLevel().hasChunkAt(player.blockPosition());
    }

    private static void writeCastState(ServerPlayer player, String abilityId, String phase, long until) {
        HunterData.mutable(player).putString("ability_cast_id", abilityId);
        HunterData.mutable(player).putString("ability_cast_phase", phase);
        HunterData.mutable(player).putLong("ability_cast_until", until);
    }

    private static void clearCastState(ServerPlayer player) {
        HunterData.mutable(player).putString("ability_cast_id", "");
        HunterData.mutable(player).putString("ability_cast_phase", "");
        HunterData.mutable(player).putLong("ability_cast_until", 0L);
    }

    private static void failure(ServerPlayer player, String feedback) {
        if (player == null) return;
        player.level().playSound(null, player.blockPosition(), ModSounds.MANA_FAIL.get(),
                SoundSource.PLAYERS, 0.55F, 1.0F);
        player.sendSystemMessage(Component.literal("[SYSTEM] " + feedback).withStyle(ChatFormatting.RED));
    }

    private AbilityService() {}
}
