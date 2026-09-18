# models.zip Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship everything usable in `models.zip` as Aquanaut content: fourteen fully designed creatures and a dissection table whose three sizes are assembled from the base table.

**Architecture:** A Blockbench-driven pipeline exports the archive into geometry and animation assets (`scripts/export_blockbench_models.py` drives a running Blockbench through the MCP plugin; `scripts/GenerateSpawnEggTextures.py` and `scripts/GenerateDissectionTableItemIcon.py` paint the item sprites). Creatures are ordinary `BaseFishEntity` subclasses plus GeckoLib models/renderers registered through the existing `core` registries; the dissection table is a waterloggable block backed by a GeckoLib block entity whose merge size is resolved by a pure, unit-tested rule.

**Tech Stack:** Java 21, NeoForge 21.1.233, GeckoLib 4.8.4, Blockbench sources, Python 3 + Pillow asset scripts, JUnit 5.

---

## Scope decisions

- **Bell Fish is out of scope.** The archive only contains `Bell Fish.animation.json` (6 bones, 8 s); there is no model or texture, so nothing is registered for it.
- **Dissection-table interaction is deferred.** The block, its three merged sizes, rendering, item, recipe and loot table ship now; `DissectionTableBlockEntity` carries a documented extension point (`TODO(dissection)`) for specimen insertion and dissection results.
- **Spawn eggs only, no natural spawning.** This matches every existing Aquanaut creature; the biome spawner lists stay vanilla-only.

## Task 1: Blockbench export pipeline

**Files:**
- Create: `scripts/export_blockbench_models.py`
- Create: `scripts/blockbench/README.md`

- [x] Drive a running Blockbench through the MCP plugin: open each `.bbmodel` (Electron single-instance file handoff), switch the project to the `geckolib_model` format, canonicalise the clip names and let Blockbench compile the geometry (`Codecs.bedrock.compile`) and the animations (`AnimationCodec.getCodec().compileFile`).
- [x] Rely on Blockbench's own Bedrock conventions instead of re-implementing them: `compileCube`/`compileGroup` mirror the model in X and negate the X/Y rotations, the GeckoLib plugin forces `format_version: "1.12.0"` and strips `item_display_transforms`, and GeckoLib projects get `{"post": {"vector": [x, y, z]}}` keyframes with inverted X/Y rotations.
- [x] Rename clips to the verbs the entity classes play (`swim`/`charge`/`attack`/`open`/`close`/`target`); the keyframes themselves are untouched, and a few clips keep their authored one-tick loop slack exactly like the shipped animations.
- [x] Export 14 entity geo + animations and 3 dissection table geo, plus the textures embedded in the projects (+4 glowmasks).
- [x] Keep the dissection table's authored vertical placement (`y = -10 … 6`) and lift it by `10/16` blocks in `DissectionTablePlacement` so the feet rest on the block floor.
- [x] Correct the one project that is not authored in the mod's conventions: `flagellonautilus` had its root yawed `-90` (head on `+Z`, i.e. backwards, and the only model whose long axis ran along X) and its geometry authored ~6.5 units off-centre. The export script fixes both inside Blockbench, state-based and idempotently, and the result measures `12 × 19.5 × 23` units with the head at `z = -7.4`, matching every other model.
- [x] Prove the pipeline against the author's own exports: the three table geos are semantically identical to the archive's `dissection_table*.geo.json`, and `golden_carp` matches the archive's `golden_carp.geo.json` (bones, cubes, `visible_bounds`, `format_version`) wherever the project was not edited afterwards. `AnimAudit` (throwaway harness) loads all 14 animations through GeckoLib's `BakedAnimationsAdapter` successfully.

## Task 2: Spawn egg sprites

**Files:**
- Create: `scripts/GenerateSpawnEggTextures.py`
- Create: `scripts/GenerateDissectionTableItemIcon.py`

