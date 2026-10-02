#!/usr/bin/env node
// Splits the Blockbench Java Block/Item export of the portable sonar into the
// files the game loads.
//
// Blockbench owns the geometry, the UVs and the display transforms; this script
// only renames things the game needs and writes the two texture states:
//
//   portable_sonar.json          builtin/entity bridge: the display transforms
//                                the custom item renderer inherits, plus the
//                                particle sprite
//   portable_sonar_gui.json      the 16x16 inventory sprite
//   portable_sonar_gui_on.json   ...and its ranging state
//   portable_sonar_held.json     the 3D element model in hand
//   portable_sonar_held_on.json  ...lit texture, identical geometry
//
// The two held models stay identical apart from the texture reference, so the
// ranging state can never move the geometry; PortableSonarAssetTest pins that.
//
// The 16x16 sprites are drawn by scripts/GeneratePortableSonarAssets.py; the
// two atlases next to this file are the 64x64 model textures and are copied in
// as <name>_model.png / <name>_model_on.png.
//
// Usage: node blockbench-scripts/portable_sonar/export_java_item.mjs
// (after exporting portable_sonar_export.json from Blockbench and saving the
// atlases next to it).

import fs from "node:fs";
import path from "node:path";

const HERE = import.meta.dirname;
const REPO = path.resolve(HERE, "../..");
const ITEMS = path.join(REPO, "src/main/resources/assets/aquanaut/models/item");
const TEXTURES = path.join(REPO, "src/main/resources/assets/aquanaut/textures/item");

const ITEM = "portable_sonar";
const MOD = "aquanaut:item/";
const ATLAS = MOD + ITEM + "_model";
const ATLAS_ON = ATLAS + "_on";
const SPRITE = MOD + ITEM;
const SPRITE_ON = SPRITE + "_on";

const exported = JSON.parse(fs.readFileSync(path.join(HERE, ITEM + "_export.json"), "utf8"));

// Blockbench numbers the export's texture entries by project index, and the sonar
// project also holds the glow mask and the ranging atlas, so the atlas is not
// always #0. Resolve whichever ref points at this item's texture.
const ATLAS_REF = Object.entries(exported.textures || {})
  .find(([, texture]) => texture === ITEM)?.[0];

if (ATLAS_REF === undefined) {
  throw new Error(`the export has no ${ITEM} texture to bind: ${JSON.stringify(exported.textures)}`);
}

// Vanilla ignores element names and zero rotations, and the editor's groups
// list; strip them so the shipped model reads like the rest of the mod's items.
// A zero-thickness plate exports its disabled faces without a uv -- vanilla would
// then draw the whole atlas across them, so they are dropped here instead.
const elements = exported.elements.map(element => {
  const clean = JSON.parse(JSON.stringify(element));
  delete clean.name;

  if (clean.rotation && clean.rotation.angle === 0) {
    delete clean.rotation;
  }

  for (const face of Object.keys(clean.faces)) {
    if (clean.faces[face].uv === undefined) {
      delete clean.faces[face];
      continue;
    }

    if (clean.faces[face].texture === "#" + ATLAS_REF) {
      clean.faces[face].texture = "#body";
    }
  }

  return clean;
});

const write = (name, value) => {
  const file = path.join(ITEMS, name);
  fs.writeFileSync(file, JSON.stringify(value, null, 2) + "\n");
  console.log("wrote", path.relative(REPO, file));
};

const held = texture => ({
  gui_light: "front",
  textures: { body: texture },
  elements,
});

write(ITEM + "_held.json", held(ATLAS));
write(ITEM + "_held_on.json", held(ATLAS_ON));
write(ITEM + "_gui.json", {
  parent: "minecraft:item/generated",
  textures: { layer0: SPRITE },
});
write(ITEM + "_gui_on.json", {
  parent: "minecraft:item/generated",
  textures: { layer0: SPRITE_ON },
});
write(ITEM + ".json", {
  parent: "minecraft:builtin/entity",
  gui_light: "front",
  textures: { particle: SPRITE },
  display: exported.display,
});

for (const [from, to] of [
  [ITEM + ".png", ITEM + "_model.png"],
  [ITEM + "_on.png", ITEM + "_model_on.png"],
]) {
  const source = path.join(HERE, from);
  const target = path.join(TEXTURES, to);
  fs.copyFileSync(source, target);
  console.log("copied", from, "->", path.relative(REPO, target));
}
