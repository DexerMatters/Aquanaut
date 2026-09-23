package com.dexer.aquanaut;

import com.dexer.aquanaut.common.FlotationHelper;
import com.dexer.aquanaut.common.FlotationHelper.FlotationDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The air bladders' buoyancy rule: which device acts, when it acts, and how hard.
 */
public final class FlotationHelperTest {

    /** Vanilla's water step: drag on the vertical velocity, then gravity / 16. */
    private static final double WATER_DRAG = 0.8;
    private static final double PLAYER_WATER_GRAVITY = 0.005;

    /**
     * Integrates the real tick order: the entity moves by the velocity left from
     * last tick, then water drags it and gravity takes its cut, then the device
     * gets the last word. Returns the net vertical displacement, up positive.
     */
    private static double climb(FlotationDevice device, int ticks) {
        double velocity = 0.0;
        double displacement = 0.0;
        for (int tick = 0; tick < ticks; tick++) {
            displacement += velocity;
            velocity = velocity * WATER_DRAG - PLAYER_WATER_GRAVITY;
            if (FlotationHelper.isActive(device, true, true, false)) {
                velocity = FlotationHelper.verticalVelocity(device, velocity);
            }
        }
        return displacement;
    }

    @Test
    public void holdDepthKeepsTheHolderLevel() {
        assertEquals(0.0, climb(FlotationDevice.HOLD_DEPTH, 100), 1.0e-9,
                "the small bladder holds the holder exactly where they are");
    }

    @Test
    public void fastAscentClimbsDrasticallyFasterThanItSinks() {
        double sinkPerTick = PLAYER_WATER_GRAVITY / (1.0 - WATER_DRAG);
        assertTrue(FlotationDevice.FAST_ASCENT.verticalVelocity() > sinkPerTick * 5.0,
                "the large bladder must overwhelm vanilla's steady sink, not match it");

        double withEmptyHands = climb(FlotationDevice.NONE, 20);
        double withLargeBladder = climb(FlotationDevice.FAST_ASCENT, 20);
        assertTrue(withEmptyHands < 0.0, "empty hands still sink, was " + withEmptyHands);
        assertTrue(withLargeBladder > 3.0,
                "one second of the large bladder should carry the holder up hard, was " + withLargeBladder);
    }

    @Test
    public void devicesAreGatedSoTheyBehaveAtTheSurface() {
        assertFalse(FlotationHelper.isActive(FlotationDevice.NONE, true, true, false));
        assertFalse(FlotationHelper.isActive(FlotationDevice.HOLD_DEPTH, false, false, false),
                "ashore, holding a bladder does nothing");
        assertTrue(FlotationHelper.isActive(FlotationDevice.HOLD_DEPTH, true, false, false),
                "floating with the head out still holds depth");
        assertTrue(FlotationHelper.isActive(FlotationDevice.FAST_ASCENT, true, true, false));
        assertFalse(FlotationHelper.isActive(FlotationDevice.FAST_ASCENT, true, false, false),
                "the large bladder stops at the surface instead of launching the holder out");
    }

    @Test
    public void sneakingBeatsEveryDevice() {
        for (FlotationDevice device : FlotationDevice.values()) {
            assertFalse(FlotationHelper.isActive(device, true, true, true),
                    device + " must leave a diver in control");
        }
    }

    @Test
    public void theStrongerHeldDeviceWins() {
        assertEquals(FlotationDevice.FAST_ASCENT, FlotationDevice.NONE.stronger(FlotationDevice.FAST_ASCENT));
        assertEquals(FlotationDevice.FAST_ASCENT, FlotationDevice.FAST_ASCENT.stronger(FlotationDevice.HOLD_DEPTH));
        assertEquals(FlotationDevice.HOLD_DEPTH, FlotationDevice.HOLD_DEPTH.stronger(FlotationDevice.NONE));
        assertEquals(FlotationDevice.HOLD_DEPTH, FlotationDevice.NONE.stronger(FlotationDevice.HOLD_DEPTH));
    }

    @Test
    public void aFasterClimbIsNotSlowed() {
        assertEquals(0.4, FlotationHelper.verticalVelocity(FlotationDevice.HOLD_DEPTH, 0.4));
        assertEquals(0.4, FlotationHelper.verticalVelocity(FlotationDevice.FAST_ASCENT, 0.4));
    }
}
