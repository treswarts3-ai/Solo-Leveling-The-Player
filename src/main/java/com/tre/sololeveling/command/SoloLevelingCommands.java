package com.tre.sololeveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.QuestHandler;
import com.tre.sololeveling.gameplay.ShadowHandler;
import com.tre.sololeveling.registry.ModItems;
import com.tre.sololeveling.quest.QuestCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SoloLevelingCommands {
    private static final String[] STATS = {"strength", "agility", "stamina", "intelligence", "sense"};
    private static final String[] RANKS = {"E-Rank", "D-Rank", "C-Rank", "B-Rank", "A-Rank", "S-Rank", "National-Level", "Shadow Monarch"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("sl"));
        dispatcher.register(root("sololeveling"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("awaken").requires(source -> source.hasPermission(2))
                        .then(playerTarget(HunterData::awaken, "System awakened")))
                .then(Commands.literal("system").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("revoke").then(playerTarget(player -> {
                            HunterData.mutable(player).putBoolean("awakened", false);
                            HunterData.sync(player);
                        }, "System revoked"))))
                .then(xpCommands())
                .then(levelCommands())
                .then(manaCommands())
                .then(statPointCommands())
                .then(statCommands())
                .then(goldCommands())
                .then(skillCommands())
                .then(rankCommands())
                .then(jobCommands())
                .then(titleCommands())
                .then(blackHeartCommands())
                .then(progressionCommands())
                .then(dailyCommands())
                .then(questCommands())
                .then(penaltyCommands())
                .then(shadowCommands())
                .then(Commands.literal("giveall").requires(source -> source.hasPermission(2))
                        .then(playerTarget(player -> ModItems.ALL.forEach(item -> player.getInventory().add(new ItemStack(item.get()))), "All mod items granted")))
                .then(Commands.literal("sync").requires(source -> source.hasPermission(2))
                        .then(playerTarget(HunterData::sync, "Data synchronized")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> xpCommands() {
        return Commands.literal("xp").requires(source -> source.hasPermission(2))
                .then(valueTarget("add", 0, Integer.MAX_VALUE, HunterData::addXp))
                .then(valueTarget("set", 0, Integer.MAX_VALUE, HunterData::setXp))
                .then(valueTarget("remove", 0, Integer.MAX_VALUE, (player, value) -> HunterData.setXp(player, Math.max(0, HunterData.getXp(player) - value))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> levelCommands() {
        return Commands.literal("level").requires(source -> source.hasPermission(2))
                .then(valueTarget("add", 0, 10000, (player, value) -> HunterData.setLevel(player, HunterData.getLevel(player) + value)))
                .then(valueTarget("set", 1, 10000, HunterData::setLevel))
                .then(valueTarget("remove", 0, 10000, (player, value) -> HunterData.setLevel(player, HunterData.getLevel(player) - value)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> manaCommands() {
        return Commands.literal("mana").requires(source -> source.hasPermission(2))
                .then(valueTarget("add", 0, 1_000_000, (player, value) -> { HunterData.addMana(player, value); HunterData.sync(player); }))
                .then(valueTarget("set", 0, 1_000_000, HunterData::setMana))
                .then(valueTarget("remove", 0, 1_000_000, (player, value) -> HunterData.setMana(player, HunterData.getMana(player) - value)))
                .then(Commands.literal("fill").then(playerTarget(player -> HunterData.setMana(player, HunterData.getMaxMana(player)), "Mana restored")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statPointCommands() {
        return Commands.literal("statpoints").requires(source -> source.hasPermission(2))
                .then(valueTarget("add", 0, 100000, HunterData::addStatPoints))
                .then(valueTarget("set", 0, 100000, HunterData::setStatPoints));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statCommands() {
        return Commands.literal("stat").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(statValueTarget((player, stat, value) -> HunterData.setStat(player, stat, value))))
                .then(Commands.literal("add").then(statValueTarget((player, stat, value) -> HunterData.setStat(player, stat, HunterData.getStat(player, stat) + value))))
                .then(Commands.literal("reset").then(playerTarget(HunterData::resetStats, "Stats reset")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> goldCommands() {
        return Commands.literal("gold").requires(source -> source.hasPermission(2))
                .then(valueTarget("add", 0, Integer.MAX_VALUE, HunterData::addGold))
                .then(valueTarget("set", 0, Integer.MAX_VALUE, HunterData::setGold))
                .then(valueTarget("remove", 0, Integer.MAX_VALUE, (player, value) -> HunterData.setGold(player, HunterData.getGold(player) - value)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> skillCommands() {
        return Commands.literal("skill").requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlock").then(skillTarget(HunterData::unlockSkill)))
                .then(Commands.literal("lock").then(skillTarget((player, skill) -> { HunterData.lockSkill(player, skill); return true; })))
                .then(Commands.literal("unlockall").then(playerTarget(player -> {
                    for (String skill : HunterData.SKILLS) HunterData.mutable(player).putBoolean("skill_" + skill, true);
                    HunterData.sync(player);
                }, "All skills unlocked")))
                .then(Commands.literal("lockall").then(playerTarget(player -> {
                    for (String skill : HunterData.SKILLS) HunterData.mutable(player).putBoolean("skill_" + skill, false);
                    HunterData.sync(player);
                }, "All skills locked")))
                .then(Commands.literal("cooldown_clear").then(playerTarget(HunterData::clearCooldowns, "Cooldowns cleared")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rankCommands() {
        return Commands.literal("rank").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("rank", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RANKS, builder))
                                        .executes(ctx -> run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                player -> HunterData.setRankOverride(player, StringArgumentType.getString(ctx, "rank")), "Rank changed")))))
                .then(Commands.literal("clear").then(playerTarget(HunterData::clearRankOverride, "Rank override cleared")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> jobCommands() {
        return Commands.literal("job").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(labelTarget("job", HunterData::setJob, "Job changed")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> titleCommands() {
        return Commands.literal("title").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(labelTarget("title", HunterData::setTitle, "Title changed")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> blackHeartCommands() {
        return Commands.literal("blackheart").requires(source -> source.hasPermission(2))
                .then(Commands.literal("grant").then(playerTarget(player -> HunterData.setBlackHeart(player, true), "Black Heart granted")))
                .then(Commands.literal("revoke").then(playerTarget(player -> HunterData.setBlackHeart(player, false), "Black Heart revoked")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> progressionCommands() {
        return Commands.literal("progression").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(valueTargetNode(0, 5, HunterData::setProgressionStage, "Progression stage changed")))
                .then(Commands.literal("reset").then(playerTarget(HunterData::resetProgression, "Quest progression reset")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dailyCommands() {
        return Commands.literal("daily").requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset").then(playerTarget(QuestHandler::resetDaily, "Daily quest reset")))
                .then(Commands.literal("complete").then(playerTarget(player -> {
                    HunterData.mutable(player).putInt("daily_kills", 10);
                    HunterData.mutable(player).putInt("daily_run", 1000);
                    HunterData.mutable(player).putInt("daily_pushups", 30);
                    HunterData.mutable(player).putInt("daily_situps", 30);
                    HunterData.mutable(player).putInt("daily_squats", 30);
                    HunterData.mutable(player).putBoolean("daily_complete", true);
                    HunterData.sync(player);
                }, "Daily quest completed")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> questCommands() {
        return QuestCommands.node();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> penaltyCommands() {
        return Commands.literal("penalty").requires(source -> source.hasPermission(2))
                .then(Commands.literal("send").then(playerTarget(QuestHandler::sendToPenalty, "Sent to penalty arena")))
                .then(Commands.literal("return").then(playerTarget(QuestHandler::returnFromPenalty, "Returned from penalty arena")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> shadowCommands() {
        return Commands.literal("shadow").requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear").then(playerTarget(ShadowHandler::clearRecords, "Shadows cleared")))
                .then(Commands.literal("dismissall").then(playerTarget(ShadowHandler::dismissAll, "Shadows dismissed")))
                .then(Commands.literal("mode").then(valueTargetNode(0, 3, ShadowHandler::setMode, "Shadow mode changed")))
                .then(Commands.literal("capacity")
                        .then(Commands.literal("add").then(valueTargetNode(0, 1000, HunterData::addShadowCapacity, "Shadow capacity changed")))
                        .then(Commands.literal("set").then(valueTargetNode(0, 1000, HunterData::setShadowCapacityBonus, "Shadow capacity set"))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> playerTarget(PlayerConsumer consumer, String success) {
        return Commands.argument("player", EntityArgument.player())
                .executes(ctx -> run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), consumer, success));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> valueTarget(String name, int min, int max, PlayerIntConsumer consumer) {
        return Commands.literal(name).then(valueTargetNode(min, max, consumer, "Value changed"));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> valueTargetNode(int min, int max, PlayerIntConsumer consumer, String success) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(min, max))
                        .executes(ctx -> run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                player -> consumer.accept(player, IntegerArgumentType.getInteger(ctx, "amount")), success)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> statValueTarget(PlayerStatIntConsumer consumer) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("stat", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(STATS, builder))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000))
                                .executes(ctx -> run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), player ->
                                        consumer.accept(player, StringArgumentType.getString(ctx, "stat"), IntegerArgumentType.getInteger(ctx, "amount")), "Stat changed"))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> skillTarget(PlayerSkillConsumer consumer) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(HunterData.SKILLS, builder))
                        .executes(ctx -> run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), player ->
                                consumer.accept(player, StringArgumentType.getString(ctx, "skill")), "Skill changed")));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> labelTarget(String argumentName, PlayerStringConsumer consumer, String success) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument(argumentName, StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), player ->
                                consumer.accept(player, StringArgumentType.getString(ctx, argumentName)), success)));
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("[Solo Leveling] " + player.getScoreboardName() + " | Lv " + HunterData.getLevel(player)
                + " | " + HunterData.getRank(player) + " | XP " + HunterData.getXp(player) + "/" + HunterData.xpNeeded(HunterData.getLevel(player))
                + " | Mana " + HunterData.getMana(player) + "/" + HunterData.getMaxMana(player) + " | Gold " + HunterData.getGold(player)).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("STR " + HunterData.getStat(player, "strength") + " AGI " + HunterData.getStat(player, "agility")
                + " STA " + HunterData.getStat(player, "stamina") + " INT " + HunterData.getStat(player, "intelligence")
                + " SEN " + HunterData.getStat(player, "sense") + " | Points " + HunterData.getStatPoints(player)
                + " | Stage " + HunterData.mutable(player).getInt("progression_stage") + " | MaxLv " + ModConfigs.MAX_HUNTER_LEVEL.get()), false);
        return 1;
    }

    private static int run(CommandSourceStack source, ServerPlayer player, PlayerConsumer consumer, String success) {
        consumer.accept(player);
        source.sendSuccess(() -> Component.literal(success + ": " + player.getScoreboardName()).withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    @FunctionalInterface private interface PlayerConsumer { void accept(ServerPlayer player); }
    @FunctionalInterface private interface PlayerIntConsumer { void accept(ServerPlayer player, int value); }
    @FunctionalInterface private interface PlayerStringConsumer { void accept(ServerPlayer player, String value); }
    @FunctionalInterface private interface PlayerStatIntConsumer { void accept(ServerPlayer player, String stat, int value); }
    @FunctionalInterface private interface PlayerSkillConsumer { boolean accept(ServerPlayer player, String skill); }
    private SoloLevelingCommands() {}
}
