#!/usr/bin/env python3
"""
Draw the 16x16 inventory icon for the dissection table.

The dissection table is rendered in the world by a GeckoLib block-entity
renderer, so it has no vanilla block model the item could inherit.  This script
draws a dedicated item sprite in the same pixel-art grammar as the other
Aquanaut items: a 1px dark outline, a lit top surface, a shaded front face and a
small accent colour (the basin fluid / scalpel blade).

The palette is sampled from the table's own block atlas so the icon and the
in-world model agree.

Usage
-----
    python3 scripts/GenerateDissectionTableItemIcon.py [--preview]
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut"
TABLE_TEXTURE = ASSETS / "textures" / "block" / "dissection_table.png"
ICON_PATH = ASSETS / "textures" / "item" / "dissection_table.png"

SIZE = 16

# 16x16 design.  The two front posts sit under the bench, the faucet riser and
# its spout hang over the basin on the right rear corner, and the pale pixels on
# the left rim are the scalpel resting across the bench.
ART: tuple[str, ...] = (
    "................",
    "................",
    "..........OO....",
    "..........OSO...",
    "..........OSO...",
    "......OOOOOSO...",
    ".OWSSSSSSSSSO...",
    ".OSSMMMMMMSSO...",
    ".OSMBBBBBBMSO...",
    ".OSMBBBBBBMSO...",
    ".OSSMMMMMMSSO...",
    ".OOSSSSSSSSOO...",
    "..OMOOOOOOMO....",
    "..OMO....OMO....",
    "..OMO....OMO....",
    "..OOO....OOO....",
)

LEGEND = {
    "O": "outline",
    "W": "highlight",
    "S": "steel_light",
    "M": "steel_mid",
    "B": "basin",
}


def load_palette() -> dict[str, tuple[int, int, int, int]]:
    if not TABLE_TEXTURE.exists():
        raise SystemExit(f"missing table texture: {TABLE_TEXTURE} (export the models first: scripts/export_blockbench_models.py)")
    image = Image.open(TABLE_TEXTURE).convert("RGBA")
    pixels = [pixel for pixel in image.getdata() if pixel[3] > 128]
    if not pixels:
        raise SystemExit("table texture has no opaque pixels")

    by_luma = sorted(pixels, key=lambda pixel: 0.2126 * pixel[0] + 0.7152 * pixel[1] + 0.0722 * pixel[2])
    darkest = by_luma[len(by_luma) // 20]
    mid = by_luma[len(by_luma) // 2]
    light = by_luma[int(len(by_luma) * 0.85)]
    brightest = by_luma[-1]

    def as_rgb(pixel) -> tuple[int, int, int]:
        return (pixel[0], pixel[1], pixel[2])

    palette = {
        "outline": (*darken(as_rgb(darkest), 0.55), 255),
        "steel_mid": (*as_rgb(mid), 255),
        "steel_light": (*as_rgb(light), 255),
        "highlight": (*brighten(as_rgb(brightest), 0.35), 255),
        # Basin fluid: a desaturated blood red so the icon reads as a wet table.
        "basin": (122, 40, 40, 255),
    }
    return palette


def darken(rgb: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(max(0, int(round(channel * (1.0 - amount)))) for channel in rgb)


def brighten(rgb: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(min(255, int(round(channel + (255 - channel) * amount))) for channel in rgb)


def build_icon(palette: dict[str, tuple[int, int, int, int]]) -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()
    for y, row in enumerate(ART):
        for x, symbol in enumerate(row):
            if symbol == ".":
                continue
            colour = palette.get(LEGEND[symbol])
            if colour is None:
                raise SystemExit(f"unknown icon symbol {symbol!r}")
            pixels[x, y] = colour

    # Simple top-lit shading inside the icon: darken the lower half of each
    # symbol run so the sprite is not flat, matching the other item icons.
    occupied = [(x, y) for y in range(SIZE) for x in range(SIZE) if pixels[x, y][3] > 0]
    if occupied:
        lowest = max(y for _, y in occupied)
        highest = min(y for _, y in occupied)
        span = max(1, lowest - highest)
        for x, y in occupied:
            if y <= highest + span // 2:
                continue
            r, g, b, a = pixels[x, y]
            factor = 0.86
            pixels[x, y] = (int(r * factor), int(g * factor), int(b * factor), a)
    return image


def ascii_preview(image: Image.Image) -> str:
    ramp = " .:-=+*#%@"
    pixels = image.load()
    lines = []
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            r, g, b, a = pixels[x, y]
            if a < 32:
                row += "."
                continue
            value = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            row += ramp[min(len(ramp) - 1, int(value * len(ramp)))]
        lines.append(row)
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--preview", action="store_true", help="print an ASCII preview")
    args = parser.parse_args(argv)

    palette = load_palette()
    icon = build_icon(palette)
    ICON_PATH.parent.mkdir(parents=True, exist_ok=True)
    icon.save(ICON_PATH)

    opaque = sum(1 for pixel in icon.getdata() if pixel[3] > 0)
    print(f"wrote {ICON_PATH.relative_to(REPO_ROOT)} ({opaque} opaque pixels)")
    if args.preview:
        print(ascii_preview(icon))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
