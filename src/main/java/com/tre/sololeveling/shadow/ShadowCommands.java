package com.tre.sololeveling.shadow;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tre.sololeveling.gameplay.ShadowHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Operator diagnostics and complete shadow-system command surface. */
@Mod.EventBusSubscriber(modid = "sololeveling", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowCommands {
    private static final String[] RANKS = {"Normal", "Elite", "Knight", "Elite Knight", "Commander", "Marshal", "Grand Marshal"};

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        registerRoot(event.getDispatcher(), "sl");
        registerRoot(event.getDispatcher(), "sololeveling");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(Commands.literal(root)
                .then(Commands.literal("shadow").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("target")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("entity", StringArgumentType.word())
                                                .executes(ctx -> target(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "entity"))))))
                        .then(Commands.literal("extract")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> result(ctx.getSource(), ShadowHandler.extract(EntityArgument.getPlayer(ctx, "player")), "Extraction executed"))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("entity", StringArgumentType.word())
                                                .executes(ctx -> add(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "entity"))))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> list(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shadow", StringArgumentType.word())
                                                .executes(ctx -> inspect(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "shadow"))))))
                        .then(Commands.literal("summon")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shadow", StringArgumentType.word())
                                                .executes(ctx -> result(ctx.getSource(), ShadowHandler.summon(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "shadow")), "Shadow summoned")))))
                        .then(Commands.literal("summonall")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> count(ctx.getSource(), ShadowHandler.summonAll(EntityArgument.getPlayer(ctx, "player")), "shadows summoned"))))
                        .then(Commands.literal("dismiss")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shadow", StringArgumentType.word())
                                                .executes(ctx -> result(ctx.getSource(), ShadowHandler.dismiss(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "shadow")), "Shadow dismissed")))))
                        .then(Commands.literal("dismissall")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> { ShadowHandler.dismissAll(EntityArgument.getPlayer(ctx, "player")); return result(ctx.getSource(), true, "All shadows dismissed"); })))
                        .then(Commands.literal("level")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shadow", StringArgumentType.word())
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, ShadowStorage.MAX_LEVEL))
                                                        .executes(ctx -> result(ctx.getSource(), ShadowHandler.setShadowLevel(EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "shadow"), IntegerArgumentType.getInteger(ctx, "level")), "Shadow level changed"))))))
                        .then(Commands.literal("rank")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("shadow", StringArgumentType.word())
                                                .then(Commands.argument("rank", StringArgumentType.greedyString())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RANKS, builder))
                                                        .executes(ctx -> result(ctx.getSource(), ShadowHandler.setShadowRank(EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "shadow"), StringArgumentType.getString(ctx, "rank")), "Shadow rank changed"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> { ShadowHandler.clearRecords(EntityArgument.getPlayer(ctx, "player")); return result(ctx.getSource(), true, "Shadow storage cleared"); })))));
    }

    private static int target(CommandSourceStack source, ServerPlayer player, String id) {
        EntityType<?> type = entityType(id);
        return result(source, type != null && ShadowHandler.createExtractionTarget(player, type), "Extraction target created");
    }

    private static int add(CommandSourceStack source, ServerPlayer player, String id) {
        EntityType<?> type = entityType(id);
        return result(source, type != null && ShadowHandler.addTestShadow(player, type), "Test shadow added");
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        ShadowSystemApi.UiSnapshot snapshot = ShadowHandler.uiSnapshot(player);
        source.sendSuccess(() -> Component.literal("Shadows " + snapshot.stored() + "/" + snapshot.capacity()
                + " | Active " + snapshot.active() + "/" + snapshot.activeLimit()).withStyle(ChatFormatting.DARK_PURPLE), false);
        int index = 1;
        for (ShadowStorage.Snapshot shadow : snapshot.shadows()) {
            int number = index++;
            source.sendSuccess(() -> Component.literal(number + ". " + shadow.name() + " [" + shadow.id().toString().substring(0, 8)
                    + "] " + shadow.rank().display() + " Lv." + shadow.level() + (shadow.active() ? " ACTIVE" : "")), false);
        }
        return snapshot.stored();
    }

    private static int inspect(CommandSourceStack source, ServerPlayer player, String selector) {
        return ShadowStorage.inspect(player, selector).map(shadow -> {
            source.sendSuccess(() -> Component.literal(shadow.name() + " | " + shadow.type() + " | " + shadow.rank().display()
                    + " | Lv." + shadow.level() + " XP " + shadow.xp() + "/" + ShadowProgressionService.xpNeeded(shadow.level())
                    + " | Active " + shadow.active() + " | ID " + shadow.id()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return 1;
        }).orElseGet(() -> result(source, false, "Shadow not found"));
    }

    private static EntityType<?> entityType(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value.contains(":") ? value : "minecraft:" + value);
        return id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
    }

    private static int count(CommandSourceStack source, int amount, String label) {
        source.sendSuccess(() -> Component.literal(amount + " " + label).withStyle(ChatFormatting.AQUA), true);
        return amount;
    }

    private static int result(CommandSourceStack source, boolean success, String message) {
        if (success) source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.AQUA), true);
        else source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return success ? 1 : 0;
    }

    private ShadowCommands() {}
}
