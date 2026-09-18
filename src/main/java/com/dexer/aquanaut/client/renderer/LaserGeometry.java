package com.dexer.aquanaut.client.renderer;

import net.minecraft.world.phys.Vec3;

/**
 * Pure geometry for the Opticichthus lance, kept separate from the renderer so the maths that
 * decides how the beam is built can be tested without a graphics context.
 *
 * <p>
 * The beam is a bundle of flat ribbons plus two helical strands. Every one of them widens or spirals
 * along an axis perpendicular to the beam, so none of this can be allowed to collapse when the beam
 * points straight at the camera or straight up.
 */
public final class LaserGeometry {

    /** Radius of the helix cone at the far end of the beam. */
    public static final double HELIX_RADIUS = 0.11D;
    /** How many times a strand wraps the beam over its full length. */
    public static final double HELIX_TURNS = 2.2D;

    private LaserGeometry() {
    }

    /**
     * The axis a ribbon is widened along so a flat quad still reads as a cylinder: perpendicular to
     * both the beam and the view direction. Falls back to any perpendicular when the beam points
     * straight down the camera's throat (or straight up, where the up-vector cross degenerates).
     */
    public static Vec3 cameraFacingAxis(Vec3 direction, Vec3 look) {
        Vec3 side = direction.cross(look);
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-6D) {
            // The beam itself is degenerate; anything perpendicular will do, but it must not be NaN.
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        return side.normalize();
    }

    /** The helix cone opens up as the lance travels, which is what makes it look pressurised. */
    public static double helixRadius(double t) {
        return HELIX_RADIUS * (0.55D + 0.85D * t);
    }

    /** Offset of a strand from the beam axis at {@code t}, spinning as {@code age} advances. */
    public static Vec3 helixOffset(Vec3 side, Vec3 side2, double t, double phase, double age) {
        double angle = t * HELIX_TURNS * Math.PI * 2.0D + age * 0.55D + phase;
        double radius = helixRadius(t);
        return side.scale(Math.cos(angle) * radius).add(side2.scale(Math.sin(angle) * radius));
    }

    /** Energy dims as it travels, and eases in over the first sliver so the muzzle stays clean. */
    public static float fade(double t) {
        if (t <= 0.0D) {
            return 0.0F;
        }
        double entry = Math.min(1.0D, t / 0.04D);
        return (float) (entry * (1.0D - 0.42D * Math.min(1.0D, t)));
    }

    /** 0..1 lance brightness through the firing window: a flash that tapers away. */
    public static float envelope(float progress) {
        return clamp(1.0F - progress);
    }

    public static float clamp(float value) {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }
}
