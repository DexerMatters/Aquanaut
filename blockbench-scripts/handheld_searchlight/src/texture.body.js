// Handheld searchlight -- diffuse atlas paint plus the derived lit atlas.
//
// The lamp is painted the way a field instrument is finished: white enamel with
// a light from the upper left, plain iron fittings, warm brass controls, a
// rubber grip and a dark glass lens.  Item models get no per-face directional
// shading from the game, so the shading is baked here by hand.
//
// The lit lens is not a second drawing: the glow mask authored in model.js
// defines the emission, and the on-state atlas is derived from it at the end of
// this script.  Both atlases share one layout, so the two Java item models can
// swap texture alone.

const MODEL_ID = "handheld_searchlight";
const PREFIX = MODEL_ID + "_";
const ON_TEXTURE = MODEL_ID + "_on";

const PALETTE = {
  w: "#F2F5F8",
  W: "#FBFDFF",
  v: "#DFE5EA",
  u: "#C9D1D8",
  t: "#B1BBC4",
  s: "#98A4AE",
  r: "#7E8B96",
  i: "#6E777F",
  I: "#838C94",
  h: "#59636C",
  H: "#464F57",
  g: "#3A424A",
  G: "#2C333A",
  f: "#20252B",
  k: "#15191D",
  j: "#3E444A",
  J: "#4E555C",
  p: "#2F343A",
  q: "#23272C",
  b: "#C98A3C",
  B: "#E0A450",
  a: "#F2C069",
  c: "#8A5A24",
  d: "#6B4419",
  l: "#20394A",
  L: "#2C4E63",
  m: "#3A657F",
  n: "#4F82A0",
  o: "#6FA6BF",
  e: "#C0503C",
};

const MATERIALS = {
  flange: "iron",
  grip: "rubber",
  collar: "enamelDark",
  trigger: "brass",
  barrel: "enamel",
  rear_cap: "iron",
  rear_nub: "ironDark",
  bezel_top: "iron",
  bezel_bottom: "iron",
  bezel_left: "iron",
  bezel_right: "iron",
  lens: "glass",
  switch_base: "iron",
  switch_knob: "brass",
};

const BASE = {
  enamel: [242, 245, 248],
  enamelDark: [201, 209, 216],
  iron: [110, 119, 127],
  ironDark: [58, 66, 74],
  rubber: [62, 68, 74],
  brass: [201, 138, 60],
  glass: [32, 57, 74],
};

// Baked key light: up is brightest, the underside darkest.
const FACE_SHADE = { up: 1.1, north: 1.03, south: 0.9, east: 0.87, west: 0.77, down: 0.64 };
const FACE_KEYS = ["north", "south", "east", "west", "up", "down"];

