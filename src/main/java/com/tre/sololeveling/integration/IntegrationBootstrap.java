package com.tre.sololeveling.integration;

import com.tre.sololeveling.dungeon.DungeonHooks;
import com.tre.sololeveling.gameplay.ShadowHandler;
import com.tre.sololeveling.gameplay.ability.AbilityIntegrationHooks;
import com.tre.sololeveling.gameplay.ability.AbilityResult;
import com.tre.sololeveling.quest.QuestApi;
import com.tre.sololeveling.shadow.ShadowExtractionService;
import com.tre.sololeveling.shadow.ShadowSummoningService;

/** Installs narrow adapters between independently developed server-side systems. */
public final class IntegrationBootstrap {
    public static void install() {
        AbilityIntegrationHooks.installShadowAdapter(new AbilityIntegrationHooks.ShadowAdapter() {
            @Override public AbilityResult exchange(net.minecraft.server.level.ServerPlayer player) {
                return ShadowHandler.exchangeValidated(player)
                        ? AbilityResult.success("Exchanged positions with an owned shadow.")
                        : AbilityResult.failure("No safe owned shadow destination is available.");
            }

            @Override public AbilityResult extract(net.minecraft.server.level.ServerPlayer player) {
                return ShadowExtractionService.extractValidated(player)
                        ? AbilityResult.success("Shadow extraction succeeded.")
                        : AbilityResult.failure("No valid shadow was extracted.");
            }

            @Override public AbilityResult summon(net.minecraft.server.level.ServerPlayer player) {
                if (!ShadowSummoningService.summonFirstValidated(player)) {
                    return AbilityResult.failure("No stored shadow can be summoned.");
                }
                QuestApi.onShadowSummoned(player, "any");
                return AbilityResult.success("An owned shadow answered the summons.");
            }
        });

        DungeonHooks.registerRewardHook((player, session, template) ->
                QuestApi.onDungeonCleared(player, template.id()));
    }

    private IntegrationBootstrap() {}
}
