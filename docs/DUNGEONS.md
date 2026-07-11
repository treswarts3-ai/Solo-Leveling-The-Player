# Dungeon and Gate System

## Scope

Dungeon state is server-authoritative and persisted in overworld `SavedData` under `sololeveling_dungeons`. The workstream is implemented in `com.tre.sololeveling.dungeon`; progression, abilities, equipment, quests, and menus are integrated through hooks rather than owned here.

## Normal player flow

1. An operator creates a correctly ranked gate with `/sl dungeon open <template>`. The longer recovery form remains `/sl dungeon create_gate <gate_id> <rank> <template>`.
2. A crying-obsidian frame is filled with a luminous blue animated, non-solid `gate_portal` surface.
3. The player walks through the surface.
4. The server checks the saved gate, level requirement, access hooks, proximity, duplicate-session state, and available arena capacity.
5. The server builds the dungeon, stores return points, registers the session, teleports the party to a checked safe entry, and starts normal solo entry automatically.
6. Encounters spawn in authored rooms. Each completed objective opens its checkpoint.
7. Defeating the boss opens the reward vault. Entering the vault completes the session and grants each online member once.
8. Manual exit, death, failure, or scheduled cleanup returns players to a checked safe position and removes session entities and arena blocks.

`enter_gate` and `start_dungeon` remain available for administration, recovery, and multiplayer party testing. Normal solo play does not require them.

## Gate implementation

- `DungeonBlocks.GATE_PORTAL` is a registered common-side block.
- It has no collision and no occlusion, emits light, and cannot be pushed.
- The client assigns a translucent render layer and uses an animated blue vanilla soul-fire texture.
- Ambient portal sound and local blue particles are produced by the block.
- Lower-rate server particles provide a visible effect for nearby players.
- Gate contact remains server-authoritative and uses saved gate lookup plus a 40-tick player cooldown.
- Gate refresh only touches loaded chunks.
- Removing a gate clears the current portal block, its crying-obsidian frame, and legacy purple-glass markers.
- Existing saved gate records need no schema migration; loaded gates are refreshed into the new presentation.

## Dungeon templates

| ID | Rank | Content |
|---|---:|---|
| `abandoned_subway` | E | Ticket hall, full platform and train, maintenance plant, security wing, Subway Warden terminal, gold-lined vault |
| `red_orc_outpost` | B | Subterranean stronghold, war yard, barracks, infernal forge, command hall, captured treasure vault |
| `demon_castle_foyer` | A | Obsidian arrival bridge, infernal nave, demon forge, chapel, Iron Sovereign throne room, sovereign vault |
| `cartenon_temple` | S | Commandment hall, sixteen-idol statue gallery, trial chamber, judgment chapel, Architect's Idol, hidden reliquary |

## Authored underground maps

All four maps are deterministic vanilla Java structure templates stored under `data/sololeveling/structures/dungeons`. Each template is 89 by 19 by 89 blocks and is placed at Y -32 inside a bounded 93 by 25 by 93 solid stone volume. The two-block side and floor buffer plus four-block roof buffer prevent exposed sky, terrain caves, water, or random holes from reaching the playable map.

The templates contain hand-positioned rooms, corridors, arches, landmark silhouettes, navigation accents, lighting, cover, props, and reward spaces. They do not use random jigsaw selection. Runtime metadata supplies a unique entry, encounter center, boss center, reward center, and three or four objective doors for each map.

The generator at `tools/generate_dungeon_structures.py` validates every template before writing NBT. Validation performs a three-dimensional walkability search from the entry, checks every encounter and checkpoint, rejects routes that touch the outer shell, and reports block/detail counts. This directly guards against the former sealed-seam and exposed-void failure modes.

Arena placement is bounded to the authored footprint and uses client-update block flags instead of full neighbor updates for every block. Existing identical states are skipped.

## Encounter pacing

The E-rank sequence is tuned as an introductory dungeon:

1. Platform: three Goblin Soldiers and two Steel Fang Raiders
2. Collection: three Goblin Soldiers and two Dungeon Archers; recover three Mana Crystals
3. Elite: one Station Guard and one escort
4. Boss: Subway Warden
5. Reward vault

The E-rank direct reward is 750 XP, 280 gold, four emeralds, eight amethyst shards, and three gold ingots. Integration reward hooks may add system-specific rewards through the one-time authoritative path.

Spawn placement searches a bounded radius for a sturdy floor, world-border validity, an empty entity collision box, and no liquid. If enough safe positions cannot be found, the encounter fails rather than spawning enemies inside blocks.

## Subway Warden

The Warden is an early-game Ravager-based boss with:

- 180 base health before rank scaling
- 8.5 base attack damage
- six armor and reduced knockback resistance
- server boss bar
- telegraphed cleave and stomp attacks
- phase change at 50 percent health
- two Steel Fang Raider reinforcements
- telegraphed directional blast in phase two
- persisted phase and reinforcement flags to prevent restart duplication
- damage filtering so only members of its dungeon session are targeted by scripted attacks

