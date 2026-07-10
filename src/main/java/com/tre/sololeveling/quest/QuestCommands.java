package com.tre.sololeveling.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class QuestCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("slquest"));
        dispatcher.register(Commands.literal("sl").then(root("quests")));
        dispatcher.register(Commands.literal("sololeveling").then(root("quests")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> list(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("inspect")
                        .then(playerQuest((source, player, quest) -> {
                            source.sendSuccess(() -> Component.literal(QuestManager.inspect(player, quest)), false);
                            return 1;
                        })))
                .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                        .then(playerQuest((source, player, quest) -> respond(source, QuestManager.start(player, quest)))))
                .then(Commands.literal("advance").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("quest", StringArgumentType.word()).suggests(QuestCommands::suggestQuests)
                                        .then(Commands.argument("objective", StringArgumentType.word())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                        .executes(ctx -> respond(ctx.getSource(), QuestManager.advanceObjective(
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                QuestRegistry.parse(StringArgumentType.getString(ctx, "quest")),
                                                                StringArgumentType.getString(ctx, "objective"),
                                                                IntegerArgumentType.getInteger(ctx, "amount")))))))))
                .then(Commands.literal("complete").requires(source -> source.hasPermission(2))
                        .then(playerQuest((source, player, quest) -> respond(source, QuestManager.forceComplete(player, quest)))))
                .then(Commands.literal("fail").requires(source -> source.hasPermission(2))
                        .then(playerQuest((source, player, quest) -> respond(source, QuestManager.fail(player, quest, "Failed by command")))))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(playerQuest((source, player, quest) -> respond(source, QuestManager.reset(player, quest)))))
                .then(Commands.literal("resetdaily").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> respond(ctx.getSource(), QuestManager.resetDaily(EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("clear").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> respond(ctx.getSource(), QuestManager.clear(EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> playerQuest(QuestCommand command) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("quest", StringArgumentType.word()).suggests(QuestCommands::suggestQuests)
                        .executes(ctx -> command.run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                QuestRegistry.parse(StringArgumentType.getString(ctx, "quest")))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestQuests(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String[] ids = QuestRegistry.ids().stream().map(ResourceLocation::toString).sorted().toArray(String[]::new);
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        int count = 0;
        for (QuestTypes.Snapshot snapshot : QuestManager.snapshots(player)) {
            if (snapshot.status() != QuestTypes.Status.ACTIVE) continue;
            source.sendSuccess(() -> Component.literal(QuestManager.inspect(player, snapshot.id())), false);
            count++;
        }
        if (count == 0) source.sendSuccess(() -> Component.literal("No active quests"), false);
        return count;
    }

    private static int respond(CommandSourceStack source, QuestTypes.Result result) {
        if (result.success()) source.sendSuccess(() -> Component.literal(result.message()), false);
        else source.sendFailure(Component.literal(result.message()));
        return result.success() ? 1 : 0;
    }

    @FunctionalInterface
    private interface QuestCommand {
        int run(CommandSourceStack source, ServerPlayer player, ResourceLocation quest) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
    }

    private QuestCommands() {}
}
