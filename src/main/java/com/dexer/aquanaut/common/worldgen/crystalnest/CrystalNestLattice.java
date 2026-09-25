package com.dexer.aquanaut.common.worldgen.crystalnest;

/**
 * The skinned pipeline lattice of the crystal nest (水晶巢).
 *
 * <p>
 * The nest is a three-dimensional pipe network whose 蒙皮管线 — the skinned struts — run
 * along the edges of a coarse spatial lattice and meet at its intersection nodes. Every
 * intersection that survives the activation roll hosts one huge geode core: a very large
 * nodule of nest stone with a big irregular hollow chamber inside where the crystals grow.
 * The struts are <b>solid</b> branches — no bore, no internal 空腔 — so the chambers are
 * sealed rooms and the lattice reads as one massive skeleton of stone instead of a pipe
 * network. Hollow exists only inside the cores.
 *
 * <p>
 * Vertical layout: two pipe rows at {@code y = ±ROW_Y} (world-fixed, so the horizontal
 * runs stay level across the map) plus roots that plunge from the bottom row into the sea
 * floor and reach from the top row up into the ceiling — which is why the terrain only
 * builds where the middle sea actually has a ceiling overhead.
 *
 * <p>
 * Pure math on {@link CrystalNestNoise}: all shape data is a function of the node indices,
 * so any consumer (fill, decoration, tests) sees exactly the same geometry.
 */
public final class CrystalNestLattice {
    /** Horizontal pitch of the pipe lattice in blocks. */
    public static final int CELL = 90;
    /** Pipe rows sit at {@code y = -ROW_Y} and {@code y = +ROW_Y} plus small jitter. */
    public static final double ROW_Y = 18.0D;
    /** Node jitter range in X/Z (blocks, applied as ±half of this). */
    public static final double NODE_JITTER_XZ = 15.0D;
    /** Node jitter range in Y (blocks, applied as ±half of this). */
    public static final double NODE_JITTER_Y = 4.5D;

    /** Huge cores: diameter lands between 40 and 56 blocks before noise. */
    public static final double CORE_RADIUS_MIN = 20.0D;
    public static final double CORE_RADIUS_MAX = 28.0D;
    /**
     * Inner chamber radius as a fraction of the core radius: a big irregular hollow with
     * a massive shell — the chamber is a room inside the rock, not a bubble with a rind.
     * Kept generous because the branches carry no bores any more: the chambers are the
     * whole 空腔 budget of the lattice.
     */
    public static final double CAVITY_RATIO_MIN = 0.62;
    public static final double CAVITY_RATIO_MAX = 0.72;
    /** Share of lattice intersections that carry a core. */
    public static final double CORE_PROBABILITY = 0.88D;
    /** Share of cores that are hollowed out into chambers (the rest stay solid nodules). */
    public static final double HOLLOW_PROBABILITY = 0.93D;

    /**
     * Strut (pipe) radii in blocks. The branches are solid (no bores), so they run slimmer
     * than the old hollow tubes — still massive, but the open water keeps its share of the
     * biome.
     */
    public static final double STRUT_RADIUS_MIN = 8.5;
    public static final double STRUT_RADIUS_MAX = 11.5;

    /** Roots anchor the lattice into the sea floor and the ceiling. */
    public static final double ROOT_RADIUS_MIN = 6.5D;
    public static final double ROOT_RADIUS_MAX = 9.5D;
    public static final double ROOT_PROBABILITY = 0.55D;

    /**
     * Width of the skinned isoband in blocks: rock forms while the shell field is within
     * this distance of its surface. A wide band welds cores and branches into one massive
     * structure — together with the depth-graded nest-rock matrix it sets how dense the
     * biome packs (target: a dense breccia that grows denser with depth).
     */
    public static final double SKIN_BAND = 1.7;

    /** Stereo noise displacement of the outer skin, as fractions of the local radius. */
    public static final double OUTER_NOISE_MAJOR = 0.17D;
    public static final double OUTER_NOISE_MINOR = 0.09D;
    /** Stereo noise displacement of chamber walls, in blocks (irregular voids). */
    public static final double CAVITY_NOISE_MAJOR = 2.2D;
    public static final double CAVITY_NOISE_MINOR = 1.1D;

    /**
     * Low-frequency domain warp of the whole field: the skinned surface is evaluated at
     * the warped position, so pipes bend, cores lean and the lattice swims instead of
     * standing to attention. This is what keeps the geometry 迷幻.
     */
    public static final double WARP_MAJOR = 4.6D;
    public static final double WARP_MINOR = 1.9D;

    /**
     * Blend radius of the metaball union in blocks: cores and tubes melt into one another
     * at their joints with a fat fillet instead of meeting in a crease.
     */
    public static final double SMOOTH_UNION_K = 2.5D;

