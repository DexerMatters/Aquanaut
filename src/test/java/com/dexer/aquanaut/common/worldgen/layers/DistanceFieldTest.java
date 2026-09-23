package com.dexer.aquanaut.common.worldgen.layers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DistanceFieldTest {

    @Test
    void supportedInteriorIsFarFromObstacles() {
        boolean[][] support = fill(true);
        float[][] dist = DistanceField.nearestUnsupportedCells(support);
        // No obstacles → infinite distance
        assertTrue(dist[6][6] > 1e8f);
    }

    @Test
    void straightEdgeDistanceMatchesBruteForce() {
        boolean[][] support = fill(true);
        for (int z = 0; z < 12; z++) {
            support[3][z] = false;
        }
        float[][] dist = DistanceField.nearestUnsupportedCells(support);

        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                float expected = bruteForce(support, x, z);
                assertEquals(expected, dist[x][z], 0.05f, "cell " + x + "," + z);
            }
        }
    }

    @Test
    void isolatedPilotHasSmallClearance() {
        boolean[][] support = fill(false);
        support[4][4] = true;
        float[][] dist = DistanceField.nearestUnsupportedCells(support);
        assertTrue(dist[4][4] >= 0.9f && dist[4][4] <= 1.5f, "pilot cell clearance ~1 cell");
    }

    private static float bruteForce(boolean[][] support, int x, int z) {
        float best = 1e9f;
        for (int i = 0; i < support.length; i++) {
            for (int j = 0; j < support[0].length; j++) {
                if (support[i][j]) {
                    continue;
                }
                float d = (float) Math.hypot(i - x, j - z);
                best = Math.min(best, d);
            }
        }
        return best;
    }

    private static boolean[][] fill(boolean value) {
        boolean[][] support = new boolean[12][12];
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                support[x][z] = value;
            }
        }
        return support;
    }
}
