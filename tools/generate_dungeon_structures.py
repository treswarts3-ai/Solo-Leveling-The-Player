#!/usr/bin/env python3
"""Generate the four authored Solo Leveling dungeon structure templates.

The maps use Minecraft's vanilla Java structure-template NBT format.  They are
deliberately deterministic: every room, connector, landmark, light, and rubble
piece is hand-positioned so a release rebuild cannot create broken seams or
random holes.
"""

from __future__ import annotations

import argparse
import gzip
import json
import struct
from collections import deque
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


DATA_VERSION = 3465  # Minecraft Java 1.20.1
MIN_X, MAX_X = -44, 44
MIN_Y, MAX_Y = -2, 16
MIN_Z, MAX_Z = -44, 44
SIZE = (MAX_X - MIN_X + 1, MAX_Y - MIN_Y + 1, MAX_Z - MIN_Z + 1)

AIR = "minecraft:air"


@dataclass(frozen=True)
class MapSpec:
    template_id: str
    display_name: str
    entry: tuple[int, int, int]
    objective_centers: tuple[tuple[int, int, int], ...]
    checkpoint_centers: tuple[tuple[int, int, int], ...]


class Blueprint:
    def __init__(self, spec: MapSpec):
        self.spec = spec
        self.blocks: dict[tuple[int, int, int], str] = {}

    def set(self, x: int, y: int, z: int, state: str) -> None:
        if not (MIN_X <= x <= MAX_X and MIN_Y <= y <= MAX_Y and MIN_Z <= z <= MAX_Z):
            raise ValueError(f"{self.spec.template_id}: block outside template at {(x, y, z)}")
        self.blocks[(x, y, z)] = state

    def box(self, min_x: int, max_x: int, min_y: int, max_y: int,
            min_z: int, max_z: int, state: str) -> None:
        for y in range(min_y, max_y + 1):
            for z in range(min_z, max_z + 1):
                for x in range(min_x, max_x + 1):
                    self.set(x, y, z, state)

    def clear(self, min_x: int, max_x: int, min_y: int, max_y: int,
              min_z: int, max_z: int) -> None:
        self.box(min_x, max_x, min_y, max_y, min_z, max_z, AIR)

    def room(self, min_x: int, max_x: int, min_z: int, max_z: int, height: int,
             wall: str, floor: str, ceiling: str, trim: str | None = None) -> None:
        self.box(min_x, max_x, 0, 0, min_z, max_z, floor)
        self.box(min_x, max_x, height, height, min_z, max_z, ceiling)
        self.box(min_x, min_x, 1, height - 1, min_z, max_z, wall)
        self.box(max_x, max_x, 1, height - 1, min_z, max_z, wall)
        self.box(min_x, max_x, 1, height - 1, min_z, min_z, wall)
        self.box(min_x, max_x, 1, height - 1, max_z, max_z, wall)
        self.clear(min_x + 1, max_x - 1, 1, height - 1, min_z + 1, max_z - 1)
        if trim:
            self.box(min_x + 1, max_x - 1, 1, 1, min_z, min_z, trim)
            self.box(min_x + 1, max_x - 1, 1, 1, max_z, max_z, trim)
            self.box(min_x, min_x, 1, 1, min_z + 1, max_z - 1, trim)
            self.box(max_x, max_x, 1, 1, min_z + 1, max_z - 1, trim)

    def corridor_x(self, min_x: int, max_x: int, min_z: int, max_z: int, height: int,
                   wall: str, floor: str, ceiling: str) -> None:
        self.room(min_x, max_x, min_z, max_z, height, wall, floor, ceiling)
        self.clear(min_x, min_x, 1, height - 1, min_z + 1, max_z - 1)
        self.clear(max_x, max_x, 1, height - 1, min_z + 1, max_z - 1)

    def corridor_z(self, min_x: int, max_x: int, min_z: int, max_z: int, height: int,
                   wall: str, floor: str, ceiling: str) -> None:
        self.room(min_x, max_x, min_z, max_z, height, wall, floor, ceiling)
        self.clear(min_x + 1, max_x - 1, 1, height - 1, min_z, min_z)
        self.clear(min_x + 1, max_x - 1, 1, height - 1, max_z, max_z)

    def pillar(self, x: int, z: int, min_y: int, max_y: int, state: str) -> None:
        self.box(x, x, min_y, max_y, z, z, state)

    def frame_x(self, x: int, min_z: int, max_z: int, height: int, state: str) -> None:
        self.pillar(x, min_z, 1, height, state)
        self.pillar(x, max_z, 1, height, state)
        self.box(x, x, height, height, min_z, max_z, state)

    def frame_z(self, z: int, min_x: int, max_x: int, height: int, state: str) -> None:
        self.pillar(min_x, z, 1, height, state)
        self.pillar(max_x, z, 1, height, state)
        self.box(min_x, max_x, height, height, z, z, state)

    def chain_light(self, x: int, z: int, ceiling_y: int, light: str = "minecraft:lantern[hanging=true]") -> None:
        self.set(x, ceiling_y - 1, z, "minecraft:chain[axis=y]")
        self.set(x, ceiling_y - 2, z, light)

    def bench_x(self, x: int, z: int, length: int = 4, wood: str = "dark_oak") -> None:
        slab = f"minecraft:{wood}_slab[type=bottom]"
        fence = f"minecraft:{wood}_fence"
        for dx in range(length):
            self.set(x + dx, 1, z, slab)
        self.set(x, 0, z, fence)
        self.set(x + length - 1, 0, z, fence)

    def rubble(self, x: int, z: int, style: str = "stone") -> None:
        if style == "blackstone":
            states = ("minecraft:blackstone", "minecraft:polished_blackstone_bricks",
                      "minecraft:cracked_polished_blackstone_bricks", "minecraft:blackstone_slab[type=bottom]")
        else:
            states = ("minecraft:cobblestone", "minecraft:mossy_cobblestone",
                      "minecraft:gravel", "minecraft:cobblestone_slab[type=bottom]")
        self.set(x, 1, z, states[0])
        self.set(x + 1, 1, z, states[1])
        self.set(x, 1, z + 1, states[2])
        self.set(x + 1, 2, z + 1, states[3])

    def crate(self, x: int, z: int, double: bool = False) -> None:
        self.set(x, 1, z, "minecraft:barrel")
        if double:
            self.set(x + 1, 1, z, "minecraft:barrel")
            self.set(x, 2, z, "minecraft:spruce_planks")


