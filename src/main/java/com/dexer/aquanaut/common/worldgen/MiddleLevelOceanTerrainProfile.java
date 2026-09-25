package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;
import com.dexer.aquanaut.common.worldgen.layers.TerrainModule;

public final class MiddleLevelOceanTerrainProfile {
    private static final long WALL_SEED = 0xDEADBEEFL;
    // Crack openings: two rotated octaves of the same value-noise family. The rotation is what
    // keeps the outlines irregular — a single axis-aligned grid reads as a field of squares.
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

        // Broad outline plus a finer irregularity field, both rotated off the block grid.
        boolean crack = crackField(blockX, blockZ, terrain.crackCellSize(), CRACK_OUTLINE_SEED,
                terrain.crackThreshold())
                && crackField(blockX, blockZ,
                        Math.max(2.0D, terrain.crackCellSize() * CRACK_DETAIL_CELL_SCALE),
                        CRACK_DETAIL_SEED, terrain.crackDetailThreshold());

        // Low sedimentary massifs rise from the floor: mesas, ridges, mounds and spires
        // with their own silhouettes, all well below the reef overhead.
        int outcropTopY = Integer.MIN_VALUE;
        if (!crack) {
            outcropTopY = OutcropGeometry.topYAt(blockX, blockZ, cavityFloorY,
                    capBottomY - cavityFloorY, mountainTopLimit(capBottomY, cavityFloorY));
        }

        return new ColumnProfile(capTopY, capBottomY, cavityFloorY, crack, outcropTopY);
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
     * True where a rotated two-octave field exceeds {@code threshold}. Rotating each octave and
     * shifting the detail octave by {@link #CRACK_DETAIL_WEIGHT} breaks up the straight,
     * axis-aligned borders a single grid-aligned sample produces.
     */
    private static boolean crackField(int blockX, int blockZ, double cellSize, long seed,
                                      double threshold) {
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
        return broad * (1.0D - CRACK_DETAIL_WEIGHT) + detail * CRACK_DETAIL_WEIGHT > threshold;
    }

    private static double sample(int blockX, int blockZ, int cellSize, long seed) {
        int cellX = Math.floorDiv(blockX, cellSize);
        int cellZ = Math.floorDiv(blockZ, cellSize);
        double localX = (double) Math.floorMod(blockX, cellSize) / cellSize;
        double localZ = (double) Math.floorMod(blockZ, cellSize) / cellSize;
        double smoothX = SoftMixNoise.smoothstep(localX);
        double smoothZ = SoftMixNoise.smoothstep(localZ);

        double sample00 = unitHash(cellX, cellZ, seed);
        double sample10 = unitHash(cellX + 1, cellZ, seed);
        double sample01 = unitHash(cellX, cellZ + 1, seed);
        double sample11 = unitHash(cellX + 1, cellZ + 1, seed);
        double lerpX0 = SoftMixNoise.lerp(smoothX, sample00, sample10);
        double lerpX1 = SoftMixNoise.lerp(smoothX, sample01, sample11);
        return SoftMixNoise.lerp(smoothZ, lerpX0, lerpX1);
    }

    /**
     * Continuous (non-integer-cell) sample of the same value-noise family, used where the crack
     * field is rotated and therefore lands between grid cells.
     */
    private static double sample(double blockX, double blockZ, double cellSize, long seed) {
        double cellX = blockX / cellSize;
        double cellZ = blockZ / cellSize;
        int baseX = (int) Math.floor(cellX);
        int baseZ = (int) Math.floor(cellZ);
        double smoothX = SoftMixNoise.smoothstep(cellX - baseX);
        double smoothZ = SoftMixNoise.smoothstep(cellZ - baseZ);

        double sample00 = unitHash(baseX, baseZ, seed);
        double sample10 = unitHash(baseX + 1, baseZ, seed);
        double sample01 = unitHash(baseX, baseZ + 1, seed);
        double sample11 = unitHash(baseX + 1, baseZ + 1, seed);
        double lerpX0 = SoftMixNoise.lerp(smoothX, sample00, sample10);
        double lerpX1 = SoftMixNoise.lerp(smoothX, sample01, sample11);
        return SoftMixNoise.lerp(smoothZ, lerpX0, lerpX1);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double unitHash(int x, int z, long seed) {
        long mixed = hash(x, z, seed);
        return ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    private static long hash(int x, int z, long seed) {
        return mix(seed ^ ((long) x * 0x632BE59BD9B4E019L) ^ ((long) z * 0x9E3779B97F4A7C15L));
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public record ColumnProfile(int capTopY, int capBottomY, int cavityFloorY, boolean crack,
                                int outcropTopY) {
        public int capThickness() {
            return capTopY - capBottomY + 1;
        }

        public int cavityHeight() {
            return capBottomY - cavityFloorY;
        }

        /** Whether a sedimentary massif rises from this column at all. */
        public boolean hasOutcrop() {
            return outcropTopY != Integer.MIN_VALUE;
        }
    }
}
