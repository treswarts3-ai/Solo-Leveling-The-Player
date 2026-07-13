#!/usr/bin/env python3
"""Build and validate the Phase 4 modular Abyssal Necropolis starter pack.

The emitted SNBT is the text form of vanilla structure-template NBT. Runtime
code also prefers normal structure-block .nbt exports when they are present,
so builders can replace any starter module without changing Java.
"""
from __future__ import annotations

import argparse
import json
import tempfile
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path

DATA_VERSION = 3465
MODULE_ROOT = Path("src/main/resources/data/sololeveling/dungeon_modules/master")
REQUIRED_MARKERS = {
    "entry", "objective:0", "objective:1", "objective:2", "objective:3",
    "objective:4", "objective:5", "objective:6", "boss", "reward",
}


@dataclass(frozen=True)
class Module:
    id: str
    offset: tuple[int, int, int]
    size: tuple[int, int, int]
    style: str
    connectors: tuple[tuple[str, int], ...]
    markers: tuple[tuple[str, tuple[int, int, int]], ...] = ()


MODULES = (
    Module("00_entry", (-15, 0, -112), (31, 18, 32), "entry", (("s", 15),),
           (("entry", (15, 1, 6)), ("audio:arrival", (15, 5, 12)), ("particle:gate", (15, 2, 4)))),
    Module("01_descent", (-15, 0, -80), (31, 18, 27), "descent", (("n", 15), ("s", 7)),
           (("checkpoint:entry", (15, 1, 3)),)),
    Module("02_outer_necropolis", (-48, 0, -53), (48, 20, 40), "necropolis", (("n", 40), ("e", 19)),
           (("objective:0", (24, 1, 20)), ("enemy:melee:0", (14, 1, 14)),
            ("enemy:ranged:0", (34, 1, 27)), ("secret:west_crypt", (6, 1, 31)))),
    Module("03_guardian_hall", (0, 0, -53), (48, 20, 40), "guardian", (("w", 19), ("s", 8)),
           (("objective:1", (24, 3, 20)), ("enemy:tank:0", (24, 1, 14)),
            ("enemy:ranged:1", (32, 1, 25)), ("door:0", (8, 1, 37)))),
    Module("04_catacombs", (-23, 0, -13), (47, 22, 44), "catacombs", (("n", 31), ("s", 23)),
           (("objective:2", (23, 1, 16)), ("enemy:melee:1", (14, 1, 16)),
            ("secret:bone_chapel", (38, 1, 31)), ("shortcut:catacomb_loop", (6, 1, 22)),
            ("door:1", (23, 1, 41)))),
    Module("05_collapsed_bridge", (-23, 0, 31), (47, 20, 34), "bridge", (("n", 23), ("s", 15)),
           (("objective:3", (23, 1, 17)), ("enemy:ranged:2", (10, 1, 17)),
            ("enemy:ranged:3", (36, 1, 17)), ("door:2", (15, 1, 31)))),
    Module("06_prison", (-47, 0, 65), (47, 22, 41), "prison", (("n", 39), ("e", 20)),
           (("objective:4", (24, 1, 20)), ("enemy:melee:2", (14, 1, 20)),
            ("enemy:tank:1", (34, 1, 20)), ("door:3", (46, 1, 20)))),
    Module("07_elite_chamber", (0, 0, 65), (47, 22, 41), "elite", (("w", 20), ("s", 8)),
           (("objective:5", (23, 1, 20)), ("enemy:elite:0", (23, 1, 15)),
            ("secret:warden_cache", (38, 1, 31)), ("door:4", (8, 1, 38)))),
    Module("08_ritual_depths", (-23, 0, 106), (47, 24, 44), "ritual", (("n", 31), ("s", 23)),
           (("objective:6", (23, 3, 22)), ("enemy:summoner:0", (23, 1, 15)),
            ("enemy:melee:3", (13, 1, 27)), ("enemy:melee:4", (33, 1, 27)),
            ("audio:ritual", (23, 5, 22)), ("particle:ritual", (23, 4, 22)),
            ("door:5", (23, 1, 41)))),
    Module("09_boss_approach", (-15, 0, 150), (31, 20, 27), "approach", (("n", 15), ("s", 15)),
           (("enemy:elite:1", (15, 1, 13)), ("checkpoint:boss", (15, 1, 23)), ("door:6", (15, 1, 24)))),
    Module("10_boss_arena", (-23, 0, 177), (47, 28, 47), "arena", (("n", 23), ("e", 30)),
           (("boss", (23, 1, 23)), ("audio:boss", (23, 8, 23)),
            ("particle:boss", (23, 2, 23)), ("door:7", (44, 1, 30)))),
    Module("11_reward_vault", (24, 0, 193), (31, 20, 31), "vault", (("w", 14),),
           (("reward", (15, 4, 14)), ("exit", (24, 1, 14)), ("audio:reward", (15, 6, 14)))),
)

