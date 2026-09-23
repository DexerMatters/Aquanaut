#!/usr/bin/env python3
"""Derive the large handheld air bladder from the small one.

The small bladder's model and sprite are authored by hand, so this script does
not redraw them.  The large item is deliberately the same device, bigger:

* the element model is the small one scaled about the item origin, and it keeps
  sampling the *same* texture sheet -- so the two can never fall out of style,
  and re-authoring the small model and re-running this keeps them in step;
* the item model's display transforms are scaled down by the same factor (with a
  small boost) so the large bladder also looks larger in the hand, which is
  where its size can actually be seen: a 16x16 slot cannot show it;
* the sprite keeps the small drawing and gains a bubble trail, which is the
  visual cue for the hard ascent it grants and what tells the two apart in the
  inventory.

Usage
-----
    python3 scripts/GenerateLargeAirBladderAssets.py            # write assets
    python3 scripts/GenerateLargeAirBladderAssets.py --preview   # + PNG previews
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut"
ITEM_MODELS = ASSETS / "models" / "item"
ITEM_TEXTURES = ASSETS / "textures" / "item"
PREVIEW_DIR = REPO_ROOT / "build" / "preview"

SMALL = "handheld_air_bladder"
LARGE = "large_handheld_air_bladder"

#: The large bladder is this much bigger than the small one.  The item model's
#: display transforms are left alone, so the size difference survives into the
#: hand -- which is the only place it can be seen.
MODEL_SCALE = 1.5
#: Item origin: both models are authored centred on it.
MODEL_CENTER = (8.0, 8.0, 8.0)
#: Framing box for the previews, so the two models are rendered to the same
#: scale and the size difference is actually visible.
PREVIEW_FIT_BOX = ((1.0, 1.0, 4.0), (15.0, 15.0, 12.0))

#: Bubble trail on the sprite: rising to the right of the cap.
BUBBLES = ((13, 3), (14, 2), (15, 1))
BUBBLE_COLOR = (170, 226, 242, 255)

# FaceInfo vertex order, as (x, y, z) min/max corner selectors (vanilla order).
FACE_VERTS = {
    "down": ((0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)),
    "up": ((0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)),
    "north": ((1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)),
    "south": ((0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)),
    "west": ((0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)),
    "east": ((1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)),
}
FACE_NORMAL = {
    "north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0),
    "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0),
}
CAM_DIR = (-0.62, 0.52, -0.92)


def read_json(path: Path) -> dict:
    if not path.is_file():
        raise SystemExit(f"missing source asset: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# model derivation
# ---------------------------------------------------------------------------


def scaled_point(point, factor: float):
    return [round(MODEL_CENTER[i] + (value - MODEL_CENTER[i]) * factor, 2)
            for i, value in enumerate(point)]


def derive_held_model(source: dict, factor: float) -> dict:
    """Same geometry, same texture sheet, scaled about the item origin."""
    elements = []
    for element in source["elements"]:
        derived = {
            "from": scaled_point(element["from"], factor),
            "to": scaled_point(element["to"], factor),
            "faces": json.loads(json.dumps(element["faces"])),
        }
        if "rotation" in element:
            rotation = json.loads(json.dumps(element["rotation"]))
            rotation["origin"] = scaled_point(rotation["origin"], factor)
            rotation["rescale"] = rotation.get("rescale", False)
            derived["rotation"] = rotation
        if "shade" in element:
            derived["shade"] = element["shade"]
        elements.append(derived)

    model = {"textures": json.loads(json.dumps(source["textures"])), "elements": elements}
    for key in ("gui_light", "ambientocclusion"):
        if key in source:
            model[key] = source[key]
    return model


def derive_item_model(source: dict) -> dict:
    """The builtin/entity model.

    The display transforms are copied unchanged on purpose: they place the item
    in the hand for a model of the small bladder's size, so reusing them is what
    makes the large bladder look large once it is held.
    """
    model = json.loads(json.dumps(source))
    model.pop("overrides", None)
    model.setdefault("textures", {})["particle"] = f"aquanaut:item/{LARGE}"
    return model


# ---------------------------------------------------------------------------
# sprite derivation
# ---------------------------------------------------------------------------


def derive_sprite(source: Image.Image) -> Image.Image:
    """The small drawing plus a bubble trail: a 16x16 slot cannot show scale,
    so the large bladder is marked by what it does."""
    sprite = source.convert("RGBA").copy()
    pixels = sprite.load()
    for x, y in BUBBLES:
        if 0 <= x < sprite.width and 0 <= y < sprite.height and pixels[x, y][3] < 8:
            pixels[x, y] = BUBBLE_COLOR
    return sprite


# ---------------------------------------------------------------------------
# preview: rasterise a model json so the result can actually be looked at
# ---------------------------------------------------------------------------


def resolve_texture(model: dict, reference: str) -> Path:
    name = model["textures"][reference.lstrip("#")]
    namespace, path = name.split(":", 1)
    return REPO_ROOT / "src" / "main" / "resources" / "assets" / namespace / "textures" / f"{path}.png"


def render_model(model_path: Path, out_size: int, supersample: int = 3, fit_box=None) -> Image.Image:
    model = read_json(model_path)
    texture = Image.open(resolve_texture(model, "#bladder")).convert("RGBA")
    texels = texture.load()
    tex_w, tex_h = texture.size

    triangles = []
    points_all = []
    for element in model["elements"]:
        low, high = element["from"], element["to"]
        corners = {(ix, iy, iz): (high[0] if ix else low[0], high[1] if iy else low[1], high[2] if iz else low[2])
                   for ix in (0, 1) for iy in (0, 1) for iz in (0, 1)}
        points_all.extend(corners.values())
        for face, definition in element["faces"].items():
            uv = definition["uv"]
            quad = [corners[corner] for corner in FACE_VERTS[face]]
            uvs = [(uv[0], uv[1]), (uv[0], uv[3]), (uv[2], uv[3]), (uv[2], uv[1])]
            for a, b, c in ((0, 1, 2), (0, 2, 3)):
                triangles.append((quad[a], quad[b], quad[c], uvs[a], uvs[b], uvs[c]))

    frame = out_size * supersample
    centre = tuple(sum(point[axis] for point in points_all) / len(points_all) for axis in range(3))
    length = math.sqrt(sum(component * component for component in CAM_DIR))
    unit = tuple(component / length for component in CAM_DIR)
    camera = tuple(centre[axis] + unit[axis] * 30.0 for axis in range(3))
    forward = tuple((centre[axis] - camera[axis]) for axis in range(3))
    length = math.sqrt(sum(component * component for component in forward))
    forward = tuple(component / length for component in forward)
    right = (forward[1] * 0 - forward[2] * 1, 0.0, forward[0] * 1 - forward[1] * 0)
    length = math.sqrt(sum(component * component for component in right))
    right = tuple(component / length for component in right)
    up = (right[1] * forward[2] - right[2] * forward[1],
          right[2] * forward[0] - right[0] * forward[2],
          right[0] * forward[1] - right[1] * forward[0])

    def project(point):
        rel = tuple(point[axis] - camera[axis] for axis in range(3))
        depth = max(1e-4, sum(rel[axis] * forward[axis] for axis in range(3)))
        return (sum(rel[axis] * right[axis] for axis in range(3)) / depth,
                sum(rel[axis] * up[axis] for axis in range(3)) / depth,
                depth)

    if fit_box is not None:
        low, high = fit_box
        points_all = [(high[0] if ix else low[0], high[1] if iy else low[1], high[2] if iz else low[2])
                      for ix in (0, 1) for iy in (0, 1) for iz in (0, 1)]
    projected = [project(point) for point in points_all]
    min_x = min(value[0] for value in projected)
    max_x = max(value[0] for value in projected)
    min_y = min(value[1] for value in projected)
    max_y = max(value[1] for value in projected)
    scale = min(frame / max(1e-6, max_x - min_x), frame / max(1e-6, max_y - min_y)) * 0.96
    off_x = (min_x + max_x) / 2.0
    off_y = (min_y + max_y) / 2.0

    def to_screen(point):
        px, py, depth = project(point)
        return ((px - off_x) * scale + frame / 2.0, frame / 2.0 - (py - off_y) * scale, depth)

    depths = [1e30] * (frame * frame)
    colors = [(0, 0, 0, 0)] * (frame * frame)
    for p0, p1, p2, uv0, uv1, uv2 in triangles:
        (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = to_screen(p0), to_screen(p1), to_screen(p2)
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if abs(area) < 1e-9:
            continue
        inv_area = 1.0 / area
        iz0, iz1, iz2 = 1.0 / z0, 1.0 / z1, 1.0 / z2
        start_x = max(0, int(min(x0, x1, x2)))
        end_x = min(frame - 1, int(max(x0, x1, x2)) + 1)
        start_y = max(0, int(min(y0, y1, y2)))
        end_y = min(frame - 1, int(max(y0, y1, y2)) + 1)
        for py in range(start_y, end_y + 1):
            sy = py + 0.5
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
                depth = 1.0 / (w0 * iz0 + w1 * iz1 + w2 * iz2)
                index = py * frame + px
                if depth >= depths[index]:
                    continue
                u = (w0 * uv0[0] * iz0 + w1 * uv1[0] * iz1 + w2 * uv2[0] * iz2) * depth
                v = (w0 * uv0[1] * iz0 + w1 * uv1[1] * iz1 + w2 * uv2[1] * iz2) * depth
                # model uv's live in a 0..16 space relative to the whole texture
                tx = min(tex_w - 1, max(0, int(u / 16.0 * tex_w)))
                ty = min(tex_h - 1, max(0, int(v / 16.0 * tex_h)))
                depths[index] = depth
                colors[index] = texels[tx, ty]

    image = Image.new("RGBA", (frame, frame), (0, 0, 0, 0))
    image.putdata(colors)
    if supersample > 1:
        image = image.resize((out_size, out_size), Image.BOX)
    return image


# ---------------------------------------------------------------------------


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--preview", action="store_true", help="also write PNG previews into build/preview")
    args = parser.parse_args(argv)

    held = derive_held_model(read_json(ITEM_MODELS / f"{SMALL}_held.json"), MODEL_SCALE)
    write_json(ITEM_MODELS / f"{LARGE}_held.json", held)

    write_json(ITEM_MODELS / f"{LARGE}.json", derive_item_model(read_json(ITEM_MODELS / f"{SMALL}.json")))
    write_json(ITEM_MODELS / f"{LARGE}_gui.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:item/{LARGE}"},
    })

    sprite = derive_sprite(Image.open(ITEM_TEXTURES / f"{SMALL}.png"))
    sprite.save(ITEM_TEXTURES / f"{LARGE}.png")

    print(f"wrote {ITEM_MODELS.relative_to(REPO_ROOT)}/{LARGE}_held.json "
          f"({len(held['elements'])} elements at {MODEL_SCALE}x)")
    print(f"wrote {ITEM_MODELS.relative_to(REPO_ROOT)}/{LARGE}.json and {LARGE}_gui.json")
    print(f"wrote {ITEM_TEXTURES.relative_to(REPO_ROOT)}/{LARGE}.png")

    if args.preview:
        PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
        for name, path in (("small", ITEM_MODELS / f"{SMALL}_held.json"),
                           ("large", ITEM_MODELS / f"{LARGE}_held.json")):
            backdrop = Image.new("RGBA", (320, 320), (36, 52, 66, 255))
            backdrop.alpha_composite(render_model(path, 320, fit_box=PREVIEW_FIT_BOX))
            backdrop.save(PREVIEW_DIR / f"{name}_air_bladder_held.png")
        for name, path in ((SMALL, ITEM_TEXTURES / f"{SMALL}.png"), (LARGE, ITEM_TEXTURES / f"{LARGE}.png")):
            image = Image.open(path).convert("RGBA")
            board = Image.new("RGBA", (image.width * 12, image.height * 12), (36, 52, 66, 255))
            board.alpha_composite(image.resize(board.size, Image.NEAREST))
            board.save(PREVIEW_DIR / f"{name}_sprite.png")
        print(f"previews in {PREVIEW_DIR.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
