# Solo Leveling: The Player

A Minecraft Forge 1.20.1 character-progression mod centered on the player: hunter XP, levels, stats, mana, equipment, quests, active skills, System interfaces, gates, dungeons, bosses, and a persistent shadow army.

## Requirements

- Minecraft Java Edition 1.20.1
- Forge 47.4.10 or newer in the Forge 47.x line
- Java 17
- No required third-party mod dependency

## Download

Download the latest JAR from this repository's **Releases** page. The first public build is published as a prerelease because full in-game and multiplayer acceptance testing is still in progress.

## Install

1. Install Forge for Minecraft 1.20.1.
2. Copy `sololeveling-1.0.0.jar` into the instance `mods` folder.
3. Start Minecraft and create or open a world.
4. An operator can use `/sl awaken <player>`, or the player can open the System with **M** and select **Awaken System**.

## Build from source

```bash
./gradlew clean build
```

The compiled mod is produced in `build/libs/`.

## Current systems

- Persistent, server-authoritative Hunter data
- Hunter XP, levels, stats, mana, gold, ranks, jobs, titles, and skill unlocks
- Status, Skills, Quests, Store, Shadows, and System Inventory interfaces
- Daily exercises, penalty flow, tutorial, Job Change, Shadow Mastery, Black Heart, and emergency progression
- Stealth, Bloodlust, Quicksilver, Mutilation, Dagger Rush, Ruler's Authority, and Dragon's Fear
- Shadow extraction, storage, summon, dismissal, AI modes, Shadow Exchange, and Monarch's Domain
- Persistent gates and dungeon sessions with objectives, timers, recovery, rewards, enemies, elites, and boss encounters
- Signature weapons, armor, accessories, equipment sets, runes, keys, potions, materials, and story items
- Original generated PNG textures, editable HTML pixel-art sources, particles, and OGG sounds

## Controls

| Action | Default key |
|---|---|
| Open System | M |
| Primary ability | R |
| Secondary ability | V |
| Shadow Extraction | G |
| Shadow menu | B |
| Shadow Exchange | X |
| Quicksilver | Z |
| Ruler's Authority | C |
| Toggle HUD | H |
| Dodge | Left Alt |

## Development workflow

This repository is structured for parallel development through isolated feature branches and pull requests. The integrated release candidate is maintained on `main`.

- Architecture and coding rules: [`DEVELOPMENT.md`](DEVELOPMENT.md)
- Workstream ownership: [`FILE_OWNERSHIP.md`](FILE_OWNERSHIP.md)
- Acceptance tracking: [`COMPLETION_MATRIX.md`](COMPLETION_MATRIX.md)
- Merge state and current checkpoint: [`INTEGRATION_STATUS.md`](INTEGRATION_STATUS.md)
- Contribution workflow: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Commands and player documentation: [`docs/`](docs/)

## Documentation

- [Commands](docs/COMMANDS.md)
- [Controls](docs/CONTROLS.md)
- [Balancing](docs/BALANCING.md)
- [Asset manifest](docs/ASSET_MANIFEST.md)
- [Research notes](docs/RESEARCH.md)
- [Testing report](docs/TESTING_REPORT.md)
- [Changelog](CHANGELOG.md)

## Release status

Version 1.0.0 is a compiled development prerelease. Automated Forge builds pass, but gameplay balance, long-session persistence, dedicated-server runtime behavior, multiplayer stress, and visual polish still require hands-on testing.