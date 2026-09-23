#!/usr/bin/env python3
"""Draw the handheld air bladder and export its in-hand model.

.. warning::

   The shipped ``handheld_air_bladder`` model and textures are now authored by
   hand, not by this script.  Running it rewrites
   ``models/item/handheld_air_bladder_held.json``, ``.../handheld_air_bladder.json``'s
   texture sheet and the inventory sprite at the paths below, so treat it as a
   reference for the original generated pass -- or delete it -- rather than as a
   step in the current pipeline.

The item is a pale, slightly brown, translucent air membrane: a smooth sack
clamped onto a brass neck, with a brass screw cap and a wrought iron bail
handle.  Both shipped assets come out of this one file, so the sprite and the
model cannot drift apart:

* ``textures/item/handheld_air_bladder_model.png`` -- the model atlas.  Every
  element face owns a rectangle of the block-style unwrap, and the rectangle is
  painted by evaluating a procedural material at the model-space point each
  texel maps to.  The lighting is *baked*, which is not optional here: the item
  renderer shades models with the world light level only and never applies the
  per-face directional shade a block would get.
* The membrane carries real alpha (opaque where the sack gathers at the base,
  clearly translucent towards the neck).  A plain item is drawn with
  ``Sheets.translucentCullBlockSheet()``, so those texels blend with the world
  behind them instead of cutting out.  The iron and brass never do: they are
  opaque by contract, and the atlas pads every part from its own texels so
  mipmapping cannot average a translucent membrane texel into the handle.
* Nothing uses value noise.  A bladder is a smooth, taut surface, so the only
  structure in the material is the shape of the sack, a warm subsurface term on
  the side turned away from the key light, and one soft pucker into the cinch.
* ``models/item/handheld_air_bladder_held.json`` -- the element model, with the
  atlas rectangles written out as per-face UVs.  The mapping follows vanilla's
  own auto-UV table (``FaceInfo`` vertex order + ``BlockFaceUV``), so the paint
  pass and the game agree on which texel lands on which corner.  Model-JSON UVs
  live in a 0..16 space that is *relative to the whole texture*, so the writer
  rescales the pixel rectangles by ``16 / atlas_size``.
* ``textures/item/handheld_air_bladder.png`` -- the 16x16 inventory sprite.  It
  is drawn, not photographed: a front elevation of the model above, resolved by
  a real depth test against the model's own boxes and its radius profile, shaded
  from a sphere normal, snapped to six flat tones, and finished with the house
  edge darkening and a one pixel contact shadow.  Filtering a render down to
  sixteen pixels turned the sack into a smudge, which is why this step rasterises
  once per pixel instead.

Usage
-----
    python3 scripts/GenerateHandheldAirBladderAssets.py             # write assets
    python3 scripts/GenerateHandheldAirBladderAssets.py --preview   # + PNG previews
"""

from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut"
ICON_PATH = ASSETS / "textures" / "item" / "handheld_air_bladder.png"
ATLAS_PATH = ASSETS / "textures" / "item" / "handheld_air_bladder_model.png"
HELD_MODEL_PATH = ASSETS / "models" / "item" / "handheld_air_bladder_held.json"
PREVIEW_DIR = REPO_ROOT / "build" / "preview"

MODEL_TEXTURE_REF = "aquanaut:item/handheld_air_bladder_model"
ICON_TEXTURE_REF = "aquanaut:item/handheld_air_bladder"

ICON_SIZE = 16
ATLAS_SIZE = (128, 128)
ATLAS_PAD = 1

# Camera: from the model towards the viewer.  Front right, above.
CAM_DIR = (-0.60, 0.50, -0.92)
CAM_DISTANCE = 27.0

# ---------------------------------------------------------------------------
# colour ramps
# ---------------------------------------------------------------------------

# The bladder is a pale, almost white membrane with only a breath of brown in
# the shadows -- the way a real swim bladder reads in daylight.
MEMBRANE_STOPS = (
    (0.00, (86, 76, 64)),
    (0.26, (148, 138, 122)),
    (0.50, (206, 197, 180)),
    (0.72, (232, 226, 212)),
    (1.00, (250, 248, 242)),
)
# Subsurface tint: light that has travelled through the membrane comes out warm.
MEMBRANE_GLOW = (255, 226, 190)
BRASS_STOPS = (
    (0.00, (52, 34, 14)),
    (0.32, (118, 84, 34)),
    (0.68, (178, 138, 62)),
    (1.00, (238, 212, 140)),
)
# Wrought iron: cool grey, dark in the hollows, near white on a polished edge.
IRON_STOPS = (
    (0.00, (26, 29, 34)),
    (0.26, (66, 72, 80)),
    (0.52, (118, 125, 134)),
    (0.76, (176, 183, 192)),
    (1.00, (240, 245, 252)),
)
OUTLINE = (26, 14, 8)


def ramp(stops, t: float):
    t = min(1.0, max(0.0, t))
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i + 1]
        if t <= t1:
            k = 0.0 if t1 <= t0 else (t - t0) / (t1 - t0)
            return tuple(int(round(c0[j] + (c1[j] - c0[j]) * k)) for j in range(3))
    return stops[-1][1]


