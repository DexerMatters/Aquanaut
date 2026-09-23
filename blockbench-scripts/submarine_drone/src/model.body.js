const MODEL_ID = "submarine_drone";
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
};
const GLOW_PALETTE = {
  L: "#BFEFFFFF",
  Y: "#FFF0BFFF",
  W: "#FFF7E2FF",
};
const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 64,
  maxHeight: 4096,
  padding: 1,
});

// One Minecraft block is 16 Blockbench units. The drone is 8 units in beam,
// 6 units across the octagonal hull and 10 units from nose tip to shroud
// exit: 0.5 x 0.5625 x 0.625 blocks over all.
const BEAM = blockUnits(0.5);
const HULL = blockUnits(0.375);
const LENGTH = blockUnits(0.625);
const HX = BEAM / 2;
const HH = HULL / 2;
const NOSE_Z = -4;
const EXIT_Z = NOSE_Z + LENGTH;
const WALL_Z = 2;
const MOTOR_Z = 4;
const FAN_Z = 4.02;

const groups = [
  { id: "body", parent: null, origin: [0, -HX, 1], rotation: [0, 0, 0] },
  { id: "beacon", parent: "body", origin: [0, HH, 0], rotation: [0, 0, 0] },
  { id: "fin_left", parent: "body", origin: [-HH, 0, 0], rotation: [0, 0, 0] },
  { id: "fin_right", parent: "body", origin: [HH, 0, 0], rotation: [0, 0, 0] },
  { id: "fan", parent: "body", origin: [0, 0, MOTOR_Z], rotation: [0, 0, 0] },
];

