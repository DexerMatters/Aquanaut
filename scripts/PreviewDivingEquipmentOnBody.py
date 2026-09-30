#!/usr/bin/env python3
"""Preview the worn diving equipment on a player model.

The game renderer (DivingEquipmentRenderLayer) wraps real box geometry around
the vanilla player model parts. This script rasterises the same boxes with the
generated textures onto a simple stand-in player so the worn look can be checked
without launching Minecraft.

    python3 scripts/PreviewDivingEquipmentOnBody.py

Writes previews to .scratch/equipment_preview/ .
"""

import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import diving_equipment_layout as layout  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
TEX_DIR = os.path.normpath(os.path.join(HERE, "..", "src", "main", "resources",
                                        "assets", "aquanaut", "textures", "equipment"))
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", ".scratch", "equipment_preview"))

PX_PER_UNIT = 5  # render resolution
LIGHT = (0.35, -0.80, 0.48)


# ---------------------------------------------------------------------------
# Geometry
# ---------------------------------------------------------------------------

# The face corner/UV convention MUST match DivingEquipmentRenderLayer.java.
def face_quad(box, face, mirrored=False):
    (x0, y0, z0), (x1, y1, z1) = box.corners(mirrored)
    u0, v0, fw, fh = box.face_rects()[face]
    atlas = layout.ATLAS
    us = 1.0 / atlas
    uvs = [(u0 * us, v0 * us), ((u0 + fw) * us, v0 * us),
           ((u0 + fw) * us, (v0 + fh) * us), (u0 * us, (v0 + fh) * us)]
    pts = {
        "+z": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "-z": [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)],
        "+x": [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)],
        "-x": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
        "top": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "bottom": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
    }[face]
    normal = {"+z": (0, 0, 1), "-z": (0, 0, -1), "+x": (1, 0, 0),
              "-x": (-1, 0, 0), "top": (0, -1, 0), "bottom": (0, 1, 0)}[face]
    return pts, us and uvs, normal


class RefBox:
    """A solid-coloured reference box (the player body)."""

    def __init__(self, pos, size, color):
        self.pos = pos
        self.size = size
        self.color = color

    def corners(self, mirrored=False):
        x0, y0, z0 = self.pos
        x1, y1, z1 = x0 + self.size[0], y0 + self.size[1], z0 + self.size[2]
        return (x0, y0, z0), (x1, y1, z1)


def ref_quads(box):
    (x0, y0, z0), (x1, y1, z1) = box.corners()
    pts = {
        "+z": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "-z": [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)],
        "+x": [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)],
        "-x": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
        "top": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "bottom": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
    }
    normals = {"+z": (0, 0, 1), "-z": (0, 0, -1), "+x": (1, 0, 0),
               "-x": (-1, 0, 0), "top": (0, -1, 0), "bottom": (0, 1, 0)}
    return [(p, None, normals[f]) for f, p in pts.items()]


def offset(quads, dx, dy, dz):
    out = []
    for pts, uvs, n in quads:
        out.append(([(x + dx, y + dy, z + dz) for x, y, z in pts], uvs, n))
    return out


def flipper_quads(img, leg_x):
    """Flipper boxes on one leg: leg pivot is at (leg_x, 12, 0)."""
    quads = []
    for box in layout.FLIPPER_BOXES:
        for face in layout.FACES:
            pts, uvs, n = face_quad(box, face)
            quads.append((pts, uvs, n, img))
    return offset_textured(quads, leg_x, 12, 0)


def offset_textured(quads, dx, dy, dz):
    out = []
    for pts, uvs, n, img in quads:
        out.append(([(x + dx, y + dy, z + dz) for x, y, z in pts], uvs, n, img))
    return out


def textured_quads(item_id, dx=0.0, dy=0.0, dz=0.0):
    category, _ = layout.EQUIPMENT[item_id]
    img = Image.open(os.path.join(TEX_DIR, item_id + "_on_body.png")).convert("RGBA")
    quads = []
    boxes = layout.CATEGORY_BOXES[category]
    for box in boxes:
        instances = [False] + ([True] if box.mirror_x else [])
        for mirrored in instances:
            for face in layout.FACES:
                pts, uvs, n = face_quad(box, face, mirrored=mirrored)
                quads.append((pts, uvs, n, img))
    return offset_textured(quads, dx, dy, dz)


