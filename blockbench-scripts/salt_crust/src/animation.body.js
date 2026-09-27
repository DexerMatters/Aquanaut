const MODEL_ID = "salt_crust";
const PREFIX = MODEL_ID + "_";
for (const anim of Animation.all.slice()) {
  if (anim.name === "swim" || anim.name === "consume" || anim.name === MODEL_ID + "_animation_swim" || anim.name === MODEL_ID + "_animation_consume") anim.remove();
}
const model = readLiveModel(PREFIX);
const TAU = Math.PI * 2;
const wave = (amplitude, cycles, phase) => t => amplitude * Math.sin(TAU * cycles * t + phase);
const ANIMATIONS = (() => {
  const swimLength = 2.0;
  const swimTracks = [];
  const swim = (bone, channel, valueAt, samples) => swimTracks.push(makeVividTrack(bone, channel, swimLength, valueAt, { samples: samples || 20 }));
  const bob = (amplitude, offset, cycles, phase) => t => [0, offset + amplitude * Math.sin(TAU * cycles * t + phase), 0];

  swim("body", "rotation", t => [
    wave(3.4, 1, 0.15)(t),
    wave(0.8, 1, 0.42)(t),
    wave(3.8, 2, 0.05)(t),
  ]);
  swim("body", "position", t => [
    wave(0.28, 1, 0.3)(t),
    wave(0.5, 2, 0.0)(t),
    wave(0.3, 1, 0.58)(t),
  ]);
  swim("head", "rotation", t => [
    wave(3.2, 2, 0.2)(t),
    wave(0.4, 1, 0.52)(t),
    wave(1.6, 1, 0.8)(t),
  ]);
  swim("eye_left", "rotation", t => [
    wave(7.5, 2, 0.1)(t),
    0,
    wave(1.5, 2, 0.3)(t),
  ]);
  swim("eye_right", "rotation", t => [
    wave(7.5, 2, 0.62)(t),
    0,
    wave(-1.5, 2, 0.82)(t),
  ]);
  swim("shell", "rotation", t => [
    wave(1.2, 2, 0.35)(t),
    wave(3.2, 1, 0.25)(t),
    wave(1.4, 2, 0.6)(t),
  ]);
  swim("shell", "position", bob(0.18, 0.18, 1, 0.4));
  swim("veil_left", "rotation", t => [0, 0, -6 - 6 * Math.sin(TAU * 1 * t + 0.0)]);
  swim("veil_right", "rotation", t => [0, 0, 6 + 6 * Math.sin(TAU * 1 * t + 0.62)]);
  swim("proboscis", "rotation", t => [wave(4.5, 1, 0.4)(t), 0, wave(2.0, 2, 0.2)(t)]);
  swim("proboscis", "position", t => [0, wave(0.15, 2, 0.5)(t), wave(0.2, 1, 0.4)(t)]);

  const consumeLength = 1.6;
  const consumeTracks = [];
  const consume = (bone, channel, valueAt, samples) => consumeTracks.push(makeVividTrack(bone, channel, consumeLength, valueAt, { samples: samples || 16 }));

  consume("body", "rotation", t => [
    wave(2.6, 2, 0.25)(t),
    wave(0.3, 1, 0.1)(t),
    wave(1.4, 1, 0.6)(t),
  ]);
  consume("body", "position", t => [
    0,
    -0.12 - 0.12 * (0.5 + 0.5 * Math.sin(TAU * 2 * t - Math.PI / 2)),
    -0.1 - 0.1 * (0.5 + 0.5 * Math.sin(TAU * 2 * t - Math.PI / 2)),
  ]);
  consume("head", "rotation", t => [
    -3 - 4 * (0.5 + 0.5 * Math.sin(TAU * 2 * t - Math.PI / 2)),
    0,
    0,
  ]);
  consume("head", "position", t => [0, 0, -0.12 - 0.12 * (0.5 + 0.5 * Math.sin(TAU * 2 * t - Math.PI / 2))]);
  consume("eye_left", "rotation", t => [4 + 4 * Math.sin(TAU * 2 * t - Math.PI / 2), 0, wave(1, 2, 0.35)(t)]);
  consume("eye_right", "rotation", t => [4 + 4 * Math.sin(TAU * 2 * t - Math.PI / 2 + 0.5), 0, wave(-1, 2, 0.6)(t)]);
  consume("shell", "rotation", t => [0, wave(2.6, 2, 0.2)(t), wave(1.0, 1, 0.5)(t)]);
  consume("shell", "position", bob(0.12, 0.12, 2, 0.3));
  consume("veil_left", "rotation", t => [0, 0, -3 - 3 * Math.sin(TAU * 2 * t + 0.1)]);
  consume("veil_right", "rotation", t => [0, 0, 3 + 3 * Math.sin(TAU * 2 * t + 0.1)]);
  consume("proboscis", "rotation", t => [wave(5, 2, 0.4)(t), 0, 0]);
  consume("proboscis", "position", t => [
    0,
    -0.1 - 0.1 * (0.5 + 0.5 * Math.sin(TAU * 2 * t - Math.PI / 2)),
    -0.35 - 0.35 * (0.5 + 0.5 * Math.sin(TAU * 2 * t + Math.PI / 2)),
  ]);

  return [
    { id: "swim", name: PREFIX + "animation:swim", length: swimLength, loop: "loop", tracks: swimTracks },
    { id: "consume", name: PREFIX + "animation:consume", length: consumeLength, loop: "loop", tracks: consumeTracks },
  ];
})();
const summary = runAnimationScript(model, ANIMATIONS);
const renameMap = {};
renameMap[MODEL_ID + "_animation_swim"] = "swim";
renameMap[MODEL_ID + "_animation_consume"] = "consume";
for (const anim of Animation.all) {
  const renamed = renameMap[anim.name];
  if (renamed) anim.name = renamed;
}
return summary;
