package com.dexer.aquanaut.client.renderer;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lance is drawn as flat ribbons and helical strands, so the maths that builds it has to hold up
 * without a graphics context: an axis that stays perpendicular (and finite) from every angle, a helix
 * that keeps a constant radius around the beam, and an envelope that actually tapers.
 */
public final class LaserGeometryTest {

    private static final double EPSILON = 1.0E-6D;

    @Test
    void widenAxisIsPerpendicularToTheBeamAndTheView() {
        Vec3 direction = new Vec3(0.3D, -0.2D, 0.9D).normalize();
        Vec3 look = new Vec3(-0.5D, 0.1D, 0.4D).normalize();

        Vec3 axis = LaserGeometry.cameraFacingAxis(direction, look);

        assertEquals(1.0D, axis.length(), EPSILON, "the widen axis must be a unit vector");
        assertEquals(0.0D, axis.dot(direction), EPSILON, "the widen axis must be perpendicular to the beam");
        assertEquals(0.0D, axis.dot(look), EPSILON, "the widen axis must be perpendicular to the view");
        assertFinite(axis);
    }

    @Test
    void widenAxisSurvivesADegenerateCameraAngle() {
        // Looking straight down the beam, and straight down the world: both crosses collapse and the
        // fallbacks have to keep the ribbons from turning into NaNs.
        Vec3 beam = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 straightUp = new Vec3(0.0D, 1.0D, 0.0D);

        for (Vec3 look : new Vec3[] { beam, beam.scale(-1.0D), straightUp, new Vec3(0.0D, -1.0D, 0.0D) }) {
            Vec3 axis = LaserGeometry.cameraFacingAxis(beam, look);
            assertFinite(axis);
            assertEquals(1.0D, axis.length(), EPSILON, "degenerate fallback must still be a unit vector");
            assertEquals(0.0D, axis.dot(beam), EPSILON, "degenerate fallback must stay perpendicular");
        }

        // Even a zero-length beam (a corrupt aim vector) must not produce NaNs.
        assertFinite(LaserGeometry.cameraFacingAxis(Vec3.ZERO, Vec3.ZERO));
        assertFinite(LaserGeometry.cameraFacingAxis(new Vec3(0.0D, 1.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D)));
    }

    @Test
    void helixStrandsKeepTheirRadiusAroundTheBeam() {
        Vec3 direction = new Vec3(1.0D, 2.0D, -0.5D).normalize();
        Vec3 side = LaserGeometry.cameraFacingAxis(direction, new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 side2 = direction.cross(side).normalize();

        for (double t = 0.0D; t <= 1.0D; t += 0.125D) {
            for (double phase : new double[] { 0.0D, Math.PI }) {
                for (double age : new double[] { 0.0D, 7.5D, 120.0D }) {
                    Vec3 offset = LaserGeometry.helixOffset(side, side2, t, phase, age);
                    assertFinite(offset);
                    assertEquals(LaserGeometry.helixRadius(t), offset.length(), 1.0E-5D,
                            "helix radius at t=" + t);
                    assertEquals(0.0D, offset.dot(direction), 1.0E-5D,
                            "helix offsets must stay perpendicular to the beam");
                }
            }
        }

        // The cone must open up along the beam, or the strands hug the core the whole way.
        assertTrue(LaserGeometry.helixRadius(1.0D) > LaserGeometry.helixRadius(0.0D) * 1.5D,
                "the helix cone must widen toward the far end");
    }

    @Test
    void theStrandsActuallyRotate() {
        Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 side = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side2 = new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 first = LaserGeometry.helixOffset(side, side2, 0.5D, 0.0D, 0.0D);
        Vec3 later = LaserGeometry.helixOffset(side, side2, 0.5D, 0.0D, 20.0D);

        assertTrue(first.subtract(later).length() > LaserGeometry.helixRadius(0.5D) * 0.5D,
                "advancing age must move a strand around the beam");
    }

    @Test
    void theEnvelopeTapersToNothing() {
        assertEquals(0.0F, LaserGeometry.fade(0.0D), 1.0E-6F, "the beam must not start behind the muzzle");
        assertEquals(0.0F, LaserGeometry.envelope(1.0F), 1.0E-6F, "the lance must end completely");
        assertEquals(1.0F, LaserGeometry.envelope(0.0F), 1.0E-6F, "the lance must start at full brightness");

        float previous = 2.0F;
        for (double t = 0.05D; t <= 1.0D; t += 0.05D) {
            float fade = LaserGeometry.fade(t);
            assertTrue(fade > 0.0F, "the beam must stay lit along its length");
            assertTrue(fade < previous, "energy must dim as it travels");
            previous = fade;
        }
        assertTrue(previous > 0.5F, "the far end must still be visible, was " + previous);
        assertEquals(1.0F, LaserGeometry.envelope(-3.0F), 1.0E-6F,
                "a not-yet-elapsed window must clamp to full brightness");
    }

    private static void assertFinite(Vec3 vector) {
        assertTrue(Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z),
                "beam geometry produced a non-finite vector: " + vector);
    }
}
