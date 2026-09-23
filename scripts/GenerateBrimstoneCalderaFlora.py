#!/usr/bin/env python3
"""Generate detailed, realistic Brimstone Caldera flora sprites (16x16).

Hydrothermal-vent plant and decoration textures: faceted sulfur crystal
clusters, glowing fireblooms, fumarole mounds, dripping sulfur stalactites,
ember kelp chains, and seamless thermophilic microbial mats.

Every sprite layers explicit botanical / mineral structure (prism facets,
petal whorls, drip columns, midribs and heat veins) under multi-octave value
noise and hand-placed micro-detail (glints, inclusions, bubbles, tufts) so the
forms read as crystal and tissue rather than flat fills.  Fixed integer seeds
keep the output byte-for-byte deterministic; tileable mats use periodic noise
and wrapped feature drawing so edges repeat seamlessly.
"""

from __future__ import annotations

import math
import random
import struct
import zlib
from pathlib import Path
from typing import List, Tuple

SIZE = 16
OUT = Path("src/main/resources/assets/aquanaut/textures/block")
RGBA = Tuple[int, int, int, int]
Grid = List[List[RGBA]]


def write_png(path: Path, grid: Grid) -> None:
    raw = bytearray()
    for row in grid:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((max(0, min(255, int(r))), max(0, min(255, int(g))),
                        max(0, min(255, int(b))), max(0, min(255, int(a)))))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                     + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    print(f"wrote {path}")


def write_mcmeta(path: Path, frametime: int = 6) -> None:
    # kept for parity with the Brine Mirror generator; single-frame sprites
    # need no animation metadata.
    path.write_text('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)
    print(f"wrote {path}")


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def lerp_c(a: RGBA, b: RGBA, t: float) -> RGBA:
    return (lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t),
            lerp(a[3], b[3], t))


def smooth(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return t * t * (3.0 - 2.0 * t)


def hash2(x: float, y: float, seed: int) -> float:
    n = math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453
    return n - math.floor(n)


def value_noise(x: float, y: float, seed: int) -> float:
    x0, y0 = math.floor(x), math.floor(y)
    fx, fy = x - x0, y - y0
    u, v = smooth(fx), smooth(fy)
    n00 = hash2(x0, y0, seed)
    n10 = hash2(x0 + 1, y0, seed)
    n01 = hash2(x0, y0 + 1, seed)
    n11 = hash2(x0 + 1, y0 + 1, seed)
    return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v)


def fbm(x: float, y: float, seed: int, octaves: int = 4) -> float:
    total, amp, freq, norm = 0.0, 0.5, 1.0, 0.0
    for i in range(octaves):
        total += amp * value_noise(x * freq, y * freq, seed + i * 17)
        norm += amp
        amp *= 0.5
        freq *= 2.0
    return total / max(norm, 1e-6)


def value_noise_p(x: float, y: float, seed: int, px: int, py: int) -> float:
    """Value noise with lattice hashing wrapped over (px, py) so fields tile."""
    x0, y0 = math.floor(x), math.floor(y)
    fx, fy = x - x0, y - y0
    u, v = smooth(fx), smooth(fy)
    n00 = hash2(x0 % px, y0 % py, seed)
    n10 = hash2((x0 + 1) % px, y0 % py, seed)
    n01 = hash2(x0 % px, (y0 + 1) % py, seed)
    n11 = hash2((x0 + 1) % px, (y0 + 1) % py, seed)
    return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v)


def fbm_p(x: float, y: float, seed: int, px: int, py: int, octaves: int = 4) -> float:
    """fbm over periodic lattice noise; tiles after (px, py) in input space."""
    total, amp, freq, norm = 0.0, 0.5, 1.0, 0.0
    for i in range(octaves):
        total += amp * value_noise_p(x * freq, y * freq, seed + i * 17,
                                     int(px * freq), int(py * freq))
        norm += amp
        amp *= 0.5
        freq *= 2.0
    return total / max(norm, 1e-6)


def domain_warp_p(x: float, y: float, seed: int, px: int, py: int,
                  amount: float = 0.35) -> Tuple[float, float]:
    wx = fbm_p(x + 3.1, y + 1.7, seed + 101, px, py, 3) - 0.5
    wy = fbm_p(x + 5.3, y + 2.9, seed + 211, px, py, 3) - 0.5
    return x + wx * amount, y + wy * amount


def plot(g: Grid, x: int, y: int, col: RGBA) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        g[y][x] = col


def plot_w(g: Grid, x: int, y: int, col: RGBA) -> None:
    """Plot one pixel through wrapped coordinates so edge features repeat."""
    g[y % SIZE][x % SIZE] = col


def peek(g: Grid, x: int, y: int) -> RGBA:
    return g[y % SIZE][x % SIZE]


def seg_frame(x: float, y: float, ax: float, ay: float, bx: float, by: float
              ) -> Tuple[float, float]:
    """Return (t, d) of point (x, y) in the frame of segment A->B.

    t runs 0..1 along the segment; d is the signed offset across it (positive
    on the right-hand side of the travel direction).
    """
    vx, vy = bx - ax, by - ay
    l2 = max(vx * vx + vy * vy, 1e-6)
    t = ((x - ax) * vx + (y - ay) * vy) / l2
    d = (vx * (y - ay) - vy * (x - ax)) / math.sqrt(l2)
    return t, d


