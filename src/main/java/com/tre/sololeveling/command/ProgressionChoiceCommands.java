package com.tre.sololeveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.ProgressionChoiceHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class ProgressionChoiceCommands {
    private static final String[] GROWTH_CHOICES = {"vanguard", "phantom", "arcane"};
    private static final String[] MILESTONE_CHOICES = {"evolution", "mastery", "cache"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("slgrowth")
                .then(Commands.literal("status")
                        .executes(context -> growthStatus(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("choose")
                        .then(Commands.argument("path", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(GROWTH_CHOICES, builder))
                                .executes(context -> chooseGrowth(context.getSource(), context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "path")))))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> resetGrowth(context.getSource(), EntityArgument.getPlayers(context, "players"))))));

        dispatcher.register(Commands.literal("slmilestone")
                .then(Commands.literal("status")
                        .executes(context -> milestoneStatus(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("choose")
                        .then(Commands.argument("reward", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(MILESTONE_CHOICES, builder))
                                .executes(context -> chooseMilestone(context.getSource(), context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "reward")))))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> resetMilestones(context.getSource(), EntityArgument.getPlayers(context, "players"))))));

        dispatcher.register(Commands.literal("slrank")
                .then(Commands.literal("status")
                        .executes(context -> rankStatus(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("begin")
                        .executes(context -> beginRankTrial(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> resetRankTrial(context.getSource(), EntityArgument.getPlayers(context, "players"))))));
    }

    private static int growthStatus(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        int pending = Math.max(0, tag.getInt("pending_growth_choices"));
        int next = Math.max(ProgressionChoiceHandler.FIRST_CHOICE_LEVEL, tag.getInt("next_growth_choice_level"));
        String text = "Growth choices: " + pending + " pending | Vanguard " + tag.getInt("growth_vanguard")
                + " | Phantom " + tag.getInt("growth_phantom") + " | Arcane " + tag.getInt("growth_arcane")
                + " | Next at level " + next;
        source.sendSuccess(() -> Component.literal(text).withStyle(pending > 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA), false);
        return 1;
    }

    private static int milestoneStatus(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        int pending = Math.max(0, tag.getInt("pending_major_milestones"));
        int next = Math.max(ProgressionChoiceHandler.FIRST_MILESTONE_LEVEL, tag.getInt("next_major_milestone_level"));
        String text = "Major milestones: " + pending + " pending | Evolution " + tag.getInt("milestone_evolution")
                + " | Mastery " + tag.getInt("milestone_mastery") + " | Cache " + tag.getInt("milestone_cache")
                + " | Tokens " + tag.getInt("skill_evolution_tokens") + " | Next at level " + next;
        source.sendSuccess(() -> Component.literal(text).withStyle(pending > 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA), false);
        return 1;
    }

    private static int rankStatus(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal(ProgressionChoiceHandler.rankStatus(player)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int chooseGrowth(CommandSourceStack source, ServerPlayer player, String path) {
        boolean success = ProgressionChoiceHandler.choose(player, path);
        if (!success) source.sendFailure(Component.literal("Choose vanguard, phantom, or arcane while a growth choice is pending."));
        return success ? 1 : 0;
    }

    private static int chooseMilestone(CommandSourceStack source, ServerPlayer player, String reward) {
        boolean success = ProgressionChoiceHandler.chooseMilestone(player, reward);
        if (!success) source.sendFailure(Component.literal("Choose evolution, mastery, or cache while a major milestone is pending."));
        return success ? 1 : 0;
    }

    private static int beginRankTrial(CommandSourceStack source, ServerPlayer player) {
        boolean success = ProgressionChoiceHandler.beginRankTrial(player);
        if (!success) source.sendFailure(Component.literal("The E-to-D evaluation cannot begin right now."));
        return success ? 1 : 0;
    }

    private static int resetGrowth(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) ProgressionChoiceHandler.reset(player);
        source.sendSuccess(() -> Component.literal("Reset growth-choice progression for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int resetMilestones(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) ProgressionChoiceHandler.resetMilestones(player);
        source.sendSuccess(() -> Component.literal("Reset major-milestone tracking for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int resetRankTrial(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) ProgressionChoiceHandler.resetRankTrial(player);
        source.sendSuccess(() -> Component.literal("Reset active rank evaluations for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private ProgressionChoiceCommands() {}
}
