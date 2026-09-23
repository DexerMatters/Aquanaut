package com.dexer.aquanaut;

import com.dexer.aquanaut.common.item.CompassTargeting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The compass targeting rules: what it points at, and how far round the needle goes for that
 * bearing.
 *
 * <p>
 * The bearing maths mirrors vanilla's {@code CompassItemPropertyFunction}, so the expected values
 * below are the behaviour a player already knows from a vanilla compass.
 */
final class CompassTargetingTest {

    private static final Path COMPASS_MODEL = Path.of(
            "src/main/resources/assets/aquanaut/models/item/submarine_compass.json");
    private static final Path FRAME_DIR = Path.of(
            "src/main/resources/assets/aquanaut/textures/item/compass");

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    // ------------------------------------------------------------------
    // which cursor
    // ------------------------------------------------------------------

    @Test
    void nothingToPointAtGivesNoTarget() {
        assertTrue(CompassTargeting.nearest(List.of(), 0.0D, 0.0D).isEmpty());
    }

    @Test
    void nearestPicksTheClosestCursor() {
        List<CompassTargeting.Candidate> candidates = List.of(
                new CompassTargeting.Candidate(A, 30.0D, 0.0D),
                new CompassTargeting.Candidate(B, 4.0D, 3.0D),
                new CompassTargeting.Candidate(C, -50.0D, 0.0D));

        Optional<CompassTargeting.Candidate> nearest = CompassTargeting.nearest(candidates, 0.0D, 0.0D);
        assertTrue(nearest.isPresent());
        assertEquals(B, nearest.get().id(), "4,3 is 5 blocks away and beats both 30 and 50");
    }

    @Test
    void nearestIgnoresHeightBecauseItIsMeasuredInTheHorizontalPlane() {
        // Two pins at the same horizontal spot: the first one wins rather than the comparison
        // depending on which is deeper, which Y is not even part of here.
        List<CompassTargeting.Candidate> candidates = List.of(
                new CompassTargeting.Candidate(A, 10.0D, 0.0D),
                new CompassTargeting.Candidate(B, 1.0D, 0.0D));
        assertEquals(B, CompassTargeting.nearest(candidates, 0.0D, 0.0D).orElseThrow().id());
    }

    @Test
    void aChosenCursorIsFoundByIdButNotAfterItIsGone() {
        List<CompassTargeting.Candidate> candidates = List.of(
                new CompassTargeting.Candidate(A, 1.0D, 1.0D));

        assertEquals(A, CompassTargeting.byId(candidates, A).orElseThrow().id());
        assertTrue(CompassTargeting.byId(candidates, B).isEmpty(), "a departed cursor has no target");
        assertTrue(CompassTargeting.byId(candidates, null).isEmpty(), "nearest mode stores no id");
    }

    // ------------------------------------------------------------------
    // bearing
    // ------------------------------------------------------------------

    @Test
    void aTargetDeadAheadReadsAsTheNeedlePointingUp() {
        // Facing north (yaw 180) with the cursor due north.
        double rotation = CompassTargeting.rotationTowards(0.0D, -10.0D, 0.0D, 0.0D, 180.0D);
        assertEquals(0.0D, rotation, 1.0E-9D);
        assertEquals(0, CompassTargeting.frameForRotation(rotation));

        // And the same holds facing east at a target due east.
        assertEquals(0, CompassTargeting.frameForRotation(
                CompassTargeting.rotationTowards(10.0D, 0.0D, 0.0D, 0.0D, 270.0D)));
    }

    @Test
    void aTargetToTheRightReadsAsTheNeedlePointingRight() {
        // Facing north, cursor due east: frame 8 is a quarter turn, i.e. the needle points right.
        double rotation = CompassTargeting.rotationTowards(10.0D, 0.0D, 0.0D, 0.0D, 180.0D);
        assertEquals(0.25D, rotation, 1.0E-9D);
        assertEquals(8, CompassTargeting.frameForRotation(rotation));
    }

    @Test
    void aTargetBehindReadsAsTheNeedlePointingDown() {
        // Facing north, cursor due south.
        double rotation = CompassTargeting.rotationTowards(0.0D, 10.0D, 0.0D, 0.0D, 180.0D);
        assertEquals(0.5D, rotation, 1.0E-9D);
        assertEquals(16, CompassTargeting.frameForRotation(rotation));
    }

