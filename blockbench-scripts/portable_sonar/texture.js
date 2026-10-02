(() => {
const EPSILON = 1e-6;
const BLOCK_UNITS = 16;
function blockUnits(blocks) {
  if (typeof blocks !== "number" || !Number.isFinite(blocks)) throw new Error("block-count-must-be-finite");
  const units = blocks * BLOCK_UNITS;
  if (!Number.isFinite(units)) throw new Error("block-count-out-of-range");
  if (!Number.isInteger(units) || (blocks !== 0 && units === 0)) throw new Error("block-count-must-produce-integral-units");
  return units;
}
const FACE_ORDER = ["north", "south", "east", "west", "up", "down"];
const FACE_AXIS = { north: [2, 0], south: [2, 1], east: [0, 1], west: [0, 0], up: [1, 1], down: [1, 0] };

function rotX(a) {
  const c = Math.cos(a), s = Math.sin(a);
  return [1, 0, 0, 0, c, -s, 0, s, c];
}
function rotY(a) {
  const c = Math.cos(a), s = Math.sin(a);
  return [c, 0, s, 0, 1, 0, -s, 0, c];
}
function rotZ(a) {
  const c = Math.cos(a), s = Math.sin(a);
  return [c, -s, 0, s, c, 0, 0, 0, 1];
}
function mulM(a, b) {
  const out = new Array(9);
  for (let r = 0; r < 3; r++) for (let c = 0; c < 3; c++) out[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
  return out;
}
function mulScale(m, s) {
  return [m[0] * s[0], m[1] * s[1], m[2] * s[2], m[3] * s[0], m[4] * s[1], m[5] * s[2], m[6] * s[0], m[7] * s[1], m[8] * s[2]];
}

function eulerMatrix(e, order) {
  const r = [e[0] * Math.PI / 180, e[1] * Math.PI / 180, e[2] * Math.PI / 180];
  if (order === "XYZ") return mulM(mulM(rotX(r[0]), rotY(r[1])), rotZ(r[2]));
  return mulM(mulM(rotZ(r[2]), rotY(r[1])), rotX(r[0]));
}
function add3(a, b) { return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]; }
function sub3(a, b) { return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]; }
function scale3(a, k) { return [a[0] * k, a[1] * k, a[2] * k]; }
function dot3(a, b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
function cross3(a, b) { return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]; }
function norm3(a) {
  const l = Math.sqrt(dot3(a, a));
  return l < 1e-12 ? [0, 0, 0] : [a[0] / l, a[1] / l, a[2] / l];
}
function m3v(m, v) { return [m[0] * v[0] + m[1] * v[1] + m[2] * v[2], m[3] * v[0] + m[4] * v[1] + m[5] * v[2], m[6] * v[0] + m[7] * v[1] + m[8] * v[2]]; }
function applyPoint(T, p) { return add3(m3v(T.m, sub3(p, T.origin)), T.t); }
function axisIndex(a) { return a === "x" ? 0 : a === "y" ? 1 : 2; }
function planarAxes(axis) { return axis === 0 ? [1, 2] : axis === 1 ? [0, 2] : [0, 1]; }
function vec3(v) { return Array.isArray(v) && v.length === 3 && v.every(n => typeof n === "number" && Number.isFinite(n)); }
function integer3(v) { return v.every(n => Number.isInteger(n)); }
const LOCAL_ID_PATTERN = /^[a-z0-9]+(?:_[a-z0-9]+)*$/;
const RESERVED_LOCAL_IDS = new Set(["__proto__", "constructor", "prototype"]);
function validLocalId(value) { return typeof value === "string" && LOCAL_ID_PATTERN.test(value) && !RESERVED_LOCAL_IDS.has(value); }
function modelIdFromPrefix(prefix) {
  if (typeof prefix !== "string" || !prefix) throw new Error("model-prefix-invalid");
  const modelId = prefix.endsWith("_") ? prefix.slice(0, -1) : prefix;
  if (!validLocalId(modelId)) throw new Error("model-prefix-invalid");
  return modelId;
}
function generatedName(modelId, kind, localId, role) {
  if (!validLocalId(modelId) || !validLocalId(localId)) throw new Error("generated-name-invalid");
  if (kind === "texture") {
    if (role === "glowmask" || localId === modelId + "_glowmask") return modelId + "_glowmask";
    if (localId === "atlas") return modelId;
    return modelId + "_texture_" + localId;
  }
  const segment = kind === "group" ? "group" : kind === "cube" ? "cube" : kind === "animation" ? "animation" : kind;
  if (!validLocalId(segment)) throw new Error("generated-name-invalid");
  return kind === "group" || kind === "cube" ? segment + "_" + localId : modelId + "_" + segment + "_" + localId;
}
function generatedNamePrefix(modelId, kind) {
  return kind === "group" || kind === "cube" ? kind + "_" : modelId + "_" + kind + "_";
}
function ownsGeneratedName(name, modelId, kind) {
  if (typeof name !== "string" || !validLocalId(modelId)) return false;
  if (kind === "texture") return name === modelId || name === modelId + "_glowmask" || name.indexOf(modelId + "_texture_") === 0;
  const prefix = generatedNamePrefix(modelId, kind);
  const legacyPrefix = modelId + "_" + kind + "_";
  return name.indexOf(prefix) === 0 || name.indexOf(legacyPrefix) === 0;
}
function cubeUsesTexture(cube, texture) {
  if (!cube || !texture || typeof texture.uuid !== "string" || !cube.faces) return false;
  for (const face of FACE_ORDER) {
    const value = cube.faces[face] && cube.faces[face].texture;
    if (value === texture.uuid || value === texture || value && value.uuid === texture.uuid) return true;
  }
  return false;
}
function parentFirstGroupIds(groups, ids) {
  const pending = new Set(ids.filter(id => groups[id]));
  const ordered = [];
  while (pending.size) {
    let progressed = false;
    for (const id of ids) {
      if (!pending.has(id)) continue;
      const group = groups[id];
      if (!group || !group.parent || !pending.has(group.parent)) {
        pending.delete(id);
        ordered.push(id);
        progressed = true;
      }
    }
    if (!progressed) return null;
  }
  return ordered;
}
function makeSolidMask(width, height, pixel) {
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1) throw new Error("surface-mask-invalid-dimensions");
  if (typeof pixel !== "string" || pixel.length !== 1 || pixel === "_") throw new Error("surface-mask-invalid-pixel");
  return Array.from({ length: height }, () => pixel.repeat(width));
}
function eraseMaskRects(mask, rects) {
  if (!Array.isArray(mask) || mask.length < 1 || typeof mask[0] !== "string" || mask[0].length < 1 || mask.some(row => typeof row !== "string" || row.length !== mask[0].length)) throw new Error("surface-mask-invalid");
  if (!Array.isArray(rects)) throw new Error("surface-mask-invalid-cutouts");
  const height = mask.length, width = mask[0].length;
  const out = mask.map(row => row.split(""));
  for (const rect of rects) {
    if (!rect || typeof rect !== "object" || !Number.isInteger(rect.x) || !Number.isInteger(rect.y) || !Number.isInteger(rect.width) || !Number.isInteger(rect.height) || rect.width < 1 || rect.height < 1) throw new Error("surface-mask-invalid-cutout");
    if (rect.x < 0 || rect.y < 0 || rect.x + rect.width > width || rect.y + rect.height > height) throw new Error("surface-mask-cutout-outside");
    for (let y = rect.y; y < rect.y + rect.height; y++) for (let x = rect.x; x < rect.x + rect.width; x++) out[y][x] = "_";
  }
  return out.map(row => row.join(""));
}
function makeTransparentPixelMap(width, height) {
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1) throw new Error("glowmask-invalid-dimensions");
  return Array.from({ length: height }, () => "_".repeat(width));
}
function writeGlowMaskRegion(target, placement, pixels) {
  if (!Array.isArray(target) || target.length < 1 || typeof target[0] !== "string" || target[0].length < 1 || target.some(row => typeof row !== "string" || row.length !== target[0].length)) throw new Error("glowmask-invalid-target");
  if (!placement || !Array.isArray(placement.uv) || placement.uv.length !== 2 || !placement.uv.every(Number.isInteger) || !Number.isInteger(placement.width) || !Number.isInteger(placement.height) || placement.width < 1 || placement.height < 1) throw new Error("glowmask-invalid-placement");
  if (!Array.isArray(pixels) || pixels.length !== placement.height || pixels.some(row => typeof row !== "string" || row.length !== placement.width)) throw new Error("glowmask-invalid-region");
  const x0 = placement.uv[0], y0 = placement.uv[1];
  if (x0 < 0 || y0 < 0 || x0 + placement.width > target[0].length || y0 + placement.height > target.length) throw new Error("glowmask-region-outside");
  const out = target.map(row => row.split(""));
  for (let y = 0; y < placement.height; y++) for (let x = 0; x < placement.width; x++) {
    const ch = pixels[y][x];
    if (typeof ch !== "string" || ch.length !== 1) throw new Error("glowmask-invalid-region");
    out[y0 + y][x0 + x] = ch;
  }
  return out.map(row => row.join(""));
}

function multipleOf225(e) {
  for (let i = 0; i < 3; i++) {
    if (Math.abs(e[i]) < EPSILON) continue;
    const q = e[i] / 22.5;
    if (Math.abs(q - Math.round(q)) > 1e-9) return false;
  }
  return true;
}
function cleanAngle(value) {
  return Math.abs(value) < 1e-9 ? 0 : value;
}
function rotationFromNormal(normal, roll) {
  if (!vec3(normal)) throw new Error("attachment-invalid-normal");
  const n = norm3(normal);
  if (dot3(n, n) < EPSILON) throw new Error("attachment-zero-normal");
  const yaw = Math.atan2(n[0], n[2]) * 180 / Math.PI;
  const pitch = Math.atan2(-n[1], Math.hypot(n[0], n[2])) * 180 / Math.PI;
  const z = roll === undefined ? 0 : roll;
  if (typeof z !== "number" || !Number.isFinite(z)) throw new Error("attachment-invalid-roll");
  return [cleanAngle(pitch), cleanAngle(yaw), cleanAngle(z)];
}
function makeSurfaceAttachment(anchor, size, normal, options) {
  if (!vec3(anchor) || !vec3(size) || !Number.isInteger(size[0]) || size[0] <= 0 || !Number.isInteger(size[1]) || size[1] <= 0 || !Number.isInteger(size[2]) || size[2] < 0) throw new Error("attachment-invalid-size");
  const config = options || {};
  const inset = config.inset === undefined ? 0 : config.inset;
  if (!Number.isInteger(inset) || inset < 0) throw new Error("attachment-invalid-inset");
  const origin = anchor.slice();
  const from = [anchor[0] - size[0] / 2, anchor[1] - size[1] / 2, anchor[2] - inset];
  const to = [anchor[0] + size[0] / 2, anchor[1] + size[1] / 2, anchor[2] + size[2]];
  const rotation = config.rotation === undefined ? rotationFromNormal(normal, config.roll) : config.rotation;
  if (!vec3(rotation)) throw new Error("attachment-invalid-rotation");
  return {
    from,
    to,
    origin,
    rotation,
    fractionalReason: config.fractionalReason || (from.concat(to, origin).some(value => !Number.isInteger(value)) ? "centered surface attachment" : undefined),
    angleReason: config.angleReason || (!multipleOf225(rotation) ? "surface-normal attachment" : undefined),
  };
}
function makeArticulatedSegment(joint, size, direction, options) {
  if (!vec3(size) || !Number.isInteger(size[2]) || size[2] <= 0) throw new Error("segment-invalid-length");
  const config = options || {};
  return makeSurfaceAttachment(joint, size, direction, {
    inset: config.inset === undefined ? 1 : config.inset,
    roll: config.roll,
    rotation: config.rotation,
    fractionalReason: config.fractionalReason,
    angleReason: config.angleReason,
  });
}
function clamp01(value) { return Math.max(0, Math.min(1, value)); }
function easeInOutSine(value) {
  const t = clamp01(value);
  return 0.5 - 0.5 * Math.cos(Math.PI * t);
}
function organicWave(t, amplitude, cycles, phase, secondaryAmplitude) {
  const secondary = secondaryAmplitude === undefined ? 0.14 : secondaryAmplitude;
  const main = Math.sin(Math.PI * 2 * cycles * t + (phase || 0));
  const detail = Math.sin(Math.PI * 4 * cycles * t + (phase || 0) * 0.7 + 0.35);
  return amplitude * (main + secondary * detail);
}
function sampleLoopKeyframes(length, samples, valueAt, interpolation) {
  if (!Number.isFinite(length) || length <= 0 || Math.abs(length / 0.05 - Math.round(length / 0.05)) > 1e-9) throw new Error("animation-helper-invalid-length");
  if (typeof valueAt !== "function") throw new Error("animation-helper-invalid-value-function");
  const tickCount = Math.round(length / 0.05);
  const requested = samples === undefined ? 16 : Math.floor(samples);
  if (!Number.isFinite(requested) || requested < 2) throw new Error("animation-helper-invalid-sample-count");
  let count = 1;
  if (tickCount > 1) {
    let bestDistance = Infinity;
    const consider = candidate => {
      if (candidate < 2) return;
      const distance = Math.abs(candidate - requested);
      if (distance < bestDistance || (distance === bestDistance && candidate > count)) {
        count = candidate;
        bestDistance = distance;
      }
    };
    for (let divisor = 1; divisor * divisor <= tickCount; divisor++) {
      if (tickCount % divisor !== 0) continue;
      consider(divisor);
      consider(tickCount / divisor);
    }
  }
  const ticks = new Set([0, tickCount]);
  for (let i = 1; i < count; i++) ticks.add(tickCount * i / count);
  const mode = interpolation || "catmullrom";
  const keyframes = [...ticks].sort((a, b) => a - b).map(tick => {
    const time = Math.round(tick * 0.05 * 1e6) / 1e6;
    const value = valueAt(time / length, time);
    if (!vec3(value)) throw new Error("animation-helper-invalid-value");
    return { time, value: value.slice(), interpolation: mode };
  });
  const first = keyframes[0].value;
  const last = keyframes[keyframes.length - 1].value;
  for (let i = 0; i < 3; i++) if (Math.abs(first[i] - last[i]) > EPSILON) throw new Error("animation-helper-loop-seam");
  keyframes[keyframes.length - 1].value = first.slice();
  return keyframes;
}
function makeVividTrack(bone, channel, length, valueAt, options) {
  const config = options || {};
  const interpolation = config.interpolation || "catmullrom";
  return {
    bone,
    channel,
    interpolation,
    keyframes: sampleLoopKeyframes(length, config.samples, valueAt, interpolation),
  };
}

function worldTransformOf(model, element) {
  let m = [1, 0, 0, 0, 1, 0, 0, 0, 1];
  let t = [0, 0, 0];
  let parentOrigin = [0, 0, 0];
  let scaled = false;
  const order = model.eulerOrder || "ZYX";
  const chain = [];
  if (element.kind === "cube" && element.parent) {
    const seen = new Set();
    let g = model.groups[element.parent];
    while (g) {
      if (seen.has(g.id)) throw new Error("group-parent-cycle: " + g.id);
      seen.add(g.id);
      chain.unshift(g);
      g = g.parent ? model.groups[g.parent] : null;
    }
  }
  for (const g of chain) {
    t = add3(t, m3v(m, sub3(g.origin, parentOrigin)));
    m = mulM(m, eulerMatrix(g.rotation, order));
    const scale = Array.isArray(g.scale) ? g.scale : [1, 1, 1];
    if (scale.some(value => Math.abs(value - 1) > EPSILON)) scaled = true;
    m = mulScale(m, scale);
    parentOrigin = g.origin;
  }
  if (element.kind === "cube") {
    t = add3(t, m3v(m, sub3(element.origin, parentOrigin)));
    m = mulM(m, eulerMatrix(element.rotation, order));
  }
  return { m, t, origin: element.origin, scaled };
}

