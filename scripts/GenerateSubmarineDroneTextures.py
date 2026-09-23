#!/usr/bin/env python3
"""Generate the submarine drone's powered-down skin from its live one.

The drone changes texture with its control state, and the two skins have to stay
in step: a hull that is repainted or re-lit regenerates its dead twin here rather
than being maintained by hand.

The transform is deliberately physical rather than arbitrary. Cold stainless
loses its warm bounce and its contrast; the lit lens and the floodlight are glass,
not paint, so they go to a flat grey-blue instead of just getting darker. That is
four operations, in order:

1. Desaturate toward luminance, so the amber shroud band and the cyan lens stop
   reading as coloured at all.
2. Cool the residue a few points toward blue, the colour of a cold sky reflecting
   off bare metal.
3. Drop the exposure, with a floor so the panel lines do not turn into a
   silhouette.
4. Rebuild the emissive mask as a single standby lamp: the lens, the floodlight
   and the scanner strip all go dark, and only the dedicated indicator stays lit,
   a deep red. A machine that is switched off but still has power in it looks
   exactly like that.

Step 4 is also load-bearing for GeckoLib, which refuses an emissive mask with no
lit pixel at all ("Invalid glow layer texture provided, must have at least one
pixel!") and takes the whole game down with it. The mask is therefore rebuilt
rather than blanked, and the lamp's cell is validated against the live mask: if
the atlas is ever repainted so that cell is no longer a lamp, this script fails
loudly instead of quietly lighting up a random spot on the hull.

Outputs:
  src/main/resources/assets/aquanaut/textures/entity/submarine_drone_deactivated.png
  src/main/resources/assets/aquanaut/textures/entity/submarine_drone_deactivated_glowmask.png

Run from the repository root:
  python3 scripts/GenerateSubmarineDroneTextures.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

TEXTURE_DIR = Path("src/main/resources/assets/aquanaut/textures/entity")
ACTIVE = TEXTURE_DIR / "submarine_drone.png"
ACTIVE_MASK = TEXTURE_DIR / "submarine_drone_glowmask.png"
OUTPUT = TEXTURE_DIR / "submarine_drone_deactivated.png"
OUTPUT_MASK = TEXTURE_DIR / "submarine_drone_deactivated_glowmask.png"

# How far to pull the colour out of the hull (0 = untouched, 1 = greyscale).
DESATURATION = 0.62

# Shift applied after desaturation: cold metal reflects a colder sky.
COOLING = (-7, -1, 7)

# Exposure multiplier and the floor a pixel may not fall below, so panel lines
# survive the darkening.
EXPOSURE = 0.60
FLOOR = 26

# The drone's dedicated indicator lamp, in atlas UV. It is the one lamp cell that
# stands alone — the lens and the scanner strip are runs of four and the floodlight
# is a pair — which makes it the natural place for a standby light.
STANDBY_LAMP = (13, 11)
STANDBY_COLOUR = (200, 62, 46, 255)


def clamp_byte(value: float) -> int:
    return int(round(min(255.0, max(0.0, value))))


def power_down(pixel: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    r, g, b, a = pixel
    if a == 0:
        return pixel

    luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    r = r + (luminance - r) * DESATURATION
    g = g + (luminance - g) * DESATURATION
    b = b + (luminance - b) * DESATURATION

    r = r * EXPOSURE + COOLING[0]
    g = g * EXPOSURE + COOLING[1]
    b = b * EXPOSURE + COOLING[2]

    return (
        max(FLOOR, clamp_byte(r)),
        max(FLOOR, clamp_byte(g)),
        max(FLOOR, clamp_byte(b)),
        a,
    )


def standby_mask(mask: Image.Image) -> Image.Image:
    """An all-dark emissive mask with just the standby lamp lit."""
    if mask.getpixel(STANDBY_LAMP)[3] == 0:
        raise SystemExit(
            f"no lamp at {STANDBY_LAMP} in {ACTIVE_MASK}; the atlas was repainted, "
            "so the standby lamp needs a new cell"
        )

    standby = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    standby.putpixel(STANDBY_LAMP, STANDBY_COLOUR)
    return standby


def main() -> None:
    active = Image.open(ACTIVE).convert("RGBA")
    mask = Image.open(ACTIVE_MASK).convert("RGBA")
    if active.size != mask.size:
        raise SystemExit(f"base and glowmask differ in size: {active.size} vs {mask.size}")

    dead = Image.new("RGBA", active.size, (0, 0, 0, 0))
    source_pixels = active.load()
    dead_pixels = dead.load()
    for y in range(active.height):
        for x in range(active.width):
            dead_pixels[x, y] = power_down(source_pixels[x, y])
    dead.save(OUTPUT)

    # Lamps out, standby in. The mask also has to exist at the base texture's size:
    # GeckoLib derives its emissive layer from it and refuses a mismatched mask.
    standby = standby_mask(mask)
    standby.save(OUTPUT_MASK)

    lit = sum(1 for y in range(standby.height) for x in range(standby.width)
              if standby.getpixel((x, y)) != (0, 0, 0, 0))
    print(f"wrote {OUTPUT} ({dead.width}x{dead.height})")
    print(f"wrote {OUTPUT_MASK} ({standby.width}x{standby.height}), {lit} lit lamp(s)")


if __name__ == "__main__":
    main()
