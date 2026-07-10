package com.tre.sololeveling.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DungeonHooks {
    @FunctionalInterface public interface GateAccessRule {
        String denialReason(ServerPlayer player, DungeonTypes.GateDefinition gate, DungeonTypes.DungeonTemplate template);
    }
    @FunctionalInterface public interface RewardHook {
        void grant(ServerPlayer player, DungeonSession session, DungeonTypes.DungeonTemplate template);
    }

    private static final List<GateAccessRule> ACCESS_RULES = new CopyOnWriteArrayList<>();
    private static final List<RewardHook> REWARD_HOOKS = new CopyOnWriteArrayList<>();

    public static void registerAccessRule(GateAccessRule rule) { if (rule != null) ACCESS_RULES.add(rule); }
    public static void registerRewardHook(RewardHook hook) { if (hook != null) REWARD_HOOKS.add(hook); }
    public static String evaluateAccess(ServerPlayer player, DungeonTypes.GateDefinition gate, DungeonTypes.DungeonTemplate template) {
        for (GateAccessRule rule : ACCESS_RULES) {
            String reason = rule.denialReason(player, gate, template);
            if (reason != null && !reason.isBlank()) return reason;
        }
        return "";
    }
    public static void grantIntegrationRewards(ServerPlayer player, DungeonSession session, DungeonTypes.DungeonTemplate template) {
        REWARD_HOOKS.forEach(hook -> hook.grant(player, session, template));
    }
    public static void post(Event event) { MinecraftForge.EVENT_BUS.post(event); }

    public abstract static class DungeonEvent extends Event {
        private final UUID sessionId;
        private final String templateId;
        protected DungeonEvent(DungeonSession session) { sessionId = session.sessionId(); templateId = session.templateId(); }
        public UUID sessionId() { return sessionId; }
        public String templateId() { return templateId; }
    }

    public static final class GateEnteredEvent extends DungeonEvent {
        private final ServerPlayer player; private final String gateId;
        public GateEnteredEvent(ServerPlayer player, DungeonSession session) { super(session); this.player = player; gateId = session.gateId(); }
        public ServerPlayer player() { return player; } public String gateId() { return gateId; }
    }
    public static final class SessionStartedEvent extends DungeonEvent { public SessionStartedEvent(DungeonSession session) { super(session); } }
    public static final class ObjectiveCompletedEvent extends DungeonEvent {
        private final String objectiveId;
        public ObjectiveCompletedEvent(DungeonSession session, String objectiveId) { super(session); this.objectiveId = objectiveId; }
        public String objectiveId() { return objectiveId; }
    }
    public static final class EnemyDefeatedEvent extends DungeonEvent {
        private final ServerPlayer creditedPlayer; private final LivingEntity enemy; private final String enemyId; private final boolean shadowExtractable;
        public EnemyDefeatedEvent(DungeonSession session, ServerPlayer creditedPlayer, LivingEntity enemy, String enemyId, boolean shadowExtractable) {
            super(session); this.creditedPlayer = creditedPlayer; this.enemy = enemy; this.enemyId = enemyId; this.shadowExtractable = shadowExtractable;
        }
        public ServerPlayer creditedPlayer() { return creditedPlayer; } public LivingEntity enemy() { return enemy; }
        public String enemyId() { return enemyId; } public boolean shadowExtractable() { return shadowExtractable; }
    }
    public static final class DungeonCompletedEvent extends DungeonEvent { public DungeonCompletedEvent(DungeonSession session) { super(session); } }
    public static final class DungeonFailedEvent extends DungeonEvent {
        private final String reason; public DungeonFailedEvent(DungeonSession session, String reason) { super(session); this.reason = reason; }
        public String reason() { return reason; }
    }
    public static final class RewardGrantedEvent extends DungeonEvent {
        private final ServerPlayer player; private final int xp; private final int gold;
        public RewardGrantedEvent(DungeonSession session, ServerPlayer player, int xp, int gold) { super(session); this.player = player; this.xp = xp; this.gold = gold; }
        public ServerPlayer player() { return player; } public int xp() { return xp; } public int gold() { return gold; }
    }

    private DungeonHooks() {}
}
