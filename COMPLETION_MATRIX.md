# Completion Matrix

Status values: **Verified**, **Implemented—needs gameplay testing**, **Partial**, **Not started**.

| Area | Status | Current checkpoint | Remaining acceptance work |
|---|---|---|---|
| Forge 1.20.1 / Java 17 build | Verified | GitHub CI clean build passed | Continue validating every merge |
| Persistent Hunter data and migration | Implemented—needs gameplay testing | Defaults, migration, clone/login/dimension synchronization | Long-session and old-save testing |
| XP, levels, stats, mana, gold, ranks and jobs | Implemented—needs gameplay testing | Core progression and admin controls exist | Balance and boundary testing |
| System HUD and interfaces | Implemented—needs gameplay testing | Unified responsive System screen, configurable HUD, notification queue, ability slots and key-conflict feedback | GUI scales 1-4, windowed/fullscreen, ultrawide, overlap and accessibility review |
| Commands | Implemented—needs gameplay testing | Progression, ability, quest, shadow, dungeon, inspection and god-powers test controls | In-game permission, tab-completion and failure-path verification |
| Daily exercises and penalty flow | Implemented—needs gameplay testing | Timed exercise sessions, interruption and penalty flow | Multiplayer anti-spam and reset testing |
| Main progression quests | Implemented—needs gameplay testing | Tutorial, dagger training, Job Change, Shadow Mastery and Black Heart | Narrative polish and edge-case testing |
| Active and passive abilities | Implemented—needs gameplay testing | Listed core skills and passives function server-side | Balance, interruption, animation and PvP testing |
| Ruler's Authority | Partial | Push/pull/item control and expanded authority controls | Flight safety, targeting polish and multiplayer tests |
| Shadow extraction and storage | Implemented—needs gameplay testing | Imprints, extraction, storage, summon and dismissal | Restart, duplicate and cap stress testing |
| Shadow AI modes | Implemented—needs gameplay testing | Follow, Guard, Passive and Aggressive modes | Pathfinding and crowded-server performance |
| Shadow Exchange | Partial | Safe exchange validation exists | Cross-dimension policy and failure recovery |
| Monarch's Domain | Partial | Domain behavior exists | Presentation, balance and multiplayer scope |
| Black Heart progression | Partial | Quest and persistent progression fields exist | Full staged upgrade path and UI feedback |
| Weapons, armor and accessories | Implemented—needs gameplay testing | Signature items, two armor sets, accessories and unique effects | Acquisition balance and tooltip audit |
| Item models, PNG textures and HTML sources | Implemented—needs visual review | Generated asset pipeline and editable HTML gallery | Hand-polish important signature assets |
| Original sounds and particles | Partial | Procedural OGG sounds and vanilla-particle presentation | Sound mix, custom particles and animation timing |
| Player and weapon animations | Partial | Vanilla poses and feedback | Optional animation dependency or safe custom adapter |
| Dedicated-server compatibility | Implemented—needs runtime testing | Common/client package separation and CI compilation | Actual dedicated server launch and two-player session |
| Performance and packet security | Partial | Packet rate limits, server validation and reduced shadow sync | Profiling, malformed packet tests and large-shadow stress test |
| Abyssal Necropolis master dungeon | Implemented—needs gameplay testing | Staged generation, marker validation, encounters, boss, rewards, exit and cleanup | Full gate-to-cleanup run, death/reconnect/full-inventory/two-player cases |
| Documentation and release package | Partial | Core docs, CI artifact packaging and a single acceptance checklist | Keep commands, controls, balancing and changelog synchronized with release JAR |
