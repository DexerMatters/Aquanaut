#!/usr/bin/env python3
"""Draw the handheld searchlight's two inventory sprites.

The lamp is drawn as a piece of *equipment*, not as an ornament: a white
enamelled barrel with a clear glass lens and plain iron fittings, the way a
laboratory or field instrument is painted. That is also what makes it read at
sixteen pixels -- the eye gets one big white shape with a dark lens ring at the
front, a grey cap at the back and a dark grip underneath, and nothing else
competes with it.

The two sprites come out of one description, so the dark lamp and the burning
lamp can never drift apart: the burning one is the same drawing with the lens,
the light spilling onto the inside of the bezel, the little indicator and a pale
rim on the barrel lit. The silhouette never moves.

The lamp is shown in side elevation, muzzle to the left and grip down, because
that is the view in which a handheld lamp is recognisable; a front elevation
would be a circle on a stick.

Shading is analytical rather than sampled: the barrel and the head are round, so
their form is the cosine of the position across the barrel, the flat parts get a
gentle gradient, everything is banded to six tones, and the whole sprite is
finished with the house silhouette darkening and a one pixel contact shadow.

Usage
-----
    python3 scripts/GenerateHandheldSearchlightAssets.py            # write sprites
    python3 scripts/GenerateHandheldSearchlightAssets.py --preview  # + zoomed previews
    python3 scripts/GenerateHandheldSearchlightAssets.py --dump     # + ASCII sprite
"""

from __future__ import annotations

import argparse
import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ITEM_DIR = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut" / "textures" / "item"
OFF_PATH = ITEM_DIR / "handheld_searchlight.png"
ON_PATH = ITEM_DIR / "handheld_searchlight_on.png"
PREVIEW_DIR = REPO_ROOT / "build" / "preview"

SIZE = 16
TONES = 6

# ---------------------------------------------------------------------------
# palette: white enamel, plain iron, clear glass
# ---------------------------------------------------------------------------

ENAMEL = ((150, 156, 160), (196, 201, 205), (226, 231, 233), (242, 246, 247), (252, 254, 254))
IRON = ((50, 56, 62), (80, 88, 95), (116, 124, 132), (154, 162, 170), (190, 198, 206))
DARK = ((30, 34, 40), (48, 54, 61), (70, 77, 85), (96, 104, 112), (122, 130, 138))
GLASS_OFF = ((64, 80, 96), (98, 116, 132), (134, 154, 170), (172, 192, 206), (206, 224, 236))
GLASS_ON = ((222, 240, 255), (244, 251, 255), (255, 255, 255), (238, 249, 255), (206, 234, 255))

INDICATOR_OFF = (92, 44, 44)
INDICATOR_ON = (255, 146, 104)

#: multiplier applied to the outermost texel of the silhouette
SILHOUETTE = 0.62


def ramp(stops, t: float):
    t = min(1.0, max(0.0, t))
    span = len(stops) - 1
    scaled = t * span
    low = min(span - 1, int(scaled))
    k = scaled - low
    a, b = stops[low], stops[low + 1]
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3))


def band(t: float) -> float:
    """Six flat steps: pixel-art bands, not a gradient."""
    t = min(1.0, max(0.0, t))
    return round(t * (TONES - 1)) / (TONES - 1)


def scale(rgb, factor: float):
    return tuple(min(255, max(0, int(round(channel * factor)))) for channel in rgb)


def mix(a, b, k: float):
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3))


# ---------------------------------------------------------------------------
# the lamp
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Part:
    """One painted region: horizontal spans (y, x0, x1) inclusive, and how it shades."""

    name: str
    material: str
    spans: tuple
    form: str = "flat"


# Left to right: glass, bezel, head, body, cap, lug, and the grip below.  Later parts paint over
# earlier ones, so the details sit on top of the shells they are bolted to.
PARTS = (
    # The head is a bigger barrel than the body and is rounded at both ends.
    Part("head", "enamel", ((2, 4, 6), (3, 3, 6), (4, 3, 6), (5, 3, 6), (6, 3, 6), (7, 3, 6), (8, 3, 6),
                            (9, 4, 6)), "barrel"),
    Part("bezel", "iron", ((3, 2, 3), (4, 2, 3), (5, 2, 3), (6, 2, 3), (7, 2, 3), (8, 2, 3)), "barrel"),
    Part("lens", "glass", ((3, 1, 1), (4, 1, 1), (5, 1, 1), (6, 1, 1), (7, 1, 1), (8, 1, 1)), "barrel"),
    Part("body", "enamel", ((4, 7, 11), (5, 7, 11), (6, 7, 11), (7, 7, 11), (8, 7, 11)), "barrel"),
    Part("cap", "iron", ((4, 12, 13), (5, 12, 13), (6, 12, 13), (7, 12, 13), (8, 12, 13)), "barrel"),
    Part("lug", "iron", ((5, 14, 14), (6, 14, 14), (7, 14, 14)), "flat"),
    # A low rail on top, one small data plate on the barrel and a lamp indicator on the cap: the
    # industrial details that make it read as equipment rather than as a toy.
    Part("rail", "iron", ((3, 7, 10),), "flat"),
    Part("plate", "dark", ((5, 9, 10),), "flat"),
    Part("indicator", "indicator", ((5, 12, 12),), "flat"),
    # Grip and trigger hang under the body: a solid, slightly raked grip, ribbed so it reads as
    # something held rather than as a leg.
    Part("grip", "dark", ((9, 9, 11), (10, 9, 11), (11, 9, 11), (12, 9, 11), (13, 9, 11)), "barrel"),
    Part("grip_rib", "dark", ((10, 9, 11), (12, 9, 11)), "flat"),
    Part("trigger", "iron", ((8, 8, 8), (9, 8, 8)), "flat"),
)

