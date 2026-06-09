#!/usr/bin/env python3
"""
Generate a 16x16 "fishing_net" item sprite.

Reads mask from a backup file (preserved from user's drawing),
fills EVERY masked pixel with an organic, irregular net mesh —
varied thread spacing, bunched areas, loose threads, rope edge,
and thin perimeter outline.  No checkerboard, no gaps.
"""
from __future__ import annotations

from pathlib import Path
from collections import deque
import struct, zlib, random

SIZE = 16
MASK_PATH = Path("/tmp/opencode/fishing_net_mask.png")
OUT_PATH  = Path("src/main/resources/assets/aquanaut/textures/item/fishing_net.png")
T = (0, 0, 0, 0)

# Palette
OU = (22, 14, 6, 255)
SH = (48, 28, 14, 255)
RP = (98, 62, 30, 255)
RL = (152, 106, 58, 255)
ND = (88, 56, 30, 255)
NM = (120, 82, 44, 255)
NL = (166, 124, 76, 255)
NH = (200, 158, 108, 255)
BK = (72, 46, 26, 255)


def new_img():
    return [[T for _ in range(SIZE)] for _ in range(SIZE)]


def p(img, x, y, c):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = c


def load_mask(path):
    from PIL import Image
    pil = Image.open(path).convert("RGBA")
    px = pil.load()
    mask = new_img()
    for y in range(SIZE):
        for x in range(SIZE):
            if px[x, y][3] > 0:
                mask[y][x] = (1, 0, 0, 0)
    return mask


def in_mask(mask, x, y):
    return 0 <= x < SIZE and 0 <= y < SIZE and mask[y][x] != T


def line(img, mask, x0, y0, x1, y1, color):
    """Bresenham line, only inside mask."""
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        if in_mask(mask, x0, y0):
            existing = img[y0][x0]
            if existing in (T, BK) or (existing in (ND, NM) and color in (NL, NH)):
                p(img, x0, y0, color)
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def outer_outline(img):
    outside = set()
    q = deque()
    for x in range(SIZE):
        if img[0][x] == T and (x, 0) not in outside:
            outside.add((x, 0)); q.append((x, 0))
        if img[SIZE-1][x] == T and (x, SIZE-1) not in outside:
            outside.add((x, SIZE-1)); q.append((x, SIZE-1))
    for y in range(1, SIZE-1):
        if img[y][0] == T and (0, y) not in outside:
            outside.add((0, y)); q.append((0, y))
        if img[y][SIZE-1] == T and (SIZE-1, y) not in outside:
            outside.add((SIZE-1, y)); q.append((SIZE-1, y))
    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE and img[ny][nx] == T and (nx, ny) not in outside:
                outside.add((nx, ny)); q.append((nx, ny))
    filled = set()
    for y in range(SIZE):
        for x in range(SIZE):
            if img[y][x] != T:
                filled.add((x, y))
    for x, y in outside:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) in filled:
                p(img, x, y, OU)
                break


