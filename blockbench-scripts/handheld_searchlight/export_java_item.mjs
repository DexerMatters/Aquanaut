#!/usr/bin/env node
// Splits the Blockbench Java Block/Item export of the handheld searchlight into
// the files the game loads.
//
// Blockbench owns the geometry, the UVs and the display transforms; this script
// only renames things the game needs and writes the two texture states:
//
//   handheld_searchlight.json          builtin/entity bridge: the display
//                                      transforms the custom item renderer
//                                      inherits, plus the particle sprite
//   handheld_searchlight_gui.json      the 16x16 inventory sprite
//   handheld_searchlight_gui_on.json   ...and its burning state
//   handheld_searchlight_held.json     the 3D element model in hand
//   handheld_searchlight_held_on.json  ...lit texture, identical geometry
//
// The two held models stay identical apart from the texture reference, so the
// lamp's state can never move the geometry; the tests pin that.
//
// Usage: node blockbench-scripts/handheld_searchlight/export_java_item.mjs
// (after exporting handheld_searchlight_export.json from Blockbench and saving
// the textures next to it).

import fs from "node:fs";
import path from "node:path";

const HERE = import.meta.dirname;
const REPO = path.resolve(HERE, "../..");
const ITEMS = path.join(REPO, "src/main/resources/assets/aquanaut/models/item");
const TEXTURES = path.join(REPO, "src/main/resources/assets/aquanaut/textures/item");

const MOD = "aquanaut:item/";
const ATLAS = MOD + "handheld_searchlight_model";
const ATLAS_ON = ATLAS + "_on";
const SPRITE = MOD + "handheld_searchlight";
const SPRITE_ON = SPRITE + "_on";

const exported = JSON.parse(fs.readFileSync(path.join(HERE, "handheld_searchlight_export.json"), "utf8"));

// Vanilla ignores element names and zero rotations, and the editor's groups
// list; strip them so the shipped model reads like the rest of the mod's items.
const elements = exported.elements.map(element => {
  const clean = JSON.parse(JSON.stringify(element));
  delete clean.name;

  if (clean.rotation && clean.rotation.angle === 0) {
    delete clean.rotation;
  }

  for (const face of Object.values(clean.faces)) {
    if (face.texture === "#0") {
      face.texture = "#body";
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

write("handheld_searchlight_held.json", held(ATLAS));
write("handheld_searchlight_held_on.json", held(ATLAS_ON));
write("handheld_searchlight_gui.json", {
  parent: "minecraft:item/generated",
  textures: { layer0: SPRITE },
});
write("handheld_searchlight_gui_on.json", {
  parent: "minecraft:item/generated",
  textures: { layer0: SPRITE_ON },
});
write("handheld_searchlight.json", {
  parent: "minecraft:builtin/entity",
  gui_light: "front",
  textures: { particle: SPRITE },
  display: exported.display,
});

const obsolete = path.join(ITEMS, "handheld_searchlight_on.json");
if (fs.existsSync(obsolete)) {
  fs.rmSync(obsolete);
  console.log("removed", path.relative(REPO, obsolete));
}

for (const [from, to] of [
  ["handheld_searchlight.png", "handheld_searchlight_model.png"],
  ["handheld_searchlight_on.png", "handheld_searchlight_model_on.png"],
]) {
  fs.copyFileSync(path.join(HERE, from), path.join(TEXTURES, to));
  console.log("copied", from, "->", path.relative(REPO, path.join(TEXTURES, to)));
}
