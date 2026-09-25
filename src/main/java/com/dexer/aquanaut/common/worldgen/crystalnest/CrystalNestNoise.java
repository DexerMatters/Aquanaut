package com.dexer.aquanaut.common.worldgen.crystalnest;

/**
 * Deterministic hashing and trilinear 3D value noise for the crystal-nest geometry.
 *
 * <p>
 * The crystal nest is built from an implicit surface ("skin") displaced by this stereo
 * noise, so every sample must be a pure function of its coordinates: chunk borders and
 * re-generation then always agree. Pure math only — no Minecraft types — so the whole
 * skinning pipeline stays unit-testable.
 */
public final class CrystalNestNoise {
    private CrystalNestNoise() {
    }

    /** Avalanche mix of a 3D lattice coordinate and a salt into a well-distributed long. */
    public static long hash(int x, int y, int z, long salt) {
        long value = 0x9E3779B97F4A7C15L;
        value ^= (long) x * 341873128712L;
        value ^= (long) y * 132897987541L;
        value ^= (long) z * 405026578501L;
        value ^= salt * 0x94D049BB133111EBL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    /** Uniform double in {@code [0, 1)}. */
    public static double unit(int x, int y, int z, long salt) {
        return ((hash(x, y, z, salt) >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    /** Uniform int in {@code [0, bound)}. */
    public static int bounded(int x, int y, int z, long salt, int bound) {
        return Math.floorMod(hash(x, y, z, salt), bound);
    }

    /**
     * Trilinear value noise in {@code [-1, 1]} over a cubic lattice of {@code cell} blocks,
     * with smoothstep corner weights. This is the 立体噪波 that displaces the skinned
     * isosurface and keeps its outlines irregular in all three axes.
     */
    public static double value3(double x, double y, double z, double cell, long salt) {
        double cx = x / cell;
        double cy = y / cell;
        double cz = z / cell;
        int ix = (int) Math.floor(cx);
        int iy = (int) Math.floor(cy);
        int iz = (int) Math.floor(cz);
        double fx = smoothstep(cx - ix);
        double fy = smoothstep(cy - iy);
        double fz = smoothstep(cz - iz);

        double c000 = corner(ix, iy, iz, salt);
        double c100 = corner(ix + 1, iy, iz, salt);
        double c010 = corner(ix, iy + 1, iz, salt);
        double c110 = corner(ix + 1, iy + 1, iz, salt);
        double c001 = corner(ix, iy, iz + 1, salt);
        double c101 = corner(ix + 1, iy, iz + 1, salt);
        double c011 = corner(ix, iy + 1, iz + 1, salt);
        double c111 = corner(ix + 1, iy + 1, iz + 1, salt);

        double x00 = lerp(fx, c000, c100);
        double x10 = lerp(fx, c010, c110);
        double x01 = lerp(fx, c001, c101);
        double x11 = lerp(fx, c011, c111);
        double y0 = lerp(fy, x00, x10);
        double y1 = lerp(fy, x01, x11);
        return lerp(fz, y0, y1);
    }

    public static double smoothstep(double value) {
        double t = value <= 0.0D ? 0.0D : (value >= 1.0D ? 1.0D : value);
        return t * t * (3.0D - 2.0D * t);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double corner(int x, int y, int z, long salt) {
        return Math.floorMod(hash(x, y, z, salt), 2001L) / 1000.0D - 1.0D;
    }
}
