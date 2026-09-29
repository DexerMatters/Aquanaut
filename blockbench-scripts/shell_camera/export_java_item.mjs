#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

const HERE = import.meta.dirname;
const REPO = path.resolve(HERE, "../..");
const MODELS = path.join(REPO, "src/main/resources/assets/aquanaut/models/item");
const TEXTURES = path.join(REPO, "src/main/resources/assets/aquanaut/textures/item");
const exported = JSON.parse(fs.readFileSync(path.join(HERE, "shell_camera_export.json"), "utf8"));
const sourceTextureKey = Object.entries(exported.textures)
  .find(([, texture]) => texture === "shell_camera")?.[0];
if (sourceTextureKey === undefined) throw new Error("shell_camera texture reference missing");
const elements = exported.elements.map(element => {
  const clean = JSON.parse(JSON.stringify(element));
  delete clean.name;
  if (clean.rotation && clean.rotation.angle === 0) delete clean.rotation;
  for (const face of Object.values(clean.faces)) {
    if (face.texture === `#${sourceTextureKey}`) face.texture = "#body";
  }
  return clean;
});
const write = (name, value) => fs.writeFileSync(path.join(MODELS, name), JSON.stringify(value, null, 2) + "\n");
write("shell_camera_held.json", {
  texture_size: exported.texture_size,
  gui_light: "front",
  textures: { body: "aquanaut:item/shell_camera_model" },
  elements
});
write("shell_camera_gui.json", {
  parent: "minecraft:item/generated",
  textures: { layer0: "aquanaut:item/shell_camera" }
});
write("shell_camera.json", {
  parent: "minecraft:builtin/entity",
  gui_light: "front",
  textures: { particle: "aquanaut:item/shell_camera" },
  display: exported.display
});
fs.copyFileSync(path.join(HERE, "shell_camera.png"), path.join(TEXTURES, "shell_camera_model.png"));
fs.copyFileSync(path.join(HERE, "shell_camera_inventory.png"), path.join(TEXTURES, "shell_camera.png"));