// Hull: an octagonal stainless pressure shell, 6 wide by 6 tall.
atlas.addBox({
  id: "hull_mid",
  parent: "body",
  from: [-HH, -2, -2],
  to: [HH, 2, WALL_Z],
  origin: [0, 0, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "a" });
atlas.addBox({
  id: "hull_top",
  parent: "body",
  from: [-2, 2, -2],
  to: [2, HH, WALL_Z],
  origin: [0, 2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "b" });
atlas.addBox({
  id: "hull_bot",
  parent: "body",
  from: [-2, -HH, -2],
  to: [2, -2, WALL_Z],
  origin: [0, -2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "4" });

// Blunt two-step nose dome that carries the main sensor lens.
atlas.addBox({
  id: "nose_a",
  parent: "body",
  from: [-2, -2, -3],
  to: [2, 2, -2],
  origin: [0, 0, -2],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "a" });
atlas.addBox({
  id: "nose_tip",
  parent: "body",
  from: [-1, -1, NOSE_Z],
  to: [1, 1, -3],
  origin: [0, 0, -3],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "K" });

// Forward floodlight bar resting on the nose crown, flush with the hull top.
atlas.addBox({
  id: "headlight",
  parent: "body",
  from: [-1, 2, -3],
  to: [1, HH, -2],
  origin: [0, 2, -2],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "K" });

// Corner fairings square up the octagon over the rear two units so the hull
// back wall exactly matches the shroud throat.
atlas.addBox({
  id: "fair_ne",
  parent: "body",
  from: [2, 2, 0],
  to: [HH, HH, WALL_Z],
  origin: [HH, 2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "6" });
atlas.addBox({
  id: "fair_nw",
  parent: "body",
  from: [-HH, 2, 0],
  to: [-2, HH, WALL_Z],
  origin: [-HH, 2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "6" });
atlas.addBox({
  id: "fair_se",
  parent: "body",
  from: [2, -HH, 0],
  to: [HH, -2, WALL_Z],
  origin: [HH, -2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "4" });
atlas.addBox({
  id: "fair_sw",
  parent: "body",
  from: [-HH, -HH, 0],
  to: [-2, -2, WALL_Z],
  origin: [-HH, -2, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "4" });

// Ducted fan shroud: 8-unit ring with a 6-unit throat, four units deep so the
// impeller sits recessed inside a visibly deep duct.
atlas.addBox({
  id: "duct_top",
  parent: "body",
  from: [-HX, HH, WALL_Z],
  to: [HX, HX, EXIT_Z],
  origin: [0, HH, MOTOR_Z],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "6" });
atlas.addBox({
  id: "duct_bot",
  parent: "body",
  from: [-HX, -HX, WALL_Z],
  to: [HX, -HH, EXIT_Z],
  origin: [0, -HH, MOTOR_Z],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "6" });
atlas.addBox({
  id: "duct_left",
  parent: "body",
  from: [-HX, -HH, WALL_Z],
  to: [-HH, HH, EXIT_Z],
  origin: [-HH, 0, MOTOR_Z],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "5" });
atlas.addBox({
  id: "duct_right",
  parent: "body",
  from: [HH, -HH, WALL_Z],
  to: [HX, HH, EXIT_Z],
  origin: [HH, 0, MOTOR_Z],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "5" });

// Motor: a two-unit drum standing off the hull back wall into the shroud.
atlas.addBox({
  id: "motor",
  parent: "body",
  from: [-1, -1, WALL_Z],
  to: [1, 1, MOTOR_Z],
  origin: [0, 0, WALL_Z],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "K" });

// Stator vanes: zero-thickness plates tying the motor drum to the shroud.
const vaneMask = makeSolidMask(2, 2, "3");
atlas.addSprite({
  id: "vane_up",
  parent: "body",
  axis: "x",
  from: [0, 1, WALL_Z],
  mask: vaneMask,
  side: "both",
});
atlas.addSprite({
  id: "vane_dn",
  parent: "body",
  axis: "x",
  from: [0, -HH, WALL_Z],
  mask: vaneMask,
  side: "both",
});
atlas.addSprite({
  id: "vane_rt",
  parent: "body",
  axis: "y",
  from: [1, 0, WALL_Z],
  mask: vaneMask,
  side: "both",
});
atlas.addSprite({
  id: "vane_lt",
  parent: "body",
  axis: "y",
  from: [-HH, 0, WALL_Z],
  mask: vaneMask,
  side: "both",
});

// Impeller: one zero-thickness plate with a four-blade cross and a two-unit
// hub opening, flush on the motor face and spinning on the fan group axis.
const fanMask = [
  "__KK__",
  "__KK__",
  "KK__KK",
  "KK__KK",
  "__KK__",
  "__KK__",
];
atlas.addSprite({
  id: "fan",
  parent: "fan",
  axis: "z",
  from: [-HH, -HH, FAN_Z],
  mask: fanMask,
  side: "both",
  fractionalReason: "fan disc flush against the motor face without a coplanar seam",
});

// Rotating dorsal sensor pod: spins about its own vertical axis, so its base
// stays flush with the hull crown at every angle.
atlas.addBox({
  id: "beacon_pod",
  parent: "beacon",
  from: [-2, HH, -1],
  to: [2, 5, 1],
  origin: [0, HH, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "K" });

// Flank dive planes. They twist about the hull seam, so their roots never
// leave the shell surface.
atlas.addBox({
  id: "fin_left_blade",
  parent: "fin_left",
  from: [-HX, -1, -2],
  to: [-HH, 1, WALL_Z],
  origin: [-HH, 0, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
}, { fill: "7" });
atlas.addBox({
  id: "fin_right_blade",
  parent: "fin_right",
  from: [HH, -1, -2],
  to: [HX, 1, WALL_Z],
  origin: [HH, 0, 0],
  rotation: [0, 0, 0],
  inflate: 0,
  cullface: null,
  mirrorOf: "fin_left_blade",
});

const atlasResult = atlas.finish();
const placements = atlasResult.placements;

// Emissive coverage: lens iris, nose sensor, floodlight lens and pod scanner.
const BOX_DIMS = {
  nose_a: [4, 4, 1],
  nose_tip: [2, 2, 1],
  headlight: [2, 1, 1],
  beacon_pod: [4, 2, 2],
};
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
const glowRegion = (id, patterns) => {
  const slot = placements[id];
  const rows = [];
  for (let y = 0; y < slot.height; y++) rows.push(new Array(slot.width).fill("_"));
  for (const face of Object.keys(patterns)) {
    const rect = faceSlot(BOX_DIMS[id], face);
    const pattern = patterns[face];
    for (let v = 0; v < rect.h; v++) for (let u = 0; u < rect.w; u++) {
      const ch = pattern[v][u];
      if (ch !== "_") rows[rect.y + v][rect.x + u] = ch;
    }
  }
  return rows.map(row => row.join(""));
};
let glowPixels = makeTransparentPixelMap(atlasResult.texture.width, atlasResult.texture.height);
glowPixels = writeGlowMaskRegion(glowPixels, placements["nose_a"], glowRegion("nose_a", {
  north: ["____", "_LL_", "_LL_", "____"],
}));
glowPixels = writeGlowMaskRegion(glowPixels, placements["nose_tip"], glowRegion("nose_tip", {
  north: ["__", "_L"],
}));
glowPixels = writeGlowMaskRegion(glowPixels, placements["headlight"], glowRegion("headlight", {
  north: ["YW"],
}));
glowPixels = writeGlowMaskRegion(glowPixels, placements["beacon_pod"], glowRegion("beacon_pod", {
  north: ["____", "WLLW"],
}));
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
