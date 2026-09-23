const MODEL_ID = "biological_detector";
const PREFIX = MODEL_ID + "_";
const PALETTE = {
  "0": "#6B747E",
  "1": "#767F89",
  "2": "#828B95",
  "3": "#8E97A1",
  "4": "#9AA3AD",
  "5": "#A6AFB9",
  "6": "#B3BCC5",
  "7": "#C0C8D1",
  "8": "#CDD5DD",
  "9": "#DAE1E8",
  a: "#E6ECF2",
  b: "#F1F5F9",
  c: "#FAFCFE",
  k: "#2A2F35",
  K: "#3F464E",
  n: "#1D2126",
  o: "#E9A257",
  O: "#F2B472",
  l: "#8FE3F2",
  L: "#C4F2FB",
  y: "#FFF2CC",
  Y: "#FFFBE8",
  r: "#E4705F",
  G: "#96A2AE",
  j: "#7CC3D4",
  e: "#7FD9AC",
  E: "#C8F6DE",
  v: "#4E9C7C",
};
const GLOW_PALETTE = {
  E: "#B7F2D2FF",
  L: "#A8ECF8FF",
  l: "#8FE3F2AA",
  y: "#FFD9A0FF",
  e: "#7FD9ACAA",
};
const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 64,
  maxHeight: 4096,
  padding: 1,
});

// An all-round radar sphere. The whole model is one ball of stacked octagonal
// bands: 4 wide at the poles, 8 wide across the hull, with the two-unit array
// belt wrapped around the equator and the sensor core seated inside. The shell
// is a clamshell: on release the upper half lifts off the belt and the glowing
// core is exposed. The rest pose is the closed 0.5-block ball, one block being
// 16 units.
const groups = [
  { id: "body", parent: null, origin: [0, 0, 0], rotation: [0, 0, 0] },
  { id: "core", parent: "body", origin: [0, 4, 0], rotation: [0, 0, 0] },
  { id: "shell_upper", parent: "body", origin: [0, 4, 0], rotation: [0, 0, 0] },
];

// Equatorial phased-array belt: the widest band of the ball, an eight-box
// octagon so its outline follows the sphere.
const RING_PROFILES = {
  wide: [
    { id: "n", a: [-2, -4], b: [2, -3] },
    { id: "ne", a: [2, -3], b: [3, -2] },
    { id: "e", a: [3, -2], b: [4, 2] },
    { id: "se", a: [2, 2], b: [3, 3] },
    { id: "s", a: [-2, 3], b: [2, 4] },
    { id: "sw", a: [-3, 2], b: [-2, 3] },
    { id: "w", a: [-4, -2], b: [-3, 2] },
    { id: "nw", a: [-3, -3], b: [-2, -2] },
  ],
  small: [
    { id: "n", a: [-1, -3], b: [1, -2] },
    { id: "ne", a: [1, -2], b: [2, -1] },
    { id: "e", a: [2, -1], b: [3, 1] },
    { id: "se", a: [1, 1], b: [2, 2] },
    { id: "s", a: [-1, 2], b: [1, 3] },
    { id: "sw", a: [-2, 1], b: [-1, 2] },
    { id: "w", a: [-3, -1], b: [-2, 1] },
    { id: "nw", a: [-2, -2], b: [-1, -1] },
  ],
};
const LAYERS = [
  { half: "lower", level: "mid", profile: "wide", y: 2, height: 1, fill: "5" },
  { half: "lower", level: "high", profile: "small", y: 1, height: 1, fill: "5" },
  { half: "upper", level: "mid", profile: "wide", y: 5, height: 1, fill: "5" },
  { half: "upper", level: "high", profile: "small", y: 6, height: 1, fill: "5" },
];
for (const layer of LAYERS) {
  const parent = layer.half === "lower" ? "body" : "shell_upper";
  for (const piece of RING_PROFILES[layer.profile]) {
    atlas.addBox({
      id: "ring_" + layer.half + "_" + layer.level + "_" + piece.id,
      parent: parent,
      from: [piece.a[0], layer.y, piece.a[1]],
      to: [piece.b[0], layer.y + layer.height, piece.b[1]],
      origin: [piece.a[0], layer.y, piece.a[1]],
      rotation: [0, 0, 0],
      inflate: 0,
      cullface: null,
    }, { fill: layer.fill });
  }
}
for (const piece of RING_PROFILES.wide) {
  atlas.addBox({
    id: "belt_" + piece.id,
    parent: "body",
    from: [piece.a[0], 3, piece.a[1]],
    to: [piece.b[0], 5, piece.b[1]],
    origin: [piece.a[0], 3, piece.a[1]],
    rotation: [0, 0, 0],
    inflate: 0,
    cullface: null,
  }, { fill: "K" });
}

