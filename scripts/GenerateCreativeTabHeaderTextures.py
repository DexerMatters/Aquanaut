#!/usr/bin/env python3
"""Generate the creative-tab header banners (162x18) for the natural tab.

Each header banner is one full row of the creative item grid -- nine slot cells, 18px each --
and is composed from the block textures of the biome it names, so the banner is literally the
ground of that biome: the strata are sampled from its rocks and flora, crossfading from tile to
tile along the row. A left-to-right scrim and darkened top and bottom edges seat the header text
the client writes on top.

Usage: python3 scripts/GenerateCreativeTabHeaderTextures.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

BLOCKS = Path("src/main/resources/assets/aquanaut/textures/block")
OUT = Path("src/main/resources/assets/aquanaut/textures/gui/creative_tab")

WIDTH = 162
HEIGHT = 18
TILE = 18

# The banner of each header, from the blocks that biome actually generates.
HEADERS = {
    "middle_level_ocean": [
        "coral_sand", "nutrient_rich_mud", "shale", "limestone",
    ],
    "coral_forest": [
        "red_coral_block", "purple_coral_block", "green_coral_block",
        "ringed_blue_coral_block", "fluorescent_blue_coral_block",
    ],
    "jelly_jungle": [
        "seaweed", "drooping_seaweed", "light_red_jelly_block",
        "white_jelly_block", "light_golden_jelly_block",
    ],
    "brine_mirror_gorge": [
        "halite_crust", "varve_shale", "brine_mirror", "halite_druse", "gypsum_rose",
    ],
    "brimstone_caldera": [
        "volcanic_basalt", "scoria", "sulfur_crust", "vent_chimney", "sinter",
    ],
    "crystal_nest": [
        "crystal_nest_stone", "crystal_druse", "crystal_column",
        "amethyst_crystal_cluster", "life_gem_cluster",
    ],
    "structures": [
        "dissection_table", "fishing_net", "plexiglass", "gas_pipe_glass",
    ],
}


def load_tile(name: str) -> Image.Image:
    path = BLOCKS / f"{name}.png"
    if not path.exists():
        raise SystemExit(f"missing block texture: {path}")
    image = Image.open(path).convert("RGBA")
    # Animated block sheets are 16 x N frames; the banner wants one square frame.
    frame = image.crop((0, 0, min(16, image.width), min(16, image.height)))
    return frame.resize((TILE, TILE), Image.NEAREST)


def smooth(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return t * t * (3.0 - 2.0 * t)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def blend(a, b, t: float):
    return tuple(int(round(lerp(a[i], b[i], t))) for i in range(4))


def flatten(pixel, base=(12, 14, 18)):
    """Composite one source pixel over a dark base: a banner covers slot cells, so it is opaque."""
    r, g, b, a = pixel
    t = a / 255.0
    return (int(round(lerp(base[0], r, t))), int(round(lerp(base[1], g, t))),
            int(round(lerp(base[2], b, t))), 255)


def compose(tiles) -> Image.Image:
    band = 4.0 / WIDTH * len(tiles)  # crossfade width, in segment units
    step = WIDTH / len(tiles)
    canvas = Image.new("RGBA", (WIDTH, HEIGHT))
    for x in range(WIDTH):
        u = x / step - 0.5
        i = math.floor(u)
        frac = u - i
        current_tile = tiles[i % len(tiles)]
        previous_tile = tiles[(i - 1) % len(tiles)]
        next_tile = tiles[(i + 1) % len(tiles)]
        for y in range(HEIGHT):
            here = flatten(current_tile.getpixel((x % TILE, y)))
            if frac < band:
                t = smooth(frac / band)
                pixel = blend(flatten(previous_tile.getpixel((x % TILE, y))), here, t)
            elif frac > 1.0 - band:
                t = smooth((frac - (1.0 - band)) / band)
                pixel = blend(here, flatten(next_tile.getpixel((x % TILE, y))), t)
            else:
                pixel = here
            canvas.putpixel((x, y), pixel)
    return canvas


def grade(canvas: Image.Image) -> Image.Image:
    """Seat the header text: dark edges top and bottom, a scrim under the title side."""
    for x in range(WIDTH):
        scrim = lerp(0.62, 0.18, smooth(x / 118.0)) if x < 118 else 0.18
        for y in range(HEIGHT):
            edge = 0.55 if y == 0 or y == HEIGHT - 1 else (0.8 if y == 1 or y == HEIGHT - 2 else 1.0)
            r, g, b, a = canvas.getpixel((x, y))
            shade = edge * (1.0 - scrim)
            canvas.putpixel((x, y), (int(r * shade), int(g * shade), int(b * shade), a))
    return canvas


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, sources in HEADERS.items():
        banner = grade(compose([load_tile(source) for source in sources]))
        path = OUT / f"{name}.png"
        banner.save(path)
        print(f"wrote {path} from {' + '.join(sources)}")


if __name__ == "__main__":
    main()
