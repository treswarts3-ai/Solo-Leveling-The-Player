#!/usr/bin/env python3
"""Apply the reviewed Solo Leveling System UI overhaul deterministically."""
from __future__ import annotations

import base64
import io
import lzma
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "tools" / "ui_patch"


def main() -> None:
    encoded = "".join(path.read_text(encoding="utf-8").strip() for path in sorted(PARTS.glob("part*")))
    if not encoded:
        raise SystemExit("No UI payload parts found")
    archive_bytes = lzma.decompress(base64.b64decode(encoded))
    with tarfile.open(fileobj=io.BytesIO(archive_bytes), mode="r:") as archive:
        members = archive.getmembers()
        root = ROOT.resolve()
        for member in members:
            destination = (ROOT / member.name).resolve()
            if destination != root and root not in destination.parents:
                raise SystemExit(f"Unsafe archive path: {member.name}")
        archive.extractall(ROOT, members=members, filter="data")
    print(f"Applied {len(members)} UI overhaul files")


if __name__ == "__main__":
    main()
