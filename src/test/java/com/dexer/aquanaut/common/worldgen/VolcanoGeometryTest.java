package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shape guarantees of the Brimstone Caldera volcanic fields: giant stratovolcanoes with
 * concave flanks, raked by barrancos, carrying breached rims and broad summit vents, plus
 * the satellite scoria-cone field of the plain, and breach conduits that always tie a vent
 * into the upper floor.
 */
public final class VolcanoGeometryTest {
    private static final int FLOOR_Y = -15;

    @Test
    void breachPlugsAlwaysReachTheUpperFloor() {
        int breaches = 0;
        for (int cellX = -12; cellX <= 12; cellX++) {
            for (int cellZ = -12; cellZ <= 12; cellZ++) {
                VolcanoGeometry.Volcano volcano = VolcanoGeometry.volcanoAt(cellX, cellZ);
                if (volcano == null || !volcano.breach()) {
                    continue;
                }
                breaches++;
                double plugTop = VolcanoGeometry.plugTopY(volcano, FLOOR_Y, 1.0D);
                assertTrue(plugTop >= VolcanoGeometry.BREACH_TOP_MIN_Y,
                        "breach plug must pierce the reef cap (Y " + plugTop + " >= "
                                + VolcanoGeometry.BREACH_TOP_MIN_Y + ")");
            }
        }
        assertTrue(breaches > 0, "the sample region should contain breach volcanoes");
    }

    @Test
    void volcanoesAreGiantMassifs() {
        int checked = 0;
        for (int cellX = -12; cellX <= 12; cellX++) {
            for (int cellZ = -12; cellZ <= 12; cellZ++) {
                VolcanoGeometry.Volcano volcano = VolcanoGeometry.volcanoAt(cellX, cellZ);
                if (volcano == null) {
                    continue;
                }
                checked++;
                assertTrue(volcano.baseRadius() >= 24, "massifs are giant (" + volcano.baseRadius() + ")");
                assertTrue(volcano.height() >= 32, "summits are majestic (" + volcano.height() + ")");
                assertTrue(volcano.craterRadius() >= 6, "calderas are wide");
                assertTrue(volcano.plugRadius() >= 3, "the vent is never a one-block spike");
                assertTrue(volcano.breachAzimuthDeg() >= 0 && volcano.breachAzimuthDeg() < 360,
                        "the breach azimuth is a real bearing");
            }
        }
        assertTrue(checked > 0, "the sample region should contain volcanoes");
    }

    @Test
    void craterInteriorKeepsPoolAndOpenWaterAbove() {
        VolcanoGeometry.Volcano volcano = firstVolcano(v -> v.fillDepth() > 0);
        // The probe sits in the ring of crater floor between the summit vent and the wall.
        int probeRadius = (int) Math.round(volcano.craterRadius() * 0.85D);
        VolcanoGeometry.ColumnShape shape = VolcanoGeometry.edificeShape(
                volcano, null, volcano.centerX() + probeRadius, volcano.centerZ(), FLOOR_Y, 1.0D);
        assertEquals(volcano, shape.volcano(), "probe column should belong to the summit edifice");
        assertTrue(shape.craterInterior(), "column between dome and wall is crater interior");
        assertTrue(shape.fillY() > shape.surfaceY(), "pool fills above the crater floor");
        assertTrue(shape.openTopY() > shape.fillY(), "open water stands above the pool");
    }

    @Test
    void summitVentsAreBroadDomesNotSpires() {
        VolcanoGeometry.Volcano volcano = firstVolcano(v -> !v.breach()
                && v.craterType() != VolcanoGeometry.CraterType.SULFUR_PAN);
        double domeRadius = VolcanoGeometry.domeRadius(volcano, 1.0D);
        assertTrue(domeRadius >= 2.0D, "the vent has girth (" + domeRadius + ")");

        VolcanoGeometry.ColumnShape centre = VolcanoGeometry.edificeShape(
                volcano, null, volcano.centerX(), volcano.centerZ(), FLOOR_Y, 1.0D);
        VolcanoGeometry.ColumnShape shoulder = VolcanoGeometry.edificeShape(
                volcano, null, volcano.centerX() + (int) Math.round(domeRadius * 0.85D),
                volcano.centerZ(), FLOOR_Y, 1.0D);
        assertTrue(centre.surfaceY() > FLOOR_Y + volcano.height() * 0.40D,
                "the dome stands proud of the crater floor");
        assertTrue(centre.surfaceY() - shoulder.surfaceY() < volcano.height() * 0.35D,
                "the dome top is rounded, not a needle");
    }

