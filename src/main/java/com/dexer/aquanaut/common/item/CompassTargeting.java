package com.dexer.aquanaut.common.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The submarine compass's targeting maths: which cursor it points at, and how far round the needle
 * has to be drawn for that bearing.
 *
 * <p>
 * This mirrors vanilla's {@code CompassItemPropertyFunction} exactly. The needle is drawn relative
 * to the holder's facing — pointing straight up the sprite when the target is dead ahead and
 * rotating as the player turns — using vanilla's rotation
 * {@code 0.5 - (yaw/360 - 0.25 - atan2(dz, dx)/2pi)}, folded into {@code [0, 1)}.
 *
 * <p>
 * Deliberately free of Minecraft types so the whole rule can be unit tested without the game
 * runtime.
 */
public final class CompassTargeting {

    /** Needle frames in the compass strip, one per 11.25°. */
    public static final int BEARING_FRAMES = 32;

    private CompassTargeting() {
    }

    /** A cursor the compass could point at. */
    public record Candidate(UUID id, double x, double z) {
    }

    /**
     * The candidate closest to the holder, or empty when there is nothing to point at.
     *
     * <p>
     * Distance is measured in the horizontal plane only: a cursor directly below or above the diver
     * is still "nearest", and including Y would make a deep pin lose to a shallow one that is
     * further away in the direction the player is actually facing.
     */
    public static Optional<Candidate> nearest(List<Candidate> candidates, double holderX, double holderZ) {
        Candidate best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Candidate candidate : candidates) {
            double dx = candidate.x() - holderX;
            double dz = candidate.z() - holderZ;
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    /** The candidate with this id, or empty when it is gone. */
    public static Optional<Candidate> byId(List<Candidate> candidates, UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Candidate candidate : candidates) {
            if (id.equals(candidate.id())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Vanilla's {@code getAngleFromEntityToPos}, as a fraction of a full turn. */
    public static double angleToTarget(double targetX, double targetZ, double holderX, double holderZ) {
        return Math.atan2(targetZ - holderZ, targetX - holderX) / (Math.PI * 2.0D);
    }

    /**
     * The part of the rotation that comes from the holder turning, as a fraction of a full turn.
     * Vanilla smooths this term with a wobble so the needle lags a fast turn; smoothing it is the
     * caller's job.
     */
    public static double yawRotation(double visualYawDegrees) {
        return 0.75D - visualYawDegrees / 360.0D;
    }

    /** Vanilla's unsmoothed compass rotation, in {@code [0, 1)}. */
    public static double rotationTowards(double targetX, double targetZ,
            double holderX, double holderZ, double visualYawDegrees) {
        return positiveModulo(
                angleToTarget(targetX, targetZ, holderX, holderZ) + yawRotation(visualYawDegrees), 1.0D);
    }

    /**
     * Maps a compass rotation onto a needle frame.
     *
     * <p>
     * Rounded rather than floored because vanilla's item model puts its overrides on half-steps
     * (1/64, 3/64, …), so each frame owns the rotation centred on it rather than the one starting
     * at it.
     */
    public static int frameForRotation(double rotation) {
        int frame = (int) Math.floor(positiveModulo(rotation, 1.0D) * BEARING_FRAMES + 0.5D);
        return Math.floorMod(frame, BEARING_FRAMES);
    }

    /** {@code Math.floorMod} for doubles; vanilla's {@code positiveModulo}. */
    public static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0D ? result + modulus : result;
    }
}
