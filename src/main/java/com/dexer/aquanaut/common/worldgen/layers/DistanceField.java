package com.dexer.aquanaut.common.worldgen.layers;

/**
 * Two-pass chamfer distance transform over a boolean support grid.
 * Distance is measured in grid cells; callers convert to block units.
 */
public final class DistanceField {
    private static final float INF = 1e9f;
    private static final float ORTHO = 1.0f;
    private static final float DIAG = 1.41421356f;

    private final float[][] distanceCells;
    private final int width;
    private final int height;

    private DistanceField(float[][] distanceCells) {
        this.distanceCells = distanceCells;
        this.width = distanceCells.length;
        this.height = distanceCells[0].length;
    }

    /**
     * @param supported true = interior (distance 0), false = obstacle / outside
     */
    public static DistanceField of(boolean[][] supported) {
        int width = supported.length;
        int height = supported[0].length;
        float[][] dist = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                dist[x][z] = supported[x][z] ? 0.0f : INF;
            }
        }

        // Forward pass
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                float d = dist[x][z];
                if (x > 0) {
                    d = Math.min(d, dist[x - 1][z] + ORTHO);
                    if (z > 0) {
                        d = Math.min(d, dist[x - 1][z - 1] + DIAG);
                    }
                    if (z < height - 1) {
                        d = Math.min(d, dist[x - 1][z + 1] + DIAG);
                    }
                }
                if (z > 0) {
                    d = Math.min(d, dist[x][z - 1] + ORTHO);
                }
                dist[x][z] = d;
            }
        }

        // Backward pass
        for (int x = width - 1; x >= 0; x--) {
            for (int z = height - 1; z >= 0; z--) {
                float d = dist[x][z];
                if (x < width - 1) {
                    d = Math.min(d, dist[x + 1][z] + ORTHO);
                    if (z > 0) {
                        d = Math.min(d, dist[x + 1][z - 1] + DIAG);
                    }
                    if (z < height - 1) {
                        d = Math.min(d, dist[x + 1][z + 1] + DIAG);
                    }
                }
                if (z < height - 1) {
                    d = Math.min(d, dist[x][z + 1] + ORTHO);
                }
                dist[x][z] = d;
            }
        }

        return new DistanceField(dist);
    }

    public float distanceCellsAt(int x, int z) {
        return distanceCells[Math.floorMod(x, width)][Math.floorMod(z, height)];
    }

    /**
     * Nearest-unsupported distance in grid cells (0 on supported cells).
     * For supported cells the chamfer field is 0; we instead want distance TO the nearest unsupported.
     * Callers should pass inverted support (unsupported=true means "distance zero source") or use
     * {@link #nearestUnsupportedCells(boolean[][])}.
     */
    public static float[][] nearestUnsupportedCells(boolean[][] supported) {
        int width = supported.length;
        int height = supported[0].length;
        boolean[][] inverted = new boolean[width][height];
        boolean anyUnsupported = false;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                inverted[x][z] = !supported[x][z];
                anyUnsupported |= inverted[x][z];
            }
        }
        if (!anyUnsupported) {
            float[][] allFar = new float[width][height];
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    allFar[x][z] = INF;
                }
            }
            return allFar;
        }
        DistanceField field = of(inverted);
        return field.distanceCells;
    }

    /**
     * Smooth edge strength in [0,1]: 0 at unsupported, 1 once {@code fullStrengthCells} away.
     * Sampled with nearest-neighbor on the coarse grid (good enough for 4-block quarts).
     */
    public static double edgeStrength(float[][] nearestUnsupportedCells,
                                      int cellX,
                                      int cellZ,
                                      double fullStrengthCells) {
        float d = nearestUnsupportedCells[Math.floorMod(cellX, nearestUnsupportedCells.length)]
                [Math.floorMod(cellZ, nearestUnsupportedCells[0].length)];
        if (d >= INF * 0.5f) {
            return 1.0D;
        }
        return SoftMixNoise.smoothstep(d / fullStrengthCells);
    }
}
