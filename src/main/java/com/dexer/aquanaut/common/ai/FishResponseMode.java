package com.dexer.aquanaut.common.ai;

public enum FishResponseMode {
    AVOIDANCE,
    STRESS,
    CHARGE,
    IRRITATE,
    PASSIVE;

    public boolean isHitTriggered() {
        return this == STRESS || this == IRRITATE;
    }

    public boolean isChargeMode() {
        return this == CHARGE || this == IRRITATE;
    }

    public boolean isEscapeMode() {
        return this == AVOIDANCE || this == STRESS;
    }
}
