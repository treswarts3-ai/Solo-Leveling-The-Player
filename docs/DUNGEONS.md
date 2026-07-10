# Gates, Dungeons, Enemies, and Boss Encounters

## Scope

This workstream is implemented in `com.tre.sololeveling.dungeon` and does not modify progression, ability, HUD, shadow AI, equipment, or quest internals.

Dungeon state is server authoritative and persisted in overworld `SavedData` under `sololeveling_dungeons`. Rewards are guarded by the persisted session-level `rewardGranted` flag before XP, gold, items, or integration reward hooks execute.

## Dungeon templates

| ID | Rank | Content |
|---|---:|---|
| `abandoned_subway` | E | Combat wave, mana-core collection, gate-keeper elite, reward room |
| `red_orc_outpost` | B | Mixed combat wave, essence-core collection, commander escort elite, reward room |
| `demon_castle_foyer` | A | Castle vanguard wave, demonic-core collection, twin commanders, Iron Sovereign boss, sovereign vault |

## Enemy definitions

Base variants:

- `goblin_soldier` — melee
- `steel_fang_raider` — fast
- `stone_guardian` — tank
- `dungeon_archer` — ranged
- `orc_commander` — elite

Shadow-extractable variants:

- `shadow_goblin`
- `shadow_raider`
- `shadow_guardian`
- `shadow_archer`
- `shadow_commander`

All dungeon enemies carry the `sl_dungeon_session`, `sl_enemy_id`, `sl_dungeon_enemy`, and `sl_shadow_extractable` persistent tags. Vanilla drops and experience are suppressed. Integration code can inspect the tags or subscribe to `DungeonHooks.EnemyDefeatedEvent`.

## Initial boss

`Iron Sovereign` uses a scaled persistent Ravager encounter with:

- Server boss health bar
- Phase one cleave and radial stomp
- Phase two transition at 50 percent health
- Phase two adds, directional sovereign blast, and larger radial stomp
- Particle and sound telegraphs before attacks
- Dungeon completion integration
- Shadow-extraction eligibility tag and defeat event

## Commands

Both `/sl dungeon` and `/sololeveling dungeon` expose the same commands.

| Command | Permission | Purpose |
|---|---:|---|
| `create_gate <gate_id> <rank> <template>` | 2 | Create a persistent gate and marker near the command source |
| `remove_gate <gate_id>` | 2 | Remove a persistent gate and its marker |
| `enter_gate <gate_id>` | Player | Enter a gate as a solo owner |
| `enter_gate <gate_id> <party selector>` | 2 | Enter with an explicit party of up to eight online players |
| `start_dungeon` | Owner | Start the current waiting session |
| `inspect_session [session_uuid]` | Player / 2 | Inspect the current or selected session |
| `spawn_wave <wave_id>` | 2 | Spawn a template wave while respecting hard caps |
| `complete_objective [session_uuid]` | 2 | Complete the current objective for testing or recovery |
| `fail_dungeon <session_uuid> [reason]` | 2 | Fail a session safely |
| `exit_dungeon` | Player | Return to the stored safe return point |
| `clear_dungeon_state` | 2 | Exit players, remove tracked entities, clear arenas, and remove sessions |
| `spawn_test_enemy <enemy_id>` | 2 | Spawn a reusable test enemy in the current arena |
| `spawn_test_boss` | 2 | Spawn the Iron Sovereign in the current arena |

## Integration APIs

### Access rules

```java
DungeonHooks.registerAccessRule((player, gate, template) -> {
    // Return an empty string to allow entry.
    // Return a user-facing denial reason to block entry.
    return "";
});
```

Use this for quest prerequisites, equipment checks, party composition, or other server-side entry requirements without changing dungeon internals.

### Extra rewards

```java
DungeonHooks.registerRewardHook((player, session, template) -> {
    // Add quest, equipment, title, or shadow-system rewards.
});
```

The hook executes only through the one-time authoritative reward path.

### Forge events

- `DungeonHooks.GateEnteredEvent`
- `DungeonHooks.SessionStartedEvent`
- `DungeonHooks.ObjectiveCompletedEvent`
- `DungeonHooks.EnemyDefeatedEvent`
- `DungeonHooks.DungeonCompletedEvent`
- `DungeonHooks.DungeonFailedEvent`
- `DungeonHooks.RewardGrantedEvent`

## Safety and lifecycle rules

- Maximum 64 live dungeon enemies per session.
- Maximum 160 total wave spawns per session.
- Arena entity queries are bounded to the session arena; no full-world entity scans run each tick.
- Collection scans run inside the arena every four ticks.
- Waiting sessions expire after five minutes without a present member.
- Active sessions fail after one minute without a present member.
- Total and per-objective timers are persisted.
- Players retain a persistent return marker in addition to the session return map.
- Completed sessions clean up after 20 seconds; failed sessions clean up after five seconds.
- Cleanup discards session-tagged entities, clears the arena, removes the boss bar, returns online players, and removes the session record.

## Current limitations

- Arenas are isolated at reserved far-overworld coordinates rather than in a custom dimension.
- Direct rewards are issued to online session members at completion; offline reward-mail persistence is not included.
- Arena geometry uses a reusable generated room rather than authored structures or structure-template files.
- Full gameplay balance and multiplayer soak testing should be performed after all worker branches are integrated.