# ---------------------------------------------------------------------------
# Rasteriser
# ---------------------------------------------------------------------------

def rotate(point, yaw, pitch):
    x, y, z = point
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy - z * sy, x * sy + z * cy
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


def render(quads, yaw, pitch, width, height, scale, center, bg=(28, 34, 44, 255)):
    """Orthographic render with a z-buffer; quads = (pts, uvs|None, normal[, img])."""
    img = Image.new("RGBA", (width, height), bg)
    pixels = img.load()
    depth = [[-1e9] * width for _ in range(height)]
    cy, sy = math.cos(yaw), math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)

    def project(p):
        x, y, z = p[0] - center[0], p[1] - center[1], p[2] - center[2]
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return (width / 2 + x * scale, height / 2 + y * scale, z)

    light = LIGHT
    ln = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ln for c in light)

    items = []
    for quad in quads:
        pts, uvs, normal = quad[0], quad[1], quad[2]
        tex = quad[3] if len(quad) > 3 else None
        n = rotate(normal, yaw, pitch)
        shade = 0.42 + 0.58 * max(0.0, n[0] * light[0] + n[1] * light[1] + n[2] * light[2])
        screen = [project(p) for p in pts]
        items.append((screen, uvs, tex, shade, normal))

    for screen, uvs, tex, shade, normal in items:
        for tri in ((0, 1, 2), (0, 2, 3)):
            draw_triangle(pixels, depth, [screen[i] for i in tri],
                          [uvs[i] for i in tri] if uvs else None, tex, shade)
    return img


