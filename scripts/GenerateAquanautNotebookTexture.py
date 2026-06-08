#!/usr/bin/env python3
"""
Generate Aquanaut's Notebook — exact vanilla book shape remapped to ocean colors.
Reads the vanilla Minecraft book texture, remaps every pixel to aquanaut palette.
"""
from __future__ import annotations

from pathlib import Path
import struct
import zlib


SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/item/aquanaut_notebook.png")

# --- Color remap table ---
# Vanilla brown/red book colors → Aquanaut ocean teal colors
REMAP = {
    # Outline — darkest brown → dark navy
    (22, 16, 5):   (14, 26, 42, 255),
    # Cover dark brown → dark teal
    (49, 33, 4):   (22, 50, 78, 255),
    # Cover medium-dark brown → medium-dark teal
    (68, 37, 10):  (30, 65, 98, 255),
    # Cover medium brown → medium teal
    (82, 46, 16):  (40, 85, 120, 255),
    # Cover medium-light brown → medium-light teal
    (84, 62, 19):  (50, 100, 135, 255),
    # Cover light brown (main cover face) → light teal
    (101, 75, 23): (65, 125, 158, 255),
    # Pages darkest gray → page edge cream
    (91, 91, 91):  (170, 155, 130, 255),
    # Pages dark gray → page shadow cream
    (153, 153, 153): (198, 185, 160, 255),
    # Pages mid gray → page mid cream
    (183, 183, 183): (222, 212, 190, 255),
    # Pages light gray → page bright cream
    (214, 214, 214): (245, 240, 225, 255),
}

# Additional accent color for a subtle wave emboss on the cover
ACCENT = (88, 165, 195, 255)  # bright aqua


def download_vanilla_book() -> bytes:
    """Download vanilla book PNG and return pixel data."""
    import urllib.request
    url = "https://raw.githubusercontent.com/Faithful-Pack/Default-Java/1.21.11/assets/minecraft/textures/item/book.png"
    with urllib.request.urlopen(url) as resp:
        return resp.read()


def decode_indexed_png(data: bytes) -> list[tuple[int, int, int, int]]:
    """Decode an indexed-color PNG into RGBA pixel list (256 entries max)."""
    pos = 8  # skip PNG signature
    palette = []
    pixels_raw = b""
    width = height = 0

    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        tag = data[pos+4:pos+8]
        chunk_data = data[pos+8:pos+8+length]

        if tag == b"IHDR":
            width, height, bit_depth, color_type = struct.unpack(">IIBB", chunk_data[:10])
        elif tag == b"PLTE":
            for i in range(0, len(chunk_data), 3):
                r, g, b = chunk_data[i], chunk_data[i+1], chunk_data[i+2]
                palette.append((r, g, b, 255))
        elif tag == b"tRNS":
            # transparency chunk — sets alpha for palette entries
            for i, a in enumerate(chunk_data):
                if i < len(palette):
                    r, g, b, _ = palette[i]
                    palette[i] = (r, g, b, a)
        elif tag == b"IDAT":
            pixels_raw += zlib.decompress(chunk_data)
        elif tag == b"IEND":
            break
        pos += 12 + length

    # Convert indexed pixels to RGBA
    result = []
    row_size = 1 + width  # filter byte + indices
    for y in range(height):
        row_start = y * row_size
        for x in range(width):
            idx = pixels_raw[row_start + 1 + x]
            result.append(palette[idx])
    return result


def remap_pixels(pixels: list) -> list[tuple[int, int, int, int]]:
    """Remap each pixel from vanilla brown to aquanaut teal."""
    result = []
    for px in pixels:
        r, g, b, a = px
        if a < 128:
            result.append((0, 0, 0, 0))
        else:
            key = (r, g, b)
            if key in REMAP:
                result.append(REMAP[key])
            else:
                # Unknown color — keep but warn
                print(f"  Warning: unmapped color RGB({r},{g},{b}) — keeping as-is")
                result.append(px)
    return result


def add_aquanaut_accent(pixels: list) -> None:
    """Overwrite a few pixels on the cover to add a subtle wave symbol."""
    # The cover face occupies roughly x=1-10 at y=5-9
    # Place a tiny wave ~ at a visible spot on the cover
    accent_pixels = [
        # Small wave shape at (x,y) positions on the cover
        (5, 5, ACCENT),
        (4, 6, ACCENT),
        (6, 6, ACCENT),
    ]
    for x, y, color in accent_pixels:
        idx = y * SIZE + x
        if pixels[idx][3] > 128:  # only overwrite non-transparent pixels
            pixels[idx] = color


def write_png(path: Path, pixels: list) -> None:
    raw = bytearray()
    for y in range(SIZE):
        raw.append(0)  # filter byte
        for x in range(SIZE):
            r, g, b, a = pixels[y * SIZE + x]
            raw.extend((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    payload = zlib.compress(bytes(raw), level=9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", payload) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def print_pixel_map(pixels: list) -> None:
    """Print a visual representation of the pixel map."""
    pal = {}
    for i, px in enumerate(pixels):
        if px not in pal:
            pal[px] = chr(ord('a') + len(pal)) if len(pal) < 26 else '?'

    print("\n  " + "".join(str(i % 10) for i in range(16)))
    for y in range(16):
        chars = []
        for x in range(16):
            px = pixels[y * 16 + x]
            if px[3] < 128:
                chars.append('.')
            else:
                chars.append(pal[px])
        print(f"{y:2d} " + " ".join(chars))

    print("\nLegend:")
    for color, ch in sorted(pal.items(), key=lambda x: x[1]):
        r, g, b, a = color
        name = "transparent" if a < 128 else f"RGB({r:3d},{g:3d},{b:3d})"
        print(f"  {ch} = {name}")


def main() -> None:
    print("Downloading vanilla book texture...")
    vanilla_data = download_vanilla_book()

    print("Decoding indexed PNG...")
    vanilla_pixels = decode_indexed_png(vanilla_data)
    print(f"  Got {len(vanilla_pixels)} pixels")

    print("Remapping to aquanaut colors...")
    aqua_pixels = remap_pixels(vanilla_pixels)

    print("Adding subtle wave accent...")
    add_aquanaut_accent(aqua_pixels)

    print("Writing output...")
    write_png(OUT_PATH, aqua_pixels)
    print(f"Wrote {OUT_PATH}")

    print_pixel_map(aqua_pixels)


if __name__ == "__main__":
    main()
