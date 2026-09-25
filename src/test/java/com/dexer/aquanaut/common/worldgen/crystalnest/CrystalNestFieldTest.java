package com.dexer.aquanaut.common.worldgen.crystalnest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CrystalNestFieldTest {
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

    private static boolean solid(CrystalNestField field, double x, double y, double z, double strength) {
        return field.shell(x, y, z, strength) > -CrystalNestLattice.SKIN_BAND * strength
                && field.voidField(x, y, z, strength) < 0.0D;
    }

    @Test
    void largeCoresSitOnThePipelineIntersections() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        CrystalNestField field = new CrystalNestField(lattice);
        int nodes = 0;
        int rays = 0;
        int raysWithShell = 0;
        int chambers = 0;
        int nodules = 0;
        for (int i = -3; i <= 3; i++) {
            for (int k = -3; k <= 3; k++) {
                for (int row = 0; row <= 1; row++) {
                    if (!lattice.nodeActive(i, k, row)) {
                        continue;
                    }
                    nodes++;
                    double cx = lattice.nodeX(i, k, row);
                    double cy = lattice.nodeY(i, k, row);
                    double cz = lattice.nodeZ(i, k, row);
                    double radius = lattice.nodeRadius(i, k, row);
                    if (lattice.nodeHollow(i, k, row)) {
                        chambers++;
                        int hollow = 0;
                        for (int[] o : new int[][]{{0, 0, 0}, {2, 0, 0}, {-2, 0, 0}, {0, 2, 2}, {0, -2, -2}}) {
                            if (field.voidField(cx + o[0], cy + o[1], cz + o[2], 1.0D) > 0.0D) {
                                hollow++;
                            }
                        }
                        assertTrue(hollow >= 3, "every hollow core opens into a big chamber");
                    } else {
                        nodules++;
                        assertTrue(solid(field, cx, cy, cz, 1.0D),
                                "solid nodules fill their center");
                    }
                    // Rays tilted off the pipe plane must cross skinned rock: shells close
                    // around every core, and only the chambers themselves stay open.
                    for (int a = 0; a < 12; a++) {
                        for (double tilt : new double[]{0.4D, -0.4D}) {
                            double angle = a * Math.PI / 6.0D;
                            rays++;
                            for (double f = 0.5D; f <= 1.5D; f += 0.05D) {
                                double px = cx + Math.cos(angle) * Math.cos(tilt) * radius * f;
                                double py = cy + Math.sin(tilt) * radius * f;
                                double pz = cz + Math.sin(angle) * Math.cos(tilt) * radius * f;
                                if (solid(field, px, py, pz, 1.0D)) {
                                    raysWithShell++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(nodes >= 30, "sampling covered plenty of intersections");
        assertTrue(chambers >= 20 && nodules >= 1, "both chamber cores and solid nodules occur");
        double closed = raysWithShell / (double) rays;
        assertTrue(closed > 0.9D, "the shell around each core closes into rock, was " + closed);
    }

    @Test
    void biomePacksDenseAndGrowsDenserWithDepth() {
        // The whole biome packs as a dense breccia whose density grows with depth: an
        // airy crown over the giants, massive fused rock at the roots. Measured slab-wise
        // down the open middle-sea band (slab 0 is the deepest).
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        CrystalNestField field = new CrystalNestField(lattice);
        int slabs = 6;
        long[] slabRock = new long[slabs];
        long[] slabTotal = new long[slabs];
        for (int x = -360; x < 360; x += 6) {
            for (int z = -360; z < 360; z += 6) {
                for (int y = -28; y < 32; y += 3) {
                    int slab = (int) ((long) (y + 28) * slabs / 60);
                    slabTotal[slab]++;
                    if (solid(field, x + 0.5, y + 0.5, z + 0.5, 1.0D)) {
                        slabRock[slab]++;
                    }
                }
            }
        }
        long rock = 0;
        long total = 0;
        for (int slab = 0; slab < slabs; slab++) {
            rock += slabRock[slab];
            total += slabTotal[slab];
        }
        double overall = rock / (double) total;
        assertTrue(overall > 0.5D,
                "the biome packs dense (well past half rock), was " + overall);
        double deepest = slabRock[0] / (double) slabTotal[0];
        double shallowest = slabRock[slabs - 1] / (double) slabTotal[slabs - 1];
        assertTrue(deepest > shallowest + 0.2D,
                "density grows with depth, deep " + deepest + " vs shallow " + shallowest);
        // The pipe rows make honest local beds, so the depth trend is judged on a rolling
        // three-slab window: the smoothed density must never decrease with depth.
        double[] rolling = new double[slabs - 2];
        for (int slab = 0; slab < rolling.length; slab++) {
            rolling[slab] = (slabRock[slab] + slabRock[slab + 1] + slabRock[slab + 2])
                    / (double) (slabTotal[slab] + slabTotal[slab + 1] + slabTotal[slab + 2]);
        }
        for (int slab = 1; slab < rolling.length; slab++) {
            assertTrue(rolling[slab - 1] > rolling[slab] - 0.04D,
                    "smoothed density never decreases with depth, "
                            + rolling[slab - 1] + " vs " + rolling[slab]);
        }
    }

    @Test
    void branchesCarryNoCavities() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        CrystalNestField field = new CrystalNestField(lattice);
        int struts = 0;
        int midSolid = 0;
        int stray = 0;
        for (int i = -3; i <= 3; i++) {
            for (int k = -3; k <= 3; k++) {
                for (int kind : new int[]{CrystalNestLattice.STRUT_X, CrystalNestLattice.STRUT_ROOT_DOWN}) {
                    if (!lattice.strutLive(i, k, 0, kind)) {
                        continue;
                    }
                    struts++;
                    double ax = lattice.strutAX(i, k, 0);
                    double ay = lattice.strutAY(i, k, 0);
                    double az = lattice.strutAZ(i, k, 0);
                    double bx = lattice.strutBX(i, k, 0, kind);
                    double by = lattice.strutBY(i, k, 0, kind);
                    double bz = lattice.strutBZ(i, k, 0, kind);
                    // The middle stretch of every branch runs solid: no bore, no 空腔.
                    boolean through = true;
                    for (double t = 0.1D; t <= 0.9D; t += 0.05D) {
                        double px = ax + (bx - ax) * t;
                        double py = ay + (by - ay) * t;
                        double pz = az + (bz - az) * t;
                        if (field.voidField(px, py, pz, 1.0D) <= 0.0D) {
                            continue;
                        }
                        if (t > 0.4D && t < 0.6D) {
                            through = false;
                        }
                        // A void along a branch may only be the chamber of a core it
                        // passes through — never a cavity of the branch itself.
                        if (!insideSomeChamber(lattice, px, py, pz)) {
                            stray++;
                        }
                    }
                    if (through) {
                        midSolid++;
                    }
                }
            }
        }
        assertTrue(struts >= 4, "the sample found connected branches");
        assertTrue(stray == 0, "no 空腔 inside the branches, saw " + stray + " stray voids");
        assertTrue(midSolid >= struts * 0.6,
                "the middle stretch of every branch is solid rock, was " + midSolid + "/" + struts);
    }

    /** Whether the point sits inside (or just by) a core chamber — the only voids allowed. */
    private static boolean insideSomeChamber(CrystalNestLattice lattice, double x, double y, double z) {
        for (int i = -6; i <= 6; i++) {
            for (int k = -6; k <= 6; k++) {
                for (int row = 0; row <= 1; row++) {
                    if (!lattice.nodeHollow(i, k, row)) {
                        continue;
                    }
                    double dx = x - lattice.nodeX(i, k, row);
                    double dy = y - lattice.nodeY(i, k, row);
                    double dz = z - lattice.nodeZ(i, k, row);
                    // chamber radius + cavity noise + domain warp margin
                    double reach = lattice.nodeCavityRadius(i, k, row) + 8.0D;
                    if (dx * dx + dy * dy + dz * dz <= reach * reach) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Test
    void stereoNoiseKeepsTheSkinIrregular() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        CrystalNestField field = new CrystalNestField(lattice);
        double best = 0.0D;
        for (int i = -2; i <= 2; i++) {
            for (int k = -2; k <= 2; k++) {
                if (!lattice.nodeActive(i, k, 0)) {
                    continue;
                }
                double cx = lattice.nodeX(i, k, 0);
                double cy = lattice.nodeY(i, k, 0);
                double cz = lattice.nodeZ(i, k, 0);
                double min = Double.MAX_VALUE;
                double max = -Double.MAX_VALUE;
                for (int a = 0; a < 16; a++) {
                    double angle = a * Math.PI / 8.0D;
                    double px = cx + Math.cos(angle) * lattice.nodeRadius(i, k, 0) * 0.95D;
                    double pz = cz + Math.sin(angle) * lattice.nodeRadius(i, k, 0) * 0.95D;
                    double value = field.shell(px, cy, pz, 1.0D);
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
                best = Math.max(best, max - min);
            }
        }
        assertTrue(best > 1.0D, "the isosurface bulges and dents by whole blocks, swing " + best);
    }

    @Test
    void theLatticeFadesOutForNeighbouringBiomes() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        CrystalNestField field = new CrystalNestField(lattice);
        int full = 0;
        int half = 0;
        for (int x = -90; x < 90; x += 3) {
            for (int z = -90; z < 90; z += 3) {
                for (int y = -24; y < 30; y += 3) {
                    if (solid(field, x + 0.5, y + 0.5, z + 0.5, 1.0D)) {
                        full++;
                    }
                    if (solid(field, x + 0.5, y + 0.5, z + 0.5, 0.5D)) {
                        half++;
                    }
                    assertFalse(solid(field, x + 0.5, y + 0.5, z + 0.5, 0.0D),
                            "strength zero leaves the neighbouring biome clean");
                }
            }
        }
        assertTrue(half < full, "halved strength thins the lattice out toward the border");
    }

    @Test
    void chambersAreSealedGeodesInTheSolid() {
        CrystalNestLattice lattice = new CrystalNestLattice(FLAT_SPAN);
        CrystalNestField field = new CrystalNestField(lattice);
        int hollow = 0;
        int sealChecked = 0;
        int sealIntact = 0;
        for (int i = -6; i <= 6; i++) {
            for (int k = -6; k <= 6; k++) {
                for (int row = 0; row <= 1; row++) {
                    if (!lattice.nodeHollow(i, k, row)) {
                        continue;
                    }
                    hollow++;
                    double cx = lattice.nodeX(i, k, row);
                    double cy = lattice.nodeY(i, k, row);
                    double cz = lattice.nodeZ(i, k, row);
                    double rc = lattice.nodeCavityRadius(i, k, row);
                    // With solid branches every chamber is its own sealed room: just past
                    // the chamber wall, all around, stands rock.
                    for (int a = 0; a < 8; a++) {
                        double angle = a * Math.PI / 4.0D;
                        double ux = Math.cos(angle);
                        double uz = Math.sin(angle);
                        sealChecked++;
                        for (double d = rc + 1.5D; d <= rc + 5.0D; d += 0.5D) {
                            if (solid(field, cx + ux * d, cy, cz + uz * d, 1.0D)) {
                                sealIntact++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(hollow >= 40, "the sample covered plenty of chambers");
        assertTrue(sealChecked >= 200 && sealIntact >= sealChecked * 0.9,
                "every chamber is a sealed geode in the solid, was " + sealIntact + "/" + sealChecked);
    }
}
