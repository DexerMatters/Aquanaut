package com.dexer.aquanaut.common.effect;

import net.minecraft.util.Mth;

/**
 * Every three-tier number behind the mod's aquatic effects, in one place.
 *
 * <p>
 * Minecraft reports effect strength as a zero-based amplifier while players talk about levels, so
 * the conversion happens here once: level I is amplifier 0, and the scaling stops at level III even
 * if a command or another mod hands out a stronger amplifier. The charged aura's reach and damage,
 * the haste pellucid is worth, and how far pellucid lifts the dark all read their numbers from this
 * class, which keeps the effects' promises and their tooltips telling the same story.
 */
public final class EffectLevelScaling {

    /** Both effects are designed as three-tier effects; anything past III keeps tier III numbers. */
    public static final int MAX_LEVEL = 3;

    /** Charged aura radius, in blocks, per level. */
    private static final double AURA_RADIUS_PER_LEVEL = 1.5D;

    /** Charged aura damage, in half-hearts per second, per level. */
    private static final float AURA_DAMAGE_PER_LEVEL = 1.0F;

    /** Vanilla haste multiplies tool speed by {@code 1 + 0.2 * level}. */
    private static final double MINING_SPEED_PER_LEVEL = 0.2D;

    /** Vanilla haste adds {@code 0.1 * level} to attack speed. */
    private static final double ATTACK_SPEED_PER_LEVEL = 0.1D;

    /**
     * How strongly pellucid drives the vanilla night-vision lightmap blend: a dark frame is taken
     * this far towards its fully lit colour. Level III is vanilla night vision, the lower tiers a
     * partial sight so that the three levels still mean something for the eyes.
     */
    private static final float[] SIGHT_STRENGTH = { 0.6F, 0.8F, 1.0F };

    private EffectLevelScaling() {
    }

    /** One-based level for an amplifier: 0 -> I, 1 -> II, 2 -> III, and III is the ceiling. */
    public static int level(int amplifier) {
        return Mth.clamp(amplifier + 1, 1, MAX_LEVEL);
    }

    /** Radius of the charged aura: 1.5 / 3 / 4.5 blocks for levels I / II / III. */
    public static double auraRadius(int amplifier) {
        return AURA_RADIUS_PER_LEVEL * level(amplifier);
    }

    /** Damage dealt by the charged aura on each pulse: 1 / 2 / 3 half-hearts per second. */
    public static float auraDamagePerSecond(int amplifier) {
        return AURA_DAMAGE_PER_LEVEL * level(amplifier);
    }

    /**
     * Whether something whose centre sits {@code distanceSqr} away from the bearer is inside the
     * aura. The aura is a sphere, not the box the entity query hands back, so the caller's cube
     * search still has to be trimmed at the exact radius.
     */
    public static boolean auraReaches(int amplifier, double distanceSqr) {
        double radius = auraRadius(amplifier);
        return distanceSqr <= radius * radius;
    }

    /** Mining speed bonus pellucid is worth: 0.2 / 0.4 / 0.6, the multiplier being one more. */
    public static double hasteMiningBonus(int amplifier) {
        return MINING_SPEED_PER_LEVEL * level(amplifier);
    }

    /** Attack speed bonus pellucid is worth: 0.1 / 0.2 / 0.3. */
    public static double hasteAttackBonus(int amplifier) {
        return ATTACK_SPEED_PER_LEVEL * level(amplifier);
    }

    /** Sight strength pellucid is worth: 0.6 / 0.8 / 1.0 of the way to a fully lit frame. */
    public static float sightStrength(int amplifier) {
        return SIGHT_STRENGTH[level(amplifier) - 1];
    }
}
