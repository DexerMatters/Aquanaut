#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import struct
import zlib


SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/item/gas_flow_meter_model.png")

OUTLINE = (37, 50, 60, 255)
GRAPHITE = (44, 56, 65, 255)
RUBBER = (61, 74, 82, 255)
BODY_DARK = (73, 92, 102, 255)
BODY = (92, 115, 124, 255)
BODY_LIGHT = (142, 169, 176, 255)
STEEL = (112, 134, 142, 255)
BRASS = (145, 111, 63, 255)
COPPER = (125, 88, 56, 255)
AQUA = (117, 176, 182, 255)
GLOW = (206, 237, 240, 255)
SCREW = (170, 183, 188, 255)
GLASS = (156, 208, 213, 255)


def new_image() -> list[list[tuple[int, int, int, int]]]:
    return [[OUTLINE for _ in range(SIZE)] for _ in range(SIZE)]


def put(img, x: int, y: int, color) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = color


def fill_rect(img, x0: int, y0: int, x1: int, y1: int, color) -> None:
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, color)


def hline(img, x0: int, x1: int, y: int, color) -> None:
    for x in range(x0, x1 + 1):
        put(img, x, y, color)


def vline(img, x: int, y0: int, y1: int, color) -> None:
    for y in range(y0, y1 + 1):
        put(img, x, y, color)


def write_png(path: Path, img) -> None:
    raw = bytearray()
    for row in img:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", payload) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def draw_model_texture() -> list[list[tuple[int, int, int, int]]]:
    img = new_image()

    # Main receiver top 0..7 x 0..2
    fill_rect(img, 0, 0, 7, 2, BODY)
    hline(img, 0, 7, 0, BODY_LIGHT)
    hline(img, 0, 7, 2, BODY_DARK)
    put(img, 1, 1, STEEL)
    put(img, 2, 1, BODY_LIGHT)
    put(img, 3, 1, BODY_LIGHT)
    put(img, 5, 1, STEEL)
    put(img, 6, 1, BODY_DARK)
    put(img, 7, 1, OUTLINE)

    # Main receiver side 0..7 x 3..7
    fill_rect(img, 0, 3, 7, 7, BODY_DARK)
    hline(img, 0, 7, 3, BODY_LIGHT)
    hline(img, 0, 7, 4, BODY)
    hline(img, 0, 7, 6, GRAPHITE)
    put(img, 1, 4, STEEL)
    put(img, 2, 4, BODY_LIGHT)
    put(img, 3, 4, GLASS)
    put(img, 4, 4, AQUA)
    put(img, 5, 4, BODY_LIGHT)
    put(img, 6, 4, STEEL)
    put(img, 2, 5, BODY)
    put(img, 4, 5, GLOW)
    put(img, 5, 5, BODY)
    put(img, 1, 6, STEEL)
    put(img, 3, 6, SCREW)
    put(img, 6, 6, OUTLINE)
    put(img, 2, 7, GRAPHITE)
    put(img, 5, 7, BODY)

    # Receiver underside 0..7 x 8..10
    fill_rect(img, 0, 8, 7, 10, GRAPHITE)
    hline(img, 0, 7, 8, BODY_DARK)
    hline(img, 0, 7, 10, OUTLINE)
    put(img, 2, 9, BODY_DARK)
    put(img, 4, 9, OUTLINE)
    put(img, 5, 9, BODY)

    # Barrel top 8..12 x 0..2
    fill_rect(img, 8, 0, 12, 2, STEEL)
    hline(img, 8, 12, 0, BODY_LIGHT)
    hline(img, 8, 12, 2, BODY_DARK)
    put(img, 9, 1, BODY_LIGHT)
    put(img, 10, 1, STEEL)
    put(img, 11, 1, BODY)
    put(img, 12, 1, OUTLINE)

    # Barrel side 8..12 x 3..5
    fill_rect(img, 8, 3, 12, 5, STEEL)
    hline(img, 8, 12, 3, BODY_LIGHT)
    hline(img, 8, 12, 5, BODY_DARK)
    put(img, 9, 4, BODY_LIGHT)
    put(img, 10, 4, STEEL)
    put(img, 11, 4, BODY)
    put(img, 12, 4, OUTLINE)

    # Muzzle/front cap 13..15 x 3..5
    fill_rect(img, 13, 3, 15, 5, BRASS)
    hline(img, 13, 15, 3, BODY_LIGHT)
    hline(img, 13, 15, 5, COPPER)
    put(img, 14, 4, OUTLINE)
    put(img, 13, 4, BRASS)
    put(img, 15, 4, COPPER)

    # Small barrel nose 13..13 x 6..8 and 13..13 x 0..2
    fill_rect(img, 13, 0, 13, 2, BRASS)
    hline(img, 13, 13, 0, BODY_LIGHT)
    fill_rect(img, 13, 6, 13, 8, COPPER)
    put(img, 13, 7, OUTLINE)

    # Gauge housing top 8..11 x 6..7
    fill_rect(img, 8, 6, 11, 7, BODY)
    hline(img, 8, 11, 6, BODY_LIGHT)
    put(img, 8, 7, STEEL)
    put(img, 9, 7, GLASS)
    put(img, 10, 7, AQUA)
    put(img, 11, 7, BODY_DARK)

    # Gauge housing faces 8..11 x 8..9 and accents around 12..15
    fill_rect(img, 8, 8, 11, 9, BODY_DARK)
    hline(img, 8, 11, 8, BODY_LIGHT)
    put(img, 8, 9, STEEL)
    put(img, 9, 9, GLASS)
    put(img, 10, 9, GLOW)
    put(img, 11, 9, BODY)
    fill_rect(img, 12, 8, 14, 9, BODY)
    put(img, 12, 8, BODY_LIGHT)
    put(img, 13, 8, BRASS)
    put(img, 14, 8, BODY_DARK)
    put(img, 13, 9, COPPER)

    # Trigger block and rear cap accents 9..15 x 10..15
    fill_rect(img, 9, 10, 11, 11, BODY_DARK)
    hline(img, 9, 11, 10, BODY_LIGHT)
    put(img, 10, 11, STEEL)
    fill_rect(img, 8, 12, 11, 15, GRAPHITE)
    hline(img, 8, 11, 12, BODY_DARK)
    hline(img, 8, 11, 13, RUBBER)
    put(img, 8, 14, SCREW)
    put(img, 10, 14, BODY_DARK)
    put(img, 11, 15, OUTLINE)

    # Grip top and side 13..15 x 6..15
    fill_rect(img, 13, 6, 15, 7, RUBBER)
    hline(img, 13, 15, 6, BODY)
    fill_rect(img, 13, 8, 15, 13, GRAPHITE)
    hline(img, 13, 15, 8, RUBBER)
    vline(img, 14, 9, 12, RUBBER)
    vline(img, 15, 9, 12, OUTLINE)
    put(img, 13, 9, RUBBER)
    put(img, 13, 11, RUBBER)
    put(img, 13, 13, RUBBER)
    fill_rect(img, 13, 14, 15, 15, COPPER)
    hline(img, 13, 15, 14, BRASS)
    put(img, 14, 15, SCREW)
    put(img, 15, 15, OUTLINE)

    return img


def main() -> None:
    write_png(OUT_PATH, draw_model_texture())
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
