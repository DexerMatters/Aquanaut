package com.dexer.aquanaut.common.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class DynamicLightMathTest {
    @Test
    void fourTickLevelFifteenCrossfadeMatchesTheContract() {
        assertEquals(11, DynamicLightMath.outgoingLevel(15, 1, 4));
        assertEquals(8, DynamicLightMath.outgoingLevel(15, 2, 4));
        assertEquals(4, DynamicLightMath.outgoingLevel(15, 3, 4));
        assertEquals(4, DynamicLightMath.incomingLevel(15, 1, 4));
        assertEquals(8, DynamicLightMath.incomingLevel(15, 2, 4));
        assertEquals(11, DynamicLightMath.incomingLevel(15, 3, 4));
        assertEquals(0, DynamicLightMath.outgoingLevel(15, 4, 4));
        assertEquals(15, DynamicLightMath.incomingLevel(15, 4, 4));
    }

    @Test
    void visualLightUsesMaximumFalloffAndPreservesBrighterVanillaLight() {
        assertEquals(15, DynamicLightMath.falloffLevel(15, 0.0D, 7.75D));
        assertEquals(0, DynamicLightMath.falloffLevel(15, 8.0D, 7.75D));
        assertEquals(0xF000F0, DynamicLightMath.applyPackedBlockLevel(0xF000F0, 4));
        assertEquals(0x0000F0, DynamicLightMath.applyPackedBlockLevel(0x000000, 15));
    }
}
