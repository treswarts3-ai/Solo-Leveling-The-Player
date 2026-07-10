# Creative Hunter Progression

## Adaptive daily training

The old push-up, sit-up, and squat buttons are removed from normal progression. Daily training now advances through actions performed during ordinary play:

- defeat 8 enemies
- sprint 600 blocks
- land 20 attacks
- jump or traverse 15 times
- successfully activate 5 abilities

The System quest screen tracks all five objectives and exposes only the reward claim button. Progress is server-authoritative and resets with the existing daily reset flow.

## Five-level growth choices

Beginning at level 5, every five levels queues one persistent growth selection. Existing characters receive selections for milestones they have already crossed. A player may hold multiple pending selections.

### Vanguard

- +2 Strength
- +1 Stamina
- intended for direct combat and durability

### Phantom

- +2 Agility
- +1 Sense
- intended for mobility, evasion, and precision

### Arcane

- +3 Intelligence
- restores Mana to the new maximum
- intended for ability-heavy and future authority/shadow builds

Normal automatic stat growth and spendable stat points remain intact. Growth selections are an additional build-defining layer rather than a replacement.

## Commands

| Command | Purpose |
|---|---|
| `/slgrowth status` | Show pending selections, path totals, and the next milestone level |
| `/slgrowth choose vanguard` | Consume one pending selection for Vanguard growth |
| `/slgrowth choose phantom` | Consume one pending selection for Phantom growth |
| `/slgrowth choose arcane` | Consume one pending selection for Arcane growth |
| `/slgrowth reset <players>` | Operator recovery command that resets milestone tracking and path totals |

## Persistence and migration

Growth data is stored inside the existing persistent Hunter data compound:

- `pending_growth_choices`
- `next_growth_choice_level`
- `growth_vanguard`
- `growth_phantom`
- `growth_arcane`
- `last_growth_choice`

No existing save fields are removed. Characters created before this system automatically queue all five-level choices earned by their current level, up to the safety cap.

## Next progression work

- Add dedicated rank evaluation trials instead of purely automatic rank changes.
- Add ten-level skill evolution choices.
- Connect growth paths to upgraded ability variants.
- Add full growth-choice cards to the Stats screen; the command interface is the reliable fallback until that UI is complete.