def subway() -> Blueprint:
    spec = MapSpec(
        "abandoned_subway", "Abandoned Subway",
        (0, 1, -38),
        ((-5, 1, -8), (35, 1, 2), (25, 1, 21), (0, 1, 35), (-36, 1, 38)),
        ((29, 2, -5), (35, 2, 15), (20, 2, 28), (-24, 2, 34)),
    )
    b = Blueprint(spec)
    wall = "minecraft:deepslate_bricks"
    cracked = "minecraft:cracked_deepslate_bricks"
    floor = "minecraft:polished_deepslate"
    ceiling = "minecraft:deepslate_tiles"

    b.room(-8, 8, -42, -34, 7, wall, "minecraft:polished_andesite", ceiling, cracked)
    b.room(-17, 17, -33, -19, 9, wall, "minecraft:smooth_stone", ceiling, cracked)
    b.room(-36, 28, -18, -4, 10, wall, floor, ceiling, cracked)
    b.room(29, 42, -10, 13, 9, wall, "minecraft:polished_andesite", ceiling, cracked)
    b.room(10, 40, 15, 27, 8, wall, "minecraft:smooth_stone", ceiling, cracked)
    b.room(-24, 24, 28, 42, 13, "minecraft:polished_blackstone_bricks",
           "minecraft:polished_blackstone", "minecraft:blackstone", "minecraft:gilded_blackstone")
    b.room(-42, -30, 27, 42, 8, "minecraft:gilded_blackstone",
           "minecraft:polished_blackstone", "minecraft:blackstone", "minecraft:gold_block")

    b.corridor_z(-4, 4, -35, -32, 6, wall, "minecraft:polished_andesite", ceiling)
    b.corridor_z(-5, 5, -20, -17, 6, wall, floor, ceiling)
    b.corridor_x(27, 31, -8, -2, 6, wall, floor, ceiling)
    b.corridor_z(32, 38, 11, 17, 6, wall, "minecraft:polished_andesite", ceiling)
    b.corridor_z(17, 23, 26, 30, 7, "minecraft:polished_blackstone_bricks",
                 "minecraft:polished_blackstone", "minecraft:blackstone")
    b.corridor_x(-31, -22, 31, 37, 6, "minecraft:polished_blackstone_bricks",
                 "minecraft:polished_blackstone", "minecraft:blackstone")

    # Entrance and ticket hall landmarks.
    b.box(-6, 6, 1, 1, -41, -41, "minecraft:chiseled_deepslate")
    for x in (-5, 5):
        b.pillar(x, -38, 1, 5, "minecraft:polished_basalt")
        b.set(x, 6, -38, "minecraft:sea_lantern")
    b.box(-5, 5, 6, 6, -42, -42, "minecraft:cyan_concrete")
    b.box(-3, 3, 6, 6, -41, -41, "minecraft:sea_lantern")
    for x in (-13, 9):
        b.box(x, x + 4, 1, 1, -30, -27, "minecraft:polished_andesite")
        b.box(x, x + 4, 2, 3, -30, -30, "minecraft:iron_bars")
        b.box(x, x + 4, 4, 4, -30, -27, "minecraft:deepslate_tile_slab[type=bottom]")
    for x in range(-8, 9, 4):
        b.set(x, 1, -20, "minecraft:iron_bars")
        b.set(x, 2, -20, "minecraft:iron_bars")
    b.clear(-2, 2, 1, 3, -20, -20)
    for x in (-12, 0, 12):
        b.chain_light(x, -25, 9)
    b.box(-15, -15, 2, 5, -27, -23, "minecraft:light_blue_stained_glass")
    b.box(-14, -14, 2, 5, -27, -23, "minecraft:blue_concrete")

    # Wide platform, recessed rail bed, yellow safety lines, train, benches, and support bays.
    b.box(-34, 26, 0, 0, -14, -9, "minecraft:gravel")
    for z in (-13, -10):
        for x in range(-33, 27):
            b.set(x, 1, z, "minecraft:rail[shape=east_west]")
    b.box(-34, 26, 1, 1, -17, -16, "minecraft:smooth_stone")
    b.box(-34, 26, 1, 1, -7, -5, "minecraft:smooth_stone")
    b.box(-34, 26, 2, 2, -16, -16, "minecraft:yellow_concrete")
    b.box(-34, 26, 2, 2, -7, -7, "minecraft:yellow_concrete")
    for x in range(-30, 25, 10):
        for z in (-16, -6):
            b.pillar(x, z, 2, 7, "minecraft:polished_basalt")
            b.set(x, 8, z, "minecraft:sea_lantern")
    # Derelict subway car on the north track.
    b.box(-22, 10, 2, 2, -14, -10, "minecraft:iron_block")
    b.box(-22, 10, 3, 5, -14, -14, "minecraft:iron_block")
    b.box(-22, 10, 3, 5, -10, -10, "minecraft:iron_block")
    b.box(-22, 10, 6, 6, -14, -10, "minecraft:smooth_stone_slab[type=bottom]")
    b.box(-21, 9, 4, 5, -14, -14, "minecraft:light_blue_stained_glass")
    b.box(-21, 9, 4, 5, -10, -10, "minecraft:light_blue_stained_glass")
    for x in (-22, -11, 0, 10):
        b.box(x, x, 3, 5, -14, -10, "minecraft:iron_block")
    b.clear(-2, 1, 3, 4, -10, -10)
    b.bench_x(-31, -6, 5)
    b.bench_x(12, -17, 5)
    b.rubble(-33, -8)
    b.rubble(20, -16)

    # Maintenance plant: copper tanks, overhead pipes, work bays, and a sealed leak.
    for z in (-7, 2, 10):
        b.box(39, 40, 1, 5, z, z + 3, "minecraft:copper_block")
        b.box(38, 41, 5, 5, z, z + 3, "minecraft:cut_copper")
    b.box(31, 40, 7, 7, -8, 10, "minecraft:exposed_copper")
    for z in range(-7, 11, 6):
        b.pillar(31, z, 1, 5, "minecraft:polished_andesite")
        b.set(31, 6, z, "minecraft:sea_lantern")
    b.box(30, 30, 2, 5, 4, 9, "minecraft:blue_stained_glass")
    b.box(31, 31, 1, 1, 5, 8, "minecraft:water_cauldron[level=3]")
    b.set(34, 1, 9, "minecraft:crafting_table")
    b.set(36, 1, 9, "minecraft:anvil")
    b.crate(32, -7, True)
    b.rubble(35, 5)

    # Security checkpoint and operations room.
    b.box(12, 38, 1, 2, 17, 17, "minecraft:iron_bars")
    b.clear(32, 37, 1, 4, 17, 17)
    for x in (14, 22, 30, 38):
        b.pillar(x, 18, 1, 5, "minecraft:polished_basalt")
        b.set(x, 6, 18, "minecraft:redstone_lamp[lit=true]")
    b.box(13, 19, 1, 1, 24, 25, "minecraft:polished_andesite")
    b.box(29, 37, 1, 1, 24, 25, "minecraft:polished_andesite")
    for x in (14, 16, 18, 30, 32, 34, 36):
        b.set(x, 2, 24, "minecraft:dark_oak_slab[type=bottom]")
    b.set(20, 1, 24, "minecraft:smithing_table")
    b.set(27, 1, 24, "minecraft:anvil")
    b.box(12, 12, 2, 5, 20, 25, "minecraft:iron_bars")

    # Warden arena: monumental blackstone station terminal and central sigil.
    for x in (-19, -10, 10, 19):
        for z in (31, 39):
            b.pillar(x, z, 1, 9, "minecraft:chiseled_polished_blackstone")
            b.set(x, 10, z, "minecraft:soul_lantern[hanging=true]")
    for x in range(-8, 9):
        for z in range(31, 40):
            if abs(x) + abs(z - 35) <= 10:
                b.set(x, 0, z, "minecraft:gilded_blackstone")
    b.box(-7, 7, 1, 1, 39, 41, "minecraft:polished_blackstone_bricks")
    b.box(-4, 4, 2, 2, 40, 41, "minecraft:gilded_blackstone")
    b.box(-16, 16, 8, 8, 42, 42, "minecraft:blue_concrete")
    b.box(-12, 12, 9, 9, 42, 42, "minecraft:sea_lantern")
    b.rubble(-21, 30, "blackstone")
    b.rubble(18, 40, "blackstone")

    # Gold-lined vault, presentation plinth, and ceiling lights.
    b.box(-40, -32, 0, 0, 29, 40, "minecraft:polished_blackstone")
    b.box(-41, -41, 2, 6, 29, 40, "minecraft:gold_block")
    b.box(-31, -31, 2, 6, 29, 40, "minecraft:gold_block")
    for z in (30, 34, 38):
        b.chain_light(-36, z, 8, "minecraft:soul_lantern[hanging=true]")
    b.box(-38, -34, 1, 1, 33, 35, "minecraft:gold_block")

    for x, y, z in spec.checkpoint_centers:
        b.frame_x(x, z - 3, z + 3, 5, "minecraft:polished_basalt") if x in (29, -24) else b.frame_z(z, x - 3, x + 3, 5, "minecraft:polished_basalt")
    # Preserve the authored center aisle through the raised platform lip.
    b.clear(-4, 4, 1, 4, -17, -16)
    b.clear(22, 28, 1, 4, -7, -5)
    b.clear(-31, -30, 1, 5, 31, 37)
    return b


