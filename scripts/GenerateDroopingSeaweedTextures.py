#!/usr/bin/env python3
"""Generate the drooping seaweed block texture set for Aquanaut.

The texture set is:
  - drooping_seaweed_top.png   : animated head / top segment
  - drooping_seaweed.png       : animated repeatable body segment
  - drooping_seaweed_tail.png  : animated tapered tail segment

Each output is an animated sprite sheet built from the same continuous plant
composition so the segments join cleanly in game. The art stays fully opaque
or fully transparent, which keeps it compatible with cutout/cross rendering.
"""

from __future__ import annotations

import math
import json
import struct
import zlib
from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple

SIZE = 16
FRAME_HEIGHT = 16
PART_COUNT = 3
TOTAL_HEIGHT = FRAME_HEIGHT * PART_COUNT
FRAME_COUNT = 24
FRAME_TIME = 8
OUT_DIR = Path("src/main/resources/assets/aquanaut/textures/block")

RGBA = Tuple[int, int, int, int]
Grid = List[List[RGBA]]

TRANSPARENT: RGBA = (0, 0, 0, 0)


@dataclass(frozen=True)
class Palette:
    highlight: RGBA
    light: RGBA
    base: RGBA
    shadow: RGBA
    deep: RGBA


@dataclass(frozen=True)
class Blade:
    base_x: float
    response: float
    lag: float
    crown_offset: float
    top_curve: float
    body_curve: float
    tail_curve: float
    top_half: float
    boundary_half: float
    body_half: float
    tip_half: float
    tail_end: float
    order: int


HEAD_PALETTE = Palette(
    highlight=(124, 172, 122, 255),
    light=(88, 141, 90, 255),
    base=(55, 110, 63, 255),
    shadow=(34, 76, 42, 255),
    deep=(21, 52, 29, 255),
)

MID_PALETTE = Palette(
    highlight=(114, 162, 114, 255),
    light=(79, 131, 82, 255),
    base=(49, 101, 57, 255),
    shadow=(31, 69, 39, 255),
    deep=(19, 47, 27, 255),
)

TAIL_PALETTE = Palette(
    highlight=(103, 149, 104, 255),
    light=(70, 120, 74, 255),
    base=(42, 92, 50, 255),
    shadow=(27, 62, 34, 255),
    deep=(16, 42, 24, 255),
)

FRUIT_PALETTE = Palette(
    highlight=(232, 223, 170, 255),
    light=(207, 194, 128, 255),
    base=(176, 160, 96, 255),
    shadow=(128, 116, 67, 255),
    deep=(89, 79, 47, 255),
)


def new_grid(height: int = SIZE) -> Grid:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(height)]


def put(g: Grid, x: int, y: int, c: RGBA) -> None:
    if 0 <= x < SIZE and 0 <= y < len(g):
        g[y][x] = c


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * max(0.0, min(1.0, t))


def fill_circle(g: Grid, cx: int, cy: int, r: int, c: RGBA) -> None:
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                put(g, x, y, c)


def fill_ellipse(g: Grid, cx: int, cy: int, rx: int, ry: int, c: RGBA) -> None:
    if rx <= 0 or ry <= 0:
        put(g, cx, cy, c)
        return
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            dx = (x - cx) / float(rx)
            dy = (y - cy) / float(ry)
            if dx * dx + dy * dy <= 1.0:
                put(g, x, y, c)


