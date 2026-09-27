#!/usr/bin/env python3
"""Generate the creative-tab header banners (162x18) for the natural tab.

Each banner is a miniature diorama of the biome it names -- what that stretch of the
ocean actually looks like, at the height of one inventory row:

    middle_level_ocean  sunlight shafts over the reef floor, dropping into a trench
    coral_forest        ringed coral trees standing on the reef
    jelly_jungle        glowing jellies under hanging seaweed
    brine_mirror_gorge  halite terraces of the gorge above a mirror pool
    brimstone_caldera   a smoking vent, its lava river and glowing fissures
    crystal_nest        the hanging crystal lattice
    structures          the things that grow nowhere: pipes, glass, nets

Scenes are painted on a 3x canvas and area-downscaled for smooth silhouettes, then
the title scrim and row edges are laid on top. Output is exactly 162x18 (one row of
the creative grid is 9 x 18px slot cells) and fully opaque, because the banner is
drawn over slot cells.

Usage: python3 scripts/GenerateCreativeTabHeaderTextures.py
"""

from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path("src/main/resources/assets/aquanaut/textures/gui/creative_tab")

WIDTH = 162
HEIGHT = 18
SS = 3  # supersample factor for the painted scene
W3 = WIDTH * SS
H3 = HEIGHT * SS

RNG = random.Random(0xA07A1)


# ---------------------------------------------------------------- utilities

def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def smooth(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return t * t * (3.0 - 2.0 * t)


def mix(c1, c2, t: float):
    return tuple(int(round(lerp(c1[i], c2[i], t))) for i in range(3))


def vgrad(img: Image.Image, stops) -> None:
    """Paint a vertical gradient; stops are ((t, rgb), ...) with t from top 0 to bottom 1."""
    px = img.load()
    for y in range(H3):
        t = y / (H3 - 1)
        for i in range(len(stops) - 1):
            (t1, c1), (t2, c2) = stops[i], stops[i + 1]
            if t1 <= t <= t2:
                row = mix(c1, c2, (t - t1) / (t2 - t1))
                break
        else:
            row = stops[-1][1]
        for x in range(W3):
            px[x, y] = row


def glow(img: Image.Image, cx: float, cy: float, radius: float, color, strength: float = 1.0) -> None:
    """Additive radial glow, painted in place."""
    px = img.load()
    r = int(radius) + 1
    for y in range(max(0, int(cy) - r), min(H3, int(cy) + r + 1)):
        for x in range(max(0, int(cx) - r), min(W3, int(cx) + r + 1)):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d > radius:
                continue
            fall = smooth(1.0 - d / radius) * strength
            old = px[x, y]
            px[x, y] = tuple(min(255, int(old[i] + color[i] * fall)) for i in range(3))


def sparkles(img: Image.Image, count: int, area, color, radius: float = 1.4) -> None:
    x0, y0, x1, y1 = area
    for _ in range(count):
        glow(img, RNG.uniform(x0, x1), RNG.uniform(y0, y1), radius, color, RNG.uniform(0.4, 0.9))


def wave(y: float, x: float, *terms) -> float:
    return y + sum(amp * math.sin(x / period + phase) for period, amp, phase in terms)


def box(d: ImageDraw.ImageDraw, x0: float, y0: float, x1: float, y1: float, fill) -> None:
    d.rectangle((min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1)), fill=fill)


# ---------------------------------------------------------------- scenes