CONNECTIONS = (
    (("00_entry", "s", 15), ("01_descent", "n", 15)),
    (("01_descent", "s", 7), ("02_outer_necropolis", "n", 40)),
    (("02_outer_necropolis", "e", 19), ("03_guardian_hall", "w", 19)),
    (("03_guardian_hall", "s", 8), ("04_catacombs", "n", 31)),
    (("04_catacombs", "s", 23), ("05_collapsed_bridge", "n", 23)),
    (("05_collapsed_bridge", "s", 15), ("06_prison", "n", 39)),
    (("06_prison", "e", 20), ("07_elite_chamber", "w", 20)),
    (("07_elite_chamber", "s", 8), ("08_ritual_depths", "n", 31)),
    (("08_ritual_depths", "s", 23), ("09_boss_approach", "n", 15)),
    (("09_boss_approach", "s", 15), ("10_boss_arena", "n", 23)),
    (("10_boss_arena", "e", 30), ("11_reward_vault", "w", 14)),
)

THEMES = {
    "entry": ("minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:cyan_concrete", "minecraft:sea_lantern"),
    "descent": ("minecraft:deepslate_tiles", "minecraft:polished_deepslate", "minecraft:chiseled_deepslate", "minecraft:soul_lantern"),
    "necropolis": ("minecraft:polished_blackstone_bricks", "minecraft:blackstone", "minecraft:bone_block", "minecraft:soul_lantern"),
    "guardian": ("minecraft:deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:gilded_blackstone", "minecraft:sea_lantern"),
    "catacombs": ("minecraft:cracked_deepslate_bricks", "minecraft:tuff", "minecraft:bone_block", "minecraft:soul_lantern"),
    "bridge": ("minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:sculk", "minecraft:sea_lantern"),
    "prison": ("minecraft:deepslate_bricks", "minecraft:polished_blackstone", "minecraft:iron_bars", "minecraft:soul_lantern"),
    "elite": ("minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone", "minecraft:gilded_blackstone", "minecraft:shroomlight"),
    "ritual": ("minecraft:deepslate_tiles", "minecraft:sculk", "minecraft:crying_obsidian", "minecraft:soul_lantern"),
    "approach": ("minecraft:polished_blackstone_bricks", "minecraft:polished_deepslate", "minecraft:obsidian", "minecraft:sea_lantern"),
    "arena": ("minecraft:obsidian", "minecraft:polished_blackstone", "minecraft:gilded_blackstone", "minecraft:sea_lantern"),
    "vault": ("minecraft:polished_blackstone_bricks", "minecraft:smooth_quartz", "minecraft:gold_block", "minecraft:sea_lantern"),
}


@dataclass
class Structure:
    module: Module
    blocks: dict[tuple[int, int, int], tuple[str, dict[str, str] | None]] = field(default_factory=dict)

    def set(self, x: int, y: int, z: int, name: str, nbt: dict[str, str] | None = None) -> None:
        sx, sy, sz = self.module.size
        if 0 <= x < sx and 0 <= y < sy and 0 <= z < sz:
            self.blocks[(x, y, z)] = (name, nbt)

    def fill(self, x0: int, x1: int, y0: int, y1: int, z0: int, z1: int, name: str) -> None:
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                for x in range(x0, x1 + 1):
                    self.set(x, y, z, name)


