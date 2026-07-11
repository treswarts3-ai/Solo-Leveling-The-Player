# Master Dungeon Research

## Research question

How should one large Forge 1.20.1 dungeon be authored so it looks handcrafted, remains sealed underground, supports the existing server-authoritative dungeon loop, and can be validated and maintained without random room spam?

## Sources reviewed

### Primary technical sources

- Forge 1.20.x documentation: https://docs.minecraftforge.net/en/1.20.x/
  - sides and logical-server authority
  - events and lifecycle integration
  - saved data
  - particles and sounds
  - debugging and profiling
- WorldEdit clipboard and schematic documentation: https://worldedit.enginehub.org/en/latest/usage/clipboard/
  - clipboard regions, origins, rotation, paste behavior, and Sponge `.schem` storage
- Axiom project documentation/site: https://axiom.moulberry.com/
  - professional in-game editing workflow and large-scale builder tooling
- Minecraft 1.20.1 mapped APIs already compiled by this project:
  - `StructureTemplate`
  - `StructurePlaceSettings`
  - `ServerLevel#setBlock`
  - chunk access and world-border checks
  - collision, fluid, and sturdy-face queries

### Existing project evidence

- Four deterministic structure templates and their source generator.
- Forge Build workflow and byte-comparison validation.
- Dungeon saved data, sessions, gates, encounter spawning, boss behavior, death/reconnect recovery, reward locking, and cleanup.
- Prior map manifest containing exact block and walkability metrics.

### Level-design synthesis

The layout applies established principles shared by action RPGs, immersive sims, Soulslike areas, survival-horror spaces, MMORPG dungeons, and strong Minecraft adventure maps:

- critical path plus optional content
- loops and unlockable shortcuts
- landmark navigation
- compression and release
- prospect and refuge
- risk/reward branches
- encounter escalation
- readable combat arenas
- foreshadowed destinations
- spatial storytelling

These principles are translated into Minecraft movement constraints: one-block steps, jump clearance, two-block player height, mob pathfinding width, line-of-sight, chunk boundaries, light levels, and block-scale landmarks.

## Why weak AI-generated Minecraft maps look artificial

Common failure modes were identified from the old implementation and cross-checked against professional building practice:

1. **Primitive-first design:** random boxes are generated before the location has a functional plan.
2. **Uniform wall depth:** every wall is a flat plane, so palette changes read as wallpaper.
3. **Noise mistaken for detail:** blocks are alternated at high frequency without structural or weathering logic.
4. **Equal emphasis:** every surface receives similar contrast, preventing focal points.
5. **Unmotivated props:** barrels, chains, rubble, and cobwebs are scattered without showing use, history, or route information.
6. **Repeated room proportions:** different spaces share the same width, height, and doorway rhythm.
7. **No ceiling composition:** builders focus on floor plans and leave ceilings flat or empty.
8. **No long-range navigation:** rooms are isolated scenes rather than parts of a coherent spatial sequence.
9. **Size inflation:** empty halls, inaccessible shell, or long corridors are counted as content.
10. **Theme mixing:** unrelated motifs are combined instead of developing one visual language deeply.

## Rules adopted

- Every region has a function, silhouette, dominant material, accent, and landmark.
- Materials appear in large patches; variation follows damage, moisture, heat, or ceremonial hierarchy.
- Major rooms use different proportions and ceiling heights.
- Structural depth comes from ribs, pillars, arches, dais geometry, bridges, shafts, galleries, and recessed floors.
- Optional dead ends contain a secret, reward, overlook, story scene, or shortcut.
- Critical-path lighting is more legible than optional-path lighting.
- Repeated motifs establish cohesion, but spacing and scale vary.
- Combat centers provide sturdy floors, two-block clearance, lateral movement, and bounded spawn searches.
- Five-block minimum shell protection surrounds authored rooms and connectors.
- Runtime variation may change encounters and rewards, not the architectural graph.

## Construction-method comparison

| Method | Visual control | Runtime reliability | Revision cost | Performance | Decision |
|---|---|---|---|---|---|
| Direct ad-hoc Java fills | Low–medium | Medium | High | Can be poor | Rejected: encourages primitive room spam |
| One giant NBT template | High | Medium | High | Large decode/place burst | Rejected: unwieldy at 173×50×265 and difficult to review |
| Multiple NBT region templates | High | High with careful seams | Medium | Good placement path | Viable, but would require an external build/export cycle not available in this repository |
| Jigsaw structure | Medium | Medium | Medium | Good | Rejected: variation is not a priority and seam/pool complexity adds risk |
| WorldEdit/Axiom-authored schematic | Very high | Depends on import path | Medium | Good after conversion | Recommended for future visual iteration, but not as a runtime dependency |
| Dedicated dimension | High isolation | High after setup | High | Good | Rejected for this milestone: changes world architecture and migration scope |
| Fixed isolated underground region | High | High | Medium | Bounded | Selected |
| Deterministic data-driven Java blueprint | High when coordinates are authored | High | Medium | Requires bounded writes | Selected for the repository-native implementation |

