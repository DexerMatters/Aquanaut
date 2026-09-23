package com.dexer.aquanaut.common.drone;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Physical targeting constants shared by the server block light and client visual light. */
public final class DroneHeadlightGeometry {
    /** A little farther than the handheld lamp in air. */
    public static final double AIR_RANGE = 24.0D;
    /** A little farther than the handheld lamp underwater too. */
    public static final double SUBMERGED_RANGE = 16.0D;
    /** The lens is just ahead of the drone's nose sensor. */
    private static final double LENS_FORWARD = 0.28D;

    private DroneHeadlightGeometry() {
    }

    public static double range(boolean submerged) {
        return submerged ? SUBMERGED_RANGE : AIR_RANGE;
    }

    public static Vec3 lens(Entity drone, float partialTick) {
        Vec3 direction = drone.getViewVector(partialTick);
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return drone.getEyePosition(partialTick).add(direction.normalize().scale(LENS_FORWARD));
    }
}
