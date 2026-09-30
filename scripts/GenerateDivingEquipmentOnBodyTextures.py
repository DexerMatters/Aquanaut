#!/usr/bin/env python3
"""Generate the worn diving equipment textures (the *_on_body.png sprites).

These textures are not item icons: they are the UV-unwrapped box faces the game
renderer wraps around the player model when a diving mask, oxygen tank, or pair
of flippers is worn (see DivingEquipmentRenderLayer). The layout is shared with
the game through ``diving_equipment_layout``.

Run from anywhere:

    python3 scripts/GenerateDivingEquipmentOnBodyTextures.py

Art notes
---------
* Everything is painted at Minecraft's pixel scale (nearest-neighbour, no
  smoothing) with a small fixed palette per material: outline, dark, mid, light,
  hi, plus accent colours.
* Shading is baked per face the way vanilla entity textures do it (top faces
  lightest, bottom faces darkest) so the gear reads as solid even in flat light.
* The random-looking speckles are seeded, so every run reproduces the exact
  same pixels.
"""

import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import diving_equipment_layout as layout  # noqa: E402

OUT_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
    "assets", "aquanaut", "textures", "equipment"))


# ---------------------------------------------------------------------------
# Colour helpers
# ---------------------------------------------------------------------------

def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, factor):
    if factor <= 1.0:
        return tuple(max(0, round(v * factor)) for v in c)
    return tuple(min(255, round(v + (255 - v) * (factor - 1.0) * 0.6)) for v in c)


def ramp(stops, t):
    """Interpolate through colour stops (t in 0..1)."""
    t = min(1.0, max(0.0, t))
    if len(stops) == 1:
        return stops[0]
    scaled = t * (len(stops) - 1)
    i = min(len(stops) - 2, int(scaled))
    return mix(stops[i], stops[i + 1], scaled - i)


# ---------------------------------------------------------------------------
# Drawing
# ---------------------------------------------------------------------------

class Face:
    """Paints one face rect of an atlas region in local face coordinates.

    Local (0, 0) is the top-left texel of the face as seen from outside the box;
    ``flip_u`` mirrors horizontally when a face has to show the same artwork as
    its opposite side (buckles and ridges must land at the same end of the box).
    """

    def __init__(self, img, rect, flip_u=False):
        self.img = img
        self.u0, self.v0, self.w, self.h = rect
        self.flip_u = flip_u

    # -- raw ---------------------------------------------------------------
    def px(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            ax = self.u0 + (self.w - 1 - x if self.flip_u else x)
            self.img.putpixel((ax, self.v0 + y), tuple(color) + (255,))

    def set_alpha(self, x, y, alpha):
        if 0 <= x < self.w and 0 <= y < self.h:
            ax = self.u0 + (self.w - 1 - x if self.flip_u else x)
            r, g, b, _ = self.img.getpixel((ax, self.v0 + y))
            self.img.putpixel((ax, self.v0 + y), (r, g, b, alpha))

    def copy_from(self, other, flip=False):
        """Paste another face's art (used to mirror side faces)."""
        for y in range(min(self.h, other.h)):
            for x in range(min(self.w, other.w)):
                sx = other.w - 1 - x if flip else x
                if 0 <= sx < other.w:
                    self.px(x, y, other.img.getpixel((other.u0 + sx, other.v0 + y))[:3])

    def fill(self, color):
        for y in range(self.h):
            for x in range(self.w):
                self.px(x, y, color)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, color)

    def hline(self, y, x0, x1, color):
        self.rect(x0, y, x1, y, color)

    def vline(self, x, y0, y1, color):
        self.rect(x, y0, x, y1, color)

    def grad_v(self, stops):
        for y in range(self.h):
            self.hline(y, 0, self.w - 1, ramp(stops, y / max(1, self.h - 1)))

    def grad_h(self, stops):
        for x in range(self.w):
            self.vline(x, 0, self.h - 1, ramp(stops, x / max(1, self.w - 1)))

    def outline(self, color):
        self.hline(0, 0, self.w - 1, color)
        self.hline(self.h - 1, 0, self.w - 1, color)
        self.vline(0, 0, self.h - 1, color)
        self.vline(self.w - 1, 0, self.h - 1, color)

    def speckle(self, colors, count, seed, area=None):
        rng = random.Random(seed)
        x0, y0, x1, y1 = area or (0, 0, self.w - 1, self.h - 1)
        for _ in range(count):
            self.px(rng.randint(x0, x1), rng.randint(y0, y1), rng.choice(colors))


