package com.dexer.aquanaut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.dexer.aquanaut.common.drone.DroneHeadlightGeometry;

final class DroneHeadlightGeometryTest {
    @Test
    void headlightReachesFartherThanTheHandheldLamp() {
        assertTrue(DroneHeadlightGeometry.AIR_RANGE > 20.0D);
        assertTrue(DroneHeadlightGeometry.SUBMERGED_RANGE > 12.0D);
    }

    @Test
    void rangeSwitchesWhenTheDroneIsSubmerged() {
        assertEquals(DroneHeadlightGeometry.AIR_RANGE, DroneHeadlightGeometry.range(false));
        assertEquals(DroneHeadlightGeometry.SUBMERGED_RANGE, DroneHeadlightGeometry.range(true));
    }
}