function obbOf(element, T) {
  const s = [element.to[0] - element.from[0], element.to[1] - element.from[1], element.to[2] - element.from[2]];
  if (element.plate) s[axisIndex(element.plateAxis)] = 0;
  const c = applyPoint(T, [(element.from[0] + element.to[0]) / 2, (element.from[1] + element.to[1]) / 2, (element.from[2] + element.to[2]) / 2]);
  const rawAxes = [[T.m[0], T.m[3], T.m[6]], [T.m[1], T.m[4], T.m[7]], [T.m[2], T.m[5], T.m[8]]];
  if (!T.scaled) {
    const h = [0, 0, 0];
    for (let k = 0; k < 3; k++) for (let i = 0; i < 3; i++) h[k] += Math.abs(T.m[i * 3 + k]) * s[i];
    return { c, axes: rawAxes, h: [h[0] / 2, h[1] / 2, h[2] / 2] };
  }
  const lengths = rawAxes.map(axis => Math.sqrt(dot3(axis, axis)));
  const orthogonal = Math.abs(dot3(rawAxes[0], rawAxes[1])) < EPSILON && Math.abs(dot3(rawAxes[0], rawAxes[2])) < EPSILON && Math.abs(dot3(rawAxes[1], rawAxes[2])) < EPSILON && lengths.every(value => value > EPSILON);
  if (orthogonal) return { c, axes: rawAxes.map((axis, index) => scale3(axis, 1 / lengths[index])), h: [s[0] * lengths[0] / 2, s[1] * lengths[1] / 2, s[2] * lengths[2] / 2] };
  const axes = [[1, 0, 0], [0, 1, 0], [0, 0, 1]];
  const h = [0, 0, 0];
  for (const sx of [-1, 1]) for (const sy of [-1, 1]) for (const sz of [-1, 1]) {
    const p = applyPoint(T, [
      (element.from[0] + element.to[0]) / 2 + sx * s[0] / 2,
      (element.from[1] + element.to[1]) / 2 + sy * s[1] / 2,
      (element.from[2] + element.to[2]) / 2 + sz * s[2] / 2,
    ]);
    for (let k = 0; k < 3; k++) h[k] = Math.max(h[k], Math.abs(p[k] - c[k]));
  }
  return { c, axes, h };
}

function faceNormal(el, face, T) {
  const spec = FACE_AXIS[face];
  const local = [0, 0, 0];
  local[spec[0]] = spec[1] ? 1 : -1;
  const m = T.m;
  return norm3([m[0] * local[0] + m[1] * local[1] + m[2] * local[2], m[3] * local[0] + m[4] * local[1] + m[5] * local[2], m[6] * local[0] + m[7] * local[1] + m[8] * local[2]]);
}


function pointInObb(p, obb) {
  const d = sub3(p, obb.c);
  for (let k = 0; k < 3; k++) if (Math.abs(dot3(d, obb.axes[k])) >= obb.h[k] - EPSILON) return false;
  return true;
}

function satOverlap(a, b) {
  const axes = [];
  for (let i = 0; i < 3; i++) axes.push(a.axes[i]);
  for (let i = 0; i < 3; i++) axes.push(b.axes[i]);
  for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
    const c = cross3(a.axes[i], b.axes[j]);
    const n = norm3(c);
    if (n[0] || n[1] || n[2]) axes.push(n);
  }
  const delta = sub3(b.c, a.c);
  for (const ax of axes) {
    let ra = 0, rb = 0;
    for (let i = 0; i < 3; i++) { ra += a.h[i] * Math.abs(dot3(ax, a.axes[i])); rb += b.h[i] * Math.abs(dot3(ax, b.axes[i])); }
    if (Math.abs(dot3(delta, ax)) >= ra + rb - EPSILON) return false;
  }
  return true;
}

function faceSamples(el, face, T) {
  const spec = FACE_AXIS[face];
  const axis = spec[0], b = spec[1] ? el.to[axis] : el.from[axis];
  const free = axis === 0 ? [1, 2] : axis === 1 ? [0, 2] : [0, 1];
  const corners = [];
  for (const a0 of [el.from[free[0]], el.to[free[0]]]) for (const a1 of [el.from[free[1]], el.to[free[1]]]) {
    const p = [0, 0, 0];
    p[axis] = b;
    p[free[0]] = a0;
    p[free[1]] = a1;
    corners.push(applyPoint(T, p));
  }
  const center = [0, 0, 0];
  for (const c of corners) for (let k = 0; k < 3; k++) center[k] += c[k] / 4;
  corners.push(center);
  return corners;
}


function triNormal(a, b, c) { return norm3(cross3(sub3(b, a), sub3(c, a))); }

function sat2DPositive(pa, pb) {
  const axes = [];
  for (const q of [pa, pb]) for (let i = 0; i < 4; i++) {
    const e = [q[(i + 1) % 4][0] - q[i][0], q[(i + 1) % 4][1] - q[i][1]];
    const l = Math.sqrt(e[0] * e[0] + e[1] * e[1]);
    if (l < 1e-12) continue;
    axes.push([-e[1] / l, e[0] / l]);
  }
  for (const ax of axes) {
    let loA = Infinity, hiA = -Infinity, loB = Infinity, hiB = -Infinity;
    for (const p of pa) { const d = p[0] * ax[0] + p[1] * ax[1]; loA = Math.min(loA, d); hiA = Math.max(hiA, d); }
    for (const p of pb) { const d = p[0] * ax[0] + p[1] * ax[1]; loB = Math.min(loB, d); hiB = Math.max(hiB, d); }
    if (Math.min(hiA, hiB) - Math.max(loA, loB) <= EPSILON) return false;
  }
  return true;
}

function quadQuadPositiveArea(qA, qB) {
  const nA = triNormal(qA[0], qA[1], qA[2]);
  const nB = triNormal(qB[0], qB[1], qB[2]);
  if (Math.abs(dot3(nA, nB)) < 1 - 1e-9) return false;
  if (Math.abs(dot3(nA, sub3(qA[0], qB[0]))) > EPSILON) return false;
  const t1 = Math.abs(nA[0]) < 0.9 ? norm3(cross3(nA, [1, 0, 0])) : norm3(cross3(nA, [0, 1, 0]));
  const t2 = cross3(nA, t1);
  const pa = qA.map(p => [dot3(p, t1), dot3(p, t2)]);
  const pb = qB.map(p => [dot3(p, t1), dot3(p, t2)]);
  return sat2DPositive(pa, pb);
}

function pointInQuad2D(p, quad, n) {
  const t1 = Math.abs(n[0]) < 0.9 ? norm3(cross3(n, [1, 0, 0])) : norm3(cross3(n, [0, 1, 0]));
  const t2 = cross3(n, t1);
  const q = quad.map(v => [dot3(v, t1), dot3(v, t2)]);
  const cx = (q[0][0] + q[1][0] + q[2][0] + q[3][0]) / 4;
  const cy = (q[0][1] + q[1][1] + q[2][1] + q[3][1]) / 4;
  const order = [0, 1, 2, 3].sort((a, b) => Math.atan2(q[a][1] - cy, q[a][0] - cx) - Math.atan2(q[b][1] - cy, q[b][0] - cx));
  const pp = [dot3(p, t1), dot3(p, t2)];
  let sign = 0;
  for (let i = 0; i < 4; i++) {
    const a = order[i];
    const b = order[(i + 1) % 4];
    const e = [q[b][0] - q[a][0], q[b][1] - q[a][1]];
    const d = [pp[0] - q[a][0], pp[1] - q[a][1]];
    const c = e[0] * d[1] - e[1] * d[0];
    if (Math.abs(c) < 1e-9) continue;
    const s = c > 0 ? 1 : -1;
    if (sign === 0) sign = s;
    else if (sign !== s) return false;
  }
  return true;
}

function coplanarCoincidentSamples(a, faceA, b, faceB, T) {
  const nA = faceNormal(a, faceA, T[a.id]);
  const nB = faceNormal(b, faceB, T[b.id]);
  if (dot3(nA, nB) < 1 - 1e-9) return null;
  const sA = faceSamples(a, faceA, T[a.id]);
  const sB = faceSamples(b, faceB, T[b.id]);
  if (Math.abs(dot3(nA, sub3(sA[0], sB[0]))) > EPSILON) return null;
  if (!quadQuadPositiveArea(sA.slice(0, 4), sB.slice(0, 4))) return null;
  return sA.filter(p => pointInQuad2D(p, sB.slice(0, 4), nA));
}

function faceParticipation(el, face, other, T, obb) {
  if (el.faces && !el.faces[face].enabled) return null;
  const samples = faceSamples(el, face, T[el.id]);
  const inside = samples.filter(p => pointInObb(p, obb[other.id]));
  if (inside.length) return inside;
  for (const oface of FACE_ORDER) {
    if (other.faces && !other.faces[oface].enabled) continue;
    const part = coplanarCoincidentSamples(el, face, other, oface, T);
    if (part && part.length) return part;
  }
  return null;
}

function sampleExposed(s, normal, ids, obb, excludeId) {
  const q = [s[0] + EPSILON * normal[0], s[1] + EPSILON * normal[1], s[2] + EPSILON * normal[2]];
  for (const id of ids) {
    if (id === excludeId) continue;
    if (pointInObb(q, obb[id])) return false;
  }
  return true;
}

function classifySolidSolid(model, A, B, T, obb, ids) {
  const parts = [];
  const faces = new Set();
  for (const f of FACE_ORDER) {
    const part = faceParticipation(A, f, B, T, obb);
    if (part) { parts.push({ normal: faceNormal(A, f, T[A.id]), samples: part }); faces.add(f); }
  }
  for (const f of FACE_ORDER) {
    const part = faceParticipation(B, f, A, T, obb);
    if (part) { parts.push({ normal: faceNormal(B, f, T[B.id]), samples: part }); faces.add(f); }
  }
  if (!parts.length) return null;
  let classification = "interior";
  for (const p of parts) {
    for (const s of p.samples) {
      if (sampleExposed(s, p.normal, ids, obb, null)) { classification = "surface"; break; }
    }
    if (classification === "surface") break;
  }
  const aName = A.name < B.name ? A.name : B.name;
  const bName = A.name < B.name ? B.name : A.name;
  return { kind: "solid-solid", a: aName, b: bName, classification, faces: [...faces] };
}

function pixelOpaque(model, el, u, v) {
  const tex = model.textures[el.texture];
  if (!tex) return false;
  const idx = ((el.uv[1] + v) * tex.width + (el.uv[0] + u)) * 4 + 3;
  return tex.pixels[idx] > 0;
}

function pixelLocalCenter(el, u, v) {
  const axis = axisIndex(el.plateAxis);
  const planar = planarAxes(axis);
  const width = el.to[planar[0]] - el.from[planar[0]];
  const height = el.to[planar[1]] - el.from[planar[1]];
  const p = [0, 0, 0];
  p[axis] = el.from[axis];
  p[planar[0]] = el.from[planar[0]] + (el.mirrorUv ? width - u - 0.5 : u + 0.5);
  p[planar[1]] = el.from[planar[1]] + height - v - 0.5;
  return p;
}

function pixelCenterWorld(el, u, v, T) { return applyPoint(T, pixelLocalCenter(el, u, v)); }

function pixelQuadWorld(el, u, v, T) {
  const axis = axisIndex(el.plateAxis);
  const planar = planarAxes(axis);
  const width = el.to[planar[0]] - el.from[planar[0]];
  const height = el.to[planar[1]] - el.from[planar[1]];
  const localU = el.mirrorUv ? [width - u - 1, width - u] : [u, u + 1];
  const localV = [height - v - 1, height - v];
  const base = [0, 0, 0];
  base[axis] = el.from[axis];
  const corners = [];
  for (const s of [[0, 0], [1, 0], [0, 1], [1, 1]]) {
    const p = base.slice();
    p[planar[0]] = el.from[planar[0]] + localU[s[0]];
    p[planar[1]] = el.from[planar[1]] + localV[s[1]];
    corners.push(applyPoint(T, p));
  }
  return corners;
}

function plateNormal(el, T) {
  const a = axisIndex(el.plateAxis);
  return [T.m[a], T.m[3 + a], T.m[6 + a]];
}

function plateFaceNames(axis, side) {
  const pair = axis === "x" ? ["east", "west"] : axis === "y" ? ["up", "down"] : ["south", "north"];
  if (side === "both") return pair;
  if (side === "negative") return [pair[1]];
  return [pair[0]];
}
function normalizedPlateSide(c) {
  if (c.plateSide !== undefined) return c.plateSide;
  if (c.side !== undefined) return c.side;
  return c.doubleSided ? "both" : "positive";
}
function enabledPlateFaces(el) {
  if (!el.plate) return [];
  const names = plateFaceNames(el.plateAxis, normalizedPlateSide(el));
  return names.filter(face => !el.faces || !el.faces[face] || el.faces[face].enabled);
}

function plateSurfaceExposed(model, el, center, T, ids, obb) {
  for (const face of enabledPlateFaces(el)) {
    if (sampleExposed(center, faceNormal(el, face, T), ids, obb, el.id)) return true;
  }
  return false;
}

function quadOverlapsSolidFaces(model, plate, u, v, solid, pT, sT) {
  const qP = pixelQuadWorld(plate, u, v, pT);
  for (const f of FACE_ORDER) {
    if (!solid.faces[f].enabled) continue;
    const samples = faceSamples(solid, f, sT);
    if (quadQuadPositiveArea(qP, samples.slice(0, 4))) return true;
  }
  return false;
}

function classifySolidPlate(model, plate, solid, T, obb, ids) {
  if (!enabledPlateFaces(plate).length) return null;
  const pT = T[plate.id];
  const planar = planarAxes(axisIndex(plate.plateAxis));
  const w = plate.to[planar[0]] - plate.from[planar[0]];
  const h = plate.to[planar[1]] - plate.from[planar[1]];
  const participating = [];
  for (let u = 0; u < w; u++) for (let v = 0; v < h; v++) {
    if (!pixelOpaque(model, plate, u, v)) continue;
    const center = pixelCenterWorld(plate, u, v, pT);
    if (pointInObb(center, obb[solid.id])) { participating.push({ u, v, center }); continue; }
    if (quadOverlapsSolidFaces(model, plate, u, v, solid, pT, T[solid.id])) participating.push({ u, v, center });
  }
  if (!participating.length) return null;
  let classification = "interior";
  for (const p of participating) {
    if (plateSurfaceExposed(model, plate, p.center, pT, ids, obb)) {
      classification = "surface";
      break;
    }
  }
  return { kind: "solid-plate", a: plate.name, b: solid.name, classification, pixels: participating.map(p => ({ u: p.u, v: p.v })) };
}

