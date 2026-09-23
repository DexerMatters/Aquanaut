#!/usr/bin/env python3
"""Generate Brine Gorge enrichment sprites (16x16) for "Brine Mirror Gorge" (盐镜峡).

Extends the halite/calcite/varve family with hopper halite, glittering halite
druse, bladed gypsum, rose-salmon sylvite crust, brine-mirror flake carpet and
a gypsum rose cross sprite.  Every texture is built from structured mineral
features -- cubic growth steps, hopper hohlraum, cleavage planes, facets,
glints, banding, pores -- under top-left light with bottom-right shade, so
surfaces read as minerals instead of flat fills or noise mush.

Deterministic output: fixed integer seeds, pure-python PNG writer (no PIL).
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


# ---------------------------------------------------------------------------
# Tileable variants: lattice indices wrap on a torus so fields (and anything
# drawn with put()) are seamless across both axes.
# ---------------------------------------------------------------------------

def value_noise_p(x: float, y: float, seed: int, px: int, py: int) -> float:
    x0, y0 = math.floor(x), math.floor(y)
    fx, fy = x - x0, y - y0
    u, v = smooth(fx), smooth(fy)
    ix0, iy0 = int(x0) % px, int(y0) % py
    ix1, iy1 = (int(x0) + 1) % px, (int(y0) + 1) % py
    n00 = hash2(ix0, iy0, seed)
    n10 = hash2(ix1, iy0, seed)
    n01 = hash2(ix0, iy1, seed)
    n11 = hash2(ix1, iy1, seed)
    return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v)


def fbm_p(x: float, y: float, seed: int, px: int, py: int, octaves: int = 4) -> float:
    """Periodic fbm: sample x in [0, px] / y in [0, py] across one tile."""
    total, amp, freq, norm = 0.0, 0.5, 1.0, 0.0
    for i in range(octaves):
        total += amp * value_noise_p(x * freq, y * freq, seed + i * 17,
                                    int(px * freq), int(py * freq))
        norm += amp
        amp *= 0.5
        freq *= 2.0
    return total / max(norm, 1e-6)


def put(g: Grid, x: int, y: int, col: RGBA) -> None:
    """Plot wrapped on the torus (keeps facet fields tileable)."""
    g[y % SIZE][x % SIZE] = col


def snap(g: Grid, palette: List[RGBA]) -> Grid:
    """Quantize to the texture's mineral palette so anchor hexes stay exact."""
    for y in range(SIZE):
        for x in range(SIZE):
            r, gg, b, a = g[y][x]
            if a == 0:
                continue
            best = min(palette, key=lambda c: (c[0] - r) ** 2 + (c[1] - gg) ** 2
                       + (c[2] - b) ** 2)
            g[y][x] = (best[0], best[1], best[2], a)
    return g


def torus_delta(a: float, b: float) -> float:
    d = abs(a - b)
    return min(d, SIZE - d)


def voronoi_p(x: float, y: float, seed: int, cells: int = 4) -> Tuple[float, float, Tuple[int, int]]:
    """Periodic Voronoi over a jittered lattice; returns (d1, d2, site_id)."""
    lat = SIZE // cells
    best, second, bid = 1e9, 1e9, (0, 0)
    ci, cj = int(x) // cells, int(y) // cells
    for dj in (-1, 0, 1):
        for di in (-1, 0, 1):
            i, j = (ci + di) % lat, (cj + dj) % lat
            sx = i * cells + 1.2 + hash2(i, j, seed) * (cells - 1.6)
            sy = j * cells + 1.2 + hash2(i + 9, j + 4, seed + 3) * (cells - 1.6)
            dx, dy = torus_delta(x, sx), torus_delta(y, sy)
            d = math.hypot(dx, dy)
            if d < best:
                second = best
                best, bid = d, (i, j)
            elif d < second:
                second = d
    return best, second, bid


# ---------------------------------------------------------------------------
# Shared halite palette (verified against the existing family sprites)
# ---------------------------------------------------------------------------

HALITE_BRIGHT: RGBA = (247, 239, 234, 255)   # #F7EFEA step top edge
HALITE_LIGHT: RGBA = (242, 228, 222, 255)    # #F2E4DE
HALITE_BODY: RGBA = (227, 203, 196, 255)     # #E3CBC4
HALITE_SHADE: RGBA = (185, 154, 147, 255)    # #B99A93
HALITE_DEEP: RGBA = (152, 124, 118, 255)     # hopper hollow depths

# per-mineral quantization palettes (anchor hexes + derived in-between tones)
HALITE_PAL: List[RGBA] = [
    HALITE_BRIGHT, HALITE_LIGHT, HALITE_BODY, HALITE_SHADE, HALITE_DEEP,
    (235, 216, 210, 255), (206, 178, 171, 255), (168, 139, 133, 255)]