def red_orc_outpost() -> Blueprint:
    spec = MapSpec(
        "red_orc_outpost", "Red Orc Stronghold",
        (0, 1, -39),
        ((0, 1, -22), (0, 1, 14), (-18, 1, 33), (24, 1, 39)),
        ((0, 2, -13), (0, 2, 24), (10, 2, 34)),
    )
    b = Blueprint(spec)
    rock = "minecraft:cobbled_deepslate"
    wall = "minecraft:polished_blackstone_bricks"
    cracked = "minecraft:cracked_polished_blackstone_bricks"
    floor = "minecraft:polished_blackstone"
    roof = "minecraft:basalt"

    b.room(-10, 10, -43, -34, 8, rock, "minecraft:tuff", "minecraft:deepslate", "minecraft:mossy_cobblestone")
    b.room(-31, 31, -33, -14, 12, wall, floor, roof, cracked)
    b.room(-42, -18, -13, 7, 9, rock, "minecraft:coarse_dirt", "minecraft:deepslate", "minecraft:mossy_cobblestone")
    b.room(18, 42, -13, 7, 10, wall, "minecraft:polished_basalt", roof, "minecraft:red_nether_bricks")
    b.room(-23, 23, 6, 23, 12, wall, floor, roof, cracked)
    b.room(-34, 8, 25, 42, 11, wall, "minecraft:polished_blackstone", roof, "minecraft:red_nether_bricks")
    b.room(12, 38, 27, 41, 9, "minecraft:red_nether_bricks",
           "minecraft:polished_blackstone", "minecraft:nether_bricks", "minecraft:gold_block")

    b.corridor_z(-5, 5, -35, -31, 7, rock, "minecraft:tuff", "minecraft:deepslate")
    b.corridor_z(-6, 6, -15, 8, 8, wall, floor, roof)
    b.corridor_x(-20, -6, -4, 3, 7, rock, "minecraft:coarse_dirt", "minecraft:deepslate")
    b.corridor_x(5, 20, -4, 3, 7, wall, floor, roof)
    b.corridor_z(-5, 5, 21, 27, 8, wall, floor, roof)
    b.corridor_x(6, 14, 31, 37, 7, "minecraft:red_nether_bricks", floor, "minecraft:nether_bricks")

    # Carved cave entrance and tusked war gate.
    for x in (-7, 7):
        b.pillar(x, -38, 1, 6, "minecraft:polished_basalt")
        b.set(x, 7, -38, "minecraft:lantern[hanging=true]")
    b.box(-8, 8, 7, 7, -34, -34, "minecraft:red_concrete")
    for x in (-5, 5):
        b.box(x, x, 2, 7, -32, -32, "minecraft:bone_block[axis=y]")
        b.set(x + (1 if x < 0 else -1), 8, -32, "minecraft:bone_block[axis=x]")
    b.clear(-3, 3, 1, 6, -32, -32)

    # Outer war yard: palisades, watchtowers, banners, cages, and supply stacks.
    for x in range(-27, 28, 6):
        b.pillar(x, -30, 1, 7, "minecraft:stripped_dark_oak_log[axis=y]")
        b.pillar(x, -17, 1, 7, "minecraft:stripped_dark_oak_log[axis=y]")
    for x in (-26, 26):
        for z in (-29, -18):
            b.box(x - 2, x + 2, 1, 1, z - 2, z + 2, "minecraft:dark_oak_planks")
            for px in (x - 2, x + 2):
                for pz in (z - 2, z + 2):
                    b.pillar(px, pz, 2, 8, "minecraft:stripped_dark_oak_log[axis=y]")
            b.box(x - 2, x + 2, 8, 8, z - 2, z + 2, "minecraft:dark_oak_slab[type=bottom]")
            b.set(x, 7, z, "minecraft:lantern[hanging=true]")
    for x in (-18, -6, 6, 18):
        b.box(x, x + 2, 3, 7, -33, -33, "minecraft:red_wool")
        b.box(x + 1, x + 1, 8, 9, -33, -33, "minecraft:chain[axis=y]")
    b.box(-23, -17, 1, 4, -23, -19, "minecraft:iron_bars")
    b.clear(-21, -19, 1, 3, -19, -19)
    for x, z in ((18, -27), (20, -20), (-13, -27)):
        b.crate(x, z, True)
    b.rubble(-28, -16, "blackstone")

    # West barracks with bunks and mess tables.
    for z in (-10, -4, 2):
        b.box(-39, -33, 1, 1, z, z + 2, "minecraft:dark_oak_planks")
        b.box(-39, -39, 2, 3, z, z + 2, "minecraft:red_wool")
        b.box(-33, -33, 2, 3, z, z + 2, "minecraft:red_wool")
    b.box(-29, -22, 1, 1, -9, -7, "minecraft:dark_oak_planks")
    for x in (-29, -22):
        b.set(x, 0, -9, "minecraft:dark_oak_fence")
        b.set(x, 0, -7, "minecraft:dark_oak_fence")
    b.chain_light(-29, 1, 9)
    b.chain_light(-22, 1, 9)

    # East forge with protected magma channels, anvils, chimneys, and weapon racks.
    b.box(20, 40, 0, 0, -10, -7, "minecraft:magma_block")
    b.box(20, 40, 1, 1, -10, -7, "minecraft:orange_stained_glass")
    for x in (22, 30, 38):
        b.pillar(x, -3, 1, 7, "minecraft:polished_basalt")
        b.set(x, 8, -3, "minecraft:shroomlight")
    for x in (24, 28, 34, 38):
        b.set(x, 1, 4, "minecraft:anvil")
        b.set(x, 2, 6, "minecraft:iron_bars")
        b.set(x, 3, 6, "minecraft:iron_bars")
    b.box(39, 40, 1, 6, 2, 5, "minecraft:blast_furnace[facing=west]")
    b.crate(20, 5, True)

    # Central war camp with a red sigil, tents, braziers, and raised command platform.
    for x in range(-8, 9):
        b.set(x, 0, 14, "minecraft:red_nether_bricks")
    for z in range(9, 20):
        b.set(0, 0, z, "minecraft:red_nether_bricks")
    for x, z in ((-16, 10), (16, 10), (-16, 19), (16, 19)):
        b.box(x - 3, x + 3, 1, 1, z - 2, z + 2, "minecraft:dark_oak_planks")
        b.box(x - 2, x + 2, 2, 4, z, z, "minecraft:red_wool")
        b.pillar(x - 3, z, 2, 5, "minecraft:dark_oak_fence")
        b.pillar(x + 3, z, 2, 5, "minecraft:dark_oak_fence")
    b.box(-6, 6, 1, 1, 20, 22, "minecraft:polished_blackstone_bricks")
    b.box(-4, 4, 2, 2, 21, 22, "minecraft:red_nether_bricks")
    for x in (-20, -10, 10, 20):
        b.chain_light(x, 8, 12, "minecraft:soul_lantern[hanging=true]")

    # Commander hall: throne, columns, trophy wall, and a broad dueling floor.
    for x in (-29, -20, -11, 3):
        b.pillar(x, 28, 1, 8, "minecraft:chiseled_polished_blackstone")
        b.pillar(x, 39, 1, 8, "minecraft:chiseled_polished_blackstone")
        b.set(x, 9, 28, "minecraft:soul_lantern[hanging=true]")
        b.set(x, 9, 39, "minecraft:soul_lantern[hanging=true]")
    b.box(-25, -11, 1, 1, 38, 41, "minecraft:polished_blackstone_bricks")
    b.box(-22, -14, 2, 3, 40, 41, "minecraft:red_nether_bricks")
    b.box(-19, -17, 4, 6, 41, 41, "minecraft:gold_block")
    for x in range(-30, 5, 5):
        b.set(x, 3, 42, "minecraft:bone_block[axis=y]")
        b.set(x, 4, 42, "minecraft:skeleton_skull[rotation=8]")
    b.box(-25, -11, 0, 0, 31, 36, "minecraft:red_nether_bricks")

    # Treasure hall framed as a captured human vault.
    b.box(14, 36, 0, 0, 29, 39, "minecraft:polished_blackstone")
    b.box(13, 13, 2, 7, 29, 39, "minecraft:gold_block")
    b.box(37, 37, 2, 7, 29, 39, "minecraft:gold_block")
    for x in (17, 25, 33):
        b.chain_light(x, 31, 9)
        b.chain_light(x, 38, 9)
    b.box(21, 27, 1, 1, 33, 36, "minecraft:gold_block")
    # Keep the command dais ceremonial without sealing the route north.
    b.clear(-4, 4, 1, 5, 20, 22)
    b.clear(12, 14, 1, 5, 31, 37)
    return b