function classifyPlatePlate(model, A, B, T, obb, ids) {
  if (!enabledPlateFaces(A).length || !enabledPlateFaces(B).length) return null;
  const planarA = planarAxes(axisIndex(A.plateAxis));
  const wA = A.to[planarA[0]] - A.from[planarA[0]];
  const hA = A.to[planarA[1]] - A.from[planarA[1]];
  const planarB = planarAxes(axisIndex(B.plateAxis));
  const wB = B.to[planarB[0]] - B.from[planarB[0]];
  const hB = B.to[planarB[1]] - B.from[planarB[1]];
  const pT = T[B.id];
  const participating = [];
  for (let u = 0; u < wB; u++) for (let v = 0; v < hB; v++) {
    if (!pixelOpaque(model, B, u, v)) continue;
    const qB = pixelQuadWorld(B, u, v, pT);
    let hit = false;
    for (let u2 = 0; u2 < wA && !hit; u2++) for (let v2 = 0; v2 < hA && !hit; v2++) {
      if (!pixelOpaque(model, A, u2, v2)) continue;
      if (quadQuadPositiveArea(qB, pixelQuadWorld(A, u2, v2, T[A.id]))) { hit = true; break; }
    }
    if (hit) participating.push({ u, v, center: pixelCenterWorld(B, u, v, pT) });
  }
  if (!participating.length) return null;
  let classification = "interior";
  for (const p of participating) {
    if (plateSurfaceExposed(model, B, p.center, T[B.id], ids, obb) || plateSurfaceExposed(model, A, p.center, T[A.id], ids, obb)) {
      classification = "surface";
      break;
    }
  }
  return { kind: "plate-plate", a: B.name, b: A.name, classification, pixels: participating.map(p => ({ u: p.u, v: p.v })) };
}


function detectOverlaps(model) {
  const ids = [];
  for (const id of Object.keys(model.elements)) if (!model.elements[id].deleted) ids.push(id);
  const T = Object.create(null);
  const obb = Object.create(null);
  for (const id of ids) {
    T[id] = worldTransformOf(model, model.elements[id]);
    obb[id] = obbOf(model.elements[id], T[id]);
  }
  const records = [];
  let checks = 0;
  for (let i = 0; i < ids.length; i++) for (let j = i + 1; j < ids.length; j++) {
    checks++;
    const a = model.elements[ids[i]];
    const b = model.elements[ids[j]];
    if (!a.plate && !b.plate) {
      if (satOverlap(obb[ids[i]], obb[ids[j]])) {
        const r = classifySolidSolid(model, a, b, T, obb, ids);
        if (r) records.push(r);
      }
    } else if (a.plate && b.plate) {
      const r = classifyPlatePlate(model, a, b, T, obb, ids);
      if (r) records.push(r);
    } else {
      const plate = a.plate ? a : b;
      const solid = a.plate ? b : a;
      const r = classifySolidPlate(model, plate, solid, T, obb, ids);
      if (r) records.push(r);
    }
  }
  records.checks = checks;
  return records;
}

function findElement(model, key) {
  if (model.elements[key] && !model.elements[key].deleted) return model.elements[key];
  for (const id of Object.keys(model.elements)) {
    const e = model.elements[id];
    if (!e.deleted && e.name === key) return e;
  }
  return null;
}

function findGroup(model, key) {
  if (model.groups[key] && !model.groups[key].deleted) return model.groups[key];
  for (const gid of Object.keys(model.groups)) {
    const g = model.groups[gid];
    if (!g.deleted && g.name === key) return g;
  }
  return null;
}

function erasePixels(model, elementId, pixels) {
  const e = findElement(model, elementId);
  if (!e) throw new Error("resolve-error: unknown element " + elementId);
  const tex = model.textures[e.texture];
  if (!tex) throw new Error("resolve-error: unknown texture " + e.texture);
  for (const p of pixels) {
    const u = p.u, v = p.v;
    if (!Number.isInteger(u) || !Number.isInteger(v) || u < 0 || v < 0) throw new Error("resolve-error: pixel out of range " + elementId);
    if (e.plate) {
      const planar = planarAxes(axisIndex(e.plateAxis));
      if (u >= e.to[planar[0]] - e.from[planar[0]] || v >= e.to[planar[1]] - e.from[planar[1]]) throw new Error("resolve-error: pixel out of range " + elementId);
      const idx = ((e.uv[1] + v) * tex.width + (e.uv[0] + u)) * 4;
      tex.pixels[idx + 3] = 0;
      model.erasedPixels.push({ element: e.name, u, v });
    } else {
      if (!p.face || !FACE_ORDER.includes(p.face)) throw new Error("resolve-error: face required for solid " + elementId);
      const f = e.faces[p.face];
      if (!f) throw new Error("resolve-error: unknown face " + p.face);
      const nx = Math.min(f.uv[0], f.uv[2]), ny = Math.min(f.uv[1], f.uv[3]);
      const rw = Math.max(f.uv[0], f.uv[2]) - nx, rh = Math.max(f.uv[1], f.uv[3]) - ny;
      if (u >= rw || v >= rh) throw new Error("resolve-error: pixel out of range " + elementId);
      const idx = ((ny + v) * tex.width + (nx + u)) * 4;
      tex.pixels[idx + 3] = 0;
      model.erasedPixels.push({ element: e.name, face: p.face, u, v });
    }
  }
  model.resolutions.push({ type: "erase-pixels", element: e.name, pixels: pixels.length });
}

function deleteElement(model, elementId) {
  const group = findGroup(model, elementId);
  if (group) {
    const g = group;
    const stack = [g.id];
    while (stack.length) {
      const gid = stack.pop();
      const gr = model.groups[gid];
      if (!gr || gr.deleted) continue;
      gr.deleted = true;
      model.deletedElements.push(gr.name);
      for (const id of Object.keys(model.elements)) {
        const c = model.elements[id];
        if (!c.deleted && c.parent === gid) { c.deleted = true; model.deletedElements.push(c.name); }
      }
      for (const id of Object.keys(model.groups)) {
        const gg = model.groups[id];
        if (!gg.deleted && gg.parent === gid) stack.push(id);
      }
    }
    model.resolutions.push({ type: "delete", element: g.name });
    return;
  }
  const e = findElement(model, elementId);
  if (!e) throw new Error("resolve-error: unknown element " + elementId);
  e.deleted = true;
  model.deletedElements.push(e.name);
  model.resolutions.push({ type: "delete", element: e.name });
}

function hideFace(model, elementId, face) {
  if (!FACE_ORDER.includes(face)) throw new Error("resolve-error: unknown face " + face);
  const e = findElement(model, elementId);
  if (!e) throw new Error("resolve-error: unknown element " + elementId);
  if (!e.faces[face].enabled) return;
  e.faces[face].enabled = false;
  model.hiddenFaces.push({ element: e.name, face });
  model.resolutions.push({ type: "hide-face", element: e.name, face });
}

function pixelsAllTransparent(model, e) {
  const planar = planarAxes(axisIndex(e.plateAxis));
  const w = e.to[planar[0]] - e.from[planar[0]];
  const h = e.to[planar[1]] - e.from[planar[1]];
  for (let u = 0; u < w; u++) for (let v = 0; v < h; v++) if (pixelOpaque(model, e, u, v)) return false;
  return true;
}

function allFacesDisabled(e) {
  for (const f of FACE_ORDER) if (e.faces[f].enabled) return false;
  return true;
}

function groupHasCubes(model, gid, visited) {
  const seen = visited || new Set();
  if (seen.has(gid)) return false;
  seen.add(gid);
  for (const id of Object.keys(model.elements)) {
    const c = model.elements[id];
    if (!c.deleted && c.parent === gid) return true;
  }
  for (const id of Object.keys(model.groups)) {
    const g = model.groups[id];
    if (!g.deleted && g.parent === gid && groupHasCubes(model, id, new Set(seen))) return true;
  }
  return false;
}

function textureReferenced(model, tid) {
  const texture = model.textures[tid];
  if (texture && texture.role === "glowmask") return true;
  for (const id of Object.keys(model.elements)) {
    const e = model.elements[id];
    if (!e.deleted && e.texture === tid) return true;
  }
  return false;
}


function removeElement(model, id, reason) {
  const el = model.elements[id] || model.groups[id] || model.textures[id];
  if (!el || el.deleted) return;
  el.deleted = true;
  model.deletedElements.push(el.name);
  model.resolutions.push({ type: "cleanup", element: el.name, reason });
}

function cleanupModel(model) {
  let changed = true;
  while (changed) {
    changed = false;
    for (const id of Object.keys(model.elements)) {
      const e = model.elements[id];
      if (e.deleted) continue;
      if (e.plate && pixelsAllTransparent(model, e)) { removeElement(model, id, "empty-plate"); changed = true; }
      else if (!e.plate && allFacesDisabled(e)) { removeElement(model, id, "all-faces-disabled"); changed = true; }
    }
    for (const gid of Object.keys(model.groups)) {
      const g = model.groups[gid];
      if (!g.deleted && !groupHasCubes(model, gid)) { removeElement(model, gid, "orphan-group"); changed = true; }
    }
    for (const tid of Object.keys(model.textures)) {
      const t = model.textures[tid];
      if (!t.deleted && !textureReferenced(model, tid)) { removeElement(model, tid, "unreferenced-texture"); changed = true; }
    }
  }
}

function verifyOverlaps(model) {
  const records = detectOverlaps(model);
  const surface = records.filter(r => r.classification === "surface");
  if (surface.length) {
    throw new Error("unresolved-overlap: " + surface.map(r => {
      let detail = "";
      if (r.pixels) detail = r.pixels.length + " pixels";
      else if (r.faces && r.faces.length) detail = "faces " + r.faces.join(",");
      return r.kind + " " + r.a + " " + r.b + " (" + detail + ")";
    }).join("; "));
  }
  return { interior: records.filter(r => r.classification === "interior") };
}

function applyPose(model, anim, t) {
  for (const tr of anim.tracks) {
    const g = model.groups[tr.bone];
    if (!g || g.deleted) throw new Error("animation-bone-missing: " + tr.bone);
    const v = poseAt(t, anim, tr.bone, tr.channel);
    if (tr.channel === "position") { g.origin[0] += v.x; g.origin[1] += v.y; g.origin[2] += v.z; }
    else if (tr.channel === "rotation") { g.rotation[0] += v.x; g.rotation[1] += v.y; g.rotation[2] += v.z; }
    else if (tr.channel === "scale") {
      if (!Array.isArray(g.scale)) g.scale = [1, 1, 1];
      g.scale[0] *= v.x;
      g.scale[1] *= v.y;
      g.scale[2] *= v.z;
    }
  }
}

function restoreBase(model, base) {
  for (const id of Object.keys(base.cubes)) {
    const target = model.elements[id];
    if (target) { target.origin = base.cubes[id].origin.slice(); target.rotation = base.cubes[id].rotation.slice(); }
  }
  for (const gid of Object.keys(base.groups)) {
    const target = model.groups[gid];
    if (target) {
      target.origin = base.groups[gid].origin.slice();
      target.rotation = base.groups[gid].rotation.slice();
      target.scale = base.groups[gid].scale.slice();
    }
  }
}

function poseCheck(model, base, anim, t) {
  restoreBase(model, base);
  applyPose(model, anim, t);
  const records = detectOverlaps(model);
  restoreBase(model, base);
  return { checks: records.checks, records };
}

function aabbOfElement(model, el) {
  const T = worldTransformOf(model, el);
  const obb = obbOf(el, T);
  const lo = [Infinity, Infinity, Infinity];
  const hi = [-Infinity, -Infinity, -Infinity];
  for (const sx of [-1, 1]) for (const sy of [-1, 1]) for (const sz of [-1, 1]) {
    const p = [
      obb.c[0] + sx * obb.h[0] * obb.axes[0][0] + sy * obb.h[1] * obb.axes[1][0] + sz * obb.h[2] * obb.axes[2][0],
      obb.c[1] + sx * obb.h[0] * obb.axes[0][1] + sy * obb.h[1] * obb.axes[1][1] + sz * obb.h[2] * obb.axes[2][1],
      obb.c[2] + sx * obb.h[0] * obb.axes[0][2] + sy * obb.h[1] * obb.axes[1][2] + sz * obb.h[2] * obb.axes[2][2],
    ];
    for (let k = 0; k < 3; k++) { lo[k] = Math.min(lo[k], p[k]); hi[k] = Math.max(hi[k], p[k]); }
  }
  return { lo, hi };
}

function aabbUnion(a, b) {
  return { lo: [Math.min(a.lo[0], b.lo[0]), Math.min(a.lo[1], b.lo[1]), Math.min(a.lo[2], b.lo[2])], hi: [Math.max(a.hi[0], b.hi[0]), Math.max(a.hi[1], b.hi[1]), Math.max(a.hi[2], b.hi[2])] };
}

function centerModelOnBodyBottom(model) {
  const liveElements = Object.keys(model.elements).map(id => model.elements[id]).filter(element => !element.deleted);
  if (!liveElements.length) throw new Error("model-centering-no-elements");
  const bodyGroup = model.groups.body && !model.groups.body.deleted ? model.groups.body : null;
  const bodyElement = model.elements.body && !model.elements.body.deleted ? model.elements.body : null;
  let candidates = [];
  let pivot = null;
  let source = "model_bounds";
  if (bodyGroup) {
    const direct = liveElements.filter(element => element.parent === "body");
    const solids = direct.filter(element => !element.plate);
    candidates = solids.length ? solids : direct;
    if (!candidates.length && bodyElement) candidates = [bodyElement];
    const probe = { kind: "cube", parent: "body", origin: bodyGroup.origin, rotation: [0, 0, 0] };
    const transform = worldTransformOf(model, probe);
    pivot = applyPoint(transform, bodyGroup.origin);
    source = "body_group";
  } else if (bodyElement) {
    candidates = [bodyElement];
    const transform = worldTransformOf(model, bodyElement);
    pivot = applyPoint(transform, bodyElement.origin);
    source = "body_cube";
  }
  if (!candidates.length) {
    const solids = liveElements.filter(element => !element.plate);
    candidates = solids.length ? solids : liveElements;
  }
  let bounds = null;
  for (const element of candidates) {
    const current = aabbOfElement(model, element);
    bounds = bounds ? aabbUnion(bounds, current) : current;
  }
  if (!bounds) throw new Error("model-centering-no-body-bounds");
  const anchor = [(bounds.lo[0] + bounds.hi[0]) / 2, bounds.lo[1], (bounds.lo[2] + bounds.hi[2]) / 2];
  if (pivot && Math.hypot(pivot[0] - anchor[0], pivot[1] - anchor[1], pivot[2] - anchor[2]) > EPSILON) {
    throw new Error("model-centering-body-pivot-mismatch: expected body pivot at bottom-surface center");
  }
  const offset = anchor.map(value => Math.abs(value) <= EPSILON ? 0 : -value);
  const move = value => value.map((coordinate, axis) => {
    const shifted = coordinate + offset[axis];
    return Math.abs(shifted) <= EPSILON ? 0 : shifted;
  });
  for (const gid of Object.keys(model.groups)) {
    const group = model.groups[gid];
    if (!group.deleted) group.origin = move(group.origin);
  }
  for (const id of Object.keys(model.elements)) {
    const element = model.elements[id];
    if (element.deleted) continue;
    element.from = move(element.from);
    element.to = move(element.to);
    element.origin = move(element.origin);
  }
  const centeredBounds = candidates.map(element => aabbOfElement(model, element)).reduce((combined, current) => combined ? aabbUnion(combined, current) : current, null);
  const centeredAnchor = [(centeredBounds.lo[0] + centeredBounds.hi[0]) / 2, centeredBounds.lo[1], (centeredBounds.lo[2] + centeredBounds.hi[2]) / 2];
  if (Math.hypot(centeredAnchor[0], centeredAnchor[1], centeredAnchor[2]) > EPSILON) throw new Error("model-centering-failed");
  model.centering = { source, originalAnchor: anchor, offset, pivot: [0, 0, 0] };
  return model.centering;
}

