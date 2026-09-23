// Handheld searchlight -- geometry, diffuse atlas reservation and glow mask.
//
// The lamp is a field instrument, not a torch: a white enamelled barrel with a
// hooded lens at the front, plain iron fittings, a brass switch and a rubber
// grip.  It is built around the point at which a hand actually closes on it.
//
// The Java item renderer pivots every display transform on model space
// [8, 8, 8] (ItemRenderer applies the display transform, then shifts the whole
// model by -0.5 blocks).  For the lamp to sit in the fist and swing about the
// grip rather than about an arbitrary point, the grip's centre is authored at
// [8, 8, 8]; the runtime's mandatory body-bottom centring is undone at the end
// of this script by the javaPivot pass, which is the one deliberate difference
// from a Bedrock-entity model.

const MODEL_ID = "handheld_searchlight";
const PREFIX = MODEL_ID + "_";

// Enamel, iron, rubber, brass and glass.  Six tones per material so the
// texture pass can band its shading without leaving this palette.
const PALETTE = {
  w: "#F2F5F8",
  W: "#FBFDFF",
  v: "#DFE5EA",
  u: "#C9D1D8",
  t: "#B1BBC4",
  s: "#98A4AE",
  r: "#7E8B96",
  i: "#6E777F",
  I: "#838C94",
  h: "#59636C",
  H: "#464F57",
  g: "#3A424A",
  G: "#2C333A",
  f: "#20252B",
  k: "#15191D",
  j: "#3E444A",
  J: "#4E555C",
  p: "#2F343A",
  q: "#23272C",
  b: "#C98A3C",
  B: "#E0A450",
  a: "#F2C069",
  c: "#8A5A24",
  d: "#6B4419",
  l: "#20394A",
  L: "#2C4E63",
  m: "#3A657F",
  n: "#4F82A0",
  o: "#6FA6BF",
  e: "#C0503C",
};

// The glass lens glows warm amber through the mask; alpha is emission strength.
const GLOW_PALETTE = {
  Z: "#FFF8E2FF",
  z: "#FFE5ACFF",
  y: "#FFC46CCC",
  x: "#F09A4477",
};

const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 64,
  maxHeight: 64,
  padding: 1,
});

const groups = [
  { id: "housing", parent: null, origin: [8, 12, 3], rotation: [0, 0, 0] },
  { id: "handle", parent: null, origin: [8, 8, 8], rotation: [0, 0, 0] },
];

// Cubes are authored in Java item space (0..16 box, lens towards -Z) with the
// grip centred on [8, 8, 8].  All origins sit on the integer grid.
const box = (id, parent, from, to, fill) => atlas.addBox({
  id: id,
  parent: parent,
  from: from,
  to: to,
  origin: from.slice(),
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: fill });

// --- handle group: rubber grip, iron flange and collar, brass trigger ---
box("flange", "handle", [6, 5, 7], [10, 6, 9], "i");
box("grip", "handle", [6, 6, 7], [10, 10, 9], "j");
box("collar", "handle", [6, 10, 7], [10, 12, 9], "t");
box("trigger", "handle", [7, 10, 5], [9, 12, 7], "b");

// --- housing group: enamelled barrel with an iron rear cap and a hooded lens ---
box("barrel", "housing", [5, 12, -2], [11, 16, 8], "w");
box("rear_cap", "housing", [5, 12, 8], [11, 16, 10], "i");
box("rear_nub", "housing", [6, 13, 10], [10, 15, 11], "g");
box("bezel_top", "housing", [4, 16, -5], [12, 17, -2], "h");
box("bezel_bottom", "housing", [4, 11, -5], [12, 12, -2], "h");
box("bezel_left", "housing", [4, 12, -5], [5, 16, -2], "h");
box("bezel_right", "housing", [11, 12, -5], [12, 16, -2], "h");
box("lens", "housing", [5, 12, -3], [11, 16, -2], "l");
box("switch_base", "housing", [7, 16, 1], [9, 17, 5], "g");
box("switch_knob", "housing", [7, 17, 2], [9, 18, 4], "b");

