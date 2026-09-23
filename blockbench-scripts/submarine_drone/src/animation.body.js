const MODEL_ID = "submarine_drone";
const PREFIX = MODEL_ID + "_";
const model = readLiveModel(PREFIX);
const ANIMATIONS = (() => {
  const length = 1.2;
  const tracks = [];
  const vivid = (bone, channel, valueAt) => makeVividTrack(bone, channel, length, valueAt, { samples: 12 });
  const spinAxis = (index, amplitude, cycles, phase) => t => {
    const value = [0, 0, 0];
    value[index] = organicWave(t, amplitude, cycles, phase);
    return value;
  };
  tracks.push(vivid("body", "rotation", t => [
    organicWave(t, 1.6, 1, 0.2),
    organicWave(t, 1.1, 1, 0.45),
    organicWave(t, 2.1, 1, 0.05),
  ]));
  tracks.push(vivid("body", "position", t => [
    organicWave(t, 0.22, 1, 0.35),
    organicWave(t, 0.7, 1, -0.45),
    organicWave(t, 0.3, 2, 0.1),
  ]));
  tracks.push(vivid("beacon", "rotation", spinAxis(1, 20, 1, 0)));
  tracks.push(vivid("fin_left", "rotation", spinAxis(0, 7, 2, 0.25)));
  tracks.push(vivid("fin_right", "rotation", spinAxis(0, 7, 2, 0.55)));
  const steps = 12;
  const spin = [];
  for (let i = 0; i <= steps; i++) {
    spin.push({
      time: Math.round((i * length * 1e6) / steps) / 1e6,
      value: [0, 0, i * 90],
      interpolation: "linear",
    });
  }
  tracks.push({ bone: "fan", channel: "rotation", keyframes: spin });

  // In an angle channel a whole-turn offset is the same pose, so the cycle
  // closes as long as every track's last key equals its first value modulo one
  // revolution. Verify that before declaring the shipped animation a loop.
  const closes = value => {
    for (const track of tracks) {
      const first = track.keyframes[0].value;
      const last = track.keyframes[track.keyframes.length - 1].value;
      for (let axis = 0; axis < 3; axis++) {
        const delta = Math.abs(first[axis] - last[axis]);
        const residual = track.channel === "rotation" ? delta % 360 : delta;
        if (residual > 1e-6) return false;
      }
    }
    return true;
  };
  if (!closes()) throw new Error("animation-cycle-does-not-close");
  return [{ id: "idle", length: length, loop: "hold", tracks: tracks }];
})();
const summary = runAnimationScript(model, ANIMATIONS);
for (const anim of Animation.all) {
  if (ownsGeneratedName(anim.name, MODEL_ID, "animation")) anim.loop = "loop";
}
return summary;
