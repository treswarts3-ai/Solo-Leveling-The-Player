#!/usr/bin/env python3
"""Validate the single handcrafted master dungeon layout.

The previous tool emitted four NBT templates. The active mod now builds one
fixed authored dungeon from Java so placement can be staged, validated and
maintained without retaining obsolete selectable maps. This script mirrors the
layout graph and produces deterministic design metrics for CI and documentation.
"""
from __future__ import annotations

import argparse
import json
from collections import defaultdict, deque
from dataclasses import asdict, dataclass
from pathlib import Path

DUNGEON_ID = "master"
DISPLAY_NAME = "Abyssal Necropolis"
SHELL_THICKNESS = 5


@dataclass(frozen=True)
class Space:
    id: str
    region: str
    min_x: int
    max_x: int
    floor_y: int
    min_z: int
    max_z: int
    major: bool = False
    secret: bool = False
    shortcut: bool = False

    @property
    def center(self) -> tuple[int, int, int]:
        return ((self.min_x + self.max_x) // 2, self.floor_y + 1, (self.min_z + self.max_z) // 2)

    @property
    def walkable_area(self) -> int:
        return max(0, self.max_x - self.min_x - 1) * max(0, self.max_z - self.min_z - 1)


SPACES = (
    Space("entry_vestibule", "Gate Descent", -10, 10, 14, -100, -90),
    Space("gate_hall", "Gate Descent", -16, 16, 14, -89, -78, major=True),
    Space("watcher_gallery", "Gate Descent", -22, 22, 14, -77, -62, major=True),
    Space("bridge_antechamber", "Gate Descent", -12, 12, 13, -61, -53),
    Space("broken_bridge", "Gate Descent", -32, 32, 12, -52, -39, major=True),
    Space("west_overlook", "Gate Descent", -45, -33, 12, -49, -40, secret=True),
    Space("east_overlook", "Gate Descent", 33, 45, 12, -49, -40, secret=True),
    Space("split_rotunda", "Gate Descent", -15, 15, 10, -38, -27, major=True),

    Space("west_descent", "Ossuary Ward", -34, -14, 9, -34, -26),
    Space("ossuary_nave", "Ossuary Ward", -70, -31, 8, -25, 2, major=True),
    Space("bone_chapel", "Ossuary Ward", -84, -71, 8, -21, -6, secret=True),
    Space("mortuary_store", "Ossuary Ward", -66, -48, 8, 3, 14),
    Space("west_loop_gallery", "Ossuary Ward", -47, -29, 7, 5, 19),
    Space("west_shortcut", "Ossuary Ward", -30, -18, 6, 12, 22, shortcut=True),

    Space("east_descent", "Abyssal Foundry", 14, 34, 9, -34, -26),
    Space("forge_approach", "Abyssal Foundry", 31, 48, 8, -25, -15),
    Space("abyssal_foundry", "Abyssal Foundry", 31, 78, 6, -14, 14, major=True),
    Space("slag_control", "Abyssal Foundry", 59, 76, 6, 13, 25),
    Space("furnace_secret", "Abyssal Foundry", 44, 58, 5, 14, 27, secret=True),
    Space("east_loop_gallery", "Abyssal Foundry", 29, 47, 5, 13, 27),
    Space("east_shortcut", "Abyssal Foundry", 18, 30, 5, 17, 24, shortcut=True),

    Space("central_spine", "Buried Kingdom", -7, 7, 7, -26, -7, shortcut=True),
    Space("central_plaza", "Buried Kingdom", -34, 34, 4, -6, 27, major=True),
    Space("monument_ring", "Buried Kingdom", -13, 13, 5, 4, 19),
    Space("ruined_market", "Buried Kingdom", -56, -31, 4, 3, 24),
    Space("guard_barracks", "Buried Kingdom", 31, 56, 4, 3, 24),
    Space("basilica_approach", "Buried Kingdom", -14, 14, 3, 26, 34),
    Space("monarch_basilica", "Buried Kingdom", -25, 25, 2, 35, 57, major=True),
    Space("sealed_reliquary", "Buried Kingdom", -40, -26, 2, 42, 55, secret=True),
    Space("broken_belfry", "Buried Kingdom", 26, 41, 6, 42, 56, secret=True),

    Space("prison_descent", "Lower Catacombs", -45, -24, 0, 51, 63),
    Space("chained_prison", "Lower Catacombs", -75, -36, -4, 58, 84, major=True),
    Space("execution_pit", "Lower Catacombs", -70, -49, -5, 85, 97),
    Space("archive_descent", "Lower Catacombs", 24, 45, -1, 51, 63),
    Space("drowned_archive", "Lower Catacombs", 36, 75, -6, 58, 86, major=True),
    Space("forbidden_stacks", "Lower Catacombs", 56, 73, -6, 87, 99, secret=True),
    Space("sunken_court", "Lower Catacombs", -32, 32, -8, 66, 96, major=True),
    Space("west_court_loop", "Lower Catacombs", -42, -29, -5, 76, 91),
    Space("east_court_loop", "Lower Catacombs", 29, 42, -6, 76, 91),

    Space("final_approach", "Monarch's Sepulcher", -12, 12, -9, 97, 105),
    Space("throne_passage", "Monarch's Sepulcher", -18, 18, -10, 106, 114),
    Space("final_arena", "Monarch's Sepulcher", -44, 44, -12, 115, 154, major=True),
    Space("reward_vault", "Monarch's Sepulcher", 42, 66, -10, 125, 146, major=True),
)

# Every edge is an authored walkable connection. shortcut edges are still part
# of the graph but become useful only after their checkpoint barrier is opened.
EDGES = (
    ("entry_vestibule", "gate_hall"),
    ("gate_hall", "watcher_gallery"),
    ("watcher_gallery", "bridge_antechamber"),
    ("bridge_antechamber", "broken_bridge"),
    ("broken_bridge", "west_overlook"),
    ("broken_bridge", "east_overlook"),
    ("broken_bridge", "split_rotunda"),
    ("split_rotunda", "west_descent"),
    ("west_descent", "ossuary_nave"),
    ("ossuary_nave", "bone_chapel"),
    ("ossuary_nave", "mortuary_store"),
    ("mortuary_store", "west_loop_gallery"),
    ("west_loop_gallery", "west_shortcut"),
    ("west_shortcut", "central_plaza"),
    ("split_rotunda", "east_descent"),
    ("east_descent", "forge_approach"),
    ("forge_approach", "abyssal_foundry"),
    ("abyssal_foundry", "slag_control"),
    ("abyssal_foundry", "furnace_secret"),
    ("slag_control", "east_loop_gallery"),
    ("furnace_secret", "east_loop_gallery"),
    ("east_loop_gallery", "east_shortcut"),
    ("east_shortcut", "central_plaza"),
    ("split_rotunda", "central_spine"),
    ("central_spine", "central_plaza"),
    ("central_plaza", "monument_ring"),
    ("central_plaza", "ruined_market"),
    ("central_plaza", "guard_barracks"),
    ("central_plaza", "basilica_approach"),
    ("basilica_approach", "monarch_basilica"),
    ("monarch_basilica", "sealed_reliquary"),
    ("monarch_basilica", "broken_belfry"),
    ("monarch_basilica", "prison_descent"),
    ("monarch_basilica", "archive_descent"),
    ("prison_descent", "chained_prison"),
    ("chained_prison", "execution_pit"),
    ("chained_prison", "west_court_loop"),
    ("execution_pit", "west_court_loop"),
    ("west_court_loop", "sunken_court"),
    ("archive_descent", "drowned_archive"),
    ("drowned_archive", "forbidden_stacks"),
    ("drowned_archive", "east_court_loop"),
    ("forbidden_stacks", "east_court_loop"),
    ("east_court_loop", "sunken_court"),
    ("sunken_court", "final_approach"),
    ("final_approach", "throne_passage"),
    ("throne_passage", "final_arena"),
    ("final_arena", "reward_vault"),
)

CRITICAL_PATH = (
    "entry_vestibule", "gate_hall", "watcher_gallery", "bridge_antechamber", "broken_bridge",
    "split_rotunda", "west_descent", "ossuary_nave", "mortuary_store", "west_loop_gallery",
    "west_shortcut", "central_plaza", "east_shortcut", "east_loop_gallery", "slag_control",
    "abyssal_foundry", "forge_approach", "east_descent", "split_rotunda", "central_spine",
    "central_plaza", "basilica_approach", "monarch_basilica", "prison_descent",
    "chained_prison", "west_court_loop", "sunken_court", "final_approach", "throne_passage",
    "final_arena", "reward_vault",
)

OBJECTIVE_SPACES = (
    "broken_bridge", "ossuary_nave", "central_plaza", "abyssal_foundry",
    "monarch_basilica", "chained_prison", "sunken_court", "final_arena", "reward_vault",
)

OLD_DUNGEON_FLOOR_AREA = 4_343  # largest validated former v3 template
OLD_DUNGEON_MAIN_PATH = 184
OLD_DUNGEON_DETAIL_BLOCKS = 23_457


def distance(a: Space, b: Space) -> int:
    ax, ay, az = a.center
    bx, by, bz = b.center
    return abs(ax - bx) + abs(az - bz) + 2 * abs(ay - by)


def validate() -> dict[str, object]:
    by_id = {space.id: space for space in SPACES}
    if len(by_id) != len(SPACES):
        raise ValueError("duplicate space id")
    for a, b in EDGES:
        if a not in by_id or b not in by_id:
            raise ValueError(f"unknown graph endpoint: {(a, b)}")

    graph: dict[str, set[str]] = defaultdict(set)
    for a, b in EDGES:
        graph[a].add(b)
        graph[b].add(a)

    reached = {CRITICAL_PATH[0]}
    queue = deque(reached)
    while queue:
        current = queue.popleft()
        for nxt in graph[current]:
            if nxt not in reached:
                reached.add(nxt)
                queue.append(nxt)
    missing = set(by_id) - reached
    if missing:
        raise ValueError(f"disconnected spaces: {sorted(missing)}")

    for a, b in zip(CRITICAL_PATH, CRITICAL_PATH[1:]):
        if b not in graph[a]:
            raise ValueError(f"critical path edge missing: {a} -> {b}")
    for objective in OBJECTIVE_SPACES:
        if objective not in reached:
            raise ValueError(f"objective unreachable: {objective}")

    regions = sorted({space.region for space in SPACES})
    vertical_layers = sorted({space.floor_y for space in SPACES})
    secrets = [space.id for space in SPACES if space.secret]
    shortcuts = [space.id for space in SPACES if space.shortcut]
    cycle_count = len(EDGES) - len(SPACES) + 1
    floor_area = sum(space.walkable_area for space in SPACES)
    path_length = sum(distance(by_id[a], by_id[b]) for a, b in zip(CRITICAL_PATH, CRITICAL_PATH[1:]))
    min_x = min(space.min_x for space in SPACES) - SHELL_THICKNESS
    max_x = max(space.max_x for space in SPACES) + SHELL_THICKNESS
    min_y = min(space.floor_y for space in SPACES) - SHELL_THICKNESS
    max_y = 27 + SHELL_THICKNESS
    min_z = min(space.min_z for space in SPACES) - SHELL_THICKNESS
    max_z = max(space.max_z for space in SPACES) + SHELL_THICKNESS

    if len(SPACES) < 30 or sum(space.major for space in SPACES) < 12:
        raise ValueError("layout is below meaningful-space targets")
    if len(regions) < 6 or len(vertical_layers) < 3:
        raise ValueError("layout lacks region or vertical-layer diversity")
    if len(secrets) < 6 or len(shortcuts) < 2 or cycle_count < 2:
        raise ValueError("layout lacks secrets, shortcuts or loops")
    if floor_area < OLD_DUNGEON_FLOOR_AREA * 4:
        raise ValueError("walkable area is not four times the old largest dungeon")
    if path_length < OLD_DUNGEON_MAIN_PATH * 4:
        raise ValueError("critical path is not four times the old largest dungeon")

    return {
        "id": DUNGEON_ID,
        "display_name": DISPLAY_NAME,
        "bounds": {
            "min": [min_x, min_y, min_z],
            "max": [max_x, max_y, max_z],
            "size": [max_x - min_x + 1, max_y - min_y + 1, max_z - min_z + 1],
        },
        "shell_thickness": SHELL_THICKNESS,
        "meaningful_spaces": len(SPACES),
        "major_rooms": sum(space.major for space in SPACES),
        "regions": len(regions),
        "region_names": regions,
        "vertical_floor_levels": vertical_layers,
        "loops": cycle_count,
        "shortcuts": len(shortcuts),
        "shortcut_ids": shortcuts,
        "secrets": len(secrets),
        "secret_ids": secrets,
        "objective_spaces": list(OBJECTIVE_SPACES),
        "walkable_floor_area": floor_area,
        "critical_path_distance": path_length,
        "largest_old_floor_area": OLD_DUNGEON_FLOOR_AREA,
        "largest_old_main_path": OLD_DUNGEON_MAIN_PATH,
        "floor_area_multiplier": round(floor_area / OLD_DUNGEON_FLOOR_AREA, 2),
        "main_path_multiplier": round(path_length / OLD_DUNGEON_MAIN_PATH, 2),
        "construction": "deterministic handcrafted Java blueprint",
        "spaces": [asdict(space) for space in SPACES],
        "edges": [list(edge) for edge in EDGES],
        "critical_path": list(CRITICAL_PATH),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, help="accepted for compatibility; no NBT files are emitted")
    parser.add_argument("--manifest", type=Path)
    args = parser.parse_args()
    metrics = validate()
    if args.output:
        args.output.mkdir(parents=True, exist_ok=True)
    if args.manifest:
        args.manifest.parent.mkdir(parents=True, exist_ok=True)
        args.manifest.write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__":
    main()
