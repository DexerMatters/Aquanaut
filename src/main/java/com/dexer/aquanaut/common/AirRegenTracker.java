package com.dexer.aquanaut.common;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Shared state between {@link com.dexer.aquanaut.mixin.LivingEntityMixin} and
 * {@link PlayerAirEvents} to track which players had {@code increaseAirSupply}
 * called this tick (including when the main bar was already full, e.g. bubble
 * columns).
 */
public final class AirRegenTracker {
    public static final Set<UUID> AIR_INCREASE_CALLED = new HashSet<>();

    private AirRegenTracker() {
    }
}
