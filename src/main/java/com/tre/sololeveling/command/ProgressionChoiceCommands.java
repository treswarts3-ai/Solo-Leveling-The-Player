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
    private static final String[] CHOICES = {"vanguard", "phantom", "arcane"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("slgrowth")
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("choose")
                        .then(Commands.argument("path", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CHOICES, builder))
                                .executes(context -> choose(context.getSource(), context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "path")))))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> reset(context.getSource(), EntityArgument.getPlayers(context, "players"))))));
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        int pending = tag.getInt("pending_growth_choices");
        int next = Math.max(ProgressionChoiceHandler.FIRST_CHOICE_LEVEL, tag.getInt("next_growth_choice_level"));
        String text = "Growth choices: " + pending + " pending | Vanguard " + tag.getInt("growth_vanguard")
                + " | Phantom " + tag.getInt("growth_phantom") + " | Arcane " + tag.getInt("growth_arcane")
                + " | Next at level " + next;
        source.sendSuccess(() -> Component.literal(text).withStyle(pending > 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA), false);
        return 1;
    }

    private static int choose(CommandSourceStack source, ServerPlayer player, String path) {
        boolean success = ProgressionChoiceHandler.choose(player, path);
        if (!success) source.sendFailure(Component.literal("Choose vanguard, phantom, or arcane while a growth choice is pending."));
        return success ? 1 : 0;
    }

    private static int reset(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) ProgressionChoiceHandler.reset(player);
        source.sendSuccess(() -> Component.literal("Reset growth-choice progression for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private ProgressionChoiceCommands() {}
}
