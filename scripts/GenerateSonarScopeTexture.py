#!/usr/bin/env python3
"""Draw the portable sonar's own textures: the scope face, its mote, and the flat white.

The scope is the instrument's dial: a dark phosphor disc with range rings, bearing ticks,
a crosshair and a machined bezel. Everything that moves — the wavefront, the phosphor trail
behind it, the blips and their height stems, the idle sweep — is drawn over this at run time
by ``SonarScope``, because none of it can be baked: it is a function of where the diver is
looking and how old the ping is.

What is baked is everything that never changes, and baking it is what lets the dial be a
*drawing* rather than a pile of fill calls. A circle of radius thirty-one drawn a scanline at
a time every frame is four hundred quads; the same circle in a sprite is four. It also lets
the face carry the things that make a screen look like a screen and would be absurd to
compute — the banded phosphor fall-off, the dithering between bands, the scanlines, the
sheen across the glass, and a bezel with a lit top-left edge and a shadowed bottom-right one.

The dial faces the diver: ahead is up, so the cardinals are not marked (they would be wrong
the moment the diver turned) and a caret at the top of the rim marks the bow.

Usage
-----
    python3 scripts/GenerateSonarScopeTexture.py            # write the assets
    python3 scripts/GenerateSonarScopeTexture.py --preview  # + a zoomed preview
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut" / "textures"
DIAL_PATH = ASSETS / "gui" / "sprites" / "hud" / "sonar_scope.png"
WHITE_PATH = ASSETS / "entity" / "sonar_white.png"
MOTE_DIR = ASSETS / "particle"
MOTE_FRAMES = 3
PREVIEW_DIR = REPO_ROOT / "build" / "preview"

#: The dial is square and its centre is the transducer. Seventy-two is the size the scope is
#: blitted at, so nothing is ever scaled and the bezel stays a pixel wide.
SIZE = 72
CENTRE = (SIZE - 1) / 2.0

#: Radii, from the outside in: the crown of the bezel, the frame body below it, the recess
#: the glass is sunk into, the rim of the glass, and the phosphor itself.
CROWN = 35.0
FRAME = 34.0
RECESS = 32.6
RIM = 31.4
GLASS = 30.4

#: Phosphor, from the rim of the glass inwards. Four bands with dithering between them: a
#: screen is banded, not smooth, and the stepping is what makes it read as one.
BANDS = ((11, 26, 31), (13, 31, 37), (15, 36, 43), (18, 42, 50))

RING_DIM = (23, 58, 68)
RING_BRIGHT = (34, 84, 96)
TICK = (64, 144, 162)
CROSSHAIR = (25, 58, 68)

FRAME_DARK = (8, 13, 16)
FRAME_MID = (24, 36, 42)
FRAME_LIT = (112, 136, 144)
RECESS_COLOUR = (5, 9, 11)

SHEEN = 9
SCANLINE = 3

#: Range rings at a quarter, a half and three quarters out. The full radius is the rim.
RINGS = (0.25, 0.5, 0.75)


def shade(colour, delta):
    return tuple(max(0, min(255, channel + int(round(delta)))) for channel in colour)


def dial() -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()

    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - CENTRE
            dy = y - CENTRE
            radius = math.hypot(dx, dy)

            if radius > CROWN:
                continue

            pixels[x, y] = (*face(x, y, dx, dy, radius), 255)

    return image


def face(x, y, dx, dy, radius):
    if radius > RECESS:
        return bezel(radius, dx, dy)

    if radius > RIM:
        # The shadow the bezel throws down into the recess, deepening outwards.
        depth = (radius - RIM) / (RECESS - RIM)
        return shade(RECESS_COLOUR, depth * 4.0)

    if radius > GLASS:
        # The ground edge of the glass, catching a little of the bezel's light.
        return RING_DIM

    return glass(x, y, radius)


def bezel(radius, dx, dy):
    # -1 at the lit upper left, +1 at the shadowed lower right.
    lean = (dx + dy) / (2.0 * CROWN * math.sqrt(2.0))
    lit = max(0.0, -lean)

    if radius > FRAME:
        # The crown: the one part of the housing that faces the light.
        return shade(FRAME_DARK, lit * (FRAME_LIT[0] - FRAME_DARK[0]))

    return shade(FRAME_MID, lit * 36.0 - (1.0 - lit) * 10.0)


def glass(x, y, radius):
    # Banded phosphor, dithered between bands so the steps interlock instead of banding hard.
    # Brightest at the transducer and falling away to the rim, the way a driven screen lights.
    position = (1.0 - radius / GLASS) * len(BANDS)
    index = min(len(BANDS) - 1, int(position))
    colour = BANDS[index]

    if position - index > 0.55 and ((x + y) & 1) == 0 and index + 1 < len(BANDS):
        colour = BANDS[index + 1]

    # A screen is lit from above, so the top of the glass is a little brighter than the foot.
    colour = shade(colour, (1.0 - y / (SIZE - 1.0)) * 5.0)

    # The sheen across the glass, upper left, gone before it reaches the middle.
    if (x - CENTRE) * 0.8 + (y - CENTRE) * 0.6 < -0.25 * GLASS:
        colour = shade(colour, SHEEN)

    # Scanlines, so the face is a screen and not a disc of paint.
    if y % 3 == 0:
        colour = shade(colour, SCANLINE)

    return graticule(x, y, x - CENTRE, y - CENTRE, radius, colour)


def graticule(x, y, dx, dy, radius, colour):
    # The rim of the glass, where the phosphor meets the shadow under the bezel.
    if radius > GLASS - 1.0:
        return RING_DIM

    for step in RINGS:
        if abs(radius - GLASS * step) < 0.45:
            return RING_BRIGHT if step == 0.5 else RING_DIM

    # The transducer the whole picture is drawn around.
    if radius < 1.3:
        return TICK

    angle = math.degrees(math.atan2(dx, -dy)) % 360.0

    # Bearing ticks: a fine one every fifteen degrees, a heavier one every forty-five.
    if radius > GLASS - 2.6 and abs(angle - round(angle / 15.0) * 15.0) < 0.75:
        return TICK

    if radius > GLASS - 4.6 and abs(angle - round(angle / 45.0) * 45.0) < 0.5:
        return TICK

    # A hairline crosshair.
    if (abs(dx) < 0.5 or abs(dy) < 0.5) and radius > 6.0:
        return CROSSHAIR

    # The caret at the bow: a small triangle pointing forward, so the dial has a top.
    apex = -(GLASS - 2.5)
    base = -(GLASS - 7.0)

    if base <= dy <= apex:
        half = 3.0 * (dy - apex) / (base - apex)
        if abs(dx) <= half:
            return TICK

    return colour


def flat_white() -> Image.Image:
    """The one opaque texel the wave's borrowed program samples; colour is all there is."""
    return Image.new("RGBA", (1, 1), (255, 255, 255, 255))


