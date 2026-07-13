package com.tre.sololeveling.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.dungeon.DungeonRuntime;
import com.tre.sololeveling.dungeon.DungeonSession;
import com.tre.sololeveling.dungeon.MasterDungeonBuilder;
import com.tre.sololeveling.gameplay.AbilityHandler;
import com.tre.sololeveling.gameplay.QuestHandler;
import com.tre.sololeveling.gameplay.ShadowHandler;
import com.tre.sololeveling.gameplay.ability.AbilityCooldownView;
import com.tre.sololeveling.registry.ModItems;
import com.tre.sololeveling.shadow.ShadowStorage;
import com.tre.sololeveling.shadow.ShadowSummoningService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Phase 2 reproducible test fixtures, measurements, and developer diagnostics. */
@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DebugInfrastructure {
    private static final String BACKUP_KEY = "sl_phase2_test_backup";
    private static long serverTickStarted;
    private static double latestServerTickMs;
    private static double rollingServerTickMs;

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), "sl");
        register(event.getDispatcher(), "sololeveling");
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            serverTickStarted = System.nanoTime();
        } else if (serverTickStarted != 0L) {
            latestServerTickMs = Math.max(0.0D, (System.nanoTime() - serverTickStarted) / 1_000_000.0D);
            rollingServerTickMs = rollingServerTickMs == 0.0D ? latestServerTickMs
                    : rollingServerTickMs * 0.90D + latestServerTickMs * 0.10D;
        }
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(Commands.literal(root)
                .then(testNode())
                .then(debugNode()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> testNode() {
        return Commands.literal("test").requires(source -> source.hasPermission(2))
                .then(Commands.literal("setup").executes(ctx -> setup(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("progression").executes(ctx -> progression(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("combat").executes(ctx -> combat(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("dungeon").executes(ctx -> dungeon(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("shadows").executes(ctx -> shadows(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("multiplayer").executes(ctx -> multiplayer(ctx.getSource(), ctx.getSource().getPlayerOrException())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("debug").requires(source -> source.hasPermission(2))
                .then(Commands.literal("overlay").executes(ctx -> toggleOverlay(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("player").executes(ctx -> player(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("dungeon").executes(ctx -> dungeon(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("packets").executes(ctx -> packets(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("entities").executes(ctx -> entities(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("performance").executes(ctx -> performance(ctx.getSource(), ctx.getSource().getPlayerOrException())));
    }

    private static int setup(CommandSourceStack source, ServerPlayer player) {
        if (player.getPersistentData().contains(BACKUP_KEY)) {
            source.sendFailure(Component.literal("A test backup already exists. Use /sl test reset first."));
            return 0;
        }
        CompoundTag backup = new CompoundTag();
        backup.put("hunter", HunterData.mutable(player).copy());
        backup.put("inventory", player.getInventory().save(new ListTag()));
        backup.putInt("selected", player.getInventory().selected);
        backup.putFloat("health", player.getHealth());
        backup.putInt("food", player.getFoodData().getFoodLevel());
        backup.putFloat("saturation", player.getFoodData().getSaturationLevel());
        backup.putInt("experience_level", player.experienceLevel);
        backup.putInt("experience_total", player.totalExperience);
        backup.putFloat("experience_progress", player.experienceProgress);
        player.getPersistentData().put(BACKUP_KEY, backup);

        clearDungeon(player);
        AbilityHandler.cancel(player);
        ShadowHandler.dismissAll(player);
        HunterData.resetAllProgression(player);
        HunterData.awaken(player);
        HunterData.setLevel(player, 40);
        CompoundTag tag = HunterData.mutable(player);
        for (String stat : HunterData.PRIMARY_STATS) tag.putInt(stat, 35);
        for (String skill : List.of("stealth", "bloodlust", "quicksilver", "mutilation", "dagger_rush",
                "rulers_authority", "dragons_fear", "shadow_extraction", "shadow_preservation",
                "shadow_exchange", "monarch_domain")) tag.putBoolean("skill_" + skill, true);
        tag.putInt("gold", 25_000);
        tag.putInt("stat_points", 25);
        tag.putInt("skill_evolution_tokens", 2);
        HunterData.clearCooldowns(player);
        QuestHandler.resetDaily(player);

        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.KNIGHT_KILLER.get()));
        player.getInventory().add(new ItemStack(ModItems.GREATER_HEALING_POTION.get(), 4));
        player.getInventory().add(new ItemStack(ModItems.GREATER_MANA_POTION.get(), 4));
        player.getInventory().armor.set(3, new ItemStack(ModItems.NOVICE_HUNTER_HOOD.get()));
        player.getInventory().armor.set(2, new ItemStack(ModItems.NOVICE_HUNTER_JACKET.get()));
        player.getInventory().armor.set(1, new ItemStack(ModItems.NOVICE_HUNTER_LEGGINGS.get()));
        player.getInventory().armor.set(0, new ItemStack(ModItems.NOVICE_HUNTER_BOOTS.get()));
        HunterData.recalculateAttributes(player);
        tag.putInt("mana", HunterData.getMaxMana(player));
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        HunterData.sync(player);
        source.sendSuccess(() -> Component.literal("[TEST] Controlled level-40 fixture loaded; original player data and inventory preserved."), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        if (!player.getPersistentData().contains(BACKUP_KEY)) {
            source.sendFailure(Component.literal("No Phase 2 test backup exists."));
            return 0;
        }
        CompoundTag backup = player.getPersistentData().getCompound(BACKUP_KEY).copy();
        clearDungeon(player);
        AbilityHandler.cancel(player);
        ShadowHandler.dismissAll(player);
        HunterData.replace(player, backup.getCompound("hunter"));
        ShadowHandler.reconcile(player);
        player.getInventory().clearContent();
        player.getInventory().load(backup.getList("inventory", net.minecraft.nbt.Tag.TAG_COMPOUND));
        player.getInventory().selected = Math.max(0, Math.min(8, backup.getInt("selected")));
        player.experienceLevel = backup.getInt("experience_level");
        player.totalExperience = backup.getInt("experience_total");
        player.experienceProgress = backup.getFloat("experience_progress");
        player.getFoodData().setFoodLevel(backup.getInt("food"));
        player.getFoodData().setSaturation(backup.getFloat("saturation"));
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(1.0F, backup.getFloat("health"))));
        player.getPersistentData().remove(BACKUP_KEY);
        HunterData.recalculateAttributes(player);
        HunterData.sync(player);
        source.sendSuccess(() -> Component.literal("[TEST] Original player data and inventory restored."), true);
        return 1;
    }

    private static int progression(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        boolean pass = HunterData.getLevel(player) >= 1 && HunterData.getXp(player) >= 0
                && HunterData.getMana(player) >= 0 && HunterData.getMana(player) <= HunterData.getMaxMana(player)
                && HunterData.getGold(player) >= 0 && HunterData.getStatPoints(player) >= 0;
        return result(source, "progression", pass, "dataVersion=" + tag.getInt("data_version")
                + ", level=" + HunterData.getLevel(player) + ", rank=" + HunterData.getRank(player)
                + ", pendingGrowth=" + tag.getInt("pending_growth_choices"));
    }

    private static void clearDungeon(ServerPlayer player) {
        if (DungeonRuntime.findSession(player.server, player.getUUID()) == null) return;
        DungeonRuntime.exit(player);
        if (DungeonRuntime.findSession(player.server, player.getUUID()) != null) DungeonRuntime.deleteCurrent(player);
    }

    private static int combat(CommandSourceStack source, ServerPlayer player) {
        List<AbilityCooldownView.Entry> abilities = AbilityHandler.cooldownData(player);
        long locked = abilities.stream().filter(entry -> !entry.unlocked()).count();
        return result(source, "combat", !abilities.isEmpty(), "abilities=" + abilities.size()
                + ", locked=" + locked + ", health=" + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()));
    }

    private static int dungeon(CommandSourceStack source, ServerPlayer player) {
        DungeonSession session = DungeonRuntime.findSession(player.server, player.getUUID());
        if (session == null) return result(source, "dungeon", true, "no active session; use /sl dungeon enter master for lifecycle testing");
        String details = DungeonRuntime.inspect(player.server, session);
        boolean pass = !session.arenaBuilt() || DungeonRuntime.validateCurrent(player).success();
        return result(source, "dungeon", pass, details);
    }

    private static int shadows(CommandSourceStack source, ServerPlayer player) {
        int stored = ShadowStorage.size(player), capacity = ShadowStorage.capacity(player);
        int active = ShadowSummoningService.activeCount(player), limit = ShadowSummoningService.activeLimit(player);
        return result(source, "shadows", stored <= capacity && active <= limit,
                "stored=" + stored + "/" + capacity + ", active=" + active + "/" + limit);
    }

    private static int multiplayer(CommandSourceStack source, ServerPlayer player) {
        int online = player.server.getPlayerCount();
        DungeonSession session = DungeonRuntime.findSession(player.server, player.getUUID());
        return result(source, "multiplayer", true, "online=" + online + ", partyMembers="
                + (session == null ? 0 : session.members().size()) + (online < 2 ? "; second client required for runtime acceptance" : ""));
    }

    private static int toggleOverlay(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        boolean enabled = !tag.getBoolean("debug_overlay");
        tag.putBoolean("debug_overlay", enabled);
        HunterData.sync(player);
        source.sendSuccess(() -> Component.literal("Developer overlay " + (enabled ? "enabled" : "disabled")), false);
        return 1;
    }

    private static int player(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        source.sendSuccess(() -> Component.literal("[DEBUG] " + player.getScoreboardName() + " Lv" + HunterData.getLevel(player)
                + " " + HunterData.getRank(player) + " mana=" + HunterData.getMana(player) + "/" + HunterData.getMaxMana(player)
                + " gold=" + HunterData.getGold(player) + " stats=" + java.util.Arrays.stream(HunterData.PRIMARY_STATS)
                .map(stat -> stat + ":" + HunterData.getStat(player, stat)).collect(Collectors.joining(","))), false);
        source.sendSuccess(() -> Component.literal("[DEBUG] pos=" + player.blockPosition().toShortString() + " dimension="
                + player.level().dimension().location() + " overlay=" + tag.getBoolean("debug_overlay")), false);
        return 1;
    }

    private static int packets(CommandSourceStack source, ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        source.sendSuccess(() -> Component.literal("[DEBUG] packets received=" + tag.getLong("debug_packets_received")
                + " rejected=" + tag.getLong("debug_packets_rejected") + " currentWindow=" + tag.getInt("packet_window_count") + "/20"), false);
        return 1;
    }

    private static int entities(CommandSourceStack source, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(64.0D);
        int nearby = player.serverLevel().getEntities(player, area).size();
        DungeonSession session = DungeonRuntime.findSession(player.server, player.getUUID());
        source.sendSuccess(() -> Component.literal("[DEBUG] entities within 64 blocks=" + nearby + ", dungeonTracked="
                + (session == null ? 0 : session.trackedEntities().size()) + ", activeShadows="
                + ShadowSummoningService.activeCount(player)), false);
        return 1;
    }

    private static int performance(CommandSourceStack source, ServerPlayer player) {
        DungeonSession session = DungeonRuntime.findSession(player.server, player.getUUID());
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[DEBUG] serverTick latest=%.2fms rolling=%.2fms; dungeonBuild=%s", latestServerTickMs,
                rollingServerTickMs, session == null ? "none" : session.pendingArenaJob() + " "
                        + session.generationChangedBlocks() + " changes/" + session.generationTicks() + " ticks")), false);
        return 1;
    }

    public static void appendSnapshot(ServerPlayer player, CompoundTag snapshot) {
        CompoundTag raw = HunterData.mutable(player);
        if (!raw.getBoolean("debug_overlay")) return;
        snapshot.putDouble("debug_tick_ms", latestServerTickMs);
        snapshot.putDouble("debug_tick_rolling_ms", rollingServerTickMs);
        snapshot.putString("debug_dimension", player.level().dimension().location().toString());
        snapshot.putString("debug_position", player.blockPosition().toShortString());
        snapshot.putInt("debug_nearby_entities", player.serverLevel().getEntities(player,
                player.getBoundingBox().inflate(64.0D)).size());
        snapshot.putInt("debug_stored_shadows", ShadowStorage.size(player));
        snapshot.putInt("debug_active_shadows", ShadowSummoningService.activeCount(player));
        snapshot.putString("debug_stats", java.util.Arrays.stream(HunterData.PRIMARY_STATS)
                .map(stat -> stat.substring(0, 3).toUpperCase(Locale.ROOT) + "=" + HunterData.getStat(player, stat))
                .collect(Collectors.joining(" ")));
        snapshot.putString("debug_active_abilities", activeAbilities(raw));
        String cooldowns = AbilityHandler.cooldownData(player).stream().filter(entry -> entry.remainingTicks() > 0)
                .limit(5).map(entry -> entry.id() + ":" + entry.remainingTicks()).collect(Collectors.joining(" "));
        snapshot.putString("debug_cooldowns", cooldowns.isBlank() ? "none" : cooldowns);
        DungeonSession session = DungeonRuntime.findSession(player.server, player.getUUID());
        if (session != null) {
            snapshot.putString("debug_session_id", session.sessionId().toString().substring(0, 8));
            snapshot.putInt("debug_dungeon_entities", session.trackedEntities().size());
            snapshot.putString("debug_generation", session.pendingArenaJob().isBlank() ? "idle"
                    : session.pendingArenaJob() + " max=" + session.generationMaxChangedPerTick());
            snapshot.putString("debug_dungeon_location", MasterDungeonBuilder.location(
                    player.blockPosition().subtract(session.arenaOrigin())));
        }
    }

    private static String activeAbilities(CompoundTag tag) {
        java.util.ArrayList<String> active = new java.util.ArrayList<>();
        if (tag.getBoolean("stealth_active")) active.add("stealth");
        if (tag.getBoolean("monarch_domain")) active.add("domain");
        if (tag.getInt("ability_mutilation_hits") > 0) active.add("mutilation:" + tag.getInt("ability_mutilation_hits"));
        if (tag.getLong("authority_flight_until") > 0L) active.add("flight");
        if (!tag.getString("authority_held").isBlank()) active.add("authority-hold");
        if (tag.getLong("ability_quicksilver_until") > 0L) active.add("quicksilver");
        if (tag.getLong("ability_bloodlust_until") > 0L) active.add("bloodlust");
        return active.isEmpty() ? "none" : String.join(" ", active);
    }

    private static int result(CommandSourceStack source, String suite, boolean pass, String detail) {
        Component message = Component.literal("[TEST " + (pass ? "PASS" : "FAIL") + "] " + suite + " - " + detail)
                .withStyle(pass ? ChatFormatting.GREEN : ChatFormatting.RED);
        if (pass) source.sendSuccess(() -> message, false); else source.sendFailure(message);
        return pass ? 1 : 0;
    }

    private DebugInfrastructure() { }
}