- [x] Measure the 24 shipped eggs: canonical silhouette (12×14 inside a 16×16 canvas, ~131 opaque pixels), mean lighting-ratio map, ~5 % boundary jitter, 54–118 distinct colours, median adjacent-pixel contrast 36, median relative luminance spread 0.46.
- [x] Draw each creature's own emblem on its egg with small pixel primitives (discs, rings, arcs, lines, spirals, sparks, minnows) in a ten-role tone palette, so every egg is a different composition built from what the creature is: a lamprey's toothed sucker with a glowing eye, an ore-studded boulder hide, a chambered shell spiral with trailing tentacles, a corner-to-corner fish fossil, a golden flank with a fan tail and a luck sparkle, a shoal of minnows racing along the blinding flash, a top hat with monocle and bow tie, glossy slime beads crawling down with a drip trail, a lightning bolt through a plasma coil, one enormous eye, twin bells mirrored across the egg, a pocket reef with coral and a minnow, a glowing maw with six reaching arms, and three shark heads in a diagonal pack.
- [x] Keep the sprites in the shipped visual language: per-species body canvas (light/dark/split), a body texture pass that never paints over the emblem, and contrast auto-matched to the shipped local-contrast median while staying inside the reference spread.
- [x] Enforce the reference envelope per sprite (opaque pixels, distinct colours, relative luminance spread, mean luminance), a minimum pairwise distinctness between eggs (the shipped set's closest pair differs by 19.6, ours by 36.4), and print the `ItemRegistry` colour pair.
- [x] Draw the 16×16 dissection table icon in the existing item-sprite grammar from the table's own palette.

## Task 3: Creatures

**Files:**
- Create: `common/entity/{AbstractCarp,Vamprey,Oresucker,Flagellonautilus,GoldenCarp,SilverCarp,SkeletonCarp,Gentlefish,Slimy,Ionfin,Opticichthus,GeminiJellyfish,Ecofish,PaleAbyssHydra,ThreeHeadedShark}Entity.java`
- Create: `client/model/*Model.java`, `client/renderer/*Renderer.java`

- [x] Implement the fourteen species with the AI envelope, hitboxes and mechanics in the table below.
- [x] Wire `EntityRegistry` (types + attributes), `ItemRegistry` (spawn eggs + creative tab), `ClientModEvents` (renderers + item colours) and `Config.ORESUCKER_GRAZING`.

| id | category / diet | health | hitbox | clips | signature behaviour |
| --- | --- | --- | --- | --- | --- |
| `vamprey` | threatening / carnivorous | 8 | 0.6 × 0.5 | swim, open, close, charge | hunts the nearest diver from 9 blocks; a bite drains 60 ticks of air and heals it |
| `oresucker` | friendly / herbivorous | 6 | 0.5 × 0.4 | swim | grinds exposed ore into stone and stores the metal (cap 8, dropped on death) |
| `flagellonautilus` | threatening / carnivorous | 14 | 0.9 × 1.2 | swim, attack | travels shell-first for as long as it is moving — the renderer draws the model half a turn from its heading and the heading follows the travel direction, so cruising and fleeing look alike; inside 4 blocks it stops, pins its heading on the diver and plays the `attack` clip, which turns the whole creature another half turn (0.25 s in, back by 1.25 s, exactly the 25-tick attack state) to bring the shell and its 45-segment whip onto the diver — the renderer drops its own half turn while that state lasts, because applying both would cancel and strike backwards — then it jets away shell-first again |
| `golden_carp` | friendly / herbivorous | 6 | 0.6 × 0.85 | swim | grants `Luck` to a nearby onlooker every 10 s |
| `silver_carp` | friendly / herbivorous | 5 | 0.6 × 0.85 | swim | shoal flash blinds its attacker and scatters the school |
| `skeleton_carp` | threatening / carnivorous | 10 | 0.6 × 0.85 | swim | reassembles once instead of dying, then cannot again for 60 s |
| `gentlefish` | friendly / herbivorous | 6 | 0.5 × 0.75 | swim | collects dropped items and hands them back to the diver |
| `slimmy` | friendly / herbivorous | 8 | 0.8 × 0.75 | swim | almost no drag, slimes (Slowness) whatever it bumps, sheds slime when hit |
| `ionfin` | threatening / energevorous | 12 | 1.5 × 1.5 | swim | charges near conductive divers and discharges for 4 damage + Weakness |
| `opticichthus` | threatening / carnivorous | 14 | 1.3 × 1.0 | swim, target | never bites: from up to 48 blocks it holds a strobing targeting line on a diver with line of sight, charges for 36 ticks behind a closing aperture flare, then fires a hitscan lance (8 damage, knockback, Blindness, 120 ticks of air) for 16 ticks; breaking line of sight for a second aborts the charge, and it cools down for 4.5 s after every shot |
| `gemini_jellyfish` | titan / energevorous | 120 | 4.5 × 9.0 | swim | alternates FAVOR (air + Regeneration) and DEVOUR (toxin sweeps) |
| `ecofish` | titan / herbivorous | 200 | 8.0 × 12.0 | swim | mobile oasis: refills air, heals nearby fish and seeds seaweed |
| `pale_abyss_hydra` | titan / carnivorous | 160 | 6.0 × 6.0 | swim, attack | dormant until 5 blocks, then grips, pulls and applies Narcosis III |
| `three_headed_shark` | titan / carnivorous | 90 | 2.4 × 1.5 | swim, charge, attack | charges from 16 blocks and opens three independent jaws in sequence at 0° / ±30° whenever a diver is within 2.75 blocks in front of it (the bite is checked while charging, not only while idle) |

## Task 4: Notebook and language data

**Files:**
- Create: `data/aquanaut/notebook/waterlife/<slug>.json` (14)
- Modify: `assets/aquanaut/lang/en_us.json`, `assets/aquanaut/lang/zh_cn.json`

- [x] Add encounter/capture field-note paragraphs (two per species) with the existing identification-plate and morphology-record components.
- [x] Add entity names, spawn egg names and Chinese translations; both language files carry the same key set (also filled three pre-existing `zh_cn` gaps: blue jellyfish, blue-ringed wormfish, flatfish).
- [x] Aliases follow the existing convention (`vampire_lamprey`, `ore_sucker`, `gentle_fish`, `twin_jellyfish`, `eye_fish`, `bone_carp`, `triple_head_shark`, `hydra`, `gold_carp`, `eco_fish`, `slimy_fish`, `ion_fin`, `abyss_hydra`, `flagellum_nautilus`).

## Task 5: Dissection table

**Files:**
- Create: `common/block/DissectionTable{Block,Multiblock,Placement}.java`, `common/block/entity/DissectionTableBlockEntity.java`
- Create: `client/model/DissectionTableGeoModel.java`, `client/renderer/DissectionTableBlockEntityRenderer.java`
- Create: blockstate, block model, item model, item icon, loot table, recipe; modify pickaxe tag and the registries.

- [x] Resolve the merge with a pure predicate-based rule, unit tested.
- [x] Render only the group's origin with the matching 1×1 / 2×1 / 2×2 geo model, centred over the footprint.
- [x] Put that pose in `DissectionTablePlacement` (spans, floor lift, quarter turn) so it is unit tested
      against the shipped geometry rather than hand-tuned in the renderer. The north-south bench used to
      be drawn half a block west and one and a half blocks north of its footprint — the turned branch
      translated the model as if it were still laid out along X — which only shows once two tables merge.
- [x] Waterloggable block, full-cube collision, pickaxe-mineable, drops itself; recipe `III / HSH / I I`.
- [x] Integration is intentionally inert: `DissectionTableBlockEntity` exposes `specimen()`, `progress()` and `TODO(dissection)` for the next pass.

**Merge rule (chosen to be overlap-free):** a cell can grow a bench only from a free north-west
corner — the cells to its west, north and north-west must be empty. From there it grows to the
largest of 2×2 → 2×1 (along X) → 2×1 (along Z) → 1×1 that is fully occupied, so a bench can run
either way round; a north-south bench renders the 2×1 model turned a quarter turn clockwise (the same
bench rotated over its master cell, so the model's left half still lands on the master).
A Z pair additionally needs the cell to its east to be free, because otherwise the shared corner
could grow both an X pair and a Z pair and the two would overlap: an L of three tables therefore
resolves to the western pair plus one loose table, and a 3×3 field still grows exactly one bench at
its north-west corner. Two blocks can never draw the same merged model.

## Task 6: Tests and documentation

- [x] `DissectionTableMultiblockTest`: single, 2×1, 2×2, separated benches, attached bench, partial shapes, diagonal contact, north–south pairs, 3×3 field.
- [x] `DissectionTablePlacementTest`: for each layout the projected model bounds land exactly on the group footprint (`x 0..widthX`, `z 0..depthZ`, feet on `y = 0`), the north-south bench really is turned (`-90°`) with the offsets that go with it, and the extents the placement assumes equal the bounds of the geometry that actually ships.
- [x] `NewSpeciesAssetTest`: geometry/animation/texture presence, animated bones exist in the geo, expected clips exist, spawn egg sprite envelope, notebook + language completeness, table assets and tag, plus the UV guards (both UV forms structurally valid; `mirror` only on the compact cube-level form; every face rectangle inside the declared texture; and a per-model box-UV : per-face : mirrored census).
- [x] `NewSpeciesAssetTest.opticichthusShipsItsLanceSprites`: both laser sprites are 16px pixel art. The beam (16x64) is a symmetric stair-step cross-section drawn from a small palette (five tones plus transparent), fully transparent at the ribbon edges, solid white across its six-pixel core, and it ships tiling metadata because it scrolls; the flare (16x16) has an opaque blocky core, mirror symmetry on both axes, no ink on its sprite border, and uses its whole palette.
- [x] `LaserGeometryTest`: the ribbon widen axis is a unit vector perpendicular to both beam and view and stays finite when the camera looks straight down the beam; helix strands hold a constant radius off-axis, open into a cone and actually rotate with age; the energy envelope tapers from 1 to 0.
- [x] `FishAnimationLoopTest`: all sixteen new looping clips, checking they start at `0.0`, stay within one tick of their declared `animation_length` and are pose-closed whenever a key sits on that length.
- [x] Add the `net.minecraft.core.BlockPos` and `net.minecraft.util.Mth` test shims (plus
      `ResourceLocation.withDefaultNamespace`) so the new tests can run without the Minecraft runtime;
      promote gson to `testImplementation`.

## Verification

```bash
# Blockbench must be running with the MCP plugin enabled
python3 scripts/export_blockbench_models.py --src scripts/blockbench/src --check
python3 scripts/GenerateSpawnEggTextures.py
python3 scripts/GenerateDissectionTableItemIcon.py
python3 scripts/GenerateOpticichthusBeamTextures.py
./gradlew compileJava
./gradlew test --tests 'com.dexer.aquanaut.NewSpeciesAssetTest' \
               --tests 'com.dexer.aquanaut.client.renderer.LaserGeometryTest' \
               --tests 'com.dexer.aquanaut.common.block.DissectionTableMultiblockTest'
java -cp build/classes/java/main:build/classes/java/test:build/resources/main \
     com.dexer.aquanaut.client.model.FishAnimationLoopTest
# the footprint harness needs joml + gson on the classpath; see the gradle cache
java -cp build/classes/java/main:build/classes/java/test:build/resources/main:<joml.jar>:<gson.jar> \
     com.dexer.aquanaut.common.inventory.aquarium.NewSpeciesFootprintTest
```

Result of the last run: `export_blockbench_models.py --check` reproduces all 17 checked-in geometry and
animation files byte for byte, `GenerateOpticichthusBeamTextures.py` reproduces its two sprites and the
tiling metadata byte for byte, `compileJava`/`compileTestJava` succeed, `NewSpeciesAssetTest` (9 cases),
`LaserGeometryTest` (5 cases) and `DissectionTableMultiblockTest` (11 cases) pass, `FishAnimationLoopTest`
passes for all sixteen new looping clips, and `NewSpeciesFootprintTest` confirms all fourteen geometries
parse with the game's aquarium footprint reader. The full suite is 63 tests with the six pre-existing
notebook-shim failures described below.

### Why the manual converter was discarded (evidence)

The first pass converted the projects with a hand-written Python converter. It looked plausible but
was wrong in a way that is invisible on symmetric shapes and very visible on asymmetric ones, so it
was replaced by Blockbench's own export:

* Blockbench's Bedrock exporter **mirrors the model in X and negates the X/Y rotations**
  (`compileCube`/`compileGroup` in `js/formats/bedrock/bedrock.js`); `parseCube`/`parseGroup` mirror
  it back on load. Copying the project's coordinates straight out therefore yields a model that is
  mirrored relative to what Blockbench shows — the fins, eyes, jaw halves and bell segments land on
  the wrong sides, and since the UVs are *not* mirrored with the geometry, left/right textures swap.
* GeckoLib projects get their keyframes rewritten as `{"post": {"vector": [x, y, z]}}` with the X/Y
  rotations inverted; copying the plain Bedrock export makes GeckoLib play mirrored rotations.
* The official exports in the archive are X-mirrored for exactly this reason. Reading them as
  "stale" and comparing payload sets instead of cube identity is what hid the defect.
* Deleting the `"mirror": true` flags was wrong as well: Blockbench emits them for mirrored auto-UV
  cubes and every shipped Aquanaut model uses them.
* After the switch, the three dissection table geos are semantically identical to the archive's own
  exports, and all 14 animations parse cleanly through GeckoLib's `BakedAnimationsAdapter`.

**Known state:** the full `./gradlew test` run has six failures in `NotebookLayoutTest`,
`NotebookCatalogTest` and `NotebookProgressTest`. They are pre-existing (verified against `HEAD`,
where the same methods fail, and the count went from seven to six because of the
`withDefaultNamespace` shim) and come from the test source set's incomplete Minecraft shims rather
than from this work.

## Manual client checklist

1. Spawn each of the fourteen eggs: orientation, animation speed, glow layers and hitbox alignment
   (catch with a scoop net) behave as the table above describes.
2. Sight a species in survival: the notebook gains the encountered paragraph and the identification
   plate, and capture adds the second paragraph and the morphology preview.
3. Place one table, two along X, two along **Z**, and a 2×2 square: each shows the 1×1, 2×1, turned
   2×1 and 2×2 bench respectively, and every merged bench must sit exactly on its own cells — flush
   with all four edges, feet on the floor, nothing hanging over a neighbour and nothing sunk into the
   ground (the north-south bench used to be offset by half a block west and one and a half blocks
   north). An L of three tables shows one pair plus one loose table; breaking any cell degrades
   cleanly; underwater placement renders waterlogged.
4. Check `ecofish`, `gemini_jellyfish`, `pale_abyss_hydra` and `three_headed_shark` in a dark biome
   for the intended titan scale and tracking behaviour.
5. Swim up to each threatening species and confirm the reported behaviours: the `vamprey` closes on
   the diver from 9 blocks without being provoked and drains air on contact; the `three_headed_shark`
   opens its three jaws (swim → attack animation) while still charging and lands damage on a diver in
   front of it; the `flagellonautilus` swims shell-first while cruising *and* while fleeing (both must
   look the same, with the shell leading, and the escape must not look like it is swimming back into
   the fight), and the whip must sweep *towards* the diver during the attack — the attack clip's half
   turn and the renderer's dropped half turn have to line up, so a strike that lands facing away means
   one of the two has been double-applied. All species start with their attacks on
   a cooldown, so let each one bite once before judging.
6. Watch the `opticichthus` from outside its 48-block range and then step into the open: a strobing
   cyan line should snap onto the diver, redden as the aperture flare closes, and end in a white-hot
   lance. It must look like Minecraft, not like a modern glow: nearest-filtered 16px sprites, a
   banded cross-section, brightness stepping once per beam segment, sprites snapping to whole texels
   and quarter turns, and the energy up the beam carried by chunky pixel sparks corkscrewing around
   it rather than a smooth helix. Confirm the beam stops on rock, that ducking behind cover for a
   second makes it abort with a deactivate chime, that the hit costs air and sight (Blindness), and
   that strafing during the last half-second can make it miss.
