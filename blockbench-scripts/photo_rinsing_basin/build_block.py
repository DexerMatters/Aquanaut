"""Builds the photo rinsing basin block: geometry, one 16x16 texture, and model UVs.

Geometry, atlas and UVs all live here so the three can never drift apart, and the script
refuses to emit a model whose cubes intersect: overlapping boxes are what causes the
z-fighting stripes you get when two faces occupy the same plane.

Parts are stacked so they *touch* rather than interpenetrate:

    plinth 0..2   skirt 2..3   cabinet 3..10   rim 10..12   walls 12..14
    rail posts stand on the rim at the left/right edges, bar spans them at 15..16

Atlas layout (16x16, UV units == pixels):
    metal  (0, 0)  .. (11, 11)   brushed light steel
    dark   (11, 0) .. (16, 11)   shadowed steel
    rinse  (0, 11) .. (6, 16)    chemical brine
    rest                         dark steel, so the particle reads solid
"""

import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
BLOCK_TEXTURE = ROOT / "src/main/resources/assets/aquanaut/textures/block/photo_rinsing_basin.png"
MODEL = ROOT / "src/main/resources/assets/aquanaut/models/block/photo_rinsing_basin.json"

SIZE = 16
TEXTURE_REF = "aquanaut:block/photo_rinsing_basin"

# name -> (u1, v1, u2, v2) in the 16x16 atlas
REGIONS = {
    "metal": (0, 0, 11, 11),
    "dark": (11, 0, 16, 11),
    "rinse": (0, 11, 6, 16),
}
TAIL = (6, 11, 16, 16)

# dissection-table steel ramp
METAL_TOP, METAL_BOTTOM = (198, 204, 210), (150, 157, 165)
DARK_TOP, DARK_BOTTOM = (108, 115, 122), (60, 65, 70)
RINSE_TOP, RINSE_BOTTOM = (44, 116, 130), (30, 94, 108)

ALL_FACES = ("north", "south", "east", "west", "up", "down")

# name, from, to, base material, faces that read as shadowed/inset
ELEMENTS = [
    ("plinth_base",      (0, 0, 0),     (16, 2, 16),    "dark",  set()),
    ("plinth_skirt",     (1, 2, 1),     (15, 3, 15),    "metal", {"down"}),
    ("cabinet_body",     (2, 3, 2),     (14, 10, 14),   "metal", {"down"}),
    ("cabinet_panel",    (3, 4, 1),     (13, 9, 2),     "metal", {"north", "east", "west", "up", "down"}),
    ("cabinet_handle_l", (4, 6, 0),     (7, 7, 1),      "metal", {"east", "south", "west", "down"}),
    ("cabinet_handle_r", (9, 6, 0),     (12, 7, 1),     "metal", {"east", "south", "west", "down"}),
    ("basin_rim",        (1, 10, 1),    (15, 12, 15),   "metal", {"down"}),
    ("basin_wall_n",     (2, 12, 2),    (14, 14, 3),    "metal", {"east", "south", "west", "down"}),
    ("basin_wall_s",     (2, 12, 13),   (14, 14, 14),   "metal", {"north", "east", "west", "down"}),
    ("basin_wall_w",     (2, 12, 3),    (3, 14, 13),    "metal", {"north", "south", "east", "down"}),
    ("basin_wall_e",     (13, 12, 3),   (14, 14, 13),   "metal", {"north", "south", "west", "down"}),
    ("rinse_liquid",     (3, 12, 3),    (13, 12.5, 13), "rinse", set()),
    ("rail_post_l",      (1, 12, 7),    (2, 16, 8),     "dark",  set()),
    ("rail_post_r",      (14, 12, 7),   (15, 16, 8),    "dark",  set()),
    ("rail_bar",         (2, 15, 7),    (14, 16, 8),    "metal", {"east", "south", "west", "down"}),
    ("clip_l",           (4, 14, 7),    (5, 15, 8),     "metal", {"down"}),
    ("clip_r",           (11, 14, 7),   (12, 15, 8),    "metal", {"down"}),
    ("rinse_nozzle",     (7, 14, 2),    (9, 15, 5),     "metal", {"east", "south", "west", "down"}),
]

GROUPS = [
    ("plinth",   ["plinth_base", "plinth_skirt"]),
    ("cabinet",  ["cabinet_body", "cabinet_panel", "cabinet_handle_l", "cabinet_handle_r"]),
    ("basin",    ["basin_rim", "basin_wall_n", "basin_wall_s", "basin_wall_w", "basin_wall_e",
                  "rinse_liquid"]),
    ("fixtures", ["rail_post_l", "rail_post_r", "rail_bar", "clip_l", "clip_r", "rinse_nozzle"]),
]

