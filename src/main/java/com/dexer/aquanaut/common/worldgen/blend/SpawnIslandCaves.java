package com.dexer.aquanaut.common.worldgen.blend;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Seeded, strictly dry cave voids inside the spawn island's solid core.
 *
 * <p>
 * Two sheared 2D value-noise sheets per family stand in for 3D noise: tunnels are the
 * intersection of two near-zero sheets (curvilinear spaghetti, the same trick vanilla uses),
 * caverns the intersection of two above-threshold sheets (compact blobs). Different shear
 * directions per field keep the families independent. Everything is a pure function of the
 * world seed and the block position, so neighbouring chunks agree bit for bit.
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

    /** Salt of the tunnel, cavern and envelope scrambles ("CAVE"). */
    private static final long TUNNEL_A_SALT = 0xCAFE11L;
    private static final long TUNNEL_B_SALT = 0xCAFE22L;
    private static final long CAVERN_A_SALT = 0xCAFE33L;
    private static final long CAVERN_B_SALT = 0xCAFE44L;
    /** Noise-space cells of the four sheared fields: broad enough for walkable tunnels. */
    private static final double TUNNEL_A_CELL = 41.0D;
    private static final double TUNNEL_B_CELL = 57.0D;
    private static final double CAVERN_A_CELL = 83.0D;
    private static final double CAVERN_B_CELL = 113.0D;
    /** Half-width of the tunnel tubes in noise units and the cavern openness threshold. */
    private static final double TUNNEL_WIDTH = 0.085D;
    private static final double CAVERN_THRESHOLD = 0.34D;
    /** Distinct shear directions so the four sheets carve independent geometry. */
    private static final double SHEAR_AX = 17.13D;
    private static final double SHEAR_AZ = -9.71D;
    private static final double SHEAR_BX = -23.77D;
    private static final double SHEAR_BZ = 14.29D;
    private static final double SHEAR_CX = 7.91D;
    private static final double SHEAR_CZ = 27.37D;
    private static final double SHEAR_DX = -31.19D;
    private static final double SHEAR_DZ = -5.53D;

    private SpawnIslandCaves() {
    }

    /**
     * Whether one block of the island core is a cave void. The caller enforces the envelope:
     * only fully emerged plateau columns ({@code mask >= 1}), at least
     * {@link #SURFACE_PROTECT_MARGIN} blocks below the building surface, above
     * {@link #CAVE_BOTTOM_Y}.
     */
    public static boolean isCave(long islandSeed, int blockX, int blockY, int blockZ) {
        if (blockY < CAVE_BOTTOM_Y) {
            return false;
        }
        double fx = blockX;
        double fy = blockY;
        double fz = blockZ;
        double tunnelA = SoftMixNoise.valueNoise(fx + SHEAR_AX * fy, fz + SHEAR_AZ * fy,
                TUNNEL_A_CELL, SpawnIslandMask.scramble(islandSeed, TUNNEL_A_SALT));
        if (Math.abs(tunnelA) < TUNNEL_WIDTH) {
            double tunnelB = SoftMixNoise.valueNoise(fx + SHEAR_BX * fy, fz + SHEAR_BZ * fy,
                    TUNNEL_B_CELL, SpawnIslandMask.scramble(islandSeed, TUNNEL_B_SALT));
            if (Math.abs(tunnelB) < TUNNEL_WIDTH) {
                return true;
            }
        }
        double cavernA = SoftMixNoise.valueNoise(fx + SHEAR_CX * fy, fz + SHEAR_CZ * fy,
                CAVERN_A_CELL, SpawnIslandMask.scramble(islandSeed, CAVERN_A_SALT));
        if (cavernA > CAVERN_THRESHOLD) {
            double cavernB = SoftMixNoise.valueNoise(fx + SHEAR_DX * fy, fz + SHEAR_DZ * fy,
                    CAVERN_B_CELL, SpawnIslandMask.scramble(islandSeed, CAVERN_B_SALT));
            if (cavernB > CAVERN_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
}
