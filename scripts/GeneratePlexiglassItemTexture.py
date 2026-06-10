#!/usr/bin/env python3
"""Generate plexiglass item texture — visible glass plate with reflection."""

from pathlib import Path
import struct
import zlib

SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/item/plexiglass.png")

RGBA = tuple[int, int, int, int]
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
        return struct.pack(">I", len(data)) + tag + data + struct.pack(
            ">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = (b"\x89PNG\r\n\x1a\n" +
           chunk(b"IHDR", header) +
           chunk(b"IDAT", payload) +
           chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def draw() -> Grid:
    g = new_grid()

    # Matching block palette — cyan glass tint + warm specular
    CYAN   = (107, 235, 255, 90)    # glass body tint
    WHITE  = (255, 255, 255, 200)   # bright specular
    WARM   = (255, 220, 201, 210)   # warm edge reflection
    WARM2  = (255, 226, 210, 240)   # brighter warm
    WHITE2 = (255, 255, 255, 235)   # peak specular
    FAINT  = (107, 235, 255, 45)    # faint glass edge

    # Glass plate shape — thin slab viewed slightly tilted
    # Outline of the plate (very subtle, just a few pixels for shape)
    plate = {
        # Top edge
        (3, 1), (4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 1),
        # Bottom edge
        (3, 13), (4, 13), (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 13), (11, 13),
        # Left edge
        (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11), (2, 12),
        # Right edge
        (12, 2), (12, 3), (12, 4), (12, 5), (12, 6), (12, 7), (12, 8), (12, 9), (12, 10), (12, 11), (12, 12),
    }

    # Faint plate body
    for y in range(2, 13):
        for x in range(3, 12):
            put(g, x, y, CYAN)

    # Subtle plate outline
    for x, y in plate:
        put(g, x, y, FAINT)

    # Diagonal specular reflection streaks (matching block style)
    # Main streak top-left to bottom-right
    streak1 = {
        (5, 3): WHITE, (6, 4): WHITE2, (7, 5): WARM,
        (8, 6): WHITE, (9, 7): WARM2,
    }
    # Secondary streak upper-right
    streak2 = {
        (11, 4): WARM, (12, 5): WHITE,
        (10, 5): WARM2,
    }
    # Third streak lower-left
    streak3 = {
        (4, 11): WHITE2, (5, 12): WARM,
    }
    for pts in (streak1, streak2, streak3):
        for (x, y), c in pts.items():
            put(g, x, y, c)

    # Corner highlights
    corner = {
        (3, 2): WARM, (4, 2): WARM2,
        (11, 2): WHITE,
        (3, 12): WHITE,
        (11, 12): WARM2,
    }
    for (x, y), c in corner.items():
        put(g, x, y, c)

    return g


def main() -> None:
    write_png(OUT_PATH, draw())
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
