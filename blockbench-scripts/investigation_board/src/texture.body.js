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
const FACE_KEYS = ["north", "south", "east", "west", "up", "down"];
const IRON = ["G", "A", "B", "C", "D", "E", "F", "H", "I"];
const BOARD = ["m", "g", "s", "w", "W"];
const PAPER = ["q", "P", "p", "t"];
const PHOTO = ["0", "1", "2", "3", "4", "5", "6"];
const noise = (x, y, s) => {
  const n = Math.sin(x * 127.1 + y * 311.7 + s * 74.7) * 43758.5453;
  return n - Math.floor(n);
};
const pick = (ramp, value) => ramp[Math.max(0, Math.min(ramp.length - 1, Math.round(value)))];
const records = beginTexturePaint(PREFIX);
try {
  const diffuse = records.filter(record => !record.glowmask)[0];
  const W = diffuse.width;
  const H = diffuse.height;
  const base = diffuse.ctx.getImageData(0, 0, W, H).data;
  const map = [];
  for (let y = 0; y < H; y++) map.push(new Array(W).fill("_"));
  const setPx = (x, y, ch) => {
    if (x < 0 || y < 0 || x >= W || y >= H) return;
    map[y][x] = ch;
  };
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
    if (sane) cubes[local] = { faces, from: cube.from.slice(), to: cube.to.slice() };
  }
  const facePaint = (local, name, fn) => {
    const cube = cubes[local];
    if (!cube) return;
    const r = cube.faces[name];
    fn(r.w, r.h, (u, v, ch) => setPx(r.x + u, r.y + v, ch), r);
  };
  const otherFaces = (local, fn) => {
    for (const name of ["south", "east", "west", "up", "down"]) facePaint(local, name, fn);
  };
  const ironFace = (u, v, w, h, base, horizontal, flip) => {
    let s = base + (noise(u * 1.9, v * 1.5, 3) - 0.5) * 0.8;
    if (horizontal) {
      s += h > 1 ? 1.6 * (1 - v / (h - 1)) : 0.8;
      if (v === h - 1) s -= 1.3;
      if (u === 0) s += flip ? -0.8 : 0.5;
      if (u === w - 1) s += flip ? 0.5 : -0.7;
    } else {
      s += w > 1 ? 1.4 * (1 - u / (w - 1)) : 0.7;
      if (u === w - 1) s -= flip ? -0.9 : 1.2;
      if (v === 0) s += 0.6;
      if (v === h - 1) s -= 1.1;
    }
    return pick(IRON, s);
  };
  const rivet = (put, u, v) => {
    put(u, v, "H");
    put(u + 1, v, "F");
    put(u, v + 1, "E");
    put(u + 1, v + 1, "B");
  };
  facePaint("frame_top", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 5.2, true, false));
    for (const u of [3, 15, 27]) rivet(put, u, 0);
  });
  facePaint("frame_bottom", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 4.3, true, false));
    for (const u of [3, 15, 27]) rivet(put, u, 0);
  });
  facePaint("frame_left", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 4.7, false, false));
    for (const v of [3, 10, 17]) rivet(put, 0, v);
  });
  facePaint("frame_right", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 4.7, false, true));
    for (const v of [3, 10, 17]) rivet(put, w - 2, v);
  });
  for (const id of ["frame_top", "frame_bottom", "frame_left", "frame_right"]) {
    facePaint(id, "up", (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 6.5, true, false));
    });
    facePaint(id, "down", (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 1.9, true, false));
    });
    facePaint(id, "south", (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 1.6, true, false));
    });
    for (const name of ["east", "west"]) {
      facePaint(id, name, (w, h, put) => {
        for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 3.5, true, false));
      });
    }
  }
  facePaint("board_panel", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) {
      for (let u = 0; u < w; u++) {
        let s = 3.5 + (1 - v / (h - 1)) * 0.8 + (noise(u * 1.3, v * 1.1, 11) - 0.5) * 0.7;
        if (u === 0 || u === w - 1) s -= 1.3;
        if (v === 0 || v === h - 1) s -= 1.5;
        put(u, v, pick(BOARD, s));
      }
    }
    for (let i = 0; i < 18; i++) {
      const gx = Math.floor(noise(i * 3.7, 2.2, 19) * (w - 3)) + 1;
      const gy = Math.floor(noise(i * 2.3, 5.1, 23) * (h - 4)) + 1;
      for (let k = 0; k < 3; k++) {
        if (noise(gx + k, gy, 29) > 0.5) put(gx + k, gy, "g");
      }
    }
    for (let u = 3; u < 24; u += 2) put(u, 0, "b");
    for (const u of [6, 13, 20]) {
      put(u, 0, "e");
      put(u - 1, 0, "f");
    }
    for (let v = 2; v < 16; v += 2) put(13, v, "g");
    for (let u = 3; u < 25; u++) {
      put(u, 17, "i");
      if (u % 5 === 3) {
        put(u, 16, "i");
        put(u, 18, "i");
      }
    }
    put(8, 17, "r");
    put(9, 17, "r");
    put(17, 17, "b");
    put(18, 17, "b");
    for (let u = 4; u < 10; u++) put(u, 19, "n");
  });
  for (const name of ["south", "east", "west", "up", "down"]) {
    facePaint("board_panel", name, (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, pick(BOARD, 1.6 + noise(u, v, 41) * 0.8));
    });
  }
  const paperBase = (u, v, w, h) => {
    let s = 2.5 + (noise(u * 2.1, v * 1.7, 5) - 0.5) * 0.9;
    if (v === 0) s += 0.8;
    if (v === h - 1) s -= 1.0;
    if (u === w - 1) s -= 0.9;
    if (u === 0) s -= 0.4;
    return pick(PAPER, s);
  };
  const paperSheet = (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, paperBase(u, v, w, h));
    for (let u = 0; u < w; u++) put(u, 0, "t");
  };
  facePaint("doc_report", "north", (w, h, put) => {
    paperSheet(w, h, put);
    for (let u = 1; u < w - 1; u++) put(u, 1, "i");
    put(1, 1, "r");
    put(2, 1, "r");
    for (let u = 1; u < w - 3; u++) put(u, 2, "j");
    for (let u = 1; u < w - 4; u++) put(u, 3, "j");
    for (let u = 1; u < w - 2; u++) put(u, 4, "j");
    for (let u = 1; u < w - 5; u++) put(u, 5, "j");
    for (let v = 5; v < 8; v++) {
      for (let u = 7; u < 10; u++) put(u, v, (u + v) % 2 === 0 ? "2" : "3");
    }
    for (let u = 1; u < 5; u++) put(u, 6, "j");
    for (let u = 1; u < 6; u++) put(u, 7, "j");
    for (let u = 1; u < 4; u++) put(u, 8, "b");
    put(8, 8, "r");
    put(9, 8, "c");
    put(7, 8, "c");
    put(9, 7, "R");
  });
  facePaint("doc_photo", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, v === h - 1 ? "P" : "t");
    for (let v = 1; v < h - 1; v++) {
      for (let u = 1; u < w - 1; u++) {
        const grain = noise(u * 1.7, v * 2.3, 13);
        let s = 1.2 + grain * 2.2;
        if (u >= 3 && u <= 7 && v === 1) s += 2.2;
        if (u === 5) s += 1.3;
        put(u, v, pick(PHOTO, s));
      }
    }
    for (let u = 0; u < w; u++) put(u, 0, "t");
  });
  facePaint("doc_chart", "north", (w, h, put) => {
    paperSheet(w, h, put);
    for (let u = 1; u < w; u++) {
      put(u, 2, "j");
      put(u, 4, "j");
      put(u, 6, "j");
    }
    for (let v = 1; v < h - 1; v++) {
      put(0, v, "i");
      put(w - 1, v, "i");
    }
    for (let u = 0; u < w; u++) put(u, h - 1, "i");
    for (let u = 1; u < w - 1; u++) {
      const t = (u - 1) / (w - 3);
      const row = Math.max(1, Math.min(h - 2, Math.round(6.2 - t * 3.6 - Math.sin(t * 3.1) * 0.6)));
      put(u, row, "r");
      put(u, row + 1, "c");
      if (u % 3 === 1 && row > 1) put(u, row - 1, "R");
    }
    for (let u = 1; u < w - 1; u++) {
      const t = (u - 1) / (w - 3);
      const row = Math.max(1, Math.min(h - 2, Math.round(6.4 - t * 1.6)));
      if (u % 2 === 0) put(u, row, "e");
    }
    for (let u = 1; u < 6; u++) put(u, 0, "i");
    for (let u = 7; u < 10; u++) put(u, 0, "j");
  });
  facePaint("doc_specimen", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, v === h - 1 ? "P" : "t");
    for (let v = 1; v < h - 1; v++) {
      for (let u = 1; u < w - 1; u++) {
        const grain = noise(u * 1.3, v * 1.9, 31);
        let s = 0.8 + grain * 1.6;
        const dx = u - 5.5;
        const dy = v - 2;
        const dist = Math.sqrt(dx * dx + dy * dy * 1.6);
        if (dist < 3) s += 2.9 - dist * 0.7;
        if (dist < 1.2) s += 1.4;
        put(u, v, pick(PHOTO, s));
      }
    }
    for (let u = 1; u < w - 1; u++) put(u, 0, "t");
    for (let u = 1; u < 5; u++) put(u, h - 1, "j");
    for (let u = 6; u < w - 1; u++) put(u, h - 1, "j");
  });
  const noteArt = (id, baseChar, shadeChar) => {
    facePaint(id, "north", (w, h, put) => {
      for (let v = 0; v < h; v++) {
        for (let u = 0; u < w; u++) {
          let ch = baseChar;
          if (v === 0 || u === w - 1 || v === h - 1) ch = shadeChar;
          if (noise(u * 2.7, v * 1.9, 17) > 0.84) ch = shadeChar;
          put(u, v, ch);
        }
      }
      for (let u = 0; u < w - 1; u++) put(u, 1, shadeChar);
      for (let u = 1; u < w; u++) put(u, h - 2, shadeChar);
      put(0, h - 1, shadeChar);
    });
  };
  noteArt("note_yellow", "y", "Y");
  noteArt("note_pink", "o", "O");
  noteArt("note_blue", "u", "U");
  const pinArt = (id) => {
    facePaint(id, "north", (w, h, put) => {
      put(0, 0, "H");
      put(1, 0, "F");
      put(0, 1, "D");
      put(1, 1, "B");
    });
    otherFaces(id, (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, v === 0 ? "E" : v === h - 1 ? "B" : "C");
    });
  };
  for (const key of ["a", "b", "c", "d", "e", "f"]) pinArt("pin_" + key);
  const markerArt = (id, barrel, light, dark) => {
    facePaint(id, "north", (w, h, put) => {
      put(0, 0, "i");
      put(1, 0, dark);
      put(2, 0, barrel);
      put(3, 0, light);
      put(4, 0, "t");
    });
    otherFaces(id, (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, u < 2 ? dark : barrel);
    });
  };
  markerArt("marker_red", "r", "R", "c");
  markerArt("marker_blue", "b", "f", "e");
  facePaint("board_eraser", "north", (w, h, put) => {
    for (let u = 0; u < w; u++) put(u, 0, "5");
    for (let u = 0; u < w; u++) put(u, 1, u % 2 === 0 ? "2" : "3");
  });
  otherFaces("board_eraser", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, v === 0 ? "4" : "2");
  });
  facePaint("marker_tray", "north", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 3.9, true, false));
  });
  facePaint("marker_tray", "up", (w, h, put) => {
    for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 5.9, true, false));
  });
  for (const name of ["south", "down", "east", "west"]) {
    facePaint("marker_tray", name, (w, h, put) => {
      for (let v = 0; v < h; v++) for (let u = 0; u < w; u++) put(u, v, ironFace(u, v, w, h, 2.0, true, false));
    });
  }
  facePaint("cords", "north", (w, h, put, r) => {
    for (let v = 0; v < h; v++) {
      for (let u = 0; u < w; u++) {
        const index = ((r.y + v) * W + (r.x + u)) * 4;
        if (base[index + 3] === 0) continue;
        const red = base[index];
        const green = base[index + 1];
        const blue = base[index + 2];
        if (red > 90 && red > green + 30 && red > blue + 30) {
          const grain = noise(u * 2.3, v * 1.7, 53);
          put(u, v, grain > 0.76 ? "S" : grain < 0.18 ? "T" : "R");
        }
      }
    }
  });
  repaintMapPreservingAlpha(diffuse.ctx, W, H, PALETTE, map.map(row => row.join("")));
  applyTextureDetail(diffuse.ctx, { seed: MODEL_ID, strength: 0.1 });
  const summary = finishTextureScript(MODEL_ID, records);
  return summary;
} catch (error) {
  abortTextureScript(MODEL_ID, records);
  throw error;
}
