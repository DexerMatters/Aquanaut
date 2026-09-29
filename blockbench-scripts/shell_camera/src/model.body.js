const MODEL_ID = "shell_camera";
const PREFIX = MODEL_ID + "_";
const PALETTE = {
  a: "#F5E8D2",
  b: "#EACFB8",
  c: "#D9AFA4",
  d: "#C88F8E",
  e: "#FFF5E7",
  f: "#B77B82",
  i: "#78838B",
  j: "#929DA4",
  k: "#566169",
  l: "#3C464D",
  m: "#B7C0C5",
  n: "#252D33",
  g: "#16384B",
  h: "#2A6577",
  o: "#72B8C4",
  p: "#B9E1DF",
  r: "#8C4B42",
  s: "#C86A56",
  t: "#442F31"
};
const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 128,
  maxHeight: 128,
  padding: 1
});
const groups = [
  { id: "body", parent: null, origin: [8, 6, 8], rotation: [0, 0, 0] },
  { id: "lens", parent: "body", origin: [8, 9, 6], rotation: [0, 0, 0] },
  { id: "back", parent: "body", origin: [8, 9, 10], rotation: [0, 0, 0] },
  { id: "controls", parent: "body", origin: [11, 12, 8], rotation: [0, 0, 0] }
];
const box = (id, parent, from, to, fill) => atlas.addBox({
  id,
  parent,
  from,
  to,
  origin: from.slice(),
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null
}, { fill });
box("shell_core", "body", [4, 6, 6], [12, 12, 10], "a");
box("shell_crown_left", "body", [5, 12, 7], [7, 13, 10], "b");
box("shell_crown_center", "body", [7, 12, 6], [9, 14, 10], "e");
box("shell_crown_right", "body", [9, 12, 7], [11, 13, 10], "c");
box("frame_left", "body", [4, 6, 5], [5, 12, 6], "i");
box("frame_right", "body", [11, 6, 5], [12, 12, 6], "i");
box("frame_bottom", "body", [5, 6, 5], [11, 7, 6], "i");
box("frame_top", "body", [5, 11, 5], [11, 12, 6], "j");
box("frame_back_left", "body", [4, 6, 10], [5, 12, 11], "i");
box("frame_back_right", "body", [11, 6, 10], [12, 12, 11], "i");
box("frame_back_bottom", "body", [5, 6, 10], [11, 7, 11], "i");
box("frame_back_top", "body", [5, 11, 10], [11, 12, 11], "j");
box("lens_plate", "lens", [5, 7, 5], [11, 11, 6], "a");
box("lens_rim_left", "lens", [5, 7, 4], [6, 11, 5], "i");
box("lens_rim_right", "lens", [10, 7, 4], [11, 11, 5], "i");
box("lens_rim_top", "lens", [6, 10, 4], [10, 11, 5], "j");
box("lens_rim_bottom", "lens", [6, 7, 4], [10, 8, 5], "i");
box("lens_glass_rear", "lens", [6, 8, 4], [10, 10, 5], "g");
box("lens_glass_front", "lens", [6, 8, 3], [10, 10, 4], "h");
box("screen", "back", [6, 8, 10], [10, 11, 11], "n");
box("back_buttons", "back", [10, 8, 10], [11, 11, 11], "i");
box("shutter", "controls", [11, 12, 7], [12, 13, 9], "m");
box("strap_lug", "controls", [12, 8, 7], [13, 10, 9], "i");
const atlasResult = atlas.finish();
while (atlasResult.texture.height < 32) {
  atlasResult.texture.pixels.push("_".repeat(atlasResult.texture.width));
  atlasResult.texture.height++;
}
const MODEL = { id: MODEL_ID, name: MODEL_ID, groups, cubes: atlasResult.cubes };
const TEXTURES = [atlasResult.texture];
const OVERLAP_RULES = [];
const summary = runModelScript(MODEL, TEXTURES, (model, records) => {
  resolveSurfaceOverlaps(model, records, OVERLAP_RULES);
});
const edited = Cube.all.slice();
Undo.initEdit({ elements: edited, outliner: true });
const shift = [8, 4, 8];
for (const cube of Cube.all) {
  cube.moveVector(shift);
  cube.origin = [cube.origin[0] + shift[0], cube.origin[1] + shift[1], cube.origin[2] + shift[2]];
}
for (const group of Group.all) {
  group.origin = [group.origin[0] + shift[0], group.origin[1] + shift[1], group.origin[2] + shift[2]];
}
const atlasTexture = Texture.all.find(texture => texture.name === MODEL_ID);
if (atlasTexture) {
  for (const cube of Cube.all) {
    for (const face of FACE_ORDER) cube.faces[face].texture = atlasTexture.uuid;
  }
}
Undo.finishEdit(MODEL_ID + " java item pivot", { elements: edited, outliner: true });
Canvas.updateAll();
summary.javaPivot = { bodyBoundsCenter: [8, 8, 8], shift };
summary.preview = { position: [26, 22, -24], target: [8, 8, 8], projection: "perspective", fov: 32 };
return summary;
