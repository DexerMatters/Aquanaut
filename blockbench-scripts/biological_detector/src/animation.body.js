const MODEL_ID = "biological_detector";
const PREFIX = MODEL_ID + "_";
const model = readLiveModel(PREFIX);
const ANIMATIONS = (() => {
  const RELEASE_LENGTH = 1.8;
  const WORK_LENGTH = 2.4;
  const release = [];
  const working = [];
  const frames = (bone, channel, points) => ({
    bone: bone,
    channel: channel,
    keyframes: points.map(point => ({
      time: point[0],
      value: point[1].slice(),
      interpolation: point[2] || "catmullrom",
    })),
  });
  const vivid = (bone, channel, valueAt, samples) =>
    makeVividTrack(bone, channel, WORK_LENGTH, valueAt, { samples: samples || 12 });

  // Release: the ball unseals. The upper shell lifts off the array belt with a
  // mechanical overshoot, rocks as it settles, and the core pulses awake inside
  // the opening while the hull takes the recoil.
  release.push(frames("shell_upper", "position", [
    [0.0, [0, 0, 0]],
    [0.25, [0, 0.14, 0]],
    [0.55, [0, 2.12, 0]],
    [0.75, [0, 1.6, 0]],
    [0.95, [0, 1.84, 0]],
    [1.25, [0, 1.75, 0]],
  ]));
  release.push(frames("shell_upper", "rotation", [
    [0.0, [0, 0, 0]],
    [0.4, [0, 0, 0]],
    [0.65, [1.6, 0, -1.2]],
    [0.9, [-0.9, 0, 0.7]],
    [1.3, [0, 0, 0]],
  ]));
  release.push(frames("core", "scale", [
    [0.0, [1, 1, 1]],
    [0.45, [0.985, 1.03, 0.985]],
    [0.75, [1.015, 0.99, 1.015]],
    [1.15, [1, 1, 1]],
  ]));
  release.push(frames("body", "position", [
    [0.0, [0, 0, 0]],
    [0.3, [0, 0.24, 0]],
    [0.55, [0, -0.12, 0]],
    [0.85, [0, 0.03, 0]],
    [1.2, [0, 0, 0]],
  ]));
  release.push(frames("body", "rotation", [
    [0.0, [0, 0, 0]],
    [0.4, [2.2, 0, -1.5]],
    [0.8, [-1, 0, 0.6]],
    [1.3, [0, 0, 0]],
  ]));
  release.push(frames("body", "scale", [
    [0.0, [1, 1, 1]],
    [0.45, [1.03, 1.045, 1.03]],
    [0.8, [0.99, 0.985, 0.99]],
    [1.3, [1, 1, 1]],
  ]));

  // Working: the ball floats and scans. The upper shell hovers and tilts a few
  // degrees, the inner array breathes and the hull drifts in the current. The
  // full azimuth turn comes from code, so the body yaw stays zero here.
  working.push(vivid("shell_upper", "position", t => [
    organicWave(t, 0.06, 2, 1.2),
    1.75 + organicWave(t, 0.14, 1, 0.35),
    organicWave(t, 0.06, 2, 2.6),
  ]));
  working.push(vivid("shell_upper", "rotation", t => [
    organicWave(t, 0.9, 1, 0.8),
    organicWave(t, 0.7, 2, 2.1),
    organicWave(t, 0.8, 2, 1.7),
  ]));
  working.push(vivid("core", "rotation", t => [
    0,
    organicWave(t, 2.4, 1, 1.2),
    0,
  ]));
  working.push(vivid("core", "scale", t => [
    1 + organicWave(t, 0.035, 1, 0.9),
    1 + organicWave(t, 0.05, 2, 1.6),
    1 + organicWave(t, 0.035, 1, 0.9),
  ]));
  working.push(vivid("body", "position", t => [
    organicWave(t, 0.05, 2, 1.1),
    organicWave(t, 0.08, 1, 0.2),
    organicWave(t, 0.05, 2, 2.2),
  ]));
  working.push(vivid("body", "rotation", t => [
    organicWave(t, 0.9, 1, 1.9),
    0,
    organicWave(t, 0.7, 1, 0.5),
  ]));
  working.push(vivid("body", "scale", t => [
    1 + organicWave(t, 0.008, 1, 0.3),
    1 + organicWave(t, 0.01, 2, 1.8),
    1 + organicWave(t, 0.008, 1, 0.3),
  ]));

  return [
    { id: "release", length: RELEASE_LENGTH, loop: "once", tracks: release },
    { id: "working", length: WORK_LENGTH, loop: "loop", tracks: working },
  ];
})();
const summary = runAnimationScript(model, ANIMATIONS);
return summary;
