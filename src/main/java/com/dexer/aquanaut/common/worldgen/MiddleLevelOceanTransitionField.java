package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.layers.DistanceField;
import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Region-edge strength around supported quart cells. Internally uses a chamfer
 * distance transform (O(N)) instead of per-block nearest-obstacle scans (O(N×cells)).
 */
public final class MiddleLevelOceanTransitionField {
    private static final double FULL_STRENGTH_RADIUS_BLOCKS = 16.0D;

    private final boolean[][] supportedQuartCells;
    private final int chunkOriginQuartX;
    private final int chunkOriginQuartZ;
    private final float[][] nearestUnsupportedCells;

    public MiddleLevelOceanTransitionField(boolean[][] supportedQuartCells, int chunkOriginQuartX, int chunkOriginQuartZ) {
        this.supportedQuartCells = supportedQuartCells;
        this.chunkOriginQuartX = chunkOriginQuartX;
        this.chunkOriginQuartZ = chunkOriginQuartZ;
        this.nearestUnsupportedCells = DistanceField.nearestUnsupportedCells(supportedQuartCells);
    }

    public boolean isCurrentChunkQuartCellSupported(int localQuartX, int localQuartZ) {
        return supportedQuartCells[chunkOriginQuartX + localQuartX][chunkOriginQuartZ + localQuartZ];
    }

    public double edgeStrengthAtBlock(int localBlockX, int localBlockZ) {
        // Map block position into continuous field-cell space (1 cell = 4 blocks).
        double blockX = (chunkOriginQuartX << 2) + localBlockX + 0.5D;
        double blockZ = (chunkOriginQuartZ << 2) + localBlockZ + 0.5D;
        double cellX = blockX / 4.0D;
        double cellZ = blockZ / 4.0D;
        float nearestInvalidCells = sampleBilinear(cellX, cellZ);
        if (nearestInvalidCells >= 1e8f) {
            return 1.0D;
        }
        // Distance field is in cells (quarts); legacy radius was in blocks → /4.
        double nearestInvalidBlocks = nearestInvalidCells * 4.0D;
        return SoftMixNoise.smoothstep(nearestInvalidBlocks / FULL_STRENGTH_RADIUS_BLOCKS);
    }

    private float sampleBilinear(double cellX, double cellZ) {
        int x0 = (int) Math.floor(cellX);
        int z0 = (int) Math.floor(cellZ);
        double fx = cellX - x0;
        double fz = cellZ - z0;
        float d00 = at(x0, z0);
        float d10 = at(x0 + 1, z0);
        float d01 = at(x0, z0 + 1);
        float d11 = at(x0 + 1, z0 + 1);
        double top = d00 + fx * (d10 - d00);
        double bot = d01 + fx * (d11 - d01);
        return (float) (top + fz * (bot - top));
    }

    private float at(int quartX, int quartZ) {
        if (quartX < 0 || quartZ < 0
                || quartX >= nearestUnsupportedCells.length
                || quartZ >= nearestUnsupportedCells[0].length) {
            return 0.0f;
        }
        return nearestUnsupportedCells[quartX][quartZ];
    }
}