def face_lit(vx: float, vy: float) -> float:
    """Sign of the top-left key light across a segment's frame: +1 when the
    right-hand side of travel faces the light."""
    lx, ly = -0.707, -0.707
    mx, my = -vy, vx  # outward normal of the +d side
    return 1.0 if mx * lx + my * ly > 0 else -1.0


def outline(g: Grid, col: RGBA, strength: float = 0.55) -> None:
    """Paint a soft 1px contour wherever a transparent pixel touches a filled
    one: the ink is blended toward the neighbouring material colour so the rim
    reads as a darker body tone (like the drooping seaweed edges) instead of a
    hard near-black sticker outline."""
    hits: List[Tuple[int, int, RGBA]] = []
    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3] != 0:
                continue
            acc = [0.0, 0.0, 0.0]
            n = 0
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < SIZE and 0 <= ny < SIZE and g[ny][nx][3] != 0:
                    c = g[ny][nx]
                    acc[0] += c[0]
                    acc[1] += c[1]
                    acc[2] += c[2]
                    n += 1
            if n:
                body = (acc[0] / n, acc[1] / n, acc[2] / n, 255.0)
                hits.append((x, y, lerp_c(body, col, strength)))
    for x, y, c in hits:
        g[y][x] = c


# ---------------------------------------------------------------------------
# Shared crystal machinery - prisms with crisp facets and top-left key light
# ---------------------------------------------------------------------------

PrismPal = Tuple[RGBA, RGBA, RGBA, RGBA, RGBA]   # (lo, hi, lit, shade, edge)


def prism_width(t: float, hw: float) -> float:
    """Prism half-width: near-constant body with a chisel point at the tip."""
    t = max(0.0, min(1.0, t))
    if t < 0.55:
        return hw * (0.88 + 0.12 * t / 0.55)
    return hw * (1.0 - smooth((t - 0.55) / 0.45) * 0.96)


def paint_prism(g: Grid, ax: float, ay: float, bx: float, by: float,
                hw: float, pal: PrismPal, seed: int) -> None:
    """Rasterise one prismatic blade: amber-to-lemon body gradient, a bright
    lit face against a shaded face, and a crisp facet boundary between them."""
    lo, hi, lit, shade, edge = pal
    vx, vy = bx - ax, by - ay
    k = face_lit(vx, vy)
    ridge = 0.12
    for y in range(SIZE):
        for x in range(SIZE):
            t, d = seg_frame(x + 0.5, y + 0.5, ax, ay, bx, by)
            if t < -0.04 or t > 1.0:
                continue
            w = prism_width(t, hw)
            if abs(d) > w or w < 0.12:
                continue
            u = d / max(w, 1e-6)
            base = lerp_c(lo, hi, smooth(0.08 + 0.92 * t))
            b = 0.5 + 0.5 * u * k
            if b > 0.5:
                col = lerp_c(base, lit, (b - 0.5) * 0.9)
            else:
                col = lerp_c(base, shade, (0.5 - b) * 1.0)
            if w > 1.25 and abs(u - ridge) < 0.18:          # crisp facet edge
                col = lerp_c(col, edge, 0.55)
            if abs(u) > 0.84:                               # outer prism rim
                col = lerp_c(col, shade, 0.35)
            n = hash2(x * 3.1 + seed, y * 1.3, seed + 9)    # fracture striations
            if n > 0.88:
                col = lerp_c(col, edge, 0.22)
            elif n > 0.975:
                col = lerp_c(col, (255, 255, 255, 255), 0.5)
            plot(g, x, y, col)


# ---------------------------------------------------------------------------
# Sulfur crystal - cluster of prismatic lemon-to-amber blades on a grit base
# ---------------------------------------------------------------------------

def gen_sulfur_crystal(seed: int = 31) -> Grid:
    amber = (201, 154, 30, 235)        # #C99A1E thick basal color
    lemon = (242, 224, 90, 235)        # #F2E05A fresh prism face
    lit = (250, 241, 165, 240)         # top-left face toward the key light
    shade = (150, 108, 18, 235)        # lee face
    edge = (106, 78, 14, 245)          # crisp facet boundary
    glint = (255, 255, 255, 255)
    incl = (155, 162, 60, 235)         # #9BA23C green inclusion
    grit = (176, 140, 44, 240)
    pal: PrismPal = (amber, lemon, lit, shade, edge)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    # back blades first, hero blade over them, front splinters last
    blades = [
        (4.8, 15.8, 2.3, 3.8, 1.75),
        (9.5, 15.8, 12.2, 3.0, 1.85),
        (7.3, 15.8, 5.8, 0.4, 2.35),
        (7.9, 15.8, 9.0, 7.4, 1.45),
        (5.7, 15.8, 4.4, 8.8, 1.15),
    ]
    for i, (ax, ay, bx, by, hw) in enumerate(blades):
        paint_prism(g, ax, ay, bx, by, hw, pal, seed + i * 5)
    # crystalline base cluster: second-generation micro prisms and grit
    for i, (ax, ay, bx, by, hw) in enumerate([
            (5.0, 15.9, 4.1, 12.5, 0.85),
            (8.7, 15.9, 9.9, 12.8, 0.8),
            (6.8, 15.9, 6.5, 13.0, 0.7),
            (10.4, 15.9, 11.3, 13.6, 0.6)]):
        paint_prism(g, ax, ay, bx, by, hw, pal, seed + 30 + i * 5)
    for y in range(12, SIZE):
        for x in range(2, 14):
            if g[y][x][3] != 0:
                continue
            r = hash2(x * 1.7, y * 2.3, seed + 5)
            if r > 0.78:
                plot(g, x, y, lerp_c(grit, lemon, hash2(x, y, seed + 6) * 0.8))
    # white facet glints at blade tips and along lit faces
    for gx, gy in ((6, 0), (5, 1), (12, 3), (2, 4), (9, 7)):
        if g[gy][gx][3] != 0:
            plot(g, gx, gy, glint)
    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3] == 0:
                continue
            h = hash2(x * 5.3, y * 2.9, seed + 40)
            if h > 0.988:
                plot(g, x, y, glint)
            elif h < 0.012:                                # green inclusions
                plot(g, x, y, lerp_c(g[y][x], incl, 0.85))
    outline(g, (36, 28, 4, 255))                           # #241C04
    return g


