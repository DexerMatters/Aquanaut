#!/usr/bin/env python3
"""Generate the sulfuric acid liquid and particle sprites of Brimstone Caldera.

Liquids and particles animate as vertical frame stacks in one PNG plus a sidecar
``<name>.png.mcmeta``. Every frame is built from phase-shifted periodic noise so the
loops are seamless in X and smooth from the last frame back to the first: viscous
currents for the acid, churning wisps for the vapors.
"""

from __future__ import annotations

import json
import math
import struct
import zlib
from pathlib import Path
from typing import List, Tuple

OUT_BLOCK = Path("src/main/resources/assets/aquanaut/textures/block")
OUT_PARTICLE = Path("src/main/resources/assets/aquanaut/textures/particle")
RGBA = Tuple[int, int, int, int]
Grid = List[List[RGBA]]

TILE = 16  # sprite width; textures are stacked frame columns


def write_png(path: Path, grid: Grid) -> None:
    height = len(grid)
    width = len(grid[0])
    raw = bytearray()
    for row in grid:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((max(0, min(255, int(r))), max(0, min(255, int(g))),
                        max(0, min(255, int(b))), max(0, min(255, int(a)))))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                     + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    print(f"wrote {path}")


def write_mcmeta(path: Path, frametime: int) -> None:
    path.write_text(json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n")
    print(f"wrote {path}")


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def smooth(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return t * t * (3.0 - 2.0 * t)


def hash2(x: float, y: float, seed: int) -> float:
    n = math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453
    return n - math.floor(n)


def periodic_noise(x: float, y: float, period: int, seed: int) -> float:
    """Value noise that wraps exactly every `period` units along X."""
    x0, y0 = math.floor(x), math.floor(y)
    fx, fy = x - x0, y - y0
    u, v = smooth(fx), smooth(fy)

    def corner(cx: int, cy: int) -> float:
        return hash2(cx % period, cy, seed)

    n00 = corner(x0, y0)
    n10 = corner(x0 + 1, y0)
    n01 = corner(x0, y0 + 1)
    n11 = corner(x0 + 1, y0 + 1)
    return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v)


def fbm(x: float, y: float, period: int, seed: int, octaves: int = 4) -> float:
    total, amp, freq, norm = 0.0, 0.5, 1.0, 0.0
    for i in range(octaves):
        total += periodic_noise(x * freq, y * freq, max(1, int(period * freq)), seed + i * 17) * amp
        norm += amp
        amp *= 0.5
        freq *= 2.0
    return total / norm


def frame_phase(frame: int, frames: int) -> float:
    return 2.0 * math.pi * frame / frames


def mix_c(a: RGBA, b: RGBA, t: float) -> RGBA:
    return (lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t),
            lerp(a[3], b[3], t))


def clamp01(t: float) -> float:
    return max(0.0, min(1.0, t))


# ---------------------------------------------------------------------------
# Sulfuric acid still: oily olive-yellow surface with rolling current filaments
# ---------------------------------------------------------------------------

def gen_acid_still(frames: int = 8) -> List[Grid]:
    deep = (110, 116, 16, 255)      # #6E7410
    base = (154, 160, 30, 255)      # #9A9A1E
    bright = (196, 198, 52, 255)    # #C4C634
    slick = (216, 218, 106, 255)    # #D8DA6A
    out: List[Grid] = []
    for frame in range(frames):
        phase = frame_phase(frame, frames)
        grid: Grid = [[base for _ in range(TILE)] for _ in range(TILE)]
        for y in range(TILE):
            for x in range(TILE):
                # Slow rolling vortex: rotate the noise domain around two centres.
                warp = 0.6 * math.sin(phase + x * 0.4)
                n = fbm(x / 4.0 + 0.35 * math.cos(phase), y / 4.0 + warp / 4.0, 4, 31)
                swirl = fbm(x / 3.0 + 1.5 * math.cos(phase + y * 0.3),
                            y / 3.0 + 1.5 * math.sin(phase + x * 0.2), 4, 37)
                tone = clamp01(n * 0.65 + swirl * 0.45)
                if tone < 0.35:
                    col = mix_c(deep, base, tone / 0.35)
                elif tone < 0.72:
                    col = mix_c(base, bright, (tone - 0.35) / 0.37)
                else:
                    col = mix_c(bright, slick, (tone - 0.72) / 0.28)
                # Long current filaments drawn through the field.
                fil = math.sin((x * 0.7 + y * 0.35) * 1.7 + swirl * 5.0 + phase)
                if fil > 0.86:
                    col = mix_c(col, deep, 0.45)
                elif fil < -0.9:
                    col = mix_c(col, slick, 0.5)
                grid[y][x] = col
        out.append(grid)
    return out