def middle_level_ocean(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (54, 128, 162)), (0.5, (20, 66, 100)), (1.0, (8, 28, 52))))

    shafts = Image.new("RGBA", (W3, H3), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shafts)
    for x0, w, a in ((60, 54, 48), (92, 18, 30), (250, 64, 40), (284, 20, 26), (410, 40, 32)):
        sd.polygon([(x0, -4), (x0 + w, -4), (x0 + w + 44, H3), (x0 + 44, H3)], fill=(190, 228, 255, a))
    img.paste(Image.alpha_composite(img.convert("RGBA"), shafts).convert("RGB"))

    # the reef floor runs across and drops into a dark trench at the right
    def floor_y(x: float) -> float:
        return wave(39.0, x, (34.0, 2.6, 0.4), (11.0, 1.3, 2.1))

    def drop_y(x: float) -> float:
        return floor_y(x) + smooth((x - 310) / 110.0) * 40

    d.polygon([(x, drop_y(x)) for x in range(0, W3 + 20, 5)] + [(W3, H3), (0, H3)], fill=(56, 50, 56))
    d.polygon([(x, floor_y(x)) for x in range(0, 330, 5)] + [(330, H3), (0, H3)], fill=(178, 152, 110))
    for depth, tone in ((6, (144, 118, 84)), (12, (116, 94, 68)), (18, (92, 74, 56))):
        d.line([(x, floor_y(x) + depth) for x in range(0, 326, 5)], fill=tone, width=2)
    for x, y in ((44, 45), (140, 43), (206, 47), (286, 44)):
        d.ellipse((x, y, x + 10, y + 5), fill=(128, 106, 76))
    d.ellipse((216, 42, 322, 54), fill=(104, 92, 68))
    sparkles(img, 24, (0, 0, W3, 38), (170, 216, 240), 1.2)


def coral_tree(d: ImageDraw.ImageDraw, x: float, top: float, body, ring, tip) -> None:
    """A ringed coral: banded trunk, stubby branches, lit tips."""
    base = H3 - 8
    box(d, x - 3, top, x + 3, base, body)
    for y in range(int(top) + 2, int(base), 5):
        box(d, x - 3, y, x + 3, y + 1, ring)
    for sign, dy in ((-1, 12), (1, 18), (-1, 25)):
        if top + dy < base - 2:
            box(d, x + sign * 3, top + dy, x + sign * 11, top + dy + 3, body)
            box(d, x + sign * 3, top + dy, x + sign * 7, top + dy + 1, ring)
            box(d, x + sign * 10, top + dy - 1, x + sign * 12, top + dy + 2, tip)


def coral_forest(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (38, 108, 134)), (0.6, (16, 56, 82)), (1.0, (10, 34, 54))))

    d.polygon([(x, wave(45.0, x, (40.0, 2.2, 1.0))) for x in range(0, W3 + 20, 8)] + [(W3, H3), (0, H3)],
              fill=(36, 46, 56))
    for x, w, h in ((60, 18, 5), (212, 22, 6), (368, 16, 4)):  # brain coral heads
        d.ellipse((x, 46 - h, x + w, 48 + h), fill=(52, 66, 78))
    coral_tree(d, 118, 30, (52, 122, 84), (86, 168, 116), (140, 226, 160))
    coral_tree(d, 258, 16, (150, 74, 66), (202, 110, 96), (255, 168, 140))
    coral_tree(d, 330, 10, (70, 92, 168), (108, 136, 216), (168, 200, 255))
    coral_tree(d, 400, 20, (122, 76, 168), (166, 112, 216), (214, 172, 255))
    coral_tree(d, 458, 24, (58, 150, 150), (94, 202, 200), (150, 240, 238))

    for x, y in ((84, 12), (226, 28), (372, 36), (300, 8)):  # fish drifting between the trees
        d.ellipse((x, y, x + 13, y + 5), fill=(172, 216, 234))
        d.polygon([(x, y + 2), (x - 6, y - 2), (x - 6, y + 6)], fill=(172, 216, 234))
    sparkles(img, 10, (0, 0, W3, 30), (170, 216, 240), 1.1)


def seaweed(d: ImageDraw.ImageDraw, x: float, length: float) -> None:
    pts = [(x + 6 * math.sin(y / 9.0 + x), y) for y in range(0, int(length), 3)]
    d.line(pts, fill=(30, 92, 62), width=7)
    d.line([(px + 1, py) for px, py in pts], fill=(52, 132, 88), width=2)


