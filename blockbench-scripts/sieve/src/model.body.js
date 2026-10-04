const MODEL_ID = "sieve";
const PREFIX = MODEL_ID + "_";
const PALETTE = {
  "1": "#B4B6B0",
  "2": "#A3A6A0",
  "3": "#92958F",
  "4": "#81847E",
  "5": "#70736D",
  "6": "#5F625C",
  "7": "#4E514B",
  "8": "#3D403A",
  "9": "#ADA695",
  a: "#9C9585",
  b: "#8B8475",
  c: "#7A7365",
  d: "#696255",
  e: "#585145",
  f: "#AEB3B7",
  g: "#9DA2A6",
  h: "#8C9195",
  i: "#7B8084",
  j: "#6A6F73",
  k: "#595E62",
  l: "#AC9A5E",
  m: "#96854C",
  n: "#806F3D",
  o: "#6A5B30",
  p: "#544726",
  q: "#3E341C",
  r: "#C0AE72",
  s: "#F7F0E2",
  t: "#EBDEC9",
  u: "#DBCAAE",
  v: "#C6B092",
  w: "#AE9778",
  x: "#947E62",
  y: "#F2D2C8",
  z: "#E1B0A5",
  A: "#CB9288",
  B: "#AE786E",
  C: "#E2E8E4",
  D: "#CBD5D0",
  E: "#AFBAB5",
  F: "#93A09B",
  G: "#3A3428",
  H: "#FFFFFF",
  I: "#937F47",
  J: "#7A6838",
  K: "#61512B",
  L: "#4A3D1F",
  M: "#A89454",
};
if (typeof Cube !== "undefined" && Cube.all && typeof Group !== "undefined" && Group.all) {
  const staleCubes = Cube.all.filter(item => typeof item.name === "string" && item.name.indexOf("cube_") === 0);
  const staleGroups = Group.all.filter(item => typeof item.name === "string" && item.name.indexOf("group_") === 0);
  if (staleCubes.length || staleGroups.length) {
    const aspects = { elements: staleCubes, groups: staleGroups, outliner: true };
    Undo.initEdit(aspects);
    for (const item of staleCubes) item.remove();
    for (const item of staleGroups) item.remove();
    Undo.finishEdit(MODEL_ID + " preclean", aspects);
  }
}
const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 128,
  maxHeight: 4096,
  padding: 1,
});

const BASE_HALF = 7;
const BASE_TOP = 1;
const BAG_HALF = 6;
const BAG_TOP = 14;
const BAG_SIZE = BAG_HALF * 2;
const BAG_HEIGHT = BAG_TOP - BASE_TOP;
const TRAY_Y0 = 14;
const TRAY_Y1 = 16;
const FRAME_HALF = 6;
const FRAME_IN = 5;
const CORD_IN = 4;
const MESH_HALF = 4;
const MESH_Y = 15;

const groups = [
  { id: "tray", parent: null, origin: [0, 0, 0], rotation: [0, 0, 0] },
  { id: "bag", parent: null, origin: [0, 0, 0], rotation: [0, 0, 0] },
];

const stone = (id, parent, from, to, fill, reason) => {
  const spec = { id: id, parent: parent, from: from, to: to };
  if (reason) spec.fractionalReason = reason;
  atlas.addBox(spec, { fill: fill });
};

stone("stone_base_west", "bag", [-BASE_HALF, 0, -BASE_HALF], [0, BASE_TOP, BASE_HALF], "3");
stone("stone_base_east", "bag", [0, 0, -BASE_HALF], [BASE_HALF, BASE_TOP, BASE_HALF], "4");