def scale_rgb(rgb, factor: float):
    return tuple(min(255, max(0, int(round(channel * factor)))) for channel in rgb)


def mix_rgb(a, b, k: float):
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3))


def clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return low if value < low else high if value > high else value


# ---------------------------------------------------------------------------
# vector helpers
# ---------------------------------------------------------------------------


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def mul(a, s: float):
    return (a[0] * s, a[1] * s, a[2] * s)


def dot(a, b) -> float:
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def norm(a):
    length = math.sqrt(dot(a, a))
    return a if length == 0 else mul(a, 1.0 / length)


def rotate_axis(v, axis: str, degrees: float):
    angle = math.radians(degrees)
    c, s = math.cos(angle), math.sin(angle)
    x, y, z = v
    if axis == "x":
        return (x, y * c - z * s, y * s + z * c)
    if axis == "y":
        return (x * c + z * s, y, -x * s + z * c)
    return (x * c - y * s, x * s + y * c, z)


# key light: up and to the left *of the camera*, so the render reads as a
# normal product shot no matter which side the camera sits on.
_CAM_UNIT = norm(CAM_DIR)
_FORWARD = mul(_CAM_UNIT, -1.0)
_RIGHT = norm(cross(_FORWARD, (0.0, 1.0, 0.0)))
_UP = cross(_RIGHT, _FORWARD)
LIGHT_DIR = norm(add(add(mul(_RIGHT, -0.42), mul(_UP, 0.62)), mul(_FORWARD, -0.66)))


# ---------------------------------------------------------------------------
# the model
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Box:
    name: str
    material: str
    lo: tuple
    hi: tuple
    faces: tuple = ("north", "south", "west", "east", "up", "down")
    rotation: tuple | None = None  # (axis, degrees, origin)
    smooth: bool = False  # shade from the implied sack surface, not the box

    @property
    def size(self):
        return (self.hi[0] - self.lo[0], self.hi[1] - self.lo[1], self.hi[2] - self.lo[2])


FACE_NORMAL = {
    "north": (0, 0, -1),
    "south": (0, 0, 1),
    "west": (-1, 0, 0),
    "east": (1, 0, 0),
    "up": (0, 1, 0),
    "down": (0, -1, 0),
}

# (u axis, u sign, v axis, v sign) -- vanilla's auto-UV orientation, so that a
# face's plain unwrap rectangle already reads correctly from outside the model.
FACE_AXES = {
    "north": ("x", -1.0, "y", -1.0),
    "south": ("x", 1.0, "y", -1.0),
    "west": ("z", 1.0, "y", -1.0),
    "east": ("z", -1.0, "y", -1.0),
    "up": ("x", 1.0, "z", 1.0),
    "down": ("x", 1.0, "z", -1.0),
}

# FaceInfo vertex order, as (x, y, z) min/max corner selectors.
FACE_VERTS = {
    "down": ((0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)),
    "up": ((0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)),
    "north": ((1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)),
    "south": ((0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)),
    "west": ((0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)),
    "east": ((1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)),
}

AXIS_INDEX = {"x": 0, "y": 1, "z": 2}


def integer_dims(box):
    """Whole-texel footprint of a box.  Coordinates are authored on a 0.1 grid,
    but an unwrap rectangle has to be an integer number of texels, so the
    painting and the UVs both work from this rounded size."""
    dx, dy, dz = box.size
    return (max(1, int(round(dx))), max(1, int(round(dy))), max(1, int(round(dz))))


def face_dims(dims, face):
    dx, dy, dz = dims
    if face in ("north", "south"):
        return dx, dy
    if face in ("west", "east"):
        return dz, dy
    return dx, dz


def unwrap_cells(origin, dims):
    """Block-style unwrap: (x0, y0, w, h) per face, in atlas pixels."""
    dx, dy, dz = dims
    ox, oy = origin[0], origin[1]
    return {
        "up": (ox + dz, oy, dx, dz),
        "down": (ox + dz + dx, oy, dx, dz),
        "east": (ox, oy + dz, dz, dy),
        "north": (ox + dz, oy + dz, dx, dy),
        "west": (ox + dz + dx, oy + dz, dz, dy),
        "south": (ox + dz + dx + dz, oy + dz, dx, dy),
    }


# The sack is a surface of revolution.  Stacked boxes can only approximate it,
# so the layer table below is also the radius profile the shader-style light
# uses: every body texel is lit by the *smooth* surface normal at its position
# instead of its flat box normal.  Without that the ledges of the stack light up
# and the sack reads as a ziggurat.
SACK_CENTER = 8.0
SACK_Z_SCALE = 0.95
SACK_PROFILE = (
    (2.0, 3.10),
    (2.9, 3.95),
    (3.8, 4.50),
    (4.8, 4.85),
    (5.9, 5.00),
    (7.1, 4.95),
    (8.4, 4.75),
    (9.7, 4.45),
    (10.9, 4.10),
    (12.0, 3.65),
    (12.9, 3.15),
    (13.6, 2.65),
    (14.1, 2.20),
)
SACK_TOP = SACK_PROFILE[-1][0]
# A narrow, clearly separate neck is what makes the sprite read as a vessel
# rather than as an egg with a cap on it.
NECK_LO, NECK_HI = 14.1, 15.9
NECK_HALF = 1.15

