# Shader-aware fog authority

Follow-up to the user's question: *"is there a way to make shader render the biome fog and
light color? a general way?"*

## Findings (verified, not assumed)

Read out of the jars and sources actually in this workspace: Iris
`iris-neoforge-1.8.12+mc1.21.1` and the Minecraft 1.21.1 sources in
`build/moddev/artifacts/neoforge-21.1.233-sources.jar`.

1. **The mod's fog already reaches a shader pack.** Iris's `FogUniforms` fills `fogColor`
   from `RenderSystem.getShaderFogColor()` and `fogStart`/`fogEnd`/`fogShape`/`fogDensity`/
   `fogMode` from the same fog state. `MixinFogRenderer` injects `setupFog` (HEAD) and
   `setupColor` (TAIL) — it *captures* the colour, it never cancels vanilla fog. So
   `FogRenderer` runs, so NeoForge's `ViewportEvent.ComputeFogColor`/`RenderFog` (our
   `ClientFogEvents`) run, so anything reading those uniforms follows the biome and the
   abyss for free.
2. Vanilla's sources of those colours: `FogRenderer.setupColor` uses
   `biome.getWaterFogColor()` while submerged, otherwise `getSkyColor` blended with
   `BiomeManager.getNoiseBiomeAtQuart(...).getFogColor()`; then `ClientHooks.getFogColor`
   (our event); `setupFog` writes `RenderSystem.setShaderFog*`.
3. **Custom fluids have no fog at all.** `Camera.getFluidInCamera()` only knows
   WATER/LAVA/POWDER_SNOW, so sulfuric acid reports `FogType.NONE`. Entering an acid lake
   showed plain water/biome fog.
4. **There is no per-biome light colour in 1.21.1.** `LightTexture` uses sky darken, night
   vision, the darkness effect and water vision only; `ViewportEvent` has no lightmap hook
   (only `RenderFog`, `ComputeFogColor`, `ComputeCameraAngles`, `ComputeFov`).
5. **No mod-facing fog API exists.** `IrisApi` v0 offers `isShaderPackInUse()`,
   `isRenderingShadowPass()`, `getSunPathRotation()`, config and the settings screen — no fog
   setters. The `biome` uniform exists, but `MixinBiomes` assigns its ids **by registration
   order**, so a pack cannot reliably key on our biomes by number.
6. **A pack-proof drawing slot exists.** `RenderGuiEvent.Pre` fires after the world render —
   and therefore after Iris composites its frame at the end of `LevelRenderer#renderLevel` —
   and before the HUD. The drone feed's picture is drawn later, as HUD, so a veil drawn here
   cannot contaminate it.

## What this means

- Packs that read vanilla fog: **already work**, and now also get acid fog and per-ocean
  visibility.
- Packs that hard-code their own atmosphere: no mod can reach inside their shaders. The only
  general answer is to draw over the finished frame, which is what the veil does.
- Per-biome keys for pack authors are **not** a dependable channel (finding 5).

## Design — one authority, three consumers

