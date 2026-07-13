package com.tre.sololeveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.AbilityHandler;
import com.tre.sololeveling.gameplay.ability.AbilityDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator-only test surface for every registered ability. */
@Mod.EventBusSubscriber(modid = "sololeveling")
public final class AbilityTestCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        registerRoot(event.getDispatcher(), "sl");
        registerRoot(event.getDispatcher(), "sololeveling");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String rootName) {
        dispatcher.register(Commands.literal(rootName).then(abilityNode()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> abilityNode() {
        return Commands.literal("ability").requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("ability", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(AbilityHandler.abilityIds(), builder))
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "ability")))))
                .then(Commands.literal("activate")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(AbilityHandler.abilityIds(), builder))
                                        .executes(ctx -> activate(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "ability"))))))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(AbilityHandler.abilityIds(), builder))
                                        .executes(ctx -> unlock(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "ability"))))))
                .then(Commands.literal("cooldown_clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clearCooldowns(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("mana_fill")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> fillMana(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("evolution_reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("quicksilver")
                                        .executes(ctx -> resetQuicksilverEvolution(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Registered abilities: " + String.join(", ", AbilityHandler.abilityIds()))
                .withStyle(ChatFormatting.AQUA), false);
        return AbilityHandler.abilityIds().size();
    }

    private static int info(CommandSourceStack source, String id) {
        AbilityDefinition definition = AbilityHandler.definition(id).orElse(null);
        if (definition == null) {
            source.sendFailure(Component.literal("Unknown ability: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(definition.displayName() + " [" + definition.id() + "] - " + definition.description())
                .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("Unlock: " + definition.unlock().description()
                + " | Mana: " + definition.manaCost()
                + " | Cooldown: " + definition.cooldownSeconds() + "s"
                + " | Range: " + definition.maximumRange()
                + " | Scaling: " + definition.scaling().summary()), false);
        return 1;
    }

    private static int activate(CommandSourceStack source, ServerPlayer player, String id) {
        boolean success = AbilityHandler.activateForTest(player, id);
        if (success) source.sendSuccess(() -> Component.literal("Ability test succeeded for " + player.getScoreboardName()), true);
        else source.sendFailure(Component.literal("Ability test failed validation or execution for " + player.getScoreboardName()));
        return success ? 1 : 0;
    }

    private static int unlock(CommandSourceStack source, ServerPlayer player, String id) {
        AbilityDefinition definition = AbilityHandler.definition(id).orElse(null);
        if (definition == null) {
            source.sendFailure(Component.literal("Unknown ability: " + id));
            return 0;
        }
        String skill = definition.unlock().skillId();
        if (skill.isBlank()) {
            source.sendSuccess(() -> Component.literal(definition.displayName() + " is unlocked by awakening/level and has no skill flag."), false);
            return 1;
        }
        HunterData.unlockSkill(player, skill);
        source.sendSuccess(() -> Component.literal("Unlocked " + skill + " for " + player.getScoreboardName()), true);
        return 1;
    }

    private static int clearCooldowns(CommandSourceStack source, ServerPlayer player) {
        for (String id : AbilityHandler.abilityIds()) HunterData.mutable(player).remove("cooldown_" + id);
        HunterData.clearCooldowns(player);
        HunterData.sync(player);
        source.sendSuccess(() -> Component.literal("Ability cooldowns cleared for " + player.getScoreboardName()), true);
        return 1;
    }

    private static int fillMana(CommandSourceStack source, ServerPlayer player) {
        HunterData.setMana(player, HunterData.getMaxMana(player));
        source.sendSuccess(() -> Component.literal("Mana filled for " + player.getScoreboardName()), true);
        return 1;
    }

    private static int resetQuicksilverEvolution(CommandSourceStack source, ServerPlayer player) {
        HunterData.mutable(player).putString("evolution_quicksilver", "");
        HunterData.mutable(player).putInt("skill_evolution_tokens",
                Math.max(1, HunterData.mutable(player).getInt("skill_evolution_tokens")));
        HunterData.clearCooldowns(player);
        HunterData.sync(player);
        source.sendSuccess(() -> Component.literal("Reset Quicksilver evolution and restored a test token for "
                + player.getScoreboardName()), true);
        return 1;
    }

    private AbilityTestCommands() {
    }
}
