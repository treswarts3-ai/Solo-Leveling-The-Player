# Solo Leveling: The Player

A Minecraft Forge 1.20.1 character-progression mod centered on the player: hunter XP, levels, stats, mana, equipment, quests, active skills, System interfaces, and a persistent shadow army.

## Requirements

- Minecraft Java Edition 1.20.1
- Forge 47.x
- Java 17
- No required third-party mod dependency

## Build

```bash
./gradlew clean build
```

The compiled mod is produced in `build/libs/`.

## Install

1. Install Forge for Minecraft 1.20.1.
2. Copy the generated `sololeveling-*.jar` into the instance `mods` folder.
3. Start Minecraft and create or open a world.
4. An operator can use `/sl awaken <player>`, or the player can open the System with **M** and select **Awaken System**.

## Current systems

- Persistent, server-authoritative Hunter data
- Hunter XP, levels, stats, mana, gold, ranks, jobs, titles, and skill unlocks
- Status, Skills, Quests, Store, Shadows, and System Inventory interfaces
- Daily exercises, penalty flow, tutorial, Job Change, Shadow Mastery, Black Heart, and emergency progression
- Stealth, Bloodlust, Quicksilver, Mutilation, Dagger Rush, Ruler's Authority, and Dragon's Fear
- Shadow extraction, storage, summon, dismissal, AI modes, Shadow Exchange, and Monarch's Domain
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

This repository is structured for parallel development through isolated feature branches and draft pull requests.

- Architecture and coding rules: [`DEVELOPMENT.md`](DEVELOPMENT.md)
- Workstream ownership: [`FILE_OWNERSHIP.md`](FILE_OWNERSHIP.md)
- PDF acceptance tracking: [`COMPLETION_MATRIX.md`](COMPLETION_MATRIX.md)
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

## Scope

Bosses are deliberately outside the current project scope. Development is focused on making the player, System, quests, equipment, abilities, and Shadow Monarch progression complete and stable first.
