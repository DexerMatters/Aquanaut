const MODEL_ID = "shell_camera";
const PREFIX = MODEL_ID + "_";
const INVENTORY_TEXTURE = "shell_camera_inventory";
const PALETTE = {
  a: "#F5E8D2",
  b: "#EACFB8",
  c: "#D9AFA4",
  d: "#C88F8E",
  e: "#FFF5E7",
  f: "#B77B82",
  i: "#78838B",
  j: "#929DA4",
  k: "#566169",
  l: "#3C464D",
  m: "#B7C0C5",
  n: "#252D33",
  g: "#16384B",
  h: "#2A6577",
  o: "#72B8C4",
  p: "#B9E1DF",
  r: "#8C4B42",
  s: "#C86A56",
  t: "#442F31"
};
const MATERIALS = {
  shell_core: "shell",
  shell_crown_left: "shell",
  shell_crown_center: "pearl",
  shell_crown_right: "shell",
  frame_left: "iron",
  frame_right: "iron",
  frame_bottom: "iron",
  frame_top: "iron",
  frame_back_left: "iron",
  frame_back_right: "iron",
  frame_back_bottom: "iron",
  frame_back_top: "iron",
  lens_plate: "shell",
  lens_rim_left: "iron",
  lens_rim_right: "iron",
  lens_rim_top: "iron",
  lens_rim_bottom: "iron",
  lens_glass_rear: "glass",
  lens_glass_front: "glass",
  screen: "screen",
  back_buttons: "iron",
  shutter: "steel",
  strap_lug: "iron"
};
const BASE = {
  shell: [235, 207, 184],
  pearl: [250, 236, 215],
  iron: [121, 132, 140],
  iron_dark: [78, 89, 97],
  steel: [171, 183, 190],
  glass: [35, 91, 111],
  screen: [28, 43, 52]
};
const FACE_SHADE = { up: 1.13, north: 1.05, south: 0.88, east: 0.92, west: 0.79, down: 0.65 };
const FACE_KEYS = ["north", "south", "east", "west", "up", "down"];
const records = beginTexturePaint(PREFIX);
try {
  const diffuse = records.find(record => !record.glowmask);
  const width = diffuse.width;
  const height = diffuse.height;
  const keys = Object.keys(PALETTE);
  const colors = keys.map(key => parsePaletteColor(PALETTE[key]));
  const nearest = color => {
    let best = 0;
    let distance = Infinity;
    for (let index = 0; index < colors.length; index++) {
      const dr = color[0] - colors[index][0];
      const dg = color[1] - colors[index][1];
      const db = color[2] - colors[index][2];
      const current = dr * dr + dg * dg + db * db;
      if (current < distance) {
        distance = current;
        best = index;
      }
    }
    return keys[best];
  };
  const scale = (color, factor) => color.map(channel => Math.max(0, Math.min(255, channel * factor)));
  const mix = (left, right, amount) => [0, 1, 2].map(index => left[index] + (right[index] - left[index]) * amount);
  const map = Array.from({ length: height }, () => new Array(width).fill("_"));
  const paintTexel = (local, face, x, y, w, h) => {
    const material = MATERIALS[local] || "iron";
    let color = scale(BASE[material], FACE_SHADE[face] || 1);
    const fx = w <= 1 ? 0 : x / (w - 1);
    const fy = h <= 1 ? 0 : y / (h - 1);
    if (material === "shell" || material === "pearl") {
      color = scale(color, 1.08 - fy * 0.2);
      if (fx >= 0.22 && fx <= 0.44) color = mix(color, [255, 245, 225], 0.28);
      if (w >= 5 && (Math.abs(fx - 0.18) < 0.08 || Math.abs(fx - 0.72) < 0.08)) color = scale(color, 0.9);
      if (h >= 4 && fy >= 0.58 && fy <= 0.76) color = mix(color, [214, 162, 157], 0.22);
      if (y === 0 || x === 0) color = mix(color, [255, 247, 229], 0.28);
      if (y === h - 1) color = scale(color, 0.82);
    } else if (material === "glass") {
      const dx = fx - 0.5;
      const dy = fy - 0.5;
      const radial = Math.min(1, Math.sqrt(dx * dx * 2.8 + dy * dy * 3.2));
      color = mix([85, 174, 188], [17, 47, 64], radial);
      if ((fx < 0.38 && fy < 0.42) || (x === 1 && y === 0)) color = mix(color, [193, 231, 224], 0.72);
    } else if (material === "screen") {
      color = scale(color, 1.08 - fy * 0.24);
      if (x === 0 || y === 0) color = mix(color, [75, 131, 142], 0.32);
      if (x === w - 1 || y === h - 1) color = scale(color, 0.68);
    } else {
      color = scale(color, 1.1 - fy * 0.2);
      if (x === 0 || y === 0) color = scale(color, 1.16);
      if (x === w - 1 || y === h - 1) color = scale(color, 0.82);
    }
    return color;
  };
  for (const cube of Cube.all) {
    if (typeof cube.name !== "string" || cube.name.indexOf("cube_") !== 0) continue;
    const local = cube.name.slice(5);
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
        for (let x = 0; x < w; x++) map[y0 + y][x0 + x] = nearest(paintTexel(local, face, x, y, w, h));
      }
    }
  }
  repaintMapPreservingAlpha(diffuse.ctx, width, height, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0 });
  const summary = finishTextureScript(MODEL_ID, records);
  const oldInventory = Texture.all.find(texture => texture.name === INVENTORY_TEXTURE);
  if (oldInventory) oldInventory.remove();
  const sprite = new Texture({ name: INVENTORY_TEXTURE, width: 16, height: 16 });
  sprite.canvas.width = 16;
  sprite.canvas.height = 16;
  const ctx = sprite.canvas.getContext("2d");
  ctx.imageSmoothingEnabled = false;
  const image = ctx.createImageData(16, 16);
  const set = (x, y, hex) => {
    if (x < 0 || y < 0 || x >= 16 || y >= 16) return;
    const rgba = parsePaletteColor(hex);
    const index = (y * 16 + x) * 4;
    image.data[index] = rgba[0];
    image.data[index + 1] = rgba[1];
    image.data[index + 2] = rgba[2];
    image.data[index + 3] = 255;
  };
  const polygon = (points, hex) => {
    const minX = Math.floor(Math.min(...points.map(point => point[0])));
    const maxX = Math.ceil(Math.max(...points.map(point => point[0])));
    const minY = Math.floor(Math.min(...points.map(point => point[1])));
    const maxY = Math.ceil(Math.max(...points.map(point => point[1])));
    for (let y = minY; y <= maxY; y++) {
      for (let x = minX; x <= maxX; x++) {
        let inside = false;
        for (let a = 0, b = points.length - 1; a < points.length; b = a++) {
          const xi = points[a][0], yi = points[a][1], xj = points[b][0], yj = points[b][1];
          if ((yi > y + 0.5) !== (yj > y + 0.5) && x + 0.5 < (xj - xi) * (y + 0.5 - yi) / (yj - yi) + xi) inside = !inside;
        }
        if (inside) set(x, y, hex);
      }
    }
  };
  const rect = (x, y, w, h, hex) => {
    for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) set(xx, yy, hex);
  };
  polygon([[3, 5], [10, 4], [13, 6], [13, 11], [6, 13], [3, 11]], "#78838B");
  polygon([[4, 5], [10, 5], [12, 6], [12, 10], [6, 12], [4, 11]], "#EACFB8");
  polygon([[4, 5], [6, 4], [7, 3], [10, 3], [11, 4], [11, 6], [4, 7]], "#FFF5E7");
  polygon([[12, 6], [13, 6], [13, 11], [12, 10]], "#566169");
  rect(3, 6, 1, 5, "#929DA4");
  polygon([[5, 6], [10, 5], [12, 7], [12, 10], [6, 11], [5, 9]], "#78838B");
  polygon([[6, 7], [10, 6], [11, 7], [11, 9], [7, 10], [6, 9]], "#2A6577");
  set(7, 7, "#B9E1DF");
  set(6, 8, "#72B8C4");
  set(5, 5, "#D9AFA4");
  set(5, 11, "#C88F8E");
  rect(11, 4, 2, 1, "#B7C0C5");
  ctx.putImageData(image, 0, 0);
  sprite.uv_width = 16;
  sprite.uv_height = 16;
  sprite.updateSource(sprite.canvas.toDataURL());
  Undo.initEdit({ elements: [sprite], outliner: false });
  sprite.add();
  Undo.finishEdit(MODEL_ID + " inventory sprite", { elements: [sprite], outliner: false });
  summary.derivedTextures = [INVENTORY_TEXTURE];
  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
