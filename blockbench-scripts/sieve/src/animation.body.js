const MODEL_ID = "sieve";
const PREFIX = MODEL_ID + "_";
const model = readLiveModel(PREFIX);
const LENGTH = 1;
const SLIDE_HALF_WIDTH = 2;
const BOUNCE_RATE = 4;
const BOUNCE_HALF_HEIGHT = 0.08;
const DRIFT_HALF_WIDTH = 0.12;
const ANIMATIONS = (() => {
  const tracks = [];
  tracks.push(makeVividTrack("tray", "position", LENGTH, (t) => {
    const slide = SLIDE_HALF_WIDTH * Math.sin(Math.PI * 2 * 2 * t);
    const bounce = BOUNCE_HALF_HEIGHT + BOUNCE_HALF_HEIGHT * Math.sin(Math.PI * 2 * BOUNCE_RATE * t + Math.PI * 0.5);
    const drift = DRIFT_HALF_WIDTH * Math.sin(Math.PI * 2 * t + Math.PI / 3);
    return [slide, bounce, drift];
  }, { samples: 20, interpolation: "catmullrom" }));
  return [{
    id: "sift",
    name: PREFIX + "animation:sift",
    length: LENGTH,
    loop: "loop",
    tracks: tracks,
  }];
})();
const summary = runAnimationScript(model, ANIMATIONS);
return summary;
