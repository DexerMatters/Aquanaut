package com.dexer.aquanaut.common.mud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MudSinkLogicTest {
    @Test
    void contactTimerResetsAfterLeavingMud() {
        assertEquals(1, MudSinkLogic.nextContactTicks(99, false));
        assertEquals(100, MudSinkLogic.nextContactTicks(99, true));
    }

    @Test
    void multipleBlocksInSameTickDoNotRestartOrAdvanceTimer() {
        assertEquals(75, MudSinkLogic.submergedTicks(75, 200L, 200L, true));
        assertEquals(76, MudSinkLogic.submergedTicks(75, 200L, 201L, true));
    }

    @Test
    void headAboveMudResetsTimerAndReentryStartsOver() {
        assertEquals(0, MudSinkLogic.submergedTicks(99, 200L, 201L, false));
        assertEquals(1, MudSinkLogic.submergedTicks(99, 200L, 203L, true));
        assertEquals(1, MudSinkLogic.submergedTicks(0, 200L, 201L, true));
    }

    @Test
    void prolongedContactDoesNotOverflowTimer() {
        assertEquals(101, MudSinkLogic.nextContactTicks(Integer.MAX_VALUE, true));
    }

    @Test
    void suffocationStartsAtFiveSeconds() {
        assertFalse(MudSinkLogic.shouldSuffocate(99));
        assertTrue(MudSinkLogic.shouldSuffocate(100));
    }

    @Test
    void damageRepeatsEverySecond() {
        assertFalse(MudSinkLogic.shouldDamage(99, 100L, -1L));
        assertTrue(MudSinkLogic.shouldDamage(100, 100L, -1L));
        assertFalse(MudSinkLogic.shouldDamage(100, 119L, 100L));
        assertTrue(MudSinkLogic.shouldDamage(100, 120L, 100L));
    }

    @Test
    void sinkingSlowsHorizontalMotionAndKeepsEntityBelowSurface() {
        assertEquals(0.55D, MudSinkLogic.slowHorizontal(1.0D), 0.000001D);
        assertEquals(-0.012D, MudSinkLogic.sinkingY(0.0D), 0.000001D);
        assertEquals(-0.55D, MudSinkLogic.slowHorizontal(-1.0D), 0.000001D);
    }

    @Test
    void fallingSpeedIsCappedButUpwardEscapeIsAllowed() {
        assertEquals(-MudZoneConfig.SINK_SPEED,
                MudSinkLogic.sinkingY(-0.8D), 0.000001D);
        assertEquals(0.2D,
                MudSinkLogic.sinkingY(0.2D), 0.000001D);
    }
}
