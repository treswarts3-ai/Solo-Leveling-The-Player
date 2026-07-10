# Gates, Dungeons, Enemies, and Boss Encounters

## Scope

Dungeon state is server authoritative and persisted in overworld `SavedData` under `sololeveling_dungeons`. Rewards are guarded by the persisted session-level `rewardGranted` flag before XP, gold, items, or integration reward hooks execute.

## Normal player flow

1. An operator creates a gate with `/sl dungeon create_gate <gate_id> <rank> <template>`.
2. The gate appears as a crying-obsidian frame with an open center and animated blue-purple portal particles.
3. A player walks into the center of the frame.
4. The server validates level and access rules, builds the dungeon, teleports the player inside, and starts the session automatically.
5. Objectives spawn in their authored encounter rooms and checkpoint barriers open as objectives are completed.
6. Defeating the boss opens the reward vault. Entering the vault completes the dungeon, grants rewards once, and returns the player safely during cleanup.

`enter_gate` and `start_dungeon` remain available for testing and recovery, but are not required during normal solo play.

## Dungeon templates

| ID | Rank | Content |
|---|---:|---|
| `abandoned_subway` | E | Authored station route, platform wave, mana-crystal recovery, security elite, Subway Warden boss, sealed reward vault |
| `red_orc_outpost` | B | Mixed combat wave, essence-core collection, commander escort elite, reward room |
| `demon_castle_foyer` | A | Castle vanguard wave, demonic-core collection, twin commanders, Iron Sovereign boss, sovereign vault |

### Abandoned Subway layout

The E-rank dungeon is no longer a flat square arena. It contains:

- gate vestibule and ticket hall
- abandoned platform with tracks, columns, benches, damaged masonry, and lighting
- maintenance tunnels with machinery and copper pipe details
- security room and elite checkpoint
- large blackstone boss chamber with a raised central dais
- sealed reward vault that opens after the Subway Warden is defeated

Encounter spawn points are tied to the current objective room. Spawn correction searches for a supported two-block-high position before adding an enemy.

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

## Bosses

### Subway Warden

The E-rank Abandoned Subway ends with a lower-health Ravager-based boss tuned for early progression. It uses:

- server boss health bar
- telegraphed cleave and radial stomp
- phase transition at 50 percent health
- phase-two adds and directional blast
- lower E-rank health and damage scaling
- shadow-extraction eligibility

### Iron Sovereign

The higher-rank Iron Sovereign retains the heavier version of the same encounter framework with increased health, armor, damage, and phase pressure.

## Commands

Both `/sl dungeon` and `/sololeveling dungeon` expose the same commands.

| Command | Permission | Purpose |
|---|---:|---|
| `create_gate <gate_id> <rank> <template>` | 2 | Create a persistent interactive gate near the command source |
| `remove_gate <gate_id>` | 2 | Remove a persistent gate and its marker |
| `enter_gate <gate_id>` | Player | Manual recovery/test entry |
| `enter_gate <gate_id> <party selector>` | 2 | Enter with an explicit party of up to eight online players |
| `start_dungeon` | Owner | Manual recovery/test start for a waiting session |
| `inspect_session [session_uuid]` | Player / 2 | Inspect the current or selected session |
| `spawn_wave <wave_id>` | 2 | Spawn a template wave while respecting hard caps |
| `complete_objective [session_uuid]` | 2 | Complete the current objective for testing or recovery |
| `fail_dungeon <session_uuid> [reason]` | 2 | Fail a session safely |
| `exit_dungeon` | Player | Return to the stored safe return point |
| `clear_dungeon_state` | 2 | Exit players, remove tracked entities, clear arenas, and remove sessions |
| `spawn_test_enemy <enemy_id>` | 2 | Spawn a reusable test enemy in the current encounter room |
| `spawn_test_boss` | 2 | Spawn the template boss in the current boss chamber |

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
- Gate contact uses a cooldown to prevent duplicate session creation.
- Waiting sessions expire after five minutes without a present member.
- Active sessions fail after one minute without a present member.
- Total and per-objective timers are persisted.
- Players retain a persistent return marker in addition to the session return map.
- Completed sessions clean up after 20 seconds; failed sessions clean up after five seconds.
- Cleanup discards session-tagged entities, clears the arena, removes the boss bar, returns online players, and removes the session record.

## Remaining limitations

- Dungeons are isolated at reserved far-overworld coordinates rather than in a custom dimension.
- Direct rewards are issued to online session members at completion; offline reward-mail persistence is not included.
- Red Orc Outpost and Demon Castle still use the reusable fallback arena and need authored layouts.
- Portal presentation currently uses an open frame plus particles rather than a registered custom no-collision portal block.
- Full gameplay balance and multiplayer soak testing remain required.
