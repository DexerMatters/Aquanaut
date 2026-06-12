package com.dexer.aquanaut.common.worldgen;

public final class MiddleLevelOceanTransitionField {
    private static final double FULL_STRENGTH_RADIUS_BLOCKS = 16.0D;

    private final boolean[][] supportedQuartCells;
    private final int chunkOriginQuartX;
    private final int chunkOriginQuartZ;

    public MiddleLevelOceanTransitionField(boolean[][] supportedQuartCells, int chunkOriginQuartX, int chunkOriginQuartZ) {
        this.supportedQuartCells = supportedQuartCells;
        this.chunkOriginQuartX = chunkOriginQuartX;
        this.chunkOriginQuartZ = chunkOriginQuartZ;
    }

    public boolean isCurrentChunkQuartCellSupported(int localQuartX, int localQuartZ) {
        return supportedQuartCells[chunkOriginQuartX + localQuartX][chunkOriginQuartZ + localQuartZ];
    }

    public double edgeStrengthAtBlock(int localBlockX, int localBlockZ) {
        double blockX = (chunkOriginQuartX << 2) + localBlockX + 0.5D;
        double blockZ = (chunkOriginQuartZ << 2) + localBlockZ + 0.5D;
        double nearestInvalid = Double.POSITIVE_INFINITY;

        for (int quartX = 0; quartX < supportedQuartCells.length; quartX++) {
            for (int quartZ = 0; quartZ < supportedQuartCells[quartX].length; quartZ++) {
                if (supportedQuartCells[quartX][quartZ]) {
                    continue;
                }

                nearestInvalid = Math.min(nearestInvalid, distanceToQuartCell(blockX, blockZ, quartX, quartZ));
            }
        }

        if (nearestInvalid == Double.POSITIVE_INFINITY) {
            return 1.0D;
        }

        return smoothClamp(nearestInvalid / FULL_STRENGTH_RADIUS_BLOCKS);
    }

    private static double distanceToQuartCell(double blockX, double blockZ, int quartX, int quartZ) {
        double minX = quartX * 4.0D;
        double maxX = minX + 4.0D;
        double minZ = quartZ * 4.0D;
        double maxZ = minZ + 4.0D;
        double dx = axisDistance(blockX, minX, maxX);
        double dz = axisDistance(blockZ, minZ, maxZ);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double axisDistance(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }

    private static double smoothClamp(double value) {
        if (value <= 0.0D) {
            return 0.0D;
        }
        if (value >= 1.0D) {
            return 1.0D;
        }
        return value * value * (3.0D - 2.0D * value);
    }
}