LAYER_STEP = 0.45

# Minecraft item models are read as if the object filled 0..16 and were centred
# on the item origin.  The authored stack spans y 2..18.9, so the exporter drops
# it by this much; the sprite is unaffected because the renderer frames the
# model from its own bounds.
MODEL_Y_OFFSET = -2.4


def sack_layer_bounds():
    bounds = []
    y = SACK_PROFILE[0][0]
    while y < SACK_TOP - 1e-6:
        y1 = min(SACK_TOP, y + LAYER_STEP)
        bounds.append((y, y1))
        y = y1
    return bounds


def build_boxes():
    body = []
    for index, (y0, y1) in enumerate(sack_layer_bounds()):
        half = profile_radius((y0 + y1) / 2.0)
        depth = half * SACK_Z_SCALE
        # layers overlap by a hair so no two faces are coplanar
        box = Box(
            f"sack_{index:02d}",
            "membrane_deep" if index == 0 else "membrane",
            (SACK_CENTER - half, y0 - 0.04, SACK_CENTER - depth),
            (SACK_CENTER + half, y1 + 0.04, SACK_CENTER + depth),
            smooth=True)
        body.append(box)
    body.append(Box(
        "neck", "membrane",
        (SACK_CENTER - NECK_HALF, NECK_LO, SACK_CENTER - NECK_HALF * SACK_Z_SCALE),
        (SACK_CENTER + NECK_HALF, NECK_HI, SACK_CENTER + NECK_HALF * SACK_Z_SCALE),
        smooth=True))
    # Brass fittings on top: an iron clamp holding the membrane onto the neck, a
    # collar, and a screw cap with a tommy bar.  A wide handwheel looked like a
    # hammer at sprite scale, so the valve is a cap you turn.
    fittings = [
        Box("neck_clamp", "iron", (6.5, 15.25, 6.6), (9.5, 15.95, 9.4)),
        Box("collar", "brass", (6.3, 15.85, 6.45), (9.7, 16.8, 9.55)),
        Box("cap", "brass", (6.85, 16.7, 6.95), (9.15, 17.7, 9.05)),
        Box("cap_bar", "brass_bright", (6.55, 17.6, 6.7), (9.45, 18.05, 9.3)),
    ]
    # The handle is a wrought iron bail: two arms on iron lugs either side of the
    # grip, set well clear of the sack so the opening reads at sprite scale.
    handle = [
        Box("lug_low", "iron", (2.85, 6.00, 6.40), (4.30, 8.00, 9.60)),
        Box("lug_high", "iron", (3.85, 9.70, 6.40), (5.30, 11.70, 9.60)),
        Box("arm_low", "iron", (0.85, 6.55, 6.85), (3.70, 7.55, 9.15)),
        Box("arm_high", "iron", (0.85, 10.15, 6.85), (4.70, 11.15, 9.15)),
        Box("grip", "iron", (0.30, 6.45, 6.80), (1.50, 11.25, 9.20)),
        Box("grip_wrap", "brass", (0.20, 8.40, 6.70), (1.60, 9.20, 9.30)),
        Box("rivet_low", "iron", (3.05, 6.55, 6.00), (3.65, 7.25, 6.45)),
        Box("rivet_high", "iron", (3.65, 10.25, 6.00), (4.25, 10.95, 6.45)),
    ]
    return body + fittings + handle


# ---------------------------------------------------------------------------
# procedural materials (baked light, subsurface tint, contact shadow)
# ---------------------------------------------------------------------------


def _diffuse(normal) -> float:
    return max(0.0, dot(normal, LIGHT_DIR))


def profile_radius(y: float) -> float:
    points = SACK_PROFILE
    if y <= points[0][0]:
        return points[0][1]
    if y >= points[-1][0]:
        return points[-1][1]
    for index in range(len(points) - 1):
        y0, r0 = points[index]
        y1, r1 = points[index + 1]
        if y <= y1:
            return r0 + (r1 - r0) * (y - y0) / (y1 - y0)
    return points[-1][1]


def profile_slope(y: float, h: float = 0.12) -> float:
    return (profile_radius(y + h) - profile_radius(y - h)) / (2 * h)


def sack_normal(point, flat):
    """Outward normal of the implied surface of revolution."""
    dx = point[0] - SACK_CENTER
    dz = (point[2] - SACK_CENTER) / SACK_Z_SCALE
    rho = math.hypot(dx, dz)
    if rho < 1e-6:
        radial = (0.0, 0.0)
    else:
        radial = (dx / rho, dz / rho)
    normal = (radial[0], -profile_slope(min(point[1], SACK_TOP)), radial[1] / SACK_Z_SCALE)
    if dot(normal, normal) < 1e-9:
        return flat
    return norm(normal)


def neck_normal(point, flat):
    dx = point[0] - SACK_CENTER
    dz = (point[2] - SACK_CENTER) / SACK_Z_SCALE
    if abs(dx) + abs(dz) < 1e-6:
        return flat
    return norm((dx, 0.0, dz / SACK_Z_SCALE))


def inside(box, point, pad: float = 0.0) -> bool:
    return (box.lo[0] - pad <= point[0] <= box.hi[0] + pad
            and box.lo[1] - pad <= point[1] <= box.hi[1] + pad
            and box.lo[2] - pad <= point[2] <= box.hi[2] + pad)


