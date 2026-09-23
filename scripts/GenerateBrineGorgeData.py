#!/usr/bin/env python3
"""Generate the JSON boilerplate for the Brine Mirror Gorge enrichment.

Covers the six new evaporite blocks (models, blockstates, items), completes the loot
tables the original gorge blocks never shipped with, and gives calcite quills the same
little glow-mask overlay language the newer biomes use. Re-run after adding a block.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path("src/main/resources")
ASSETS = ROOT / "assets" / "aquanaut"
DATA = ROOT / "data" / "aquanaut"

CUBE_BOTTOM_TOP = ["hopper_halite", "sylvite_crust"]
PILLAR = ["gypsum_blade"]
GLOW_CUBES = ["halite_druse"]
GLOW_CROSS = ["gypsum_rose", "calcite_quill"]
MATS = ["mirror_flake"]
# Loot tables for the whole evaporite family, old and new.
LOOT_ALL = [
    "halite_crust", "halite_pipe", "varve_shale", "brine_mirror",
    "calcite_quill", "halite_rosette", "salt_fringe",
    "hopper_halite", "halite_druse", "gypsum_blade", "sylvite_crust",
    "mirror_flake", "gypsum_rose",
]


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


def cross_glow_model(name: str) -> dict:
    """Cross plant with a full-bright glow plane hugging each cutout plane."""
    base = f"aquanaut:block/{name}"
    glow = f"aquanaut:block/{name}_glowmask"
    rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
    return {
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"particle": base, "cross": base, "glow": glow},
        "elements": [
            {"from": [0.8, 0, 8], "to": [15.2, 16, 8], "rotation": rot, "shade": False,
             "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                       "south": {"uv": [0, 0, 16, 16], "texture": "#cross"}}},
            {"from": [8, 0, 0.8], "to": [8, 16, 15.2], "rotation": rot, "shade": False,
             "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                       "east": {"uv": [0, 0, 16, 16], "texture": "#cross"}}},
            {"from": [0.8, 0, 7.99], "to": [15.2, 16, 8.01], "rotation": rot, "shade": False,
             "faces": {"north": glow_face("#glow"), "south": glow_face("#glow")}},
            {"from": [7.99, 0, 0.8], "to": [8.01, 16, 15.2], "rotation": rot, "shade": False,
             "faces": {"west": glow_face("#glow"), "east": glow_face("#glow")}},
        ],
    }


def cube_glow_model(name: str) -> dict:
    """Full cube with the proven seaweed-fruit-style inflated glow box."""
    base = f"aquanaut:block/{name}"
    glow = f"aquanaut:block/{name}_glowmask"
    sides = ("down", "up", "north", "south", "west", "east")
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": base, "all": base, "glow_all": glow},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16],
             "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#all", "cullface": d}
                       for d in sides}},
            {"from": [-0.01, -0.01, -0.01], "to": [16.01, 16.01, 16.01],
             "faces": {d: glow_face("#glow_all", d) for d in sides}},
        ],
    }


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


def glow_cube(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    write(ASSETS / "models" / "block" / f"{name}.json", cube_glow_model(name))
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}"})


def glow_cross(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    write(ASSETS / "models" / "block" / f"{name}.json", cross_glow_model(name))
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


def main() -> None:
    for name in CUBE_BOTTOM_TOP:
        cube_bottom_top(name)
    for name in PILLAR:
        pillar(name)
    for name in GLOW_CUBES:
        glow_cube(name)
    for name in GLOW_CROSS:
        glow_cross(name)
    for name in MATS:
        mat(name)
    for name in LOOT_ALL:
        write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))
    print("brine gorge enrichment data written")


if __name__ == "__main__":
    main()