# ---------------------------------------------------------------------------
# Firebloom - recurved sulfur petals around a glowing hydrothermal throat
# ---------------------------------------------------------------------------

def paint_petal(g: Grid, cx: float, cy: float, ang_deg: float, r1: float,
                bend: float, wid: float, pal: PrismPal, seed: int,
                r0: float = 1.25) -> None:
    """Rasterise one recurved petal as a tapered curved lamina with a folded
    mid-crease and dew-bright lit face."""
    lo, hi, lit, shade, edge = pal
    a = math.radians(ang_deg)
    dx, dy = math.cos(a), math.sin(a)
    px, py = -dy, dx
    n = 9
    pts = []
    for i in range(n + 1):
        s = i / n
        r = lerp(r0, r1, s)
        lat = bend * s * s * (r1 - r0) * 0.55
        pts.append((cx + dx * r + px * lat, cy + dy * r + py * lat))
    k = face_lit(dx, dy)
    for y in range(SIZE):
        for x in range(SIZE):
            best_d, best_s = 1e9, 0.0
            for i in range(n):
                t, d = seg_frame(x + 0.5, y + 0.5, *pts[i], *pts[i + 1])
                if 0.0 <= t <= 1.0 and abs(d) < best_d:
                    best_d, best_s = abs(d), (i + t) / n
            if best_d > 1e8:
                continue
            s = best_s
            w = wid * (0.55 + 0.5 * math.sin(math.pi * min(1.0, s ** 0.8)))
            if best_d > w:
                continue
            u = math.copysign(best_d / max(w, 1e-6), 0.0)
            t2, d2 = seg_frame(x + 0.5, y + 0.5, *pts[min(n - 1, int(s * n))],
                               *pts[min(n, int(s * n) + 1)])
            u = d2 / max(w, 1e-6) if abs(d2) < 1e8 else u
            base = lerp_c(lo, hi, 0.35 + 0.45 * math.sin(math.pi * s))
            b = 0.5 + 0.5 * u * k
            if b > 0.5:
                col = lerp_c(base, lit, (b - 0.5) * 0.95)
            else:
                col = lerp_c(base, shade, (0.5 - b) * 0.95)
            if abs(u) < 0.16 and 0.25 < s < 0.95:           # recurved fold
                col = lerp_c(col, shade, 0.3)
            elif abs(u) > 0.8:                              # lamina rim
                col = lerp_c(col, edge, 0.3)
            n2 = hash2(x * 2.7, y * 1.9, seed + 3)
            if n2 > 0.94:
                col = lerp_c(col, (255, 255, 255, 255), 0.4)
            plot(g, x, y, col)


