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
- Unified Player, Stats, Abilities, Inventory, Equipment, Quests, Shop, Dungeons, Shadows, and Settings interfaces
- Configurable responsive HUD with six ability slots, cooldowns, quest tracking, opacity, scaling, anchoring, offsets, compact mode, and minimal mode
- Daily exercises, penalty flow, tutorial, Job Change, Shadow Mastery, Black Heart, and emergency progression
- Stealth, Bloodlust, Quicksilver, Mutilation, Dagger Rush, Ruler's Authority, Dragon's Fear, and every registered active ability
- Shadow extraction, storage, summon, dismissal, AI modes, Shadow Exchange, and Monarch's Domain
- Persistent gates and four authored underground dungeon maps with objectives, timers, recovery, rewards, enemies, elites, and bosses
- Signature weapons, armor, accessories, equipment sets, runes, keys, potions, materials, and story items
- Original generated PNG textures, editable HTML pixel-art sources, particles, OGG sounds, and original System UI icon assets

## Controls

| Action | Default key |
|---|---|
| Open or close the System | M |
| Open Shadow tab | B |
| Toggle System HUD | H |
| Dash | Left Alt |
| Mutilation | R |
| Dagger Rush | V |
| Shadow Extraction | G |
| Shadow Exchange | X |
| Quicksilver | Z |
| Ruler's Authority | C |

All implemented abilities appear under **Options > Controls > Solo Leveling**. Unassigned abilities can be bound there. The System's Abilities tab shows current keys, conflicts, cooldowns, requirements, and six configurable HUD slots.

## Development workflow

This repository is structured for parallel development through isolated feature branches and pull requests. The integrated release candidate is maintained on `main`.

- Architecture and coding rules: [`DEVELOPMENT.md`](DEVELOPMENT.md)
- Workstream ownership: [`FILE_OWNERSHIP.md`](FILE_OWNERSHIP.md)
- Acceptance tracking: [`COMPLETION_MATRIX.md`](COMPLETION_MATRIX.md)
- Merge state and current checkpoint: [`INTEGRATION_STATUS.md`](INTEGRATION_STATUS.md)
- Contribution workflow: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Commands and player documentation: [`docs/`](docs/)

## Documentation

- [System UI overhaul](docs/UI_OVERHAUL.md)
- [Commands](docs/COMMANDS.md)
- [Controls](docs/CONTROLS.md)
- [Dungeons](docs/DUNGEONS.md)
- [Balancing](docs/BALANCING.md)
- [Asset manifest](docs/ASSET_MANIFEST.md)
- [Research notes](docs/RESEARCH.md)
- [Testing report](docs/TESTING_REPORT.md)
- [Changelog](CHANGELOG.md)

## Release status

Version 1.0.0 is a compiled development prerelease. Automated Forge builds pass, but gameplay balance, long-session persistence, dedicated-server runtime behavior, multiplayer stress, and final visual acceptance still require hands-on testing.
