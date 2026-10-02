// Portable sonar -- diffuse atlas paint plus the derived active atlas.
//
// The instrument is finished the way a rugged field unit is: cool white enamel
// with a light from the upper left, a dark anodised sun hood, a recessed glass
// display, rubber keys and grips and a brushed chrome collar.  Item models get
// no per-face directional shading from the game, so the shading is baked here.
//
// The ranging state is not a second drawing: the glow mask authored in
// model.js defines the emission, and the on-state atlas is derived from it at
// the end of this script.  Both atlases share one layout, so the two Java item
// models can swap texture alone.

const MODEL_ID = "portable_sonar";
const PREFIX = MODEL_ID + "_";
const ON_TEXTURE = MODEL_ID + "_on";

const PALETTE = {
  "0": "#FFFFFF",
  "1": "#F9FCFE",
  "2": "#F1F6FA",
  "3": "#E7EEF4",
  "4": "#DBE4EB",
  "5": "#CDD7E0",
  "6": "#BDC8D3",
  "7": "#AAB6C2",
  "8": "#95A2AF",
  "9": "#808D9A",
  a: "#EAEDF0",
  b: "#D7DCE1",
  c: "#C0C7CD",
  d: "#A7AFB7",
  e: "#575F68",
  f: "#474F58",
  g: "#394048",
  h: "#2B3138",
  i: "#1F242A",
  j: "#DDE3E9",
  k: "#C3CBD3",
  l: "#A9B3BD",
  m: "#8F9AA5",
  n: "#75808B",
  o: "#5B656F",
  p: "#2B6D81",
  q: "#1F5569",
  r: "#173F51",
  s: "#102D3B",
  t: "#0A1F2A",
  u: "#232930",
  v: "#2F363E",
  w: "#3C444D",
  x: "#464E57",
  y: "#373E46",
  z: "#E8A24A",
  A: "#F2C069",
  B: "#D9503F",
  C: "#6FE3F5",
  D: "#3FA9C9",
  E: "#7FD9AC",
  F: "#EFF4F8",
  G: "#9BE8DE",
  H: "#5F6A75",
};

const MATERIALS = {
  grip_heel: "rubberDark",
  grip: "rubber",
  grip_collar: "metal",
  chassis: "enamel",
  bezel_top: "enamel",
  bezel_bottom: "enamel",
  bezel_left: "enamel",
  bezel_right: "enamel",
  screen: "glass",
  key_a: "keypad",
  key_b: "keypad",
  key_c: "keypad",
  key_d: "keypad",
  visor: "visor",
  visor_left: "visor",
  visor_right: "visor",
  sonar_drum: "enamel",
  sonar_face: "emitter",
  thumbwheel: "wheel",
  side_pad: "pad",
  grille_top: "enamel",
  grille_bottom: "enamel",
  grille_left: "enamel",
  grille_right: "enamel",
  speaker: "grille",
};

const BASE = {
  enamel: [242, 246, 250],
  rubber: [64, 70, 78],
  rubberDark: [40, 45, 51],
  metal: [178, 187, 196],
  glass: [24, 62, 80],
  keypad: [56, 62, 70],
  visor: [38, 43, 50],
  emitter: [30, 36, 43],
  grille: [80, 88, 97],
  wheel: [58, 64, 72],
  pad: [60, 66, 74],
};