function aabbPositiveOverlap(a, b) {
  for (let k = 0; k < 3; k++) if (Math.min(a.hi[k], b.hi[k]) - Math.max(a.lo[k], b.lo[k]) <= EPSILON) return false;
  return true;
}

function sweptIntersects(model, base, anim, t0, t1) {
  const ids = [];
  for (const id of Object.keys(model.elements)) if (!model.elements[id].deleted) ids.push(id);
  const boxes0 = Object.create(null);
  const boxes1 = Object.create(null);
  restoreBase(model, base);
  applyPose(model, anim, t0);
  for (const id of ids) boxes0[id] = aabbOfElement(model, model.elements[id]);
  restoreBase(model, base);
  applyPose(model, anim, t1);
  for (const id of ids) boxes1[id] = aabbOfElement(model, model.elements[id]);
  restoreBase(model, base);
  for (let i = 0; i < ids.length; i++) for (let j = i + 1; j < ids.length; j++) {
    if (aabbPositiveOverlap(aabbUnion(boxes0[ids[i]], boxes1[ids[i]]), aabbUnion(boxes0[ids[j]], boxes1[ids[j]]))) return true;
  }
  return false;
}

function checkAnimatedOverlaps(model, ANIMATIONS) {
  let checks = 0;
  const surfaceHits = [];
  const interior = [];
  const seen = new Set();
  const base = { cubes: Object.create(null), groups: Object.create(null) };
  for (const id of Object.keys(model.elements)) {
    const e = model.elements[id];
    base.cubes[id] = { origin: e.origin.slice(), rotation: e.rotation.slice() };
  }
  for (const gid of Object.keys(model.groups)) {
    const g = model.groups[gid];
    base.groups[gid] = { origin: g.origin.slice(), rotation: g.rotation.slice(), scale: (g.scale || [1, 1, 1]).slice() };
  }
  const collect = (res, t) => {
    checks += res.checks;
    for (const r of res.records) {
      if (r.classification === "surface") surfaceHits.push({ kind: r.kind, a: r.a, b: r.b, t });
      else {
        const key = r.a + "|" + r.b + "|" + r.kind;
        if (!seen.has(key)) { seen.add(key); interior.push({ a: r.a, b: r.b, kind: r.kind }); }
      }
    }
  };
  for (const anim of ANIMATIONS) {
    const times = new Set();
    const steps = Math.round(anim.length / 0.05);
    for (let k = 0; k <= steps; k++) times.add(Math.round(k * 0.05 * 1e6) / 1e6);
    for (const tr of anim.tracks) for (const kf of tr.keyframes) times.add(kf.time);
    const sorted = [...times].sort((x, y) => x - y);
    for (let i = 0; i < sorted.length; i++) {
      const t0 = sorted[i];
      collect(poseCheck(model, base, anim, t0), t0);
      const t1 = sorted[i + 1];
      if (t1 === undefined) break;
      if (t1 - t0 < 1e-9) continue;
      if (sweptIntersects(model, base, anim, t0, t1)) {
        const dt = t1 - t0;
        for (let k = 1; k <= 3; k++) {
          const t = Math.round((t0 + (dt * k) / 4) * 1e6) / 1e6;
          collect(poseCheck(model, base, anim, t), t);
        }
      }
    }
  }
  if (surfaceHits.length) throw new Error("animated-overlap: " + surfaceHits.map(r => r.kind + " " + r.a + " " + r.b + " at t=" + r.t).join("; "));
  return { checks, interiorOverlaps: interior, remaining: [] };
}

function nextPowerOfTwo(value) {
  let out = 1;
  while (out < value) out *= 2;
  return out;
}

function createTextureAtlas(options) {
  options = options || {};
  const id = options.id;
  const prefix = options.prefix;
  const maxWidth = options.maxWidth === undefined ? 64 : options.maxWidth;
  const maxHeight = options.maxHeight === undefined ? 4096 : options.maxHeight;
  const padding = options.padding === undefined ? 0 : options.padding;
  if (typeof id !== "string" || !id || typeof prefix !== "string" || !prefix) throw new Error("atlas-invalid-options");
  const modelId = modelIdFromPrefix(prefix);
  if (!Number.isInteger(maxWidth) || maxWidth < 1 || !Number.isInteger(maxHeight) || maxHeight < 1 || !Number.isInteger(padding) || padding < 0) throw new Error("atlas-invalid-options");
  const atlasPalette = options.palette && typeof options.palette === "object" ? options.palette : {};
  for (const key of Object.keys(atlasPalette)) {
    if (key.length !== 1 || key === "_" || !parsePaletteColor(atlasPalette[key])) throw new Error("atlas-invalid-palette: " + key);
  }
  const textureId = "atlas";
  const entries = [];
  const byId = new Map();
  const reservations = [];
  let cursorX = 0;
  let cursorY = 0;
  let shelfHeight = 0;
  let usedHeight = 0;
  const vector = (value, fallback) => {
    if (value === undefined) {
      if (fallback === undefined) throw new Error("atlas-invalid-vector");
      return fallback.slice();
    }
    const result = value.slice();
    if (!vec3(result)) throw new Error("atlas-invalid-vector");
    return result;
  };
  const baseSpec = (spec, kind) => {
    if (!spec || typeof spec !== "object" || !validLocalId(spec.id) || byId.has(spec.id)) throw new Error("atlas-invalid-" + kind + "-id");
    const from = vector(spec.from);
    const origin = vector(spec.origin, from);
    const rotation = vector(spec.rotation, [0, 0, 0]);
    return { id: spec.id, parent: spec.parent === undefined ? null : spec.parent, from, origin, rotation };
  };
  const reserve = (kind, width, height, entry) => {
    if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1) throw new Error("atlas-invalid-" + kind + "-dimensions");
    if (width > maxWidth) throw new Error("atlas-entry-too-wide: " + entry.id);
    if (cursorX && cursorX + width > maxWidth) {
      cursorY += shelfHeight + padding;
      cursorX = 0;
      shelfHeight = 0;
    }
    const nextHeight = cursorY + height;
    if (nextHeight > maxHeight) throw new Error("atlas-too-tall: " + entry.id);
    const reservation = { kind, id: entry.id, x: cursorX, y: cursorY, width, height, pixels: null };
    reservations.push(reservation);
    cursorX += width + padding;
    shelfHeight = Math.max(shelfHeight, height);
    usedHeight = Math.max(usedHeight, nextHeight);
    return reservation;
  };
  const paint = (reservation, value, kind) => {
    if (!value || typeof value !== "object" || (Object.keys(value).length !== 1)) throw new Error("atlas-invalid-" + kind + "-paint: " + reservation.id);
    if (Object.prototype.hasOwnProperty.call(value, "fill")) {
      if (typeof value.fill !== "string" || value.fill.length !== 1 || !Object.prototype.hasOwnProperty.call(atlasPalette, value.fill)) throw new Error("atlas-invalid-" + kind + "-paint: " + reservation.id);
      reservation.pixels = Array.from({ length: reservation.height }, () => value.fill.repeat(reservation.width));
      return;
    }
    if (!Object.prototype.hasOwnProperty.call(value, "pixels") || !Array.isArray(value.pixels) || value.pixels.length !== reservation.height || value.pixels.some(row => typeof row !== "string" || row.length !== reservation.width)) throw new Error("atlas-invalid-" + kind + "-paint: " + reservation.id);
    for (const row of value.pixels) for (const ch of row) if (ch !== "_" && !Object.prototype.hasOwnProperty.call(atlasPalette, ch)) throw new Error("atlas-invalid-" + kind + "-paint: " + reservation.id);
    reservation.pixels = value.pixels.slice();
  };
  const cubeName = localId => generatedName(modelId, "cube", localId);
  const makeCube = (entry, reservation, extra) => ({
    id: entry.id,
    name: cubeName(entry.id),
    parent: entry.parent,
    from: entry.from.slice(),
    to: entry.to.slice(),
    origin: entry.origin.slice(),
    rotation: entry.rotation.slice(),
    texture: textureId,
    uv: [reservation.x, reservation.y],
    mirrorUv: !!entry.mirrorUv,
    inflate: entry.inflate || 0,
    fractionalReason: entry.fractionalReason,
    angleReason: entry.angleReason,
    ...extra,
  });
  const mirrorReservation = (entry, kind, width, height, mirrorOf) => {
    if (typeof mirrorOf !== "string" || !byId.has(mirrorOf)) throw new Error("atlas-invalid-mirrorOf: " + entry.id);
    const earlier = byId.get(mirrorOf);
    if (earlier.kind !== kind || earlier.reservation.width !== width || earlier.reservation.height !== height) throw new Error("atlas-mirror-dimensions-mismatch: " + entry.id);
    return earlier.reservation;
  };
  const addBox = (spec, paintSpec) => {
    const base = baseSpec(spec, "box");
    const to = vector(spec.to);
    if ((!integer3(base.from) || !integer3(to) || !integer3(base.origin)) && !spec.fractionalReason) throw new Error("atlas-box-fractional-coordinates-need-reason: " + base.id);
    const size = [to[0] - base.from[0], to[1] - base.from[1], to[2] - base.from[2]];
    if (size.some(value => !Number.isInteger(value) || value <= 0)) throw new Error("atlas-invalid-box-dimensions: " + base.id);
    const width = 2 * size[0] + 2 * size[2], height = size[2] + size[1];
    let reservation;
    const mirrored = spec.mirrorOf !== undefined;
    if (mirrored) {
      if (paintSpec !== undefined) throw new Error("atlas-mirror-paint-forbidden: " + base.id);
      reservation = mirrorReservation(base, "box", width, height, spec.mirrorOf);
    } else {
      reservation = reserve("box", width, height, base);
      paint(reservation, paintSpec, "box");
    }
    const entry = { ...base, kind: "box", reservation, mirrorOf: mirrored ? spec.mirrorOf : null, mirrorUv: mirrored };
    entry.to = to;
    entry.inflate = spec.inflate === undefined ? 0 : spec.inflate;
    entry.cullface = spec.cullface === undefined ? null : spec.cullface;
    entry.fractionalReason = spec.fractionalReason;
    entry.angleReason = spec.angleReason;
    byId.set(base.id, entry);
    entries.push(entry);
    return makeCube(entry, reservation, { cullface: entry.cullface });
  };
  const addSprite = spec => {
    const base = baseSpec(spec, "sprite");
    if ((!integer3(base.from) || !integer3(base.origin)) && !spec.fractionalReason) throw new Error("atlas-sprite-fractional-coordinates-need-reason: " + base.id);
    if (typeof spec.axis !== "string" || !["x", "y", "z"].includes(spec.axis)) throw new Error("atlas-invalid-sprite-axis: " + base.id);
    const axis = axisIndex(spec.axis);
    const planar = planarAxes(axis);
    const requestedSide = spec.side === undefined ? (spec.doubleSided === true ? "both" : undefined) : spec.side;
    if (requestedSide !== undefined && !["positive", "negative", "both"].includes(requestedSide)) throw new Error("atlas-invalid-sprite-side: " + base.id);
    const mirrored = spec.mirrorOf !== undefined;
    let mask = spec.mask;
    let width;
    let height;
    let reservation;
    let earlier = null;
    if (mirrored) {
      if (mask !== undefined) throw new Error("atlas-mirror-mask-forbidden: " + base.id);
      earlier = byId.get(spec.mirrorOf);
      if (!earlier || earlier.kind !== "sprite" || earlier.axis !== spec.axis) throw new Error("atlas-invalid-mirrorOf: " + base.id);
      width = earlier.width;
      height = earlier.height;
      reservation = mirrorReservation(base, "sprite", width, height, spec.mirrorOf);
    } else {
      if (!Array.isArray(mask) || mask.length < 1 || typeof mask[0] !== "string" || mask[0].length < 1 || mask.some(row => typeof row !== "string" || row.length !== mask[0].length)) throw new Error("atlas-invalid-sprite-mask: " + base.id);
      width = mask[0].length;
      height = mask.length;
      let opaque = false;
      for (const row of mask) for (const ch of row) {
        if (ch === "_") continue;
        if (!Object.prototype.hasOwnProperty.call(atlasPalette, ch)) throw new Error("atlas-invalid-sprite-mask: " + base.id);
        opaque = true;
      }
      if (!opaque) throw new Error("atlas-empty-sprite-mask: " + base.id);
      reservation = reserve("sprite", width, height, base);
      reservation.pixels = mask.slice();
    }
    const to = base.from.slice();
    to[planar[0]] += width;
    to[planar[1]] += height;
    const side = requestedSide || (earlier ? normalizedPlateSide(earlier) : "positive");
    const entry = { ...base, kind: "sprite", axis: spec.axis, side, plateSide: side, width, height, to, reservation, mirrorOf: mirrored ? spec.mirrorOf : null, mirrorUv: mirrored, inflate: 0, fractionalReason: spec.fractionalReason, angleReason: spec.angleReason };
    byId.set(base.id, entry);
    entries.push(entry);
    return makeCube(entry, reservation, { plate: true, plateAxis: spec.axis, plateSide: side, translucent: true, cullface: null });
  };
  const finish = () => {
    if (!entries.length) throw new Error("atlas-empty");
    const height = nextPowerOfTwo(Math.max(1, usedHeight));
    if (height > maxHeight) throw new Error("atlas-too-tall");
    const rows = Array.from({ length: height }, () => "_".repeat(maxWidth).split(""));
    for (const reservation of reservations) {
      if (!reservation.pixels) throw new Error("atlas-unpainted: " + reservation.id);
      for (let y = 0; y < reservation.height; y++) for (let x = 0; x < reservation.width; x++) rows[reservation.y + y][reservation.x + x] = reservation.pixels[y][x];
    }
    const placements = Object.create(null);
    for (const entry of entries) placements[entry.id] = { kind: entry.kind, uv: [entry.reservation.x, entry.reservation.y], width: entry.reservation.width, height: entry.reservation.height, mirrorOf: entry.mirrorOf };
    return {
      cubes: entries.map(entry => makeCube(entry, entry.reservation, entry.kind === "sprite" ? { plate: true, plateAxis: entry.axis, plateSide: entry.plateSide, translucent: true, cullface: null } : { cullface: entry.cullface })),
      texture: { id: textureId, name: generatedName(modelId, "texture", textureId), width: maxWidth, height, palette: { ...atlasPalette }, pixels: rows.map(row => row.join("")) },
      placements,
    };
  };
  return { addBox, addSprite, finish };
}

function resolveSurfaceOverlaps(model, records, rules) {
  const surface = (records || []).filter(record => record.classification === "surface");
  const pending = Array.isArray(rules) ? rules : [];
  const used = new Set();
  const pairKey = (a, b) => [a, b].sort().join("|");
  for (const record of surface) {
    const a = findElement(model, record.a);
    const b = findElement(model, record.b);
    if (!a || !b) throw new Error("unhandled-surface-overlap: " + record.a + "," + record.b);
    const key = pairKey(a.id, b.id);
    const matches = pending.map((rule, index) => ({ rule, index })).filter(({ rule, index }) => !used.has(index) && Array.isArray(rule.between) && rule.between.length === 2 && pairKey(rule.between[0], rule.between[1]) === key);
    if (!matches.length) throw new Error("unhandled-surface-overlap: " + a.id + "," + b.id);
    if (matches.length > 1) throw new Error("ambiguous-surface-overlap: " + a.id + "," + b.id);
    const { rule, index } = matches[0];
    if (rule.element !== a.id && rule.element !== b.id) throw new Error("unhandled-surface-overlap: " + a.id + "," + b.id);
    const target = findElement(model, rule.element);
    if (!target) throw new Error("unhandled-surface-overlap: " + rule.element);
    if (rule.action === "erase_plate_pixels") {
      if (!target.plate || !Array.isArray(record.pixels) || target.id !== a.id) throw new Error("unhandled-surface-overlap: " + a.id + "," + b.id);
      erasePixels(model, target.id, record.pixels);
    } else if (rule.action === "hide_face") {
      if (typeof rule.face !== "string") throw new Error("unhandled-surface-overlap: " + a.id + "," + b.id);
      hideFace(model, target.id, rule.face);
    } else if (rule.action === "delete_element") {
      deleteElement(model, target.id);
    } else {
      throw new Error("unhandled-surface-overlap: " + a.id + "," + b.id);
    }
    used.add(index);
  }
  for (let index = 0; index < pending.length; index++) if (!used.has(index)) throw new Error("unused-overlap-rule: " + JSON.stringify(pending[index]));
}

