package com.dexer.aquanaut.common.worldgen.crystalnest;

/**
 * The decoration skin of the crystal nest: a pure, chunk-seamless pass that walks the
 * skinned terrain once and dresses every surface.
 *
 * <p>
 * Coverage rules, in claim order:
 * <ol>
 *   <li><b>Algae mats</b> — every upward-facing surface of the terrain (lattice rock and
 *       legacy floor alike) is carpeted with one layer of algae vegetation.</li>
 *   <li><b>Crystal columns and fringes</b> — shards hang and columns grow where a
 *       <b>晶巢岩 ceiling</b> allows; both are crystal and obey the support rule.</li>
 *   <li><b>Face decor</b> — every exposed face may grow a cluster pointing away from its
 *       support ({@code facing} is the growth direction). Open surfaces grow only the
 *       quiet rock-hued trio — white, smoky and amethyst crystals; chamber surfaces grow
 *       crystals of every colour, with the glowing resonant crystals and life gems kept
 *       rare among them.</li>
 * </ol>
 *
 * <p>
 * <b>Crystal support rule:</b> crystals (clusters, columns, sprouts, fringe) only root in
 * 晶巢岩 — the lattice rock. The druse crust yields to crystal roots: where a crystal
 * claims a druse-lined face, the support stays plain nest stone. Legacy terrain, mats and
 * partial blocks never carry crystal; a mat may carry algae tufts (vegetation, not
 * crystal) and nothing else.
 *
 * <p>
 * All rolls hash world coordinates, chains only ever grow along their own column, and
 * every support lookup sees the padded neighborhood, so decisions are pure functions of
 * position: chunk borders cannot seam. The output is a {@link Cell} grid which the
 * worldgen bridge maps onto real block states.
 */
public final class CrystalNestSkin {
    public enum Kind {
        /** Leave the block to the regular middle-sea column planner. */
        LEGACY,
        /** Plain water: no opinion, fall through to the column planner. */
        WATER,
        /** Crystal-nest stone: the rock of the skinned cores and struts. */
        ROCK,
        /** Chamber lining: druse-crusted nest stone against a hollow. */
        DRUSE,
        /** One whole solid block of algae turf carpeting a top surface. */
        ALGAE,
        /** Algae strands standing on a mat. */
        TUFT,
        /** A tiny crystal sprig rooted in 晶巢岩. */
        SPROUT,
        /** A crystal column spire (always vertical); variant 0. */
        COLUMN,
        /** A crystal cluster; variant is the material, facing the growth direction. */
        CLUSTER,
        /** A drooping crystal fringe strand; variant is TOP/BODY/TAIL. */
        FRINGE
    }

    /** Cluster materials: the first five never glow, the last two carry glowmasks. */
    public static final int CLUSTER_WHITE = 0;
    public static final int CLUSTER_ROSE = 1;
    public static final int CLUSTER_AMETHYST = 2;
    public static final int CLUSTER_AQUA = 3;
    public static final int CLUSTER_SMOKY = 4;
    public static final int CLUSTER_RESONANT = 5;
    public static final int CLUSTER_LIFE = 6;

    /** Fringe chain parts, matching the drooping seaweed part order. */
    public static final int FRINGE_TOP = 0;
    public static final int FRINGE_BODY = 1;
    public static final int FRINGE_TAIL = 2;

    /** Growth/attachment directions as vanilla 3D data values: D U N S W E. */
    public static final int DIR_DOWN = 0;
    public static final int DIR_UP = 1;
    public static final int DIR_NORTH = 2;
    public static final int DIR_SOUTH = 3;
    public static final int DIR_WEST = 4;
    public static final int DIR_EAST = 5;

    public static final int SIZE = 16;
    public static final int PAD = 1;

