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
const FACE_KEYS = ["north", "south", "east", "west", "up", "down"];
const SALT_HI = [255, 252, 246];
const SALT_LT = [244, 238, 226];
const SALT_MD = [230, 220, 203];
const SALT_BA = [214, 200, 177];
const SALT_SH = [194, 180, 154];
const SALT_DK = [171, 155, 127];
const SALT_BR = [140, 126, 100];
const FLESH_HI = [248, 240, 233];
const FLESH_MD = [237, 222, 210];
const PINK_LT = [244, 205, 211];
const PINK_MD = [235, 170, 182];
const PINK_DK = [219, 136, 152];
const ROSE = [197, 106, 124];
const DARK = [58, 50, 48];
const DK_VIOLET = [96, 74, 86];
const GLOWP = [243, 186, 208];
const GLOWPI = [255, 230, 241];
const HAL_BL = [198, 219, 228];
const HAL_HI = [234, 245, 249];
const HAL_SH = [157, 177, 186];
const WHITE = [255, 255, 255];
const mix = (a, b, t) => {
  const k = Math.max(0, Math.min(1, t));
  return [a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k];
};
const mul = (c, k) => [c[0] * k, c[1] * k, c[2] * k];
const noise = (x, y, s) => {
  const n = Math.sin(x * 127.1 + y * 311.7 + s * 74.7) * 43758.5453;
  return n - Math.floor(n);
};
const vfrac = (v, h) => (h <= 1 ? 1 : 1 - v / (h - 1));
const edgeShade = (c, u, v, w, h, amount) => {
  const e = Math.min(u, w - 1 - u, v, h - 1 - v);
  if (e === 0) return mix(c, SALT_BR, amount);
  if (e === 1) return mix(c, SALT_DK, amount * 0.4);
  return c;
};

