# Deep-Ocean Reach, Middle-Sea Balance & Crystal Rules

Six related requests on the ocean worldgen, the Crystal Nest (水晶巢) and the Brimstone
Caldera (火山群系):

1. **Extend the deep-ocean area of Minecraft** — horizontally — so mod biomes have more
   map area to spread into.
2. **Balance the distribution of the added biomes** — even shares among the four
   middle-sea biomes; the reef pair stays 50/50.
3. **Crystals grow only on 晶巢岩** (`crystal_nest_stone`) — strictly, everywhere:
   worldgen rooting *and* block survival/placement. No druse, no legacy rock, no columns.
4. **晶巢岩 is the rock, not a crystal kind** — redraw `crystal_nest_stone` as plain
   stone matrix (it currently carries embedded crystal glints).
5. **Crystal palette by context** — glowing crystals only inside the cores; outside only
   烟晶, 白晶 and 紫晶; inside the cores any 晶 may grow, with the glowing kinds rare.
   (This supersedes the earlier "new nest-coloured crystal outside" request: that kind is
   dropped — the rock is plain rock and the outside trio does the work.)
6. **Solid branches** — the lattice struts must have no 空腔 (no internal bores/tunnels).
   And **火山群系 generates structures, not just mountains** — more non-mountain
   formations and denser feature structures in Brimstone Caldera.

Decisions confirmed with the requester: horizontal (not vertical) deep-ocean expansion;
even shares for `middle_level_ocean` / `brine_mirror_gorge` / `brimstone_caldera` /
`crystal_nest`; strict 晶巢岩-only support; drop the new cluster block in favour of a
plain-rock nest stone; caldera variety via formations + features (no vanilla structure
sets).

## A. Widening the deep-ocean domain

The mod's biomes live inside deep-ocean columns: `NoiseBasedChunkGeneratorMixin` hands
every fully submerged parent-biome column to the `OceanLayerStack`, which rewrites it
into the surface / reef / middle-sea bands. More deep-ocean area on the map therefore
means directly more room for every biome on the stack — for this mod and for any
datapack or mod that contributes layers.

The existing lever is `RandomStateMixin`: it wraps `continents`/`continentalness` in
`ScaledDensityFunction(wrapped, 3.0)`, which evaluates `min(f(x), f(x/3))` — the climate
noise sampled three times compressed horizontally, merged with `min`, so negative
(ocean) continentalness dilates and every coastline pushes outward.

Changes:

- **Raise the horizontal scale 3.0 → 4.5**, promoted to one named constant
  (`OceanCoverage.HORIZONTAL_SCALE`, new tiny class in `common.worldgen`) so the mixin
  and the tests read from one place. `min` semantics stay: the transform may only widen
  oceans, never narrow them, and router and sampler must keep using the same scale.
- **Widen the admission gate**: lower `min_open_water_columns` 16 → 12 in
  `default_deep_stack.json` and `DefaultOceanLayerStacks.defaultDeepStack()` in lockstep,
  so marginal submerged columns join the stack instead of being left vanilla. (Tuning
  knob: revert to 16 if transition field quality degrades.)
- **Tests**: new `ScaledDensityFunctionTest` with a synthetic `DensityFunction` (linear
  in X, constant in Y/Z) asserting the `min(original, compressed)` widening contract and
  Y/Z pass-through; update `OceanLayerStackJsonTest` for the new default. In-game
  coverage is a dev-world smoke check (the share depends on vanilla climate noise and is
  not unit-testable) — record before/after `F3` biome-share screenshots in the PR.

## B. Balancing the middle-sea mix

Today's `middle_sea` mix (in `OceanLayer.middleSea()` and `default_deep_stack.json`,
kept in sync by the stack-parity tests) carries weights 0.30 / 0.55 / 0.35 / 0.50 for
MLO / brine gorge / brimstone caldera / crystal nest. Two structural quirks distort the
shares beyond the raw weights:

- `middle_level_ocean` and `brine_mirror_gorge` share one noise channel (same offsets
  and salt 7) inside what is otherwise a per-entry argmax-of-weight×affinity mix, so the
  pair behaves like a soft binary sub-mix and the gorge crowds out the others.