def demon_castle() -> Blueprint:
    spec = MapSpec(
        "demon_castle_foyer", "Demon Castle Foyer",
        (0, 2, -39),
        ((0, 1, -16), (33, 1, 2), (0, 1, 10), (0, 1, 34), (-36, 1, 39)),
        ((30, 2, -7), (19, 2, 14), (0, 2, 23), (-28, 2, 34)),
    )
    b = Blueprint(spec)
    wall = "minecraft:polished_blackstone_bricks"
    cracked = "minecraft:cracked_polished_blackstone_bricks"
    floor = "minecraft:polished_blackstone"
    roof = "minecraft:nether_bricks"

    b.room(-11, 11, -43, -34, 10, wall, "minecraft:obsidian", roof, "minecraft:crying_obsidian")
    b.room(-21, 21, -33, -23, 12, wall, floor, roof, cracked)
    b.room(-37, 37, -22, -7, 15, wall, floor, roof, "minecraft:red_nether_bricks")
    b.room(20, 43, -6, 11, 12, wall, "minecraft:polished_basalt", roof, "minecraft:magma_block")
    b.room(-19, 19, 8, 22, 13, wall, floor, roof, "minecraft:crying_obsidian")
    b.room(-28, 28, 23, 43, 16, wall, "minecraft:polished_blackstone", "minecraft:obsidian", "minecraft:gilded_blackstone")
    b.room(-43, -31, 27, 42, 10, "minecraft:gilded_blackstone", floor, "minecraft:obsidian", "minecraft:gold_block")

    b.corridor_z(-5, 5, -35, -31, 8, wall, "minecraft:obsidian", roof)
    b.corridor_z(-6, 6, -24, -20, 9, wall, floor, roof)
    b.corridor_x(18, 22, -5, 3, 8, wall, floor, roof)
    b.corridor_x(17, 22, 11, 17, 8, wall, floor, roof)
    b.corridor_z(-5, 5, 20, 25, 9, wall, floor, roof)
    b.corridor_x(-33, -26, 31, 37, 8, wall, floor, "minecraft:obsidian")

    # Obsidian arrival bridge and animated-looking portal ribs.
    b.box(-4, 4, 1, 1, -41, -35, "minecraft:crying_obsidian")
    for z in (-40, -36):
        b.frame_z(z, -7, 7, 8, "minecraft:obsidian")
        b.box(-5, 5, 7, 7, z, z, "minecraft:purple_stained_glass")
    for x in (-8, 8):
        b.chain_light(x, -38, 10, "minecraft:soul_lantern[hanging=true]")

    # Ceremonial approach with towering ribs and a demonic face mosaic.
    for x in (-17, -9, 9, 17):
        b.pillar(x, -29, 1, 9, "minecraft:chiseled_polished_blackstone")
        b.set(x, 10, -29, "minecraft:shroomlight")
    b.box(-8, 8, 6, 10, -33, -33, "minecraft:red_nether_bricks")
    b.box(-6, -3, 8, 9, -32, -32, "minecraft:shroomlight")
    b.box(3, 6, 8, 9, -32, -32, "minecraft:shroomlight")
    b.box(-2, 2, 5, 7, -32, -32, "minecraft:obsidian")
    b.box(-5, 5, 2, 3, -32, -32, "minecraft:nether_bricks")

    # Grand nave with safe glass-covered magma canals and a central infernal seal.
    b.box(-34, -24, 0, 0, -20, -9, "minecraft:magma_block")
    b.box(24, 34, 0, 0, -20, -9, "minecraft:magma_block")
    b.box(-34, -24, 1, 1, -20, -9, "minecraft:orange_stained_glass")
    b.box(24, 34, 1, 1, -20, -9, "minecraft:orange_stained_glass")
    for x in (-20, -10, 10, 20):
        for z in (-19, -9):
            b.pillar(x, z, 1, 11, "minecraft:polished_basalt")
            b.set(x, 12, z, "minecraft:soul_lantern[hanging=true]")
    for radius, block in ((8, "minecraft:red_nether_bricks"), (5, "minecraft:crying_obsidian"), (2, "minecraft:gilded_blackstone")):
        for x in range(-radius, radius + 1):
            for z in range(-18, -7):
                if abs(abs(x) + abs(z + 13) - radius) <= 0:
                    b.set(x, 0, z, block)
    for x in (-32, -16, 16, 32):
        b.box(x, x + 2, 4, 10, -22, -22, "minecraft:red_wool")
        b.box(x + 1, x + 1, 11, 13, -22, -22, "minecraft:chain[axis=y]")

    # Infernal forge: furnaces, magma crucibles, chains, and protected smelting lanes.
    b.box(22, 41, 0, 0, -4, 0, "minecraft:magma_block")
    b.box(22, 41, 1, 1, -4, 0, "minecraft:orange_stained_glass")
    for z in (3, 8):
        for x in (24, 31, 38):
            b.box(x, x + 2, 1, 3, z, z + 1, "minecraft:blast_furnace[facing=west]")
            b.pillar(x + 1, z + 1, 4, 8, "minecraft:polished_basalt")
    for x in (25, 33, 40):
        b.chain_light(x, 5, 12, "minecraft:soul_lantern[hanging=true]")
    b.box(42, 42, 2, 8, -3, 8, "minecraft:red_stained_glass")
    b.crate(23, 9, True)

    # Obsidian chapel and twin commander platforms.
    b.box(-16, 16, 0, 0, 11, 20, "minecraft:obsidian")
    for x in (-15, -8, 8, 15):
        b.pillar(x, 10, 1, 10, "minecraft:crying_obsidian")
        b.pillar(x, 20, 1, 10, "minecraft:crying_obsidian")
        b.set(x, 11, 10, "minecraft:soul_lantern[hanging=true]")
        b.set(x, 11, 20, "minecraft:soul_lantern[hanging=true]")
    b.box(-14, -5, 1, 2, 17, 21, "minecraft:polished_blackstone_bricks")
    b.box(5, 14, 1, 2, 17, 21, "minecraft:polished_blackstone_bricks")
    b.box(-11, -8, 3, 4, 20, 21, "minecraft:red_nether_bricks")
    b.box(8, 11, 3, 4, 20, 21, "minecraft:red_nether_bricks")
    b.box(-3, 3, 1, 1, 12, 18, "minecraft:gilded_blackstone")

    # Sovereign throne room: tiered dais, monumental throne, chains, and lava-glass windows.
    for x in (-23, -14, 14, 23):
        for z in (27, 39):
            b.pillar(x, z, 1, 12, "minecraft:chiseled_polished_blackstone")
            b.set(x, 13, z, "minecraft:soul_lantern[hanging=true]")
    b.box(-10, 10, 1, 1, 36, 41, "minecraft:polished_blackstone_bricks")
    b.box(-8, 8, 2, 2, 38, 41, "minecraft:gilded_blackstone")
    b.box(-6, 6, 3, 7, 40, 42, "minecraft:obsidian")
    b.box(-4, 4, 4, 8, 39, 39, "minecraft:red_nether_bricks")
    b.box(-2, 2, 8, 11, 40, 41, "minecraft:gold_block")
    for x in range(-25, 26, 5):
        b.box(x, x + 2, 4, 11, 43, 43, "minecraft:red_stained_glass")
    b.box(-25, 25, 12, 12, 42, 42, "minecraft:shroomlight")
    for x, z in ((-20, 31), (20, 31), (-20, 40), (20, 40)):
        b.rubble(x, z, "blackstone")

    # Sovereign vault.
    b.box(-41, -33, 0, 0, 29, 40, "minecraft:gilded_blackstone")
    b.box(-42, -42, 2, 8, 29, 40, "minecraft:gold_block")
    b.box(-32, -32, 2, 8, 29, 40, "minecraft:gold_block")
    for z in (30, 35, 40):
        b.chain_light(-37, z, 10, "minecraft:soul_lantern[hanging=true]")
    b.box(-39, -35, 1, 2, 33, 36, "minecraft:gold_block")
    # The face remains readable above a full-height processional doorway.
    b.clear(-3, 3, 1, 5, -33, -32)
    b.clear(27, 35, 1, 6, -7, -6)
    b.clear(17, 23, 1, 6, 10, 12)
    b.clear(-33, -31, 1, 6, 31, 37)
    return b