DRUSE_PAL: List[RGBA] = [
    (255, 244, 238, 255), (236, 217, 211, 255), (213, 197, 192, 255),
    (198, 184, 180, 255), (168, 154, 150, 255), (152, 138, 134, 255),
    (138, 124, 120, 255)]
GYPSUM_PAL: List[RGBA] = [
    (248, 244, 230, 255), (237, 230, 210, 255), (226, 217, 193, 255),
    (216, 205, 176, 255), (205, 193, 161, 255), (192, 180, 146, 255)]
SYLVITE_PAL: List[RGBA] = [
    (232, 183, 174, 255), (216, 154, 146, 255), (208, 140, 135, 255),
    (201, 127, 124, 255), HALITE_BRIGHT, HALITE_LIGHT, HALITE_BODY,
    (206, 178, 171, 255), HALITE_SHADE, (158, 128, 122, 255)]
FLAKE_PAL: List[RGBA] = [
    (234, 227, 221, 255), (201, 191, 184, 255), (182, 172, 165, 255),
    (162, 152, 146, 255), (89, 100, 122, 255), (58, 66, 80, 255),
    (28, 32, 40, 255), (18, 20, 26, 255), (10, 11, 14, 255)]
ROSE_PAL: List[RGBA] = [
    (239, 230, 204, 255), (228, 216, 187, 255), (216, 201, 168, 255),
    (194, 179, 147, 255), (187, 172, 142, 255), (168, 154, 124, 255)]


# ---------------------------------------------------------------------------
# Hopper halite (side) - stacked cubic growth steps, each face a hollow hopper
# square depression; diagonal stepped terraces wrap tileably on both axes.
# ---------------------------------------------------------------------------

