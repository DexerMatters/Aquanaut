package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Strategy: how a rock family is chosen where two or more districts' materials meet.
 * A hard argmax draws a one-block-sharp contact line; a weighted hash dither instead
 * produces geological contact interdigitation — lenses and patches of each family whose
 * local frequency follows the district weights, so contacts read as interbedded rock
 * rather than as a wall of one material butting into another.
 */
public interface ContactMaterial {
    /**
     * @param familyWeights weights ≥ 0, one per candidate family (need not be normalised)
     * @param blockX/blockY/blockZ world position of the block being shaded
     * @param seed                family-independent dither salt
     * @return index of the chosen family, or the argmax when weights carry no mixture
     */
    int pickFamily(double[] familyWeights, int blockX, int blockY, int blockZ, long seed);

    /** Plain argmax — sharp contacts, kept for comparisons and tests. */
    ContactMaterial DOMINANT = (familyWeights, blockX, blockY, blockZ, seed) -> {
        int best = 0;
        for (int i = 1; i < familyWeights.length; i++) {
            if (familyWeights[i] > familyWeights[best]) {
                best = i;
            }
        }
        return best;
    };

    /**
     * Weighted hash dither over cubic cells of {@code cellSize} blocks. Weights are
     * sharpened by {@code gamma} before the draw so near-pure districts stay visually
     * clean (a 0.9/0.1 contact sprinkles ~1% lenses, not 10% confetti) while genuine
     * 50/50 contacts interdigitate fully.
     */
    record HashDither(int cellSize, double gamma) implements ContactMaterial {
        public static final HashDither DEFAULT = new HashDither(2, 2.0D);

        public HashDither {
            if (cellSize <= 0) {
                throw new IllegalArgumentException("dither cell size must be positive");
            }
            if (gamma < 1.0D) {
                throw new IllegalArgumentException("dither gamma must be >= 1");
            }
        }

        @Override
        public int pickFamily(double[] familyWeights, int blockX, int blockY, int blockZ, long seed) {
            int n = familyWeights.length;
            double total = 0.0D;
            for (double weight : familyWeights) {
                total += Math.max(0.0D, weight);
            }
            if (n == 0 || total <= 0.0D) {
                return 0;
            }

            // Sharpen (gamma) then renormalise: near-pure districts stay clean, true
            // contacts interdigitate. Allocation-free: the hot fill path calls this
            // per block.
            double sharpenedTotal = 0.0D;
            int dominant = 0;
            for (int i = 0; i < n; i++) {
                sharpenedTotal += Math.pow(Math.max(0.0D, familyWeights[i]) / total, gamma);
                if (familyWeights[i] > familyWeights[dominant]) {
                    dominant = i;
                }
            }
            if (sharpenedTotal <= 0.0D) {
                return dominant;
            }

            int cellY = Math.floorDiv(blockY, cellSize);
            double roll = BlendMath.unitHash(
                    Math.floorDiv(blockX, cellSize) + cellY * 7,
                    Math.floorDiv(blockZ, cellSize) - cellY * 13,
                    seed) * sharpenedTotal;
            double cumulative = 0.0D;
            for (int i = 0; i < n; i++) {
                cumulative += Math.pow(Math.max(0.0D, familyWeights[i]) / total, gamma);
                if (roll < cumulative) {
                    return i;
                }
            }
            return dominant;
        }
    }

    /** Convenience: dither draw straight from the shared hash family. */
    static double ditherRoll(int cellX, int cellZ, long seed) {
        long mixed = SoftMixNoise.mix(cellX, cellZ, seed);
        return ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }
}
