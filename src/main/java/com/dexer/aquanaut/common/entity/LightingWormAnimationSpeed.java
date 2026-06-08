package com.dexer.aquanaut.common.entity;

final class LightingWormAnimationSpeed {
    private static final double CRUISE_MIN = 0.78D;
    private static final double CRUISE_MAX = 1.08D;
    private static final double SPRINT_MIN = 1.0D;
    private static final double SPRINT_MAX = 1.55D;

    private LightingWormAnimationSpeed() {
    }

    static double resolve(double motionSpeed, double referenceSpeed, boolean sprintingAway) {
        double normalizedSpeed = referenceSpeed <= 1.0E-4D
                ? 0.0D
                : Math.max(0.0D, Math.min(motionSpeed / referenceSpeed, 1.0D));
        double min = sprintingAway ? SPRINT_MIN : CRUISE_MIN;
        double max = sprintingAway ? SPRINT_MAX : CRUISE_MAX;
        return min + (max - min) * normalizedSpeed;
    }
}
