package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.layers.SoftMixNoise;

/**
 * Low sedimentary massifs of the quiet middle sea: mesas, ridges, mounds and the odd
 * sugarloaf spire scattered in patch regions. Pure functions of world position, so every
 * chunk derives the same silhouette and the seams cannot step.
 *
 * <p>Unlike the old pillar outcrops — uniform columns at one chamber height — each
 * edifice carries its own kind, footprint and height: flat-topped mesas with terraced
 * cliffs, elongated ridges whose crests rise and fall through saddles, broad talus
 * mounds, and rare steep spires. All heights live in the lower half of the chamber so
 * the reef ceiling always keeps a wide band of open water above the summits.</p>
 *
 * <p>The class is intentionally free of Minecraft types so the shape maths stays
 * unit-testable.</p>
 */
public final class OutcropGeometry {
    /** Field cell size in blocks: one outcrop candidate per cell. */
    public static final int CELL = 60;
    /** Fraction of cells that carry an outcrop at all. */
    public static final double ACTIVE_CHANCE = 0.55D;
    /** Terrace step of mesa cliffs, in blocks. */
    public static final int TERRACE_STEP = 3;

    private static final long CELL_SEED = 0x0C7C0975L;

    /** Silhouette family of one massif. */
    public enum Kind {
        /** Flat-topped butte with steep terraced cliffs. */
        MESA,
        /** Elongated crest with saddles along its spine. */
        RIDGE,
        /** Broad talus cone, gentle all round. */
        MOUND,
        /** Rare steep sugarloaf. */
        SPIRE
    }

    /**
     * One massif: elliptical footprint rotated by {@code angleRad}, summit plateau
     * fraction {@code plateauRatio} (mesas), and a crest phase for ridge saddles.
     * {@code heightRatio} is the summit relief as a fraction of the chamber height.
     */
    public record Outcrop(Kind kind, int centerX, int centerZ,
                          double radiusA, double radiusB, double angleRad,
                          double heightRatio, double plateauRatio, double crestPhase) {
    }

    private OutcropGeometry() {
    }

    /** The massif seeded for a field cell, or {@code null} when the cell stays flat. */
    public static Outcrop outcropAt(int cellX, int cellZ) {
        long roll = SoftMixNoise.mix(cellX, cellZ, CELL_SEED);
        if (unit(roll, 0) >= ACTIVE_CHANCE) {
            return null;
        }
        int cx = cellX * CELL + CELL / 4 + (int) (unit(roll, 1) * (CELL / 2.0D));
        int cz = cellZ * CELL + CELL / 4 + (int) (unit(roll, 2) * (CELL / 2.0D));
        double kindRoll = unit(roll, 3);
        Kind kind;
        if (kindRoll < 0.30D) {
            kind = Kind.MESA;
        } else if (kindRoll < 0.60D) {
            kind = Kind.RIDGE;
        } else if (kindRoll < 0.85D) {
            kind = Kind.MOUND;
        } else {
            kind = Kind.SPIRE;
        }

        double radiusA;
        double radiusB;
        double heightRatio;
        double plateau;
        switch (kind) {
            case MESA -> {
                radiusA = 9.0D + unit(roll, 4) * 6.0D;
                radiusB = radiusA * (0.75D + unit(roll, 5) * 0.35D);
                heightRatio = 0.12D + unit(roll, 6) * 0.08D;
                plateau = 0.40D + unit(roll, 7) * 0.15D;
            }
            case RIDGE -> {
                radiusA = 13.0D + unit(roll, 4) * 9.0D;
                radiusB = radiusA * (0.26D + unit(roll, 5) * 0.18D);
                heightRatio = 0.10D + unit(roll, 6) * 0.07D;
                plateau = 0.0D;
            }
            case SPIRE -> {
                radiusA = 4.0D + unit(roll, 4) * 3.0D;
                radiusB = radiusA * (0.80D + unit(roll, 5) * 0.30D);
                heightRatio = 0.15D + unit(roll, 6) * 0.09D;
                plateau = 0.0D;
            }
            default -> {
                radiusA = 10.0D + unit(roll, 4) * 7.0D;
                radiusB = radiusA * (0.85D + unit(roll, 5) * 0.30D);
                heightRatio = 0.07D + unit(roll, 6) * 0.05D;
                plateau = 0.0D;
            }
        }
        return new Outcrop(kind, cx, cz, radiusA, radiusB, unit(roll, 8) * Math.PI * 2.0D,
                heightRatio, plateau, unit(roll, 9) * Math.PI * 2.0D);
    }