#: The glass is the one thing that must never be treated as a silhouette edge: darkening the
#: outermost texel would swallow the lamp's whole face, which is a single column wide.
BRIGHT_PARTS = frozenset({"lens", "indicator"})


def pixel_part(x: int, y: int):
    covering = None

    for part in PARTS:
        for (row, x0, x1) in part.spans:
            if row == y and x0 <= x <= x1:
                covering = part

    return covering


def shade(part: Part, x: int, y: int, lit: bool):
    rows = [row for (row, _, _) in part.spans]
    top, bottom = min(rows), max(rows)
    middle = (top + bottom) / 2.0
    half = max(0.5, (bottom - top) / 2.0)
    across = (y - middle) / half  # -1 at the top of the part, +1 at the bottom

    if part.form == "barrel":
        # The barrel is round and the key light sits above, so the brightest texel is a little above
        # the axis and the sides fall away as a cosine.
        form = math.cos(min(1.0, abs(across + 0.3)) * math.pi / 2.0)
        value = 0.34 + 0.66 * form
    else:
        value = 0.66 - 0.18 * across

    if part.name == "lens":
        return ramp(GLASS_ON if lit else GLASS_OFF, band(0.45 + 0.55 * value))

    if part.name == "indicator":
        return INDICATOR_ON if lit else INDICATOR_OFF

    if part.name == "grip_rib":
        return scale(ramp(DARK, band(value)), 1.35)

    stops = {"enamel": ENAMEL, "iron": IRON, "dark": DARK}[part.material]
    rgb = ramp(stops, band(value))

    # A seam where the parts meet reads as separate pieces of equipment.
    left_edge = min(x0 for (_, x0, _) in part.spans)
    if x == left_edge and part.name in ("body", "cap", "bezel"):
        rgb = scale(rgb, 0.74)

    if lit and part.name in ("bezel", "head") and x <= 3:
        rgb = mix(rgb, GLASS_ON[2], 0.32)

    return rgb


def draw(lit: bool) -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()

    for y in range(SIZE):
        for x in range(SIZE):
            part = pixel_part(x, y)

            if part is None:
                continue

            rgb = shade(part, x, y, lit)
            pixels[x, y] = (*rgb, 255)

    painted = {(x, y) for y in range(SIZE) for x in range(SIZE) if pixels[x, y][3] > 0}

    # House silhouette darkening: the outer ring keeps the material colour but a good deal darker,
    # so the sprite holds together on any slot background.  There is deliberately no drop shadow:
    # the slot already supplies the backdrop, and a translucent halo reads as dirt at this size.
    for x, y in sorted(painted):
        if pixel_part(x, y).name in BRIGHT_PARTS:
            continue

        if any(neighbour not in painted for neighbour in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))):
            pixels[x, y] = (*scale(pixels[x, y][:3], SILHOUETTE), 255)

    return image


# ---------------------------------------------------------------------------


def dump(image: Image.Image) -> None:
    """ASCII view, for checking the silhouette without opening the file."""
    glyphs = " .:-=+*#%@"

    for y in range(image.height):
        row = ""

        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            row += " " if a == 0 else glyphs[min(len(glyphs) - 1, (r + g + b) // (3 * 26))]

        print(row)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--preview", action="store_true", help="also write zoomed previews into build/preview")
    parser.add_argument("--dump", action="store_true", help="also print an ASCII view of both sprites")
    args = parser.parse_args(argv)

    ITEM_DIR.mkdir(parents=True, exist_ok=True)
    off = draw(lit=False)
    on = draw(lit=True)
    off.save(OFF_PATH)
    on.save(ON_PATH)

    print(f"wrote {OFF_PATH.relative_to(REPO_ROOT)}")
    print(f"wrote {ON_PATH.relative_to(REPO_ROOT)}")

    if args.preview:
        PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
        board = Image.new("RGBA", (SIZE * 2 + 3, SIZE), (52, 52, 60, 255))

        for index, (image, name) in enumerate(((off, "off"), (on, "on"))):
            image.resize((SIZE * 16, SIZE * 16), Image.NEAREST).save(
                PREVIEW_DIR / f"handheld_searchlight_icon_{name}.png")
            board.alpha_composite(image, (index * (SIZE + 3), 0))

        board.resize((board.width * 12, board.height * 12), Image.NEAREST).save(
            PREVIEW_DIR / "handheld_searchlight_icons.png")
        print(f"previews in {PREVIEW_DIR.relative_to(REPO_ROOT)}")

    if args.dump:
        print("\noff:")
        dump(off)
        print("\non:")
        dump(on)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
