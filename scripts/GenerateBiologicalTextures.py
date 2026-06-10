#!/usr/bin/env python3
"""
Generate biological item textures for Aquanaut mod.
Produces 16x16 RGBA PNGs in Minecraft item style with thin colored outlines.

Items:
  1. transparent_tissue  — light-brown semitransparent membrane
  2. hard_rib            — curved bone rib with 3D shading
  3. red_jelly           — crimson gelatinous blob
  4. white_jelly         — pale translucent blob
  5. light_cyan_jelly    — teal gelatinous blob
  6. golden_jelly        — amber gelatinous blob
  7. ring_rib            — ring-shaped vertebra bone
  8. rotten_tissue       — decayed dark tissue
  9. spring              — bone-coloured coil spring
 10. air_sac             — organ-like light-cyan sac
"""

from __future__ import annotations

from pathlib import Path
import struct
import zlib
from typing import List, Tuple, Set

SIZE = 16
OUT_DIR = Path("src/main/resources/assets/aquanaut/textures/item")

RGBA = Tuple[int, int, int, int]
Grid = List[List[RGBA]]

TRANSPARENT: RGBA = (0, 0, 0, 0)


def new_grid() -> Grid:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def put(g: Grid, x: int, y: int, c: RGBA) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        g[y][x] = c


def fill_circle(g: Grid, cx: int, cy: int, r: int, c: RGBA) -> None:
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                put(g, x, y, c)


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


def get_edge_pixels(g: Grid) -> Set[Tuple[int, int]]:
    """Return the set of pixel coords that are on the outer edge
    (adjacent to at least one transparent pixel in 4-neighbourhood)."""
    edges: Set[Tuple[int, int]] = set()
    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3] == 0:
                continue
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if 0 <= nx < SIZE and 0 <= ny < SIZE and g[ny][nx][3] == 0:
                    edges.add((x, y))
                    break
    return edges


def apply_outline(g: Grid, edge_set: Set[Tuple[int, int]], outline_color: RGBA) -> None:
    """Colour all edge pixels with the outline colour."""
    for x, y in edge_set:
        put(g, x, y, outline_color)


def apply_shading(g: Grid, edge_set: Set[Tuple[int, int]],
                  shading_map: dict) -> None:
    """Apply shading only to interior (non-edge) pixels.
    shading_map: {(x, y): color, ...}"""
    for (x, y), color in shading_map.items():
        if (x, y) not in edge_set and 0 <= x < SIZE and 0 <= y < SIZE:
            if g[y][x][3] != 0:  # only on non-transparent
                put(g, x, y, color)


# ══════════════════════════════════════════════
#  ITEM 1 — transparent_tissue
# ══════════════════════════════════════════════
def draw_transparent_tissue() -> Grid:
    g = new_grid()

    T_O  = (50, 30, 18, 225)
    T_B  = (178, 138, 108, 150)
    T_L  = (215, 178, 145, 160)
    T_S  = (142, 108, 80, 155)
    T_D  = (110, 78, 54, 160)
    T_F  = (158, 120, 92, 145)

    # Irregular tissue shape — slightly crumpled sheet
    body = {
        (5,3), (6,3), (7,3), (8,3), (9,3),
        (4,4), (5,4), (6,4), (7,4), (8,4), (9,4), (10,4),
        (3,5), (4,5), (5,5), (6,5), (7,5), (8,5), (9,5), (10,5), (11,5),
        (2,6), (3,6), (4,6), (5,6), (6,6), (7,6), (8,6), (9,6), (10,6), (11,6),
        (2,7), (3,7), (4,7), (5,7), (6,7), (7,7), (8,7), (9,7), (10,7), (11,7),
        (2,8), (3,8), (4,8), (5,8), (6,8), (7,8), (8,8), (9,8), (10,8), (11,8),
        (3,9), (4,9), (5,9), (6,9), (7,9), (8,9), (9,9), (10,9), (11,9),
        (3,10), (4,10), (5,10), (6,10), (7,10), (8,10), (9,10), (10,10),
        (4,11), (5,11), (6,11), (7,11), (8,11), (9,11),
        (5,12), (6,12), (7,12), (8,12),
    }
    for x, y in body:
        put(g, x, y, T_B)

    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, T_O)

    # Shading on interior
    shading = {}
    # Top-left highlights
    for x, y in [(4,4),(5,4),(3,5),(4,5),(2,6),(3,6),(2,7),(3,7),(3,8),(4,8),(3,9),(4,9),(4,10)]:
        shading[(x, y)] = T_L
    # Bottom-right shadows
    for x, y in [(9,7),(10,7),(11,7),(8,8),(9,8),(10,8),(9,9),(10,9),(11,9),(8,10),(9,10),(7,11),(8,11),(7,12)]:
        shading[(x, y)] = T_S
    # Fold line
    for x, y in [(5,7),(6,7),(7,7),(8,7),(9,7)]:
        shading[(x, y)] = T_F
    # Dark corners
    for x, y in [(10,10),(9,11),(6,12),(8,12)]:
        shading[(x, y)] = T_D

    apply_shading(g, edge_set, shading)
    return g


