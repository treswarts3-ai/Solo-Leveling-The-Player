# Commands

The root aliases are `/sl` and `/sololeveling`. Administrative mutations require permission level 2.

| Command | Purpose |
|---|---|
| `/sl status [player]` | Show level, rank, XP, mana, gold, and stats |
| `/sl awaken <player>` | Awaken the System |
| `/sl system revoke <player>` | Revoke awakening |
| `/sl xp add|set|remove <player> <amount>` | Change hunter XP |
| `/sl level add|set|remove <player> <amount>` | Change hunter level |
| `/sl statpoints add|set <player> <amount>` | Change unspent points |
| `/sl stat set|add <player> <stat> <value>` | Change a primary stat |
| `/sl stat reset <player>` | Reset primary stats and refund points |
| `/sl gold add|set|remove <player> <amount>` | Change System gold |
| `/sl mana add|set|remove <player> <amount>` | Change current hunter mana |
| `/sl skill unlock|lock <player> <skill>` | Change a skill unlock |
| `/sl skill cooldown_clear <player>` | Clear skill cooldowns |
| `/sl daily reset <player>` | Reset today's daily counters |
| `/sl quest complete <player>` | Mark daily quest complete |
| `/sl penalty send|return <player>` | Enter or recover from the penalty arena |
| `/sl shadow clear <player>` | Clear stored and active shadows |
| `/sl shadow dismissall <player>` | Dismiss active shadows |
| `/sl shadow capacity add <player> <amount>` | Increase storage capacity |
| `/sl giveall <player>` | Grant all registered mod items |
| `/sl givemegodpowers <player>` | Grant the complete operator test loadout, unlocks, maximum progression and infinite test gold |
| `/sl sync <player>` | Force an owner-data sync |
| `/sl dungeon open master` | Spawn the A-rank Abyssal Necropolis portal |

### Ability test controls

| Command | Purpose |
|---|---|
| `/sl ability list` | List every registered ability ID |
| `/sl ability info <ability>` | Show unlock, mana, cooldown, range, and scaling metadata |
| `/sl ability activate <player> <ability>` | Run the normal server-authoritative activation path |
| `/sl ability cooldown_clear <player>` | Clear ability cooldowns |
| `/sl ability mana_fill <player>` | Restore mana |
| `/sl ability evolution_reset <player> quicksilver` | Clear the permanent Quicksilver branch and restore one test token |

### Phase 2 testing and debugging

| Command | Purpose |
|---|---|
| `/sl test setup` | Preserve the current player and load the controlled test fixture |
| `/sl test reset` | Restore the preserved player data and inventory |
| `/sl test progression|combat|dungeon|shadows|multiplayer` | Run one structured diagnostic suite |
| `/sl debug overlay` | Toggle the synchronized developer overlay |
| `/sl debug player|dungeon|packets|entities|performance` | Print measured subsystem state |

Full fields and expected runtime evidence are documented in [`DEBUGGING.md`](DEBUGGING.md).


## Master dungeon commands

| Command | Purpose |
|---|---|
| `/sl dungeon generate master [seed]` | Spawn the fixed master-dungeon gate; seed is accepted for test workflow compatibility |
| `/sl dungeon enter master` | Create a debug gate, enter, and start the dungeon immediately |
| `/sl dungeon exit` | Return to the saved safe location |
| `/sl dungeon regenerate master` | Clear and rebuild the active master arena |
| `/sl dungeon delete master` | Delete the active session and restore its arena envelope |
| `/sl dungeon validate master` | Validate objective markers, support, collision, and liquids |
| `/sl dungeon info master` | Show measured layout information |
| `/sl dungeon debug bounds` | Display particles at the authored outer corners |
| `/sl dungeon debug shell` | Run shell/marker validation |
| `/sl dungeon debug encounters` | List authored encounter regions |
| `/sl dungeon debug lighting` | Describe the active lighting palette |
| `/sl dungeon teleport <region>` | Teleport safely to an authored debug region |
| `/sl dungeon complete` | Complete the current dungeon for testing |

## Master dungeon generation behavior

Entering or regenerating `master` queues staged generation rather than changing the whole arena in one tick. Use `/sl dungeon inspect_session` to see percentage, elapsed job ticks, changed blocks, and the enforced per-tick maximum. Portal entry can queue the dungeon start; it begins automatically after validation and safe teleportation.
