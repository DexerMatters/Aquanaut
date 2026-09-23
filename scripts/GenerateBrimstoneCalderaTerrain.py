#!/usr/bin/env python3
"""Generate detailed, realistic Brimstone Caldera volcanic terrain sprites (16x16).

Each texture layers multi-octave value noise under hand-coded volcanic structure
(columnar flutes, vesicles, pillow lobes, angular clasts, conchoidal streaks,
sulfur crusts) so every surface reads as a believable material rather than flat
noise.  Seeds are fixed integers so output is deterministic; tileable sprites use
periodic noise and wrapped feature drawing so edges repeat seamlessly.
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
OUT_ITEM = Path("src/main/resources/assets/aquanaut/textures/item")
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


def grain_p(x: float, y: float, seed: int, px: int, py: int) -> float:
    return fbm_p(x, y, seed, px, py, 5)


def domain_warp_p(x: float, y: float, seed: int, px: int, py: int,
                  amount: float = 0.35) -> Tuple[float, float]:
    wx = fbm_p(x + 3.1, y + 1.7, seed + 101, px, py, 3) - 0.5
    wy = fbm_p(x + 5.3, y + 2.9, seed + 211, px, py, 3) - 0.5
    return x + wx * amount, y + wy * amount


def plot(g: Grid, x: int, y: int, col: RGBA) -> None:
    """Plot one pixel through wrapped coordinates so edge features repeat."""
    g[y % SIZE][x % SIZE] = col


def peek(g: Grid, x: int, y: int) -> RGBA:
    return g[y % SIZE][x % SIZE]


def cell_field(x: float, y: float, seed: int, cells: int
               ) -> Tuple[float, float, float, float]:
    """Tileable Voronoi: (cell id, edge distance, dx, dy toward cell center)."""
    px, py = x * cells / SIZE, y * cells / SIZE
    best, second, bid, bx, by = 1e9, 1e9, 0.0, 0.0, 0.0
    ix, iy = math.floor(px), math.floor(py)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            gx, gy = (ix + dx) % cells, (iy + dy) % cells
            cx = ix + dx + 0.15 + 0.7 * hash2(gx, gy, seed)
            cy = iy + dy + 0.15 + 0.7 * hash2(gx + 9, gy + 3, seed + 7)
            d = (cx - px) ** 2 + (cy - py) ** 2
            if d < best:
                second = best
                best, bid, bx, by = d, hash2(gx, gy, seed + 41), cx - px, cy - py
            elif d < second:
                second = d
    return bid, math.sqrt(second) - math.sqrt(best), bx, by


def clast_radii(rng: random.Random, n: int = 6) -> List[float]:
    """Per-sector outline radii giving an angular blob (clast, lobe, nodule)."""
    return [rng.uniform(0.6, 1.3) for _ in range(n)]


def clast_r(radii: List[float], ang: float) -> float:
    """Angular radius at a given angle, smoothly interpolating the sectors."""
    t = (ang % (2.0 * math.pi)) / (2.0 * math.pi / len(radii))
    j = int(t)
    f = smooth(t - j)
    return lerp(radii[j % len(radii)], radii[(j + 1) % len(radii)], f)


def ang_dist(dx: float, dy: float, cx: float, cy: float) -> Tuple[float, float]:
    """Wrapped delta from center to (dx, dy) taking the short way over the tile."""
    wx = ((dx - cx + SIZE / 2) % SIZE) - SIZE / 2
    wy = ((dy - cy + SIZE / 2) % SIZE) - SIZE / 2
    return wx, wy


# ---------------------------------------------------------------------------
# Volcanic basalt - dark columnar flutes with crack joints and mineral speckle
# ---------------------------------------------------------------------------

def gen_volcanic_basalt(seed: int = 1) -> Grid:
    base_dark = (26, 27, 32, 255)    # #1A1B20
    base_lit = (46, 48, 56, 255)     # #2E3038
    crack = (13, 14, 17, 255)
    speck = (104, 118, 142, 255)     # faint grey-blue mineral speckle
    streak = (60, 64, 77, 255)       # pale weathering smear
    g: Grid = [[base_dark for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        # crack meander wanders but wraps vertically (integer harmonics)
        wob = (0.20 * math.sin(2.0 * math.pi * 2.0 * y / SIZE + 0.7)
               + 0.13 * math.sin(2.0 * math.pi * 3.0 * y / SIZE + 2.1))
        for x in range(SIZE):
            c = x / 3.2 + wob                  # 5 fluted columns across 16px
            ci = math.floor(c)
            t = c - ci
            colid = int(ci) % 5
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            # flute shading, light crests biased left (top-left key light)
            lit = 0.5 + 0.5 * math.cos(2.0 * math.pi * (t - 0.33))
            v = 0.42 + lit * 0.34 + n * 0.18 + (hash2(colid, 0, seed) - 0.5) * 0.22
            col = lerp_c(base_dark, base_lit, smooth(v))
            # thin darker crack lines between the columns
            edge = min(t, 1.0 - t)
            if edge < 0.06 + n * 0.05:
                col = lerp_c(col, crack, 0.8)
            elif edge < 0.15 + n * 0.05:
                col = lerp_c(col, crack, 0.3)
            # subtle vertical weathering streaks
            st = fbm_p(x * 12 / SIZE, y * 2 / SIZE, seed + 5, 12, 2, 4)
            if st > 0.6:
                col = lerp_c(col, streak, (st - 0.6) * 0.9)
            elif st < 0.35:
                col = lerp_c(col, crack, (0.35 - st) * 0.45)
            # faint grey-blue mineral speckle
            h = hash2(x, y, seed + 9)
            if h > 0.955:
                col = lerp_c(col, speck, 0.6)
            elif h < 0.03:
                col = lerp_c(col, crack, 0.45)
            g[y][x] = col
    return g


def gen_volcanic_basalt_top(seed: int = 2) -> Grid:
    lo = (23, 24, 29, 255)
    hi = (53, 56, 65, 255)
    joint = (10, 11, 14, 255)
    glint = (76, 84, 99, 255)
    g: Grid = [[lo for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            cid, edge, bx, by = cell_field(x, y, seed, 5)
            n = grain_p(x * 5 / SIZE, y * 5 / SIZE, seed, 5, 5)
            # each polygonal column end sits at its own dark grey value
            v = 0.3 + cid * 0.4 + n * 0.22
            col = lerp_c(lo, hi, smooth(v))
            # hairline dark joints with a bevel: highlight top-left, shade bottom-right
            if edge < 0.05:
                col = lerp_c(col, joint, 0.85)
            elif edge < 0.13:
                if bx + by > 0.3:
                    col = lerp_c(col, glint, 0.45)
                elif bx + by < -0.3:
                    col = lerp_c(col, joint, 0.4)
            # faint mineral dust
            h = hash2(x, y, seed + 11)
            if h > 0.96:
                col = lerp_c(col, glint, 0.5)
            elif h < 0.025:
                col = lerp_c(col, joint, 0.4)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Scoria - vesicular red-brown lava with rust mottling and olivine specks
# ---------------------------------------------------------------------------

def gen_scoria(seed: int = 3) -> Grid:
    deep = (36, 20, 22, 255)       # #241416
    mid = (74, 42, 34, 255)        # #4A2A22
    rust = (92, 52, 42, 255)       # #5C342A
    core = (12, 7, 8, 255)
    rim = (102, 66, 50, 255)       # subtle inner rim light
    oliv = (122, 106, 85, 255)     # #7A6A55
    oliv_hi = (172, 156, 128, 255)
    g: Grid = [[deep for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            col = lerp_c(deep, mid, smooth(0.22 + n * 0.72))
            # rust mottling drifting through the matrix
            rust_n = fbm_p(x * 4 / SIZE, y * 4 / SIZE, seed + 30, 4, 4, 3)
            if rust_n > 0.55:
                col = lerp_c(col, rust, smooth((rust_n - 0.55) / 0.3) * 0.8)
            # scoriaceous grit
            h = hash2(x, y, seed + 3)
            if h > 0.94:
                col = lerp_c(col, core, 0.45)
            elif h > 0.9:
                col = lerp_c(col, rim, 0.3)
            g[y][x] = col
    # ~15 small angular vesicles: dark cores with a subtle top-left rim light
    for i in range(15):
        cx, cy = rng.uniform(0.5, 15.5), rng.uniform(0.5, 15.5)
        r = rng.uniform(0.8, 1.6)
        radii = clast_radii(rng, 5)
        for oy in range(-3, 4):
            for ox in range(-3, 4):
                px, py = int(cx) + ox, int(cy) + oy
                wx, wy = ang_dist(px + 0.5, py + 0.5, cx, cy)
                d = math.hypot(wx, wy) / max(r * clast_r(radii, math.atan2(wy, wx)), 1e-6)
                if d >= 1.0:
                    continue
                col = lerp_c(core, (30, 17, 17, 255), smooth(d) * 0.5)
                if d > 0.62:
                    if wx + wy < -0.3:
                        col = lerp_c(col, rim, 0.65)   # inner rim catches the light
                    else:
                        col = lerp_c(col, core, 0.4)
                plot(g, px, py, col)
    # a few bright olivine specks
    for i in range(4):
        px, py = rng.randrange(SIZE), rng.randrange(SIZE)
        plot(g, px, py, oliv)
        plot(g, px + 1, py, lerp_c(oliv, oliv_hi, 0.6))
        if i % 2 == 0:
            plot(g, px, py - 1, oliv_hi)
    return g


# ---------------------------------------------------------------------------
# Pillow basalt - bulbous glassy lobes stacked two rows deep
# ---------------------------------------------------------------------------

def gen_pillow_basalt(seed: int = 4) -> Grid:
    body_d = (35, 42, 38, 255)     # #232A26
    body_l = (57, 66, 58, 255)     # #39423A
    glass = (90, 107, 88, 255)     # #5A6B58
    crack = (17, 21, 19, 255)
    g: Grid = [[crack for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    lobes = []
    for row, cy in enumerate((4.0, 12.0)):
        for i in range(3):
            cx = i * (SIZE / 3.0) + (SIZE / 6.0 if row else 0.0)
            lobes.append((cx, cy, rng.uniform(2.9, 3.5), rng.uniform(2.7, 3.2),
                          clast_radii(rng, 7)))
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed + 2, 6, 6)
            best, binfo = 1e9, None
            for cx, cy, rx, ry, radii in lobes:
                wx, wy = ang_dist(x + 0.5, y + 0.5, cx, cy)
                d = math.hypot(wx / rx, wy / ry) / max(
                    clast_r(radii, math.atan2(wy, wx)), 1e-6)
                if d < best:
                    best, binfo = d, (wx, wy, rx, ry)
            wx, wy, rx, ry = binfo
            if best < 1.0:
                # bulge shading: top-left of each lobe catches the light
                v = 0.55 - 0.2 * (wx / rx) - 0.18 * (wy / ry) + (n - 0.5) * 0.35
                col = lerp_c(body_d, body_l, smooth(v))
                if best > 0.68:                       # paler glassy rim
                    col = lerp_c(col, glass, smooth((best - 0.68) / 0.32) * 0.75)
                # fine contraction cracks inside the lobe
                if hash2(x, y, seed + 7) > 0.94 and best < 0.8:
                    col = lerp_c(col, crack, 0.4)
                g[y][x] = col
            else:
                # crack network where lobes meet
                g[y][x] = lerp_c(crack, body_d, n * 0.35)
                if hash2(x, y, seed + 8) > 0.9:
                    g[y][x] = lerp_c(g[y][x], glass, 0.25)
    return g


def gen_pillow_basalt_top(seed: int = 5) -> Grid:
    body_d = (35, 42, 38, 255)
    body_l = (57, 66, 58, 255)
    glass = (90, 107, 88, 255)
    crack = (16, 20, 18, 255)
    g: Grid = [[body_d for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            # warp bends Voronoi joints into rounded interlocking lobes
            wx, wy = domain_warp_p(x * 4 / SIZE, y * 4 / SIZE, seed, 4, 4, 0.55)
            cid, edge, bx, by = cell_field(wx * SIZE / 4, wy * SIZE / 4, seed, 3)
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed + 2, 6, 6)
            cxv, cyv = -bx, -by                     # pixel offset from lobe center
            rr = math.hypot(cxv, cyv) / 3.1
            ang = math.atan2(cyv, cxv)
            v = 0.55 - 0.12 * (cxv / 3.1) - 0.12 * (cyv / 3.1)
            v += (cid - 0.5) * 0.16 + (n - 0.5) * 0.3 - rr * 0.2
            col = lerp_c(body_d, body_l, smooth(v))
            if rr > 0.55:                            # glassy rim toward the joint
                col = lerp_c(col, glass, smooth((rr - 0.55) / 0.45) * 0.6)
            # radial cooling cracks fanning out from each lobe center
            fan = abs(math.sin(ang * 3.0 + cid * 9.0))
            if fan < 0.09 and 0.12 < rr < 0.85:
                col = lerp_c(col, crack, 0.55 * (1.0 - fan / 0.09) + 0.25)
            if edge < 0.06:                          # joint crack network
                col = lerp_c(col, crack, 0.85)
            elif edge < 0.12 and bx + by > 0.3:
                col = lerp_c(col, glass, 0.4)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Volcanic agglomerate - angular clasts packed in fine dark ash matrix
# ---------------------------------------------------------------------------

def gen_volcanic_agglomerate(seed: int = 6) -> Grid:
    matrix = (51, 49, 46, 255)      # #33312E
    tones = [(85, 80, 74, 255),     # #55504A
             (107, 90, 72, 255),    # #6B5A48
             (62, 58, 54, 255)]     # #3E3A36
    hi = (128, 122, 113, 255)
    shadow = (28, 27, 25, 255)
    grit = (72, 69, 64, 255)
    g: Grid = [[matrix for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 8 / SIZE, y * 8 / SIZE, seed, 8, 8)
            g[y][x] = lerp_c(matrix, grit, (n - 0.5) * 0.5 + 0.2)
            if hash2(x, y, seed + 2) > 0.93:        # fine grit between clasts
                g[y][x] = lerp_c(g[y][x], shadow, 0.4)
    clasts = []
    for i in range(10):
        clasts.append((rng.uniform(0.5, 15.5), rng.uniform(0.5, 15.5),
                       rng.uniform(1.5, 2.7), clast_radii(rng, 6),
                       tones[rng.randrange(len(tones))], rng.uniform(0, 6.28)))
    for cx, cy, r, radii, tone, rot in clasts:
        # drop shadow first, offset bottom-right
        for oy in range(-4, 5):
            for ox in range(-4, 5):
                wx, wy = ang_dist(int(cx) + ox + 0.5, int(cy) + oy + 0.5, cx, cy)
                d = math.hypot(wx, wy) / max(r * clast_r(radii, math.atan2(wy, wx) - rot), 1e-6)
                if 1.0 <= d < 1.28 and wx + wy > 0.2:
                    px, py = int(cx) + ox, int(cy) + oy
                    plot(g, px, py, lerp_c(peek(g, px, py), shadow, 0.55))
        for oy in range(-4, 5):
            for ox in range(-4, 5):
                px, py = int(cx) + ox, int(cy) + oy
                wx, wy = ang_dist(px + 0.5, py + 0.5, cx, cy)
                d = math.hypot(wx, wy) / max(r * clast_r(radii, math.atan2(wy, wx) - rot), 1e-6)
                if d >= 1.0:
                    continue
                n = grain_p(px * 8 / SIZE, py * 8 / SIZE, seed + 5, 8, 8)
                col = lerp_c(tone, hi, (n - 0.5) * 0.4 + 0.1)
                if d > 0.6 and wx + wy < -0.2:      # 1px highlight top-left
                    col = lerp_c(col, hi, 0.55)
                elif d > 0.6:                        # soft shade bottom-right
                    col = lerp_c(col, shadow, 0.3)
                plot(g, px, py, col)
    return g


# ---------------------------------------------------------------------------
# Pumice - frothy pale glass riddled with tiny rimmed pores
# ---------------------------------------------------------------------------

def gen_pumice(seed: int = 7) -> Grid:
    lo = (183, 175, 162, 255)      # #B7AFA2
    hi = (216, 210, 198, 255)      # #D8D2C6
    pore = (138, 128, 116, 255)    # #8A8074
    rim = (234, 229, 219, 255)
    sulfur = (201, 185, 106, 255)  # #C9B96A pale sulfur staining
    g: Grid = [[lo for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 7 / SIZE, y * 7 / SIZE, seed, 7, 7)
            col = lerp_c(lo, hi, smooth(0.3 + n * 0.6))
            stain = fbm_p(x * 3 / SIZE, y * 3 / SIZE, seed + 21, 3, 3, 3)
            if stain > 0.6:                         # patches of sulfur staining
                col = lerp_c(col, sulfur, smooth((stain - 0.6) / 0.3) * 0.7)
            if hash2(x, y, seed + 3) > 0.92:        # frothy micro grain
                col = lerp_c(col, rim, 0.45)
            g[y][x] = col
    rng = random.Random(seed)
    # abundant small irregular pores, each with a tiny highlight rim
    for i in range(26):
        cx, cy = rng.uniform(0, 16), rng.uniform(0, 16)
        r = rng.uniform(0.5, 1.15)
        radii = clast_radii(rng, 5)
        for oy in range(-2, 3):
            for ox in range(-2, 3):
                px, py = int(cx) + ox, int(cy) + oy
                wx, wy = ang_dist(px + 0.5, py + 0.5, cx, cy)
                d = math.hypot(wx, wy) / max(r * clast_r(radii, math.atan2(wy, wx)), 1e-6)
                if d < 0.85:
                    plot(g, px, py, lerp_c(pore, (110, 100, 90, 255), d * 0.5))
                elif d < 1.05 and wx + wy < 0.0:
                    plot(g, px, py, lerp_c(peek(g, px, py), rim, 0.6))
    return g


# ---------------------------------------------------------------------------
# Obsidian glass - glossy black with curved conchoidal reflection streaks
# ---------------------------------------------------------------------------

def gen_obsidian_glass(seed: int = 8) -> Grid:
    deep = (11, 12, 16, 255)       # #0B0C10
    body = (22, 24, 31, 255)       # #16181F
    streak = (58, 66, 80, 255)     # #3A4250
    shine = (89, 100, 122, 255)    # #59647A
    glint = (205, 218, 236, 255)
    g: Grid = [[deep for _ in range(SIZE)] for _ in range(SIZE)]
    arcs = [(3.0, 2.5, 4.6, 1.1, 0.4), (11.5, 12.5, 6.4, 0.9, 2.2),
            (8.5, 6.0, 2.6, 0.5, 4.0)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 5 / SIZE, y * 5 / SIZE, seed, 5, 5)
            col = lerp_c(deep, body, smooth(0.3 + n * 0.6))
            for acx, acy, ar, aw, ph in arcs:
                wx, wy = ang_dist(x + 0.5, y + 0.5, acx, acy)
                d = math.hypot(wx, wy)
                ang = math.atan2(wy, wx)
                rr = ar + 0.7 * math.sin(2.0 * ang + ph)   # conchoidal curvature
                t = 1.0 - abs(d - rr) / aw
                if t > 0.0:
                    col = lerp_c(col, streak, smooth(t) * 0.85)
                    if t > 0.82:                    # thin bright ridge
                        col = lerp_c(col, shine, smooth((t - 0.82) / 0.18) * 0.9)
            g[y][x] = col
    # 2-3 tiny bright glints
    rng = random.Random(seed)
    for i in range(3):
        px, py = rng.randrange(2, 14), rng.randrange(2, 14)
        plot(g, px, py, glint)
        plot(g, px + 1, py, lerp_c(glint, shine, 0.5))
    return g


# ---------------------------------------------------------------------------
# Acid-etched basalt - bleached rock with etch pits, drips and ghost clasts
# ---------------------------------------------------------------------------

def gen_acid_etched_basalt(seed: int = 9) -> Grid:
    lo = (143, 139, 122, 255)      # #8F8B7A
    hi = (181, 176, 155, 255)      # #B5B09B
    pit = (110, 106, 92, 255)
    drip = (198, 193, 172, 255)
    ghost = (86, 83, 72, 255)
    g: Grid = [[lo for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            col = lerp_c(lo, hi, smooth(0.3 + n * 0.6))
            # vertical drip streaks of paler dissolved tone
            st = fbm_p(x * 10 / SIZE, y * 2 / SIZE, seed + 5, 10, 2, 4)
            if st > 0.58:
                col = lerp_c(col, drip, smooth((st - 0.58) / 0.35) * 0.8)
            g[y][x] = col
    # etched pitting: tiny pale-rimmed pits
    for i in range(18):
        cx, cy = rng.uniform(0, 16), rng.uniform(0, 16)
        for oy in range(-1, 2):
            for ox in range(-1, 2):
                px, py = int(cx) + ox, int(cy) + oy
                wx, wy = ang_dist(px + 0.5, py + 0.5, cx, cy)
                d = math.hypot(wx, wy)
                if d < 0.55:
                    plot(g, px, py, lerp_c(pit, lo, 0.25))
                elif d < 1.0 and wx + wy < 0.0:
                    plot(g, px, py, lerp_c(peek(g, px, py), (210, 206, 188, 255), 0.5))
    # ghost outlines of dark clasts the acid never fully ate through
    for i in range(3):
        cx, cy = rng.uniform(0, 16), rng.uniform(0, 16)
        r = rng.uniform(2.0, 3.0)
        radii = clast_radii(rng, 6)
        for oy in range(-4, 5):
            for ox in range(-4, 5):
                px, py = int(cx) + ox, int(cy) + oy
                wx, wy = ang_dist(px + 0.5, py + 0.5, cx, cy)
                d = math.hypot(wx, wy) / max(r * clast_r(radii, math.atan2(wy, wx)), 1e-6)
                if 0.85 < d < 1.08:
                    plot(g, px, py, lerp_c(peek(g, px, py), ghost, 0.4))
    return g


# ---------------------------------------------------------------------------
# Sulfur crust - bright mineral cap crumbling over dark basalt
# ---------------------------------------------------------------------------

def gen_sulfur_crust(seed: int = 10) -> Grid:
    sul_lo = (216, 183, 42, 255)   # #D8B72A
    sul_hi = (242, 222, 106, 255)  # #F2DE6A
    sul_sh = (176, 143, 32, 255)
    rock = (38, 38, 42, 255)       # #26262A
    rock_hi = (62, 62, 68, 255)
    glint = (252, 246, 200, 255)
    g: Grid = [[rock for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            # ragged crusty boundary, wraps horizontally
            bx = fbm_p(x * 4 / SIZE, 0.5, seed + 13, 4, 1, 4)
            bnd = 7.4 + (bx - 0.5) * 3.4
            frac = y + (hash2(x, y, seed + 17) - 0.5) * 1.2 - bnd
            if frac < -0.6:
                # bright granular sulfur crust, light from the top-left
                v = 0.45 + n * 0.4 - y * 0.01
                col = lerp_c(sul_lo, sul_hi, smooth(v))
                if hash2(x, y, seed + 2) > 0.93:
                    col = lerp_c(col, glint, 0.7)        # crystal glints
                elif hash2(x, y, seed + 3) < 0.08:
                    col = lerp_c(col, sul_sh, 0.5)
                if frac < -2.0 and hash2(x, y, seed + 23) > 0.95:
                    col = lerp_c(col, rock, 0.35)        # tiny cap holes
            elif frac < 0.9:
                # ragged crumbly transition
                if hash2(x, y, seed + 5) < 0.5:
                    col = lerp_c(sul_lo, sul_sh, n)
                else:
                    col = lerp_c(rock, rock_hi, n * 0.8)
            else:
                col = lerp_c(rock, rock_hi, smooth(0.3 + n * 0.5))
                if hash2(x, y, seed + 7) > 0.94:
                    col = lerp_c(col, sul_sh, 0.35)      # stray crumb below cap
            g[y][x] = col
    return g


def gen_sulfur_crust_top(seed: int = 11) -> Grid:
    rock = (42, 42, 46, 255)       # #2A2A2E
    rock_hi = (66, 66, 72, 255)
    fissure = (18, 18, 21, 255)
    sul = (232, 201, 58, 255)      # #E8C93A
    sul_hi = (250, 232, 130, 255)
    sul_sh = (186, 156, 40, 255)
    glint = (255, 252, 226, 255)
    g: Grid = [[rock for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            col = lerp_c(rock, rock_hi, smooth(0.25 + n * 0.6))
            # crack network through the dark rock
            cid, edge, bx, by = cell_field(x, y, seed + 3, 4)
            if edge < 0.05:
                col = lerp_c(col, fissure, 0.85)
            elif edge < 0.1:
                col = lerp_c(col, fissure, 0.3)
            # irregular sulfur islands floating over the rock
            wx, wy = domain_warp_p(x * 3 / SIZE, y * 3 / SIZE, seed, 3, 3, 0.5)
            isl = fbm_p(wx, wy, seed + 30, 3, 3, 4)
            if isl > 0.55:
                t = smooth((isl - 0.55) / 0.12)
                col2 = lerp_c(sul_sh, sul, smooth(0.35 + n * 0.6))
                if isl > 0.66:
                    col2 = lerp_c(col2, sul_hi, smooth((isl - 0.66) / 0.2) * 0.5)
                col = lerp_c(col, col2, t)
                if hash2(x, y, seed + 9) > 0.9:          # micro-crystalline sparkle
                    col = lerp_c(col, glint, 0.75)
                elif hash2(x, y, seed + 10) < 0.06:
                    col = lerp_c(col, sul_sh, 0.5)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# (The former native sulfur ore texture was retired with its block: all sulfur
# now reads as the crusty sulfur_crust family.)
# ---------------------------------------------------------------------------


# ---------------------------------------------------------------------------
# Sinter - creamy siliceous laminae with pores and ochre staining
# ---------------------------------------------------------------------------

def gen_sinter(seed: int = 13) -> Grid:
    lam_a = (217, 210, 191, 255)   # #D9D2BF
    lam_b = (239, 233, 218, 255)   # #EFE9DA
    seam = (196, 187, 164, 255)
    pore = (150, 142, 122, 255)
    ochre = (194, 169, 126, 255)   # #C2A97E
    g: Grid = [[lam_a for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    for y in range(SIZE):
        for x in range(SIZE):
            wave = (fbm_p(x * 3 / SIZE, y * 3 / SIZE, seed + 4, 3, 3, 3) - 0.5) * 1.6
            p = (y + wave) * 0.5                        # 8 laminae per 16px
            idx = int(math.floor(p)) % 4
            tone = [lam_b, lam_a, lam_b, seam][idx]
            t = smooth(p - math.floor(p))
            col = lerp_c(tone, [lam_a, lam_b, seam, lam_b][idx], t * 0.5)
            if (p - math.floor(p)) < 0.1:               # thin darker seam
                col = lerp_c(col, seam, 0.55)
            n = grain_p(x * 8 / SIZE, y * 8 / SIZE, seed, 8, 8)
            col = lerp_c(col, lam_b, (n - 0.5) * 0.35 + 0.1)
            stain = fbm_p(x * 3 / SIZE, y * 3 / SIZE, seed + 21, 3, 3, 3)
            if stain > 0.62:                            # faint ochre staining
                col = lerp_c(col, ochre, smooth((stain - 0.62) / 0.3) * 0.55)
            g[y][x] = col
    for i in range(8):                                  # small sinter pores
        cx, cy = rng.uniform(0, 16), rng.uniform(0, 16)
        plot(g, int(cx), int(cy), lerp_c(pore, lam_a, 0.2))
        if rng.random() < 0.5:
            plot(g, int(cx) + 1, int(cy), pore)
    return g


def gen_sinter_top(seed: int = 14) -> Grid:
    pool = (239, 233, 218, 255)     # #EFE9DA
    step = (221, 214, 196, 255)
    deep = (196, 187, 165, 255)
    rim_hi = (252, 249, 240, 255)
    pore = (142, 134, 116, 255)
    g: Grid = [[pool for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            cid, edge, bx, by = cell_field(x, y, seed, 4)
            n = grain_p(x * 7 / SIZE, y * 7 / SIZE, seed, 7, 7)
            d = math.hypot(bx, by) + (n - 0.5) * 0.35   # scalloped terrace rims
            lvl = math.floor(d * 2.6)
            col = [pool, step, deep][int(lvl) % 3]
            frac = d * 2.6 - lvl
            if frac < 0.12:                             # step rim highlight/shadow
                if bx + by > 0.15:
                    col = lerp_c(col, rim_hi, 0.6)
                else:
                    col = lerp_c(col, deep, 0.5)
            col = lerp_c(col, rim_hi, (n - 0.5) * 0.3 + 0.08)
            if edge < 0.04:                             # pool boundary crack
                col = lerp_c(col, deep, 0.6)
            if hash2(x, y, seed + 9) > 0.95:            # shadowed pores
                col = lerp_c(col, pore, 0.65)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Vent chimney - fluted black smoker ribs banded with sulfides
# ---------------------------------------------------------------------------

def gen_vent_chimney(seed: int = 15) -> Grid:
    rib_d = (32, 33, 36, 255)      # #202124
    rib_l = (53, 54, 59, 255)      # #35363B
    gold = (140, 122, 62, 255)     # #8C7A3E marcasite
    gold_hi = (184, 164, 96, 255)
    white = (185, 180, 165, 255)   # #B9B4A5 anhydrite
    black = (14, 14, 16, 255)
    g: Grid = [[rib_d for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            c = x / 4.0 + 0.18 * math.sin(2.0 * math.pi * y / SIZE + 1.3)
            t = c - math.floor(c)
            lit = 0.5 + 0.5 * math.cos(2.0 * math.pi * (t - 0.3))
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            col = lerp_c(rib_d, rib_l, smooth(0.35 + lit * 0.35 + n * 0.25))
            edge = min(t, 1.0 - t)
            if edge < 0.08:                             # rough rib grooves
                col = lerp_c(col, black, 0.6)
            # mineral banding: rhythmic phases wrap vertically
            meander = 0.9 * math.sin(2.0 * math.pi * x / SIZE + 0.6) \
                + 0.5 * (fbm_p(x * 4 / SIZE, y * 2 / SIZE, seed + 8, 4, 2, 3) - 0.5)
            p = ((y + meander) % 4.0 + 4.0) % 4.0
            thick = 0.1 + 0.08 * hash2(int(math.floor((y + meander) / 4.0)) % 4,
                                       x // 4, seed + 9)
            if 1.0 < p < 1.0 + thick * 2.2:             # marcasite gold band
                col = lerp_c(col, gold, 0.85)
                if hash2(x, y, seed + 11) > 0.88:
                    col = lerp_c(col, gold_hi, 0.7)
            elif 2.55 < p < 2.55 + thick * 1.8:         # anhydrite white band
                col = lerp_c(col, white, 0.8)
                if hash2(x, y, seed + 12) > 0.9:
                    col = lerp_c(col, (222, 219, 208, 255), 0.6)
            elif abs(p - 1.0) < 0.08 or abs(p - 2.55) < 0.08:
                col = lerp_c(col, black, 0.35)
            g[y][x] = col
    rng = random.Random(seed)
    # rough mineral drips hanging off the bands
    for i in range(6):
        px = rng.randrange(SIZE)
        py = rng.randrange(SIZE)
        col = gold if rng.random() < 0.5 else white
        for k in range(rng.randint(1, 3)):
            plot(g, px, py + k, lerp_c(col, black, k * 0.25))
    return g


def gen_vent_chimney_top(seed: int = 16) -> Grid:
    wall = (40, 41, 45, 255)
    wall_hi = (62, 63, 69, 255)
    gold = (140, 122, 62, 255)     # #8C7A3E
    white = (185, 180, 165, 255)   # #B9B4A5
    hole = (10, 10, 12, 255)       # #0A0A0C
    g: Grid = [[wall for _ in range(SIZE)] for _ in range(SIZE)]
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            # rough circular rim
            rr = d + (n - 0.5) * 0.5 + 0.15 * math.sin(5.0 * ang + 1.0)
            if rr < 2.1:
                col = lerp_c(hole, (18, 18, 21, 255), smooth(rr / 2.1) * 0.5)
            elif rr < 5.6:
                # thick mineral-banded rim with concentric growth rings
                ring = rr - 2.1
                col = lerp_c(wall, wall_hi, smooth(0.35 + n * 0.4))
                if 0.4 < ring < 0.75:
                    col = lerp_c(col, gold, 0.75)
                elif 1.5 < ring < 1.8:
                    col = lerp_c(col, white, 0.7)
                elif 2.55 < ring < 2.85:
                    col = lerp_c(col, gold, 0.55)
                elif abs(ring - 1.1) < 0.09 or abs(ring - 2.2) < 0.09:
                    col = lerp_c(col, hole, 0.45)
                if dx + dy < -0.5 and rr > 4.6:         # top-left light, bottom-right shade
                    col = lerp_c(col, wall_hi, 0.35)
                elif dx + dy > 0.5:
                    col = lerp_c(col, hole, 0.2)
            else:
                col = lerp_c(wall, wall_hi, smooth(0.3 + n * 0.5))
                if hash2(x, y, seed + 7) > 0.93:
                    col = lerp_c(col, white, 0.35)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Volcanic ash - fine grey dust with grit and wind streaks
# ---------------------------------------------------------------------------

def gen_volcanic_ash(seed: int = 17) -> Grid:
    lo = (85, 86, 90, 255)         # #55565A
    hi = (119, 120, 124, 255)      # #77787C
    grit = (58, 59, 63, 255)
    g: Grid = [[lo for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 8 / SIZE, y * 8 / SIZE, seed, 8, 8)
            fine = hash2(x, y, seed + 1) * 0.5 + hash2(x, y, seed + 2) * 0.5
            col = lerp_c(lo, hi, smooth(0.25 + n * 0.5 + fine * 0.25))
            # subtle diagonal wind streaks (integer slopes wrap both axes)
            st = fbm_p((x - 2 * y) * 8 / SIZE, (2 * x + y) * 2 / SIZE,
                       seed + 5, 8, 2, 4)
            if st > 0.58:
                col = lerp_c(col, hi, (st - 0.58) * 0.8)
            elif st < 0.4:
                col = lerp_c(col, grit, (0.4 - st) * 0.35)
            if hash2(x, y, seed + 7) > 0.95:            # faint darker grit specks
                col = lerp_c(col, grit, 0.6)
            g[y][x] = col
    return g


def gen_ash_layer_top(seed: int = 18) -> Grid:
    lo = (105, 106, 110, 255)
    hi = (139, 140, 144, 255)
    lapilli = (60, 61, 64, 255)    # #3C3D40
    g: Grid = [[lo for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    for y in range(SIZE):
        for x in range(SIZE):
            n = grain_p(x * 6 / SIZE, y * 6 / SIZE, seed, 6, 6)
            fine = hash2(x, y, seed + 1)
            col = lerp_c(lo, hi, smooth(0.3 + n * 0.5 + fine * 0.2))
            if hash2(x, y, seed + 3) > 0.92:
                col = lerp_c(col, hi, 0.4)              # soft grain highlights
            g[y][x] = col
    for i in range(7):                                  # tiny dark lapilli specks
        px, py = rng.randrange(SIZE), rng.randrange(SIZE)
        plot(g, px, py, lapilli)
        if rng.random() < 0.6:
            plot(g, px + 1, py, lerp_c(lapilli, lo, 0.35))
    return g


def gen_ash_layer_side(seed: int = 19) -> Grid:
    lo = (85, 86, 90, 255)
    hi = (119, 120, 124, 255)
    grit = (58, 59, 63, 255)
    clear = (0, 0, 0, 0)
    g: Grid = [[clear for _ in range(SIZE)] for _ in range(SIZE)]
    for x in range(SIZE):
        # ragged granular top edge peaking around rows 7-9 (wraps in x)
        wob = fbm_p(x * 4 / SIZE, 0.5, seed + 13, 4, 1, 4)
        top = 9 - int(round(smooth((wob - 0.3) / 0.45) * 2.0))  # rows 7..9
        for y in range(SIZE):
            if y < top - 1:
                continue
            n = grain_p(x * 8 / SIZE, y * 8 / SIZE, seed, 8, 8)
            fine = hash2(x, y, seed + 1) * 0.5 + hash2(x, y, seed + 2) * 0.5
            col = lerp_c(lo, hi, smooth(0.25 + n * 0.5 + fine * 0.25))
            if hash2(x, y, seed + 7) > 0.95:
                col = lerp_c(col, grit, 0.6)
            if y >= top + 1:
                plot(g, x, y, col)                      # dense granular fill
            elif y == top:
                if hash2(x, y, seed + 5) < 0.85:
                    plot(g, x, y, col)
            elif hash2(x, y, seed + 6) < 0.3:           # stray grains above the edge
                plot(g, x, y, col)
    return g


# ---------------------------------------------------------------------------
# Sulfur moss - mounded fuzzy yellow-green cushion with tufts and shadow gaps
# ---------------------------------------------------------------------------

def gen_sulfur_moss(seed: int = 20) -> Grid:
    olive = (110, 116, 36, 255)    # #6E7424
    mid = (154, 162, 46, 255)      # #9AA22E
    light = (196, 198, 74, 255)    # #C4C64A
    gap = (62, 68, 22, 255)        # #3E4416
    g: Grid = [[olive for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            # mounded height field; slope from top-left key light
            h0 = fbm_p(x * 4 / SIZE, y * 4 / SIZE, seed, 4, 4, 4)
            hx = fbm_p((x - 1) * 4 / SIZE, y * 4 / SIZE, seed, 4, 4, 4)
            hy = fbm_p(x * 4 / SIZE, (y - 1) * 4 / SIZE, seed, 4, 4, 4)
            slope = (h0 - hx) + (h0 - hy)
            fuzz = hash2(x, y, seed + 3)                 # fuzzy micro tufts
            v = 0.25 + h0 * 0.45 + slope * 1.6 + fuzz * 0.18
            col = lerp_c(olive, light, smooth(v))
            if h0 < 0.38 and slope < 0.02:
                col = lerp_c(col, gap, smooth((0.38 - h0) / 0.3) * 0.8)  # shadow gaps
            g[y][x] = col
    rng = random.Random(seed)
    for i in range(22):                                  # bubble-like tufts
        cx, cy = rng.uniform(0, 16), rng.uniform(0, 16)
        r = rng.uniform(0.5, 0.95)
        for oy in range(-2, 3):
            for ox in range(-2, 3):
                px, py = int(cx) + ox, int(cy) + oy
                wx, wy = ang_dist(px + 0.5, py + 0.5, cx, cy)
                d = math.hypot(wx, wy)
                if d < r:
                    plot(g, px, py, lerp_c(mid, light, 1.0 - d / max(r, 1e-6)))
                elif d < r + 0.45 and wx + wy < -0.2:
                    plot(g, px, py, lerp_c(peek(g, px, py), light, 0.45))
                elif d < r + 0.45:
                    plot(g, px, py, lerp_c(peek(g, px, py), gap, 0.3))
    return g


# ---------------------------------------------------------------------------
# Sulfur lump item - crystalline chunk with facets, glints and rock matrix
# ---------------------------------------------------------------------------

def gen_sulfur_lump(seed: int = 21) -> Grid:
    clear = (0, 0, 0, 0)
    outline = (36, 28, 4, 255)     # #241C04
    lemon = (240, 218, 74, 255)    # #F0DA4A
    mustard = (185, 154, 34, 255)  # #B99A22
    glint = (255, 255, 255, 255)
    rock = (138, 133, 120, 255)
    rock_d = (94, 90, 80, 255)
    g: Grid = [[clear for _ in range(SIZE)] for _ in range(SIZE)]
    rng = random.Random(seed)
    cx, cy, r = 7.5, 8.2, 4.6
    radii = clast_radii(rng, 8)
    for y in range(SIZE):
        for x in range(SIZE):
            wx, wy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(wy, wx)
            d = math.hypot(wx, wy) / max(r * clast_r(radii, ang), 1e-6)
            if d >= 1.0:
                continue
            # crystalline facets split the chunk into tone planes
            fac = (ang % (2.0 * math.pi)) / (2.0 * math.pi / 4.0)
            fj = int(fac)
            col = [lemon, mustard, lerp_c(lemon, mustard, 0.4), lerp_c(lemon, mustard, 0.7)][fj]
            # top-left light, bottom-right shade
            shade = 0.5 - wx * 0.06 - wy * 0.07
            col = lerp_c(mustard, col, smooth(shade + 0.25))
            fedge = abs(fac - fj - 0.5)
            if fedge > 0.43 and d < 0.8:               # sharp facet edges with glints
                col = lerp_c(col, glint, 0.55)
            elif fedge > 0.43:
                col = lerp_c(col, mustard, 0.4)
            if d > 0.78:
                col = lerp_c(col, mustard, 0.45)
            g[y][x] = col
    # micro crystal spikes on the edges
    for i in range(5):
        ang = rng.uniform(0, 2 * math.pi)
        rr = r * clast_r(radii, ang)
        px, py = int(cx + math.cos(ang) * rr), int(cy + math.sin(ang) * rr)
        plot(g, px, py, lerp_c(lemon, glint, 0.3))
        plot(g, px + (1 if math.cos(ang) > 0 else -1),
             py + (1 if math.sin(ang) > 0 else -1), glint)
    # small grey rock matrix patch at the base
    for y in range(10, 15):
        for x in range(3, 12):
            if g[y][x][3] == 0:
                continue
            if y + abs(x - 7) > 14 and hash2(x, y, seed + 5) > 0.3:
                col = lerp_c(rock, rock_d, hash2(x, y, seed + 6))
                if hash2(x, y, seed + 7) > 0.85:
                    col = lerp_c(col, (170, 166, 155, 255), 0.5)
                g[y][x] = col
    # thin dark outline over the whole silhouette
    fill = [[g[y][x][3] >= 128 for x in range(SIZE)] for y in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if fill[y][x]:
                continue
            near = (fill[(y - 1) % SIZE][x] or fill[(y + 1) % SIZE][x]
                    or fill[y][(x - 1) % SIZE] or fill[y][(x + 1) % SIZE])
            if near:
                g[y][x] = outline
    return g


# ---------------------------------------------------------------------------
# Sulfuric acid bucket item - iron bucket of viscous olive acid with vapor
# ---------------------------------------------------------------------------

def gen_sulfuric_acid_bucket(seed: int = 22) -> Grid:
    clear = (0, 0, 0, 0)
    metal = (200, 203, 208, 255)   # #C8CBD0
    metal_d = (157, 161, 168, 255)
    outline = (110, 114, 120, 255)  # #6E7278
    rim_hi = (236, 239, 244, 255)
    sheen = (201, 196, 58, 255)    # #C9C43A
    depth = (142, 138, 30, 255)    # #8E8A1E
    menisc = (222, 217, 96, 255)
    bubble = (226, 222, 120, 255)
    g: Grid = [[clear for _ in range(SIZE)] for _ in range(SIZE)]

    def metal_at(x: int, y: int) -> RGBA:
        v = 0.55 - (x - 7.5) * 0.045 - (y - 6.0) * 0.02  # top-left key light
        col = lerp_c(metal_d, metal, smooth(v))
        return col

    # tapered front wall of the bucket
    for y in range(5, 15):
        left = 3 if y < 11 else 4
        right = 12 if y < 11 else 11
        for x in range(left, right + 1):
            g[y][x] = metal_at(x, y)
    for x in range(4, 12):
        g[14][x] = lerp_c(g[14][x], outline, 0.4)        # bottom shade
    # rim flange: metal ring around the open mouth
    for y in range(2, 7):
        for x in range(1, 15):
            out_d = ((x - 7.5) / 5.6) ** 2 + ((y - 4.6) / 2.3) ** 2
            in_d = ((x - 7.5) / 4.3) ** 2 + ((y - 4.8) / 1.6) ** 2
            if out_d <= 1.0 and in_d > 1.0:
                g[y][x] = metal_at(x, y)
    # viscous sulfuric acid seen through the open mouth
    for y in range(2, 7):
        for x in range(1, 15):
            in_d = ((x - 7.5) / 4.3) ** 2 + ((y - 4.8) / 1.6) ** 2
            if in_d <= 1.0:
                t = smooth((y - 3.6) / 2.6)              # top sheen fades to depth
                col = lerp_c(sheen, depth, t)
                if in_d > 0.6:                           # meniscus climbs the rim
                    col = lerp_c(col, menisc, 0.5)
                g[y][x] = col
    # 2-3 tiny bubbles suspended in the acid
    for px, py in ((6, 4), (9, 4), (8, 5)):
        if g[py][px][3] >= 128:
            g[py][px] = bubble
    # side handle: squared bail loop on the right
    for y in range(4, 10):
        g[y][14] = metal_at(14, y)
    for y in (4, 9):
        g[y][13] = metal_at(13, y)
    # thin dark outline over the bucket silhouette
    fill = [[g[y][x][3] >= 128 for x in range(SIZE)] for y in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if fill[y][x]:
                if (not fill[(y - 1) % SIZE][x] or not fill[(y + 1) % SIZE][x]
                        or not fill[y][(x - 1) % SIZE] or not fill[y][(x + 1) % SIZE]):
                    g[y][x] = outline
    # bright rim highlight along the top of the rim ring
    for x in range(2, 14):
        for y in range(2, 5):
            if fill[y][x] and not fill[y - 1][x]:
                g[y][x] = lerp_c(g[y][x], rim_hi, 0.7)
    # one faint translucent yellow vapor wisp rising above the rim
    for i in range(4):
        y = 3 - i
        if y < 0:
            break
        x = 7 + int(round(math.sin(i * 1.3 + 0.4) * 1.5))
        if g[y][x][3] < 128:
            g[y][x] = (226, 216, 110, 90) if i % 2 == 0 else (226, 216, 110, 60)
    return g


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    OUT_ITEM.mkdir(parents=True, exist_ok=True)
    write_png(OUT / "volcanic_basalt.png", gen_volcanic_basalt())
    write_png(OUT / "volcanic_basalt_top.png", gen_volcanic_basalt_top())
    write_png(OUT / "scoria.png", gen_scoria())
    write_png(OUT / "pillow_basalt.png", gen_pillow_basalt())
    write_png(OUT / "pillow_basalt_top.png", gen_pillow_basalt_top())
    write_png(OUT / "volcanic_agglomerate.png", gen_volcanic_agglomerate())
    write_png(OUT / "pumice.png", gen_pumice())
    write_png(OUT / "obsidian_glass.png", gen_obsidian_glass())
    write_png(OUT / "acid_etched_basalt.png", gen_acid_etched_basalt())
    write_png(OUT / "sulfur_crust.png", gen_sulfur_crust())
    write_png(OUT / "sulfur_crust_top.png", gen_sulfur_crust_top())
    write_png(OUT / "sinter.png", gen_sinter())
    write_png(OUT / "sinter_top.png", gen_sinter_top())
    write_png(OUT / "vent_chimney.png", gen_vent_chimney())
    write_png(OUT / "vent_chimney_top.png", gen_vent_chimney_top())
    write_png(OUT / "volcanic_ash.png", gen_volcanic_ash())
    write_png(OUT / "ash_layer_top.png", gen_ash_layer_top())
    write_png(OUT / "ash_layer_side.png", gen_ash_layer_side())
    # sulfur_moss.png is owned by scripts/GenerateBrimstoneCalderaFlora.py so the
    # organic cushion art stays with the rest of the caldera flora.
    write_png(OUT_ITEM / "sulfur_lump.png", gen_sulfur_lump())
    write_png(OUT_ITEM / "sulfuric_acid_bucket.png", gen_sulfuric_acid_bucket())


if __name__ == "__main__":
    main()
