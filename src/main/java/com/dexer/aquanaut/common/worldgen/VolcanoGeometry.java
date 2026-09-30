package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.blend.BlendMath;
import com.dexer.aquanaut.common.worldgen.blend.CellSource;
import com.dexer.aquanaut.common.worldgen.blend.EmergenceCurve;
import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

import java.util.Arrays;
import java.util.Objects;

/**
 * Deterministic, chunk-seamless geometry for the volcanic fields of Brimstone Caldera.
 *
 * <p>Volcanic edifices are shaped here as pure functions of world position so every chunk
 * derives the same cone from the same hashes — no cross-chunk setBlock is needed for the
 * giant terrain. A coarse field grid holds one volcano per active cell; a column takes the
 * strongest edifice among the neighbouring cells (main cone or parasitic flank cone) and
 * blends its height with the biome-mix strength so volcanic districts fade into the
 * surrounding middle sea instead of ending on a cliff.</p>
 *
 * <p>The massifs are built like real stratovolcanoes rather than cones of revolution:
 * radial buttresses and spurs swell the outline, downslope-deepening barrancos (erosion
 * gullies) rake the flanks, the summit carries a wide caldera with a raised rim, breached
 * volcanoes have a notch torn out of the rim with a spill channel running down the flank,
 * and the vent itself is a rounded, fluted lava dome or a broad resurgent neck instead of a
 * one-block spire. Between the giants a field of satellite scoria cones and spatter ridges
 * fills the plain — the many small vents of a real volcanic field.</p>
 *
 * <p>All heights are returned in block Y and relative to the column's geological floor Y.
 * The class is intentionally free of Minecraft types so the shape maths stays unit-testable.</p>
 */
public final class VolcanoGeometry {
    /** Field cell size in blocks: one volcanic centre candidate per cell. */
    public static final int FIELD_CELL = 96;
    /** Fraction of cells that carry a volcano at all. */
    public static final double ACTIVE_CHANCE = 0.68D;
    /** Strength below which the volcanic relief sinks back into the plain. */
    public static final double MIN_STRENGTH = 0.25D;
    /** Cell size of the satellite scoria-cone and spatter-ridge field. */
    public static final int STACK_CELL = 30;
    /** Fraction of stack cells that actually erupt a satellite vent. */
    public static final double STACK_CHANCE = 0.58D;
    /** Angular half-width (radians) of the notch torn out of a breached crater rim. */
    private static final double BREACH_NOTCH_RADIANS = 0.40D;
    /**
     * Half-width of the rift trench in noise units. The C1 taper window spans |rift| < this;
     * the width sets the graben's flanks — wide enough that the raw walls stay erodible by
     * the cliff guard instead of cutting slot canyons.
     */
    private static final double RIFT_HALF_WIDTH = 0.075D;
    /** Deepest point of a rift trench, in blocks. */
    private static final double RIFT_DEPTH = 9.0D;
    /** Strength below which the (cheap) edifice scan is skipped entirely — sub-block relief. */
    private static final double EDIFICE_SKIP_STRENGTH = 0.02D;
    /** Strength below which satellite vents stay buried. */
    private static final double STACK_SKIP_STRENGTH = 0.05D;

    private static final long CELL_SEED = 0x564F4C43L; // "VOLC"
    private static final long CONE_SEED = 0x434F4E45L; // "CONE"
    private static final long PLAIN_SEED = 0x504C4E54L; // "PLNT"
    private static final long STACK_SEED = 0x5354434BL; // "STCK"

    private VolcanoGeometry() {
    }

    /** What pools at the bottom of a summit crater. */
    public enum CraterType {
        /** A still crater lake (火山口湖). */
        CRATER_LAKE,
        /** A sinter-rimmed hot spring pool (热泉). */
        HOT_SPRING,
        /** A dry sulfur pan dusted with fumarole vents (硫磺). */
        SULFUR_PAN
    }

