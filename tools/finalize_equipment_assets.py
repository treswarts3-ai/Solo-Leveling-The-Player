from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
LANG_PATH = RES / "assets/sololeveling/lang/en_us.json"
lang = json.loads(LANG_PATH.read_text(encoding="utf-8"))

exact_names = {
    "training_dagger": "Training Dagger",
    "moonshadow_dagger": "Moonshadow Dagger",
    "shadow_reaper_dagger": "Shadow Reaper Dagger",
    "novice_hunter_hood": "Novice Hunter Hood",
    "novice_hunter_jacket": "Novice Hunter Jacket",
    "novice_hunter_leggings": "Novice Hunter Leggings",
    "novice_hunter_boots": "Novice Hunter Boots",
    "shadow_monarch_hood": "Shadow Monarch Hood",
    "hunters_ring": "Hunter’s Ring",
    "mana_weave_necklace": "Mana-Weave Necklace",
    "shadow_belt": "Shadow Belt",
    "monarch_earrings": "Monarch Earrings",
    "reinforcement_core": "Reinforcement Core",
    "monarch_reinforcement_core": "Monarch Reinforcement Core",
    "equipment_fragment": "Equipment Fragment",
    "enchanted_steel": "Enchanted Steel",
    "quest_relic": "System Quest Relic",
    "shadow_silk": "Shadow Silk"
}
for item_id, name in exact_names.items():
    lang[f"item.sololeveling.{item_id}"] = name

generic = {
    "tooltip.sololeveling.rarity": "Rarity: %s",
    "tooltip.sololeveling.category_slot": "%s · %s",
    "tooltip.sololeveling.accessory_slot": "Accessory slot: %s",
    "tooltip.sololeveling.upgrade": "Upgrade: +%s / +%s",
    "tooltip.sololeveling.stat": "%s %s",
    "tooltip.sololeveling.set_piece": "%s set piece",
    "tooltip.sololeveling.ability": "Item ability: %s",
    "tooltip.sololeveling.acquisition": "Acquisition: %s",
    "tooltip.sololeveling.equipment_id": "Equipment ID: %s",
    "tooltip.sololeveling.accessory_use": "Right-click to equip or unequip",
    "tooltip.sololeveling.rune_use": "Absorb to unlock %s",
    "tooltip.sololeveling.bound_item": "Bound progression item",
    "tooltip.sololeveling.upgrade_material": "Reinforces equipment by +%s",
    "tooltip.sololeveling.upgrade_instructions": "Hold equipment in the opposite hand, then use this core",
    "message.sololeveling.upgrade_no_target": "Hold an equipment item in the opposite hand.",
    "message.sololeveling.upgrades_disabled": "Equipment upgrades are disabled by server configuration.",
    "message.sololeveling.upgrade_max": "That equipment is already fully reinforced.",
    "message.sololeveling.upgrade_success": "%s reinforced to +%s / +%s."
}
lang.update(generic)

for rarity, name in {
    "common": "Common", "uncommon": "Uncommon", "rare": "Rare", "epic": "Epic",
    "legendary": "Legendary", "monarch": "Monarch"
}.items():
    lang[f"rarity.sololeveling.{rarity}"] = name

for category, name in {"dagger": "Dagger", "sword": "Sword", "armor": "Armor", "accessory": "Accessory"}.items():
    lang[f"category.sololeveling.{category}"] = name

for slot, name in {
    "main_hand": "Main Hand", "off_hand": "Off Hand", "either_hand": "Either Hand",
    "head": "Head", "chest": "Chest", "legs": "Legs", "feet": "Feet", "accessory": "Accessory"
}.items():
    lang[f"slot.sololeveling.{slot}"] = name

for slot, name in {
    "none": "None", "ring": "Ring", "necklace": "Necklace", "belt": "Belt",
    "earring": "Earrings", "hands": "Hands", "orb": "Orb"
}.items():
    lang[f"accessory_slot.sololeveling.{slot}"] = name

