# Research and Technical Decisions

## Target

Minecraft 1.20.1, Forge 47.4.10, Java 17, Mojang mappings, ForgeGradle 6, Gradle 8.8.

## Forge architecture

Registries use `DeferredRegister`. Runtime events use the Forge event bus. Client key registration uses the mod event bus with a client-only subscriber. Player progression is stored in the player's persistent NBT under one namespaced compound and copied during player cloning. A `SimpleChannel` carries small server action requests and owner-only hunter snapshots.

## Security model

The client only sends action names. The server checks awakening, skill unlocks, mana, cooldowns, targets, item requirements, quest state, store prices, shadow capacity, and destination collision. XP, levels, gold, damage, quest counters, inventory, and shadow records are never accepted directly from client packets.

## Animation approach

The initial release avoids GeckoLib and PlayerAnimator as hard dependencies. Vanilla pose/swing/effect animation is used so the mod compiles and runs without optional libraries. The source layout leaves client animation work isolated for a later version.

## Shadow implementation

Deaths create short-lived sanitized records containing only entity registry ID, position, dimension, expiry, power, and attempt count. Extraction stores a minimal shadow record. Summoning creates a fresh supported vanilla mob marked with owner UUID and shadow flags. Shadow mobs drop no loot or XP and are discarded on dismissal.

## Texture workflow

Every registered item has a deterministic original PNG and corresponding standalone HTML canvas generator in `tools/pixel_art/items/`. No remote images or copied franchise assets are used. Interface, rank, quest, ability, logo, and armor-layer assets are generated locally.

## Dedicated-server safety

Client screens, key mappings, and HUD classes are annotated for client distribution only. Common registration does not import or instantiate client classes. No client class is referenced by server packets.

## Performance

Mana/stat maintenance runs once per second. Shadow AI maintenance runs twice per second. Entity scans are limited to ability activation or a small penalty-area interval. No chunks are force-loaded and no complete player snapshots are sent every tick.