const atlasResult = atlas.finish();
const placements = atlasResult.placements;

// The lens is the only emissive surface.  Its north face sits d units into the
// lens box rectangle of the atlas; the mask fades from the filament at the
// centre to the cold rim, so the burning sprite reads as a lit lens rather
// than a painted yellow square.
const LENS_W = 6;
const LENS_H = 4;
const LENS_D = 1;
let glowPixels = makeTransparentPixelMap(atlasResult.texture.width, atlasResult.texture.height);
const lensPlacement = placements["lens"];
const glowRows = [];

for (let y = 0; y < lensPlacement.height; y++) {
  let row = "";

  for (let x = 0; x < lensPlacement.width; x++) {
    const lx = x - LENS_D;
    const ly = y - LENS_D;
    let ch = "_";

    if (lx >= 0 && lx < LENS_W && ly >= 0 && ly < LENS_H) {
      const dx = lx - (LENS_W - 1) / 2;
      const dy = ly - (LENS_H - 1) / 2;
      const distance = Math.sqrt(dx * dx + dy * dy) / 2.6;
      ch = distance < 0.35 ? "Z" : distance < 0.65 ? "z" : distance < 0.9 ? "y" : "x";
    }

    row += ch;
  }

  glowRows.push(row);
}

glowPixels = writeGlowMaskRegion(glowPixels, lensPlacement, glowRows);
const glowmask = createGlowMaskTexture({
  source: atlasResult.texture,
  id: MODEL_ID + "_glowmask",
  prefix: PREFIX,
  palette: GLOW_PALETTE,
  pixels: glowPixels,
});
const EXTRA_TEXTURES = [glowmask];

const MODEL = { id: MODEL_ID, name: MODEL_ID, groups: groups, cubes: atlasResult.cubes };
const TEXTURES = [atlasResult.texture, ...EXTRA_TEXTURES];
const OVERLAP_RULES = [];
const summary = runModelScript(MODEL, TEXTURES, (model, records) => {
  resolveSurfaceOverlaps(model, records, OVERLAP_RULES);
});

// Java item convention: put the grip's centre back on the [8, 8, 8] display
// pivot the item renderer rotates around, undoing the runtime's entity-style
// bottom centring by one rigid translation of every live node.
const gripCube = Cube.all.find(cube => cube.name === "cube_grip");
if (gripCube) {
  const centre = [
    (gripCube.from[0] + gripCube.to[0]) / 2,
    (gripCube.from[1] + gripCube.to[1]) / 2,
    (gripCube.from[2] + gripCube.to[2]) / 2,
  ];
  const shift = [8 - centre[0], 8 - centre[1], 8 - centre[2]];
  const moved = value => [value[0] + shift[0], value[1] + shift[1], value[2] + shift[2]];
  const edited = Cube.all.slice();
  Undo.initEdit({ elements: edited, outliner: true });
  for (const cube of Cube.all) {
    cube.moveVector(shift);
    cube.origin = moved(cube.origin);
  }
  for (const group of Group.all) {
    group.origin = moved(group.origin);
  }
  // Blockbench 5 keeps a face's texture as the texture UUID; the shared
  // runtime's constructor form is a no-op there, so bind the atlas explicitly.
  const atlasTexture = Texture.all.find(texture => texture.name === MODEL_ID);
  if (atlasTexture) {
    for (const cube of Cube.all) {
      for (const face of FACE_ORDER) {
        cube.faces[face].texture = atlasTexture.uuid;
      }
    }
  }
  Undo.finishEdit(MODEL_ID + " java pivot", { elements: edited, outliner: true });
  Canvas.updateAll();
  summary.javaPivot = { gripCentre: [8, 8, 8], shift: shift };
}

return summary;
