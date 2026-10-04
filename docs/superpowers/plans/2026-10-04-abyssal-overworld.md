# Abyssal Overworld — breaching the vanilla Y limit

The overworld now spans **Y=-512 … Y=319 (832 blocks)** instead of `-64 … 319`. Extending the Y
range changes exactly one thing about the mod's vertical story: the **abyss band (`deep_sea`) grows
downward until it is as tall as the middle sea**. Everything else — the surface ocean, the reef
ceiling, the middle sea and its chamber — keeps the position and height it was authored with, and
the area below the abyss band is deliberately left unclaimed for future abyssal content.

Decisions taken with the requester:

1. **Abyssal depth** — bottom at `Y=-512`, total height 832, top unchanged at `Y=320`.
2. **Every overworld** — the vanilla `minecraft:overworld` dimension type is deepened too, not only
   Aquanaut's own world preset, so existing saves and the vanilla presets get the new depth.
3. **Only the Y range is extended** — the middle sea is *not* resized, and no other band moves.
4. **`deep_sea` as tall as `middle_sea`** — the abyss band is extended downward to the middle sea's
   height; everything below it stays reserved for a future implementation.

## The height contract (read before touching any of these files)

* `data/aquanaut/dimension_type/abyssal_overworld.json` — the mod's own overworld type
  (`min_y=-512`, `height=832`, `logical_height=832`). The water world preset uses it.
* `abyssal_overworld_pack/data/minecraft/dimension_type/overworld.json` — the vanilla override,
  shipped in a **discoverable, default-enabled, disableable** data pack (see below).
* `abyssal_overworld_pack/data/minecraft/worldgen/noise_settings/{overworld,large_biomes,amplified}.json`
  and `data/aquanaut/worldgen/noise_settings/water_world.json` — the generator envelopes, widened to
  `min_y=-512`, `height=832`. **A dimension type deeper than its noise settings is a void**: the
  engine intersects the two (`NoiseSettings.clampToHeightAccessor`) and never generates the
  difference, so these four files must move together with the dimension type.

Rules that must not be broken:

* **Never lower `min_y` again.** Shrinking a loaded world deletes every section below the new floor
  on the next save, silently (`ChunkSerializer` skips out-of-range sections without a word).
* **Never rename or remove `abyssal_overworld`.** Saves store the dimension type *id*, not its
  contents, so removing an id a world references makes that world fail to load.
* **Never move between two heights that share the same `ceillog2(height + 1)`** (832 and 576 both use
  10 bits). Heightmaps are stored as min-build-height-relative values with that bit width; the length
  check misses them and every stored height is silently offset by the delta. 384 → 832 is safe (9 →
  10 bits, so the engine logs `Ignoring heightmap data … size does not match` and re-primes).
* `logical_height` must equal `height`. It is the only cap on nether-portal Y and chorus fruit; a
  stale 384 at `-512` makes `PortalForge` return no portal at all.

## Why the override travels as a data pack

`WorldgenPackRegistry` registers the override through NeoForge's `AddPackFindersEvent` with
`PackSource.DEFAULT` and `alwaysActive = false`: it is enabled by default, appears in the Data Packs
screen as **“Abyssal Overworld (832 blocks tall)”**, and can be switched off again. An override this
invasive should be visible and reversible instead of hiding in `data/minecraft/...`.

Packs registered that way are auto-added to the end of the selection list, and
`FallbackResourceManager` resolves the *last* pack first, while `reorderNewlyDiscoveredPacks` only
moves `BOTTOM`-position packs — so this pack wins over `vanilla` and `mod_data`.

Disabling it *before* a world has generated terrain below Y=-64 restores vanilla height. Disabling
it afterwards deletes that range on the next save — the pack's own description says so.

Aquanaut's water world does **not** depend on that pack: its preset points at
`aquanaut:abyssal_overworld`, which always ships with the mod.

## How the Y extension is expressed

`OceanLayerStack.deepenedFor(minBuildHeight)` (used through `OceanLayerStacks.activeFor`) touches a
single band:

```
abyssHeight = height of the band above the deepest layer   // middle sea: 77 blocks
newMinY     = max(minBuildHeight, abyss.maxY() - abyssHeight + 1)
```

* At the vanilla floor (`minBuildHeight = -64`) the world floor clamps the band back to its authored
  `-64 … -38`, and the stack itself is returned — identity, so nothing changes for existing worlds.