# ══════════════════════════════════════════════
#  ITEM 2 — hard_rib
# ══════════════════════════════════════════════
def draw_hard_rib() -> Grid:
    g = new_grid()

    B_O  = (62, 52, 38, 255)
    B_SH = (118, 105, 82, 255)
    B_M  = (195, 182, 155, 255)
    B_L  = (228, 220, 200, 255)
    B_H  = (248, 242, 230, 255)
    B_D  = (88, 76, 58, 255)

    body = {
        (4,2), (5,2),
        (3,3), (4,3), (5,3), (6,3),
        (3,4), (4,4), (5,4), (6,4),
        (2,5), (3,5), (4,5), (5,5), (6,5), (7,5),
        (2,6), (3,6), (4,6), (5,6), (6,6), (7,6), (8,6),
        (3,7), (4,7), (5,7), (6,7), (7,7), (8,7), (9,7),
        (4,8), (5,8), (6,8), (7,8), (8,8), (9,8), (10,8),
        (5,9), (6,9), (7,9), (8,9), (9,9), (10,9), (11,9),
        (6,10), (7,10), (8,10), (9,10), (10,10), (11,10),
        (7,11), (8,11), (9,11), (10,11), (11,11), (12,11),
        (8,12), (9,12), (10,12), (11,12),
        (9,13), (10,13), (11,13),
        (10,14),
    }
    for x, y in body:
        put(g, x, y, B_M)

    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, B_O)

    shading = {}
    # Top/left highlight
    for x, y in [(4,2),(3,3),(2,5),(2,6),(3,7),(4,8),(5,9),(6,10),(7,11)]:
        shading[(x, y)] = B_H
    for x, y in [(5,2),(4,3),(3,4),(3,5),(3,6),(4,7),(5,8),(6,9),(7,10),(8,11),(8,12)]:
        shading[(x, y)] = B_L
    # Bottom/right shadow
    for x, y in [(8,6),(9,7),(10,8),(11,9),(11,10),(12,11),(11,12),(11,13)]:
        shading[(x, y)] = B_SH
    for x, y in [(9,10),(10,11),(10,12),(9,13),(10,14)]:
        shading[(x, y)] = B_D

    apply_shading(g, edge_set, shading)
    return g