DISPLAY = {
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                              "scale": [0.35, 0.35, 0.35]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0],
                              "scale": [0.4, 0.4, 0.4]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.7, 0.7, 0.7]},
    "ground": {"translation": [0, 3, 0], "scale": [0.3, 0.3, 0.3]},
    "fixed": {"scale": [0.5, 0.5, 0.5]},
}


def clamp(v):
    return max(0, min(255, v))


def gradient(img, region, top, bottom, seed, streaks=0):
    x1, y1, x2, y2 = region
    w, h = x2 - x1, y2 - y1
    rng = random.Random(seed)
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        base = [int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)]
        for x in range(w):
            n = rng.randint(-4, 4)
            px[x1 + x, y1 + y] = (clamp(base[0] + n), clamp(base[1] + n), clamp(base[2] + n), 255)
    for _ in range(streaks):
        sy = y1 + rng.randrange(h)
        sx = x1 + rng.randrange(w)
        for x in range(sx, min(x2, sx + rng.randint(1, max(1, w // 3)))):
            r, g, b, a = px[x, sy]
            d = rng.choice((-1, 1)) * rng.randint(3, 8)
            px[x, sy] = (clamp(r + d), clamp(g + d), clamp(b + d), a)


def build_texture():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
    gradient(img, REGIONS["metal"], METAL_TOP, METAL_BOTTOM, 7, streaks=10)
    gradient(img, REGIONS["dark"], DARK_TOP, DARK_BOTTOM, 11, streaks=6)
    gradient(img, REGIONS["rinse"], RINSE_TOP, RINSE_BOTTOM, 19, streaks=4)
    gradient(img, TAIL, DARK_TOP, DARK_BOTTOM, 23, streaks=4)

    px = img.load()
    rng = random.Random(31)
    rx1, ry1, rx2, ry2 = REGIONS["rinse"]
    for _ in range(5):
        px[rng.randrange(rx1, rx2), rng.randrange(ry1, ry2)] = (168, 221, 216, 255)

    img.save(BLOCK_TEXTURE)
    print(f"wrote {BLOCK_TEXTURE} ({img.width}x{img.height})")


def uv_for(face_w, face_h, region):
    """Scale the face into its region, preserving aspect, and centre it."""
    x1, y1, x2, y2 = region
    rw, rh = x2 - x1, y2 - y1
    scale = min(rw / face_w, rh / face_h)
    w, h = face_w * scale, face_h * scale
    ox = x1 + (rw - w) / 2.0
    oy = y1 + (rh - h) / 2.0
    return [round(ox, 2), round(oy, 2), round(ox + w, 2), round(oy + h, 2)]


def face_extent(frm, to, face):
    dx, dy, dz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    if face in ("north", "south"):
        return dx, dy
    if face in ("east", "west"):
        return dz, dy
    return dx, dz


def check_overlaps():
    """Refuse to build a model with intersecting cubes: that is what z-fights."""
    clashes = []
    boxes = [(n, f, t) for n, f, t, _, _ in ELEMENTS]
    for i in range(len(boxes)):
        for j in range(i + 1, len(boxes)):
            an, af, at = boxes[i]
            bn, bf, bt = boxes[j]
            depths = []
            for axis in range(3):
                lo = max(af[axis], bf[axis])
                hi = min(at[axis], bt[axis])
                if hi - lo <= 1e-6:
                    break
                depths.append(round(hi - lo, 3))
            else:
                clashes.append((an, bn, depths))
    if clashes:
        for a, b, d in clashes:
            print(f"  OVERLAP {a} x {b} depth={d}")
        raise SystemExit(f"{len(clashes)} overlapping cube pair(s); fix ELEMENTS before building")
    print(f"geometry clean: {len(ELEMENTS)} cubes, 0 overlaps")


def build_model():
    check_overlaps()
    elements = []
    by_name = {}

    for name, frm, to, base, dark_faces in ELEMENTS:
        by_name[name] = len(elements)
        faces = {}
        # the brine is a surface, not a solid: only its top is ever seen
        wanted = ("up",) if name == "rinse_liquid" else ALL_FACES
        for face in wanted:
            material = "dark" if face in dark_faces else base
            w, h = face_extent(frm, to, face)
            faces[face] = {"uv": uv_for(w, h, REGIONS[material]), "texture": "#all"}
        elements.append({"name": name, "from": list(frm), "to": list(to), "faces": faces})

    groups = [{"name": gname, "origin": [8, 8, 8], "scope": 0, "color": 0,
               "children": [by_name[n] for n in members]} for gname, members in GROUPS]

    model = {
        "parent": "minecraft:block/block",
        "textures": {"all": TEXTURE_REF, "particle": TEXTURE_REF},
        "elements": elements,
        "groups": groups,
        "display": DISPLAY,
    }

    MODEL.write_text(json.dumps(model, indent=2) + "\n")
    print(f"wrote {MODEL} ({len(elements)} elements, {len(groups)} groups, 1 texture)")


if __name__ == "__main__":
    build_texture()
    build_model()