// Pole caps: solid three-wide decks that close the shell at the north and
// south poles, tucked inside the last ring.
atlas.addBox({
  id: "pole_bottom",
  parent: "body",
  from: [-1.5, 0, -1.5],
  to: [1.5, 1, 1.5],
  origin: [0, 0, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
  fractionalReason: "centers the odd-width pole cap on the deploy axis",
}, { fill: "6" });
atlas.addBox({
  id: "pole_top",
  parent: "shell_upper",
  from: [-1.5, 7, -1.5],
  to: [1.5, 8, 1.5],
  origin: [0, 7, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
  fractionalReason: "centers the odd-width pole cap on the deploy axis",
}, { fill: "6" });

// Sensor core: the four-sided array drum seated in the upper rings, exposed
// when the clamshell lifts.
atlas.addBox({
  id: "core",
  parent: "core",
  from: [-1.5, 2, -1.5],
  to: [1.5, 6, 1.5],
  origin: [0, 2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
  fractionalReason: "centers the odd-width sensor drum on the deploy axis",
}, { fill: "k" });

const atlasResult = atlas.finish();
const placements = atlasResult.placements;

// Emissive coverage: array belt cells, core panels, ring seam nodes and the
// pole cap beacons.
const DIMS = {
  core: [3, 4, 3],
  pole_top: [3, 1, 3],
  pole_bottom: [3, 1, 3],
  belt_n: [4, 2, 1],
  belt_s: [4, 2, 1],
  belt_e: [1, 2, 4],
  belt_w: [1, 2, 4],
  ring_lower_mid_n: [4, 1, 1],
  ring_lower_mid_s: [4, 1, 1],
  ring_lower_mid_e: [1, 1, 4],
  ring_lower_mid_w: [1, 1, 4],
  ring_upper_mid_n: [4, 1, 1],
  ring_upper_mid_s: [4, 1, 1],
  ring_upper_mid_e: [1, 1, 4],
  ring_upper_mid_w: [1, 1, 4],
  ring_lower_high_n: [2, 1, 1],
  ring_lower_high_s: [2, 1, 1],
  ring_lower_high_e: [1, 1, 2],
  ring_lower_high_w: [1, 1, 2],
  ring_upper_high_n: [2, 1, 1],
  ring_upper_high_s: [2, 1, 1],
  ring_upper_high_e: [1, 1, 2],
  ring_upper_high_w: [1, 1, 2],
};
const BOX_FACES = { n: "north", s: "south", e: "east", w: "west" };
const faceSlot = (dims, face) => {
  const w = dims[0];
  const h = dims[1];
  const d = dims[2];
  if (face === "east") return { x: 0, y: d, w: d, h: h };
  if (face === "west") return { x: d + w, y: d, w: d, h: h };
  if (face === "up") return { x: d, y: 0, w: w, h: d };
  if (face === "down") return { x: d + w, y: 0, w: w, h: d };
  if (face === "north") return { x: d, y: d, w: w, h: h };
  return { x: 2 * d + w, y: d, w: w, h: h };
};
let glowPixels = makeTransparentPixelMap(atlasResult.texture.width, atlasResult.texture.height);
const writeGlow = (id, patterns) => {
  const dims = DIMS[id];
  const slot = placements[id];
  const rows = [];
  for (let y = 0; y < slot.height; y++) rows.push(new Array(slot.width).fill("_"));
  for (const face of Object.keys(patterns)) {
    const rect = faceSlot(dims, face);
    const pattern = patterns[face];
    for (let v = 0; v < rect.h; v++) for (let u = 0; u < rect.w; u++) {
      const ch = pattern[v][u];
      if (ch !== "_") rows[rect.y + v][rect.x + u] = ch;
    }
  }
  glowPixels = writeGlowMaskRegion(glowPixels, slot, rows.map(row => row.join("")));
};
writeGlow("core", {
  north: ["L_L", "___", "___", "L_L"],
  south: ["L_L", "___", "___", "L_L"],
  east: ["L_L", "___", "___", "L_L"],
  west: ["L_L", "___", "___", "L_L"],
});
writeGlow("pole_top", { up: ["_L_", "L_L", "_L_"] });
writeGlow("pole_bottom", { down: ["_l_", "l_l", "_l_"] });
for (const side of ["n", "s", "e", "w"]) {
  writeGlow("belt_" + side, { [BOX_FACES[side]]: ["eeee", "eL_l"] });
}
for (const half of ["lower", "upper"]) {
  for (const side of ["n", "s", "e", "w"]) {
    writeGlow("ring_" + half + "_mid_" + side, { [BOX_FACES[side]]: ["L__l"] });
    writeGlow("ring_" + half + "_high_" + side, { [BOX_FACES[side]]: ["Ll"] });
  }
}
const glowmask = createGlowMaskTexture({
  source: atlasResult.texture,
  id: MODEL_ID + "_glowmask",
  prefix: PREFIX,
  palette: GLOW_PALETTE,
  pixels: glowPixels,
});
const EXTRA_TEXTURES = [glowmask];

const MODEL = { id: MODEL_ID, name: MODEL_ID, groups, cubes: atlasResult.cubes };
const TEXTURES = [atlasResult.texture, ...EXTRA_TEXTURES];
const OVERLAP_RULES = [];
const summary = runModelScript(MODEL, TEXTURES, (model, records) => {
  resolveSurfaceOverlaps(model, records, OVERLAP_RULES);
});
return summary;
