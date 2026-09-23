#!/usr/bin/env python3
"""
Draw the 16x16 inventory icon for the biological detector.

The detector is a GeckoLib entity, so the item has no vanilla model to inherit:
this script draws the sprite the item renders through, in the same pixel-art
grammar as the other Aquanaut items (a one pixel dark outline, a lit top-left,
a shaded lower-right) and with the detector's own palette, so the icon matches
the ball it deploys:

* a stainless sphere, lit from the top-left;
* the bio-green collar that wraps its equator, with the phased-array cells
  glinting inside it and a faint green halo bleeding past the outline;
* the service deck on the north pole.

Usage
-----
    python3 scripts/GenerateBiologicalDetectorItemIcon.py [--preview]
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ICON_PATH = (REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut"
             / "textures" / "item" / "biological_detector.png")

SIZE = 16
CENTRE = 7.5
OUTLINE_INNER = 5.55
OUTLINE_OUTER = 6.35

# The detector's own palette: the steel hull, the bio-green collar and the
# phased-array cells, straight from the model's atlas.
OUTLINE = (29, 33, 38, 255)
STEEL = [
    (250, 252, 254, 255),
    (230, 236, 242, 255),
    (192, 200, 209, 255),
    (154, 163, 173, 255),
    (118, 127, 137, 255),
    (107, 116, 126, 255),
]
BIO_BRIGHT = (200, 246, 222, 255)
BIO = (127, 217, 172, 255)
BIO_DEEP = (78, 156, 124, 255)
BIO_SHADOW = (46, 95, 76, 255)
CELL_BRIGHT = (196, 242, 251, 255)
CELL = (124, 195, 212, 255)
CELL_DEEP = (86, 146, 162, 255)
HALO = (127, 217, 172, 110)


def ball_mask() -> set[tuple[int, int]]:
    """Every pixel whose centre is inside the sphere, outline included."""
    pixels = set()
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - CENTRE
            dy = y - CENTRE
            if (dx * dx + dy * dy) ** 0.5 <= OUTLINE_OUTER:
                pixels.add((x, y))
    return pixels


def metal(dx: float, dy: float) -> tuple[int, int, int, int]:
    """The sphere's stainless shading: lit from the top-left, rim fading down."""
    light = -(dx + dy) / (2.0 * OUTLINE_INNER)
    if light > 0.72:
        return STEEL[0]
    if light > 0.4:
        return STEEL[1]
    if light > 0.1:
        return STEEL[2]
    if light > -0.2:
        return STEEL[3]
    if light > -0.55:
        return STEEL[4]
    return STEEL[5]


def draw() -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()
    inside = ball_mask()

    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - CENTRE
            dy = y - CENTRE
            radius = (dx * dx + dy * dy) ** 0.5
            if (x, y) not in inside:
                # A soft bio-green halo either side of the collar, just past
                # the silhouette, so the collar reads through the outline.
                if 6.35 < radius <= 7.25 and abs(dy) <= 1.5 and abs(dx) > 2.0:
                    pixels[x, y] = HALO
                continue
            if radius > OUTLINE_INNER:
                pixels[x, y] = OUTLINE
                continue
            colour = metal(dx, dy)
            if radius > 4.9 and dx + dy > -1.0:
                # The rim turns away from the light, but only on the far side:
                # the lit shoulder keeps its steel.
                colour = STEEL[4]
            if dy <= -4.3:
                # The pole deck: a flat cap, set off from the hull by a seam.
                if abs(dx) <= 1.5:
                    colour = STEEL[1]
                elif abs(dx) <= 3.0:
                    colour = STEEL[3]
            if dy > -4.3 and dy < -2.7 and abs(dx) <= 2.5:
                colour = STEEL[3]
            pixels[x, y] = colour

    # The collar: two rows of bio-green across the equator, lit on top.
    for x in range(SIZE):
        for y in (7, 8):
            if (x, y) not in inside or pixels[x, y] == OUTLINE:
                continue
            dx = x - CENTRE
            if y == 7:
                colour = BIO_BRIGHT if dx < 0 else BIO
            else:
                colour = BIO if dx < -1 else BIO_DEEP
            if abs(dx) > 5.0:
                colour = BIO_SHADOW
            pixels[x, y] = colour

    # A dark seam under the collar, so it sits on the hull rather than in it.
    for x in range(SIZE):
        if (x, 9) in inside and pixels[x, 9] != OUTLINE and abs(x - CENTRE) < 5.2:
            pixels[x, 9] = BIO_SHADOW

    # The phased-array cells glinting along the collar.
    for x, y, colour in ((4, 7, CELL_BRIGHT), (6, 8, CELL),
                         (9, 7, CELL), (11, 8, CELL_DEEP),
                         (3, 8, CELL_DEEP)):
        if pixels[x, y] not in (OUTLINE, (0, 0, 0, 0)):
            pixels[x, y] = colour

    # A specular highlight, and the deck's dark rim.
    for x, y in ((5, 3), (6, 2), (4, 4)):
        if pixels[x, y] != OUTLINE:
            pixels[x, y] = STEEL[0]
    return image


def preview(image: Image.Image) -> None:
    glyphs = {
        (0, 0, 0, 0): "..",
        OUTLINE: "##",
        STEEL[0]: "@@",
        STEEL[1]: "++",
        STEEL[2]: "**",
        STEEL[3]: "==",
        STEEL[4]: "--",
        STEEL[5]: "::",
        BIO_BRIGHT: "EE",
        BIO: "ee",
        BIO_DEEP: "vv",
        BIO_SHADOW: "vv",
        CELL_BRIGHT: "LL",
        CELL: "ll",
        CELL_DEEP: "jj",
        HALO: "''",
    }
    for y in range(SIZE):
        print("".join(glyphs.get(image.getpixel((x, y)), "??") for x in range(SIZE)))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", action="store_true", help="print the sprite as text")
    args = parser.parse_args()

    image = draw()
    if args.preview:
        preview(image)
    ICON_PATH.parent.mkdir(parents=True, exist_ok=True)
    image.save(ICON_PATH)
    print(f"wrote {ICON_PATH.relative_to(REPO_ROOT)} ({image.width}x{image.height})")


if __name__ == "__main__":
    main()
