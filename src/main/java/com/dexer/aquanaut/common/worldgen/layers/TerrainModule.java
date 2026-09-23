package com.dexer.aquanaut.common.worldgen.layers;

/**
 * Parameterized cap/cavity geometry for a carved lower sea.
 * Defaults match the legacy middle-ocean chamber.
 *
 * <p>
 * {@code crackCellSize} is the coarse cell of the rotated crack field: larger values make broader,
 * fewer openings through the cap, and the field's second octave is derived from it, so this one
 * knob scales the whole crack pattern.
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
        int wallCellSize
) {
    public static TerrainModule reefCap() {
        return new TerrainModule(35, 39, 4, 5, 58, 9, 12,
                0.05D, 0.20D, 0.20D, 0.40D, 4,
                80, 0.56D, 0.44D, 0.32D, 0.45D, 0.58D, 0.06D,
                62, 0.15D, 64);
    }

    public static TerrainModule middleCavity() {
        return reefCap();
    }

    public static TerrainModule none() {
        return new TerrainModule(0, -1, 0, 0, 0, 0, 0,
                0.0D, 0.0D, 0.0D, 0.0D, 0,
                80, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                62, 0.0D, 64);
    }

    public boolean enabled() {
        return capTopMaxY >= capTopMinY && minCavityDepth > 0;
    }
}