    /**
     * Top Y of the massif surface at a column, clamped to {@code mountainTopY}, or
     * {@link Integer#MIN_VALUE} where no outcrop reaches. The strongest of the 3x3
     * neighbouring field cells wins, exactly like the volcanic field.
     */
    public static int topYAt(int blockX, int blockZ, int cavityFloorY, int cavityHeight,
                             int mountainTopY) {
        int cellX = Math.floorDiv(blockX, CELL);
        int cellZ = Math.floorDiv(blockZ, CELL);
        double best = 0.0D;
        boolean any = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Outcrop outcrop = outcropAt(cellX + dx, cellZ + dz);
                if (outcrop == null) {
                    continue;
                }
                double relief = reliefAt(outcrop, blockX, blockZ, cavityHeight,
                        mountainTopY - cavityFloorY);
                if (relief > best) {
                    best = relief;
                    any = true;
                }
            }
        }
        if (!any) {
            return Integer.MIN_VALUE;
        }
        return cavityFloorY + (int) Math.round(best);
    }

    private static double reliefAt(Outcrop outcrop, int blockX, int blockZ,
                                   double cavityHeight, double maxRelief) {
        double dx = blockX - outcrop.centerX();
        double dz = blockZ - outcrop.centerZ();
        double cos = Math.cos(outcrop.angleRad());
        double sin = Math.sin(outcrop.angleRad());
        double ax = (dx * cos + dz * sin) / outcrop.radiusA();
        double az = (-dx * sin + dz * cos) / outcrop.radiusB();
        double t = Math.sqrt(ax * ax + az * az);
        if (t >= 1.3D) {
            return 0.0D;
        }

        double height = outcrop.heightRatio() * cavityHeight;
        double relief;
        switch (outcrop.kind()) {
            case MESA -> {
                if (t <= outcrop.plateauRatio()) {
                    relief = height;
                } else {
                    double u = (t - outcrop.plateauRatio()) / (1.0D - outcrop.plateauRatio());
                    relief = height * Math.max(0.0D, 1.0D - u * 1.05D);
                    // Terraced cliffs: the beds step back in floors of TERRACE_STEP blocks.
                    relief = Math.round(relief / TERRACE_STEP) * TERRACE_STEP;
                }
            }
            case RIDGE -> {
                double crest = Math.pow(Math.max(0.0D, 1.0D - t * t), 0.7D);
                // Saddles rise and fall along the spine so the crest is never a plain bar.
                double spine = 0.72D + 0.28D * Math.sin(2.2D * ax * outcrop.radiusA()
                        * 0.08D + outcrop.crestPhase());
                relief = height * crest * spine;
            }
            case SPIRE -> relief = height * Math.pow(Math.max(0.0D, 1.0D - t), 0.55D);
            default -> {
                relief = height * Math.pow(Math.max(0.0D, 1.0D - t), 1.3D);
                // Talus skirt: the cone spreads into a low run-out apron.
                if (t > 0.85D) {
                    relief = Math.max(relief, height * 0.14D * (1.3D - t) / 0.45D);
                }
            }
        }
        return Math.min(relief, maxRelief);
    }

    /** Deterministic unit hash in [0, 1) for one cell roll and a draw index. */
    private static double unit(long roll, int draw) {
        long mixed = SoftMixNoise.mix((int) (roll & 0x7FFFFFFFL), draw, CELL_SEED ^ 0x9E37L);
        return ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }
}