The higher-rank Iron Sovereign retains the heavier version of the encounter framework.

## Checkpoints and objectives

Checkpoint barriers are placed at the platform-to-maintenance, maintenance-to-security, and security-to-boss transitions. A barrier opens immediately when its objective completes and is also synchronized periodically from the persisted objective index for restart recovery.

Collection tokens are session-tagged Mana Crystal item entities. They cannot be picked up as normal items. When a party member moves within three blocks, the server removes the token and advances the objective. Session cleanup removes remaining tokens.

The reward room is created only during the reward objective. Entering its center completes the objective; opening the chest is not required and cannot grant the authoritative XP or gold reward a second time.

## Persistence and lifecycle safety

Persisted session data includes:

- session, gate, template, owner, and member IDs
- dungeon dimension, arena origin, arena slot, and arena layout version
- return points
- state, timers, objective index, and progress
- tracked entities and live-spawn counters
- boss ID
- reward-distribution lock and rewarded-member IDs
- cleanup time and failure reason

Safety behavior:

- Active arena slots are never reallocated. Allocation fails cleanly if all bounded slots are occupied.
- A session is inserted into saved data before party teleportation, preventing duplicate entry during the transition.
- All party members must be in the gate dimension and near the gate.
- Entry and return teleports search for a supported, collision-free, liquid-free position.
- Waiting sessions using an older arena layout are rebuilt and migrated.
- Active sessions using an incompatible old layout fail safely instead of rebuilding around players or enemies.
- Login recovery returns active members to a safe dungeon entry when they are outside the arena.
- Completed members who reconnect before cleanup receive a still-pending per-member reward at most once.
- Reward recipients are marked in saved data before XP, gold, items, or integration hooks run.
- Boss phase-two reinforcement state is persisted on the boss entity.
- Player death fails an active session. Respawn returns the player after terminal state.
- Manual exit returns the player; if no party member remains, the session fails.
- Waiting sessions expire after five minutes without a member in the arena.
- Active sessions fail after one minute without a member in the arena.
- Completed sessions clean up after 20 seconds; failed sessions clean up after five seconds.
- Cleanup queries only the session arena, discards session-tagged entities, restores the bounded underground volume to solid stone, removes the boss bar, and deletes the saved session.

## Commands

Both `/sl dungeon` and `/sololeveling dungeon` expose the same commands.

| Command | Permission | Purpose |
|---|---:|---|
| `open <template>` | 2 | Create a correctly ranked gate for one of the four maps in front of the player |
| `create_gate <gate_id> <rank> <template>` | 2 | Create a persistent interactive gate |
| `remove_gate <gate_id>` | 2 | Remove a gate and every recognized marker block |
| `enter_gate <gate_id>` | Player | Manual solo recovery/test entry |
| `enter_gate <gate_id> <party selector>` | 2 | Enter with up to eight nearby online players |
| `start_dungeon` | Owner | Start a waiting session manually |
| `inspect_session [session_uuid]` | Player / 2 | Inspect objective, timers, layout, enemies, and reward recipients |
| `spawn_wave <wave_id>` | 2 | Spawn a template wave within hard caps |
| `complete_objective [session_uuid]` | 2 | Complete the current objective for testing or recovery |
| `fail_dungeon <session_uuid> [reason]` | 2 | Fail a session safely |
| `exit_dungeon` | Player | Return to a checked safe return point |
| `clear_dungeon_state` | 2 | Return players, clear arenas and entities, and remove sessions |
| `spawn_test_enemy <enemy_id>` | 2 | Spawn a test enemy near the player using safe placement |
| `spawn_test_boss` | 2 | Spawn the current template boss |

## Integration APIs

`DungeonHooks` exposes access rules, reward hooks, and Forge events:

- `GateEnteredEvent`
- `SessionStartedEvent`
- `ObjectiveCompletedEvent`
- `EnemyDefeatedEvent`
- `DungeonCompletedEvent`
- `DungeonFailedEvent`
- `RewardGrantedEvent`

Dungeon enemies carry session, enemy-ID, dungeon-enemy, and shadow-extractable tags. Vanilla drops and experience are suppressed.

## Hard limits

- Maximum eight requested party members
- Maximum 64 live dungeon enemies per session
- Maximum 160 total wave spawns per session
- Maximum 4,096 reserved arena slots
- Each arena is isolated by 104 blocks and bounded to a 48-block radius
- Each structure template is exactly 89 by 19 by 89 blocks
- Bounded arena entity queries; no full-world entity scan
- Collection scans limited to the arena every four ticks
- Gate animation skips unloaded chunks

## Validation and remaining risks

The required build command is:

```bash
./gradlew clean build --stacktrace --no-daemon
```

A successful Forge build proves source/resource compatibility. The generator additionally proves template dimensions, NBT generation, route connectivity, marker reachability, and sealed boundaries. Perceived combat balance and presentation still benefit from normal release playtesting, but no map depends on the removed fallback arena.
