package com.tre.sololeveling.gameplay.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

/**
 * Narrow public extension points for the shadow and quest workstreams.
 * Adapters execute on the logical server thread after shared validation.
 */
public final class AbilityIntegrationHooks {
    public interface ShadowAdapter {
        Entity exchangeTarget(ServerPlayer player);
        AbilityResult exchange(ServerPlayer player, Entity preparedTarget);
        AbilityResult extract(ServerPlayer player);
        AbilityResult summon(ServerPlayer player);
    }

    public interface QuestListener {
        void onAbilityResolved(ServerPlayer player, AbilityDefinition definition, int manaSpent);
    }

    private static final ShadowAdapter UNAVAILABLE_SHADOWS = new ShadowAdapter() {
        @Override public Entity exchangeTarget(ServerPlayer player) { return null; }
        @Override public AbilityResult exchange(ServerPlayer player, Entity preparedTarget) { return AbilityResult.failure("Shadow Exchange is not connected yet."); }
        @Override public AbilityResult extract(ServerPlayer player) { return AbilityResult.failure("Shadow Extraction is not connected yet."); }
        @Override public AbilityResult summon(ServerPlayer player) { return AbilityResult.failure("Shadow Summoning is not connected yet."); }
    };

    private static volatile ShadowAdapter shadowAdapter = UNAVAILABLE_SHADOWS;
    private static volatile QuestListener questListener = (player, definition, manaSpent) -> { };

    public static void installShadowAdapter(ShadowAdapter adapter) {
        shadowAdapter = Objects.requireNonNull(adapter, "adapter");
    }

    public static void clearShadowAdapter() {
        shadowAdapter = UNAVAILABLE_SHADOWS;
    }

    public static ShadowAdapter shadows() {
        return shadowAdapter;
    }

    public static void installQuestListener(QuestListener listener) {
        questListener = Objects.requireNonNull(listener, "listener");
    }

    public static void clearQuestListener() {
        questListener = (player, definition, manaSpent) -> { };
    }

    static void notifyResolved(ServerPlayer player, AbilityDefinition definition, int manaSpent) {
        questListener.onAbilityResolved(player, definition, manaSpent);
    }

    private AbilityIntegrationHooks() {
    }
}