    /**
     * Nest-rock matrix: a coarse 3D noise fused into the skeleton as chunky breccia
     * masses. Its isosurface sinks with depth ({@code MATRIX_TOP_THRESHOLD} down by
     * {@code MATRIX_GRADIENT} over {@code MATRIX_DEPTH_RANGE} blocks below
     * {@code MATRIX_TOP_Y}), so the crown of the biome stays airy and the roots pack
     * massive: density grows with depth.
     */
    public static final double MATRIX_TOP_THRESHOLD = 0.9D;
    public static final double MATRIX_GRADIENT = 1.3D;
    public static final double MATRIX_SCALE = 7.0D;
    public static final int MATRIX_TOP_Y = 32;
    public static final int MATRIX_DEPTH_RANGE = 64;

    /**
     * Geode chambers pinch shut with depth at this rate: deep chambers are tighter and
     * the rock around them denser, while the crown keeps its grand hollow rooms.
     */
    public static final double CAVITY_DEPTH_PINCH = 0.5D;

    /**
     * Border riving: through the biome transition the domain warp and skin noise amplify
     * up to this factor, tearing and distorting the lattice where the crystal nest runs
     * into its neighbours instead of fading out along clean lines.
     */
    public static final double RIFT_DISTORTION = 1.6D;

    /** Half-extent of the bounding sphere of any primitive's influence (incl. the warp). */
    public static final double INFLUENCE = CORE_RADIUS_MAX * 1.3D + SKIN_BAND + 8.0D;

    // Strut kinds on the lattice graph.
    public static final int STRUT_X = 0;
    public static final int STRUT_Z = 1;
    public static final int STRUT_Y = 2;
    public static final int STRUT_ROOT_DOWN = 3;
    public static final int STRUT_ROOT_UP = 4;
    /** Diagonal pipes across the cell: the branches that make the network dense and tangled. */
    public static final int STRUT_DIAG_1 = 5;
    public static final int STRUT_DIAG_2 = 6;
    public static final int STRUT_KINDS = 7;

    private static final long SEED = 0x435253544E535400L;
    private static final long SALT_JITTER_X = SEED ^ 0x01L;
    private static final long SALT_JITTER_Y = SEED ^ 0x02L;
    private static final long SALT_JITTER_Z = SEED ^ 0x03L;
    private static final long SALT_RADIUS = SEED ^ 0x04L;
    private static final long SALT_CAVITY = SEED ^ 0x05L;
    private static final long SALT_ACTIVE = SEED ^ 0x06L;
    private static final long SALT_HOLLOW = SEED ^ 0x07L;
    private static final long SALT_STRUT = SEED ^ 0x08L;
    private static final long SALT_ROOT = SEED ^ 0x09L;
    private static final long SALT_ROOT_DRIFT = SEED ^ 0x0AL;

    /** Local floor/ceiling of the middle sea, needed to root the lattice down and up. */
    public interface SpanQuery {
        int floorY(double x, double z);

        int ceilingY(double x, double z);
    }

    /** One intersection of the skinned pipeline: an (optional) geode core. */
    public record Node(int i, int k, int row,
                       double x, double y, double z,
                       double radius, double cavityRadius,
                       boolean active, boolean hollow) {
    }

    private final SpanQuery span;

    public CrystalNestLattice(SpanQuery span) {
        this.span = span;
    }

    public SpanQuery span() {
        return span;
    }

    /** Full description of one lattice intersection (tests and debugging; not a hot path). */
    public Node nodeAt(int i, int k, int row) {
        return new Node(i, k, row, nodeX(i, k, row), nodeY(i, k, row), nodeZ(i, k, row),
                nodeRadius(i, k, row), nodeCavityRadius(i, k, row),
                nodeActive(i, k, row), nodeHollow(i, k, row));
    }

    // --- node scalars (hot path: keep allocation-free) -------------------------

    public double nodeX(int i, int k, int row) {
        return i * (double) CELL + (CrystalNestNoise.unit(i, k, row, SALT_JITTER_X) - 0.5D) * NODE_JITTER_XZ;
    }

    public double nodeY(int i, int k, int row) {
        double base = row == 0 ? -ROW_Y : ROW_Y;
        return base + (CrystalNestNoise.unit(i, k, row, SALT_JITTER_Y) - 0.5D) * NODE_JITTER_Y;
    }

    public double nodeZ(int i, int k, int row) {
        return k * (double) CELL + (CrystalNestNoise.unit(i, k, row, SALT_JITTER_Z) - 0.5D) * NODE_JITTER_XZ;
    }

    public double nodeRadius(int i, int k, int row) {
        return CORE_RADIUS_MIN
                + CrystalNestNoise.unit(i, k, row, SALT_RADIUS) * (CORE_RADIUS_MAX - CORE_RADIUS_MIN);
    }