function plateFaceMapping(axis, face, mirrorUv) {
  let rotation = 0;
  let flipU = !!mirrorUv;
  let flipV = true;
  if (axis === "x") {
    if (face === "east") { rotation = 90; flipU = !flipU; }
    else if (face === "west") rotation = 270;
  } else if (axis === "y") {
    if (face === "down") flipV = false;
  } else if (axis === "z") {
    if (face === "north") rotation = 180;
    else if (face === "south") flipV = false;
  }
  return { rotation, flipU, flipV };
}
function plateFaceUv(axis, face, u, v, width, height, mirrorUv) {
  const mapping = plateFaceMapping(axis, face, mirrorUv);
  return {
    uv: [
      mapping.flipU ? u + width : u,
      mapping.flipV ? v + height : v,
      mapping.flipU ? u : u + width,
      mapping.flipV ? v : v + height,
    ],
    rotation: mapping.rotation,
  };
}

function cubeFaces(c) {
  const w = c.to[0] - c.from[0], h = c.to[1] - c.from[1], d = c.to[2] - c.from[2];
  const u = c.uv[0], v = c.uv[1];
  if (c.plate) {
    const planar = planarAxes(axisIndex(c.plateAxis));
    const pw = c.to[planar[0]] - c.from[planar[0]];
    const ph = c.to[planar[1]] - c.from[planar[1]];
    const enabled = new Set(plateFaceNames(c.plateAxis, normalizedPlateSide(c)));
    const makeFace = face => {
      const mapping = plateFaceUv(c.plateAxis, face, u, v, pw, ph, !!c.mirrorUv);
      return { uv: mapping.uv, rotation: mapping.rotation, enabled: enabled.has(face) };
    };
    return {
      north: makeFace("north"),
      south: makeFace("south"),
      east: makeFace("east"),
      west: makeFace("west"),
      up: makeFace("up"),
      down: makeFace("down"),
    };
  }
  let face_list = [
    { face: "east",  from: [0, d],         size: [d, h] },
    { face: "west",  from: [d + w, d],     size: [d, h] },
    { face: "up",    from: [d + w, d],     size: [-w, -d] },
    { face: "down",  from: [d + w * 2, 0], size: [-w, d] },
    { face: "south", from: [d * 2 + w, d], size: [w, h] },
    { face: "north", from: [d, d],         size: [w, h] },
  ];
  if (c.mirrorUv) {
    face_list.forEach(f => { f.from[0] += f.size[0]; f.size[0] *= -1; });
    const p = { from: face_list[0].from.slice(), size: face_list[0].size.slice() };
    face_list[0].from = face_list[1].from.slice();
    face_list[0].size = face_list[1].size.slice();
    face_list[1].from = p.from.slice();
    face_list[1].size = p.size.slice();
  }
  const faces = {};
  for (const f of face_list) {
    faces[f.face] = { uv: [f.from[0] + u, f.from[1] + v, f.from[0] + f.size[0] + u, f.from[1] + f.size[1] + v], rotation: 0, enabled: true };
  }
  return faces;
}

function parsePaletteColor(value) {
  if (typeof value === "string" && /^#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?$/.test(value)) {
    const hex = value.slice(1);
    const rgba = hex.length === 6 ? hex + "ff" : hex;
    return [parseInt(rgba.slice(0, 2), 16), parseInt(rgba.slice(2, 4), 16), parseInt(rgba.slice(4, 6), 16), parseInt(rgba.slice(6, 8), 16)];
  }
  if (Array.isArray(value) && value.length === 4 && value.every(channel => Number.isInteger(channel) && channel >= 0 && channel <= 255)) {
    return value.slice();
  }
  return null;
}

function expandPixels(texture) {
  const out = new Uint8ClampedArray(texture.width * texture.height * 4);
  for (let r = 0; r < texture.height; r++) {
    const row = texture.pixels[r];
    for (let c = 0; c < texture.width; c++) {
      const ch = row[c];
      const base = (r * texture.width + c) * 4;
      if (ch === "_") continue;
      const rgba = parsePaletteColor(texture.palette[ch]);
      if (!rgba) throw new Error("invalid-palette-color: " + texture.id + "." + ch);
      out[base] = rgba[0];
      out[base + 1] = rgba[1];
      out[base + 2] = rgba[2];
      out[base + 3] = rgba[3];
    }
  }
  return out;
}
function createGlowMaskTexture(options) {
  const config = options || {};
  const source = config.source;
  const id = config.id;
  const prefix = config.prefix;
  const palette = config.palette && typeof config.palette === "object" ? config.palette : {};
  const pixels = config.pixels;
  const modelId = modelIdFromPrefix(prefix);
  if (!source || typeof source !== "object" || !Number.isInteger(source.width) || !Number.isInteger(source.height) || !Array.isArray(source.pixels) || !source.palette) throw new Error("glowmask-invalid-source");
  if (typeof id !== "string" || !validLocalId(id) || id !== modelId + "_glowmask") throw new Error("glowmask-invalid-id");
  for (const key of Object.keys(palette)) if (key.length !== 1 || key === "_" || !parsePaletteColor(palette[key]) || parsePaletteColor(palette[key])[3] === 0) throw new Error("glowmask-invalid-palette: " + key);
  if (!Array.isArray(pixels) || pixels.length !== source.height || pixels.some(row => typeof row !== "string" || row.length !== source.width)) throw new Error("glowmask-invalid-pixels");
  for (const row of pixels) for (const ch of row) if (ch !== "_" && !Object.prototype.hasOwnProperty.call(palette, ch)) throw new Error("glowmask-invalid-pixel: " + ch);
  const texture = {
    id,
    name: generatedName(modelId, "texture", id, "glowmask"),
    width: source.width,
    height: source.height,
    palette: { ...palette },
    pixels: pixels.slice(),
    role: "glowmask",
    sourceTexture: source.id,
  };
  const sourceData = expandPixels(source);
  const maskData = expandPixels(texture);
  let emissivePixels = 0;
  for (let i = 0; i < source.width * source.height; i++) {
    if (maskData[i * 4 + 3] <= 0) continue;
    emissivePixels++;
    if (sourceData[i * 4 + 3] <= 0) throw new Error("glowmask-pixel-outside-source: " + id);
  }
  if (!emissivePixels) throw new Error("glowmask-empty: " + id);
  return texture;
}


function buildModel(MODEL, TEXTURES, ANIMATIONS) {
  const model = {
    id: MODEL.id,
    name: MODEL.id,
    eulerOrder: "ZYX",
    groups: Object.create(null),
    groupOrder: [],
    elements: Object.create(null),
    textures: Object.create(null),
    animations: [],
    resolutions: [],
    hiddenFaces: [],
    erasedPixels: [],
    deletedElements: [],
    interiorOverlaps: [],
    centering: null,
  };
  const declaredGroupIds = [];
  for (const g of MODEL.groups) {
    declaredGroupIds.push(g.id);
    model.groups[g.id] = { kind: "group", id: g.id, name: generatedName(MODEL.id, "group", g.id), parent: g.parent, origin: g.origin.slice(), rotation: g.rotation.slice(), scale: [1, 1, 1], deleted: false };
  }
  model.groupOrder = parentFirstGroupIds(model.groups, declaredGroupIds) || declaredGroupIds;
  for (const t of TEXTURES) {
    const role = t.role || "diffuse";
    model.textures[t.id] = { id: t.id, name: generatedName(MODEL.id, "texture", t.id, role), width: t.width, height: t.height, pixels: expandPixels(t), role, sourceTexture: t.sourceTexture || null, deleted: false };
  }
  for (const c of MODEL.cubes) {
    model.elements[c.id] = {
      kind: "cube",
      id: c.id,
      name: generatedName(MODEL.id, "cube", c.id),
      parent: c.parent,
      from: c.from.slice(),
      to: c.to.slice(),
      origin: c.origin.slice(),
      rotation: c.rotation.slice(),
      texture: c.texture,
      uv: c.uv.slice(),
      mirrorUv: !!c.mirrorUv,
      inflate: c.inflate || 0,
      plate: !!c.plate,
      plateAxis: c.plateAxis || null,
      plateSide: c.plateSide || c.side || (c.doubleSided ? "both" : "positive"),
      translucent: !!c.translucent,
      cullface: c.cullface || null,
      faces: cubeFaces(c),
      deleted: false,
    };
  }
  model.animations = buildAnimations(MODEL.id, ANIMATIONS);
  return model;
}

function buildAnimations(modelId, ANIMATIONS) {
  return (ANIMATIONS || []).map(a => ({
    id: a.id,
    name: generatedName(modelId, "animation", a.id),
    length: a.length,
    loop: a.loop,
    tracks: (a.tracks || []).map(tr => ({
      bone: tr.bone,
      channel: tr.channel,
      interpolation: tr.interpolation || "linear",
      keyframes: (tr.keyframes || []).map(kf => ({ time: kf.time, value: kf.value.slice(), interpolation: kf.interpolation || "linear" })),
    })),
  }));
}

