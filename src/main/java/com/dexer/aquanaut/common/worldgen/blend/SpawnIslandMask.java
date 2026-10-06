package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Radial mask of the water-world spawn island with a randomized coastline.
 *
 * <p>
 * The water world preset turns every column into open ocean, so a fresh spawn would drown
 * (design note {@code Aquanaut-Design/Changes.md}). This mask raises the planned geological
 * floor near the world origin to a dry plateau: the floor is pinned to at least
 * {@link #ISLAND_TOP_Y}, a few blocks above the preset's sea level 63, and the target fades
 * out over a smoothstep ramp so the seamount flanks merge into the ordinary ocean floor
 * without a contour cliff. The planner's cliff guard runs over the blended field like over
 * any other raw floor, so chunk borders and the analytic single-column path stay
 * bit-identical.
 * </p>
 *
 * <p>
 * The outline is not a circle: the plateau and fade radii are stretched per direction by two
 * octaves of value noise sampled on the unit circle in noise space, which is periodic by
 * construction and therefore cannot tear at the ±π seam. Bays and peninsulas stay gentle —
 * the smallest plateau radius {@link #MIN_PLATEAU_RADIUS} still covers the spawn chunk
 * (0, 0) that the world-spawn scan relies on. The modulation seeds are fixed constants like
 * every other field of the layer stack, so the island keeps one authored silhouette across
 * worlds instead of flickering with the world seed.
 * </p>
 */
public final class SpawnIslandMask {
    /** Plateau floor target; the water world preset's sea level is 63. */
    public static final int ISLAND_TOP_Y = 70;
    /** Mean radius (blocks) of the fully emerged plateau around the spawn column (0, 0). */
    public static final int FULL_RADIUS = 48;
    /** Nominal radius at which the lift reaches zero (kept for the mean outline). */
    public static final int FADE_RADIUS = 192;
    /** Fade width (blocks) from the plateau edge down to untouched ocean floor. */
    public static final int FADE_WIDTH = FADE_RADIUS - FULL_RADIUS;

    /** Seed of the coastline modulation ("ISLA"). */
    private static final long COAST_SEED = 0x51A0C1A7L;
    /** Noise-space radii of the two coastline octaves: ~6 broad lobes, ~13 fine ones. */
    private static final double COAST_BROAD_K = 0.95D;
    private static final double COAST_FINE_K = 2.1D;
    private static final double COAST_BROAD_AMPLITUDE = 0.16D;
    private static final double COAST_FINE_AMPLITUDE = 0.06D;

    /** Smallest radius that is still fully emerged, in the tightest bay direction. */
    public static final double MIN_PLATEAU_RADIUS =
            FULL_RADIUS * (1.0D - COAST_BROAD_AMPLITUDE - COAST_FINE_AMPLITUDE);
    /** Beyond this radius (in the longest peninsula direction) the lift is exactly zero. */
    public static final double MAX_FADE_RADIUS =
            FULL_RADIUS * (1.0D + COAST_BROAD_AMPLITUDE + COAST_FINE_AMPLITUDE) + FADE_WIDTH;

    private SpawnIslandMask() {
    }

    /** Continuous blend weight in [0, 1]: 1 on the plateau, 0 at and beyond the coastline fade. */
    public static double maskAt(int blockX, int blockZ) {
        double distSq = (double) blockX * blockX + (double) blockZ * blockZ;
        double maxFadeSq = MAX_FADE_RADIUS * MAX_FADE_RADIUS;
        if (distSq >= maxFadeSq) {
            return 0.0D;
        }
        double dist = Math.sqrt(distSq);
        if (dist < 1.0e-9D) {
            return 1.0D;
        }
        // Direction on the unit circle, sampled in noise space: periodic, so bays and
        // peninsulas wrap smoothly around the seam at ±π.
        double unitX = blockX / dist;
        double unitZ = blockZ / dist;
        double broad = SoftMixNoise.valueNoise(unitX * COAST_BROAD_K, unitZ * COAST_BROAD_K, 1.0D, COAST_SEED);
        double fine = SoftMixNoise.valueNoise(unitX * COAST_FINE_K, unitZ * COAST_FINE_K, 1.0D, COAST_SEED ^ 0x5AL);
        double coastScale = 1.0D + COAST_BROAD_AMPLITUDE * broad + COAST_FINE_AMPLITUDE * fine;
        double fullRadius = FULL_RADIUS * coastScale;
        return SoftMixNoise.smoothstep((fullRadius + FADE_WIDTH - dist) / FADE_WIDTH);
    }

    /**
     * Blends the raw geological floor toward the island plateau. The target is
     * {@code max(rawFloor, ISLAND_TOP_Y)} so the mask never lowers ground that already
     * rises above the plateau on its own (e.g. volcanic relief riding the same field).
     */
    public static double blendFloor(double rawFloor, double mask) {
        if (mask <= 0.0D) {
            return rawFloor;
        }
        double target = Math.max(rawFloor, ISLAND_TOP_Y);
        return SoftMixNoise.lerp(mask, rawFloor, target);
    }
}
