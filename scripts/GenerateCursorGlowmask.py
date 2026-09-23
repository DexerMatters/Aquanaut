#!/usr/bin/env python3
"""Generate the cursor entity's glowmask.

The cursor's beacon is the little pod on top of the antenna — the topmost cube of
`geo/cursor.geo.json`, `cube_tip_pod`, which is box-UV mapped to the 8x5 block at
(0, 23) in the 64x32 entity sheet. The glowmask is the same sheet with everything
transparent except that block, warmed towards the beacon's lit colour, so a
GeckoLib `AutoGlowingGeoLayer` draws the tip at full brightness and nothing else.

Output:
  src/main/resources/assets/aquanaut/textures/entity/cursor_glowmask.png

Run from the repository root:
  python3 scripts/GenerateCursorGlowmask.py
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ENTITY_DIR = Path("src/main/resources/assets/aquanaut/textures/entity")
GEO = Path("src/main/resources/assets/aquanaut/geo/cursor.geo.json")
SOURCE = ENTITY_DIR / "cursor.png"
OUTPUT = ENTITY_DIR / "cursor_glowmask.png"

# The beacon's lit colour: the warm end of the amber ramp the tip is painted with.
LIT = (255, 236, 190)

# How far the sampled pixel is pushed towards LIT, 0..1.
WARMTH = 0.55


def topmost_cube_box_uv(geo_path: Path) -> tuple[int, int, int, int]:
    """Box-UV rectangle of the highest cube in the model, as (x, y, width, height)."""
    geometry = json.loads(geo_path.read_text(encoding="utf-8"))["minecraft:geometry"][0]

    best = None
    for bone in geometry["bones"]:
        for cube in bone.get("cubes", []):
            origin = cube["origin"]
            size = cube["size"]
            if best is None or origin[1] + size[1] > best[0]:
                best = (origin[1] + size[1], cube)

    cube = best[1]
    u, v = cube["uv"]
    width, height, depth = (int(dimension) for dimension in cube["size"])
    # Bedrock box UV: the block is (d + w + d + w) wide and (d + h) tall, starting at (u, v).
    return u, v, 2 * (width + depth), depth + height


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    x, y, width, height = topmost_cube_box_uv(GEO)

    mask = Image.new("RGBA", source.size, (0, 0, 0, 0))
    for row in range(y, y + height):
        for column in range(x, x + width):
            r, g, b, a = source.getpixel((column, row))
            if a == 0:
                continue
            mask.putpixel((column, row), (
                round(r + (LIT[0] - r) * WARMTH),
                round(g + (LIT[1] - g) * WARMTH),
                round(b + (LIT[2] - b) * WARMTH),
                255,
            ))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    mask.save(OUTPUT)

    lit = sum(1 for pixel in mask.convert("RGBA").load().values() if pixel[3] > 0) \
        if False else sum(
            1 for row in range(mask.size[1]) for column in range(mask.size[0])
            if mask.getpixel((column, row))[3] > 0)
    print(f"{OUTPUT.name}  {mask.size[0]}x{mask.size[1]}  "
          f"beacon block {width}x{height} at ({x}, {y})  lit pixels={lit}")


if __name__ == "__main__":
    main()