def occlusion(point, normal, sources, strength: float) -> float:
    """Cheap ambient occlusion: march a few samples along the normal and see
    what geometry they run into.  This is what puts contact shadows where the
    handle, collar and valve meet the sack."""
    if not sources or strength <= 0.0:
        return 1.0
    steps = (0.35, 0.75, 1.15, 1.6)
    hits = 0
    for distance in steps:
        sample = add(point, mul(normal, distance))
        for other in sources:
            if inside(other, sample):
                hits += 1
                break
    return 1.0 - strength * (hits / len(steps))


def membrane_color(box, face, point, normal, deep: bool = False) -> tuple:
    """The sack: smooth lambert form, a warm subsurface term where the light
    comes through, and a soft pucker where the membrane is cinched onto the
    neck.  Deliberately free of grain: a real bladder is smooth, so the only
    structure here is the shape."""
    p = point
    diffuse = _diffuse(normal)
    ambient = 0.26 + 0.10 * clamp((p[1] - 2.0) / 13.0)
    shade = ambient + 0.74 * diffuse

    # light transmitted through the membrane: strongest on the side turned away
    # from the key light, which is what makes thin skin look lit from within
    through = (1.0 - diffuse) ** 2
    color = ramp(MEMBRANE_STOPS, shade + 0.34 * through)
    color = mix_rgb(color, MEMBRANE_GLOW, 0.38 * through)

    # a single smooth pucker running into the cinch, plus a slack fold above the
    # base -- both broad sine bands, never noise
    if 11.0 < p[1] < 15.8:
        theta = math.atan2(p[2] - SACK_CENTER, p[0] - SACK_CENTER)
        fold = math.sin(theta * 5.0 + 0.6) ** 2
        color = scale_rgb(color, 1.0 - 0.10 * fold * clamp((p[1] - 11.0) / 3.4))
    if p[1] < 4.2:
        color = scale_rgb(color, 1.0 - 0.10 * clamp((4.2 - p[1]) / 2.4))
    if deep:
        color = scale_rgb(color, 0.90)

    # thickness: opaque where the membrane gathers at the base, clearly
    # translucent towards the neck, and always blending rather than cutting out
    alpha = 176 + 46 * clamp(1.0 - (p[1] - 2.0) / 15.0)
    return (*color, int(round(alpha)))


def brass_color(box, face, point, normal, bright: bool) -> tuple:
    p = point
    t = 0.26 + 0.74 * _diffuse(normal)
    # turned grooves on the collar and the valve body
    t -= 0.16 * (math.sin(p[1] * 6.2 + p[0] * 1.3) * 0.5 + 0.5) ** 3
    color = ramp(BRASS_STOPS, t)
    if bright:
        color = scale_rgb(color, 1.06)
    return (*color, 255)


def iron_color(box, face, point, normal) -> tuple:
    """Wrought iron: a cool, smooth grey.  The shading uses the roll of the bar
    across its narrow axis, which is what stops a flat face reading as painted
    cardboard, plus a tight sheen where the surface meets the key light."""
    diffuse = _diffuse(normal)
    axis = max(range(3), key=lambda index: abs(normal[index]))
    across_axes = [index for index in range(3) if index != axis]
    across = min(across_axes, key=lambda index: box.size[index])
    span = max(1e-3, box.size[across])
    roll = 1.0 - (2.0 * clamp((point[across] - box.lo[across]) / span) - 1.0) ** 2
    t = 0.10 + 0.58 * diffuse + 0.40 * roll + 0.26 * max(0.0, diffuse) ** 8
    return (*ramp(IRON_STOPS, t), 255)


#: Materials that must never lose opacity: the membrane blends with the world,
#: the iron and brass are solid and are drawn over it.
OPAQUE_MATERIALS = frozenset({"brass", "brass_bright", "iron"})


def material_color(box, face, point, normal) -> tuple:
    if box.material == "membrane":
        return membrane_color(box, face, point, normal)
    if box.material == "membrane_deep":
        return membrane_color(box, face, point, normal, deep=True)
    if box.material == "brass":
        return brass_color(box, face, point, normal, bright=False)
    if box.material == "brass_bright":
        return brass_color(box, face, point, normal, bright=True)
    if box.material == "iron":
        return iron_color(box, face, point, normal)
    raise SystemExit(f"unknown material {box.material!r}")


# ---------------------------------------------------------------------------
# atlas packing and painting
# ---------------------------------------------------------------------------


def pack_atlas(boxes):
    entries = []
    for box in boxes:
        dx, dy, dz = integer_dims(box)
        width = 2 * (dx + dz)
        height = dz + dy
        entries.append((box, width, height))
    entries.sort(key=lambda entry: (-entry[2], -entry[1]))

    limit_w, limit_h = ATLAS_SIZE
    regions = {}
    x = y = shelf = 0
    for box, width, height in entries:
        if x + width > limit_w:
            x = 0
            y += shelf + ATLAS_PAD
            shelf = 0
        if y + height > limit_h:
            raise SystemExit(
                f"model atlas overflow: {box.name} needs {width}x{height} and the "
                f"{limit_w}x{limit_h} sheet is full -- raise ATLAS_SIZE")
        regions[box.name] = (x, y, width, height)
        x += width + ATLAS_PAD
        shelf = max(shelf, height)
    return regions


