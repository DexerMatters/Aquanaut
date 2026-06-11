#!/usr/bin/env python3
"""
Generate Aquanaut's Notebook — exact vanilla book shape remapped to ocean colors.
Reads a local vanilla Minecraft book texture when available, otherwise falls back to network,
then remaps every pixel to an aquanaut palette.
"""
from __future__ import annotations

from pathlib import Path
import struct
import zlib


SIZE = 16
OUT_PATH = Path("src/main/resources/assets/aquanaut/textures/item/aquanaut_notebook.png")

# --- Color remap table ---
# Vanilla brown/red book colors → deeper Aquanaut navy/ocean colors
REMAP = {
    # Outline — darkest brown → deep navy
    (22, 16, 5):   (6, 18, 34, 255),
    # Cover dark brown → dark navy
    (49, 33, 4):   (13, 30, 52, 255),
    # Cover medium-dark brown → deep blue
    (68, 37, 10):  (19, 43, 71, 255),
    # Cover medium brown → blue-teal
    (82, 46, 16):  (27, 58, 94, 255),
    # Cover medium-light brown → brighter blue-teal
    (84, 62, 19):  (37, 77, 120, 255),
    # Cover light brown (main cover face) → bright ocean blue
    (101, 75, 23): (51, 103, 150, 255),
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
ACCENT = (94, 188, 222, 255)  # bright aqua


def find_local_vanilla_book() -> Path | None:
    """Return the first local vanilla book texture we can find."""
    candidates = [
        Path("src/main/resources/assets/minecraft/textures/item/book.png"),
        Path("build/resources/main/assets/minecraft/textures/item/book.png"),
        Path.home() / ".minecraft/assets/minecraft/textures/item/book.png",
    ]
    for base in [
        Path.home() / ".local/share/PrismLauncher/instances",
        Path.home() / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances",
    ]:
        if base.exists():
            candidates.extend(base.rglob("assets/minecraft/textures/item/book.png"))

    for candidate in candidates:
        if candidate.is_file():
            return candidate
    return None


def load_vanilla_book() -> bytes:
    """Load vanilla book PNG bytes from disk first, then the network."""
    local = find_local_vanilla_book()
    if local is not None:
        print(f"Using local vanilla book texture: {local}")
        return local.read_bytes()

    import urllib.request
    url = "https://raw.githubusercontent.com/Faithful-Pack/Default-Java/1.21.11/assets/minecraft/textures/item/book.png"
    print(f"Downloading vanilla book texture from {url}")
    with urllib.request.urlopen(url) as resp:
        return resp.read()


def paeth_predictor(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def decode_png(data: bytes) -> list[tuple[int, int, int, int]]:
    """Decode a non-interlaced 8-bit PNG into RGBA pixels."""
    pos = 8  # skip PNG signature
    palette = []
    pixels_raw = b""
    width = height = 0
    bit_depth = color_type = None

    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        tag = data[pos+4:pos+8]
        chunk_data = data[pos+8:pos+8+length]

        if tag == b"IHDR":
            width, height, bit_depth, color_type, compression, filter_method, interlace = struct.unpack(
                ">IIBBBBB", chunk_data[:13]
            )
            if bit_depth != 8 or interlace != 0 or compression != 0 or filter_method != 0:
                raise ValueError(f"Unsupported PNG format: bit_depth={bit_depth}, interlace={interlace}")
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

    if color_type is None:
        raise ValueError("PNG missing IHDR chunk")

    if color_type == 3:
        bytes_per_pixel = 1
    elif color_type == 2:
        bytes_per_pixel = 3
    elif color_type == 6:
        bytes_per_pixel = 4
    else:
        raise ValueError(f"Unsupported PNG color type: {color_type}")

    # Convert PNG scanlines to RGBA
    result = []
    row_size = 1 + width * bytes_per_pixel  # filter byte + pixels
    previous = bytearray(width * bytes_per_pixel)
    for y in range(height):
        row_start = y * row_size
        filter_type = pixels_raw[row_start]
        scanline = pixels_raw[row_start + 1:row_start + row_size]
        current = bytearray(width * bytes_per_pixel)
        for i in range(len(scanline)):
            raw = scanline[i]
            left = current[i - bytes_per_pixel] if i >= bytes_per_pixel else 0
            up = previous[i] if previous else 0
            up_left = previous[i - bytes_per_pixel] if (previous and i >= bytes_per_pixel) else 0

            if filter_type == 0:
                value = raw
            elif filter_type == 1:
                value = (raw + left) & 0xFF
            elif filter_type == 2:
                value = (raw + up) & 0xFF
            elif filter_type == 3:
                value = (raw + ((left + up) // 2)) & 0xFF
            elif filter_type == 4:
                value = (raw + paeth_predictor(left, up, up_left)) & 0xFF
            else:
                raise ValueError(f"Unsupported PNG filter type: {filter_type}")
            current[i] = value
        previous = current

        for x in range(width):
            offset = x * bytes_per_pixel
            if color_type == 3:
                idx = current[offset]
                result.append(palette[idx])
            elif color_type == 2:
                r, g, b = current[offset:offset + 3]
                result.append((r, g, b, 255))
            else:
                r, g, b, a = current[offset:offset + 4]
                result.append((r, g, b, a))
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
    vanilla_data = load_vanilla_book()

    print("Decoding indexed PNG...")
    vanilla_pixels = decode_png(vanilla_data)
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