    @Test
    void turningOnTheSpotSweepsTheNeedleTheOtherWay() {
        // Same world target, holder spinning: the needle has to swing as the facing does.
        // A cursor due north is dead ahead when facing north, and off to the left when facing east.
        int facingNorth = CompassTargeting.frameForRotation(
                CompassTargeting.rotationTowards(0.0D, -10.0D, 0.0D, 0.0D, 180.0D));
        int facingEast = CompassTargeting.frameForRotation(
                CompassTargeting.rotationTowards(0.0D, -10.0D, 0.0D, 0.0D, 270.0D));

        assertEquals(0, facingNorth, "dead ahead reads as the needle pointing up");
        assertEquals(24, facingEast, "north of an east-facing diver is to their left");
    }

    @Test
    void framesWrapInsteadOfRunningOffTheEndOfTheStrip() {
        assertEquals(0, CompassTargeting.frameForRotation(0.0D));
        assertEquals(1, CompassTargeting.frameForRotation(1.0D / 32.0D));
        assertEquals(31, CompassTargeting.frameForRotation(31.0D / 32.0D));
        assertEquals(0, CompassTargeting.frameForRotation(0.999D));
        assertEquals(CompassTargeting.BEARING_FRAMES - 1, CompassTargeting.frameForRotation(-1.0D / 32.0D));
    }

    @Test
    void everyRotationLandsOnARealFrame() {
        for (int step = 0; step < 1000; step++) {
            int frame = CompassTargeting.frameForRotation(step / 1000.0D);
            assertTrue(frame >= 0 && frame < CompassTargeting.BEARING_FRAMES, "frame " + frame);
        }
    }

    @Test
    void positiveModuloKeepsNegativesInRange() {
        assertEquals(0.75D, CompassTargeting.positiveModulo(-0.25D, 1.0D), 1.0E-9D);
        assertEquals(0.25D, CompassTargeting.positiveModulo(1.25D, 1.0D), 1.0E-9D);
    }

    // ------------------------------------------------------------------
    // the generated model agrees with the runtime mapping
    // ------------------------------------------------------------------

    /**
     * The needle only points correctly if the thresholds baked into the item model agree with
     * {@link CompassTargeting#frameForRotation}. Vanilla puts them on half-steps, so this is easy to
     * get subtly wrong; walking every possible rotation closes that gap for good.
     */
    @Test
    void theGeneratedItemModelPicksTheFrameTheRuntimeComputes() throws IOException {
        JsonObject model = JsonParser
                .parseString(Files.readString(COMPASS_MODEL, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonArray overrides = model.getAsJsonArray("overrides");
        assertEquals(CompassTargeting.BEARING_FRAMES + 1, overrides.size(),
                "one override per frame, plus the wrap back to the base model");

        List<double[]> thresholds = new ArrayList<>();
        List<String> models = new ArrayList<>();
        for (var element : overrides) {
            JsonObject override = element.getAsJsonObject();
            thresholds.add(new double[] {
                    override.getAsJsonObject("predicate").get("aquanaut:angle").getAsDouble() });
            models.add(override.get("model").getAsString());
        }

        for (int step = 0; step < 640; step++) {
            double rotation = step / 640.0D;
            int frame = CompassTargeting.frameForRotation(rotation);

            String expected = frame == 0
                    ? "aquanaut:item/submarine_compass"
                    : "aquanaut:item/submarine_compass_%02d".formatted(frame);
            assertEquals(expected, selectModel(thresholds, models, rotation),
                    "rotation " + rotation + " should show frame " + frame);
        }
    }

    /** Vanilla's rule: the last override whose threshold is at or below the value. */
    private static String selectModel(List<double[]> thresholds, List<String> models, double rotation) {
        String selected = models.get(0);
        for (int i = 0; i < thresholds.size(); i++) {
            if (thresholds.get(i)[0] <= rotation) {
                selected = models.get(i);
            }
        }
        return selected;
    }

    @Test
    void everyFrameTheModelCanShowHasASprite() throws IOException {
        for (int frame = 0; frame < CompassTargeting.BEARING_FRAMES; frame++) {
            Path sprite = FRAME_DIR.resolve("frame_%02d.png".formatted(frame));
            assertTrue(Files.isRegularFile(sprite), "missing compass frame " + frame);
        }
        assertFalse(Files.isRegularFile(FRAME_DIR.resolve("frame_32.png")),
                "the strip has exactly 32 bearings; a 33rd frame would be unreachable");
    }
}
