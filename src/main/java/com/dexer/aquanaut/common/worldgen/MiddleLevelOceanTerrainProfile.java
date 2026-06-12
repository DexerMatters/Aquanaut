package com.dexer.aquanaut.common.worldgen;

public final class MiddleLevelOceanTerrainProfile {
    private static final int CAP_TOP_MIN_Y = CoralForestPlacement.layerStartBlockY() - 1;
    private static final int CAP_TOP_MAX_Y = CoralForestPlacement.layerStartBlockY() + 3;
    private static final int MIN_CAP_THICKNESS = 4;
    private static final int CAP_THICKNESS_VARIANTS = 5;
    private static final double PILLAR_CHANCE = 0.05D;
    private static final double PILLAR_CONNECTED_CHANCE = 0.20D;
    private static final double PILLAR_HEIGHT_MIN_RATIO = 0.20D;
    private static final double PILLAR_HEIGHT_MAX_RATIO = 0.40D;
    private static final int PILLAR_BASE_EXTRA = 4;
    private static final int MIN_CAVITY_DEPTH = 40;
    private static final int CAVITY_DEPTH_VARIANTS = 9;
    private static final int MIN_FLOOR_MARGIN = 12;
    private static final double CRACK_THRESHOLD = 0.50D;
    private static final double CRACK_DETAIL_THRESHOLD = 0.40D;
    private static final long WALL_SEED = 0xDEADBEEFL;
    private static final int WALL_CELL_SIZE = 64;
    private static final double WALL_INTRUSION_STRENGTH = 0.15D;

    private MiddleLevelOceanTerrainProfile() {
    }

    public static double chamberWallFade(int blockX, int blockZ) {
        double wallNoise = sample(blockX, blockZ, WALL_CELL_SIZE, WALL_SEED);
        double wallDetail = sample(blockX, blockZ, 16, WALL_SEED ^ 0x12345678L);
        double combined = wallNoise * 0.7D + wallDetail * 0.3D;
        double fade = (combined - WALL_INTRUSION_STRENGTH) / (1.0D - WALL_INTRUSION_STRENGTH);
        return smoothClamp(fade);
    }

    private static double smoothClamp(double value) {
        if (value <= 0.0D) return 0.0D;
        if (value >= 1.0D) return 1.0D;
        return value * value * (3.0D - 2.0D * value);
    }

    public static ColumnProfile profileFor(int blockX, int blockZ, int minBuildHeight) {
        double capTopNoise = sample(blockX, blockZ, 28, 0x5F3759DFL);
        double capTopDetail = sample(blockX, blockZ, 10, 0x5F3759DFL ^ 0xABCDEF01L);
        double capTopBlend = capTopNoise * 0.7D + capTopDetail * 0.3D;
        int capTopY = CAP_TOP_MIN_Y + floor(capTopBlend * ((CAP_TOP_MAX_Y - CAP_TOP_MIN_Y) + 1));

        double capThicknessNoise = sample(blockX, blockZ, 20, 0x6A09E667L);
        int capThickness = MIN_CAP_THICKNESS + floor(capThicknessNoise * CAP_THICKNESS_VARIANTS);
        int capBottomY = Math.max(MiddleLevelOceanPlacement.layerStartBlockY() + 1,
                capTopY - capThickness + 1);

        double basinNoise = sample(blockX, blockZ, 52, 0x243F6A88L);
        double basinDetail = sample(blockX, blockZ, 24, 0xB7E15162L);
        double cavityBlend = basinNoise * 0.72D + basinDetail * 0.28D;
        int cavityDepth = MIN_CAVITY_DEPTH + floor(cavityBlend * CAVITY_DEPTH_VARIANTS);
        int cavityFloorY = Math.max(minBuildHeight + MIN_FLOOR_MARGIN, capBottomY - cavityDepth);

        double crackField = sample(blockX, blockZ, 44, 0x9E3779B9L);
        double crackDetail = sample(blockX, blockZ, 12, 0x7F4A7C15L);
        boolean crack = crackField > CRACK_THRESHOLD && crackDetail > CRACK_DETAIL_THRESHOLD;

        int pillarTopY = 0;
        if (!crack) {
            double pillarNoise = unitHash(blockX, blockZ, 0xA1B2C3D4L);
            if (pillarNoise < PILLAR_CHANCE) {
                double connectNoise = unitHash(blockX ^ 0x55, blockZ ^ 0xAA, 0xD4C3B2A1L);
                if (connectNoise < PILLAR_CONNECTED_CHANCE) {
                    pillarTopY = capBottomY;
                } else {
                    double heightNoise = unitHash(blockX ^ 0x7F, blockZ ^ 0x3A, 0x1A2B3C4DL);
                    double ratio = PILLAR_HEIGHT_MIN_RATIO
                            + heightNoise * (PILLAR_HEIGHT_MAX_RATIO - PILLAR_HEIGHT_MIN_RATIO);
                    int cavHeight = capBottomY - cavityFloorY;
                    pillarTopY = cavityFloorY + (int) Math.round(ratio * cavHeight);
                }
            }
        }

        return new ColumnProfile(capTopY, capBottomY, cavityFloorY, crack, pillarTopY);
    }

    public static boolean isPillarAt(int blockX, int blockZ) {
        double pillarNoise = unitHash(blockX, blockZ, 0xA1B2C3D4L);
        return pillarNoise < PILLAR_CHANCE;
    }

    private static double sample(int blockX, int blockZ, int cellSize, long seed) {
        int cellX = Math.floorDiv(blockX, cellSize);
        int cellZ = Math.floorDiv(blockZ, cellSize);
        double localX = (double) Math.floorMod(blockX, cellSize) / cellSize;
        double localZ = (double) Math.floorMod(blockZ, cellSize) / cellSize;
        double smoothX = smooth(localX);
        double smoothZ = smooth(localZ);

        double sample00 = unitHash(cellX, cellZ, seed);
        double sample10 = unitHash(cellX + 1, cellZ, seed);
        double sample01 = unitHash(cellX, cellZ + 1, seed);
        double sample11 = unitHash(cellX + 1, cellZ + 1, seed);
        double lerpX0 = lerp(smoothX, sample00, sample10);
        double lerpX1 = lerp(smoothX, sample01, sample11);
        return lerp(smoothZ, lerpX0, lerpX1);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
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
        value ^= value >>> 31;
        return value;
    }

    public record ColumnProfile(int capTopY, int capBottomY, int cavityFloorY, boolean crack,
                                int pillarTopY) {
        public int capThickness() {
            return capTopY - capBottomY + 1;
        }

        public int cavityHeight() {
            return capBottomY - cavityFloorY;
        }
    }
}
