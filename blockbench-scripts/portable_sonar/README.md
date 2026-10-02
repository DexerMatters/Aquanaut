# Portable sonar

A hand-held depth sounder for divers. The body is an enamelled instrument shell
carrying a hooded sonar display, a rubber keypad, a rotary thumbwheel on the
flank and a chin-mounted transducer drum whose dark aperture points forward and
down into the water. The grip is sealed and ribbed, the back carries a louvred
speaker, and every control is real geometry rather than paint.

The geometry, atlas, glow mask and display transforms are authored in
Blockbench through the `blockbench-script` skill. This folder is the source of
truth for the model.

## What is here

| file | role |
| --- | --- |
| `src/model.body.js` | geometry + diffuse atlas reservation + glow mask, assembled into `model.js` with the skill runtime |
| `src/texture.body.js` | atlas paint plus the derived active atlas, assembled into `texture.js` |
| `build.sh` | assembles both scripts (comment-free, `console`-free) with the skill's `runtime.js` |
| `portable_sonar.bbmodel` | the saved Blockbench project |
| `portable_sonar_export.json` | Blockbench's raw Java Block/Item export of the project |
| `export_java_item.mjs` | splits that export into the five game model JSONs and copies the two 64x64 atlases into the mod |
| `portable_sonar.png` | the diffuse atlas (64x64) |
| `portable_sonar_glowmask.png` | the emission mask |
| `portable_sonar_on.png` | the active atlas, derived from the glow mask |

## What the 25 cuboids are

| part | role |
| --- | --- |
| `grip_heel`, `grip`, `grip_collar` | sealed rubber heel, ribbed rubber grip, brushed chrome collar |
| `chassis` | enamelled shell with moulded carry ribs on top, a moulding seam and rating plate on the flanks |
| `bezel_top/bottom/left/right` | the faceplate: a 2-unit-deep recess standing proud of the shell |
| `screen` | glass display, one unit back inside the recess |
| `key_a` .. `key_d` | four rubber keys standing on the recess floor; `key_d` is the ranging key |
| `visor`, `visor_left`, `visor_right` | three-sided anodised sun hood over the glass |
| `sonar_drum`, `sonar_face` | the transducer drum under the front, with its dark aperture proud of the drum face and a dark radiating face below |
| `thumbwheel`, `side_pad` | rotary gain wheel on the left flank, moulded rubber pad on the right |
| `grille_top/bottom/left/right`, `speaker` | raised rear bezel with a holed speaker plate flush in its opening |

## Rebuilding

```bash
blockbench &                                  # MCP plugin on port 3000
./build.sh                                    # assemble model.js / texture.js
```

Run the phases in Blockbench through the MCP `risky_eval` tool, in order. The
assembled scripts embed the skill runtime (~113 KB) and are saved files, so they
are executed byte-for-byte from disk rather than retyped:

```js
(() => eval(require("fs").readFileSync("blockbench-scripts/portable_sonar/model.js", "utf8")))()
(() => eval(require("fs").readFileSync("blockbench-scripts/portable_sonar/texture.js", "utf8")))()
```

A `model.js` rerun rebuilds geometry and textures, so always rerun `texture.js`
afterwards. A texture-only tweak needs `texture.js` alone.

## The two states

The idle and ranging sonars share one geometry. The display, the transducer
aperture and the ranging key are the only emissive surfaces: `model.js` authors
`portable_sonar_glowmask`, and `texture.js` derives `portable_sonar_on` from it,
so the two states can only ever differ in those three places. Swapping states is
a texture swap on the existing faces, not a second model.

## Scale and the hand hold

One Minecraft block is 16 Blockbench units. The instrument measures
8 x 13 x 10 units (about 0.5 x 0.8 x 0.6 block), i.e. a compact hand-held unit,
not a block-sized object. The grip cube's centre is authored at `[8, 8, 8]`, the
model-space point the Java item renderer pivots every display transform on
(`ItemRenderer` shifts the whole model by `-0.5` blocks before drawing it), so
the sonar swings about the fist rather than an arbitrary point. `model.js`
re-seats the runtime's entity-style bottom centring with a `javaPivot` pass to
keep that true.

Display transforms are stored on the project and exported with the model:
third person `[90, 0, 0]` at 0.85, first person `[0, 0, -12]` at 0.8, ground
lifted 3 units and `fixed` turned `[0, -180, 0]`.