def gen_firebloom(seed: int = 32) -> Grid:
    sulfur = (239, 216, 78, 245)       # #EFD84E petal lamina
    pet_lit = (250, 236, 140, 250)
    pet_shade = (196, 168, 52, 245)
    pet_edge = (168, 140, 36, 250)
    core = (255, 224, 160, 255)        # brightest emissive pixels
    throat_hi = (255, 179, 71, 255)    # #FFB347 glowing centre
    throat_mid = (240, 138, 56, 255)
    throat_lo = (224, 100, 42, 255)    # #E0642A outer throat
    stem = (110, 122, 42, 245)         # olive stem
    stem_lit = (142, 154, 62, 250)
    stem_shade = (74, 78, 26, 245)
    glint = (255, 255, 255, 255)
    petal_pal: PrismPal = (pet_shade, sulfur, pet_lit, pet_shade, pet_edge)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    cx, cy = 7.5, 5.8
    # slender olive stem with a soft S-curve, drawn behind the flower
    for y in range(7, SIZE):
        sx = 7.3 + 0.5 * smooth((y - 7) / 8.5) + 0.12 * math.sin(y * 0.9)
        w = 1.05 - 0.25 * smooth((y - 7) / 8.5)
        for x in range(SIZE):
            u = (x + 0.5 - sx) / max(w, 1e-6)
            if abs(u) > 1.0:
                continue
            col = lerp_c(stem, stem_lit, smooth(-u * 0.5 + 0.5) * 0.7)
            if u > 0.35:
                col = lerp_c(stem, stem_shade, smooth(u) * 0.8)
            if abs(u) > 0.8:
                col = lerp_c(col, stem_shade, 0.4)
            plot(g, x, y, col)
    # 2 small fronds off the stem
    for fx0, fy0, fx1, fy1 in ((6.9, 11.5, 4.5, 10.1), (8.1, 13.6, 10.6, 12.5)):
        for y in range(SIZE):
            for x in range(SIZE):
                t, d = seg_frame(x + 0.5, y + 0.5, fx0, fy0, fx1, fy1)
                if not 0.0 <= t <= 1.0:
                    continue
                w = 0.85 * (1.0 - smooth((t - 0.35) / 0.65) * 0.85)
                if abs(d) > w:
                    continue
                u = d / max(w, 1e-6)
                col = lerp_c(stem, stem_lit, smooth(-u * 0.5 + 0.5) * 0.6)
                if abs(u) < 0.3:                            # midvein
                    col = lerp_c(col, stem_shade, 0.45)
                plot(g, x, y, col)
    # petal whorl: short back petal, two lower petals, two upper, one crown
    for ang, r1, bend, wid, ps in (
            (133, 3.1, -0.5, 1.0, 3), (170, 3.9, -0.8, 1.2, 4),
            (10, 3.9, 0.8, 1.2, 5), (-152, 4.3, -0.9, 1.3, 6),
            (-28, 4.3, 0.9, 1.3, 7), (-90, 4.6, 0.8, 1.35, 8)):
        paint_petal(g, cx, cy, ang, r1, bend, wid, petal_pal, seed + ps)
    # glowing throat: emissive gradient, brightest pixels at the very core
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot((x + 0.5 - cx) * 1.0, (y + 0.5 - cy) * 0.92)
            if d > 2.35 + 0.3 * hash2(x, y, seed + 11):
                continue
            if d < 0.55:
                col = core
            elif d < 1.15:
                col = lerp_c(core, throat_hi, smooth((d - 0.55) / 0.6))
            elif d < 1.75:
                col = lerp_c(throat_hi, throat_mid, smooth((d - 1.15) / 0.6))
            else:
                col = lerp_c(throat_mid, throat_lo, smooth((d - 1.75) / 0.6))
            if hash2(x * 2.2, y * 3.1, seed + 12) > 0.93 and d < 1.4:
                col = lerp_c(col, core, 0.6)                # inner sparkle
            plot(g, x, y, col)
    # dew glints on petal laminae
    for gx, gy in ((5, 2), (11, 3), (3, 5), (12, 6)):
        if g[gy][gx][3] != 0:
            plot(g, gx, gy, glint)
    outline(g, (42, 30, 5, 255))                            # #2A1E05
    return g


# ---------------------------------------------------------------------------
# Fumarole - mineral vent mound with sulfur frosting and a pitch-black mouth
# ---------------------------------------------------------------------------

def gen_fumarole(seed: int = 33) -> Grid:
    rock = (58, 58, 62, 255)           # #3A3A3E mineral rim
    rock_lit = (94, 94, 102, 255)
    rock_dark = (30, 30, 34, 255)
    frost = (216, 194, 74, 255)        # #D8C24A sulfur frosting
    frost_lit = (236, 220, 130, 255)
    mouth = (5, 5, 7, 255)             # pitch-black vent mouth
    nodule = (232, 208, 84, 255)
    pebble = (74, 74, 80, 255)
    glint = (255, 255, 255, 255)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    # bottom-heavy mound profile (top of the mound sits in the lower 2/3)
    def top_y(x: float) -> float:
        base = (13.2 - 8.0 * math.exp(-((x - 7.2) / 5.4) ** 2)
                - 2.2 * math.exp(-((x - 2.6) / 2.3) ** 2))
        return base + (fbm(x / 3.0, 4.0, seed, 3) - 0.5) * 1.1

    for x in range(SIZE):
        ty = top_y(x + 0.5)
        for y in range(SIZE):
            if y + 1.0 < ty:
                continue
            depth = (y + 0.5 - ty) / max(15.5 - ty, 1e-6)
            n = grain = fbm(x / 2.6, y / 2.6, seed + 7, 4)
            lum = 0.28 + 0.34 * (1.0 - depth) + 0.3 * (1.0 - x / 15.0) + n * 0.3
            col = lerp_c(rock_dark, rock_lit, smooth(lum))
            if n < 0.32:                                    # crevice shadow
                col = lerp_c(col, rock_dark, 0.55)
            elif n > 0.72:                                  # facet catchlight
                col = lerp_c(col, rock_lit, 0.5)
            if y + 1.0 < ty + 0.8:                          # lit crest of rim
                col = lerp_c(col, rock_lit, 0.45)
            plot(g, x, y, col)
    # sulfur frosting ringing the vent mouth
    mx, my, mrx, mry = 7.0, 8.4, 2.9, 2.1
    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3] == 0:
                continue
            d = math.hypot((x + 0.5 - mx) / mrx, (y + 0.5 - my) / mry)
            d += (fbm(x / 1.8, y / 1.8, seed + 13, 3) - 0.5) * 0.35
            if d < 1.0:                                     # black mouth
                plot(g, x, y, mouth)
            elif d < 1.85 and hash2(x * 1.4, y * 2.1, seed + 17) > 0.22:
            # frosted rim, brighter where it faces the key light
                k = smooth((mx - x) / 5.0) * 0.5 + smooth((my - y) / 4.0) * 0.5
                col = lerp_c(frost, frost_lit, k * 0.8)
                if hash2(x, y, seed + 19) > 0.86:
                    col = lerp_c(col, glint, 0.35)          # frosting glints
                plot(g, x, y, col)
    # tiny pebbles and sulfur nodule glints along the base
    rng = random.Random(seed)
    for i in range(5):
        bx = 1.5 + rng.uniform(0, 13.0)
        by = 13.8 + rng.uniform(0, 1.4)
        r = rng.uniform(0.55, 0.95)
        for yy in range(int(by) - 1, int(by) + 2):
            for xx in range(int(bx) - 1, int(bx) + 2):
                if math.hypot(xx + 0.5 - bx, yy + 0.5 - by) < r:
                    lit = (xx + 0.5 - bx) + (yy + 0.5 - by) < -0.2
                    plot(g, xx, yy, lerp_c(rock_dark, pebble, 0.85 if lit else 0.3))
    for nx, ny in ((3.4, 13.3), (11.6, 14.1), (13.2, 12.6)):
        for yy in (int(ny), int(ny) + 1):
            for xx in (int(nx) - 1, int(nx)):
                if g[yy][xx][3] == 0:
                    continue
                plot(g, xx, yy, lerp_c(nodule, frost, 0.4 if yy == int(ny) else 0.1))
        plot(g, int(nx) - 1, int(ny), glint)                # nodule specular
    # stray frosting crumbs on the upper rim
    for y in range(4, 12):
        for x in range(SIZE):
            if g[y][x][3] != 0 and hash2(x * 3.7, y * 1.3, seed + 23) > 0.955:
                plot(g, x, y, lerp_c(g[y][x], frost, 0.7))
    outline(g, (26, 26, 28, 255))
    return g