    private static final long SALT_MAT = 0x4D41544341525001L;
    private static final long SALT_HANG_COLUMN = 0x48414E47434F4C01L;
    private static final long SALT_HANG_COLUMN_H = 0x48414E47434F4C02L;
    private static final long SALT_FRINGE = 0x4652494E47455001L;
    private static final long SALT_FRINGE_LEN = 0x4652494E47455002L;
    private static final long SALT_DECOR = 0x4445434F52434C01L;
    private static final long SALT_DECOR_KIND = 0x4445434F52434C02L;
    private static final long SALT_DECOR_COLOR = 0x4445434F52434C03L;

    /** Surface faces get mats at full strength once the biome fade passes this. */
    private static final double MAT_FULL_STRENGTH = 0.45D;
    private static final double HANG_COLUMN_CHAMBER_CHANCE = 0.11D;
    private static final double HANG_COLUMN_OPEN_CHANCE = 0.03D;
    private static final double FRINGE_CHAMBER_CHANCE = 0.34D;
    private static final double FRINGE_OPEN_CHANCE = 0.13D;
    private static final double DECOR_CHAMBER_CHANCE = 0.45D;
    private static final double DECOR_OPEN_CHANCE = 0.28D;
    private static final double DECOR_FULL_STRENGTH = 0.5D;
    /**
     * Chamber context is a property of the geometry, not of the biome fade: surfaces are
     * classified against the full-strength chambers so border thinning cannot flip a
     * wall's crystal palette.
     */
    private static final double FULL_STRENGTH_CONTEXT = 1.0D;

    public record Cell(Kind kind, int variant, int facing) {
        public static final Cell LEGACY = new Cell(Kind.LEGACY, 0, 0);
        public static final Cell WATER = new Cell(Kind.WATER, 0, 0);
        public static final Cell ROCK = new Cell(Kind.ROCK, 0, 0);
        public static final Cell DRUSE = new Cell(Kind.DRUSE, 0, 0);
        public static final Cell TUFT = new Cell(Kind.TUFT, 0, 0);
        public static final Cell ALGAE = new Cell(Kind.ALGAE, 0, 0);

        public static Cell cluster(int material, int facing) {
            return new Cell(Kind.CLUSTER, material, facing);
        }

        public static Cell fringe(int part) {
            return new Cell(Kind.FRINGE, part, 0);
        }
    }

    /**
     * Terrain solidity outside the lattice — the regular middle-sea floor, cap and pillars.
     * Implementations must answer for whole, opaque, non-translucent blocks only. This is
     * the substrate of mats and vegetation: crystal never roots here, only on 晶巢岩.
     */
    public interface SolidQuery {
        boolean solid(int x, int y, int z);
    }

    /** Continuous crystal-nest strength of a block column (0 = neighboring biome). */
    public interface StrengthQuery {
        double strength(int x, int z);
    }

    /** The skinned lattice geometry the decoration pass dresses ({@link CrystalNestField}). */
    public interface Field {
        int OPEN = 0;
        int ROCK = 1;
        int DRUSE = 2;

        /** Rock classification of the skinned lattice at its fade strength. */
        int rockAt(int x, int y, int z, double strength);

        /** Whether the block lies inside a core chamber (crystal-growth context). */
        boolean chamberAt(int x, int y, int z);
    }

    private CrystalNestSkin() {
    }

