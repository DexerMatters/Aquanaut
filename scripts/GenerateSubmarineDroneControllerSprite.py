#!/usr/bin/env python3
"""Generate the submarine drone controller's two inventory sprites.

Every handheld item in this pack is drawn on a diagonal with the light coming
from the top-left, so the controller follows suit: the device is built square in
its own frame - a rounded body, a cyan status screen, a D-pad and two amber
buttons, with a stub antenna whose lit tip matches the drone's sensor pod - and
then rotated onto the canvas, so the screen and buttons stay square to the body
instead of leaning with it.

There are two of them, and the difference is meant to be readable at sixteen
pixels:

* **deactivated** - the standby unit. The screen is dark, its only light a single
  standby pip in the corner; the antenna tip is unlit grey and both buttons are
  cold.
* **activated** - a controller that is holding a link. The screen comes up bright,
  the antenna tip burns the same cyan as the drone's sensor pod (with a one-pixel
  bloom on the shell around it) and the primary button is lit amber.

Which one the game draws is decided by an item model property over the link in
the stack's NBT, so the sprite changes the moment a drone is paired.

Outputs:
  src/main/resources/assets/aquanaut/textures/item/submarine_drone_controller.png
  src/main/resources/assets/aquanaut/textures/item/submarine_drone_controller_active.png

Run from the repository root:
  python3 scripts/GenerateSubmarineDroneControllerSprite.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

TEXTURE_DIR = Path("src/main/resources/assets/aquanaut/textures/item")
OUTPUT_STANDBY = TEXTURE_DIR / "submarine_drone_controller.png"
OUTPUT_ACTIVE = TEXTURE_DIR / "submarine_drone_controller_active.png"

SIZE = 16

# Sprite centre and tilt. The body runs along the local u axis and leans the same
# way as the scoop net and the gas flow meter.
CENTRE = (7.2, 8.4)
TILT = math.radians(-22.0)

# Half extents of the body in its own frame, and its corner rounding.
HALF_LENGTH = 4.4
HALF_WIDTH = 3.8
CORNER_RADIUS = 2.1

# Antenna: a thin stub out of the top end of the body.
ANTENNA_REACH = 1.5
ANTENNA_HALF_WIDTH = 0.8
ANTENNA_TIP_FROM = 0.9

# How far the outline extends past the shell, in body units (~one pixel).
OUTLINE_GROW = 0.95

# Palette, keyed to the drone's own materials: the same stainless shell, the same
# cyan lens and the same amber shroud band.
OUTLINE = (42, 49, 56)
SHELL_DARK = (87, 94, 100)
SHELL_MID = (122, 130, 136)
SHELL_LIGHT = (176, 180, 184)
SHELL_HIGHLIGHT = (214, 218, 221)
GRIP = (58, 64, 69)
SCREEN_FRAME = (46, 122, 140)
SCREEN = (95, 216, 236)
SCREEN_LIT = (191, 240, 250)
SCREEN_DIM = (44, 96, 112)
BUTTON = (232, 164, 104)
BUTTON_DARK = (165, 85, 39)
ANTENNA = (147, 153, 157)
ANTENNA_TIP = (191, 240, 250)
ANTENNA_TIP_DEAD = (104, 110, 116)

# The primary of the two amber buttons only lights while linked.
BUTTON_LIT = (255, 214, 150)
BUTTON_COLD = (122, 92, 66)


def to_local(x: float, y: float) -> tuple[float, float]:
    """Canvas pixel centre -> body frame: u along the length, v across it.

    Positive v points down-and-right, so the lit top-left edge is the negative v
    side.
    """
    dx = x - CENTRE[0]
    dy = y - CENTRE[1]
    cos = math.cos(TILT)
    sin = math.sin(TILT)
    return dx * cos + dy * sin, -dx * sin + dy * cos


def inside_body(u: float, v: float, grow: float = 0.0) -> bool:
    """Rounded-rectangle test in the body frame, optionally inflated."""
    half_length = HALF_LENGTH + grow
    half_width = HALF_WIDTH + grow
    radius = CORNER_RADIUS + grow
    if abs(v) > half_width or abs(u) > half_length:
        return False
    du = abs(u) - (half_length - radius)
    dv = abs(v) - (half_width - radius)
    if du <= 0 or dv <= 0:
        return True
    return du * du + dv * dv <= radius * radius


# Flat shading ramp for the shell, taken from the steel tones the rest of the
# pack's items are shaded with. Snapping to a ramp rather than scaling each pixel
# is what keeps the icon reading as pixel art.
SHELL_RAMP = (
    (214, 218, 221),
    (176, 180, 184),
    (147, 153, 157),
    (110, 118, 124),
    (87, 94, 100),
)


def shell_colour(u: float, v: float) -> tuple[int, int, int]:
    """Shell shading: a cylinder across the body, the grip a stop darker."""
    across = 1.0 - abs(v) / HALF_WIDTH
    step = int(round((1.0 - across) * (len(SHELL_RAMP) - 1)))
    if u < -0.4:
        step = min(len(SHELL_RAMP) - 1, step + 2)
    return SHELL_RAMP[step]


def detail(u: float, v: float, active: bool) -> tuple[int, int, int] | None:
    """Screen, D-pad and buttons, laid out in the body frame."""
    # Status screen, sunk into a dark bezel near the top end. Only the screen
    # itself changes with the link: the standby panel is dark with a single lit
    # pip in its outer corner, the linked one is backlit edge to edge.
    if 0.5 <= u <= 3.6 and abs(v) <= 2.2:
        if u >= 3.15 or abs(v) >= 1.85:
            return SCREEN_FRAME if active else SCREEN_DIM
        if active:
            # Backlit panel: hot along the lit top-left edge, cooler into the body.
            return SCREEN_LIT if v <= -0.9 else SCREEN
        # Standby panel: dark, save for one pip by the antenna end.
        return SCREEN if (u >= 2.6 and v <= -1.1) else SCREEN_DIM

    # D-pad cross on the grip.
    if abs(u + 2.5) <= 1.7 and abs(v) <= 0.5:
        return SHELL_LIGHT
    if abs(v) <= 1.7 and abs(u + 2.5) <= 0.5:
        return SHELL_LIGHT

    # Two amber buttons between the screen and the D-pad. The outer one is the
    # link button, so it is the one that lights.
    for index, button_u in enumerate((-0.35, -1.85)):
        if abs(u - button_u) <= 0.7 and abs(v + 2.7) <= 0.7:
            lit = active and index == 0
            if lit:
                return BUTTON_LIT if v <= -2.7 else BUTTON
            return BUTTON_COLD if v <= -2.7 else BUTTON_DARK
    return None


def bloom_around_tip(image: Image.Image) -> None:
    """Ring the lit antenna tip with a one-pixel halo.

    Done as a pass over the finished sprite rather than as a shape test, so the
    halo can only ever land next to a pixel that actually exists - a geometric
    neighbourhood test leaks stray pixels a pixel or two clear of the antenna.
    """
    pixels = image.load()
    tip = ANTENNA_TIP
    halo: list[tuple[int, int]] = []
    for y in range(image.height):
        for x in range(image.width):
            if pixels[x, y][:3] != tip or pixels[x, y][3] == 0:
                continue
            # Orthogonal neighbours only: a diagonal halo on a thin antenna reads as
            # a detached pixel rather than as light.
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < image.width and 0 <= ny < image.height and pixels[nx, ny][3] == 0:
                    halo.append((nx, ny))
    for nx, ny in halo:
        pixels[nx, ny] = (*SCREEN_FRAME, 80)


def render(active: bool) -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()

    for y in range(SIZE):
        for x in range(SIZE):
            px = x + 0.5
            py = y + 0.5
            u, v = to_local(px, py)

            # Antenna stub, tipped with the drone's sensor colour while linked.
            if abs(v) <= ANTENNA_HALF_WIDTH and HALF_LENGTH < u <= HALF_LENGTH + ANTENNA_REACH:
                tip = u > HALF_LENGTH + ANTENNA_TIP_FROM
                if tip:
                    pixels[x, y] = (*(ANTENNA_TIP if active else ANTENNA_TIP_DEAD), 255)
                else:
                    pixels[x, y] = (*ANTENNA, 255)
                continue

            # Outside the silhouette, or the one-pixel outline around it: the
            # shell inflated by a pixel, minus the shell itself. A local-frame
            # neighbour test would thicken on the diagonal, so the whole body is
            # inflated instead.
            if not inside_body(u, v, OUTLINE_GROW):
                continue
            if not inside_body(u, v):
                pixels[x, y] = (*OUTLINE, 255)
                continue

            painted = detail(u, v, active)
            if painted is not None:
                pixels[x, y] = (*painted, 255)
                continue

            # A one-pixel highlight down the lit top-left edge.
            if v < -(HALF_WIDTH - 1.2) and u > -1.0:
                pixels[x, y] = (*SHELL_HIGHLIGHT, 255)
                continue

            pixels[x, y] = (*shell_colour(u, v), 255)

    if active:
        bloom_around_tip(image)
    return image


LEGEND = {
    OUTLINE: "o",
    SHELL_HIGHLIGHT: "H",
    SHELL_LIGHT: "L",
    SHELL_MID: "m",
    SHELL_DARK: "d",
    GRIP: "g",
    SCREEN: "S",
    SCREEN_FRAME: "s",
    SCREEN_LIT: "!",
    SCREEN_DIM: "-",
    BUTTON: "B",
    BUTTON_DARK: "b",
    BUTTON_LIT: "e",
    BUTTON_COLD: "c",
    ANTENNA: "a",
    ANTENNA_TIP: "T",
    ANTENNA_TIP_DEAD: "t",
}


def preview(image: Image.Image, label: str) -> None:
    print(f"-- {label} --")
    for y in range(image.height):
        row = ""
        for x in range(image.width):
            r, g, b, a = image.getpixel((x, y))
            if a == 0:
                row += "."
                continue
            letter, best = "?", None
            for colour, candidate in LEGEND.items():
                distance = sum((c - k) ** 2 for c, k in zip(colour, (r, g, b)))
                if best is None or distance < best:
                    letter, best = candidate, distance
            row += letter
        print(row)


def main() -> None:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)

    standby = render(active=False)
    standby.save(OUTPUT_STANDBY)
    print(f"wrote {OUTPUT_STANDBY} ({standby.width}x{standby.height})")

    active = render(active=True)
    active.save(OUTPUT_ACTIVE)
    print(f"wrote {OUTPUT_ACTIVE} ({active.width}x{active.height})")

    preview(standby, "standby")
    preview(active, "active")


if __name__ == "__main__":
    main()