for stat, name in {
    "strength": "Strength", "agility": "Agility", "stamina": "Stamina", "intelligence": "Intelligence",
    "sense": "Sense", "attack_damage": "Attack Damage", "attack_speed": "Attack Speed",
    "movement_speed": "Movement Speed", "max_health": "Max Health", "armor": "Armor",
    "armor_toughness": "Armor Toughness", "knockback_resistance": "Knockback Resistance",
    "luck": "Luck", "mana": "Mana", "shadow_capacity": "Shadow Capacity"
}.items():
    lang[f"stat.sololeveling.{stat}"] = name

for set_id, name in {
    "novice_hunter": "Novice Hunter", "high_knight": "High Knight", "assassins": "Assassin",
    "truth_seeker": "Truth Seeker", "demon_monarch": "Demon Monarch", "shadow_monarch": "Shadow Monarch",
    "red_knight": "Red Knight"
}.items():
    lang[f"set.sololeveling.{set_id}"] = name

abilities = {
    "training": "Practice Edge", "venom": "Venom and Paralysis", "knight_killer": "Armor Breaker",
    "frost_step": "Frost Step", "demon_pair": "Two as One", "demon_fire": "White Flame",
    "moonshadow": "Moonshadow Eclipse", "shadow_reaper": "Reaper’s Mark", "kamish_pair": "Dragon Wrath",
    "antares_flame": "Flames of Destruction", "novice_resolve": "Novice Resolve",
    "fortress": "Fortress", "silent_step": "Silent Step", "mana_weave": "Mana Weave",
    "demon_authority": "Demon Authority", "monarch_presence": "Monarch’s Presence"
}
for key, name in abilities.items():
    lang[f"ability.sololeveling.{key}"] = name

acquisitions = {
    "unknown": "Unknown", "crafted": "Crafting", "low_rank_dungeon": "Low-rank dungeon loot",
    "mid_rank_dungeon": "Mid-rank dungeon loot", "demon_castle": "Demon Castle loot",
    "monarch_cache": "Monarch cache loot", "admin": "Administrator or test access",
    "quest_reward": "Quest reward", "main_quest": "Main quest", "system_reward": "System reward",
    "rune_drop": "Rune drop", "dungeon_loot": "Dungeon loot"
}
for key, name in acquisitions.items():
    lang[f"acquisition.sololeveling.{key}"] = name

LANG_PATH.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

recipes = RES / "data/sololeveling/recipes"
recipes.mkdir(parents=True, exist_ok=True)

def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

shaped = {
    "training_dagger": ([" I", "S "], {"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "minecraft:stick"}}),
    "novice_hunter_hood": (["LLL", "L L"], {"L": {"item": "minecraft:leather"}}),
    "novice_hunter_jacket": (["L L", "LLL", "LLL"], {"L": {"item": "minecraft:leather"}}),
    "novice_hunter_leggings": (["LLL", "L L", "L L"], {"L": {"item": "minecraft:leather"}}),
    "novice_hunter_boots": (["L L", "L L"], {"L": {"item": "minecraft:leather"}})
}
for output, (pattern, key) in shaped.items():
    dump(recipes / f"{output}.json", {
        "type": "minecraft:crafting_shaped", "pattern": pattern, "key": key,
        "result": {"item": f"sololeveling:{output}", "count": 1}
    })

shapeless = {
    "enchanted_steel": ["minecraft:iron_ingot", "sololeveling:mana_crystal"],
    "reinforcement_core": [
        "sololeveling:equipment_fragment", "sololeveling:equipment_fragment",
        "sololeveling:equipment_fragment", "sololeveling:mana_crystal"
    ],
    "knight_killer": ["sololeveling:training_dagger", "sololeveling:enchanted_steel", "sololeveling:reinforcement_core"],
    "mana_weave_necklace": ["minecraft:string", "sololeveling:mana_crystal", "minecraft:amethyst_shard"],
    "hunters_ring": ["minecraft:iron_nugget", "minecraft:iron_nugget", "sololeveling:mana_crystal"],
    "moonshadow_dagger": ["sololeveling:barukas_dagger", "sololeveling:shadow_silk", "sololeveling:reinforcement_core"],
    "shadow_reaper_dagger": [
        "sololeveling:moonshadow_dagger", "sololeveling:enchanted_steel",
        "sololeveling:reinforcement_core", "sololeveling:reinforcement_core"
    ],
    "monarch_reinforcement_core": [
        "sololeveling:reinforcement_core", "sololeveling:reinforcement_core",
        "sololeveling:shadow_silk", "sololeveling:essence_stone"
    ]
}
for output, ingredients in shapeless.items():
    dump(recipes / f"{output}.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": item} for item in ingredients],
        "result": {"item": f"sololeveling:{output}", "count": 1}
    })

