package com.dexer.aquanaut.common.ai;

public final class FishSteeringMathTest {
    public static void main(String[] args) {
        FishSteeringMathTest test = new FishSteeringMathTest();
        test.verticalBiasKeepsThreeDimensionalMotion();
        test.surfaceBiasReducesUpwardMotionNearTheTop();
        test.barrierSearchCanChooseADownwardDetour();
    }

    private void verticalBiasKeepsThreeDimensionalMotion() {
        FishSteeringMath.Direction direction = FishSteeringMath.direction(0.35D, 0.72D, 0.18D);

        FishSteeringMath.Direction adjusted = FishSteeringMath.applyVerticalBias(direction, false, false, true, true);

        assertTrue(adjusted.y() > 0.0D, "vertical component should remain positive");
        assertTrue(adjusted.y() < direction.normalize().y(), "upward motion should be softened");
    }

    private void surfaceBiasReducesUpwardMotionNearTheTop() {
        FishSteeringMath.Direction direction = FishSteeringMath.direction(0.20D, 0.80D, 0.10D);

        FishSteeringMath.Direction adjusted = FishSteeringMath.applyVerticalBias(direction, true, false, true, true);

        assertTrue(adjusted.y() < direction.normalize().y(), "surface bias should reduce upward motion");
    }

    private void barrierSearchCanChooseADownwardDetour() {
        FishSteeringMath.Direction desired = FishSteeringMath.direction(1.0D, 0.0D, 0.0D);

        FishSteeringMath.Direction chosen = FishSteeringMath.chooseBarrierAwareDirection(desired, true, candidate -> {
            if (candidate.y() < -0.15D) {
                return 10.0D;
            }
            if (candidate.y() > 0.15D) {
                return 1.5D;
            }
            return 0.0D;
        });

        assertTrue(chosen.y() < -0.15D, "barrier search should be able to pick a vertical detour");
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