def gen_acid_flow(frames: int = 4) -> List[Grid]:
    deep = (108, 108, 18, 235)      # translucent olive body
    base = (142, 138, 30, 225)
    foam = (200, 204, 74, 245)      # #C8CC4A
    out: List[Grid] = []
    for frame in range(frames):
        slide = frame * 4  # the sheet creeps downward each frame
        grid: Grid = [[(0, 0, 0, 0) for _ in range(TILE)] for _ in range(TILE)]
        for y in range(TILE):
            for x in range(TILE):
                n = fbm(x / 2.5, (y + slide) / 5.0, 4, 41)
                streak = math.sin(x * 1.3 + n * 4.0)
                tone = clamp01(0.5 + 0.5 * streak)
                col = mix_c(deep, base, tone)
                # Meniscus ridges and foam flecks sliding with the sheet.
                if streak > 0.8:
                    col = mix_c(col, foam, 0.55)
                if hash2(x, (y + slide) // 2, 43) > 0.93:
                    col = mix_c(col, foam, 0.8)
                grid[y][x] = col
        out.append(grid)
    return out


# ---------------------------------------------------------------------------
# Wisp particles: churning vapor with alpha fading to zero at the frame border
# ---------------------------------------------------------------------------

def wisp_frames(core: RGBA, edge: RGBA, seed: int, frames: int = 4,
                 density: float = 0.9) -> List[Grid]:
    out: List[Grid] = []
    center = (TILE - 1) / 2.0
    for frame in range(frames):
        phase = frame_phase(frame, frames)
        grid: Grid = [[(0, 0, 0, 0) for _ in range(TILE)] for _ in range(TILE)]
        for y in range(TILE):
            for x in range(TILE):
                dx, dy = x - center, y - center
                radius = math.hypot(dx, dy) / (center + 0.5)
                if radius > 1.0:
                    continue
                churn = fbm(x / 3.5 + 0.5 * math.cos(phase), y / 3.5 + 0.5 * math.sin(phase), 4, seed)
                blob = clamp01(1.0 - radius) ** 1.2
                alpha = clamp01(blob * (0.55 + density * churn) * 1.4 - 0.12)
                if alpha <= 0.02:
                    continue
                tone = clamp01(0.35 + 0.65 * churn)
                col = mix_c(edge, core, tone)
                grid[y][x] = (col[0], col[1], col[2], int(alpha * 255))
        out.append(grid)
    return out


def gen_ash_mote() -> Grid:
    speck = (58, 59, 64, 255)       # #3A3B40
    pale = (96, 97, 102, 255)
    grid: Grid = [[(0, 0, 0, 0) for _ in range(TILE)] for _ in range(TILE)]
    clusters = [(8, 7, 2.6), (5, 10, 1.4), (11, 9, 1.2), (10, 5, 1.0), (6, 5, 0.8)]
    for y in range(TILE):
        for x in range(TILE):
            for cx, cy, radius in clusters:
                distance = math.hypot(x - cx, y - cy)
                if distance <= radius:
                    tone = hash2(x, y, 61)
                    col = mix_c(speck, pale, 0.35 + 0.5 * tone)
                    alpha = 255 if distance < radius - 0.6 else 170
                    grid[y][x] = (col[0], col[1], col[2], alpha)
                    break
    return grid


def stack(frames: List[Grid]) -> Grid:
    return [row for frame in frames for row in frame]


def main() -> None:
    write_png(OUT_BLOCK / "sulfuric_acid_still.png", stack(gen_acid_still()))
    write_mcmeta(OUT_BLOCK / "sulfuric_acid_still.png.mcmeta", 8)
    write_png(OUT_BLOCK / "sulfuric_acid_flow.png", stack(gen_acid_flow()))
    write_mcmeta(OUT_BLOCK / "sulfuric_acid_flow.png.mcmeta", 6)

    write_png(OUT_PARTICLE / "sulfuric_acid_mist.png",
              stack(wisp_frames((200, 204, 122, 255), (168, 172, 90, 255), 71)))
    write_mcmeta(OUT_PARTICLE / "sulfuric_acid_mist.png.mcmeta", 4)
    write_png(OUT_PARTICLE / "vent_steam.png",
              stack(wisp_frames((230, 232, 234, 255), (196, 199, 203, 255), 73)))
    write_mcmeta(OUT_PARTICLE / "vent_steam.png.mcmeta", 4)
    write_png(OUT_PARTICLE / "sulfur_gas.png",
              stack(wisp_frames((216, 216, 122, 255), (176, 178, 84, 255), 79)))
    write_mcmeta(OUT_PARTICLE / "sulfur_gas.png.mcmeta", 4)
    write_png(OUT_PARTICLE / "ash_mote.png", gen_ash_mote())
    print("sulfuric acid assets written")


if __name__ == "__main__":
    main()
