package com.tre.sololeveling.quest;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Locale;

public final class QuestManager {
    public static final String STATES_KEY = "quest_states";
    public static final String UI_KEY = "quest_ui";
    public static final String DAILY_DATE_KEY = "quest_daily_date";

    public static void tick(ServerPlayer player) {
        if (!HunterData.isAwakened(player) || player.tickCount % 20 != 0) return;
        boolean changed = ensureDailyCycle(player);
        changed |= startNextStoryQuest(player);
        changed |= refreshDerivedObjectives(player);
        if (changed) sync(player);
    }

    public static void onLogin(ServerPlayer player) {
        if (HunterData.isAwakened(player)) {
            ensureDailyCycle(player);
            startNextStoryQuest(player);
            refreshDerivedObjectives(player);
        }
        prepareSync(player);
    }

    public static boolean start(ServerPlayer player, String questId) {
        QuestDefinition definition = QuestRegistry.get(questId);
        if (definition == null || !canStart(player, definition)) return false;
        CompoundTag state = state(player, definition.id(), true);
        QuestStatus status = status(state);
        if (status == QuestStatus.ACTIVE) return false;
        if (!definition.repeatable() && state.getInt("completion_count") > 0) return false;
        beginState(player, definition, state);
        notify(player, "started", definition, "");
        sync(player);
        return true;
    }

    public static boolean complete(ServerPlayer player, String questId, boolean force) {
        QuestDefinition definition = QuestRegistry.get(questId);
        if (definition == null) return false;
        CompoundTag state = state(player, definition.id(), false);
        if (state == null || status(state) != QuestStatus.ACTIVE) return false;
        if (force) {
            CompoundTag progress = state.getCompound("progress");
            for (QuestObjective objective : definition.objectives()) progress.putInt(objective.id(), objective.required());
            state.put("progress", progress);
        }
        if (!objectivesComplete(definition, state)) return false;
        return completeInternal(player, definition, state, true);
    }

    public static boolean fail(ServerPlayer player, String questId, String reason) {
        QuestDefinition definition = QuestRegistry.get(questId);
        if (definition == null) return false;
        CompoundTag state = state(player, definition.id(), false);
        if (state == null || status(state) != QuestStatus.ACTIVE) return false;
        state.putString("status", QuestStatus.FAILED.name());
        state.putString("failure_reason", sanitize(reason, 160));
        state.putLong("failed_at", player.level().getGameTime());
        if (!state.getBoolean("penalty_applied")) {
            state.putBoolean("penalty_applied", true);
            definition.penalty().apply(player, reason == null ? "" : reason);
        }
        notify(player, "failed", definition, state.getString("failure_reason"));
        sync(player);
        return true;
    }

    public static boolean reset(ServerPlayer player, String questId) {
        QuestDefinition definition = QuestRegistry.get(questId);
        if (definition == null) return false;
        CompoundTag state = state(player, definition.id(), true);
        for (String key : java.util.Set.copyOf(state.getAllKeys())) state.remove(key);
        state.putString("id", definition.id().toString());
        state.putString("status", QuestStatus.NOT_STARTED.name());
        state.putInt("completion_count", 0);
        notify(player, "reset", definition, "");
        sync(player);
        return true;
    }

    public static void resetDaily(ServerPlayer player, boolean manual) {
        CompoundTag data = HunterData.mutable(player);
        for (QuestDefinition definition : QuestRegistry.values()) {
            if (!definition.daily()) continue;
            CompoundTag state = state(player, definition.id(), true);
            if (status(state) == QuestStatus.ACTIVE && !manual) {
                state.putString("status", QuestStatus.FAILED.name());
                if (!state.getBoolean("penalty_applied")) {
                    state.putBoolean("penalty_applied", true);
                    definition.penalty().apply(player, "Daily reset");
                }
            }
            int completions = state.getInt("completion_count");
            for (String key : java.util.Set.copyOf(state.getAllKeys())) state.remove(key);
            state.putString("id", definition.id().toString());
            state.putString("status", QuestStatus.NOT_STARTED.name());
            state.putInt("completion_count", completions);
            state.putString("daily_cycle", today());
            if (canStart(player, definition)) beginState(player, definition, state);
            QuestApi.fire(player, "daily_reset", definition, manual ? "manual" : "scheduled");
        }
        data.putString(DAILY_DATE_KEY, today());
        sync(player);
    }

    public static void clear(ServerPlayer player) {
        CompoundTag data = HunterData.mutable(player);
        data.put(STATES_KEY, new ListTag());
        data.put(UI_KEY, new ListTag());
        data.putString(DAILY_DATE_KEY, today());
        startNextStoryQuest(player);
        sync(player);
    }

