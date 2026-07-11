#!/usr/bin/env python3
"""One-shot CI transport for the master dungeon rebuild; deletes itself after applying."""
from __future__ import annotations
import base64, io, lzma, tarfile
from pathlib import Path
root = Path(__file__).resolve().parents[1]
parts = sorted((root / "tools").glob(".master_payload_*.txt"))
payload = "".join(part.read_text(encoding="utf-8") for part in parts)
with tarfile.open(fileobj=io.BytesIO(lzma.decompress(base64.b64decode(payload))), mode="r:") as archive:
    archive.extractall(root)
for relative in (
    "src/main/resources/data/sololeveling/structures/dungeons/abandoned_subway.nbt",
    "src/main/resources/data/sololeveling/structures/dungeons/red_orc_outpost.nbt",
    "src/main/resources/data/sololeveling/structures/dungeons/demon_castle_foyer.nbt",
    "src/main/resources/data/sololeveling/structures/dungeons/cartenon_temple.nbt",
    "docs/DUNGEON_MAP_RESEARCH.md",
):
    (root / relative).unlink(missing_ok=True)
for part in parts:
    part.unlink(missing_ok=True)
Path(__file__).unlink(missing_ok=True)
