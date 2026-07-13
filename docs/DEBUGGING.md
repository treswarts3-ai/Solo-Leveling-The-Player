# Testing and Debugging Infrastructure

Phase 2 turns vague playtest reports into reproducible fixtures and measured server state. Every command is operator-only and available through `/sl` and `/sololeveling`.

## Reversible fixture

| Command | Result |
|---|---|
| `/sl test setup` | Saves the player's Hunter compound, complete inventory/equipment, health, hunger, and vanilla XP; clears transient combat/dungeon state; loads a controlled level-40 hunter with 35 in each stat, 25 stat points, 25,000 gold, a defined ability set, full resources, Knight Killer, novice armor, and test potions. |
| `/sl test reset` | Deletes any test dungeon session and restores the saved Hunter data, inventory/equipment, health, hunger, and vanilla XP. |

Setup refuses to overwrite an existing backup. Reset refuses to run without one. Active shadow entities and temporary ability states are deliberately dismissed; persistent stored shadows are restored and reconciled safely.

## Structured tests

| Command | Measurement |
|---|---|
| `/sl test progression` | Schema version, non-negative resources, mana bounds, level/rank, and pending growth state. |
| `/sl test combat` | Registered ability roster, unlock count, and health bounds. |
| `/sl test dungeon` | Current session inspection and authored arena validation when built. |
| `/sl test shadows` | Stored/active counts against persistent and active caps. |
| `/sl test multiplayer` | Online player count and current dungeon party membership; reports when a second client is still required. |

Every result begins with `[TEST PASS]` or `[TEST FAIL]` and includes the measured values.

## Debug commands

| Command | Output |
|---|---|
| `/sl debug overlay` | Toggles the synchronized developer overlay. |
| `/sl debug player` | Level, rank, mana, gold, stats, coordinates, and dimension. |
| `/sl debug dungeon` | Session ID/state, objective, timers, entities, generation workload, and rewarded members. |
| `/sl debug packets` | Received, rejected, and current one-second packet-window counts. |
| `/sl debug entities` | Entities within 64 blocks, session-tracked entities, and active shadows. |
| `/sl debug performance` | Latest and rolling server tick milliseconds plus dungeon generation workload. |

The overlay is disabled by default. When enabled it shows player progression/resources, stats, live ability states, cooldown ticks, session/objective/location/entity data, shadow counts, packet counters, server tick time, generation workload, coordinates, dimension, and nearby entities. Expensive nearby-entity/shadow measurements are omitted from normal snapshots while the overlay is disabled.

## Dungeon logs

Transition-only server logs use `[DUNGEON]` and include the session UUID, current state, party UUIDs, and reason/context. Logged transitions are:

- Session created
- Build started
- Build validated
- Player teleported
- Encounter started
- Objective completed
- Reward locked
- Failure
- Cleanup completed

No per-tick dungeon log is emitted.

## Runtime acceptance matrix

| Scenario | Required evidence |
|---|---|
| New single-player world | Awakening/tutorial/System/save creation pass. |
| Existing save | Migration retains levels, items, skills, gold, and shadows. |
| Death/respawn | Persistent data remains; overlay shows no stale transient abilities. |
| Dimension change | Player debug snapshot remains synchronized. |
| Logout during combat | Active-state line clears after reconnect. |
| Logout during dungeon | Session recovers or returns the player safely. |
| Restart during build | Build resumes or fails into recoverable cleanup. |
| Restart during boss | No duplicate reward; session remains recoverable. |
| Full inventory reward | Reward reaches System storage or explicit fallback. |
| Two-player gate | Party, teleport, objective, and exactly-once reward evidence. |
| One disconnects | Remaining player continues; disconnected player has defined rejoin behavior. |
| Dedicated server | Launches without client-class loading failures. |
| GUI scales 1-4 | Overlay and normal UI remain readable without inaccessible controls. |
| Maximum shadows | Tick measurements show no severe degradation or orphan entities. |

Record actual runtime results and evidence in [`ACCEPTANCE_TESTS.md`](ACCEPTANCE_TESTS.md). These commands make tests repeatable; they do not claim that a scenario passed without a real runtime execution.