- The argmax scheme gives each entry its weight-proportional share only when the noise
  channels are statistically symmetric (same scale, distinct salts).

Changes (target: **25% ± 3 pp per biome** of the middle-sea floor area):

- Give each of the four entries **its own noise channel** (distinct offsets and salts,
  following the crystal-nest entry's pattern) so districts stay coherent and the
  channels are statistically independent.
- Set **equal weights** (1.0 each) as the first calibration point, then measure and
  nudge weights against the tolerance band below. Mirror every change in both
  `OceanLayer.middleSea()` and `default_deep_stack.json`.
- Re-check the crystal nest's visibility ramp (`CrystalNestTerrain.WEIGHT_FLOOR` /
  `WEIGHT_FULL`, 0.08 → 0.35): with equal weights the nest's continuous weight peaks
  lower, so lower `WEIGHT_FULL` (e.g. → 0.28) if full-strength lattice districts shrink;
  `CrystalNestPlacementTest` sampling guards this.
- **Tests**: new `BiomeDistributionTest` in `common.worldgen.layers` — sample
  `dominantBiomeAt` over a large quart grid (several 1024×1024 windows at different
  offsets) and assert each biome's share lands in 25% ± 3 pp. The reef pair keeps its
  50/50 soft-binary mix untouched. Update the existing parity/weight assertions in
  `BrimstoneFeatureLayoutTest`, `CrystalNestPlacementTest` and
  `MiddleLevelOceanColumnRulesTest` where they encode today's channels.

## C. Crystals root only on 晶巢岩

**Rule**: every crystal kind — the seven clusters, `crystal_sprout` (晶芽),
`crystal_column` (晶柱), `crystal_fringe` (垂晶穗) — may only grow out of
`aquanaut:crystal_nest_stone`. Everything else (legacy floor rock, 晶簇壳 druse, other
crystal blocks, algae mats) is not a valid support. This applies to natural generation
*and* to block survival: a crystal whose support stops being 晶巢岩 pops off.

- **Support tag** `data/aquanaut/tags/block/crystal_growth_support.json` containing only
  `aquanaut:crystal_nest_stone`. Strict by content, but tag-based so a sibling mod can
  opt its own stone in deliberately.
- **Block rules**:
  - `CrystalClusterBlock.canSurvive` — support face must be sturdy **and** backed by the
    tag; `getStateForPlacement` refuses other faces. `updateShape` already drops the
    block when `canSurvive` fails.
  - `CrystalPlantBlock` gains an optional support-tag predicate (constructor parameter,
    default `null` = current behaviour) so `algae_tuft`, `calcite_quill`, `gypsum_rose`,
    `sulfur_crystal` etc. are untouched; `crystal_sprout` gets the tag.
  - `crystal_column` (RotatedPillarBlock) and `crystal_fringe` (DroopingSeaweedBlock):
    the anchor end (column base / fringe top) must meet 晶巢岩 or the block breaks.
- **Worldgen** (`CrystalNestSkin` + `CrystalNestTerrain`):
  - The crystal support rule becomes *the support cell is lattice rock* (`Kind.ROCK` =
    晶巢岩). The current "whole opaque non-translucent block" rule (`isFullBlockSupport` /
    `CrystalNestTerrain.isCrystalBase`) is split: crystals check ROCK only; vegetation
    (tufts on mats) keeps the old permissive check. `isCrystalBase` retires — legacy
    terrain never supports crystals any more.
  - **Druse interplay (key decision)**: chamber walls are lined with 晶簇壳 today, and
    the strict rule would strip the chambers bare. Instead the crust *yields to crystal
    roots*: where a cluster/column/fringe roots on a chamber wall, the support cell stays
    晶巢岩 instead of being crusted to DRUSE. Implementation: the decor/column claim pass
    runs before the ROCK→DRUSE lining pass (or the lining pass skips claimed supports).
    Every crystal then literally grows out of 晶巢岩, and druse coats only the
    crystal-free remainder of the lining.
