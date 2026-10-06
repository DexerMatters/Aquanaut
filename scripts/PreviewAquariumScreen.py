#!/usr/bin/env python3
"""Render a preview of the aquarium screen without launching the game.

The drawing below mirrors ``client/screen/AquariumScreen.java``,
``client/screen/aquarium/AquariumTabSkin.java`` and ``AquariumTabLayout`` pixel for pixel: the same
panel recipe, the same tank frame and water bands, the same 26x32 tab silhouette, and the same
layout constants. Text is rendered with Minecraft's own ``ascii.png`` sheet, using the advance rule
from ``BitmapProvider`` (last visible column plus one), so the labels sit exactly where the game
would put them.

What it cannot show is the fish: those are live entities drawn by the game's entity renderer into the
tank, so the preview renders the empty tank instead.

Usage
-----
    python3 scripts/PreviewAquariumScreen.py [--out preview.png] [--scale 3] [--jar <client jar>]
"""

from __future__ import annotations

import argparse
import zipfile
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JAR = Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"
SARDINE = REPO_ROOT / "src/main/resources/assets/aquanaut/textures/item/sadine.png"

# --- layout, mirroring AquariumContainerMenu and AquariumScreen --------------------------------
IMAGE_WIDTH = 176
IMAGE_HEIGHT = 166
CELL = 18
COLS = 9
ROWS = 2
GRID_LEFT = 8
GRID_TOP = 26
MAIN_INV_Y = 84
HOTBAR_Y = 142
OVERFLOW = 6

WATER_LEFT = GRID_LEFT
WATER_TOP = GRID_TOP - OVERFLOW
WATER_WIDTH = COLS * CELL
WATER_HEIGHT = ROWS * CELL + 2 * OVERFLOW
TANK_INSET = 3

# --- palette -----------------------------------------------------------------------------------
PANEL = (198, 198, 198, 255)
OUTLINE = (0, 0, 0, 255)
EDGE_LIGHT = (255, 255, 255, 255)
EDGE_DARK = (85, 85, 85, 255)
WELL_SHADE = (55, 55, 55, 255)
GLASS_HIGHLIGHT = (233, 248, 253, 255)
LABEL = (64, 64, 64, 255)

WATER_SHALLOW = (62, 127, 163)
WATER_MID = (29, 72, 100)
WATER_DEEP = (10, 30, 46)

# The mod's tab, painted in the diving equipment panel palette (ClientInventoryPanelEvents).
AQUARIUM_TAB = {
    "outline": (0x32, 0x48, 0x48, 255),
    "highlight": (0xF2, 0xFF, 0xFF, 255),
    "fill": (0x88, 0x9A, 0x9A, 255),
    "selectedFill": (0xC2, 0xDB, 0xDB, 255),
    "shadow": (0x5D, 0x7A, 0x7A, 255),
}
INVENTORY_TAB = {
    "outline": (0, 0, 0, 255),
    "highlight": (255, 255, 255, 255),
    "fill": (139, 139, 139, 255),
    "selectedFill": (198, 198, 198, 255),
    "shadow": (85, 85, 85, 255),
}


def read(jar: zipfile.ZipFile, name: str) -> Image.Image:
    with jar.open(name) as handle:
        return Image.open(handle).convert("RGBA")


class Font:
    """Minecraft's default bitmap font: an 8x8 grid of glyphs with a per glyph advance."""

    def __init__(self, sheet: Image.Image):
        self.sheet = sheet
        self._advance: dict[str, int] = {}

    def advance(self, char: str) -> int:
        if char == " ":
            return 4
        if char in self._advance:
            return self._advance[char]
        code = ord(char)
        cell_x = code % 16 * 8
        cell_y = code // 16 * 8
        width = 0
        for column in range(8):
            for row in range(8):
                if self.sheet.getpixel((cell_x + column, cell_y + row))[3] != 0:
                    width = column + 1
                    break
        self._advance[char] = width + 1
        return self._advance[char]

    def width(self, text: str) -> int:
        return sum(self.advance(char) for char in text)

    def draw(self, canvas: Image.Image, text: str, x: int, y: int, color, scale: int, shadow: bool = False):
        if shadow:
            self._run(canvas, text, x + 1, y + 1, (63, 63, 63, 255), scale)
        self._run(canvas, text, x, y, color, scale)

    def _run(self, canvas: Image.Image, text: str, x: int, y: int, color, scale: int):
        for char in text:
            code = ord(char)
            if char != " ":
                cell_x = code % 16 * 8
                cell_y = code // 16 * 8
                glyph = self.sheet.crop((cell_x, cell_y, cell_x + 8, cell_y + 8))
                tinted = Image.new("RGBA", glyph.size, color[:3] + (0,))
                tinted.putalpha(glyph.getchannel("A"))
                if scale != 1:
                    tinted = tinted.resize((8 * scale, 8 * scale), Image.NEAREST)
                canvas.alpha_composite(tinted, (x * scale, y * scale))
            x += self.advance(char)


