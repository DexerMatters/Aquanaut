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
const FACE_KEYS = ["north", "south", "east", "west", "up", "down"];
const C = {};
for (const key of Object.keys(PALETTE)) C[key] = parsePaletteColor(PALETTE[key]).slice(0, 3);
const mix = (a, b, t) => {
  const k = Math.max(0, Math.min(1, t));
  return [a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k];
};
const scale = (c, k) => [c[0] * k, c[1] * k, c[2] * k];
const noise = (x, y, s) => {
  const n = Math.sin(x * 127.1 + y * 311.7 + s * 74.7) * 43758.5453;
  return n - Math.floor(n);
};
const hash = (text) => {
  let h = 2166136261;
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 16777619) >>> 0;
  }
  return h;
};

const STONE_RAMPS = [
  ["1", "2", "3", "4", "5", "6"],
  ["9", "a", "b", "c", "d", "e"],
  ["f", "g", "h", "i", "j", "k"],
  ["2", "3", "4", "5", "6", "7"],
  ["3", "4", "5", "6", "7", "8"],
];

const SHELL_CHIPS = [
  { color: C.s, strength: 0.9 },
  { color: C.H, strength: 0.82 },
  { color: C.t, strength: 0.78 },
  { color: C.v, strength: 0.66 },
  { color: C.D, strength: 0.6 },
  { color: C.y, strength: 0.62 },
];
const shellChip = (u, v, seed, density) => {
  const n = noise(u * 2.9 + seed * 0.7, v * 2.6 + seed * 1.1, seed + 53);
  if (n <= density) return null;
  const pick = noise(u * 5.1, v * 4.3, seed + 91);
  return SHELL_CHIPS[Math.min(SHELL_CHIPS.length - 1, Math.floor(pick * SHELL_CHIPS.length))];
};
const shellArc = (u, v, seed, salt) => {
  const cx = 0.5 + noise(seed * 0.31, salt, seed + 7) * 5;
  const cy = 0.5 + noise(seed * 0.57, salt + 1.7, seed + 29) * 3;
  const r = 1.5 + noise(seed * 0.13, salt + 5.3, seed + 61) * 2.4;
  const d = Math.sqrt((u - cx) * (u - cx) + (v - cy) * (v - cy));
  return Math.abs(d - r) < 0.58;
};

const stoneShader = (local, face, cap) => {
  const h = hash(local);
  const rich = cap || ((h >>> 23) % 5) === 0;
  const rawRamp = STONE_RAMPS[rich ? 1 : h % STONE_RAMPS.length].map(k => C[k]);
  const pale = cap ? 0.5 : rich ? 0.4 : 0;
  const ramp = rich ? rawRamp.map((c, i) => mix(c, C.t, pale - i * 0.05)) : rawRamp;
  const seed = ((h >>> 3) % 89) + 1;
  const faceLift = face === "up" ? 0.15 : face === "down" ? -0.2 : face === "north" ? 0.05 : face === "south" ? -0.04 : 0;
  const wet = ((h >>> 11) % 3) === 0;
  const density = (cap ? 0.56 : rich ? 0.72 : 0.89) + ((h >>> 17) % 5) * 0.02 + (rich && (face === "up" || face === "down") ? 0.08 : 0);
  return (u, v, w, ht) => {
    const flat = face === "up" || face === "down";
    const t = ht <= 1 || flat ? 0.55 : 1 - v / (ht - 1);
    let col = mix(ramp[4], ramp[1], 0.1 + 0.75 * t);
    const cx = w <= 1 ? 0 : Math.abs((u + 0.5) / w - 0.5) * 2;
    const cy = ht <= 1 ? 0 : Math.abs((v + 0.5) / ht - 0.5) * 2;
    const bulge = Math.max(0, 1 - Math.max(cx, cy) * 0.8 - 0.12);
    col = mix(col, ramp[0], bulge * 0.5);
    col = scale(col, 1 + faceLift);
    const edge = Math.min(u, w - 1 - u, v, ht - 1 - v);
    if (edge === 0) col = mix(col, ramp[5], 0.62);
    else if (edge === 1) col = mix(col, ramp[5], 0.2);
    const chip = shellChip(u, v, seed, density);
    if (chip) col = mix(col, chip.color, chip.strength * (rich ? 0.95 : 0.72));
    if (rich && w * ht >= 4 && shellArc(u, v, seed, 12.3)) col = mix(col, C.w, 0.5);
    if (rich && w * ht >= 8 && shellArc(u, v, seed, 4.6)) col = mix(col, C.s, 0.4);
    if (!rich && w * ht >= 10 && shellArc(u, v, seed, 6.4)) col = mix(col, C.v, 0.3);
    const n = noise(u * 1.9 + seed, v * 1.6 + seed * 0.7, seed);
    if (n > 0.84) col = mix(col, ramp[0], 0.55);
    else if (n < 0.15) col = mix(col, ramp[5], 0.5);
    const n2 = noise(u * 3.4 + seed, v * 3.1, seed + 13);
    if (n2 > 0.9) col = mix(col, ramp[1], 0.45);
    else if (n2 < 0.07) col = scale(col, 0.85);
    const n3 = noise(u * 5.3 + seed, v * 4.7, seed + 41);
    if (n3 > 0.93) col = mix(col, wet ? C.j : C.c, 0.4);
    return col;
  };
};

