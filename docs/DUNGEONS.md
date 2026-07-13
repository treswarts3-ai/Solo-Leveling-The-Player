# Dungeon and Gate System

## Active dungeon

The mod exposes exactly one dungeon:

| ID | Name | Rank | Scope |
|---|---|---:|---|
| `master` | Abyssal Necropolis | A | 173×50×265, 43 meaningful spaces, 13 major rooms, six regions, 21 floor elevations, six loops, three shortcuts, seven secrets |

The four former 89×19×89 maps and their NBT templates are removed from the active source tree.

## Player flow

1. Create the portal with `/sl dungeon open master` or `/sl dungeon generate master`.
2. Walk into the ranked portal surface.
3. The server validates access, allocates an isolated underground slot, builds the authored dungeon, records return points, and teleports the party into a safe staging room.
4. Nine objectives progress automatically through authored encounter spaces.
5. The Abyssal Monarch unlocks the Heart Vault when defeated.
6. Entering the vault grants one-time rewards and completes the session.
7. Exit, death, failure, logout recovery, and cleanup use checked safe teleports.

## Architecture

- Server-authoritative state persists in overworld `SavedData` under `sololeveling_dungeons`.
- Session arena slots begin near X/Z 50,000 and are spaced 224×320 blocks.
- The arena origin is Y -32.
- The authored bounds are 173×50×265 with five-block local protection around rooms and corridors.
- The layout is fixed and handcrafted. Runtime randomness affects combat/rewards, not architecture.
- Arena queries and cleanup are bounded to the session AABB.
- Placement skips identical states and uses client-update flags rather than full neighbor updates for every block.

## Regions

1. Gate Descent
2. Ossuary Ward
3. Abyssal Foundry
4. Buried Kingdom
5. Lower Catacombs
6. Monarch's Sepulcher

## Objectives

1. Break the Broken Bridge vanguard.
2. Recover death sigils in the Ossuary Ward.
3. Survive the Buried Kingdom plaza siege.
4. Defeat the Foundry overseers.
5. Unbind seals in the Monarch Basilica.
6. Break the Chained Prison wardens.
7. Clear the Sunken Court.
8. Defeat the Abyssal Monarch.
9. Enter the Heart Vault.

## Safety

- Safe player positions require empty feet/head collision, no fluid, valid world border, and sturdy flooring.
- Enemy spawn searches are bounded and reject blocked/liquid positions.
- Session records are created before party teleportation.
- Reward recipients are persisted before grants run.
- When every party member disconnects, encounter and objective timers pause for a one-minute reconnect grace period; a remaining party member keeps the run active.
- Completed sessions retain reconnect reward and safe-return recovery for five minutes before staged cleanup.
- Death, logout, reconnect, stale-session recovery, and cleanup preserve return safety.
- Old layout sessions are invalidated through arena layout version 5 and removed safely.

## Metrics

| Metric | Largest old map | Master | Multiplier |
|---|---:|---:|---:|
| Walkable floor area | 4,343 | 20,774 | 4.78× |
| Objective-route distance | ~184 | 848 | 4.61× |
| Meaningful rooms/spaces | 7 | 43 | 6.14× |
| Major rooms | ~5 | 13 | 2.6× |
| Distinct regions | 1 theme map | 6 connected districts | — |

## Build validation

```bash
python tools/generate_dungeon_structures.py --manifest /tmp/master-dungeon-metrics.json
./gradlew clean build --stacktrace --no-daemon
```

See `MASTER_DUNGEON_RESEARCH.md`, `MASTER_DUNGEON_ART_DIRECTION.md`, `MASTER_DUNGEON_LAYOUT.md`, and `MASTER_DUNGEON_BUILD_GUIDE.md` for full rationale and testing instructions.
