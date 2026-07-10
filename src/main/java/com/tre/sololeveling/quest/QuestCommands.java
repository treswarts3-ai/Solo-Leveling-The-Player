package com.tre.sololeveling.quest;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class QuestCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("quest").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(questArgument().executes(context -> result(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        QuestManager.start(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest")), "Quest started")))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(questArgument().executes(context -> inspect(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"))))))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> list(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("advance")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(questArgument()
                                        .then(Commands.argument("objective", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(objectiveIds(), builder))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                        .executes(context -> result(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                                QuestManager.advance(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"),
                                                                        StringArgumentType.getString(context, "objective"), IntegerArgumentType.getInteger(context, "amount")), "Objective advanced")))))))
                .then(Commands.literal("complete")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(questArgument().executes(context -> result(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        QuestManager.complete(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"), true), "Quest completed")))))
                .then(Commands.literal("fail")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(questArgument().executes(context -> result(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        QuestManager.fail(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"), "Failed by command"), "Quest failed")))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(questArgument().executes(context -> result(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        QuestManager.reset(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest")), "Quest reset")))))
                .then(Commands.literal("resetdaily")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    QuestManager.resetDaily(player, true);
                                    return result(context.getSource(), player, true, "Daily quests reset");
                                })))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    QuestManager.clear(player);
                                    return result(context.getSource(), player, true, "Quest data cleared");
                                })));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> questArgument() {
        return Commands.argument("quest", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(QuestRegistry.ids(), builder));
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        QuestManager.prepareSync(player);
        ListTag quests = HunterData.mutable(player).getList(QuestManager.UI_KEY, Tag.TAG_COMPOUND);
        int active = 0;
        for (int index = 0; index < quests.size(); index++) {
            CompoundTag quest = quests.getCompound(index);
            if (!QuestStatus.ACTIVE.name().equals(quest.getString("status"))) continue;
            active++;
            source.sendSuccess(() -> Component.literal(quest.getString("id") + " — " + quest.getString("name")).withStyle(ChatFormatting.AQUA), false);
        }
        int count = active;
        source.sendSuccess(() -> Component.literal("Active quests: " + count).withStyle(ChatFormatting.GRAY), false);
        return active;
    }

    private static int inspect(CommandSourceStack source, ServerPlayer player, String id) {
        QuestManager.prepareSync(player);
        ListTag quests = HunterData.mutable(player).getList(QuestManager.UI_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < quests.size(); index++) {
            CompoundTag quest = quests.getCompound(index);
            if (!id.equals(quest.getString("id"))) continue;
            source.sendSuccess(() -> Component.literal(quest.getString("name") + " [" + quest.getString("status") + "]").withStyle(ChatFormatting.AQUA), false);
            source.sendSuccess(() -> Component.literal(quest.getString("description")).withStyle(ChatFormatting.GRAY), false);
            ListTag objectives = quest.getList("objectives", Tag.TAG_COMPOUND);
            for (int objectiveIndex = 0; objectiveIndex < objectives.size(); objectiveIndex++) {
                CompoundTag objective = objectives.getCompound(objectiveIndex);
                source.sendSuccess(() -> Component.literal("- " + objective.getString("description") + ": "
                        + objective.getInt("progress") + "/" + objective.getInt("required")), false);
            }
            return 1;
        }
        source.sendFailure(Component.literal("Unknown or locked quest: " + id));
        return 0;
    }

    private static int result(CommandSourceStack source, ServerPlayer player, boolean success, String message) {
        if (!success) {
            source.sendFailure(Component.literal(message + " failed for " + player.getScoreboardName()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(message + ": " + player.getScoreboardName()).withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    private static String[] objectiveIds() {
        return QuestRegistry.values().stream().flatMap(definition -> definition.objectives().stream())
                .map(QuestObjective::id).distinct().sorted().toArray(String[]::new);
    }

    private QuestCommands() { }
}