#: The mote's three frames, as (radius, falloff): a hard bright speck that opens out into a
#: soft one as the water closes over it. The particle walks these by age, so the speck swells
#: by dissolving rather than by growing.
MOTE_SHAPES = ((2.1, 1.1), (3.1, 1.7), (4.1, 2.4))


def mote_frames():
    """The sonar's particle: a white speck, so the colour it is drawn in is the whole of it."""
    frames = []

    for frame in range(MOTE_FRAMES):
        radius, falloff = MOTE_SHAPES[frame]
        size = 8
        image = Image.new("RGBA", (size, size), (255, 255, 255, 0))
        pixels = image.load()
        centre = (size - 1) / 2.0

        for y in range(size):
            for x in range(size):
                distance = math.hypot(x - centre, y - centre)
                fall = max(0.0, 1.0 - distance / radius)

                if fall <= 0.0:
                    continue

                pixels[x, y] = (255, 255, 255, int(round(255.0 * fall ** falloff)))

        frames.append(image)

    return frames


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--preview", action="store_true", help="also write a zoomed preview into build/preview")
    args = parser.parse_args(argv)

    DIAL_PATH.parent.mkdir(parents=True, exist_ok=True)
    WHITE_PATH.parent.mkdir(parents=True, exist_ok=True)
    MOTE_DIR.mkdir(parents=True, exist_ok=True)

    image = dial()
    image.save(DIAL_PATH)
    flat_white().save(WHITE_PATH)

    print(f"wrote {DIAL_PATH.relative_to(REPO_ROOT)}")
    print(f"wrote {WHITE_PATH.relative_to(REPO_ROOT)}")

    for index, frame in enumerate(mote_frames()):
        path = MOTE_DIR / f"sonar_mote_{index}.png"
        frame.save(path)
        print(f"wrote {path.relative_to(REPO_ROOT)}")

    if args.preview:
        PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
        board = Image.new("RGBA", (SIZE, SIZE), (24, 28, 34, 255))
        board.alpha_composite(image)
        board.resize((SIZE * 8, SIZE * 8), Image.NEAREST).save(PREVIEW_DIR / "sonar_scope.png")

        motes = Image.new("RGBA", (8 * MOTE_FRAMES + 2 * (MOTE_FRAMES - 1), 8), (24, 28, 34, 255))

        for index, frame in enumerate(mote_frames()):
            motes.alpha_composite(frame, (index * 10, 0))

        motes.resize((motes.width * 16, motes.height * 16), Image.NEAREST).save(
            PREVIEW_DIR / "sonar_mote.png")
        print(f"previews in {PREVIEW_DIR.relative_to(REPO_ROOT)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