    public static boolean advance(ServerPlayer player, String questId, String objectiveId, int amount) {
        QuestDefinition definition = QuestRegistry.get(questId);
        if (definition == null || amount <= 0) return false;
        CompoundTag state = state(player, definition.id(), false);
        if (state == null || status(state) != QuestStatus.ACTIVE) return false;
        QuestObjective objective = definition.objectives().stream()
                .filter(value -> value.id().equalsIgnoreCase(objectiveId)).findFirst().orElse(null);
        if (objective == null) return false;
        boolean changed = addProgress(player, definition, state, objective, amount);
        if (changed) sync(player);
        return changed;
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        String target = key == null ? victim.getType().toString() : key.toString();
        updateMatching(player, QuestObjectiveType.KILL, target, 1, victim instanceof Enemy);
    }

    public static void onCollected(ServerPlayer player, ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && !stack.isEmpty()) updateMatching(player, QuestObjectiveType.COLLECTION, key.toString(), stack.getCount(), false);
    }

    public static void onAbilityUsed(ServerPlayer player, String abilityId) { updateMatching(player, QuestObjectiveType.ABILITY_USE, abilityId, 1, false); }
    public static void onManaSpent(ServerPlayer player, int amount) { if (amount > 0) updateMatching(player, QuestObjectiveType.MANA_SPENT, "any", amount, false); }
    public static void onStatAllocated(ServerPlayer player, String stat, int amount) { if (amount > 0) updateMatching(player, QuestObjectiveType.STAT_ALLOCATION, stat, amount, false); }
    public static void onDungeonCleared(ServerPlayer player, String dungeonId) { updateMatching(player, QuestObjectiveType.DUNGEON_CLEAR, dungeonId, 1, false); }
    public static void onShadowSummoned(ServerPlayer player, String shadowId) { updateMatching(player, QuestObjectiveType.SHADOW_SUMMON, shadowId, 1, false); }
    public static void onSkillUnlocked(ServerPlayer player, String skillId) { updateMatching(player, QuestObjectiveType.SKILL_UNLOCK, skillId, 1, false); }
    public static void onSystemOpened(ServerPlayer player) { updateMatching(player, QuestObjectiveType.SYSTEM_OPEN, "system", 1, false); }
    public static void onExercise(ServerPlayer player, String exercise, int amount) { if (amount > 0) updateMatching(player, QuestObjectiveType.EXERCISE, exercise, amount, false); }
    public static void onDistanceMoved(ServerPlayer player, int blocks) { if (blocks > 0) updateMatching(player, QuestObjectiveType.DISTANCE, "sprint", blocks, false); }
    public static void onEnemyRole(ServerPlayer player, String role) { updateMatching(player, QuestObjectiveType.ENEMY_ROLE, role, 1, false); }
    public static void onEncounterConstraint(ServerPlayer player, String constraint) { updateMatching(player, QuestObjectiveType.ENCOUNTER_CONSTRAINT, constraint, 1, false); }
    public static void onSecretDiscovered(ServerPlayer player, String secret) { updateMatching(player, QuestObjectiveType.SECRET_DISCOVERY, secret, 1, false); }
    public static void onShadowDeveloped(ServerPlayer player, String event) { updateMatching(player, QuestObjectiveType.SHADOW_DEVELOPMENT, event, 1, false); }
    public static void onEquipmentUpgraded(ServerPlayer player, String rarity) { updateMatching(player, QuestObjectiveType.EQUIPMENT_UPGRADE, rarity, 1, false); }
    public static void onCombatStyle(ServerPlayer player, String style) { updateMatching(player, QuestObjectiveType.COMBAT_STYLE, style, 1, false); }

    private static void updateMatching(ServerPlayer player, QuestObjectiveType type, String eventTarget, int amount, boolean hostile) {
        if (!HunterData.isAwakened(player) || amount <= 0) return;
        boolean changed = false;
        for (QuestDefinition definition : QuestRegistry.values()) {
            CompoundTag state = state(player, definition.id(), false);
            if (state == null || status(state) != QuestStatus.ACTIVE) continue;
            for (QuestObjective objective : definition.objectives()) {
                if (objective.type() != type || !matches(objective.target(), eventTarget, hostile)) continue;
                changed |= addProgress(player, definition, state, objective, amount);
                if (status(state) != QuestStatus.ACTIVE) break;
            }
        }
        if (changed) sync(player);
    }

    private static boolean addProgress(ServerPlayer player, QuestDefinition definition, CompoundTag state, QuestObjective objective, int amount) {
        CompoundTag progress = state.getCompound("progress");
        int old = Math.max(0, progress.getInt(objective.id()));
        int next = Math.min(objective.required(), old + Math.max(0, amount));
        if (old == next) return false;
        progress.putInt(objective.id(), next);
        state.put("progress", progress);
        notify(player, "progress", definition, objective.id() + ":" + next + "/" + objective.required());
        if (objectivesComplete(definition, state)) completeInternal(player, definition, state, false);
        return true;
    }

    private static boolean completeInternal(ServerPlayer player, QuestDefinition definition, CompoundTag state, boolean explicit) {
        if (status(state) != QuestStatus.ACTIVE || state.getBoolean("reward_claimed")) return false;
        state.putString("status", QuestStatus.COMPLETED.name());
        state.putBoolean("reward_claimed", true);
        state.putLong("completed_at", player.level().getGameTime());
        state.putInt("completion_count", state.getInt("completion_count") + 1);
        definition.reward().apply(player);
        notify(player, "completed", definition, explicit ? "command" : "objectives");
        startNextStoryQuest(player);
        return true;
    }

    private static boolean refreshDerivedObjectives(ServerPlayer player) {
        boolean changed = false;
        CompoundTag hunter = HunterData.mutable(player);
        for (QuestDefinition definition : QuestRegistry.values()) {
            CompoundTag state = state(player, definition.id(), false);
            if (state == null || status(state) != QuestStatus.ACTIVE) continue;
            for (QuestObjective objective : definition.objectives()) {
                int desired = -1;
                if (objective.type() == QuestObjectiveType.SYSTEM_OPEN && hunter.getBoolean("tutorial_system_opened")) desired = objective.required();
                else if (objective.type() == QuestObjectiveType.SKILL_UNLOCK && HunterData.hasSkill(player, objective.target())) desired = objective.required();
                else if (objective.type() == QuestObjectiveType.COLLECTION) desired = inventoryCount(player, objective.target());
                else if (objective.type() == QuestObjectiveType.DISTANCE && objective.target().equals("sprint")) desired = hunter.getInt("daily_run");
                if (desired < 0) continue;
                CompoundTag progress = state.getCompound("progress");
                int old = progress.getInt(objective.id());
                int next = Math.min(objective.required(), Math.max(0, desired));
                if (old == next) continue;
                progress.putInt(objective.id(), next);
                state.put("progress", progress);
                changed = true;
            }
            if (status(state) == QuestStatus.ACTIVE && objectivesComplete(definition, state)) {
                completeInternal(player, definition, state, false);
                changed = true;
            }
        }
        return changed;
    }

    private static int inventoryCount(ServerPlayer player, String target) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && matches(target, key.toString(), false)) count += stack.getCount();
        }
        return count;
    }

    private static boolean startNextStoryQuest(ServerPlayer player) {
        QuestDefinition next = QuestRegistry.values().stream()
                .filter(definition -> definition.storyOrder() >= 0)
                .filter(definition -> {
                    CompoundTag state = state(player, definition.id(), false);
                    return state == null || state.getInt("completion_count") <= 0;
                })
                .filter(definition -> canStart(player, definition))
                .min(Comparator.comparingInt(QuestDefinition::storyOrder)).orElse(null);
        if (next == null) return false;
        CompoundTag state = state(player, next.id(), true);
        if (status(state) == QuestStatus.ACTIVE) return false;
        if (status(state) == QuestStatus.FAILED && !next.repeatable()) return false;
        beginState(player, next, state);
        notify(player, "started", next, "story");
        return true;
    }

    private static boolean canStart(ServerPlayer player, QuestDefinition definition) {
        if (!HunterData.isAwakened(player)) return false;
        for (ResourceLocation prerequisite : definition.prerequisites()) {
            CompoundTag state = state(player, prerequisite, false);
            if (state == null || state.getInt("completion_count") <= 0) return false;
        }
        return true;
    }

    private static void beginState(ServerPlayer player, QuestDefinition definition, CompoundTag state) {
        int completions = state.getInt("completion_count");
        for (String key : java.util.Set.copyOf(state.getAllKeys())) state.remove(key);
        state.putString("id", definition.id().toString());
        state.putString("status", QuestStatus.ACTIVE.name());
        state.putInt("completion_count", completions);
        state.putLong("started_at", player.level().getGameTime());
        state.putBoolean("reward_claimed", false);
        state.putBoolean("penalty_applied", false);
        state.put("progress", new CompoundTag());
        if (definition.daily()) state.putString("daily_cycle", today());
    }

    private static boolean ensureDailyCycle(ServerPlayer player) {
        CompoundTag data = HunterData.mutable(player);
        String current = data.getString(DAILY_DATE_KEY);
        if (today().equals(current)) return false;
        if (!current.isBlank()) resetDaily(player, false);
        else {
            data.putString(DAILY_DATE_KEY, today());
            for (QuestDefinition definition : QuestRegistry.values()) {
                if (!definition.daily() || !canStart(player, definition)) continue;
                CompoundTag state = state(player, definition.id(), true);
                if (state.getInt("completion_count") <= 0 && status(state) != QuestStatus.ACTIVE) beginState(player, definition, state);
            }
        }
        return true;
    }

    private static boolean objectivesComplete(QuestDefinition definition, CompoundTag state) {
        CompoundTag progress = state.getCompound("progress");
        for (QuestObjective objective : definition.objectives()) {
            if (progress.getInt(objective.id()) < objective.required()) return false;
        }
        return true;
    }

    private static boolean matches(String expected, String actual, boolean hostile) {
        String left = expected == null ? "any" : expected.toLowerCase(Locale.ROOT);
        String right = actual == null ? "" : actual.toLowerCase(Locale.ROOT);
        if (left.equals("any")) return true;
        if (left.equals("hostile")) return hostile;
        return left.equals(right) || (!left.contains(":") && right.endsWith(":" + left));
    }

    private static QuestStatus status(CompoundTag state) { return QuestStatus.parse(state.getString("status")); }

    private static CompoundTag state(ServerPlayer player, ResourceLocation id, boolean create) {
        ListTag states = HunterData.mutable(player).getList(STATES_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < states.size(); index++) {
            CompoundTag state = states.getCompound(index);
            if (id.toString().equals(state.getString("id"))) return state;
        }
        if (!create) return null;
        CompoundTag state = new CompoundTag();
        state.putString("id", id.toString());
        state.putString("status", QuestStatus.NOT_STARTED.name());
        state.putInt("completion_count", 0);
        states.add(state);
        HunterData.mutable(player).put(STATES_KEY, states);
        return state;
    }

    public static void prepareSync(ServerPlayer player) {
        ListTag ui = new ListTag();
        for (QuestDefinition definition : QuestRegistry.values()) {
            CompoundTag state = state(player, definition.id(), false);
            if (state == null && !canStart(player, definition)) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("id", definition.id().toString());
            entry.putString("name", definition.displayName());
            entry.putString("description", definition.description());
            entry.putString("category", definition.category().name());
            entry.putString("status", state == null ? QuestStatus.NOT_STARTED.name() : status(state).name());
            entry.putBoolean("repeatable", definition.repeatable());
            entry.putBoolean("daily", definition.daily());
            entry.putInt("completion_count", state == null ? 0 : state.getInt("completion_count"));
            ListTag objectives = new ListTag();
            CompoundTag progress = state == null ? new CompoundTag() : state.getCompound("progress");
            for (QuestObjective objective : definition.objectives()) {
                CompoundTag objectiveTag = new CompoundTag();
                objectiveTag.putString("id", objective.id());
                objectiveTag.putString("type", objective.type().name());
                objectiveTag.putString("target", objective.target());
                objectiveTag.putString("description", objective.description());
                objectiveTag.putInt("progress", Math.min(objective.required(), progress.getInt(objective.id())));
                objectiveTag.putInt("required", objective.required());
                objectiveTag.putBoolean("complete", progress.getInt(objective.id()) >= objective.required());
                objectives.add(objectiveTag);
            }
            entry.put("objectives", objectives);
            ui.add(entry);
        }
        HunterData.mutable(player).put(UI_KEY, ui);
    }

    private static void sync(ServerPlayer player) {
        prepareSync(player);
        HunterData.sync(player);
    }

    private static void notify(ServerPlayer player, String event, QuestDefinition definition, String detail) {
        String prefix = switch (event) {
            case "completed" -> "[QUEST COMPLETE] ";
            case "failed" -> "[QUEST FAILED] ";
            case "started" -> "[QUEST STARTED] ";
            default -> "[QUEST] ";
        };
        if (!event.equals("progress")) {
            ChatFormatting color = event.equals("failed") ? ChatFormatting.RED : event.equals("completed") ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            player.sendSystemMessage(Component.literal(prefix + definition.displayName()).withStyle(color));
        }
        if (event.equals("completed")) player.level().playSound(null, player.blockPosition(), ModSounds.QUEST_COMPLETE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        QuestApi.fire(player, event, definition, detail == null ? "" : detail);
    }

    private static String today() { return LocalDate.now(ZoneOffset.UTC).toString(); }
    private static String sanitize(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}§]", "").trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private QuestManager() { }
}
