package com.dexer.aquanaut.common.worldgen.crystalnest;

/**
 * The implicit crystal-nest field: a noise-displaced, domain-warped metaball skin over the
 * {@link CrystalNestLattice} pipe graph.
 *
 * <p>
 * {@link #shell} is the skinned isosurface of the lattice — huge cores bulging at the
 * pipeline intersections, tubes swelling along every axis and diagonal of its edges, the
 * whole skin displaced by {@link CrystalNestNoise#value3} stereo noise and then domain
 * warped by a second, slower stereo field so the pipes bend and swim. {@link #voidField}
 * is the matching negative shape: one big irregular chamber inside every core. The
 * branches run solid — the chambers are the only 空腔 in the whole lattice — so a block
 * of rock exists wherever the skin encloses space ({@code shell > 0 && voidField < 0}) and
 * every geode is its own sealed room inside the stone.
 *
 * <p>
 * {@code strength} (0..1) is the biome transition fade: it scales every radius, so the
 * lattice thins into open water at the borders of neighboring biomes and vanishes
 * completely at strength 0.
 */
public final class CrystalNestField implements CrystalNestSkin.Field {
    private static final long NOISE_OUTER_MAJOR = 0x4E4553544F555401L;
    private static final long NOISE_OUTER_MINOR = 0x4E4553544F555402L;
    private static final long NOISE_CAVITY_MAJOR = 0x434156495459001L;
    private static final long NOISE_CAVITY_MINOR = 0x434156495459002L;
    private static final long NOISE_WARP_X = 0x574152505858001L;
    private static final long NOISE_WARP_Y = 0x574152505959002L;
    private static final long NOISE_WARP_Z = 0x574152505A5A003L;
    private static final long NOISE_WARP_FINE = 0x57415250464E004L;

    private static final double OUTER_CELL_MAJOR = 15.0D;
    private static final double OUTER_CELL_MINOR = 6.5D;
    private static final double CAVITY_CELL_MAJOR = 12.0D;
    private static final double CAVITY_CELL_MINOR = 6.0D;
    private static final double WARP_CELL_MAJOR = 24.0D;
    private static final double WARP_CELL_MINOR = 10.0D;
    private static final double MATRIX_CELL_MAJOR = 26.0D;
    private static final double MATRIX_CELL_MINOR = 11.0D;

    private static final long NOISE_MATRIX_MAJOR = 0x4D4154524958001L;
    private static final long NOISE_MATRIX_MINOR = 0x4D4154524958002L;

    private final CrystalNestLattice lattice;
    // Scratch for the domain warp of one sample: field evaluation never recurses, and one
    // field instance belongs to exactly one chunk's generation pass.
    private final double[] warp = new double[3];

    public CrystalNestField(CrystalNestLattice lattice) {
        this.lattice = lattice;
    }

    public CrystalNestLattice lattice() {
        return lattice;
    }

    /**
     * Skinned outer field in blocks; positive inside the rock of cores and struts. Rock
     * forms across the whole isoband {@code (-SKIN_BAND, +∞)} — a wide skinning threshold
     * that welds the lattice into one massive structure.
     * Each primitive is a noise-modulated distance blob and the union is a plain max,
     * which metaball-blends where cores and tubes meet.
     */
    public double shell(double x, double y, double z, double strength) {
        if (strength <= 0.0D) {
            return -CrystalNestLattice.SKIN_BAND - 1.0D;
        }
        warpPoint(x, y, z, strength);
        return shellWarped(x + warp[0], y + warp[1], z + warp[2], strength);
    }

    /**
     * Hollow field in blocks; positive inside a core chamber. The chamber noise is
     * absolute (blocks, not fractions), which is what makes the big cavities genuinely
     * irregular instead of neat bubbles.
     */
    public double voidField(double x, double y, double z, double strength) {
        if (strength <= 0.0D) {
            return -1.0D;
        }
        warpPoint(x, y, z, strength);
        return voidWarped(x + warp[0], y + warp[1], z + warp[2], strength);
    }

    /** True where the skinned lattice is rock rather than water. */
    public boolean solidAt(int x, int y, int z, double strength) {
        double sx = x + 0.5D;
        double sy = y + 0.5D;
        double sz = z + 0.5D;
        if (strength <= 0.0D) {
            return false;
        }
        warpPoint(sx, sy, sz, strength);
        double wx = sx + warp[0];
        double wy = sy + warp[1];
        double wz = sz + warp[2];
        return shellWarped(wx, wy, wz, strength) > -CrystalNestLattice.SKIN_BAND * strength
                && voidWarped(wx, wy, wz, strength) < 0.0D;
    }

    /** True where the block sits in the hollow of a core chamber. */
    public boolean hollowAt(int x, int y, int z, double strength) {
        if (strength <= 0.0D) {
            return false;
        }
        double sx = x + 0.5D;
        double sy = y + 0.5D;
        double sz = z + 0.5D;
        warpPoint(sx, sy, sz, strength);
        return voidWarped(sx + warp[0], sy + warp[1], sz + warp[2], strength) > 0.0D;
    }