# ══════════════════════════════════════════════
#  ITEM 3 — jelly variants
# ══════════════════════════════════════════════
def _jelly_body_pixels() -> Set[Tuple[int, int]]:
    return {
        (7,2),
        (5,3), (6,3), (7,3), (8,3), (9,3),
        (4,4), (5,4), (6,4), (7,4), (8,4), (9,4), (10,4),
        (3,5), (4,5), (5,5), (6,5), (7,5), (8,5), (9,5), (10,5), (11,5),
        (2,6), (3,6), (4,6), (5,6), (6,6), (7,6), (8,6), (9,6), (10,6), (11,6), (12,6),
        (2,7), (3,7), (4,7), (5,7), (6,7), (7,7), (8,7), (9,7), (10,7), (11,7), (12,7),
        (2,8), (3,8), (4,8), (5,8), (6,8), (7,8), (8,8), (9,8), (10,8), (11,8), (12,8),
        (2,9), (3,9), (4,9), (5,9), (6,9), (7,9), (8,9), (9,9), (10,9), (11,9), (12,9),
        (3,10), (4,10), (5,10), (6,10), (7,10), (8,10), (9,10), (10,10), (11,10),
        (4,11), (5,11), (6,11), (7,11), (8,11), (9,11), (10,11),
        (5,12), (6,12), (7,12), (8,12), (9,12),
    }


def _draw_jelly(body: Set, outline: RGBA, base: RGBA,
                light: RGBA, highlight: RGBA, shadow: RGBA,
                dark: RGBA, shine: RGBA) -> Grid:
    g = new_grid()
    for x, y in body:
        put(g, x, y, base)

    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, outline)

    shading = {}
    # Highlight zone (top-left)
    for x, y in [(5,3),(4,4),(5,4),(3,5),(4,5),(2,6),(3,6),(2,7),(3,7),
                 (2,8),(3,8),(2,9),(3,9)]:
        shading[(x, y)] = light
    for x, y in [(4,5),(3,6),(4,6),(3,7)]:
        shading[(x, y)] = highlight
    # Shadow zone (bottom-right)
    for x, y in [(11,5),(10,6),(11,6),(12,6),(10,7),(11,7),(12,7),
                 (10,8),(11,8),(12,8),(10,9),(11,9),(12,9),
                 (9,10),(10,10),(11,10),(9,11),(10,11),(8,12),(9,12)]:
        shading[(x, y)] = shadow
    for x, y in [(11,7),(12,7),(11,8),(12,8),(11,9),(12,9),(10,10),(11,10)]:
        shading[(x, y)] = dark
    # Specular shine
    for x, y in [(5,6),(6,6),(4,7),(5,7)]:
        shading[(x, y)] = shine

    apply_shading(g, edge_set, shading)
    return g


def draw_red_jelly() -> Grid:
    return _draw_jelly(_jelly_body_pixels(),
        outline=(66, 12, 16, 255), base=(193, 40, 50, 195),
        light=(228, 88, 96, 210), highlight=(248, 158, 163, 205),
        shadow=(143, 26, 34, 205), dark=(103, 16, 23, 215),
        shine=(255, 213, 213, 190))

def draw_white_jelly() -> Grid:
    return _draw_jelly(_jelly_body_pixels(),
        outline=(65, 60, 55, 255), base=(208, 198, 183, 185),
        light=(236, 230, 218, 195), highlight=(252, 248, 241, 190),
        shadow=(163, 153, 138, 200), dark=(123, 116, 103, 210),
        shine=(255, 253, 249, 165))

def draw_light_cyan_jelly() -> Grid:
    return _draw_jelly(_jelly_body_pixels(),
        outline=(20, 65, 68, 255), base=(113, 208, 216, 190),
        light=(168, 236, 240, 205), highlight=(208, 248, 251, 195),
        shadow=(68, 163, 173, 205), dark=(38, 118, 128, 215),
        shine=(228, 252, 254, 170))

def draw_golden_jelly() -> Grid:
    return _draw_jelly(_jelly_body_pixels(),
        outline=(72, 48, 8, 255), base=(213, 158, 28, 190),
        light=(243, 198, 68, 205), highlight=(255, 233, 153, 195),
        shadow=(163, 116, 18, 205), dark=(118, 78, 10, 215),
        shine=(255, 246, 198, 170))


