#!/usr/bin/env python3
"""Generate the submarine drone's inventory sprite.

The drone item deploys the ROV that ``geo/submarine_drone.geo.json`` renders, so
the icon has to read as that machine and not as another handheld gadget. The
controller sprite is a *tilted* body with a big backlit screen and a D-pad; this
one is deliberately the opposite: a flat, horizontal side profile, no display,
and a silhouette built from the parts the model actually has.

Everything here is drawn on the same materials as the entity atlas, sampled
straight out of ``textures/entity/submarine_drone.png`` so the icon and the model
cannot drift apart:

* **stainless shell** - the bluish steel ramp ``f1f5f9 -> dae1e8 -> c4cdd6 ->
  a6afb9 -> 767f89 -> 3f464e``, lit from the top-left.
* **sensor lens** - the drone's cyan glass ``c2f0f9 / 8fe3f2 / 7ac0d1``, sunk in
  a dark bezel like the model's front glass.
* **shroud band** - the amber trim ``f6b774 / f2b472 / e9a257`` that runs around
  the hull.
* **impeller** - the dark four-blade fan (``3e454d``) inside its shroud, on the
  pale blade disc ``dfe6ed``.
* **floodlight** - the lamp's ``feffff`` glass under the bow.
* **outline / panel gaps** - the atlas' deep shadow ``2a2f35``.

Layout, left to right: the shrouded impeller at the stern, the long hull with its
amber collar just behind the bow, the sensor pod carrying the cyan lens and the
floodlight, a keel-like skeg below the hull and a stubby beacon dome on the deck
(as on the model). Leaving the outline pass to darken the rim, the sprite fills
columns 1-14 and rows 3-13 of the canvas, so it sits on the dark inventory
background the way the pack's other items do.

Method: the silhouette and the round details are samples of an 8x supersampled
coverage mask, so curves are resolved on a 128x128 grid; the result is then
snapped to hard alpha at 16x16. That gives clean round shapes with no
semi-transparent pixel left inside the hull, which is what an item sprite needs.

Outputs:
  src/main/resources/assets/aquanaut/textures/item/submarine_drone.png

Run from the repository root:
  python3 scripts/GenerateSubmarineDroneItemSprite.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

TEXTURE_DIR = Path("src/main/resources/assets/aquanaut/textures/item")
OUTPUT = TEXTURE_DIR / "submarine_drone.png"

SIZE = 16

# Samples per axis per output pixel. The mask is evaluated at SIZE * SUPERSAMPLE,
# which is what keeps the round lens and the duct rim from stair-stepping.
SUPERSAMPLE = 8

# Coverage that turns a sample into a solid pixel. 0.5 is the natural midpoint;
# the point of using a mask at all is that this decision is made once, on a
# high-resolution shape, instead of per-pixel with jagged geometry.
COVERAGE_THRESHOLD = 0.5

# --- Palette, sampled from textures/entity/submarine_drone.png -----------------
OUTLINE = (42, 47, 53)  # 2a2f35 - panel gaps / the deepest steel
HULL_HILIGHT = (241, 245, 249)  # f1f5f9 - specular top-left edge
HULL_BRIGHT = (218, 225, 232)  # dae1e8
HULL_LIGHT = (196, 205, 214)  # c4cdd6
HULL_MID = (166, 175, 185)  # a6afb9
HULL_DARK = (118, 127, 137)  # 767f89
HULL_SHADOW = (63, 70, 78)  # 3f464e
LENS_LIT = (194, 240, 249)  # c2f0f9
LENS = (143, 227, 242)  # 8fe3f2
LENS_DIM = (122, 192, 209)  # 7ac0d1
LENS_BEZEL = (46, 52, 60)  # 2e343c - the dark ring the glass is set into
FAN_DISC = (223, 230, 237)  # dfe6ed - the pale impeller backplate
FAN_BLADE = (62, 69, 77)  # 3e454d - the fan's dark blades
AMBER_BRIGHT = (246, 183, 116)  # f6b774
AMBER = (242, 180, 114)  # f2b472
AMBER_DARK = (233, 162, 87)  # e9a257
FLOODLIGHT = (254, 255, 255)  # feffff - lamp glass under the bow

# --- Geometry, in canvas units with pixel centres at x + 0.5 -------------------
# The whole machine is laid out so its silhouette lands on columns 1-14 and rows
# 3-12: one pixel of margin on the sides and a little more at top and bottom, so
# the outline pass has room to work without clipping.
# Stern: the shroud ring around the impeller, and the fan inside it.
DUCT_CENTRE = (3.5, 7.7)
DUCT_RADIUS = 2.9
FAN_RADIUS = 2.0
FAN_HUB_RADIUS = 0.7
# Half-width of the fan's four blades, measured across the diagonal.
FAN_BLADE_HALF_WIDTH = 0.42
# Blades run along the two diagonals; a point is on one when its perpendicular
# distance to that diagonal is under the half-width.
FAN_DIAGONAL_SCALE = 2.0 ** 0.5

# Main hull: a long rounded slab, the core of the silhouette.
HULL_CENTRE = (7.4, 7.7)
HULL_HALF_LENGTH = 4.6
HULL_HALF_WIDTH = 3.1
HULL_CORNER_RADIUS = 2.4

# Bow / sensor pod, overlapping the hull so the two read as one body.
NOSE_CENTRE = (12.8, 7.6)
NOSE_HALF_LENGTH = 1.9
NOSE_HALF_WIDTH = 2.6
NOSE_CORNER_RADIUS = 2.2

# Cyan sensor lens, sunk into a dark bezel on the bow. Kept small enough that a
# rim of steel and bezel still frames it.
LENS_CENTRE = (12.8, 7.6)
LENS_RADIUS = 1.35
LENS_BEZEL_RADIUS = 1.95

# Floodlight: a small lamp low on the bow, clear of the lens.
FLOODLIGHT_CENTRE = (12.2, 9.6)
FLOODLIGHT_RADIUS = 0.75

# Amber shroud collar: a one-pixel vertical band behind the bow.
BAND_LEFT = 8.4
BAND_RIGHT = 9.4

# Skeg / keel under the hull.
SKEG_CENTRE = (5.6, 11.8)
SKEG_HALF_LENGTH = 1.0
SKEG_HALF_WIDTH = 1.6
SKEG_CORNER_RADIUS = 0.5

# Stubby beacon dome on the deck, as on the model (a low bump, not an antenna).
MAST_CENTRE = (5.0, 4.4)
MAST_HALF_LENGTH = 0.7
MAST_HALF_WIDTH = 1.0
MAST_CORNER_RADIUS = 0.6

# Vertical extent the shell ramp is spread over: deck to keel.
HULL_TOP = HULL_CENTRE[1] - HULL_HALF_WIDTH
HULL_BOTTOM = HULL_CENTRE[1] + HULL_HALF_WIDTH

# Top-lit ramp, brightest at the deck and darkest at the keel. The last two
# entries repeat so the shadow band at the waterline stays flat rather than
# fading out gradually.
SHELL_RAMP = (
    HULL_BRIGHT,
    HULL_LIGHT,
    HULL_MID,
    HULL_DARK,
    HULL_SHADOW,
    HULL_SHADOW,
)


# --- Shape tests --------------------------------------------------------------

def _rounded_rect(px: float, py: float, cx: float, cy: float,
                  hx: float, hy: float, radius: float) -> bool:
    """Point-in-rounded-rectangle, in canvas units."""
    dx = abs(px - cx)
    dy = abs(py - cy)
    if dx > hx or dy > hy:
        return False
    ex = dx - (hx - radius)
    ey = dy - (hy - radius)
    if ex <= 0.0 or ey <= 0.0:
        return True
    return ex * ex + ey * ey <= radius * radius


def _circle(px: float, py: float, cx: float, cy: float, radius: float) -> bool:
    return (px - cx) ** 2 + (py - cy) ** 2 <= radius * radius


def _in_duct(px: float, py: float) -> bool:
    return _circle(px, py, *DUCT_CENTRE, DUCT_RADIUS)


def _in_fan(px: float, py: float) -> bool:
    return _circle(px, py, *DUCT_CENTRE, FAN_RADIUS)


def _in_hull(px: float, py: float) -> bool:
    return _rounded_rect(px, py, *HULL_CENTRE, HULL_HALF_LENGTH, HULL_HALF_WIDTH,
                         HULL_CORNER_RADIUS)


def _in_nose(px: float, py: float) -> bool:
    return _rounded_rect(px, py, *NOSE_CENTRE, NOSE_HALF_LENGTH, NOSE_HALF_WIDTH,
                         NOSE_CORNER_RADIUS)


def _in_lens(px: float, py: float) -> bool:
    return _circle(px, py, *LENS_CENTRE, LENS_RADIUS)


def _in_lens_bezel(px: float, py: float) -> bool:
    return _circle(px, py, *LENS_CENTRE, LENS_BEZEL_RADIUS)


def _in_floodlight(px: float, py: float) -> bool:
    return _circle(px, py, *FLOODLIGHT_CENTRE, FLOODLIGHT_RADIUS)


def _in_skeg(px: float, py: float) -> bool:
    return _rounded_rect(px, py, *SKEG_CENTRE, SKEG_HALF_LENGTH, SKEG_HALF_WIDTH,
                         SKEG_CORNER_RADIUS)


def _in_mast(px: float, py: float) -> bool:
    return _rounded_rect(px, py, *MAST_CENTRE, MAST_HALF_LENGTH, MAST_HALF_WIDTH,
                         MAST_CORNER_RADIUS)


def _in_silhouette(px: float, py: float) -> bool:
    """The union of every solid part: hull, bow, duct, skeg and mast."""
    return (_in_hull(px, py) or _in_nose(px, py) or _in_duct(px, py)
            or _in_skeg(px, py) or _in_mast(px, py))


# --- Materials ----------------------------------------------------------------

def _fan_colour(px: float, py: float) -> tuple[int, int, int]:
    """The four-blade impeller: dark spokes on the pale backplate."""
    dx = px - DUCT_CENTRE[0]
    dy = py - DUCT_CENTRE[1]
    if dx * dx + dy * dy <= FAN_HUB_RADIUS * FAN_HUB_RADIUS:
        return FAN_BLADE
    # Perpendicular distance to the two diagonals |dx| = |dy|.
    on_blade = min(abs(dx - dy), abs(dx + dy)) / FAN_DIAGONAL_SCALE
    if on_blade <= FAN_BLADE_HALF_WIDTH:
        return FAN_BLADE
    return FAN_DISC


def _duct_colour(px: float, py: float) -> tuple[int, int, int]:
    """Shroud ring around the fan, lit from the top-left like the hull.

    ``py`` is borrowed as the shading axis by the skeg as well, which passes an
    offset so it gets the dark end of the ramp.
    """
    dy = py - DUCT_CENTRE[1]
    if dy <= -1.9:
        return HULL_MID
    if dy <= -0.5:
        return HULL_DARK
    if dy <= 0.9:
        return HULL_SHADOW
    return OUTLINE


def _lens_colour(px: float, py: float) -> tuple[int, int, int]:
    """Cyan glass with a hot top-left quadrant and a darker lower-right."""
    diagonal = (px - LENS_CENTRE[0]) + (py - LENS_CENTRE[1])
    if diagonal <= -0.7:
        return LENS_LIT
    if diagonal >= 0.8:
        return LENS_DIM
    return LENS


def _shell_colour(px: float, py: float) -> tuple[int, int, int]:
    """Top-lit steel ramp, one stop brighter toward the bow (light top-left)."""
    ratio = (py - HULL_TOP) / (HULL_BOTTOM - HULL_TOP)
    step = int(max(0.0, min(0.999, ratio)) * len(SHELL_RAMP))
    if px < HULL_CENTRE[0] - 1.0 and step > 0:
        step -= 1
    return SHELL_RAMP[step]


def _band_colour(px: float, py: float) -> tuple[int, int, int]:
    """Amber collar: bright at the deck, trim tone amidships, dark at the keel."""
    if py <= HULL_CENTRE[1] - 2.0:
        return AMBER_BRIGHT
    if py >= HULL_CENTRE[1] + 2.2:
        return AMBER_DARK
    return AMBER


def _beacon_colour(px: float, py: float) -> tuple[int, int, int]:
    """The dome reads as the model's beacon: its cap carries a lit amber cell."""
    if py <= MAST_CENTRE[1] + 0.3 and abs(px - MAST_CENTRE[0]) <= 0.8:
        return AMBER
    return HULL_LIGHT