    @Test
    void breachNecksAreBroadAndFlatTopped() {
        VolcanoGeometry.Volcano volcano = firstVolcano(v -> v.breach());
        double domeRadius = VolcanoGeometry.domeRadius(volcano, 1.0D);
        assertTrue(domeRadius >= 2.0D, "the breach neck is a massif, not a pillar");
        VolcanoGeometry.ColumnShape centre = VolcanoGeometry.edificeShape(
                volcano, null, volcano.centerX(), volcano.centerZ(), FLOOR_Y, 1.0D);
        VolcanoGeometry.ColumnShape flank = VolcanoGeometry.edificeShape(
                volcano, null, volcano.centerX() + (int) Math.round(domeRadius * 0.5D),
                volcano.centerZ(), FLOOR_Y, 1.0D);
        assertTrue(VolcanoGeometry.plugTopY(volcano, FLOOR_Y, 1.0D) >= VolcanoGeometry.BREACH_TOP_MIN_Y);
        assertTrue(centre.surfaceY() - flank.surfaceY() < volcano.height() * 0.25D,
                "the neck climbs as a thick buttress, not a needle");
    }

    @Test
    void spillChuteFollowsTheBreachAzimuthOnly() {
        VolcanoGeometry.Volcano volcano = firstVolcano(v -> v.breach());
        double azimuth = Math.toRadians(volcano.breachAzimuthDeg());
        int probe = (int) Math.round(volcano.craterRadius() * 1.6D);
        VolcanoGeometry.ColumnShape inChute = VolcanoGeometry.edificeShape(volcano, null,
                volcano.centerX() + (int) Math.round(Math.cos(azimuth) * probe),
                volcano.centerZ() + (int) Math.round(Math.sin(azimuth) * probe), FLOOR_Y, 1.0D);
        VolcanoGeometry.ColumnShape offChute = VolcanoGeometry.edificeShape(volcano, null,
                volcano.centerX() - (int) Math.round(Math.cos(azimuth) * probe),
                volcano.centerZ() - (int) Math.round(Math.sin(azimuth) * probe), FLOOR_Y, 1.0D);
        assertTrue(inChute.channelStrength() > 0.35D,
                "the spill chute runs down the breach bearing");
        assertEquals(0.0D, offChute.channelStrength(), 1e-9D,
                "the opposite flank stays intact");
        assertTrue(inChute.surfaceY() < offChute.surfaceY(),
                "the chute is carved below the untouched flank");
    }

    @Test
    void breachedRimsCarryANotchAndSpillChannel() {
        VolcanoGeometry.Volcano breach = firstVolcano(v -> v.breach());
        double atBreach = VolcanoGeometry.breachFactor(breach,
                Math.toRadians(breach.breachAzimuthDeg()));
        double offNotch = VolcanoGeometry.breachFactor(breach,
                Math.toRadians(breach.breachAzimuthDeg() + 90.0D));
        assertEquals(1.0D, atBreach, 1e-6D, "the rim is torn open at the breach bearing");
        assertEquals(0.0D, offNotch, 1e-9D, "the rest of the rim stays raised");

        VolcanoGeometry.Volcano quiet = firstVolcano(v -> !v.breach());
        assertEquals(0.0D, VolcanoGeometry.breachFactor(quiet, 0.0D), 1e-9D,
                "quiet volcanoes keep an unbroken rim");
    }

