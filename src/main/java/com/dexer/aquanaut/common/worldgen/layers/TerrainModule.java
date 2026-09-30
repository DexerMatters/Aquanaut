package com.dexer.aquanaut.common.worldgen.layers;

import com.dexer.aquanaut.common.worldgen.blend.BoundaryWarp;
import com.dexer.aquanaut.common.worldgen.blend.CliffGuard;
import com.dexer.aquanaut.common.worldgen.blend.ContactMaterial;
import com.dexer.aquanaut.common.worldgen.blend.DissolutionField;
import com.dexer.aquanaut.common.worldgen.blend.EmergenceCurve;
import com.dexer.aquanaut.common.worldgen.blend.HeightCombiner;

/**
 * Parameterized cap/cavity geometry for a carved lower sea.
 * Defaults match the legacy middle-ocean chamber.
 *
 * <p>
 * {@code crackCellSize} is the coarse cell of the rotated crack field: larger values make broader,
 * fewer openings through the cap, and the field's second octave is derived from it, so this one
 * knob scales the whole crack pattern.
 *
 * <p>
 * {@code blend} carries the tunables of the generic terrain-mix strategies (dissolution karst,
 * boundary warp of fissure fields, feature emergence windows, cliff guard, contact dither and the
 * relief combiner). All of them have production defaults; datapacks may override any subset through
 * the optional {@code blend} section of the ocean-terrain JSON.
 * </p>
 */
public record TerrainModule(
        int capTopMinY,
        int capTopMaxY,
        int minCapThickness,
        int capThicknessVariants,
        int minCavityDepth,
        int cavityDepthVariants,
        int minFloorMargin,
        double pillarChance,
        double pillarConnectedChance,
        double pillarHeightMinRatio,
        double pillarHeightMaxRatio,
        int pillarBaseExtra,
        int crackCellSize,
        double crackThreshold,
        double crackDetailThreshold,
        double crackOpenEdge,
        double pillarEdge,
        double coralTreeEdge,
        double coralTreeChance,
        int topWaterY,
        double wallIntrusion,
        int wallCellSize,
        BlendProfile blend
) {
    public TerrainModule {
        if (blend == null) {
            blend = BlendProfile.defaults();
        }
    }

    /** Legacy constructor kept for existing call sites: production blend defaults. */
    public TerrainModule(int capTopMinY, int capTopMaxY, int minCapThickness, int capThicknessVariants,
                         int minCavityDepth, int cavityDepthVariants, int minFloorMargin,
                         double pillarChance, double pillarConnectedChance,
                         double pillarHeightMinRatio, double pillarHeightMaxRatio, int pillarBaseExtra,
                         int crackCellSize, double crackThreshold, double crackDetailThreshold,
                         double crackOpenEdge, double pillarEdge, double coralTreeEdge,
                         double coralTreeChance, int topWaterY, double wallIntrusion, int wallCellSize) {
        this(capTopMinY, capTopMaxY, minCapThickness, capThicknessVariants, minCavityDepth,
                cavityDepthVariants, minFloorMargin, pillarChance, pillarConnectedChance,
                pillarHeightMinRatio, pillarHeightMaxRatio, pillarBaseExtra, crackCellSize,
                crackThreshold, crackDetailThreshold, crackOpenEdge, pillarEdge, coralTreeEdge,
                coralTreeChance, topWaterY, wallIntrusion, wallCellSize, BlendProfile.defaults());
    }

    public static TerrainModule reefCap() {
        return new TerrainModule(35, 39, 4, 5, 58, 9, 12,
                0.05D, 0.20D, 0.20D, 0.40D, 4,
                80, 0.40D, 0.32D, 0.32D, 0.45D, 0.58D, 0.06D,
                62, 0.15D, 64, BlendProfile.defaults());
    }

    public static TerrainModule middleCavity() {
        return reefCap();
    }

    public static TerrainModule none() {
        return new TerrainModule(0, -1, 0, 0, 0, 0, 0,
                0.0D, 0.0D, 0.0D, 0.0D, 0,
                80, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                62, 0.0D, 64, BlendProfile.defaults());
    }

    public boolean enabled() {
        return capTopMaxY >= capTopMinY && minCavityDepth > 0;
    }

    /**
     * Tunables of the generic terrain-mix strategies. Every accessor materialises a small
     * immutable strategy object; callers hold one instance per chunk build.
     */
    public record BlendProfile(
            double dissolutionGain,
            double dissolutionModulation,
            double dissolutionCut,
            double dissolutionWidth,
            double warpAmplitudeBlocks,
            double warpBroadCell,
            double warpFineCell,
            double emergenceEdificeFull,
            double emergenceSatelliteFrom,
            double emergenceSatelliteFull,
            double softMaxP,
            double slopeMaxStep,
            double slopeMaxDeviation,
            int slopeReach,
            int ditherCell,
            double ditherGamma,
            double crackOpenWidth
    ) {
        /** Seed of the fissure/karst boundary warp ("KARST"). */
        private static final long FIELD_WARP_SEED = 0xCA45717EL;

        public static BlendProfile defaults() {
            return new BlendProfile(
                    1.25D, 0.45D, 0.72D, 0.22D,
                    6.0D, 41.0D, 13.0D,
                    0.45D, 0.05D, 0.5D,
                    4.0D,
                    1.0D, 2.0D, 12,
                    2, 2.0D,
                    0.25D);
        }

        /** Karst dissolution behind every void: open reef, brine fissures, cap shafts. */
        public DissolutionField dissolution() {
            return new DissolutionField.Karst(dissolutionGain, dissolutionModulation,
                    dissolutionCut, dissolutionWidth);
        }

        /** Domain warp of the fissure/karst noise coordinates (blocks units). */
        public BoundaryWarp fieldWarp() {
            return new BoundaryWarp.Fbm(warpAmplitudeBlocks, warpBroadCell, warpFineCell, FIELD_WARP_SEED);
        }

        /** Emergence window of the giant edifices: relief grows from zero with the weight. */
        public EmergenceCurve edificeEmergence() {
            return new EmergenceCurve.SmoothstepWindow(0.0D, emergenceEdificeFull);
        }

        /** Emergence window of satellite vents and small dressings. */
        public EmergenceCurve satelliteEmergence() {
            return new EmergenceCurve.SmoothstepWindow(emergenceSatelliteFrom, emergenceSatelliteFull);
        }

        /** Anti-cliff guard over the composed floor grid. */
        public CliffGuard cliffGuard() {
            return new CliffGuard.ConeErosion(slopeMaxStep, slopeMaxDeviation, slopeReach);
        }

        /** Weighted hash dither of rock families across district contacts. */
        public ContactMaterial contactMaterial() {
            return new ContactMaterial.HashDither(ditherCell, ditherGamma);
        }

        /** Smoothed-max composition of overlapping district relief. */
        public HeightCombiner reliefCombiner() {
            return HeightCombiner.softMax(softMaxP);
        }

        /** Base floors and slab thickness compose by weighted mean. */
        public HeightCombiner baseCombiner() {
            return HeightCombiner.LINEAR;
        }
    }
}
