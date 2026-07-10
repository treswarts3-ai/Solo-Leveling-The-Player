# Parallel Workstream Ownership

Each implementation chat owns one feature branch. Shared files marked **integration-controlled** should be changed through small extension points or coordinated with the integration chat.

| Workstream | Primary ownership | Integration-sensitive dependencies |
|---|---|---|
| Core systems | `data/`, `config/`, common synchronization and persistence | `HunterData`, `CommonEvents`, network registration |
| System UI | `client/screen/`, HUD and client presentation | `SystemScreen`, sync packet payloads |
| Quests | `gameplay/QuestHandler.java`, quest-specific data and UI | `HunterData`, `CommonEvents`, `SystemScreen` |
| Abilities | `gameplay/AbilityHandler.java`, passive processing | `ServerActionHandler`, cooldown and mana data |
| Ruler's Authority | authority targeting, movement, flight and item-control code | `AbilityHandler`, action packets, client key handling |
| Shadow system | `gameplay/ShadowHandler.java`, storage, summon and AI | `HunterData`, death events, shadow UI |
| Advanced Shadow Monarch | Shadow Exchange, Monarch's Domain, Black Heart progression | quests, shadow handler, abilities and UI |
| Items and equipment | `registry/ModItems.java`, item classes and equipment effects | `HunterData`, creative tab, generated resources |
| Assets and presentation | `tools/`, textures, sounds, models, particles and animation adapters | resource names and item registry IDs |
| Integration and testing | `.github/`, documentation, shared files, merges and release packaging | all workstreams |

## Shared-file rule

Before editing an integration-sensitive file, record the intended change in the pull request description. Prefer adding a new class or helper over expanding a shared class with unrelated logic.