def write_png(path: Path, g: Grid) -> None:
    height = len(g)
    width = len(g[0]) if g else 0

    raw = bytearray()
    for row in g:
        raw.append(0)
        for r, g_, b, a in row:
            raw.extend((r, g_, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", payload) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def stack_frames(frames: List[Grid]) -> Grid:
    sheet = new_grid(FRAME_HEIGHT * len(frames))
    for frame_index, frame in enumerate(frames):
        y_offset = frame_index * FRAME_HEIGHT
        for y, row in enumerate(frame):
            for x, color in enumerate(row):
                sheet[y + y_offset][x] = color
    return sheet


def slice_grid(g: Grid, y0: int, height: int = FRAME_HEIGHT) -> Grid:
    return [row[:] for row in g[y0:y0 + height]]


def validate_alpha(g: Grid) -> None:
    for y, row in enumerate(g):
        for x, (_, _, _, a) in enumerate(row):
            if a not in (0, 255):
                raise ValueError(f"Unexpected semitransparent pixel at ({x}, {y}): alpha={a}")


BLADES = (
    Blade(4.1, 0.82, -0.03, -1.15, 0.42, 0.30, 0.18, 0.10, 0.42, 0.72, 0.03, 0.62, 0),
    Blade(5.9, 0.92, -0.01, -0.55, 0.34, 0.24, 0.22, 0.16, 0.68, 1.08, 0.04, 1.00, 1),
    Blade(7.8, 0.98, 0.00, 0.00, 0.28, 0.18, 0.20, 0.18, 0.82, 1.16, 0.03, 0.96, 2),
    Blade(9.7, 0.94, 0.01, 0.48, 0.30, 0.20, 0.22, 0.16, 0.70, 1.02, 0.04, 0.90, 3),
    Blade(11.8, 0.76, 0.04, 0.95, 0.40, 0.28, 0.18, 0.08, 0.38, 0.60, 0.02, 0.54, 4),
    Blade(8.8, 0.62, 0.02, 0.22, 0.22, 0.12, 0.16, 0.10, 0.26, 0.46, 0.03, 0.76, 5),
)


def current_push(phase_shift: float, lag: float) -> float:
    return math.sin(phase_shift - 0.55 + lag) + 0.12 * math.sin(phase_shift * 2.0 - 1.15 + lag * 0.5)


def boundary_center(blade: Blade, phase_shift: float) -> float:
    return blade.base_x + current_push(phase_shift, blade.lag) * blade.response


def top_center(blade: Blade, t: float, phase_shift: float) -> float:
    push = current_push(phase_shift, blade.lag)
    start = blade.base_x + blade.crown_offset + push * blade.response * 0.34
    boundary = boundary_center(blade, phase_shift)
    return lerp(start, boundary, t) + push * blade.top_curve * math.sin(math.pi * t)


def body_center(blade: Blade, t: float, phase_shift: float) -> float:
    push = current_push(phase_shift, blade.lag)
    return boundary_center(blade, phase_shift) + push * blade.body_curve * math.sin(math.pi * t)


def tail_center(blade: Blade, t: float, phase_shift: float) -> float:
    push = current_push(phase_shift, blade.lag)
    return boundary_center(blade, phase_shift) + push * blade.tail_curve * (t ** 1.35)


def top_half(blade: Blade, t: float) -> float:
    return lerp(blade.top_half, blade.boundary_half, t)


def body_half(blade: Blade, t: float) -> float:
    return blade.boundary_half + (blade.body_half - blade.boundary_half) * math.sin(math.pi * t)


def tail_half(blade: Blade, t: float) -> float:
    if t >= blade.tail_end:
        return 0.0
    fade_start = max(0.0, blade.tail_end - 0.20)
    fade = 1.0 if t <= fade_start else (blade.tail_end - t) / max(0.001, blade.tail_end - fade_start)
    normalized = t / max(0.001, blade.tail_end)
    return lerp(blade.boundary_half, blade.tip_half, normalized ** 0.92) * max(0.0, fade)


def render_segment_blade(
    g: Grid,
    blade: Blade,
    phase_shift: float,
    palette: Palette,
    center_fn,
    half_fn,
    segment: str,
) -> None:
    for y in range(FRAME_HEIGHT):
        t = y / float(FRAME_HEIGHT - 1)
        center = center_fn(blade, t, phase_shift)
        half = half_fn(blade, t)
        if half <= 0.03:
            continue

        prev_t = max(0.0, t - 1.0 / max(1, FRAME_HEIGHT - 1))
        next_t = min(1.0, t + 1.0 / max(1, FRAME_HEIGHT - 1))
        slope = center_fn(blade, next_t, phase_shift) - center_fn(blade, prev_t, phase_shift)
        light_on_left = slope >= 0.0

        x0 = math.floor(center - half - 1)
        x1 = math.ceil(center + half + 1)
        for x in range(x0, x1 + 1):
            if not (0 <= x < SIZE):
                continue
            offset = (x - center) / max(0.001, half)
            if abs(offset) > 0.78:
                continue

            edge = abs(offset)
            leading = -offset if light_on_left else offset
            if edge < 0.14:
                c = palette.light
            elif edge < 0.42:
                c = palette.base if leading > -0.10 else palette.light
            elif edge < 0.72:
                c = palette.shadow if leading > 0.12 else palette.base
            else:
                c = palette.deep

            if segment != "tail" and 2 < y < FRAME_HEIGHT - 3 and edge > 0.60 and half >= 0.8:
                if (y + blade.order * 2) % 9 == 0:
                    if (offset < 0 and (y + blade.order) % 2 == 0) or (offset > 0 and (y + blade.order) % 2 == 1):
                        continue

            if edge < 0.18 and leading < -0.24 and y > 1:
                c = palette.highlight

            put(g, x, y, c)


def render_head(g: Grid, phase_shift: float) -> None:
    crown_push = current_push(phase_shift, -0.02) * 0.62
    crown_shift = int(round(crown_push))
    fruit_x = 8 + crown_shift

    fill_ellipse(g, 6 + crown_shift, 4, 3, 2, HEAD_PALETTE.shadow)
    fill_ellipse(g, 10 + crown_shift, 4, 3, 2, HEAD_PALETTE.shadow)
    fill_ellipse(g, 7 + crown_shift, 3, 2, 2, HEAD_PALETTE.base)
    fill_ellipse(g, 9 + crown_shift, 3, 2, 2, HEAD_PALETTE.base)
    fill_ellipse(g, 8 + crown_shift, 5, 3, 2, HEAD_PALETTE.base)
    fill_ellipse(g, 8 + crown_shift, 6, 2, 1, HEAD_PALETTE.light)

    fill_ellipse(g, fruit_x, 1, 3, 2, FRUIT_PALETTE.shadow)
    fill_ellipse(g, fruit_x, 1, 2, 2, FRUIT_PALETTE.base)
    put(g, fruit_x, 0, FRUIT_PALETTE.highlight)
    put(g, fruit_x - 1, 0, FRUIT_PALETTE.light)
    put(g, fruit_x + 1, 0, FRUIT_PALETTE.light)
    put(g, fruit_x - 2, 1, FRUIT_PALETTE.light)
    put(g, fruit_x + 2, 1, FRUIT_PALETTE.light)
    put(g, fruit_x, 2, FRUIT_PALETTE.shadow)
    put(g, fruit_x - 1, 2, FRUIT_PALETTE.deep)
    put(g, fruit_x + 1, 2, FRUIT_PALETTE.deep)

    for x, y in [
        (4 + crown_shift, 4), (12 + crown_shift, 4),
        (5 + crown_shift, 2), (11 + crown_shift, 2),
        (6 + crown_shift, 5), (10 + crown_shift, 5),
    ]:
        put(g, x, y, TRANSPARENT)


def build_top_frame(phase_shift: float) -> Grid:
    g = new_grid(FRAME_HEIGHT)
    for blade in BLADES:
        render_segment_blade(g, blade, phase_shift, HEAD_PALETTE, top_center, top_half, "top")
    render_head(g, phase_shift)
    return g


def build_body_frame(phase_shift: float) -> Grid:
    g = new_grid(FRAME_HEIGHT)
    for blade in BLADES:
        render_segment_blade(g, blade, phase_shift, MID_PALETTE, body_center, body_half, "body")
    return g


def build_tail_frame(phase_shift: float) -> Grid:
    g = new_grid(FRAME_HEIGHT)
    for blade in BLADES:
        render_segment_blade(g, blade, phase_shift, TAIL_PALETTE, tail_center, tail_half, "tail")
    return g


def write_animation_meta(path: Path, frametime: int, frame_count: int) -> None:
    frames = list(range(frame_count))
    meta = {
        "animation": {
            "frametime": frametime,
            "frames": frames,
        }
    }
    path.write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")


def write_animated_texture(file_name: str, frames: List[Grid], frametime: int) -> None:
    sheet = stack_frames(frames)
    validate_alpha(sheet)
    texture_path = OUT_DIR / file_name
    write_png(texture_path, sheet)
    write_animation_meta(texture_path.with_name(texture_path.name + ".mcmeta"), frametime, len(frames))
    print(f"Wrote {texture_path}")


def main() -> None:
    top_frames: List[Grid] = []
    body_frames: List[Grid] = []
    tail_frames: List[Grid] = []

    for frame_index in range(FRAME_COUNT):
        phase_shift = (math.tau * frame_index) / FRAME_COUNT
        top_frames.append(build_top_frame(phase_shift))
        body_frames.append(build_body_frame(phase_shift))
        tail_frames.append(build_tail_frame(phase_shift))

    write_animated_texture("drooping_seaweed_top.png", top_frames, FRAME_TIME)
    write_animated_texture("drooping_seaweed.png", body_frames, FRAME_TIME)
    write_animated_texture("drooping_seaweed_tail.png", tail_frames, FRAME_TIME)


if __name__ == "__main__":
    main()