def face_point(box, face, fu, fv):
    """Model-space point for the centre of a texel, addressed by its fraction
    across the face."""
    u_axis, u_sign, v_axis, v_sign = FACE_AXES[face]
    ui, vi = AXIS_INDEX[u_axis], AXIS_INDEX[v_axis]
    lo_u, hi_u = box.lo[ui], box.hi[ui]
    lo_v, hi_v = box.lo[vi], box.hi[vi]
    u = hi_u - fu * (hi_u - lo_u) if u_sign < 0 else lo_u + fu * (hi_u - lo_u)
    v = hi_v - fv * (hi_v - lo_v) if v_sign < 0 else lo_v + fv * (hi_v - lo_v)
    point = [0.0, 0.0, 0.0]
    point[ui] = u
    point[vi] = v
    rest = 3 - ui - vi
    point[rest] = box.lo[rest] if FACE_NORMAL[face][rest] < 0 else box.hi[rest]
    return tuple(point)


def paint_atlas(boxes, regions) -> Image.Image:
    image = Image.new("RGBA", ATLAS_SIZE, (0, 0, 0, 0))
    pixels = image.load()
    sack = {box.name for box in boxes if box.smooth}

    for box in boxes:
        dims = integer_dims(box)
        cells = unwrap_cells(regions[box.name], dims)
        if box.name in sack:
            # the sack is one continuous surface: only the hardware bolted onto
            # it may cast contact shadows, never the next layer of the stack
            sources = [other for other in boxes if other.name not in sack]
            ao_strength = 0.34
        else:
            sources = [other for other in boxes if other is not box]
            ao_strength = 0.52

        for face in box.faces:
            cx, cy, cw, ch = cells[face]
            flat = FACE_NORMAL[face]
            for j in range(ch):
                for i in range(cw):
                    point = face_point(box, face, (i + 0.5) / cw, (j + 0.5) / ch)
                    if box.smooth:
                        normal = neck_normal(point, flat) if box.name == "neck" else sack_normal(point, flat)
                    else:
                        normal = flat
                    # unpack before shading: edge and occlusion shading must
                    # never touch alpha, or the hardware's silhouette ring comes
                    # out part transparent
                    texel = material_color(box, face, point, normal)
                    rgb, alpha = texel[:3], texel[3]
                    if box.material in OPAQUE_MATERIALS and alpha != 255:
                        raise SystemExit(f"{box.name} is hardware and must stay opaque")

                    if not box.smooth:
                        # panel edge shading: the outermost ring of a face is a
                        # shade darker, which is what makes the hardware read as
                        # separate pieces instead of painted-on rectangles
                        edge = min(i, j, cw - 1 - i, ch - 1 - j)
                        if edge == 0:
                            rgb = scale_rgb(rgb, 0.80)
                        elif edge == 1:
                            rgb = scale_rgb(rgb, 0.93)

                    rgb = scale_rgb(rgb, occlusion(point, normal, sources, ao_strength))
                    pixels[cx + i, cy + j] = (*rgb, alpha)

        # Pad the whole packed rectangle from this box's own texels before
        # moving on.  A shared nearest-neighbour fill would let a translucent
        # membrane texel end up in the iron's padding, and mipmapping would then
        # average the handle's edge towards transparent.
        fill_region(image, regions[box.name])

    bleed_atlas(image)
    return image


def fill_region(image: Image.Image, region) -> None:
    """Flood the unpainted corners of one packed rectangle from that box."""
    from collections import deque

    x0, y0, width, height = region
    pixels = image.load()
    queue = deque()
    for y in range(y0, y0 + height):
        for x in range(x0, x0 + width):
            if pixels[x, y][3] != 0:
                queue.append((x, y))
    while queue:
        x, y = queue.popleft()
        color = pixels[x, y]
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if x0 <= nx < x0 + width and y0 <= ny < y0 + height and pixels[nx, ny][3] == 0:
                pixels[nx, ny] = color
                queue.append((nx, ny))


def bleed_atlas(image: Image.Image) -> None:
    """Flood every unpainted texel with its nearest painted neighbour.

    The unwrap leaves the corners of each packed rectangle empty, and the sheet
    as a whole has unused space.  Those texels are never sampled directly, but
    the atlas is mipmapped, so a coarse level would otherwise average them in
    and fringe every face.  A breadth-first pass is O(pixels) and makes the
    sheet fully opaque."""
    from collections import deque

    pixels = image.load()
    width, height = image.size
    queue = deque()
    for y in range(height):
        for x in range(width):
            if pixels[x, y][3] != 0:
                queue.append((x, y))
    while queue:
        x, y = queue.popleft()
        color = pixels[x, y]
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < width and 0 <= ny < height and pixels[nx, ny][3] == 0:
                pixels[nx, ny] = color
                queue.append((nx, ny))


# ---------------------------------------------------------------------------
# model JSON
# ---------------------------------------------------------------------------


def uv_rect(cell):
    """Atlas pixels -> the 0..16 UV space a model JSON uses."""
    x, y, w, h = cell
    sx = 16.0 / ATLAS_SIZE[0]
    sy = 16.0 / ATLAS_SIZE[1]
    return [round(x * sx, 4), round(y * sy, 4), round((x + w) * sx, 4), round((y + h) * sy, 4)]


