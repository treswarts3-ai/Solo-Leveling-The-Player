from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
MOD_ITEMS = (ROOT / "src/main/java/com/tre/sololeveling/registry/ModItems.java").read_text(encoding="utf-8")
CATALOG = (ROOT / "src/main/java/com/tre/sololeveling/equipment/EquipmentCatalog.java").read_text(encoding="utf-8")

ids = []
for match in re.finditer(r'(?:weapon|armor|story|accessory|rune|material|consumable)\("([a-z0-9_]+)"', MOD_ITEMS):
    if match.group(1) not in ids:
        ids.append(match.group(1))

equipment_ids = set(re.findall(r'(?:weapon|armor|accessory)\("([a-z0-9_]+)"', CATALOG))
registry_ids = set(ids)
errors = []

lang_path = RES / "assets/sololeveling/lang/en_us.json"
try:
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
except Exception as exc:
    errors.append(f"Invalid language file: {exc}")
    lang = {}

for item_id in ids:
    model = RES / f"assets/sololeveling/models/item/{item_id}.json"
    texture = RES / f"assets/sololeveling/textures/item/{item_id}.png"
    if not model.exists():
        errors.append(f"Missing model: {item_id}")
    else:
        try:
            data = json.loads(model.read_text(encoding="utf-8"))
            layer = data.get("textures", {}).get("layer0", "")
            if layer != f"sololeveling:item/{item_id}":
                errors.append(f"Wrong model texture reference: {item_id} -> {layer}")
        except Exception as exc:
            errors.append(f"Invalid model JSON {item_id}: {exc}")
    if not texture.exists():
        errors.append(f"Missing texture: {item_id}")
    elif texture.read_bytes()[:8] != b"\x89PNG\r\n\x1a\n":
        errors.append(f"Invalid PNG signature: {item_id}")
    if f"item.sololeveling.{item_id}" not in lang:
        errors.append(f"Missing translation: {item_id}")

missing_registry = sorted(equipment_ids - registry_ids)
if missing_registry:
    errors.append("Equipment definitions missing registry entries: " + ", ".join(missing_registry))

required_translation_prefixes = [
    "tooltip.sololeveling.rarity", "tooltip.sololeveling.upgrade",
    "tooltip.sololeveling.acquisition", "message.sololeveling.upgrade_success"
]
for key in required_translation_prefixes:
    if key not in lang:
        errors.append(f"Missing framework translation: {key}")

registered = {f"sololeveling:{item_id}" for item_id in ids}
json_roots = [
    RES / "data/sololeveling/recipes",
    RES / "data/sololeveling/loot_tables",
    RES / "data/sololeveling/loot_modifiers",
    RES / "data/forge/loot_modifiers"
]

def walk(value, source):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in {"item", "name"} and isinstance(child, str) and child.startswith("sololeveling:") and child not in registered:
                errors.append(f"Unknown item reference {child} in {source}")
            walk(child, source)
    elif isinstance(value, list):
        for child in value:
            walk(child, source)

parsed = 0
for root in json_roots:
    if not root.exists():
        errors.append(f"Missing data directory: {root.relative_to(ROOT)}")
        continue
    for path in root.rglob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            parsed += 1
            walk(data, path.relative_to(ROOT))
        except Exception as exc:
            errors.append(f"Invalid JSON {path.relative_to(ROOT)}: {exc}")

if errors:
    print("\n".join(f"ERROR: {error}" for error in errors))
    sys.exit(1)
print(f"validated {len(ids)} items, {len(equipment_ids)} equipment definitions and {parsed} recipe/loot JSON files")
