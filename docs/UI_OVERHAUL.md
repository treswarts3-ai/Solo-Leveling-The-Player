# System UI Overhaul

## Scope

This overhaul replaces the prototype interface with one responsive client-side System shell while preserving the existing server-authoritative progression, abilities, inventory, equipment, quests, shop, dungeons, and shadow systems.

The design uses original dark translucent panels, cyan highlights, violet accents, sharp pixel borders, a 128×128 icon atlas, custom controls, bounded scrolling, and non-pausing screens. No copyrighted images are included.

## Baseline audit

Before the overhaul, the client UI consisted primarily of one 634-line `SystemScreen`, one HUD event class, and one utility class.

| Existing surface | How it opened | Main problems found |
|---|---|---|
| System screen | `M` | Vanilla buttons, nine inconsistent tabs, no dungeon page, fixed arrays, rendering/input/networking in one class, no remembered tab |
| Shadow page | `B` | Opened a second System screen directly, could replace unrelated screens, actions lacked explanations |
| HUD | Automatic after awakening; `H` toggled it | Four hardcoded abilities, only four corner anchors, no opacity or offsets, stacked notification overlap |
| Stats | System > Stats | One-point buttons sent immediately, no bulk confirmation, no base/bonus explanation |
| Skills | System > Skills | Progression-skill array did not match the complete registered ability set, no binding workflow, incomplete metadata |
| System inventory | System > Inventory | Small paged grid with weak instructions and immediate retrieval packets |
| Equipment | System > Equipment | Mostly read-only text, weak separation of weapon, armor, and accessories |
| Quests | System > Quests | Fixed daily layout, limited hierarchy, scrolling accepted outside the content region |
| Store | System > Store | Immediate purchases, no expensive-purchase confirmation, weak product descriptions |
| Notifications | HUD overlay | Several could render simultaneously and cover a large central area |

Additional issues found:

- System and Shadow keys could replace other open screens.
- Ability keybinds could activate while another screen or chat was open.
- The HUD hotbar did not represent all registered abilities.
- Client data used fallback keys instead of the synchronized `xp_needed` and `hunter_rank` fields.
- Client settings lacked opacity, element visibility, position offsets, minimal mode, and action-slot persistence.
- Irreversible actions depended only on the broad packet-rate ceiling rather than action-specific debounce windows.
- There were no dedicated reusable button, tab, or confirmation components.

No `AbstractContainerScreen` or custom menu-backed inventory was present. The overhaul therefore retains Minecraft's normal inventory authority rather than replacing it with an unsafe client-only container.

## System Menu

Default key: **M**. Escape closes the menu. The last selected tab is stored locally and restored next time.

The menu is non-pausing and will not open over chat, inventory, pause, or another unrelated screen. Pressing M while the System Menu is already open closes it cleanly.

Available tabs:

- **Player** — level, rank, XP, health, mana, gold, stat points, primary equipment, active quest, dungeon state, and shadow capacity.
- **Stats** — five primary stats, base and synchronized bonus values, effect descriptions, +1 upgrades, and confirmed +5 upgrades.
- **Abilities** — every definition in `AbilityService.registry()`, not a fixed list. Shows name, category, description, mana, cooldown, range, unlock requirement, keybind, conflict state, readiness, and six HUD action slots.
- **Inventory** — server-backed System storage with item tooltips, pagination, held-item storage, and slot retrieval.
- **Equipment** — primary weapon, armor, accessories, and synchronized equipment context. Minecraft inventory remains authoritative.
- **Quests** — active progression, daily objectives, completion state, emergency quest state, and claim action.
- **Shop** — product descriptions, prices, affordability, storage capacity, disabled reasons, and confirmations for expensive products.
- **Dungeons** — the Abyssal Necropolis master dungeon, rank, recommended level, description, and live synchronized session state/objective progress. Entry remains gate-driven.
- **Shadows** — stored and active counts, army mode, shadow list, and only actions supported by current server logic.
- **Settings** — HUD visibility, scale, opacity, anchor, offsets, compact/minimal modes, names, notifications, and reset.

## Ability binding

Every ability registered in `AbilityService` is listed. The screen reads the actual Minecraft `KeyMapping`, so rebinding remains compatible with the normal Controls screen and saved Minecraft options.

1. Select an ability.
2. Use **Open Controls** to change its Minecraft key.
3. Assign or remove it from one of six local HUD action slots.
4. Resolve any conflict marked by the System before relying on the binding.
5. Use **Reset Ability Keys** to restore all mod ability keys to their registered defaults.

A key press activates only its own mapped ability. Ability input is ignored while any screen is open, including chat and text fields. The server still validates player state, unlock policy, mana, cooldown, target, and execution requirements.

