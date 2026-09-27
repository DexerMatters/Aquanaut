#!/usr/bin/env python3
"""Generate the JSON boilerplate for Brimstone Caldera blocks (models, blockstates, items, loot).

Every volcanic block follows the house conventions already used by the Brine Mirror Gorge
assets: plain cubes use cube_all, layered flows use cube_bottom_top, chimney columns use
the cube_column pair, plants are cutout crosses, hanging flora is a three-part drooping
chain, and thin covers are element models in the spirit of vanilla snow layers. Re-run
after adding a block to keep every variant in sync.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path("src/main/resources")
ASSETS = ROOT / "assets" / "aquanaut"
DATA = ROOT / "data" / "aquanaut"

CUBE_ALL = ["scoria", "volcanic_agglomerate", "pumice", "obsidian_glass",
            "acid_etched_basalt", "volcanic_ash", "sulfur_moss"]
CUBE_BOTTOM_TOP = ["pillow_basalt", "sulfur_crust", "sinter"]
PILLAR = ["volcanic_basalt", "vent_chimney"]
CROSS = ["sulfur_crystal", "firebloom", "fumarole"]
CHAIN = ["sulfur_stalactite", "ember_kelp"]
MATS = ["thermophilic_mat_gold", "thermophilic_mat_rust", "thermophilic_mat_olive"]
CUTOUT = {"sulfur_moss"}
# Little glow masks: accent sprites rendered full-bright on an inflated overlay box,
# exactly like the fluorescent coral and seaweed fruit of the earlier biomes.
GLOW_CUBES: set[str] = set()
GLOW_CROSS = {"sulfur_crystal", "firebloom", "fumarole"}
GLOW_CHAIN_PARTS = {"sulfur_stalactite": {"tail"}, "ember_kelp": {"top", "body", "tail"}}


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
            # The glow planes sit 0.02 off the cutout planes: full-bright, never z-fighting.
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


def cube_all(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    model = {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"aquanaut:block/{name}"},
    }
    if name in CUTOUT:
        model = {"render_type": "minecraft:cutout", **model}
    if name in GLOW_CUBES:
        model = cube_glow_model(name)
    write(ASSETS / "models" / "block" / f"{name}.json", model)
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}"})
    write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))


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
    write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))


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
    write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))


def cross(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", simple_variant(f"aquanaut:block/{name}"))
    if name in GLOW_CROSS:
        write(ASSETS / "models" / "block" / f"{name}.json", cross_glow_model(name))
    else:
        write(ASSETS / "models" / "block" / f"{name}.json", {
            "render_type": "minecraft:cutout",
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"aquanaut:block/{name}"},
        })
    write(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:block/{name}"},
    })
    write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))


def chain(name: str) -> None:
    write(ASSETS / "blockstates" / f"{name}.json", {
        "variants": {
            "part=top": {"model": f"aquanaut:block/{name}_top"},
            "part=body": {"model": f"aquanaut:block/{name}_body"},
            "part=tail": {"model": f"aquanaut:block/{name}_tail"},
        },
    })
    glow_parts = GLOW_CHAIN_PARTS.get(name, set())
    for part in ("top", "body", "tail"):
        if part in glow_parts:
            write(ASSETS / "models" / "block" / f"{name}_{part}.json",
                  cross_glow_model(f"{name}_{part}"))
        else:
            write(ASSETS / "models" / "block" / f"{name}_{part}.json", {
                "render_type": "minecraft:cutout",
                "parent": "minecraft:block/cross",
                "textures": {"cross": f"aquanaut:block/{name}_{part}"},
            })
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"aquanaut:block/{name}_top"})
    write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))


def thin_cover(model_name: str, texture: str, height: int, uv_top: int) -> dict:
    return {
        "parent": "minecraft:block/thin_block",
        "textures": {
            "particle": texture,
            "texture": texture,
            "top": texture,
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, height, 16],
            "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "north": {"uv": [0, uv_top, 16, 16], "texture": "#texture", "cullface": "north"},
                "south": {"uv": [0, uv_top, 16, 16], "texture": "#texture", "cullface": "south"},
                "west": {"uv": [0, uv_top, 16, 16], "texture": "#texture", "cullface": "west"},
                "east": {"uv": [0, uv_top, 16, 16], "texture": "#texture", "cullface": "east"},
            },
        }],
    }


def mat(name: str) -> None:
    variants = {
        "waterlogged=false": {"model": f"aquanaut:block/{name}"},
        "waterlogged=true": {"model": f"aquanaut:block/{name}"},
    }
    write(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
    write(ASSETS / "models" / "block" / f"{name}.json",
          thin_cover(name, f"aquanaut:block/{name}", 1, 15))
    write(ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"aquanaut:block/{name}"},
    })
    write(DATA / "loot_table" / "blocks" / f"{name}.json", loot(name))


def ash_layer() -> None:
    # layers=1 -> 2px, layers=2..4 -> 5px, layers=5..8 -> 9px drifts.
    models = {1: "ash_layer_1", 2: "ash_layer_2", 5: "ash_layer_3"}
    for key, (height, uv) in {1: (2, 14), 2: (5, 11), 5: (9, 7)}.items():
        model_name = models[key]
        write(ASSETS / "models" / "block" / f"{model_name}.json",
              thin_cover(f"top_{model_name}", "aquanaut:block/ash_layer_side", height, uv))
        top_model = json.loads((ASSETS / "models" / "block" / f"{model_name}.json").read_text())
        top_model["textures"]["top"] = "aquanaut:block/ash_layer_top"
        write(ASSETS / "models" / "block" / f"{model_name}.json", top_model)
    variants = {}
    for layers in range(1, 9):
        model_name = models[1 if layers == 1 else 2 if layers <= 4 else 5]
        for waterlogged in ("false", "true"):
            variants[f"layers={layers},waterlogged={waterlogged}"] = {
                "model": f"aquanaut:block/{model_name}",
            }
    write(ASSETS / "blockstates" / "ash_layer.json", {"variants": variants})
    write(ASSETS / "models" / "item" / "ash_layer.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "aquanaut:block/ash_layer_top"},
    })
    write(DATA / "loot_table" / "blocks" / "ash_layer.json", loot("ash_layer"))


def particle_descriptions() -> None:
    # 1.21 builds every sprite-set particle from assets/<ns>/particles/<type>.json;
    # without these the engine leaves the sprite set unbound and crashes on spawn.
    for name in ("vent_steam", "sulfur_gas", "ash_mote"):
        write(ASSETS / "particles" / f"{name}.json", {"textures": [f"aquanaut:{name}"]})


def main() -> None:
    for name in CUBE_ALL:
        cube_all(name)
    for name in CUBE_BOTTOM_TOP:
        cube_bottom_top(name)
    for name in PILLAR:
        pillar(name)
    for name in CROSS:
        cross(name)
    for name in CHAIN:
        chain(name)
    for name in MATS:
        mat(name)
    ash_layer()
    particle_descriptions()
    print("brimstone caldera data written")


if __name__ == "__main__":
    main()
