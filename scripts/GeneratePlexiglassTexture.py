#!/usr/bin/env python3
"""Generate plexiglass block texture — fully transparent with subtle luster/reflection streaks."""

from __future__ import annotations

from pathlib import Path
import struct
import zlib
from typing import Tuple

SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/block/plexiglass.png")

RGBA = Tuple[int, int, int, int]
Grid = list[list[RGBA]]

TRANSPARENT: RGBA = (0, 0, 0, 0)


def new_grid() -> Grid:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def put(g: Grid, x: int, y: int, c: RGBA) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        g[y][x] = c


def write_png(path: Path, g: Grid) -> None:
    raw = bytearray()
    for row in g:
        raw.append(0)
        for r, g_, b, a in row:
            raw.extend((r, g_, b, a))
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", payload) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def draw_plexiglass() -> Grid:
    g = new_grid()

    # Very subtle gloss — no outline, just faint reflection streaks
    # White/ice tones at very low alpha
    REFLECTION_BRIGHT = (255, 252, 248, 55)   # brightest specular
    REFLECTION_MID    = (240, 245, 250, 38)   # mid reflection
    REFLECTION_DIM    = (220, 235, 245, 25)   # dim reflection
    REFLECTION_FAINT  = (210, 228, 240, 16)   # faint
    REFLECTION_EDGE   = (200, 220, 235, 12)   # barely visible

    # --- Main diagonal reflection streak (top-left → bottom-right) ---
    main_streak = {
        # row 0-2: faint start
        (1, 0): REFLECTION_FAINT, (2, 0): REFLECTION_EDGE,
        (1, 1): REFLECTION_FAINT, (2, 1): REFLECTION_EDGE, (3, 1): REFLECTION_EDGE,
        (1, 2): REFLECTION_EDGE, (2, 2): REFLECTION_DIM, (3, 2): REFLECTION_FAINT,
        # row 3-6: brightening
        (1, 3): REFLECTION_EDGE, (2, 3): REFLECTION_DIM, (3, 3): REFLECTION_MID, (4, 3): REFLECTION_FAINT,
        (2, 4): REFLECTION_DIM, (3, 4): REFLECTION_MID, (4, 4): REFLECTION_BRIGHT, (5, 4): REFLECTION_FAINT,
        (2, 5): REFLECTION_FAINT, (3, 5): REFLECTION_MID, (4, 5): REFLECTION_BRIGHT, (5, 5): REFLECTION_MID, (6, 5): REFLECTION_DIM,
        (3, 6): REFLECTION_FAINT, (4, 6): REFLECTION_MID, (5, 6): REFLECTION_BRIGHT, (6, 6): REFLECTION_MID, (7, 6): REFLECTION_DIM,
        # row 7-9: peak brightness
        (3, 7): REFLECTION_EDGE, (4, 7): REFLECTION_DIM, (5, 7): REFLECTION_BRIGHT, (6, 7): REFLECTION_MID, (7, 7): REFLECTION_DIM,
        (4, 8): REFLECTION_DIM, (5, 8): REFLECTION_MID, (6, 8): REFLECTION_BRIGHT, (7, 8): REFLECTION_MID, (8, 8): REFLECTION_DIM,
        (4, 9): REFLECTION_FAINT, (5, 9): REFLECTION_DIM, (6, 9): REFLECTION_MID, (7, 9): REFLECTION_BRIGHT, (8, 9): REFLECTION_DIM,
        # row 10-13: fading
        (5, 10): REFLECTION_FAINT, (6, 10): REFLECTION_DIM, (7, 10): REFLECTION_MID, (8, 10): REFLECTION_DIM, (9, 10): REFLECTION_FAINT,
        (6, 11): REFLECTION_FAINT, (7, 11): REFLECTION_DIM, (8, 11): REFLECTION_MID, (9, 11): REFLECTION_DIM,
        (7, 12): REFLECTION_FAINT, (8, 12): REFLECTION_DIM, (9, 12): REFLECTION_FAINT,
        (8, 13): REFLECTION_FAINT, (9, 13): REFLECTION_EDGE,
        (9, 14): REFLECTION_EDGE, (10, 14): REFLECTION_EDGE,
    }
    for (x, y), c in main_streak.items():
        put(g, x, y, c)

    # --- Secondary reflection (opposite diagonal, top-right → bottom-left) ---
    sec_streak = {
        (13, 3): REFLECTION_EDGE, (14, 3): REFLECTION_EDGE,
        (12, 4): REFLECTION_EDGE, (13, 4): REFLECTION_DIM, (14, 4): REFLECTION_FAINT,
        (12, 5): REFLECTION_DIM, (13, 5): REFLECTION_MID, (14, 5): REFLECTION_FAINT,
        (11, 6): REFLECTION_DIM, (12, 6): REFLECTION_MID, (13, 6): REFLECTION_MID,
        (11, 7): REFLECTION_FAINT, (12, 7): REFLECTION_MID, (13, 7): REFLECTION_DIM,
        (10, 8): REFLECTION_FAINT, (11, 8): REFLECTION_DIM, (12, 8): REFLECTION_DIM,
        (10, 9): REFLECTION_EDGE, (11, 9): REFLECTION_FAINT, (12, 9): REFLECTION_FAINT,
        (10, 10): REFLECTION_EDGE, (11, 10): REFLECTION_EDGE,
    }
    for (x, y), c in sec_streak.items():
        put(g, x, y, c)

    # --- Edge/rim subtle brightening (very faint, simulates light catching edges) ---
    # Top-left corner gleam
    corner_tl = {
        (0, 0): REFLECTION_EDGE,
        (0, 1): REFLECTION_EDGE, (1, 1): REFLECTION_EDGE,
    }
    for (x, y), c in corner_tl.items():
        put(g, x, y, c)

    return g


def main() -> None:
    write_png(OUT_PATH, draw_plexiglass())
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
