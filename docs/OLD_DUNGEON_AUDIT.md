# Old Dungeon Audit

## Baseline

The audit covers the four active layout-v3 dungeons that existed on `main` before the master rebuild. The baseline branch passed Forge Build run 309 before the rebuild branch was created. The old generator was executed with its manifest output to obtain exact dimensions, explicit block counts, carved-air counts, palette-state counts, walkable floor cells, objectives, and checkpoints.

All four maps used the same 89×19×89 template envelope and the same 93×25×93 restored underground shell. Each consisted of seven rectangular room calls and six axis-aligned corridor calls. This repeated topology was the largest structural quality problem: theme changed, but scale, rhythm, room count, route density, vertical range, and pacing remained nearly identical.

## Measurements

| Dungeon | Dimensions | Explicit blocks | Structural/detail blocks | Carved air | Walkable floor cells | Rooms | Corridors | Objective route | Branches | Vertical floor levels |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Abandoned Subway | 89×19×89 | 36,670 | 14,326 | 22,344 | 2,489 | 7 | 6 | ~171 blocks | 1 | 1 primary level |
| Red Orc Stronghold | 89×19×89 | 57,277 | 18,561 | 38,716 | 4,068 | 7 | 6 | ~155 blocks | 2 | 1 primary level |
| Demon Castle Foyer | 89×19×89 | 64,778 | 19,646 | 45,132 | 3,459 | 7 | 6 | ~177 blocks | 1 | 2 shallow levels |
| Cartenon Temple | 89×19×89 | 81,374 | 23,457 | 57,917 | 4,343 | 7 | 6 | ~184 blocks | 1 | 2 shallow levels |

The objective-route estimates use Manhattan travel between the authored entry and successive objective centers. They are conservative and reproducible. Cartenon Temple was the largest old map by walkable area and objective-route length.

## Shared visual problems

1. **Repeated cuboid grammar.** Every map was assembled from seven rectangular `room` calls and six straight `corridor_x`/`corridor_z` calls. Material swaps could not conceal the identical room-and-tunnel silhouette.
2. **Flat architectural surfaces.** The generic room primitive produced one-block planar walls, a flat floor, and a flat ceiling before sparse decoration. Most room volume remained visually uniform.
3. **Insufficient vertical design.** The 19-block template height restricted floors to a narrow band. Stairs, overlooks, stacked routes, shafts, bridges, and long vertical reveals were minimal or absent.
4. **Decoration as isolated props.** Repeated helper calls for rubble, crates, chain lights, pillars, and benches created object placement without enough architectural hierarchy or environmental logic.
5. **No large-scale composition.** Each map had local landmarks but no distant destination visible across multiple regions, no city-scale central landmark, and no major compression/release sequence.
6. **Low route density.** Most paths were linear. Optional wings were short and did not form meaningful loops, shortcuts, or return routes.
7. **Theme breadth exceeded build depth.** Four themes split development effort. None received enough unique geometry, transition design, ceiling work, or refinement.

## Per-map findings

### Abandoned Subway

- **Palette:** deepslate, smooth stone, polished andesite, iron, cyan/blue accents, blackstone reward areas.
- **Gameplay spaces:** entry, ticket hall, platform/train, maintenance plant, security wing, boss terminal, reward vault.
- **Strengths:** strongest functional storytelling of the four; recognizable train and station language; 47 palette states.
- **Visual weaknesses:** long rectangular platform dominates the map; train and ticket props sit inside generic boxes; ceiling treatment repeats; reward wing changes palette abruptly.
- **Layout weaknesses:** nearly linear six-connector chain; one shallow side reward route; no meaningful vertical loop.
- **Technical weaknesses:** template-specific coordinates and door planes were hardcoded in `DungeonArena`; the same fixed 89×19×89 envelope constrained future expansion.
- **Reusable components:** safe entry search, encounter markers, checkpoint barrier logic, session lifecycle, train/industrial motif ideas.
- **Deleted components:** NBT template, layout registry entry, map-specific boss branch, command/UI entry, documentation entry.

### Red Orc Stronghold

- **Palette:** cobbled deepslate, blackstone, basalt, dark oak, red nether brick, magma and bone accents.
- **Gameplay spaces:** cave entrance, war yard, barracks, forge, camp, commander hall, vault.
- **Strengths:** clear functional zoning and readable warm/cool contrast.
- **Visual weaknesses:** rooms remain rectangular despite fortress theme; repeated palisade/pillar patterns; props do not change the underlying seven-box plan.
- **Layout weaknesses:** east/west side rooms reconnect weakly; most progression follows one flat spine; no long reveal or layered battlement route.
- **Performance weaknesses:** 57,277 explicit states for only seven rooms and one main floor, indicating a poor content-to-write ratio.
- **Reusable components:** safe enemy placement and foundry palette concepts.
- **Deleted components:** NBT, registry/template definition, commands/UI/docs references.

### Demon Castle Foyer

- **Palette:** blackstone, obsidian, red nether brick, basalt, magma, gold.
- **Gameplay spaces:** arrival bridge, nave, forge, chapel, throne room, sovereign vault.
- **Strengths:** strongest high-rank color identity and broad boss-room concept.
- **Visual weaknesses:** the “castle” remained a foyer-scale series of boxes; repeated black/red surfaces flattened depth; large empty air volumes inflated explicit structure size.
- **Layout weaknesses:** minimal branching and shallow vertical change; the throne destination was not used as a persistent orientation landmark.
- **Technical weaknesses:** boss behavior fell through to a generic Iron Sovereign path shared with another map.
- **Reusable components:** blackstone/gold climax palette and dramatic bridge concept.
- **Deleted components:** NBT, template definition, generic boss selection branch, command/UI/docs references.

### Cartenon Temple

- **Palette:** stone brick, smooth stone, polished andesite, quartz, gold, redstone-light accents.
- **Gameplay spaces:** commandment hall, statue gallery, trials, judgment chapel, idol arena, reliquary.
- **Strengths:** largest old walkable area, clear statue motif, most imposing single-room scale.
- **Visual weaknesses:** symmetrical statue repetition became mechanical; the temple still used the same seven-room/six-corridor grammar; palette-state count was only 25 despite the largest explicit-block count.
- **Layout weaknesses:** 4,343 cells were mostly on one broad plane; only short side content; no interconnected lower/upper temple routes.
- **Performance weaknesses:** 81,374 explicit blocks and 57,917 carved-air entries made it the heaviest old template while offering only seven meaningful rooms.
- **Reusable components:** monumental scale, watcher/statue motif, final-arena readability.
- **Deleted components:** NBT, template definition, Architect's Idol branch, commands/UI/docs references.

## References removed

The rebuild removes:

- Four `.nbt` files under `data/sololeveling/structures/dungeons`.
- Four layout records from `DungeonArena`.
- Four template definitions and their map-specific wave sets from `DungeonContent`.
- Old map-specific boss naming/reinforcement branches.
- Four client UI cards.
- Old command examples and documentation tables.
- The obsolete four-map NBT generator and old map research document.

The gate block, rank visuals, saved-data model, party/session lifecycle, safe teleport search, encounter spawning, reward hooks, death/reconnect recovery, and bounded cleanup are retained because they are reusable infrastructure rather than old-map content.
