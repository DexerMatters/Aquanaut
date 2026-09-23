package com.dexer.aquanaut.common.light;

import java.util.Objects;

/** Movement policy for a real block-light source. Removal is always immediate. */
public record LightTransition(int ticks, Strategy strategy) {
    public enum Strategy {
        /** Fade the old cell down while fading the replacement cell up. */
        CROSSFADE,
        /** Install the replacement at full strength before retiring the old cell. */
        MAKE_BEFORE_BREAK
    }

    public static final LightTransition IMMEDIATE = new LightTransition(0, Strategy.MAKE_BEFORE_BREAK);
    public static final LightTransition FOUR_TICK_CROSSFADE = new LightTransition(4, Strategy.CROSSFADE);
    public static final LightTransition FOUR_TICK_HANDOFF = new LightTransition(4, Strategy.MAKE_BEFORE_BREAK);

    /** Retains the original constructor as the generic crossfade shorthand. */
    public LightTransition(int ticks) {
        this(ticks, Strategy.CROSSFADE);
    }

    public LightTransition {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must be non-negative");
        }
        Objects.requireNonNull(strategy, "strategy");
    }

    public int outgoingLevel(int requestedLevel, int progress) {
        if (ticks <= 0 || progress >= ticks) {
            return 0;
        }
        return strategy == Strategy.MAKE_BEFORE_BREAK
                ? requestedLevel
                : DynamicLightMath.outgoingLevel(requestedLevel, progress, ticks);
    }

    public int incomingLevel(int requestedLevel, int progress) {
        if (ticks <= 0 || strategy == Strategy.MAKE_BEFORE_BREAK) {
            return requestedLevel;
        }
        return DynamicLightMath.incomingLevel(requestedLevel, progress, ticks);
    }
}
