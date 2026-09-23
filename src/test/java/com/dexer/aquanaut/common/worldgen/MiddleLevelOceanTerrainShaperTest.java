package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MiddleLevelOceanTerrainShaperTest {

    @Test
    void coralCapUndulatesNaturallyWithinTheTransitionBand() {
        for (int blockX = -96; blockX <= 96; blockX += 24) {
            for (int blockZ = -96; blockZ <= 96; blockZ += 24) {
                MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                        MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, -64);

                assertTrue(profile.capTopY() >= CoralForestPlacement.layerStartBlockY() - 1);
                assertTrue(profile.capTopY() <= CoralForestPlacement.layerStartBlockY() + 3);
                assertTrue(profile.capThickness() >= 4);
                assertTrue(profile.capThickness() <= 8);
                assertTrue(profile.capBottomY() > MiddleLevelOceanPlacement.layerStartBlockY(),
                        "cap should stay above the middle-ocean water band");
            }
        }
    }

    @Test
    void lowerSeaAlwaysFormsABroadCavityUnderTheCap() {
        for (int blockX = -128; blockX <= 128; blockX += 32) {
            for (int blockZ = -128; blockZ <= 128; blockZ += 32) {
                MiddleLevelOceanTerrainProfile.ColumnProfile profile =
                        MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, -64);

                assertTrue(profile.cavityHeight() >= 56, "lower sea should be at least 56 blocks tall");
                assertTrue(profile.cavityHeight() <= 68, "lower sea should be at most 68 blocks tall");
                assertTrue(profile.cavityFloorY() >= -40, "lower sea floor stays above world bottom margin");
            }
        }
    }

    @Test
    void cracksFormLargeOpeningsIntoTheMiddleSea() {
        int cracks = 0;
        int samples = 0;

        for (int blockX = -160; blockX <= 160; blockX += 16) {
            for (int blockZ = -160; blockZ <= 160; blockZ += 16) {
                if (MiddleLevelOceanTerrainProfile.profileFor(blockX, blockZ, -64).crack()) {
                    cracks++;
                }
                samples++;
            }
        }

        assertTrue(cracks >= samples / 5, "cracks should be common to connect coral forest to the middle sea");
        assertTrue(cracks <= samples / 2, "some solid cap should remain for structural integrity");
    }

    @Test
    void crackOutlinesAreIrregularRatherThanAxisAlignedSquares() {
        int size = 128;
        boolean[][] crack = new boolean[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                crack[x][z] = MiddleLevelOceanTerrainProfile.profileFor(x, z, -64).crack();
            }
        }

        // A grid-aligned value-noise intersection leaves long straight borders (the legacy
        // 56/28 fields ran 20-38 blocks without bending, reading as squares). The rotated
        // two-octave field should keep the longest straight stretch below ~18 blocks.
        int longestVertical = 0;
        for (int x = 0; x + 1 < size; x++) {
            int run = 0;
            for (int z = 0; z < size; z++) {
                run = crack[x][z] != crack[x + 1][z] ? run + 1 : 0;
                longestVertical = Math.max(longestVertical, run);
            }
        }
        int longestHorizontal = 0;
        for (int z = 0; z + 1 < size; z++) {
            int run = 0;
            for (int x = 0; x < size; x++) {
                run = crack[x][z] != crack[x][z + 1] ? run + 1 : 0;
                longestHorizontal = Math.max(longestHorizontal, run);
            }
        }

        assertTrue(longestVertical <= 18, "vertical crack borders should meander, was " + longestVertical);
        assertTrue(longestHorizontal <= 18, "horizontal crack borders should meander, was " + longestHorizontal);
    }
}
