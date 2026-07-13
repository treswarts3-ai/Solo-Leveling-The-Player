# Testing Report

## Automated build checks

- Gradle clean build
- Java 17 compilation
- Forge reobfuscation task
- Resource packaging
- JAR output verification

## Reproducible diagnostics

Phase 2 adds reversible `/sl test setup` and `/sl test reset` fixtures, subsystem test commands, a synchronized developer overlay, packet rejection counters, server tick measurements, bounded entity counts, dungeon generation workload, and transition-only structured dungeon logs. See [`DEBUGGING.md`](DEBUGGING.md).

Phase 3 adds Hunter schema-v5 migration/clamping, guarded dungeon lifecycle transitions, per-record saved-data recovery, checked return coordinates, transient-state lifecycle cleanup, strict action-ID validation, mutation debouncing, and rate-limited rejection logs. See [`RUNTIME_RELIABILITY.md`](RUNTIME_RELIABILITY.md).

## Code-path checks included

- Player login synchronization
- Death clone persistence
- Dimension-change synchronization
- Multiple level-up loop
- Non-negative XP, gold, mana, and stat points
- Hunter-data schema migration for existing worlds
- Health and mana regeneration scaling
- Critical-hit and evasion bounds
- Server-side packet validation
- Unknown/malformed action rejection with bounded diagnostics
- Independent malformed dungeon-record recovery and legacy lifecycle migration
- Loss-safe full-storage handling and indexed System Inventory retrieval
- Ability mana and cooldown checks
- Target line raycast and range checks
- Shadow capacity and duplicate prevention flags
- Staggered shadow target acquisition and suspended far-distance searches
- Shadow loot and XP suppression
- Penalty recovery command
- Penalty arena construction capped at 512 placement operations per server tick with persisted resume state
- Missing optional dependency safety

## Runtime testing limitation

The deliverable is compile-verified. A full graphical client, two-player session, and long-running dedicated-server soak test cannot be executed inside the artifact build runner. Those remain manual acceptance tests after installing the JAR.

Runtime results must be recorded in [`ACCEPTANCE_TESTS.md`](ACCEPTANCE_TESTS.md) against an exact commit and JAR hash. This report describes covered code paths; it is not evidence that those paths passed in Minecraft.
