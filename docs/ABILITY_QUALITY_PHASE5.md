# Phase 5 — Combat and Ability Quality

Phase 5 gives each flagship ability an explicit quality contract and keeps gameplay authority on the logical server. The client can request an ability and play a server-issued presentation event; it never decides hits, damage, movement, mana, cooldowns, progression, or rewards.

## Shared cast contract

Every flagship definition declares its role, animation identifier, startup, active, recovery, failure recovery, interruption rules, movement tolerance, and progression path. `AbilityService` performs the common sequence:

1. Validate player, unlock, cooldown, target, and start position.
2. Reserve mastery-adjusted mana.
3. Broadcast the startup presentation event.
4. Revalidate the prepared target when startup completes.
5. Resolve the ability on the server at the declared timing.
6. Apply cooldown, mastery, combo, quest, sound, and presentation feedback only after success.
7. Enter normal or failure recovery; damage, attacks, movement, and lifecycle events can interrupt eligible startup phases.

Failed or interrupted startup refunds its reserved mana. Lifecycle cancellation clears active casts, combos, movement/stance state, held targets, temporary flight, and stealth.

## Flagship completion matrix

| Ability | Role and readability | Phase 5 behavior |
|---|---|---|
| Quicksilver | High-speed repositioning; accelerate, active trail, recovery | Collision-safe burst, controlled turning, afterimage trail, Flash Execution momentum finisher, evolution and mastery scaling |
| Mutilation | Single-target four-cut execution | Prepared target, dagger and range revalidation, alternating server-timed strikes, damage interruption, final impact, tier-two cone finisher, combo payoff |
| Dagger Rush | Targeted gap closer | Server target indicator, sampled collision path, safe arrival slash, failure recovery, Quicksilver combo, tier-three kill reset |
| Dragon's Fear | Expanding crowd-control wave | Buildup, expanding visible ring, one response per cast, boss resistance, cross-cast diminishing duration |
| Ruler's Authority | Weight-aware telekinetic control | Pull, push, hold, throw, dropped-object, dash, and flight modes; bounded force, weight classes, boss resistance, hold mana drain |
| Stealth | Infiltration and opener stance | Readable fade/distortion, reduced detection, server-calculated 1.5x rear backstab, reveal on attack/damage/ability/lifecycle |
| Shadow Exchange | Tactical shadow relocation | Deterministic owned-shadow selection, startup destination preview, same-dimension and two-way collision validation, invalid feedback, server transition |
| Monarch's Domain | Shadow empowerment area stance | Mastery-scaled visible boundary, restrained particles, mana-draining stance, buffs only for owned shadows inside the boundary |

## Mastery and combos

Resolved uses are stored per ability. Mastery tiers unlock at 10, 30, and 75 uses and improve mana efficiency, cooldown, range, and power. Ability-specific benefits include Mutilation's cone finisher, Dagger Rush's kill reset, and a larger Domain. Quicksilver can feed Dagger Rush or Mutilation within a short server-owned combo window; Dagger Rush can feed Mutilation.

## Multiplayer and performance policy

Targets must be alive, loaded, same-dimension, non-allied, non-owned pets, and non-shadow entities. Player targets additionally respect the PVP configuration, creative/spectator immunity, and teams. Area queries remain capped at 64 entities and 32 blocks. Particles are capped, trails are low-count, and Domain/shadow effects are distance-gated.

## Verification

`python tools/validate_ability_quality.py` checks the eight quality contracts, network direction, server resolution order, bounded targeting, mastery/combos, and absence of authoritative operations in client visuals. Forge CI then compiles, tests, reobfuscates, and packages the mod. Hands-on timing, latency, balance, collision edge cases, and multi-player readability remain acceptance-test responsibilities.