def fill(canvas: Image.Image, x1: int, y1: int, x2: int, y2: int, color, scale: int, origin=(0, 0)):
    if x2 <= x1 or y2 <= y1:
        return
    box = ((origin[0] + x1) * scale, (origin[1] + y1) * scale,
           (origin[0] + x2) * scale, (origin[1] + y2) * scale)
    canvas.paste(Image.new("RGBA", (box[2] - box[0], box[3] - box[1]), color), box[:2])


def blit(canvas: Image.Image, sprite: Image.Image, x: int, y: int, scale: int, origin=(0, 0)):
    if scale != 1:
        sprite = sprite.resize((sprite.width * scale, sprite.height * scale), Image.NEAREST)
    canvas.alpha_composite(sprite, ((origin[0] + x) * scale, (origin[1] + y) * scale))


def draw_panel(canvas, origin, scale, corners: Image.Image):
    left, top = origin
    right, bottom = left + IMAGE_WIDTH, top + IMAGE_HEIGHT
    fill(canvas, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, PANEL, scale, origin)
    fill(canvas, 0, 0, IMAGE_WIDTH, 1, OUTLINE, scale, origin)
    fill(canvas, 0, 0, 1, IMAGE_HEIGHT, OUTLINE, scale, origin)
    fill(canvas, IMAGE_WIDTH - 1, 0, IMAGE_WIDTH, IMAGE_HEIGHT, OUTLINE, scale, origin)
    fill(canvas, 0, IMAGE_HEIGHT - 1, IMAGE_WIDTH, IMAGE_HEIGHT, OUTLINE, scale, origin)
    fill(canvas, 1, 1, IMAGE_WIDTH - 1, 3, EDGE_LIGHT, scale, origin)
    fill(canvas, 1, 1, 3, IMAGE_HEIGHT - 1, EDGE_LIGHT, scale, origin)
    fill(canvas, 1, IMAGE_HEIGHT - 3, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1, EDGE_DARK, scale, origin)
    fill(canvas, IMAGE_WIDTH - 3, 1, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1, EDGE_DARK, scale, origin)
    for corner, source in (
        ((0, 0), (0, 0)),
        ((IMAGE_WIDTH - 4, 0), (172, 0)),
        ((0, IMAGE_HEIGHT - 4), (0, 162)),
        ((IMAGE_WIDTH - 4, IMAGE_HEIGHT - 4), (172, 162)),
    ):
        blit(canvas, corners.crop((source[0], source[1], source[0] + 4, source[1] + 4)), corner[0], corner[1],
             scale, origin)
    del right, bottom


def water_band(band: int, bands: int):
    t = band / (bands - 1)
    if t < 0.5:
        return mix(WATER_SHALLOW, WATER_MID, t * 2)
    return mix(WATER_MID, WATER_DEEP, (t - 0.5) * 2)


def mix(from_color, to_color, t: float):
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(from_color, to_color)) + (255,)


def lift(argb, amount: int = 0x18):
    return tuple(min(255, channel + amount) for channel in argb[:3]) + (argb[3],)


