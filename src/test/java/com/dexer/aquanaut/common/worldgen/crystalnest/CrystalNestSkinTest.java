package com.dexer.aquanaut.common.worldgen.crystalnest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CrystalNestSkinTest {
    private static final CrystalNestSkin.SolidQuery FLAT_FLOOR =
            (x, y, z) -> y <= 0;
    private static final CrystalNestSkin.SolidQuery NO_LEGACY =
            (x, y, z) -> false;
    private static final CrystalNestSkin.StrengthQuery FULL =
            (x, z) -> 1.0D;

    /** Synthetic geometry: open water unless a chamber blob says otherwise. */
    private record BoxField(boolean chamber) implements CrystalNestSkin.Field {
        @Override
        public int rockAt(int x, int y, int z, double strength) {
            return OPEN;
        }

        @Override
        public boolean chamberAt(int x, int y, int z) {
            return chamber;
        }
    }

    /** Nest-rock geometry: lattice rock (or its druse lining) wherever the probe says. */
    private record RockField(RockProbe probe, boolean chamber) implements CrystalNestSkin.Field {
        @Override
        public int rockAt(int x, int y, int z, double strength) {
            return probe.rockAt(x, y, z);
        }

        @Override
        public boolean chamberAt(int x, int y, int z) {
            return chamber;
        }
    }

    private interface RockProbe {
        int rockAt(int x, int y, int z);
    }

    private static CrystalNestSkin.Cell at(CrystalNestSkin.Cell[] cells, int x, int y, int z,
                                           int yLo, int ySize) {
        return cells[(x * CrystalNestSkin.SIZE + z) * ySize + (y - yLo)];
    }

    @Test
    void everyTopSurfaceIsCarpetedWithAlgae() {
        int yLo = -2;
        int ySize = 12;
        CrystalNestSkin.Cell[] cells = CrystalNestSkin.skin(0, 0, yLo, ySize,
                FLAT_FLOOR, new BoxField(false), FULL);
        for (int x = 0; x < CrystalNestSkin.SIZE; x++) {
            for (int z = 0; z < CrystalNestSkin.SIZE; z++) {
                CrystalNestSkin.Cell mat = at(cells, x, 1, z, yLo, ySize);
                assertEquals(CrystalNestSkin.Kind.ALGAE, mat.kind(),
                        "the open floor top at " + x + "," + z + " must wear its algae layer");
                assertEquals(CrystalNestSkin.Kind.ALGAE, mat.kind(), "one whole solid turf block");
                assertEquals(CrystalNestSkin.Kind.LEGACY, at(cells, x, 0, z, yLo, ySize).kind(),
                        "the terrain itself stays with the column planner");
            }
        }
    }

    @Test
    void clustersAlwaysGrowAwayFromTheirSupport() {
        int yLo = -2;
        int ySize = 14;
        // one narrow 晶巢岩 tower in the open water: its every face is a growth site
        RockField tower = new RockField(
                (x, y, z) -> y >= 0 && y <= 9 && x == 8 && z == 8
                        ? CrystalNestSkin.Field.ROCK : CrystalNestSkin.Field.OPEN,
                false);
        CrystalNestSkin.Cell[] cells = CrystalNestSkin.skin(0, 0, yLo, ySize,
                NO_LEGACY, tower, FULL);
        int clusters = 0;
        for (int x = 0; x < CrystalNestSkin.SIZE; x++) {
            for (int z = 0; z < CrystalNestSkin.SIZE; z++) {
                for (int y = yLo; y < yLo + ySize; y++) {
                    CrystalNestSkin.Cell cell = at(cells, x, y, z, yLo, ySize);
                    if (cell.kind() != CrystalNestSkin.Kind.CLUSTER) {
                        continue;
                    }
                    clusters++;
                    int[] back = backOffset(cell.facing());
                    boolean supported = inRange(x + back[0], y + back[1], z + back[2], yLo, ySize)
                            && at(cells, x + back[0], y + back[1], z + back[2], yLo, ySize).kind()
                            == CrystalNestSkin.Kind.ROCK;
                    assertTrue(supported, "cluster at " + x + "," + y + "," + z
                            + " faces " + cell.facing() + " but does not root in 晶巢岩");
                }
            }
        }
        assertTrue(clusters >= 3, "a ten-block tower grows a crowd of crystals, saw " + clusters);
    }

    @Test
    void crystalsOnlyRootOnNestRock() {
        int yLo = -2;
        int ySize = 14;
        // Legacy floor plus a 晶巢岩 ceiling (which grows hanging chains) and two 3×3
        // towers: one plain nest rock, one druse-lined. The crust must yield to crystal
        // roots, so every crystal still ends up rooted in plain nest rock and the legacy
        // floor carries mats and tufts but never crystal.
        RockField towers = new RockField((x, y, z) -> {
            if (y >= 10) {
                return CrystalNestSkin.Field.ROCK;
            }
            if (Math.abs(x - 4) <= 1 && Math.abs(z - 4) <= 1 && y >= 1 && y <= 8) {
                return CrystalNestSkin.Field.ROCK;
            }
            if (Math.abs(x - 12) <= 1 && Math.abs(z - 12) <= 1 && y >= 1 && y <= 8) {
                return CrystalNestSkin.Field.DRUSE;
            }
            return CrystalNestSkin.Field.OPEN;
        }, false);
        CrystalNestSkin.Cell[] cells = CrystalNestSkin.skin(0, 0, yLo, ySize,
                FLAT_FLOOR, towers, FULL);
        int rooted = 0;
        int tufts = 0;
        int druseRooted = 0;
        for (int x = 0; x < CrystalNestSkin.SIZE; x++) {
            for (int z = 0; z < CrystalNestSkin.SIZE; z++) {
                for (int y = yLo; y < yLo + ySize; y++) {
                    CrystalNestSkin.Cell cell = at(cells, x, y, z, yLo, ySize);
                    if (cell.kind() == CrystalNestSkin.Kind.TUFT) {
                        tufts++;
                        continue;
                    }
                    if (!isCrystal(cell)) {
                        continue;
                    }
                    rooted++;
                    int[] root = crystalRoot(cells, x, y, z, yLo, ySize);
                    boolean onNestRock = root != null
                            && at(cells, root[0], root[1], root[2], yLo, ySize).kind()
                            == CrystalNestSkin.Kind.ROCK;
                    assertTrue(onNestRock, cell.kind() + " at " + x + "," + y + "," + z
                            + " does not root in 晶巢岩");
                    if (root != null
                            && Math.abs(root[0] - 12) <= 1 && Math.abs(root[2] - 12) <= 1
                            && root[1] >= 1 && root[1] <= 8) {
                        druseRooted++;
                    }
                }
            }
        }
        assertTrue(rooted >= 5, "the sample actually grows crystal, saw " + rooted);
        assertTrue(tufts >= 1, "the matted floor still grows its algae tufts, saw " + tufts);
        assertTrue(druseRooted >= 1,
                "the druse crust yields to crystal roots on the lining tower, saw " + druseRooted);
    }

    @Test
    void glowingCrystalsAreChamberOnlyAndTheSurfaceStaysQuiet() {
        int yLo = -2;
        int ySize = 14;
        RockField floorAndCeiling = new RockField(
                (x, y, z) -> y <= 0 || y >= 10 ? CrystalNestSkin.Field.ROCK : CrystalNestSkin.Field.OPEN,
                false);
        CrystalNestSkin.Cell[] open = CrystalNestSkin.skin(0, 0, yLo, ySize,
                FLAT_FLOOR, floorAndCeiling, FULL);
        int openClusters = 0;
        for (int x = 0; x < CrystalNestSkin.SIZE; x++) {
            for (int z = 0; z < CrystalNestSkin.SIZE; z++) {
                for (int y = yLo; y < yLo + ySize; y++) {
                    CrystalNestSkin.Cell cell = at(open, x, y, z, yLo, ySize);
                    if (cell.kind() != CrystalNestSkin.Kind.CLUSTER) {
                        continue;
                    }
                    openClusters++;
                    assertTrue(cell.variant() == CrystalNestSkin.CLUSTER_WHITE
                                    || cell.variant() == CrystalNestSkin.CLUSTER_SMOKY
                                    || cell.variant() == CrystalNestSkin.CLUSTER_AMETHYST,
                            "open surfaces grow only 白晶/烟晶/紫晶: variant " + cell.variant());
                }
            }
        }
        assertTrue(openClusters >= 3, "the open rock faces really grow the trio, saw " + openClusters);

        RockField chamberField = new RockField(
                (x, y, z) -> y <= 0 || y >= 10 ? CrystalNestSkin.Field.ROCK : CrystalNestSkin.Field.OPEN,
                true);
        CrystalNestSkin.Cell[] chamber = CrystalNestSkin.skin(0, 0, yLo, ySize,
                FLAT_FLOOR, chamberField, FULL);
        int glowing = 0;
        int quiet = 0;
        boolean[] variants = new boolean[8];
        for (int x = 0; x < CrystalNestSkin.SIZE; x++) {
            for (int z = 0; z < CrystalNestSkin.SIZE; z++) {
                for (int y = yLo; y < yLo + ySize; y++) {
                    CrystalNestSkin.Cell cell = at(chamber, x, y, z, yLo, ySize);
                    if (cell.kind() != CrystalNestSkin.Kind.CLUSTER) {
                        continue;
                    }
                    variants[cell.variant()] = true;
                    if (cell.variant() == CrystalNestSkin.CLUSTER_RESONANT
                            || cell.variant() == CrystalNestSkin.CLUSTER_LIFE) {
                        glowing++;
                    } else {
                        quiet++;
                    }
                }
            }
        }
        assertTrue(glowing >= 1, "chambers host resonant crystals and life gems");
        int total = glowing + quiet;
        assertTrue(glowing * 7 <= total,
                "glowing 晶体 stay rare inside the cores, was " + glowing + "/" + total);
        int kinds = 0;
        for (boolean present : variants) {
            if (present) {
                kinds++;
            }
        }
        assertTrue(kinds >= 5, "inside the cores any 晶 may grow, saw only " + kinds + " kinds");
    }

    @Test
    void skinningIsChunkSeamless() {
        // real lattice geometry, skinned twice from two chunk origins: the overlap must agree
        CrystalNestLattice.SpanQuery span = new CrystalNestLattice.SpanQuery() {
            @Override
            public int floorY(double x, double z) {
                return -30;
            }

            @Override
            public int ceilingY(double x, double z) {
                return 33;
            }
        };
        CrystalNestSkin.SolidQuery floor = (x, y, z) -> y <= -28;
        int yLo = -28;
        int ySize = 60;
        CrystalNestSkin.Cell[] a = CrystalNestSkin.skin(0, 0, yLo, ySize, floor,
                new CrystalNestField(new CrystalNestLattice(span)), (x, z) -> 1.0D);
        CrystalNestSkin.Cell[] b = CrystalNestSkin.skin(8, 4, yLo, ySize, floor,
                new CrystalNestField(new CrystalNestLattice(span)), (x, z) -> 1.0D);
        for (int x = 8; x < 16; x++) {
            for (int z = 4; z < 16; z++) {
                for (int y = yLo; y < yLo + ySize; y++) {
                    assertEquals(at(a, x, y, z, yLo, ySize), at(b, x - 8, y, z - 4, yLo, ySize),
                            "cell " + x + "," + y + "," + z + " must not depend on the chunk origin");
                }
            }
        }
        assertFalse(allWater(a, yLo, ySize), "the sample area actually contains lattice rock");
    }

    private static boolean isCrystal(CrystalNestSkin.Cell cell) {
        return switch (cell.kind()) {
            case CLUSTER, COLUMN, SPROUT, FRINGE -> true;
            default -> false;
        };
    }

    private static boolean isChain(CrystalNestSkin.Cell cell, CrystalNestSkin.Kind kind) {
        return cell != null && cell.kind() == kind;
    }

    /**
     * The support cell behind the growth: directly behind clusters and sprouts, or the
     * 晶巢岩 at the top of the chain for hanging columns and fringes ({@code null} when the
     * chain leaves the sampled volume).
     */
    private static int[] crystalRoot(CrystalNestSkin.Cell[] cells, int x, int y, int z,
                                     int yLo, int ySize) {
        CrystalNestSkin.Cell cell = at(cells, x, y, z, yLo, ySize);
        if (cell.kind() == CrystalNestSkin.Kind.CLUSTER
                || cell.kind() == CrystalNestSkin.Kind.SPROUT) {
            int[] back = backOffset(growthDir(cell));
            int bx = x + back[0];
            int by = y + back[1];
            int bz = z + back[2];
            return inRange(bx, by, bz, yLo, ySize) ? new int[]{bx, by, bz} : null;
        }
        int top = y;
        while (top + 1 < yLo + ySize
                && isChain(at(cells, x, top + 1, z, yLo, ySize), cell.kind())) {
            top++;
        }
        return top + 1 < yLo + ySize ? new int[]{x, top + 1, z} : null;
    }

    private static boolean inRange(int x, int y, int z, int yLo, int ySize) {
        return x >= 0 && x < CrystalNestSkin.SIZE && z >= 0 && z < CrystalNestSkin.SIZE
                && y >= yLo && y < yLo + ySize;
    }

    /** The growth direction of a decor cell: clusters carry it, sprouts grow upward. */
    private static int growthDir(CrystalNestSkin.Cell cell) {
        return cell.kind() == CrystalNestSkin.Kind.CLUSTER ? cell.facing() : CrystalNestSkin.DIR_UP;
    }

    private static boolean allWater(CrystalNestSkin.Cell[] cells, int yLo, int ySize) {
        for (int x = 0; x < CrystalNestSkin.SIZE; x++) {
            for (int z = 0; z < CrystalNestSkin.SIZE; z++) {
                for (int y = yLo; y < yLo + ySize; y++) {
                    CrystalNestSkin.Kind kind = at(cells, x, y, z, yLo, ySize).kind();
                    if (kind != CrystalNestSkin.Kind.WATER && kind != CrystalNestSkin.Kind.LEGACY) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The support sits behind the growth direction (vanilla 3D data values: D U N S W E). */
    private static int[] backOffset(int facing) {
        return switch (facing) {
            case 0 -> new int[]{0, 1, 0};
            case 2 -> new int[]{0, 0, 1};
            case 3 -> new int[]{0, 0, -1};
            case 4 -> new int[]{1, 0, 0};
            case 5 -> new int[]{-1, 0, 0};
            default -> new int[]{0, -1, 0};
        };
    }
}
