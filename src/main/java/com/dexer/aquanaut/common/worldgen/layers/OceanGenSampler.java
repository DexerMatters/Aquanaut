package com.dexer.aquanaut.common.worldgen.layers;

import net.minecraft.resources.ResourceLocation;

/**
 * Per-chunk cached samples used by biome rewrite + terrain carve.
 * Open-water is sampled on a 4×4 quart grid (16 probes) instead of a full 16×16 second noise pass.
 */
public final class OceanGenSampler {
    public static final int QUARTS = 4;
    public static final int HALO_QUART_RADIUS = 4;
    public static final int FIELD_SIZE = QUARTS + HALO_QUART_RADIUS * 2;

    private final ResourceLocation[][] surfaceBiomes = new ResourceLocation[QUARTS][QUARTS];
    private final int[][] openWaterCounts = new int[QUARTS][QUARTS];
    private final boolean[][] support = new boolean[FIELD_SIZE][FIELD_SIZE];
    private final float[][] nearestUnsupportedCells;
    private final double[] edgeAtBlock = new double[256];
    private final OceanLayerStack stack;
    private final int baseQuartX;
    private final int baseQuartZ;

    public OceanGenSampler(OceanLayerStack stack,
                           ResourceLocation[][] surfaceBiomes,
                           boolean[] quartOpenWater,
                           boolean[][] haloParentBiome,
                           int baseQuartX,
                           int baseQuartZ) {
        this.stack = stack;
        this.baseQuartX = baseQuartX;
        this.baseQuartZ = baseQuartZ;
        for (int x = 0; x < QUARTS; x++) {
            System.arraycopy(surfaceBiomes[x], 0, this.surfaceBiomes[x], 0, QUARTS);
            for (int z = 0; z < QUARTS; z++) {
                this.openWaterCounts[x][z] = quartOpenWater[x * QUARTS + z] ? 16 : 0;
            }
        }

        for (int qx = 0; qx < FIELD_SIZE; qx++) {
            for (int qz = 0; qz < FIELD_SIZE; qz++) {
                int localX = qx - HALO_QUART_RADIUS;
                int localZ = qz - HALO_QUART_RADIUS;
                if (localX >= 0 && localX < QUARTS && localZ >= 0 && localZ < QUARTS) {
                    support[qx][qz] = stack.supportsQuartCell(
                            this.surfaceBiomes[localX][localZ],
                            this.openWaterCounts[localX][localZ]);
                } else {
                    support[qx][qz] = haloParentBiome[qx][qz];
                }
            }
        }

        this.nearestUnsupportedCells = DistanceField.nearestUnsupportedCells(support);
        double fullStrengthCells = Math.max(0.25, stack.regionEdgeFadeBlocks() / 4.0D);
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int cellX = HALO_QUART_RADIUS + (localX >> 2);
                int cellZ = HALO_QUART_RADIUS + (localZ >> 2);
                // Intra-quart offset in cells (0..0.75) for smoother fade than pure nearest-quart.
                double cellOffsetX = (localX & 3) * 0.25;
                double cellOffsetZ = (localZ & 3) * 0.25;
                float d = sampleDistance(cellX + cellOffsetX, cellZ + cellOffsetZ);
                edgeAtBlock[localX * 16 + localZ] = SoftMixNoise.smoothstep(d / fullStrengthCells);
            }
        }
    }

    private float sampleDistance(double cellX, double cellZ) {
        int x0 = (int) Math.floor(cellX);
        int z0 = (int) Math.floor(cellZ);
        double fx = cellX - x0;
        double fz = cellZ - z0;
        float d00 = cell(x0, z0);
        float d10 = cell(x0 + 1, z0);
        float d01 = cell(x0, z0 + 1);
        float d11 = cell(x0 + 1, z0 + 1);
        double top = d00 + fx * (d10 - d00);
        double bot = d01 + fx * (d11 - d01);
        return (float) (top + fz * (bot - top));
    }

    private float cell(int x, int z) {
        if (x < 0 || z < 0 || x >= FIELD_SIZE || z >= FIELD_SIZE) {
            return 0.0f;
        }
        return nearestUnsupportedCells[x][z];
    }

    public OceanLayerStack stack() {
        return stack;
    }

    public ResourceLocation surfaceBiome(int localQuartX, int localQuartZ) {
        return surfaceBiomes[localQuartX][localQuartZ];
    }

    public int openWaterCount(int localQuartX, int localQuartZ) {
        return openWaterCounts[localQuartX][localQuartZ];
    }

    public boolean supportsCurrentChunkCell(int localQuartX, int localQuartZ) {
        return support[HALO_QUART_RADIUS + localQuartX][HALO_QUART_RADIUS + localQuartZ];
    }

    public double edgeStrengthAtLocalBlock(int localX, int localZ) {
        return edgeAtBlock[localX * 16 + localZ];
    }

    public double[] edgeAtBlock() {
        return edgeAtBlock;
    }

    public int baseQuartX() {
        return baseQuartX;
    }

    public int baseQuartZ() {
        return baseQuartZ;
    }

    public LayerBlendField blendField() {
        return new LayerBlendField(stack, edgeAtBlock, baseQuartX, baseQuartZ);
    }

    public boolean anySupported() {
        for (int x = 0; x < QUARTS; x++) {
            for (int z = 0; z < QUARTS; z++) {
                if (supportsCurrentChunkCell(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }
}
