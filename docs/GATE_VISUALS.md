# Ranked Gate Visuals

## Design basis

Solo Leveling gates are presented as luminous spatial rifts, usually with a bright blue core. A Red Gate is a dangerous abnormal state rather than a normal rank color. This implementation keeps the swirling, high-energy rift language while giving each gameplay rank a distinct readable palette.

## Rank palette

| Rank | Portal palette | Frame palette |
|---|---|---|
| E | Ice cyan | Polished deepslate and sculk |
| D | Emerald teal | Prismarine and sea lantern |
| C | Sapphire blue | Crying obsidian and lapis |
| B | Violet | Amethyst |
| A | Crimson | Polished blackstone and magma |
| S | Abyssal violet with gold-white arcs | Obsidian and gilded blackstone |

## Presentation

- Gates use a seven-block-wide rounded arch rather than a flat rectangular Nether-portal frame.
- The portal surface uses original 32×32 procedural animation frames with interpolation.
- Rank is stored in the portal block state, so saved gates refresh into the correct visual automatically.
- Particle type, light emission, ambient pitch, frame material, and accent blocks scale with rank.
- The trigger volume follows the larger arch while entry remains server-authoritative.
- Cleanup removes the entire managed gate footprint, including the thicker base anchors.

## Testing

Use `/sl dungeon create_gate <id> <rank> <template>` to compare E, D, C, B, A, and S gates. Existing template shortcuts continue to use the template's configured rank.
