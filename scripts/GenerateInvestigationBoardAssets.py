#!/usr/bin/env python3
"""Build the investigation board's game assets from the Blockbench export.

Three jobs, in one re-runnable step:

1. **Geometry** - take the raw Bedrock export of
   ``blockbench-scripts/investigation_board/investigation_board.geo.json`` and
   normalise it to the form every other shipped Aquanaut geo uses:
   ``format_version`` 1.12.0 and a ``geometry.<slug>`` identifier.  Blockbench
   compiles the geometry itself (the same code path as File > Export), so the X
   mirror and the negated rotations are Blockbench's own Bedrock conventions,
   not ours.
2. **Block texture** - copy the model's atlas to ``textures/block``.
3. **Item icon** - draw the 16x16 inventory sprite.  The board is rendered in
   the world by a GeckoLib block-entity renderer, so it has no vanilla block
   model the item could inherit.  The sprite is drawn as an oblique slab: the
   board's face is a parallelogram, with its top and one side edge turned back
   behind it, so the flat panel reads as an object with thickness.  Colours come
   from the board's own atlas, lifted into a brighter register for the 16px
   scale.

Usage
-----
    python3 scripts/GenerateInvestigationBoardAssets.py [--preview]
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut"
SOURCE = REPO_ROOT / "blockbench-scripts" / "investigation_board"
EXPORTED_GEO = SOURCE / "investigation_board.geo.json"

GEO_OUT = ASSETS / "geo" / "investigation_board.geo.json"
BLOCK_TEXTURE_OUT = ASSETS / "textures" / "block" / "investigation_board.png"
ICON_OUT = ASSETS / "textures" / "item" / "investigation_board.png"

SIZE = 16

# Palette taken from the board atlas (blockbench-scripts/investigation_board),
# brightened so the 16px sprite stays readable in an inventory grid.
OUTLINE = (22, 26, 32, 255)
IRON_DARK = (78, 92, 106, 255)
IRON = (126, 143, 158, 255)
IRON_LIGHT = (176, 194, 208, 255)
BOARD = (236, 247, 244, 255)
BOARD_SHADE = (186, 208, 203, 255)
PAPER = (255, 250, 232, 255)
PAPER_SHADE = (232, 214, 172, 255)
INK = (38, 44, 52, 255)
STRING = (226, 68, 56, 255)
STRING_LIGHT = (255, 112, 92, 255)
NOTE_YELLOW = (255, 226, 92, 255)
NOTE_PINK = (255, 148, 172, 255)
NOTE_GREEN = (128, 216, 138, 255)
PIN = (244, 248, 252, 255)

# The slab: a sheared board face with its top and one side edge turned back.
FACE_W = 11
FACE_H = 8
SHEAR = 2
DEPTH = (-1, -1)
ORIGIN = (2, 13)


def draw_flat_board() -> Image.Image:
    """The board face, square on: frame, white surface, pinned evidence."""
    img = Image.new("RGBA", (FACE_W, FACE_H), (0, 0, 0, 0))
    px = img.load()

    def rect(x: int, y: int, w: int, h: int, colour) -> None:
        for j in range(y, y + h):
            for i in range(x, x + w):
                px[i, j] = colour

    def line(x0: int, y0: int, x1: int, y1: int, colour) -> None:
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            px[x0, y0] = colour
            if x0 == x1 and y0 == y1:
                return
            twice = 2 * err
            if twice >= dy:
                err += dy
                x0 += sx
            if twice <= dx:
                err += dx
                y0 += sy

    # Frame and board surface.
    rect(0, 0, FACE_W, FACE_H, IRON)
    rect(0, 0, FACE_W, 1, IRON_LIGHT)
    rect(1, 1, FACE_W - 2, FACE_H - 2, BOARD)
    rect(1, FACE_H - 2, FACE_W - 2, 1, BOARD_SHADE)

    # Pinned papers.
    rect(1, 1, 4, 3, PAPER)
    rect(1, 1, 4, 1, PAPER_SHADE)
    rect(6, 1, 4, 2, PAPER)
    rect(6, 1, 4, 1, PAPER_SHADE)
    rect(4, 4, 6, 3, PAPER)
    rect(4, 4, 6, 1, PAPER_SHADE)

    # Typed lines.
    rect(2, 2, 2, 1, INK)
    rect(7, 2, 2, 1, INK)
    rect(5, 5, 4, 1, INK)
    rect(5, 6, 3, 1, INK)

    # Sticky notes, vivid against the paper.
    rect(1, 4, 2, 2, NOTE_YELLOW)
    rect(1, 4, 2, 1, PAPER_SHADE)
    rect(1, 6, 2, 1, NOTE_GREEN)

    # Two evidence cords crossing over the sheets.
    line(2, 2, 8, 5, STRING)
    line(2, 6, 8, 2, STRING_LIGHT)

    # Pin heads sit on top of the cords.
    for x, y in ((2, 2), (8, 5), (2, 6), (8, 2)):
        px[x, y] = PIN
    return img


def draw_icon() -> Image.Image:
    """The flat face, sheared into a parallelogram and backed by its two edges."""
    flat = draw_flat_board()
    icon = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = icon.load()

    def shear(row: int) -> int:
        """How far right this row of the face slides: zero at the bottom, SHEAR at the top."""
        return SHEAR * (FACE_H - 1 - row) // (FACE_H - 1)

    def vertex(u: int, v: int, back: int = 0) -> tuple[int, int]:
        """u right along the face, v down from its top edge; back steps behind it."""
        return (ORIGIN[0] + u + shear(v) + DEPTH[0] * back,
                ORIGIN[1] - (FACE_H - 1 - v) + DEPTH[1] * back)

    # The two turned-back edges first, so the face overlaps them where they meet.
    draw = ImageDraw.Draw(icon)
    draw.polygon([vertex(0, 0), vertex(FACE_W - 1, 0), vertex(FACE_W - 1, 0, 1), vertex(0, 0, 1)],
                 fill=IRON_LIGHT)
    draw.polygon([vertex(0, 0), vertex(0, FACE_H - 1), vertex(0, FACE_H - 1, 1), vertex(0, 0, 1)],
                 fill=IRON_DARK)

    # The face itself, sheared row by row so the frame and its contents skew with it.
    for row in range(FACE_H):
        shift = shear(row)
        for u in range(FACE_W):
            colour = flat.getpixel((u, row))
            if colour[3]:
                px[ORIGIN[0] + u + shift, ORIGIN[1] - (FACE_H - 1 - row)] = colour

    return _outline(icon)


def _outline(icon: Image.Image) -> Image.Image:
    """Paint a one pixel dark ring around the slab, so it reads as a solid object."""
    from PIL import ImageFilter

    silhouette = icon.getchannel("A")
    grown = silhouette.filter(ImageFilter.MaxFilter(3))
    out = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    out.paste(OUTLINE, (0, 0), grown)
    out.paste(icon, (0, 0), silhouette)
    return out


def normalise_geometry() -> dict:
    geo = json.loads(EXPORTED_GEO.read_text(encoding="utf-8"))
    geo["format_version"] = "1.12.0"
    geometries = geo.get("minecraft:geometry", [])
    if len(geometries) != 1:
        raise SystemExit(f"expected exactly one geometry, found {len(geometries)}")
    geometries[0]["description"]["identifier"] = "geometry.investigation_board"
    GEO_OUT.parent.mkdir(parents=True, exist_ok=True)
    GEO_OUT.write_text(json.dumps(geo, indent="\t") + "\n", encoding="utf-8")
    return {
        "bones": len(geometries[0].get("bones", [])),
        "cubes": sum(len(bone.get("cubes", [])) for bone in geometries[0].get("bones", [])),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", action="store_true", help="also write the icon scaled up for review")
    args = parser.parse_args()

    stats = normalise_geometry()
    print(f"geo      -> {GEO_OUT.relative_to(REPO_ROOT)} ({stats['bones']} bones, {stats['cubes']} cubes)")

    texture = SOURCE / "investigation_board.png"
    BLOCK_TEXTURE_OUT.parent.mkdir(parents=True, exist_ok=True)
    BLOCK_TEXTURE_OUT.write_bytes(texture.read_bytes())
    with Image.open(texture) as atlas:
        print(f"block tex-> {BLOCK_TEXTURE_OUT.relative_to(REPO_ROOT)} ({atlas.width}x{atlas.height})")

    icon = draw_icon()
    ICON_OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.save(ICON_OUT)
    print(f"item icon-> {ICON_OUT.relative_to(REPO_ROOT)} ({icon.width}x{icon.height})")

    if args.preview:
        preview = SOURCE / "item_icon_preview.png"
        icon.resize((SIZE * 12, SIZE * 12), Image.Resampling.NEAREST).save(preview)
        print(f"preview  -> {preview.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
