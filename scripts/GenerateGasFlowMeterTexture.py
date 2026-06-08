#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import struct
import zlib


SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/item/gas_flow_meter.png")

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (37, 50, 60, 255)
SHADOW = (52, 70, 79, 255)
BODY = (83, 107, 117, 255)
STEEL = (107, 128, 136, 255)
HIGHLIGHT = (184, 218, 221, 255)
BRASS = (146, 112, 58, 255)
COPPER = (132, 92, 54, 255)
AQUA = (118, 181, 188, 255)
GLOW = (211, 241, 244, 255)
GRAPHITE = (40, 55, 64, 255)


def new_image() -> list[list[tuple[int, int, int, int]]]:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def put(img, x: int, y: int, color) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = color


def line(img, x0: int, y0: int, x1: int, y1: int, color) -> None:
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        put(img, x0, y0, color)
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def fill_polygon(img, points: list[tuple[int, int]], color) -> None:
    if not points:
        return
    min_y = max(0, min(y for _, y in points))
    max_y = min(SIZE - 1, max(y for _, y in points))
    edges = list(zip(points, points[1:] + points[:1]))
    for y in range(min_y, max_y + 1):
        scan_y = y + 0.5
        xs: list[float] = []
        for (x0, y0), (x1, y1) in edges:
            if y0 == y1:
                continue
            low_y = min(y0, y1)
            high_y = max(y0, y1)
            if not (low_y <= scan_y < high_y):
                continue
            t = (scan_y - y0) / (y1 - y0)
            xs.append(x0 + (x1 - x0) * t)
        xs.sort()
        for i in range(0, len(xs), 2):
            if i + 1 >= len(xs):
                break
            x_start = max(0, int(xs[i] + 0.5))
            x_end = min(SIZE - 1, int(xs[i + 1] - 0.5))
            for x in range(x_start, x_end + 1):
                put(img, x, y, color)


def fill_circle(img, cx: int, cy: int, radius: int, color) -> None:
    r2 = radius * radius
    for y in range(cy - radius, cy + radius + 1):
        for x in range(cx - radius, cx + radius + 1):
            if (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r2:
                put(img, x, y, color)


def stroke_circle(img, cx: int, cy: int, radius: int, color) -> None:
    x = radius
    y = 0
    err = 0
    while x >= y:
        for px, py in (
            (cx + x, cy + y), (cx + y, cy + x),
            (cx - y, cy + x), (cx - x, cy + y),
            (cx - x, cy - y), (cx - y, cy - x),
            (cx + y, cy - x), (cx + x, cy - y),
        ):
            put(img, px, py, color)
        y += 1
        if err <= 0:
            err += 2 * y + 1
        if err > 0:
            x -= 1
            err -= 2 * x + 1


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


def draw_gas_flow_meter() -> list[list[tuple[int, int, int, int]]]:
    img = new_image()

    # Receiver and barrel outline in a true diagonal placement.
    fill_polygon(img, [
        (4, 10), (5, 8), (7, 6), (10, 4), (13, 3), (15, 3), (15, 5),
        (14, 6), (12, 7), (10, 8), (9, 9), (8, 10), (6, 11), (5, 11)
    ], OUTLINE)

    # Rear grip outline.
    fill_polygon(img, [(4, 10), (5, 10), (6, 14), (4, 14), (3, 12)], OUTLINE)

    # Main receiver fill.
    fill_polygon(img, [
        (5, 10), (6, 8), (8, 6), (10, 5), (13, 4), (14, 4), (14, 5),
        (12, 6), (10, 7), (9, 8), (8, 9), (6, 10)
    ], BODY)

    # Grip fill.
    fill_polygon(img, [(4, 10), (5, 10), (6, 13), (5, 13), (4, 12)], SHADOW)

    # Top plane of the receiver and barrel.
    fill_polygon(img, [(8, 6), (10, 5), (13, 4), (14, 4), (13, 5), (10, 6), (8, 7)], STEEL)
    line(img, 8, 6, 10, 5, HIGHLIGHT)
    line(img, 10, 5, 13, 4, HIGHLIGHT)
    put(img, 14, 4, HIGHLIGHT)

    # Darker lower belly to separate top and side volume.
    fill_polygon(img, [(6, 10), (8, 9), (10, 8), (9, 10), (7, 11), (6, 11)], SHADOW)

    # Brass muzzle and front fitting.
    put(img, 14, 4, BRASS)
    put(img, 15, 4, BRASS)
    put(img, 14, 5, COPPER)
    put(img, 15, 5, OUTLINE)

    # Small side display for the "meter" read instead of a giant round blob.
    put(img, 7, 8, BRASS)
    put(img, 8, 8, AQUA)
    put(img, 9, 8, AQUA)
    put(img, 7, 9, BRASS)
    put(img, 8, 9, GLOW)
    put(img, 9, 9, HIGHLIGHT)
    put(img, 10, 8, COPPER)
    put(img, 10, 9, OUTLINE)

    # Trigger gap and handle separation for better spatial read.
    put(img, 6, 11, TRANSPARENT)
    put(img, 7, 11, TRANSPARENT)
    put(img, 6, 12, TRANSPARENT)
    put(img, 7, 12, SHADOW)
    put(img, 5, 12, GRAPHITE)

    # Structural seams and a small indicator line.
    line(img, 9, 7, 12, 6, OUTLINE)
    line(img, 8, 10, 10, 9, GRAPHITE)
    put(img, 11, 7, STEEL)
    put(img, 12, 5, HIGHLIGHT)
    put(img, 6, 9, HIGHLIGHT)
    put(img, 6, 10, GRAPHITE)

    return img


def main() -> None:
    write_png(OUT_PATH, draw_gas_flow_meter())
    print(f"Wrote {OUT_PATH}")


if __name__ == "__main__":
    main()