- **Tests**: rewrite `CrystalNestSkinTest.crystalsAndTuftsOnlyRootOnWholeSolidBlocks` as
  `crystalsOnlyRootOnNestRock` (ROCK supports grow crystals; legacy-solid and DRUSE
  supports never do; tufts still root on mats); add a skin case asserting a chamber wall
  keeps ROCK under every crystal it carries.

## D. 晶巢岩 is the rock; palette by context

**Plain rock.** `crystal_nest_stone` / `crystal_nest_stone_top` are redrawn as ordinary
stone matrix: in `scripts/GenerateCrystalNestTextures.py`, `nest_stone()` keeps its
slate-blue mottling and fracture seams but **drops the embedded micro-crystal glints and
facet flecks**. The rock reads as rock; the crystals on it are the only crystal imagery.
(`crystal_druse` keeps its druzy teeth — that is its job as the chamber lining.)

**Palette policy** (supersedes both earlier versions of this rule; plain integer shares
of the decor roll, starting points to tune):

- **Outside — exposed/open faces of the lattice**: only 白晶 `white_crystal_cluster`,
  烟晶 `smoky_crystal_cluster`, 紫晶 `amethyst_crystal_cluster` (e.g. 45 / 35 / 20).
  Never glowing, never rose/aqua/life outside. This replaces both the "mostly white plus
  a coloured minority" open palette and the dropped nest-crystal idea in `decorFor` /
  `openColor` (`CLUSTER_WHITE`, `CLUSTER_SMOKY`, `CLUSTER_AMETHYST` only).
- **Inside — the core chambers**: any 晶 may grow — white, rose, amethyst, aqua, smoky
  plus the glowing pair `resonant_crystal_cluster` / `life_gem_cluster`. The glowing
  kinds stay **rare** (≈5–8% combined, e.g. resonant 4% / life 3%), the rest of the
  chamber roll split across the five quiet kinds (`chamberColor` retuned). Glowing
  crystals appear **only** in chambers — the existing chamber-only constraint stays,
  now enforced against the new outside trio as well.

**Tests**: `CrystalNestSkinTest` — open faces carry only the trio (never glowing, never
rose/aqua/life); chamber faces carry kinds from the full set with the glowing pair under
the rare-share bound (the existing `glowingCrystalsAreChamberOnlyAndTheSurfaceStaysQuiet`
tightened to assert the trio and the share cap).

## E. Solid branches — no 空腔 in the struts

Today `CrystalNestLattice` pipes are hollow tubes (`TUNNEL_RATIO 0.72` strut bores,
`ROOT_TUNNEL_RATIO 0.45` root bores) and `CrystalNestField` drills them through the
struts so core chambers link up ("mouths", `MOUTH_PROBABILITY`, `SEAL_MARGIN`).

Change: **the branches become solid.** Struts and roots are full nest stone along their
whole run — no bore, no internal cavity. The 空腔 remain only where they belong: the
big irregular chamber inside each geode core (`CAVITY_RATIO_*`, `HOLLOW_PROBABILITY`).

- `CrystalNestLattice`: retire `TUNNEL_RATIO`, `ROOT_TUNNEL_RATIO`, `strutTunnelRadius`,
  `nodeMouth` / `MOUTH_PROBABILITY` / `SEAL_MARGIN` (with no bores there is nothing for
  a mouth to open into; every chamber is its own sealed room in the core).
- `CrystalNestField`: drop the tunnel/hollow-bore pass (`tunnelValue`, tunnel noise) —
  `hollowAt` answers only for core chambers.
- Javadoc rewrite: "the struts are hollow tubes…" → solid skinned branches; the walkable
  network is the core chambers bored through rock, not pipe bores.
- **Tests**: `CrystalNestLatticeTest` — replace `strutTunnelRadius < strutRadius` with
  solidity assertions (no tunnel API remains; strut radii unchanged); `CrystalNestFieldTest`
  — hollow samples occur only inside core chambers, never inside a strut's span.

## F. Brimstone Caldera: structures, not just mountains

The caldera floor today is dominated by stratovolcano cones ("mountains") with a modest
decoration layer. Goal: every volcano district has a real chance to be a *structure* —
a built-looking or compound formation — and the prop features dress the biome densely
enough that it never reads as bare cones.

