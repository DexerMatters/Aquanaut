# Blockbench asset pipeline

Everything added by `models.zip` (fourteen creatures plus the dissection table) is exported from the
Blockbench projects in the archive **by Blockbench itself**, driven over the
[Blockbench MCP plugin](https://github.com/jasonjgardner/blockbench-mcp-plugin). No geometry, UV or
keyframe is converted by hand: the exporter is Blockbench's own code path, so the assets carry
Blockbench's Bedrock conventions exactly like the shipped Aquanaut creatures.

## Provenance

The sources are the contents of `models.zip`:

| kind | files |
| --- | --- |
| Blockbench projects | `*.bbmodel` (14 creatures, `dissection_table/`, `dissection_table_x2`, `dissection_table_x4`) |
| Animation exports | `*.animation.json` per creature (used as a cross-check, not as the source) |
| Generated source scripts | `blockbench-scripts/<slug>/{model,texture,animation}.js` |

The archive is **not** committed (≈9 MB including the 1024×2048 ecofish texture). Extract it locally
(the same tree also lives at `/home/dexer/repos/models_new`):

```bash
unzip models.zip -d scripts/blockbench/src
```

## Commands

```bash
# 1. geometry, animation and texture assets, driven through a running Blockbench
blockbench &                     # start Blockbench, enable the MCP plugin (port 3000, /bb-mcp)
python3 scripts/export_blockbench_models.py --src scripts/blockbench/src --write
python3 scripts/export_blockbench_models.py --check      # re-export and fail on any drift

# 2. spawn egg sprites (measured from the 24 shipped eggs)
python3 scripts/GenerateSpawnEggTextures.py

# 3. dissection table item icon
python3 scripts/GenerateDissectionTableItemIcon.py
```

`export_blockbench_models.py` supports `--only <slug>`, `--write`, `--check` and `--url`, and writes
into `src/main/resources/assets/aquanaut/{geo,animations}`. It opens each `.bbmodel` through
Blockbench's single-instance file handoff, so a Blockbench window must be running with the MCP plugin
enabled (`Settings > mcp_port`/`mcp_endpoint`, defaults `3000` and `/bb-mcp`).

## What Blockbench does on export (and why it must not be re-implemented)

All of this is visible in `js/formats/bedrock/bedrock.js` and in the GeckoLib plugin:

* **The model is mirrored into the Bedrock convention.** `compileCube` writes
  `origin[0] = -(from[0] + size[0])` and mirrors the rotation pivot, and `compileGroup` negates every
  bone pivot's X plus the X/Y rotation components. `parseCube`/`parseGroup` apply the same mirror
  when loading, so the project and the export are mirror images by design. A converter that copies
  the project's coordinates straight out produces a mirrored model — and because the *UVs are not
  mirrored with it*, every left/right pair (fins, eyes, bell segments) ends up on the wrong texture.
* **The project must be in the `geckolib_model` format.** The plugin then forces
  `format_version: "1.12.0"` and strips `item_display_transforms` from the compiled geometry, and it
  replaces the Bedrock keyframe compiler so animations are written as
  `{"post": {"vector": [x, y, z]}}` with the X/Y rotations inverted — the exact shape of every
  shipped Aquanaut animation.
* **Clip names are canonicalised before export.** The projects name their clips
  `golden_carp_animation_swim`, `bbpi:pale_abyss_hydra:animation:attack`,
  `animation.ecofish.swim` or plainly `swim`; the export renames each to the verb the entity
  classes play (`swim`, `charge`, `attack`, `open`, `close`, `target`). Keyframes are untouched.
* **Clip loop closure is whatever the project says.** Blockbench has no loop-wrapping option for
  Bedrock exports, so a few clips end up to one tick short of (or past) their declared
  `animation_length`, exactly like the shipped animations (`icerail`, `red_jellyfish`, `ringfish`,
  `swirl_maker.open`). `FishAnimationLoopTest` allows that one-tick slack and still checks the pose
  closure whenever the exporter does write a key on the declared length.
* **Mirrored auto-UV cubes keep the compact cube-level UV form** with `"mirror": true`, which is what
  the shipped models do; per-face cubes carry their six rectangles and never a `mirror` flag.
* **Block models keep the authored vertical placement.** The dissection table's geometry spans
  `y = -10 … 6`; `DissectionTableBlockEntityRenderer` lifts it by `10/16` blocks so the feet rest on
  the block floor.

## Authoring corrections

`export_blockbench_models.py` carries a small `PROJECT_FIXES` table for the handful of places where a
source project does not follow the conventions every other model uses. The fixes run inside
Blockbench, before the export, and are state-based (they detect the condition instead of applying a
one-shot offset), so re-exporting an already-open tab is idempotent.

* **`flagellonautilus`** is authored with its root group yawed `-90`, which turns its head onto `+Z`
  while every other model (and every shipped Aquanaut creature) faces `-Z`, and its geometry is
  authored about 6.5 units off-centre in X. The fix measures the head/tail groups' world positions
  and the cube centre, turns the root 180° when the model points backwards and recentres the cubes
  and pivots when the centre is more than half a unit out. Verified afterwards: posed size
  `12 × 19.5 × 23` units (long axis Z, like every other fish), centre `x≈0`, head at `z=-7.4` and tail
  at `z=+6.3`, and the geo still bakes through GeckoLib while both clips parse.

## Verification

* `NewSpeciesAssetTest` (8 cases) checks the exported assets: geometry/animation/texture presence,
  animated bones existing in the geometry, the expected clips, spawn egg sprite envelope, notebook
  and language completeness, table assets, and the UV census (`boxUV : per-face : mirrored` per
  model) plus per-face rectangles staying inside the declared texture.
* `FishAnimationLoopTest` runs the exported animations through the same shape checks the shipped
  clips satisfy.
* The dissection table exports are byte-for-byte semantically identical to the archive's own
  `dissection_table*.geo.json`, and `golden_carp` matches the archive's `golden_carp.geo.json` on
  every unchanged bone, cube, `visible_bounds` and `format_version` — the strongest available proof
  that this pipeline reproduces the author's export.
* `AnimAudit` (a throwaway harness, not committed) loads every exported animation through GeckoLib's
  own `BakedAnimationsAdapter`; all 14 parse with the expected clip names, tick lengths and loop
  types.
