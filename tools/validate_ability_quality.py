#!/usr/bin/env python3
"""Static guardrails for the Phase 5 server-authoritative ability contract."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/tre/sololeveling"


def require(condition: bool, message: str) -> None:
    if not condition:
        print(f"ability quality validation failed: {message}", file=sys.stderr)
        raise SystemExit(1)


standard = (JAVA / "gameplay/ability/StandardAbilities.java").read_text()
service = (JAVA / "gameplay/ability/AbilityService.java").read_text()
network = (JAVA / "network/ModNetwork.java").read_text()
visual_packet = (JAVA / "network/packet/AbilityVisualPacket.java").read_text()
targeting = (JAVA / "gameplay/ability/AbilityTargeting.java").read_text()
client_visuals = (JAVA / "client/ClientAbilityVisuals.java").read_text()

flagships = {
    "quicksilver": "ability.quicksilver.accelerate",
    "mutilation": "ability.mutilation.four_cut",
    "dagger_rush": "ability.dagger_rush.dash_slash",
    "dragons_fear": "ability.dragons_fear.roar",
    "rulers_authority": "ability.rulers_authority.",
    "stealth": "ability.stealth.fade",
    "shadow_exchange": "ability.shadow_exchange.transition",
    "monarch_domain": "ability.monarch_domain.expand",
}
for ability_id, animation_id in flagships.items():
    require(f'"{ability_id}"' in standard, f"missing flagship ability {ability_id}")
    require(animation_id in standard, f"missing animation contract for {ability_id}")

require("validatePhaseFiveQuality" in service, "registry quality validation is not installed")
require(service.index("spendMana") < service.index("ability.activate"), "mana must be reserved before resolution")
require(service.index("ability.activate") < service.index("setCooldown"), "cooldown must follow successful resolution")
require("targetStillAvailable" in service and "interruptStartup" in service,
        "cast target revalidation or interruption handling is missing")
require("AbilityMastery.recordResolvedUse" in service and "AbilityCombos.record" in service,
        "mastery or combo progression is missing")
require('PROTOCOL = "2"' in network, "ability visuals require network protocol 2")
require("PLAY_TO_CLIENT" in network and "AbilityVisualPacket.class" in network,
        "server visual packet is not registered client-bound")
require("carries no authority" in visual_packet, "visual packet authority warning is missing")
require("MAX_AREA_TARGETS = 64" in targeting and "PVP_ABILITIES" in targeting,
        "bounded multiplayer targeting protections are missing")
for forbidden in ("dealAbilityDamage", "spendMana", "setCooldown", "teleportTo"):
    require(forbidden not in client_visuals, f"client visuals contain authoritative operation {forbidden}")

print("Phase 5 ability quality contract validated.")