const records = beginTexturePaint(PREFIX);
try {
  const diffuse = records.filter(record => !record.glowmask)[0];
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
    if (sane) cubes[local] = { faces: faces, from: cube.from.slice(), to: cube.to.slice() };
  }

  const paint = (local, face, shader) => {
    const cube = cubes[local];
    if (!cube) return;
    const r = cube.faces[face];
    for (let v = 0; v < r.h; v++) for (let u = 0; u < r.w; u++) {
      const c = shader(u, v, r.w, r.h, cube, face);
      if (c) map[r.y + v][r.x + u] = nearest(c);
    }
  };
  const paintAll = (local, shader) => {
    for (const face of FACE_KEYS) paint(local, face, shader);
  };
  const paintSides = (local, shader) => {
    for (const face of ["north", "south", "east", "west"]) paint(local, face, shader);
  };

  const footSide = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(PINK_LT, SALT_MD, 0.18 + 0.72 * t);
    const n = noise(u * 1.3, v * 1.1, 11);
    if (n > 0.74) c = mix(c, SALT_HI, 0.5);
    else if (n < 0.2) c = mix(c, FLESH_MD, 0.6);
    if (v >= h - 1) c = mix(c, PINK_MD, 0.65);
    else if (v === h - 2) c = mix(c, PINK_LT, 0.35);
    return edgeShade(c, u, v, w, h, 0.26);
  };
  const footTop = (u, v, w, h) => {
    let c = mix(SALT_MD, SALT_LT, 0.45);
    const n = noise(u * 1.9, v * 1.7, 13);
    if (n > 0.7) c = mix(c, PINK_LT, 0.4);
    else if (n < 0.24) c = mix(c, SALT_SH, 0.4);
    if (u === 0 || v === 0 || u === w - 1 || v === h - 1) c = mix(c, SALT_BR, 0.24);
    return c;
  };
  const footSole = (u, v, w, h) => {
    let c = mix(FLESH_HI, PINK_LT, 0.4);
    const d = Math.hypot(u + 0.5 - w / 2, v + 0.5 - h / 2);
    if (Math.abs(((d % 2.4) + 2.4) % 2.4 - 1.2) < 0.4) c = mix(c, PINK_MD, 0.45);
    if (w >= 4 && Math.abs(u + 0.5 - w / 2) < 1.1) c = mix(c, PINK_DK, 0.28);
    if (u === 0 || v === 0 || u === w - 1 || v === h - 1) c = mix(c, PINK_DK, 0.5);
    return c;
  };
  const mantle = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(SALT_BA, SALT_LT, 0.16 + 0.76 * t);
    const n = noise(u * 1.7, v * 1.4, 3);
    if (n > 0.66) c = mix(c, PINK_LT, 0.42);
    else if (n < 0.24) c = mix(c, SALT_DK, 0.44);
    if (v >= h - 1) c = mix(c, PINK_MD, 0.4);
    return edgeShade(c, u, v, w, h, 0.24);
  };
  const flesh = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(FLESH_MD, FLESH_HI, 0.28 + 0.68 * t);
    const n = noise(u * 2.3, v * 1.9, 5);
    if (n > 0.72) c = mix(c, PINK_LT, 0.42);
    else if (n < 0.22) c = mix(c, PINK_MD, 0.34);
    if (v >= h - 1) c = mix(c, PINK_DK, 0.32);
    return edgeShade(c, u, v, w, h, 0.2);
  };
  const saltBlock = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(mix(SALT_MD, SALT_DK, 0.32), SALT_HI, 0.1 + 0.82 * t);
    const bx = Math.floor(u / 3);
    const by = Math.floor(v / 3);
    c = mul(c, 1 + (((bx + by) % 2 === 0) ? 0.075 : -0.075));
    if (h > 3 && v % 4 === 0) c = mix(c, SALT_BR, 0.16);
    const n = noise(u * 1.9, v * 1.6, 41);
    if (n > 0.86) c = mix(c, GLOWP, 0.42);
    else if (n < 0.1) c = mix(c, SALT_HI, 0.5);
    const n2 = noise(u * 3.3, v * 2.7, 43);
    if (n2 > 0.9) c = mix(c, SALT_DK, 0.5);
    if (v >= h - 1) c = mix(c, GLOWP, 0.45);
    else if (v === h - 2 && h > 2) c = mix(c, PINK_LT, 0.2);
    return edgeShade(c, u, v, w, h, 0.26);
  };
  const saltTop = (u, v, w, h) => {
    let c = mix(SALT_LT, SALT_HI, 0.4);
    const n = noise(u * 2.1, v * 1.8, 17);
    if (n > 0.72) c = mix(c, SALT_BR, 0.24);
    else if (n < 0.26) c = mix(c, PINK_LT, 0.3);
    const n2 = noise(u * 3.7, v * 3.1, 47);
    if (n2 > 0.9) c = mix(c, GLOWP, 0.4);
    if (u === 0 || v === 0 || u === w - 1 || v === h - 1) c = mix(c, SALT_SH, 0.35);
    return c;
  };
  const crystal = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(HAL_BL, WHITE, 0.35 + 0.65 * t);
    if (w <= 2 && h <= 2) {
      if (u === w - 1 && v === h - 1) return mix(HAL_SH, WHITE, 0.5);
      if (u === 0 && v === 0) return WHITE;
      return c;
    }
    if (u === 0 && v === 0) c = WHITE;
    if (u === w - 1 && v === h - 1) c = mix(HAL_SH, WHITE, 0.3);
    if (w >= 3 && h >= 3 && u === Math.floor(w / 2) && v === Math.floor(h / 2)) c = mix(GLOWP, PINK_MD, 0.4);
    const n = noise(u * 3.1, v * 2.7, 19);
    if (n > 0.82) c = mix(c, HAL_HI, 0.6);
    return mix(c, HAL_SH, 0.14);
  };
  const eyeFront = (u, v, w, h) => {
    if (u === w - 1 && v === 0) return WHITE;
    if (u === 0 && v === h - 1) return mix(ROSE, PINK_DK, 0.5);
    return mix(DARK, DK_VIOLET, 0.25);
  };
  const eyeSide = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(PINK_MD, GLOWP, 0.35 + 0.3 * t);
    if (u === w - 1) c = mix(c, DARK, 0.5);
    if (v >= h - 1) c = mix(c, ROSE, 0.4);
    return c;
  };
  const tentacle = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(PINK_MD, PINK_LT, 0.25 + 0.62 * t);
    if (u === 0) c = mix(c, GLOWPI, 0.3);
    if (u === w - 1) c = mix(c, PINK_DK, 0.3);
    const n = noise(u * 2.9, v * 1.7, 31);
    if (n > 0.78) c = mix(c, GLOWPI, 0.5);
    else if (n < 0.18) c = mix(c, ROSE, 0.3);
    if (v >= h - 1) c = mix(c, PINK_DK, 0.35);
    return c;
  };
  const mouth = (u, v, w, h) => {
    if (v === 0 && h > 1) return mix(FLESH_HI, SALT_HI, 0.4);
    if (v >= h - 1) return mix(DARK, ROSE, 0.35);
    return mix(PINK_DK, DARK, 0.45);
  };
  const frill = (u, v, w, h) => {
    let c = mix(PINK_LT, FLESH_HI, 0.4);
    if (u % 2 === 0) c = mix(c, PINK_MD, 0.18);
    const n = noise(u * 1.6, v * 1.4, 29);
    if (n > 0.78) c = mix(c, GLOWPI, 0.4);
    else if (n < 0.2) c = mix(c, PINK_DK, 0.4);
    if (v === 0) c = mix(c, SALT_SH, 0.22);
    if (v >= h - 1) c = mix(c, PINK_DK, 0.55);
    return edgeShade(c, u, v, w, h, 0.2);
  };
  const siphon = (u, v, w, h) => {
    const t = vfrac(v, h);
    let c = mix(PINK_MD, GLOWP, 0.25 + 0.5 * t);
    if (u % 2 === 0) c = mix(c, PINK_DK, 0.2);
    if (v === 0) c = mix(c, DARK, 0.5);
    return edgeShade(c, u, v, w, h, 0.26);
  };

  for (const id of ["foot_main", "foot_front", "foot_rear"]) {
    paintSides(id, footSide);
    paint(id, "up", footTop);
    paint(id, "down", footSole);
  }
  paintAll("foot_lip", (u, v, w, h) => mix(flesh(u, v, w, h), PINK_MD, 0.3));
  paintSides("body_hump", mantle);
  paint("body_hump", "up", saltTop);
  paint("body_hump", "down", (u, v, w, h) => mix(mantle(u, v, w, h), SALT_BR, 0.3));
  paintAll("collar", flesh);
  paintAll("head_core", flesh);
  paint("head_brow", "north", (u, v, w, h) => mix(PINK_MD, GLOWP, 0.35));
  for (const face of ["south", "east", "west", "up", "down"]) paint("head_brow", face, (u, v, w, h) => mix(flesh(u, v, w, h), PINK_LT, 0.4));
  paintAll("tentacle_left", (u, v, w, h) => mix(flesh(u, v, w, h), GLOWP, 0.25));
  paintAll("tentacle_right", (u, v, w, h) => mix(flesh(u, v, w, h), GLOWP, 0.25));
  paint("proboscis", "north", mouth);
  for (const face of ["south", "east", "west", "up", "down"]) paint("proboscis", face, (u, v, w, h) => mix(PINK_DK, ROSE, 0.3 * vfrac(v, h)));
  paintAll("siphon", siphon);
  paintAll("tentacle_eye_left", tentacle);
  paintAll("tentacle_eye_right", tentacle);
  for (const face of ["south", "east", "west", "up", "down"]) {
    paint("globe_left", face, eyeSide);
    paint("globe_right", face, eyeSide);
  }
  paint("globe_left", "north", eyeFront);
  paint("globe_right", "north", eyeFront);
  paintSides("shell_block", saltBlock);
  paint("shell_block", "up", saltTop);
  paint("shell_block", "down", (u, v, w, h) => mix(saltBlock(u, v, w, h), SALT_BR, 0.32));
  for (const id of ["nod_l1", "nod_l2", "nod_r1", "nod_r2", "nod_f1", "nod_f2", "nod_f3", "nod_t1", "nod_t2", "nod_t3", "nod_b1"]) {
    paintAll(id, crystal);
  }
  paintAll("parapodium_left", frill);
  paintAll("parapodium_right", frill);

  repaintMapPreservingAlpha(diffuse.ctx, W, H, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0.1 });

  const summary = finishTextureScript(MODEL_ID, records);
  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