def jelly(d: ImageDraw.ImageDraw, x: float, y: float, r: float, body, edge) -> None:
    d.pieslice((x - r, y - r, x + r, y + r), 180, 360, fill=body)
    box(d, x - r, y - 1, x + r, y + 1, body)
    d.arc((x - r, y - r, x + r, y + r), 180, 360, fill=edge, width=2)
    for k in (-1, 0, 1):
        tx = x + k * (r * 0.55)
        d.line([(tx, y + 1), (tx + 2 * k, y + 4 + abs(k) * 2), (tx - k, y + 8 + abs(k) * 2)],
               fill=body, width=2)


def jelly_jungle(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (26, 82, 90)), (0.55, (14, 52, 62)), (1.0, (8, 28, 38))))

    for x, length in ((40, 40), (146, 30), (286, 44), (338, 26), (452, 36)):
        seaweed(d, x, length)

    jellies = ((124, 32, 10, (198, 82, 98), (240, 130, 142)),
               (242, 30, 13, (78, 190, 200), (130, 232, 238)),
               (382, 38, 11, (202, 168, 82), (238, 212, 132)),
               (438, 16, 6, (198, 210, 214), (238, 246, 248)))
    for x, y, r, body, edge in jellies:
        glow(img, x, y - r * 0.3, r * 2.5, body, 0.34)
        jelly(d, x, y, r, body, edge)

    sparkles(img, 26, (0, 0, W3, H3 - 12), (160, 220, 220), 1.2)


def ledge(d: ImageDraw.ImageDraw, x0: float, x1: float, top: float) -> None:
    """One halite terrace: bright crust on top, varve-banded face below."""
    face, varve, crust = (238, 228, 208), (174, 156, 138), (252, 248, 238)
    d.rectangle((x0, top, x1, H3), fill=face)
    d.rectangle((x0, top, x1, top + 2), fill=crust)
    for y in range(int(top) + 5, H3, 5):
        d.rectangle((x0, y, x1, y + 1), fill=varve)


def brine_mirror_gorge(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (206, 198, 228)), (0.45, (128, 110, 164)), (1.0, (66, 52, 104))))

    # the gorge: halite terraces closing in from both sides over the mirror pool
    ledge(d, 0, 140, 10)
    ledge(d, 0, 104, 22)
    ledge(d, 0, 70, 32)
    ledge(d, 356, W3, 8)
    ledge(d, 392, W3, 20)
    ledge(d, 428, W3, 30)

    d.rectangle((128, 38, 386, H3), fill=(104, 90, 148))   # the pool, still and bright
    d.rectangle((128, 38, 386, 40), fill=(236, 240, 252))
    d.rectangle((128, 40, 386, 42), fill=(186, 192, 226))
    for x in range(132, 380, 22):                          # the walls, smeared in it
        d.rectangle((x, 42, x + 12, 50), fill=(196, 202, 234))
        d.rectangle((x + 3, 42, x + 6, 52), fill=(160, 166, 208))
    d.rectangle((128, 49, 386, H3), fill=(84, 70, 130))    # shadow where wall meets water
    for x in range(140, 380, 30):
        glow(img, x, 40, 2.2, (255, 255, 255), 0.9)        # speculars on the surface

    for x, top, h in ((120, 10, 12), (176, 10, 8), (372, 8, 12), (414, 8, 8)):  # halite spikes
        d.polygon([(x - 5, top), (x + 5, top), (x, top - h)], fill=(242, 236, 252))
        d.line([(x, top - h), (x + 5, top)], fill=(255, 255, 255), width=1)
    for x, y in ((86, 26), (232, 30), (440, 24)):           # gypsum roses
        d.line([(x - 4, y), (x + 4, y)], fill=(196, 168, 216), width=1)
        d.line([(x, y - 4), (x, y + 4)], fill=(196, 168, 216), width=1)
    glow(img, 256, 36, 34, (110, 92, 150), 0.35)
    sparkles(img, 10, (128, 36, 386, 48), (255, 255, 255), 1.5)


