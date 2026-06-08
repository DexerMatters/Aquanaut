package com.dexer.aquanaut.client.model;

public final class GasFlowMeterTargetingHelper {
    private GasFlowMeterTargetingHelper() {
    }

    public static Transform targeting(boolean rightHanded) {
        float side = rightHanded ? 1.0F : -1.0F;
        return new Transform(
                side * 0.14F,
                -0.18F,
                -0.58F,
                side * 18.0F,
                -4.0F,
                side * -3.0F,
                0.90F);
    }

    public record Transform(
            float translateX,
            float translateY,
            float translateZ,
            float yawDegrees,
            float pitchDegrees,
            float rollDegrees,
            float scale) {
    }
}
