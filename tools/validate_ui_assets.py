#!/usr/bin/env python3
from pathlib import Path
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
PNG = ROOT / "src/main/resources/assets/sololeveling/textures/gui/system_icons.png"


def fail(message: str) -> None:
    raise SystemExit(f"UI asset validation failed: {message}")


def main() -> None:
    if not PNG.is_file():
        fail(f"missing {PNG.relative_to(ROOT)}")
    data = PNG.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        fail("icon atlas is not a PNG")
    width, height = struct.unpack(">II", data[16:24])
    if (width, height) != (128, 128):
        fail(f"icon atlas must be 128x128, got {width}x{height}")
    if len(data) > 64 * 1024:
        fail("icon atlas is unexpectedly large")
    print("Validated System UI icon atlas: 128x128 PNG")


if __name__ == "__main__":
    main()
