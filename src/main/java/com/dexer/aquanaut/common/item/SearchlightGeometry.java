package com.dexer.aquanaut.common.item;

import net.minecraft.world.phys.Vec3;

/**
 * The shared geometry for the handheld searchlight's real block-light source.
 *
 * <p>The lamp has no client-rendered beam anymore. This class deliberately contains only the
 * physical placement data used by the server light manager and by the item's tooltip: where the
 * lens sits, how far the source may be placed, and how an obstruction shortens that distance.
 */
public final class SearchlightGeometry {

    /** How far the source may be placed through air, in blocks. */
    public static final double AIR_RANGE = 20.0D;

    /** How far the source may be placed while the holder is submerged, in blocks. */
    public static final double SUBMERGED_RANGE = 12.0D;

    /** The source is kept just on the lamp side of the block hit by the ray. */
    public static final double TARGET_EPSILON = 0.01D;

    /** Candidate cells are sampled at quarter-block intervals while backing away from a surface. */
    public static final double PLACEMENT_STEP = 0.25D;

    /** Do not move a target more than two blocks toward the lamp to find a placeable cell. */
    public static final double MAX_PLACEMENT_BACKOFF = 2.0D;

    /** The lens sits this far in front of the holder's eye, in blocks. */
    private static final double LENS_FORWARD = 0.55D;

    /** The lens is offset toward the hand, in blocks. */
    private static final double LENS_OUTWARD = 0.22D;

    /** The lamp hangs this far below the eye line, in blocks. */
    private static final double LENS_DROP = 0.18D;

    /** Below this a direction is replaced by a stable usable default. */
    private static final double DIRECTION_EPSILON = 1.0E-8D;

    private SearchlightGeometry() {
    }

    /** {@return the lamp's range for the holder's current fluid state} */
    public static double range(boolean submerged) {
        return submerged ? SUBMERGED_RANGE : AIR_RANGE;
    }

    /**
     * {@return the distance at which a source should be targeted after clipping to an obstruction}
     *
     * <p>The endpoint is kept a tiny amount before the hit so the light block can occupy an empty
     * cell instead of trying to replace the wall it is meant to illuminate.
     */
    public static double targetDistance(double unblockedRange, double obstructionDistance) {
        if (!(unblockedRange > 0.0D)) {
            return 0.0D;
        }

        if (!(obstructionDistance >= 0.0D) || obstructionDistance >= unblockedRange) {
            return unblockedRange;
        }

        return Math.min(unblockedRange, Math.max(0.0D, obstructionDistance - TARGET_EPSILON));
    }

    /** {@return a point on the lamp's axis at {@code distance} blocks from its lens} */
    public static Vec3 targetPoint(Vec3 lens, Vec3 direction, double distance) {
        Vec3 axis = direction.lengthSqr() < DIRECTION_EPSILON
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();

        return lens.add(axis.scale(Math.max(0.0D, distance)));
    }

    /** {@return the distance of a placement candidate after the requested backoff sample} */
    public static double placementDistance(double terminalDistance, int sample) {
        if (!(terminalDistance > 0.0D)) {
            return 0.0D;
        }

        int clampedSample = Math.max(0, sample);
        double backoff = Math.min(MAX_PLACEMENT_BACKOFF, clampedSample * PLACEMENT_STEP);
        return Math.max(0.0D, terminalDistance - backoff);
    }

    /**
     * {@return the world position of the lamp's lens}
     *
     * <p>The side offset comes from the body's yaw rather than from the look vector, so the lens
     * stays beside the holder's head even when they look straight up or down.
     */
    public static Vec3 lens(Vec3 eye, Vec3 look, float bodyYawDegrees, boolean leftHand) {
        Vec3 forward = look.lengthSqr() < DIRECTION_EPSILON
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : look.normalize();
        double outwardYaw = Math.toRadians(bodyYawDegrees + 90.0F);
        Vec3 outward = new Vec3(-Math.sin(outwardYaw), 0.0D, Math.cos(outwardYaw));
        double side = leftHand ? -LENS_OUTWARD : LENS_OUTWARD;

        return eye.add(forward.scale(LENS_FORWARD))
                .add(outward.scale(side))
                .add(0.0D, -LENS_DROP, 0.0D);
    }
}
