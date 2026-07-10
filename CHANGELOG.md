# Changelog

All notable changes to Solo Leveling: The Player are documented here.

## [1.0.0] - 2026-07-10

### Added

- Persistent, server-authoritative Hunter progression with XP, levels, stats, mana, gold, ranks, jobs, titles, and unlocks.
- System HUD and interfaces for status, skills, quests, store, shadows, inventory, notifications, and cooldowns.
- Reusable ability framework with validation, mana costs, cooldowns, targeting, scaling, and integration hooks.
- Active and passive abilities including Stealth, Bloodlust, Quicksilver, Mutilation, Dagger Rush, Ruler's Authority, Dragon's Fear, Shadow Exchange, Shadow Extraction, and Monarch's Domain.
- Quest framework with persistent state, starter progression, repeatable content, rewards, and operator testing commands.
- Persistent shadow extraction, storage, summoning, dismissal, AI modes, capacity rules, and restart recovery.
- Equipment framework with weapons, armor, accessories, set bonuses, upgrades, acquisition metadata, and signature items.
- Gate and dungeon framework with persistent sessions, objectives, timers, waves, elites, boss encounters, rewards, cleanup, and reconnect recovery.
- Original generated item models, PNG textures, GUI assets, sounds, particles, and editable asset sources.
- Forge 1.20.1 CI build and downloadable artifact packaging.

### Fixed

- Resolved conflicting equipment enum imports that prevented the integrated release candidate from compiling.
- Preserved server/client package separation and server-authoritative state transitions across integrated workstreams.

### Known limitations

- This version is a prerelease and still needs hands-on single-player, dedicated-server, multiplayer, persistence, balance, stress, GUI-scale, sound-mix, and visual acceptance testing.
- Some animations and presentation use safe vanilla-compatible fallbacks.
- Balance values and progression pacing are provisional.
