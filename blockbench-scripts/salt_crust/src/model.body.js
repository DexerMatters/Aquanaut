const MODEL_ID = "salt_crust";
const PREFIX = MODEL_ID + "_";
const PALETTE = {
  E: "#FFFCF6",
  e: "#F4EEE2",
  d: "#E6DCCB",
  c: "#D6C8B1",
  b: "#C2B49A",
  a: "#AB9B7F",
  "9": "#8C7E64",
  f: "#F8F0E9",
  g: "#EDDED2",
  p: "#F4CDD3",
  P: "#EBAAB6",
  o: "#DB8898",
  r: "#C56A7C",
  k: "#3A3230",
  K: "#5E534B",
  n: "#736352",
  i: "#F3BAD0",
  I: "#FFE6F1",
  h: "#C6DBE4",
  H: "#EAF5F9",
  s: "#9DB1BA",
  w: "#FFFFFF",
  u: "#B9A98E",
};
const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 128,
  maxHeight: 4096,
  padding: 1,
});

const groups = [
  { id: "body", parent: null, origin: [0, 0, 0], rotation: [0, 0, 0] },
  { id: "head", parent: "body", origin: [0, 4, -6], rotation: [0, 0, 0] },
  { id: "eye_left", parent: "head", origin: [-2, 7, -7], rotation: [0, 0, 0] },
  { id: "eye_right", parent: "head", origin: [2, 7, -7], rotation: [0, 0, 0] },
  { id: "proboscis", parent: "head", origin: [0, 3, -8], rotation: [0, 0, 0] },
  { id: "shell", parent: "body", origin: [0, 6, 1], rotation: [0, 0, 0] },
  { id: "veil_left", parent: "body", origin: [-4, 4, 0], rotation: [0, 0, 0] },
  { id: "veil_right", parent: "body", origin: [4, 4, 0], rotation: [0, 0, 0] },
];

const box = (id, parent, from, to, fill) => {
  atlas.addBox({ id, parent, from, to }, { fill });
};

box("foot_main", "body", [-4, 0, -4], [4, 3, 4], "d");
box("foot_front", "body", [-3, 0, -6], [3, 3, -4], "d");
box("foot_rear", "body", [-3, 0, 4], [3, 3, 7], "d");
box("foot_lip", "body", [-4, 1, -7], [4, 2, -6], "g");
box("body_hump", "body", [-3, 3, -4], [3, 6, 4], "c");
box("collar", "body", [-3, 3, -6], [3, 5, -4], "g");
box("siphon", "body", [-1, 4, 4], [1, 6, 7], "P");

box("head_core", "head", [-3, 3, -8], [3, 6, -6], "g");
box("head_brow", "head", [-3, 6, -8], [3, 7, -6], "p");
box("tentacle_left", "head", [-2, 3, -9], [-1, 4, -8], "g");
box("tentacle_right", "head", [1, 3, -9], [2, 4, -8], "g");

box("proboscis", "proboscis", [-1, 2, -9], [1, 4, -8], "o");

box("globe_left", "eye_left", [-3, 12, -8], [-1, 14, -6], "i");
box("globe_right", "eye_right", [1, 12, -8], [3, 14, -6], "i");

const tentacleMask = ["PP", "PP", "PP", "PP", "P_"];
atlas.addSprite({
  id: "tentacle_eye_left",
  parent: "eye_left",
  axis: "z",
  from: [-3, 7, -7],
  mask: tentacleMask,
  side: "both",
});
atlas.addSprite({
  id: "tentacle_eye_right",
  parent: "eye_right",
  axis: "z",
  from: [1, 7, -7],
  mirrorOf: "tentacle_eye_left",
});

box("shell_block", "shell", [-5, 6, -4], [5, 13, 5], "c");

box("nod_l1", "shell", [-6, 8, -1], [-4, 10, 1], "w");
box("nod_l2", "shell", [-6, 8, 2], [-4, 10, 4], "w");
box("nod_r1", "shell", [4, 8, 0], [6, 10, 2], "w");
box("nod_r2", "shell", [4, 8, -3], [6, 10, -1], "w");
box("nod_f1", "shell", [-1, 8, -5], [1, 10, -3], "w");
box("nod_f2", "shell", [-4, 8, -5], [-2, 10, -3], "w");
box("nod_f3", "shell", [2, 8, -5], [4, 10, -3], "w");
box("nod_t1", "shell", [-1, 12, -1], [1, 14, 1], "w");
box("nod_t2", "shell", [-4, 12, -2], [-2, 14, 0], "w");
box("nod_t3", "shell", [2, 12, -2], [4, 14, 0], "w");
box("nod_b1", "shell", [-1, 8, 4], [0, 10, 6], "w");

box("parapodium_left", "veil_left", [-5, 1, -5], [-4, 4, 5], "p");
box("parapodium_right", "veil_right", [4, 1, -5], [5, 4, 5], "p");

const atlasResult = atlas.finish();
const MODEL = { id: MODEL_ID, name: MODEL_ID, groups, cubes: atlasResult.cubes };
const TEXTURES = [atlasResult.texture];
const OVERLAP_RULES = [];
const summary = runModelScript(MODEL, TEXTURES, (model, records) => {
  resolveSurfaceOverlaps(model, records, OVERLAP_RULES);
});
return summary;