function validateMinecraftModel(MODEL, TEXTURES, ANIMATIONS, formatInfo) {
  const errors = [];
  const warnings = [];
  if (!validLocalId(MODEL.id)) errors.push("model id must match ^[a-z0-9]+(?:_[a-z0-9]+)*$ and not be reserved");
  const groupIds = new Set();
  const groupMap = Object.create(null);
  const groupOrder = [];
  const groupList = MODEL.groups || [];
  for (const g of groupList) {
    if (!validLocalId(g.id)) errors.push("group id must match ^[a-z0-9]+(?:_[a-z0-9]+)*$ and not be reserved: " + g.id);
    else if (groupIds.has(g.id)) errors.push("duplicate group id " + g.id);
    else {
      groupIds.add(g.id);
      groupMap[g.id] = g;
      groupOrder.push(g.id);
    }
    if (!vec3(g.origin) || !vec3(g.rotation)) errors.push("group " + g.id + " invalid vectors");
    else {
      if (!integer3(g.origin) && !g.fractionalReason) errors.push("group " + g.id + " fractional origin needs fractionalReason");
    }
  }
  for (const g of groupList) if (g.parent !== null && !groupIds.has(g.parent)) errors.push("group " + g.id + " parent " + g.parent + " missing");
  if (!parentFirstGroupIds(groupMap, groupOrder)) errors.push("group parent graph contains a cycle");
  const textureIds = new Set();
  for (const t of TEXTURES || []) {
    if (!validLocalId(t.id)) errors.push("texture id must match ^[a-z0-9]+(?:_[a-z0-9]+)*$ and not be reserved: " + t.id);
    else if (textureIds.has(t.id)) errors.push("duplicate texture id " + t.id);
    else textureIds.add(t.id);
    if (t.role !== undefined && t.role !== "diffuse" && t.role !== "glowmask") errors.push("texture " + t.id + " invalid role");

    const palette = t.palette && typeof t.palette === "object" ? t.palette : {};
    for (const key of Object.keys(palette)) {
      if (key.length !== 1 || key === "_" || !parsePaletteColor(palette[key])) errors.push("texture " + t.id + " invalid palette entry " + key);
    }
    if (!Number.isInteger(t.width) || !Number.isInteger(t.height) || t.width <= 0 || t.height <= 0) errors.push("texture " + t.id + " invalid dimensions");
    else {
      if ((t.pixels || []).length !== t.height) errors.push("texture " + t.id + " row count mismatch");
      for (const row of t.pixels || []) if (row.length !== t.width) errors.push("texture " + t.id + " row length mismatch");
      for (const row of t.pixels || []) for (const ch of row) if (ch !== "_" && (!Object.prototype.hasOwnProperty.call(palette, ch) || !parsePaletteColor(palette[ch]))) errors.push("texture " + t.id + " palette char " + ch);
    }
    if (formatInfo && formatInfo.maxTextureSize && (t.width > formatInfo.maxTextureSize || t.height > formatInfo.maxTextureSize)) {
      warnings.push("texture " + t.id + " exceeds format texture limit " + formatInfo.maxTextureSize);
    }
  }
  for (const glow of TEXTURES || []) {
    if (glow.role !== "glowmask") continue;
    for (const key of Object.keys(glow.palette || {})) {
      const rgba = parsePaletteColor(glow.palette[key]);
      if (rgba && rgba[3] === 0) errors.push("glowmask " + glow.id + " palette entry " + key + " has zero alpha");
    }
    if (typeof glow.id !== "string" || glow.id !== MODEL.id + "_glowmask" || !/^[a-z0-9]+(?:_[a-z0-9]+)*_glowmask$/.test(glow.id)) errors.push("glowmask " + glow.id + " invalid id");
    if (typeof glow.name !== "string" || glow.name !== MODEL.id + "_glowmask") errors.push("glowmask " + glow.id + " invalid name");
    const source = TEXTURES.find(texture => texture.id === glow.sourceTexture);
    if (!source) {
      errors.push("glowmask " + glow.id + " source texture missing");
      continue;
    }
    if (source.role === "glowmask") errors.push("glowmask " + glow.id + " source cannot be a glowmask");
    if (source.width !== glow.width || source.height !== glow.height) errors.push("glowmask " + glow.id + " dimensions must match source texture");
    if (!Number.isInteger(glow.width) || !Number.isInteger(glow.height) || glow.width < 1 || glow.height < 1 || !Array.isArray(glow.pixels) || glow.pixels.length !== glow.height || glow.pixels.some(row => typeof row !== "string" || row.length !== glow.width)) continue;
    try {
      const sourceData = expandPixels(source);
      const glowData = expandPixels(glow);
      let emissivePixels = 0;
      for (let i = 0; i < glow.width * glow.height; i++) {
        if (glowData[i * 4 + 3] <= 0) continue;
        emissivePixels++;
        if (sourceData[i * 4 + 3] <= 0) {
          errors.push("glowmask " + glow.id + " emits outside source texture");
          break;
        }
      }
      if (!emissivePixels) errors.push("glowmask " + glow.id + " has no emissive pixels");
    } catch {}
  }

  const uvRegions = new Map();
  const uvRects = [];
  const cubeIds = new Set();
  const addUvRect = (c, face, x, y, w, h) => {
    if (w > 0 && h > 0) uvRects.push({ cube: c, face: face, rect: [x, y, x + w, y + h] });
  };
  for (const c of MODEL.cubes || []) {
    if (!validLocalId(c.id)) errors.push("cube id must match ^[a-z0-9]+(?:_[a-z0-9]+)*$ and not be reserved: " + c.id);
    else if (cubeIds.has(c.id)) errors.push("duplicate cube id " + c.id);
    else cubeIds.add(c.id);
    if (c.parent !== null && !groupIds.has(c.parent)) errors.push("cube " + c.id + " parent " + c.parent + " missing");
    if (!vec3(c.from) || !vec3(c.to) || !vec3(c.origin) || !vec3(c.rotation)) errors.push("cube " + c.id + " invalid vectors");
    else {
      if ((!integer3(c.from) || !integer3(c.to) || !integer3(c.origin)) && !c.fractionalReason) errors.push("cube " + c.id + " fractional coordinates need fractionalReason");
      for (let i = 0; i < 3; i++) {
        const size = c.to[i] - c.from[i];
        if (Math.abs(size - Math.round(size)) > EPSILON) errors.push("cube " + c.id + " non-integer size on axis " + i);
      }
    }
    const validUv = Array.isArray(c.uv) && c.uv.length === 2 && Number.isInteger(c.uv[0]) && Number.isInteger(c.uv[1]);
    if (!validUv) errors.push("cube " + c.id + " invalid uv");
    if (Array.isArray(c.uv) && c.uv.length === 2 && c.uv.some(value => typeof value === "number" && value < 0)) errors.push("cube " + c.id + " uv must be non-negative");
    if (!Number.isInteger(c.inflate) || c.inflate < 0) errors.push("cube " + c.id + " invalid inflate");
    if (!textureIds.has(c.texture)) errors.push("cube " + c.id + " texture " + c.texture + " missing");
    let zeroAxes = 0;
    for (let i = 0; i < 3; i++) {
      if (c.from[i] > c.to[i]) errors.push("cube " + c.id + " from > to on axis " + i);
      if (c.from[i] === c.to[i]) zeroAxes++;
    }
    if (c.plate) {
      if (zeroAxes !== 1) errors.push("plate " + c.id + " must have exactly one zero axis");
      if (!c.translucent) errors.push("plate " + c.id + " needs translucent");
      if (typeof c.plateAxis !== "string" || !["x", "y", "z"].includes(c.plateAxis)) errors.push("plate " + c.id + " needs plateAxis");
      else if (c.from[axisIndex(c.plateAxis)] !== c.to[axisIndex(c.plateAxis)]) errors.push("plate " + c.id + " plateAxis does not match zero axis");
      if (c.inflate !== 0) errors.push("plate " + c.id + " inflate must be 0");
      if (c.cullface) errors.push("plate " + c.id + " cullface not allowed");
      if (!["positive", "negative", "both"].includes(normalizedPlateSide(c))) errors.push("plate " + c.id + " invalid side");
    } else {
      if (zeroAxes > 0) errors.push("cube " + c.id + " zero axis without plate");
      if (c.cullface && !FACE_ORDER.includes(c.cullface)) errors.push("cube " + c.id + " invalid cullface");
    }
    if (!multipleOf225(c.rotation) && !c.angleReason) errors.push("cube " + c.id + " non-22.5 angle needs angleReason");
    if (textureIds.has(c.texture) && validUv) {
      const tex = TEXTURES.find(t => t.id === c.texture);
      if (c.plate) {
        const planar = planarAxes(axisIndex(c.plateAxis));
        const pw = c.to[planar[0]] - c.from[planar[0]];
        const ph = c.to[planar[1]] - c.from[planar[1]];
        if (c.uv[0] + pw > tex.width || c.uv[1] + ph > tex.height) errors.push("cube " + c.id + " uv outside texture");
        const key = "p:" + c.uv[0] + "," + c.uv[1] + "," + pw + "," + ph;
        if (!uvRegions.has(key)) uvRegions.set(key, []);
        uvRegions.get(key).push(c);
        addUvRect(c, "plate", c.uv[0], c.uv[1], pw, ph);
      } else {
        const w = c.to[0] - c.from[0], h = c.to[1] - c.from[1], d = c.to[2] - c.from[2];
        if (c.uv[0] + 2 * w + 2 * d > tex.width || c.uv[1] + h + d > tex.height) errors.push("cube " + c.id + " uv outside texture");
        const key = "s:" + c.uv[0] + "," + c.uv[1] + "," + w + "," + h + "," + d;
        if (!uvRegions.has(key)) uvRegions.set(key, []);
        uvRegions.get(key).push(c);
        addUvRect(c, "east", c.uv[0], c.uv[1] + d, d, h);
        addUvRect(c, "west", c.uv[0] + d + w, c.uv[1] + d, d, h);
        addUvRect(c, "up", c.uv[0] + w, c.uv[1], d, d);
        addUvRect(c, "down", c.uv[0] + d + w, c.uv[1], d, d);
        addUvRect(c, "south", c.uv[0] + 2 * d + w, c.uv[1] + d, w, h);
        addUvRect(c, "north", c.uv[0] + d, c.uv[1] + d, w, h);
      }
    }
  }
  const allowedUvPairs = new Set();
  for (const list of uvRegions.values()) {
    if (list.length < 2) continue;
    const mirrors = list.filter(c => c.mirrorUv);
    if (list.length === 2 && mirrors.length === 1) {
      allowedUvPairs.add(list.map(c => c.id).sort().join("|"));
    } else {
      errors.push("duplicate uv region for " + list.map(c => c.id).join(", "));
    }
  }
  const norm = r => [Math.min(r[0], r[2]), Math.min(r[1], r[3]), Math.max(r[0], r[2]), Math.max(r[1], r[3])];
  for (let i = 0; i < uvRects.length; i++) for (let j = i + 1; j < uvRects.length; j++) {
    const a = uvRects[i], b = uvRects[j];
    if (a.cube.id === b.cube.id) continue;
    const ar = norm(a.rect), br = norm(b.rect);
    const overlapX = Math.min(ar[2], br[2]) - Math.max(ar[0], br[0]);
    const overlapY = Math.min(ar[3], br[3]) - Math.max(ar[1], br[1]);
    if (overlapX <= EPSILON || overlapY <= EPSILON) continue;
    const exact = ar[0] === br[0] && ar[1] === br[1] && ar[2] === br[2] && ar[3] === br[3];
    if (exact && allowedUvPairs.has([a.cube.id, b.cube.id].sort().join("|"))) continue;
    errors.push("overlapping uv regions " + a.cube.id + "." + a.face + " and " + b.cube.id + "." + b.face);
  }
  const animationErrors = validateAnimations(ANIMATIONS, groupIds, formatInfo);
  for (const e of animationErrors) errors.push(e);
  return { errors, warnings };
}

function validateAnimations(ANIMATIONS, groupIds, formatInfo) {
  const errors = [];
  const animations = ANIMATIONS === undefined ? [] : Array.isArray(ANIMATIONS) ? ANIMATIONS : [];
  if (ANIMATIONS !== undefined && !Array.isArray(ANIMATIONS)) errors.push("animations must be an array");
  const hasGroup = value => groupIds instanceof Set ? groupIds.has(value) : Array.isArray(groupIds) && groupIds.includes(value);
  const animationIds = new Set();
  for (const a of animations) {
    if (!a || typeof a !== "object") { errors.push("animation entry invalid"); continue; }
    if (!validLocalId(a.id)) errors.push("animation id must match ^[a-z0-9]+(?:_[a-z0-9]+)*$ and not be reserved: " + a.id);
    else if (animationIds.has(a.id)) errors.push("duplicate animation id " + a.id);
    else animationIds.add(a.id);
    if (!["once", "hold", "loop"].includes(a.loop)) errors.push("animation " + a.id + " invalid loop mode");
    if (!Number.isFinite(a.length) || a.length < 0) errors.push("animation " + a.id + " invalid length");
    else if (Math.abs(a.length / 0.05 - Math.round(a.length / 0.05)) > 1e-9) errors.push("animation " + a.id + " length not on 20-tick grid");
    const tracks = a.tracks === undefined ? [] : Array.isArray(a.tracks) ? a.tracks : [];
    if (a.tracks !== undefined && !Array.isArray(a.tracks)) errors.push("animation " + a.id + " tracks must be an array");
    const trackTargets = new Set();
    for (const tr of tracks) {
      if (!tr || typeof tr !== "object") { errors.push("animation " + a.id + " track invalid"); continue; }
      if (!hasGroup(tr.bone)) errors.push("animation " + a.id + " bone " + tr.bone + " missing");
      if (!["rotation", "position", "scale"].includes(tr.channel)) errors.push("animation " + a.id + " invalid channel " + tr.channel);
      const target = String(tr.bone) + "\u0000" + String(tr.channel);
      if (trackTargets.has(target)) errors.push("animation " + a.id + " duplicate track " + tr.bone + "." + tr.channel);
      else trackTargets.add(target);
      const keyframes = tr.keyframes === undefined ? [] : Array.isArray(tr.keyframes) ? tr.keyframes : [];
      if (tr.keyframes !== undefined && !Array.isArray(tr.keyframes)) errors.push("animation " + a.id + " keyframes must be an array");
      let prev = -1;
      for (const kf of keyframes) {
        if (!Number.isFinite(kf.time) || kf.time < 0 || kf.time > a.length + 1e-9) errors.push("animation " + a.id + " keyframe time out of range");
        else if (Math.abs(kf.time / 0.05 - Math.round(kf.time / 0.05)) > 1e-9) errors.push("animation " + a.id + " sub-tick keyframe time " + kf.time);
        if (kf.time <= prev) errors.push("animation " + a.id + " keyframe times not increasing");
        prev = kf.time;
        if (!vec3(kf.value)) errors.push("animation " + a.id + " invalid keyframe value");
        else if (tr.channel === "scale" && kf.value.some(value => value <= 0)) errors.push("animation " + a.id + " scale values must be positive");
      }
      if (tr.channel === "scale" && Number.isFinite(a.length) && a.length >= 0) {
        const sampleTimes = new Set();
        const steps = Math.round(a.length / 0.05);
        for (let k = 0; k <= steps; k++) sampleTimes.add(Math.round(k * 0.05 * 1e6) / 1e6);
        for (const kf of keyframes) if (Number.isFinite(kf.time) && kf.time >= 0 && kf.time <= a.length) sampleTimes.add(kf.time);
        const ordered = [...sampleTimes].sort((x, y) => x - y);
        for (let i = 0; i < ordered.length - 1; i++) {
          const t0 = ordered[i], dt = ordered[i + 1] - t0;
          for (let k = 1; k <= 3; k++) sampleTimes.add(Math.round((t0 + (dt * k) / 4) * 1e6) / 1e6);
        }
        for (const time of [...sampleTimes].sort((x, y) => x - y)) {
          const value = poseAt(time, a, tr.bone, tr.channel);
          if (value.x <= 0 || value.y <= 0 || value.z <= 0) {
            errors.push("animation " + a.id + " scale interpolation must stay positive on " + tr.bone + " at t=" + time);
            break;
          }
        }
      }
    }
  }
  const loopErrors = validateLoopCoherence(animations, groupIds).errors;
  for (const e of loopErrors) errors.push(e);
  if (animations.length && formatInfo && !formatInfo.animation_mode) errors.push("format " + formatInfo.id + " has no animation mode");
  return errors;
}

function buildBlockbench(model, ANIMATIONS) {
  const modelId = model.id;
  const nodeName = (name, kind) => typeof name === "string" && (name.indexOf(kind + "_") === 0 || name.indexOf(modelId + "_" + kind + "_") === 0);
  const elementTargets = [];
  const groupTargets = [];
  const groupTargetSet = new Set();
  const textureTargets = [];
  const animationTargets = [];
  for (const t of Texture.all) if (ownsGeneratedName(t.name, modelId, "texture")) textureTargets.push(t);
  const diffuseTarget = textureTargets.find(t => t.name === modelId) || null;
  for (const c of Cube.all) if (nodeName(c.name, "cube") && diffuseTarget && cubeUsesTexture(c, diffuseTarget)) elementTargets.push(c);
  for (const cube of elementTargets) {
    const seen = new Set();
    let parent = cube.parent;
    while (parent && typeof parent === "object" && !seen.has(parent)) {
      seen.add(parent);
      if (nodeName(parent.name, "group")) groupTargetSet.add(parent);
      parent = parent.parent;
    }
  }
  groupTargets.push(...groupTargetSet);
  for (const a of Animation.all) if (ownsGeneratedName(a.name, modelId, "animation")) animationTargets.push(a);
  const created = [];
  const editAspects = { elements: elementTargets, groups: groupTargets, textures: textureTargets, animations: animationTargets, outliner: true };
  Undo.initEdit(editAspects);
  try {
  for (const t of elementTargets) t.remove();
  for (const g of groupTargets) g.remove();
  for (const t of textureTargets) t.remove();
  for (const a of animationTargets) a.remove();
  const liveGroups = Object.create(null);
  const groupIds = parentFirstGroupIds(model.groups, model.groupOrder || Object.keys(model.groups));
  if (!groupIds) throw new Error("group-parent-cycle");
  for (const gid of groupIds) {
    const g = model.groups[gid];
    if (g.deleted) continue;
    const parent = g.parent && model.groups[g.parent] && !model.groups[g.parent].deleted ? liveGroups[g.parent] : "root";
    const group = new Group({ name: g.name, origin: g.origin, rotation: g.rotation }).init().addTo(parent);
    liveGroups[gid] = group;
  }
  const maxExistingTextureSize = { width: Number(Project.texture_width) || 0, height: Number(Project.texture_height) || 0 };
  for (const existing of Texture.all) {
    maxExistingTextureSize.width = Math.max(maxExistingTextureSize.width, existing.canvas ? existing.canvas.width : existing.width || 0);
    maxExistingTextureSize.height = Math.max(maxExistingTextureSize.height, existing.canvas ? existing.canvas.height : existing.height || 0);
  }
  const liveTextures = Object.create(null);
  for (const tid of Object.keys(model.textures)) {
    const t = model.textures[tid];
    if (t.deleted) continue;
    liveTextures[tid] = new Texture({ name: t.name, width: t.width, height: t.height });
  }
  paintTexturePixels(model, liveTextures);
  for (const tid of Object.keys(liveTextures)) {
    const tex = liveTextures[tid];
    tex.fromDataURL(tex.canvas.toDataURL());
    tex.uv_width = tex.canvas.width;
    tex.uv_height = tex.canvas.height;
    tex.add();
    syncTextureSource(tex);
    maxExistingTextureSize.width = Math.max(maxExistingTextureSize.width, tex.canvas.width);
    maxExistingTextureSize.height = Math.max(maxExistingTextureSize.height, tex.canvas.height);
  }
  if (Project) {
    Project.texture_width = Math.max(Number(Project.texture_width) || 0, maxExistingTextureSize.width);
    Project.texture_height = Math.max(Number(Project.texture_height) || 0, maxExistingTextureSize.height);
    if (typeof updateProjectResolution === "function") updateProjectResolution();
  }
  for (const eid of Object.keys(model.elements)) {
    const e = model.elements[eid];
    if (e.deleted) continue;
    const from = e.from.slice();
    const to = e.to.slice();
    if (e.plate) to[axisIndex(e.plateAxis)] = from[axisIndex(e.plateAxis)] + 0.001;
    const parent = e.parent && model.groups[e.parent] && !model.groups[e.parent].deleted ? liveGroups[e.parent] : "root";
    const cube = new Cube({ name: e.name, from, to, origin: e.origin, rotation: e.rotation, texture: liveTextures[e.texture], box_uv: !e.plate, uv_offset: e.uv.slice(), mirror_uv: e.mirrorUv, inflate: e.inflate }).init().addTo(parent);
    for (const face of FACE_ORDER) {
      cube.faces[face].uv = e.faces[face].uv;
      cube.faces[face].rotation = Number.isFinite(e.faces[face].rotation) ? e.faces[face].rotation : 0;
      cube.faces[face].enabled = e.faces[face].enabled;
    }
    created.push(cube);
  }
  createAnimations(model, ANIMATIONS, liveGroups);
    return created;
  } catch (error) {
    closeFailedEdit(modelId + " model script failed", {
      elements: elementTargets.concat(created),
      groups: groupTargets,
      textures: textureTargets,
      animations: animationTargets,
      outliner: true,
    });
    throw error;
  }
}

