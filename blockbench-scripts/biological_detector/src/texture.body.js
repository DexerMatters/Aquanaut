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
const FACE_KEYS = ["north", "south", "east", "west", "up", "down"];
const STEEL = [231, 236, 242];
const STEEL_HI = [250, 252, 254];
const STEEL_MID = [198, 206, 215];
const STEEL_LO = [156, 166, 177];
const STEEL_DK = [118, 128, 139];
const GRAPHITE = [66, 73, 81];
const DARK = [44, 49, 56];
const NIGHT = [30, 34, 39];
const CYAN = [140, 226, 242];
const CYAN_HI = [200, 244, 252];
const CYAN_DK = [86, 146, 162];
const BIO = [127, 217, 172];
const BIO_HI = [200, 246, 222];
const BIO_DK = [78, 156, 124];
const AMBER = [245, 200, 130];
const LAMP_HI = [255, 251, 233];
const ORANGE = [233, 160, 84];
const mix = (a, b, t) => {
  const k = Math.max(0, Math.min(1, t));
  return [a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k];
};
const mul = (c, k) => [c[0] * k, c[1] * k, c[2] * k];

const records = beginTexturePaint(PREFIX);
try {
  const diffuse = records.filter(record => !record.glowmask)[0];
  const glowRecord = records.filter(record => record.glowmask)[0];
  const W = diffuse.width;
  const H = diffuse.height;
  const keys = Object.keys(PALETTE);
  const cols = keys.map(key => parsePaletteColor(PALETTE[key]));
  const nearest = c => {
    let best = 0;
    let bestDistance = Infinity;
    for (let i = 0; i < cols.length; i++) {
      const dr = c[0] - cols[i][0];
      const dg = c[1] - cols[i][1];
      const db = c[2] - cols[i][2];
      const distance = dr * dr + dg * dg + db * db;
      if (distance < bestDistance) {
        bestDistance = distance;
        best = i;
      }
    }
    return keys[best];
  };
  const map = [];
  for (let y = 0; y < H; y++) map.push(new Array(W).fill("_"));

  const cubes = {};
  for (const cube of Cube.all) {
    if (typeof cube.name !== "string" || cube.name.indexOf("cube_") !== 0) continue;
    const local = cube.name.slice(5);
    const faces = {};
    let sane = true;
    for (const face of FACE_KEYS) {
      const uv = cube.faces[face].uv;
      const x = Math.round(Math.min(uv[0], uv[2]));
      const y = Math.round(Math.min(uv[1], uv[3]));
      const w = Math.round(Math.abs(uv[2] - uv[0]));
      const h = Math.round(Math.abs(uv[3] - uv[1]));
      if (w < 1 || h < 1 || x < 0 || y < 0 || x + w > W || y + h > H) sane = false;
      faces[face] = { x, y, w, h };
    }
    if (sane) cubes[local] = { faces: faces, from: cube.from.slice(), to: cube.to.slice(), plate: !!cube.plate };
  }

  const paint = (local, face, shader) => {
    const cube = cubes[local];
    if (!cube) return;
    const r = cube.faces[face];
    for (let v = 0; v < r.h; v++) for (let u = 0; u < r.w; u++) {
      const c = shader(u, v, r.w, r.h, cube);
      if (c) map[r.y + v][r.x + u] = nearest(c);
    }
  };
  const paintAll = (local, shader) => {
    for (const face of FACE_KEYS) paint(local, face, shader);
  };
  const softEdge = (c, u, v, w, h, amount) => {
    const edge = Math.min(u, w - 1 - u, v, h - 1 - v);
    if (edge === 0) return mix(c, STEEL_DK, amount);
    return c;
  };
  const darkInterior = (u, v, w, h) => softEdge(mix(NIGHT, DARK, v / Math.max(1, h - 1)), u, v, w, h, 0.2);
  const OUTER = { n: "north", s: "south", e: "east", w: "west" };
  const CORNERS = ["ne", "se", "sw", "nw"];

  // Hull bands: curved steel shell strips between the array belt and the poles.
  const band = (brightness, rivets, bio) => (u, v, w, h) => {
    let c = mix(STEEL_LO, STEEL_HI, brightness * (0.82 + 0.28 * (1 - v / Math.max(1, h - 1))));
    if (bio) c = mix(c, BIO_DK, 0.34);
    if (v === 0) c = mix(c, bio ? BIO : STEEL_DK, 0.4);
    if (u === 0 || u === w - 1) c = mix(c, STEEL_DK, 0.4);
    else if (w >= 4 && rivets && (u === 1 || u === w - 2)) c = mix(c, STEEL_MID, 0.5);
    else if (w >= 6 && u === Math.floor(w / 2)) c = mix(c, STEEL_DK, 0.22);
    return softEdge(c, u, v, w, h, 0.32);
  };
  const bandEnd = brightness => (u, v, w, h) => {
    const c = mix(STEEL_LO, STEEL_HI, brightness * (0.72 + 0.28 * (1 - v / Math.max(1, h - 1))));
    return softEdge(c, u, v, w, h, 0.34);
  };
  const bandUnder = (u, v, w, h) => mix(STEEL_DK, NIGHT, 0.55);

  // Equatorial array belt: emitter cells on a machined steel ring.
  const beltFace = (u, v, w, h) => {
    const t = 1 - v / Math.max(1, h - 1);
    if (v === 0) {
      if (u === 0 || u === w - 1) return mix(BIO_DK, STEEL_DK, 0.35);
      return mix(BIO_DK, BIO_HI, 0.3 + 0.35 * t);
    }
    if (v === h - 1) return mix(STEEL_MID, STEEL_LO, 0.5);
    if (u === 0 || u === w - 1) return mix(STEEL_DK, GRAPHITE, 0.4);
    if (u % 2 === 0) return mix(CYAN_DK, CYAN, 0.35);
    return mix(CYAN, CYAN_HI, 0.12 + 0.4 * t);
  };

  // Pole caps: flat service decks that close the shell at the poles.
  const poleSide = (u, v, w, h) => {
    let c = mix(STEEL_LO, STEEL_HI, 0.5);
    if (u === 0 || u === w - 1) c = mix(c, STEEL_DK, 0.38);
    else if (w >= 3 && u % 2 === 1) c = mix(c, STEEL_MID, 0.4);
    return softEdge(c, u, v, w, h, 0.3);
  };
  const poleDeck = bright => (u, v, w, h) => {
    const edge = u === 0 || u === w - 1 || v === 0 || v === h - 1;
    let c = mix(STEEL_LO, STEEL_HI, bright * (0.72 + 0.28 * (1 - v / Math.max(1, h - 1))));
    if (edge) c = mix(c, STEEL_DK, 0.4);
    if (v === 1 && u > 0 && u < w - 1) c = mix(c, BIO, 0.5);
    if (u === Math.floor(w / 2) && v === Math.floor(h / 2)) c = mix(c, BIO_DK, 0.6);
    if (w >= 3 && !edge && (u + v) % 2 === 1) c = mul(c, 0.96);
    return c;
  };

  // Shell layers: widest at the mid bands, stepping in toward the poles.
  const LAYERS = [
    { id: "lower_mid", brightness: 0.86, rivets: true, bio: true },
    { id: "lower_high", brightness: 0.74, rivets: true, bio: false },
    { id: "upper_mid", brightness: 0.8, rivets: true, bio: true },
    { id: "upper_high", brightness: 0.68, rivets: true, bio: false },
  ];
  for (const layer of LAYERS) {
    const outer = band(layer.brightness, layer.rivets, layer.bio);
    const end = bandEnd(layer.brightness);
    for (const side of ["n", "s", "e", "w"]) {
      const box = "ring_" + layer.id + "_" + side;
      paint(box, OUTER[side], outer);
      const ends = side === "n" || side === "s" ? ["east", "west"] : ["north", "south"];
      paint(box, ends[0], end);
      paint(box, ends[1], end);
      paint(box, side === "n" ? "south" : side === "s" ? "north" : side === "w" ? "east" : "west", darkInterior);
      paint(box, "up", bandUnder);
      paint(box, "down", bandUnder);
    }
    for (const corner of CORNERS) paintAll("ring_" + layer.id + "_" + corner, end);
  }

  // Belt cells and its corner pieces.
  for (const side of ["n", "s", "e", "w"]) {
    const box = "belt_" + side;
    paint(box, OUTER[side], beltFace);
    const ends = side === "n" || side === "s" ? ["east", "west"] : ["north", "south"];
    const beltEnd = (u, v, w, h) => softEdge(mix(STEEL_LO, STEEL_HI, 0.42 + 0.3 * (1 - v / Math.max(1, h - 1))), u, v, w, h, 0.34);
    paint(box, ends[0], beltEnd);
    paint(box, ends[1], beltEnd);
    paint(box, side === "n" ? "south" : side === "s" ? "north" : side === "w" ? "east" : "west", (u, v, w, h) => mix(NIGHT, GRAPHITE, 0.28));
    paint(box, "up", (u, v, w, h) => mix(STEEL_LO, STEEL_DK, 0.35 + 0.4 * (v / Math.max(1, h - 1))));
    paint(box, "down", (u, v, w, h) => mix(STEEL_LO, STEEL_DK, 0.35 + 0.4 * (v / Math.max(1, h - 1))));
  }
  for (const corner of CORNERS) {
    paintAll("belt_" + corner, (u, v, w, h) => {
      const edge = u === 0 || u === w - 1 || v === 0 || v === h - 1;
      if (v === 0) return mix(BIO_DK, BIO, 0.45);
      if (edge) return mix(BIO_DK, STEEL_DK, 0.5);
      return mix(CYAN_DK, CYAN, 0.45);
    });
  }

  // Pole caps.
  paint("pole_top", "up", poleDeck(0.9));
  paint("pole_top", "down", (u, v, w, h) => mix(NIGHT, DARK, 0.5));
  paint("pole_bottom", "down", poleDeck(0.55));
  paint("pole_bottom", "up", (u, v, w, h) => mix(NIGHT, DARK, 0.5));
  for (const pole of ["pole_top", "pole_bottom"]) {
    for (const face of ["north", "south", "east", "west"]) paint(pole, face, poleSide);
  }

  // Sensor core: the four-sided array drum exposed by the clamshell.
  const corePanel = (u, v, w, h) => {
    const edge = u === 0 || u === w - 1 || v === 0 || v === h - 1;
    if (edge) return mix(GRAPHITE, STEEL_DK, 0.3);
    return mix(NIGHT, GRAPHITE, 0.42);
  };
  for (const face of ["north", "south", "east", "west"]) paint("core", face, corePanel);
  paint("core", "up", (u, v, w, h) => {
    const dx = u + 0.5 - w / 2;
    const dy = v + 0.5 - h / 2;
    const r = Math.hypot(dx, dy);
    let c = mix(STEEL_LO, STEEL_HI, 0.32 + 0.5 * (1 - v / Math.max(1, h - 1)));
    if (r < 1.05) c = mix(GRAPHITE, NIGHT, 0.4);
    if (u === 1 && v === 1) c = mix(BIO_DK, c, 0.35);
    return softEdge(c, u, v, w, h, 0.28);
  });
  paint("core", "down", (u, v, w, h) => mix(NIGHT, DARK, 0.5));

  repaintMapPreservingAlpha(diffuse.ctx, W, H, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0.06 });

  if (glowRecord) {
    const image = glowRecord.ctx.getImageData(0, 0, glowRecord.width, glowRecord.height);
    for (let i = 0; i < glowRecord.width * glowRecord.height; i++) {
      if (image.data[i * 4 + 3] === 0) continue;
      image.data[i * 4] = Math.min(255, image.data[i * 4] * 1.08 + 5);
      image.data[i * 4 + 1] = Math.min(255, image.data[i * 4 + 1] * 1.07 + 5);
      image.data[i * 4 + 2] = Math.min(255, image.data[i * 4 + 2] * 1.04 + 3);
    }
    glowRecord.ctx.putImageData(image, 0, 0);
  }

  const summary = finishTextureScript(MODEL_ID, records);
  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
