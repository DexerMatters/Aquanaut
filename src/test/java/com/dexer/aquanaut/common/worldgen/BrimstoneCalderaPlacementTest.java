package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class BrimstoneCalderaPlacementTest {
    @Test
    void biomeLocationIsNamespacedUnderAquanaut() {
        assertEquals("aquanaut", BrimstoneCalderaPlacement.location().getNamespace());
        assertEquals("brimstone_caldera", BrimstoneCalderaPlacement.location().getPath());
    }

    @Test
    void hiddenBiomeAnchorIsUnique() {
        float parameter = BrimstoneCalderaPlacement.holderAnchorParameter();
        float offset = BrimstoneCalderaPlacement.holderAnchorOffset();
        assertNotEquals(MiddleLevelOceanPlacement.holderAnchorParameter(), parameter);
        assertNotEquals(BrineMirrorGorgePlacement.holderAnchorParameter(), parameter);
        assertNotEquals(CoralForestPlacement.holderAnchorParameter(), parameter);
        assertNotEquals(JellyJunglePlacement.holderAnchorParameter(), parameter);
        assertNotEquals(MiddleLevelOceanPlacement.holderAnchorOffset(), offset);
        assertNotEquals(BrineMirrorGorgePlacement.holderAnchorOffset(), offset);
        assertNotEquals(CoralForestPlacement.holderAnchorOffset(), offset);
        assertNotEquals(JellyJunglePlacement.holderAnchorOffset(), offset);
    }
}