`ClientFogState` resolves, once per camera per frame: the medium (acid / water / air), the
colour (the world's own, unless the medium has none), the near/far planes, the depth ramp and
the veil opacity. Three consumers read that one decision:

1. **The world's fog state** (`ClientFogEvents` → `ComputeFogColor`, `RenderFog`) — this is
   what a shader pack sees.
2. **The fallback veil** (`FogVeilRenderer`), drawn from `RenderGuiEvent.Pre` when Iris
   reports a pack in use, so the biome cast and the abyss read whatever the pack does.
3. **The vanilla lightmap** (`LightTextureMixin` → `AbyssLightmap`), scaled by the same depth
   ramp so the abyss takes the light and not merely the distance — only when no pack is
   driving the frame, since a pack owns the lightmap.

Colour has exactly one owner: while submerged the authoritative colour is resolved by the
authority itself — a blend of the oceans around the camera — and handed to the world's fog
state unchanged. `FogMath.fogRgb` is what both the fog state and the veil call, so they cannot
disagree. Only media the world cannot describe — acid — bring a colour of their own
(`FogTable`, from the shipped `assets/aquanaut/fog_profile.json`).

## Nothing changes in a single frame

The first cut of this authority resolved the biome under the camera and applied it, which made
every boundary a step: cross into another ocean and the colour and the visible distance flipped
in one frame. Three mechanisms now prevent that, and they answer the three transitions that
were raised (swimming between oceans, the biome colour changing around you, and descending
between the vertical layers):

1. **Spatial blending** (`FogField`). The fog is sampled as a field: a three-dimensional cross
   of seven samples around the camera, weighted by distance, each resolved through the chunk's
   own (post-rewrite) biome. A boundary between two oceans is therefore a gradient tens of
   blocks wide, vertically as well as horizontally. A camera well inside one ocean still
   resolves to exactly that ocean's profile and colour.
2. **Temporal easing** (`ClientFogState`). Colour, planes and veil opacity each ease toward
   their target with a time constant (0.30–0.35 s), frame-rate independently
   (`FogMath.ease`), which covers what a spatial gradient cannot: a genuinely discrete change
   such as swimming into acid.
3. **Submersion crossfade.** `submersion` eases between 0 and 1, and everything the authority
   applies is mixed by it: entering a medium fades its tint in, and surfacing hands the world's
   own fog colour *and* the world's own fog distances back gradually
   (`FogMath.lerp(submersion, vanillaPlane, ourPlane)`), so the view opens up instead of
   jumping.

Vanilla's own biome-colour easing is bypassed rather than doubled: `ComputeFogColor` runs
*after* `FogRenderer`'s internal blend and our value replaces it, so there is exactly one fade
and it is the one described above.

## Files

Added:

- `common/fog/FogMedium`, `FogVisibility`, `FogMediumProfile`, `FogMath`, `FogTable`,
  `FogProfiles` — the model, the ramps and the blends, free of Minecraft so every one is
  unit-testable.
- `client/fog/ClientFogState` (the decision and the easing), `FogField` (the spatial blend),
  `FogVeilRenderer` (the veil), `IrisCompat` (reflective detection, no Iris dependency),
  `FogClientConfig` (client config), `AbyssLightmap` (lightmap scale).
- `core/TagRegistry` (`aquanaut:sulfuric_acid` fluid tag) and
  `data/aquanaut/tags/fluid/sulfuric_acid.json` — what makes acid a medium, and lets another
  mod's corrosive liquid opt in without further wiring.
- `assets/aquanaut/fog_profile.json` — per-ocean visibility (`near`, `far`, `cast`) and the
  acid's colour. Client-side by design: a resource pack can retune it.
- `mixin/LightTextureMixin` (registered in `aquanaut.mixins.json`).
- `FogMathTest`, `FogTableTest` — 19 tests.

Changed: `ClientFogEvents` (medium-aware, field-driven, crossfaded), `ClientModEvents` (reload
listener), `Aquanaut` (client config registration), `aquanaut.mixins.json`.

## Config — `config/aquanaut-client.toml`

| Option | Default | Meaning |
| --- | --- | --- |
| `shaderFogFallback` | `true` | Draw the veil while a shader pack is in use |
| `shaderFogCastStrength` | `0.75` | Veil strength, 0 disables it; lower it if your pack also reads vanilla fog |
| `abyssLightmapDarkening` | `0.6` | How far the abyss dims the lightmap without shaders |

The veil is bounded at `FogMath.MAX_VEIL_ALPHA` (0.55), so a pack that *also* honours the fog
state we hand it can never be pushed into double darkness.

## For shader pack authors

- The mod writes the vanilla fog state only: `fogColor`, `fogStart`, `fogEnd`, `fogShape`,
  `fogDensity`. A pack that uses them follows the biome automatically.
- Acid is not a `FogType`: inside acid the world's fog colour is `FogType.NONE` and the
  state carries the acid's colour and its tight planes.
- A pack cannot identify our biomes by number; if you want per-ocean behaviour, read the
  shipped `assets/aquanaut/fog_profile.json` and match on whatever your pack can see.

## Verification

- `./gradlew test`: **223 tests, only the 6 pre-existing notebook failures** (was 204 before
  this work; 19 of the new ones cover the fog model, the blends and the easing).
- `compileJava`/`compileTestJava` clean; `runData` loads registries with 0 errors.
- **A dev client was launched, three times.** It confirmed what unit tests cannot: both
  `LightTexture` injections apply (`Mixing LightTextureMixin ... into
  net.minecraft.client.renderer.LightTexture`), `FogVeilRenderer` subscribes, the client config
  is written to `run/config/aquanaut-client.toml`, and — after fixing the loader — the table
  itself is read: `Fog profile loaded from 1 pack resource(s); 5 biomes declared`. No
  exceptions from any fog class.
- **A first cut of the loader silently found nothing** (`listResources("")` returned an empty
  map) and fell back to the built-in defaults, which is invisible in game and would have made
  the shipped data file a lie. It now loads through `getResourceStack` on a known resource id
  and logs the result, precisely so that cannot happen unnoticed again.
- **Still needs your eyes:** whether the pacing feels right. The knobs are
  `FogField.OFFSETS`/`FALLOFF` (how wide a boundary's gradient is), the time constants at the
  top of `ClientFogState` (0.25–0.35 s), `castStrength` and `abyssLightmapDarkening` in the
  client config, and `near`/`far`/`cast` per ocean in `fog_profile.json`. `/f3` prints the
  authority's state, including the crossfade weight:
  `Fog: water depth=0.42 in=100% near=-4.0 far=31.2 shaders=yes veil=on at 75%`.

## Honest limitation

The veil is a screen-space colour cast, not volumetric fog, and it is deliberately bounded.
It guarantees that biome fog and abyss darkness *read* under any pack; it cannot make a pack
that hard-codes noon-blue atmosphere render our biome's colour inside its own atmospheric
model. Nothing can, short of that pack doing it itself.