def build():
    random.seed(137)
    mask = load_mask(MASK_PATH)
    img = new_img()

    min_y, max_y, min_x, max_x = 16, -1, 16, -1
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] != T:
                min_y = min(min_y, y); max_y = max(max_y, y)
                min_x = min(min_x, x); max_x = max(max_x, x)
    cx = (min_x + max_x) / 2.0
    cy = (min_y + max_y) / 2.0
    top_x, top_y = cx, min_y

    # Step 1: Fill ALL masked pixels with BK
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] != T:
                p(img, x, y, BK)

    # Step 2: Organic vertical-ish threads fanning from top
    n_vert = random.randint(8, 11)
    for i in range(n_vert):
        sx = int(top_x + random.uniform(-0.5, 0.5))
        sy = top_y
        ex = int(min_x + (max_x - min_x) * (i + random.uniform(0.2, 0.7)) / n_vert)
        ex = max(min_x, min(max_x, ex + random.randint(-1, 1)))
        ey = max_y
        mx = (sx + ex) // 2 + random.randint(-2, 2)
        my = (sy + ey) // 2 + random.randint(-1, 1)
        c = random.choice([ND, NM, NL])
        line(img, mask, sx, sy, mx, my, c)
        line(img, mask, mx, my, ex, ey, c)

    # Step 3: Organic horizontal-ish crossing threads
    n_horiz = random.randint(9, 12)
    valid_y_range = [(y, xl, xr) for y in range(min_y, max_y + 1)
                     for xl in range(min_x, max_x + 1) if in_mask(mask, xl, y)
                     for xr in range(max_x, min_x - 1, -1) if in_mask(mask, xr, y)]
    # Actually simpler: for each target y, draw a wavy horizontal
    for _ in range(n_horiz):
        y = random.randint(min_y + 1, max_y - 1)
        # Find leftmost and rightmost mask pixel at this y
        lx = None
        for tx in range(min_x, max_x + 1):
            if in_mask(mask, tx, y):
                lx = tx; break
        rx = None
        for tx in range(max_x, min_x - 1, -1):
            if in_mask(mask, tx, y):
                rx = tx; break
        if lx is None or rx is None or rx - lx < 2:
            continue
        my = y + random.randint(-1, 1)
        mx = (lx + rx) // 2 + random.randint(-1, 1)
        c = random.choice([ND, NM, NL])
        line(img, mask, lx, y, mx, my, c)
        line(img, mask, mx, my, rx, y, c)

    # Step 4: Extra random crossing threads
    for _ in range(random.randint(4, 7)):
        sx = random.randint(min_x, max_x)
        sy = random.randint(min_y + 1, max_y - 1)
        ex = random.randint(min_x, max_x)
        ey = random.randint(min_y + 1, max_y - 1)
        if abs(sx - ex) + abs(sy - ey) > 3:
            c = random.choice([NM, NL])
            line(img, mask, sx, sy, ex, ey, c)

    # Step 5: Guarantee fill — any mask pixel still BK gets a neighbour's color
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] != T and img[y][x] == BK:
                # Look at neighbours for a thread color to copy
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < SIZE and 0 <= ny < SIZE and img[ny][nx] not in (T, BK, OU):
                        p(img, x, y, img[ny][nx])
                        break
                else:
                    p(img, x, y, ND)  # fallback

    # Step 6: Knot highlights at thread crossings
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] == T or img[y][x] in (T, BK, OU):
                continue
            neighbours = 0
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < SIZE and 0 <= ny < SIZE and img[ny][nx] not in (T, BK, OU):
                    neighbours += 1
            if neighbours >= 3:
                p(img, x, y, NH)

    # Step 7: Depth shading
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] == T or img[y][x] == T:
                continue
            dx = abs(x - top_x) / max(1, max_x - top_x)
            dy = abs(y - min_y) / max(1, max_y - min_y)
            dist = (dx + dy) / 2.0
            cur = img[y][x]
            if cur == BK:
                if dist < 0.3:       p(img, x, y, NM)
                elif dist < 0.55:     p(img, x, y, ND)
                else:                 p(img, x, y, SH)
            elif cur == ND and dist < 0.25:
                p(img, x, y, NM)
            elif cur == NM and dist < 0.15:
                p(img, x, y, NL)

    # Step 8: Rope edge
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] == T or img[y][x] == T:
                continue
            is_edge = any(
                0 <= x+dx < SIZE and 0 <= y+dy < SIZE and mask[y+dy][x+dx] == T
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
            )
            if is_edge:
                cur = img[y][x]
                if cur in (SH, ND, NM, BK):
                    p(img, x, y, RP)
                elif cur in (NL, NH):
                    p(img, x, y, RL)

    # Step 9: Loose thread detail
    edge_spots = [(x, y) for y in range(SIZE) for x in range(SIZE)
                  if img[y][x] in (RP, RL) and in_mask(mask, x, y)]
    if edge_spots:
        spot = random.choice(edge_spots)
        dx = 1 if spot[0] > cx else -1
        for step in range(1, random.randint(1, 3)):
            nx, ny = spot[0] + dx * step, spot[1] + random.choice([-1, 0, 1])
            if 0 <= nx < SIZE and 0 <= ny < SIZE and img[ny][nx] == T:
                p(img, nx, ny, RP)

    # Step 10: Perimeter outline
    outer_outline(img)

    return img


def write_png(path, img):
    raw = bytearray()
    for row in img:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))
    def ch(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    hdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    pld = zlib.compress(bytes(raw), 9)
    png = b"\x89PNG\r\n\x1a\n" + ch(b"IHDR", hdr) + ch(b"IDAT", pld) + ch(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


if __name__ == "__main__":
    write_png(OUT_PATH, build())
    print(f"Wrote {OUT_PATH}")