    @Test
    void flankProfileFallsMonotonicallyBeneathTheBarrancos() {
        VolcanoGeometry.Volcano volcano = firstVolcano(v -> true);
        double previous = Double.POSITIVE_INFINITY;
        for (int r = (int) Math.ceil(volcano.craterRadius() * 1.4D);
                r <= volcano.baseRadius() * 0.75D; r++) {
            // Average the ring: individual barrancos and spurs are allowed to be rugged,
            // the flank as a whole must still descend toward the apron.
            double sum = 0.0D;
            int samples = 0;
            for (int step = 0; step < 16; step++) {
                double angle = step * Math.PI / 8.0D;
                VolcanoGeometry.ColumnShape shape = VolcanoGeometry.edificeShape(volcano, null,
                        volcano.centerX() + (int) Math.round(Math.cos(angle) * r),
                        volcano.centerZ() + (int) Math.round(Math.sin(angle) * r), FLOOR_Y, 1.0D);
                assertEquals(volcano, shape.volcano(), "flank column stays on the main cone");
                sum += shape.surfaceY();
                samples++;
            }
            double ring = sum / samples;
            assertTrue(ring <= previous + 0.5D, "ring height must fall toward the apron");
            previous = ring;
        }
    }

    @Test
    void columnsAreDeterministicAcrossCalls() {
        for (int x = -200; x <= 200; x += 37) {
            VolcanoGeometry.ColumnShape first = VolcanoGeometry.shapeAt(x, x / 2, FLOOR_Y, 0.8D);
            VolcanoGeometry.ColumnShape second = VolcanoGeometry.shapeAt(x, x / 2, FLOOR_Y, 0.8D);
            assertEquals(first, second, "column plans must be pure functions of position");
        }
    }

    @Test
    void plainColumnsCarryAshApronsButNoEdifice() {
        for (int x = -500; x <= 500; x += 25) {
            VolcanoGeometry.ColumnShape shape = VolcanoGeometry.shapeAt(x, x, FLOOR_Y, 1.0D);
            if (!shape.partOfEdifice()) {
                assertTrue(shape.apronHeight() >= 0.0D, "ash aprons never dig below the floor");
                assertFalse(shape.craterInterior(), "plains have no craters");
                assertNull(shape.volcano());
            }
        }
    }

    @Test
    void parasitesStayOnTheFlanksAndBelowTheSummit() {
        for (int cellX = -12; cellX <= 12; cellX++) {
            for (int cellZ = -12; cellZ <= 12; cellZ++) {
                VolcanoGeometry.Volcano volcano = VolcanoGeometry.volcanoAt(cellX, cellZ);
                if (volcano == null) {
                    continue;
                }
                assertTrue(VolcanoGeometry.strataTableValid(volcano));
                for (VolcanoGeometry.Parasite parasite : volcano.parasites()) {
                    double offset = Math.sqrt(parasite.offsetX() * parasite.offsetX()
                            + parasite.offsetZ() * parasite.offsetZ());
                    assertTrue(parasite.radius() < volcano.baseRadius(), "parasites stay smaller");
                    assertTrue(parasite.height() < volcano.height(), "parasites stay lower");
                    assertTrue(offset > volcano.baseRadius() * 0.3D
                                    && offset < volcano.baseRadius() * 0.9D,
                            "parasites sit on the flanks, not the summit");
                }
            }
        }
    }

    @Test
    void districtStrengthFadesSmoothlyAndNeverAboveOne() {
        assertEquals(0.0D, VolcanoGeometry.strength(0.2D, 1.0D), 1e-9);
        assertEquals(0.0D, VolcanoGeometry.strength(0.5D, 0.0D), 1e-9);
        double weak = VolcanoGeometry.strength(0.4D, 1.0D);
        double strong = VolcanoGeometry.strength(0.55D, 1.0D);
        assertTrue(weak > 0.0D && strong <= 1.0D && strong >= weak, "strength is monotone in [0, 1]");
    }

