package com.dexer.aquanaut;

import com.dexer.aquanaut.common.item.SearchlightGeometry;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the shared placement maths used by the server block-light manager. */
final class SearchlightGeometryTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    void theLensSitsInFrontOfAndBesideTheEye() {
        Vec3 eye = new Vec3(0.0D, 64.0D, 0.0D);
        Vec3 north = new Vec3(0.0D, 0.0D, -1.0D);

        Vec3 rightHand = SearchlightGeometry.lens(eye, north, 180.0F, false);
        Vec3 leftHand = SearchlightGeometry.lens(eye, north, 180.0F, true);

        assertTrue(rightHand.z < eye.z);
        assertTrue(rightHand.y < eye.y);
        assertEquals(eye.y - rightHand.y, eye.y - leftHand.y, EPSILON);
        assertTrue(rightHand.x > eye.x);
        assertTrue(leftHand.x < eye.x);
        assertEquals(rightHand.z, leftHand.z, EPSILON);
    }

    @Test
    void theSideOffsetSurvivesLookingStraightUpOrDown() {
        Vec3 eye = new Vec3(10.0D, 64.0D, 10.0D);

        Vec3 up = SearchlightGeometry.lens(eye, new Vec3(0.0D, 1.0D, 0.0D), 90.0F, false);
        Vec3 down = SearchlightGeometry.lens(eye, new Vec3(0.0D, -1.0D, 0.0D), 90.0F, false);

        assertTrue(up.y > eye.y);
        assertTrue(down.y < eye.y);

        for (Vec3 lens : new Vec3[] { up, down }) {
            assertTrue(Math.hypot(lens.x - eye.x, lens.z - eye.z) > 0.1D);
        }
    }

    @Test
    void aDegenerateDirectionStillProducesStableTargetMath() {
        Vec3 eye = new Vec3(0.0D, 64.0D, 0.0D);
        Vec3 lens = SearchlightGeometry.lens(eye, Vec3.ZERO, 0.0F, false);
        Vec3 target = SearchlightGeometry.targetPoint(lens, Vec3.ZERO, 4.0D);

        assertTrue(target.z > lens.z);
        assertTrue(Math.sqrt(Math.pow(lens.x - target.x, 2.0D)
                + Math.pow(lens.y - target.y, 2.0D)
                + Math.pow(lens.z - target.z, 2.0D)) > 3.9D);
    }

    @Test
    void airAndWaterKeepTheirDistinctRanges() {
        assertEquals(SearchlightGeometry.AIR_RANGE, SearchlightGeometry.range(false), EPSILON);
        assertEquals(SearchlightGeometry.SUBMERGED_RANGE, SearchlightGeometry.range(true), EPSILON);
        assertTrue(SearchlightGeometry.SUBMERGED_RANGE < SearchlightGeometry.AIR_RANGE);
    }

    @Test
    void aMissUsesFullRangeAndAHitStopsJustBeforeTheWall() {
        assertEquals(SearchlightGeometry.AIR_RANGE,
                SearchlightGeometry.targetDistance(SearchlightGeometry.AIR_RANGE,
                        Double.POSITIVE_INFINITY), EPSILON);
        assertEquals(2.99D,
                SearchlightGeometry.targetDistance(SearchlightGeometry.AIR_RANGE, 3.0D), EPSILON);
        assertEquals(0.09D,
                SearchlightGeometry.targetDistance(SearchlightGeometry.AIR_RANGE, 0.1D), EPSILON);
    }

    @Test
    void targetPointFollowsTheNormalizedAimAxis() {
        Vec3 origin = new Vec3(4.0D, 8.0D, -2.0D);
        Vec3 target = SearchlightGeometry.targetPoint(origin, new Vec3(0.0D, 0.0D, 2.0D), 5.0D);

        assertEquals(origin.x, target.x, EPSILON);
        assertEquals(origin.y, target.y, EPSILON);
        assertEquals(3.0D, target.z, EPSILON);
        assertEquals(origin.x, SearchlightGeometry.targetPoint(origin, new Vec3(1.0D, 0.0D, 0.0D), -1.0D).x,
                EPSILON);
    }

    @Test
    void placementBackoffSearchesQuarterBlocksForAtMostTwoBlocks() {
        double terminal = 10.0D;

        assertEquals(terminal, SearchlightGeometry.placementDistance(terminal, 0), EPSILON);
        assertEquals(9.75D, SearchlightGeometry.placementDistance(terminal, 1), EPSILON);
        assertEquals(8.0D, SearchlightGeometry.placementDistance(terminal, 8), EPSILON);
        assertEquals(8.0D, SearchlightGeometry.placementDistance(terminal, 100), EPSILON);
    }
}