    public double nodeCavityRadius(int i, int k, int row) {
        return nodeRadius(i, k, row)
                * (CAVITY_RATIO_MIN + CrystalNestNoise.unit(i, k, row, SALT_CAVITY)
                * (CAVITY_RATIO_MAX - CAVITY_RATIO_MIN));
    }

    public boolean nodeActive(int i, int k, int row) {
        return CrystalNestNoise.unit(i, k, row, SALT_ACTIVE) < CORE_PROBABILITY;
    }

    public boolean nodeHollow(int i, int k, int row) {
        return nodeActive(i, k, row)
                && CrystalNestNoise.unit(i, k, row, SALT_HOLLOW) < HOLLOW_PROBABILITY;
    }

    // --- strut scalars ---------------------------------------------------------

    /** Whether the pipe of the given kind leaving this node exists on the lattice graph. */
    public boolean strutLive(int i, int k, int row, int kind) {
        if (!nodeActive(i, k, row)) {
            return false;
        }
        return switch (kind) {
            case STRUT_X -> nodeActive(i + 1, k, row);
            case STRUT_Z -> nodeActive(i, k + 1, row);
            case STRUT_Y -> row == 0 && nodeActive(i, k, 1);
            case STRUT_ROOT_DOWN -> row == 0
                    && CrystalNestNoise.unit(i, k, row, SALT_ROOT) < ROOT_PROBABILITY;
            case STRUT_ROOT_UP -> row == 1
                    && CrystalNestNoise.unit(i, k, row, SALT_ROOT) < ROOT_PROBABILITY;
            case STRUT_DIAG_1 -> nodeActive(i + 1, k + 1, row);
            case STRUT_DIAG_2 -> nodeActive(i + 1, k - 1, row);
            default -> false;
        };
    }

    public double strutRadius(int i, int k, int row, int kind) {
        if (kind == STRUT_ROOT_DOWN || kind == STRUT_ROOT_UP) {
            return ROOT_RADIUS_MIN + CrystalNestNoise.unit(i, k, row, SALT_STRUT)
                    * (ROOT_RADIUS_MAX - ROOT_RADIUS_MIN);
        }
        return STRUT_RADIUS_MIN + CrystalNestNoise.unit(i, k, row, SALT_STRUT)
                * (STRUT_RADIUS_MAX - STRUT_RADIUS_MIN);
    }

    /** Start of the pipe segment (the node center in every kind). */
    public double strutAX(int i, int k, int row) {
        return nodeX(i, k, row);
    }

    public double strutAY(int i, int k, int row) {
        return nodeY(i, k, row);
    }

    public double strutAZ(int i, int k, int row) {
        return nodeZ(i, k, row);
    }

    /** End of the pipe segment: the far node, or the floor/ceiling anchor for roots. */
    public double strutBX(int i, int k, int row, int kind) {
        return switch (kind) {
            case STRUT_X -> nodeX(i + 1, k, row);
            case STRUT_Z -> nodeX(i, k + 1, row);
            case STRUT_Y -> nodeX(i, k, 1);
            case STRUT_DIAG_1 -> nodeX(i + 1, k + 1, row);
            case STRUT_DIAG_2 -> nodeX(i + 1, k - 1, row);
            default -> nodeX(i, k, row)
                    + (CrystalNestNoise.unit(i, k, row, SALT_ROOT_DRIFT) - 0.5D) * 8.0D;
        };
    }

    public double strutBY(int i, int k, int row, int kind) {
        return switch (kind) {
            case STRUT_X -> nodeY(i + 1, k, row);
            case STRUT_Z -> nodeY(i, k + 1, row);
            case STRUT_Y -> nodeY(i, k, 1);
            case STRUT_DIAG_1 -> nodeY(i + 1, k + 1, row);
            case STRUT_DIAG_2 -> nodeY(i + 1, k - 1, row);
            case STRUT_ROOT_DOWN -> span.floorY(nodeX(i, k, row), nodeZ(i, k, row)) - 2.5D;
            default -> span.ceilingY(nodeX(i, k, row), nodeZ(i, k, row)) + 2.5D;
        };
    }

    public double strutBZ(int i, int k, int row, int kind) {
        return switch (kind) {
            case STRUT_X -> nodeZ(i + 1, k, row);
            case STRUT_Z -> nodeZ(i, k + 1, row);
            case STRUT_Y -> nodeZ(i, k, 1);
            case STRUT_DIAG_1 -> nodeZ(i + 1, k + 1, row);
            case STRUT_DIAG_2 -> nodeZ(i + 1, k - 1, row);
            default -> nodeZ(i, k, row)
                    + (CrystalNestNoise.unit(i, k, row, SALT_ROOT_DRIFT + 1L) - 0.5D) * 8.0D;
        };
    }
}