# ---------------------------------------------------------------------------
# Sulfur stalactite - translucent amber drip chain (top root / body / tail)
# ---------------------------------------------------------------------------

def drip_shade(u: float, core_w: float = 0.3) -> RGBA:
    """Glassy sulfur cross-section: lit left rim, pale core, shaded right rim."""
    body = (232, 198, 58, 232)         # #E8C63A
    lit = (244, 216, 110, 238)
    shade = (168, 126, 24, 238)        # #A87E18
    core = (246, 228, 140, 242)
    b = smooth((1.0 - u) / 2.0)        # light from the top-left
    col = lerp_c(shade, body, b)
    if u < -0.55:
        col = lerp_c(col, lit, 0.55)
    if abs(u) < core_w:
        col = lerp_c(col, core, smooth((core_w - abs(u)) / core_w) * 0.75)
    return col


def gen_sulfur_stalactite(kind: str, seed: int) -> Grid:
    outline_c = (74, 52, 4, 255)       # #4A3404
    crust = (86, 80, 66, 245)
    crust_lit = (118, 110, 90, 245)
    frost = (216, 194, 74, 245)        # #D8C24A root frosting
    shade = (168, 126, 24, 238)
    incl = (155, 162, 60, 238)         # #9BA23C greenish inclusion
    glint = (255, 255, 255, 255)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)

    def wander(y: float) -> float:
        return 7.6 + 0.3 * math.sin(y * 0.45 + seed * 0.31)

    if kind == "top":
        # crusty mineral root: wide and jagged where it meets the rock,
        # narrowing into the translucent drip column
        for y in range(SIZE):
            cx = wander(y)
            if y < 5:
                w = 4.15 - 0.22 * y + (hash2(0.5, y * 1.7, seed) - 0.5) * 1.15
            else:
                w = lerp(3.05, 1.95, smooth((y - 5) / 10.0)) \
                    + (hash2(1.5, y * 1.3, seed) - 0.5) * 0.2
            for x in range(SIZE):
                u = (x + 0.5 - cx) / max(w, 1e-6)
                if abs(u) > 1.0:
                    continue
                if y < 5:                                   # mineral root crust
                    n = fbm(x / 1.8, y / 1.5, seed + 3, 3)
                    col = lerp_c(crust, crust_lit, smooth(0.25 + n * 0.6
                                                          - u * 0.2))
                    if hash2(x * 2.3, y * 1.1, seed + 5) > 0.72:
                        col = lerp_c(col, frost, 0.55)      # sulfur frosting
                    if abs(u) > 0.75:
                        col = lerp_c(col, shade, 0.3)
                else:                                       # amber drip column
                    col = drip_shade(u)
                    col = lerp_c(col, crust, smooth((5.9 - y) / 1.4) * 0.5)
                if hash2(x * 3.1, y * 2.7, seed + 7) > 0.94:
                    col = lerp_c(col, glint, 0.45)          # crystalline sparkle
                plot(g, x, y, col)
    elif kind == "body":
        for y in range(SIZE):
            cx = wander(y)
            w = lerp(1.95, 1.6, y / 15.0) + (hash2(2.5, y * 1.1, seed) - 0.5) * 0.25
            for x in range(SIZE):
                u = (x + 0.5 - cx) / max(w, 1e-6)
                if abs(u) > 1.0:
                    continue
                col = drip_shade(u, 0.32)
                n = hash2(x * 2.9, y * 3.7, seed + 3)
                if n > 0.93:
                    col = lerp_c(col, glint, 0.4)
                elif n < 0.04:
                    col = lerp_c(col, shade, 0.35)          # interior murk
                plot(g, x, y, col)
        # tiny irregular drip beads along the edges
        for y, side in ((2, -1), (5, 1), (8, -1), (11, 1), (13, -1)):
            cx = wander(y)
            w = lerp(1.95, 1.6, y / 15.0)
            bx = int(cx + side * (w + 0.4))
            col = drip_shade(0.55 * side)
            plot(g, bx, y, col)
            if y + 1 < SIZE and rng.random() < 0.5:
                plot(g, bx, y + 1, lerp_c(col, shade, 0.3))
    else:  # tail
        bulb = [0.9, 1.5, 2.0, 2.15, 2.0, 1.55, 0.85, 0.2]
        for y in range(SIZE):
            cx = wander(y) + 0.35 * smooth(y / 14.0)
            if y < 4:
                w = lerp(1.55, 1.15, y / 4.0)
            elif y < 8:
                w = lerp(1.15, 0.9, (y - 4) / 4.0)          # necking above bulb
            else:
                w = bulb[y - 8]
            for x in range(SIZE):
                u = (x + 0.5 - cx) / max(w, 1e-6)
                if abs(u) > 1.0:
                    continue
                col = drip_shade(u, 0.34 if y < 8 else 0.22)
                if y >= 8:                                  # glossy teardrop
                    v = (y - 8.5) / 4.5
                    col = lerp_c(col, drip_shade(0.0, 0.5), 0.25 * (1.0 - v))
                plot(g, x, y, col)
        # one bright specular highlight on the bulb + greenish inclusion
        hx, hy = int(wander(11.0) + 0.35) - 1, 10
        plot(g, hx, hy, glint)
        plot(g, hx + 1, hy, lerp_c(drip_shade(-0.5), glint, 0.45))
        for ix, iy in ((int(wander(12.0)) + 1, 12), (int(wander(13.0)), 13)):
            if g[iy][ix][3] != 0:
                plot(g, ix, iy, incl)
    outline(g, outline_c)
    return g


