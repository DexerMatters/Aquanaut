package com.dexer.aquanaut;

import com.dexer.aquanaut.common.entity.DetectorGeometry;
import com.dexer.aquanaut.common.entity.DetectorScan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the biological detector's array arithmetic: the shrink from the scan volume to the sphere,
 * the bearing a return is plotted at, and the moment the array wakes up.
 *
 * <p>
 * These are the numbers the hologram is drawn from and the only ones in the machine that can be
 * wrong without anything failing to load. A scale that no longer maps the range onto the shell would
 * put contacts outside the sphere or bunch them uselessly at its centre; a sweep lag with the wrong
 * sign would light every return a full turn early and make the whole display read backwards; and an
 * off-by-one on the wake tick would start the hull spinning while the animation still has it folded.
 * None of that shows up at build time, and all of it is arithmetic, so it is checked here rather
 * than by eye.
 */
final class DetectorScanTest {

    /**
     * The whole point of the mapping: a return at the edge of the array's range is drawn on the
     * shell, and everything nearer is drawn proportionally inside it.
     */
    @Test
    void theWholeRangeIsDrawnOnTheShell() {
        assertEquals(DetectorScan.SPHERE_RADIUS, DetectorScan.RANGE * DetectorScan.scale(), 1.0E-6F);
        assertTrue(DetectorScan.scale() < 0.1F,
                "the picture is only a picture if it is a reduction: " + DetectorScan.scale());
    }

    /**
     * Half way out is half way in, on every axis, because the plot is a scaled copy of the water and
     * not a projection: a creature straight above the buoy is drawn above the middle of the sphere.
     */
    @Test
    void plottingIsTheSameShrinkOnEveryAxis() {
        float scale = DetectorScan.scale();
        assertEquals(DetectorScan.SPHERE_RADIUS / 2.0F, DetectorScan.RANGE / 2.0F * scale, 1.0E-5F);
        assertEquals(DetectorScan.RANGE * scale, DetectorScan.RANGE * scale, 0.0F);
        assertTrue(DetectorScan.SPHERE_CENTRE_Y > 0.0F && DetectorScan.SPHERE_CENTRE_Y < 0.5F,
                "the sphere should be centred on the hull, not above or below it");
    }

    /** On the bearing means on the bearing, whatever the bearing is. */
    @Test
    void theBeamIsOnBearingWhenItPointsThere() {
        for (float bearing = -350.0F; bearing <= 350.0F; bearing += 17.0F) {
            assertEquals(0.0F, DetectorScan.sweepLag(bearing, bearing), 1.0E-3F,
                    "the beam lagged itself at " + bearing);
        }
    }

    /**
     * The lag grows as the beam moves on, and wraps when it comes back around rather than running
     * negative — the sign of this is the difference between a return being lit as the beam arrives
     * and being lit the instant it has left.
     */
    @Test
    void theLagGrowsBehindTheBeamAndWrapsAtAFullTurn() {
        assertEquals(90.0F, DetectorScan.sweepLag(90.0F, 0.0F), 1.0E-4F);
        assertEquals(270.0F, DetectorScan.sweepLag(0.0F, 90.0F), 1.0E-4F);
        assertEquals(20.0F, DetectorScan.sweepLag(10.0F, 350.0F), 1.0E-4F);
        assertEquals(340.0F, DetectorScan.sweepLag(350.0F, 10.0F), 1.0E-4F);
        assertEquals(350.0F, DetectorScan.sweepLag(-10.0F, 0.0F), 1.0E-4F);
        assertEquals(0.0F, DetectorScan.sweepLag(720.0F, 0.0F), 1.0E-4F,
                "a beam that has been round twice is still on the bearing");
    }

    /** Whatever the beam and the return are doing, the lag is an angle in one turn. */
    @Test
    void theLagIsAlwaysInsideOneTurn() {
        for (float sweep = -720.0F; sweep <= 720.0F; sweep += 7.0F) {
            for (float bearing = -360.0F; bearing <= 360.0F; bearing += 11.0F) {
                float lag = DetectorScan.sweepLag(sweep, bearing);
                assertTrue(lag >= 0.0F && lag < 360.0F,
                        "lag of " + bearing + " behind " + sweep + " was " + lag);
            }
        }
    }

    /**
     * The array wakes exactly as the deployment clip hands over, and a full scan takes the four
     * seconds the spin rate promises.
     */
    @Test
    void theArrayWakesWhenTheDeploymentClipHandsOver() {
        assertEquals(Math.round(DetectorGeometry.RELEASE_SECONDS * 20.0F), DetectorScan.DEPLOY_TICKS);
        assertEquals(36, DetectorScan.DEPLOY_TICKS, "the release clip is 1.8 seconds");
        assertEquals(4.0F, 360.0F / DetectorScan.SPIN_DEGREES_PER_TICK / 20.0F, 1.0E-3F,
                "a full sweep should take about four seconds");
        assertTrue(DetectorScan.BOOT_TICKS > 0.0F && DetectorScan.BOOT_TICKS < DetectorScan.DEPLOY_TICKS,
                "the hologram has to come up inside the deployment, not after it");
        assertTrue(DetectorScan.BOOT_START_SCALE > 0.0F && DetectorScan.BOOT_START_SCALE < 1.0F);
    }
}
