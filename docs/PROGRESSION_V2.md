# Creative Hunter Progression

## Adaptive daily training

The old push-up, sit-up, and squat buttons are removed from normal progression. Daily training advances through ordinary play:

- defeat 8 hostile enemies
- sprint 600 blocks
- land 20 valid attacks
- jump or traverse 15 times
- successfully activate 5 abilities

The System quest screen displays current and target values and exposes only the server-authoritative reward claim button. The reward is marked claimed before XP, gold, stat points, mana, health, or the Blessed Box are granted, preventing duplicate claims.

### Daily exploit protections

- Ability damage and generated weapon-bonus damage do not count as ordinary attacks.
- Failed, locked, untargeted, insufficient-mana, and cooldown-blocked abilities do not count.
- Ability credit is rate-limited in addition to the global packet limiter.
- A jump counts only on a grounded-to-air transition with an additional tick guard.
- Sprint distance uses precise horizontal movement, requires grounded sprinting in the same dimension, and rejects large teleport-like deltas.
- Penalty mobs do not satisfy the kill objective.
- Every auxiliary counter, precise distance value, position sample, and debounce timestamp resets with the daily date.

The starting targets remain 8 kills, 600 sprint blocks, 20 attacks, 15 jumps, and 5 abilities. Together they require several types of normal play without making the first daily dependent on dungeon access.

## Five-level growth choices

Beginning at level 5, every five levels queues one persistent growth selection. Existing characters receive all earned selections that were not previously tracked, up to the safety cap. Multiple pending choices are handled one at a time.

### Vanguard

- +2 Strength
- +1 Stamina
- direct combat and durability

### Phantom

- +2 Agility
- +1 Sense
- mobility, evasion, and precision

### Arcane

- +3 Intelligence
- restores Mana to the new maximum
- ability, authority, and future shadow synergy

The System Stats screen shows pending choices, total selections in each path, reward summaries, and three server-validated buttons. The buttons disable when no choice is pending. `/slgrowth choose` remains the accessibility and recovery fallback.

## Ten-level major milestones

Beginning at level 10, every ten levels queues one persistent major reward choice. These rewards are deliberate rather than automatically unlocking every ability.

### Evolution

- +1 persistent Skill Evolution Token
- spent on a permanent ability branch from the Abilities screen

The first complete evolution pair is Quicksilver:

- **Phantom Step** - 16 mana and an 8-second cooldown, emphasizing frequent controlled movement.
- **Flash Execution** - 36 mana and a 14-second cooldown; requires a visible target within 10 blocks, moves to a safe arrival point, and attempts an agility-scaled finishing strike before granting the speed burst.

The server validates the unlocked base ability, token balance, variant ID, and one-time choice. The System asks for confirmation because the branch is permanent, synchronizes the effective mana/cooldown display, and never trusts the client to spend the token.

### Mastery

- +5 spendable stat points
- supports flexible build correction

### Cache

- +1000 System gold
- +1 Blessed Random Box in System inventory
- selection is blocked while System inventory is full so the reward cannot be lost

The Stats screen and `/slmilestone status` show pending rewards, historical choice totals, evolution-token count, and the next milestone level.

## Rank evaluation trials

Rank no longer needs to rise only because a level threshold was crossed. The first evaluation is E-Rank to D-Rank:

1. Reach level 10 while still E-Rank.
2. Begin the evaluation deliberately from the Stats screen or `/slrank begin`.
3. Defeat 10 hostile enemies within five minutes without dying.
4. Passing sets the persistent rank to D-Rank and grants the one-time D-Rank reward.
5. Death or timeout fails the attempt; retry becomes available after 60 seconds.

The one-time D-Rank reward is 5 stat points, 1000 gold, and 750 Hunter XP. A persisted grant flag prevents duplicate rewards. Shadows and penalty mobs do not count toward the trial.

## XP and dungeon integration

Generic combat Hunter XP skips entities tagged `sl_dungeon_enemy`. Dungeon enemies already suppress vanilla drops and XP and use the dungeon runtime's persisted one-time reward path, preventing generic and dungeon-specific XP from stacking unintentionally. Direct player kills still advance relevant quests and rank-trial objectives. Shadow kills are not assigned ambiguous player ownership.

Dungeon completion rewards remain owned by the dungeon system through its hooks and session-level `rewardGranted` protection. This progression work does not alter arena generation, gate blocks, objective sequencing, or dungeon cleanup.

## Commands

| Command | Purpose |
|---|---|
| `/slgrowth status` | Show pending growth choices, path totals, and the next five-level milestone |
| `/slgrowth choose <vanguard|phantom|arcane>` | Consume one pending growth choice |
| `/slgrowth reset <players>` | Operator recovery for growth-choice tracking |
| `/slmilestone status` | Show pending ten-level rewards, reward totals, tokens, and next milestone |
| `/slmilestone choose <evolution|mastery|cache>` | Consume one pending major milestone reward |
| `/slmilestone reset <players>` | Operator recovery for major-milestone tracking |
| `/slrank status` | Show current evaluation eligibility or active objective progress |
| `/slrank begin` | Deliberately begin the E-to-D evaluation |
| `/slrank reset <players>` | Operator recovery for an active/cooldown-blocked evaluation |

## Persistence and migration

All new values are stored inside the existing persistent Hunter data compound. No existing save fields are removed.

Growth fields:

- `pending_growth_choices`
- `next_growth_choice_level`
- `growth_vanguard`
- `growth_phantom`
- `growth_arcane`
- `last_growth_choice`

Major milestone fields:

- `pending_major_milestones`
- `next_major_milestone_level`
- `skill_evolution_tokens`
- `milestone_evolution`
- `milestone_mastery`
- `milestone_cache`
- `last_major_milestone_choice`
- `evolution_quicksilver`

Rank evaluation fields:

- `rank_progress_initialized`
- `rank_progress_tier`
- `rank_trial_eligible_notified`
- `rank_trial_active`
- `rank_trial_kills`
- `rank_trial_target`
- `rank_trial_end`
- `rank_trial_retry_after`
- `rank_trial_attempts`
- `rank_reward_d_granted`

Daily auxiliary fields include `activity_daily_date`, precise sprint distance, last sampled position/dimension, grounded state, and attack/jump/ability debounce timestamps.

### Existing-save behavior

- Current level, XP, stats, stat points, gold, inventory, skills, shadows, and unrelated quest progress remain untouched.
- Missing fields use safe defaults when first read.
- Existing high-level characters queue earned five-level and ten-level choices up to the safety cap.
- Characters that already displayed an automatically earned higher rank migrate that rank into the persistent rank tier instead of being demoted.
- New characters begin at E-Rank and must pass the evaluation to reach D-Rank.
- Hunter progression is copied through death/respawn, dimension changes, logout/login, and server restart.

## Remaining gameplay-test risks

Compilation validates registration and mappings but does not prove balance or presentation at every GUI scale. Hands-on tests should verify the compact Stats layout, five-minute evaluation pacing, early daily completion time, high-level migration with multiple pending rewards, and multiplayer kill attribution.