const ropeSide = (u, v, w, h) => {
  const band = ((u + v * 2) % 4 + 4) % 4;
  let col = band < 2 ? C.l : C.o;
  if (v === 0) col = mix(col, C.r, 0.45);
  else if (v === h - 1) col = mix(col, C.q, 0.55);
  const n = noise(u * 2.2, v * 1.8, 7);
  if (n > 0.82) col = mix(col, C.r, 0.5);
  else if (n < 0.15) col = mix(col, C.q, 0.5);
  return col;
};
const ropeTop = (u, v, w, h) => {
  const band = (v % 4 + 4) % 4;
  let col = band < 2 ? C.r : C.m;
  const n = noise(v * 2.6, u * 1.7, 17);
  if (n > 0.85) col = mix(col, C.l, 0.6);
  else if (n < 0.12) col = mix(col, C.o, 0.6);
  return col;
};
const ropeEnd = (u, v, w, h) => {
  let col = mix(C.m, C.l, v > 0 ? 0.2 : 0.5);
  const n = noise(u * 2.1, v * 2.3, 23);
  if (n > 0.8) col = mix(col, C.r, 0.5);
  else if (n < 0.2) col = mix(col, C.q, 0.5);
  return col;
};

const meshFace = (u, v, w, h) => {
  const strandU = u % 2 === 0;
  const strandV = v % 2 === 0;
  if (!strandU && !strandV) return null;
  const i = Math.floor(u / 2);
  const j = Math.floor(v / 2);
  const crossing = strandU && strandV;
  let col;
  if (crossing) col = ((i + j) % 2 === 0) ? mix(C.J, C.M, 0.45) : mix(C.K, C.L, 0.4);
  else if (strandV) col = mix(C.I, C.J, 0.5);
  else col = mix(C.J, C.K, 0.45);
  const n = noise(u * 2.4, v * 2.1, 29);
  col = mix(col, C.M, n * 0.18);
  if (n > 0.87) col = mix(col, C.L, 0.45);
  else if (n < 0.1) col = mix(col, C.M, 0.45);
  const sag = Math.min(1, Math.hypot(u - (w - 1) / 2, v - (h - 1) / 2) / (Math.max(w, h) * 0.75));
  col = scale(col, 0.9 + sag * 0.16);
  const grain = noise(u * 7.3, v * 6.1, 53);
  if (grain > 0.975) col = mix(col, C.f, 0.7);
  else if (grain > 0.93) col = mix(col, C.v, 0.65);
  const border = Math.min(u, w - 1 - u, v, h - 1 - v);
  if (border === 0) col = mix(col, C.L, 0.3);
  return col;
};

