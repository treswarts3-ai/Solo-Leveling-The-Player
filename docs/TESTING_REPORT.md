# Testing Report

## Automated build checks

- Gradle clean build
- Java 17 compilation
- Forge reobfuscation task
- Resource packaging
- JAR output verification

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
- Loss-safe full-storage handling and indexed System Inventory retrieval
- Ability mana and cooldown checks
- Target line raycast and range checks
- Shadow capacity and duplicate prevention flags
- Shadow loot and XP suppression
- Penalty recovery command
- Missing optional dependency safety

## Runtime testing limitation

The deliverable is compile-verified. A full graphical client, two-player session, and long-running dedicated-server soak test cannot be executed inside the artifact build runner. Those remain manual acceptance tests after installing the JAR.