# ══════════════════════════════════════════════
#  ITEM 7 — ring_rib  (thicker ring)
# ══════════════════════════════════════════════
def draw_ring_rib() -> Grid:
    g = new_grid()

    R_O  = (62, 52, 38, 255)
    R_SH = (118, 105, 82, 255)
    R_M  = (195, 182, 155, 255)
    R_L  = (228, 220, 200, 255)
    R_H  = (248, 242, 230, 255)
    R_D  = (88, 76, 58, 255)

    # Bigger ring: outer radius 6, inner radius 3 (narrow wall ~3px)
    fill_circle(g, 7, 7, 6, R_M)
    fill_circle(g, 7, 7, 3, TRANSPARENT)

    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, R_O)

    # Directional shading on interior pixels
    shading = {}
    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3] == 0:
                continue
            dx, dy = x - 7, y - 7
            dsq = dx * dx + dy * dy
            if dsq < 2:
                continue
            dist = dsq ** 0.5
            # Light from top-left (-1,-1)
            light_val = -(dx + dy) / (dist * 1.8)
            if light_val > 0.30:
                shading[(x, y)] = R_H
            elif light_val > 0.10:
                shading[(x, y)] = R_L
            elif light_val < -0.30:
                shading[(x, y)] = R_D
            elif light_val < -0.10:
                shading[(x, y)] = R_SH

    apply_shading(g, edge_set, shading)
    return g


# ══════════════════════════════════════════════
#  ITEM 8 — rotten_tissue
# ══════════════════════════════════════════════
def draw_rotten_tissue() -> Grid:
    g = new_grid()

    RT_O   = (26, 16, 8, 255)
    RT_B   = (80, 56, 33, 240)
    RT_L   = (115, 88, 53, 242)
    RT_S   = (53, 36, 20, 242)
    RT_D   = (36, 23, 12, 248)
    RT_MOLD = (68, 73, 35, 232)
    RT_DECAY = (92, 52, 52, 237)

    body = {
        (5,3), (6,3), (7,3), (8,3), (9,3),
        (4,4), (5,4), (6,4), (7,4), (8,4), (9,4), (10,4),
        (3,5), (4,5), (5,5), (6,5), (7,5), (8,5), (9,5), (10,5), (11,5),
        (2,6), (3,6), (4,6), (5,6), (6,6), (7,6), (8,6), (9,6), (10,6), (11,6),
        (2,7), (3,7), (4,7), (5,7), (6,7), (7,7), (8,7), (9,7), (10,7), (11,7),
        (2,8), (3,8), (4,8), (5,8), (6,8), (7,8), (8,8), (9,8), (10,8), (11,8),
        (3,9), (4,9), (5,9), (6,9), (7,9), (8,9), (9,9), (10,9), (11,9),
        (4,10), (5,10), (6,10), (7,10), (8,10), (9,10), (10,10),
        (5,11), (6,11), (7,11), (8,11), (9,11),
        (6,12), (7,12), (8,12),
    }
    for x, y in body:
        put(g, x, y, RT_B)

    # Ragged holes
    holes = {(6, 7), (8, 8), (7, 9)}
    for hx, hy in holes:
        put(g, hx, hy, TRANSPARENT)

    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, RT_O)

    shading = {}
    # Highlights
    for x, y in [(4,4),(3,5),(4,5),(2,6),(3,6),(2,7),(3,7),(2,8),(3,8)]:
        shading[(x, y)] = RT_L
    # Shadows
    for x, y in [(9,6),(10,6),(11,6),(9,7),(10,7),(11,7),
                 (9,8),(10,8),(11,8),(9,9),(10,9),(11,9),
                 (8,10),(9,10),(10,10),(8,11),(9,11),(7,12),(8,12)]:
        shading[(x, y)] = RT_S
    for x, y in [(6,11),(7,11),(6,12)]:
        shading[(x, y)] = RT_D
    # Mold spots
    for x, y in [(4,5),(5,5),(8,6),(9,6),(3,7),(4,7),(9,8),(10,8)]:
        shading[(x, y)] = RT_MOLD
    # Decay spots
    for x, y in [(10,5),(9,7),(10,7),(8,9),(9,9),(7,10),(8,10)]:
        shading[(x, y)] = RT_DECAY

    apply_shading(g, edge_set, shading)
    return g