def cartenon_temple() -> Blueprint:
    spec = MapSpec(
        "cartenon_temple", "Cartenon Temple",
        (0, 2, -39),
        ((0, 1, -8), (-30, 1, 13), (0, 1, 10), (0, 1, 34), (39, 1, 40)),
        ((-34, 2, 5), (-18, 2, 15), (0, 2, 25), (34, 2, 35)),
    )
    b = Blueprint(spec)
    wall = "minecraft:stone_bricks"
    cracked = "minecraft:cracked_stone_bricks"
    floor = "minecraft:smooth_stone"
    roof = "minecraft:stone"

    b.room(-10, 10, -43, -34, 10, wall, floor, roof, "minecraft:chiseled_stone_bricks")
    b.room(-21, 21, -33, -20, 13, wall, floor, roof, "minecraft:chiseled_stone_bricks")
    b.room(-39, 39, -19, 5, 15, wall, floor, roof, cracked)
    b.room(-43, -19, 6, 23, 12, wall, "minecraft:polished_andesite", roof, "minecraft:mossy_stone_bricks")
    b.room(-17, 17, 7, 24, 14, wall, floor, roof, "minecraft:chiseled_stone_bricks")
    b.room(-34, 34, 25, 43, 16, wall, "minecraft:smooth_stone", roof, "minecraft:chiseled_stone_bricks")
    b.room(35, 43, 29, 42, 10, "minecraft:chiseled_stone_bricks",
           "minecraft:smooth_quartz", "minecraft:smooth_stone", "minecraft:gold_block")

    b.corridor_z(-5, 5, -35, -31, 8, wall, floor, roof)
    b.corridor_z(-6, 6, -22, -17, 9, wall, floor, roof)
    b.corridor_x(-41, -37, 9, 15, 8, wall, "minecraft:polished_andesite", roof)
    b.corridor_x(-20, -15, 12, 18, 8, wall, floor, roof)
    b.corridor_z(-5, 5, 22, 27, 10, wall, floor, roof)
    b.corridor_x(32, 37, 32, 38, 8, "minecraft:chiseled_stone_bricks",
                 "minecraft:smooth_quartz", "minecraft:smooth_stone")

    # Descending entry and tablet-lined commandment hall.
    for z, width in ((-41, 4), (-39, 5), (-37, 6), (-35, 7)):
        b.box(-width, width, 1, 1, z, z, "minecraft:stone_brick_stairs[facing=north]")
    for x in (-8, 8):
        b.pillar(x, -38, 1, 8, "minecraft:chiseled_stone_bricks")
        b.set(x, 9, -38, "minecraft:soul_lantern[hanging=true]")
    for x in (-17, -10, 10, 17):
        b.box(x, x + 3, 3, 9, -33, -33, "minecraft:chiseled_stone_bricks")
        b.box(x + 1, x + 2, 5, 7, -32, -32, "minecraft:glowstone")
    b.box(-13, 13, 0, 0, -27, -23, "minecraft:chiseled_stone_bricks")
    b.box(-11, 11, 1, 1, -26, -24, "minecraft:smooth_quartz")
    for x in (-15, -5, 5, 15):
        b.chain_light(x, -27, 13, "minecraft:soul_lantern[hanging=true]")

    # Statue gallery: twelve towering idols, a central blue seal, and readable side aisles.
    for x in (-33, -26, -19, -12, 12, 19, 26, 33):
        temple_statue(b, x, -17, 1, facing_south=True)
        temple_statue(b, x, 3, 1, facing_south=False)
    b.box(-8, 8, 0, 0, -13, -1, "minecraft:smooth_quartz")
    for x in range(-7, 8):
        for z in range(-12, 0):
            if abs(x) + abs(z + 6) in (4, 7):
                b.set(x, 0, z, "minecraft:light_blue_stained_glass")
    b.set(0, 0, -6, "minecraft:sea_lantern")
    for x in (-35, -21, -7, 7, 21, 35):
        b.pillar(x, -4, 1, 11, "minecraft:smooth_stone")
        b.set(x, 12, -4, "minecraft:soul_lantern[hanging=true]")
    b.box(-37, 37, 13, 13, -19, -19, "minecraft:chiseled_stone_bricks")
    b.box(-37, 37, 13, 13, 5, 5, "minecraft:chiseled_stone_bricks")

    # Western trial chamber: twelve protected braziers and an altar maze.
    for x in (-39, -33, -27, -21):
        for z in (9, 15, 21):
            b.box(x - 1, x + 1, 1, 1, z - 1, z + 1, "minecraft:chiseled_stone_bricks")
            b.set(x, 2, z, "minecraft:soul_sand")
            b.set(x, 3, z, "minecraft:soul_fire")
            b.box(x - 1, x + 1, 2, 3, z - 1, z - 1, "minecraft:iron_bars")
    b.box(-36, -24, 1, 2, 17, 20, "minecraft:mossy_stone_bricks")
    b.clear(-32, -28, 1, 3, 17, 20)
    for z in (9, 21):
        b.chain_light(-30, z, 12, "minecraft:soul_lantern[hanging=true]")

    # Prayer hall with twin judgment platforms and an illuminated central altar.
    for x in (-14, -7, 7, 14):
        b.pillar(x, 10, 1, 10, "minecraft:chiseled_stone_bricks")
        b.pillar(x, 22, 1, 10, "minecraft:chiseled_stone_bricks")
        b.set(x, 11, 10, "minecraft:soul_lantern[hanging=true]")
        b.set(x, 11, 22, "minecraft:soul_lantern[hanging=true]")
    b.box(-12, -4, 1, 2, 19, 23, "minecraft:smooth_quartz")
    b.box(4, 12, 1, 2, 19, 23, "minecraft:smooth_quartz")
    b.box(-3, 3, 1, 1, 12, 20, "minecraft:chiseled_stone_bricks")
    b.box(-2, 2, 2, 2, 15, 18, "minecraft:sea_lantern")
    b.box(-1, 1, 3, 4, 17, 18, "minecraft:light_blue_stained_glass")

    # Grand chamber and colossal seated Architect idol.
    for x in (-29, -19, -9, 9, 19, 29):
        b.pillar(x, 28, 1, 12, "minecraft:smooth_stone")
        b.pillar(x, 40, 1, 12, "minecraft:smooth_stone")
        b.set(x, 13, 28, "minecraft:soul_lantern[hanging=true]")
        b.set(x, 13, 40, "minecraft:soul_lantern[hanging=true]")
    b.box(-10, 10, 1, 2, 38, 42, "minecraft:chiseled_stone_bricks")
    b.box(-8, 8, 3, 10, 40, 42, "minecraft:smooth_stone")
    b.box(-6, -2, 3, 7, 36, 40, "minecraft:smooth_stone")
    b.box(2, 6, 3, 7, 36, 40, "minecraft:smooth_stone")
    b.box(-7, 7, 8, 12, 39, 41, "minecraft:smooth_stone")
    b.box(-5, 5, 12, 15, 39, 41, "minecraft:chiseled_stone_bricks")
    b.box(-3, 3, 13, 14, 38, 38, "minecraft:smooth_quartz")
    b.set(-2, 14, 37, "minecraft:redstone_block")
    b.set(2, 14, 37, "minecraft:redstone_block")
    b.box(-16, 16, 0, 0, 30, 38, "minecraft:chiseled_stone_bricks")
    for x in range(-14, 15):
        if x % 4 == 0:
            b.set(x, 0, 34, "minecraft:sea_lantern")
    b.rubble(-31, 27)
    b.rubble(27, 39)

    # Secret quartz-and-gold reliquary.
    b.box(37, 41, 0, 0, 31, 40, "minecraft:smooth_quartz")
    b.box(36, 36, 2, 8, 31, 40, "minecraft:gold_block")
    b.box(42, 42, 2, 8, 31, 40, "minecraft:gold_block")
    for z in (32, 36, 40):
        b.chain_light(39, z, 10, "minecraft:soul_lantern[hanging=true]")
    b.box(37, 41, 1, 2, 34, 37, "minecraft:gold_block")
    b.clear(-38, -30, 1, 6, 5, 6)
    b.clear(35, 37, 1, 6, 32, 38)
    return b


