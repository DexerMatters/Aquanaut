package com.dexer.aquanaut.common.sonar;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where a contact lands on the scope.
 *
 * <p>
 * The scope is a plan position indicator: the transducer sits at the middle, true bearing runs
 * around the dial, and range runs outwards. That is the one view of a ping that stays readable
 * whatever the diver is doing with their head — a contact ahead is always up the screen — and it is
 * the picture a real sounder draws. Its one blind spot is height, which a plan view cannot show, so
 * the vertical offset is carried alongside and drawn as a stem on the blip rather than being folded
 * into the radius. Plotting the true three-dimensional distance instead would put a fish directly
 * below in the middle of the dial and a fish directly ahead on the rim, which is worse than useless
 * when the two are the same distance away in the same direction of interest.
 *
 * <p>
 * The dial faces the diver, not the world: ahead is up. {@code yaw} is the vanilla yaw in degrees,
 * where the look direction is {@code (-sin yaw, 0, cos yaw)} and the diver's right is
 * {@code (-cos yaw, 0, -sin yaw)}. Turning on the spot therefore swings the whole plot, which is
 * what a handheld instrument does and what makes "which way do I swim" answerable at a glance.
 *
 * <p>
 * Deliberately free of Minecraft and of any renderer, so the projection can be checked by a plain
 * unit test rather than by looking at the screen.
 */
public final class SonarPlot {

    private SonarPlot() {
    }

    /** How far ahead of the diver a contact lies, in blocks; negative is behind. */
    public static double ahead(Vec3 offset, float yaw) {
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        return offset.x * -sin + offset.z * cos;
    }

    /** How far to the diver's right a contact lies, in blocks; negative is to the left. */
    public static double athwart(Vec3 offset, float yaw) {
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        return offset.x * -cos + offset.z * -sin;
    }

    /** How far off the bow a contact lies, in degrees, positive to the right, in {@code -180..180}. */
    public static float bearing(Vec3 offset, float yaw) {
        double ahead = ahead(offset, yaw);
        double athwart = athwart(offset, yaw);
        if (ahead == 0.0D && athwart == 0.0D) {
            return 0.0F;
        }
        return (float) Math.toDegrees(Math.atan2(athwart, ahead));
    }

    /** The distance across the ground to a contact, which is the radius the dial plots it at. */
    public static double planar(Vec3 offset) {
        return Math.sqrt(offset.x * offset.x + offset.z * offset.z);
    }

    /** How far above the transducer a contact is; negative is below it, which is where the fish are. */
    public static double above(Vec3 offset) {
        return offset.y;
    }

    /** How much of the dial a contact fills, {@code 0} at the middle and {@code 1} at the rim. */
    public static float rangeFraction(Vec3 offset) {
        return Mth.clamp((float) (planar(offset) / SonarPulse.RANGE), 0.0F, 1.0F);
    }

    /** Screen X of a contact on a dial of {@code radius} centred on {@code centreX}. */
    public static float plotX(Vec3 offset, float yaw, float centreX, float radius) {
        return centreX + (float) (athwart(offset, yaw) / SonarPulse.RANGE) * radius;
    }

    /** Screen Y of a contact on a dial of {@code radius} centred on {@code centreY}. */
    public static float plotY(Vec3 offset, float yaw, float centreY, float radius) {
        return centreY - (float) (ahead(offset, yaw) / SonarPulse.RANGE) * radius;
    }

    /**
     * The length of a blip's height stem, in dial units, positive above the contact and negative
     * below. Scaled against the range rather than against the world so that a contact near the rim
     * does not grow a stem off the edge of the dial, and compressed, because the interesting thing
     * about height is only ever its sign.
     */
    public static float stem(Vec3 offset, float radius) {
        return Mth.clamp((float) (above(offset) / SonarPulse.RANGE) * radius * 0.55F,
                -radius * 0.42F, radius * 0.42F);
    }

    /**
     * The compass heading the diver is facing, in degrees, north zero and east ninety. Vanilla yaw
     * zero faces south — {@code +Z} — so the heading is the yaw turned half a circle.
     */
    public static float heading(float yaw) {
        return (Mth.wrapDegrees(yaw + 180.0F) + 360.0F) % 360.0F;
    }

    /** The name of a point of the compass, for the readout. */
    public static String compassPoint(float heading) {
        String[] points = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
        int index = Mth.floor(((Mth.wrapDegrees(heading) + 22.5F) % 360.0F + 360.0F) % 360.0F / 45.0F) % 8;
        return points[index];
    }
}
