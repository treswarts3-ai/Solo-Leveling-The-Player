# Master Dungeon Structure-Template Workflow

## Runtime contract

`master` is the only active dungeon ID. Java owns sessions, staged placement, marker validation, objectives, checkpoints, encounters, rewards, recovery, and cleanup. Artistic geometry is loaded from twelve replaceable structure modules; `MasterDungeonBuilder` contains no room-carving or decoration pass.

Normal structure-block exports under `data/sololeveling/structures/dungeons/master/<module>.nbt` take priority. The checked-in `.snbt` files under `data/sololeveling/dungeon_modules/master/` are text-form vanilla structure templates that provide a deterministic starter pack and CI baseline.

## Modules

| ID | Size | Primary purpose |
|---|---:|---|
| `00_entry` | 31x18x32 | Arrival, gate landmark, safe staging |
| `01_descent` | 31x18x27 | Compressed traversal and vertical transition |
| `02_outer_necropolis` | 48x20x40 | Crypt exploration, first combat, secret |
| `03_guardian_hall` | 48x20x40 | Shielded/tank front line and ranged pressure |
| `04_catacombs` | 47x22x44 | Tomb groups, secret, unlockable shortcut |
| `05_collapsed_bridge` | 47x20x34 | Narrow traversal under ranged pressure |
| `06_prison` | 47x22x41 | Cells and deliberate wave lanes |
| `07_elite_chamber` | 47x22x41 | Elite skill check and optional cache |
| `08_ritual_depths` | 47x24x44 | Interruptible ritual encounter |
| `09_boss_approach` | 31x20x27 | Short final skill check and buildup |
| `10_boss_arena` | 47x28x47 | Boss telegraphs, lateral routes, throne landmark |
| `11_reward_vault` | 31x20x31 | Reward presentation and exit |

Every axis remains within vanilla's 48-block structure limit.

## In-game authoring and export

1. Use a dedicated creative development world and a building tool such as WorldEdit.
2. Load the starter module or build inside the exact module dimensions above. Keep connector openings five blocks wide and five blocks tall.
3. Add a recognizable silhouette, primary landmark, secondary detail layer, deliberate floor/wall/ceiling treatment, readable route lighting, and combat geometry appropriate to the module purpose.
4. Place structure blocks in `DATA` mode at gameplay locations. Set metadata to one of the marker IDs below. The marker occupies the player's feet position and must have sturdy flooring plus two clear blocks.
5. Save with entities disabled and structure voids only where existing world blocks must remain untouched.
6. Name the export `sololeveling:dungeons/master/<module_id>` and copy the resulting NBT from the development world's `generated/sololeveling/structures/dungeons/master/` folder into `src/main/resources/data/sololeveling/structures/dungeons/master/`.
7. Reload resources, generate a fresh session, run validation, and complete a real walkthrough before approval.

Binary NBT takes priority over the starter SNBT, so an artist can replace one module without changing Java or regenerating the other eleven.

## Marker vocabulary

- Required flow: `entry`, `objective:0` through `objective:6`, `boss`, `reward`, `exit`
- Encounters: `enemy:melee`, `enemy:ranged`, `enemy:tank`, `enemy:elite`, `enemy:summoner`
- Progression: `door:0` through `door:7`, `checkpoint:<name>`
- Optional content: `secret:<id>`, `shortcut:<id>`
- Presentation: `audio:<id>`, `particle:<id>`

The loader rejects missing/moved required markers, duplicate markers, invalid palette indices, out-of-bounds blocks, modules over 48 blocks on any axis, command blocks, barriers, bedrock, and jigsaw blocks. The pack must retain at least two secrets and one shortcut.

## Placement and cleanup

Each session receives an isolated arena slot. Module volumes are cleared and template blocks are placed through restartable jobs capped at 16,384 visits and 4,096 mutations per server tick. Rebuild and cleanup restore only the twelve owned module volumes to deepslate. Arena layout version 6 migrates pre-template sessions through the normal recovery path.

## Validation

```bash
python tools/generate_dungeon_structures.py \
  --check src/main/resources/data/sololeveling/dungeon_modules/master \
  --manifest /tmp/master-dungeon-metrics.json
./gradlew clean build --stacktrace --no-daemon
```

```text
/sl dungeon enter master
/sl dungeon validate master
/sl dungeon inspect_session
/sl dungeon debug bounds
/sl dungeon teleport <entrance|necropolis|guardian|catacombs|bridge|prison|elite|ritual|arena|reward>
```

Runtime validation checks the complete pack contract, authored lighting minimum, marker clearance, sturdy floors, liquids, and containment corners. A clean build is not a substitute for the required in-game route, spawn, door, secret, shortcut, boss, reconnect, reward-overflow, and cleanup walkthrough.
