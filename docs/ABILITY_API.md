# Ability Framework API

The ability system is server-authoritative. Clients may send existing `ABILITY:<id>`, `AUTHORITY:<mode>`, or `TOGGLE_DOMAIN` action requests, but the server decides whether activation succeeds.

## Public entry points

- `AbilityHandler.activate(ServerPlayer, String)` — compatibility entry point for ability packets and keybinds.
- `AbilityHandler.activateAuthority(ServerPlayer, String)` — compatibility entry point for Ruler's Authority modes.
- `AbilityHandler.abilityIds()` — canonical registered ability IDs.
- `AbilityHandler.definition(String)` — immutable metadata, including unlock, mana, cooldown, range, and scaling values.
- `AbilityHandler.cooldownData(ServerPlayer)` — HUD-facing read-only cooldown snapshots.
- `AbilityService.registry()` — registry access for integration code that needs full definitions.

## Shadow integration

The shadow workstream should install one adapter during common setup:

```java
AbilityIntegrationHooks.installShadowAdapter(new AbilityIntegrationHooks.ShadowAdapter() {
    @Override
    public AbilityResult exchange(ServerPlayer player) {
        // Validate shadow-specific state and perform exchange.
        return AbilityResult.success("Exchanged positions with a shadow.");
    }

    @Override
    public AbilityResult extract(ServerPlayer player) {
        // Perform extraction after the shared unlock/mana/cooldown checks.
        return AbilityResult.success("Shadow extracted.");
    }

    @Override
    public AbilityResult summon(ServerPlayer player) {
        // Summon through the shadow system's public API.
        return AbilityResult.success("Shadows summoned.");
    }
});
```

Returning `AbilityResult.failure(...)` refunds the shared mana charge and does not start the cooldown. Shadow adapters must not trust client-selected targets and must keep their own shadow-specific entity validation.

## Quest integration

The quest workstream can receive successful activations without editing ability implementations:

```java
AbilityIntegrationHooks.installQuestListener((player, definition) -> {
    // Progress ability-use objectives using definition.id().
});
```

The listener runs only after successful server activation and after the cooldown is applied.

## Lifecycle guarantees

- Ability state is canceled on death, logout, respawn, and dimension change.
- Login also clears transient state left by a hard server stop before the first client snapshot.
- Ruler's Authority releases held entities and revokes non-creative flight safely.
- Stealth, Mutilation, Quicksilver, Bloodlust, Dragon's Fear, and Monarch's Domain do not resume after reconnect.
- Unlocks, progression, mana, and cooldowns remain persistent; only live cast/stance state is cleared.

## Test commands

- `/sl ability list`
- `/sl ability info <ability>`
- `/sl ability unlock <player> <ability>`
- `/sl ability activate <player> <ability>`
- `/sl ability cooldown_clear <player>`
- `/sl ability mana_fill <player>`

The same commands are also available under `/sololeveling ability ...`.

## Registered roster

Movement: `dash`, `quicksilver`, `shadow_step`.

Combat: `dagger_mastery`, `mutilation`, `bloodlust`, `area_slash`, `dragons_fear`.

Utility: `stealth`, `rulers_authority` and its mode IDs, `enhanced_senses`.

Monarch: `monarch_domain`, `shadow_exchange`, `shadow_extraction`, `shadow_summoning`.

## Ability evolution

Quicksilver reads the server-owned `evolution_quicksilver` choice. `phantom_step` lowers mana and cooldown; `flash_execution` adds validated targeting, safe-position movement, and a finishing strike with higher cost and cooldown. Evolution selection is handled by progression authority and is not accepted from arbitrary ability packet data.
