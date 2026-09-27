#!/usr/bin/env python3
"""Generate detailed, realistic Brine Mirror Gorge mineral sprites (16x16).

Each texture uses multi-octave value noise, domain warping, and mineral-specific
structure (cleavage facets, flutes, varves, refraction cores) so surfaces read as
stone rather than flat noise.
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


def domain_warp(x: float, y: float, seed: int, amount: float = 0.35) -> Tuple[float, float]:
    wx = fbm(x + 3.1, y + 1.7, seed + 101, 3) - 0.5
    wy = fbm(x + 5.3, y + 2.9, seed + 211, 3) - 0.5
    return x + wx * amount, y + wy * amount


def grain(x: float, y: float, seed: int) -> float:
    return fbm(x, y, seed, 5)


def facet_angle(x: float, y: float, seed: int) -> float:
    """Voronoi-ish cleavage facet id + local plane for crystal shading."""
    px, py = x * 1.35, y * 1.35
    best, second, bid = 1e9, 1e9, 0.0
    ix, iy = math.floor(px), math.floor(py)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            cx = ix + dx + hash2(ix + dx, iy + dy, seed)
            cy = iy + dy + hash2(ix + dx + 9, iy + dy + 3, seed + 7)
            d = (cx - px) ** 2 + (cy - py) ** 2
            if d < best:
                second = best
                best = d
                bid = hash2(cx, cy, seed + 41)
            elif d < second:
                second = d
    edge = smooth((math.sqrt(second) - math.sqrt(best)) * 2.2)
    return bid * 0.65 + edge * 0.35


# ---------------------------------------------------------------------------
# Halite crust - sugary salt with rose impurities, cleavage sparkles
# ---------------------------------------------------------------------------

def gen_halite_crust(seed: int = 1) -> Grid:
    deep = (168, 142, 152, 255)
    mid = (228, 210, 216, 255)
    pale = (248, 238, 242, 255)
    rose = (214, 148, 168, 255)
    rose_deep = (178, 110, 132, 255)
    sparkle = (255, 255, 255, 255)
    g: Grid = [[pale for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            wx, wy = domain_warp(x / 4.0, y / 4.0, seed)
            n = grain(wx * 1.4, wy * 1.4, seed)
            fac = facet_angle(x / 2.8, y / 2.8, seed + 3)
            imp = fbm(wx * 0.9 + 4, wy * 0.9, seed + 55, 3)
            base = lerp_c(deep, pale, smooth(0.35 + n * 0.55 + fac * 0.15))
            if imp > 0.58:
                base = lerp_c(base, rose, smooth((imp - 0.58) / 0.35) * 0.85)
            if imp > 0.72:
                base = lerp_c(base, rose_deep, smooth((imp - 0.72) / 0.28) * 0.5)
            # micro sugar pits
            pit = hash2(x * 3.1, y * 2.7, seed + 9)
            if pit < 0.06:
                base = lerp_c(base, deep, 0.45)
            elif pit > 0.965:
                base = lerp_c(base, sparkle, 0.75)
            # cleavage glint
            if fac > 0.82 and hash2(x + 1, y + 2, seed + 12) > 0.55:
                base = lerp_c(base, sparkle, 0.55)
            g[y][x] = base
    return g


# ---------------------------------------------------------------------------
# Halite pipe - vertical flutes with prism striations and growth rings
# ---------------------------------------------------------------------------

def gen_halite_pipe_side(seed: int = 2) -> Grid:
    hi = (252, 246, 248, 255)
    mid = (214, 198, 208, 255)
    dark = (148, 128, 142, 255)
    shadow = (110, 94, 108, 255)
    rose = (230, 180, 196, 255)
    g: Grid = [[mid for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            flute = 0.5 + 0.5 * math.sin((x / SIZE) * math.pi * 5.0)
            fine = 0.5 + 0.5 * math.sin((x / SIZE) * math.pi * 16.0 + y * 0.08)
            stri = 0.5 + 0.5 * math.sin(y * 1.7 + x * 0.15)
            n = grain(x / 3.0, y / 5.0, seed)
            shade = flute * 0.55 + fine * 0.18 + stri * 0.12 + n * 0.2
            col = lerp_c(shadow, hi, smooth(shade))
            if fine < 0.18:
                col = lerp_c(col, dark, 0.55)
            if flute > 0.85 and stri > 0.55:
                col = lerp_c(col, rose, 0.25)
            # vertical growth ticks
            if hash2(x, y * 2.3, seed + 8) > 0.92:
                col = lerp_c(col, hi, 0.4)
            g[y][x] = col
    return g


def gen_halite_pipe_top(seed: int = 3) -> Grid:
    hi = (250, 244, 246, 255)
    mid = (208, 190, 200, 255)
    ring = (160, 138, 152, 255)
    core = (235, 210, 220, 255)
    g: Grid = [[mid for _ in range(SIZE)] for _ in range(SIZE)]
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            ang = math.atan2(y - cy, x - cx)
            rings = 0.5 + 0.5 * math.sin(d * 2.8 + fbm(x / 3, y / 3, seed) * 2.0)
            spoke = 0.5 + 0.5 * math.sin(ang * 6.0)
            n = grain(x / 2.5, y / 2.5, seed + 2)
            v = rings * 0.55 + spoke * 0.15 + n * 0.3
            col = lerp_c(ring, hi, smooth(v))
            if d < 2.2:
                col = lerp_c(core, col, smooth(d / 2.2))
            if abs(rings - 0.5) < 0.06:
                col = lerp_c(col, ring, 0.45)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Varve shale - fine horizontal time bands with silt flecks and microfolds
# ---------------------------------------------------------------------------

def gen_varve_shale(seed: int = 4) -> Grid:
    bands = [
        (78, 72, 88, 255),
        (118, 108, 124, 255),
        (96, 88, 104, 255),
        (140, 126, 142, 255),
        (64, 60, 76, 255),
        (108, 98, 118, 255),
    ]
    fleck = (170, 158, 175, 255)
    dark = (48, 44, 58, 255)
    g: Grid = [[bands[0] for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            fold = math.sin(x * 0.22 + y * 0.05) * 0.55
            warp = (grain(x / 4.0, y / 2.0, seed) - 0.5) * 0.8
            yi = y + fold + warp
            band_f = yi / 2.15
            idx = int(math.floor(band_f)) % len(bands)
            nxt = (idx + 1) % len(bands)
            t = smooth(band_f - math.floor(band_f))
            col = lerp_c(bands[idx], bands[nxt], t)
            # lamination micro-lines
            if (yi * 2.7) % 1.0 < 0.12:
                col = lerp_c(col, dark, 0.35)
            n = grain(x / 2.2, y / 2.2, seed + 11)
            col = lerp_c(col, fleck, max(0.0, n - 0.55) * 0.9)
            if hash2(x * 1.7, y * 3.3, seed + 15) > 0.94:
                col = lerp_c(col, fleck, 0.55)
            if hash2(x + 4, y + 1, seed + 19) > 0.97:
                col = lerp_c(col, dark, 0.5)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Brine mirror - near-black glass with meniscus rim and faint depth glints
# ---------------------------------------------------------------------------

def gen_brine_mirror_side(seed: int = 5) -> Grid:
    body = (8, 12, 18, 255)
    deep = (4, 6, 10, 255)
    rim = (36, 48, 58, 255)
    sheen = (55, 72, 82, 220)
    g: Grid = [[body for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            n = fbm(x / 5.0, y / 5.0, seed, 4)
            edge = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            col = lerp_c(deep, body, 0.35 + n * 0.5)
            if edge == 0:
                col = lerp_c(rim, col, 0.25 + n * 0.3)
            elif edge == 1 and (x + y) % 3 == 0:
                col = lerp_c(col, rim, 0.35)
            # vertical wet sheen
            if fbm(x / 1.5, y / 8.0, seed + 3, 3) > 0.62:
                col = lerp_c(col, sheen, 0.35)
            g[y][x] = col
    return g


def gen_brine_mirror_top(seed: int = 6) -> Grid:
    ink = (6, 10, 16, 255)
    abyss = (3, 5, 9, 255)
    meniscus = (42, 58, 68, 255)
    glint = (70, 95, 110, 200)
    dust = (22, 30, 38, 255)
    g: Grid = [[ink for _ in range(SIZE)] for _ in range(SIZE)]
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            n = fbm(x / 4.5, y / 4.5, seed, 5)
            col = lerp_c(abyss, ink, 0.3 + n * 0.55)
            # elliptical meniscus
            e = math.hypot((x - cx) / 7.2, (y - cy) / 6.8)
            if e > 0.88:
                col = lerp_c(col, meniscus, smooth((e - 0.88) / 0.14) * 0.85)
            if e > 0.97:
                col = lerp_c(meniscus, dust, 0.3)
            # sparse depth glints
            if hash2(x * 2.1, y * 1.9, seed + 9) > 0.93:
                col = lerp_c(col, glint, 0.65)
            # faint current streak
            streak = math.sin((x * 0.4 + y * 0.15 + n * 3.0))
            if streak > 0.85:
                col = lerp_c(col, glint, 0.18)
            g[y][x] = col
    return g


# ---------------------------------------------------------------------------
# Calcite quill - translucent ice needle with refractive core and tip flare
# ---------------------------------------------------------------------------

def gen_calcite_quill(seed: int = 7) -> Grid:
    body = (188, 220, 228, 235)
    core = (235, 252, 255, 250)
    edge = (130, 165, 178, 210)
    tip = (255, 255, 255, 255)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        t = y / 15.0
        width = 2 if y > 2 else 1
        cx = 7
        # slight lean
        lean = int(round(math.sin(t * 1.2) * 0.6))
        for dx in range(-width, width + 1):
            x = cx + dx + lean
            if x < 0 or x >= SIZE:
                continue
            if abs(dx) == width:
                col = edge
            elif abs(dx) == 0:
                col = core if y < 10 else body
            else:
                col = lerp_c(edge, core, 1.0 - abs(dx) / (width + 0.5))
                col = lerp_c(col, core, smooth(t) * 0.25)
            # internal fracture glint
            if hash2(x, y * 1.3, seed) > 0.88 and abs(dx) < width:
                col = lerp_c(col, tip, 0.55)
            g[y][x] = col
    # tip flare
    for x in range(6, 10):
        g[0][x] = tip
        g[1][x] = lerp_c(tip, body, 0.4)
    # base bloom
    for x in range(6, 10):
        if g[15][x][3] == 0:
            g[15][x] = lerp_c(edge, body, 0.5)
    return g


# ---------------------------------------------------------------------------
# Halite rosette - radial crystal fan, champagne / rose petals with facets
# ---------------------------------------------------------------------------

def gen_halite_rosette(seed: int = 8) -> Grid:
    rose = (255, 205, 175, 255)
    champ = (255, 238, 205, 255)
    deep = (198, 145, 125, 255)
    shine = (255, 250, 235, 255)
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    cx, cy = 7.5, 11.0
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, (y - cy) * 1.55
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            petals = abs(math.sin(ang * 5.0 + 0.2))
            n = grain(x / 2.0, y / 2.0, seed)
            # starburst arms with soft falloff
            arm = petals * (1.0 - smooth(d / 6.2))
            if d < 6.4 and arm > 0.28 and d > 1.1:
                t = arm + n * 0.15
                col = lerp_c(deep, rose, smooth(t * 1.1))
                col = lerp_c(col, champ, smooth(arm) * 0.45)
                if petals > 0.78:
                    col = lerp_c(col, shine, 0.35)
                # crystal facet edge
                if abs(petals - 0.5) < 0.08 and d > 3:
                    col = lerp_c(col, deep, 0.35)
                g[y][x] = col
            # nucleus
            if d <= 1.5:
                col = lerp_c(shine, champ, smooth(d / 1.5))
                if d < 0.6:
                    col = shine
                g[y][x] = col
            # basal scar
            if d < 2.2 and y > cy:
                g[y][x] = lerp_c(g[y][x] if g[y][x][3] else champ, deep, 0.35)
    return g


# ---------------------------------------------------------------------------
# Salt fringe - a hanging curtain of crystalline salt, drawn to CONNECT: one
# shared ribbon layout (wrapping across the tile edge) and one shared wobble
# (periodic in y) keep top/body/tail aligned, so a drop stacks into a single
# drape and neighbouring drops read as one continuous curtain.
# ---------------------------------------------------------------------------

SALT_MAIN = (228, 208, 214, 255)
SALT_SHADE = (175, 150, 162, 255)
SALT_DEEP = (128, 102, 116, 255)
SALT_TIP = (255, 232, 222, 255)
SALT_WET = (245, 220, 215, 255)

# Ribbon spans as (start, width) around the tile. The first straddles the tile
# edge (13..2) and is wide enough to keep the seam covered whatever the wobble
# does, so neighbouring blocks merge into one drape instead of showing a seam.
SALT_RIBBON_SPANS = ((13, 6), (3, 3), (7, 2), (10, 2))
# Where each ribbon dies out in the tail, staggered so the drop ends ragged.
SALT_TAIL_TIPS = (10, 14, 8, 12)


def salt_ribbon_at(x: int, y: int) -> Tuple[int, int, int] | None:
    """(ribbon index, offset in ribbon, ribbon width) at (x, y), or None in a slit.

    Edges drift on a y-periodic wobble, so the layout continues across the
    top/body/tail seam and tiles on itself.
    """
    for index, (start, width) in enumerate(SALT_RIBBON_SPANS):
        phase = index * 1.9
        shift = int(round(math.sin(2.0 * math.pi * y / SIZE + phase)))
        pinch = 1 if math.sin(4.0 * math.pi * y / SIZE + phase * 2.0) > 0.8 else 0
        width_at = max(1, width - pinch)
        for offset in range(width_at):
            if (start + shift + offset) % SIZE == x:
                return index, offset, width_at
    return None


def salt_crust_depth(x: int) -> int:
    """Underside of the valance the curtain hangs from; wraps at the tile edge."""
    return 2 + (1 if (x * 5 + x * x) % 16 < 7 else 0)


def salt_top_drip_end(x: int) -> int:
    """Where a thin secondary drip weeping off the valance stops."""
    return salt_crust_depth(x) + 3 + (x * 3 + 1) % 5


def salt_ribbon_pixel(x: int, y: int, offset: int, width: int, seed: int) -> RGBA:
    """Ribbon cross-section: deep edges, lit flank, mineral banding, sparkles."""
    t = (offset + 0.5) / max(1, width)
    if t < 0.3:
        col = lerp_c(SALT_DEEP, SALT_MAIN, smooth(t / 0.3))
    else:
        col = lerp_c(SALT_MAIN, SALT_SHADE, smooth((t - 0.3) / 0.7) * 0.8)
    col = lerp_c(col, SALT_TIP, grain(x / 2.0, y / 3.0, seed) * 0.22)
    if y % 4 == 3:
        col = lerp_c(col, SALT_SHADE, 0.35)      # mineral growth rings
    if hash2(x, y, seed + 3) > 0.93:
        col = lerp_c(col, SALT_TIP, 0.6)         # crystalline sparkle
    return col


def gen_salt_fringe(kind: str, seed: int = 7) -> Grid:
    g: Grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    if kind == "top":
        # the crust valance the whole curtain hangs from
        for x in range(SIZE):
            depth = salt_crust_depth(x)
            for y in range(depth):
                col = lerp_c(SALT_SHADE, SALT_MAIN,
                             0.35 + grain(x / 2.0, y / 3.0, seed) * 0.5)
                if y == depth - 1:
                    col = lerp_c(col, SALT_DEEP, 0.45)
                if hash2(x, y, seed + 1) > 0.9:
                    col = lerp_c(col, SALT_TIP, 0.5)
                g[y][x] = col

    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3]:
                continue
            cell = salt_ribbon_at(x, y)
            if cell is not None:
                index, offset, width = cell
                if kind == "tail":
                    # ribbons die out on staggered wet tips, some still dripping
                    tip_row = SALT_TAIL_TIPS[index]
                    if y == tip_row + 1 and index % 2 == 0:
                        g[y][x] = lerp_c(SALT_WET, SALT_TIP, 0.4)   # hanging bead
                        continue
                    if y > tip_row:
                        continue
                    col = salt_ribbon_pixel(x, y, offset, width, seed)
                    if y >= tip_row - 2:
                        col = lerp_c(col, SALT_WET, 0.55)           # wet tip
                    if y == tip_row:
                        col = SALT_TIP
                    g[y][x] = col
                else:
                    g[y][x] = salt_ribbon_pixel(x, y, offset, width, seed)
            elif kind == "top" and y < salt_top_drip_end(x):
                end = salt_top_drip_end(x)
                g[y][x] = SALT_TIP if y == end - 1 else lerp_c(SALT_SHADE, SALT_WET, 0.5)
            elif (x * 3 + y * 5) % 16 < 4:
                g[y][x] = lerp_c(SALT_SHADE, SALT_MAIN, 0.25)      # membrane webbing
    return g


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    write_png(OUT / "halite_crust.png", gen_halite_crust())
    write_png(OUT / "halite_pipe.png", gen_halite_pipe_side())
    write_png(OUT / "halite_pipe_top.png", gen_halite_pipe_top())
    write_png(OUT / "varve_shale.png", gen_varve_shale())
    write_png(OUT / "brine_mirror.png", gen_brine_mirror_side())
    write_png(OUT / "brine_mirror_top.png", gen_brine_mirror_top())
    write_png(OUT / "calcite_quill.png", gen_calcite_quill())
    write_png(OUT / "halite_rosette.png", gen_halite_rosette())
    for kind in ("top", "body", "tail"):
        write_png(OUT / f"salt_fringe_{kind}.png", gen_salt_fringe(kind, 7))
        write_mcmeta(OUT / f"salt_fringe_{kind}.png.mcmeta", 6)


if __name__ == "__main__":
    main()
