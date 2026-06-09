#!/usr/bin/env python3
"""
Animate existing fishing_net.png — wind-blown effect.
Analyzes the existing 16×16 texture and generates 8 frames
by shifting lines with a sine-wave sway that tiles seamlessly.
"""
from __future__ import annotations

from pathlib import Path
import struct
import zlib
import math

SIZE = 16
FRAMES = 8
SRC_PATH = Path("src/main/resources/assets/aquanaut/textures/block/_fishing_net_original.png")
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/block/fishing_net.png")


def read_png_rgba(path: Path) -> list[list[tuple[int, int, int, int]]]:
    """Read a PNG file and return RGBA pixel grid."""
    data = path.read_bytes()
    pos = 8
    pixels_raw = b""
    w = h = 0
    color_type = 0
    palette = []

    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        tag = data[pos+4:pos+8]
        chunk_data = data[pos+8:pos+8+length]
        if tag == b"IHDR":
            w, h, bit_depth, color_type = struct.unpack(">IIBB", chunk_data[:10])
        elif tag == b"PLTE":
            for i in range(0, len(chunk_data), 3):
                palette.append((chunk_data[i], chunk_data[i+1], chunk_data[i+2], 255))
        elif tag == b"tRNS":
            for i, a in enumerate(chunk_data):
                if i < len(palette):
                    r, g, b, _ = palette[i]
                    palette[i] = (r, g, b, a)
        elif tag == b"IDAT":
            pixels_raw += zlib.decompress(chunk_data)
        elif tag == b"IEND":
            break
        pos += 12 + length

    img = [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]
    if color_type == 3:
        row_size = 1 + w
        for y in range(h):
            for x in range(w):
                idx = pixels_raw[y * row_size + 1 + x]
                img[y][x] = palette[idx]
    elif color_type == 6:
        row_size = 1 + w * 4
        for y in range(h):
            for x in range(w):
                px = y * row_size + 1 + x * 4
                img[y][x] = (pixels_raw[px], pixels_raw[px+1], pixels_raw[px+2], pixels_raw[px+3])
    return img


def sample(img, fx: float, fy: float) -> tuple[int, int, int, int]:
    """Bilinear-ish sample from image at fractional coordinates (wrapping)."""
    h = len(img)
    w = len(img[0])
    x = int(fx) % w
    y = int(fy) % h
    if x < 0:
        x += w
    if y < 0:
        y += h
    return img[y][x]


def write_png(path: Path, img) -> None:
    h = len(img)
    w = len(img[0])
    raw = bytearray()
    for row in img:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", payload) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def generate_frame(original, frame: int, total_frames: int) -> list[list[tuple[int, int, int, int]]]:
    """Generate one animation frame by displacing the original.
    Uses a position-dependent sine wave for coherent tiling:
    displacement is the SAME for every block at the same world position.
    Since all frames come from the same function, adjacent blocks tile.
    """
    t = frame / total_frames
    sway_x = math.sin(t * 2 * math.pi) * 0.8
    sway_y = math.cos(t * 2 * math.pi) * 0.6

    frame_img = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            # Displacement based on position — same for every block at this texel
            dx = math.sin((y + sway_x) * 0.9) * 0.7
            dy = math.cos((x + sway_y) * 0.9) * 0.5
            px = sample(original, x + dx, y + dy)
            frame_img[y][x] = px
    return frame_img


def main() -> None:
    print(f"Reading original: {SRC_PATH}")
    original = read_png_rgba(SRC_PATH)
    print(f"  Size: {len(original[0])}x{len(original)}")

    # Create sprite sheet: FRAMES stacked vertically
    sheet = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE * FRAMES)]

    for f in range(FRAMES):
        frame = generate_frame(original, f, FRAMES)
        for y in range(SIZE):
            for x in range(SIZE):
                sheet[f * SIZE + y][x] = frame[y][x]

    write_png(OUT_PATH, sheet)
    print(f"Wrote {OUT_PATH} ({SIZE}x{SIZE * FRAMES})")


if __name__ == "__main__":
    main()
