#!/usr/bin/env python3
"""Generate the JSON boilerplate for the Crystal Nest (水晶巢).

Covers the fifteen new blocks — models, blockstates, item models, loot tables — and
extends the vanilla tool tags. The wall crystals get the six-orientation cluster
language (a four-plane cluster model rotated to every facing), the luminous pair gets
the proven inflated glow-plane overlay with full-bright neoforge face data, and the
algae mat borrows the layered ash-drift layout. Re-run after adding a block.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path("src/main/resources")
ASSETS = ROOT / "assets" / "aquanaut"
DATA = ROOT / "data"

CUBE_BOTTOM_TOP = ["crystal_nest_stone"]
CUBE_ALL = ["crystal_druse"]
PILLAR = ["crystal_column"]
CLUSTERS = [
    "white_crystal_cluster", "rose_crystal_cluster", "amethyst_crystal_cluster",
    "aqua_crystal_cluster", "smoky_crystal_cluster",
]
GLOW_CLUSTERS = ["resonant_crystal_cluster", "life_gem_cluster"]
CROSS_PLANTS = ["algae_tuft", "crystal_sprout"]
FRINGE_PARTS = ["top", "body", "tail"]

LOOT_ALL = (
    CUBE_BOTTOM_TOP + CUBE_ALL + PILLAR + CLUSTERS + GLOW_CLUSTERS
    + CROSS_PLANTS + ["algae_mat", "crystal_fringe"]
)
PICKAXE = CUBE_BOTTOM_TOP + CUBE_ALL + PILLAR + CLUSTERS + GLOW_CLUSTERS
SHOVEL = ["algae_mat"]

# vanilla facing rotations for a model whose base points up (amethyst convention)
FACING_VARIANTS = {
    "down": {"x": 180},
    "up": {},
    "north": {"x": 90},
    "south": {"x": 90, "y": 180},
    "west": {"x": 90, "y": 270},
    "east": {"x": 90, "y": 90},
}


def write(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n")
    print(f"wrote {path}")


def loot(name: str) -> dict:
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"aquanaut:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def simple_variant(model: str) -> dict:
    return {"variants": {"": {"model": model}}}


def glow_face(texture: str, cullface: str | None = None) -> dict:
    face = {
        "uv": [0, 0, 16, 16],
        "texture": texture,
        "neoforge_data": {"block_light": 15, "sky_light": 15},
    }
    if cullface:
        face["cullface"] = cullface
    return face


def cube_bottom_top(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": f"aquanaut:block/{name}_top",
            "side": f"aquanaut:block/{name}",
            "bottom": f"aquanaut:block/{name}_top",
        },
    })
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}"})


def cube_all(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"aquanaut:block/{name}"},
    })
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}"})


def pillar(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", {
        "variants": {
            "axis=x": {"model": f"aquanaut:block/{name}_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": f"aquanaut:block/{name}"},
            "axis=z": {"model": f"aquanaut:block/{name}_horizontal", "x": 90},
        },
    })
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": f"aquanaut:block/{name}_top", "side": f"aquanaut:block/{name}"},
    })
    write(ASSETS / "models" / "block" / f"{name}_horizontal.json", {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": f"aquanaut:block/{name}_top", "side": f"aquanaut:block/{name}"},
    })
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}"})


def cluster_model(name: str, glow: bool) -> dict:
    """Four crossed cutout planes (45° star) — a full crystal bush from any angle."""
    base = f"aquanaut:block/{name}"
    glow_tex = f"aquanaut:block/{name}_glowmask"
    rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
    planes = [
        # (from, to, first face, second face, rotation, thin axis)
        ([0.8, 0, 8], [15.2, 16, 8], "north", "south", rot, 2),
        ([8, 0, 0.8], [8, 16, 15.2], "west", "east", rot, 0),
        ([0.8, 0, 8], [15.2, 16, 8], "north", "south", None, 2),
        ([8, 0, 0.8], [8, 16, 15.2], "west", "east", None, 0),
    ]
    elements = []
    textures = {"particle": base, "cross": base}
    for start, end, face_a, face_b, rotation, _axis in planes:
        element = {"from": start, "to": end, "shade": False}
        if rotation:
            element["rotation"] = rotation
        element["faces"] = {
            face_a: {"uv": [0, 0, 16, 16], "texture": "#cross"},
            face_b: {"uv": [0, 0, 16, 16], "texture": "#cross"},
        }
        elements.append(element)
    if glow:
        textures["glow"] = glow_tex
        for start, end, face_a, face_b, rotation, axis in planes:
            # the glow box straddles its plane: each face rides 0.01 outside the sprite
            start = [v - (0.01 if i == axis else 0.0) for i, v in enumerate(start)]
            end = [v + (0.01 if i == axis else 0.0) for i, v in enumerate(end)]
            element = {"from": start, "to": end, "shade": False}
            if rotation:
                element["rotation"] = rotation
            element["faces"] = {
                face_a: glow_face("#glow"),
                face_b: glow_face("#glow"),
            }
            elements.append(element)
    return {
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": textures,
        "elements": elements,
    }


def cluster(name: str, glow: bool) -> None:
    variants = {}
    for facing, rotation in FACING_VARIANTS.items():
        for waterlogged in ("false", "true"):
            entry = {"model": f"aquanaut:block/{name}"}
            entry.update(rotation)
            variants[f"facing={facing},waterlogged={waterlogged}"] = entry
    write(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
    write(ASSETS / "models" / "block" / f"{name}.json", cluster_model(name, glow))
    write(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:block/{name}"},
    })


def cross_plant(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "render_type": "minecraft:cutout",
        "parent": "minecraft:block/cross",
        "textures": {"cross": f"aquanaut:block/{name}"},
    })
    write(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:block/{name}"},
    })


def mat(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", {
        "variants": {
            "waterlogged=false": {"model": f"aquanaut:block/{name}"},
            "waterlogged=true": {"model": f"aquanaut:block/{name}"},
        },
    })
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/thin_block",
        "textures": {
            "particle": f"aquanaut:block/{name}",
            "texture": f"aquanaut:block/{name}",
            "top": f"aquanaut:block/{name}",
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 1, 16],
            "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "north": {"uv": [0, 15, 16, 16], "texture": "#texture", "cullface": "north"},
                "south": {"uv": [0, 15, 16, 16], "texture": "#texture", "cullface": "south"},
                "west": {"uv": [0, 15, 16, 16], "texture": "#texture", "cullface": "west"},
                "east": {"uv": [0, 15, 16, 16], "texture": "#texture", "cullface": "east"},
            },
        }],
    })
    write(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:block/{name}"},
    })


def algae_mat() -> None:
    """One whole solid block of algae turf: lush top, ragged side, earthy underside."""
    name = "algae_mat"
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": f"aquanaut:block/{name}_top",
            "side": f"aquanaut:block/{name}_side",
            "bottom": f"aquanaut:block/{name}_top",
        },
    })
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}"})


def drooping(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", {
        "variants": {
            f"part={part}": {"model": f"aquanaut:block/{name}_{part}"}
            for part in FRINGE_PARTS
        },
    })
    for part in FRINGE_PARTS:
        write(ASSETS / "models" / "block" / f"{name}_{part}.json", {
            "render_type": "minecraft:cutout",
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"aquanaut:block/{name}_{part}"},
        })
    write(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:block/{name}_top"},
    })


def extend_tag(path: Path, values: list[str]) -> None:
    payload = json.loads(path.read_text())
    existing = payload.setdefault("values", [])
    for value in values:
        if value not in existing:
            existing.append(f"aquanaut:{value}")
    path.write_text(json.dumps(payload, indent=2) + "\n")
    print(f"updated {path}")


def main() -> None:
    for name in CUBE_BOTTOM_TOP:
        cube_bottom_top(name)
    for name in CUBE_ALL:
        cube_all(name)
    for name in PILLAR:
        pillar(name)
    for name in CLUSTERS:
        cluster(name, glow=False)
    for name in GLOW_CLUSTERS:
        cluster(name, glow=True)
    for name in CROSS_PLANTS:
        cross_plant(name)
    algae_mat()
    drooping("crystal_fringe")
    for name in LOOT_ALL:
        write(DATA / "aquanaut" / "loot_table" / "blocks" / f"{name}.json", loot(name))
    extend_tag(DATA / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json", PICKAXE)
    extend_tag(DATA / "minecraft" / "tags" / "block" / "mineable" / "shovel.json", SHOVEL)
    print("crystal nest data written")


if __name__ == "__main__":
    main()
