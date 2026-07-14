package com.tre.sololeveling.dungeon;

import com.mojang.logging.LogUtils;
import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import org.slf4j.Logger;

public final class DungeonRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    public record Result(boolean success, String message) {
        public static Result ok(String message) { return new Result(true, message); }
        public static Result fail(String message) { return new Result(false, message); }
    }

    public static final int ARENA_LAYOUT_VERSION = 6;

    private static final Map<UUID, Integer> MISSING_BOSS_TICKS = new HashMap<>();
    private static final String PLAYER_RETURN_TAG = "sl_dungeon_return";

    public static Result createTemplateGate(ServerPlayer creator, String templateId) {
        DungeonTypes.DungeonTemplate template = DungeonContent.template(templateId);
        if (template == null) return Result.fail("Unknown dungeon template: " + templateId);
        String gateId = template.id() + "_" + Long.toString(creator.level().getGameTime(), 36);
        return createGate(creator, gateId, template.rank(), template.id());
    }

    public static Result createGate(ServerPlayer creator, String rawId, DungeonTypes.GateRank rank, String templateId) {
        String gateId = DungeonTypes.id(rawId);
        DungeonTypes.DungeonTemplate template = DungeonContent.template(templateId);
        if (template == null) return Result.fail("Unknown dungeon template: " + templateId);
        DungeonSavedData data = DungeonSavedData.get(creator.server);
        if (data.gates().containsKey(gateId)) return Result.fail("Gate already exists: " + gateId);
        BlockPos position = creator.blockPosition().relative(creator.getDirection(), 4);
        DungeonTypes.GateDefinition gate = new DungeonTypes.GateDefinition(gateId, rank, template.id(),
                creator.level().dimension(), position,
                Math.max(rank.minimumLevel(), template.rank().minimumLevel()), creator.getUUID());
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
        if (!isNearGate(owner, gate, 12.0D)) return Result.fail("Move within 12 blocks of the gate");

        List<ServerPlayer> party = requestedParty.stream().distinct().limit(8).toList();
        if (party.isEmpty() || !party.contains(owner)) party = List.of(owner);
        for (ServerPlayer member : party) {
            if (!isNearGate(member, gate, 16.0D)) {
                return Result.fail(member.getScoreboardName() + " must be near the gate");
            }
            if (findSession(server, member.getUUID()) != null) {
                return Result.fail(member.getScoreboardName() + " already owns or belongs to a dungeon session");
            }
            if (HunterData.getLevel(member) < gate.minimumLevel()
                    && !com.tre.sololeveling.gameplay.RankTrialService.permitsEvaluationEntry(member)) {
                return Result.fail(member.getScoreboardName() + " requires level " + gate.minimumLevel());
            }
            String denial = DungeonHooks.evaluateAccess(member, gate, template);
            if (!denial.isBlank()) return Result.fail(member.getScoreboardName() + ": " + denial);
        }

        int slot = data.allocateArenaSlot();
        if (slot < 0) return Result.fail("No dungeon arena slot is currently available");
        DungeonSession session = new DungeonSession(data.nextSessionId(), gate.gateId(), template.id(), owner.getUUID(),
                party.stream().map(ServerPlayer::getUUID).toList(), template.totalTimeTicks(), slot);
        ServerLevel dungeonLevel = server.overworld();
        session.setDungeonLocation(Level.OVERWORLD, DungeonArena.originForSlot(slot));
        session.setLastActiveGameTime(dungeonLevel.getGameTime());
        session.setState(DungeonTypes.SessionState.BUILDING);
        session.setArenaBuilt(false);
        session.setPendingArenaJob("build");

        for (ServerPlayer member : party) {
            DungeonTypes.ReturnPoint returnPoint = new DungeonTypes.ReturnPoint(member.level().dimension(),
                    member.getX(), member.getY(), member.getZ(), member.getYRot(), member.getXRot());
            session.returnPoints().put(member.getUUID(), returnPoint);
            CompoundTag recovery = returnPoint.save(member.getUUID());
            recovery.putUUID("session_id", session.sessionId());
            member.getPersistentData().put(PLAYER_RETURN_TAG, recovery);
        }
        data.sessions().put(session.sessionId(), session);
        log(session, "Session created", "gate=" + gate.gateId() + ", template=" + template.id());
        if (!DungeonArena.queueBuild(session)) {
            data.sessions().remove(session.sessionId());
            data.setDirty();
            return Result.fail("Master dungeon generation could not be queued");
        }
        log(session, "Build started", "purpose=build, slot=" + slot);
        data.setDirty();
        for (ServerPlayer member : party) {
            member.sendSystemMessage(Component.literal("[GATE] Stabilizing " + template.displayName()
                    + " in bounded server-tick batches...").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return Result.ok("Queued session " + shortId(session.sessionId()) + " for staged generation");
    }

    public static Result start(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("You are not in a dungeon session");
        if (!session.owner().equals(player.getUUID())) return Result.fail("Only the session owner can start the dungeon");
        if (session.state() == DungeonTypes.SessionState.BUILDING) {
            session.setStartRequested(true);
            DungeonSavedData.get(player.server).setDirty();
            return Result.ok("Dungeon start queued until staged generation finishes");
        }
        return startSession(player.server, session);
    }

    private static Result startSession(MinecraftServer server, DungeonSession session) {
        if (session.state() != DungeonTypes.SessionState.READY) return Result.fail("Session is not ready to start");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        if (template == null) return Result.fail("Dungeon template is unavailable");
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        session.setState(stateForObjective(objective));
        session.setStartRequested(false);
        session.setRemainingTicks(template.totalTimeTicks());
        session.setObjectiveTicksRemaining(objective == null ? 20 : objective.timeLimitTicks());
        ServerLevel level = server.getLevel(session.dungeonDimension());
        session.setLastActiveGameTime(level == null ? server.overworld().getGameTime() : level.getGameTime());
        DungeonSavedData.get(server).setDirty();
        broadcast(server, session, "[DUNGEON] Started: " + template.displayName(), ChatFormatting.RED);
        announceObjective(server, session, objective);
        DungeonHooks.post(new DungeonHooks.SessionStartedEvent(session));
        return Result.ok("Dungeon started");
    }

    public static void tick(MinecraftServer server) {
        DungeonSavedData data = DungeonSavedData.get(server);
        for (DungeonArena.JobResult result : DungeonArena.tickJobs(server)) {
            handleArenaJobResult(server, data, result);
        }

        long now = server.overworld().getGameTime();
        for (DungeonSession session : new ArrayList<>(data.sessions().values())) {
            if (session.state() == DungeonTypes.SessionState.CLOSED) {
                data.sessions().remove(session.sessionId());
                data.setDirty();
                continue;
            }
            if (session.state() == DungeonTypes.SessionState.CLEANING) {
                if (!DungeonArena.hasJob(session.sessionId())) {
                    if (session.arenaBuilt()) {
                        if (!DungeonArena.queueClear(session)) {
                            session.setState(DungeonTypes.SessionState.CLOSED);
                            data.sessions().remove(session.sessionId());
                        }
                    } else {
                        session.setState(DungeonTypes.SessionState.CLOSED);
                        data.sessions().remove(session.sessionId());
                    }
                    data.setDirty();
                }
                continue;
            }
            if (session.isTerminal()) {
                if (session.cleanupAfterGameTime() <= now) cleanup(server, session, true);
                continue;
            }
            DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
            if (template == null) {
                fail(server, session, "Dungeon template was removed");
                continue;
            }
            DungeonTypes.SessionState objectiveState = stateForObjective(session.currentObjective(template));
            if ((session.state() == DungeonTypes.SessionState.ACTIVE
                    || session.state() == DungeonTypes.SessionState.BOSS)
                    && session.state() != objectiveState) {
                session.setState(objectiveState);
                data.setDirty();
            }
            if (session.state() == DungeonTypes.SessionState.BUILDING) {
                if (!DungeonArena.hasJob(session.sessionId()) && !resumeArenaJob(session)) {
                    fail(server, session, "Staged dungeon generation could not resume");
                } else if (now % 200L == 0L) {
                    DungeonArena.JobStatus status = DungeonArena.jobStatus(session.sessionId());
                    if (status != null) {
                        broadcast(server, session, "[GATE] Stabilization "
                                + Math.round(status.progress() * 100.0D) + "% (bounded to "
                                + MasterDungeonBuilder.MAX_BLOCK_CHANGES_PER_TICK + " changes/tick).",
                                ChatFormatting.DARK_PURPLE);
                    }
                }
                continue;
            }
            if (!recoverArenaVersion(server, session)) continue;
            boolean memberPresent = hasMemberInArena(server, session);
            if (memberPresent) session.setLastActiveGameTime(now);
            if (session.state() == DungeonTypes.SessionState.READY) {
                if (!memberPresent && now - session.lastActiveGameTime() > 20L * 60L * 5L) {
                    fail(server, session, "Preparation session expired");
                }
                continue;
            }
            if (!memberPresent && now - session.lastActiveGameTime() > 20L * 60L) {
                fail(server, session, "All hunters left the dungeon");
                continue;
            }
            // A short disconnect must not consume the encounter or objective clock. If at
            // least one party member remains in the arena the run continues normally.
            if (!memberPresent) continue;
            session.tickTimers();
            if (session.remainingTicks() <= 0) {
                fail(server, session, "Dungeon timer expired");
                continue;
            }
            if (session.objectiveTicksRemaining() <= 0) {
                fail(server, session, "Objective timer expired");
                continue;
            }
            tickObjective(server, data, session, template);
        }
        if (now % 20L == 0L) data.setDirty();
    }

    private static boolean resumeArenaJob(DungeonSession session) {
        return switch (session.pendingArenaJob()) {
            case "rebuild" -> DungeonArena.queueRebuild(session);
            case "upgrade" -> session.hasMigrationOrigin()
                    && DungeonArena.queueLayoutUpgrade(session, session.migrationOrigin());
            case "migration" -> session.hasMigrationOrigin()
                    && DungeonArena.queueMigration(session, session.migrationOrigin());
            default -> DungeonArena.queueBuild(session);
        };
    }

    private static void handleArenaJobResult(MinecraftServer server, DungeonSavedData data,
                                             DungeonArena.JobResult result) {
        DungeonSession session = data.sessions().get(result.sessionId());
        if (session == null) return;
        if (!result.success()) {
            if (result.purpose() == DungeonArena.JobPurpose.CLEAR) {
                session.setArenaBuilt(false);
                session.setPendingArenaJob("");
                if (session.state() == DungeonTypes.SessionState.CLEANING) {
                    session.setState(DungeonTypes.SessionState.CLOSED);
                }
                data.sessions().remove(session.sessionId());
                data.setDirty();
            } else {
                // A failed staged job may already have mutated owned module volumes. Mark the
                // arena for bounded cleanup even when preparation failed before the first visit.
                session.setArenaBuilt(true);
                fail(server, session, result.error().isBlank() ? "Dungeon generation job failed" : result.error());
            }
            return;
        }
        if (result.purpose() == DungeonArena.JobPurpose.CLEAR) {
            session.setArenaBuilt(false);
            session.setPendingArenaJob("");
            if (session.state() == DungeonTypes.SessionState.CLEANING) {
                log(session, "Cleanup completed", "arena slot released");
                session.setState(DungeonTypes.SessionState.CLOSED);
                data.sessions().remove(session.sessionId());
            }
            data.setDirty();
            return;
        }

        session.recordGeneration(result.report());
        session.setPendingArenaJob("");
        session.setMigrationOrigin(null);
        session.setArenaBuilt(true);
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) {
            fail(server, session, "Dungeon dimension unavailable after generation");
            return;
        }
        List<String> errors = DungeonArena.validate(level, session);
        if (!errors.isEmpty()) {
            fail(server, session, "Generated dungeon failed validation: "
                    + String.join("; ", errors.subList(0, Math.min(4, errors.size()))));
            return;
        }
        log(session, "Build validated", "changed=" + result.report().changedBlocks()
                + ", ticks=" + result.report().elapsedTicks());
        MasterDungeonBuilder.closeCheckpoints(level, session.arenaOrigin());
        session.setArenaVersion(ARENA_LAYOUT_VERSION);
        session.setState(DungeonTypes.SessionState.READY);
        BlockPos entry = DungeonArena.findSafePlayerPosition(level, DungeonArena.entryPoint(session), 8);
        if (entry == null) {
            fail(server, session, "Generated dungeon entry is unsafe");
            return;
        }
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        boolean teleportedMember = false;
        for (UUID memberId : session.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member == null) continue;
            member.teleportTo(level, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, 0.0F, 0.0F);
            teleportedMember = true;
            member.sendSystemMessage(Component.literal("[GATE] Entered "
                    + (template == null ? MasterDungeonBuilder.DISPLAY_NAME : template.displayName()) + ".")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            DungeonHooks.post(new DungeonHooks.GateEnteredEvent(member, session));
        }
        if (teleportedMember) log(session, "Player teleported", "party entered safe staging position");
        data.setDirty();
        if (session.startRequested() && teleportedMember) startSession(server, session);
    }

    private static boolean recoverArenaVersion(MinecraftServer server, DungeonSession session) {
        if (session.arenaVersion() >= ARENA_LAYOUT_VERSION && session.arenaBuilt()) return true;
        if (session.state() != DungeonTypes.SessionState.READY) {
            fail(server, session, "Dungeon layout changed during an active saved session");
            return false;
        }
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) {
            fail(server, session, "Dungeon dimension unavailable during recovery");
            return false;
        }
        DungeonArena.discardSessionEntities(level, session);
        BlockPos oldOrigin = session.arenaOrigin();
        boolean legacySurfaceArena = oldOrigin.getY() > 0;
        session.setDungeonLocation(Level.OVERWORLD, DungeonArena.originForSlot(session.arenaSlot()));
        session.setState(DungeonTypes.SessionState.BUILDING);
        session.setArenaBuilt(false);
        session.setPendingArenaJob(legacySurfaceArena ? "migration" : "upgrade");
        session.setMigrationOrigin(oldOrigin);
        boolean queued = legacySurfaceArena ? DungeonArena.queueMigration(session, oldOrigin)
                : DungeonArena.queueLayoutUpgrade(session, oldOrigin);
        if (!queued) {
            fail(server, session, "Master dungeon recovery could not be queued");
            return false;
        }
        broadcast(server, session, "[DUNGEON] Updating saved arena in bounded tick batches.", ChatFormatting.YELLOW);
        DungeonSavedData.get(server).setDirty();
        return false;
    }

    private static void tickObjective(MinecraftServer server, DungeonSavedData data, DungeonSession session,
                                      DungeonTypes.DungeonTemplate template) {
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        if (objective == null) {
            complete(server, session);
            return;
        }
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level == null) {
            fail(server, session, "Dungeon dimension unavailable");
            return;
        }
        if (!session.encounterSpawned()) {
            switch (objective.type()) {
                case WAVE, COLLECTION, ELITE -> {
                    DungeonTypes.WaveDefinition wave = template.waves().get(objective.encounterId());
                    int spawned = DungeonEnemies.spawnWave(level, session, wave, template.rank());
                    if (spawned < objective.target()) {
                        fail(server, session, "Encounter could not spawn within hard limits");
                        return;
                    }
                    session.setEncounterSpawned(true);
                    level.playSound(null, session.arenaOrigin(), SoundEvents.RAID_HORN.value(),
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                }
                case BOSS -> {
                    if (DungeonBoss.spawn(level, session, template.rank()) == null) {
                        fail(server, session, "Boss could not spawn");
                        return;
                    }
                    session.setEncounterSpawned(true);
                }
                case REWARD -> {
                    if (!session.rewardRoomCreated()) DungeonArena.buildRewardRoom(level, session);
                    session.setEncounterSpawned(true);
                    broadcast(server, session, "[DUNGEON] The reward room is open.", ChatFormatting.GOLD);
                }
            }
            log(session, "Encounter started", "objective=" + objective.id() + ", type=" + objective.type());
            data.setDirty();
        }
        if (objective.type() == DungeonTypes.ObjectiveType.COLLECTION && level.getGameTime() % 4L == 0L) {
            collectTokens(server, session, objective);
        }
        if (objective.type() == DungeonTypes.ObjectiveType.BOSS) {
            if (DungeonBoss.tick(server, session, template.rank()) == DungeonBoss.TickResult.MISSING) {
                int missing = MISSING_BOSS_TICKS.merge(session.sessionId(), 1, Integer::sum);
                if (missing > 100) fail(server, session, "Boss encounter was lost during recovery");
            } else {
                MISSING_BOSS_TICKS.remove(session.sessionId());
            }
        }
        if (objective.type() == DungeonTypes.ObjectiveType.REWARD) {
            BlockPos center = DungeonArena.rewardCenter(session);
            for (UUID memberId : session.members()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member != null && member.level() == level
                        && member.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D) <= 25.0D) {
                    completeObjective(server, session);
                    break;
                }
            }
        }
    }

    private static void collectTokens(MinecraftServer server, DungeonSession session,
                                      DungeonTypes.ObjectiveDefinition objective) {
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
                if (member != null && member.level() == level && member.distanceToSqr(token) <= 9.0D) {
                    collected = true;
                    break;
                }
            }
            if (collected) {
                token.discard();
                session.addObjectiveProgress(1);
                if (session.objectiveProgress() >= objective.target()) {
                    completeObjective(server, session);
                    return;
                }
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
        if (sourceEntity instanceof ServerPlayer sourcePlayer && session.contains(sourcePlayer.getUUID())) credited = sourcePlayer;
        else if (enemy.getKillCredit() instanceof ServerPlayer lastPlayer && session.contains(lastPlayer.getUUID())) credited = lastPlayer;
        DungeonHooks.post(new DungeonHooks.EnemyDefeatedEvent(session, credited, enemy,
                DungeonEnemies.enemyId(enemy), DungeonEnemies.shadowExtractable(enemy)));
        if (credited != null) {
            DungeonTypes.EnemyDefinition defeated = DungeonContent.enemy(DungeonEnemies.enemyId(enemy));
            if (defeated != null) com.tre.sololeveling.quest.QuestApi.onEnemyRole(credited,
                    defeated.kind().name().toLowerCase(java.util.Locale.ROOT));
        }
        if (objective != null && objective.type() == DungeonTypes.ObjectiveType.COLLECTION) {
            DungeonEnemies.dropCollectionToken(level, enemy);
        }
        if (DungeonBoss.isBoss(enemy)) {
            DungeonBoss.onDeath(session);
            if (objective != null && objective.type() == DungeonTypes.ObjectiveType.BOSS) {
                completeObjective(level.getServer(), session);
            }
        } else if (objective != null && (objective.type() == DungeonTypes.ObjectiveType.WAVE
                || objective.type() == DungeonTypes.ObjectiveType.ELITE)) {
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
        int completedIndex = session.objectiveIndex();
        log(session, "Objective completed", "objective=" + objective.id() + ", index=" + completedIndex);
        DungeonHooks.post(new DungeonHooks.ObjectiveCompletedEvent(session, objective.id()));
        for (UUID memberId : session.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) com.tre.sololeveling.gameplay.RankTrialService.onDungeonObjective(member);
        }
        broadcast(server, session, "[OBJECTIVE COMPLETE] " + objective.displayName(), ChatFormatting.GREEN);
        if (objective.type() == DungeonTypes.ObjectiveType.REWARD) {
            complete(server, session);
            return;
        }
        ServerLevel level = server.getLevel(session.dungeonDimension());
        if (level != null) {
            DungeonArena.discardSessionEntities(level, session);
            DungeonArena.openCheckpoint(level, session, completedIndex);
        }
        DungeonBoss.remove(session);
        MISSING_BOSS_TICKS.remove(session.sessionId());
        session.advanceObjective();
        DungeonTypes.ObjectiveDefinition next = session.currentObjective(template);
        if (next == null) complete(server, session);
        else {
            session.setState(stateForObjective(next));
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
        // Keep the completed session available long enough for a disconnected party
        // member to rejoin, receive their exactly-once reward, and recover their return
        // point. Twenty seconds made ordinary reconnects lose that recovery path.
        session.setCleanupAfterGameTime(server.overworld().getGameTime() + 20L * 60L * 5L);
        if (!session.rewardGranted() && template != null) {
            session.setRewardGranted(true);
            log(session, "Reward locked", "eligibleMembers=" + session.members().size());
            data.setDirty();
            for (UUID memberId : session.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(memberId);
                grantRewardIfPending(player, session, template, data);
            }
        }
        broadcast(server, session, "[DUNGEON CLEAR] Rewards granted. Reconnect recovery remains available for 5 minutes.", ChatFormatting.GOLD);
        DungeonHooks.post(new DungeonHooks.DungeonCompletedEvent(session));
        data.setDirty();
    }

    private static void grantRewardIfPending(ServerPlayer player, DungeonSession session,
                                             DungeonTypes.DungeonTemplate template, DungeonSavedData data) {
        if (player == null || session.rewardGrantedTo(player.getUUID())) return;
        if (!session.markRewardGranted(player.getUUID())) return;
        data.setDirty();
        grantReward(player, session, template);
    }

    private static void grantReward(ServerPlayer player, DungeonSession session,
                                    DungeonTypes.DungeonTemplate template) {
        DungeonTypes.RewardDefinition reward = template.reward();
        HunterData.addXp(player, reward.xp());
        HunterData.addGold(player, reward.gold());
        for (DungeonTypes.ItemReward itemReward : reward.items()) {
            Item item = ForgeRegistries.ITEMS.getValue(itemReward.itemId());
            if (item == null || item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item, itemReward.count());
            if (!player.getInventory().add(stack)) {
                // Dungeon loot must not depend on a temporary world drop. The persistent
                // System inventory is the loss-safe overflow path used by the rest of the mod.
                // ItemStack#copy is intentional because inventory insertion may mutate stack.
                ItemStack overflow = stack.copy();
                if (!overflow.isEmpty() && !HunterData.storeSystemItem(player, overflow)) {
                    // Both inventories being full is visible and recoverable, but never silent.
                    // Retain vanilla ownership protection as the final fallback.
                    var dropped = player.drop(overflow, false);
                    if (dropped != null) {
                        dropped.setPickUpDelay(0);
                        dropped.setExtendedLifetime();
                    }
                    player.sendSystemMessage(Component.literal(
                                    "[SYSTEM] Reward storage is full. Loot was placed at your feet.")
                            .withStyle(ChatFormatting.RED));
                } else if (!overflow.isEmpty()) {
                    player.sendSystemMessage(Component.literal(
                                    "[SYSTEM] Inventory full. Dungeon loot moved to System storage.")
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }
        com.tre.sololeveling.equipment.DungeonLootService.grantMasterDungeonRoll(player);
        HunterData.sync(player);
        try {
            DungeonHooks.grantIntegrationRewards(player, session, template);
        } catch (RuntimeException ignored) {
            // Integration rewards must not replay core rewards.
        }
        DungeonHooks.post(new DungeonHooks.RewardGrantedEvent(session, player, reward.xp(), reward.gold()));
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1.0D, player.getZ(), 50, 0.8D, 1.0D, 0.8D, 0.15D);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
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
        log(session, "Failure", reason);
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
        if (!session.isTerminal() && !hasMemberInArena(player.server, session)) {
            fail(player.server, session, "All hunters exited");
        }
        return Result.ok("Returned safely from the dungeon");
    }

    public static void onPlayerDeath(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session != null && !session.isTerminal()) {
            fail(player.server, session, player.getScoreboardName() + " was defeated");
        }
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session != null && session.isTerminal()) returnPlayer(player.server, session, player);
    }

    public static void recoverPlayer(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session != null && session.state() == DungeonTypes.SessionState.BUILDING) {
            player.sendSystemMessage(Component.literal("[GATE] Dungeon generation is still stabilizing.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (session != null && !session.isTerminal()) {
            if (!session.arenaBuilt()) return;
            ServerLevel level = player.server.getLevel(session.dungeonDimension());
            if (level == null) {
                fail(player.server, session, "Dungeon dimension unavailable during login recovery");
                return;
            }
            if (player.level() != level || !DungeonArena.bounds(session).contains(player.position())) {
                BlockPos entry = DungeonArena.findSafePlayerPosition(level, DungeonArena.entryPoint(session), 6);
                if (entry == null) {
                    fail(player.server, session, "Dungeon entry unavailable during login recovery");
                    return;
                }
                player.teleportTo(level, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D, 0.0F, 0.0F);
            }
            if (session.state() == DungeonTypes.SessionState.READY && session.startRequested()) {
                startSession(player.server, session);
            }
            return;
        }
        if (session != null && session.state() == DungeonTypes.SessionState.COMPLETED) {
            DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
            if (template != null) {
                grantRewardIfPending(player, session, template, DungeonSavedData.get(player.server));
            }
            returnPlayer(player.server, session, player);
            return;
        }
        if (!player.getPersistentData().contains(PLAYER_RETURN_TAG)) return;
        CompoundTag recovery = player.getPersistentData().getCompound(PLAYER_RETURN_TAG);
        try {
            Map.Entry<UUID, DungeonTypes.ReturnPoint> entry = DungeonTypes.ReturnPoint.load(recovery);
            teleportToPoint(player.server, player, entry.getValue());
        } catch (RuntimeException malformed) {
            teleportToPoint(player.server, player, null);
            LOGGER.warn("[DUNGEON] Recovered {} from a malformed persisted return point", player.getUUID());
        }
        player.getPersistentData().remove(PLAYER_RETURN_TAG);
    }

    public static Result clearAll(MinecraftServer server) {
        DungeonSavedData data = DungeonSavedData.get(server);
        int count = data.sessions().size();
        for (DungeonSession session : new ArrayList<>(data.sessions().values())) cleanup(server, session, true);
        data.setDirty();
        return Result.ok("Queued cleanup for " + count + " dungeon sessions");
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
        return count > 0 ? Result.ok("Spawned " + count + " enemies")
                : Result.fail("Wave spawn blocked by hard limits");
    }

    public static Result spawnTestEnemy(ServerPlayer player, String enemyId) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter a dungeon session first");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        LivingEntity entity = level == null || template == null ? null
                : DungeonEnemies.spawn(level, session, enemyId, player.blockPosition().offset(3, 0, 0),
                template.rank(), false);
        DungeonSavedData.get(player.server).setDirty();
        return entity == null ? Result.fail("Enemy spawn failed")
                : Result.ok("Spawned test enemy " + DungeonTypes.id(enemyId));
    }

    public static Result spawnTestBoss(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter a dungeon session first");
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        LivingEntity boss = level == null || template == null ? null
                : DungeonBoss.spawn(level, session, template.rank());
        DungeonSavedData.get(player.server).setDirty();
        return boss == null ? Result.fail("Boss spawn failed") : Result.ok("Spawned the template boss");
    }


    public static Result enterTemplate(ServerPlayer player, String templateId) {
        DungeonTypes.DungeonTemplate template = DungeonContent.template(templateId);
        if (template == null) return Result.fail("Unknown dungeon: " + templateId);
        String gateId = "debug_" + template.id() + "_" + Long.toString(player.level().getGameTime(), 36);
        Result created = createGate(player, gateId, template.rank(), template.id());
        if (!created.success()) return created;
        Result entered = enterGate(player, gateId);
        if (!entered.success()) return entered;
        Result started = start(player);
        return started.success() ? Result.ok("Entered " + template.displayName() + " through debug gate " + gateId) : started;
    }

    public static Result regenerate(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter the master dungeon first");
        if (session.state() == DungeonTypes.SessionState.BUILDING
                || session.state() == DungeonTypes.SessionState.CLEANING) {
            return Result.fail("A dungeon generation job is already active");
        }
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        if (level == null) return Result.fail("Dungeon dimension unavailable");
        boolean restartAfterBuild = session.state() == DungeonTypes.SessionState.ACTIVE
                || session.state() == DungeonTypes.SessionState.BOSS
                || session.state() == DungeonTypes.SessionState.REWARD;
        for (UUID memberId : session.members()) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
            if (member != null) returnPlayer(player.server, session, member);
        }
        discardEncounterState(level, session);
        DungeonArena.cancelJob(session.sessionId());
        session.setState(DungeonTypes.SessionState.BUILDING);
        session.setArenaBuilt(false);
        session.setArenaVersion(0);
        session.setStartRequested(restartAfterBuild);
        session.setPendingArenaJob("rebuild");
        session.setMigrationOrigin(null);
        if (!DungeonArena.queueRebuild(session)) return Result.fail("Master dungeon rebuild could not be queued");
        DungeonSavedData.get(player.server).setDirty();
        return Result.ok("Queued staged regeneration for " + MasterDungeonBuilder.DISPLAY_NAME);
    }

    public static Result deleteCurrent(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("No dungeon session found");
        cleanup(player.server, session, true);
        return Result.ok("Queued deletion of the active master dungeon session");
    }

    public static Result validateCurrent(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter the master dungeon first");
        if (!session.arenaBuilt()) return Result.fail("Dungeon generation is still in progress");
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        if (level == null) return Result.fail("Dungeon dimension unavailable");
        List<String> errors = DungeonArena.validate(level, session);
        return errors.isEmpty() ? Result.ok("Validation passed: " + DungeonArena.info(session))
                : Result.fail("Validation failed (" + errors.size() + "): " + String.join("; ", errors.subList(0, Math.min(6, errors.size()))));
    }

    public static Result teleportRegion(ServerPlayer player, String region) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter the master dungeon first");
        if (!session.arenaBuilt()) return Result.fail("Dungeon generation is still in progress");
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        BlockPos target = DungeonArena.regionPoint(session, region);
        if (level == null || target == null) return Result.fail("Unknown dungeon region: " + region);
        BlockPos safe = DungeonArena.findSafePlayerPosition(level, target, 8);
        if (safe == null) return Result.fail("Region has no safe teleport position");
        player.teleportTo(level, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, 0.0F, 0.0F);
        return Result.ok("Teleported to " + region);
    }

    public static Result debugBounds(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("Enter the master dungeon first");
        if (!session.arenaBuilt()) return Result.fail("Dungeon generation is still in progress");
        ServerLevel level = player.server.getLevel(session.dungeonDimension());
        if (level == null) return Result.fail("Dungeon dimension unavailable");
        DungeonArena.debugBounds(level, session);
        return Result.ok("Displayed master dungeon bounds");
    }

    public static Result completeCurrent(ServerPlayer player) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) return Result.fail("No dungeon session found");
        complete(player.server, session);
        return Result.ok("Completed the master dungeon");
    }

    private static void discardEncounterState(ServerLevel level, DungeonSession session) {
        DungeonArena.discardSessionEntities(level, session);
        DungeonBoss.remove(session);
        session.setBossId(null);
        session.setEncounterSpawned(false);
        session.setRewardRoomCreated(false);
        session.setLiveEnemyCount(0);
    }

    public static DungeonSession findSession(MinecraftServer server, UUID playerId) {
        for (DungeonSession session : DungeonSavedData.get(server).sessions().values()) {
            if (session.contains(playerId)) return session;
        }
        return null;
    }

    /** Adds server-authoritative dungeon state to the normal Hunter data snapshot. */
    public static void appendSnapshot(ServerPlayer player, CompoundTag snapshot) {
        DungeonSession session = findSession(player.server, player.getUUID());
        if (session == null) {
            snapshot.putBoolean("dungeon_active", false);
            snapshot.putString("dungeon_state", "none");
            snapshot.putString("dungeon_name", "");
            snapshot.putString("dungeon_objective", "");
            return;
        }
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        snapshot.putBoolean("dungeon_active", !session.isTerminal());
        snapshot.putString("dungeon_state", session.state().name().toLowerCase(java.util.Locale.ROOT));
        snapshot.putString("dungeon_name", template == null ? session.templateId() : template.displayName());
        snapshot.putString("dungeon_objective", objective == null ? "" : objective.displayName());
        snapshot.putInt("dungeon_objective_progress", session.objectiveProgress());
        snapshot.putInt("dungeon_objective_target", objective == null ? 0 : objective.target());
        snapshot.putInt("dungeon_time_seconds", Math.max(0, session.remainingTicks() / 20));
    }

    public static String inspect(MinecraftServer server, DungeonSession session) {
        DungeonTypes.DungeonTemplate template = DungeonContent.template(session.templateId());
        DungeonTypes.ObjectiveDefinition objective = session.currentObjective(template);
        String objectiveText = objective == null ? "none"
                : objective.id() + " " + session.objectiveProgress() + "/" + objective.target();
        DungeonArena.JobStatus job = DungeonArena.jobStatus(session.sessionId());
        String jobText = job == null ? "none" : job.mode() + " " + Math.round(job.progress() * 100.0D) + "%"
                + " (" + job.changedBlocks() + " changed, " + job.elapsedTicks() + " ticks)";
        return "Session " + shortId(session.sessionId()) + " | gate=" + session.gateId()
                + " | template=" + session.templateId() + " | state=" + session.state()
                + " | objective=" + objectiveText + " | time=" + session.remainingTicks() / 20
                + "s | objectiveTime=" + session.objectiveTicksRemaining() / 20 + "s | live="
                + session.liveEnemyCount() + " | spawned=" + session.totalSpawns()
                + " | layout=" + session.arenaVersion() + " | buildJob=" + jobText
                + " | lastBuild=" + session.generationChangedBlocks() + " changed/"
                + session.generationTicks() + " ticks, max=" + session.generationMaxChangedPerTick() + " changes/tick"
                + " | rewarded=" + session.rewardedMemberCount() + "/" + session.members().size();
    }

    private static void log(DungeonSession session, String transition, String reason) {
        LOGGER.info("[DUNGEON] {} | session={} state={} party={} reason={}", transition,
                session.sessionId(), session.state(), session.members(), reason == null ? "" : reason);
    }

    private static void cleanup(MinecraftServer server, DungeonSession session, boolean removeRecord) {
        ServerLevel level = server.getLevel(session.dungeonDimension());
        for (UUID memberId : session.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) returnPlayer(server, session, player);
        }
        boolean partialJob = DungeonArena.cancelJob(session.sessionId());
        if (level != null) DungeonArena.discardSessionEntities(level, session);
        DungeonBoss.remove(session);
        MISSING_BOSS_TICKS.remove(session.sessionId());
        session.setState(DungeonTypes.SessionState.CLEANING);
        session.setStartRequested(false);
        session.setPendingArenaJob("clear");
        session.setMigrationOrigin(null);
        DungeonSavedData data = DungeonSavedData.get(server);
        boolean requiresClear = level != null && (session.arenaBuilt() || partialJob);
        if (requiresClear) {
            session.setArenaBuilt(true);
            if (!DungeonArena.queueClear(session) && removeRecord) {
                session.setState(DungeonTypes.SessionState.CLOSED);
                data.sessions().remove(session.sessionId());
            }
        } else if (removeRecord) {
            session.setState(DungeonTypes.SessionState.CLOSED);
            data.sessions().remove(session.sessionId());
        }
        data.setDirty();
    }

    private static void returnPlayer(MinecraftServer server, DungeonSession session, ServerPlayer player) {
        DungeonTypes.ReturnPoint point = session.returnPoints().get(player.getUUID());
        if (point == null && player.getPersistentData().contains(PLAYER_RETURN_TAG)) {
            try {
                point = DungeonTypes.ReturnPoint.load(player.getPersistentData().getCompound(PLAYER_RETURN_TAG)).getValue();
            } catch (RuntimeException malformed) {
                LOGGER.warn("[DUNGEON] Ignored malformed return point for {}", player.getUUID());
            }
        }
        teleportToPoint(server, player, point);
        player.getPersistentData().remove(PLAYER_RETURN_TAG);
    }

    private static DungeonTypes.SessionState stateForObjective(DungeonTypes.ObjectiveDefinition objective) {
        if (objective == null) return DungeonTypes.SessionState.ACTIVE;
        return switch (objective.type()) {
            case BOSS -> DungeonTypes.SessionState.BOSS;
            case REWARD -> DungeonTypes.SessionState.REWARD;
            default -> DungeonTypes.SessionState.ACTIVE;
        };
    }

    private static void teleportToPoint(MinecraftServer server, ServerPlayer player, DungeonTypes.ReturnPoint point) {
        ServerLevel target = point == null ? server.overworld() : server.getLevel(point.dimension());
        if (target == null) target = server.overworld();
        float yaw = point == null ? target.getSharedSpawnAngle() : point.yaw();
        float pitch = point == null ? 0.0F : point.pitch();
        BlockPos preferred = point == null ? target.getSharedSpawnPos().above()
                : BlockPos.containing(point.x(), point.y(), point.z());
        BlockPos safe = DungeonArena.findSafePlayerPosition(target, preferred, 8);
        if (safe == null) {
            preferred = target.getSharedSpawnPos().above();
            safe = DungeonArena.findSafePlayerPosition(target, preferred, 8);
        }
        if (safe == null) safe = preferred;
        player.teleportTo(target, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, yaw, pitch);
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

    private static boolean isNearGate(ServerPlayer player, DungeonTypes.GateDefinition gate, double distance) {
        return player.level().dimension().equals(gate.dimension())
                && player.distanceToSqr(gate.position().getX() + 0.5D, gate.position().getY() + 1.0D,
                gate.position().getZ() + 0.5D) <= distance * distance;
    }

    private static void announceObjective(MinecraftServer server, DungeonSession session,
                                          DungeonTypes.ObjectiveDefinition objective) {
        if (objective != null) {
            broadcast(server, session, "[OBJECTIVE] " + objective.displayName()
                    + " (" + objective.target() + ")", ChatFormatting.AQUA);
        }
    }

    private static void broadcast(MinecraftServer server, DungeonSession session, String message,
                                  ChatFormatting color) {
        for (UUID memberId : session.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) player.sendSystemMessage(Component.literal(message).withStyle(color));
        }
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private DungeonRuntime() {}
}
