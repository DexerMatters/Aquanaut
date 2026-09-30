package com.dexer.aquanaut.common.worldgen.blend;

/**
 * Strategy: the final anti-cliff pass over a composed height grid.
 *
 * <p>The default guard is a fixed three-step, purely local pipeline:</p>
 * <ol>
 *   <li><b>Binomial 5×5 smoothing</b> — spreads single-column spikes over their
 *       neighbours;</li>
 *   <li><b>Iterated cone relaxation</b> — {@code reach} passes of a 3×3 min-plus step of
 *       cost {@code maxStep}. After k passes the result equals the infimal convolution
 *       with the cone {@code maxStep·min(k, ‖d‖∞)}: cliffs up to {@code reach} blocks tall
 *       are re-sloped to at most {@code maxStep} per column, exactly like a classical
 *       slope limit;</li>
 *   <li><b>Deviation floor after every pass</b> — {@code h ≥ g − 2·maxDeviation} keeps the
 *       guard honest (it re-slopes cliffs, it never excavates basins) and, crucially,
 *       makes each pass a strictly local function: the value of one column depends only
 *       on raw values within {@link #radius()}, identically from any grid that contains
 *       that neighbourhood. Two chunks sharing a column therefore compute bit-identical
 *       floors — there is no global scan whose truncation could differ per chunk.</li>
 * </ol>
 *
 * <p>Guarantee: adjacent-column steps ≤ {@code maxStep} wherever the relaxation is in
 * charge, and ≤ the smoothed geology's own slope (≈1.3·maxStep for a raw step) in the
 * narrow band where the deviation floor catches the erosion — i.e. only at the feet of
 * deliberately steep features. The 4–15 block walls of hard threshold transitions are
 * gone by construction.</p>
 */
public interface CliffGuard {
    /** Raw-value radius the guarded output of one column depends on. */
    int radius();

    /**
     * Guard a square row-major grid of raw heights ({@code raw[x * extent + z]}).
     * Output values are exact only for cells at least {@link #radius()} away from the
     * grid border; callers size their halo accordingly.
     */
    double[] guard(double[] raw, int extent);

    /** Guard a single column analytically: builds the radius window around it on the fly. */
    default double guardPoint(RawFloor rawFloor, int blockX, int blockZ) {
        int radius = radius();
        int extent = radius * 2 + 1;
        double[] window = new double[extent * extent];
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                window[(dx + radius) * extent + (dz + radius)] =
                        rawFloor.at(blockX + dx, blockZ + dz);
            }
        }
        return guard(window, extent)[radius * extent + radius];
    }

    /** Raw (unguarded) floor height of one world column. */
    interface RawFloor {
        double at(int blockX, int blockZ);
    }

    /** No-op guard for comparisons and tests. */
    CliffGuard IDENTITY = new CliffGuard() {
        @Override
        public int radius() {
            return 0;
        }

        @Override
        public double[] guard(double[] raw, int extent) {
            return raw.clone();
        }
    };

    /**
     * Binomial smoothing + iterated Chebyshev cone relaxation + deviation floor — the
     * production guard. Re-slopes raw cliffs up to {@code reach} blocks tall to
     * {@code maxStep} per column while staying within roughly {@code 2·maxDeviation} of
     * the smoothed geology.
     */
    record ConeErosion(double maxStep, double maxDeviation, int reach) implements CliffGuard {
        /** Binomial kernel radius; the 5×5 kernel [1,4,6,4,1]⊗[1,4,6,4,1]/256. */
        private static final int SMOOTH_RADIUS = 2;
        private static final int[] BINOMIAL = {1, 4, 6, 4, 1};

        public ConeErosion {
            if (maxStep <= 0.0D) {
                throw new IllegalArgumentException("max step must be positive");
            }
            if (maxDeviation < 0.0D) {
                throw new IllegalArgumentException("max deviation must be >= 0");
            }
            if (reach < 1) {
                throw new IllegalArgumentException("reach must be at least 1");
            }
        }

        /** Production defaults: 1-block slopes, ±2-block geology leash, 12-block reach. */
        public static ConeErosion defaults() {
            return new ConeErosion(1.0D, 2.0D, 12);
        }

        @Override
        public int radius() {
            return SMOOTH_RADIUS + reach;
        }

        @Override
        public double[] guard(double[] raw, int extent) {
            // 1) Binomial smoothing, edge-replicated so the field is uniformly defined;
            //    outputs at least radius() inside the border never read replicated cells.
            double[] smoothed = new double[extent * extent];
            for (int x = 0; x < extent; x++) {
                for (int z = 0; z < extent; z++) {
                    smoothed[x * extent + z] = binomialAt(raw, extent, x, z);
                }
            }
            // 2) Iterated 3×3 cone relaxation with the deviation floor after every pass.
            double floorDrop = 2.0D * maxDeviation;
            double[] h = smoothed.clone();
            double[] next = new double[extent * extent];
            for (int pass = 0; pass < reach; pass++) {
                for (int x = 0; x < extent; x++) {
                    int xm = Math.max(0, x - 1);
                    int xp = Math.min(extent - 1, x + 1);
                    for (int z = 0; z < extent; z++) {
                        int zm = Math.max(0, z - 1);
                        int zp = Math.min(extent - 1, z + 1);
                        double best = h[x * extent + z];
                        best = Math.min(best, h[xm * extent + zm] + maxStep);
                        best = Math.min(best, h[xm * extent + z] + maxStep);
                        best = Math.min(best, h[xm * extent + zp] + maxStep);
                        best = Math.min(best, h[x * extent + zm] + maxStep);
                        best = Math.min(best, h[x * extent + zp] + maxStep);
                        best = Math.min(best, h[xp * extent + zm] + maxStep);
                        best = Math.min(best, h[xp * extent + z] + maxStep);
                        best = Math.min(best, h[xp * extent + zp] + maxStep);
                        double floored = smoothed[x * extent + z] - floorDrop;
                        next[x * extent + z] = Math.max(best, floored);
                    }
                }
                double[] swap = h;
                h = next;
                next = swap;
            }
            return h;
        }

        private static double binomialAt(double[] raw, int extent, int x, int z) {
            double total = 0.0D;
            for (int dx = -SMOOTH_RADIUS; dx <= SMOOTH_RADIUS; dx++) {
                int sx = clampIndex(x + dx, extent);
                double wx = BINOMIAL[dx + SMOOTH_RADIUS];
                for (int dz = -SMOOTH_RADIUS; dz <= SMOOTH_RADIUS; dz++) {
                    int sz = clampIndex(z + dz, extent);
                    total += raw[sx * extent + sz] * wx * BINOMIAL[dz + SMOOTH_RADIUS];
                }
            }
            return total / 256.0D;
        }

        private static int clampIndex(int index, int extent) {
            return Math.max(0, Math.min(extent - 1, index));
        }
    }
}