loot_root = RES / "data/sololeveling/loot_tables/inject"
modifier_root = RES / "data/sololeveling/loot_modifiers"
global_root = RES / "data/forge/loot_modifiers"

def item_entry(name, weight, minimum=1.0, maximum=1.0):
    entry = {"type": "minecraft:item", "name": f"sololeveling:{name}", "weight": weight}
    if minimum != 1.0 or maximum != 1.0:
        entry["functions"] = [{
            "function": "minecraft:set_count",
            "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum},
            "add": False
        }]
    return entry

tables = {
    "low_rank_dungeon": [
        item_entry("equipment_fragment", 20, 1.0, 3.0),
        item_entry("mana_crystal", 16, 1.0, 3.0),
        item_entry("healing_potion", 12, 1.0, 2.0),
        item_entry("mana_potion", 12, 1.0, 2.0),
        item_entry("training_dagger", 3)
    ],
    "mid_rank_dungeon": [
        item_entry("equipment_fragment", 18, 2.0, 5.0),
        item_entry("enchanted_steel", 12),
        item_entry("reinforcement_core", 8),
        item_entry("knight_killer", 2),
        item_entry("barukas_dagger", 1),
        item_entry("assassins_hood", 1)
    ],
    "monarch_cache": [
        item_entry("shadow_silk", 14, 1.0, 3.0),
        item_entry("monarch_reinforcement_core", 6),
        item_entry("moonshadow_dagger", 2),
        item_entry("shadow_reaper_dagger", 1),
        item_entry("shadow_monarch_hood", 1),
        item_entry("shadow_belt", 1),
        item_entry("monarch_earrings", 1)
    ]
}
for name, entries in tables.items():
    dump(loot_root / f"{name}.json", {
        "type": "minecraft:chest",
        "pools": [{
            "rolls": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0},
            "bonus_rolls": 0.0,
            "entries": entries
        }]
    })

modifier_targets = {
    "low_rank_dungeon": "minecraft:chests/simple_dungeon",
    "mid_rank_dungeon": "minecraft:chests/abandoned_mineshaft",
    "monarch_cache": "minecraft:chests/ancient_city"
}
for name, target in modifier_targets.items():
    dump(modifier_root / f"{name}.json", {
        "type": "forge:add_table",
        "conditions": [{"condition": "forge:loot_table_id", "loot_table_id": target}],
        "table": f"sololeveling:inject/{name}"
    })
dump(global_root / "global_loot_modifiers.json", {
    "replace": False,
    "entries": [f"sololeveling:{name}" for name in modifier_targets]
})

summary = {
    "recipes": sorted(path.stem for path in recipes.glob("*.json")),
    "loot_tables": sorted(path.stem for path in loot_root.glob("*.json")),
    "loot_modifiers": sorted(path.stem for path in modifier_root.glob("*.json"))
}
(ROOT / "EQUIPMENT_ACQUISITION.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
