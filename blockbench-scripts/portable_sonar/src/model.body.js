// Portable sonar -- geometry, diffuse atlas reservation and glow mask.
//
// A hand-held depth sounder for divers, not a lamp: an enamelled instrument
// body carrying a hooded sonar display, a rubber keypad, a rotary thumbwheel
// and a chin-mounted transducer drum whose dark aperture points forward and
// down into the water.  The grip is sealed and ribbed, the back carries a
// louvred speaker, and every control is real geometry the hand can find.
//
// The Java item renderer pivots every display transform on model space
// [8, 8, 8].  For the sonar to sit in the fist and swing about the grip rather
// than an arbitrary point, the grip's centre is authored at [8, 8, 8]; the
// runtime's mandatory body-bottom centring is undone at the end of this script
// by the javaPivot pass.
//
// Front is -Z (north): the display and the transducer face the diver.  +Y is
// up, +X is the model's right.

const MODEL_ID = "portable_sonar";
const PREFIX = MODEL_ID + "_";

// Enamel, rubber, chrome, glass, keypad and instrument accents.  Six to ten
// tones per material so the texture pass can band its shading without leaving
// this list.
const PALETTE = {
  "0": "#FFFFFF",
  "1": "#F9FCFE",
  "2": "#F1F6FA",
  "3": "#E7EEF4",
  "4": "#DBE4EB",
  "5": "#CDD7E0",
  "6": "#BDC8D3",
  "7": "#AAB6C2",
  "8": "#95A2AF",
  "9": "#808D9A",
  a: "#EAEDF0",
  b: "#D7DCE1",
  c: "#C0C7CD",
  d: "#A7AFB7",
  e: "#575F68",
  f: "#474F58",
  g: "#394048",
  h: "#2B3138",
  i: "#1F242A",
  j: "#DDE3E9",
  k: "#C3CBD3",
  l: "#A9B3BD",
  m: "#8F9AA5",
  n: "#75808B",
  o: "#5B656F",
  p: "#2B6D81",
  q: "#1F5569",
  r: "#173F51",
  s: "#102D3B",
  t: "#0A1F2A",
  u: "#232930",
  v: "#2F363E",
  w: "#3C444D",
  x: "#464E57",
  y: "#373E46",
  z: "#E8A24A",
  A: "#F2C069",
  B: "#D9503F",
  C: "#6FE3F5",
  D: "#3FA9C9",
  E: "#7FD9AC",
  F: "#EFF4F8",
  G: "#9BE8DE",
  H: "#5F6A75",
};

// The display, the transducer aperture and the ranging key glow cold cyan
// while the sonar is pinging; alpha is emission strength, so the echo floor
// burns brighter than the water column.
const GLOW_PALETTE = {
  G: "#F4FFFDFF",
  g: "#9FF0E6D8",
  h: "#3FC9B899",
  H: "#8FE8FF88",
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
  { id: "grip", parent: null, origin: [8, 8, 8], rotation: [0, 0, 0] },
  { id: "housing", parent: null, origin: [8, 11, 8], rotation: [0, 0, 0] },
];

// All cuboids sit on the integer grid; every span below is integral.
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

// --- grip group: sealed heel, ribbed rubber grip, chrome collar ---
box("grip_heel", "grip", [6, 5, 6], [10, 6, 10], "i");
box("grip", "grip", [6, 6, 6], [10, 10, 10], "g");
box("grip_collar", "grip", [6, 10, 6], [10, 11, 10], "m");

// --- instrument body ---
box("chassis", "housing", [5, 11, 5], [11, 18, 11], "2");

// The faceplate is a real 2-unit-deep recess: the bezel stands proud of the
// chassis wall, the display glass sits one unit back inside it and the keys
// stand on the recess floor, so the panel genuinely reads as inset.
box("bezel_top", "housing", [5, 17, 3], [11, 18, 5], "3");
box("bezel_bottom", "housing", [5, 11, 3], [11, 12, 5], "3");
box("bezel_left", "housing", [5, 12, 3], [6, 17, 5], "3");
box("bezel_right", "housing", [10, 12, 3], [11, 17, 5], "3");
box("screen", "housing", [6, 13, 4], [10, 17, 5], "r");
box("key_a", "housing", [6, 12, 4], [7, 13, 5], "g");
box("key_b", "housing", [7, 12, 4], [8, 13, 5], "g");
box("key_c", "housing", [8, 12, 4], [9, 13, 5], "g");
box("key_d", "housing", [9, 12, 4], [10, 13, 5], "g");

// Sun hood: a three-sided visor over the display.
box("visor", "housing", [5, 17, 2], [11, 18, 3], "v");
box("visor_left", "housing", [5, 14, 2], [6, 17, 3], "v");
box("visor_right", "housing", [10, 14, 2], [11, 17, 3], "v");

// Chin-mounted transducer: the drum hangs under the front of the body, ahead
// of the grip, with the dark aperture standing proud of the drum face so it
// points forward and down into the water.
box("sonar_drum", "housing", [5, 8, 3], [11, 11, 6], "3");
box("sonar_face", "housing", [6, 8, 2], [10, 10, 3], "u");

// Working controls: a ridged thumbwheel on the left flank and a moulded rubber
// pad where the fingers close on the right.
box("thumbwheel", "housing", [4, 13, 7], [5, 15, 9], "f");
box("side_pad", "housing", [11, 12, 6], [12, 16, 10], "g");

// Rear louvred speaker: a raised bezel with a holed plate flush in its opening.
box("grille_top", "housing", [5, 17, 11], [11, 18, 12], "3");
box("grille_bottom", "housing", [5, 11, 11], [11, 12, 12], "3");
box("grille_left", "housing", [5, 12, 11], [6, 17, 12], "3");
box("grille_right", "housing", [10, 12, 11], [11, 17, 12], "3");
const speakerMask = eraseMaskRects(makeSolidMask(4, 5, "x"), [
  { x: 1, y: 1, width: 2, height: 3 },
]);
atlas.addSprite({
  id: "speaker",
  parent: "housing",
  axis: "z",
  from: [6, 12, 12],
  origin: [6, 12, 12],
  rotation: [0, 0, 0],
  side: "positive",
  mask: speakerMask,
});

const atlasResult = atlas.finish();
const placements = atlasResult.placements;

// Emissive regions, authored in the north-face (front) pixel grid of each
// cuboid.  Every glowing box is one unit deep, so its north face starts one
// pixel into the Box-UV rectangle.
const GLOW_REGIONS = [
  {
    id: "screen",
    rows: [
      "hggH",
      "gGGg",
      "gGGg",
      "GGGG",
    ],
  },
  {
    id: "sonar_face",
    rows: [
      "hGGh",
      "GhhG",
    ],
  },
  {
    id: "key_d",
    rows: ["G"],
  },
];

let glowPixels = makeTransparentPixelMap(atlasResult.texture.width, atlasResult.texture.height);
for (const region of GLOW_REGIONS) {
  const placement = placements[region.id];
  const rows = [];
  for (let y = 0; y < placement.height; y++) {
    let row = "";
    for (let x = 0; x < placement.width; x++) {
      const lx = x - 1;
      const ly = y - 1;
      const inside = lx >= 0 && ly >= 0 && ly < region.rows.length && lx < region.rows[0].length;
      row += inside ? region.rows[ly][lx] : "_";
    }
    rows.push(row);
  }
  glowPixels = writeGlowMaskRegion(glowPixels, placement, rows);
}

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