def _material(x: int, y: int, coverage) -> tuple[int, int, int]:
    """Pick the flat colour of one opaque pixel from its supersampled mask."""
    px = x + 0.5
    py = y + 0.5

    # Stern impeller and its shroud sit on top of the hull's rear.
    if coverage(_in_fan) >= COVERAGE_THRESHOLD:
        return _fan_colour(px, py)
    if coverage(_in_duct) >= COVERAGE_THRESHOLD:
        return _duct_colour(px, py)

    # Keel and beacon break the silhouette without owning the shell ramp.
    if coverage(_in_skeg) >= COVERAGE_THRESHOLD:
        return _duct_colour(px, py + 4.0)
    if coverage(_in_mast) >= COVERAGE_THRESHOLD:
        return _beacon_colour(px, py)

    # Bow: cyan glass inside its bezel, then the floodlight below it.
    if coverage(_in_lens) >= COVERAGE_THRESHOLD:
        return _lens_colour(px, py)
    if coverage(_in_lens_bezel) >= COVERAGE_THRESHOLD:
        return LENS_BEZEL
    if coverage(_in_floodlight) >= COVERAGE_THRESHOLD:
        return FLOODLIGHT

    # Amber shroud collar, a crisp one-pixel stripe.
    if BAND_LEFT <= px < BAND_RIGHT:
        return _band_colour(px, py)

    return _shell_colour(px, py)