# ══════════════════════════════════════════════
#  ITEM 9 — spring  (zigzag coil, side view)
# ══════════════════════════════════════════════
def draw_spring() -> Grid:
    """Bone-coloured coil spring — continuous zigzag coil path, 3px thick."""
    g = new_grid()

    SP_O  = (60, 50, 36, 255)
    SP_SH = (116, 103, 80, 255)
    SP_M  = (193, 180, 153, 255)
    SP_L  = (226, 218, 198, 255)
    SP_H  = (247, 241, 229, 255)
    SP_D  = (86, 74, 56, 255)

    # Build the spring as a continuous thick path that zigzags:
    #   start top-centre → down-right → right-turn → down-left → left-turn → down-right → end bottom-centre
    #
    # Each row lists x coordinates for that row of the spring body.
    # Rows with no entry inherit nothing (gap/bend).
    rows_x: dict[int, list[int]] = {}

    # -- Top cap --
    rows_x[0] = [7, 8, 9]

    # -- Segment 1: down-right (centres: 8 → 12) --
    rows_x[1] = [7, 8, 9, 10]
    rows_x[2] = [8, 9, 10, 11]
    rows_x[3] = [9, 10, 11, 12]
    rows_x[4] = [10, 11, 12]

    # -- Right turn (curve around; centre stays ~12) --
    rows_x[5] = [11, 12, 13]
    rows_x[6] = [11, 12, 13]

    # -- Segment 2: down-left (centres: 12 → 2) --
    rows_x[7] = [9, 10, 11, 12]
    rows_x[8] = [7, 8, 9, 10]
    rows_x[9] = [5, 6, 7, 8]

    # -- Left turn (curve around; centre stays ~2) --
    rows_x[10] = [2, 3, 4, 5]
    rows_x[11] = [1, 2, 3]

    # -- Segment 3: down-right (centres: 2 → 8) --
    rows_x[12] = [2, 3, 4, 5]
    rows_x[13] = [4, 5, 6, 7]
    rows_x[14] = [6, 7, 8, 9]

    # -- Bottom cap --
    rows_x[15] = [7, 8, 9]

    # Fill body
    for y, xs in rows_x.items():
        for x in xs:
            put(g, x, y, SP_M)

    # -- Edge detection & outline --
    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, SP_O)

    # -- Directional shading on interior --
    shading = {}
    for y in range(SIZE):
        for x in range(SIZE):
            if g[y][x][3] == 0:
                continue
            above = g[y - 1][x][3] if y > 0 else 0
            below = g[y + 1][x][3] if y < SIZE - 1 else 0

            if above == 0 and below != 0:
                shading[(x, y)] = SP_H
            elif above == 0:
                shading[(x, y)] = SP_L
            elif below == 0 and above != 0:
                shading[(x, y)] = SP_D
            elif below == 0:
                shading[(x, y)] = SP_SH

    apply_shading(g, edge_set, shading)
    return g


