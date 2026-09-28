const MODEL_ID = "investigation_board";
const PREFIX = MODEL_ID + "_";
const PALETTE = {
  G: "#121519",
  A: "#1E242A",
  B: "#2B333B",
  C: "#3B454F",
  D: "#4F5B66",
  E: "#66737F",
  F: "#818F9B",
  H: "#A2B0BC",
  I: "#C2CED8",
  w: "#EAF0EE",
  W: "#FBFDFC",
  s: "#D2DBD8",
  g: "#B7C3BF",
  m: "#98A6A1",
  k: "#1B2026",
  b: "#2C5FA6",
  r: "#B23B34",
  n: "#2C7A55",
  p: "#F4EEDF",
  P: "#E7DCC5",
  q: "#D2C4A6",
  t: "#FBF6EA",
  i: "#2E3339",
  j: "#5A626B",
  v: "#969EA6",
  "0": "#12171C",
  "1": "#262E35",
  "2": "#3E4852",
  "3": "#5B6670",
  "4": "#7C8791",
  "5": "#A3ADB5",
  "6": "#CCD4D9",
  y: "#F2E08A",
  Y: "#D8BC57",
  o: "#F2A9B4",
  O: "#D57F91",
  u: "#9FC6E8",
  U: "#6E9BC6",
  x: "#A8D8A8",
  X: "#72AC7C",
  R: "#C4453C",
  S: "#E2705F",
  T: "#8C2F2A",
  e: "#1D3F72",
  f: "#5E93D6",
  c: "#7E2B26",
  l: "#4A7A3C",
  z: "#87C183",
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
const BLOCK = blockUnits(1);
const BOARD_W = blockUnits(2);
const BOARD_H = blockUnits(1.5);
const HALF_W = BOARD_W / 2;
const FRAME = 2;
const OPEN_X = HALF_W - FRAME;
const OPEN_Y0 = FRAME;
const OPEN_Y1 = BOARD_H - FRAME;
const LAYER_REASON = "half unit of depth separates sheets, notes and cords so none share a plane";
const TILT_REASON = "sheet is pinned casually and sits slightly skewed";
const atlas = createTextureAtlas({
  id: MODEL_ID,
  prefix: PREFIX,
  palette: PALETTE,
  maxWidth: 128,
  maxHeight: 4096,
  padding: 1,
});
const groups = [
  { id: "frame", parent: null, origin: [0, BOARD_H / 2, BLOCK / 16], rotation: [0, 0, 0] },
  { id: "board", parent: null, origin: [0, BOARD_H / 2, BLOCK / 16], rotation: [0, 0, 0] },
  { id: "tray", parent: null, origin: [0, 2, 0], rotation: [0, 0, 0] },
  { id: "documents", parent: null, origin: [0, 12, 0], rotation: [0, 0, 0] },
  { id: "notes", parent: null, origin: [0, 12, 0], rotation: [0, 0, 0] },
  { id: "web", parent: null, origin: [0, 12, 0], rotation: [0, 0, 0] },
  { id: "pins", parent: null, origin: [0, 12, -1], rotation: [0, 0, 0] },
];
const box = (id, parent, from, to, fill) => {
  atlas.addBox({ id, parent, from, to }, { fill });
};
const sheet = (id, parent, x, y, w, h, z, fill, angle) => {
  const mask = Array.from({ length: h }, () => fill.repeat(w));
  atlas.addSprite({
    id,
    parent,
    axis: "z",
    from: [x, y, z],
    origin: [x + w / 2, y + h / 2, z],
    rotation: [0, 0, angle || 0],
    mask,
    side: "negative",
    fractionalReason: LAYER_REASON,
    angleReason: angle ? TILT_REASON : undefined,
  });
};
box("frame_top", "frame", [-HALF_W, OPEN_Y1, 0], [HALF_W, BOARD_H, 1], "C");
box("frame_bottom", "frame", [-HALF_W, 0, 0], [HALF_W, OPEN_Y0, 1], "C");
box("frame_left", "frame", [-HALF_W, OPEN_Y0, 0], [-OPEN_X, OPEN_Y1, 1], "C");
box("frame_right", "frame", [OPEN_X, OPEN_Y0, 0], [HALF_W, OPEN_Y1, 1], "C");
box("board_panel", "board", [-OPEN_X, OPEN_Y0, 0], [OPEN_X, OPEN_Y1, 1], "w");
box("marker_tray", "tray", [-11, 1, -1], [11, 3, 0], "D");
box("marker_red", "tray", [-9, 3, -1], [-4, 4, 0], "r");
box("marker_blue", "tray", [-2, 3, -1], [3, 4, 0], "b");
box("board_eraser", "tray", [5, 3, -1], [9, 5, 0], "3");
sheet("doc_report", "documents", -13, 11, 11, 9, -0.125, "p", -2);
sheet("doc_photo", "documents", -13, 6, 11, 4, -0.125, "p", 1.5);
sheet("doc_chart", "documents", 1, 12, 12, 8, -0.125, "p", 2);
sheet("doc_specimen", "documents", 1, 6, 12, 5, -0.125, "p", -1.5);
sheet("note_yellow", "notes", -8, 16, 3, 3, -0.25, "y", 4);
sheet("note_pink", "notes", 5, 16, 3, 3, -0.25, "o", -3);
sheet("note_blue", "notes", -8, 7, 3, 3, -0.25, "u", 5);
const PIN_POS = {
  a: [-10, 18],
  b: [-4, 12],
  c: [3, 18],
  d: [11, 13],
  e: [-10, 8],
  f: [11, 8],
};
for (const key of Object.keys(PIN_POS)) {
  const center = PIN_POS[key];
  box("pin_" + key, "pins", [center[0] - 1, center[1] - 1, -1], [center[0] + 1, center[1] + 1, 0], "E");
}
const WEB_X = -OPEN_X;
const WEB_Y = OPEN_Y0;
const WEB_W = OPEN_X * 2;
const WEB_H = OPEN_Y1 - OPEN_Y0;
const webMask = (edges) => {
  const grid = Array.from({ length: WEB_H }, () => new Array(WEB_W).fill("_"));
  const put = (c, r, ch) => {
    if (c >= 0 && c < WEB_W && r >= 0 && r < WEB_H) grid[r][c] = ch;
  };
  const brush = (c, r) => {
    put(c, r, "R");
  };
  for (const edge of edges) {
    const start = PIN_POS[edge[0]];
    const end = PIN_POS[edge[1]];
    const c0 = Math.floor(start[0] - WEB_X - 0.5);
    const r0 = Math.floor(WEB_Y + WEB_H - start[1] - 0.5);
    const c1 = Math.floor(end[0] - WEB_X - 0.5);
    const r1 = Math.floor(WEB_Y + WEB_H - end[1] - 0.5);
    const dc = Math.abs(c1 - c0);
    const dr = Math.abs(r1 - r0);
    const sc = c0 < c1 ? 1 : -1;
    const sr = r0 < r1 ? 1 : -1;
    let cx = c0;
    let ry = r0;
    let err = dc - dr;
    for (let guard = 0; guard < 64; guard++) {
      brush(cx, ry);
      if (cx === c1 && ry === r1) break;
      const e2 = 2 * err;
      if (e2 > -dr) {
        err -= dr;
        cx += sc;
      }
      if (e2 < dc) {
        err += dc;
        ry += sr;
      }
    }
  }
  return grid.map(row => row.join(""));
};
const EDGES = [["a", "c"], ["c", "d"], ["d", "f"], ["e", "f"], ["a", "b"], ["b", "e"], ["b", "d"]];
atlas.addSprite({
  id: "cords",
  parent: "web",
  axis: "z",
  from: [WEB_X, WEB_Y, -0.375],
  origin: [0, 12, -0.375],
  mask: webMask(EDGES),
  side: "negative",
  fractionalReason: LAYER_REASON,
});
const atlasResult = atlas.finish();
const placements = atlasResult.placements;
const EXTRA_TEXTURES = [];
const MODEL = { id: MODEL_ID, name: MODEL_ID, groups, cubes: atlasResult.cubes };
const TEXTURES = [atlasResult.texture, ...EXTRA_TEXTURES];
const OVERLAP_RULES = [];
const summary = runModelScript(MODEL, TEXTURES, (model, records) => {
  resolveSurfaceOverlaps(model, records, OVERLAP_RULES);
});
return summary;
