package com.dexer.aquanaut.common.worldgen.blend;

/**
 * Strategy: how several district contributions to one scalar height/thickness field are
 * composed into a single value. All implementations are continuous in both the values and
 * the weights, so a district fading out can never tear the composed surface.
 */
public interface HeightCombiner {
    /**
     * @param values  one contribution per district
     * @param weights district weights ≥ 0 (need not be normalised; all-zero falls back to
     *                an unweighted combination)
     */
    double combine(double[] values, double[] weights);

    /** Weighted mean Σwᵢhᵢ/Σwᵢ — the default for base floors and reef thickness. */
    HeightCombiner LINEAR = (values, weights) -> {
        double total = 0.0D;
        double totalWeight = 0.0D;
        for (int i = 0; i < values.length; i++) {
            double w = weightAt(weights, i);
            total += values[i] * w;
            totalWeight += w;
        }
        if (totalWeight <= 0.0D) {
            return mean(values);
        }
        return total / totalWeight;
    };

    /** Hard max over the entries with positive weight — continuous because every member field is. */
    HeightCombiner MAX = (values, weights) -> {
        double max = Double.NEGATIVE_INFINITY;
        boolean any = false;
        for (int i = 0; i < values.length; i++) {
            if (weightAt(weights, i) <= 0.0D) {
                continue;
            }
            any = true;
            max = Math.max(max, values[i]);
        }
        return any ? max : mean(values);
    };

    /**
     * Smoothed max (log-sum-exp with sharpness {@code p}) — overlapping relief merges
     * through rounded saddles instead of kinked max-seams.
     */
    static HeightCombiner softMax(double p) {
        return (values, weights) -> {
            double composed = BlendMath.softMax(values, weights, p);
            if (Double.isFinite(composed)) {
                return composed;
            }
            return mean(values);
        };
    }

    private static double weightAt(double[] weights, int index) {
        if (weights == null || index >= weights.length) {
            return 1.0D;
        }
        return Math.max(0.0D, weights[index]);
    }

    private static double mean(double[] values) {
        if (values.length == 0) {
            return 0.0D;
        }
        double total = 0.0D;
        for (double value : values) {
            total += value;
        }
        return total / values.length;
    }
}