// Baked key light from the upper left front: up is brightest, the underside
// darkest, the west/front faces catch the light and the east/back fall away.
const FACE_SHADE = { up: 1.12, north: 1.05, west: 0.99, south: 0.9, east: 0.87, down: 0.64 };
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
    const material = MATERIALS[local] || "enamel";
    let color = scale(BASE[material], FACE_SHADE[face] || 1);
    const t = fraction(y, h);

    if (material === "enamel") {
      color = scale(color, 1.06 - 0.22 * t + (t < 0.16 ? 0.05 : 0));
      if (w >= 6 && (x === w - 2 || x === 1)) color = scale(color, 0.88);
      if (w >= 6 && h >= 6 && face === "south" && x === Math.floor(w / 2)) color = scale(color, 0.95);
    } else if (material === "rubber") {
      color = scale(color, y % 2 === 0 ? 1.16 : 0.84);
      if (face === "up" || face === "down") color = scale(color, 0.94);
      if (y === h - 1) color = scale(color, 0.82);
    } else if (material === "rubberDark") {
      color = scale(color, 0.94 - 0.1 * t);
      if (x === 1 || x === w - 2) color = scale(color, 0.88);
    } else if (material === "metal") {
      color = scale(color, 1.06 - 0.2 * t);
      if (x % 3 === 0) color = scale(color, 1.07);
      else if (x % 3 === 2) color = scale(color, 0.94);
      if (y === 0) color = scale(color, 1.08);
    } else if (material === "glass") {
      const cx = (w - 1) / 2;
      const cy = (h - 1) / 2;
      const radial = clamp01(Math.sqrt(((x - cx) / Math.max(cx, 0.5)) ** 2 + ((y - cy) / Math.max(cy, 0.5)) ** 2));
      color = mix([54, 132, 156], [8, 26, 36], radial);
      const ringDistance = Math.abs(radial - 0.62);
      if (ringDistance < 0.12) color = scale(color, 1.16);
      if (y === Math.floor(h / 2) || x === Math.floor(w / 2)) color = scale(color, 1.12);
      const glint = Math.sqrt((x - w * 0.3) ** 2 + (y - h * 0.25) ** 2);
      if (glint < Math.max(0.9, w * 0.22)) color = mix(color, [150, 214, 232], 0.55 * (1 - glint / Math.max(0.9, w * 0.22)));
    } else if (material === "keypad") {
      if (face === "up") color = scale(color, 1.5);
      if (face === "north") color = scale(color, 1.15);
      if (face === "down") color = scale(color, 0.7);
    } else if (material === "visor") {
      color = scale(color, 1.1 - 0.24 * t);
      if (face === "up") color = scale(color, 1.25);
      if (face === "down") color = scale(color, 0.7);
    } else if (material === "emitter") {
      color = scale(color, 0.9 + (y === 0 ? 0.5 : 0));
      if (y === h - 1) color = scale(color, 0.78);
    } else if (material === "grille") {
      color = scale(color, (x + y) % 2 === 0 ? 1.14 : 0.86);
      if (x === 0 || y === 0) color = scale(color, 1.12);
      if (x === w - 1 || y === h - 1) color = scale(color, 0.86);
    } else if (material === "wheel") {
      if (x % 2 === 0) color = scale(color, 1.22);
      else color = scale(color, 0.8);
      if (face === "up") color = scale(color, 1.1);
    } else if (material === "pad") {
      color = scale(color, (x + y) % 2 === 0 ? 1.12 : 0.88);
      if (x === 0 || y === 0) color = scale(color, 1.16);
      if (x === w - 1 || y === h - 1) color = scale(color, 0.84);
    }

    if (y === 0) color = scale(color, 1.06);
    if (x === 0) color = scale(color, 1.03);
    if (y === h - 1) color = scale(color, 0.86);
    if (x === w - 1) color = scale(color, 0.9);

    // Moulded carry ribs across the top of the shell.
    if (local === "chassis" && face === "up") {
      if (y % 2 === 1) color = scale(color, 0.94);
      if (y === 1 || y === 3) color = scale(color, 0.9);
    }
    // Moulding seam and engraved rating plate on both flanks.
    if (local === "chassis" && (face === "east" || face === "west")) {
      if (y === 4) color = scale(color, 0.88);
      if (x >= 1 && x <= 4 && y >= 1 && y <= 2) color = scale(color, 0.96);
      if ((y === 1 || y === 2) && x >= 1 && x <= 4 && x % 2 === 1) color = scale(color, 0.78);
    }
    // The speaker opens onto the rear wall, so the visible patch is a dark well.
    if (local === "chassis" && face === "south" && x >= 1 && x <= 4 && y >= 1 && y <= 5) {
      color = scale(color, 0.55);
    }
    // Dark gasket band where the transducer aperture is bolted to the drum.
    if (local === "sonar_drum" && face === "north" && (x === 0 || x === w - 1 || y === h - 1)) {
      color = scale(color, 0.45);
    }
    // The radiating face of the transducer points down into the water: a dark
    // plate with one bright ring, so the drum reads as a sound head from below.
    if (local === "sonar_drum" && face === "down") {
      color = scale(color, 0.4);
      if (x === 1 || x === w - 2 || y === 1) color = scale(color, 1.6);
    }
    // Screw heads at the four corners of the faceplate bezel.
    if (face === "north" && (local === "bezel_top" || local === "bezel_bottom") && (x === 0 || x === w - 1)) {
      color = scale(color, 0.6);
    }
    if (face === "north" && (local === "bezel_left" || local === "bezel_right") && (y === 0 || y === h - 1)) {
      color = scale(color, 0.6);
    }
    // Shadowed inner walls of the display recess.
    if (local === "bezel_top" && face === "down") color = scale(color, 0.5);
    if (local === "bezel_bottom" && face === "up") color = scale(color, 0.5);
    if (local === "bezel_left" && face === "east") color = scale(color, 0.5);
    if (local === "bezel_right" && face === "west") color = scale(color, 0.5);
    // The hood shades the glass it stands over.
    if ((local === "visor_left" || local === "visor_right") && (face === "east" || face === "west")) {
      color = scale(color, 0.82);
    }

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
    const length = pixels.length;

    for (let index = 0; index < length; index += 4) {
      const alpha = glowData[index + 3];
      if (alpha <= 0) continue;
      const k = alpha / 255;
      const lift = k > 0.85 ? (k - 0.85) * 1.6 : 0;
      for (let channel = 0; channel < 3; channel++) {
        const lit = pixels[index + channel] + (glowData[index + channel] - pixels[index + channel]) * k;
        pixels[index + channel] = Math.round(lit + (255 - lit) * Math.min(1, lift));
      }
    }

    for (let y = 1; y < height - 1; y++) {
      for (let x = 1; x < width - 1; x++) {
        const index = (y * width + x) * 4;
        if (glowData[index + 3] > 0 || pixels[index + 3] === 0) continue;
        let spill = 0;
        let source = -1;
        for (const [dx, dy] of [[0, -1], [0, 1], [-1, 0], [1, 0]]) {
          const neighbour = index + (dy * width + dx) * 4;
          if (glowData[neighbour + 3] > spill) {
            spill = glowData[neighbour + 3];
            source = neighbour;
          }
        }
        if (source < 0) continue;
        const k = (spill / 255) * 0.22;
        for (let channel = 0; channel < 3; channel++) {
          pixels[index + channel] = Math.round(pixels[index + channel] + (glowData[source + channel] - pixels[index + channel]) * k);
        }
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