def write_held_model(boxes, regions) -> None:
    elements = []
    for box in boxes:
        cells = unwrap_cells(regions[box.name], integer_dims(box))
        faces = {}
        for face in box.faces:
            faces[face] = {"uv": uv_rect(cells[face]), "texture": "#bladder"}
        element = {
            "from": [round(v, 2) for v in (box.lo[0], box.lo[1] + MODEL_Y_OFFSET, box.lo[2])],
            "to": [round(v, 2) for v in (box.hi[0], box.hi[1] + MODEL_Y_OFFSET, box.hi[2])],
            "faces": faces,
        }
        # all geometry here is hand-set, so the model shades like a block: the
        # atlas already carries the light
        element["shade"] = True
        elements.append(element)

    model = {
        "textures": {
            "particle": ICON_TEXTURE_REF,
            "bladder": MODEL_TEXTURE_REF,
        },
        "gui_light": "front",
        "elements": elements,
    }
    HELD_MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    HELD_MODEL_PATH.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# renderer: soft perspective, z-buffer, baked-light textures
# ---------------------------------------------------------------------------


def transformed_corners(box):
    corners = {}
    rotation = box.rotation
    for ix in (0, 1):
        for iy in (0, 1):
            for iz in (0, 1):
                point = (
                    box.hi[0] if ix else box.lo[0],
                    box.hi[1] if iy else box.lo[1],
                    box.hi[2] if iz else box.lo[2],
                )
                if rotation is not None:
                    axis, degrees, origin = rotation
                    local = sub(point, origin)
                    if abs(degrees) == 22.5:
                        factor = 1.0 + (1.0 / math.cos(math.radians(22.5)) - 1.0)
                    else:
                        factor = math.sqrt(2.0)
                    scaled = []
                    for index, char in enumerate("xyz"):
                        if char == axis:
                            scaled.append(local[index])
                        else:
                            scaled.append(local[index] * factor)
                    point = add(rotate_axis(tuple(scaled), axis, degrees), origin)
                corners[(ix, iy, iz)] = point
    return corners


def rotate_normal(normal, rotation):
    if rotation is None:
        return normal
    axis, degrees, _ = rotation
    return rotate_axis(normal, axis, degrees)