def draw_tank(canvas, origin, scale, water_frame: Image.Image, millis: int):
    fill(canvas, WATER_LEFT - TANK_INSET, WATER_TOP - TANK_INSET,
         WATER_LEFT + WATER_WIDTH + TANK_INSET, WATER_TOP + WATER_HEIGHT + TANK_INSET, OUTLINE, scale, origin)
    fill(canvas, WATER_LEFT - TANK_INSET + 1, WATER_TOP - TANK_INSET + 1,
         WATER_LEFT + WATER_WIDTH + TANK_INSET - 1, WATER_TOP, WELL_SHADE, scale, origin)
    fill(canvas, WATER_LEFT - TANK_INSET + 1, WATER_TOP - TANK_INSET + 1,
         WATER_LEFT, WATER_TOP + WATER_HEIGHT + TANK_INSET - 1, WELL_SHADE, scale, origin)
    fill(canvas, WATER_LEFT - TANK_INSET + 1, WATER_TOP + WATER_HEIGHT,
         WATER_LEFT + WATER_WIDTH + TANK_INSET - 1, WATER_TOP + WATER_HEIGHT + TANK_INSET - 1,
         GLASS_HIGHLIGHT, scale, origin)
    fill(canvas, WATER_LEFT + WATER_WIDTH, WATER_TOP - TANK_INSET + 1,
         WATER_LEFT + WATER_WIDTH + TANK_INSET - 1, WATER_TOP + WATER_HEIGHT + TANK_INSET - 1,
         GLASS_HIGHLIGHT, scale, origin)

    bands = 12
    for band in range(bands):
        fill(canvas, WATER_LEFT, WATER_TOP + band * WATER_HEIGHT // bands,
             WATER_LEFT + WATER_WIDTH, WATER_TOP + (band + 1) * WATER_HEIGHT // bands,
             water_band(band, bands), scale, origin)

    # The game scrolls the vanilla water sprite over the gradient with a 42% tint; the preview tiles
    # one frame of it the same way, minus the animation.
    tile = water_frame.copy()
    tint = Image.new("RGBA", tile.size, (107, 173, 219, 77))
    tile = Image.blend(Image.new("RGBA", tile.size, (0, 0, 0, 0)), Image.alpha_composite(tile, tint), 1.0)
    for row in range(0, WATER_HEIGHT, CELL):
        for col in range(0, WATER_WIDTH, CELL):
            patch = tile.crop((0, 0, min(CELL, WATER_WIDTH - col), min(CELL, WATER_HEIGHT - row)))
            blit(canvas, patch, WATER_LEFT + col, WATER_TOP + row, scale, origin)

    for index in range(7):
        period = 2600 + (index % 4) * 430
        phase = ((millis + index * 811) % period) / period
        bubble_x = WATER_LEFT + 5 + (index * 53 + 11) % (WATER_WIDTH - 10)
        bubble_y = WATER_TOP + (WATER_HEIGHT - 6) - int(phase * (WATER_HEIGHT - 6))
        size = 2 if index % 3 == 0 else 1
        alpha = int(0x7F * (1.0 - phase * 0.7))
        fill(canvas, bubble_x, bubble_y, bubble_x + size, bubble_y + size, (214, 242, 255, alpha), scale, origin)
    fill(canvas, WATER_LEFT, WATER_TOP, WATER_LEFT + WATER_WIDTH, WATER_TOP + 1, (207, 239, 255, 0x66), scale, origin)


def draw_slots(canvas, origin, scale, slot: Image.Image):
    for row in range(3):
        for col in range(COLS):
            blit(canvas, slot, GRID_LEFT + col * CELL - 1, MAIN_INV_Y + row * CELL - 1, scale, origin)
    for col in range(COLS):
        blit(canvas, slot, GRID_LEFT + col * CELL - 1, HOTBAR_Y - 1, scale, origin)


