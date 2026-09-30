package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.blend.BoundaryWarp;
import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;
import com.dexer.aquanaut.common.worldgen.layers.TerrainModule;

public final class MiddleLevelOceanTerrainProfile {
    private static final long WALL_SEED = 0xDEADBEEFL;
    // Crack openings: two rotated octaves of the same value-noise family, sampled through a
    // two-octave domain warp. The rotation and the warp are what keep the outlines irregular —
    // a single axis-aligned grid reads as a field of squares, and an unwarped threshold reads
    // as a drawn contour.
    private static final long CRACK_OUTLINE_SEED = 0x9E3779B9L;
    private static final long CRACK_DETAIL_SEED = 0x7F4A7C15L;
    private static final double CRACK_ROTATION = 0.62D;
    private static final double CRACK_DETAIL_ROTATION = 1.67D;
    private static final double CRACK_DETAIL_RATIO = 1.0D / Math.sqrt(2.0D);
    private static final double CRACK_DETAIL_WEIGHT = 0.38D;
    // Second (finer) outline field, as a fraction of the coarse crack cell size.
    private static final double CRACK_DETAIL_CELL_SCALE = 0.30D;

    private MiddleLevelOceanTerrainProfile() {
    }

    public static double chamberWallFade(int blockX, int blockZ) {
        return chamberWallFade(blockX, blockZ, TerrainModule.reefCap());
    }

    /**
     * Broad chamber outline only (large cells). Must stay smooth so floor height
     * does not inherit high-frequency wall detail.
     */
    public static double chamberWallFade(int blockX, int blockZ, TerrainModule terrain) {
        double wallNoise = sample(blockX, blockZ, terrain.wallCellSize(), WALL_SEED);
        double wallMid = sample(blockX, blockZ, 48, WALL_SEED ^ 0x12345678L);
        double combined = wallNoise * 0.75D + wallMid * 0.25D;
        double fade = (combined - terrain.wallIntrusion()) / (1.0D - terrain.wallIntrusion());
        return SoftMixNoise.smoothstep(fade);
    }

    public static ColumnProfile profileFor(int blockX, int blockZ, int minBuildHeight) {
        return profileFor(blockX, blockZ, minBuildHeight, TerrainModule.reefCap());
    }

    public static ColumnProfile profileFor(int blockX, int blockZ, int minBuildHeight, TerrainModule terrain) {
        return profileFor(blockX, blockZ, minBuildHeight, terrain, OutcropGeometry::outcropAt);
    }

    /** Source-injected variant so a chunk build can memoize the massif cell records. */
    public static ColumnProfile profileFor(int blockX, int blockZ, int minBuildHeight, TerrainModule terrain,
                                           com.dexer.aquanaut.common.worldgen.blend.CellSource<OutcropGeometry.Outcrop> outcrops) {
        // Cap top: broad undulation only (no 10-block detail).
        double capTopNoise = sample(blockX, blockZ, 36, 0x5F3759DFL);
        double capTopMid = sample(blockX, blockZ, 18, 0x5F3759DFL ^ 0xABCDEF01L);
        double capTopBlend = capTopNoise * 0.8D + capTopMid * 0.2D;
        int capRange = (terrain.capTopMaxY() - terrain.capTopMinY()) + 1;
        int capTopY = terrain.capTopMinY() + floor(capTopBlend * capRange);

        double capThicknessNoise = sample(blockX, blockZ, 28, 0x6A09E667L);
        int capThickness = terrain.minCapThickness() + floor(capThicknessNoise * terrain.capThicknessVariants());
        int layerFloor = terrain.capTopMinY() - 7;
        int capBottomY = Math.max(layerFloor + 1, capTopY - capThickness + 1);

        // Basin depth: large-scale only so the lower sea floor stays broad and gentle.
        double basinNoise = sample(blockX, blockZ, 80, 0x243F6A88L);
        double basinMid = sample(blockX, blockZ, 40, 0xB7E15162L);
        double cavityBlend = basinNoise * 0.8D + basinMid * 0.2D;
        int cavityDepth = terrain.minCavityDepth() + floor(cavityBlend * terrain.cavityDepthVariants());
        int cavityFloorY = Math.max(minBuildHeight + terrain.minFloorMargin(), capBottomY - cavityDepth);

        // Continuous shaft openness through the cap: the product of two warped, rotated
        // ramp fields. The cap thins into a lens as openness grows and vanishes at 1, so
        // shafts are funnel-shaped sinkholes with sloped walls — never vertical slots.
        double capOpenness = capOpenness(blockX, blockZ, terrain);

        // Low sedimentary massifs rise from the floor: mesas, ridges, mounds and spires
        // with their own silhouettes, all well below the reef overhead. Their relief scales
        // with the remaining cap solidity so nothing towers under an open shaft and no
        // massif ends on the crack contour line.
        double outcropRelief = OutcropGeometry.reliefAt(blockX, blockZ,
                capBottomY - cavityFloorY,
                mountainTopLimit(capBottomY, cavityFloorY) - cavityFloorY,
                outcrops)
                * (1.0D - capOpenness);
        int outcropTopY = outcropRelief > 0.05D
                ? cavityFloorY + (int) Math.round(outcropRelief)
                : Integer.MIN_VALUE;

        return new ColumnProfile(capTopY, capBottomY, cavityFloorY, capOpenness,
                outcropRelief, outcropTopY);
    }

