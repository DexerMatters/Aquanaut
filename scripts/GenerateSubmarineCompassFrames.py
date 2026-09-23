#!/usr/bin/env python3
"""Slice the submarine compass strip into the per-bearing frames the item model needs.

Vanilla's compass is not one texture with the needle drawn at an angle: it is 32 separate 16x16
textures (`compass_00` … `compass_31`) plus an item model whose `overrides` pick one of them from an
`angle` property. This script reproduces that layout from the master strip, so
`textures/item/submarine_compass.png` stays the single source of art and everything downstream is
generated.

Frame numbering follows the strip: frame `i` has the needle pointing at bearing `i * 11.25°`
clockwise from north, so frame 0 is the needle pointing "up" the sprite.

The override thresholds are vanilla's: they sit on half-steps (1/64, 3/64, …) rather than on the
frame boundaries, which is why the runtime mapping rounds the rotation instead of flooring it. The
first and last overrides both point back at the base model, so the strip wraps cleanly.

Output:
  src/main/resources/assets/aquanaut/textures/item/compass/frame_NN.png        (32)
  src/main/resources/assets/aquanaut/models/item/submarine_compass_NN.json     (32)
  src/main/resources/assets/aquanaut/models/item/submarine_compass.json

Run from the repository root (after GenerateSubmarineInstrumentSprites.py and RefineItemOutlines.py):
  python3 scripts/GenerateSubmarineCompassFrames.py
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ITEM_DIR = Path("src/main/resources/assets/aquanaut/textures/item")
MODEL_DIR = Path("src/main/resources/assets/aquanaut/models/item")
STRIP = ITEM_DIR / "submarine_compass.png"
FRAME_DIR = ITEM_DIR / "compass"

MOD_ID = "aquanaut"
FRAMES = 32
ITEM = "submarine_compass"
PROPERTY = f"{MOD_ID}:angle"
BASE_MODEL = f"{MOD_ID}:item/{ITEM}"


def frame_model(index: int) -> str:
    return f"{MOD_ID}:item/{ITEM}_{index:02d}"


def base_model_json() -> dict:
    overrides = [{"predicate": {PROPERTY: 0.0}, "model": BASE_MODEL}]
    for index in range(1, FRAMES):
        # Vanilla's half-step thresholds: frame k owns the rotation centred on k/32.
        overrides.append({
            "predicate": {PROPERTY: (2 * index - 1) / (FRAMES * 2)},
            "model": frame_model(index),
        })
    overrides.append({
        "predicate": {PROPERTY: (FRAMES * 2 - 1) / (FRAMES * 2)},
        "model": BASE_MODEL,
    })
    return {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MOD_ID}:item/compass/frame_00"},
        "overrides": overrides,
    }


def main() -> None:
    strip = Image.open(STRIP).convert("RGBA")
    if strip.width != 16 or strip.height != FRAMES * 16:
        raise SystemExit(
            f"{STRIP} is {strip.width}x{strip.height}; expected 16x{FRAMES * 16} "
            f"({FRAMES} stacked frames)")

    FRAME_DIR.mkdir(parents=True, exist_ok=True)

    for index in range(FRAMES):
        frame = strip.crop((0, index * 16, 16, index * 16 + 16))
        frame.save(FRAME_DIR / f"frame_{index:02d}.png")
        (MODEL_DIR / f"{ITEM}_{index:02d}.json").write_text(
            json.dumps({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{MOD_ID}:item/compass/frame_{index:02d}"},
            }, indent=2) + "\n",
            encoding="utf-8")

    (MODEL_DIR / f"{ITEM}.json").write_text(
        json.dumps(base_model_json(), indent=2) + "\n", encoding="utf-8")

    print(f"{FRAMES} frames -> {FRAME_DIR}")
    print(f"{FRAMES} frame models + {ITEM}.json ({FRAMES + 1} overrides) -> {MODEL_DIR}")


if __name__ == "__main__":
    main()
