#!/usr/bin/env python3
"""Procedurally paints the Opticichthus laser beam + flare sprites.

Both sprites are additive, so they are painted as *masks*: the alpha channel carries the shape and
the RGB channel carries a deliberately tiny palette (white -> pale cyan -> blue -> deep blue), the
same trick vanilla uses for its glow and beam textures.

They are also **16 pixels across on purpose**. The beam cross-section is a hard stair-step rather
than a gradient, and the flare is a blocky pixel star, so with nearest-neighbour sampling the lance
reads as chunky Minecraft pixels instead of a smooth modern glow.

`opticichthus_beam.png` (16x64)
    x axis = profile across the beam (u = 0..1 across the ribbon), symmetric, six discrete tones
    y axis = position along the beam (v repeats + scrolls)
    The outer bands are dithered into 4px dashes so the scrolling reads as packets of energy while
    the core stays solid.

`opticichthus_flare.png` (16x32, two 16x16 frames)
    A blocky muzzle/impact burst that animates the way vanilla textures do - by swapping frames, not
    by spinning (a sprite this symmetric would look identical rotated). Frame 0 is a plus star with a
    square aperture rim, frame 1 is the same burst turned 45 degrees.

Usage:
    python3 scripts/GenerateOpticichthusBeamTextures.py [--preview] [--dry-run]
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ENTITY_DIR = REPO_ROOT / "src/main/resources/assets/aquanaut/textures/entity"

BEAM_TEXTURE = ENTITY_DIR / "opticichthus_beam.png"
FLARE_TEXTURE = ENTITY_DIR / "opticichthus_flare.png"

#: The beam tiles along its length, so it must not be clamped at the v edges.
BEAM_METADATA = {"texture": {"blur": False, "clamp": False}}

BEAM_WIDTH = 16
BEAM_HEIGHT = 64
FLARE_SIZE = 16
FLARE_FRAMES = 2
STRIP_HEIGHT = 4

#: Eight discrete tones, ordered from the outside of the beam (index 0) to its centre (index 7).
PROFILE: tuple[tuple[int, tuple[int, int, int]], ...] = (
    (0, (0, 0, 0)),
    (40, (42, 79, 168)),
    (96, (58, 111, 216)),
    (152, (90, 168, 240)),
    (208, (143, 208, 255)),
    (255, (200, 240, 255)),
    (255, (234, 249, 255)),
    (255, (255, 255, 255)),
)

#: One entry per 4px strip for each dithered band (16 strips to a texture, so 64px of variety).
#: False drops that band two levels, which cuts the beam's edge into dashes without ever inventing a
#: tone outside the palette.
DASH: dict[int, tuple[bool, ...]] = {
    1: (True, False, True, True, False, True, False, True,
        True, False, True, False, True, True, False, True),
    2: (True, True, False, True, True, False, True, False,
        True, True, False, True, False, True, True, False),
    3: (False, True, True, True, False, True, True, False,
        True, True, True, False, True, False, True, True),
    4: (True, True, True, False, True, True, True, True,
        False, True, True, True, True, False, True, True),
}

WHITE = (255, 255, 255)
PALE = (200, 240, 255)
LIGHT = (143, 208, 255)
MID = (90, 168, 240)
BLUE = (58, 111, 216)
DARK = (42, 79, 168)

#: Beams wider than this many pixels paint the solid core; everything outside is a dithered band.
CORE_BAND = 5


def paint_beam() -> Image.Image:
    image = Image.new("RGBA", (BEAM_WIDTH, BEAM_HEIGHT))
    pixels = image.load()
    for y in range(BEAM_HEIGHT):
        strip = (y // STRIP_HEIGHT) % 16
        for x in range(BEAM_WIDTH):
            band = x if x <= 7 else BEAM_WIDTH - 1 - x
            if band == 0:
                pixels[x, y] = (0, 0, 0, 0)
                continue
            alpha, colour = PROFILE[band]
            if band < CORE_BAND and not DASH[band][strip]:
                alpha, colour = PROFILE[max(0, band - 2)]
            if alpha == 0:
                pixels[x, y] = (0, 0, 0, 0)
            else:
                pixels[x, y] = (colour[0], colour[1], colour[2], alpha)
    return image


def flare_pixel(frame: int, x: int, y: int) -> tuple[tuple[int, int, int], int]:
    """One pixel of one flare frame. Coordinates are doubled so every test lands on a whole pixel."""
    span = FLARE_SIZE - 1
    dx = 2 * x - span
    dy = 2 * y - span
    ax = abs(dx)
    ay = abs(dy)
    chebyshev = max(ax, ay)
    diagonal = frame == 1

    if ax <= 3 and ay <= 3:
        return WHITE, 255
    if chebyshev <= 5:
        return PALE, 255
    if diagonal:
        if (abs(dx - dy) <= 3 or abs(dx + dy) <= 3) and chebyshev <= 13:
            return LIGHT, 255
    elif (ay <= 3 and ax <= 11) or (ax <= 3 and ay <= 11):
        return LIGHT, 255
    if chebyshev == 13 and ax >= 7 and ay >= 7:
        # Aperture rim: only the four corners of the outer square, in the dimmest tone, so it reads as
        # an iris bracket instead of a frame. Symmetric by construction.
        return DARK, 255
    if not diagonal and ax == ay and ax in (7, 9):
        return BLUE, 255
    return (0, 0, 0), 0


def paint_flare() -> Image.Image:
    image = Image.new("RGBA", (FLARE_SIZE, FLARE_SIZE * FLARE_FRAMES))
    pixels = image.load()
    for frame in range(FLARE_FRAMES):
        for y in range(FLARE_SIZE):
            for x in range(FLARE_SIZE):
                colour, alpha = flare_pixel(frame, x, y)
                if alpha == 0:
                    pixels[x, y + frame * FLARE_SIZE] = (0, 0, 0, 0)
                else:
                    pixels[x, y + frame * FLARE_SIZE] = (colour[0], colour[1], colour[2], alpha)
    return image


def report(name: str, image: Image.Image) -> None:
    pixels = image.load()
    ink = sum(1 for y in range(image.height) for x in range(image.width) if pixels[x, y][3] > 0)
    alphas = sorted({pixels[x, y][3] for y in range(image.height) for x in range(image.width)})
    print(f"  {name}: {image.width}x{image.height} ink={ink} alphas={alphas}")
    if image.width == BEAM_WIDTH:
        row = [pixels[x, STRIP_HEIGHT][3] for x in range(BEAM_WIDTH)]
        print("    cross-section " + " ".join(f"{alpha:3d}" for alpha in row))


def ascii_preview(name: str, image: Image.Image) -> None:
    pixels = image.load()
    ramp = " .:-=+*#%@"
    print(f"\n{name}:")
    for y in range(image.height):
        row = "".join(ramp[min(9, pixels[x, y][3] * 10 // 256)] for x in range(image.width))
        print("    " + row)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="analyse without writing files")
    parser.add_argument("--preview", action="store_true", help="print an ASCII preview of each sprite")
    args = parser.parse_args(argv)

    beam = paint_beam()
    flare = paint_flare()

    print("opticichthus laser sprites")
    report("beam ", beam)
    report("flare", flare)

    if args.preview:
        ascii_preview("opticichthus_beam", beam)
        ascii_preview("opticichthus_flare", flare)

    failures: list[str] = []
    beam_pixels = beam.load()
    flare_pixels = flare.load()
    palette = {entry[0] for entry in PROFILE}

    for y in range(BEAM_HEIGHT):
        for x in range(BEAM_WIDTH):
            if beam_pixels[x, y][3] not in palette:
                failures.append("beam tone outside the palette")
            if beam_pixels[x, y] != beam_pixels[BEAM_WIDTH - 1 - x, y]:
                failures.append("beam cross-section must be symmetric")
    if max(beam_pixels[x, y][3] for x in range(BEAM_WIDTH) for y in range(BEAM_HEIGHT)) < 255:
        failures.append("beam core must be opaque")
    if beam_pixels[0, 0][3] > 0 or beam_pixels[BEAM_WIDTH - 1, 0][3] > 0:
        failures.append("beam profile must fade to nothing at the ribbon edges")
    if len({beam_pixels[x, STRIP_HEIGHT][3] for x in range(BEAM_WIDTH)}) > 6:
        failures.append("beam cross-section must be a stair-step, not a gradient")
    core = {beam_pixels[x, y][3] for y in range(BEAM_HEIGHT) for x in range(5, 11)}
    if core != {255}:
        failures.append("beam core must be a solid white block")

    flare_ink = []
    for frame in range(FLARE_FRAMES):
        offset = frame * FLARE_SIZE
        ink = 0
        for y in range(FLARE_SIZE):
            for x in range(FLARE_SIZE):
                pixel = flare_pixels[x, y + offset]
                if pixel != flare_pixels[FLARE_SIZE - 1 - x, y + offset] \
                        or pixel != flare_pixels[x, FLARE_SIZE - 1 - y + offset]:
                    failures.append(f"flare frame {frame} must be mirror symmetric on both axes")
                if pixel[3] > 0:
                    ink += 1
        if min(flare_pixels[x, y + offset][3] for x in (6, 7, 8, 9) for y in (6, 7, 8, 9)) < 255:
            failures.append(f"flare frame {frame} core must be an opaque block")
        for i in range(FLARE_SIZE):
            if max(flare_pixels[i, offset][3], flare_pixels[i, FLARE_SIZE - 1 + offset][3],
                   flare_pixels[0, i + offset][3], flare_pixels[FLARE_SIZE - 1, i + offset][3]) > 0:
                failures.append(f"flare frame {frame} must not touch its sprite border (it is billboarded)")
        flare_ink.append(ink)
    if any(not 40 <= ink <= 160 for ink in flare_ink):
        failures.append(f"each flare frame must be a lean pixel star, found {flare_ink}")
    if flare_ink[0] == flare_ink[1]:
        failures.append("the two flare frames must differ, otherwise the animation is invisible")

    if failures:
        print("\nFAILURES:", file=sys.stderr)
        for failure in sorted(set(failures)):
            print(f"  - {failure}", file=sys.stderr)
        return 1

    if not args.dry_run:
        ENTITY_DIR.mkdir(parents=True, exist_ok=True)
        beam.save(BEAM_TEXTURE)
        flare.save(FLARE_TEXTURE)
        BEAM_TEXTURE.with_suffix(".png.mcmeta").write_text(
            json.dumps(BEAM_METADATA, indent=2) + "\n", encoding="utf-8")

    print(f"\nwrote {BEAM_TEXTURE.name}, {FLARE_TEXTURE.name} and "
          f"{BEAM_TEXTURE.with_suffix('.png.mcmeta').name} "
          f"into {ENTITY_DIR.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