    /**
     * Continuous openness of the reef cap at a column, in [0, 1]: 0 = sealed cap,
     * 1 = a wide-open shaft connecting the two seas. Both underlying fields are rotated
     * off the block grid and sampled through the module's boundary warp, and each enters
     * through a smoothstep ramp of width {@code crack_open_width} instead of a hard
     * threshold, so the opening tapers in over several blocks and its outline meanders
     * and branches at two scales.
     */
    public static double capOpenness(int blockX, int blockZ, TerrainModule terrain) {
        double[] warped = terrain.blend().fieldWarp().warp(blockX, blockZ, new double[2]);
        double width = Math.max(1e-3, terrain.blend().crackOpenWidth());
        double broad = crackField(warped[0], warped[1], terrain.crackCellSize(), CRACK_OUTLINE_SEED, 1.0D - CRACK_DETAIL_WEIGHT);
        double detail = crackField(warped[0], warped[1],
                Math.max(2.0D, terrain.crackCellSize() * CRACK_DETAIL_CELL_SCALE),
                CRACK_DETAIL_SEED, CRACK_DETAIL_WEIGHT);
        double broadRamp = SoftMixNoise.smoothstep((broad - terrain.crackThreshold()) / width);
        double detailRamp = SoftMixNoise.smoothstep((detail - terrain.crackDetailThreshold()) / width);
        return broadRamp * detailRamp;
    }

    /**
     * The highest Y any middle-sea relief may reach. Mountains, pillars and cones top out
     * at 32% of the chamber height — low hills and seamounts, with a very wide band of
     * open water between their summits and the reef overhead.
     */
    public static int mountainTopLimit(int capBottomY, int cavityFloorY) {
        return cavityFloorY + (int) Math.round((capBottomY - cavityFloorY) * 0.32D);
    }

    /**
     * One octave of the crack outline field: rotated off the block grid and shifted by
     * {@code weight} toward the finer detail octave, so straight axis-aligned borders break up.
     */
    private static double crackField(double blockX, double blockZ, double cellSize, long seed,
                                     double detailWeight) {
        double cos = Math.cos(CRACK_ROTATION);
        double sin = Math.sin(CRACK_ROTATION);
        double broad = sample(
                blockX * cos - blockZ * sin,
                blockX * sin + blockZ * cos,
                cellSize, seed);
        double detailCos = Math.cos(CRACK_DETAIL_ROTATION);
        double detailSin = Math.sin(CRACK_DETAIL_ROTATION);
        double detail = sample(
                blockX * detailCos - blockZ * detailSin,
                blockX * detailSin + blockZ * detailCos,
                cellSize * CRACK_DETAIL_RATIO, seed ^ 0x51L);
        return broad * (1.0D - detailWeight) + detail * detailWeight;
    }

    private static double sample(int blockX, int blockZ, int cellSize, long seed) {
        return sample((double) blockX, (double) blockZ, (double) cellSize, seed);
    }

    /**
     * Continuous (non-integer-cell) sample of the value-noise family, used where the crack
     * field is rotated and warped and therefore lands between grid cells. Lattice values are
     * mapped into [0, 1].
     */
    private static double sample(double blockX, double blockZ, double cellSize, long seed) {
        double noise = SoftMixNoise.valueNoise(blockX, blockZ, cellSize, seed);
        return (noise + 1.0D) * 0.5D;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    /**
     * The rock band surviving dissolution of a cap [{@code capBottomY}, {@code capTopY}]:
     * the band thins symmetrically from both faces as openness grows (a lens), so shafts
     * are funnel-shaped sinkholes with sloped walls instead of vertical slots, and the
     * band disappears entirely once less than one block of cap remains.
     */
    public record LensBand(int topY, int bottomY) {
        public boolean sealed() {
            return bottomY <= topY;
        }
    }

    public static LensBand lensBand(int capTopY, int capBottomY, double openness) {
        int thickness = capTopY - capBottomY + 1;
        double effective = thickness * (1.0D - SoftMixNoise.clamp01(openness));
        if (effective < 1.0D) {
            return new LensBand(capBottomY - 1, capBottomY);
        }
        int removed = thickness - (int) Math.floor(effective);
        int top = capTopY - removed / 2;
        return new LensBand(top, top - (int) Math.ceil(effective) + 1);
    }

    /**
     * One column of the cap/cavity story. The cap is a lens: {@link #capOpenness()} thins it
     * symmetrically from both faces until it vanishes, so the effective band
     * [{@link #effCapBottomY()}, {@link #effCapTopY()}] is what the planner actually fills.
     */
    public record ColumnProfile(int capTopY, int capBottomY, int cavityFloorY, double capOpenness,
                                double outcropRelief, int outcropTopY) {
        public int capThickness() {
            return capTopY - capBottomY + 1;
        }

        /** Effective (post-dissolution) cap thickness in blocks; 0 once the shaft is open. */
        public double effectiveThickness() {
            return capThickness() * (1.0D - capOpenness);
        }

        public LensBand lens() {
            return lensBand(capTopY, capBottomY, capOpenness);
        }

        /** Top of the rock band actually present. */
        public int effCapTopY() {
            return lens().topY();
        }

        /** Bottom of the rock band actually present; above {@link #effCapTopY()} when open. */
        public int effCapBottomY() {
            return lens().bottomY();
        }

        /** Whether any cap rock survives at this column at all. */
        public boolean capSealed() {
            return lens().sealed();
        }

        public int cavityHeight() {
            return capBottomY - cavityFloorY;
        }

        /** Legacy boolean view of the shaft: open once more than half dissolved. */
        public boolean crack() {
            return capOpenness >= 0.5D;
        }

        /** Whether a sedimentary massif rises from this column at all. */
        public boolean hasOutcrop() {
            return outcropTopY != Integer.MIN_VALUE;
        }
    }
}
