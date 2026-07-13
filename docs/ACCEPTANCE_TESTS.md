# Release Acceptance Tests

This is the authoritative version 1.0 runtime gate. Record the exact commit, JAR SHA-256, Forge version, Java version, tester, date, and evidence link for every run. `Implemented` and `build passed` never mean `runtime passed`.

## Test record

| Field | Value |
|---|---|
| Commit | Unrecorded |
| JAR SHA-256 | Unrecorded |
| Minecraft / Forge | 1.20.1 / 47.4.10 |
| Java | 17 |
| Tester and date | Unrecorded |

Use `PASS`, `FAIL`, or `BLOCKED`. A release requires every required row to be `PASS` against the same commit and JAR.

## Required matrix

| Area | Required test | Result | Evidence / issue |
|---|---|---|---|
| Build | Fresh checkout runs `./gradlew clean build`; expected JAR exists | BLOCKED | Run in CI or a Java 17 environment with Gradle dependencies |
| Client | Launch, create world, awaken through UI, save and reload | BLOCKED | |
| Progression | Earn XP naturally, level multiple times, allocate stats, complete daily, make growth and milestone choices | BLOCKED | |
| Persistence | Verify death, logout/login, dimension change, restart, and an older save migration | BLOCKED | |
| Abilities | Test all registered abilities for success, cooldown, low mana, locked state, invalid target, interruption and death | BLOCKED | |
| Controls | Rebind every ability, detect conflicts, verify six HUD slots, restart and confirm bindings persist | BLOCKED | |
| Dungeon | Enter Abyssal Necropolis through its gate; finish every objective and boss; receive reward; exit; verify cleanup | BLOCKED | |
| Dungeon recovery | Test death, `/sl dungeon exit`, disconnect/reconnect, failure, regeneration and deletion | BLOCKED | |
| Reward safety | Complete with full inventory/System storage and verify exactly-once delivery after reconnect | BLOCKED | |
| Shadows | Extract, fail extraction, fill storage, summon to cap, command modes, restart, dismiss and clean up | BLOCKED | |
| UI | GUI scales 1-4 in windowed/fullscreen; check 16:9 and ultrawide; no clipping or HUD overlap | BLOCKED | |
| Dedicated server | Launch without client-class errors; join and play for 30 minutes | BLOCKED | |
| Multiplayer | Two players enter/finish one dungeon; verify party ownership, credit, rewards, shadows and reconnect | BLOCKED | |
| Performance | Profile master generation, full shadow cap, boss plus shadows, repeated abilities and session cleanup; server tick below 50 ms | BLOCKED | |
| Security | Reject malformed/duplicate/rate-limited ability, UI, reward and dungeon requests without state corruption | BLOCKED | |
| Documentation | README, commands, controls, balancing and changelog match the tested JAR | BLOCKED | |

## Fast operator setup

- `/sl givemegodpowers <player>` grants the complete test loadout.
- `/sl ability list` and `/sl ability info <ability>` expose the registered ability set and requirements.
- `/sl dungeon enter master` starts the master-dungeon test route.
- `/sl dungeon inspect_session` reports staged generation progress and its per-tick work cap.
- `/sl dungeon validate master` checks structure markers, support, collision, and liquids.
- `/sl data inspect <player>` and `/sl status [player]` capture progression state before and after a test.

Admin commands accelerate setup only. The normal-player loop must also pass without them.

## Failure handling

For each failure, record reproduction steps, expected and actual behavior, log excerpt, world/player conditions, and an issue link. Fixes invalidate affected results; retest the changed feature plus persistence, multiplayer ownership, and reward duplication when relevant.
