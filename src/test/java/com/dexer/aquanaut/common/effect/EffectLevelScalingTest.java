package com.dexer.aquanaut.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two three-tier effects are specified in levels while Minecraft stores amplifiers, so the
 * conversion in one place is what keeps their promises honest: level I is amplifier 0, level III is
 * the ceiling the effects were balanced for, and a stronger amplifier from a command or another mod
 * must not push the aura past it or hand out a fourth tier of haste.
 */
public final class EffectLevelScalingTest {

    private static final double EPSILON = 1.0E-9D;

    /** Float-valued rules lose a little precision when widened for the comparison. */
    private static final double FLOAT_EPSILON = 1.0E-6D;

    @Test
    void levelIsOneBasedAndCappedAtTheThirdTier() {
        assertEquals(1, EffectLevelScaling.level(0), "amplifier 0 is level I");
        assertEquals(2, EffectLevelScaling.level(1), "amplifier 1 is level II");
        assertEquals(3, EffectLevelScaling.level(2), "amplifier 2 is level III");
        assertEquals(3, EffectLevelScaling.level(3), "level IV does not exist");
        assertEquals(3, EffectLevelScaling.level(64), "an outsized amplifier still reads as level III");
        assertEquals(1, EffectLevelScaling.level(-4), "a negative amplifier cannot drop below level I");
    }

    @Test
    void auraRadiusIsOneAndAHalfBlocksPerLevel() {
        assertEquals(1.5D, EffectLevelScaling.auraRadius(0), EPSILON, "level I reaches 1.5 blocks");
        assertEquals(3.0D, EffectLevelScaling.auraRadius(1), EPSILON, "level II reaches 3 blocks");
        assertEquals(4.5D, EffectLevelScaling.auraRadius(2), EPSILON, "level III reaches 4.5 blocks");
        assertEquals(4.5D, EffectLevelScaling.auraRadius(9), EPSILON, "the radius stops growing at level III");
    }

    @Test
    void auraDamageIsOneHalfHeartPerSecondPerLevel() {
        assertEquals(1.0D, EffectLevelScaling.auraDamagePerSecond(0), EPSILON, "level I deals 1 half-heart");
        assertEquals(2.0D, EffectLevelScaling.auraDamagePerSecond(1), EPSILON, "level II deals 2 half-hearts");
        assertEquals(3.0D, EffectLevelScaling.auraDamagePerSecond(2), EPSILON, "level III deals 3 half-hearts");
        assertEquals(3.0D, EffectLevelScaling.auraDamagePerSecond(4), EPSILON, "level V is clamped to level III numbers");
        assertEquals(1.0D, EffectLevelScaling.auraDamagePerSecond(-8), EPSILON, "a negative amplifier still deals level I damage");
    }

    @Test
    void auraReachesExactlyTheSphereItAdvertises() {
        // a target one block away is inside a level I aura; one a hair outside 1.5 blocks is not
        assertTrue(EffectLevelScaling.auraReaches(0, 1.0D), "level I covers a target one block away");
        assertTrue(EffectLevelScaling.auraReaches(0, 2.25D), "a target exactly on the radius is caught");
        assertFalse(EffectLevelScaling.auraReaches(0, 2.26D), "a target past the radius is spared");
        assertFalse(EffectLevelScaling.auraReaches(0, 9.0D), "a target three blocks away is spared");

        // the same test at level III, where the radius is three times as wide
        assertTrue(EffectLevelScaling.auraReaches(2, 20.25D), "level III reaches 4.5 blocks");
        assertFalse(EffectLevelScaling.auraReaches(2, 20.26D), "level III stops at 4.5 blocks");
    }

    @Test
    void hasteKeepsVanillasPerLevelAmountsAndStopsAtTheThirdTier() {
        assertEquals(0.2D, EffectLevelScaling.hasteMiningBonus(0), EPSILON, "pellucid I mines 20% faster");
        assertEquals(0.4D, EffectLevelScaling.hasteMiningBonus(1), EPSILON, "pellucid II mines 40% faster");
        assertEquals(0.6D, EffectLevelScaling.hasteMiningBonus(2), EPSILON, "pellucid III mines 60% faster");
        assertEquals(0.6D, EffectLevelScaling.hasteMiningBonus(7), EPSILON, "mining speed stops at level III");

        assertEquals(0.1D, EffectLevelScaling.hasteAttackBonus(0), EPSILON, "pellucid I swings 10% faster");
        assertEquals(0.2D, EffectLevelScaling.hasteAttackBonus(1), EPSILON, "pellucid II swings 20% faster");
        assertEquals(0.3D, EffectLevelScaling.hasteAttackBonus(2), EPSILON, "pellucid III swings 30% faster");
        assertEquals(0.3D, EffectLevelScaling.hasteAttackBonus(7), EPSILON, "attack speed stops at level III");
    }

    @Test
    void sightGetsStrongerWithEachLevelAndPeaksAtVanillaNightVision() {
        // the strengths are floats, so the tolerance has to survive the widening to double
        assertEquals(0.6D, EffectLevelScaling.sightStrength(0), FLOAT_EPSILON, "level I is a partial lift");
        assertEquals(0.8D, EffectLevelScaling.sightStrength(1), FLOAT_EPSILON, "level II is most of the way");
        assertEquals(1.0D, EffectLevelScaling.sightStrength(2), FLOAT_EPSILON, "level III matches vanilla night vision");
        assertEquals(1.0D, EffectLevelScaling.sightStrength(30), FLOAT_EPSILON, "sight cannot go past fully lit");

        assertTrue(EffectLevelScaling.sightStrength(0) < EffectLevelScaling.sightStrength(1),
                "each level must see further than the last");
        assertTrue(EffectLevelScaling.sightStrength(1) < EffectLevelScaling.sightStrength(2),
                "each level must see further than the last");
    }
}
