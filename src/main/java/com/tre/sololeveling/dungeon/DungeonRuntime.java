package com.tre.sololeveling.dungeon;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DungeonRuntime {
    public record Result(boolean success, String message) {
        public static Result ok(String message) { return new Result(true, message); }
        public static Result fail(String message) { return new Result(false, message); }
    }

    private static final Map<UUID, Integer> MISSING_BOSS_TICKS = new HashMap<>();
    private static final String PLAYER_RETURN_TAG = "sl_dungeon_return";

    public static Result createGate(ServerPlayer creator, String rawId, DungeonTypes.GateRank rank, String templateId) {
        String gateId = DungeonTypes.id(rawId);
        DungeonTypes.DungeonTemplate template = DungeonContent.template(templateId);
        if (template == null) return Result.fail("Unknown dungeon template: " + templateId);
        DungeonSavedData data = DungeonSavedData.get(creator.server);
        if (data.gates().containsKey(gateId)) return Result.fail("Gate already exists: " + gateId);
        BlockPos position = creator.blockPosition().relative(creator.getDirection(), 4);
        DungeonTypes.GateDefinition gate = new DungeonTypes.GateDefinition(gateId, rank, template.id(), creator.level().dimension(),
                position, Math.max(rank.minimumLevel(), template.rank().minimumLevel()), creator.getUUID());
        data.gates().put(gateId, gate);
        data.setDirty();
        DungeonArena.placeGateMarker(creator.serverLevel(), gate);
        return Result.ok("Created " + rank.name() + " gate " + gateId + " for " + template.displayName());
    }

    public static Result removeGate(MinecraftServer server, String rawId) {
        DungeonSavedData data = DungeonSavedData.get(server);
        DungeonTypes.GateDefinition gate = data.gates().remove(DungeonTypes.id(rawId));
        if (gate == null) return Result.fail("Gate not found");
        ServerLevel level = server.getLevel(gate.dimension());
        if (level != null) DungeonArena.removeGateMarker(level, gate);
        data.setDirty();
        return Result.ok("Removed gate " + gate.gateId());
    }

    public static Result enterGate(ServerPlayer owner, String gateId) {
        return enterGate(owner, gateId, List.of(owner));
    }

    public static Result enterGate(ServerPlayer owner, String rawGateId, Collection<ServerPlayer> requestedParty) {
        MinecraftServer server = owner.server;
        DungeonSavedData data = DungeonSavedData.get(server);
        DungeonTypes.GateDefinition gate = data.gates().get(DungeonTypes.id(rawGateId));
        if (gate == null) return Result.fail("Gate not found");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(gate.templateId());
        if (template == null) return Result.fail("Gate template is unavailable");
        if (!owner.level().dimension().equals(gate.dimension()) || owner.distanceToSqr(gate.position().getX() + 0.5D, gate.position().getY() + 1.0D, gate.position().getZ() + 0.5D) > 144.0D) {
            return Result.fail("Move within 12 blocks of the gate");
        }
        List<ServerPlayer> party = requestedParty.stream().distinct().limit(8).toList();
        if (party.isEmpty() || !party.contains(owner)) party = List.of(owner);
        for (ServerPlayer member : party) {
            if (findSession(server, member.getUUID()) != null) return Result.fail(member.getScoreboardName() + " already owns or belongs to a dungeon session");
            if (HunterData.getLevel(member) < gate.minimumLevel()) return Result.fail(member.getScoreboardName() + " requires level " + gate.minimumLevel());
            String denial = DungeonHooks.evaluateAccess(member, gate, template);
            if (!denial.isBlank()) return Result.fail(member.getScoreboardName() + ": " + denial);
        }
        int slot = data.allocateArenaSlot();
        DungeonSession session = new DungeonSession(data.nextSessionId(), gate.gateId(), template.id(), owner.getUUID(),
                party.stream().map(ServerPlayer::getUUID).toList(), template.totalTimeTicks(), slot);
        ServerLevel dungeonLevel = server.overworld();
        session.setDungeonLocation(Level.OVERWORLD, DungeonArena.originForSlot(slot));
        session.setLastActiveGameTime(dungeonLevel.getGameTime());
        DungeonArena.build(dungeonLevel, session);
        BlockPos entry = DungeonArena.entryPoint(session);
        for (ServerPlayer member : party) {
            DungeonTypes.ReturnPoint returnPoint = new DungeonTypes.ReturnPoint(member.level().dimension(), member.getX(), member.getY(), member.getZ(), member.getYRot(), member.getXRot());
            session.returnPoints().put(member.getUUID(), returnPoint);
            CompoundTag recovery = returnPoint.save(member.getUUID());
            recovery.putUUID("session_id", session.sessionId());
            member.getPersistentData().put(PLAYER_RETURN_TAG, recovery);
            member.teleportTo(dungeonLevel, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, 0.0F, 0.0F);
            member.sendSystemMessage(Component.literal("[GATE] Entered " + template.displayName() + ". Use /sl dungeon start when ready.").withStyle(ChatFormatting.LIGHT_PURPLE));
            DungeonHooks.post(new DungeonHooks.GateEnteredEvent(member, session));
        }
        data.sessions().put(session.sessionId(), session);
        data.setDirty();
        return Result.ok("Entered session " + shortId(session.sessionId()));
    }

    public static Result start(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("You are not in a dungeon session");
        if (!session.owner().equals(player.getUUID())) return Result.fail("Only the session owner can start the dungeon");
        if (session.state() != DungeonTypes.SessionState.WAITING) return Result.fail("Session is not waiting to start");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        if (template == null) return Result.fail("Dungeon template is unavailable");
        session.setState(DungeonTypes.SessionState.ACTIVE);
        session.setRemainingTicks(template.totalTimeTicks());
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        session.setObjectiveTicksRemaining(objective == null ? 20 : objective.timeLimitTicks());
        session.setLastActiveGameTime(player.serverLevel().getGameTime());
        DungeonSavedData.get(player.server).setDirty();
        broadcast(player.server, session, "[DUNGEON] Started: " + template.displayName(), ChatFormatting.RED);
        announceObjective(player.server, session, objective);
        DungeonHooks.post(new DungeonHooks.SessionStartedEvent(session));
        return Result.ok("Dungeon started");
    }

    public static void tick(MinecraftServer server) {
        DungeonSavedData data = DungeonSavedData.get(server);
        long now = server.overworld().getGameTime();
        for (DungeonSession session : new ArrayList<>(data.sessions().values())) {
            if (session.isTerminal()) {
                if (session.cleanupAfterGameTime() <= now) cleanup(server, session, true);
                continue;
            }
            DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
            if (template == null) { fail(server, session, "Dungeon template was removed"); continue; }
            boolean memberPresent = hasMemberInArena(server, session);
            if (memberPresent) session.setLastActiveGameTime(now);
            if (session.state() == DungeonTypes.SessionState.WAITING) {
                if (!memberPresent && now - session.lastActiveGameTime() > 20L * 60L * 5L) fail(server, session, "Preparation session expired");
                continue;
            }
            if (!memberPresent && now - session.lastActiveGameTime() > 20L * 60L) {
                fail(server, session, "All hunters left the dungeon");
                continue;
            }
            session.tickTimers();
            if (session.remainingTicks() <= 0) { fail(server, session, "Dungeon timer expired"); continue; }
            if (session.objectiveTicksRemaining() <= 0) { fail(server, session, "Objective timer expired"); continue; }
            tickObjective(server, data, session, template);
        }
        if (now % 20L == 0L) data.setDirty();
    }

    private static void tickObjective(MinecraftServer server, DungeonSavedData data, DungeonSession session, DungeonTypes.DungeonTemplate template) {
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        if (objective == null) { complete(server, session); return; }
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) { fail(server, session, "Dungeon dimension unavailable"); return; }
        if (!session.encounterSpawned()) {
            switch (objective.type()) {
                case WAVE, COLLECTION, ELITE -> {
                    DungeonTypes.WaveDefinition wave = template.waves().get(objective.encounterId());
                    int spawned = DungeonEnemies.spawnWave(level, session, wave, template.rank());
                    if (spawned < objective.target()) { fail(server, session, "Encounter could not spawn within hard limits"); return; }
                    session.setEncounterSpawned(true);
                    level.playSound(null, session.arenaOrigin(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
                }
                case BOSS -> {
                    if (DungeonBoss.spawn(level, session, template.rank()) == null) { fail(server, session, "Boss could not spawn"); return; }
                    session.setEncounterSpawned(true);
                }
                case REWARD -> {
                    if (!session.rewardRoomCreated()) DungeonArena.buildRewardRoom(level, session);
                    session.setEncounterSpawned(true);
                    broadcast(server, session, "[DUNGEON] The reward room is open.", ChatFormatting.GOLD);
                }
            }
            data.setDirty();
        }
        if (objective.type() == DungeonTypes.ObjectiveType.COLLECTION && level.getGameTime() % 4L == 0L) collectTokens(server, session, objective);
        if (objective.type() == DungeonTypes.ObjectiveType.BOSS) {
            if (DungeonBoss.tick(server, session, template.rank()) == DungeonBoss.TickResult.MISSING) {
                int missing = MISSING_BOSS_TICKS.merge(session.sessionId(), 1, Integer::sum);
                if (missing > 100) fail(server, session, "Boss encounter was lost during recovery");
            } else MISSING_BOSS_TICKS.remove(session.sessionId());
        }
        if (objective.type() == DungeonTypes.ObjectiveType.REWARD) {
            BlockPos center = DungeonArena.rewardCenter(session);
            for (UUID memberId : session.members()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member != null && member.level() == level && member.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D) <= 25.0D) {
                    completeObjective(server, session);
                    break;
                }
            }
        }
    }

    private static void collectTokens(MinecraftServer server, DungeonSession session, DungeonTypes.ObjectiveDefinition objective) {
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) return;
        AABB bounds = DungeonArena.bounds(session);
        List<ItemEntity> tokens = level.getEntitiesOfClass(ItemEntity.class, bounds, item ->
                item.getPersistentData().getBoolean(DungeonTypes.TAG_COLLECTION_ITEM)
                        && item.getPersistentData().hasUUID(DungeonTypes.TAG_SESSION)
                        && session.sessionId().equals(item.getPersistentData().getUUID(DungeonTypes.TAG_SESSION)));
        for (ItemEntity token : tokens) {
            boolean collected = false;
            for (UUID memberId : session.members()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member != null && member.level() == level && member.distanceToSqr(token) <= 9.0D) { collected = true; break; }
            }
            if (collected) {
                token.discard();
                session.addObjectiveProgress(1);
                if (session.objectiveProgress() >= objective.target()) { completeObjective(server, session); return; }
            }
        }
    }

    public static void onEnemyDeath(LivingEntity enemy, Entity sourceEntity) {
        if (!DungeonEnemies.isDungeonEnemy(enemy) || !(enemy.level() instanceof ServerLevel level)) return;
        UUID sessionId = DungeonEnemies.sessionId(enemy);
        if (sessionId == null) return;
        DungeonSavedData data = DungeonSavedData.get(level.getServer());
        DungeonSession session = data.sessions().get(sessionId);
        if (session == null) return;
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        session.entityRemoved(enemy.getUUID());
        ServerPlayer credited = null;
        if (sourceEntity instanceof ServerPlayer sourcePlayer) credited = sourcePlayer;
        else if (enemy.getKillCredit() instanceof ServerPlayer lastPlayer) credited = lastPlayer;
        DungeonHooks.post(new DungeonHooks.EnemyDefeatedEvent(session, credited, enemy, DungeonEnemies.enemyId(enemy), DungeonEnemies.shadowExtractable(enemy)));
        if (objective != null && objective.type() == DungeonTypes.ObjectiveType.COLLECTION) DungeonEnemies.dropCollectionToken(level, enemy);
        if (DungeonBoss.isBoss(enemy)) {
            DungeonBoss.onDeath(session);
            if (objective != null && objective.type() == DungeonTypes.ObjectiveType.BOSS) completeObjective(level.getServer(), session);
        } else if (objective != null && (objective.type() == DungeonTypes.ObjectiveType.WAVE || objective.type() == DungeonTypes.ObjectiveType.ELITE)) {
            session.addObjectiveProgress(1);
            if (session.objectiveProgress() >= objective.target()) completeObjective(level.getServer(), session);
        }
        data.setDirty();
    }

    public static Result forceCompleteObjective(MinecraftServer server, UUID sessionId) {
        DungeonSession session = DungeonSavedData.get(server).sessions().get(sessionId);
        if (session == null) return Result.fail("Session not found");
        completeObjective(server, session);
        return Result.ok("Objective completed");
    }

    private static void completeObjective(MinecraftServer server, DungeonSession session) {
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        if (objective == null || session.isTerminal()) return;
        DungeonHooks.post(new DungeonHooks.ObjectiveCompletedEvent(session, objective.id()));
        broadcast(server, session, "[OBJECTIVE COMPLETE] " + objective.displayName(), ChatFormatting.GREEN);
        if (objective.type() == DungeonTypes.ObjectiveType.REWARD) { complete(server, session); return; }
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level != null) DungeonArena.discardSessionEntities(level, session);
        DungeonBoss.remove(session);
        MISSING_BOSS_TICKS.remove(session.sessionId());
        session.advanceObjective();
        DungeonTypes.ObjectiveDefinition next = session.currentObjective(template);
        if (next == null) complete(server, session);
        else {
            session.setObjectiveTicksRemaining(next.timeLimitTicks());
            announceObjective(server, session, next);
        }
        DungeonSavedData.get(server).setDirty();
    }

    public static void complete(MinecraftServer server, DungeonSession session) {
        if (session.isTerminal()) return;
        DungeonSavedData data = DungeonSavedData.get(server);
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        session.setState(DungeonTypes.SessionState.COMPLETED);
        session.setCleanupAfterGameTime(server.overworld().getGameTime() + 20L * 20L);
        if (!session.rewardGranted() && template != null) {
            session.setRewardGranted(true);
            data.setDirty();
            for (UUID memberId : session.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(memberId);
                if (player != null) grantReward(player, session, template);
            }
        }
        broadcast(server, session, "[DUNGEON CLEAR] Rewards granted. Exit is available for 20 seconds.", ChatFormatting.GOLD);
        DungeonHooks.post(new DungeonHooks.DungeonCompletedEvent(session));
        data.setDirty();
    }

    private static void grantReward(ServerPlayer player, DungeonSession session, DungeonTypes.DungeonTemplate template) {
        DungeonTypes.RewardDefinition reward = template.reward();
        HunterData.addXp(player, reward.xp());
        HunterData.addGold(player, reward.gold());
        for (DungeonTypes.ItemReward itemReward : reward.items()) {
            Item item = ForgeRegistries.ITEMS.getValue(itemReward.itemId());
            if (item == null || item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item, itemReward.count());
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        HunterData.sync(player);
        try { DungeonHooks.grantIntegrationRewards(player, session, template); } catch (RuntimeException ignored) {}
        DungeonHooks.post(new DungeonHooks.RewardGrantedEvent(session, player, reward.xp(), reward.gold()));
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 50, 0.8D, 1.0D, 0.8D, 0.15D);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static Result fail(MinecraftServer server, UUID sessionId, String reason) {
        DungeonSession session = DungeonSavedData.get(server).sessions().get(sessionId);
        if (session == null) return Result.fail("Session not found");
        fail(server, session, reason);
        return Result.ok("Dungeon failed");
    }

    private static void fail(MinecraftServer server, DungeonSession session, String reason) {
        if (session.isTerminal()) return;
        session.setState(DungeonTypes.SessionState.FAILED);
        session.setFailureReason(reason);
        session.setCleanupAfterGameTime(server.overworld().getGameTime() + 100L);
        DungeonBoss.remove(session);
        MISSING_BOSS_TICKS.remove(session.sessionId());
        broadcast(server, session, "[DUNGEON FAILED] " + reason, ChatFormatting.RED);
        DungeonHooks.post(new DungeonHooks.DungeonFailedEvent(session, reason));
        DungeonSavedData.get(server).setDirty();
    }

    public static Result exit(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("You are not assigned to a dungeon session");
        returnPlayer(player.server, session, player);
        if (!session.isTerminal() && !hasMemberInArena(player.server, session)) fail(player.server, session, "All hunters exited");
        return Result.ok("Returned safely from the dungeon");
    }

    public static void onPlayerDeath(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session != null && !session.isTerminal()) fail(player.server, session, player.getScoreboardName() + " was defeated");
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session != null && session.isTerminal()) returnPlayer(player.server, session, player);
    }

    public static void recoverPlayer(ServerPlayer player) {
        if (!player.getPersistentData().contains(PLAYER_RETURN_TAG)) return;
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session != null && !session.isTerminal()) {
            ServerLevel level = player.server.getLevel(session.dungeonDimension());
            if (level != null && (player.level() != level || !DungeonArena.bounds(session).contains(player.position()))) {
                BlockPos entry = DungeonArena.entryPoint(session);
                player.teleportTo(level, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, 0.0F, 0.0F);
            }
            return;
        }
        CompoundTag recovery = player.getPersistentData().getCompound(PLAYER_RETURN_TAG);
        Map.Entry<UUID, DungeonTypes.ReturnPoint> entry = DungeonTypes.ReturnPoint.load(recovery);
        teleportToPoint(player.server, player, entry.getValue());
        player.getPersistentData().remove(PLAYER_RETURN_TAG);
    }

    public static Result clearAll(MinecraftServer server) {
        DungeonSavedData data = DungeonSavedData.get(server);
        int count = data.sessions().size();
        for (DungeonSession session : new ArrayList<>(data.sessions().values())) cleanup(server, session, true);
        data.setDirty();
        return Result.ok("Cleared " + count + " dungeon sessions");
    }

    public static Result spawnWave(ServerPlayer player, String waveId) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter a dungeon session first");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        DungeonTypes.WaveDefinition wave = template == null ? null : template.waves().get(DungeonTypes.id(waveId));
        if (wave == null) return Result.fail("Wave not found in this template");
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        int count = level == null ? 0 : DungeonEnemies.spawnWave(level, session, wave, template.rank());
        DungeonSavedData.get(player.server).setDirty();
        return count > 0 ? Result.ok("Spawned " + count + " enemies") : Result.fail("Wave spawn blocked by hard limits");
    }

    public static Result spawnTestEnemy(ServerPlayer player, String enemyId) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter a dungeon session first");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        LivingEntity entity = level == null || template == null ? null : DungeonEnemies.spawn(level, session, enemyId, player.blockPosition().offset(3, 0, 0), template.rank(), false);
        DungeonSavedData.get(player.server).setDirty();
        return entity == null ? Result.fail("Enemy spawn failed") : Result.ok("Spawned test enemy " + DungeonTypes.id(enemyId));
    }

    public static Result spawnTestBoss(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter a dungeon session first");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        LivingEntity boss = level == null || template == null ? null : DungeonBoss.spawn(level, session, template.rank());
        DungeonSavedData.get(player.server).setDirty();
        return boss == null ? Result.fail("Boss spawn failed") : Result.ok("Spawned the Iron Sovereign");
    }

    public static DungeonSession findSession(MinecraftServer server, UUID playerId) {
        for (DungeonSession session : DungeonSavedData.get(server).sessions().values()) if (session.contains(playerId)) return session;
        return null;
    }

    public static String inspect(MinecraftServer server, DungeonSession session) {
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        String objectiveText = objective == null ? "none" : objective.id() + " " + session.objectiveProgress() + "/" + objective.target();
        return "Session " + shortId(session.sessionId()) + " | gate=" + session.gateId() + " | template=" + session.templateId()
                + " | state=" + session.state() + " | objective=" + objectiveText + " | time=" + session.remainingTicks() / 20
                + "s | objectiveTime=" + session.objectiveTicksRemaining() / 20 + "s | live=" + session.liveEnemyCount()
                + " | spawned=" + session.totalSpawns() + " | rewarded=" + session.rewardGranted();
    }

    private static void cleanup(MinecraftServer server, DungeonSession session, boolean removeRecord) {
        ServerLevel level = server.getLevel(session.dungeonDimension());
        for (UUID memberId : session.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) returnPlayer(server, session, player);
        }
        if (level != null) {
            DungeonArena.discardSessionEntities(level, session);
            DungeonArena.clear(level, session);
        }
        DungeonBoss.remove(session);
        MISSING_BOSS_TICKS.remove(session.sessionId());
        session.setState(DungeonTypes.SessionState.CLEANUP);
        DungeonSavedData data = DungeonSavedData.get(server);
        if (removeRecord) data.sessions().remove(session.sessionId());
        data.setDirty();
    }

    private static void returnPlayer(MinecraftServer server, DungeonSession session, ServerPlayer player) {
        DungeonTypes.ReturnPoint point = session.returnPoints().get(player.getUUID());
        if (point == null && player.getPersistentData().contains(PLAYER_RETURN_TAG)) {
            point = DungeonTypes.ReturnPoint.load(player.getPersistentData().getCompound(PLAYER_RETURN_TAG)).getValue();
        }
        teleportToPoint(server, player, point);
        player.getPersistentData().remove(PLAYER_RETURN_TAG);
    }

    private static void teleportToPoint(MinecraftServer server, ServerPlayer player, DungeonTypes.ReturnPoint point) {
        ServerLevel target = point == null ? server.overworld() : server.getLevel(point.dimension());
        if (target == null) target = server.overworld();
        if (point == null) {
            BlockPos spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, target.getSharedSpawnAngle(), 0.0F);
        } else player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
    }

    private static boolean hasMemberInArena(MinecraftServer server, DungeonSession session) {
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) return false;
        AABB bounds = DungeonArena.bounds(session);
        for (UUID memberId : session.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null && player.level() == level && bounds.contains(player.position())) return true;
        }
        return false;
    }

    private static void announceObjective(MinecraftServer server, DungeonSession session, DungeonTypes.ObjectiveDefinition objective) {
        if (objective != null) broadcast(server, session, "[OBJECTIVE] " + objective.displayName() + " (" + objective.target() + ")", ChatFormatting.AQUA);
    }

    private static void broadcast(MinecraftServer server, DungeonSession session, String message, ChatFormatting color) {
        for (UUID memberId : session.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) player.sendSystemMessage(Component.literal(message).withStyle(color));
        }
    }

    private static String shortId(UUID id) { return id.toString().substring(0, 8); }

    private DungeonRuntime() {}
}