def draw_tab(canvas, origin, x: int, y: int, scale: int, palette, shape: str, icon: Image.Image | None = None):
    """Mirrors AquariumTabSkin: 26 wide, corner steps of one pixel, three rows deep.

    ``y`` is the sprite's top edge, which for an attached tab sits four pixels above its first
    visible row, exactly as ``AquariumTab#getY`` places the widget.
    """
    outline = palette["outline"]
    highlight = palette["highlight"]
    body_fill = palette["selectedFill"] if shape == "SELECTED" else palette["fill"]
    shadow = palette["shadow"]

    def body(from_y: int, to_y: int):
        fill(canvas, x, from_y, x + 1, to_y, outline, scale, origin)
        fill(canvas, x + 1, from_y, x + 3, to_y, highlight, scale, origin)
        fill(canvas, x + 3, from_y, x + 23, to_y, body_fill, scale, origin)
        fill(canvas, x + 23, from_y, x + 25, to_y, shadow, scale, origin)
        fill(canvas, x + 25, from_y, x + 26, to_y, outline, scale, origin)

    def bottom_steps(at: int):
        fill(canvas, x + 1, at, x + 2, at + 1, outline, scale, origin)
        fill(canvas, x + 2, at, x + 3, at + 1, body_fill, scale, origin)
        fill(canvas, x + 3, at, x + 25, at + 1, shadow, scale, origin)
        fill(canvas, x + 25, at, x + 26, at + 1, outline, scale, origin)
        fill(canvas, x + 2, at + 1, x + 3, at + 2, outline, scale, origin)
        fill(canvas, x + 3, at + 1, x + 24, at + 2, shadow, scale, origin)
        fill(canvas, x + 24, at + 1, x + 25, at + 2, outline, scale, origin)
        fill(canvas, x + 3, at + 2, x + 24, at + 3, outline, scale, origin)

    if shape == "SELECTED":
        fill(canvas, x, y, x + 26, y + 1, body_fill, scale, origin)
        fill(canvas, x, y + 1, x + 2, y + 3, shadow, scale, origin)
        fill(canvas, x + 2, y + 1, x + 3, y + 3, highlight, scale, origin)
        fill(canvas, x + 3, y + 1, x + 23, y + 3, body_fill, scale, origin)
        fill(canvas, x + 23, y + 1, x + 26, y + 3, shadow, scale, origin)
        fill(canvas, x, y + 3, x + 1, y + 4, outline, scale, origin)
        fill(canvas, x + 1, y + 3, x + 3, y + 4, highlight, scale, origin)
        fill(canvas, x + 3, y + 3, x + 23, y + 4, body_fill, scale, origin)
        fill(canvas, x + 23, y + 3, x + 25, y + 4, shadow, scale, origin)
        fill(canvas, x + 25, y + 3, x + 26, y + 4, outline, scale, origin)
        body(y + 4, y + 29)
        bottom_steps(y + 29)
        icon_y = y + 7
    else:
        body(y, y + 21)
        bottom_steps(y + 21)
        icon_y = y + 3

    if icon is not None:
        blit(canvas, icon, x + 5, icon_y, scale, origin)


def verify_tab_shape(jar: zipfile.ZipFile) -> int:
    """Compare the drawn tab silhouettes against the vanilla sprites they were traced from.

    Only the geometry is compared: the drawn pixels are flattened to opaque, then measured against the
    vanilla sprite's own alpha, so a colour choice can never hide a shape mistake.
    """
    problems = 0
    for shape, sprite_name, offset, height in (
        ("ATTACHED", "tab_bottom_unselected_2", 4, 24),
        ("SELECTED", "tab_bottom_selected_4", 0, 32),
    ):
        vanilla = read(jar, f"assets/minecraft/textures/gui/sprites/container/creative_inventory/{sprite_name}.png")
        drawn = Image.new("RGBA", (26, 32), (0, 0, 0, 0))
        draw_tab(drawn, (0, 0), 0, 0, 1, AQUARIUM_TAB, shape)
        mismatch = 0
        for y in range(height):
            for x in range(26):
                want = vanilla.getpixel((x, y + offset))[3] > 0
                got = drawn.getpixel((x, y))[3] > 0
                if want != got:
                    mismatch += 1
        state = "matches" if mismatch == 0 else f"{mismatch} pixels differ"
        print(f"tab shape {shape:<9} vs {sprite_name}: {state}")
        problems += mismatch
    return problems


