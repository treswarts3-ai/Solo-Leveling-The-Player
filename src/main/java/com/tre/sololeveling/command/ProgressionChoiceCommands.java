package com.tre.sololeveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tre.sololeveling.gameplay.ProgressionChoiceHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class ProgressionChoiceCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("slgrowth")
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> reset(context.getSource(), EntityArgument.getPlayers(context, "players"))))));
    }

    private static int reset(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) ProgressionChoiceHandler.reset(player);
        source.sendSuccess(() -> Component.literal("Reset growth-choice progression for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private ProgressionChoiceCommands() {}
}
