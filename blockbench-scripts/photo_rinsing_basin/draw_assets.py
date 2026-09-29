"""Sprites and block textures for the photo rinsing basin workflow.

Palette deliberately samples the dissection-table steel ramp
(#C2C7CC light, #B0B6BD, #9BA1A8 mid, #7A8087 dark, #3A3D40 accent)
so the two stations read as the same equipment family.
"""

import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ITEMS = ROOT / "src/main/resources/assets/aquanaut/textures/item"
BLOCKS = ROOT / "src/main/resources/assets/aquanaut/textures/block"


def canvas(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def rect(px, x1, y1, x2, y2, color):
    for y in range(y1, y2):
        for x in range(x1, x2):
            px[x, y] = color


def clamp(value):
    return max(0, min(255, value))


def save(image, folder, name):
    folder.mkdir(parents=True, exist_ok=True)
    image.save(folder / f"{name}.png")
    print(f"wrote {folder / (name + '.png')} ({image.width}x{image.height})")


# ---------------------------------------------------------------- photographs
# Both are horizontal (landscape) prints with the same silhouette, so an
# exposed sheet and a finished print are recognisably the same object.

SHADOW = (35, 47, 47, 140)
EDGE = (113, 115, 105, 255)
PAPER = (232, 219, 194, 255)
PEARL = (255, 242, 216, 255)
SEA0 = (13, 43, 55, 255)
SEA1 = (24, 85, 101, 255)
SEA2 = (65, 144, 151, 255)

img = canvas()
p = img.load()
rect(p, 2, 5, 16, 16, SHADOW)
rect(p, 1, 3, 15, 14, EDGE)
rect(p, 2, 4, 14, 13, PAPER)
rect(p, 2, 4, 14, 5, PEARL)
rect(p, 3, 5, 13, 11, SEA0)
rect(p, 4, 6, 13, 9, SEA1)
rect(p, 8, 6, 13, 8, SEA2)
rect(p, 3, 9, 13, 11, (10, 34, 42, 255))
p[4, 9] = (202, 146, 116, 255)
p[5, 8] = (237, 192, 151, 255)
p[6, 9] = (202, 146, 116, 255)
p[5, 9] = PEARL
p[9, 8] = (120, 196, 190, 255)
p[10, 7] = (151, 216, 205, 255)
rect(p, 3, 11, 8, 12, (164, 154, 135, 255))
rect(p, 3, 12, 6, 13, (193, 179, 153, 255))
p[13, 13] = EDGE
save(img, ITEMS, "developed_photo")

img = canvas()
p = img.load()
FILM_EDGE = (46, 50, 47, 255)
FILM_BASE = (20, 35, 42, 255)
LATENT0 = (30, 74, 84, 255)
LATENT1 = (42, 102, 110, 255)
SPROCKET = (126, 138, 132, 255)
rect(p, 2, 5, 16, 16, SHADOW)
rect(p, 1, 3, 15, 14, FILM_EDGE)
rect(p, 2, 4, 14, 13, FILM_BASE)
rect(p, 2, 4, 14, 5, (52, 62, 60, 255))
rect(p, 3, 5, 13, 11, (16, 30, 36, 255))
rect(p, 4, 6, 13, 9, LATENT0)
rect(p, 8, 6, 13, 8, LATENT1)
rect(p, 3, 9, 13, 11, (12, 24, 30, 255))
p[4, 9] = (94, 72, 60, 255)
p[5, 8] = (112, 88, 70, 255)
p[6, 9] = (94, 72, 60, 255)
p[9, 8] = (72, 128, 126, 255)
p[10, 7] = (92, 148, 142, 255)
rect(p, 3, 11, 8, 12, (58, 66, 62, 255))
rect(p, 3, 12, 6, 13, (44, 52, 50, 255))
for x in (3, 5, 7, 9, 11, 13):
    p[x, 4] = SPROCKET
    p[x, 12] = (72, 82, 78, 255)
save(img, ITEMS, "exposed_film")


# ------------------------------------------------------------ block textures
# The block atlas and its model UVs are generated together by build_block.py, which keeps a
# single 16x16 texture in step with the model's face rectangles. Nothing to emit here.


# ------------------------------------------------------------- basin item sprite
img = canvas()
p = img.load()
DARK = (66, 72, 78, 255)
DARK2 = (46, 51, 56, 255)
STEEL = (155, 163, 172, 255)
STEEL_L = (198, 205, 212, 255)
STEEL_D = (112, 120, 128, 255)
WATER = (52, 138, 150, 255)
WATER_L = (108, 190, 190, 255)

# faucet: post + spout over the bath
rect(p, 6, 1, 8, 5, DARK2)
rect(p, 6, 2, 8, 3, STEEL_D)
rect(p, 6, 1, 11, 3, DARK2)
rect(p, 7, 2, 11, 3, STEEL_D)
rect(p, 10, 3, 11, 5, DARK)

# basin rim (light steel) and the rinse visible inside it
rect(p, 1, 4, 15, 7, STEEL)
rect(p, 1, 4, 15, 5, STEEL_L)
rect(p, 2, 5, 14, 7, DARK2)
rect(p, 3, 5, 13, 6, WATER)
rect(p, 4, 5, 11, 6, WATER_L)
rect(p, 1, 6, 15, 7, STEEL_D)

# cabinet body
rect(p, 2, 7, 14, 13, STEEL)
rect(p, 2, 7, 14, 8, STEEL_L)
rect(p, 3, 9, 13, 10, STEEL_D)
rect(p, 3, 10, 13, 11, DARK)
rect(p, 7, 9, 9, 11, DARK2)
rect(p, 2, 12, 14, 13, STEEL_D)

# plinth
rect(p, 1, 13, 15, 15, DARK)
rect(p, 1, 13, 15, 14, DARK2)
p[3, 14] = STEEL_D
p[12, 14] = STEEL_D
save(img, ITEMS, "photo_rinsing_basin")


# ------------------------------------------------------- film and developing salts
# The same film stock as the exposed sheet: identical outline, sprocket run and edge rails, so an
# unshot sheet and an exposed one read as one object in two states. Only the emulsion differs —
# here it is still blank, where the exposed sheet carries a latent image.
img = canvas()
p = img.load()
rect(p, 2, 5, 16, 16, SHADOW)
rect(p, 1, 3, 15, 14, FILM_EDGE)
rect(p, 2, 4, 14, 13, FILM_BASE)
rect(p, 2, 4, 14, 5, (52, 62, 60, 255))
rect(p, 3, 5, 13, 11, (150, 196, 190, 255))
rect(p, 4, 6, 13, 9, (196, 232, 222, 255))
rect(p, 8, 6, 13, 8, (232, 248, 240, 255))
rect(p, 3, 9, 13, 11, (172, 210, 202, 255))
rect(p, 3, 11, 8, 12, (58, 66, 62, 255))
rect(p, 3, 12, 6, 13, (44, 52, 50, 255))
for x in (3, 5, 7, 9, 11, 13):
    p[x, 4] = SPROCKET
    p[x, 12] = (72, 82, 78, 255)
save(img, ITEMS, "photosensitive_film")

# Brine/sulfur developing salts in a waxed submarine sachet.
img = canvas()
p = img.load()
rect(p, 4, 3, 13, 14, (43, 52, 50, 140))
rect(p, 3, 2, 12, 13, (181, 157, 112, 255))
rect(p, 4, 3, 11, 12, (220, 196, 141, 255))
rect(p, 4, 3, 11, 5, (237, 221, 175, 255))
rect(p, 5, 6, 10, 10, (72, 126, 126, 255))
p[6, 7] = (213, 214, 151, 255)
p[8, 8] = (237, 218, 104, 255)
p[9, 7] = (196, 234, 211, 255)
rect(p, 5, 11, 10, 12, (128, 104, 78, 255))
p[3, 2] = (245, 225, 183, 255)
p[11, 12] = (97, 78, 63, 255)
save(img, ITEMS, "brine_developing_salts")
