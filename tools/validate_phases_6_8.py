#!/usr/bin/env python3
"""Static contracts for the Phase 6–8 implementation; Forge build remains the compile gate."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    value = ROOT / path
    assert value.is_file(), f"missing {path}"
    return value.read_text(encoding="utf-8")


types = read("src/main/java/com/tre/sololeveling/dungeon/DungeonTypes.java")
combat = read("src/main/java/com/tre/sololeveling/dungeon/DungeonCombatBehavior.java")
boss = read("src/main/java/com/tre/sololeveling/dungeon/DungeonBoss.java")
rank = read("src/main/java/com/tre/sololeveling/gameplay/RankTrialService.java")
equipment = read("src/main/java/com/tre/sololeveling/equipment/EquipmentDefinition.java")
loot = read("src/main/java/com/tre/sololeveling/equipment/DungeonLootService.java")
quests = read("src/main/java/com/tre/sololeveling/quest/QuestObjectiveType.java")
storage = read("src/main/java/com/tre/sololeveling/shadow/ShadowStorage.java")
ai = read("src/main/java/com/tre/sololeveling/shadow/ShadowAiService.java")
wheel = read("src/main/java/com/tre/sololeveling/client/screen/ShadowCommandWheelScreen.java")

for role in ("MELEE", "ASSASSIN", "TANK", "RANGED", "SUMMONER", "ELITE"):
    assert role in types and f"case {role}" in combat, f"enemy role contract missing: {role}"
for contract in ("melee_combo", "melee_heavy", "assassin_lunge", "tank_slam", "ranged_shot", "summoner_channel"):
    assert contract in combat, f"role action missing: {contract}"
for contract in ("TAG_INTRO_UNTIL", "TAG_RECOVERY_UNTIL", "arenaFissures", "TAG_LAST_HURT", "hasLineOfSight"):
    assert contract in boss, f"boss quality contract missing: {contract}"

for transition in (("HunterRank.E", "HunterRank.D"), ("HunterRank.D", "HunterRank.C"),
                   ("HunterRank.C", "HunterRank.B"), ("HunterRank.B", "HunterRank.A"),
                   ("HunterRank.A", "HunterRank.S")):
    assert f"new Trial({transition[0]}, {transition[1]}" in rank, f"rank transition missing: {transition}"
for field in ("minimumLevel()", "maximumLevel()", "statBudget()", "sellValue()", "upgradeCost("):
    assert field in equipment, f"equipment economy field missing: {field}"
assert "DUPLICATE CONVERSION" in loot and "EquipmentRarity.LEGENDARY" in loot
for objective in ("ENEMY_ROLE", "ENCOUNTER_CONSTRAINT", "SECRET_DISCOVERY", "SHADOW_DEVELOPMENT", "EQUIPMENT_UPGRADE", "COMBAT_STYLE"):
    assert objective in quests, f"quest objective missing: {objective}"

for field in ("Role role", "UUID owner", "Trait trait", "boolean evolutionEligible"):
    assert field in storage, f"shadow record field missing: {field}"
for formation in ("FOLLOW", "DEFENSIVE_RING", "FORWARD_ASSAULT", "RANGED_REAR_LINE", "BOSS_FOCUS"):
    assert formation in ai, f"formation missing: {formation}"
for action in ("SUMMON_SHADOW", "DISMISS_SHADOWS", "SHADOW_FOLLOW", "SHADOW_GUARD", "SHADOW_ATTACK",
               "SHADOW_HOLD", "SHADOW_RETURN", "SHADOW_FORMATION"):
    assert action in wheel, f"wheel action missing: {action}"
assert "MAX_STORED = 128" in storage and "ACTIVE_SHADOW_MAX" in read("src/main/java/com/tre/sololeveling/shadow/ShadowSummoningService.java")
summoning = read("src/main/java/com/tre/sololeveling/shadow/ShadowSummoningService.java")
assert "entity != null && entity.getPersistentData()" in summoning, "null damage-source shadow guard missing"
assert "PHASES_6_8" in "PHASES_6_8.md"

print("Phase 6–8 enemy, progression, loot, quest, and shadow contracts validated.")