# ══════════════════════════════════════════════
#  ITEM 10 — air_sac  (stereoscopic 3D cube)
# ══════════════════════════════════════════════
def draw_air_sac() -> Grid:
    """Isometric 3D cube — light-cyan, slightly translucent, organ-like veins."""
    g = new_grid()

    CUBE_TOP    = (175, 235, 240, 185)
    CUBE_LEFT   = (120, 200, 210, 190)
    CUBE_RIGHT  = (75, 160, 175, 195)
    CUBE_VEIN_T = (155, 218, 225, 180)
    CUBE_VEIN_L = (100, 180, 192, 185)
    CUBE_VEIN_R = (58, 140, 158, 190)
    CUBE_OUT    = (14, 48, 54, 255)
    CUBE_FRONT  = (10, 38, 44, 255)

    # Top face: diamond  (8,1)→(2,6)→(14,6)→(8,11)
    # Left face:         (2,6)→(8,11)→(8,15)→(2,10)
    # Right face:        (8,11)→(14,6)→(14,10)→(8,15)
    top_rows = {
        1:  [8],
        2:  [7, 8, 9],
        3:  [6, 7, 8, 9, 10],
        4:  [5, 6, 7, 8, 9, 10, 11],
        5:  [4, 5, 6, 7, 8, 9, 10, 11, 12],
        6:  [3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13],
        7:  [4, 5, 6, 7, 8, 9, 10, 11, 12],
        8:  [5, 6, 7, 8, 9, 10, 11],
        9:  [6, 7, 8, 9, 10],
        10: [7, 8, 9],
        11: [8],
    }
    left_rows = {
        6:  [2],
        7:  [2, 3],
        8:  [2, 3, 4],
        9:  [2, 3, 4, 5],
        10: [2, 3, 4, 5, 6],
        11: [3, 4, 5, 6, 7],
        12: [4, 5, 6, 7],
        13: [6, 7],
        14: [7],
        15: [8],
    }
    right_rows = {
        6:  [13],
        7:  [12, 13],
        8:  [11, 12, 13],
        9:  [10, 11, 12, 13],
        10: [9, 10, 11, 12, 13],
        11: [9, 10, 11, 12],
        12: [9, 10, 11],
        13: [9, 10],
        14: [9],
        15: [8],
    }

    for y, xs in top_rows.items():
        for x in xs:
            put(g, x, y, CUBE_TOP)
    for y, xs in left_rows.items():
        for x in xs:
            put(g, x, y, CUBE_LEFT)
    for y, xs in right_rows.items():
        for x in xs:
            put(g, x, y, CUBE_RIGHT)

    # Outline
    edge_set = get_edge_pixels(g)
    apply_outline(g, edge_set, CUBE_OUT)
    # Front vertical edge darker
    for y in range(6, 16):
        put(g, 8, y, CUBE_FRONT)

    # Veins
    top_veins = {
        (8, 3), (8, 4), (8, 5),
        (6, 4), (7, 4), (9, 4), (10, 4),
        (5, 5), (11, 5),
        (7, 7), (8, 7), (9, 7),
    }
    for x, y in top_veins:
        if g[y][x] not in (CUBE_OUT, CUBE_FRONT):
            put(g, x, y, CUBE_VEIN_T)

    left_veins = {
        (3, 8), (4, 8), (5, 8),
        (4, 9), (5, 9),
        (4, 11), (5, 11), (6, 11),
        (5, 12), (6, 12),
    }
    for x, y in left_veins:
        if g[y][x] not in (CUBE_OUT, CUBE_FRONT):
            put(g, x, y, CUBE_VEIN_L)

    right_veins = {
        (11, 8), (12, 8),
        (10, 9), (11, 9),
        (10, 12), (11, 12),
        (10, 13),
    }
    for x, y in right_veins:
        if g[y][x] not in (CUBE_OUT, CUBE_FRONT):
            put(g, x, y, CUBE_VEIN_R)

    return g


# ══════════════════════════════════════════════
#  Main
# ══════════════════════════════════════════════
def main() -> None:
    items = [
        ("transparent_tissue.png", draw_transparent_tissue),
        ("hard_rib.png",           draw_hard_rib),
        ("red_jelly.png",          draw_red_jelly),
        ("white_jelly.png",        draw_white_jelly),
        ("light_cyan_jelly.png",   draw_light_cyan_jelly),
        ("golden_jelly.png",       draw_golden_jelly),
        ("ring_rib.png",           draw_ring_rib),
        ("rotten_tissue.png",      draw_rotten_tissue),
        ("spring.png",             draw_spring),
        ("air_sac.png",            draw_air_sac),
    ]

    for filename, draw_fn in items:
        path = OUT_DIR / filename
        grid = draw_fn()
        write_png(path, grid)
        print(f"Wrote {path}")


if __name__ == "__main__":
    main()
