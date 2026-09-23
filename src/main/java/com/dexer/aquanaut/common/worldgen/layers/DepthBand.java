package com.dexer.aquanaut.common.worldgen.layers;

/**
 * Absolute block-Y band for a depth layer, with optional soft edges.
 * Soft edge widths are in blocks (0 = hard cut).
 */
public record DepthBand(int minY, int maxY, double blendDown, double blendUp) {

    public DepthBand {
        if (maxY < minY) {
            throw new IllegalArgumentException("DepthBand max_y must be >= min_y");
        }
    }

    public static DepthBand hard(int minY, int maxY) {
        return new DepthBand(minY, maxY, 0.0D, 0.0D);
    }

    public static DepthBand soft(int minY, int maxY, double blend) {
        return new DepthBand(minY, maxY, blend, blend);
    }

    public boolean containsBlockY(int blockY) {
        return blockY >= minY && blockY <= maxY;
    }

    public double weightAtBlockY(int blockY) {
        return SoftMixNoise.bandWeight(blockY, minY, maxY, blendDown, blendUp);
    }

    public int minQuartY() {
        return QuartY.fromBlock(minY);
    }

    public int maxQuartY() {
        return QuartY.fromBlock(maxY);
    }
}