    /**
     * Dresses a 16×16 chunk footprint whose cells span {@code [yLo, yLo + ySize)} and
     * returns the cells indexed {@code (x * 16 + z) * ySize + (y - yLo)}.
     */
    public static Cell[] skin(int minX, int minZ, int yLo, int ySize,
                              SolidQuery legacySolid, Field field,
                              StrengthQuery strengthQuery) {
        int padded = SIZE + PAD * 2;
        boolean[] solid = new boolean[padded * padded * ySize];
        boolean[] chamber = new boolean[padded * padded * ySize];
        double[] strength = new double[padded * padded];
        Cell[] cells = new Cell[padded * padded * ySize];

        // --- pass 1: occupancy of the skinned lattice plus the legacy terrain ----------
        // One field probe per cell: the lattice rock and its druse lining come back
        // together with the chamber flag the decoration passes reuse.
        for (int px = 0; px < padded; px++) {
            int x = minX - PAD + px;
            for (int pz = 0; pz < padded; pz++) {
                int z = minZ - PAD + pz;
                double s = strengthQuery.strength(x, z);
                strength[px * padded + pz] = s;
                for (int py = 0; py < ySize; py++) {
                    int y = yLo + py;
                    int index = (px * padded + pz) * ySize + py;
                    boolean legacyHere = legacySolid.solid(x, y, z);
                    int rock = s > 0.0D ? field.rockAt(x, y, z, s) : Field.OPEN;
                    boolean nestSolid = rock != Field.OPEN;
                    solid[index] = legacyHere || nestSolid;
                    if (legacyHere) {
                        // The regular planner already draws the floor, cap and pillars.
                        cells[index] = Cell.LEGACY;
                    } else if (rock == Field.DRUSE) {
                        cells[index] = Cell.DRUSE;
                    } else if (nestSolid) {
                        cells[index] = Cell.ROCK;
                    } else {
                        cells[index] = Cell.WATER;
                        chamber[index] = field.chamberAt(x, y, z);
                    }
                }
            }
        }

        // --- pass 2: every upward-facing surface gets its algae mat ---------------------
        for (int px = 0; px < padded; px++) {
            int x = minX - PAD + px;
            for (int pz = 0; pz < padded; pz++) {
                int z = minZ - PAD + pz;
                double s = strength[px * padded + pz];
                double matGate = Math.min(1.0D, s / MAT_FULL_STRENGTH);
                for (int py = 1; py < ySize; py++) {
                    int y = yLo + py;
                    int index = (px * padded + pz) * ySize + py;
                    if (cells[index] != Cell.WATER || !solid[index - 1]) {
                        continue;
                    }
                    if (CrystalNestNoise.unit(x, y, z, SALT_MAT) >= matGate) {
                        continue;
                    }
                    cells[index] = Cell.ALGAE;
                }
            }
        }

        // Every pass runs over the padded grid: a neighbour chunk must see the same mats,
        // growth chains and crystal-support claims when it tests faces at the border, or
        // seams appear. This includes the face decor — its druse-yield claim mutates the
        // backing cell, so pad-positioned decor decides the fate of interior rock and must
        // be computed identically by every chunk that sees it. Only the interior 16x16 is
        // exported — each chunk writes its own cells.
        for (int px = 0; px < padded; px++) {
            int x = minX - PAD + px;
            for (int pz = 0; pz < padded; pz++) {
                int z = minZ - PAD + pz;
                double s = strength[px * padded + pz];
                for (int py = 0; py < ySize; py++) {
                    int y = yLo + py;
                    int index = (px * padded + pz) * ySize + py;

                    // --- pass 3: anchored growths along the column ---------------------
                    Cell current = cells[index];
                    Cell above = py + 1 < ySize ? cells[index + 1] : null;
                    boolean ceilingAnchor = current == Cell.WATER && isNestRock(above);
                    if (ceilingAnchor) {
                        // Ceiling anchor on 晶巢岩: a hanging column shard or a fringe
                        // strand. Both grow downward within this same column.
                        double columnChance = (chamber[index] ? HANG_COLUMN_CHAMBER_CHANCE : HANG_COLUMN_OPEN_CHANCE)
                                * fade(s);
                        if (CrystalNestNoise.unit(x, y, z, SALT_HANG_COLUMN) < columnChance) {
                            int height = 2 + CrystalNestNoise.bounded(x, y, z, SALT_HANG_COLUMN_H, 4);
                            growColumnDown(cells, padded, ySize, px, py, pz, height);
                            claimCrystalSupport(cells, index + 1);
                        }
                    }
                    if (ceilingAnchor && cells[index] == Cell.WATER) {
                        double fringeChance = (chamber[index] ? FRINGE_CHAMBER_CHANCE : FRINGE_OPEN_CHANCE) * fade(s);
                        if (CrystalNestNoise.unit(x, y, z, SALT_FRINGE) < fringeChance) {
                            int length = 1 + CrystalNestNoise.bounded(x, y, z, SALT_FRINGE_LEN, 3);
                            growFringeDown(cells, padded, ySize, px, py, pz, length);
                            claimCrystalSupport(cells, index + 1);
                        }
                    }

                    // --- pass 4: face decor on exposed surfaces ------------------------
                    if (cells[index] != Cell.WATER) {
                        continue;
                    }
                    placeFaceDecor(cells, solid, chamber[index], padded, ySize, x, y, z, px, py, pz, s);
                }
            }
        }

        Cell[] result = new Cell[SIZE * SIZE * ySize];
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                int source = ((lx + PAD) * padded + lz + PAD) * ySize;
                int target = (lx * SIZE + lz) * ySize;
                System.arraycopy(cells, source, result, target, ySize);
            }
        }
        return result;
    }

    private static double fade(double strength) {
        return Math.min(1.0D, strength / DECOR_FULL_STRENGTH);
    }

    private static void growColumnDown(Cell[] cells, int padded, int ySize,
                                       int px, int basePy, int pz, int height) {
        for (int down = 0; down < height && basePy - down >= 0; down++) {
            int index = (px * padded + pz) * ySize + basePy - down;
            if (cells[index] != Cell.WATER) {
                break;
            }
            cells[index] = new Cell(Kind.COLUMN, 0, DIR_DOWN);
        }
    }

    private static void growFringeDown(Cell[] cells, int padded, int ySize,
                                       int px, int basePy, int pz, int length) {
        for (int down = 0; down < length && basePy - down >= 0; down++) {
            int index = (px * padded + pz) * ySize + basePy - down;
            if (cells[index] != Cell.WATER) {
                break;
            }
            int part = down == 0 ? FRINGE_TOP
                    : (down == length - 1 ? FRINGE_TAIL : FRINGE_BODY);
            cells[index] = Cell.fringe(part);
        }
    }

    /**
     * One decor item per free cell: the first exposed face (in growth priority order)
     * whose roll passes hosts a cluster, tuft, sprout or shard scatter. Crystal kinds
     * root only in 晶巢岩 (lattice rock, druse yielding to the root); on mats and whole
     * terrain tops only an algae tuft may stand.
     */
    private static void placeFaceDecor(Cell[] cells, boolean[] solid, boolean chamber,
                                       int padded, int ySize,
                                       int x, int y, int z, int px, int py, int pz, double s) {
        double chance = fade(s);
        if (chance <= 0.0D) {
            return;
        }
        // Growth priority: up first (surface scatter), then walls, then ceilings.
        int[] order = {DIR_UP, DIR_NORTH, DIR_SOUTH, DIR_WEST, DIR_EAST, DIR_DOWN};
        for (int dir : order) {
            // The support sits BEHIND the growth direction: the cluster points the way
            // its facing says, straight off the face it is anchored to.
            int sx = px - offset(0, dir);
            int sy = py - offset(1, dir);
            int sz = pz - offset(2, dir);
            if (sx < 0 || sz < 0 || sx >= padded || sz >= padded || sy < 0 || sy >= ySize) {
                continue;
            }
            int backIndex = (sx * padded + sz) * ySize + sy;
            Cell backing = cells[backIndex];
            boolean nestRock = isNestRock(backing);
            // Vegetation keeps its permissive substrate: mats, whole terrain tops and
            // column tops may still carry a tuft — but never crystal.
            boolean tuftTop = dir == DIR_UP && (backing == Cell.ALGAE
                    || (backing != null && backing.kind() == Kind.COLUMN)
                    || (solid[backIndex] && !nestRock));
            if (!nestRock && !tuftTop) {
                continue;
            }
            double threshold = (chamber ? DECOR_CHAMBER_CHANCE : DECOR_OPEN_CHANCE) * chance;
            double roll = CrystalNestNoise.unit(x, y, z, SALT_DECOR + dir);
            if (roll >= threshold) {
                continue;
            }
            if (!nestRock) {
                cells[(px * padded + pz) * ySize + py] = Cell.TUFT;
                return;
            }
            Cell decor = decorFor(x, y, z, dir, chamber);
            if (isCrystal(decor)) {
                claimCrystalSupport(cells, backIndex);
            }
            cells[(px * padded + pz) * ySize + py] = decor;
            return;
        }
    }

    /** 晶巢岩: the lattice rock — with its druse crust, which yields to crystal roots. */
    private static boolean isNestRock(Cell cell) {
        return cell != null && (cell.kind() == Kind.ROCK || cell.kind() == Kind.DRUSE);
    }

    /** Crystals: clusters, hanging columns, sprouts and fringes (not algae). */
    private static boolean isCrystal(Cell cell) {
        return switch (cell.kind()) {
            case CLUSTER, COLUMN, SPROUT, FRINGE -> true;
            default -> false;
        };
    }

    /**
     * The druse crust yields to crystal roots: where a crystal grows out of a lining
     * cell, the support stays plain 晶巢岩 so every crystal literally roots in the rock.
     */
    private static void claimCrystalSupport(Cell[] cells, int supportIndex) {
        if (cells[supportIndex] != null && cells[supportIndex].kind() == Kind.DRUSE) {
            cells[supportIndex] = Cell.ROCK;
        }
    }

    private static Cell decorFor(int x, int y, int z, int dir, boolean chamber) {
        double kind = CrystalNestNoise.unit(x, y, z, SALT_DECOR_KIND);
        double color = CrystalNestNoise.unit(x, y, z, SALT_DECOR_COLOR);
        if (dir == DIR_UP) {
            if (chamber) {
                if (kind < 0.35D) {
                    return new Cell(Kind.SPROUT, 0, 0);
                }
                return Cell.cluster(chamberColor(color), DIR_UP);
            }
            if (kind < 0.45D) {
                return Cell.TUFT;
            }
            if (kind < 0.60D) {
                return new Cell(Kind.SPROUT, 0, 0);
            }
            return Cell.cluster(openColor(color), DIR_UP);
        }
        if (chamber) {
            return Cell.cluster(chamberColor(color), dir);
        }
        return Cell.cluster(openColor(color), dir);
    }

    /**
     * Chamber palette: inside the cores any 晶 may grow — the five quiet hues in bulk,
     * and the glowing resonant crystals and life gems kept rare (7% combined).
     */
    private static int chamberColor(double roll) {
        if (roll < 0.28D) {
            return CLUSTER_WHITE;
        }
        if (roll < 0.44D) {
            return CLUSTER_AMETHYST;
        }
        if (roll < 0.58D) {
            return CLUSTER_ROSE;
        }
        if (roll < 0.71D) {
            return CLUSTER_AQUA;
        }
        if (roll < 0.93D) {
            return CLUSTER_SMOKY;
        }
        if (roll < 0.97D) {
            return CLUSTER_RESONANT;
        }
        return CLUSTER_LIFE;
    }

    /** Open-surface palette: only the quiet rock-hued trio — white, smoky and amethyst. */
    private static int openColor(double roll) {
        if (roll < 0.45D) {
            return CLUSTER_WHITE;
        }
        if (roll < 0.80D) {
            return CLUSTER_SMOKY;
        }
        return CLUSTER_AMETHYST;
    }

    private static int offset(int axis, int dir) {
        return switch (axis) {
            case 1 -> (dir == DIR_UP) ? 1 : (dir == DIR_DOWN ? -1 : 0);
            case 2 -> (dir == DIR_NORTH) ? -1 : (dir == DIR_SOUTH ? 1 : 0);
            default -> (dir == DIR_WEST) ? -1 : (dir == DIR_EAST ? 1 : 0);
        };
    }
}