def brimstone_caldera(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (88, 42, 30)), (0.45, (48, 22, 26)), (1.0, (16, 9, 14))))

    def ridge_y(x: float) -> float:
        return wave(30.0, x, (70.0, 8.0, 0.6), (20.0, 2.6, 2.2))

    d.polygon([(x, ridge_y(x)) for x in range(0, W3 + 20, 5)] + [(W3, H3), (0, H3)], fill=(42, 24, 31))
    for x0, x1, y in ((20, 96, 40), (140, 214, 44), (232, 300, 46)):   # cooling rock shelves
        d.polygon([(x0, y), (x1, y - 4), (x1, H3), (x0, H3)], fill=(32, 18, 26))
        d.line([(x0, y), (x1, y - 4)], fill=(96, 48, 34), width=1)

    d.polygon([(330, ridge_y(330)), (360, 12), (392, 12), (422, ridge_y(422))], fill=(30, 17, 24))
    glow(img, 376, 13, 18, (255, 122, 36), 0.9)
    d.ellipse((354, 8, 398, 18), fill=(255, 156, 52))
    d.ellipse((364, 10, 388, 15), fill=(255, 226, 120))

    river = [(376, 16), (368, 26), (352, 34), (330, 42), (300, 50), (268, H3)]
    d.line(river, fill=(255, 138, 30), width=7)
    d.line(river, fill=(255, 214, 74), width=2)
    for x0, y0 in ((110, 44), (200, 48), (66, 38)):         # fissures across the front rock
        pts = [(x0, y0)] + [(x0 + k * 10 + RNG.randint(-2, 2), y0 - k * 3 + RNG.randint(-2, 2))
                            for k in range(1, 4)]
        d.line(pts, fill=(214, 96, 26), width=2)
    d.line([(x, ridge_y(x)) for x in range(230, 430, 4)], fill=(172, 82, 30), width=1)

    for x, y in ((352, 8), (392, 5), (338, 20), (412, 12), (370, 2)):
        glow(img, x, y, 3.2, (255, 150, 50), 0.9)
    for x, y in ((52, 32), (150, 40), (250, 44), (448, 32)):
        d.line([(x, y), (x + 13, y - 3)], fill=(198, 206, 78), width=2)
    sparkles(img, 12, (300, 0, W3, 20), (150, 120, 110), 3.0)


def prism(d: ImageDraw.ImageDraw, x: float, top: float, half: float, length: float, body, edge) -> None:
    d.polygon([(x - half, top), (x + half, top), (x, top + length)], fill=body)
    d.line([(x - half, top), (x, top + length)], fill=edge, width=1)


def crystal_nest(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (52, 36, 100)), (0.5, (28, 20, 62)), (1.0, (12, 9, 32))))

    for x0 in (10, 120, 230, 340, 440):                     # the lattice they hang from
        pts = [(x0, 0), (x0 + 70, 18), (x0 + 30, 42)]
        d.line(pts, fill=(96, 72, 172), width=1)
        for p in pts[1:]:
            glow(img, p[0], p[1], 2.2, (140, 110, 220), 0.6)

    clusters = ((110, 0, 10, 34, (208, 108, 158), (246, 168, 206)),
                (242, 0, 13, 44, (140, 84, 208), (198, 150, 242)),
                (330, 0, 8, 26, (128, 128, 152), (188, 188, 212)),
                (412, 0, 11, 36, (92, 208, 112), (158, 242, 168)))
    for x, top, half, length, body, edge in clusters:
        prism(d, x - half - 7, top, half * 0.6, length * 0.55, mix(body, (20, 14, 40), 0.3), edge)
        prism(d, x, top, half, length, body, edge)
        glow(img, x, top + length, 8, body, 0.8)

    for x, half, length in ((64, 7, 13), (82, 4, 8), (298, 8, 15), (316, 4, 9)):  # grown from below
        d.polygon([(x - half, H3), (x + half, H3), (x, H3 - length)],
                  fill=mix((140, 110, 200), (20, 14, 40), 0.35))
        glow(img, x, H3 - length, 4, (150, 120, 220), 0.6)
    sparkles(img, 18, (0, 0, W3, H3), (200, 200, 255), 1.2)