class Sheet:
    """One 64x64 texture with its boxes' faces."""

    def __init__(self):
        self.img = Image.new("RGBA", (layout.ATLAS, layout.ATLAS), (0, 0, 0, 0))
        self._faces = {}

    def face(self, box, face, flip_u=False):
        key = (box.name, face)
        if key not in self._faces:
            self._faces[key] = Face(self.img, box.face_rects()[face], flip_u)
        return self._faces[key]

    def paint(self, box, painter, mirror_side=False):
        """Paint every face of a box; ``painter(face, face_name)`` draws one."""
        for face_name in layout.FACES:
            flip = mirror_side and face_name in ("-x", "+x")
            painter(self.face(box, face_name, flip_u=flip), face_name)

    def save(self, name):
        path = os.path.join(OUT_DIR, name + "_on_body.png")
        self.img.save(path)
        return path


# ---------------------------------------------------------------------------
# Materials
# ---------------------------------------------------------------------------

MATERIALS = {
    "iron": {
        "outline": (31, 39, 46), "dark": (74, 89, 98), "mid": (128, 143, 150),
        "light": (178, 191, 195), "hi": (219, 228, 228),
        "accent": (122, 88, 48), "accent_light": (198, 160, 94),
        "strap": (48, 57, 63), "strap_light": (92, 103, 109),
        "glass": [(26, 58, 92), (48, 106, 148), (112, 176, 202)], "glint": (206, 236, 240),
    },
    "wood": {
        "outline": (48, 29, 18), "dark": (108, 63, 29), "mid": (158, 99, 43),
        "light": (205, 148, 72), "hi": (233, 189, 120),
        "accent": (72, 78, 80), "accent_light": (156, 166, 160),
        "strap": (94, 62, 32), "strap_light": (150, 104, 58),
        "glass": [(30, 62, 92), (56, 112, 150), (120, 180, 204)], "glint": (212, 238, 240),
    },
    "shell": {
        "outline": (66, 58, 44), "dark": (152, 138, 106), "mid": (204, 191, 154),
        "light": (233, 222, 188), "hi": (251, 245, 216),
        "accent": (222, 178, 172), "accent_light": (246, 214, 200),
        "strap": (120, 106, 78), "strap_light": (186, 170, 128),
        "glass": [(36, 72, 104), (66, 128, 162), (138, 196, 214)], "glint": (222, 244, 238),
    },
    "hard_shell": {
        "outline": (23, 54, 58), "dark": (55, 100, 101), "mid": (100, 145, 134),
        "light": (152, 188, 160), "hi": (208, 220, 180),
        "accent": (40, 112, 122), "accent_light": (112, 182, 182),
        "strap": (34, 76, 80), "strap_light": (86, 138, 132),
        "glass": [(30, 70, 102), (58, 126, 158), (130, 194, 210)], "glint": (206, 240, 234),
    },
    "marine_alloy": {
        "outline": (15, 47, 63), "dark": (27, 100, 124), "mid": (50, 160, 182),
        "light": (122, 212, 218), "hi": (198, 243, 239),
        "accent": (120, 84, 46), "accent_light": (224, 182, 98),
        "strap": (26, 68, 82), "strap_light": (74, 122, 134),
        "glass": [(22, 62, 100), (42, 130, 170), (140, 214, 226)], "glint": (216, 248, 240),
    },
    "shark": {
        "outline": (27, 37, 43), "dark": (63, 77, 85), "mid": (105, 121, 129),
        "light": (151, 167, 171), "hi": (198, 210, 210),
        "accent": (36, 52, 62), "accent_light": (86, 104, 114),
        "strap": (46, 58, 66), "strap_light": (92, 108, 116),
        "glass": [(30, 66, 98), (58, 118, 152), (126, 186, 206)], "glint": (210, 238, 240),
    },
    "coral": {
        "outline": (72, 33, 29), "dark": (162, 59, 47), "mid": (216, 90, 63),
        "light": (243, 142, 94), "hi": (253, 192, 142),
        "accent": (196, 88, 110), "accent_light": (244, 152, 158),
        "strap": (128, 56, 46), "strap_light": (190, 96, 74),
        "glass": [(34, 74, 106), (62, 130, 164), (140, 200, 216)], "glint": (212, 242, 236),
    },
    "anglerfish": {
        "outline": (16, 26, 38), "dark": (41, 61, 83), "mid": (71, 101, 127),
        "light": (121, 157, 173), "hi": (182, 211, 213),
        "accent": (238, 202, 96), "accent_light": (255, 238, 150),
        "strap": (28, 44, 60), "strap_light": (66, 92, 112),
        "glass": [(20, 46, 84), (36, 88, 132), (96, 158, 192)], "glint": (172, 226, 224),
    },
    "ender": {
        "outline": (29, 13, 47), "dark": (68, 31, 112), "mid": (114, 59, 168),
        "light": (170, 112, 216), "hi": (218, 172, 247),
        "accent": (128, 42, 168), "accent_light": (212, 104, 232),
        "strap": (44, 22, 68), "strap_light": (96, 56, 138),
        "glass": [(38, 22, 88), (92, 48, 158), (178, 116, 226)], "glint": (230, 176, 255),
    },
    "slime": {
        "outline": (29, 65, 23), "dark": (71, 133, 37), "mid": (118, 187, 55),
        "light": (168, 223, 89), "hi": (214, 247, 142),
        "accent": (60, 112, 30), "accent_light": (148, 212, 74),
        "strap": (58, 100, 32), "strap_light": (118, 172, 58),
        "glass": [(52, 112, 42), (104, 180, 58), (178, 232, 108)], "glint": (226, 252, 172),
    },
}