def temple_statue(b: Blueprint, x: int, z: int, base_y: int, facing_south: bool) -> None:
    inward = 1 if facing_south else -1
    b.box(x - 1, x + 1, base_y, base_y + 3, z, z, "minecraft:smooth_stone")
    b.box(x - 2, x + 2, base_y + 4, base_y + 7, z, z, "minecraft:stone_bricks")
    b.box(x - 3, x + 3, base_y + 5, base_y + 6, z, z, "minecraft:smooth_stone")
    b.box(x - 1, x + 1, base_y + 8, base_y + 10, z, z, "minecraft:chiseled_stone_bricks")
    b.set(x, base_y + 9, z + inward, "minecraft:redstone_lamp[lit=true]")
    b.box(x - 2, x + 2, base_y, base_y, z - 1, z + 1, "minecraft:polished_andesite")


def _base_name(state: str) -> str:
    return state.split("[", 1)[0]


def validate(blueprint: Blueprint) -> dict[str, object]:
    def state(x: int, y: int, z: int) -> str:
        return blueprint.blocks.get((x, y, z), "minecraft:deepslate")

    def is_air(x: int, y: int, z: int) -> bool:
        return _base_name(state(x, y, z)) in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

    def walkable(x: int, y: int, z: int) -> bool:
        return not is_air(x, y - 1, z) and is_air(x, y, z) and is_air(x, y + 1, z)

    start = blueprint.spec.entry
    if not walkable(*start):
        raise ValueError(f"{blueprint.spec.template_id}: entry is not walkable: {start}")
    reached = {start}
    queue = deque([start])
    while queue:
        x, y, z = queue.popleft()
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if not (MIN_X <= nx <= MAX_X and MIN_Z <= nz <= MAX_Z):
                continue
            for ny in (y, y + 1, y - 1):
                nxt = (nx, ny, nz)
                if nxt not in reached and MIN_Y + 1 <= ny <= MAX_Y - 1 and walkable(*nxt):
                    reached.add(nxt)
                    queue.append(nxt)
                    break

    missing = [marker for marker in blueprint.spec.objective_centers if marker not in reached]
    missing.extend(marker for marker in blueprint.spec.checkpoint_centers
                   if not any(rx == marker[0] and rz == marker[2] for rx, _ry, rz in reached))
    if missing:
        raise ValueError(f"{blueprint.spec.template_id}: unreachable authored markers: {missing}")
    leaks = [(x, z) for x, _y, z in reached if abs(x) >= MAX_X - 1 or abs(z) >= MAX_Z - 1]
    if leaks:
        raise ValueError(f"{blueprint.spec.template_id}: walkable route leaks into outer shell: {leaks[:8]}")
    palette = sorted(set(blueprint.blocks.values()))
    explicit_air = sum(1 for value in blueprint.blocks.values() if value == AIR)
    return {
        "id": blueprint.spec.template_id,
        "display_name": blueprint.spec.display_name,
        "size": list(SIZE),
        "explicit_blocks": len(blueprint.blocks),
        "carved_air_blocks": explicit_air,
        "detail_blocks": len(blueprint.blocks) - explicit_air,
        "palette_states": len(palette),
        "walkable_floor_cells": len(reached),
        "objectives": len(blueprint.spec.objective_centers),
        "checkpoints": len(blueprint.spec.checkpoint_centers),
    }