function paintTexturePixels(model, textures) {
  for (const tid of Object.keys(model.textures)) {
    const src = model.textures[tid];
    if (src.deleted) continue;
    const tex = textures[tid];
    if (!tex) continue;
    const canvas = tex.canvas;
    if (canvas.width !== src.width) canvas.width = src.width;
    if (canvas.height !== src.height) canvas.height = src.height;
    const ctx = canvas.getContext("2d");
    const img = ctx.createImageData(src.width, src.height);
    for (let i = 0; i < src.width * src.height; i++) {
      img.data[i * 4] = src.pixels[i * 4];
      img.data[i * 4 + 1] = src.pixels[i * 4 + 1];
      img.data[i * 4 + 2] = src.pixels[i * 4 + 2];
      img.data[i * 4 + 3] = src.pixels[i * 4 + 3];
    }
    ctx.putImageData(img, 0, 0);
  }
}

function syncTextureSource(texture) {

  texture.updateSource(texture.canvas.toDataURL());
}

function closeFailedEdit(message, aspects) {
  if (typeof Undo === "undefined" || typeof Undo.finishEdit !== "function") return;
  try {
    Undo.finishEdit(message, aspects);
  } catch {}
}
function createAnimations(model, ANIMATIONS, liveGroups) {
  for (const a of model.animations) {
    for (const tr of a.tracks) if (!liveGroups[tr.bone]) throw new Error("animation-live-bone-missing: " + tr.bone);
  }
  for (const a of model.animations) {
    const anim = new Animation({ name: a.name, length: a.length, loop: a.loop }).add();
    for (const tr of a.tracks) {
      const animator = anim.getBoneAnimator(liveGroups[tr.bone]);
      if (!animator) throw new Error("animation-animator-unavailable: " + tr.bone);
      for (const kf of tr.keyframes) {
        animator.addKeyframe({
          time: kf.time,
          channel: tr.channel,
          data_points: [{ x: kf.value[0], y: kf.value[1], z: kf.value[2] }],
          interpolation: kf.interpolation || "linear",
        });
      }
    }
  }
}

function validateLoopCoherence(ANIMATIONS, groupIds) {
  const errors = [];
  const has = v => groupIds instanceof Set ? groupIds.has(v) : groupIds.indexOf(v) >= 0;
  const distance = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
  const magnitude = value => Math.hypot(value[0], value[1], value[2]);
  for (const a of ANIMATIONS) {
    if (!a || a.loop !== "loop") continue;
    const dynamicSeams = [];
    for (const tr of Array.isArray(a.tracks) ? a.tracks : []) {
      if (!tr || !has(tr.bone)) continue;
      const keyframes = Array.isArray(tr.keyframes) ? tr.keyframes : [];
      const t0 = keyframes.find(k => k && Math.abs(k.time) < 1e-9);
      const t1 = keyframes.find(k => k && Math.abs(k.time - a.length) < 1e-9);
      if (!t0) errors.push("animation " + a.id + " track " + tr.bone + "." + tr.channel + " missing time 0 keyframe");
      if (!t1) errors.push("animation " + a.id + " track " + tr.bone + "." + tr.channel + " missing time " + a.length + " keyframe");
      if (!t0 || !t1 || !vec3(t0.value) || !vec3(t1.value)) continue;
      let seamMatches = true;
      for (let i = 0; i < 3; i++) {
        if (Math.abs(t0.value[i] - t1.value[i]) > EPSILON) {
          errors.push("animation " + a.id + " seam mismatch " + tr.bone + "." + tr.channel);
          seamMatches = false;
          break;
        }
      }
      if (!seamMatches) continue;
      const startIndex = keyframes.indexOf(t0);
      const endIndex = keyframes.indexOf(t1);
      const previous = endIndex > 0 ? keyframes[endIndex - 1] : null;
      const next = startIndex >= 0 && startIndex + 1 < keyframes.length ? keyframes[startIndex + 1] : null;
      const outgoingMode = t0.interpolation || tr.interpolation || "linear";
      const endpointMode = t1.interpolation || tr.interpolation || "linear";
      const incomingMode = previous && (previous.interpolation || tr.interpolation || "linear");
      if (outgoingMode !== endpointMode || (incomingMode && incomingMode !== outgoingMode)) {
        errors.push("animation " + a.id + " seam interpolation mismatch " + tr.bone + "." + tr.channel);
      }
      const dynamic = keyframes.some(k => k && vec3(k.value) && distance(k.value, t0.value) > EPSILON);
      if (!dynamic || !previous || !next || !vec3(previous.value) || !vec3(next.value)) continue;
      const incomingSpan = t1.time - previous.time;
      const outgoingSpan = next.time - t0.time;
      if (incomingSpan <= EPSILON || outgoingSpan <= EPSILON) continue;
      const incomingDelta = distance(previous.value, t1.value);
      const outgoingDelta = distance(t0.value, next.value);
      const stalled = incomingDelta <= EPSILON || outgoingDelta <= EPSILON;
      if (stalled) errors.push("animation " + a.id + " seam hold " + tr.bone + "." + tr.channel);
      if (outgoingMode === "catmullrom") {
        if (Math.abs(incomingSpan - outgoingSpan) > EPSILON) errors.push("animation " + a.id + " seam timing mismatch " + tr.bone + "." + tr.channel);
      } else if (outgoingMode === "step") {
        errors.push("animation " + a.id + " step interpolation cannot cross a moving loop seam " + tr.bone + "." + tr.channel);
      } else {
        const incomingVelocity = t1.value.map((value, index) => (value - previous.value[index]) / incomingSpan);
        const outgoingVelocity = next.value.map((value, index) => (value - t0.value[index]) / outgoingSpan);
        const velocityError = distance(incomingVelocity, outgoingVelocity);
        const tolerance = Math.max(EPSILON * 100, Math.max(magnitude(incomingVelocity), magnitude(outgoingVelocity)) * 0.05);
        if (velocityError > tolerance) errors.push("animation " + a.id + " seam velocity mismatch " + tr.bone + "." + tr.channel);
      }
      let peakSpeed = 0;
      for (let i = 0; i < keyframes.length - 1; i++) {
        const left = keyframes[i], right = keyframes[i + 1];
        if (!left || !right || !vec3(left.value) || !vec3(right.value) || right.time <= left.time) continue;
        peakSpeed = Math.max(peakSpeed, distance(left.value, right.value) / (right.time - left.time));
      }
      const neutral = tr.channel === "scale" ? [1, 1, 1] : [0, 0, 0];
      dynamicSeams.push({
        neutral: distance(t0.value, neutral) <= EPSILON,
        motionRatio: peakSpeed > EPSILON ? Math.max(incomingDelta / incomingSpan, outgoingDelta / outgoingSpan) / peakSpeed : 1,
        stalled,
      });
    }
    if (dynamicSeams.length && dynamicSeams.every(seam => seam.neutral) && !dynamicSeams.some(seam => seam.stalled) && Math.max(...dynamicSeams.map(seam => seam.motionRatio)) < 0.2) {
      errors.push("animation " + a.id + " neutral rest seam; cut the loop during motion instead of easing every track to neutral");
    }
  }
  return { errors };
}

function catmullRom(p0, p1, p2, p3, t) {
  const t2 = t * t;
  const t3 = t2 * t;
  return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
}
function animationInterpolation(kfs, index, f, animation) {
  const k0 = kfs[index];
  const k1 = kfs[index + 1];
  const interpolation = k0.interpolation || "linear";
  const value = [0, 0, 0];
  if (interpolation === "step") return k0.value.slice();
  if (interpolation !== "catmullrom") {
    for (let c = 0; c < 3; c++) value[c] = k0.value[c] + (k1.value[c] - k0.value[c]) * f;
    return value;
  }
  const previous = index > 0 ? kfs[index - 1] : animation.loop === "loop" && kfs.length > 2 ? kfs[kfs.length - 2] : k0;
  const next = index + 2 < kfs.length ? kfs[index + 2] : animation.loop === "loop" && kfs.length > 2 ? kfs[1] : k1;
  for (let c = 0; c < 3; c++) value[c] = catmullRom(previous.value[c], k0.value[c], k1.value[c], next.value[c], f);
  return value;
}
function poseAt(t, animation, bone, channel) {
  const neutral = channel === "scale" ? 1 : 0;
  const track = (animation.tracks || []).find(tr => tr.bone === bone && tr.channel === channel);
  if (!track) return { x: neutral, y: neutral, z: neutral };
  const kfs = track.keyframes || [];
  if (!kfs.length) return { x: neutral, y: neutral, z: neutral };
  if (t <= kfs[0].time + 1e-9) return { x: kfs[0].value[0], y: kfs[0].value[1], z: kfs[0].value[2] };
  const last = kfs[kfs.length - 1];
  if (t >= last.time - 1e-9) {
    if (animation.loop === "loop" && last.time > 0) return poseAt(t - last.time * Math.floor(t / last.time), animation, bone, channel);
    return { x: last.value[0], y: last.value[1], z: last.value[2] };
  }
  for (let i = 0; i < kfs.length - 1; i++) {
    const k0 = kfs[i], k1 = kfs[i + 1];
    if (t >= k0.time - 1e-9 && t <= k1.time + 1e-9) {
      const span = k1.time - k0.time;
      const f = span < 1e-9 ? 0 : (t - k0.time) / span;
      const v = animationInterpolation(kfs, i, f, animation);
      return { x: v[0], y: v[1], z: v[2] };
    }
  }
  return { x: last.value[0], y: last.value[1], z: last.value[2] };
}

function readLiveModel(prefix) {
  const modelId = modelIdFromPrefix(prefix);
  const model = {
    id: modelId,
    name: modelId,
    eulerOrder: "ZYX",
    groups: Object.create(null),
    elements: Object.create(null),
    textures: Object.create(null),
    animations: [],
    resolutions: [],
    hiddenFaces: [],
    erasedPixels: [],
    deletedElements: [],
    interiorOverlaps: [],
  };
  const groupPrefix = generatedNamePrefix(modelId, "group");
  const cubePrefix = generatedNamePrefix(modelId, "cube");
  const texturePrefix = modelId + "_texture_";
  const localId = (name, kind) => name.slice(generatedNamePrefix(modelId, kind).length);
  const liveTextures = Object.create(null);
  for (const t of Texture.all) {
    if (!ownsGeneratedName(t.name, modelId, "texture")) continue;
    const tid = t.name === modelId ? "atlas" : t.name === modelId + "_glowmask" ? modelId + "_glowmask" : t.name.slice(texturePrefix.length);
    liveTextures[tid] = t;
    const role = tid.endsWith("_glowmask") ? "glowmask" : "diffuse";
    model.textures[tid] = { id: tid, name: t.name, width: t.width, height: t.height, pixels: textureAlphaPixels(t), role, sourceTexture: null, deleted: false };
  }
  const firstDiffuseTexture = Object.keys(model.textures).find(tid => model.textures[tid].role !== "glowmask") || null;
  for (const tid of Object.keys(model.textures)) if (model.textures[tid].role === "glowmask") model.textures[tid].sourceTexture = firstDiffuseTexture;
  const diffuseTexture = Texture.all.find(t => t.name === modelId) || null;
  const ownedCubeSet = new Set(Cube.all.filter(c => {
    if (typeof c.name !== "string" || (c.name.indexOf(cubePrefix) !== 0 && c.name.indexOf(modelId + "_cube_") !== 0)) return false;
    return !diffuseTexture || cubeUsesTexture(c, diffuseTexture);
  }));
  const ownedGroupSet = new Set();
  for (const cube of ownedCubeSet) {
    const seen = new Set();
    let parent = cube.parent;
    while (parent && typeof parent === "object" && !seen.has(parent)) {
      seen.add(parent);
      if (typeof parent.name === "string" && parent.name.indexOf(groupPrefix) === 0) ownedGroupSet.add(parent);
      parent = parent.parent;
    }
  }
  const parentId = p => p && typeof p === "object" && ownedGroupSet.has(p) ? localId(p.name, "group") : null;
  const liveGroups = Object.create(null);
  for (const g of Group.all) {
    if (!ownedGroupSet.has(g)) continue;
    const gid = localId(g.name, "group");
    liveGroups[gid] = g;
    model.groups[gid] = { kind: "group", id: gid, name: g.name, parent: parentId(g.parent), origin: [g.origin[0], g.origin[1], g.origin[2]], rotation: [g.rotation[0], g.rotation[1], g.rotation[2]], scale: Array.isArray(g.scale) ? [g.scale[0], g.scale[1], g.scale[2]] : [1, 1, 1], deleted: false };
  }

  for (const c of ownedCubeSet) {
    const cid = localId(c.name, "cube");
    const from = [c.from[0], c.from[1], c.from[2]];
    const to = [c.to[0], c.to[1], c.to[2]];
    const thinAxes = [];
    for (let k = 0; k < 3; k++) if (to[k] - from[k] <= 0.0011) thinAxes.push(k);
    if (thinAxes.length > 1) throw new Error("live-model-invalid-plate: " + c.name);
    let plate = thinAxes.length === 1;
    let plateAxis = thinAxes.length === 1 ? "xyz"[thinAxes[0]] : null;
    if (plate) to[thinAxes[0]] = from[thinAxes[0]];
    let texture = null;
    const texUuid = c.faces && c.faces.north && c.faces.north.texture;
    if (texUuid) for (const tid of Object.keys(liveTextures)) if (liveTextures[tid].uuid === texUuid) { texture = tid; break; }
    const faces = {};
    for (const f of FACE_ORDER) {
      const lf = c.faces[f];
      faces[f] = {
        uv: [lf.uv[0], lf.uv[1], lf.uv[2], lf.uv[3]],
        rotation: Number.isFinite(lf.rotation) ? lf.rotation : 0,
        enabled: !!lf.enabled,
      };
    }
    const plateSide = plate
      ? (() => {
        const pair = plateFaceNames(plateAxis, "both");
        const positive = !!faces[pair[0]].enabled;
        const negative = !!faces[pair[1]].enabled;
        return positive && negative ? "both" : positive ? "positive" : negative ? "negative" : "none";
      })()
      : null;
    const uv = (c.uv_offset && Array.isArray(c.uv_offset) && c.uv_offset.length === 2)
      ? [c.uv_offset[0], c.uv_offset[1]]
      : faces.north.uv.slice(0, 2);
    model.elements[cid] = { kind: "cube", id: cid, name: c.name, parent: parentId(c.parent), from, to, origin: [c.origin[0], c.origin[1], c.origin[2]], rotation: [c.rotation[0], c.rotation[1], c.rotation[2]], texture, uv, mirrorUv: false, inflate: 0, plate, plateAxis, plateSide, translucent: plate, cullface: null, faces, deleted: false };
  }
  return model;
}

function textureAlphaPixels(t) {
  const w = t.canvas.width;
  const h = t.canvas.height;
  const data = t.canvas.getContext("2d").getImageData(0, 0, w, h).data;
  const out = new Uint8ClampedArray(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    out[i * 4] = data[i * 4];
    out[i * 4 + 1] = data[i * 4 + 1];
    out[i * 4 + 2] = data[i * 4 + 2];
    out[i * 4 + 3] = data[i * 4 + 3];
  }
  return out;
}

