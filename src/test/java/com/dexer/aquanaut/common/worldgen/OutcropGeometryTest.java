package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OutcropGeometryTest {
    private static final int FLOOR_Y = -25;
    private static final int CAVITY_HEIGHT = 62;
    private static final int MOUNTAIN_TOP = FLOOR_Y + (int) Math.round(CAVITY_HEIGHT * 0.45D);

    @Test
    void silhouettesAreDeterministic() {
        for (int x = -500; x <= 500; x += 37) {
            assertEquals(OutcropGeometry.topYAt(x, 123, FLOOR_Y, CAVITY_HEIGHT, MOUNTAIN_TOP),
                    OutcropGeometry.topYAt(x, 123, FLOOR_Y, CAVITY_HEIGHT, MOUNTAIN_TOP));
        }
    }

    @Test
    void everyKindRisesSomewhere() {
        EnumSet<OutcropGeometry.Kind> seen = EnumSet.noneOf(OutcropGeometry.Kind.class);
        for (int cellX = -40; cellX <= 40; cellX++) {
            for (int cellZ = -40; cellZ <= 40; cellZ++) {
                OutcropGeometry.Outcrop outcrop = OutcropGeometry.outcropAt(cellX, cellZ);
                if (outcrop != null) {
                    seen.add(outcrop.kind());
                }
            }
        }
        assertEquals(EnumSet.allOf(OutcropGeometry.Kind.class), seen,
                "mesa, ridge, mound and spire all grow");
    }

    @Test
    void summitsNeverReachTheMountainLine() {
        for (int x = -1200; x <= 1200; x += 7) {
            for (int z = -1200; z <= 1200; z += 97) {
                int top = OutcropGeometry.topYAt(x, z, FLOOR_Y, CAVITY_HEIGHT, MOUNTAIN_TOP);
                assertTrue(top <= MOUNTAIN_TOP, "summit rose above the mountain line");
                assertTrue(top == Integer.MIN_VALUE || top >= FLOOR_Y, "summit sank below the floor");
            }
        }
    }

    @Test
    void massifHeightsVaryInsteadOfStandingAtOneLevel() {
        int peaks = 0;
        boolean varied = false;
        int previous = Integer.MIN_VALUE;
        for (int x = -1200; x <= 1200; x += 3) {
            for (int z = -1200; z <= 1200; z += 51) {
                int top = OutcropGeometry.topYAt(x, z, FLOOR_Y, CAVITY_HEIGHT, MOUNTAIN_TOP);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                peaks++;
                if (previous != Integer.MIN_VALUE && top != previous) {
                    varied = true;
                }
                previous = top;
            }
        }
        assertTrue(peaks > 0, "massifs exist across the survey area");
        assertTrue(varied, "summit heights vary instead of standing at one level");
    }
}