def draw_triangle(pixels, depth, tri, uvs, tex, shade):
    (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = tri
    area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
    if abs(area) < 1e-9:
        return
    min_x = max(0, int(math.floor(min(x0, x1, x2))))
    max_x = min(len(depth[0]) - 1, int(math.ceil(max(x0, x1, x2))))
    min_y = max(0, int(math.floor(min(y0, y1, y2))))
    max_y = min(len(depth) - 1, int(math.ceil(max(y0, y1, y2))))
    tex_img = tex.load() if tex is not None else None
    tw, th = tex.size if tex is not None else (1, 1)
    for py in range(min_y, max_y + 1):
        for px in range(min_x, max_x + 1):
            w0 = (x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)
            w1 = (x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)
            w2 = (x0 - px) * (y1 - py) - (x1 - px) * (y0 - py)
            if area < 0:
                w0, w1, w2, sign = -w0, -w1, -w2, -1
            else:
                sign = 1
            if (w0 < 0 or w1 < 0 or w2 < 0) and sign > 0:
                continue
            if (w0 > 0 or w1 > 0 or w2 > 0) and sign < 0:
                continue
            b0, b1, b2 = w0 / area, w1 / area, w2 / area
            b0, b1, b2 = abs(b0), abs(b1), abs(b2)
            total = b0 + b1 + b2
            b0, b1, b2 = b0 / total, b1 / total, b2 / total
            z = b0 * z0 + b1 * z1 + b2 * z2
            if z <= depth[py][px]:
                continue
            if tex_img is not None:
                u = b0 * uvs[0][0] + b1 * uvs[1][0] + b2 * uvs[2][0]
                v = b0 * uvs[0][1] + b1 * uvs[1][1] + b2 * uvs[2][1]
                tx = min(tw - 1, max(0, int(u * tw)))
                ty = min(th - 1, max(0, int(v * th)))
                r, g, bl, a = tex_img[tx, ty]
                if a < 128:
                    continue
                color = (int(r * shade), int(g * shade), int(bl * shade), 255)
            else:
                color = None
            depth[py][px] = z
            if color is not None:
                pixels[px, py] = color
            else:
                pixels[px, py] = shade_flat(pixels[px, py], shade)


def shade_flat(existing, shade):
    return existing


# ---------------------------------------------------------------------------
# Scenes
# ---------------------------------------------------------------------------

def player_quads():
    """A plain Steve-shaped stand-in so the gear can be judged on a body."""
    quads = []
    quads += [(p, None, n, (188, 152, 118)) for p, uvs, n in ref_quads(
        RefBox((-4, -8, -4), (8, 8, 8), None))]                     # head
    quads += [(p, None, n, (54, 128, 140)) for p, uvs, n in ref_quads(
        RefBox((-4, 0, -2), (8, 12, 4), None))]                     # body
    for leg_x in (-3.9, 1.9):
        quads += [(p, None, n, (52, 58, 140)) for p, uvs, n in ref_quads(
            RefBox((leg_x, 12, -2), (4, 12, 4), None))]
    for arm_x in (-8.0, 4.0):
        quads += [(p, None, n, (178, 140, 108)) for p, uvs, n in ref_quads(
            RefBox((arm_x, 0, -2), (4, 12, 4), None))]
    return quads


def outfit_quads(items):
    quads = list(player_quads())
    for item_id in items:
        category, _ = layout.EQUIPMENT[item_id]
        if category == "flipper":
            for leg_x in (-3.9, 1.9):
                quads += flipper_quads(load(item_id), leg_x)
        else:
            quads += textured_quads(item_id)
    return quads


def load(item_id):
    return Image.open(os.path.join(TEX_DIR, item_id + "_on_body.png")).convert("RGBA")


def shaded_player(quads):
    out = []
    for entry in quads:
        pts, uvs, normal, img = entry
        if isinstance(img, tuple):
            color = img
            ln = math.sqrt(sum(c * c for c in LIGHT))
            light = tuple(c / ln for c in LIGHT)
            shade = 0.45 + 0.55 * max(0.0, normal[0] * light[0] + normal[1] * light[1]
                                              + normal[2] * light[2])
            flat = Image.new("RGBA", (1, 1), (int(color[0] * shade), int(color[1] * shade),
                                              int(color[2] * shade), 255))
            out.append((pts, ((0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)), normal, flat))
        else:
            out.append(entry)
    return out


def shot(name, quads, yaw_deg, pitch_deg, center=(0, 8, 0), px_per_unit=PX_PER_UNIT,
         span=32):
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    width = int(span * px_per_unit * 2)
    height = int(span * px_per_unit * 2.2)
    img = render(shaded_player(quads), yaw, pitch, width, height, px_per_unit,
                 (center[0], center[1], center[2]))
    path = os.path.join(OUT_DIR, name + ".png")
    img.save(path)
    return path


def atlas_sheet(name, item_id):
    img = load(item_id)
    big = img.resize((img.width * 6, img.height * 6), Image.NEAREST)
    path = os.path.join(OUT_DIR, name + ".png")
    big.save(path)
    return path


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    written = []
    outfit = ["marine_alloy_mask", "iron_oxygen_tank", "marine_alloy_flippers"]
    written.append(shot("outfit_front", outfit_quads(outfit), 0, -6))
    written.append(shot("outfit_three_quarter", outfit_quads(outfit), 35, -6))
    written.append(shot("outfit_side", outfit_quads(outfit), 90, -4))
    written.append(shot("outfit_back", outfit_quads(outfit), 180, -6))

    heads = ["iron_mask", "coral_mask", "shell_mask", "hard_shell_mask",
             "marine_alloy_mask", "anglerfish_mask", "ender_mask", "slime_mask"]
    for item_id in heads:
        quads = [(p, u, n, i) for p, u, n, i in shaded_player(player_quads())
                 if not (isinstance(i, tuple) and i == (54, 128, 140))]
        quads += textured_quads(item_id)
        written.append(shot("close_" + item_id, quads, 20, -10, center=(0, -4.5, 0),
                            px_per_unit=13, span=11))

    tanks = ["iron_oxygen_tank", "wood_oxygen_tank", "shell_oxygen_tank",
             "hard_shell_oxygen_tank", "marine_alloy_oxygen_tank"]
    for item_id in tanks:
        quads = shaded_player(player_quads()) + textured_quads(item_id)
        written.append(shot("close_" + item_id, quads, 150, -8, center=(0, 6, 0),
                            px_per_unit=10, span=14))

    flippers = ["shark_flippers", "wood_flippers", "coral_flippers",
                "shell_flippers", "hard_shell_flippers", "marine_alloy_flippers"]
    for item_id in flippers:
        quads = shaded_player(player_quads())
        for leg_x in (-3.9, 1.9):
            quads += flipper_quads(load(item_id), leg_x)
        written.append(shot("close_" + item_id, quads, 30, -12, center=(0, 21, 1.5),
                            px_per_unit=11, span=10))

    for item_id in outfit:
        written.append(atlas_sheet("atlas_" + item_id, item_id))

    print("Wrote %d previews to %s" % (len(written), OUT_DIR))


if __name__ == "__main__":
    main()