def shell(structure: Structure) -> None:
    sx, sy, sz = structure.module.size
    wall, floor, accent, light = THEMES[structure.module.style]
    structure.fill(0, sx - 1, 0, 0, 0, sz - 1, floor)
    structure.fill(0, sx - 1, sy - 1, sy - 1, 0, sz - 1, wall)
    for thickness in (0, 1):
        structure.fill(thickness, thickness, 1, sy - 2, 0, sz - 1, wall)
        structure.fill(sx - 1 - thickness, sx - 1 - thickness, 1, sy - 2, 0, sz - 1, wall)
        structure.fill(0, sx - 1, 1, sy - 2, thickness, thickness, wall)
        structure.fill(0, sx - 1, 1, sy - 2, sz - 1 - thickness, sz - 1 - thickness, wall)
    # Readable route frame, landmarks, and secondary detail layer.
    for x in range(4, sx - 3, 7):
        structure.fill(x, x, 1, min(5, sy - 2), 2, 2, accent)
        structure.fill(x, x, 1, min(5, sy - 2), sz - 3, sz - 3, accent)
    for z in range(5, sz - 3, 8):
        structure.fill(2, 2, 1, min(5, sy - 2), z, z, accent)
        structure.fill(sx - 3, sx - 3, 1, min(5, sy - 2), z, z, accent)
    for x, z in ((3, 3), (sx - 4, 3), (3, sz - 4), (sx - 4, sz - 4)):
        structure.set(x, min(6, sy - 3), z, light)

    for side, center in structure.module.connectors:
        if side in ("n", "s"):
            z0, z1 = (0, 1) if side == "n" else (sz - 2, sz - 1)
            for x in range(center - 2, center + 3):
                for y in range(1, min(6, sy - 1)):
                    structure.blocks.pop((x, y, z0), None)
                    structure.blocks.pop((x, y, z1), None)
        else:
            x0, x1 = (0, 1) if side == "w" else (sx - 2, sx - 1)
            for z in range(center - 2, center + 3):
                for y in range(1, min(6, sy - 1)):
                    structure.blocks.pop((x0, y, z), None)
                    structure.blocks.pop((x1, y, z), None)


