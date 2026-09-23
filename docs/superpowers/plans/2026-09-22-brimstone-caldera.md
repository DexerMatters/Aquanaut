# Brimstone Caldera — the volcanic middle-sea biome

Design plan for the third middle-level sea biome of Aquanaut: a volcanic province of
giant, majestic volcanoes with hot springs (热泉), sulfur (硫磺), drifting dust (尘),
and a new sulfuric acid liquid pooling in acid lakes (硫酸湖).

## Concept

**Brimstone Caldera** (`aquanaut:brimstone_caldera`, 硫磺火山区) is a middle-sea
district where the crust is thin: fields of stratovolcanoes rise from an ash-dusted
plain cut by rift grabens and seamount swells. Calderas hold sulfuric acid lakes,
sinter-rimmed hot springs or dry sulfur pans; black smoker chimneys, fumarole fields
and native sulfur veins fume across the floor; thermophilic mats, sulfur moss, ember
kelp, fireblooms and sulfur stalactites colonize every warm rock. Roughly a third of
the volcanoes breach: their columnar plug pierces the reef cap and spills rubble onto
the upper floor, physically connecting the two levels.

## Architecture

The giant terrain rides the existing deterministic column planner (no cross-chunk
writes, seams impossible):

- `VolcanoGeometry` (pure, unit-tested) — field-grid volcano seeds, stratovolcano
  profiles with radial buttresses, caldera bowls with raised rims, summit plugs and
  breach conduits (`plugTopY` forces Y ≥ 41 through the reef cap), parasitic flank
  cones, dipping strata tables, talus aprons, swell-and-rift plain relief.
- `VolcanicTerrain` — binds geometry to block states: volcanic strata
  (basalt/scoria/agglomerate/pumice), columnar plugs with native sulfur veins, crater
  fills (sulfuric acid lakes / hot spring pools / dry pans), fumarole-studded rims,
  breach rubble, and the ash dusting of the plains (`AshLayerBlock` drifts).
- `OceanColumnPlanner` — plans volcanic columns per block column after floor smoothing,
  scaled by `strength(biome-mix weight × region edge fade)` so districts blend away.
- Layer stack: `middle_sea` mix gains `aquanaut:brimstone_caldera` on its own noise
  channel (`default_deep_stack.json` + `OceanLayer.middleSea()` in sync).

Decorations are ordinary placed features (small radii, biome-gated): `hot_spring`,
`smoker_cluster`, `fumarole_field`, `sulfur_veins`, `brimstone_garden`, `ash_drifts`,
`acid_lake`, `vent_flora`.

## Content

- Rocks: volcanic basalt (columns), scoria, pillow basalt, volcanic agglomerate,
  pumice, obsidian glass, acid-etched basalt, sinter, vent chimney.
- Sulphur: sulfur crust, native sulfur (drops `sulfur_lump`), sulfur crystal,
  sulfur stalactite (dripping chains).
- Dust: volcanic ash (falling), ash drift (1–8 layered dust).
- Flora: firebloom (glowing), sulfur moss, ember kelp (hanging), thermophilic mats
  (gold/rust/olive).
- Vents: fumarole (emits steam + sulfur gas particles).
- Liquid: **sulfuric acid** (source/flowing/bucket) — dense, viscous, non-renewable;
  animated still/flowing textures; exhales `sulfuric_acid_mist`. Biome ambient
  particles: `ash_mote` dust.

## Assets

Deterministic generators in `scripts/` (pure-python PNG writers, fixed seeds):
`GenerateBrimstoneCalderaTerrain.py`, `GenerateBrimstoneCalderaFlora.py`,
`GenerateSulfuricAcidAssets.py`, plus `GenerateBrimstoneCalderaData.py` for the
blockstate/model/item/loot boilerplate.

## Tests

`VolcanoGeometryTest` (shape invariants: breach heights reach the upper floor, crater
pools, flank monotonicity, determinism, parasite placement), `BrimstoneFeatureLayoutTest`
(feature suite + stage parity + stack sync), `BrimstoneCalderaPlacementTest` (unique
TerraBlender anchors).

## Revision — majesty pass (follow-up)

Two follow-up requests: retire the sulfur ore, and rebuild the volcanic landscape so it is
not "scattered one-block pillars" but a field of giant, majestic, realistic volcanoes.

**Sulfur ore removed.** `native_sulfur` is gone end to end: registry (block + item +
creative tab), lang (en/zh), blockstate/model/loot/pickaxe-tag entries, both textures and
the glowmask, and the three generators that produced them. All sulfur now reads as the
`sulfur_crust` family (crater pans, veins, vent frost, dome seams, scoria crowns).

**Root cause of the pillars.** Three separate sources, all fixed:

1. The generic middle-sea terrain module grew sedimentary outcrops (`pillarStateFor`:
   shale/limestone/stone 1x1 columns up to 20 blocks tall) — including inside the caldera.
   `OceanColumnPlanner.ColumnPlan#stateForY` now gates both pillar branches on
   `volcanic == null`; the caldera floor grows volcanic landforms instead.
2. Summit vents were thin tapered spines. The vent is now a broad fluted lava dome, and a
   breached volcano's conduit is a thick resurgent neck (`domeRadius`, >= 4 blocks wide)
   that climbs through the reef cap to the upper floor.
3. `SmokerClusterFeature` built lone 1x1 chimneys. It now builds honeycomb vent complexes:
   a rubbly sulfide mound crowned by 2-5 fluted, tapering chimneys with bulbous flanges, a
   frosted sulfur rim and a fuming mouth on the lead stack. `FumaroleFieldFeature` vents
   now sit on siliceous sinter cones with sulfur frosting and crystal blades.

**Majesty.** Volcanoes are bigger (base radius 24..43, height 32..53) and shaped like real
stratovolcanoes: a concave profile, radial buttresses and re-entrants on the outline,
downslope-deepening barrancos raking the flanks, a wide caldera with a raised rim, lobate
debris aprons, and — for the ~34% that breach — a notch torn out of the rim plus a spill
chute (sciara) scoured down one flank and surfaced with acid-etched basalt / sinter /
sulfur crust by crater type. Between the giants, a field of satellite scoria cones and
spatter ridges (`STACK_CELL` 30, `STACK_CHANCE` 0.58) covers ~58% of the district's cells:
the many small vents of a real volcanic field.

**Verification.** `VolcanoGeometryTest` grew to 16 tests (giant massifs, broad flat-topped
breach necks, breach notch factor, spill-chute azimuth, satellite-vent density and
low-cone proportions; flank monotonicity now checked as a ring average so individual
barrancos may be rugged). Full suite: 204 tests, only the 6 pre-existing notebook
failures. `runData` loads registries with 0 errors, and the three edited generators run
clean; cross-sections rendered straight from `VolcanoGeometry` confirm the silhouettes.

