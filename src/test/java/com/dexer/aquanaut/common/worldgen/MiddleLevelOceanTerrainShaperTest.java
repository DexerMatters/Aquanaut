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

                assertTrue(profile.cavityHeight() >= 40, "lower sea should be at least 40 blocks tall");
                assertTrue(profile.cavityHeight() <= 48, "lower sea should be at most 48 blocks tall");
                assertTrue(profile.cavityFloorY() >= -52, "lower sea floor stays above world bottom margin");
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
}
