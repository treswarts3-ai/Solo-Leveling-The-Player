# Phase 3 Runtime Reliability and Data Safety

## Hunter data

Hunter data uses schema version 5. Access performs ordered migration, fills only missing owned fields, repairs malformed owned lists, clamps progression resources and pending-choice counts, and preserves unrelated keys in the player's persistent compound. Existing versioned records emit one `[HUNTER DATA]` migration log when upgraded; fresh players do not produce migration noise.

Death cloning copies the complete Hunter compound. Login, respawn, logout, death, and dimension changes cancel transient abilities; login and respawn then apply the equipment-aware mana cap, rebuild managed attributes, and synchronize an authoritative snapshot. Growth and milestone thresholds remain persisted beside their bounded pending counts so a restart cannot queue the same choice twice.

## Dungeon state machine

The persisted lifecycle is:

`WAITING -> BUILDING -> READY -> ACTIVE -> BOSS -> REWARD -> COMPLETED -> CLEANING -> CLOSED`

Any non-closed gameplay state may fail into `FAILED`, followed by `CLEANING -> CLOSED`. Recovery/admin rebuilds are explicit transitions back to `BUILDING`. `DungeonSession` rejects all other transitions. Version-1 `WAITING` records migrate to `READY`, and legacy `CLEANUP` records migrate to `CLEANING`.

Each saved gate and session is loaded independently. A malformed record is rejected without discarding healthy records, one aggregate warning is logged, and repaired saved data is marked dirty. Invalid return-point UUIDs, dimensions, non-finite coordinates, and coordinates outside conservative world bounds cannot trap a player; recovery falls back to a checked overworld spawn.

Tracked dungeon entities are discarded during failure/rebuild/cleanup, arena clearing remains staged, disconnected sessions pause encounter clocks, and completed sessions retain exactly-once reward and return recovery for five minutes.

## Network authority

The action packet is bounded to 128 characters and every action is checked on the logical server. The handler validates live/non-spectator/chunk-loaded state, uses a 20-packet-per-second ceiling, debounces irreversible mutations, validates stat/growth/milestone/evolution/ability/authority/store/slot IDs, and never accepts client damage, reward quantity, currency values, or stat values.

Malformed, impossible, rate-limited, and unknown actions increment the synchronized rejection counter. Server warnings are limited to one per player per five seconds to avoid log flooding.

## Acceptance status

Code paths and compilation are automated gates. Death/restart, old-save migration, two-player reconnect, full-storage rewards, dedicated-server launch, and malformed-packet fuzzing still require the runtime evidence recorded in `ACCEPTANCE_TESTS.md`; implementation alone is not a runtime pass.
