#!/usr/bin/env python3
"""Generate the Crystal Nest (水晶巢) textures.

Every sprite is drawn procedurally from a small crystal-physics vocabulary:

* faceted prisms with parallel shafts and pyramidal terminations (the wall crystals),
* cut gems with banded facets (the life gems),
* druzy crusts (the chamber linings) over plain slate-blue rock (晶巢岩 — the nest stone
  is rock, never crystal; the crystals are their own sprites),
* filamentous algae mats and tufts (the green carpet over every surface),
* hanging crystal fringe (the tinsel draping from chamber ceilings).

Crystals are drawn the way real ones photograph: dark terminator facets against the
light, one narrow specular core per prism, saturated bases where the crystal grows out
of the rock, and white-hot tips. The two luminous kinds get a _glowmask derived from the
core geometry, so the mask can never drift out of alignment with the sprite.
"""

from __future__ import annotations

import random
import struct
import zlib
from pathlib import Path
from typing import Dict, List, Tuple

BLOCK = Path("src/main/resources/assets/aquanaut/textures/block")
RGBA = Tuple[int, int, int, int]
Grid = List[List[RGBA]]

TRANSPARENT: RGBA = (0, 0, 0, 0)

# --- PNG I/O (same conventions as the other generators) ----------------------

def write_png(path: Path, grid: Grid) -> None:
    height = len(grid)
    width = len(grid[0])
    raw = bytearray()
    for row in grid:
        raw.append(0)
        for r, g, b, a in row:
            raw.append(max(0, min(255, int(r))))
            raw.append(max(0, min(255, int(g))))
            raw.append(max(0, min(255, int(b))))
            raw.append(max(0, min(255, int(a))))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (struct.pack(">I", len(payload)) + tag + payload
                + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                     + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    print(f"wrote {path}")


# --- tiny color kit ----------------------------------------------------------

def clamp8(v: float) -> int:
    return max(0, min(255, int(round(v))))


def shade(c: RGBA, factor: float) -> RGBA:
    return (clamp8(c[0] * factor), clamp8(c[1] * factor), clamp8(c[2] * factor), c[3])


def mix(a: RGBA, b: RGBA, t: float) -> RGBA:
    return (clamp8(a[0] + (b[0] - a[0]) * t), clamp8(a[1] + (b[1] - a[1]) * t),
            clamp8(a[2] + (b[2] - a[2]) * t), clamp8(a[3] + (b[3] - a[3]) * t))


class Canvas:
    def __init__(self, size: int = 16) -> None:
        self.grid: Grid = [[TRANSPARENT for _ in range(size)] for _ in range(size)]
        self.size = size
        self.glow: Grid = [[TRANSPARENT for _ in range(size)] for _ in range(size)]

    def px(self, x: int, y: int, c: RGBA) -> None:
        if 0 <= x < self.size and 0 <= y < self.size:
            self.grid[y][x] = c

    def get(self, x: int, y: int) -> RGBA:
        if 0 <= x < self.size and 0 <= y < self.size:
            return self.grid[y][x]
        return TRANSPARENT

    def glow_px(self, x: int, y: int, c: RGBA) -> None:
        if 0 <= x < self.size and 0 <= y < self.size:
            self.glow[y][x] = (c[0], c[1], c[2], 255)


# --- crystal vocabulary ------------------------------------------------------

Palette = Dict[str, RGBA]

PALETTES: Dict[str, Palette] = {
    "white": {
        "deep": (94, 108, 126, 255), "dark": (150, 164, 182, 255), "base": (200, 212, 226, 255),
        "light": (232, 240, 248, 255), "hi": (252, 253, 255, 255), "tip": (255, 255, 255, 255),
    },
    "rose": {
        "deep": (118, 56, 82, 255), "dark": (184, 104, 136, 255), "base": (228, 150, 178, 255),
        "light": (246, 192, 212, 255), "hi": (255, 226, 238, 255), "tip": (255, 244, 248, 255),
    },
    "amethyst": {
        "deep": (64, 40, 108, 255), "dark": (112, 76, 166, 255), "base": (158, 118, 208, 255),
        "light": (198, 166, 234, 255), "hi": (232, 212, 250, 255), "tip": (248, 240, 255, 255),
    },
    "aqua": {
        "deep": (26, 82, 96, 255), "dark": (66, 150, 164, 255), "base": (116, 200, 210, 255),
        "light": (170, 232, 238, 255), "hi": (216, 248, 250, 255), "tip": (240, 254, 255, 255),
    },
    "smoky": {
        "deep": (46, 40, 46, 255), "dark": (88, 78, 86, 255), "base": (132, 120, 128, 255),
        "light": (172, 162, 168, 255), "hi": (210, 202, 206, 255), "tip": (232, 226, 228, 255),
    },
    "resonant": {
        "deep": (28, 84, 110, 255), "dark": (80, 168, 190, 255), "base": (140, 216, 230, 255),
        "light": (192, 240, 248, 255), "hi": (228, 252, 255, 255), "tip": (255, 255, 255, 255),
        "glow": (150, 238, 255, 255),
    },
    "life": {
        "deep": (22, 82, 44, 255), "dark": (56, 150, 84, 255), "base": (104, 200, 130, 255),
        "light": (158, 232, 176, 255), "hi": (206, 248, 214, 255), "tip": (240, 255, 242, 255),
        "glow": (140, 255, 164, 255),
    },
}


def draw_prism(canvas: Canvas, x0: float, base_y: int, height: int, half_w: float,
               pal: Palette, lean: float = 0.0, glow: bool = False) -> None:
    """One faceted crystal prism growing upward from (x0, base_y).

    Parallel shaft over the lower ~55%, pyramidal termination above it, three facet
    columns (terminator / specular core / body), and a darker root where it meets rock.
    """
    for r in range(height):
        t = r / max(1, height - 1)
        y = base_y - r
        width = half_w if t < 0.55 else half_w * max(0.12, (1.0 - (t - 0.55) / 0.45) ** 0.9)
        x = x0 + lean * (t ** 1.6) * height * 0.16
        left = int(round(x - width))
        right = int(round(x + width))
        for col in range(left, right + 1):
            u = (col - left) / max(1, right - left)
            if u < 0.30:
                c = pal["dark"]
            elif u < 0.52:
                c = pal["light"]
            elif u < 0.62:
                c = pal["hi"]
            else:
                c = pal["base"]
            # faint horizontal growth striations
            if (r + col) % 5 == 0:
                c = shade(c, 0.92)
            if r == 0:
                c = pal["deep"]
            canvas.px(col, y, c)
        # specular core line and hot tip
        core_x = int(round(x - width * 0.35))
        if 0 < r < height - 1:
            canvas.px(core_x, y, pal["hi"])
            if glow:
                canvas.glow_px(core_x, y, pal.get("glow", pal["hi"]))
    tip_x = int(round(x0 + lean * 0.16 * height))
    tip_y = base_y - height + 1
    canvas.px(tip_x, tip_y, pal["tip"])
    canvas.px(tip_x, tip_y + 1, pal["hi"])
    if glow:
        canvas.glow_px(tip_x, tip_y, pal.get("glow", pal["tip"]))
        canvas.glow_px(tip_x, tip_y + 1, pal.get("glow", pal["hi"]))


def draw_gem(canvas: Canvas, cx: int, cy: int, radius: int, pal: Palette, glow: bool = False) -> None:
    """A cut gem: octagonal outline with banded facets and one hot interior glint."""
    for dy in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            # octagon metric
            if abs(dx) + abs(dy) * 0.8 > radius * 1.15 or abs(dx) * 0.8 + abs(dy) > radius * 1.15:
                continue
            edge = max(abs(dx) + abs(dy) * 0.8, abs(dx) * 0.8 + abs(dy)) > radius * 0.92
            band = dy
            if edge:
                c = pal["deep"]
            elif band < -radius * 0.3:
                c = pal["light"]
            elif band < radius * 0.25:
                c = pal["base"]
            else:
                c = pal["dark"]
            canvas.px(cx + dx, cy + dy, c)
    canvas.px(cx - 1, cy - 1, pal["hi"])
    canvas.px(cx, cy - 1, pal["hi"])
    canvas.px(cx - 1, cy, pal["hi"])
    canvas.px(cx + radius - 1, cy + radius - 1, pal["tip"])
    if glow:
        canvas.glow_px(cx - 1, cy - 1, pal.get("glow", pal["hi"]))
        canvas.glow_px(cx, cy - 1, pal.get("glow", pal["hi"]))
        canvas.glow_px(cx - 1, cy, pal.get("glow", pal["hi"]))
        canvas.glow_px(cx, cy, pal.get("glow", pal["base"]))


CLUSTER_LAYOUTS: Dict[str, List[Tuple[float, int, int, float, float]]] = {
    # (x, base_y, height, half_width, lean)
    "white": [(8, 15, 12, 2.2, 0.0), (4, 15, 8, 1.6, -1.0), (12, 15, 9, 1.7, 1.0),
              (6, 15, 5, 1.2, -0.6), (10, 15, 4, 1.1, 0.6)],
    "rose": [(7, 15, 11, 2.1, -0.5), (11, 15, 9, 1.8, 0.8), (4, 15, 6, 1.4, -1.0),
             (9, 15, 5, 1.2, 0.2)],
    "amethyst": [(8, 15, 13, 2.3, 0.2), (5, 15, 8, 1.6, -0.9), (12, 15, 7, 1.5, 0.9),
                 (10, 15, 4, 1.0, 0.5)],
    "aqua": [(8, 15, 11, 2.0, 0.0), (5, 15, 9, 1.7, -0.8), (11, 15, 8, 1.6, 0.9),
             (7, 15, 5, 1.1, -0.3), (12, 15, 4, 1.0, 0.7)],
    "smoky": [(7, 15, 12, 2.2, -0.3), (11, 15, 7, 1.6, 0.7), (4, 15, 6, 1.3, -0.8),
              (10, 15, 10, 1.5, 0.4)],
    "resonant": [(8, 15, 13, 2.0, 0.0), (5, 15, 9, 1.5, -0.8), (11, 15, 10, 1.6, 0.8),
                 (9, 15, 5, 1.0, 0.3)],
}


def crystal_cluster(name: str, material: str) -> None:
    canvas = Canvas()
    pal = PALETTES[material]
    glow = material in ("resonant", "life")
    rng = random.Random(f"cluster-{material}")
    if material == "life":
        for cx, cy, radius in [(8, 12, 3), (4, 13, 2), (12, 13, 2), (6, 15, 2), (11, 15, 1)]:
            draw_gem(canvas, cx, cy, radius, pal, glow)
        # rocky matrix shards under the gems
        for x, y in [(3, 15), (5, 15), (9, 15), (13, 15), (7, 15)]:
            canvas.px(x, y, pal["deep"])
    else:
        for x, base_y, height, half_w, lean in CLUSTER_LAYOUTS[material]:
            draw_prism(canvas, x + rng.uniform(-0.3, 0.3), base_y, height, half_w, pal,
                       lean, glow)
        # rocky matrix at the root
        for x in range(3, 13):
            if rng.random() < 0.5:
                canvas.px(x, 15, PALETTES[material]["deep"])
    write_png(BLOCK / f"{name}.png", canvas.grid)
    if glow:
        write_png(BLOCK / f"{name}_glowmask.png", canvas.glow)


# --- rock vocabulary ---------------------------------------------------------

def hash_grid(seed: str) -> List[List[float]]:
    rng = random.Random(seed)
    return [[rng.random() for _ in range(16)] for _ in range(16)]


def smooth_noise(seed: str) -> List[List[float]]:
    """Two-octave value-ish noise for stone mottling."""
    coarse = hash_grid(seed)
    fine = hash_grid(seed + "-fine")
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            c = coarse[(y // 4) % 16][(x // 4) % 16]
            out[y][x] = c * 0.65 + fine[y][x] * 0.35
    return out


def nest_stone(name: str, seed: str, top: bool) -> None:
    """晶巢岩 is plain rock: slate-blue matrix, mottled grain and fracture seams.

    No crystal imagery at all — the crystals that grow out of this stone are their own
    sprites; the rock stays rock all the way down.
    """
    canvas = Canvas()
    rng = random.Random(seed)
    noise = smooth_noise(seed)
    base = (62, 80, 100, 255)
    for y in range(16):
        for x in range(16):
            value = 0.78 + noise[y][x] * 0.5
            c = shade(base, value)
            canvas.px(x, y, c)
    # fracture seams: two jagged walks in a darker rock tone
    for _ in range(2):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(10):
            canvas.px(x, y, shade(base, 0.55))
            x = (x + rng.choice([-1, 0, 1])) % 16
            y = (y + rng.choice([0, 1])) % 16
    # plain mineral grain: quiet specks in the rock's own tones, never crystal glints
    for _ in range(26 if top else 20):
        x, y = rng.randrange(16), rng.randrange(16)
        canvas.px(x, y, shade(base, rng.choice([0.62, 0.68, 1.18, 1.28])))
    write_png(BLOCK / f"{name}.png", canvas.grid)


def crystal_druse() -> None:
    """Geode lining: dark rind studded with dense druzy crystal teeth."""
    canvas = Canvas()
    rng = random.Random("druse")
    rind = (48, 56, 72, 255)
    noise = smooth_noise("druse-rind")
    for y in range(16):
        for x in range(16):
            canvas.px(x, y, shade(rind, 0.75 + noise[y][x] * 0.55))
    teeth = [PALETTES["white"], PALETTES["aqua"], PALETTES["white"], PALETTES["amethyst"]]
    for _ in range(120):
        x, y = rng.randrange(16), rng.randrange(16)
        pal = teeth[rng.randrange(len(teeth))]
        height = rng.choice([1, 1, 2, 2, 3])
        for r in range(height):
            c = pal["dark"] if r == 0 else (pal["base"] if r < height - 1 else pal["hi"])
            canvas.px(x + (1 if r == height - 1 and rng.random() < 0.4 else 0), y - r, c)
        if rng.random() < 0.30:
            canvas.px(x, y - height, pal["tip"])
    # scattered star glints
    for _ in range(16):
        x, y = rng.randrange(16), rng.randrange(16)
        canvas.px(x, y, PALETTES["white"]["tip"])
    write_png(BLOCK / "crystal_druse.png", canvas.grid)


def crystal_column() -> None:
    """A giant cut prism: five clean shaft facets with quartz phantoms, hexagon on top."""
    import math
    canvas = Canvas()
    rng = random.Random("column-v2")
    ice = (158, 194, 216, 255)
    aqua = PALETTES["aqua"]
    pal = PALETTES["white"]

    # --- shaft: five vertical facets, dark at both silhouette edges, one specular ridge
    facets = [(0, 3, 0.60), (3, 7, 0.86), (7, 9, 1.12), (9, 12, 0.97), (12, 16, 0.68)]
    for y in range(16):
        vertical = 0.92 + y * 0.012  # light falls from above
        for x in range(16):
            factor = next(f for lo, hi, f in facets if lo <= x < hi)
            c = shade(mix(ice, aqua["base"], 0.16), factor * vertical)
            canvas.px(x, y, c)
    # facet edges: crisp one-pixel seams between the five faces
    for x in (3, 7, 9, 12):
        for y in range(16):
            canvas.px(x, y, shade(canvas.get(x, y), 0.78))
    # quartz phantoms: faint inner growth planes running up the shaft
    for x in (5, 10):
        for y in range(16):
            if (y + x) % 2 == 0:
                canvas.px(x, y, mix(canvas.get(x, y), pal["light"], 0.25))
    # growth striations: thin, quiet, never banded across the whole face
    for y in (4, 9, 13):
        for x in range(1, 15):
            if (x * 3 + y) % 5 != 0:
                canvas.px(x, y, shade(canvas.get(x, y), 0.94))
    # the specular ridge glints
    for y in (1, 6, 11):
        canvas.px(8, y, pal["hi"])
    canvas.px(7, 2, mix(canvas.get(7, 2), pal["hi"], 0.6))

    # --- top: a proper hexagonal cross-section, facet wedges and a centre star
    top = Canvas()
    radius = 9.2
    cx = cy = 7.5
    wedges = [1.00, 0.93, 1.05, 0.95, 0.90, 0.97]
    for y in range(16):
        for x in range(16):
            dx = x - cx
            dy = y - cy
            edge_y = radius - 0.5774 * abs(dx)
            inside = abs(dx) <= 0.866 * radius and abs(dy) <= edge_y
            if not inside:
                # the prism's beveled shoulder: dark crystal, still opaque
                top.px(x, y, shade(mix(ice, aqua["deep"], 0.35), 0.42 + rng.random() * 0.05))
                continue
            angle = math.atan2(dy, dx)
            sector = int(round((angle + math.pi) / (math.pi / 3))) % 6
            rim = edge_y - abs(dy) < 1.1 or abs(dx) > 0.866 * radius - 0.9
            to_vertex = abs(((angle + math.pi / 6) % (math.pi / 3)) - math.pi / 6) < 0.13
            c = shade(mix(ice, aqua["base"], 0.16), wedges[sector])
            if to_vertex:
                c = shade(c, 0.80)          # crisp radial seam toward each vertex
            elif rim:
                c = shade(c, 1.12)          # bevel catches the light
            top.px(x, y, c)
    # centre star: six converging glints
    for a in range(6):
        ang = a * math.pi / 3 + math.pi / 6
        top.px(int(round(cx + math.cos(ang) * 1.3)), int(round(cy + math.sin(ang) * 1.3)), pal["hi"])
    top.px(7, 7, pal["tip"])
    top.px(8, 7, pal["hi"])
    top.px(7, 8, pal["hi"])
    write_png(BLOCK / "crystal_column.png", canvas.grid)
    write_png(BLOCK / "crystal_column_top.png", top.grid)



# --- flora vocabulary --------------------------------------------------------

ALGAE_DARK = (22, 46, 30, 255)
ALGAE_MID = (48, 92, 54, 255)
ALGAE_LIGHT = (96, 150, 84, 255)
ALGAE_TEAL = (58, 128, 110, 255)
ALGAE_TIP = (150, 196, 132, 255)


def algae_mat_top() -> None:
    canvas = Canvas()
    rng = random.Random("algae-top")
    noise = smooth_noise("algae-top")
    for y in range(16):
        for x in range(16):
            n = noise[y][x]
            if n < 0.34:
                c = ALGAE_DARK
            elif n < 0.62:
                c = ALGAE_MID
            elif n < 0.82:
                c = ALGAE_TEAL
            else:
                c = ALGAE_LIGHT
            canvas.px(x, y, c)
    # filament streaks and pale growing tips
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        length = rng.randrange(2, 5)
        color = ALGAE_TIP if rng.random() < 0.35 else ALGAE_LIGHT
        for i in range(length):
            canvas.px((x + i) % 16, min(15, y + (i % 2)), mix(color, ALGAE_MID, i / length))
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        canvas.px(x, y, ALGAE_TIP)
    write_png(BLOCK / "algae_mat_top.png", canvas.grid)


def algae_mat_side() -> None:
    canvas = Canvas()
    rng = random.Random("algae-side")
    for y in range(16):
        for x in range(16):
            base = ALGAE_DARK if y > 11 else ALGAE_MID
            canvas.px(x, y, shade(base, 0.8 + rng.random() * 0.4))
    # ragged filament edge along the top of the visible band
    for x in range(16):
        top = 10 + rng.randrange(3)
        for y in range(top, 16):
            c = ALGAE_LIGHT if y == top else (ALGAE_MID if y < 13 else ALGAE_DARK)
            canvas.px(x, y, mix(c, ALGAE_TEAL, rng.random() * 0.35))
        if rng.random() < 0.4:
            canvas.px(x, top - 1, ALGAE_TIP)
    write_png(BLOCK / "algae_mat_side.png", canvas.grid)


def algae_tuft() -> None:
    canvas = Canvas()
    rng = random.Random("algae-tuft")
    strands = [(2, 9), (4, 12), (6, 8), (8, 13), (10, 9), (12, 11), (14, 7)]
    for x, height in strands:
        lean = rng.choice([-1, 0, 0, 1])
        for r in range(height):
            y = 15 - r
            t = r / max(1, height - 1)
            px = x + int(round(lean * (t ** 1.5) * 2))
            c = mix(ALGAE_MID, ALGAE_TEAL if x % 4 else ALGAE_LIGHT, t)
            canvas.px(px, y, c)
            if t > 0.75:
                canvas.px(px, y, mix(c, ALGAE_TIP, (t - 0.75) * 2.4))
            if r % 4 == 2 and rng.random() < 0.5:
                canvas.px(px + rng.choice([-1, 1]), y, shade(c, 0.85))
    write_png(BLOCK / "algae_tuft.png", canvas.grid)


def crystal_sprout() -> None:
    canvas = Canvas()
    draw_prism(canvas, 8, 15, 8, 1.5, PALETTES["white"], 0.0)
    draw_prism(canvas, 5, 15, 5, 1.1, PALETTES["aqua"], -0.8)
    draw_prism(canvas, 11, 15, 4, 1.0, PALETTES["rose"], 0.7)
    write_png(BLOCK / "crystal_sprout.png", canvas.grid)


# Fringe strands hold FIXED x positions at every sprite boundary (y=0 and y=15), so a
# top/body/tail stack always welds into one continuous strand; only mid-sprite wobble is
# allowed, and it returns to the anchor before the edge.
FRINGE_STRANDS = [(3, "white"), (6, "amethyst"), (9, "aqua"), (12, "white")]


def fringe_x(base_x: int, y: int, phase: float) -> int:
    import math
    envelope = math.sin(math.pi * y / 16.0)  # zero at both block boundaries
    wobble = 1.3 * envelope * math.cos(phase + y * 0.45)
    return base_x + int(round(wobble))


def crystal_fringe(part: str) -> None:
    canvas = Canvas()
    rng = random.Random(f"fringe-{part}")
    for base_x, material in FRINGE_STRANDS:
        pal = PALETTES[material]
        phase = rng.uniform(0.0, 3.0)
        if part == "top":
            # a crystal root gripping the ceiling: a small diamond over the strand head
            for dx in range(-1, 2):
                canvas.px(base_x + dx, 0, pal["deep"])
                canvas.px(base_x + dx, 1, pal["base"] if dx == 0 else pal["dark"])
            canvas.px(base_x, 2, pal["light"])
            y_first, y_last = 0, 15
        elif part == "body":
            y_first, y_last = 0, 15
        else:
            # tapering tips: the strand must still arrive at y=0 on its anchor
            length = rng.randrange(7, 11)
            y_first, y_last = 0, length
        for y in range(y_first, y_last + 1):
            px = fringe_x(base_x, y, phase)
            if part == "tail" and y == y_last:
                canvas.px(px, y, pal["tip"])
                break
            c = pal["dark"] if y % 3 else pal["base"]
            canvas.px(px, y, c)
            if part == "tail" and y > y_last - 3 and rng.random() < 0.4:
                continue  # thinning tip
            if rng.random() < 0.30:
                canvas.px(px, y, pal["hi"])
            # boundary rows carry the bare strand only: a stray side pixel here would
            # punch a gap where the next sprite in the chain has to weld on
            if part != "tail" and y > 0 and y < 15 and rng.random() < 0.35:
                canvas.px(px + 1, y, pal["deep"])
        if part == "tail":
            px = fringe_x(base_x, y_last, phase)
            canvas.px(px, min(15, y_last + 1) if y_last < 15 else y_last, pal["tip"])
    # star glints along the strands (never on the boundary rows: they must weld cleanly)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(1, 15)
        if canvas.get(x, y) != TRANSPARENT:
            canvas.px(x, y, PALETTES["white"]["tip"])
    write_png(BLOCK / f"crystal_fringe_{part}.png", canvas.grid)


def main() -> None:
    for name, material in [
        ("white_crystal_cluster", "white"),
        ("rose_crystal_cluster", "rose"),
        ("amethyst_crystal_cluster", "amethyst"),
        ("aqua_crystal_cluster", "aqua"),
        ("smoky_crystal_cluster", "smoky"),
        ("resonant_crystal_cluster", "resonant"),
        ("life_gem_cluster", "life"),
    ]:
        crystal_cluster(name, material)
    nest_stone("crystal_nest_stone", "nest-stone", top=False)
    nest_stone("crystal_nest_stone_top", "nest-stone-top", top=True)
    crystal_druse()
    crystal_column()
    algae_mat_top()
    algae_mat_side()
    algae_tuft()
    crystal_sprout()
    for part in ("top", "body", "tail"):
        crystal_fringe(part)
    print("crystal nest textures written")


if __name__ == "__main__":
    main()
