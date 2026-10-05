#!/usr/bin/env python3
"""Regenerate assets/icon.png, assets/icon.ico and assets/icon.icns.

The source is the 16x16 pixel-art sprite src/pacmanRight.png, upscaled with
nearest-neighbour scaling so the retro look is preserved. Requires ImageMagick
(https://imagemagick.org) on the PATH.
"""

import struct
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "src" / "pacmanRight.png"
ASSETS = ROOT / "assets"

# Modern macOS icon types that carry PNG data (type code, pixel size).
ICNS_TYPES = [
    (b"icp4", 16),
    (b"icp5", 32),
    (b"icp6", 64),
    (b"ic07", 128),
    (b"ic08", 256),
    (b"ic09", 512),
    (b"ic10", 1024),
]


def resize_png(size: int) -> bytes:
    result = subprocess.run(
        ["magick", str(SOURCE), "-filter", "point", "-resize", f"{size}x{size}", "png:-"],
        check=True,
        capture_output=True,
    )
    return result.stdout


def main() -> None:
    if not SOURCE.exists():
        sys.exit(f"source icon not found: {SOURCE}")

    ASSETS.mkdir(exist_ok=True)

    # Linux launcher icon.
    (ASSETS / "icon.png").write_bytes(resize_png(512))

    # Multi-resolution Windows icon.
    subprocess.run(
        [
            "magick", str(SOURCE),
            "-filter", "point", "-resize", "256x256",
            "-define", "icon:auto-resize=256,128,64,48,32,16",
            str(ASSETS / "icon.ico"),
        ],
        check=True,
    )

    # macOS .icns container (big-endian: magic, total size, then type/size/PNG entries).
    entries = []
    for type_code, size in ICNS_TYPES:
        payload = resize_png(size)
        entries.append(struct.pack(">4sI", type_code, len(payload) + 8) + payload)
    body = b"".join(entries)
    (ASSETS / "icon.icns").write_bytes(b"icns" + struct.pack(">I", len(body) + 8) + body)

    print("Wrote assets/icon.png, assets/icon.ico and assets/icon.icns")


if __name__ == "__main__":
    main()
