package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Pure radial mask of the water-world spawn island.
 *
 * <p>
 * The water world preset turns every column into open ocean, so a fresh spawn would drown
 * (design note {@code Aquanaut-Design/Changes.md}). This mask raises the planned geological
 * floor near the world origin to a dry plateau: within {@link #FULL_RADIUS} blocks of the
 * spawn column (0, 0) the floor is pinned to at least {@link #ISLAND_TOP_Y}, a few blocks
 * above the preset's sea level 63, and the target fades out over a smoothstep ramp so the
 * seamount flanks merge into the ordinary ocean floor without a contour cliff. The planner's
 * cliff guard runs over the blended field like over any other raw floor, so chunk borders and
 * the analytic single-column path stay bit-identical.
 * </p>
 */
public final class SpawnIslandMask {
    /** Plateau floor target; the water world preset's sea level is 63. */
    public static final int ISLAND_TOP_Y = 70;
    /** Radius (blocks) of the fully emerged plateau around the spawn column (0, 0). */
    public static final int FULL_RADIUS = 48;
    /** Radius (blocks) at which the lift reaches zero and the ocean floor is untouched. */
    public static final int FADE_RADIUS = 192;

    private SpawnIslandMask() {
    }

    /** Continuous blend weight in [0, 1]: 1 on the plateau, 0 at and beyond the fade radius. */
    public static double maskAt(int blockX, int blockZ) {
        double distSq = (double) blockX * blockX + (double) blockZ * blockZ;
        if (distSq >= (double) FADE_RADIUS * FADE_RADIUS) {
            return 0.0D;
        }
        double dist = Math.sqrt(distSq);
        return SoftMixNoise.smoothstep((FADE_RADIUS - dist) / (FADE_RADIUS - FULL_RADIUS));
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