def mock_chest_icon() -> Image.Image:
    """A 16x16 stand-in for the chest item, which vanilla renders as a 3D block entity."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dark = (52, 34, 18, 255)
    body = (124, 82, 42, 255)
    light = (154, 104, 55, 255)
    latch = (206, 184, 122, 255)

    def box(x1, y1, x2, y2, color):
        image.paste(Image.new("RGBA", (x2 - x1, y2 - y1), color), (x1, y1))

    box(2, 3, 14, 13, dark)
    box(3, 4, 13, 12, body)
    box(3, 4, 13, 5, light)
    box(3, 6, 13, 7, dark)
    box(3, 7, 13, 12, body)
    box(7, 7, 9, 10, dark)
    box(7, 8, 9, 9, latch)
    return image


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, default=DEFAULT_JAR, help="Minecraft client jar to read vanilla art from")
    parser.add_argument("--out", type=Path, default=REPO_ROOT / ".scratch/aquarium_screen_preview.png")
    parser.add_argument("--scale", type=int, default=3, help="pixel size of one GUI pixel")
    args = parser.parse_args()

    with zipfile.ZipFile(args.jar) as jar:
        inventory = read(jar, "assets/minecraft/textures/gui/container/inventory.png")
        slot = read(jar, "assets/minecraft/textures/gui/sprites/container/slot.png")
        water = read(jar, "assets/minecraft/textures/block/water_still.png").crop((0, 0, 16, 16))
        font_sheet = read(jar, "assets/minecraft/textures/font/ascii.png")
        problems = verify_tab_shape(jar)
    font = Font(font_sheet)
    # The chest item is a block-entity model in vanilla, so it has no flat sprite to lift; the preview
    # stands in with the classic front-on chest, drawn at the same 16x16 the game renders it at.
    chest = mock_chest_icon()
    sardine = Image.open(SARDINE).convert("RGBA")

    scale = args.scale
    margin = 14
    label_height = 12
    panels_width = IMAGE_WIDTH * 2 + margin * 3
    canvas = Image.new("RGBA", (panels_width * scale, (IMAGE_HEIGHT + 150 + label_height) * scale), (24, 26, 32, 255))

    inventory_origin = (margin, 24)
    aquarium_origin = (margin * 2 + IMAGE_WIDTH, 24)
    attached_y = IMAGE_HEIGHT
    selected_y = IMAGE_HEIGHT - 4

    # Left: the vanilla inventory with the new tab pair hung off its bottom-right corner.
    blit(canvas, inventory, 0, 0, scale, inventory_origin)
    draw_tab(canvas, inventory_origin, IMAGE_WIDTH - 26, selected_y, scale, INVENTORY_TAB, "SELECTED", chest)
    draw_tab(canvas, inventory_origin, IMAGE_WIDTH - 26 - 27, attached_y, scale, AQUARIUM_TAB, "ATTACHED", sardine)

    # Right: the aquarium itself, with the same pair and the fish tab selected.
    draw_panel(canvas, aquarium_origin, scale, inventory)
    draw_tank(canvas, aquarium_origin, scale, water, 1500)
    draw_slots(canvas, aquarium_origin, scale, slot)
    font.draw(canvas, "Aquarium", aquarium_origin[0] + 8, aquarium_origin[1] + 6, LABEL, scale)
    capacity = "0 / 18"
    font.draw(canvas, capacity, aquarium_origin[0] + IMAGE_WIDTH - 8 - font.width(capacity),
              aquarium_origin[1] + 6, LABEL, scale)
    font.draw(canvas, "Inventory", aquarium_origin[0] + 8, aquarium_origin[1] + 72, LABEL, scale)
    draw_tab(canvas, aquarium_origin, IMAGE_WIDTH - 26, attached_y, scale, INVENTORY_TAB, "ATTACHED", chest)
    draw_tab(canvas, aquarium_origin, IMAGE_WIDTH - 26 - 27, selected_y, scale, AQUARIUM_TAB, "SELECTED", sardine)

    # Detail strip: the aquarium's own pair, cropped back out of the canvas and doubled, so the corner
    # steps and the merge into the panel can be checked pixel for pixel.
    detail_box = (
        (aquarium_origin[0] + IMAGE_WIDTH - 53) * scale, (aquarium_origin[1] + IMAGE_HEIGHT - 6) * scale,
        (aquarium_origin[0] + IMAGE_WIDTH) * scale, (aquarium_origin[1] + IMAGE_HEIGHT + 30) * scale,
    )
    detail = canvas.crop(detail_box).resize((53 * 2 * scale, 36 * 2 * scale), Image.NEAREST)
    canvas.alpha_composite(detail, (margin * scale, (IMAGE_HEIGHT + 72) * scale))

    font.draw(canvas, "inventory + tabs", inventory_origin[0], 8, (170, 180, 190, 255), scale)
    font.draw(canvas, "aquarium, empty tank", aquarium_origin[0], 8, (170, 180, 190, 255), scale)
    font.draw(canvas, "selected + neighbour, doubled", margin + 112, IMAGE_HEIGHT + 86,
              (170, 180, 190, 255), scale)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(args.out)
    print(f"wrote {args.out} ({canvas.width}x{canvas.height})")
    if problems:
        raise SystemExit(f"tab shape check failed: {problems} pixel(s) differ from the vanilla sprites")


if __name__ == "__main__":
    main()