## Selected architecture

The active dungeon is a deterministic handcrafted Java blueprint placed in isolated overworld slots beginning around X/Z 50,000 at Y -32. Slot spacing is 224 blocks on X and 320 blocks on Z, preventing overlap for the 173×50×265 authored bounds.

The blueprint is not a random generator. Forty-three named spaces, all links, room heights, palettes, objective centers, secrets, loops, shortcuts, landmarks, and final-arena geometry are fixed. The Python design validator mirrors the graph and metrics for CI.

### Why not retain the four NBT maps

- They split refinement effort across four themes.
- Their shared 89×19×89 envelope prevented the required scale and verticality.
- Their topology was nearly identical.
- Keeping them selectable would preserve obsolete commands, UI, balancing, and test burden.

### Why not use a new dimension now

A custom dimension would provide stronger isolation, but it expands the change into dimension JSON, worldgen registration, migration, respawn behavior, compatibility, and save handling. The existing isolated-slot architecture already preserves return points, sessions, cleanup, and multiplayer behavior. The master dungeon therefore uses a far-underground isolated region with a thick local shell.

## Underground placement conclusions

- The master bounds are 173×50×265 relative to the session origin.
- The origin remains at Y -32, placing authored floors from Y -44 to Y -18 and the highest protected geometry at approximately Y 0.
- Every room and corridor receives at least five blocks of deepslate protection beneath, above, and beside the playable cavity.
- Fluids are not used as uncontrolled exterior features.
- Safe player and mob placement require empty collision, empty fluids, valid world border, and sturdy flooring.
- Arena slots are separated beyond their full bounds.
- Cleanup restores only the authored room/corridor envelopes rather than scanning the entire world.

## Performance conclusions

The old implementation already avoided full-world entity scans and used client-update block flags. The rebuild retains those protections and adds:

- deterministic bounds
- skipped writes when the target state already matches
- no block entities except the reward chest
- no random jigsaw search
- no entity placement in the blueprint
- bounded chunk loading for the session arena
- bounded entity cleanup inside the session AABB
- low-frequency gate animation and encounter logic

The main remaining performance risk is first-time construction of a substantially larger map. CI can prove compilation and deterministic design metrics, but it cannot measure client frame time or server tick duration. Runtime profiling remains a release playtest requirement. A future optimization path is exporting each of the six regions to reviewed structure templates or staging blueprint operations over several ticks.

## Layout conclusions

The final plan uses six regions:

1. Gate Descent
2. Ossuary Ward
3. Abyssal Foundry
4. Buried Kingdom
5. Lower Catacombs
6. Monarch's Sepulcher

The player repeatedly sees and re-enters the central kingdom, creating orientation and shortcut value. West/east wings form loops. The route descends across 21 distinct floor elevations. The throne destination is foreshadowed before the final approach.

## Lighting conclusions

- Cyan/sea-lantern light establishes the gate and early route.
- Soul lanterns define ossuary, kingdom, catacomb, and sepulcher architecture.
- Shroomlight and magma identify the foundry.
- Landmark lighting is brighter and more regular than optional-path lighting.
- Combat centers avoid total darkness.
- The final arena uses high-value sea-lantern/gold accents against blackstone and obsidian.

## Rejected approaches

- Random cuboid rooms.
- Noise-based palette mixing.
- Four replacement themes.
- Retaining old maps as debug-only selections.
- Sky placement.
- One-block shell walls.
- Counting inaccessible shell as playable size.
- Runtime dependency on WorldEdit or Axiom.
- Procedural graph generation.

## Testing strategy

1. Run the Python graph/metric validator.
2. Assert all four old IDs and NBT files are absent from active dungeon code, UI, commands, and docs.
3. Run the clean Forge Java 17 build.
4. Use `/sl dungeon generate master` and walk through the portal.
5. Validate safe entry and each objective center.
6. Walk every region, branch, secret, loop, shortcut, and final route.
7. Test death, exit, reconnect, completion, reward locking, regeneration, deletion, and multiplayer.
8. Use debug bounds and validation commands.
9. Profile generation and cleanup on a dedicated server.

## Risk assessment

| Risk | Mitigation | Residual risk |
|---|---|---|
| First-build tick spike | bounded writes, skipped identical states, no neighbor updates | requires runtime profiling |
| Corridor/room seam | fixed graph and post-build marker validation | visual seam review still required |
| Unsafe spawns | sturdy-floor/collision/fluid search | mob-specific pathfinding requires playtest |
| Old saved sessions | layout version bump; removed-template sessions fail and clean safely | old active players may be returned on next tick |
| Navigation confusion | six region identities, central landmark, lighting, loops, shortcuts | needs full human walkthrough |
| Visual repetition | distinct region palettes, ceiling heights, landmarks, architectural families | code-authored detail cannot replace final in-game art pass |
| Oversized cleanup | bounded room/link envelopes | cleanup tick impact needs profiling |