def structures(img: Image.Image, d: ImageDraw.ImageDraw) -> None:
    vgrad(img, ((0.0, (54, 64, 76)), (0.5, (34, 42, 52)), (1.0, (18, 22, 28))))

    d.line([(90, 0), (90, 20)], fill=(70, 80, 92), width=2)          # a hanging bracket
    box(d, 78, 20, 102, 24, (96, 106, 120))
    d.rectangle((0, 24, W3, 33), fill=(86, 96, 108))                 # the pipe run
    d.rectangle((0, 24, W3, 26), fill=(124, 136, 150))
    for x in range(14, W3, 46):
        d.rectangle((x, 27, x + 2, 29), fill=(66, 74, 86))           # rivets
    for x in (110, 292):
        box(d, x, 21, x + 12, 36, (112, 122, 136))
        box(d, x + 2, 21, x + 4, 36, (146, 156, 170))
    d.rectangle((178, 22, 252, 35), fill=(132, 194, 210))            # plexiglass joint
    d.line([(182, 33), (210, 23)], fill=(226, 246, 252), width=2)
    d.line([(216, 33), (238, 25)], fill=(226, 246, 252), width=1)
    for x in range(330, W3, 12):                                     # netting
        d.line([(x, 6), (x + 18, 22)], fill=(72, 116, 96), width=1)
        d.line([(x, 22), (x + 18, 6)], fill=(72, 116, 96), width=1)
    d.rectangle((372, 40, W3, H3), fill=(62, 70, 82))                # the workbench
    d.rectangle((372, 40, W3, 42), fill=(88, 98, 112))
    glow(img, 452, 14, 11, (232, 176, 74), 1.0)                      # its lamp
    glow(img, 452, 44, 14, (232, 176, 74), 0.35)


# ---------------------------------------------------------------- output

def finish(img: Image.Image) -> Image.Image:
    img = img.convert("RGB").resize((WIDTH, HEIGHT), Image.BOX)
    px = img.load()
    for y in range(HEIGHT):
        edge = 0.5 if y in (0, HEIGHT - 1) else (0.82 if y in (1, HEIGHT - 2) else 1.0)
        for x in range(WIDTH):
            # the title reads over the left third: darken behind it, let the scene breathe right
            scrim = lerp(0.52, 0.10, smooth(x / 104.0)) if x < 104 else 0.10
            r, g, b = px[x, y]
            r, g, b = ((c - 128) * 1.12 + 128 for c in (r, g, b))  # a touch of contrast
            shade = edge * (1.0 - scrim)
            px[x, y] = tuple(max(0, min(255, int(c * shade))) for c in (r, g, b))
    return img


SCENES = {
    "middle_level_ocean": middle_level_ocean,
    "coral_forest": coral_forest,
    "jelly_jungle": jelly_jungle,
    "brine_mirror_gorge": brine_mirror_gorge,
    "brimstone_caldera": brimstone_caldera,
    "crystal_nest": crystal_nest,
    "structures": structures,
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, scene in SCENES.items():
        canvas = Image.new("RGB", (W3, H3))
        scene(canvas, ImageDraw.Draw(canvas))
        banner = finish(canvas)
        path = OUT / f"{name}.png"
        banner.save(path)
        print(f"wrote {path}")


if __name__ == "__main__":
    main()
