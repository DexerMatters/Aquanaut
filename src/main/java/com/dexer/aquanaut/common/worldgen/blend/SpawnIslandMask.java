package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Radial mask of the water-world spawn island with a randomized, world-seeded coastline.
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
 * construction and therefore cannot tear at the ±π seam. Both the coastline phase and the
 * modulation amplitudes are scrambled from the world seed, so every world gets its own bay
 * and peninsula layout with its own coastal character, while the smallest plateau radius
 * {@link #MIN_PLATEAU_RADIUS} still covers the spawn chunk (0, 0) that the world-spawn scan
 * relies on. Seed 0 keeps a fixed mid-amplitude silhouette for tests and seedless fallbacks.
 * A low dune relief on the plateau ({@link #duneLift}) is seeded the same way.
 * </p>
 */
public final class SpawnIslandMask {
    /** Plateau floor target; the water world preset's sea level is 63. */
    public static final int ISLAND_TOP_Y = 70;
    /**
     * Radius (blocks) of the dead-flat building core: dune relief is suppressed inside, so the
     * middle of the island stays a clean construction site. The boundary is noise-wobbled per
     * world so the flat zone reads as a natural clearing instead of a drawn circle.
     */
    public static final int FLAT_RADIUS = 24;
    /** Mean radius (blocks) of the fully emerged plateau around the spawn column (0, 0). */
    public static final int FULL_RADIUS = 100;
    /** Fade width (blocks) from the plateau edge down to untouched ocean floor. */
    public static final int FADE_WIDTH = 144;
    /** Nominal radius at which the lift reaches zero (kept for the mean outline). */
    public static final int FADE_RADIUS = FULL_RADIUS + FADE_WIDTH;

    /** Salt of the coastline phase ("ISLA"). */
    private static final long COAST_SEED_SALT = 0x51A0C1A7L;
    /** Salt of the amplitude scramble and of the fine coastline octave. */
    private static final long COAST_FINE_SEED_SALT = 0x5AL;
    private static final long DUNE_SEED_SALT = 0xD0A7L;
    private static final long FLAT_EDGE_SALT = 0x7A31L;
    private static final long SAND_PATCH_SALT = 0x5A1DL;
    /** Cell (blocks) of the wobble that makes the flat-core boundary organic. */
    private static final double FLAT_EDGE_CELL = 31.0D;
    /** Cell (blocks) of the sand patches speckled across the grassland plateau. */
    public static final double SAND_PATCH_CELL = 22.0D;
    private static final double SAND_PATCH_THRESHOLD = 0.55D;
    /** Noise-space radii of the two coastline octaves: ~6 broad lobes, ~13 fine ones. */
    private static final double COAST_BROAD_K = 0.95D;
    private static final double COAST_FINE_K = 2.1D;
    /** Authored amplitude bounds; the world seed picks a value inside each. */
    private static final double COAST_BROAD_AMPLITUDE_MIN = 0.10D;
    private static final double COAST_BROAD_AMPLITUDE_MAX = 0.18D;
    private static final double COAST_FINE_AMPLITUDE_MIN = 0.04D;
    private static final double COAST_FINE_AMPLITUDE_MAX = 0.08D;
    /** Mid amplitudes of the seedless (seed 0) silhouette. */
    private static final double COAST_BROAD_AMPLITUDE_DEFAULT = 0.14D;
    private static final double COAST_FINE_AMPLITUDE_DEFAULT = 0.06D;
    /** Cell (blocks) and coverage of the seeded plateau dunes. */
    private static final double DUNE_CELL = 9.0D;
    private static final double DUNE_THRESHOLD = 0.5D;

    /** Smallest fully emerged radius any world seed can produce, in the tightest bay. */
    public static final double MIN_PLATEAU_RADIUS = FULL_RADIUS * (1.0D
            - COAST_BROAD_AMPLITUDE_MAX - COAST_FINE_AMPLITUDE_MAX);
    /** Beyond this radius (in the longest peninsula of any seed) the lift is exactly zero. */
    public static final double MAX_FADE_RADIUS = FULL_RADIUS * (1.0D
            + COAST_BROAD_AMPLITUDE_MAX + COAST_FINE_AMPLITUDE_MAX) + FADE_WIDTH;

    private SpawnIslandMask() {
    }

    /** Seedless overload: the fixed mid-amplitude silhouette (tests, seedless fallbacks). */
    public static double maskAt(int blockX, int blockZ) {
        return maskAt(0L, blockX, blockZ);
    }

    /** Continuous blend weight in [0, 1]: 1 on the plateau, 0 at and beyond the coastline fade. */
    public static double maskAt(long islandSeed, int blockX, int blockZ) {
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
        long phaseSeed = scramble(islandSeed, COAST_SEED_SALT);
        double broad = SoftMixNoise.valueNoise(unitX * COAST_BROAD_K, unitZ * COAST_BROAD_K,
                1.0D, phaseSeed);
        double fine = SoftMixNoise.valueNoise(unitX * COAST_FINE_K, unitZ * COAST_FINE_K,
                1.0D, phaseSeed ^ COAST_FINE_SEED_SALT);
        double broadAmplitude;
        double fineAmplitude;
        if (islandSeed == 0L) {
            broadAmplitude = COAST_BROAD_AMPLITUDE_DEFAULT;
            fineAmplitude = COAST_FINE_AMPLITUDE_DEFAULT;
        } else {
            broadAmplitude = COAST_BROAD_AMPLITUDE_MIN
                    + (COAST_BROAD_AMPLITUDE_MAX - COAST_BROAD_AMPLITUDE_MIN) * unit01(scramble(islandSeed, 0x101L));
            fineAmplitude = COAST_FINE_AMPLITUDE_MIN
                    + (COAST_FINE_AMPLITUDE_MAX - COAST_FINE_AMPLITUDE_MIN) * unit01(scramble(islandSeed, 0x202L));
        }
        double coastScale = 1.0D + broadAmplitude * broad + fineAmplitude * fine;
        double fullRadius = FULL_RADIUS * coastScale;
        return SoftMixNoise.smoothstep((fullRadius + FADE_WIDTH - dist) / FADE_WIDTH);
    }

    /**
     * Seeded micro relief of the plateau: 1 extra block on roughly a quarter of the outer
     * ground as low grassy knolls, and nothing on the beach flanks (any {@code mask < 1}) or
     * inside the organic flat building core, so the waterline and the construction site stay
     * smooth. The cliff guard downstream relaxes the one-block steps.
     */
    public static int duneLift(long islandSeed, int blockX, int blockZ, double mask) {
        if (mask < 1.0D) {
            return 0;
        }
        // The flat core's boundary is wobbled per world: a natural clearing, not a circle.
        double flatWobble = (SoftMixNoise.valueNoise(blockX, blockZ, FLAT_EDGE_CELL,
                scramble(islandSeed, FLAT_EDGE_SALT)) + 1.0D) * 0.5D;
        double flatRadius = FLAT_RADIUS * (0.75D + 0.5D * flatWobble);
        double distSq = (double) blockX * blockX + (double) blockZ * blockZ;
        if (distSq <= flatRadius * flatRadius) {
            return 0;
        }
        double dune = SoftMixNoise.valueNoise(blockX, blockZ, DUNE_CELL,
                scramble(islandSeed, DUNE_SEED_SALT));
        return dune > DUNE_THRESHOLD ? 1 : 0;
    }

    /**
     * Whether the plateau grassland at this column is a speckled coral-sand patch: the sand
     * spots are scattered by seeded noise instead of forming a ring, so the plateau reads as
     * irregular ground. Trees skip sand on their own (would_survive fails there).
     */
    public static boolean sandPatchAt(long islandSeed, int blockX, int blockZ) {
        return SoftMixNoise.valueNoise(blockX, blockZ, SAND_PATCH_CELL,
                scramble(islandSeed, SAND_PATCH_SALT)) > SAND_PATCH_THRESHOLD;
    }

    /**
     * Blends the raw geological floor toward the island plateau. The target is
     * {@code max(rawFloor, ISLAND_TOP_Y)} so the mask never lowers ground that already
     * rises above the plateau on its own (e.g. volcanic relief riding the same field).
     * The blend runs on the squared mask, which steepens the drop just past the plateau
     * edge and keeps the sandy beach a narrow fringe instead of a wide shelf.
     */
    public static double blendFloor(double rawFloor, double mask) {
        if (mask <= 0.0D) {
            return rawFloor;
        }
        double steepened = mask * mask;
        double target = Math.max(rawFloor, ISLAND_TOP_Y);
        return SoftMixNoise.lerp(steepened, rawFloor, target);
    }

    /** SplitMix64-style scramble so unrelated world seeds give unrelated island characters. */
    static long scramble(long worldSeed, long salt) {
        long z = (worldSeed ^ salt) * 0x9E3779B97F4A7C15L;
        z ^= z >>> 30;
        z *= 0xBF58476D1CE4E5B9L;
        z ^= z >>> 27;
        z *= 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return z;
    }

    static double unit01(long scrambled) {
        return (scrambled >>> 11) / (double) (1L << 53);
    }
}