const netFace = (u, v, w, h) => {
  const hem = u === 0 || u === w - 1 || v === 0 || v === h - 1;
  const diagA = (u + v) % 4 === 0;
  const diagB = ((u - v) % 4 + 4) % 4 === 0;
  if (!hem && !diagA && !diagB) return null;
  let col;
  if (hem) col = mix(C.r, C.l, 0.35);
  else if (diagA && diagB) col = mix(C.o, C.q, 0.4);
  else if (diagA) col = mix(C.l, C.r, 0.4);
  else col = C.n;
  const n = noise(u * 2.3, v * 2.1, 37);
  if (n > 0.86) col = mix(col, C.r, 0.5);
  else if (n < 0.14) col = mix(col, C.q, 0.55);
  const wear = noise(u * 1.3 + 5, v * 1.1 + 7, 61);
  if (wear > 0.88) col = scale(col, 0.86);
  return col;
};

const records = beginTexturePaint(PREFIX);
try {
  const diffuse = records.filter(record => !record.glowmask)[0];
  const W = diffuse.width;
  const H = diffuse.height;
  const keys = Object.keys(PALETTE);
  const cols = keys.map(key => parsePaletteColor(PALETTE[key]));
  const nearest = (c) => {
    const clamped = [Math.max(0, Math.min(255, c[0])), Math.max(0, Math.min(255, c[1])), Math.max(0, Math.min(255, c[2]))];
    let best = 0;
    let bestDistance = Infinity;
    for (let i = 0; i < cols.length; i++) {
      const dr = clamped[0] - cols[i][0];
      const dg = clamped[1] - cols[i][1];
      const db = clamped[2] - cols[i][2];
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
      faces[face] = { x: x, y: y, w: w, h: h };
    }
    if (sane) cubes[local] = { faces: faces, from: cube.from.slice(), to: cube.to.slice() };
  }

  const paint = (local, face, shader) => {
    const cube = cubes[local];
    if (!cube) return;
    const r = cube.faces[face];
    for (let v = 0; v < r.h; v++) for (let u = 0; u < r.w; u++) {
      const c = shader(u, v, r.w, r.h, face);
      if (!c) continue;
      map[r.y + v][r.x + u] = nearest(c);
    }
  };
  const paintAll = (local, shader) => {
    for (const face of FACE_KEYS) paint(local, face, shader);
  };
  const atlasAlpha = diffuse.ctx.getImageData(0, 0, W, H).data;
  const alphaAt = (x, y) => (x < 0 || y < 0 || x >= W || y >= H) ? 0 : atlasAlpha[(y * W + x) * 4 + 3];
  const paintPlate = (local, shader, flipV) => {
    const cube = cubes[local];
    if (!cube) return;
    const r = cube.faces.up;
    for (let v = 0; v < r.h; v++) for (let u = 0; u < r.w; u++) {
      const edge = alphaAt(r.x + u - 1, r.y + v) === 0 || alphaAt(r.x + u + 1, r.y + v) === 0 || alphaAt(r.x + u, r.y + v - 1) === 0 || alphaAt(r.x + u, r.y + v + 1) === 0;
      const c = shader(u, v, r.w, r.h, edge);
      if (!c) continue;
      map[r.y + (flipV ? r.h - 1 - v : v)][r.x + u] = nearest(c);
    }
  };

  const stoneIds = Object.keys(cubes).filter(id => id.indexOf("stone_") === 0);
  for (const id of stoneIds) {
    const cap = id.indexOf("stone_cap_") === 0;
    for (const face of FACE_KEYS) paint(id, face, stoneShader(id, face, cap));
  }

  const bandFaces = { east: ropeSide, west: ropeSide, north: ropeSide, south: ropeSide, up: ropeTop, down: ropeEnd };
  for (const id of ["band_north", "band_south", "band_west", "band_east"]) {
    for (const face of FACE_KEYS) paint(id, face, bandFaces[face]);
  }

  paintPlate("mesh_weave", meshFace, false);

  for (const id of ["bag_north", "bag_south", "bag_west", "bag_east", "bag_floor"]) {
    paintPlate(id, netFace, false);
  }


  repaintMapPreservingAlpha(diffuse.ctx, W, H, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0.11 });

  const summary = finishTextureScript(MODEL_ID, records);
  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