# --- Rendering ----------------------------------------------------------------

def _coverage(predicate, x: int, y: int) -> float:
    """Fraction of an 8x8 sub-pixel grid inside ``predicate`` for pixel (x, y)."""
    samples = SUPERSAMPLE
    hits = 0
    for sy in range(samples):
        sub_y = y + (sy + 0.5) / samples
        for sx in range(samples):
            sub_x = x + (sx + 0.5) / samples
            if predicate(sub_x, sub_y):
                hits += 1
    return hits / (samples * samples)


def render() -> Image.Image:
    """Draw the sprite: supersampled silhouette, flat materials, hard alpha."""
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()

    for y in range(SIZE):
        for x in range(SIZE):
            if _coverage(_in_silhouette, x, y) < COVERAGE_THRESHOLD:
                continue

            def mask(predicate, xx=x, yy=y):
                return _coverage(predicate, xx, yy)

            colour = _material(x, y, mask)

            # Top-left lighting: the upper rim catches the light. Only steel
            # gets the rim, so the collar and the glass keep their colour edge
            # to edge.
            if colour in SHELL_RAMP:
                above = _coverage(_in_silhouette, x, y - 1) if y > 0 else 0.0
                left = _coverage(_in_silhouette, x - 1, y) if x > 0 else 0.0
                if above < COVERAGE_THRESHOLD:
                    colour = HULL_HILIGHT
                elif left < COVERAGE_THRESHOLD:
                    colour = SHELL_RAMP[max(0, SHELL_RAMP.index(colour) - 1)]

            pixels[x, y] = (*colour, 255)

    return image


LEGEND = {
    OUTLINE: "o",
    HULL_HILIGHT: "H",
    HULL_BRIGHT: "L",
    HULL_LIGHT: "l",
    HULL_MID: "m",
    HULL_DARK: "d",
    HULL_SHADOW: "s",
    LENS_LIT: "!",
    LENS: "C",
    LENS_DIM: "c",
    LENS_BEZEL: "b",
    AMBER_BRIGHT: "A",
    AMBER: "a",
    AMBER_DARK: "B",
    FAN_DISC: "F",
    FAN_BLADE: "f",
    FLOODLIGHT: "W",
}


def preview(image: Image.Image, label: str) -> None:
    print(f"-- {label} --")
    for y in range(image.height):
        row = ""
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                row += "."
                continue
            letter, best = "?", None
            for colour, candidate in LEGEND.items():
                distance = sum((c - k) ** 2 for c, k in zip(colour, (r, g, b)))
                if best is None or distance < best:
                    letter, best = candidate, distance
            row += letter
        print(row)


def main() -> None:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)

    sprite = render()
    sprite.save(OUTPUT)
    print(f"wrote {OUTPUT} ({sprite.width}x{sprite.height})")

    preview(sprite, "submarine_drone")


if __name__ == "__main__":
    main()