    @Override
    public int rockAt(int x, int y, int z, double strength) {
        double sx = x + 0.5D;
        double sy = y + 0.5D;
        double sz = z + 0.5D;
        if (strength <= 0.0D) {
            return OPEN;
        }
        warpPoint(sx, sy, sz, strength);
        double wx = sx + warp[0];
        double wy = sy + warp[1];
        double wz = sz + warp[2];
        double shellValue = shellWarped(wx, wy, wz, strength);
        if (shellValue <= -CrystalNestLattice.SKIN_BAND * strength) {
            return OPEN;
        }
        double voidValue = voidWarped(wx, wy, wz, strength);
        if (voidValue >= 0.0D) {
            return OPEN;
        }
        // Right against a chamber wall the rock becomes druse-crusted geode lining.
        return voidValue > -1.3D ? DRUSE : ROCK;
    }

    @Override
    public boolean chamberAt(int x, int y, int z) {
        return hollowAt(x, y, z, 1.0D);
    }

    /** True where the block is rock right against a chamber wall (the druse lining band). */
    public boolean liningAt(int x, int y, int z, double strength) {
        return rockAt(x, y, z, strength) == DRUSE;
    }

    private void warpPoint(double x, double y, double z, double strength) {
        // Border riving: through the biome transition the warp amplifies up to
        // (1 + RIFT_DISTORTION), tearing and distorting the lattice where the crystal
        // nest runs into its neighbours. Full strength stays undistorted.
        double rift = riftFactor(strength);
        warp[0] = rift * (CrystalNestLattice.WARP_MAJOR
                * CrystalNestNoise.value3(x, y, z, WARP_CELL_MAJOR, NOISE_WARP_X)
                + CrystalNestLattice.WARP_MINOR
                * CrystalNestNoise.value3(x, y, z, WARP_CELL_MINOR, NOISE_WARP_FINE));
        warp[1] = rift * (CrystalNestLattice.WARP_MAJOR
                * CrystalNestNoise.value3(x, y, z, WARP_CELL_MAJOR, NOISE_WARP_Y)
                + CrystalNestLattice.WARP_MINOR
                * CrystalNestNoise.value3(x, y, z, WARP_CELL_MINOR, NOISE_WARP_FINE + 1L));
        warp[2] = rift * (CrystalNestLattice.WARP_MAJOR
                * CrystalNestNoise.value3(x, y, z, WARP_CELL_MAJOR, NOISE_WARP_Z)
                + CrystalNestLattice.WARP_MINOR
                * CrystalNestNoise.value3(x, y, z, WARP_CELL_MINOR, NOISE_WARP_FINE + 2L));
    }

    /** Distortion factor of the biome transition band: 1 in the interior, 1+RIFT at half fade. */
    private static double riftFactor(double strength) {
        return 1.0D + CrystalNestLattice.RIFT_DISTORTION * 4.0D * strength * (1.0D - strength);
    }

    private double shellWarped(double x, double y, double z, double strength) {
        double mod = outerModulation(x, y, z, riftFactor(strength));
        // True distance semantics (radius - distance): unbounded below, so the isoband
        // threshold measures real distance to the skin instead of a clamped sentinel.
        double best = Double.NEGATIVE_INFINITY;

        int i0 = indexFloor(x - CrystalNestLattice.INFLUENCE);
        int i1 = indexFloor(x + CrystalNestLattice.INFLUENCE);
        int k0 = indexFloor(z - CrystalNestLattice.INFLUENCE);
        int k1 = indexFloor(z + CrystalNestLattice.INFLUENCE);

        for (int i = i0; i <= i1; i++) {
            for (int k = k0; k <= k1; k++) {
                for (int row = 0; row <= 1; row++) {
                    if (!lattice.nodeActive(i, k, row)) {
                        continue;
                    }
                    double radius = lattice.nodeRadius(i, k, row) * strength * mod;
                    double value = radius - distance(x, y, z,
                            lattice.nodeX(i, k, row), lattice.nodeY(i, k, row), lattice.nodeZ(i, k, row));
                    best = smoothUnion(best, value);
                }
            }
        }

        // Struts may lean one lattice cell past the node window, so widen it by one.
        for (int i = i0 - 1; i <= i1 + 1; i++) {
            for (int k = k0 - 1; k <= k1 + 1; k++) {
                for (int row = 0; row <= 1; row++) {
                    for (int kind = 0; kind < CrystalNestLattice.STRUT_KINDS; kind++) {
                        if (!lattice.strutLive(i, k, row, kind)) {
                            continue;
                        }
                        double radius = lattice.strutRadius(i, k, row, kind) * strength * mod;
                        double value = radius - distanceToSegment(x, y, z, i, k, row, kind, 0.0D, 1.0D);
                        best = smoothUnion(best, value);
                    }
                }
            }
        }
        // The nest-rock matrix welds the skeleton into chunky breccia, ever more of it
        // the deeper the sample sits.
        return smoothUnion(best, matrixValue(x, y, z, strength));
    }

