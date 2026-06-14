#!/usr/bin/env python3
"""Generate a seaweed variant with light-aqua glowing fruit plus glowmask.

The output is derived from the existing tileable seaweed block texture so the
variant stays coherent at chunk boundaries. The glowmask uses the same fruit
placements, but only emits the emissive regions.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Tuple

from PIL import Image

SIZE = 16
IN_PATH = Path("src/main/resources/assets/aquanaut/textures/block/seaweed.png")
OUT_VARIANT = Path("src/main/resources/assets/aquanaut/textures/block/seaweed_fruit.png")
OUT_GLOW = Path("src/main/resources/assets/aquanaut/textures/block/seaweed_fruit_glowmask.png")

RGBA = Tuple[int, int, int, int]

FRUIT_CORE: RGBA = (205, 255, 250, 255)
FRUIT_LIGHT: RGBA = (131, 241, 236, 255)
FRUIT_DEEP: RGBA = (56, 168, 171, 255)
FRUIT_GLOW: RGBA = (92, 186, 183, 255)
STEM: RGBA = (63, 108, 77, 255)


@dataclass(frozen=True)
class Fruit:
    cx: float
    cy: float
    rx: float
    ry: float
    bud: float
    offset: float


FRUITS = (
    Fruit(1.2, 1.4, 1.35, 1.15, 0.86, -0.10),
    Fruit(6.7, 1.1, 1.45, 1.20, 0.82, 0.18),
    Fruit(12.8, 2.8, 1.30, 1.05, 0.78, -0.08),
    Fruit(3.7, 6.0, 1.25, 1.08, 0.90, 0.04),
    Fruit(9.1, 6.7, 1.55, 1.30, 0.84, -0.14),
    Fruit(14.0, 8.3, 1.20, 1.00, 0.76, 0.10),
    Fruit(2.0, 11.1, 1.35, 1.12, 0.80, -0.04),
    Fruit(7.8, 12.7, 1.50, 1.22, 0.88, 0.12),
    Fruit(13.2, 14.2, 1.25, 1.05, 0.76, -0.06),
)


def load_image(path: Path) -> Image.Image:
    return Image.open(path).convert("RGBA")


def save_image(path: Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def wrap_delta(a: float, b: float) -> float:
    d = a - b
    while d <= -SIZE / 2:
        d += SIZE
    while d > SIZE / 2:
        d -= SIZE
    return d


def torus_distance(px: float, py: float, cx: float, cy: float, rx: float, ry: float) -> float:
    dx = wrap_delta(px, cx) / rx
    dy = wrap_delta(py, cy) / ry
    return dx * dx + dy * dy


def draw_fruit(variant: Image.Image, glow: Image.Image, fruit: Fruit) -> None:
    # A slightly elongated fruit pod with a small stem and a brighter cap.
    for y in range(SIZE):
        for x in range(SIZE):
            dist = torus_distance(x + 0.5, y + 0.5, fruit.cx, fruit.cy, fruit.rx, fruit.ry)
            if dist > 1.0:
                continue

            radial = dist ** 0.5
            shell = dist <= 0.82
            core = dist <= 0.42
            highlight = dist <= 0.18

            if shell:
                color = FRUIT_DEEP
            elif core:
                color = FRUIT_LIGHT
            else:
                color = FRUIT_CORE

            if highlight:
                color = FRUIT_CORE

            if shell:
                variant.putpixel((x, y), color)
                glow.putpixel((x, y), FRUIT_GLOW)

    # A tiny stem / attachment point gives the fruit a more natural read.
    stem_dx = 0.65
    stem_dy = -0.55
    stem_x = fruit.cx + stem_dx
    stem_y = fruit.cy + stem_dy
    for y in range(SIZE):
        for x in range(SIZE):
            dist = torus_distance(x + 0.5, y + 0.5, stem_x, stem_y, 0.55, 0.55)
            if dist <= 0.95:
                variant.putpixel((x, y), STEM)
                glow.putpixel((x, y), FRUIT_GLOW)

    # Small bright cap for the glowmask and the visible variant.
    cap_x = int(round(fruit.cx))
    cap_y = int(round(fruit.cy - 1))
    for dy in (-1, 0):
        for dx in (-1, 0, 1):
            x = cap_x + dx
            y = cap_y + dy
            if 0 <= x < SIZE and 0 <= y < SIZE:
                if ((dx * dx) + (dy * dy)) <= 2:
                    variant.putpixel((x, y), FRUIT_CORE)
                    glow.putpixel((x, y), FRUIT_GLOW)


def validate_alpha(image: Image.Image) -> None:
    for alpha in image.getchannel("A").tobytes():
        if alpha not in (0, 255):
            raise ValueError("Unexpected semitransparent pixel in output")


def main() -> None:
    base = load_image(IN_PATH)
    variant = base.copy()
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    for fruit in FRUITS:
        draw_fruit(variant, glow, fruit)

    validate_alpha(variant)
    validate_alpha(glow)
    save_image(OUT_VARIANT, variant)
    save_image(OUT_GLOW, glow)
    print(f"Wrote {OUT_VARIANT}")
    print(f"Wrote {OUT_GLOW}")


if __name__ == "__main__":
    main()
