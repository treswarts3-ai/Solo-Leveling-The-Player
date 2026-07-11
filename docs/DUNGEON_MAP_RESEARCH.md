# Dungeon Map Research and Architecture

## Primary technical sources

- Forge 1.20.x GameTest documentation: structure scenes are loaded from `.nbt` templates stored at `data/<namespace>/structures`, and relative positions are the supported way to address authored template locations. <https://github.com/MinecraftForge/Documentation/blob/1.20.x/docs/misc/gametest.md>
- Forge 1.20.x `StructureTemplate` patch: confirms server-side `placeInWorld`, placement settings, block processors, entity processors, bounding boxes, and placement flags in the exact Forge branch used by Minecraft 1.20.1. <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/patches/minecraft/net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate.java.patch>
- Forge 1.20.x GameTest implementation: demonstrates namespaced structure templates and server-run structure-backed validation on Java 17. <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/src/test_old/java/net/minecraftforge/debug/misc/GameTestTest.java>

## Chosen approach

The previous arena was authored directly through Java fill calls. That was adequate for a prototype, but it tied appearance, traversal metadata, and lifecycle code together and left two templates on a generic fallback room.

The replacement separates those responsibilities:

1. `tools/generate_dungeon_structures.py` owns deterministic authored geometry.
2. Four compressed vanilla structure NBT files own the shipped block layouts.
3. `DungeonArena` owns bounded underground placement, solid-shell restoration, entries, encounter centers, checkpoints, boss centers, and reward centers.
4. `DungeonRuntime` continues to own sessions, objectives, rewards, return points, failure, reconnect, and cleanup.

Structure templates were selected over random jigsaw generation because these are instanced combat maps. A fixed route makes encounter pacing, checkpoint doors, spawn safety, visual landmarks, cleanup bounds, and multiplayer recovery predictable. It also allows exact automated connectivity checks.

## Map construction rules

- Every map occupies an 89 by 19 by 89 template.
- Every active arena is built at Y -32.
- Before placement, a 93 by 25 by 93 volume is restored to a map-appropriate solid stone material.
- The authored template then carves only deliberate air and places floors, walls, ceilings, decoration, and lighting.
- No playable cell reaches the outer two-block side/floor shell or four-block roof shell.
- Every room connection is explicitly authored and validated after all decoration is placed.
- Spawn and objective centers are kept clear of furniture, raised trim, fluid, and narrow doors.
- Fluids are represented with protected glass/magma features so map decoration cannot flood a session.
- Runtime doors are separate from the NBT geometry and reopen from persisted objective progress.
- Cleanup restores stone instead of leaving a permanent void underground.

## Automated template results

| Map | Explicit blocks | Detail blocks | Connected floor cells | Checkpoints |
|---|---:|---:|---:|---:|
| Abandoned Subway | 36,670 | 14,326 | 2,489 | 4 |
| Red Orc Stronghold | 57,277 | 18,561 | 4,068 | 3 |
| Demon Castle Foyer | 64,778 | 19,646 | 3,459 | 4 |
| Cartenon Temple | 81,374 | 23,457 | 4,343 | 4 |

The generator exits nonzero if an entry is unsafe, an encounter/checkpoint is unreachable, a block lies outside the declared template, or a walkable route leaks into the shell.