def soft_edge(face, color, face_name=None):
    """Outline a face, or just edge-light it when it is too small for a border."""
    if face.w >= 4 and face.h >= 4:
        face.outline(color)
    else:
        face.hline(0, 0, face.w - 1, color)
        if face.h > 1:
            face.hline(face.h - 1, 0, face.w - 1, color)


def baked(kind):
    """Per-face baked lighting, like vanilla entity textures."""
    return {
        "top": 1.06, "bottom": 0.62,
        "-x": 0.86, "+x": 0.86,
        "+z": 1.0, "-z": 0.76,
    }[kind]


def lit(color, face_name, amount=1.0):
    return shade(color, baked(face_name) * amount)


# ---------------------------------------------------------------------------
# Shared detail painters
# ---------------------------------------------------------------------------

def webbing(face, pal, face_name, buckle_x=None, weave=True):
    """Nylon harness webbing with edge stitching and an optional metal buckle."""
    light = lit(pal["strap_light"], face_name)
    dark = lit(pal["strap"], face_name)
    face.fill(dark)
    if weave and face.h >= 4:
        for y in range(1, face.h - 1):
            face.hline(y, 1, face.w - 2, light if y % 2 == 0 else mix(light, dark, 0.45))
        for y in (0, face.h - 1):
            face.hline(y, 0, face.w - 1, lit(pal["outline"], face_name))
    elif weave and face.h == 2:
        face.hline(0, 0, face.w - 1, light)
        face.hline(1, 0, face.w - 1, lit(pal["outline"], face_name))
    elif weave:
        for x in range(0, face.w, 3):
            face.px(x, 0, light)
    if buckle_x is not None and face.w >= 5:
        x = buckle_x
        face.rect(x - 1, 0, x + 1, face.h - 1, lit(pal["accent"], face_name))
        face.vline(x - 1, 0, face.h - 1, lit(pal["accent_light"], face_name))
        face.vline(x, 1, max(1, face.h - 2), lit(pal["accent_light"], face_name))
        face.vline(x + 1, 0, face.h - 1, lit(pal["outline"], face_name))


def rivets(face, pal, face_name, points):
    for (x, y) in points:
        face.px(x, y, lit(pal["hi"], face_name))
        if face.w > x + 1 and face.h > y + 1:
            face.px(x + 1, y + 1, lit(pal["outline"], face_name))


def metal_surface(face, pal, face_name, seams=(), vertical=True):
    """Baked cylinder-ish shading: bright stripe off-centre, dark far edge."""
    stops = [lit(pal["dark"], face_name), lit(pal["mid"], face_name),
             lit(pal["hi"], face_name, 1.02), lit(pal["light"], face_name),
             lit(pal["mid"], face_name), lit(pal["dark"], face_name, 0.9)]
    if vertical:
        face.grad_h(stops)
    else:
        face.grad_v(stops)
    for s in seams:
        face.hline(s, 0, face.w - 1, lit(pal["outline"], face_name, 1.1))


# ---------------------------------------------------------------------------
# Masks
# ---------------------------------------------------------------------------