# ---------------------------------------------------------------------------
# Ember kelp - rust-orange blade chain with midrib and dull-red heat veins
# ---------------------------------------------------------------------------

def kelp_shade(u: float, wave: float) -> RGBA:
    body = (194, 90, 34, 240)          # #C25A22
    hi = (224, 128, 58, 245)           # #E0803A
    lo = (150, 66, 26, 240)
    b = smooth((1.0 - u) / 2.0)        # key light from the top-left
    col = lerp_c(lo, body, 0.35 + b * 0.6)
    if u < -0.45:
        col = lerp_c(col, hi, 0.5)
    if abs(u) > 0.7:                   # wavy translucent margin
        col = lerp_c(col, lo, 0.25)
    return col


def gen_ember_kelp(kind: str, seed: int) -> Grid:
    outline_c = (110, 46, 16, 255)     # #6E2E10
    vein = (142, 58, 24, 245)          # #8E3A18 heat veins
    vein_hi = (176, 88, 40, 245)
    body = (194, 90, 34, 240)
    hi = (224, 128, 58, 245)
    lo = (150, 66, 26, 240)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    def wander(y: float) -> float:
        drift = 2.2 * smooth((y - 3.0) / 11.0) ** 1.6 if kind == "tail" else 0.0
        return 7.6 + 0.22 * math.sin(y * 0.5 + seed * 0.23) + drift

    def half_w(y: float) -> float:
        if kind == "top":
            return 3.1 - 0.13 * y if y < 4 else lerp(2.5, 2.05, (y - 4) / 11.0)
        if kind == "body":
            return 2.1 + 0.32 * math.sin(y * 0.85 + 0.6)
        return lerp(2.0, 0.0, smooth((y - 2.0) / 12.5) ** 1.25)

    for y in range(SIZE):
        cx = wander(y)
        w = half_w(y)
        if w < 0.2 and kind == "tail" and y > 13:
            w = 0.35                  # keep the curled tip reaching the edge
        for x in range(SIZE):
            u = (x + 0.5 - cx) / max(w, 1e-6)
            if abs(u) > 1.0:
                continue
            if kind == "top" and y < 4:                     # holdfast clamp
                n = fbm(x / 1.7, y / 1.3, seed + 3, 3)
                col = lerp_c(lo, body, smooth(0.3 + n * 0.55 - u * 0.15))
                if u < -0.4:
                    col = lerp_c(col, hi, 0.4)
                if abs(u) > 0.72:
                    col = lerp_c(col, outline_c, 0.45)
            else:
                col = kelp_shade(u, 0.0)
            # darker midrib with a relief highlight beside it
            if abs(u) < 0.18:
                col = lerp_c(col, vein, 0.75)
            elif abs(abs(u) - 0.3) < 0.1:
                col = lerp_c(col, vein_hi, 0.3)
            # branching heat veins angling off the midrib
            for s, side in ((2, -1), (5, 1), (8, -1), (11, 1), (14, -1)):
                if abs(y - s) < 1.35 and u * side > 0.15 and u * side < 0.85:
                    col = lerp_c(col, vein, 0.55)
            wave = math.sin(y * 1.35 + seed * 0.4)
            if abs(u) > 0.66:                               # translucent margin
                a = 255 if kind == "top" and y < 4 else 168 + int(38 * wave)
                col = (col[0], col[1], col[2], a)
            if hash2(x * 2.6, y * 3.4, seed + 9) > 0.95:
                col = lerp_c(col, hi, 0.5)                  # tissue sparkle
            plot(g, x, y, col)
    if kind == "tail":
        # warm rim light along the outer curl, tip pointing to one side
        for y in range(6, SIZE):
            cx = wander(y)
            w = max(half_w(y), 0.35)
            for x in range(SIZE):
                if g[y][x][3] == 0:
                    continue
                u = (x + 0.5 - cx) / w
                if u < -0.55 and (x == 0 or g[y][x - 1][3] == 0):
                    plot(g, x, y, lerp_c(g[y][x], hi, 0.55))
    outline(g, outline_c)
    return g