def decorate(structure: Structure) -> None:
    module = structure.module
    sx, sy, sz = module.size
    wall, floor, accent, light = THEMES[module.style]
    cx, cz = sx // 2, sz // 2
    if module.style == "descent":
        for z in range(4, sz - 3):
            y = 1 + min(4, z // 5)
            structure.fill(3, 7, 1, y, z, z, floor)
            structure.fill(sx - 8, sx - 4, 1, y, z, z, floor)
    elif module.style == "necropolis":
        for x in (10, 18, 29, 37):
            structure.fill(x, x + 2, 1, 3, 8, 12, "minecraft:bone_block")
            structure.fill(x, x + 2, 1, 3, 28, 32, "minecraft:bone_block")
    elif module.style == "guardian":
        for x in (12, 35):
            structure.fill(x, x, 1, 10, 10, 10, accent)
            structure.fill(x, x, 1, 10, 29, 29, accent)
        structure.fill(cx - 5, cx + 5, 1, 2, cz - 3, cz + 3, "minecraft:polished_blackstone_bricks")
    elif module.style == "catacombs":
        for z in (9, 17, 26, 34):
            structure.fill(5, 10, 1, 2, z, z + 2, "minecraft:bone_block")
            structure.fill(sx - 11, sx - 6, 1, 2, z, z + 2, "minecraft:bone_block")
        structure.fill(2, sx - 3, 1, 7, cz, cz, wall)
        for x in range(29, 34):
            for y in range(1, 6): structure.blocks.pop((x, y, cz), None)
    elif module.style == "bridge":
        # Narrow traversable spine with broken side decks and strong ranged sightlines.
        for x in range(2, sx - 2):
            for z in range(5, sz - 5):
                if abs(x - cx) > 5 and (x + z) % 4 != 0:
                    structure.blocks.pop((x, 0, z), None)
        structure.fill(cx - 4, cx + 4, 0, 0, 2, sz - 3, floor)
        # Explicit ranged pads keep encounter markers safe while preserving broken side decks.
        for marker, (x, y, z) in module.markers:
            if marker.startswith("enemy:ranged:"):
                structure.fill(x - 1, x + 1, y - 1, y - 1, z - 1, z + 1, floor)
        for z in range(7, sz - 5, 7):
            structure.fill(cx - 6, cx - 6, 1, 7, z, z, accent)
            structure.fill(cx + 6, cx + 6, 1, 7, z, z, accent)
    elif module.style == "prison":
        for z in (8, 16, 24, 32):
            structure.fill(3, 12, 1, 6, z, z, "minecraft:iron_bars")
            structure.fill(sx - 13, sx - 4, 1, 6, z, z, "minecraft:iron_bars")
    elif module.style == "elite":
        for x, z in ((12, 12), (34, 12), (12, 29), (34, 29)):
            structure.fill(x, x, 1, 12, z, z, accent)
            structure.set(x, 13, z, light)
    elif module.style == "ritual":
        for x in range(cx - 10, cx + 11):
            for z in range(cz - 10, cz + 11):
                radius = abs(x - cx) + abs(z - cz)
                if radius in (9, 10): structure.set(x, 0, z, "minecraft:crying_obsidian")
        structure.fill(cx - 3, cx + 3, 1, 2, cz - 3, cz + 3, "minecraft:sculk")
    elif module.style == "approach":
        for z in range(4, sz - 3, 5):
            structure.fill(4, 4, 1, 10, z, z, accent)
            structure.fill(sx - 5, sx - 5, 1, 10, z, z, accent)
    elif module.style == "arena":
        structure.fill(cx - 8, cx + 8, 1, 2, sz - 10, sz - 5, "minecraft:polished_blackstone_bricks")
        structure.fill(cx - 4, cx + 4, 3, 6, sz - 8, sz - 5, "minecraft:gold_block")
        for x, z in ((8, 8), (sx - 9, 8), (8, sz - 9), (sx - 9, sz - 9)):
            structure.fill(x, x, 1, 16, z, z, "minecraft:obsidian")
            structure.set(x, 17, z, light)
    elif module.style == "vault":
        structure.fill(4, sx - 5, 0, 0, 5, sz - 6, "minecraft:smooth_quartz")
        structure.fill(cx - 4, cx + 4, 1, 3, cz - 3, cz + 3, "minecraft:gold_block")

    for marker, pos in module.markers:
        structure.set(*pos, "minecraft:structure_block", {"mode": "DATA", "metadata": marker})
        if marker.startswith("secret:"):
            structure.set(pos[0] + 1, pos[1], pos[2], "minecraft:chest",
                          {"id": "minecraft:chest", "LootTable": "minecraft:chests/simple_dungeon"})


def build(module: Module) -> Structure:
    structure = Structure(module)
    shell(structure)
    decorate(structure)
    return structure


def validate_structure(structure: Structure) -> None:
    module = structure.module
    light = THEMES[module.style][3]
    authored_lights = sum(name == light for name, _ in structure.blocks.values())
    if authored_lights < 4:
        raise ValueError(f"module has fewer than four authored light blocks: {module.id}")

    def solid(pos: tuple[int, int, int]) -> bool:
        persisted = structure.blocks.get(pos)
        return persisted is not None and persisted[0] not in {
            "minecraft:structure_block", "minecraft:structure_void",
        }

    for marker, (x, y, z) in module.markers:
        if marker not in {"entry", "boss", "reward"} and not marker.startswith(("objective:", "enemy:")):
            continue
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if solid((x + dx, y, z + dz)) or solid((x + dx, y + 1, z + dz)):
                    raise ValueError(f"blocked standing clearance at {module.id}:{marker}")
                if not solid((x + dx, y - 1, z + dz)):
                    raise ValueError(f"unsupported standing clearance at {module.id}:{marker}")


def quote(value: str) -> str:
    return json.dumps(value, ensure_ascii=True)


def to_snbt(structure: Structure) -> str:
    palette_names = sorted({name for name, _ in structure.blocks.values()})
    palette_index = {name: index for index, name in enumerate(palette_names)}
    palette = ",".join(f'{{Name:{quote(name)}}}' for name in palette_names)
    entries = []
    for (x, y, z), (name, nbt) in sorted(structure.blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        extra = ""
        if nbt:
            extra = ",nbt:{" + ",".join(f"{key}:{quote(value)}" for key, value in nbt.items()) + "}"
        entries.append(f"{{pos:[{x},{y},{z}],state:{palette_index[name]}{extra}}}")
    sx, sy, sz = structure.module.size
    return (f"{{DataVersion:{DATA_VERSION},size:[{sx},{sy},{sz}],palette:[{palette}],entities:[],blocks:[\n"
            + ",\n".join(entries) + "\n]}\n")


def validate_catalog() -> dict[str, object]:
    ids = [module.id for module in MODULES]
    if len(ids) != len(set(ids)) or ids != sorted(ids):
        raise ValueError("module ids must be unique and ordered")
    markers = [name for module in MODULES for name, _ in module.markers]
    missing = REQUIRED_MARKERS - set(markers)
    if missing:
        raise ValueError(f"missing required markers: {sorted(missing)}")
    for module in MODULES:
        if any(axis <= 0 or axis > 48 for axis in module.size):
            raise ValueError(f"module exceeds vanilla structure bounds: {module.id}")
        for _, pos in module.markers:
            if any(value < 0 or value >= module.size[index] for index, value in enumerate(pos)):
                raise ValueError(f"marker outside module {module.id}: {pos}")
        validate_structure(build(module))
    by_id = {module.id: module for module in MODULES}
    used_connectors: set[tuple[str, str, int]] = set()
    graph: dict[str, set[str]] = defaultdict(set)
    for left, right in CONNECTIONS:
        if left in used_connectors or right in used_connectors:
            raise ValueError(f"connector reused: {left} or {right}")
        used_connectors.update((left, right))
        left_module, right_module = by_id[left[0]], by_id[right[0]]
        if (left[1], left[2]) not in left_module.connectors or (right[1], right[2]) not in right_module.connectors:
            raise ValueError(f"unknown connector in edge: {left} -> {right}")
        if connector_point(left_module, left[1], left[2]).dist(connector_point(right_module, right[1], right[2])) != 1:
            raise ValueError(f"connector seam is not adjacent: {left} -> {right}")
        graph[left[0]].add(right[0]); graph[right[0]].add(left[0])
    reached = {MODULES[0].id}
    queue = deque(reached)
    while queue:
        for target in graph[queue.popleft()]:
            if target not in reached: reached.add(target); queue.append(target)
    if reached != set(ids):
        raise ValueError(f"disconnected modules: {sorted(set(ids) - reached)}")
    all_connectors = {(module.id, side, center) for module in MODULES for side, center in module.connectors}
    if used_connectors != all_connectors:
        raise ValueError(f"unpaired connectors: {sorted(all_connectors - used_connectors)}")
    min_x = min(m.offset[0] for m in MODULES)
    min_y = min(m.offset[1] for m in MODULES)
    min_z = min(m.offset[2] for m in MODULES)
    max_x = max(m.offset[0] + m.size[0] - 1 for m in MODULES)
    max_y = max(m.offset[1] + m.size[1] - 1 for m in MODULES)
    max_z = max(m.offset[2] + m.size[2] - 1 for m in MODULES)
    return {
        "id": "master", "display_name": "Abyssal Necropolis", "construction": "modular structure templates",
        "module_count": len(MODULES), "module_ids": ids, "marker_count": len(markers),
        "topology_edges": len(CONNECTIONS),
        "required_markers": sorted(REQUIRED_MARKERS), "secret_count": sum(x.startswith("secret:") for x in markers),
        "shortcut_count": sum(x.startswith("shortcut:") for x in markers),
        "bounds": {"min": [min_x, min_y, min_z], "max": [max_x, max_y, max_z],
                   "size": [max_x - min_x + 1, max_y - min_y + 1, max_z - min_z + 1]},
        "modules": [{"id": m.id, "offset": m.offset, "size": m.size, "style": m.style,
                     "connectors": m.connectors, "markers": [name for name, _ in m.markers]} for m in MODULES],
    }


@dataclass(frozen=True)
class Point:
    x: int
    y: int
    z: int

    def dist(self, other: "Point") -> int:
        return abs(self.x - other.x) + abs(self.y - other.y) + abs(self.z - other.z)


def connector_point(module: Module, side: str, center: int) -> Point:
    ox, oy, oz = module.offset
    sx, _, sz = module.size
    if side == "n": return Point(ox + center, oy + 1, oz)
    if side == "s": return Point(ox + center, oy + 1, oz + sz - 1)
    if side == "w": return Point(ox, oy + 1, oz + center)
    if side == "e": return Point(ox + sx - 1, oy + 1, oz + center)
    raise ValueError(f"unknown connector side: {side}")


def write_modules(output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    for module in MODULES:
        (output / f"{module.id}.snbt").write_text(to_snbt(build(module)), encoding="utf-8")


def check_modules(existing: Path) -> None:
    with tempfile.TemporaryDirectory() as directory:
        generated = Path(directory)
        write_modules(generated)
        for module in MODULES:
            name = f"{module.id}.snbt"
            path = existing / name
            if not path.is_file() or path.read_bytes() != (generated / name).read_bytes():
                raise ValueError(f"stale or missing structure module: {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    parser.add_argument("--check", type=Path)
    parser.add_argument("--manifest", type=Path)
    args = parser.parse_args()
    metrics = validate_catalog()
    if args.output: write_modules(args.output)
    if args.check: check_modules(args.check)
    if args.manifest:
        args.manifest.parent.mkdir(parents=True, exist_ok=True)
        args.manifest.write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__":
    main()