const records = beginTexturePaint(PREFIX);
try {
  const diffuse = records.find(record => !record.glowmask);
  const glowRecord = records.find(record => record.glowmask) || null;
  const width = diffuse.width;
  const height = diffuse.height;

  const keys = Object.keys(PALETTE);
  const colors = keys.map(key => parsePaletteColor(PALETTE[key]));
  const nearest = color => {
    let best = 0;
    let bestDistance = Infinity;
    for (let index = 0; index < colors.length; index++) {
      const dr = color[0] - colors[index][0];
      const dg = color[1] - colors[index][1];
      const db = color[2] - colors[index][2];
      const distance = dr * dr + dg * dg + db * db;
      if (distance < bestDistance) {
        bestDistance = distance;
        best = index;
      }
    }
    return keys[best];
  };
  const scale = (color, factor) => color.map(channel => Math.max(0, Math.min(255, channel * factor)));
  const mix = (a, b, t) => [0, 1, 2].map(index => a[index] + (b[index] - a[index]) * t);
  const fraction = (value, size) => size <= 1 ? 0 : value / (size - 1);
  const clamp01 = value => Math.max(0, Math.min(1, value));

  const map = [];
  for (let y = 0; y < height; y++) map.push(new Array(width).fill("_"));

  const paintTexel = (local, face, x, y, w, h) => {
    const material = MATERIALS[local] || "iron";
    let color = scale(BASE[material], FACE_SHADE[face] || 1);

    if (material === "enamel") {
      if (face === "east" || face === "west") {
        const t = fraction(y, h);
        color = scale(color, 1.05 - 0.38 * t + (t < 0.22 ? 0.07 : 0));
      }
      if ((face === "east" || face === "west" || face === "up") && (x === Math.floor(w / 3) || x === Math.floor(2 * w / 3))) {
        color = scale(color, 0.87);
      }
      if ((face === "east" || face === "west" || face === "up") && x >= w - 2) {
        color = scale(color, 0.93);
      }
    } else if (material === "rubber") {
      color = scale(color, y % 2 === 0 ? 1.1 : 0.86);
      if (face === "east" || face === "west") color = scale(color, 1.05);
      if (face === "up" || face === "down") color = scale(color, 0.95);
    } else if (material === "enamelDark") {
      color = scale(color, 1.04 - 0.16 * fraction(y, h));
      if (y === 0) color = scale(color, 1.06);
    } else if (material === "brass") {
      color = scale(color, 1.1 - 0.3 * fraction(y, h));
      if (x === Math.floor(w / 2)) color = scale(color, 1.06);
      if (x === 0 || y === 0) color = scale(color, 1.06);
    } else if (material === "glass") {
      const cx = (w - 1) / 2;
      const cy = (h - 1) / 2;
      const radial = clamp01(Math.sqrt(((x - cx) / Math.max(cx, 0.5)) ** 2 + ((y - cy) / Math.max(cy, 0.5)) ** 2));
      color = mix([78, 130, 158], [18, 38, 54], radial);
      const glintX = w * 0.32;
      const glintY = h * 0.28;
      const glint = Math.sqrt((x - glintX) ** 2 + (y - glintY) ** 2);
      if (glint < Math.max(1, w * 0.2)) color = mix(color, [154, 206, 224], 0.6 * (1 - glint / Math.max(1, w * 0.2)));
    } else {
      if (w >= 4 && h >= 3 && (x === 1 || x === w - 2) && (y === 1 || y === h - 2)) {
        color = scale(color, 0.68);
      }
    }

    if (y === 0) color = scale(color, 1.09);
    if (x === 0) color = scale(color, 1.04);
    if (y === h - 1) color = scale(color, 0.83);
    if (x === w - 1) color = scale(color, 0.9);

    return color;
  };

  for (const cube of Cube.all) {
    if (typeof cube.name !== "string" || cube.name.indexOf("cube_") !== 0) continue;
    const local = cube.name.slice("cube_".length);

    for (const face of FACE_KEYS) {
      const data = cube.faces[face];
      if (!data || !data.enabled || !Array.isArray(data.uv)) continue;
      const x0 = Math.round(Math.min(data.uv[0], data.uv[2]));
      const x1 = Math.round(Math.max(data.uv[0], data.uv[2]));
      const y0 = Math.round(Math.min(data.uv[1], data.uv[3]));
      const y1 = Math.round(Math.max(data.uv[1], data.uv[3]));
      const w = x1 - x0;
      const h = y1 - y0;
      if (w <= 0 || h <= 0) continue;

      for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
          map[y0 + y][x0 + x] = nearest(paintTexel(local, face, x, y, w, h));
        }
      }
    }
  }

  repaintMapPreservingAlpha(diffuse.ctx, width, height, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0.05 });
  const summary = finishTextureScript(MODEL_ID, records);

  if (glowRecord) {
    const existing = Texture.all.find(texture => texture.name === ON_TEXTURE);
    if (existing) existing.remove();

    const onTexture = new Texture({ name: ON_TEXTURE, width: width, height: height });
    onTexture.canvas.width = width;
    onTexture.canvas.height = height;
    const onCtx = onTexture.canvas.getContext("2d");
    onCtx.imageSmoothingEnabled = false;
    onCtx.drawImage(diffuse.texture.canvas, 0, 0);

    const glowData = glowRecord.ctx.getImageData(0, 0, width, height).data;
    const onImage = onCtx.getImageData(0, 0, width, height);
    const pixels = onImage.data;
    const snapshotLength = pixels.length;

    for (let index = 0; index < snapshotLength; index += 4) {
      const glow = glowData[index + 3];
      if (glow <= 0) continue;
      const t = glow / 255;
      pixels[index] = Math.round(pixels[index] * (1 - t) + 255 * t);
      pixels[index + 1] = Math.round(pixels[index + 1] * (1 - t) + 244 * t);
      pixels[index + 2] = Math.round(pixels[index + 2] * (1 - t) + 205 * t);
    }

    for (let y = 1; y < height - 1; y++) {
      for (let x = 1; x < width - 1; x++) {
        const index = (y * width + x) * 4;
        if (glowData[index + 3] > 0 || pixels[index + 3] === 0) continue;
        const spill = glowData[index - width * 4 + 3] > 0 || glowData[index + width * 4 + 3] > 0
          || glowData[index - 4 + 3] > 0 || glowData[index + 4 + 3] > 0;
        if (!spill) continue;
        pixels[index] = Math.round(pixels[index] * 0.74 + 255 * 0.26);
        pixels[index + 1] = Math.round(pixels[index + 1] * 0.74 + 244 * 0.26);
        pixels[index + 2] = Math.round(pixels[index + 2] * 0.74 + 205 * 0.26);
      }
    }

    onCtx.putImageData(onImage, 0, 0);
    onTexture.uv_width = onTexture.canvas.width;
    onTexture.uv_height = onTexture.canvas.height;
    onTexture.updateSource(onTexture.canvas.toDataURL());
    Undo.initEdit({ elements: [onTexture], outliner: false });
    onTexture.add();
    Undo.finishEdit(MODEL_ID + " on-state texture", { elements: [onTexture], outliner: false });
    summary.derivedTextures = [ON_TEXTURE];
  }

  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