- **Formation kinds** (`VolcanoGeometry` + `VolcanicTerrain`, driven per field-grid seed
  through `OceanColumnPlanner`): give each seed a weighted formation kind instead of a
  cone by default. Keep the existing vocabulary (stratovolcano with buttresses, caldera
  bowl with rim, summit plug + breach, parasitic cones, talus apron, swell-and-rift
  plain) and rebalance so **non-plain-mountain outcomes are at least half of all seeds**,
  adding a few structure-like kinds: lava dome complexes, obsidian spine fields, rift
  cross-section structures (fault scarps stepping the floor), collapsed caldera groups
  with nested craters, and fumarole plateaus.
- **Denser feature structures**: raise the placement density of the eight biome props —
  `hot_spring`, `smoker_cluster`, `fumarole_field`, `sulfur_veins`, `brimstone_garden`,
  `ash_drifts`, `acid_lake`, `vent_flora` — in their `placed_feature` JSONs (rarer
  `rarity` steps / higher count ranges) and bias them into *complexes* (smokers cluster
  near fumarole fields, gardens around hot springs) instead of uniform scatter.
- Keep everything on the deterministic column planner and placed features — no vanilla
  structure sets (confirmed).
- **Tests**: `VolcanoGeometryTest` gains a formation-kind distribution assertion
  (non-cone kinds ≥ 50% of a large seeded sample, per-kind presence); extend
  `BrimstoneFeatureLayoutTest` with density parity between the `placed_feature` JSONs and
  expectations, and the JSON↔code stack-sync checks.

## Rollout order

1. **§C + §E + §D** — one Crystal Nest pass: support rule, solid branches, plain-rock
   texture and the palette split (the skin touches all three).
2. **§F** — caldera formations + feature density.
3. **§B** — balance pass, tuned against the distribution band.
4. **§A** — deep-ocean widening, verified last in-game.

## File change map

| Area | Files |
| --- | --- |
| A | `mixin/RandomStateMixin.java`, new `common/worldgen/OceanCoverage.java`, `layers/DefaultOceanLayerStacks.java`, `data/…/ocean_layer_stack/default_deep_stack.json`, new `ScaledDensityFunctionTest` |
| B | `layers/OceanLayer.java`, `data/…/ocean_layer_stack/default_deep_stack.json`, `CrystalNestTerrain.java` (ramp constants), new `layers/BiomeDistributionTest`, parity tests listed above |
| C | new `data/aquanaut/tags/block/crystal_growth_support.json`, `block/CrystalClusterBlock.java`, `block/CrystalPlantBlock.java`, `BlockRegistry.java` (column/fringe support), `worldgen/crystalnest/CrystalNestSkin.java`, `worldgen/CrystalNestTerrain.java`, `CrystalNestSkinTest` |
| D | `scripts/GenerateCrystalNestTextures.py` (plain `nest_stone`, palette retune), regenerated `crystal_nest_stone*.png`, `CrystalNestSkin.java` (`decorFor` / `openColor` / `chamberColor`), `CrystalNestSkinTest` |
| E | `worldgen/crystalnest/CrystalNestLattice.java`, `worldgen/crystalnest/CrystalNestField.java`, `CrystalNestLatticeTest`, `CrystalNestFieldTest` |
| F | `worldgen/VolcanoGeometry.java`, `worldgen/VolcanicTerrain.java`, `layers/OceanColumnPlanner.java`, `data/…/placed_feature/{hot_spring,smoker_cluster,fumarole_field,sulfur_veins,brimstone_garden,ash_drifts,acid_lake,vent_flora}.json`, `VolcanoGeometryTest`, `BrimstoneFeatureLayoutTest` |

## Superseded / open points

- **Superseded**: the "nest crystal" cluster block (§D of the previous revision) and the
  "white only inside" palette rule — replaced by §D here.
- Exact roll splits inside the outside trio (45/35/20 starting point) and the chamber
  glowing share (5–8%) are tunable against screenshots.
- §A's exact scale value (4.5 starting point) is decided by the in-game smoke check.
