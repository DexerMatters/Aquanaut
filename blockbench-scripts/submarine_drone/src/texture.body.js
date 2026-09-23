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
const LAMP = [255, 240, 189];
const LAMP_HI = [255, 251, 233];
const ORANGE = [233, 160, 84];
const ORANGE_HI = [242, 182, 116];
const RED = [228, 112, 95];
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
    const edge = Math.min(u, w - 1 - u, h - 1 - v);
    if (edge === 0) return mix(c, STEEL_DK, amount);
    return c;
  };
  const vertical = (top, bottom, bias) => (u, v, w, h) => {
    const t = h <= 1 ? 1 : 1 - v / (h - 1);
    return softEdge(mix(bottom, top, Math.pow(t, bias === undefined ? 1 : bias)), u, v, w, h, 0.3);
  };

  // ---- Hull shell ---------------------------------------------------------
  const flank = (u, v, w, h) => {
    const t = h <= 1 ? 1 : 1 - v / (h - 1);
    let c = mix(STEEL_LO, STEEL_HI, 0.34 + 0.62 * t);
    if (w >= 4 && Math.abs(u - (w - 1) / 2) < 0.6) c = mul(c, 0.965);
    if (h >= 4 && v === 2) c = ORANGE;
    else if (h >= 4 && v === 3) c = mix(c, ORANGE, 0.24);
    else if (h >= 4 && v === 1) c = mix(c, ORANGE, 0.1);
    return softEdge(c, u, v, w, h, 0.32);
  };
  paint("hull_mid", "east", flank);
  paint("hull_mid", "west", flank);
  paint("hull_mid", "up", vertical(STEEL_HI, STEEL_MID, 0.85));
  paint("hull_mid", "down", vertical(STEEL_MID, STEEL_DK, 1.1));
  paint("hull_mid", "north", vertical(STEEL_DK, NIGHT, 1.2));

  const fanCube = cubes["fan"];
  const axis = fanCube
    ? { x: (fanCube.from[0] + fanCube.to[0]) / 2, y: (fanCube.from[1] + fanCube.to[1]) / 2 }
    : { x: 0, y: 0 };

  const bulkhead = (u, v, w, h, cube) => {
    const x = cube.from[0] + u + 0.5 - axis.x;
    const y = cube.to[1] - (v + 0.5) - axis.y;
    const r = Math.min(1, Math.hypot(x, y) / 3.1);
    let c = mix(NIGHT, STEEL_DK, 0.12 + 0.88 * r);
    if (r < 0.5) c = mix(c, NIGHT, 0.5);
    return softEdge(c, u, v, w, h, 0.2);
  };
  paint("hull_mid", "south", bulkhead);
  paint("hull_top", "south", bulkhead);
  paint("hull_bot", "south", bulkhead);
  paint("fair_ne", "south", bulkhead);
  paint("fair_nw", "south", bulkhead);
  paint("fair_se", "south", bulkhead);
  paint("fair_sw", "south", bulkhead);

  paint("hull_top", "up", vertical(STEEL_HI, STEEL, 0.8));
  paint("hull_top", "east", vertical(STEEL_HI, STEEL_MID, 1));
  paint("hull_top", "west", vertical(STEEL_HI, STEEL_MID, 1));
  paint("hull_top", "down", vertical(STEEL_DK, NIGHT, 1));
  paint("hull_top", "north", vertical(STEEL_MID, NIGHT, 1));
  paint("hull_bot", "down", (u, v, w, h) => {
    let c = mix(STEEL_LO, STEEL_DK, 0.2 + 0.6 * (1 - v / Math.max(1, h - 1)));
    if (w >= 4 && Math.abs(u - (w - 1) / 2) < 1.1) c = mul(c, 0.9);
    return softEdge(c, u, v, w, h, 0.3);
  });
  paint("hull_bot", "east", vertical(STEEL_MID, STEEL_DK, 1));
  paint("hull_bot", "west", vertical(STEEL_MID, STEEL_DK, 1));
  paint("hull_bot", "up", vertical(STEEL_DK, NIGHT, 1));
  paint("hull_bot", "north", vertical(STEEL_MID, NIGHT, 1));

  for (const fair of ["fair_ne", "fair_nw", "fair_se", "fair_sw"]) {
    paint(fair, "east", vertical(STEEL_HI, STEEL_LO, 1));
    paint(fair, "west", vertical(STEEL_HI, STEEL_LO, 1));
    paint(fair, "up", vertical(STEEL_HI, STEEL_MID, 0.9));
    paint(fair, "down", vertical(STEEL_MID, STEEL_DK, 1));
  }

  // ---- Nose, lens and floodlight ------------------------------------------
  const lens = (u, v, w, h) => {
    const dx = u + 0.5 - w / 2;
    const dy = v + 0.5 - h / 2;
    const r = Math.hypot(dx, dy) / (Math.min(w, h) / 2);
    if (r <= 0.62) return mix(CYAN, CYAN_HI, 1 - r / 0.62);
    if (r <= 1) return mix(NIGHT, DARK, (r - 0.62) / 0.38);
    return mix(STEEL_HI, STEEL_LO, Math.min(1, (r - 1) * 1.6));
  };
  paint("nose_a", "north", lens);
  paint("nose_a", "east", vertical(STEEL_HI, STEEL_DK, 1));
  paint("nose_a", "west", vertical(STEEL_HI, STEEL_DK, 1));
  paint("nose_a", "up", vertical(STEEL_HI, STEEL, 0.9));
  paint("nose_a", "down", vertical(STEEL_MID, STEEL_DK, 1));
  paint("nose_a", "south", vertical(STEEL_DK, NIGHT, 1.2));
  paint("nose_tip", "north", (u, v, w, h) => {
    if (u === w - 1 && v === h - 1) return CYAN_HI;
    return mix(NIGHT, GRAPHITE, 0.15 + 0.5 * ((u + v) / Math.max(1, w + h - 2)));
  });
  for (const face of ["east", "west", "up", "down", "south"]) {
    paint("nose_tip", face, vertical(GRAPHITE, NIGHT, 1));
  }
  paint("headlight", "north", (u, v, w, h) => mix(LAMP, LAMP_HI, u / Math.max(1, w - 1)));
  paint("headlight", "south", vertical(GRAPHITE, NIGHT, 1));
  paint("headlight", "east", vertical(GRAPHITE, DARK, 1));
  paint("headlight", "west", vertical(GRAPHITE, DARK, 1));
  paint("headlight", "up", vertical(STEEL_HI, STEEL_MID, 0.9));
  paint("headlight", "down", vertical(GRAPHITE, NIGHT, 1));

  // ---- Shroud ring --------------------------------------------------------
  const ringDepth = t => 1 - Math.abs(2 * t - 1);
  const rimEdge = (c, u, v, w, h, amount) => {
    const edge = Math.min(u, w - 1 - u, v, h - 1 - v);
    return edge === 0 ? mix(c, STEEL_DK, amount) : c;
  };
  // Outer shroud walls: the atlas v runs down the model y, u runs along depth.
  const shroudSide = (u, v, w, h) => {
    const t = h <= 1 ? 1 : 1 - v / (h - 1);
    let c = mix(STEEL_LO, STEEL_HI, 0.14 + 0.86 * t);
    const depth = w <= 1 ? 1 : ringDepth((u + 0.5) / w);
    c = mix(c, STEEL_DK, 0.36 * (1 - depth));
    if (w >= 3 && (u === 0 || u === w - 1)) c = mix(c, ORANGE, 0.62);
    if (w >= 4 && (u === 1 || u === w - 2)) c = mix(c, STEEL_MID, 0.3);
    return softEdge(c, u, v, w, h, 0.24);
  };
  // Shroud crown and belly: the atlas v runs along depth, u spans the beam.
  const shroudTop = (u, v, w, h) => {
    const along = w <= 1 ? 1 : 1 - Math.abs((u + 0.5) / w * 2 - 1) * 0.5;
    let c = mix(STEEL_MID, STEEL_HI, 0.35 + 0.6 * along);
    const depth = h <= 1 ? 1 : ringDepth((v + 0.5) / h);
    c = mix(c, STEEL_DK, 0.34 * (1 - depth));
    if (h >= 3 && (v === 0 || v === h - 1)) c = mix(c, ORANGE, 0.62);
    if (h >= 4 && (v === 1 || v === h - 2)) c = mix(c, STEEL_MID, 0.3);
    return rimEdge(c, u, v, w, h, 0.24);
  };
  const throatInner = (u, v, w, h) => {
    const t = h <= 1 ? 1 : 1 - v / (h - 1);
    return softEdge(mix(NIGHT, DARK, 0.3 + 0.45 * t), u, v, w, h, 0.16);
  };
  paint("duct_top", "up", shroudTop);
  paint("duct_top", "down", throatInner);
  paint("duct_top", "east", shroudSide);
  paint("duct_top", "west", shroudSide);
  paint("duct_top", "north", vertical(GRAPHITE, NIGHT, 1));
  paint("duct_top", "south", vertical(DARK, NIGHT, 1));
  paint("duct_bot", "down", shroudTop);
  paint("duct_bot", "up", throatInner);
  paint("duct_bot", "east", shroudSide);
  paint("duct_bot", "west", shroudSide);
  paint("duct_bot", "north", vertical(GRAPHITE, NIGHT, 1));
  paint("duct_bot", "south", vertical(DARK, NIGHT, 1));
  paint("duct_left", "west", shroudSide);
  paint("duct_left", "east", throatInner);
  paint("duct_left", "north", vertical(GRAPHITE, NIGHT, 1));
  paint("duct_left", "south", vertical(DARK, NIGHT, 1));
  paint("duct_left", "up", shroudTop);
  paint("duct_left", "down", shroudTop);
  paint("duct_right", "east", shroudSide);
  paint("duct_right", "west", throatInner);
  paint("duct_right", "north", vertical(GRAPHITE, NIGHT, 1));
  paint("duct_right", "south", vertical(DARK, NIGHT, 1));
  paint("duct_right", "up", shroudTop);
  paint("duct_right", "down", shroudTop);

  // ---- Motor drum and stator vanes ---------------------------------------
  paint("motor", "up", vertical(mix(STEEL_HI, GRAPHITE, 0.55), GRAPHITE, 0.9));
  paint("motor", "down", vertical(GRAPHITE, NIGHT, 1));
  paint("motor", "east", vertical(GRAPHITE, DARK, 1));
  paint("motor", "west", vertical(GRAPHITE, DARK, 1));
  paint("motor", "north", vertical(DARK, NIGHT, 1));
  paint("motor", "south", (u, v, w, h) => {
    const dx = u + 0.5 - w / 2;
    const dy = v + 0.5 - h / 2;
    const r = Math.hypot(dx, dy) / (Math.min(w, h) / 2);
    return mix(mix(GRAPHITE, DARK, 0.5), NIGHT, Math.min(1, r));
  });
  const vaneShader = vertical(STEEL_DK, NIGHT, 1);
  for (const vane of ["vane_up", "vane_dn", "vane_rt", "vane_lt"]) paint(vane, "north", vaneShader);

  // ---- Fan: one plate, four blades, bright tips ---------------------------
  paint("fan", "south", (u, v, w, h, cube) => {
    const x = cube.from[0] + u + 0.5 - axis.x;
    const y = cube.to[1] - (v + 0.5) - axis.y;
    const r = Math.hypot(x, y);
    let c = mix(NIGHT, GRAPHITE, 0.14 + 0.8 * Math.min(1, r / 2.6));
    if (r > 1.95) c = mix(c, STEEL_HI, 0.88);
    else if (r > 1.3) c = mix(c, STEEL_MID, 0.18);
    return c;
  });

  // ---- Sensor pod --------------------------------------------------------
  paint("beacon_pod", "north", (u, v, w, h) => {
    if (v === h - 1) {
      const middle = u > 0 && u < w - 1;
      return middle ? mix(CYAN, CYAN_HI, 0.78) : mix(CYAN, [86, 146, 162], 0.45);
    }
    return mix(GRAPHITE, NIGHT, 1 - u / Math.max(1, w - 1));
  });
  paint("beacon_pod", "south", (u, v, w, h) => {
    if (v === h - 1) return u === w - 2 ? RED : mix(DARK, NIGHT, 0.6);
    return mix(GRAPHITE, NIGHT, 0.4 + 0.6 * (u / Math.max(1, w - 1)));
  });
  paint("beacon_pod", "up", vertical(STEEL_HI, STEEL_MID, 0.85));
  paint("beacon_pod", "down", vertical(DARK, NIGHT, 1));
  paint("beacon_pod", "east", (u, v, w, h) => vertical(GRAPHITE, NIGHT, 1)(u, v, w, h));
  paint("beacon_pod", "west", (u, v, w, h) => vertical(GRAPHITE, NIGHT, 1)(u, v, w, h));

  // ---- Dive planes -------------------------------------------------------
  paint("fin_left_blade", "west", (u, v, w, h) => {
    let c = mix(STEEL_LO, STEEL_HI, 0.85 - (v / Math.max(1, h - 1)) * 0.7);
    const along = w <= 1 ? 0 : u / (w - 1);
    c = mix(c, STEEL_LO, 0.4 * along);
    if (h >= 2 && v === h - 1) {
      c = mix(c, STEEL_DK, 0.25);
      if (u === 0) c = ORANGE_HI;
    }
    if (u === w - 1) c = mix(c, STEEL_DK, 0.18);
    return c;
  });
  paint("fin_left_blade", "east", vertical(STEEL_MID, STEEL_DK, 1));
  paint("fin_left_blade", "up", (u, v, w, h) => {
    const t = h <= 1 ? 0.5 : Math.abs(v / (h - 1) - 0.5) * 2;
    let c = mix(STEEL_HI, STEEL_MID, t * 0.75);
    if (v === 0) c = mix(c, ORANGE_HI, 0.35);
    return c;
  });
  paint("fin_left_blade", "down", (u, v, w, h) => {
    const t = h <= 1 ? 0.5 : Math.abs(v / (h - 1) - 0.5) * 2;
    let c = mix(STEEL_LO, STEEL_DK, 0.3 + t * 0.6);
    if (v === 0) c = mix(c, ORANGE, 0.3);
    return c;
  });
  paint("fin_left_blade", "north", vertical(STEEL_HI, STEEL_MID, 1));
  paint("fin_left_blade", "south", vertical(STEEL_MID, STEEL_DK, 1));

  repaintMapPreservingAlpha(diffuse.ctx, W, H, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0.02 });

  if (glowRecord) {
    const image = glowRecord.ctx.getImageData(0, 0, glowRecord.width, glowRecord.height);
    for (let i = 0; i < glowRecord.width * glowRecord.height; i++) {
      if (image.data[i * 4 + 3] === 0) continue;
      image.data[i * 4] = Math.min(255, image.data[i * 4] * 1.1 + 6);
      image.data[i * 4 + 1] = Math.min(255, image.data[i * 4 + 1] * 1.08 + 6);
      image.data[i * 4 + 2] = Math.min(255, image.data[i * 4 + 2] * 1.05 + 4);
    }
    glowRecord.ctx.putImageData(image, 0, 0);
  }

  const summary = finishTextureScript(MODEL_ID, records);
  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