def gen_hopper_halite(seed: int = 21) -> Grid:
    bright = HALITE_BRIGHT
    light = HALITE_LIGHT
    body = HALITE_BODY
    shade = HALITE_SHADE
    deep = HALITE_DEEP
    g: Grid = [[body for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            bx, by = x // 4, y // 4
            lx, ly = x % 4, y % 4
            terr = (bx + by) % 4            # diagonal terrace step id
            var = hash2(bx, by, seed + 2)   # per-cube hopper growth stage
            # -- one cubic growth step = one hollow hopper square face --
            on_rim = lx in (0, 3) or ly in (0, 3)
            if on_rim:
                if ly == 0:
                    col = bright                        # bright top step edge
                elif ly == 3:
                    col = lerp_c(body, shade, 0.62)     # bottom in shade
                elif lx == 0:
                    col = light                         # left flank lit
                else:
                    col = lerp_c(body, shade, 0.50)     # right flank shaded
            else:
                # hollow hopper depression: darker inner wall, lit top-left
                if var < 0.58:      # mature hopper, deep 2x2 hollow
                    col = {(1, 1): lerp_c(shade, light, 0.45),
                           (2, 1): lerp_c(shade, body, 0.20),
                           (1, 2): lerp_c(shade, body, 0.30),
                           (2, 2): deep}[(lx, ly)]
                elif var < 0.85:    # young hopper, shallow hollow
                    col = {(1, 1): lerp_c(shade, light, 0.62),
                           (2, 1): lerp_c(shade, light, 0.38),
                           (1, 2): lerp_c(shade, light, 0.45),
                           (2, 2): lerp_c(shade, body, 0.42)}[(lx, ly)]
                else:               # stepped hopper with a fresh glint
                    col = {(1, 1): lerp_c(shade, bright, 0.55),
                           (2, 1): shade,
                           (1, 2): lerp_c(shade, body, 0.12),
                           (2, 2): deep}[(lx, ly)]
            # stepped terraces recede toward the bottom-right
            col = lerp_c(col, shade, terr * 0.055)
            # micro grain / pores / cleavage glints (keep step edges crisp)
            n = fbm_p(x / 2.0, y / 2.0, seed, 8, 8)
            face = not (ly == 0)
            if face:
                col = lerp_c(col, body if n > 0.5 else shade, abs(n - 0.5) * 0.34)
            h = hash2(x * 1.3, y * 1.7, seed + 9)
            if face and h < 0.045:
                col = lerp_c(col, shade, 0.38)          # pore
            elif face and h > 0.965:
                col = lerp_c(col, bright, 0.55)         # cleavage glint
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Hopper halite (top) - one large square-spiral hopper terraced inward:
# square-in-square descending steps with a four-arm pinwheel twist.
# ---------------------------------------------------------------------------

def gen_hopper_halite_top(seed: int = 22) -> Grid:
    bright = HALITE_BRIGHT
    light = HALITE_LIGHT
    body = HALITE_BODY
    shade = HALITE_SHADE
    deep = HALITE_DEEP
    g: Grid = [[body for _ in range(SIZE)] for _ in range(SIZE)]
    risers = (2, 4, 6)                      # square rings that step down
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - 7.5, y - 7.5
            r = max(abs(dx), abs(dy))       # Chebyshev: square-in-square rings
            ang = math.atan2(dy, dx)
            frac = ((ang + math.pi) / (math.pi / 2.0)) % 1.0   # pinwheel twist
            s = math.floor(r + 0.7 * frac)  # spiral-shifted square ring index
            if s in risers:
                # crisp riser face; top-left light across the four step faces
                if abs(dy) >= abs(dx):
                    col = bright if dy < 0 else lerp_c(shade, deep, 0.45)
                else:
                    col = lerp_c(light, bright, 0.35) if dx < 0 else shade
            else:
                level = sum(1 for k in risers if k > s)  # 0 rim tread .. 3 pit
                col = [light, body, lerp_c(body, shade, 0.55),
                       lerp_c(shade, deep, 0.6)][level]
                n = fbm_p(x / 2.0, y / 2.0, seed, 8, 8)
                col = lerp_c(col, body if n > 0.5 else shade, abs(n - 0.5) * 0.26)
                # periodic diagonal light keeps tile borders matching
                t = 0.5 + 0.5 * math.sin((x - y) * math.pi / 8.0)
                col = lerp_c(col, light if t > 0.5 else shade, abs(t - 0.5) * 0.16)
                h = hash2(x * 1.7, y * 1.1, seed + 5)
                if h < 0.05:
                    col = lerp_c(col, shade, 0.40)          # pore
                elif h > 0.972:
                    col = lerp_c(col, bright, 0.55)         # cleavage glint
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Halite druse - rocky matrix crowded with dozens of tiny halite facets;
# each facet a diamond/square with a white-pink glint pixel and shadow pixel.
# ---------------------------------------------------------------------------

def gen_halite_druse(seed: int = 23) -> Tuple[Grid, List[Tuple[int, int]]]:
    rock = (168, 154, 150, 255)      # #A89A96 matrix
    rock_l = (198, 184, 180, 255)
    shadow = (138, 124, 120, 255)    # #8A7C78 facet shadow pixel
    glint = (255, 244, 238, 255)     # #FFF4EE facet glint pixel
    facet = (236, 217, 211, 255)     # facet body, pale pink
    g: Grid = [[rock for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = fbm_p(x / 4.0, y / 4.0, seed, 4, 4)
            fine = fbm_p(x / 2.0, y / 2.0, seed + 3, 8, 8)
            col = lerp_c(shadow, rock_l, 0.30 + n * 0.55 + fine * 0.15)
            h = hash2(x * 1.9, y * 2.3, seed + 6)
            if h < 0.07:
                col = lerp_c(col, shadow, 0.50)             # matrix pore
            elif h > 0.955:
                col = lerp_c(col, rock_l, 0.75)             # dry salt film
            g[y][x] = col
    # ---- crowded facet field (the glowmask is derived from these glints) ----
    glints: List[Tuple[int, int]] = []
    protected = set()
    for j in range(8):
        for i in range(8):
            if hash2(i, j, seed + 7) > 0.80:
                continue
            fx = int(i * 2 + hash2(i, j, seed + 11) * 1.5) % SIZE
            fy = int(j * 2 + hash2(i + 5, j + 3, seed + 13) * 1.5) % SIZE
            kind = hash2(i, j, seed + 17)
            if kind < 0.45:
                cells = [(fx, fy, glint), (fx + 1, fy, facet),
                         (fx, fy + 1, lerp_c(facet, shadow, 0.35)), (fx + 1, fy + 1, shadow)]
            elif kind < 0.75:
                cells = [(fx, fy, glint), (fx + 1, fy, lerp_c(glint, facet, 0.5)),
                         (fx, fy + 1, facet), (fx + 1, fy + 1, lerp_c(facet, shadow, 0.6))]
            else:
                cells = [(fx, fy, glint), (fx + 1, fy, lerp_c(facet, shadow, 0.2)),
                         (fx, fy + 1, facet), (fx + 1, fy + 1, shadow)]
            if any((cx % SIZE, cy % SIZE) in protected for cx, cy, _ in cells):
                continue
            for cx, cy, col in cells:
                put(g, cx, cy, col)
            gx, gy = fx % SIZE, fy % SIZE
            protected.add((gx, gy))
            glints.append((gx, gy))
    return g, glints


def gen_halite_druse_glowmask(glints: List[Tuple[int, int]], seed: int = 24) -> Grid:
    """Accent glowmask: brightest glint pixels of ~20 facets, nothing else."""
    ranked = sorted(glints, key=lambda p: hash2(p[0], p[1], seed + 31), reverse=True)
    keep = ranked[:20]
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    for x, y in keep:
        g[y][x] = (255, 244, 238, 255)
    return g


# ---------------------------------------------------------------------------
# Gypsum blade (side) - tall ivory bladed bundle: pearly left edges, vertical
# cleavage lines, slightly splintery tapered tips (staggered per column so the
# sprite stays seamless on both axes).
# ---------------------------------------------------------------------------

def gen_gypsum_blade(seed: int = 25) -> Grid:
    body = (237, 230, 210, 255)     # #EDE6D2
    shade = (216, 205, 176, 255)    # #D8CDB0
    pearly = (248, 244, 230, 255)   # #F8F4E6 highlight edges
    seam = (192, 180, 146, 255)     # joint between stacked blades
    widths = [2, 3, 2, 2, 3, 2, 2]
    g: Grid = [[seam for _ in range(SIZE)] for _ in range(SIZE)]
    x0 = 0
    for c, w in enumerate(widths):
        tip_y = int(hash2(c, 3, seed) * 16) % SIZE
        for ly in range(SIZE):
            y = (tip_y + ly) % SIZE
            z = ly                                    # 0 = splintery tip (top)
            # blade silhouette: taper toward the tip
            if z == 0:
                ww = 1
            elif z == 1:
                ww = max(1, w - 1)
            elif z == 2:
                ww = max(2, w - 1) if w > 2 else w
            else:
                ww = w
            for k in range(w):
                x = x0 + k
                if k >= ww:
                    continue
                if z == 1 and k == ww - 1 and w > 2 and hash2(x, y, seed + 8) < 0.45:
                    continue                            # splintery break
                n = fbm_p(x / 2.0, y / 8.0, seed, 8, 2)  # vertical striation
                if k == 0:
                    col = lerp_c(pearly, body, 0.25)
                    if z <= 2:
                        col = pearly                   # tip catches the light
                elif k == w - 1:
                    col = lerp_c(shade, body, 0.25 + n * 0.2)
                elif w >= 3 and k == 1:
                    col = lerp_c(body, shade, 0.48 + (n - 0.5) * 0.3)   # cleavage line
                else:
                    col = lerp_c(body, shade, 0.15 + (1.0 - n) * 0.35)
                if z >= 3:
                    col = lerp_c(col, shade, max(0.0, n - 0.55) * 0.5)
                if z == 15:
                    col = lerp_c(col, seam, 0.45)       # base joint in shade
                if z == 0:
                    col = lerp_c(col, pearly, 0.5)
                h = hash2(x * 2.1, y * 1.3, seed + 12)
                if z >= 3 and h > 0.965:
                    col = lerp_c(col, pearly, 0.6)      # pearly cleavage glint
                put(g, x, y, col)
        x0 += w
    return g


# ---------------------------------------------------------------------------
# Gypsum blade (top) - cross-section: packed lens-shaped blade ends with
# pearly highlights on their upper-left flanks.
# ---------------------------------------------------------------------------

def gen_gypsum_blade_top(seed: int = 26) -> Grid:
    body = (237, 230, 210, 255)
    shade = (216, 205, 176, 255)
    pearly = (248, 244, 230, 255)
    filler = (198, 186, 152, 255)
    g: Grid = [[filler for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = fbm_p(x / 3.0, y / 3.0, seed, 4, 4)
            g[y][x] = lerp_c(filler, shade, 0.25 + n * 0.55)
    lenses = []
    for j in range(4):
        for i in range(4):
            lenses.append((i, j))
            if hash2(i, j, seed + 5) > 0.55:
                lenses.append((i + 0.5, j + 0.5))   # small companion lens
    for i, j in lenses:
        cx = (i * 4 + 1.3 + hash2(i, j, seed + 7) * 1.4) % SIZE
        cy = (j * 4 + 1.3 + hash2(i + 3, j + 8, seed + 9) * 1.4) % SIZE
        rx = 1.6 + hash2(i, j, seed + 11) * 1.4
        ry = 0.8 + hash2(i + 2, j, seed + 13) * 0.6
        ang = (hash2(i, j, seed + 15) - 0.5) * 0.7
        ca, sa = math.cos(ang), math.sin(ang)
        span = int(math.ceil(rx + ry)) + 1
        for py in range(int(cy) - span, int(cy) + span + 1):
            for px in range(int(cx) - span, int(cx) + span + 1):
                dx, dy = px - cx, py - cy
                if dx > SIZE / 2:
                    dx -= SIZE
                elif dx < -SIZE / 2:
                    dx += SIZE
                if dy > SIZE / 2:
                    dy -= SIZE
                elif dy < -SIZE / 2:
                    dy += SIZE
                u = dx * ca + dy * sa
                v = -dx * sa + dy * ca
                e = (u / max(rx, 0.4)) ** 2 + (v / max(ry, 0.4)) ** 2
                if e > 1.0:
                    continue
                lam = -(u / max(rx, 0.4) + v / max(ry, 0.4)) / 2.0   # up-left lit
                if e > 0.62:
                    col = lerp_c(shade, body, 0.18 + lam * 0.22)    # soft rim
                elif lam > 0.25:
                    col = lerp_c(body, pearly, smooth((lam - 0.25) / 0.75) * 0.9)
                else:
                    col = lerp_c(body, shade, smooth((-lam) / 0.9) * 0.75)
                h = hash2(px * 1.7, py * 2.3, seed + 17)
                if h > 0.95:
                    col = lerp_c(col, pearly, 0.5)
                elif h < 0.06:
                    col = lerp_c(col, shade, 0.4)
                put(g, px, py, col)
        # pearly gleam on the upper-left tip of each lens end
        gx = int(cx + (rx * -0.7) * ca + (ry * -0.5) * -sa) % SIZE
        gy = int(cy + (rx * -0.7) * sa + (ry * -0.5) * ca) % SIZE
        put(g, gx, gy, pearly)
    return g


# ---------------------------------------------------------------------------
# Sylvite crust (side) - rose-salmon potash crust capping pale halite below a
# ragged crystalline boundary with tiny hopper cubes in the crust.
# ---------------------------------------------------------------------------

def gen_sylvite_crust(seed: int = 27) -> Grid:
    syl = (216, 154, 146, 255)        # #D89A92
    syl_shade = (201, 127, 124, 255)  # #C97F7C
    glassy = (232, 183, 174, 255)     # #E8B7AE glassy glints
    halite = HALITE_BODY              # #E3CBC4
    halite_l = HALITE_LIGHT           # #F2E4DE
    halite_s = HALITE_SHADE           # #B99A93
    g: Grid = [[halite for _ in range(SIZE)] for _ in range(SIZE)]
    # ragged crystalline crust/halite boundary (periodic in x -> wraps)
    yb = [0] * SIZE
    for x in range(SIZE):
        n = fbm_p(x / 4.0, 2.5, seed, 4, 4)
        j = hash2(x, 1.0, seed + 3)
        yb[x] = max(4, min(11, 4 + int(round(n * 5.0 + j * 1.4))))
    for y in range(SIZE):
        for x in range(SIZE):
            n = fbm_p(x / 3.5, y / 3.5, seed + 1, 4, 4)
            fine = fbm_p(x / 2.0, y / 2.0, seed + 2, 8, 8)
            if y < yb[x]:
                col = lerp_c(syl_shade, syl, 0.35 + n * 0.55 + fine * 0.15)
                h = hash2(x * 1.5, y * 2.1, seed + 5)
                if h > 0.90:
                    col = lerp_c(col, glassy, 0.85)        # glassy cubic glint
                elif h < 0.06:
                    col = lerp_c(col, syl_shade, 0.55)     # potash pore
            else:
                col = lerp_c(halite_s, halite_l, 0.35 + n * 0.5 + fine * 0.2)
                h = hash2(x * 2.3, y * 1.7, seed + 7)
                if h > 0.955:
                    col = lerp_c(col, HALITE_BRIGHT, 0.55)  # cleavage sparkle
                elif h < 0.05:
                    col = lerp_c(col, halite_s, 0.5)        # pit
            g[y][x] = col
    # boundary treatment: crust lip, cast shadow, ragged specks
    for x in range(SIZE):
        b = yb[x]
        g[max(b - 1, 0)][x] = lerp_c(g[max(b - 1, 0)][x], glassy, 0.35)   # lip gleam
        if b < SIZE:
            g[b][x] = lerp_c(g[b][x], halite_s, 0.65)                     # shadow
        if b + 1 < SIZE:
            g[b + 1][x] = lerp_c(g[b + 1][x], halite_s, 0.35)
        h = hash2(x, 9.0, seed + 11)
        if h > 0.72 and b + 2 < SIZE:                 # sylvite crumbs below
            g[b + 2][x] = lerp_c(syl, glassy, 0.3)
        if h < 0.22 and b >= 2:                       # halite grit above
            g[b - 2][x] = lerp_c(HALITE_LIGHT, syl, 0.3)
    # tiny hopper cubes suspended in the crust
    for j in range(4):
        for i in range(5):
            if hash2(i, j, seed + 13) > 0.55:
                continue
            hx = int(i * 3 + hash2(i, j, seed + 17) * 2.0) % SIZE
            hy = int(hash2(i + 4, j, seed + 19) * 8.0) % SIZE
            if hy + 1 >= yb[hx] or hy + 1 >= yb[(hx + 1) % SIZE]:
                continue
            put(g, hx, hy, lerp_c(glassy, HALITE_BRIGHT, 0.4))     # bright top edge
            put(g, hx + 1, hy, glassy)
            put(g, hx, hy + 1, lerp_c(syl, glassy, 0.35))
            put(g, hx + 1, hy + 1, syl_shade)                      # inner wall
    return g


# ---------------------------------------------------------------------------
# Sylvite crust (top) - patchy rose sylvite islands with glassy cubic glints
# over pale halite showing through a wrapped crack network.
# ---------------------------------------------------------------------------

def gen_sylvite_crust_top(seed: int = 28) -> Grid:
    syl = (216, 154, 146, 255)
    syl_shade = (201, 127, 124, 255)
    glassy = (232, 183, 174, 255)
    halite = HALITE_BODY
    halite_l = HALITE_LIGHT
    halite_s = HALITE_SHADE
    crack = (158, 128, 122, 255)
    g: Grid = [[halite for _ in range(SIZE)] for _ in range(SIZE)]
    island = {}
    for y in range(SIZE):
        for x in range(SIZE):
            d1, d2, bid = voronoi_p(x, y, seed)
            n = fbm_p(x / 3.5, y / 3.5, seed + 1, 4, 4)
            fine = fbm_p(x / 2.0, y / 2.0, seed + 2, 8, 8)
            is_isl = hash2(bid[0], bid[1], seed + 9) > 0.40
            if is_isl:
                col = lerp_c(syl_shade, syl, 0.35 + n * 0.55 + fine * 0.15)
                h = hash2(x * 1.5, y * 2.1, seed + 5)
                if h > 0.88:
                    col = lerp_c(col, glassy, 0.9)          # glassy cubic glint
                elif h < 0.06:
                    col = lerp_c(col, syl_shade, 0.55)
                if d2 - d1 < 1.1:
                    col = lerp_c(col, syl_shade, 0.45)      # island rim at crack
            else:
                col = lerp_c(halite_s, halite_l, 0.35 + n * 0.5 + fine * 0.2)
                h = hash2(x * 2.3, y * 1.7, seed + 7)
                if h > 0.955:
                    col = lerp_c(col, HALITE_BRIGHT, 0.55)
                elif h < 0.05:
                    col = lerp_c(col, halite_s, 0.5)
            if d2 - d1 < 0.55:
                col = crack                               # crack core
            elif d2 - d1 < 0.95:
                col = lerp_c(col, crack, 0.35)            # crack shoulder
            g[y][x] = col
            island[(x, y)] = is_isl and d1 > 1.35
    # tiny hopper cubes on the larger sylvite islands
    for y in range(2, SIZE - 1):
        for x in range(2, SIZE - 1):
            if not (island.get((x, y)) and island.get((x + 1, y))
                    and island.get((x, y + 1)) and island.get((x + 1, y + 1))):
                continue
            if hash2(x, y, seed + 21) > 0.955:
                g[y][x] = lerp_c(glassy, HALITE_BRIGHT, 0.4)
                g[y][x + 1] = glassy
                g[y + 1][x] = lerp_c(syl, glassy, 0.35)
                g[y + 1][x + 1] = syl_shade
    return g


# ---------------------------------------------------------------------------
# Mirror flake - carpet of broken brine-mirror glass shards with cool glint
# edges over pale salt grit.
# ---------------------------------------------------------------------------

def gen_mirror_flake(seed: int = 29) -> Grid:
    ink = (10, 11, 14, 255)       # #0A0B0E
    ink_l = (18, 20, 26, 255)     # #12141A
    glint = (58, 66, 80, 255)     # #3A4250
    glint_l = (89, 100, 122, 255)  # #59647A
    grit = (201, 191, 184, 255)   # #C9BFB8
    grit_s = (162, 152, 146, 255)
    g: Grid = [[grit for _ in range(SIZE)] for _ in range(SIZE)]
    shard = {}
    for y in range(SIZE):
        for x in range(SIZE):
            d1, d2, bid = voronoi_p(x, y, seed)
            is_shard = hash2(bid[0], bid[1], seed + 9) < 0.78
            n = fbm_p(x / 3.0, y / 3.0, seed + 1, 4, 4)
            edge = d2 - d1 < 0.7
            if is_shard:
                col = lerp_c(ink, ink_l, n * 0.9)           # black glass body
                # faint internal sheen along cleavage
                if fbm_p(x / 1.5, y / 6.0, seed + 3, 8, 2) > 0.66:
                    col = lerp_c(col, glint, 0.30)
                if edge:
                    # glint edge; brighter where the facet faces up-left
                    sx, sy = bid
                    up_left = (sx - x) + (sy - y) > 0
                    col = lerp_c(glint, glint_l, 0.9 if up_left else 0.1)
                    if hash2(x, y, seed + 13) > 0.85:
                        col = glint_l                        # edge spark
            else:
                col = lerp_c(grit_s, grit, 0.5 + n * 0.5)
                h = hash2(x * 1.9, y * 2.7, seed + 5)
                if h > 0.90:
                    col = lerp_c(col, (234, 227, 221, 255), 0.75)  # salt grain
                if edge:
                    col = lerp_c(col, grit_s, 0.55)          # grit shadow at shard
            g[y][x] = col
            shard[(x, y)] = is_shard
    # pale salt grit caught between a few shards
    for y in range(SIZE):
        for x in range(SIZE):
            if not shard[(x, y)]:
                continue
            for kx, ky in ((1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
                nx, ny = (x + kx) % SIZE, (y + ky) % SIZE
                if not shard[(nx, ny)] and hash2(nx * 3.1, ny * 2.3, seed + 21) > 0.80:
                    g[ny][nx] = lerp_c(grit, (234, 227, 221, 255), 0.5)
    return g


# ---------------------------------------------------------------------------
# Gypsum rose - desert-rose rosette cross sprite on transparency: 5-6 sandy
# cream petal blades radiating from a speckled center, soft blended rim.
# ---------------------------------------------------------------------------

def gen_gypsum_rose(seed: int = 30) -> Tuple[Grid, List[Tuple[int, int]]]:
    body = (216, 201, 168, 255)     # #D8C9A8
    hi = (239, 230, 204, 255)       # #EFE6CC
    shade = (168, 154, 124, 255)    # #A89A7C
    rim = lerp_c(shade, body, 0.40)  # soft outline blended toward body tone
    cx, cy = 7.5, 8.5
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    meta = {}
    petals = []
    for k in range(6):
        a = k * math.pi / 3.0 + 0.12 * (hash2(k, 0, seed) - 0.5)
        length = 5.4 + hash2(k, 1, seed) * 1.4
        width = 1.25 + hash2(k, 2, seed) * 0.55
        petals.append((a, length, width))
    # back petals first so front ones overlap cleanly
    order = sorted(range(6), key=lambda k: math.sin(petals[k][0]))
    for k in order:
        a, length, width = petals[k]
        dxu, dyu = math.cos(a), math.sin(a) / 1.35     # low-wide stretch
        nrm = math.hypot(dxu, dyu)
        dxu, dyu = dxu / nrm, dyu / nrm
        pxu, pyu = -dyu, dxu
        lp = pxu + pyu                                   # which flank is lit
        for y in range(SIZE):
            for x in range(SIZE):
                vx, vy = x - cx, (y - cy) * 1.35
                u = vx * dxu + vy * dyu
                v = vx * pxu + vy * pyu
                if u < -0.2 or u > length:
                    continue
                t = max(0.0, u / length)
                w = width * math.sin(math.pi * (0.28 + 0.72 * t)) ** 0.75
                if abs(v) > w + 0.35:
                    continue
                lam = (v if lp > 0 else -v) / max(w, 0.3)   # + = toward shade flank
                if abs(v) > w - 0.45 or t > 0.94:
                    col = rim
                    is_rim, lit = True, lam < -0.1
                else:
                    if lam < 0:
                        col = lerp_c(body, hi, smooth(-lam * 1.4))
                    else:
                        col = lerp_c(body, shade, smooth(lam * 1.4))
                    if t < 0.15:
                        col = lerp_c(col, shade, 0.28)      # overlap at the heart
                    if t > 0.8 and lam < 0:
                        col = lerp_c(col, hi, 0.3)          # blade tip catches light
                    h = hash2(x * 1.7, y * 2.3, seed + 7)   # fine sand-grain speckle
                    if h < 0.10:
                        col = lerp_c(col, shade, 0.30)
                    elif h > 0.90:
                        col = lerp_c(col, hi, 0.35)
                    is_rim, lit = False, lam < -0.15
                g[y % SIZE][x % SIZE] = col
                meta[(x % SIZE, y % SIZE)] = (k, t, is_rim, lit)
    # sandy heart of the rosette
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot((x - cx) * 1.0, (y - cy) * 1.35)
            if d < 1.6:
                h = hash2(x * 3.1, y * 2.7, seed + 11)
                col = lerp_c(shade, body, h * 0.45)     # darker sandy heart
                if d < 0.8:
                    col = lerp_c(col, hi, 0.35 * (1.0 - d))
                g[y][x] = col
                meta.pop((x, y), None)
    # bright petal-edge glints (drives the glowmask)
    cand = []
    for (x, y), (k, t, is_rim, lit) in meta.items():
        if is_rim and lit and 0.30 <= t <= 0.95:
            cand.append((t, hash2(x, y, seed + 13), x, y))
    cand.sort(reverse=True)
    glints: List[Tuple[int, int]] = []
    seen = set()
    for t, _, x, y in cand:                 # first one per petal, then fillers
        k = meta[(x, y)][0]
        key = k if t >= 0.30 else None
        if key not in seen and len(glints) < 9:
            glints.append((x, y))
            seen.add(key)
    for t, _, x, y in cand:
        if len(glints) >= 9:
            break
        if (x, y) not in glints:
            glints.append((x, y))
    return g, glints[:9]


def gen_gypsum_rose_glowmask(glints: List[Tuple[int, int]]) -> Grid:
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    for x, y in glints:
        g[y][x] = (255, 250, 224, 255)
    return g


# ---------------------------------------------------------------------------
# Emit + verify
# ---------------------------------------------------------------------------

def coverage(grid: Grid) -> Tuple[int, float]:
    n = sum(1 for row in grid for r, g, b, a in row if a == 255)
    return n, 100.0 * n / (SIZE * SIZE)


def verify_png(path: Path) -> None:
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path}: bad signature"
    length, tag = struct.unpack(">I4s", data[8:16])
    assert tag == b"IHDR" and length == 13, f"{path}: missing IHDR"
    w, h, depth, ctype = struct.unpack(">IIBB", data[16:26])
    assert (w, h) == (SIZE, SIZE), f"{path}: expected 16x16, got {w}x{h}"
    assert depth == 8 and ctype == 6, f"{path}: expected 8-bit RGBA"


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    druse, druse_glints = gen_halite_druse()
    rose, rose_glints = gen_gypsum_rose()
    outputs = [
        (OUT / "hopper_halite.png", gen_hopper_halite(),
         "hopper halite side: pale pink-white halite of stacked 4px cubic growth "
         "steps, each face a hollow hopper square (bright #F7EFEA top edge, "
         "#B99A93 inner wall), diagonal terraces receding to the lower right"),
        (OUT / "hopper_halite_top.png", gen_hopper_halite_top(),
         "hopper halite top: one large square-spiral hopper terraced inward in "
         "square-in-square descending steps with a four-arm pinwheel twist, "
         "crisp riser rings and a deep central hollow"),
        (OUT / "halite_druse.png", druse,
         "halite druse: grey-pink rocky matrix crowded with ~50 tiny crystal "
         "facets, each a diamond/square with a #FFF4EE glint pixel and a "
         "#8A7C78 shadow pixel (dense geode sparkle)"),
        (OUT / "halite_druse_glowmask.png", gen_halite_druse_glowmask(druse_glints),
         "halite druse glowmask: transparent except the brightest glint pixels "
         "of 20 facets (opaque #FFF4EE accents)"),
        (OUT / "gypsum_blade.png", gen_gypsum_blade(),
         "gypsum blade side: tall ivory bladed bundle with pearly #F8F4E6 left "
         "edges, #EDE6D2 bodies, #D8CDB0 shade flanks, vertical cleavage lines "
         "and staggered splintery tips"),
        (OUT / "gypsum_blade_top.png", gen_gypsum_blade_top(),
         "gypsum blade top: cross-section of the bundle as packed lens-shaped "
         "blade ends with pearly highlights on their upper-left flanks"),
        (OUT / "sylvite_crust.png", gen_sylvite_crust(),
         "sylvite crust side: rose-salmon #D89A92/#C97F7C potash crust with "
         "glassy #E8B7AE glints and tiny hopper cubes capping pale #E3CBC4 "
         "halite across a ragged crystalline boundary"),
        (OUT / "sylvite_crust_top.png", gen_sylvite_crust_top(),
         "sylvite crust top: patchy rose sylvite islands with glassy cubic "
         "glints over pale halite showing through a crack network"),
        (OUT / "mirror_flake.png", gen_mirror_flake(),
         "mirror flake: carpet of broken black glass shards (#0A0B0E/#12141A) "
         "with cool grey-blue #3A4250/#59647A glint edges over pale #C9BFB8 "
         "salt grit"),
        (OUT / "gypsum_rose.png", rose,
         "gypsum rose (cross sprite): desert-rose rosette of 6 sandy cream "
         "blades radiating from a speckled heart, sand-grain speckle and a soft "
         "body-toned rim, low-wide and centered"),
        (OUT / "gypsum_rose_glowmask.png", gen_gypsum_rose_glowmask(rose_glints),
         "gypsum rose glowmask: transparent except %d bright petal-edge glints "
         "(opaque #FFFAE0)" % len(rose_glints)),
    ]
    palettes = {
        "hopper_halite.png": HALITE_PAL,
        "hopper_halite_top.png": HALITE_PAL,
        "halite_druse.png": DRUSE_PAL,
        "gypsum_blade.png": GYPSUM_PAL,
        "gypsum_blade_top.png": GYPSUM_PAL,
        "sylvite_crust.png": SYLVITE_PAL,
        "sylvite_crust_top.png": SYLVITE_PAL,
        "mirror_flake.png": FLAKE_PAL,
        "gypsum_rose.png": ROSE_PAL,
    }
    for path, grid, _ in outputs:
        pal = palettes.get(path.name)
        write_png(path, snap(grid, pal) if pal else grid)
    print()
    for path, grid, desc in outputs:
        verify_png(path)
        print(f"OK {path.name}: 16x16 RGBA - {desc}")
    print()
    for path, grid, _ in outputs:
        if "glowmask" in path.name:
            n, pct = coverage(grid)
            print(f"glowmask coverage {path.name}: {n}/256 = {pct:.1f}%")


if __name__ == "__main__":
    main()
