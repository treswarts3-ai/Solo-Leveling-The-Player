# Combat and Visual Overhaul

## Ability damage safety

Generated ability and equipment damage now use a shared depth-aware guard. The guard preserves normal player kill credit while preventing generated hits from recursively receiving critical, dagger, equipment, daily-attack, or other normal-hit bonuses. Ability hits carry the ability ID, owner UUID, and hit time on the target for future integrations.

Targeting remains server-authoritative and bounded. Ray targets stop at blocks, area queries are capped, and abilities reject the caster, dead entities, unloaded entities, allied players, owned pets, and summoned shadows.

## Ability changes

### Quicksilver

- Immediate agility-scaled movement burst.
- Six-second controlled Speed and Jump effect.
- Low-count movement trail and distinct activation cue.
- Velocity is capped; no flight or permanent stacking is used.

### Dragon's Fear

- Expands outward over twelve ticks to an eighteen-block radius.
- Each hostile target is affected once per cast.
- Normal enemies lose aggression, flee, and receive Weakness and Slowness.
- Bosses keep their AI and receive reduced debuffs.
- Only visible enemies inside the bounded wave are considered.

### Mutilation

- Requires a dagger.
- Acquires a visible target within seven blocks.
- Uses a safe nearby position when the target is outside immediate dagger range.
- Executes four server-timed cuts with alternating swings and a brief movement restriction.
- Stops if the target dies, leaves range, becomes invalid, or the dagger requirement is lost.

### Dagger Rush

- Has its own implementation instead of aliasing Area Slash.
- Requires a dagger and a visible target within fourteen blocks.
- Selects a supported, collision-free arrival point near the target.
- Teleports only after destination validation and strikes once on arrival.
- Bosses receive reduced base damage.

### Ruler's Authority

- Pull, push, hold, throw, dash, and flight remain mechanically separate.
- Entity velocity is clamped.
- Bosses strongly resist movement and cannot be suspended.
- Hold drains mana, ends on obstruction, lost line of sight, excessive distance, death, logout, or dimension change, and lasts only two seconds on players.
- Temporary flight is shortened and always revoked through the shared cancellation path.

### Stealth

- Ten-second invisibility and movement effect with distinct entry and exit cues.
- Attacking, taking damage, or using another ability breaks stealth.
- Hostiles may lose detection when the player is outside close range and no longer visible.
- Death, logout, respawn, dimension change, and expiry clear state.

### Bloodlust

- Now functions as a self-focused offensive stance rather than a second fear ability.
- Grants temporary Strength and movement speed for eight seconds.
- The tradeoff is twenty percent increased incoming damage.
- State cannot stack permanently and is cleared by cancellation and lifecycle events.

### Monarch and shadows

- Monarch's Domain has a readable low-frequency boundary effect.
- Owned shadows gain short refreshed Strength and Resistance while inside an active domain.
- Shadow owner, same-owner shadow, pet, team, and non-PVP player protections remain enforced.
- Shadow storage and record persistence were not redesigned.

## Dungeon enemy roles

The role controller only visits entities already tracked by active dungeon sessions and processes at most the dungeon live-enemy cap.

- **Melee:** pursues directly and telegraphs a heavy close strike.
- **Fast:** closes quickly, telegraphs a lunge, and enters a short recovery period.
- **Tank:** applies slow pressure and telegraphs a visible radial slam.
- **Ranged:** maintains distance, repositions when cornered, and telegraphs shots.
- **Elite:** alternates a dash attack and a wider cleave.

## Boss presentation

- Abyssal Monarch attacks retain server-authoritative schedules and authored arena ownership.
- Cleave, stomp, and directional blast now require line of sight.
- Damage is limited to players belonging to the dungeon session.
- Phase two sends a clear notification and continues to guard add spawning against duplication.

## Performance protections

- No full-world entity scans were added.
- Ability area searches are capped at sixty-four targets and thirty-two blocks.
- Enemy role logic ticks every four server ticks and uses session-tracked UUIDs.
- Particles are capped and continuous effects use low-frequency, low-count bursts.
- Temporary state is changed only on activation, scheduled attacks, expiration, or lifecycle cleanup.

## Runtime test risks

A clean Forge build validates source and resources, but hands-on playtesting is still needed for collision edge cases, balance at high Hunter stats, latency during multi-hit abilities, AI navigation in authored rooms, particle readability with large parties, and texture readability at different GUI scales.