    /**
     * One volcanic edifice. {@code baseRadius} is measured at the geological floor of the
     * summit column; heights scale with biome strength at shading time.
     */
    public record Volcano(int centerX, int centerZ,
                          int baseRadius, int height,
                          double craterRadiusRatio, int craterDepth, int rimHeight,
                          CraterType craterType, boolean breach, int breachAzimuthDeg,
                          int plugRadius, int plugHeight,
                          int fillDepth,
                          Parasite[] parasites,
                          int[] strata) {

        public int craterRadius() {
            return Math.max(6, (int) Math.round(baseRadius * craterRadiusRatio));
        }

        public int apronRadius() {
            return (int) Math.round(baseRadius * 1.55D);
        }

        // Records would otherwise compare the raw arrays by identity, breaking the
        // pure-geometry determinism contract that every chunk derives the same edifice.
        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Volcano volcano
                    && centerX == volcano.centerX
                    && centerZ == volcano.centerZ
                    && baseRadius == volcano.baseRadius
                    && height == volcano.height
                    && Double.compare(craterRadiusRatio, volcano.craterRadiusRatio) == 0
                    && craterDepth == volcano.craterDepth
                    && rimHeight == volcano.rimHeight
                    && craterType == volcano.craterType
                    && breach == volcano.breach
                    && breachAzimuthDeg == volcano.breachAzimuthDeg
                    && plugRadius == volcano.plugRadius
                    && plugHeight == volcano.plugHeight
                    && fillDepth == volcano.fillDepth
                    && Arrays.equals(parasites, volcano.parasites)
                    && Arrays.equals(strata, volcano.strata);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(centerX, centerZ, baseRadius, height, craterType,
                    breach, breachAzimuthDeg, plugRadius, plugHeight, fillDepth);
            result = 31 * result + Arrays.hashCode(parasites);
            return 31 * result + Arrays.hashCode(strata);
        }
    }

    /** A parasitic flank cone: an offset daughter vent on the main edifice. */
    public record Parasite(int offsetX, int offsetZ, int radius, int height) {
    }

    /**
     * Per-column shading plan. Non-null {@code volcano()} means this column is part of an
     * edifice (main cone, crater, dome, neck or parasitic cone); {@code apronHeight()} may
     * be positive even without one (talus run-out and ash plains). {@code channelStrength()}
     * is how deep inside a breached volcano's spill chute the column sits, 0 outside it.
     */
    public record ColumnShape(Volcano volcano, Parasite parasite,
                              double distance, double surfaceY,
                              double apronHeight, double fillY,
                              boolean craterInterior, double openTopY,
                              double channelStrength) {
        public boolean partOfEdifice() {
            return volcano != null;
        }
    }

    /**
     * A satellite vent of the plain: a scoria cone, spatter ridge or tuff mound. Elliptical
     * radii make some of them elongated dike-fed ramparts rather than round cones.
     */
    public record Stack(int centerX, int centerZ, double radiusX, double radiusZ,
                        double height, long seed) {
    }

    /**
     * Shading plan of the satellite vent under a column: the vent top Y and how close the
     * column sits to the vent axis ({@code axisFraction} 0 at the conduit, 1 at the rim).
     * {@code NONE} means no vent stands here.
     */
    public record StackShape(double topY, double axisFraction) {
        public static final StackShape NONE = new StackShape(Double.NEGATIVE_INFINITY, 2.0D);

        public boolean present() {
            return axisFraction <= 1.0D;
        }
    }

    /**
     * The pure per-column volcanic plan: everything the block shader needs to answer state
     * queries for one column. Deliberately free of Minecraft types so planning stays
     * unit-testable; the block states live in {@code VolcanicTerrain}.
     */
    public record VolcanicColumnPlan(int blockX, int blockZ, int floorY, double strength,
                                     ColumnShape shape,
                                     double stackTopY, double stackAxis) {

        /** Whether a satellite vent stands on this column. */
        public boolean hasStack() {
            return stackAxis <= 1.0D;
        }
    }

    /**
     * The volcanic plan of one column, or {@code null} outside the volcanic biome.
     * {@code maxRelief} caps how far cones may rise above the floor so summits keep clear
     * water below the reef overhead. Relief scales with {@code strength}, which ramps from
     * zero, so edifices grow out of the plain at the district fringe instead of popping in.
     */
    public static VolcanicColumnPlan columnPlan(double strength, int blockX, int blockZ,
                                                int floorY, double maxRelief,
                                                CellSource<Volcano> volcanoes,
                                                CellSource<Stack> stacks,
                                                EmergenceCurve satelliteEmergence) {
        if (strength <= 0.0D) {
            return null;
        }
        ColumnShape shape = shapeAt(blockX, blockZ, floorY, strength, maxRelief, volcanoes);
        // Satellite vents only erupt between the giants, never on an edifice's own flanks.
        StackShape satellite = shape.partOfEdifice()
                ? StackShape.NONE
                : stackShapeAt(blockX, blockZ, shape.surfaceY(), strength,
                        stacks, satelliteEmergence);
        return new VolcanicColumnPlan(blockX, blockZ, floorY, strength, shape,
                satellite.topY(), satellite.axisFraction());
    }

    /**
     * Swell-and-rift relief of the volcanic plain itself: broad seamount swells cut by
     * narrow graben trenches along the zero contour of a large noise field. Creative
     * terrain that never touches the cave ceiling.
     *
     * <p>The trench is a C1 {@link BlendMath#taper} window over the rift field: depth
     * reaches its maximum on the zero contour and both value and slope decay to exactly
     * zero at the window edge. The old hard band ({@code if |rift| < w then −(4+5·depth)})
     * left a constant 4-block step along the contour — a cliff-walled, machine-drawn
     * crack; the taper turns the same trench into a smooth-sided graben.</p>
     */
    public static double floorOffset(int blockX, int blockZ) {
        double swell = SoftMixNoise.valueNoise(blockX, blockZ, 120, PLAIN_SEED) * 6.5D
                + SoftMixNoise.valueNoise(blockX, blockZ, 44, PLAIN_SEED ^ 0x1234L) * 2.5D;
        double rift = SoftMixNoise.valueNoise(blockX, blockZ, 96, PLAIN_SEED ^ 0x5678L);
        double detail = SoftMixNoise.valueNoise(blockX, blockZ, 20, PLAIN_SEED ^ 0x9ABCL) * 1.5D;
        double trench = RIFT_DEPTH * BlendMath.taper(rift / RIFT_HALF_WIDTH);
        return swell + detail - trench;
    }

    /**
     * Smooth volcanic strength in [0, 1] from the biome-mix weight of Brimstone Caldera and
     * the region edge fade. Cone heights and aprons scale with this so districts blend.
     */
    public static double strength(double brimstoneWeight, double edgeFade) {
        return strength(brimstoneWeight, edgeFade, 0.55D);
    }

    /**
     * As {@link #strength(double, double)}, with a configurable full-relief weight: the
     * emergence window of the giant edifices. Relief grows from exactly zero at
     * {@link #MIN_STRENGTH}, so cones rise out of the plain instead of popping into
     * existence on the window contour.
     */
    public static double strength(double brimstoneWeight, double edgeFade, double fullWeight) {
        double span = Math.max(1e-6, fullWeight - MIN_STRENGTH);
        double district = SoftMixNoise.smoothstep((brimstoneWeight - MIN_STRENGTH) / span);
        return SoftMixNoise.clamp01(district) * SoftMixNoise.clamp01(edgeFade);
    }

    /** The volcano seeded for a field cell, or {@code null} when the cell stays quiet. */
    public static Volcano volcanoAt(int cellX, int cellZ) {
        long roll = SoftMixNoise.mix(cellX, cellZ, CELL_SEED);
        if (unit(roll, 0) >= ACTIVE_CHANCE) {
            return null;
        }
        int cx = cellX * FIELD_CELL + FIELD_CELL / 4 + (int) (unit(roll, 1) * (FIELD_CELL / 2.0D));
        int cz = cellZ * FIELD_CELL + FIELD_CELL / 4 + (int) (unit(roll, 2) * (FIELD_CELL / 2.0D));

        int baseRadius = 16 + (int) (unit(roll, 3) * 12.0D);            // 16..27 — compact massifs
        int height = 22 + (int) (unit(roll, 4) * 12.0D);                // 22..33 — modest summits
        double craterRatio = 0.30D + unit(roll, 5) * 0.16D;             // wide summit calderas
        int craterRadius = Math.max(6, (int) Math.round(baseRadius * craterRatio));
        int craterDepth = 6 + (int) (unit(roll, 6) * 7.0D);
        int rimHeight = 2 + (int) (unit(roll, 7) * 4.0D);
        CraterType craterType = switch ((int) (unit(roll, 8) * 3.0D)) {
            case 0 -> CraterType.CRATER_LAKE;
            case 1 -> CraterType.HOT_SPRING;
            default -> CraterType.SULFUR_PAN;
        };
        boolean breach = unit(roll, 9) < 0.34D;
        int breachAzimuthDeg = (int) (unit(roll, 15) * 360.0D);
        // The dome never chokes its own crater: a full ring of crater floor survives.
        int plugRadius = Math.max(3, Math.min(3 + (int) (unit(roll, 10) * 3.0D), craterRadius - 3));
        int plugHeight = breach
                ? 10 + (int) (unit(roll, 11) * 8.0D)                    // the neck stands proud of the rim
                : 4 + (int) (unit(roll, 11) * 7.0D);                    // resurgent dome in the crater
        int fillDepth = craterType == CraterType.SULFUR_PAN ? 0 : 1 + (int) (unit(roll, 12) * 3.0D);

        int parasiteCount = unit(roll, 13) < 0.22D ? 0 : 1 + (int) (unit(roll, 14) * 3.0D);
        Parasite[] parasites = new Parasite[parasiteCount];
        for (int i = 0; i < parasiteCount; i++) {
            double angle = unit(roll, 20 + i * 4) * Math.PI * 2.0D;
            double dist = (0.45D + unit(roll, 21 + i * 4) * 0.35D) * baseRadius;
            int radius = Math.max(3, (int) ((0.18D + unit(roll, 22 + i * 4) * 0.16D) * baseRadius));
            int pHeight = Math.max(4, (int) ((0.12D + unit(roll, 23 + i * 4) * 0.18D) * height));
            parasites[i] = new Parasite((int) Math.round(Math.cos(angle) * dist),
                    (int) Math.round(Math.sin(angle) * dist), radius, pHeight);
        }

        int[] strata = new int[6];
        for (int i = 0; i < strata.length; i++) {
            strata[i] = (int) (unit(roll, 40 + i) * 4.0D);              // 4 lithologies
        }
        return new Volcano(cx, cz, baseRadius, height, craterRatio, craterDepth, rimHeight,
                craterType, breach, breachAzimuthDeg, plugRadius, plugHeight, fillDepth,
                parasites, strata);
    }

    /**
     * The strongest edifice touching this column among the 3x3 neighbouring field cells
     * (main cones and their parasitic cones), or {@code null} on open plain.
     */
    public static ColumnShape shapeAt(int blockX, int blockZ, int floorY, double strength) {
        return shapeAt(blockX, blockZ, floorY, strength, Double.POSITIVE_INFINITY);
    }

    /**
     * As {@link #shapeAt(int, int, int, double)}, but every summit, rim and vent dome is
     * kept within {@code maxRelief} blocks above the geological floor so middle-sea
     * mountains never crowd the reef overhead.
     */
    public static ColumnShape shapeAt(int blockX, int blockZ, int floorY, double strength,
                                      double maxRelief) {
        return shapeAt(blockX, blockZ, floorY, strength, maxRelief, VolcanoGeometry::volcanoAt);
    }

    /**
     * Source-injected variant so a chunk build can memoize the edifice records.
     *
     * <p>There is deliberately no relief threshold here: every part of an edifice scales
     * with {@code strength}, which itself ramps from zero, so at the district fringe the
     * cones are flush mounds growing out of the plain. The old hard
     * {@code strength <= MIN_STRENGTH} gate made each edifice pop into existence at a
     * quarter of its height along a contour line — a cliff ring around every volcano.</p>
     */
    public static ColumnShape shapeAt(int blockX, int blockZ, int floorY, double strength,
                                      double maxRelief, CellSource<Volcano> volcanoes) {
        if (strength <= EDIFICE_SKIP_STRENGTH) {
            return plainShape(blockX, blockZ, floorY, strength);
        }
        int cellX = Math.floorDiv(blockX, FIELD_CELL);
        int cellZ = Math.floorDiv(blockZ, FIELD_CELL);
        ColumnShape best = plainShape(blockX, blockZ, floorY, strength);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Volcano volcano = volcanoes.at(cellX + dx, cellZ + dz);
                if (volcano == null) {
                    continue;
                }
                ColumnShape candidate = edificeShape(volcano, null, blockX, blockZ, floorY, strength, maxRelief);
                if (candidate != null && candidate.surfaceY > best.surfaceY) {
                    best = candidate;
                }
                for (Parasite parasite : volcano.parasites()) {
                    ColumnShape flank = edificeShape(volcano, parasite, blockX, blockZ, floorY, strength, maxRelief);
                    if (flank != null && flank.surfaceY > best.surfaceY) {
                        best = flank;
                    }
                }
            }
        }
        return best;
    }

    private static ColumnShape plainShape(int blockX, int blockZ, int floorY, double strength) {
        double apron = SoftMixNoise.valueNoise(blockX, blockZ, 28, PLAIN_SEED ^ 0x7777L);
        double apronHeight = apron > 0.55D ? (apron - 0.55D) * 8.0D * strength : 0.0D;
        double surfaceY = floorY + apronHeight;
        return new ColumnShape(null, null, Double.MAX_VALUE, surfaceY, apronHeight,
                Double.NEGATIVE_INFINITY, false, surfaceY, 0.0D);
    }

    /** Package-private for tests: the shading plan of exactly one cone or flank cone. */
    static ColumnShape edificeShape(Volcano volcano, Parasite parasite,
                                    int blockX, int blockZ, int floorY, double strength) {
        return edificeShape(volcano, parasite, blockX, blockZ, floorY, strength,
                Double.POSITIVE_INFINITY);
    }

    /** Package-private for tests: the shading plan of exactly one cone or flank cone. */
    static ColumnShape edificeShape(Volcano volcano, Parasite parasite,
                                    int blockX, int blockZ, int floorY, double strength,
                                    double maxRelief) {
        int centerX = volcano.centerX() + (parasite == null ? 0 : parasite.offsetX());
        int centerZ = volcano.centerZ() + (parasite == null ? 0 : parasite.offsetZ());
        int baseRadius = parasite == null ? volcano.baseRadius() : parasite.radius();
        int height = parasite == null ? volcano.height() : parasite.height();
        int craterRadius = parasite == null ? volcano.craterRadius() : Math.max(2, parasite.radius() / 3);
        int craterDepth = parasite == null ? volcano.craterDepth() : Math.max(2, volcano.craterDepth() / 3);
        int rimHeight = parasite == null ? volcano.rimHeight() : Math.max(1, volcano.rimHeight() / 2);

        double dx = blockX - centerX;
        double dz = blockZ - centerZ;
        double angle = Math.atan2(dz, dx);
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Radial buttresses and re-entrants: the outline of a stratovolcano is never a
        // circle, it is a star of spurs separated by eroded re-entrants.
        double spurPhase = volcano.centerX() * 0.37D;
        double massif = 1.0D
                + 0.10D * Math.sin(3.0D * angle + spurPhase)
                + 0.065D * Math.sin(7.0D * angle + volcano.centerZ() * 0.53D)
                + 0.040D * Math.sin(11.0D * angle + volcano.baseRadius() * 0.71D);
        double effectiveRadius = baseRadius * massif;
        if (distance > effectiveRadius * 1.65D) {
            return null;
        }

        double scaledHeight = height * strength;
        double t = Math.min(1.0D, distance / effectiveRadius);
        // Concave stratovolcano profile: steep near the vent, flaring into the talus apron.
        double profile = Math.pow(1.0D - t, 1.55D);
        double surfaceY = floorY + scaledHeight * profile;

        // Barrancos: erosion gullies raking down the flanks, deepening away from the summit.
        double gullyPhase = volcano.centerX() * 0.017D + volcano.centerZ() * 0.031D
                + volcano.baseRadius() * 0.013D;
        double gullyWave = Math.sin(5.0D * angle + gullyPhase) * 0.55D
                + Math.sin(9.0D * angle + gullyPhase * 1.7D) * 0.45D;
        double gully = gullyWave < 0.0D ? gullyWave * gullyWave : 0.0D;
        surfaceY -= scaledHeight * 0.20D * gully * SoftMixNoise.smoothstep(t);

        // Breached volcanoes: a notch is torn out of the rim and a spill channel (the
        // sciara) runs down one flank — Stromboli's Sciara del Fuoco in miniature.
        double breach = parasite == null ? breachFactor(volcano, angle) : 0.0D;
        double channelStrength = 0.0D;

        double craterRadiusScaled = craterRadius * (0.55D + 0.45D * strength);
        double u = distance / craterRadiusScaled;
        if (u < 1.35D) {
            double bowl = u >= 1.0D ? 0.0D : Math.pow(1.0D - u * u, 1.2D);
            double rim = Math.max(0.0D, 1.0D - Math.abs(u - 1.0D) / 0.35D);
            surfaceY -= craterDepth * strength * bowl * (1.0D - 0.45D * breach);
            surfaceY += rimHeight * (1.0D - 0.9D * breach) * SoftMixNoise.smoothstep(rim);
        }
        if (breach > 0.0D && distance > craterRadiusScaled * 0.8D) {
            double runOut = SoftMixNoise.clamp01(
                    (distance - craterRadiusScaled * 0.8D) / (effectiveRadius * 0.95D));
            double channel = (3.5D + craterDepth * 0.7D) * strength * breach
                    * (1.0D - runOut * runOut);
            surfaceY -= channel;
            channelStrength = breach * (1.0D - runOut);
        }

        double fillY = Double.NEGATIVE_INFINITY;
        boolean craterInterior = false;
        double openTopY = Double.NEGATIVE_INFINITY;
        if (parasite == null) {
            double domeRadius = domeRadius(volcano, strength);
            double domeTop = plugTopY(volcano, floorY, strength);
            if (distance <= domeRadius) {
                double flute = 1.0D + 0.10D * Math.sin(6.0D * angle + gullyPhase * 2.3D);
                double v = Math.min(1.0D, distance / (domeRadius * flute));
                double domeBase = floorY + scaledHeight
                        * (volcano.craterType() == CraterType.SULFUR_PAN ? 0.30D : 0.42D);
                double dome = domeBase + (domeTop - domeBase) * Math.pow(1.0D - v * v, 0.55D);
                surfaceY = Math.max(surfaceY, dome);
            } else if (distance <= craterRadiusScaled * 0.9D) {
                // The ring of crater floor between the vent and the caldera wall keeps its
                // pool and the open water standing above it.
                craterInterior = true;
                fillY = surfaceY + volcano.fillDepth() * (0.5D + 0.5D * strength);
                openTopY = surfaceY + craterDepth * strength * (1.0D - 0.45D * breach)
                        + rimHeight + 2.0D;
            }
        } else {
            // Daughter cones carry their own little crater, shallower than the parent's.
            double parasiteCrater = craterRadius * (0.55D + 0.45D * strength) * 0.9D;
            if (distance <= parasiteCrater) {
                craterInterior = true;
                fillY = surfaceY + 0.5D;
                openTopY = surfaceY + craterDepth * strength + rimHeight + 1.0D;
            }
        }

        double apronDistance = distance - effectiveRadius;
        if (apronDistance > 0.0D) {
            // Lobate debris tongues: the run-out varies with the azimuth like real
            // volcaniclastic fans instead of ending on a perfect circle.
            double lobe = 1.0D + 0.28D * Math.sin(4.0D * angle + gullyPhase * 1.3D)
                    + 0.14D * Math.sin(7.0D * angle + gullyPhase);
            double runOut = SoftMixNoise.clamp01(
                    1.0D - apronDistance / (effectiveRadius * 0.72D * Math.max(0.55D, lobe)));
            double apronHeight = 4.5D * runOut * runOut * strength;
            surfaceY = Math.max(surfaceY, floorY + apronHeight);
        }
        // Keep the whole edifice under the mountain ceiling: one uniform vertical squeeze
        // per volcano preserves the profile — peaks shrink, they never get sliced flat.
        double scale = verticalScale(volcano, floorY, strength, maxRelief);
        if (scale < 1.0D) {
            surfaceY = floorY + (surfaceY - floorY) * scale;
            if (fillY > Double.NEGATIVE_INFINITY) {
                fillY = floorY + (fillY - floorY) * scale;
            }
            if (openTopY > Double.NEGATIVE_INFINITY) {
                openTopY = floorY + (openTopY - floorY) * scale;
            }
        }
        return new ColumnShape(volcano, parasite, distance, surfaceY,
                Math.max(0.0D, surfaceY - floorY), fillY, craterInterior, openTopY,
                channelStrength);
    }

    /**
     * Uniform vertical scale factor that keeps every part of the edifice within
     * {@code maxRelief} blocks of the floor. The raw bound covers the tallest of the
     * cone-and-rim profile and the vent dome (including breach necks that would
     * otherwise pierce the reef).
     */
    static double verticalScale(Volcano volcano, int floorY, double strength, double maxRelief) {
        if (!Double.isFinite(maxRelief)) {
            return 1.0D;
        }
        double plugRelief = plugTopY(volcano, floorY, strength) - floorY;
        double rawMax = Math.max(volcano.height() * strength + volcano.rimHeight(), plugRelief) + 2.0D;
        return rawMax <= 0.0D ? 1.0D : SoftMixNoise.clamp01(maxRelief / rawMax);
    }

    /** 1 inside the notch torn out of a breached rim, fading to 0 outside it. */
    static double breachFactor(Volcano volcano, double angle) {
        if (!volcano.breach()) {
            return 0.0D;
        }
        double target = Math.toRadians(volcano.breachAzimuthDeg());
        double delta = angle - target;
        while (delta > Math.PI) {
            delta -= Math.PI * 2.0D;
        }
        while (delta < -Math.PI) {
            delta += Math.PI * 2.0D;
        }
        if (Math.abs(delta) >= BREACH_NOTCH_RADIANS) {
            return 0.0D;
        }
        return SoftMixNoise.smoothstep(1.0D - Math.abs(delta) / BREACH_NOTCH_RADIANS);
    }

    /**
     * Radius of the summit vent at the given strength. The vent is always a broad dome or
     * neck — wide enough that the volcano reads as a massif, never as a lone pillar.
     */
    public static double domeRadius(Volcano volcano, double strength) {
        double craterRadiusScaled = volcano.craterRadius() * (0.55D + 0.45D * strength);
        double plugRadius = volcano.plugRadius() * (0.7D + 0.3D * strength);
        return Math.max(2.0D, Math.min(craterRadiusScaled * 0.66D, plugRadius * 2.1D));
    }

    /**
     * Top Y of the summit vent. The neck never overtops its own cone — relief stays
     * proportional so the whole edifice squashes as one shape under the mountain line —
     * but a breach neck still stands proud of the crater rim, the conduit that runs out
     * through the breached flank. A resurgent dome stays inside its crater.
     */
    public static double plugTopY(Volcano volcano, double floorY, double strength) {
        double scaledHeight = volcano.height() * strength;
        double plugBase = floorY + (volcano.craterType() == CraterType.SULFUR_PAN
                ? scaledHeight * 0.35D
                : scaledHeight * 0.55D);
        double plugTop = Math.min(plugBase + volcano.plugHeight() * strength,
                floorY + scaledHeight);
        if (volcano.breach()) {
            plugTop = Math.max(plugTop, floorY + scaledHeight * 0.85D);
        }
        return plugTop;
    }

    /**
     * Lithology band index (into {@link Volcano#strata()}) for a depth below the cone
     * surface. Bands dip gently outward like real volcanic strata.
     */
    public static int strataIndex(Volcano volcano, double distance, double depthBelowSurface) {
        double dip = distance * 0.08D;
        int band = (int) Math.floor((depthBelowSurface + dip) / 4.5D);
        int[] table = volcano.strata();
        return table[Math.floorMod(band, table.length)];
    }

    /** Shallow weathered cap of a cone: 0 scoria, 1 ash, 2 sulfur crust, 3 agglomerate. */
    public static int surfaceLithology(int blockX, int blockZ, double distance, Volcano volcano) {
        double summitBias = volcano == null ? 0.0D
                : 1.0D - Math.min(1.0D, distance / Math.max(1.0D, (double) volcano.baseRadius()));
        double patch = SoftMixNoise.valueNoise(blockX, blockZ, 7, CONE_SEED ^ 0x3333L);
        if (summitBias > 0.62D && patch > 0.35D) {
            return 2; // sulfur crust fuming near the vents
        }
        if (patch > 0.62D) {
            return 3;
        }
        return patch < -0.25D ? 1 : 0;
    }

    /** Probability weight [0,1] that a summit-rim column carries a fumarole vent. */
    public static double fumaroleChance(int blockX, int blockZ, Volcano volcano, double distance) {
        if (volcano == null) {
            return 0.0D;
        }
        int craterRadius = volcano.craterRadius();
        double ring = Math.abs(distance - craterRadius) / Math.max(1.0D, craterRadius * 0.8D);
        if (ring > 1.0D) {
            return 0.0D;
        }
        double rim = 1.0D - SoftMixNoise.smoothstep(ring);
        return rim * (0.35D + 0.4D * SoftMixNoise.valueNoise(blockX, blockZ, 5, CONE_SEED ^ 0x4444L));
    }

    /**
     * Layers of the breach rubble apron spilled across the plain around a breach neck:
     * returns the rubble top Y for this column, or 0 when outside.
     */
    public static double breachRubbleTop(Volcano volcano, double distance, double plugTopY, double strength) {
        if (volcano == null || !volcano.breach() || strength < 0.5D) {
            return 0.0D;
        }
        double inner = volcano.plugRadius();
        double outer = inner * 2.6D;
        if (distance < inner || distance > outer) {
            return 0.0D;
        }
        double runOut = 1.0D - (distance - inner) / (outer - inner);
        return plugTopY - 3.0D + runOut * 2.5D;
    }

    /** The satellite vent seeded for a stack cell, or {@code null} on quiet plain. */
    public static Stack stackAt(int cellX, int cellZ) {
        long roll = SoftMixNoise.mix(cellX, cellZ, STACK_SEED);
        if (unit(roll, 0) >= STACK_CHANCE) {
            return null;
        }
        int cx = cellX * STACK_CELL + STACK_CELL / 5 + (int) (unit(roll, 1) * STACK_CELL * 0.6D);
        int cz = cellZ * STACK_CELL + STACK_CELL / 5 + (int) (unit(roll, 2) * STACK_CELL * 0.6D);
        double radiusX = 3.5D + unit(roll, 3) * 5.0D;
        // Dike-fed ramparts are elongated; ash cones stay round.
        double radiusZ = radiusX * (0.55D + unit(roll, 4) * 1.05D);
        double height = 2.5D + unit(roll, 5) * 6.0D;
        return new Stack(cx, cz, radiusX, radiusZ, height, (long) (unit(roll, 6) * 1_000_000.0D));
    }

    /**
     * Shading plan of the satellite vent (scoria cone, spatter ridge or tuff mound) under a
     * column, or {@link StackShape#NONE}. Cones are truncated with a shallow crown basin,
     * so the plain reads as a field of small volcanoes rather than scattered pillars.
     */
    public static StackShape stackShapeAt(int blockX, int blockZ, double groundY, double strength) {
        return stackShapeAt(blockX, blockZ, groundY, strength,
                VolcanoGeometry::stackAt, EmergenceCurve.DEFAULT_SATELLITE);
    }

    /**
     * Source- and curve-injected variant. The emergence curve replaces the old hard
     * {@code strength < 0.35} gate: satellite relief grows from zero at the window start,
     * so vents bud out of the plain instead of appearing full-sized on a contour.
     */
    public static StackShape stackShapeAt(int blockX, int blockZ, double groundY, double strength,
                                          CellSource<Stack> stacks, EmergenceCurve emergence) {
        if (strength <= STACK_SKIP_STRENGTH) {
            return StackShape.NONE;
        }
        double relief = strength * emergence.apply(strength);
        if (relief <= 1e-3D) {
            return StackShape.NONE;
        }
        int cellX = Math.floorDiv(blockX, STACK_CELL);
        int cellZ = Math.floorDiv(blockZ, STACK_CELL);
        double bestTop = Double.NEGATIVE_INFINITY;
        double bestAxis = 2.0D;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Stack stack = stacks.at(cellX + dx, cellZ + dz);
                if (stack == null) {
                    continue;
                }
                double nx = (blockX - stack.centerX()) / stack.radiusX();
                double nz = (blockZ - stack.centerZ()) / stack.radiusZ();
                double radius = Math.sqrt(nx * nx + nz * nz);
                if (radius >= 1.6D) {
                    continue;
                }
                double theta = Math.atan2(nz, nx);
                double lobed = 1.0D + 0.16D * Math.sin(3.0D * theta + stack.seed() * 0.017D)
                        + 0.09D * Math.sin(5.0D * theta + stack.seed() * 0.031D);
                double t = radius / lobed;
                if (t >= 1.0D) {
                    continue;
                }
                double profile = Math.pow(1.0D - t * t, 0.72D);
                if (t < 0.30D) {
                    // Truncated crown: a shallow summit basin, not a needle.
                    profile = Math.pow(1.0D - 0.09D, 0.72D) * 0.86D;
                }
                double top = groundY + stack.height() * relief * profile;
                if (top > bestTop) {
                    bestTop = top;
                    bestAxis = t;
                }
            }
        }
        return bestTop > Double.NEGATIVE_INFINITY
                ? new StackShape(bestTop, bestAxis) : StackShape.NONE;
    }

    /** Deterministic unit hash in [0, 1) for one volcano seed and a draw index. */
    private static double unit(long roll, int draw) {
        long mixed = SoftMixNoise.mix((int) (roll & 0x7FFFFFFFL), draw, CELL_SEED ^ 0x9E37L);
        return ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    /** Exposed for tests: the strata table is bounded to the four volcanic lithologies. */
    public static boolean strataTableValid(Volcano volcano) {
        return volcano.strata().length == 6
                && Arrays.stream(volcano.strata()).allMatch(i -> i >= 0 && i < 4);
    }
}
