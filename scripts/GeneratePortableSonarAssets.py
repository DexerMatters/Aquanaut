#!/usr/bin/env python3
"""Draw the portable sonar's two inventory sprites.

The sonar is drawn the way the Blockbench model is built: a white enamelled
shell whose faceplate stands proud of the body under a dark anodised sun hood,
with a rubber keypad, a chrome collar, a ribbed grip and a transducer drum slung
under the front so its dark aperture points at the water.

The instrument is drawn in side elevation, front to the left and one texel per
model unit. The shell is six units across and six deep, so a head-on elevation
of it is a square; the profile is the one that says what the object is. It shows
the hood jutting over the recessed display at the top of the faceplate, the drum
hanging under the front with the aperture proud of it, the rear bezel closing
the back, the sealed grip dropping out of the middle, and -- on the flank the
model actually gives a flank -- the moulded grooves, the rating plate and the
ribbed rotary thumbwheel. The top of the shell is drawn as one row above the
hood.

The display faces along the view, so a strictly orthographic profile would never
show it: the glass sits one unit back inside a two-unit-deep recess and the
frame's own edge hides it. The three columns in front of the hood are therefore
the faceplate itself, read the way an item icon reads a box -- the near edge of
the frame, the glass, the keypad under it and the drum's aperture below that.
The recess mouth is drawn dark under the hood, which is what leaves the ranging
state something to light from the side at all.

Depth comes from the top row above the hood, the faceplate standing off the
shell, the moulded grooves and rating plate on the flank, and a cross-section
fall-off so the housing catches the light along its middle.

The two sprites come out of one description, so the idle sounder and the ranging
sounder can never drift apart: the ranging one is the same drawing with the
display and the transducer aperture lit and a little cyan spilling onto the
metal around them. The silhouette never moves.

Shading is analytical rather than sampled: the shell is a vertical gradient
modulated by the cross-section profile, the display is a lit ramp, everything is
banded to six tones, and the whole sprite is finished with the house silhouette
darkening. There is deliberately no drop shadow: the slot supplies the
backdrop, and a translucent halo reads as dirt at this size.

Usage
-----
    python3 scripts/GeneratePortableSonarAssets.py            # write sprites
    python3 scripts/GeneratePortableSonarAssets.py --preview  # + zoomed previews
    python3 scripts/GeneratePortableSonarAssets.py --dump     # + ASCII sprite
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ITEM_DIR = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut" / "textures" / "item"
OFF_PATH = ITEM_DIR / "portable_sonar.png"
ON_PATH = ITEM_DIR / "portable_sonar_on.png"
PREVIEW_DIR = REPO_ROOT / "build" / "preview"

SIZE = 16
TONES = 6

#: multiplier applied to the outermost texel of the silhouette
SILHOUETTE = 0.6

# ---------------------------------------------------------------------------
# palette: white enamel, dark anodised hood, rubber, chrome, glass
# ---------------------------------------------------------------------------

ENAMEL = ((132, 140, 149), (176, 184, 193), (208, 216, 224), (232, 238, 244), (250, 253, 255))
HOOD = ((26, 30, 36), (42, 48, 56), (60, 67, 76), (82, 90, 100), (106, 115, 126))
RUBBER = ((26, 30, 35), (40, 45, 52), (56, 62, 70), (78, 85, 94), (100, 108, 118))
CHROME = ((116, 124, 133), (154, 163, 172), (190, 199, 208), (220, 227, 234), (246, 250, 254))
GLASS_OFF = ((16, 40, 54), (24, 62, 80), (36, 88, 112), (52, 116, 142), (74, 146, 172))
GLASS_ON = ((188, 244, 246), (222, 252, 250), (244, 255, 254), (255, 255, 255), (214, 250, 246))
APERTURE_OFF = ((20, 28, 38), (32, 42, 54), (46, 58, 72), (62, 76, 92), (82, 98, 114))
APERTURE_ON = ((150, 232, 236), (196, 248, 246), (232, 255, 254), (255, 255, 255), (198, 246, 244))

#: The display and the aperture must never be treated as silhouette edges.
BRIGHT = frozenset("SA")

#: Light escaping the two apertures, as (manhattan distance, mix strength).
SPILL = ((1, 0.30), (2, 0.13))


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
# the sonar in profile, one character per texel
# ---------------------------------------------------------------------------

#: ``.`` nothing.  The first three columns are the faceplate read head on: ``V``
#: its enamelled lip and frame, ``S`` the glass, ``K`` the keypad, ``A`` the
#: transducer aperture.  The rest is the profile, one column per unit of depth.
#: Hood: ``x`` its top face, ``H`` lit, ``h`` shaded.  Shell: ``t`` the top face,
#: ``W`` lit, ``B`` the faceplate edge, ``G`` the rear bezel.  Flank relief:
#: ``m`` moulded groove, ``P`` rating plate, ``q`` engraved line on it, ``s``
#: moulding seam.  Recess: ``F`` its shadowed mouth.  Lower works: ``D`` drum,
#: ``N`` its dark gasket, ``C`` chrome collar, ``R``/``r`` the ribbed
#: thumbwheel, ``g`` ribbed grip, ``e`` grip heel.
#:
#: One string per row.  ``v`` 0-2 is the faceplate read head on; 3-12 is the
#: profile, 3 being the front of the instrument -- hood, recess and aperture --
#: 4-5 the faceplate standing off the shell, 6-11 the shell's flank and 12 the
#: rear bezel.  ``u`` counts model units down the instrument, -1 being the top of
#: the shell and 7 the underside of it, where the drum, the collar and the grip
#: take over.
ROWS = (
    (-1, "xxxxttttttttt"),
    (0, "HHHHBBWWWWWWG"),
    (1, "BSShBBWqPqPWG"),
    (2, "BSShBBWqPqPWG"),
    (3, "BSShBBWmRrmWG"),
    (4, "BSSFBBssRrssG"),
    (5, "BKKFBBWmWWmWG"),
    (6, "VVVFBBWmWWmWG"),
    (7, "VVVNDDDCCCC.."),
    (8, "VAAADDDgggg.."),
    (9, "VAAADDDgggg.."),
    (10, ".......gggg.."),
    (11, ".......gggg.."),
    (12, ".......eeee.."),
)

#: One model unit of depth is one texel and one unit down the instrument is one
#: texel, so the profile is not redrawn to flatter the icon: it is the model's
#: own flank, held upright the way it sits in the fist.  Ten units of depth, the
#: faceplate read across the front of it, thirteen units tall.
ORIGIN_X = 1
ORIGIN_Y = 1

#: Cross-section fall-off of the shell, indexed by column: the flank faces the
#: light, so it is brightest along its middle and rolls away at the hood in front
#: and the rear bezel behind.  The faceplate read head on is flat to the viewer
#: and is left alone.
ACROSS = (1.0, 1.0, 1.0, 1.0, 0.9, 0.97, 1.01, 1.04, 1.04, 1.0, 0.95, 0.89, 0.82)

#: Rows of the shell, for the vertical fall-off: the hood is its top and the
#: bezel under the keys is its bottom.
FACE_TOP = 0
FACE_BOTTOM = 6


def face_t(u: int) -> float:
    return min(1.0, max(0.0, (u - FACE_TOP) / float(FACE_BOTTOM - FACE_TOP)))


def place(u: int, v: int):
    return ORIGIN_X + v, ORIGIN_Y + u


def shade(glyph: str, u: int, v: int, x: int, y: int, lit: bool):
    """The colour of one texel, before the silhouette pass."""

    t = face_t(u)

    shell = ramp(ENAMEL, band(1.0 - 0.6 * t))

    if glyph == "t":
        # The brightest plane: the top of the shell looks straight at the sky.
        colour = ramp(ENAMEL, band(1.0))
    elif glyph == "x":
        colour = ramp(HOOD, band(0.78))
    elif glyph == "H":
        colour = ramp(HOOD, band(0.56 - 0.14 * t))
    elif glyph == "h":
        colour = ramp(HOOD, band(0.32 - 0.08 * t))
    elif glyph == "B":
        # The faceplate's own edge, standing proud of the shell it carries.
        colour = scale(shell, 0.9)
    elif glyph == "W":
        colour = shell
    elif glyph in ("m", "s", "P", "q"):
        relief = {"m": 0.74, "s": 0.66, "P": 0.86, "q": 0.55}[glyph]
        colour = scale(shell, relief)
    elif glyph == "G":
        # The rear bezel: the same enamel turned into the housing's back.
        colour = scale(shell, 0.82)
    elif glyph == "V":
        # The faceplate turned to the viewer: the one plane that faces the light.
        colour = ramp(ENAMEL, band(0.96))
    elif glyph == "F":
        # The shadowed mouth of the display recess, under the hood.
        colour = ramp(HOOD, band(0.5))
    elif glyph == "R":
        colour = ramp(RUBBER, band(0.78))
    elif glyph == "r":
        colour = ramp(RUBBER, band(0.3))
    elif glyph == "C":
        colour = ramp(CHROME, band(0.9 - 0.25 * (u - 7)))
    elif glyph == "D":
        colour = ramp(ENAMEL, band(1.0 - 0.32 * (u - 7)))
    elif glyph == "N":
        # Dark gasket band where the aperture is bolted to the drum.
        colour = ramp(HOOD, band(0.18))
    elif glyph == "K":
        colour = ramp(RUBBER, band(0.5))
    elif glyph == "g":
        colour = ramp(RUBBER, band(0.24 if u == 11 else (0.46 if u % 2 else 0.72)))
    elif glyph == "e":
        colour = ramp(RUBBER, band(0.16))
    elif glyph == "S":
        if not lit:
            # Off, the sliver of glass under the hood only holds a reflection.
            colour = ramp(GLASS_OFF, band(0.72))
        else:
            colour = ramp(GLASS_ON, band(0.98))
    elif glyph == "A":
        if not lit:
            colour = ramp(APERTURE_OFF, band(0.72 - 0.18 * (u - 8)))
        else:
            colour = ramp(APERTURE_ON, band(1.0 - 0.22 * (u - 8)))
    else:
        return None

    return scale(colour, ACROSS[v])


def draw(lit: bool) -> Image.Image:
    cells = {}

    for u, glyphs in ROWS:
        for v, glyph in enumerate(glyphs):
            if glyph != ".":
                cells[place(u, v)] = (glyph, u, v)

    # Light escaping the display recess and the transducer onto the metal around
    # them, close in on the source.
    spill = {}

    if lit:
        sources = [xy for xy, (glyph, _, _) in cells.items() if glyph in BRIGHT]

        for xy, (glyph, _, _) in cells.items():
            if glyph in BRIGHT:
                continue

            distance = min(abs(xy[0] - sx) + abs(xy[1] - sy) for sx, sy in sources)

            for reach, strength in SPILL:
                if distance == reach:
                    spill[xy] = strength
                    break

    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()

    for (x, y), (glyph, u, v) in cells.items():
        rgb = shade(glyph, u, v, x, y, lit)

        if rgb is None:
            continue

        if (x, y) in spill:
            rgb = mix(rgb, GLASS_ON[2], spill[(x, y)])

        pixels[x, y] = (*rgb, 255)

    painted = set(cells)

    for x, y in sorted(painted):
        if cells[(x, y)][0] in BRIGHT:
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
                PREVIEW_DIR / f"portable_sonar_icon_{name}.png")
            board.alpha_composite(image, (index * (SIZE + 3), 0))

        board.resize((board.width * 12, board.height * 12), Image.NEAREST).save(
            PREVIEW_DIR / "portable_sonar_icons.png")
        print(f"previews in {PREVIEW_DIR.relative_to(REPO_ROOT)}")

    if args.dump:
        print("\noff:")
        dump(off)
        print("\non:")
        dump(on)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
