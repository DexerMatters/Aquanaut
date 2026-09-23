# Handheld searchlight

The lamp's geometry, atlas, glow mask and display transforms are authored in Blockbench through the
`blockbench-script` skill. This folder is the source of truth for the model; the game files under
`src/main/resources/assets/aquanaut/` are generated from it.

## What is here

| file | role |
| --- | --- |
| `src/model.body.js` | geometry + diffuse atlas reservation + glow mask, assembled into `model.js` with the skill runtime |
| `src/texture.body.js` | atlas paint plus the derived lit atlas, assembled into `texture.js` |
| `build.sh` | assembles both scripts (comment-free, `console`-free) with the skill's `runtime.js` |
| `handheld_searchlight.bbmodel` | the saved Blockbench project |
| `handheld_searchlight_export.json` | Blockbench's Java Block/Item export of the project |
| `handheld_searchlight.png` / `_glowmask.png` / `_on.png` | the atlas, the glow mask and the lit atlas |
| `export_java_item.mjs` | splits the export into the five game model JSONs and copies the atlases |

## Rebuilding

```bash
blockbench &                                  # MCP plugin on port 3000
./build.sh                                    # assemble model.js / texture.js
```

Run the phases in Blockbench through the MCP `risky_eval` tool, in order. Because the assembled
scripts embed the skill runtime (~113 KB) and are saved files, they are executed byte-for-byte from
disk rather than retyped:

```js
(() => eval(require("fs").readFileSync("blockbench-scripts/handheld_searchlight/model.js", "utf8")))()
(() => eval(require("fs").readFileSync("blockbench-scripts/handheld_searchlight/texture.js", "utf8")))()
```

Then set the display transforms (or tune them in Display mode) and export:

```bash
# Export Java Block/Item + Blockbench Project from Blockbench to this folder, then:
node blockbench-scripts/handheld_searchlight/export_java_item.mjs
```

## The two states

The dark and lit lamps share one geometry. `texture.js` derives the lit atlas
(`handheld_searchlight_on`) from the glow mask the model script authored, so the two states can only
ever differ in the lens. The item renderer swaps both half-pairs by the `Lit` tag on the stack:

* `handheld_searchlight_gui` / `..._gui_on` -- the 16x16 inventory sprites;
* `handheld_searchlight_held` / `..._held_on` -- the element models used in hand, on the ground and
  in item frames.

## The hand hold

Java item models pivot their display transforms on model space `[8, 8, 8]`, because `ItemRenderer`
shifts every model by `-0.5` blocks before drawing it. The lamp's rubber grip is therefore authored
with its centre exactly on `[8, 8, 8]`, and `model.js` re-seats the runtime's entity-style bottom
centring afterwards so that stays true (`javaPivot`). `HandheldSearchlightAssetTest` fails if the
grip ever drifts off that point.

The display transforms themselves are derived from the vanilla hand frames:

* **third person** (`ItemInHandLayer`): the hand frame is `Rx(-90) * Ry(180)`, and the entity
  renderer then flips the model's X/Y axes (`scale(-1, -1, 1)`). In that flipped frame the lamp
  needs its grip on `-Y` and its lens on `-Z`, which `H * D` solves to `D = Rx(90)` -- so the
  position is `rotation [90, 0, 0]`. Adding a Z turn here (the first attempt used `[90, 0, 180]`)
  hangs the lamp head-down under the fist, which reads as the player yanking a pump handle;
  `HandheldSearchlightAssetTest` now pins that Z stays at zero;
* **first person**: the item is drawn in view space, so `rotation [0, 0, -12]` is enough -- the small
  Z tilt leans the lamp away from the crosshair, and the engine's left-hand negation mirrors it;
* **ground** lifts the flange to the floor and **fixed** turns the lens to the item frame.