    /**
     * Depth-graded nest-rock matrix in the same blocks-as-distance units as the shell
     * field, so the isoband welds it to the skeleton naturally. The noise threshold
     * sinks with depth: an airy crown of distinct giants, massive fused breccia at the
     * roots.
     */
    private double matrixValue(double x, double y, double z, double strength) {
        double noise = 0.7D * CrystalNestNoise.value3(x, y, z, MATRIX_CELL_MAJOR, NOISE_MATRIX_MAJOR)
                + 0.3D * CrystalNestNoise.value3(x, y, z, MATRIX_CELL_MINOR, NOISE_MATRIX_MINOR);
        double threshold = CrystalNestLattice.MATRIX_TOP_THRESHOLD
                - depthGain(y) * CrystalNestLattice.MATRIX_GRADIENT;
        return (noise - threshold) * CrystalNestLattice.MATRIX_SCALE * strength;
    }

    /** 0 at the crown of the biome, 1 at the bottom of its depth range. */
    private static double depthGain(double y) {
        double depth = (CrystalNestLattice.MATRIX_TOP_Y - y) / CrystalNestLattice.MATRIX_DEPTH_RANGE;
        return CrystalNestNoise.smoothstep(depth);
    }

    private double voidWarped(double x, double y, double z, double strength) {
        double cavityNoise = CrystalNestLattice.CAVITY_NOISE_MAJOR
                * CrystalNestNoise.value3(x, y, z, CAVITY_CELL_MAJOR, NOISE_CAVITY_MAJOR)
                + CrystalNestLattice.CAVITY_NOISE_MINOR
                * CrystalNestNoise.value3(x, y, z, CAVITY_CELL_MINOR, NOISE_CAVITY_MINOR);
        // True distance semantics (radius - distance): unbounded below, so the isoband
        // threshold measures real distance to the skin instead of a clamped sentinel.
        double best = Double.NEGATIVE_INFINITY;

        int i0 = indexFloor(x - CrystalNestLattice.INFLUENCE);
        int i1 = indexFloor(x + CrystalNestLattice.INFLUENCE);
        int k0 = indexFloor(z - CrystalNestLattice.INFLUENCE);
        int k1 = indexFloor(z + CrystalNestLattice.INFLUENCE);

        for (int i = i0; i <= i1; i++) {
            for (int k = k0; k <= k1; k++) {
                for (int row = 0; row <= 1; row++) {
                    if (!lattice.nodeHollow(i, k, row)) {
                        continue;
                    }
                    // Chambers pinch shut with depth: grand hollow rooms in the crown,
                    // tight sealed geodes at the roots where the rock packs dense.
                    double pinch = 1.0D - CrystalNestLattice.CAVITY_DEPTH_PINCH * depthGain(y);
                    double radius = (lattice.nodeCavityRadius(i, k, row) + cavityNoise) * strength * pinch;
                    double value = radius - distance(x, y, z,
                            lattice.nodeX(i, k, row), lattice.nodeY(i, k, row), lattice.nodeZ(i, k, row));
                    best = smoothUnion(best, value);
                }
            }
        }
        return best;
    }

    /**
     * Quadratic smooth max: neighbouring metaballs melt into one another with a fat
     * fillet of radius {@link CrystalNestLattice#SMOOTH_UNION_K} at their joints instead
     * of meeting in a crease — the skeleton welds into one giant organism.
     */
    private static double smoothUnion(double best, double value) {
        double k = CrystalNestLattice.SMOOTH_UNION_K;
        double blend = Math.max(k - Math.abs(best - value), 0.0D) / k;
        return Math.max(best, value) + blend * blend * k * 0.25D;
    }

    private double outerModulation(double x, double y, double z, double rift) {
        return 1.0D
                + rift * (CrystalNestLattice.OUTER_NOISE_MAJOR
                * CrystalNestNoise.value3(x, y, z, OUTER_CELL_MAJOR, NOISE_OUTER_MAJOR)
                + CrystalNestLattice.OUTER_NOISE_MINOR
                * CrystalNestNoise.value3(x, y, z, OUTER_CELL_MINOR, NOISE_OUTER_MINOR));
    }

    private double distanceToSegment(double x, double y, double z, int i, int k, int row, int kind,
                                     double tMin, double tMax) {
        double ax = lattice.strutAX(i, k, row);
        double ay = lattice.strutAY(i, k, row);
        double az = lattice.strutAZ(i, k, row);
        double bx = lattice.strutBX(i, k, row, kind);
        double by = lattice.strutBY(i, k, row, kind);
        double bz = lattice.strutBZ(i, k, row, kind);
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double length2 = dx * dx + dy * dy + dz * dz;
        double t = length2 <= 1e-9D ? 0.0D
                : ((x - ax) * dx + (y - ay) * dy + (z - az) * dz) / length2;
        t = Math.max(tMin, Math.min(tMax, t));
        return distance(x, y, z, ax + t * dx, ay + t * dy, az + t * dz);
    }

    private static double distance(double x, double y, double z, double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static int indexFloor(double value) {
        return (int) Math.floor(value / CrystalNestLattice.CELL);
    }
}
