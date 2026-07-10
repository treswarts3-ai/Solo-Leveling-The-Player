# Development Rules

## Technical baseline

- Minecraft Java Edition 1.20.1
- Forge 47.x
- Java 17
- Mojang mappings
- Server-authoritative gameplay state
- Dedicated-server-safe class loading

## Required validation

Every implementation branch must run:

```bash
./gradlew clean build --stacktrace --no-daemon
```

A branch is not ready for integration while compilation, tests, data generation, or resource processing fails.

## Shared architecture

- Persistent player state belongs in `HunterData` or a narrowly scoped data service.
- Client classes must never be loaded by common or dedicated-server code.
- Client packets request actions; the server validates and executes them.
- Packet handlers must validate awakening state, skill ownership, cooldown, mana, target, range, dimension, and rate limits.
- Registry names and NBT keys are permanent compatibility contracts after release.
- New persistent fields require a default value and migration behavior.
- Gameplay state must survive reconnect, dimension change, death cloning according to configuration, and server restart.

## Naming conventions

- Java package: `com.tre.sololeveling`
- Registry namespace: `sololeveling`
- NBT keys: lower snake case
- Network actions: upper snake case or `CATEGORY:value`
- Feature branches: `feature/<workstream>-<short-description>`
- Fix branches: `fix/<system>-<short-description>`

## Commit policy

- Commit one coherent change at a time.
- Never commit a broken build.
- Do not mix generated output with source changes.
- Do not commit `build/`, `.gradle/`, `run/`, logs, archives, or recovery chunks.
- Mention the affected workstream in the commit subject.

## Integration policy

1. Branch from the current `integration` commit.
2. Restrict edits to the assigned ownership area.
3. Rebase or merge the latest `integration` before handoff.
4. Run the required validation command.
5. Open a draft pull request targeting `integration`.
6. The integration owner merges one pull request at a time and reruns Gradle after every merge.
7. `main` only receives verified integration checkpoints.

## Conflict prevention

The following files are integration-sensitive and should not be edited by multiple workstreams without coordination:

- `HunterData.java`
- `CommonEvents.java`
- `ServerActionHandler.java`
- `ModItems.java`
- `SystemScreen.java`
- `ModNetwork.java`
- `SoloLevelingCommands.java`

Changes to those files should be minimal, documented in the pull request, and assigned to the integration owner when multiple systems are affected.