    @Test
    void volcanicPlainReliefStaysModest() {
        for (int x = -300; x <= 300; x += 13) {
            int offset = VolcanoGeometry.floorOffset(x, -x / 3);
            assertTrue(offset >= -16 && offset <= 12,
                    "swells and rifts stay gentle (" + offset + ")");
        }
    }

    @Test
    void breachRubbleSpreadsOnlyAroundThePlug() {
        VolcanoGeometry.Volcano volcano = firstVolcano(v -> true);
        double plugTop = VolcanoGeometry.plugTopY(volcano, FLOOR_Y, 1.0D);
        if (volcano.breach()) {
            assertTrue(VolcanoGeometry.breachRubbleTop(volcano, volcano.plugRadius() + 1.5D,
                    plugTop, 1.0D) > plugTop - 5.0D, "rubble hugs the conduit");
            assertEquals(0.0D, VolcanoGeometry.breachRubbleTop(volcano, volcano.baseRadius(),
                    plugTop, 1.0D), 1e-9);
        } else {
            assertEquals(0.0D, VolcanoGeometry.breachRubbleTop(volcano, 2.0D, plugTop, 1.0D),
                    1e-9, "quiet volcanoes spill no upper-floor rubble");
        }
    }

    @Test
    void satelliteVentFieldCoversThePlain() {
        int active = 0;
        int total = 0;
        for (int cellX = -10; cellX <= 10; cellX++) {
            for (int cellZ = -10; cellZ <= 10; cellZ++) {
                total++;
                if (VolcanoGeometry.stackAt(cellX, cellZ) != null) {
                    active++;
                }
            }
        }
        assertTrue(active > total * 0.40D,
                "the plain is a volcanic field of many satellite vents (" + active + "/" + total + ")");
    }

    @Test
    void satelliteVentsAreWideLowConesNotPillars() {
        int checked = 0;
        for (int cellX = -10; cellX <= 10 && checked < 24; cellX++) {
            for (int cellZ = -10; cellZ <= 10 && checked < 24; cellZ++) {
                VolcanoGeometry.Stack stack = VolcanoGeometry.stackAt(cellX, cellZ);
                if (stack == null) {
                    continue;
                }
                checked++;
                assertTrue(stack.radiusX() >= 3.5D && stack.radiusZ() >= 1.9D,
                        "satellite vents are broad landforms");
                assertTrue(stack.height() <= stack.radiusX() * 2.5D,
                        "satellite vents are low cones, not needles");
                VolcanoGeometry.StackShape centre = VolcanoGeometry.stackShapeAt(
                        stack.centerX(), stack.centerZ(), FLOOR_Y, 1.0D);
                assertTrue(centre.present(), "the vent axis stands on its own vent");
                assertTrue(centre.topY() > FLOOR_Y, "the vent rises above the plain");
                assertTrue(centre.topY() <= FLOOR_Y + 10.0D, "the vent honours its low profile");
                assertEquals(VolcanoGeometry.stackShapeAt(stack.centerX(), stack.centerZ(),
                                FLOOR_Y, 1.0D),
                        centre, "vent shading is a pure function of position");
                assertFalse(VolcanoGeometry.stackShapeAt(stack.centerX(), stack.centerZ(),
                                FLOOR_Y, 0.2D).present(),
                        "satellite vents only erupt inside the volcanic district");
            }
        }
        assertTrue(checked > 5, "sample should contain satellite vents");
    }

    private static VolcanoGeometry.Volcano firstVolcano(
            java.util.function.Predicate<VolcanoGeometry.Volcano> predicate) {
        for (int cellX = -12; cellX <= 12; cellX++) {
            for (int cellZ = -12; cellZ <= 12; cellZ++) {
                VolcanoGeometry.Volcano volcano = VolcanoGeometry.volcanoAt(cellX, cellZ);
                if (volcano != null && predicate.test(volcano)) {
                    assertNotNull(volcano.strata());
                    return volcano;
                }
            }
        }
        throw new AssertionError("sample region should contain a matching volcano");
    }
}