def render(boxes, regions, atlas: Image.Image, out_size: int, supersample: int) -> Image.Image:
    frame = out_size * supersample
    corners = [corner for box in boxes for corner in transformed_corners(box).values()]
    target = tuple(sum(point[axis] for point in corners) / len(corners) for axis in range(3))
    camera = add(target, mul(norm(CAM_DIR), CAM_DISTANCE))
    forward = norm(sub(target, camera))
    right = norm(cross(forward, (0.0, 1.0, 0.0)))
    up = cross(right, forward)

    def project(point):
        rel = sub(point, camera)
        depth = dot(rel, forward)
        depth = max(1e-4, depth)
        return (dot(rel, right) / depth, dot(rel, up) / depth, depth)

    triangles = []
    corners_all = []
    for box in boxes:
        corners = transformed_corners(box)
        corners_all.extend(corners.values())
        cells = unwrap_cells(regions[box.name], integer_dims(box))
        for face in box.faces:
            normal = rotate_normal(FACE_NORMAL[face], box.rotation)
            face_center = tuple(sum(corners[c][k] for c in FACE_VERTS[face]) / 4.0 for k in range(3))
            if dot(normal, sub(camera, face_center)) <= 0.0:
                continue
            _, _, cw, ch = cells[face]
            x0, y0 = cells[face][0], cells[face][1]
            # pixel rectangles: the sampler below indexes the atlas directly
            uv = [
                (x0, y0),
                (x0, y0 + ch),
                (x0 + cw, y0 + ch),
                (x0 + cw, y0),
            ]
            points = [corners[c] for c in FACE_VERTS[face]]
            for a, b, c in ((0, 1, 2), (0, 2, 3)):
                triangles.append((points[a], points[b], points[c], uv[a], uv[b], uv[c]))

    projected = [project(point) for point in corners_all]
    min_x = min(p[0] for p in projected)
    max_x = max(p[0] for p in projected)
    min_y = min(p[1] for p in projected)
    max_y = max(p[1] for p in projected)
    scale = min(frame / max(1e-6, max_x - min_x), frame / max(1e-6, max_y - min_y))
    off_x = (min_x + max_x) / 2.0
    off_y = (min_y + max_y) / 2.0

    def to_screen(point):
        px, py, depth = project(point)
        return ((px - off_x) * scale + frame / 2.0, frame / 2.0 - (py - off_y) * scale, depth)

    depth_buffer = [1e30] * (frame * frame)
    color_buffer = [(0, 0, 0, 0)] * (frame * frame)
    atlas_pixels = atlas.load()
    atlas_w, atlas_h = atlas.size

    for tri in triangles:
        (p0, p1, p2, uv0, uv1, uv2) = tri
        (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (to_screen(p0), to_screen(p1), to_screen(p2))
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if abs(area) < 1e-9:
            continue
        inv_area = 1.0 / area
        start_x = max(0, int(math.floor(min(x0, x1, x2))))
        end_x = min(frame - 1, int(math.ceil(max(x0, x1, x2))))
        start_y = max(0, int(math.floor(min(y0, y1, y2))))
        end_y = min(frame - 1, int(math.ceil(max(y0, y1, y2))))
        if end_x < start_x or end_y < start_y:
            continue

        iz0, iz1, iz2 = 1.0 / z0, 1.0 / z1, 1.0 / z2
        u0, v0 = uv0
        u1, v1 = uv1
        u2, v2 = uv2

        for py in range(start_y, end_y + 1):
            sy = py + 0.5
            row = py * frame
            for px in range(start_x, end_x + 1):
                sx = px + 0.5
                w0 = ((x1 - sx) * (y2 - sy) - (x2 - sx) * (y1 - sy)) * inv_area
                if w0 < -1e-6:
                    continue
                w1 = ((x2 - sx) * (y0 - sy) - (x0 - sx) * (y2 - sy)) * inv_area
                if w1 < -1e-6:
                    continue
                w2 = 1.0 - w0 - w1
                if w2 < -1e-6:
                    continue
                inv_z = w0 * iz0 + w1 * iz1 + w2 * iz2
                depth = 1.0 / inv_z
                index = row + px
                if depth >= depth_buffer[index]:
                    continue
                u = (w0 * u0 * iz0 + w1 * u1 * iz1 + w2 * u2 * iz2) * depth
                v = (w0 * v0 * iz0 + w1 * v1 * iz1 + w2 * v2 * iz2) * depth
                tx = min(atlas_w - 1, max(0, int(u)))
                ty = min(atlas_h - 1, max(0, int(v)))
                depth_buffer[index] = depth
                color_buffer[index] = atlas_pixels[tx, ty]

    image = Image.new("RGBA", (frame, frame), (0, 0, 0, 0))
    image.putdata(color_buffer)
    if supersample > 1:
        image = image.resize((out_size, out_size), Image.BOX)
    return image


# ---------------------------------------------------------------------------
# the 16x16 sprite
#
# A render squeezed down to sixteen pixels turns a white sack into a grey
# smudge, so the sprite is *drawn* instead -- but drawn from the model: every
# pixel is decided by the model's own boxes, radius profile and light, then
# snapped to a handful of flat tones.  It is a front elevation of the same
# object, with a real depth test, not a filtered photograph of it.
# ---------------------------------------------------------------------------

# key light for the sprite: up and to the left of the viewer, out of the screen
ICON_LIGHT = norm((-0.55, -0.60, 0.58))
ICON_TONES = 6
ICON_MARGIN = 0.88


def icon_mapping(boxes):
    """Fit the model's bounds into the slot, so the sprite cannot drift out of
    frame when a part moves."""
    x_min = min(min(box.lo[0], box.hi[0]) for box in boxes)
    x_max = max(max(box.lo[0], box.hi[0]) for box in boxes)
    y_min = min(min(box.lo[1], box.hi[1]) for box in boxes)
    y_max = max(max(box.lo[1], box.hi[1]) for box in boxes)
    ppu = min(ICON_SIZE / (x_max - x_min), ICON_SIZE / (y_max - y_min)) * ICON_MARGIN
    off_x = (ICON_SIZE - (x_max - x_min) * ppu) / 2.0
    off_y = (ICON_SIZE - (y_max - y_min) * ppu) / 2.0
    return ppu, off_x, off_y, x_min, y_max


def icon_model_point(i: int, j: int, mapping):
    """Model-space point under the centre of sprite pixel (i, j)."""
    ppu, off_x, off_y, x_min, y_max = mapping
    x = (i + 0.5 - off_x) / ppu + x_min
    y = y_max - (j + 0.5 - off_y) / ppu
    return x, y


def _band(value: float) -> float:
    """Six flat steps: pixel art bands, not a gradient."""
    value = clamp(value)
    return round(value * (ICON_TONES - 1)) / (ICON_TONES - 1)


def membrane_icon_pixel(x: float, y: float) -> tuple:
    """Shade one membrane pixel off a sphere normal, add the warm light that
    comes through the thin skin, and return an opaque texel."""
    nx = (x - SACK_CENTER) / 5.0
    ny = (y - 8.05) / 6.05
    rho = math.hypot(nx, ny)
    front = math.sqrt(max(0.0, 1.0 - min(1.0, rho) ** 2))
    diffuse = max(0.0, dot(norm((nx, -ny, front)), ICON_LIGHT))
    color = ramp(MEMBRANE_STOPS, _band(0.20 + 0.80 * diffuse))
    # thin skin at the rim and on the side turned away glows through
    through = clamp(0.60 * (1.0 - diffuse) ** 2 + 0.40 * clamp((rho - 0.70) / 0.30))
    color = mix_rgb(color, MEMBRANE_GLOW, 0.45 * through)
    return (*color, 255)


def metal_icon_pixel(x: float, y: float, stops, gain: float) -> tuple:
    """Iron and brass share one screen-space key light, so the handle and the
    fittings agree on where it is.  Hardware is always opaque."""
    tone = 0.52 - 0.30 * (x - SACK_CENTER) / 6.0 + 0.22 * (y - SACK_CENTER) / 7.0
    color = ramp(stops, 0.16 + 0.80 * _band(tone))
    return (*scale_rgb(color, gain), 255)


def icon_pixel(x: float, y: float, boxes) -> tuple | None:
    """Front elevation: whatever is nearest the viewer wins."""
    material = None
    front_z = None

    # the sack and neck are surfaces of revolution, so use the profile rather
    # than the stack of boxes that approximates it
    if 2.0 <= y <= SACK_TOP and abs(x - SACK_CENTER) <= profile_radius(y):
        material, front_z = "membrane", SACK_CENTER - profile_radius(y) * SACK_Z_SCALE
    elif NECK_LO <= y <= NECK_HI and abs(x - SACK_CENTER) <= NECK_HALF:
        material, front_z = "membrane", SACK_CENTER - NECK_HALF * SACK_Z_SCALE

    for box in boxes:
        if box.material in ("membrane", "membrane_deep"):
            continue
        if not (box.lo[0] <= x <= box.hi[0] and box.lo[1] <= y <= box.hi[1]):
            continue
        if front_z is None or box.lo[2] < front_z:
            material, front_z = box.material, box.lo[2]

    if material is None:
        return None
    if material in ("membrane", "membrane_deep"):
        return membrane_icon_pixel(x, y)
    if material == "iron":
        return metal_icon_pixel(x, y, IRON_STOPS, 1.0)
    if material == "brass":
        return metal_icon_pixel(x, y, BRASS_STOPS, 1.0)
    if material == "brass_bright":
        return metal_icon_pixel(x, y, BRASS_STOPS, 1.08)
    raise SystemExit(f"unhandled sprite material {material!r}")


def draw_icon(boxes) -> Image.Image:
    icon = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    pixels = icon.load()
    mapping = icon_mapping(boxes)
    for j in range(ICON_SIZE):
        for i in range(ICON_SIZE):
            color = icon_pixel(*icon_model_point(i, j, mapping), boxes)
            if color is not None:
                pixels[i, j] = color

    opaque = {(x, y) for y in range(ICON_SIZE) for x in range(ICON_SIZE) if pixels[x, y][3] > 0}

    # contact shadow one pixel down and to the right, opposite the key light
    shadow = (18, 18, 24, 76)
    for x, y in sorted(opaque):
        for nx, ny in ((x + 1, y), (x, y + 1), (x + 1, y + 1)):
            if 0 <= nx < ICON_SIZE and 0 <= ny < ICON_SIZE and pixels[nx, ny][3] == 0:
                pixels[nx, ny] = shadow

    # house silhouette shading: the outer ring keeps the material colour but a
    # good deal darker, so the sprite holds together on any slot background
    for x, y in sorted(opaque):
        if any(neighbour not in opaque for neighbour in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))):
            pixels[x, y] = scale_rgb(pixels[x, y][:3], 0.58) + (255,)
    return icon


