# Combat and Ability Baseline

This document records the behavior on `main` at commit `c15d4e916198ccf81bfa08f0833165ab007030c9` before the combat overhaul.

## Shared framework

- `AbilityService` validates awakened state, unlocks, cooldowns, and mana on the server.
- Mana is refunded when an ability reports failure.
- Successful abilities receive a generic activation sound/particle burst and a system message.
- `AbilityEffects.dealDamage` sets a temporary `sl_ability_damage` boolean on the caster and uses a normal player-attack damage source.
- Generated ability and equipment bonus damage are skipped by critical-hit and dagger-mastery modifiers in `CommonEvents`.
- Target searches use bounded AABBs and reject summoned shadows, same-team players, creative players, spectators, unloaded targets, and cross-dimension targets.

## Major abilities

| Ability | Baseline behavior |
|---|---|
| Dash | Applies one facing-direction velocity burst. |
| Quicksilver | Applies Speed and Jump Boost for 10 seconds and emits one particle burst. |
| Shadow Step | Ray-targets a living entity and teleports behind it when the destination is clear. |
| Mutilation | Requires a dagger, attempts a blink, and deals one high-damage hit. |
| Bloodlust | Slows and weakens nearby monsters and clears mob targets. It does not buff the caster. |
| Dagger Rush | Does not have its own implementation; it aliases to Area Slash, a forward-cone multi-target hit. |
| Stealth | Applies invisibility and Speed for 15 seconds and records stealth state. Damage, attacking, logout, and dimension change clear stealth through existing hooks. |
| Ruler's Authority | Provides push, pull, hold, throw, dash, and temporary flight modes. Velocity and target resistance are basic; hold teleports the target in front of the player. |
| Monarch's Domain | Toggles a persistent mana-draining flag and emits a one-time particle burst. |
| Dragon's Fear | Immediately clears targets, applies Weakness/Slowness, and knocks nearby monsters away. |
| Shadow abilities | Delegate to the shadow subsystem for exchange, extraction, and summoning. |

## Shadow combat

- Summoned shadows carry owner and record UUID tags.
- Shadow AI runs every five ticks, supports follow/guard/passive/aggressive modes, and uses bounded target searches.
- The attack-cancellation hook protects owners, same-owner shadows, allied targets, and owned pets.
- Shadows reconcile persistent records and active entity IDs; logout/dimension change dismisses active shadows.

## Equipment damage

- Equipment hit hooks apply status effects and bonus damage.
- Bonus damage uses `sl_weapon_bonus` to avoid recursively receiving critical and dagger modifiers.
- Bonus damage does not share a richer generated-damage context with abilities.

## Dungeon enemies and bosses

- Enemy roles select different vanilla entity bases and attributes, but role-specific active combat logic is minimal.
- The active Abyssal Monarch has telegraphed ring, cleave, stomp, and directional blast attacks.
- Boss damage queries are bounded, but line-of-sight checks are not applied before area or cone damage.

## Presentation

- Ability icons and item textures exist, but style and readability are inconsistent.
- Major abilities reuse a small set of generic particles and the same custom ability sound.
- Combat feedback is primarily chat/action-bar text plus activation particles.
