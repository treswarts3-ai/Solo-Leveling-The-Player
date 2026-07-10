package com.tre.sololeveling.dungeon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DungeonEvents {
    private static final String[] RANKS = {"E", "D", "C", "B", "A", "S"};
    private static final String GATE_CONTACT_COOLDOWN = "sl_gate_contact_cooldown";

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        registerRoot(event.getDispatcher(), "sl");
        registerRoot(event.getDispatcher(), "sololeveling");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String rootName) {
        dispatcher.register(Commands.literal(rootName).then(dungeonCommands()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dungeonCommands() {
        return Commands.literal("dungeon")
                .then(Commands.literal("create_gate").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("gate_id", StringArgumentType.word())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(RANKS, builder))
                                        .then(Commands.argument("template", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DungeonContent.templateIds(), builder))
                                                .executes(context -> send(context.getSource(), DungeonRuntime.createGate(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "gate_id"),
                                                        DungeonTypes.GateRank.parse(StringArgumentType.getString(context, "rank")),
                                                        StringArgumentType.getString(context, "template"))))))))
                .then(Commands.literal("remove_gate").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("gate_id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DungeonSavedData.get(context.getSource().getServer()).gates().keySet(), builder))
                                .executes(context -> send(context.getSource(), DungeonRuntime.removeGate(context.getSource().getServer(), StringArgumentType.getString(context, "gate_id"))))))
                .then(Commands.literal("enter_gate")
                        .then(Commands.argument("gate_id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DungeonSavedData.get(context.getSource().getServer()).gates().keySet(), builder))
                                .executes(context -> send(context.getSource(), DungeonRuntime.enterGate(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "gate_id"))))
                                .then(Commands.argument("party", EntityArgument.players())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> {
                                            ServerPlayer owner = context.getSource().getPlayerOrException();
                                            Collection<ServerPlayer> party = EntityArgument.getPlayers(context, "party");
                                            return send(context.getSource(), DungeonRuntime.enterGate(owner, StringArgumentType.getString(context, "gate_id"), party));
                                        }))))
                .then(Commands.literal("start_dungeon")
                        .executes(context -> send(context.getSource(), DungeonRuntime.start(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("inspect_session")
                        .executes(context -> inspectCurrent(context.getSource()))
                        .then(Commands.argument("session_id", StringArgumentType.word()).requires(source -> source.hasPermission(2))
                                .executes(context -> inspect(context.getSource(), StringArgumentType.getString(context, "session_id")))))
                .then(Commands.literal("spawn_wave").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("wave_id", StringArgumentType.word())
                                .executes(context -> send(context.getSource(), DungeonRuntime.spawnWave(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "wave_id"))))))
                .then(Commands.literal("complete_objective").requires(source -> source.hasPermission(2))
                        .executes(context -> completeCurrent(context.getSource()))
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .executes(context -> complete(context.getSource(), StringArgumentType.getString(context, "session_id")))))
                .then(Commands.literal("fail_dungeon").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .executes(context -> fail(context.getSource(), StringArgumentType.getString(context, "session_id"), "Failed by command"))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> fail(context.getSource(), StringArgumentType.getString(context, "session_id"), StringArgumentType.getString(context, "reason"))))))
                .then(Commands.literal("exit_dungeon")
                        .executes(context -> send(context.getSource(), DungeonRuntime.exit(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("clear_dungeon_state").requires(source -> source.hasPermission(2))
                        .executes(context -> send(context.getSource(), DungeonRuntime.clearAll(context.getSource().getServer()))))
                .then(Commands.literal("spawn_test_enemy").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enemy_id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DungeonContent.enemyIds(), builder))
                                .executes(context -> send(context.getSource(), DungeonRuntime.spawnTestEnemy(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "enemy_id"))))))
                .then(Commands.literal("spawn_test_boss").requires(source -> source.hasPermission(2))
                        .executes(context -> send(context.getSource(), DungeonRuntime.spawnTestBoss(context.getSource().getPlayerOrException()))));
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            animateGates(server);
            DungeonRuntime.tick(server);
            DungeonCombatBehavior.tick(server);
            synchronizeDungeonDoors(server);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 2 != 0 || player.isSpectator()) return;
        MinecraftServer server = player.getServer();
        if (server == null || DungeonRuntime.findSession(server, player.getUUID()) != null) return;

        long now = player.level().getGameTime();
        for (DungeonTypes.GateDefinition gate : DungeonSavedData.get(server).gates().values()) {
            if (!player.level().dimension().equals(gate.dimension()) || !DungeonArena.gateTrigger(gate).intersects(player.getBoundingBox())) continue;
            if (now < player.getPersistentData().getLong(GATE_CONTACT_COOLDOWN)) return;
            player.getPersistentData().putLong(GATE_CONTACT_COOLDOWN, now + 40L);

            DungeonRuntime.Result entered = DungeonRuntime.enterGate(player, gate.gateId());
            if (!entered.success()) {
                player.sendSystemMessage(Component.literal("[GATE] " + entered.message()).withStyle(ChatFormatting.RED));
                return;
            }

            DungeonRuntime.Result started = DungeonRuntime.start(player);
            if (!started.success()) player.sendSystemMessage(Component.literal("[DUNGEON] " + started.message()).withStyle(ChatFormatting.RED));
            return;
        }
    }

    private static void animateGates(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 4L != 0L) return;
        for (DungeonTypes.GateDefinition gate : DungeonSavedData.get(server).gates().values()) {
            ServerLevel level = server.getLevel(gate.dimension());
            if (level == null) continue;
            if (now % 40L == 0L) DungeonArena.placeGateMarker(level, gate);
            BlockPos base = gate.position();
            level.sendParticles(ParticleTypes.PORTAL, base.getX() + 0.5D, base.getY() + 2.2D, base.getZ() + 0.5D,
                    12, 1.05D, 1.35D, 0.18D, 0.08D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, base.getX() + 0.5D, base.getY() + 2.0D, base.getZ() + 0.5D,
                    6, 0.9D, 1.25D, 0.12D, 0.015D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, base.getX() + 0.5D, base.getY() + 2.0D, base.getZ() + 0.5D,
                    3, 0.7D, 1.1D, 0.12D, 0.02D);
        }
    }

    private static void synchronizeDungeonDoors(MinecraftServer server) {
        if (server.overworld().getGameTime() % 10L != 0L) return;
        for (DungeonSession session : DungeonSavedData.get(server).sessions().values()) {
            if (session.isTerminal() || !session.arenaBuilt()) continue;
            ServerLevel level = server.getLevel(session.dungeonDimension());
            if (level == null) continue;
            int opened = Math.min(3, session.objectiveIndex());
            for (int objective = 0; objective < opened; objective++) DungeonArena.openCheckpoint(level, session, objective);
        }
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (DungeonEnemies.isDungeonEnemy(victim)) DungeonRuntime.onEnemyDeath(victim, event.getSource().getEntity());
        if (victim instanceof ServerPlayer player) DungeonRuntime.onPlayerDeath(player);
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (DungeonEnemies.isDungeonEnemy(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void experienceDrop(LivingExperienceDropEvent event) {
        if (DungeonEnemies.isDungeonEnemy(event.getEntity())) event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) DungeonRuntime.recoverPlayer(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) DungeonRuntime.onPlayerRespawn(player);
    }

    private static int inspectCurrent(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DungeonSession session = DungeonRuntime.findSession(source.getServer(), player.getUUID());
        if (session == null) return send(source, DungeonRuntime.Result.fail("No dungeon session found"));
        source.sendSuccess(() -> Component.literal(DungeonRuntime.inspect(source.getServer(), session)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int inspect(CommandSourceStack source, String rawId) {
        UUID id = parseUuid(rawId);
        if (id == null) return send(source, DungeonRuntime.Result.fail("Invalid session UUID"));
        DungeonSession session = DungeonSavedData.get(source.getServer()).sessions().get(id);
        if (session == null) return send(source, DungeonRuntime.Result.fail("Session not found"));
        source.sendSuccess(() -> Component.literal(DungeonRuntime.inspect(source.getServer(), session)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int completeCurrent(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        DungeonSession session = DungeonRuntime.findSession(source.getServer(), source.getPlayerOrException().getUUID());
        return session == null ? send(source, DungeonRuntime.Result.fail("No dungeon session found"))
                : send(source, DungeonRuntime.forceCompleteObjective(source.getServer(), session.sessionId()));
    }

    private static int complete(CommandSourceStack source, String rawId) {
        UUID id = parseUuid(rawId);
        return id == null ? send(source, DungeonRuntime.Result.fail("Invalid session UUID"))
                : send(source, DungeonRuntime.forceCompleteObjective(source.getServer(), id));
    }

    private static int fail(CommandSourceStack source, String rawId, String reason) {
        UUID id = parseUuid(rawId);
        return id == null ? send(source, DungeonRuntime.Result.fail("Invalid session UUID"))
                : send(source, DungeonRuntime.fail(source.getServer(), id, reason));
    }

    private static UUID parseUuid(String value) {
        try { return UUID.fromString(value.toLowerCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static int send(CommandSourceStack source, DungeonRuntime.Result result) {
        Component message = Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.AQUA : ChatFormatting.RED);
        if (result.success()) source.sendSuccess(() -> message, true);
        else source.sendFailure(message);
        return result.success() ? 1 : 0;
    }

    private DungeonEvents() {}
}