function refreshCanvas() {
  if (typeof Canvas.updateAll === "function") Canvas.updateAll();
  else if (typeof Canvas.updateView === "function") Canvas.updateView({ elements: Outliner.elements, element_aspects: { geometry: true, transform: true, faces: true, uv: true } });
  else throw new Error("canvas-refresh-unavailable");
}

function previewFromModel(model) {
  let lo = [Infinity, Infinity, Infinity];
  let hi = [-Infinity, -Infinity, -Infinity];
  for (const id of Object.keys(model.elements)) {
    const e = model.elements[id];
    if (e.deleted) continue;
    const b = aabbOfElement(model, e);
    for (let k = 0; k < 3; k++) { if (b.lo[k] < lo[k]) lo[k] = b.lo[k]; if (b.hi[k] > hi[k]) hi[k] = b.hi[k]; }
  }
  if (!Number.isFinite(lo[0])) return { position: [9, 6, 9], target: [2, 4, 2], projection: "orthographic" };
  const target = [(lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2];
  const size = Math.max(hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2], 2);
  return { position: [target[0] + size, target[1] + size * 0.7, target[2] + size], target, projection: "orthographic" };
}

function runModelScript(MODEL, TEXTURES, resolve) {
  const formatInfo = { id: "bedrock_block", animation_mode: false, euler_order: "ZYX" };
  if (typeof Project !== "undefined" && Project.format) {
    formatInfo.id = Project.format.id;
    formatInfo.animation_mode = !!Project.format.animation_mode;
    formatInfo.euler_order = Project.format.euler_order || "ZYX";
  }
  const v = validateMinecraftModel(MODEL, TEXTURES, [], formatInfo);
  if (v.errors.length) throw new Error("validation-error: " + v.errors.join("; "));
  if (typeof Texture === "undefined" || typeof document === "undefined" || typeof Canvas === "undefined") throw new Error("texture-runtime-unavailable");
  const model = buildModel(MODEL, TEXTURES, []);
  model.eulerOrder = formatInfo.euler_order;
  const records = detectOverlaps(model);
  if (resolve) resolve(model, records);
  cleanupModel(model);
  centerModelOnBodyBottom(model);
  const verified = verifyOverlaps(model);
  model.interiorOverlaps = verified.interior.map(r => ({ a: r.a, b: r.b, kind: r.kind }));
  const created = buildBlockbench(model, []);
  Undo.finishEdit(MODEL.id + " model script", { elements: created, outliner: true });
  refreshCanvas();
  return buildModelSummary(model, records.checks, v.warnings);
}

function buildModelSummary(model, checks, warnings) {
  const groupNames = [];
  for (const gid of Object.keys(model.groups)) if (!model.groups[gid].deleted) groupNames.push(model.groups[gid].name);
  const cubeNames = [];
  for (const id of Object.keys(model.elements)) if (!model.elements[id].deleted) cubeNames.push(model.elements[id].name);
  const textureNames = [];
  for (const tid of Object.keys(model.textures)) if (!model.textures[tid].deleted) textureNames.push(model.textures[tid].name);
  return {
    schemaVersion: 2,
    phase: "model",
    modelId: model.id,
    project: { name: Project.name, format: Project.format.id },
    groups: groupNames,
    cubes: cubeNames,
    textures: textureNames,
    counts: { groups: groupNames.length, cubes: cubeNames.length, textures: textureNames.length },
    overlapChecks: checks,
    interiorOverlaps: model.interiorOverlaps,
    overlapResolutions: model.resolutions,
    hiddenFaces: model.hiddenFaces,
    erasedPlatePixels: model.erasedPixels,
    deletedElements: model.deletedElements,
    remainingOverlaps: [],
    warnings,
    centering: model.centering,
    preview: previewFromModel(model),
  };
}

function runAnimationScript(model, ANIMATIONS) {
  const formatInfo = { id: "bedrock_block", animation_mode: false, euler_order: "ZYX" };
  if (typeof Project !== "undefined" && Project.format) {
    formatInfo.id = Project.format.id;
    formatInfo.animation_mode = !!Project.format.animation_mode;
    formatInfo.euler_order = Project.format.euler_order || "ZYX";
  }
  const groupIds = new Set(Object.keys(model.groups));
  const errors = validateAnimations(ANIMATIONS, groupIds, formatInfo);
  if (errors.length) throw new Error("validation-error: " + errors.join("; "));
  if (typeof Animation === "undefined") throw new Error("animation-runtime-unavailable");
  const modelId = model.id;
  const liveGroups = Object.create(null);
  const modelGroupIds = new Map(Object.values(model.groups).map(group => [group.name, group.id]));
  for (const g of Group.all) {
    const gid = modelGroupIds.get(g.name);
    if (gid) liveGroups[gid] = g;
  }
  for (const animation of ANIMATIONS) for (const track of animation.tracks || []) {
    if (!liveGroups[track.bone]) throw new Error("animation-live-bone-missing: " + track.bone);
  }
  const animated = checkAnimatedOverlaps(model, ANIMATIONS);
  const targets = [];
  for (const a of Animation.all) if (ownsGeneratedName(a.name, modelId, "animation")) targets.push(a);
  const editAspects = { elements: targets, outliner: true };
  Undo.initEdit(editAspects);
  try {
    for (const t of targets) t.remove();
    model.animations = buildAnimations(model.id, ANIMATIONS);
    createAnimations(model, ANIMATIONS, liveGroups);
    Undo.finishEdit(modelId + " animation script", editAspects);
  } catch (error) {
    closeFailedEdit(modelId + " animation script failed", editAspects);
    throw error;
  }
  refreshCanvas();
  return buildAnimationSummary(model, animated, []);
}

function buildAnimationSummary(model, animated, warnings) {
  let keyframes = 0;
  for (const a of model.animations) for (const tr of a.tracks) keyframes += tr.keyframes.length;
  return {
    schemaVersion: 2,
    phase: "animation",
    modelId: model.id,
    project: { name: Project.name, format: Project.format.id },
    animations: model.animations.map(a => a.name),
    counts: { animations: model.animations.length, keyframes },
    animatedOverlapChecks: animated.checks,
    animatedInteriorOverlaps: animated.interiorOverlaps,
    animatedRemainingOverlaps: animated.remaining,
    warnings,
    preview: previewFromModel(model),
  };
}

function isGlowMaskTexture(textureOrName) {
  const name = typeof textureOrName === "string" ? textureOrName : textureOrName && textureOrName.name;
  return typeof name === "string" && name.endsWith("_glowmask");
}

function beginTexturePaint(prefix) {
  if (typeof Texture === "undefined" || typeof document === "undefined") throw new Error("texture-runtime-unavailable");
  const modelId = modelIdFromPrefix(prefix);
  const records = [];
  for (const t of Texture.all) {
    if (!ownsGeneratedName(t.name, modelId, "texture")) continue;
    const ctx = t.canvas.getContext("2d");
    ctx.imageSmoothingEnabled = false;
    records.push({ texture: t, ctx, width: t.canvas.width, height: t.canvas.height, before: ctx.getImageData(0, 0, t.canvas.width, t.canvas.height).data.slice(), glowmask: isGlowMaskTexture(t) });
  }
  if (!records.length) throw new Error("texture-missing: " + modelId);
  Undo.initEdit({ elements: records.map(r => r.texture), outliner: false });
  return records;
}
function abortTextureScript(modelId, records) {
  closeFailedEdit(modelId + " texture script failed", { elements: records.map(r => r.texture), outliner: false });
}


function finishTextureScript(modelId, records) {
  const textures = [];
  const glowMasks = [];
  const warnings = [];
  try {
  for (const r of records) {
    const after = r.ctx.getImageData(0, 0, r.width, r.height).data;
    for (let i = 3; i < r.width * r.height * 4; i += 4) {
      if (after[i] !== r.before[i]) throw new Error("texture-alpha-changed: " + r.texture.name);
    }
    if (r.glowmask) {
      let pixels = 0;
      let maxAlpha = 0;
      for (let i = 3; i < r.width * r.height * 4; i += 4) {
        if (after[i] > 0) pixels++;
        if (after[i] > maxAlpha) maxAlpha = after[i];
      }
      if (!pixels) warnings.push("glowmask " + r.texture.name + " has no emissive pixels");
      glowMasks.push({ name: r.texture.name, pixels, maxAlpha });
    } else {
      const detail = textureDetailReport(after, r.width, r.height);
      if (detail.opaquePixels >= 16 && detail.toneCount < 3) warnings.push("texture " + r.texture.name + " needs at least 3 opaque tones");
      if (detail.opaquePixels >= 16 && detail.colorTransitions < Math.max(2, Math.floor(detail.opaquePixels / 32))) warnings.push("texture " + r.texture.name + " needs clustered color transitions");
    }
    r.texture.updateSource(r.texture.canvas.toDataURL());
    textures.push(r.texture.name);
  }
  Undo.finishEdit(modelId + " texture script", { elements: records.map(r => r.texture), outliner: false });
  refreshCanvas();
  return {
    schemaVersion: 2,
    phase: "texture",
    modelId,
    project: { name: Project.name, format: Project.format.id },
    textures,
    glowMasks,
    counts: { textures: textures.length },
    alphaPreserved: true,
    warnings,
  };
  } catch (error) {
    abortTextureScript(modelId, records);
    throw error;
  }
}

function paintRectPreservingAlpha(ctx, x, y, w, h, hex) {
  const rgba = parsePaletteColor(hex);
  if (!rgba) throw new Error("invalid-palette-color");
  const img = ctx.getImageData(x, y, w, h);
  for (let i = 0; i < w * h; i++) {
    img.data[i * 4] = rgba[0];
    img.data[i * 4 + 1] = rgba[1];
    img.data[i * 4 + 2] = rgba[2];
  }
  ctx.putImageData(img, x, y);
}

function repaintMapPreservingAlpha(ctx, width, height, palette, pixels) {
  for (const key of Object.keys(palette || {})) if (key.length !== 1 || key === "_" || !parsePaletteColor(palette[key])) throw new Error("invalid-palette-color: " + key);
  const img = ctx.getImageData(0, 0, width, height);
  const d = img.data;
  for (let r = 0; r < height; r++) {
    const row = pixels[r];
    if (!row) continue;
    for (let c = 0; c < width; c++) {
      const ch = row[c];
      if (ch === "_" || ch === undefined) continue;
      const rgba = parsePaletteColor(palette[ch]);
      if (!rgba) throw new Error("invalid-palette-color: " + ch);
      const idx = (r * width + c) * 4;
      if (d[idx + 3] === 0) continue;
      d[idx] = rgba[0];
      d[idx + 1] = rgba[1];
      d[idx + 2] = rgba[2];
    }
  }
  ctx.putImageData(img, 0, 0);
}
function textureDetailReport(data, width, height) {
  if (!data || !Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1 || data.length < width * height * 4) throw new Error("texture-detail-invalid-image");
  const alphaAt = (x, y) => x < 0 || y < 0 || x >= width || y >= height ? 0 : data[(y * width + x) * 4 + 3];
  const colorAt = (x, y) => {
    const index = (y * width + x) * 4;
    return data[index] + "," + data[index + 1] + "," + data[index + 2];
  };
  const colors = new Set();
  let opaquePixels = 0;
  let edgePixels = 0;
  let colorTransitions = 0;
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const index = (y * width + x) * 4;
    if (data[index + 3] === 0) continue;
    opaquePixels++;
    colors.add(colorAt(x, y));
    if (alphaAt(x - 1, y) === 0 || alphaAt(x + 1, y) === 0 || alphaAt(x, y - 1) === 0 || alphaAt(x, y + 1) === 0) edgePixels++;
    if (x + 1 < width && alphaAt(x + 1, y) > 0 && colorAt(x, y) !== colorAt(x + 1, y)) colorTransitions++;
    if (y + 1 < height && alphaAt(x, y + 1) > 0 && colorAt(x, y) !== colorAt(x, y + 1)) colorTransitions++;
  }
  return { opaquePixels, toneCount: colors.size, edgePixels, colorTransitions };
}
function applyTextureDetail(ctx, options) {
  if (!ctx || !ctx.canvas || typeof ctx.getImageData !== "function") throw new Error("texture-detail-invalid-context");
  const width = ctx.canvas.width;
  const height = ctx.canvas.height;
  const config = options || {};
  const requestedStrength = config.strength === undefined ? 0.12 : config.strength;
  if (typeof requestedStrength !== "number" || !Number.isFinite(requestedStrength) || requestedStrength < 0) throw new Error("texture-detail-invalid-strength");
  const strength = Math.min(requestedStrength, 0.5);
  const seedText = config.seed === undefined ? "" : String(config.seed);
  let seed = 0;
  for (let i = 0; i < seedText.length; i++) seed = (seed * 33 + seedText.charCodeAt(i)) >>> 0;
  const image = ctx.getImageData(0, 0, width, height);
  const data = image.data;
  const alphaAt = (x, y) => x < 0 || y < 0 || x >= width || y >= height ? 0 : data[(y * width + x) * 4 + 3];
  const clampByte = value => Math.max(0, Math.min(255, Math.round(value)));
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const index = (y * width + x) * 4;
    if (data[index + 3] === 0) continue;
    let factor = 1;
    if (alphaAt(x, y - 1) === 0) factor += strength * 0.8;
    if (alphaAt(x, y + 1) === 0) factor -= strength * 0.8;
    if (alphaAt(x - 1, y) === 0) factor += strength * 0.35;
    if (alphaAt(x + 1, y) === 0) factor -= strength * 0.35;
    const hash = (x * 92821 + y * 68917 + seed) % 29;
    if (hash === 0 || hash === 7) factor += strength * 0.35;
    else if (hash === 13 || hash === 23) factor -= strength * 0.3;
    data[index] = clampByte(data[index] * factor);
    data[index + 1] = clampByte(data[index + 1] * factor);
    data[index + 2] = clampByte(data[index + 2] * factor);
  }
  ctx.putImageData(image, 0, 0);
  return textureDetailReport(data, width, height);
}


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

    if (local === "chassis" && face === "up") {
      if (y % 2 === 1) color = scale(color, 0.94);
      if (y === 1 || y === 3) color = scale(color, 0.9);
    }
    if (local === "chassis" && (face === "east" || face === "west")) {
      if (y === 4) color = scale(color, 0.88);
      if (x >= 1 && x <= 4 && y >= 1 && y <= 2) color = scale(color, 0.96);
      if ((y === 1 || y === 2) && x >= 1 && x <= 4 && x % 2 === 1) color = scale(color, 0.78);
    }
    if (local === "chassis" && face === "south" && x >= 1 && x <= 4 && y >= 1 && y <= 5) {
      color = scale(color, 0.55);
    }
    if (local === "sonar_drum" && face === "north" && (x === 0 || x === w - 1 || y === h - 1)) {
      color = scale(color, 0.45);
    }
    if (local === "sonar_drum" && face === "down") {
      color = scale(color, 0.4);
      if (x === 1 || x === w - 2 || y === 1) color = scale(color, 1.6);
    }
    if (face === "north" && (local === "bezel_top" || local === "bezel_bottom") && (x === 0 || x === w - 1)) {
      color = scale(color, 0.6);
    }
    if (face === "north" && (local === "bezel_left" || local === "bezel_right") && (y === 0 || y === h - 1)) {
      color = scale(color, 0.6);
    }
    if (local === "bezel_top" && face === "down") color = scale(color, 0.5);
    if (local === "bezel_bottom" && face === "up") color = scale(color, 0.5);
    if (local === "bezel_left" && face === "east") color = scale(color, 0.5);
    if (local === "bezel_right" && face === "west") color = scale(color, 0.5);
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
})();