def _u16(value: int) -> bytes:
    return struct.pack(">H", value)


def _i32(value: int) -> bytes:
    return struct.pack(">i", value)


def _name(name: str) -> bytes:
    encoded = name.encode("utf-8")
    return _u16(len(encoded)) + encoded


def _named_int(name: str, value: int) -> bytes:
    return b"\x03" + _name(name) + _i32(value)


def _string_payload(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return _u16(len(encoded)) + encoded


def _named_string(name: str, value: str) -> bytes:
    return b"\x08" + _name(name) + _string_payload(value)


def _list_payload(element_type: int, values: Iterable[bytes]) -> bytes:
    materialized = list(values)
    return bytes((element_type,)) + _i32(len(materialized)) + b"".join(materialized)


def _named_list(name: str, element_type: int, values: Iterable[bytes]) -> bytes:
    return b"\x09" + _name(name) + _list_payload(element_type, values)


def _compound_payload(entries: Iterable[bytes]) -> bytes:
    return b"".join(entries) + b"\x00"


def _named_compound(name: str, entries: Iterable[bytes]) -> bytes:
    return b"\x0a" + _name(name) + _compound_payload(entries)


def parse_state(state: str) -> tuple[str, dict[str, str]]:
    if "[" not in state:
        return state, {}
    name, raw = state[:-1].split("[", 1)
    properties = {}
    for entry in raw.split(","):
        key, value = entry.split("=", 1)
        properties[key] = value
    return name, properties


def write_structure(blueprint: Blueprint, output: Path) -> None:
    palette = sorted(set(blueprint.blocks.values()))
    state_ids = {state: index for index, state in enumerate(palette)}
    palette_entries = []
    for state in palette:
        name, properties = parse_state(state)
        entries = [_named_string("Name", name)]
        if properties:
            entries.append(_named_compound("Properties", (
                _named_string(key, value) for key, value in sorted(properties.items())
            )))
        palette_entries.append(_compound_payload(entries))

    block_entries = []
    for (x, y, z), state in sorted(blueprint.blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        normalized = (x - MIN_X, y - MIN_Y, z - MIN_Z)
        block_entries.append(_compound_payload((
            _named_list("pos", 3, (_i32(value) for value in normalized)),
            _named_int("state", state_ids[state]),
        )))

    root = b"\x0a" + _name("") + _compound_payload((
        _named_list("size", 3, (_i32(value) for value in SIZE)),
        _named_list("entities", 10, ()),
        _named_list("blocks", 10, block_entries),
        _named_list("palette", 10, palette_entries),
        _named_int("DataVersion", DATA_VERSION),
    ))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(gzip.compress(root, compresslevel=9, mtime=0))


def write_preview(blueprint: Blueprint, path: Path) -> None:
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        return
    scale = 5
    image = Image.new("RGB", (SIZE[0] * scale, SIZE[2] * scale), (20, 22, 26))
    draw = ImageDraw.Draw(image)
    colors = {
        "minecraft:air": (58, 61, 68),
        "minecraft:polished_deepslate": (70, 70, 78),
        "minecraft:smooth_stone": (145, 145, 145),
        "minecraft:polished_blackstone": (45, 39, 53),
        "minecraft:polished_andesite": (115, 115, 117),
        "minecraft:tuff": (92, 100, 92),
        "minecraft:coarse_dirt": (112, 82, 55),
        "minecraft:obsidian": (36, 24, 53),
        "minecraft:smooth_quartz": (226, 221, 207),
        "minecraft:gilded_blackstone": (126, 92, 38),
        "minecraft:gold_block": (230, 184, 35),
        "minecraft:red_nether_bricks": (91, 25, 27),
        "minecraft:magma_block": (150, 58, 30),
    }
    for z in range(MIN_Z, MAX_Z + 1):
        for x in range(MIN_X, MAX_X + 1):
            state = _base_name(blueprint.blocks.get((x, 0, z), "minecraft:deepslate"))
            above = _base_name(blueprint.blocks.get((x, 1, z), "minecraft:deepslate"))
            if above == "minecraft:air":
                color = colors.get(state, (105, 105, 108))
            else:
                color = colors.get(above, (36, 38, 43))
            px, pz = (x - MIN_X) * scale, (z - MIN_Z) * scale
            draw.rectangle((px, pz, px + scale - 1, pz + scale - 1), fill=color)
    marker_colors = [(40, 210, 255), (255, 203, 57), (255, 91, 91), (185, 85, 255), (62, 232, 140)]
    ex, _ey, ez = blueprint.spec.entry
    draw.ellipse(((ex - MIN_X) * scale - 5, (ez - MIN_Z) * scale - 5,
                  (ex - MIN_X) * scale + 5, (ez - MIN_Z) * scale + 5), fill=(255, 255, 255))
    for index, (x, _y, z) in enumerate(blueprint.spec.objective_centers):
        color = marker_colors[index % len(marker_colors)]
        draw.ellipse(((x - MIN_X) * scale - 5, (z - MIN_Z) * scale - 5,
                      (x - MIN_X) * scale + 5, (z - MIN_Z) * scale + 5), fill=color)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path,
                        default=Path("src/main/resources/data/sololeveling/structures/dungeons"))
    parser.add_argument("--preview-dir", type=Path)
    parser.add_argument("--manifest", type=Path)
    args = parser.parse_args()

    blueprints = (subway(), red_orc_outpost(), demon_castle(), cartenon_temple())
    metrics = []
    for blueprint in blueprints:
        metrics.append(validate(blueprint))
        write_structure(blueprint, args.output / f"{blueprint.spec.template_id}.nbt")
        if args.preview_dir:
            write_preview(blueprint, args.preview_dir / f"{blueprint.spec.template_id}.png")

    if args.manifest:
        args.manifest.parent.mkdir(parents=True, exist_ok=True)
        args.manifest.write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__":
    main()