def paint_mask(sheet, variant):
    """A diving helmet: shell over the whole head, protruding rounded visor,
    crown valve and ear pods - the worn form of the rounded mask icons."""
    pal = MATERIALS[variant]
    shell = layout.MASK_SHELL

    def shell_face(face, face_name):
        o = lit(pal["outline"], face_name)
        mid = lit(pal["mid"], face_name)
        light = lit(pal["light"], face_name)
        hi = lit(pal["hi"], face_name)
        dark = lit(pal["dark"], face_name)
        w, h = face.w, face.h
        if face_name == "+z":
            # faceplate: bevelled rim around a dark visor socket, chin vents
            face.fill(mid)
            face.outline(o)
            face.hline(1, 1, w - 2, light)
            face.vline(1, 1, h - 2, light)
            face.hline(h - 2, 1, w - 2, dark)
            face.vline(w - 2, 1, h - 2, dark)
            # visor window: gasket ring around a see-through opening
            wx0, wy0, wx1, wy1 = 2, 3, w - 3, h - 3
            face.hline(wy0, wx0, wx1, o)
            face.hline(wy1, wx0, wx1, o)
            face.vline(wx0, wy0, wy1, o)
            face.vline(wx1, wy0, wy1, o)
            for y in range(wy0 + 1, wy1):
                for x in range(wx0 + 1, wx1):
                    face.set_alpha(x, y, 0)
            for x in range(3, w - 3, 2):
                face.px(x, h - 2, dark)
            rivets(face, pal, face_name,
                   [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)])
            mask_variant_front(face, pal, variant, face_name)
        elif face_name == "top":
            # crown: concentric shading into the valve base
            face.fill(mid)
            face.outline(o)
            face.rect(1, 1, w - 2, h - 2, light)
            face.rect(2, 2, w - 3, h - 3, mid)
            cx, cy = w // 2, h // 2
            face.rect(cx - 2, cy - 2, cx + 1, cy + 1, dark)
            face.rect(cx - 1, cy - 1, cx, cy, lit(pal["accent"], face_name))
            face.hline(1, cx - 1, cx, hi)
        elif face_name == "bottom":
            # neck ring
            face.fill(dark)
            face.outline(o)
            face.rect(2, 2, w - 3, h - 3, o)
            face.rect(3, 3, w - 4, h - 4, shade(o, 0.75))
        elif face_name == "-z":
            # back plate: centre seam and exhaust louvres
            face.fill(mid)
            face.outline(o)
            face.hline(1, 1, w - 2, light)
            face.vline(w // 2, 1, h - 2, dark)
            for y in range(3, h - 2, 2):
                face.hline(y, 2, w - 3, mix(mid, dark, 0.55))
            face.hline(h - 2, 1, w - 2, dark)
        else:
            # sides: pod socket ring at ear height
            face.fill(mid)
            face.outline(o)
            face.hline(1, 1, w - 2, light)
            face.hline(h - 2, 1, w - 2, dark)
            cx, cy = w // 2, h // 2
            face.rect(cx - 2, cy - 2, cx + 1, cy + 1, dark)
            face.rect(cx - 1, cy - 1, cx, cy, lit(pal["accent"], face_name))

    sheet.paint(shell, shell_face)

    def visor_face(face, face_name):
        if face_name == "-z":
            return  # open back: the player's face shows through the glass
        if face_name == "+z":
            paint_glass(face, pal, face_name, variant)
            # round the visor corners into the rim
            rim = lit(pal["mid"], face_name)
            corners = [(1, 1), (face.w - 2, 1), (1, face.h - 2), (face.w - 2, face.h - 2),
                       (1, 2), (face.w - 2, 2)]
            for (x, y) in corners:
                face.px(x, y, rim)
            # the pane itself is see-through glass
            for y in range(1, face.h - 1):
                for x in range(1, face.w - 1):
                    face.set_alpha(x, y, 150)
            for (x, y) in corners:
                face.set_alpha(x, y, 255)
        else:
            face.fill(lit(pal["mid"], face_name))
            soft_edge(face, lit(pal["outline"], face_name))
            face.hline(0, 0, face.w - 1, lit(pal["hi"], face_name))

    sheet.paint(layout.MASK_VISOR, visor_face)

    def valve_face(face, face_name):
        bulb = variant == "anglerfish"
        base = pal["accent_light"] if bulb and face_name == "top" else pal["accent"]
        face.fill(lit(base, face_name))
        face.hline(0, 0, face.w - 1, lit(pal["accent_light"], face_name))
        if face.h > 1:
            face.hline(face.h - 1, 0, face.w - 1, lit(pal["outline"], face_name))

    sheet.paint(layout.MASK_VALVE, valve_face)

    def pod_face(face, face_name):
        face.fill(lit(pal["dark"], face_name))
        face.px(face.w // 2, face.h // 2, lit(pal["accent_light"], face_name))
        face.px(0, 0, lit(pal["outline"], face_name))

    sheet.paint(layout.MASK_POD, pod_face, mirror_side=True)


def paint_glass(face, pal, face_name, variant):
    glass = pal["glass"]
    stops = [shade(glass[2], baked(face_name) * 1.05), shade(glass[1], baked(face_name)),
             shade(glass[0], baked(face_name) * 0.95)]
    face.grad_v(stops)
    glint = lit(pal["glint"], face_name)
    # diagonal reflection across the top-left corner
    for i in range(min(2, face.w)):
        face.px(i, min(face.h - 1, i), glint)
    face.px(min(2, face.w - 1), 0, glint)
    if face.w >= 3 and face.h >= 3:
        face.px(face.w - 1, face.h - 1, shade(glass[0], baked(face_name) * 0.8))
        face.px(face.w - 2, face.h - 1, mix(glint, glass[1], 0.55))
    if variant == "ender":
        # the icon's dark cross shining through the visor
        cx = face.w // 2
        cy = face.h // 2
        dark = lit((30, 12, 46), face_name)
        face.vline(cx, 0, face.h - 1, dark)
        face.hline(cy, 0, face.w - 1, dark)
        face.px(cx, cy, glint)
    elif variant == "slime":
        for x in range(1, face.w - 1, 2):
            face.px(x, face.h - 1, mix(glint, glass[1], 0.5))
    if face.w >= 4 and face.h >= 4:
        face.outline(lit(pal["outline"], face_name, 0.9))


def mask_variant_front(face, pal, variant, face_name):
    w, h = face.w, face.h
    hi = lit(pal["hi"], face_name)
    accent = lit(pal["accent_light"], face_name)
    if variant == "coral":
        for (x, y) in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
            face.px(x, y, accent)
        face.px(2, 1, hi)
    elif variant == "shell":
        for y in range(1, h - 1):
            t = y / max(1, h - 1)
            band = ramp([pal["hi"], pal["accent_light"], pal["accent"], pal["light"]], t)
            face.px(1, y, lit(band, face_name))
            face.px(w - 2, y, lit(band, face_name, 0.9))
    elif variant == "hard_shell":
        for x in (1, w - 2):
            face.vline(x, 1, h - 2, lit(pal["dark"], face_name))
        face.hline(0, 2, 3, lit(pal["hi"], face_name))
        face.hline(0, w - 4, w - 3, lit(pal["hi"], face_name))
    elif variant == "marine_alloy":
        # gold fasteners on the rim, visor keeps its sheen
        face.px(1, 1, lit(pal["accent_light"], face_name))
        face.px(w - 2, 1, lit(pal["accent_light"], face_name))
        face.px(1, h - 2, lit(pal["accent"], face_name))
        face.px(w - 2, h - 2, lit(pal["accent"], face_name))
        face.px(2, 1, hi)
    elif variant == "anglerfish":
        rng = random.Random(7)
        for _ in range(6):
            face.px(rng.randint(1, w - 2), rng.choice((0, h - 1)),
                    lit(pal["light"], face_name, 0.92))
    elif variant == "ender":
        # the icon's dark cross shining through the visor
        dark = lit((30, 12, 46), face_name)
        face.vline(w // 2, 1, h - 2, dark)
        face.hline(min(2, h - 2), 2, w - 3, dark)
        face.px(w // 2, 1, lit(pal["glint"], face_name))
        face.px(1, 1, accent)
        face.px(w - 2, h - 2, accent)
    elif variant == "slime":
        for x in range(3, w - 3, 3):
            face.px(x, h - 2, lit(pal["hi"], face_name, 1.05))
        face.px(2, 2, lit(pal["hi"], face_name, 1.05))
        face.px(w - 3, 2, lit(pal["light"], face_name))
        face.px(2, 1, hi)
        face.px(w - 3, 1, hi)


# ---------------------------------------------------------------------------
# Oxygen tanks
# ---------------------------------------------------------------------------

def paint_tank(sheet, variant):
    pal = MATERIALS[variant]
    bottle = layout.TANK_BOTTLE

    def bottle_front(face, face_name):
        metal_surface(face, pal, face_name)
        o = lit(pal["outline"], face_name)
        dark = lit(pal["dark"], face_name)
        hi = lit(pal["hi"], face_name)
        accent = lit(pal["accent"], face_name)
        accent_hi = lit(pal["accent_light"], face_name)
        if face.w < 8:
            # vanilla-density cylinder: curvature columns, label, valve glint
            face.grad_h([lit(pal["dark"], face_name), lit(pal["mid"], face_name),
                         lit(pal["hi"], face_name), lit(pal["mid"], face_name),
                         lit(pal["dark"], face_name)])
            face.hline(0, 0, face.w - 1, dark)
            face.hline(face.h - 1, 0, face.w - 1, o)
            face.hline(face.h - 3, 1, face.w - 2, accent)
            face.px(face.w - 2, 2, accent_hi)
            face.px(face.w - 2, 3, dark)
            face.px(1, 1, hi)
            return
        # rounded shoulder and foot bands
        face.hline(0, 0, face.w - 1, dark)
        face.hline(1, 2, face.w - 3, lit(pal["light"], face_name))
        face.hline(1, 4, 6, hi)
        face.hline(face.h - 2, 0, face.w - 1, dark)
        face.hline(face.h - 1, 0, face.w - 1, o)
        # weld seams down the shell and ribs around it
        face.vline(2, 2, face.h - 3, lit(pal["dark"], face_name))
        face.vline(face.w - 3, 2, face.h - 3, lit(pal["dark"], face_name))
        for rib in (6, 8):
            face.hline(rib, 1, face.w - 2, lit(pal["dark"], face_name))
            face.hline(rib + 1, 2, face.w - 3, lit(pal["light"], face_name))
        # pressure gauge: dark bezel, bright dial, brass needle
        gx, gy = face.w - 5, 4
        face.rect(gx - 2, gy - 2, gx + 2, gy + 2, o)
        face.rect(gx - 1, gy - 1, gx + 1, gy + 1, hi)
        face.px(gx + 1, gy - 1, lit(pal["light"], face_name))
        face.px(gx, gy, accent)
        face.px(gx - 1, gy - 1, accent_hi)
        face.px(gx - 2, gy + 1, accent)
        # label band with stripes
        face.rect(1, face.h - 6, face.w - 2, face.h - 4, accent)
        face.hline(face.h - 6, 1, face.w - 2, accent_hi)
        face.hline(face.h - 5, 2, face.w - 3, lit(pal["accent"], face_name, 1.15))
        for x in range(2, face.w - 2, 3):
            face.vline(x, face.h - 5, face.h - 5, o)
        tank_variant_bottle(face, pal, variant, face_name)

    def bottle_side(face, face_name):
        stops = [lit(pal["dark"], face_name), lit(pal["light"], face_name),
                 lit(pal["mid"], face_name), lit(pal["dark"], face_name, 0.9)]
        face.grad_h(stops)
        soft_edge(face, lit(pal["outline"], face_name))
        if face.h > 3:
            for y in (1, face.h - 2):
                face.hline(y, 0, face.w - 1, lit(pal["outline"], face_name, 1.05))
            face.rect(1, face.h - 6, face.w - 2, face.h - 4, lit(pal["accent"], face_name))

    def bottle_cap(face, face_name):
        stops = [lit(pal["dark"], face_name), lit(pal["mid"], face_name),
                 lit(pal["hi"], face_name), lit(pal["mid"], face_name), lit(pal["dark"], face_name)]
        face.grad_h(stops) if face_name in ("-x", "+x", "+z", "-z") else face.grad_v(stops)
        face.outline(lit(pal["outline"], face_name))

    sheet.paint(bottle, bottle_front, mirror_side=False)
    for side in ("-x", "+x"):
        bottle_side(sheet.face(bottle, side), side)
    for side in ("top", "bottom", "-z"):
        bottle_cap(sheet.face(bottle, side), side)

    def ring(face, face_name):
        face.fill(lit(pal["dark"], face_name))
        face.outline(lit(pal["outline"], face_name))
        face.hline(0, 1, face.w - 2, lit(pal["hi"], face_name))
        if face.h >= 4:
            face.hline(1, 0, face.w - 1, lit(pal["mid"], face_name))
            rivets(face, pal, face_name,
                   [(2, 1), (face.w // 2, 1), (face.w - 3, 1)])
        if face.h > 1:
            face.hline(face.h - 1, 0, face.w - 1, lit(pal["outline"], face_name))

    sheet.paint(layout.TANK_RING, ring)

    def cap(face, face_name):
        face.fill(lit(pal["mid"], face_name))
        soft_edge(face, lit(pal["outline"], face_name))
        face.hline(0, 0, face.w - 1, lit(pal["hi"], face_name))
        if face.w > 2:
            face.vline(face.w - 1, 1, face.h - 1, lit(pal["dark"], face_name))

    sheet.paint(layout.TANK_CAP, cap)

    def valve(face, face_name):
        face.fill(lit(pal["accent"], face_name))
        face.hline(0, 0, face.w - 1, lit(pal["accent_light"], face_name))
        if face.h > 1:
            face.hline(face.h - 1, 0, face.w - 1, lit(pal["outline"], face_name))

    sheet.paint(layout.TANK_VALVE, valve)

    def shoulder(face, face_name):
        webbing(face, pal, face_name, buckle_x=face.w // 2 if face.w > 6 else None)

    sheet.paint(layout.TANK_SHOULDER, shoulder, mirror_side=True)

    def chest(face, face_name):
        webbing(face, pal, face_name)
        if face_name == "+z" and face.h >= 8:
            y = face.h // 2
            face.rect(0, y - 1, face.w - 1, y + 1, lit(pal["accent"], face_name))
            face.hline(y - 1, 0, face.w - 1, lit(pal["accent_light"], face_name))
            face.px(face.w // 2, y, lit(pal["outline"], face_name))

    sheet.paint(layout.TANK_CHEST, chest, mirror_side=True)

    def belt(face, face_name):
        webbing(face, pal, face_name)
        if face_name == "+z":
            x = face.w // 2
            face.rect(x - 2, 0, x + 2, face.h - 1, lit(pal["accent"], face_name))
            face.rect(x - 1, 1, x + 1, face.h - 2, lit(pal["accent_light"], face_name))
            face.vline(x, 1, face.h - 2, lit(pal["outline"], face_name))

    sheet.paint(layout.TANK_BELT, belt)


def tank_variant_bottle(face, pal, variant, face_name):
    o = lit(pal["outline"], face_name)
    hi = lit(pal["hi"], face_name)
    if variant == "wood":
        for x in range(3, face.w - 2, 3):
            face.vline(x, 2, face.h - 7, o)
        for y in (3, 8):
            face.hline(y, 2, face.w - 3, lit(pal["dark"], face_name))
        face.px(4, 5, hi)
        face.px(face.w - 5, 10, hi)
    elif variant == "shell":
        cx, cy = face.w // 2, face.h // 2
        for i in range(4):
            face.px(cx - 1 + i, cy - 2 + i // 2, hi)
            face.px(cx + 1 - i, cy + 2 - i // 2, lit(pal["accent_light"], face_name))
        face.hline(cy, 2, face.w - 3, lit(pal["light"], face_name))
    elif variant == "hard_shell":
        for y in (4, 9, 13):
            face.hline(y, 1, face.w - 2, o)
        rivets(face, pal, face_name, [(2, 4), (face.w - 3, 4), (2, 9), (face.w - 3, 9), (2, 13), (face.w - 3, 13)])
    elif variant == "marine_alloy":
        face.hline(3, 2, face.w - 3, lit(pal["accent_light"], face_name))
        face.hline(face.h - 7, 2, face.w - 3, lit(pal["accent_light"], face_name))
        face.px(3, 7, hi)
        face.px(face.w - 4, 8, hi)


# ---------------------------------------------------------------------------
# Flippers
# ---------------------------------------------------------------------------

def paint_flipper(sheet, variant):
    pal = MATERIALS[variant]
    pocket = layout.FLIPPER_POCKET

    def pocket_face(face, face_name):
        if face_name == "top":
            # foot opening with a raised rim
            face.fill(lit(pal["mid"], face_name))
            face.outline(lit(pal["outline"], face_name))
            face.rect(1, 1, face.w - 2, face.h - 2, lit(pal["light"], face_name))
            face.rect(2, 2, face.w - 3, face.h - 3, lit(pal["dark"], face_name, 0.85))
            face.rect(3, 3, face.w - 4, face.h - 4, lit(pal["outline"], face_name, 0.9))
            face.hline(1, 1, face.w - 2, lit(pal["hi"], face_name))
        elif face_name == "bottom":
            # sole tread
            face.fill(lit(pal["dark"], face_name))
            face.outline(lit(pal["outline"], face_name))
            for x in range(2, face.w - 2, 3):
                face.vline(x, 1, face.h - 2, lit(pal["outline"], face_name, 1.1))
        else:
            metal_surface(face, pal, face_name, vertical=(face_name in ("+z", "-z")))
            soft_edge(face, lit(pal["outline"], face_name))
            # heel strap anchor + rail
            face.hline(1, 1, face.w - 2, lit(pal["hi"], face_name))
            face.hline(face.h - 2, 1, face.w - 2, lit(pal["dark"], face_name))
            face.vline(1, 2, face.h - 3, lit(pal["light"], face_name))
            face.vline(face.w - 2, 2, face.h - 3, lit(pal["dark"], face_name))
            rivets(face, pal, face_name, [(2, 2), (face.w - 3, 2), (2, face.h - 3), (face.w - 3, face.h - 3)])
        flipper_variant(face, pal, variant, face_name, "pocket")

    sheet.paint(pocket, pocket_face)

    def strap(face, face_name):
        webbing(face, pal, face_name, buckle_x=face.w // 2 if face.w >= 6 else None)

    sheet.paint(layout.FLIPPER_STRAP, strap)

    for blade, index in ((layout.FLIPPER_BLADE1, 1), (layout.FLIPPER_BLADE2, 2), (layout.FLIPPER_BLADE3, 3)):
        def blade_face(face, face_name, blade=blade, index=index):
            if face_name in ("top", "bottom"):
                stops = [lit(pal["dark"], face_name), lit(pal["mid"], face_name),
                         lit(pal["hi"], face_name), lit(pal["mid"], face_name),
                         lit(pal["dark"], face_name)]
                face.grad_h(stops)
                # central ridge and side rails
                cx = face.w // 2
                face.vline(cx, 0, face.h - 1, lit(pal["hi"], face_name, 1.04))
                face.vline(cx - 1, 0, face.h - 1, lit(pal["light"], face_name))
                face.vline(0, 0, face.h - 1, lit(pal["outline"], face_name))
                face.vline(face.w - 1, 0, face.h - 1, lit(pal["outline"], face_name))
                if face_name == "bottom":
                    for x in range(2, face.w - 2, 3):
                        face.vline(x, 0, face.h - 1, lit(pal["dark"], face_name, 0.9))
            elif face.h == 1:
                face.fill(lit(pal["mid"], face_name))
                face.px(face.w // 2, 0, lit(pal["hi"], face_name))
            else:
                face.fill(lit(pal["mid"], face_name))
                face.outline(lit(pal["outline"], face_name))
                face.hline(0, 0, face.w - 1, lit(pal["hi"], face_name))
                face.hline(face.h - 1, 0, face.w - 1, lit(pal["dark"], face_name))
            flipper_variant(face, pal, variant, face_name, "blade%d" % index)

        sheet.paint(blade, blade_face)


def flipper_variant(face, pal, variant, face_name, part):
    hi = lit(pal["hi"], face_name)
    o = lit(pal["outline"], face_name)
    if variant == "shark":
        face.speckle([lit(pal["dark"], face_name), lit(pal["light"], face_name)],
                     max(2, face.w * face.h // 12), hash((part, face_name)) & 0xFFFF)
    elif variant == "wood":
        for y in range(1, face.h - 1, 3):
            face.hline(y, 1, face.w - 2, lit(pal["dark"], face_name))
        face.px(2, 1, hi)
    elif variant == "coral":
        for i in range(min(face.w, face.h)):
            face.px(1 + i, min(face.h - 1, i), lit(pal["accent_light"], face_name))
        face.px(face.w - 2, 1, hi)
    elif variant == "shell":
        for x in range(1, face.w - 1, 2):
            face.vline(x, 1, max(1, face.h - 2), lit(pal["accent_light"], face_name, 1.02))
    elif variant == "hard_shell":
        for x in range(2, face.w - 1, 4):
            face.vline(x, 0, face.h - 1, o)
        rivets(face, pal, face_name, [(1, 1), (face.w - 2, 1), (1, face.h - 2), (face.w - 2, face.h - 2)])
    elif variant == "marine_alloy":
        if face.w >= 4:
            face.vline(face.w // 2, 0, face.h - 1, lit(pal["accent_light"], face_name))
        face.px(1, 1, hi)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

GENERATORS = {"mask": paint_mask, "tank": paint_tank, "flipper": paint_flipper}


def main():
    layout.validate()
    os.makedirs(OUT_DIR, exist_ok=True)
    written = []
    for item_id, (category, variant) in layout.EQUIPMENT.items():
        sheet = Sheet()
        GENERATORS[category](sheet, variant)
        written.append(sheet.save(item_id))
    print("Generated %d worn equipment textures in %s" % (len(written), OUT_DIR))


if __name__ == "__main__":
    main()