## HUD customization

Saved locally in `config/sololeveling-system-ui.properties`:

- Vitals visibility
- Gold visibility
- Quest tracker visibility
- Ability hotbar visibility
- Ability name visibility
- Notification visibility
- Compact mode
- Minimal mode
- HUD scale from 60% to 150%
- HUD opacity from 35% to 100%
- Four corner anchors
- Horizontal offset from -240 to +240
- Vertical offset from -160 to +160
- Six ability action slots
- Last selected System tab
- Reset to defaults

The overlay derives all major positions from the current scaled window dimensions. It clamps the configured offsets to the visible screen and reserves space above the vanilla hotbar for bottom anchors.

## Notifications

Notifications are queued and displayed one at a time with entry/exit motion and bounded duration. Categories include:

- Information, warning, and error
- Level up and rank advancement
- Ability unlocked
- Quest accepted, completed, or failed
- Item obtained
- Gold gained or spent
- Stat increased
- Dungeon state

The renderer uses category colors and safe built-in sound feedback. Missing backend detail falls back to text rather than crashing.

Pending messages follow the release priority order: level up, ability unlock, quest complete, rank advancement, dungeon objective, item obtained, then failure/error. A notification already being displayed is never interrupted, equal-priority messages remain ordered, and queue overflow discards the lowest-priority tail.

## Reusable UI components

- `SystemUi.Theme` — panels, insets, section rules, progress bars, slots, icon rendering, opacity, clipping text, and wrapping.
- `SystemButton` — custom non-vanilla System button with active/disabled presentation and disabled reason.
- `SystemTabButton` — selected-tab treatment with atlas icon.
- `SystemConfirmScreen` — reusable confirmation dialog for high-impact actions.
- `SystemUi.Actions` — client-side duplicate-action throttle.
- `SystemUi.Cooldowns` — cached cooldown progress and readable timers.
- `SystemUi.Notifications` — bounded queue and animation state.
- Responsive layout helpers inside `SystemScreen` — compact top navigation, wide side navigation, scissored lists, and bounded scroll offsets.

## Client and server responsibilities

Client responsibilities:

- Presentation and animation
- Local HUD preferences
- Minecraft keybind display and conflict detection
- Action-slot display assignment
- Client-side duplicate-click suppression
- Safe preflight messages for obvious locked, cooldown, mana, or conflict states

Server responsibilities:

- Hunter data and synchronization
- Stat spending and point limits
- Purchases, gold withdrawal, storage delivery, and refunds
- Inventory storage and retrieval
- Ability unlocks, mana, cooldowns, targets, effects, and quest tracking
- Quest rewards
- Shadow actions
- Dungeon sessions and gate entry

The server keeps the existing global packet ceiling and now adds short action-specific debounce windows for stat allocation, quest claims, shop purchases, and inventory mutation. Client values are never trusted as prices, point balances, cooldowns, or item data.

## Failure handling

- Empty or missing synchronized tags use bounded fallbacks.
- Unknown abilities are omitted or shown as unavailable rather than invoked.
- Empty ability, inventory, quest, equipment, or shadow data renders a clear empty state.
- Malformed settings reset to safe defaults.
- Missing textures leave the rest of the screen functional through procedural panel rendering.
- Player-null and disconnected states do not dereference inventory or health data.
- Screen input is ignored while chat or another screen owns focus.
- Action throttles prevent rapid clicks from flooding irreversible server operations.

## Validation performed

- Baseline Forge Build run 254 passed on the latest main predecessor used for this branch.
- UI atlas validator checks the PNG signature, exact 128×128 dimensions, and bounded file size.
- Java source syntax and resource structure are checked before publishing.
- GitHub Forge CI runs `./gradlew clean build --stacktrace --no-daemon` for this feature branch and pull request.
- CI also validates deterministic dungeon structures so the UI branch cannot silently damage the merged map work.

Automated compilation does not prove visual feel inside every real window size. Normal release playtesting is still recommended at GUI scales 1 through Auto, 1280×720, 1920×1080, and ultrawide resolutions, plus multiplayer, death/respawn, reconnect, and dimension transitions.

## Known limitations

- The dungeon page is intentionally informational. Existing gates and `/sl dungeon open <template>` remain the authoritative entry flow.
- Equipment is displayed safely, but custom drag/drop equipment management is not added because the repository does not yet expose a menu-backed server container for it.
- Quest history is limited to data currently included in the Hunter synchronization snapshot.
- Action-slot assignments are local presentation preferences; Minecraft Controls remain the authoritative ability activation bindings.
- Controller navigation receives normal Minecraft focus behavior but has not been validated with a physical controller.
