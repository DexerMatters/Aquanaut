package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Seeded, strictly dry cave voids inside the spawn island's solid core, carved from true 3D
 * value-noise fields the way vanilla shapes its cheese and spaghetti caves.
 *
 * <p>
 * <b>Tunnels</b> are the intersection of two independent 3D fields near zero
 * ({@code |f1| < w && |f2| < w}): each near-zero set is a curving surface, and two surfaces
 * intersect along a one-dimensional curve — a winding gallery with a roughly circular
 * cross-section that meanders through all three axes instead of slanting like a fissure.
 * <b>Caverns</b> are the region where a broader third field crosses a depth-dependent
 * threshold: closed, rounded cheese pockets that grow deeper down. Both families taper out
 * against the envelope planes (the protected building surface above, the cave floor below),
 * so no void ends in a flat, layer-like ceiling or floor — the failure mode of sheared 2D
 * sheet fields, whose threshold plates intersect into lens-shaped geological voids.
 * Everything is a pure function of the world seed and the block position, so neighbouring
 * chunks agree bit for bit.
 * </p>
 *
 * <p>
 * <b>Dry guarantee.</b> The caller only opens voids where the island mask is fully emerged
 * ({@code mask >= 1}, i.e. the plateau core), below
 * {@link #SURFACE_PROTECT_MARGIN} blocks of rock under the building surface, and above
 * {@link #CAVE_BOTTOM_Y}. Inside that envelope the nearest open water is sealed behind at
 * least ~34 blocks of solid seamount at the shallowest cave depth and far more below, the
 * planner path never consults vanilla aquifers, carved AIR is not scheduled for fluid
 * post-processing, and vanilla carvers are suppressed in covered chunks — so a generated
 * cave cannot hold or touch water. Players who mine into a connected water body trigger the
 * mod's BreachFloodEvents instead; that consequence is by design.
 * </p>
 */
public final class SpawnIslandCaves {
    /** Lowest Y a cave void may occupy; keeps a solid floor above the world bottom. */
    public static final int CAVE_BOTTOM_Y = -54;
    /** Solid rock that must separate the building surface from any cave void below it. */
    public static final int SURFACE_PROTECT_MARGIN = 14;
    /**
     * Absolute top of the cave envelope: the standard plateau target (70) minus the surface
     * margin. Hill columns carry extra headroom, but their caves stay capped here — the
     * voids belong to the body of the seamount, not to the hills riding it.
     */
    public static final int CAVE_TOP_Y = 70 - SURFACE_PROTECT_MARGIN;

    /** Salts of the two tunnel fields and the cheese field ("CAVE"). */
    private static final long TUNNEL_A_SALT = 0xCAFE11L;
    private static final long TUNNEL_B_SALT = 0xCAFE22L;
    private static final long CHEESE_SALT = 0xCAFE55L;
    /** Cells of the two tunnel fields: a broad meander octave plus a wall-texture octave. */
    private static final double TUNNEL_A_BROAD_CELL = 84.0D;
    private static final double TUNNEL_A_FINE_CELL = 27.0D;
    private static final double TUNNEL_B_BROAD_CELL = 68.0D;
    private static final double TUNNEL_B_FINE_CELL = 22.0D;
    private static final double TUNNEL_FINE_WEIGHT = 0.35D;
    /** Half-width of the tunnel tubes in noise units (each field lives in [-1, 1]). */
    private static final double TUNNEL_WIDTH = 0.07D;
    /**
     * Y expansion of the tunnel fields: the fields vary faster along Y than horizontally,
     * so tube axes favour gentle horizontal meanders over steep dives and the galleries run
     * wide rather than tall.
     */
    private static final double TUNNEL_Y_SCALE = 1.5D;
    /** Cells of the cheese field: broad pockets plus a finer lobe octave. */
    private static final double CHEESE_BROAD_CELL = 120.0D;
    private static final double CHEESE_FINE_CELL = 46.0D;
    private static final double CHEESE_FINE_WEIGHT = 0.30D;
    /** Y expansion of the cheese field: wide, low cavern halls instead of vertical shafts. */
    private static final double CHEESE_Y_SCALE = 1.4D;
    /**
     * Cheese threshold at the top and at the bottom of the envelope: the threshold sinks
     * with depth, so pockets grow from rare slivers under the building surface to full
     * caverns near the cave floor.
     */
    private static final double CHEESE_TOP_THRESHOLD = 0.58D;
    private static final double CHEESE_BOTTOM_THRESHOLD = 0.40D;
    /** Height over which the voids taper out against the envelope's top and bottom planes. */
    private static final double TAPER_HEIGHT = 16.0D;
    /** Above any reachable threshold, used to fade the cheese field out at the planes. */
    private static final double IMPOSSIBLE_THRESHOLD = 1.01D;
    /** Cheese field total = broad weight + fine weight; the broad weight for early-outs. */
    private static final double CHEESE_BROAD_WEIGHT = 1.0D - CHEESE_FINE_WEIGHT;

    private SpawnIslandCaves() {
    }

    /**
     * Whether one block of the island core is a cave void. The caller enforces the wider
     * envelope (only fully emerged plateau columns at least {@link #SURFACE_PROTECT_MARGIN}
     * below the building surface); this method additionally refuses everything outside
     * [{@link #CAVE_BOTTOM_Y}, {@link #CAVE_TOP_Y}].
     */
    public static boolean isCave(long islandSeed, int blockX, int blockY, int blockZ) {
        if (blockY < CAVE_BOTTOM_Y || blockY > CAVE_TOP_Y) {
            return false;
        }
        double width = TUNNEL_WIDTH * tunnelScale(blockY);
        if (width > 0.0D) {
            double a = tunnelField(blockX, blockY, blockZ, TUNNEL_A_BROAD_CELL,
                    TUNNEL_A_FINE_CELL, SpawnIslandMask.scramble(islandSeed, TUNNEL_A_SALT));
            if (Math.abs(a) < width) {
                double b = tunnelField(blockX, blockY, blockZ, TUNNEL_B_BROAD_CELL,
                        TUNNEL_B_FINE_CELL, SpawnIslandMask.scramble(islandSeed, TUNNEL_B_SALT));
                if (Math.abs(b) < width) {
                    return true;
                }
            }
        }
        return isCheese(islandSeed, blockX, blockY, blockZ);
    }

    /**
     * The rounded cheese pockets: a broad field crosses a depth-dependent threshold, and the
     * threshold fades to impossible at the envelope planes so pockets never end flat.
     */
    private static boolean isCheese(long islandSeed, int blockX, int blockY, int blockZ) {
        double scale = planeScale(blockY);
        double depth = SoftMixNoise.clamp01((CAVE_TOP_Y - blockY)
                / (double) (CAVE_TOP_Y - CAVE_BOTTOM_Y));
        double threshold = SoftMixNoise.lerp(depth, CHEESE_TOP_THRESHOLD, CHEESE_BOTTOM_THRESHOLD);
        threshold = SoftMixNoise.lerp(scale, IMPOSSIBLE_THRESHOLD, threshold);
        double cheeseY = blockY * CHEESE_Y_SCALE;
        double broad = SoftMixNoise.valueNoise3(blockX, cheeseY, blockZ, CHEESE_BROAD_CELL,
                SpawnIslandMask.scramble(islandSeed, CHEESE_SALT));
        // The fine lobe can add at most CHEESE_FINE_WEIGHT; skip it when even a perfect
        // lobe cannot lift the broad field over the threshold.
        if (CHEESE_BROAD_WEIGHT * broad + CHEESE_FINE_WEIGHT <= threshold) {
            return false;
        }
        double fine = SoftMixNoise.valueNoise3(blockX, cheeseY, blockZ, CHEESE_FINE_CELL,
                SpawnIslandMask.scramble(islandSeed, CHEESE_SALT) ^ 0x51L);
        return CHEESE_BROAD_WEIGHT * broad + CHEESE_FINE_WEIGHT * fine > threshold;
    }

    /** Two octaves of 3D value noise: a broad meander plus finer wall texture. */
    private static double tunnelField(int blockX, int blockY, int blockZ,
                                      double broadCell, double fineCell, long seed) {
        // The fields vary slower along Y than horizontally, so the tube axes favour gentle
        // horizontal meanders over steep dives — walkable galleries, not slides.
        double y = blockY * TUNNEL_Y_SCALE;
        double broad = SoftMixNoise.valueNoise3(blockX, y, blockZ, broadCell, seed);
        double fine = SoftMixNoise.valueNoise3(blockX, y, blockZ, fineCell, seed ^ 0x51L);
        return (1.0D - TUNNEL_FINE_WEIGHT) * broad + TUNNEL_FINE_WEIGHT * fine;
    }

    /**
     * Tunnel tubes shrink to nothing against the envelope planes: full width through the
     * body of the seamount, pinching to a rounded point at the top and the bottom, so no
     * gallery is cut off flat by the envelope.
     */
    private static double tunnelScale(int blockY) {
        double top = SoftMixNoise.smoothstep((CAVE_TOP_Y - blockY) / TAPER_HEIGHT);
        double bottom = SoftMixNoise.smoothstep((blockY - CAVE_BOTTOM_Y) / TAPER_HEIGHT);
        return top * bottom;
    }

    /** Same fade for the cheese threshold: 1 at the planes' neighbourhood, 0 in between. */
    private static double planeScale(int blockY) {
        double top = SoftMixNoise.smoothstep((CAVE_TOP_Y - blockY) / TAPER_HEIGHT);
        double bottom = SoftMixNoise.smoothstep((blockY - CAVE_BOTTOM_Y) / TAPER_HEIGHT);
        return 1.0D - top * bottom;
    }
}