## Wiring it into the mod

Done. The item is registered as `aquanaut:portable_sonar` and works in both states:

| mod file | role |
| --- | --- |
| `scripts/GeneratePortableSonarAssets.py` | draws the 16x16 inventory sprites, both states |
| `scripts/GenerateSonarScopeTexture.py` | draws the scope face and the flat white the water effect samples |
| `models/item/portable_sonar.json` | the `builtin/entity` bridge carrying the display transforms and the particle sprite |
| `models/item/portable_sonar_gui.json` / `_gui_on.json` | the inventory sprites |
| `models/item/portable_sonar_held.json` / `_held_on.json` | the element model, in hand, on the ground and in a frame |
| `textures/item/portable_sonar_model.png` / `_model_on.png` | the two atlases, copied from this folder |
| `common/item/PortableSonarItem.java` | the trigger: the cool-down and the powered flag |
| `client/renderer/item/PortableSonarItemRenderer.java` | swaps the four models by the ranging flag |
| `data/aquanaut/recipes/portable_sonar.json` | the crafting recipe |
| `PortableSonarAssetTest` | pins the sprites, the shared geometry, the held pivot and the locale keys |

## The pulse

A right-click fires. Sneak-right-click powers the instrument down. The display lights
for exactly as long as the reading lasts and then goes dark on its own.

A cavity is a hollow *behind* rock, in both senses: the pulse goes into the face it struck
rather than along it — probing along the ray steps sideways out of a wall met at a glancing
angle and lands back in the water it came from — and the hollow has to be out of sight from
where the diver is floating, or the instrument is only reporting their own surroundings back
at them. `SonarPulseTest.theCavityProbeGoesIntoTheFaceAndNotBackOutOfIt` pins the first half.

The instrument reports water that answered, never what answered. A contact crosses the
wire as a bearing, a range and a strength, and the four voices are a colour and a sound
the diver learns to read — there is no name, no figure for the distance and no legend on
the dial, and `SonarPulseTest.noVoiceIsEverNamedToThePlayer` pins that absence. The last
reading stays on the scope until that instrument fires again, resolved against wherever
the diver is now, so the plot stays true while they swim and turns with their head.

| piece | where | what it is |
| --- | --- | --- |
| `common/sonar/SonarPulse.java` | timing | range, cool-down, and every question the water, the scope and the echoes ask of a ping's age |
| `common/sonar/SonarSignal.java` | voices | the four kinds of return, their colours and how tightly each one merges |
| `common/sonar/SonarSphere.java` | rays | the listening directions, and folding rays into contacts |
| `common/sonar/SonarScan.java` | reading | the server's scan: bodies, the mineral tag, the hollows behind faces |
| `common/sonar/SonarCavity.java` | reading | which blocks are looked at past a face, and in which direction |
| `common/sonar/SonarPlot.java` | dial | bearing, range and height as the scope draws them |
| `network/SonarPingPayload.java` | wire | one packet per ping, to everyone close enough to watch it |
| `client/sonar/ClientSonarData.java` | playback | the ping's clock, its echo sounds and its bubbles |
| `client/sonar/SonarWaveRenderer.java` | water | the shell, the horizon circle, the strike flares and the comets running home |
| `client/sonar/SonarScope.java` | HUD | the bottom-right dial: sweep, blips, height stems and the contact list |
| `data/aquanaut/tags/block/sonar_minerals.json` | data | what the instrument hears as crystal |
| `data/aquanaut/tags/entity_type/sonar_abyssal.json` | data | what it hears as the long echo |

Everything the pulse knows lives in the tags and in the four voices, so new ore, new
fish and the abyssal content still to come arrive by pointing data at them. The
sounding that survives from the first version is the vertical ray that ignores fluids
and reports the distance to the sea floor rather than to the water above it; it is now
the one number the scope has no room for, so it is still said in the action bar.

Rebuild the game assets after re-exporting from Blockbench:

```bash
python3 scripts/GeneratePortableSonarAssets.py
python3 scripts/GenerateSonarScopeTexture.py
node blockbench-scripts/portable_sonar/export_java_item.mjs
./gradlew test --tests 'com.dexer.aquanaut.PortableSonarAssetTest' --tests 'com.dexer.aquanaut.SonarPulseTest'
```
