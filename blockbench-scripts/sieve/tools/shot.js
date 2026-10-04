(() => {
  const fs = require("fs");
  const cfg = JSON.parse(fs.readFileSync("/tmp/sieve_shot.json", "utf8"));
  const preview = Preview.selected ?? Preview.all.find(p => p.canvas && p.canvas.isConnected);
  if (!preview) return JSON.stringify({ error: "no-preview" });
  if (cfg.width && cfg.height && preview.canvas && (preview.canvas.width !== cfg.width || preview.canvas.height !== cfg.height)) {
    try { preview.resize(cfg.width, cfg.height); } catch (error) { }
  }
  const out = [];
  for (const shot of cfg.shots) {
    if (shot.position) {
      const preset = { position: shot.position, projection: shot.projection || "orthographic" };
      if (shot.target) preset.target = shot.target;
      if (shot.rotation) preset.rotation = shot.rotation;
      if (shot.zoom !== undefined) preset.zoom = shot.zoom;
      if (shot.locked_angle) preset.locked_angle = shot.locked_angle;
      try { preview.loadAnglePreset(preset); } catch (error) { out.push({ path: shot.path, error: "camera:" + String(error) }); continue; }
    }
    let url = null;
    Canvas.withoutGizmos(() => { preview.render(); url = preview.canvas.toDataURL("image/png"); });
    if (!url) { out.push({ path: shot.path, error: "no-data-url" }); continue; }
    const buffer = Buffer.from(url.split(",")[1], "base64");
    fs.writeFileSync(shot.path, buffer);
    const camera = preview.camera ? {
      pos: preview.camera.position.toArray().map(v => Math.round(v * 100) / 100),
      isOrtho: !!preview.isOrtho,
      zoom: preview.camera.zoom,
      fov: preview.camera.fov,
      target: preview.controls && preview.controls.target ? preview.controls.target.toArray().map(v => Math.round(v * 100) / 100) : null,
    } : null;
    out.push({ path: shot.path, bytes: buffer.length, camera: camera });
  }
  return JSON.stringify({ ok: true, shots: out });
})()
