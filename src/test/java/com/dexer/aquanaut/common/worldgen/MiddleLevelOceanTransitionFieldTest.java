package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MiddleLevelOceanTransitionFieldTest {
    @Test
    void fullSupportedInteriorKeepsMaximumStrength() {
        MiddleLevelOceanTransitionField field = new MiddleLevelOceanTransitionField(fill(true), 4, 4);

        assertEquals(1.0D, field.edgeStrengthAtBlock(8, 8), 0.0001D);
        assertEquals(1.0D, field.edgeStrengthAtBlock(15, 15), 0.0001D);
    }

    @Test
    void straightBoundaryFadesMonotonicallyTowardTheInterior() {
        boolean[][] support = fill(true);
        for (int qz = 0; qz < support[0].length; qz++) {
            support[3][qz] = false;
        }

        MiddleLevelOceanTransitionField field = new MiddleLevelOceanTransitionField(support, 4, 4);

        double edge = field.edgeStrengthAtBlock(0, 8);
        double nearInterior = field.edgeStrengthAtBlock(4, 8);
        double midInterior = field.edgeStrengthAtBlock(8, 8);
        double deepInterior = field.edgeStrengthAtBlock(12, 8);

        assertTrue(edge < nearInterior);
        assertTrue(nearInterior < midInterior);
        assertTrue(midInterior <= deepInterior);
    }

    @Test
    void cornerBoundaryRecoversSmoothlyAlongTheDiagonal() {
        boolean[][] support = fill(true);
        for (int q = 0; q < support.length; q++) {
            support[3][q] = false;
            support[q][3] = false;
        }

        MiddleLevelOceanTransitionField field = new MiddleLevelOceanTransitionField(support, 4, 4);

        double corner = field.edgeStrengthAtBlock(0, 0);
        double diagonalMid = field.edgeStrengthAtBlock(6, 6);
        double interior = field.edgeStrengthAtBlock(12, 12);

        assertTrue(corner < diagonalMid);
        assertTrue(diagonalMid < interior);
    }

    @Test
    void isolatedSupportedPocketCannotOpenIntoAFullMiddleSea() {
        boolean[][] support = fill(false);
        support[4][4] = true;

        MiddleLevelOceanTransitionField field = new MiddleLevelOceanTransitionField(support, 4, 4);

        assertTrue(field.edgeStrengthAtBlock(1, 1) < 0.35D);
        assertTrue(field.edgeStrengthAtBlock(2, 2) < 0.35D);
    }

    private static boolean[][] fill(boolean value) {
        boolean[][] support = new boolean[12][12];
        for (int qx = 0; qx < support.length; qx++) {
            for (int qz = 0; qz < support[qx].length; qz++) {
                support[qx][qz] = value;
            }
        }
        return support;
    }
}
