#!/usr/bin/env python3
"""Generate an opaque seaweed stem texture atlas.

The atlas is painted as a continuous organic surface so the arm slice and the
center slice meet without a visible collar at their UV boundary.
"""

from __future__ import annotations

import struct
import zlib
from pathlib import Path
from typing import List, Tuple
import math

SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/block/seaweed_stem.png")

RGBA = Tuple[int, int, int, int]
Grid = List[List[RGBA]]

DEEP: RGBA = (23, 55, 34, 255)
SHADOW: RGBA = (34, 77, 46, 255)
BASE: RGBA = (49, 103, 63, 255)
LIGHT: RGBA = (73, 133, 82, 255)
HIGHLIGHT: RGBA = (102, 156, 101, 255)
CAP_DARK: RGBA = (38, 78, 47, 255)
CAP_LIGHT: RGBA = (86, 133, 86, 255)


def new_grid() -> Grid:
    return [[BASE for _ in range(SIZE)] for _ in range(SIZE)]


def write_png(path: Path, grid: Grid) -> None:
    raw = bytearray()
    for row in grid:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", payload) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def paint_background(grid: Grid) -> None:
    for y in range(SIZE):
        for x in range(SIZE):
            band = (x * 3 + y * 5) % 7
            if band in (0, 1):
                color = SHADOW
            elif band == 2:
                color = DEEP
            elif band in (3, 4):
                color = BASE
            elif band == 5:
                color = LIGHT
            else:
                color = SHADOW

            if (x + y) % 5 == 0:
                color = LIGHT
            if x in (0, 15) or y in (0, 15):
                color = SHADOW

            grid[y][x] = color


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def lerp_color(a: RGBA, b: RGBA, t: float) -> RGBA:
    t = clamp01(t)
    return (
        round(a[0] + (b[0] - a[0]) * t),
        round(a[1] + (b[1] - a[1]) * t),
        round(a[2] + (b[2] - a[2]) * t),
        255,
    )


def stem_surface_color(nx: float, ny: float) -> RGBA:
    lean = math.sin(ny * math.pi * 1.3) * 0.22 + math.sin(ny * math.pi * 2.7 + 0.6) * 0.08
    ridge = abs(nx - (0.5 + lean))
    fiber = 0.5 + 0.5 * math.sin((ny * 18.0) + (nx * 9.0))
    grain = 0.5 + 0.5 * math.sin((ny * 31.0) - (nx * 5.5) + 0.8)

    if ridge < 0.08:
        color = lerp_color(LIGHT, HIGHLIGHT, fiber * 0.7 + 0.15)
    elif ridge < 0.18:
        color = lerp_color(BASE, LIGHT, fiber * 0.65)
    elif ridge < 0.30:
        color = lerp_color(SHADOW, BASE, fiber * 0.55 + grain * 0.15)
    else:
        color = lerp_color(DEEP, SHADOW, fiber * 0.35 + grain * 0.20)

    if ridge > 0.42:
        color = lerp_color(color, DEEP, 0.30)
    return color


def paint_continuous_side_strip(grid: Grid, x0: int, y0: int, width: int, height: int) -> None:
    for dy in range(height):
        for dx in range(width):
            x = x0 + dx
            y = y0 + dy
            nx = dx / max(1.0, width - 1.0)
            ny = (dy + 0.5) / max(1.0, height)
            color = stem_surface_color(nx, ny)
            if dx in (0, width - 1):
                color = lerp_color(color, DEEP, 0.25)
            grid[y][x] = color


def paint_cap_region(grid: Grid, x0: int, y0: int, size: int) -> None:
    center = (size - 1) / 2.0
    for dy in range(size):
        for dx in range(size):
            x = x0 + dx
            y = y0 + dy
            rx = (dx - center) / max(1.0, center + 0.35)
            ry = (dy - center) / max(1.0, center + 0.35)
            radial = math.sqrt((rx * rx) + (ry * ry))
            color = lerp_color(CAP_LIGHT, CAP_DARK, clamp01((radial - 0.10) / 0.95))

            vein = 0.5 + 0.5 * math.sin((rx - ry) * 7.0 + (dx + dy) * 0.35)
            color = lerp_color(color, LIGHT, vein * 0.18)
            if radial > 0.90:
                color = lerp_color(color, DEEP, 0.28)
            grid[y][x] = color


def validate_alpha(grid: Grid) -> None:
    for row in grid:
        for _, _, _, alpha in row:
            if alpha != 255:
                raise ValueError("Seaweed stem texture must be fully opaque")


def main() -> None:
    grid = new_grid()
    paint_background(grid)

    paint_cap_region(grid, 5, 0, 6)
    paint_continuous_side_strip(grid, 0, 5, 11, 6)
    paint_continuous_side_strip(grid, 11, 5, 5, 6)
    paint_cap_region(grid, 5, 10, 6)

    validate_alpha(grid)
    write_png(OUT_PATH, grid)
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