# ---------------------------------------------------------------------------
# Thermophilic mats - seamless mottled microbial swirls with bubble domes
# ---------------------------------------------------------------------------

MAT_PALETTES = {
    "gold": ((110, 92, 34, 255), (74, 62, 20, 255),          # #6E5C22 base
             (201, 168, 58, 255), (228, 202, 112, 255),      # #C9A83A swirls
             (240, 228, 176, 255)),                          # silky sheen
    "rust": ((78, 46, 18, 255), (48, 28, 10, 255),           # #4E2E12 base
             (180, 90, 34, 255), (208, 122, 52, 255),        # #B45A22 swirls
             (232, 176, 120, 255)),                          # #D07A34 crests
    "olive": ((46, 52, 22, 255), (26, 30, 12, 255),          # #2E3416 base
              (110, 122, 42, 255), (142, 154, 62, 255),      # #6E7A2A swirls
              (176, 186, 116, 255)),                         # #8E9A3E crests
}


def gen_thermophilic_mat(kind: str, seed: int) -> Grid:
    base, dark, swirl, crest, sheen = MAT_PALETTES[kind]
    g: Grid = [[base for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = x / 4.0, y / 4.0
            wx, wy = domain_warp_p(u, v, seed, 4, 4, 0.7)
            m = fbm_p(wx, wy, seed + 31, 4, 4, 3)           # broad tonal drift
            # swirl streamers stretched along the flow diagonal (tiles exactly)
            q = fbm_p(2.0 * (wx + wy), wx - wy, seed + 17, 8, 4, 4)
            col = lerp_c(base, dark, smooth((0.35 - m) / 0.35) * 0.55)
            col = lerp_c(col, base, smooth((m - 0.35) / 0.4) * 0.5)
            ribbon = smooth((0.085 - abs(q - 0.56)) / 0.085)
            col = lerp_c(col, swirl, ribbon * 0.9)
            col = lerp_c(col, crest, smooth((0.03 - abs(q - 0.575)) / 0.03) * 0.65)
            h = hash2(x * 1.3, y * 0.9, seed + 6)           # micro mottle
            col = lerp_c(col, crest if h > 0.5 else dark, abs(h - 0.5) * 0.16)
            g[y][x] = col
    # tiny bubble domes with top-left highlights
    for i in range(8):
        bx = hash2(i, 3.7, seed + 5) * SIZE
        by = hash2(i, 8.1, seed + 5) * SIZE
        r = 0.8 + hash2(i, 4.4, seed + 5) * 0.6
        for oy in range(-2, 3):
            for ox in range(-2, 3):
                px, py = int(bx) + ox, int(by) + oy
                d = math.hypot(px + 0.5 - bx, py + 0.5 - by)
                if d > r:
                    continue
                nx, ny = (px + 0.5 - bx) / max(r, 1e-6), (py + 0.5 - by) / max(r, 1e-6)
                dome = lerp_c(dark, swirl, smooth(0.5 - 0.5 * (nx + ny)))
                if d > r - 0.55:
                    dome = lerp_c(dome, dark, 0.5)          # dome rim
                if nx < -0.35 and ny < -0.35 and d < r * 0.6:
                    dome = lerp_c(dome, sheen, 0.75)        # specular cap
                plot_w(g, px, py, dome)
    # silky sheen streaks running with the swirl flow
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = x / 4.0, y / 4.0
            s = fbm_p(3.0 * (u + v), u - v, seed + 51, 12, 4, 3)
            t = smooth((s - 0.62) / 0.18) * 0.4
            if t > 0.02:
                g[y][x] = lerp_c(g[y][x], sheen, t)
    return g


# ---------------------------------------------------------------------------
# Sulfur moss - tileable dense fuzzy cushion with bubble tufts and gaps
# ---------------------------------------------------------------------------

def gen_sulfur_moss(seed: int = 43) -> Grid:
    olive = (110, 116, 36, 255)        # #6E7424
    mid = (154, 162, 46, 255)          # #9AA22E
    light = (196, 198, 74, 255)        # #C4C64A
    gap = (62, 68, 22, 255)            # #3E4416
    g: Grid = [[olive for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = x / 4.0, y / 4.0
            wx, wy = domain_warp_p(u, v, seed, 4, 4, 0.6)
            h = fbm_p(wx, wy, seed + 3, 4, 4, 4)            # mound height field
            hx = fbm_p(wx - 0.06, wy, seed + 3, 4, 4, 4)
            hy = fbm_p(wx, wy - 0.06, seed + 3, 4, 4, 4)
            slope = (h - hx) + (h - hy)                     # top-left key light
            fuzz = hash2(x * 1.7, y * 1.1, seed + 7) - 0.5  # fuzzy micro tufts
            v2 = 0.2 + h * 0.5 + slope * 1.5 + fuzz * 0.22
            col = lerp_c(olive, light, smooth(v2))
            if v2 > 0.82:
                col = lerp_c(col, light, 0.55)              # cushion tips
            if h < 0.4 + fuzz * 0.1 and slope < 0.03:
                col = lerp_c(col, gap, smooth((0.4 - h) / 0.3) * 0.85)
            g[y][x] = col
    # bubble-like tuft domes over the mound tops
    for i in range(18):
        bx = hash2(i, 2.3, seed + 11) * SIZE
        by = hash2(i, 6.9, seed + 11) * SIZE
        r = 0.55 + hash2(i, 3.3, seed + 11) * 0.5
        for oy in range(-2, 3):
            for ox in range(-2, 3):
                px, py = int(bx) + ox, int(by) + oy
                d = math.hypot(px + 0.5 - bx, py + 0.5 - by)
                if d < r:
                    col = lerp_c(mid, light, 1.0 - d / max(r, 1e-6))
                    if px + 0.5 - bx < -0.1 and py + 0.5 - by < -0.1:
                        col = lerp_c(col, light, 0.5)       # tuft cap highlight
                    plot_w(g, px, py, col)
                elif d < r + 0.45:
                    side = (px + 0.5 - bx) + (py + 0.5 - by)
                    col = lerp_c(peek(g, px, py), light if side < -0.2 else gap,
                                 0.4 if side < -0.2 else 0.3)
                    plot_w(g, px, py, col)
    return g


# ---------------------------------------------------------------------------

DESCRIPTIONS = [
    ("sulfur_crystal.png",
     "cluster of five sharp prismatic sulfur blades plus micro-prisms on a grit "
     "base; lemon-to-amber translucent gradient, crisp facet edges, white tip "
     "glints and green inclusions"),
    ("firebloom.png",
     "hydrothermal blossom: six recurved sulfur petals with dew glints around an "
     "emissive orange-red throat glowing brightest at the core, on a slender "
     "olive stem with two small fronds"),
    ("fumarole.png",
     "bottom-heavy vent mound: dark grey mineral rim frosted with pale yellow "
     "sulfur around a pitch-black vent mouth, with pebbles and three sulfur "
     "nodule glints at the base"),
    ("sulfur_stalactite_top.png",
     "crusty mineral root wider at the rock face, frosting into a translucent "
     "amber-yellow sulfur drip column with crystalline sparkles"),
    ("sulfur_stalactite_body.png",
     "slim tapering translucent sulfur drip with a glassy pale core and tiny "
     "irregular drip beads along the edges"),
    ("sulfur_stalactite_tail.png",
     "sulfur drip necking into a glossy teardrop bulb with one bright specular "
     "highlight and a greenish inclusion, drawn to a hanging point"),
    ("ember_kelp_top.png",
     "rust-orange kelp holdfast clamp gripping the rock above the first "
     "descending blade, with darker midrib and dull-red heat veins"),
    ("ember_kelp_body.png",
     "kelp blade with wavy translucent margins, darker midrib and branching "
     "dull-red heat veins with relief highlights"),
    ("ember_kelp_tail.png",
     "tapering kelp blade tip curling to one side with a warm rim light along "
     "the outer curve"),
    ("thermophilic_mat_gold.png",
     "seamless mustard-gold microbial mat: warped swirl streamers with crest "
     "highlights over olive-brown base, bubble domes and silky sheen streaks"),
    ("thermophilic_mat_rust.png",
     "seamless rust-orange microbial mat: #B45A22 swirls with #D07A34 crests "
     "over dark brown base, bubble domes and silky sheen streaks"),
    ("thermophilic_mat_olive.png",
     "seamless olive-green microbial mat: #6E7A2A swirls with #8E9A3E crests "
     "over dark mossy base, bubble domes and silky sheen streaks"),
    ("sulfur_moss.png",
     "seamless dense sulfur-moss cushion: mounded fuzzy yellow-green tops with "
     "bubble-like tufts and dark #3E4416 shadow gaps in the crevices"),
]


def verify_png(path: Path) -> Tuple[int, int]:
    """Re-open a written PNG and confirm its IHDR claims a full 16x16 sprite."""
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path}: bad PNG signature"
    assert len(data) > 64, f"{path}: truncated file"
    w, h = struct.unpack(">II", data[16:24])
    assert (w, h) == (SIZE, SIZE), f"{path}: expected {SIZE}x{SIZE}, got {w}x{h}"
    return w, h


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    write_png(OUT / "sulfur_crystal.png", gen_sulfur_crystal())
    write_png(OUT / "firebloom.png", gen_firebloom())
    write_png(OUT / "fumarole.png", gen_fumarole())
    write_png(OUT / "sulfur_stalactite_top.png", gen_sulfur_stalactite("top", 34))
    write_png(OUT / "sulfur_stalactite_body.png", gen_sulfur_stalactite("body", 35))
    write_png(OUT / "sulfur_stalactite_tail.png", gen_sulfur_stalactite("tail", 36))
    write_png(OUT / "ember_kelp_top.png", gen_ember_kelp("top", 37))
    write_png(OUT / "ember_kelp_body.png", gen_ember_kelp("body", 38))
    write_png(OUT / "ember_kelp_tail.png", gen_ember_kelp("tail", 39))
    write_png(OUT / "thermophilic_mat_gold.png", gen_thermophilic_mat("gold", 40))
    write_png(OUT / "thermophilic_mat_rust.png", gen_thermophilic_mat("rust", 41))
    write_png(OUT / "thermophilic_mat_olive.png", gen_thermophilic_mat("olive", 42))
    write_png(OUT / "sulfur_moss.png", gen_sulfur_moss())
    print()
    for name, desc in DESCRIPTIONS:
        w, h = verify_png(OUT / name)
        print(f"verified {w}x{h}  {name} - {desc}")


if __name__ == "__main__":
    main()