stone("stone_tray_front_west", "tray", [-FRAME_HALF, TRAY_Y0, -FRAME_HALF], [-2, TRAY_Y1, -FRAME_IN], "3");
stone("stone_tray_front_mid", "tray", [-2, TRAY_Y0, -FRAME_HALF], [2, TRAY_Y1, -FRAME_IN], "4");
stone("stone_tray_front_east", "tray", [2, TRAY_Y0, -FRAME_HALF], [FRAME_HALF, TRAY_Y1, -FRAME_IN], "2");
stone("stone_tray_back_west", "tray", [-FRAME_HALF, TRAY_Y0, FRAME_IN], [-1, TRAY_Y1, FRAME_HALF], "4");
stone("stone_tray_back_mid", "tray", [-1, TRAY_Y0, FRAME_IN], [4, TRAY_Y1, FRAME_HALF], "3");
stone("stone_tray_back_east", "tray", [4, TRAY_Y0, FRAME_IN], [FRAME_HALF, TRAY_Y1, FRAME_HALF], "2");
stone("stone_tray_left_front", "tray", [-FRAME_HALF, TRAY_Y0, -FRAME_IN], [-FRAME_IN, TRAY_Y1, -1], "2");
stone("stone_tray_left_back", "tray", [-FRAME_HALF, TRAY_Y0, -1], [-FRAME_IN, TRAY_Y1, FRAME_IN], "3");
stone("stone_tray_right_front", "tray", [FRAME_IN, TRAY_Y0, -FRAME_IN], [FRAME_HALF, TRAY_Y1, 0], "3");
stone("stone_tray_right_back", "tray", [FRAME_IN, TRAY_Y0, 0], [FRAME_HALF, TRAY_Y1, FRAME_IN], "4");

const band = (id, from, to) => {
  atlas.addBox({ id: id, parent: "tray", from: from, to: to }, { fill: "m" });
};
band("band_north", [-FRAME_IN, TRAY_Y0, -FRAME_IN], [FRAME_IN, TRAY_Y1, -CORD_IN]);
band("band_south", [-FRAME_IN, TRAY_Y0, CORD_IN], [FRAME_IN, TRAY_Y1, FRAME_IN]);
band("band_west", [-FRAME_IN, TRAY_Y0, -CORD_IN], [-CORD_IN, TRAY_Y1, CORD_IN]);
band("band_east", [CORD_IN, TRAY_Y0, -CORD_IN], [FRAME_IN, TRAY_Y1, CORD_IN]);

const meshMask = [];
for (let v = 0; v < MESH_HALF * 2; v++) {
  let row = "";
  for (let u = 0; u < MESH_HALF * 2; u++) {
    const strandU = u % 2 === 0;
    const strandV = v % 2 === 0;
    if (strandU && strandV) row += (Math.floor(u / 2) + Math.floor(v / 2)) % 2 === 0 ? "l" : "n";
    else if (strandV) row += "l";
    else if (strandU) row += "n";
    else row += "_";
  }
  meshMask.push(row);
}
atlas.addSprite({
  id: "mesh_weave",
  parent: "tray",
  axis: "y",
  from: [-MESH_HALF, MESH_Y, -MESH_HALF],
  mask: meshMask,
  side: "both",
});

const netRows = (w, h) => {
  const rows = [];
  for (let v = 0; v < h; v++) {
    let row = "";
    for (let u = 0; u < w; u++) {
      const hem = u === 0 || u === w - 1 || v === 0 || v === h - 1;
      const knot = (u + v) % 4 === 0 && (u - v + 4) % 4 === 0;
      const strand = (u + v) % 4 === 0 || ((u - v) % 4 + 4) % 4 === 0;
      row += hem ? "r" : knot ? "q" : strand ? "l" : "_";
    }
    rows.push(row);
  }
  return rows;
};
const netMaskZ = netRows(BAG_SIZE, BAG_HEIGHT);
const netMaskX = netRows(BAG_HEIGHT, BAG_SIZE);

atlas.addSprite({
  id: "bag_north",
  parent: "bag",
  axis: "z",
  from: [-BAG_HALF, BASE_TOP, -BAG_HALF],
  mask: netMaskZ,
  side: "both",
});
atlas.addSprite({
  id: "bag_south",
  parent: "bag",
  axis: "z",
  from: [-BAG_HALF, BASE_TOP, BAG_HALF],
  mirrorOf: "bag_north",
});
atlas.addSprite({
  id: "bag_west",
  parent: "bag",
  axis: "x",
  from: [-BAG_HALF, BASE_TOP, -BAG_HALF],
  mask: netMaskX,
  side: "both",
});
atlas.addSprite({
  id: "bag_east",
  parent: "bag",
  axis: "x",
  from: [BAG_HALF, BASE_TOP, -BAG_HALF],
  mirrorOf: "bag_west",
});

const atlasResult = atlas.finish();
const EXTRA_TEXTURES = [];
const MODEL = { id: MODEL_ID, name: MODEL_ID, groups, cubes: atlasResult.cubes };
const TEXTURES = [atlasResult.texture, ...EXTRA_TEXTURES];
const OVERLAP_RULES = [];
const summary = runModelScript(MODEL, TEXTURES, (model, records) => {
  resolveSurfaceOverlaps(model, records, OVERLAP_RULES);
});
return summary;