# ---------------------------------------------------------------------------
# previews
# ---------------------------------------------------------------------------


def checkerboard(size, square=8):
    board = Image.new("RGBA", size, (58, 58, 66, 255))
    pixels = board.load()
    for y in range(size[1]):
        for x in range(size[0]):
            if ((x // square) + (y // square)) % 2:
                pixels[x, y] = (74, 74, 84, 255)
    return board


def write_previews(held: Image.Image, atlas: Image.Image, icon: Image.Image) -> None:
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    backdrop = Image.new("RGBA", held.size, (36, 52, 66, 255))
    backdrop.alpha_composite(held)
    backdrop.save(PREVIEW_DIR / "handheld_air_bladder_held.png")

    atlas_zoom = atlas.resize((atlas.width * 4, atlas.height * 4), Image.NEAREST)
    atlas_zoom.save(PREVIEW_DIR / "handheld_air_bladder_atlas.png")

    zoom = 16
    board = checkerboard((icon.width * zoom, icon.height * zoom))
    board.alpha_composite(icon.resize((icon.width * zoom, icon.height * zoom), Image.NEAREST))
    board.save(PREVIEW_DIR / "handheld_air_bladder_icon.png")
    print(f"previews in {PREVIEW_DIR.relative_to(REPO_ROOT)}")


# ---------------------------------------------------------------------------


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--preview", action="store_true", help="also write PNG previews into build/preview")
    args = parser.parse_args(argv)

    boxes = build_boxes()
    regions = pack_atlas(boxes)

    atlas = paint_atlas(boxes, regions)
    ATLAS_PATH.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(ATLAS_PATH)

    write_held_model(boxes, regions)

    icon = draw_icon(boxes)
    icon.save(ICON_PATH)

    alpha_histogram = icon.getchannel("A").histogram()
    opaque = sum(alpha_histogram[1:])
    print(f"wrote {ATLAS_PATH.relative_to(REPO_ROOT)} ({atlas.width}x{atlas.height})")
    print(f"wrote {HELD_MODEL_PATH.relative_to(REPO_ROOT)} ({len(boxes)} elements)")
    print(f"wrote {ICON_PATH.relative_to(REPO_ROOT)} ({opaque} opaque pixels)")

    if args.preview:
        held_preview = render(boxes, regions, atlas, 256, 2)
        write_previews(held_preview, atlas, icon)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