* At the abyssal floor the band becomes `-114 … -38`: **77 blocks, exactly the middle sea's height.**
* Everything below `-115` is claimed by no band at all, so `BiomeRewriter` leaves those cells alone
  (they keep the noise biome source's parent biome) — the reserved area for future content.

The terrain pipeline is untouched: `MiddleLevelOceanTerrainProfile`, the planner and the shading
still produce the authored chamber (cavity floor ≈ -30, reef slab 9–17 blocks). The one
bottom-anchored quantity is the **abyssal floor**, which follows the new world floor and is a *flat
sediment plain* at `min_y + 4` — it rises to the reef underside only inside the region's edge fade
(`region_edge_fade_blocks`, 16 blocks), so the basin closes against the surrounding crust without
filling the deep sea with relief. The plain is driven by the region-edge field alone; it must never
be tied to `chamberWallFade`, the broad noise that shoals the middle sea, or the abyss turns back
into a mountain range as soon as a world is hundreds of blocks deeper.

Resulting envelope at `-512`:

| Band / quantity | Reference (-64) | Abyssal (-512) |
|---|---|---|
| surface ocean | 40 … 320 | unchanged |
| reef ceiling | 35 … 39 | unchanged |
| middle sea | -42 … 34 (77) | **unchanged** |
| deep sea (abyss band) | -64 … -38 (27) | **-114 … -38 (77)** |
| below the abyss band | — | **unclaimed (reserved)** |
| chamber floor / reef slab | -22 … -37 / 9–17 thick | unchanged |
| abyssal floor | -60, flat | **-508, flat** (+ a rise only at the region border) |

Consequences worth knowing:

* The area below `Y=-115` is water with the parent deep-ocean biome over a flat, featureless plain
  and no mod content. Future abyssal biomes/terrain go there by adding layers or bands (pure data)
  plus their terrain modules — the even floor is what keeps that work free to lay out its own relief.
* Vanilla ore/feature placements are absolutely anchored, so the deep volume is mineral-poor; the
  mod's own geology fills the middle sea exactly as before.
* Mob spawning draws a uniform Y over the taller column, diluting attempts in vanilla land worlds.

## Existing worlds

The engine has no runtime height migration. New chunks in an old save generate the full depth; chunks
generated earlier keep their 24 sections and stay empty (air, plains-biome defaults) below `-64`,
with a hard seam at old chunk borders. `AbyssHeightNotice` logs this once per level, and the startup
line reports the envelope the running overworld actually has:

```
Overworld envelope is abyssal: y=-512..319 (832 blocks).
```

A retro-fill (planning the missing range analytically per loaded chunk, writing blocks + biomes,
updating heightmaps and light) is possible — `OceanColumnPlanner.planColumnAt` is the
chunk-independent reference path — but it cannot place features and is deliberately out of scope.

## Files

* `common/worldgen/layers/OceanLayerStack.java` — the band extension (`deepenedFor`).
* `common/worldgen/layers/OceanLayerStacks.java` — `activeFor(minBuildHeight)` plus its cache.
* `common/worldgen/layers/OceanColumnPlanner.java` — the flat abyssal plain (`deepFloorY`), driven by
  `PlanSource.regionEdge` (region border) rather than `columnEdge` (border + chamber-wall noise).
* `common/worldgen/MiddleLevelOceanColumnRules.java` — world-floor aware façade overloads.
* `mixin/NoiseBasedChunkGeneratorMixin.java` — resolves the stack for the chunk's floor.
* `common/worldgen/AbyssHeightNotice.java` — startup envelope report + legacy-chunk warning.
* `core/WorldgenPackRegistry.java` — the pack registration.
* Tests: `DeepSeaBandExtensionTest` (band geometry, reserved area), `AbyssEnvelopeJsonTest`
  (shipped dimension types, noise settings, preset wiring, pack contents, language keys), and the
  `-512` cases in `TerrainBlendContinuityTest` (chamber comes out identical, plain stays flat, the
  plain closes only at the region border).

## Follow-ups

1. Populate the reserved area below the abyss band: new layers/bands and their terrain modules.
2. Deep-noise rework for land columns (they are a solid unshaped crust below `-64`).
3. Optional retro-fill for legacy chunks, and a depth choice on the create-world screen (the
   `PresetEditor` + `WorldDimensions.withOverworld` hook is available).
4. Vanilla's underground ruined portals anchor their search to `level.getMinBuildHeight() + 15`,
   which now reaches deep into that crust; a targeted fix is worth considering.
