package com.dexer.aquanaut.common.entity;

public final class LightingWormAnimationSpeedTest {

    public static void main(String[] args) {
        LightingWormAnimationSpeedTest test = new LightingWormAnimationSpeedTest();
        test.cruiseAnimationKeepsAMinimumMotionFloor();
        test.cruiseAnimationScalesUpWithMovement();
        test.sprintAnimationUsesAHigherRange();
    }

    private void cruiseAnimationKeepsAMinimumMotionFloor() {
        double animationSpeed = LightingWormAnimationSpeed.resolve(0.0D, 0.038D, false);

        assertEquals(0.78D, animationSpeed, 1.0E-9D,
                "lighting worm cruise animation should not idle near a visual stall");
    }

    private void cruiseAnimationScalesUpWithMovement() {
        double slow = LightingWormAnimationSpeed.resolve(0.0D, 0.038D, false);
        double fast = LightingWormAnimationSpeed.resolve(0.038D, 0.038D, false);

        assertTrue(fast > slow, "cruise animation speed should increase as the worm moves faster");
        assertEquals(1.08D, fast, 1.0E-9D, "full cruise speed should hit the tuned upper bound");
    }

    private void sprintAnimationUsesAHigherRange() {
        double slowSprint = LightingWormAnimationSpeed.resolve(0.0D, 0.28D, true);
        double fastSprint = LightingWormAnimationSpeed.resolve(0.28D, 0.28D, true);

        assertEquals(1.0D, slowSprint, 1.0E-9D, "sprint animation should never drop below active motion");
        assertEquals(1.55D, fastSprint, 1.0E-9D, "full sprint should use the faster upper range");
    }

    private void assertEquals(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + " expected " + expected + " but was " + actual);
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
