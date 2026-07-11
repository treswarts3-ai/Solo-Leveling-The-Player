# Master Dungeon Build Guide

## Active ID

`master`

Display name: **Abyssal Necropolis**

## Construction

`MasterDungeonBuilder` contains the fixed room list, graph links, palettes, landmarks, objective centers, checkpoints, region debug points, bounds, cleanup envelopes, and validation.

`DungeonArena` owns session integration:

- isolated slot origin
- resumable build/rebuild/migration/cleanup jobs
- hard limits of 16,384 block visits and 4,096 changed blocks per server tick
- saved job intent and idempotent restart after reload
- safe entry only after marker validation
- encounter/boss/reward coordinates
- checkpoint opening
- validation
- debug bounds
- ranked gate rendering and collision trigger

The Python tool `tools/generate_dungeon_structures.py` no longer emits NBT. It mirrors the authored graph, verifies connectivity and scale requirements, and emits deterministic metrics for CI.

## Commands

```text
/sl dungeon generate master
/sl dungeon generate master <seed>
/sl dungeon open master
/sl dungeon enter master
/sl dungeon exit
/sl dungeon regenerate master
/sl dungeon delete master
/sl dungeon validate master
/sl dungeon info master
/sl dungeon debug bounds
/sl dungeon debug shell
/sl dungeon debug encounters
/sl dungeon debug lighting
/sl dungeon teleport <entrance|bridge|ossuary|foundry|plaza|basilica|prison|archive|court|arena|reward>
/sl dungeon complete
```

The optional seed is accepted for workflow compatibility; the architecture is intentionally fixed.

## Normal player flow

1. Operator runs `/sl dungeon open master` or `/sl dungeon generate master`.
2. An A-rank portal appears in front of the player.
3. Walking into the portal creates a persisted `BUILDING` session and stores return points while the arena is generated in bounded server-tick batches.
4. Players remain outside the arena and receive progress messages until validation succeeds; the party is then teleported to the safe staging room and the queued start request is applied.
5. Objectives spawn at authored centers and open checkpoints automatically.
6. Boss defeat opens the Heart Vault.
7. Entering the vault grants the one-time reward and completes the session.
8. Exit, death, completion cleanup, or recovery returns players to a checked safe position; the arena slot is released only after staged cleanup finishes.

## Editing rules

- Do not add selectable dungeon IDs.
- Do not use random room dimensions or connections.
- Add a named `Room` and explicit `Link` for every new space.
- Update the mirrored Python `SPACES`, `EDGES`, critical path, and metrics.
- Maintain at least five blocks of shell around all playable cavities.
- Keep encounter centers on sturdy, liquid-free floors with two-block clearance.
- Add a purpose to every dead end.
- Update art direction and layout documentation when changing region identity.

## Validation

Run:

```bash
python tools/generate_dungeon_structures.py --manifest /tmp/master-dungeon-metrics.json
./gradlew clean build --stacktrace --no-daemon
```

In game:

```text
/sl dungeon enter master
/sl dungeon validate master
/sl dungeon debug bounds
/sl dungeon teleport arena
```

Validation checks authored objective markers for collision, liquid, and sturdy floors. CI also rejects the four deleted IDs and old NBT files, requires the hard generation budgets, and rejects restoration of a synchronous full-dungeon build API.

`/sl dungeon inspect_session` reports live job percentage, changed blocks, elapsed job ticks, and the previous build's maximum changes in one tick.

## Known limitations

- The initial blueprint is code-authored rather than exported from a visual editor.
- Clean compilation does not prove perceived visual quality or hardware-specific tick duration.
- The block mutation ceiling is enforced in code, but a complete in-game walkthrough and dedicated-server millisecond profile remain required before a public release.
- Seed variation affects no architecture by design.
