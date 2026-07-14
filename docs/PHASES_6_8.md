# Phases 6–8 — Content Quality and Shadow Army

This delivery keeps combat, progression, rewards, and shadow commands authoritative on the logical server. Existing save records migrate in place; the dungeon session remains the boundary for enemy processing and rewards remain exactly-once.

## Phase 6 — enemies and Abyssal Monarch

| Role | Readable loop | Counterplay |
|---|---|---|
| Melee | two light strikes, telegraphed heavy, recovery | face and block the heavy, punish recovery |
| Assassin | circle, telegraphed lunge, retreat | track lateral movement and punish retreat/recovery |
| Tank | slow approach, frontal defense, area slam | flank the defense or punish post-slam recovery |
| Ranged | preserve distance, reposition, telegraph shot | break line of sight or close distance |
| Summoner | avoid melee, channel up to two reinforcement casts | damage interrupts the channel |
| Elite | dash/cleave pattern and a half-health transition | learn both patterns; punish every recovery |

The Abyssal Monarch has a locked arena introduction, five learnable attacks (cleave, stomp, sovereign line, phase-two stomp, crossing fissures), line-of-sight checks, explicit recovery windows, a half-health arena transition, and pressure fissures after prolonged non-engagement. The phase transition changes arena positioning and does not depend on an ordinary-mob wave. Boss removal still advances the dungeon objective and the persistent reward pipeline still grants each member once.

## Phase 7 — rank path, loot, and quests

The evaluation path is `Awakening → E → D → C → B → A → S → Job Change → Shadow Monarch`.

| Evaluation | Identity |
|---|---|
| E → D | basic hostile combat under a timer and without death |
| D → C | timed dungeon clear |
| C → B | elite defeat plus three objectives |
| B → A | solo raid with at most three recovery events |
| A → S | boss clear with damage, survival, and successful-ability score |
| Job Change | S-rank story-dungeon clear plus job objectives |

Equipment definitions expose rarity, source, usable level range, stat budget, sell value, upgrade limit, and next upgrade cost. The master dungeon adds an Epic/Legendary equipment roll above its core reward, and owned duplicates convert into 65% of their sell value. Upgrades consume both a catalyst and visible gold cost, making each upgrade a resource decision. Signature equipment continues to use ability hooks rather than only larger attributes.

Quest events now cover enemy roles, encounter constraints, secrets/shortcuts, successful combat styles, equipment upgrades, and shadow development. Authored secret markers use bounded player-proximity checks and cannot progress from key presses alone.

## Phase 8 — shadow army

Every stored record carries `Level`, `Rank`, `XP`, `Role`, `Name`, `Owner`, `Active`, `Trait`, and `Evolution Eligibility`. Named shadows receive one deterministic behavior trait: Bulwark, Executioner, Soul Siphon, or Blink Guard.

The `B` key opens the eight-action command wheel:

`Summon | Dismiss | Follow | Guard | Attack | Hold Position | Return | Formation`

Formation cycles through Follow, Defensive Ring, Forward Assault, Ranged Rear Line, and Boss Focus. All actions are requests; the server validates ownership and mutates AI state. AI only iterates the owner's capped active-ID set, uses staggered bounded local target searches, suspends cross-dimension work, teleports safely after stuck detection, and reconciles persisted entity/record state. Stored shadows cap at 128 and active shadows remain config- and capacity-bounded.

Visual identity uses a restrained particle budget, violet names, glow silhouettes, named-shadow eye accents, and the owner's name in every active shadow label. Ambient particles stop beyond 24 blocks.

## Verification

- `python tools/validate_phases_6_8.py`
- `python tools/validate_ability_quality.py`
- `python tools/validate_equipment.py`
- `./gradlew clean build`

Runtime acceptance should exercise each enemy counter, interrupt a summoner, complete every rank trial, reconnect during a dungeon reward, issue all eight shadow commands with two players online, and restart with active shadows to verify cleanup/reconciliation.
