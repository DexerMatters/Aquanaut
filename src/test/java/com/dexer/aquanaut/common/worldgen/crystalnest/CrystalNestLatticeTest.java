package com.dexer.aquanaut.common.worldgen.crystalnest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CrystalNestLatticeTest {
    private static final CrystalNestLattice.SpanQuery FLAT_SPAN = new CrystalNestLattice.SpanQuery() {
        @Override
        public int floorY(double x, double z) {
            return -30;
        }

        @Override
        public int ceilingY(double x, double z) {
            return 33;
        }
    };

    @Test
    void coresAreHugeAndChambersKeepShellThickness() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        for (int i = -3; i <= 3; i++) {
            for (int k = -3; k <= 3; k++) {
                for (int row = 0; row <= 1; row++) {
                    double radius = lattice.nodeRadius(i, k, row);
                    assertTrue(radius >= CrystalNestLattice.CORE_RADIUS_MIN
                            && radius <= CrystalNestLattice.CORE_RADIUS_MAX,
                            "cores stay in the huge range");
                    double ratio = lattice.nodeCavityRadius(i, k, row) / radius;
                    assertTrue(ratio >= CrystalNestLattice.CAVITY_RATIO_MIN - 1e-9
                                    && ratio <= CrystalNestLattice.CAVITY_RATIO_MAX + 1e-9,
                            "chambers keep a solid shell all around");
                }
            }
        }
    }

    @Test
    void mostIntersectionsCarryACoreAndMostCoresAreHollow() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        int nodes = 0;
        int active = 0;
        int hollow = 0;
        for (int i = -40; i <= 40; i++) {
            for (int k = -40; k <= 40; k++) {
                for (int row = 0; row <= 1; row++) {
                    nodes++;
                    if (lattice.nodeActive(i, k, row)) {
                        active++;
                    }
                    if (lattice.nodeHollow(i, k, row)) {
                        hollow++;
                    }
                }
            }
        }
        double activeShare = active / (double) nodes;
        assertTrue(activeShare > 0.80 && activeShare < 0.95,
                "cores keep the lattice dense without becoming a solid slab, was " + activeShare);
        assertTrue(hollow > active / 2, "most cores are hollowed into chambers");
    }

    @Test
    void nodesJitterOffTheRawLatticeGrid() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        int jittered = 0;
        for (int i = -8; i <= 8; i++) {
            for (int k = -8; k <= 8; k++) {
                if (Math.abs(lattice.nodeX(i, k, 0) - i * (double) CrystalNestLattice.CELL) > 0.5D) {
                    jittered++;
                }
            }
        }
        assertTrue(jittered > 200, "nearly every node drifts off the grid intersection");
    }

    @Test
    void pipesRunBetweenNeighbouringIntersectionsAndRootsAnchorTheLattice() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        int linked = 0;
        for (int i = -8; i <= 8; i++) {
            for (int k = -8; k <= 8; k++) {
                if (!lattice.strutLive(i, k, 0, CrystalNestLattice.STRUT_X)) {
                    continue;
                }
                linked++;
                assertEquals(lattice.nodeX(i + 1, k, 0), lattice.strutBX(i, k, 0, CrystalNestLattice.STRUT_X), 1e-9);
                assertEquals(lattice.nodeY(i + 1, k, 0), lattice.strutBY(i, k, 0, CrystalNestLattice.STRUT_X), 1e-9);
                assertTrue(lattice.strutRadius(i, k, 0, CrystalNestLattice.STRUT_X)
                                >= CrystalNestLattice.STRUT_RADIUS_MIN,
                        "branches stay thick — and solid: no bore runs through them");
            }
        }
        assertTrue(linked > 50, "the skinned pipeline is well connected");

        int roots = 0;
        for (int i = -8; i <= 8; i++) {
            for (int k = -8; k <= 8; k++) {
                if (!lattice.strutLive(i, k, 0, CrystalNestLattice.STRUT_ROOT_DOWN)) {
                    continue;
                }
                roots++;
                assertTrue(lattice.strutBY(i, k, 0, CrystalNestLattice.STRUT_ROOT_DOWN) < -30.0D,
                        "downward roots plunge past the sea floor");
            }
        }
        assertTrue(roots > 8, "rooted pipes anchor the lattice into the floor");

        int upRoots = 0;
        for (int i = -8; i <= 8; i++) {
            for (int k = -8; k <= 8; k++) {
                if (lattice.strutLive(i, k, 1, CrystalNestLattice.STRUT_ROOT_UP)) {
                    upRoots++;
                    assertTrue(lattice.strutBY(i, k, 1, CrystalNestLattice.STRUT_ROOT_UP) > 33.0D,
                            "upward roots reach into the ceiling");
                }
                assertFalse(lattice.strutLive(i, k, 1, CrystalNestLattice.STRUT_Y),
                        "the vertical pipe only leaves the bottom row upward");
            }
        }
        assertTrue(upRoots > 8, "rooted pipes anchor the lattice into the ceiling");
    }
}
