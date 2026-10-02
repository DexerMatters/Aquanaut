package com.dexer.aquanaut;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dexer.aquanaut.common.sonar.SonarCavity;
import com.dexer.aquanaut.common.sonar.SonarPlot;
import com.dexer.aquanaut.common.sonar.SonarPulse;
import com.dexer.aquanaut.common.sonar.SonarReturn;
import com.dexer.aquanaut.common.sonar.SonarSignal;
import com.dexer.aquanaut.common.sonar.SonarSphere;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shape of a ping.
 *
 * <p>
 * The instrument is one event drawn in three places — the water, the scope and the diver's ears — and
 * all three read the same clock. Nothing here pins the drawing, but everything here pins the clock:
 * how far the wavefront has reached, when an echo is due, how long the picture is held and how long
 * the instrument then stays quiet. Those are the numbers that would break silently, and they would
 * break in three places at once.
 *
 * <p>
 * It also pins the two tables that have to agree with the resource files rather than with the code:
 * every voice the enum can produce must have a sound in {@code sounds.json} and a name in both
 * locales, and the tags the scan reads must exist as files. A voice that is added to the enum and
 * nowhere else would otherwise be a silent, unnamed blip.
 */
public final class SonarPulseTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path DATA = Path.of("src/main/resources/data/aquanaut");

    // ------------------------------------------------------------------
    // the clock
    // ------------------------------------------------------------------

    @Test
    public void theWavefrontCrossesTheRangeInTheAdvertisedTime() {
        assertEquals(0.0D, SonarPulse.wavefrontRadius(0.0D), 1.0E-6D);
        assertEquals(SonarPulse.RANGE, SonarPulse.wavefrontRadius(SonarPulse.TRAVEL_TICKS), 1.0E-6D);

        double previous = -1.0D;

        for (double age = 0.0D; age <= SonarPulse.TRAVEL_TICKS; age += 0.25D) {
            double radius = SonarPulse.wavefrontRadius(age);
            assertTrue(radius > previous, "the wavefront has to keep going out, at " + age);
            previous = radius;
        }

        // Constant speed in water, not an ease: a sonar's range axis is linear.
        double step = SonarPulse.wavefrontRadius(10.0D) - SonarPulse.wavefrontRadius(9.0D);
        assertEquals(SonarPulse.SPEED, step, 1.0E-6D);
    }

    @Test
    public void anEchoFallsDueWhenTheWavefrontReachesIt() {
        double near = 3.0D;
        double far = 17.0D;

        assertTrue(SonarPulse.arrival(near) < SonarPulse.arrival(far),
                "a contact further out has to answer later");
        assertTrue(SonarPulse.hasReached(SonarPulse.arrival(far) + 0.01D, far),
                "the wavefront has to have reached a contact once its arrival has passed");
        assertFalse(SonarPulse.hasReached(SonarPulse.arrival(far) - 0.01D, far),
                "an echo must not be struck before the wavefront gets there");
        assertEquals(0.0F, SonarPulse.echoProgress(SonarPulse.arrival(near), near), 1.0E-4F);
        assertEquals(1.0F, SonarPulse.echoProgress(SonarPulse.arrival(near) + SonarPulse.RETURN_TICKS, near),
                1.0E-4F);
    }

    @Test
    public void theReadoutRisesHoldsAndThenClears() {
        assertEquals(0.0F, SonarPulse.readout(-1.0D), 1.0E-6F);
        assertTrue(SonarPulse.readout(0.0D) < SonarPulse.readout(3.0D),
                "the scope fades up rather than snapping on");

        double held = SonarPulse.TRAVEL_TICKS + SonarPulse.RETURN_TICKS + SonarPulse.HOLD_TICKS;
        assertEquals(1.0F, SonarPulse.readout(held - 1.0D), 1.0E-4F);
        assertTrue(SonarPulse.readout(held + SonarPulse.FADE_TICKS / 2.0D) < 0.6F,
                "the picture has to be on its way out once the hold is up");
        assertEquals(0.0F, SonarPulse.readout(SonarPulse.DISPLAY_TICKS + 1.0D), 1.0E-6F);
        assertFalse(SonarPulse.isAlive(SonarPulse.DISPLAY_TICKS + 1.0D));
    }

    @Test
    public void theInstrumentIsQuietBeforeItCanSpeakAgain() {
        assertTrue(SonarPulse.DISPLAY_TICKS < SonarPulse.COOLDOWN_TICKS,
                "the picture has to finish before the next pulse is allowed, or two readings overlap");
        assertEquals(100, SonarPulse.COOLDOWN_TICKS, "five seconds is the advertised recharge");
        assertEquals(20.0D, SonarPulse.RANGE, 1.0E-9D, "twenty blocks is the advertised range");
    }

    @Test
    public void aPulseIsDarkBeforeItIsFiredAndAfterItHasSpentItself() {
        assertEquals(0.0F, SonarPulse.waveGlow(-1.0D), 1.0E-6F);
        assertEquals(0.0F, SonarPulse.waveGlow(SonarPulse.DISPLAY_TICKS + 1.0D), 1.0E-6F);

        for (double age = 0.0D; age <= SonarPulse.TRAVEL_TICKS; age += 0.5D) {
            float glow = SonarPulse.waveGlow(age);
            assertTrue(glow > 0.0F && glow <= 1.0F, "the shell has to stay visible while it travels: " + age);
        }
    }

    @Test
    public void aDistantContactAnswersMoreQuietlyThanANearOne() {
        float near = SonarPulse.echoGlow(SonarPulse.arrival(2.0D) + 1.0D, 2.0D, 1.0F);
        float far = SonarPulse.echoGlow(SonarPulse.arrival(18.0D) + 1.0D, 18.0D, 1.0F);

        assertTrue(near > far, "a reflection loses everything it has to the distance it came from");
        assertTrue(far > 0.0F, "a contact at the edge of the range is still a contact");
    }

    // ------------------------------------------------------------------
    // the rays
    // ------------------------------------------------------------------

    @Test
    public void theRaysAreUnitLengthAndSpreadOverTheWholeSphere() {
        List<Vec3> directions = SonarSphere.directions(640);
        assertEquals(640, directions.size());

        for (Vec3 direction : directions) {
            assertEquals(1.0D, direction.length(), 1.0E-9D, "a listening direction has to be a direction");
        }

        // Every direction is listened in once and none twice: the closest pair of the set is still a
        // good fraction of the spacing an ideal packing would manage.
        double closest = Double.MAX_VALUE;

        for (int one = 0; one < directions.size(); one++) {
            for (int other = one + 1; other < directions.size(); other++) {
                closest = Math.min(closest, directions.get(one).subtract(directions.get(other)).length());
            }
        }

        assertTrue(closest > 0.05D, "two rays are nearly on top of each other: " + closest);
    }

    @Test
    public void theRaysAreTheSameEveryTime() {
        List<Vec3> once = SonarSphere.directions(64);
        List<Vec3> twice = SonarSphere.directions(64);

        for (int index = 0; index < once.size(); index++) {
            assertEquals(once.get(index).lengthSqr(), twice.get(index).lengthSqr(), 1.0E-12D,
                    "a ping fired twice from the same spot has to read the same water the same way");
        }

        assertEquals(0, SonarSphere.directions(0).size());
    }

    // ------------------------------------------------------------------
    // the contacts
    // ------------------------------------------------------------------

    @Test
    public void raysThatFoundTheSameThingBecomeOneContact() {
        List<SonarReturn> rays = new ArrayList<>();
        rays.add(SonarReturn.at(SonarSignal.MINERAL, new Vec3(0.0D, 0.0D, 10.0D), 0.9F));
        rays.add(SonarReturn.at(SonarSignal.MINERAL, new Vec3(1.0D, 0.0D, 10.5D), 0.4F));
        rays.add(SonarReturn.at(SonarSignal.CAVITY, new Vec3(1.0D, 0.0D, 10.5D), 0.4F));

        List<SonarReturn> contacts = SonarSphere.cluster(rays, 24);

        assertEquals(2, contacts.size(), "one vein and one hollow, however many rays found each");
        assertEquals(SonarSignal.MINERAL, contacts.get(0).signal(), "strongest first");
        assertEquals(2, contacts.get(0).count(), "both rays of the vein are behind the one contact");
        assertEquals(0.9F, contacts.get(0).strength(), 1.0E-6F,
                "the report is the ray the instrument heard best");
    }

    @Test
    public void twoBodiesOfTheSameVoiceStayTwoContacts() {
        List<SonarReturn> rays = new ArrayList<>();
        rays.add(SonarReturn.at(SonarSignal.BIOLOGICAL, new Vec3(0.0D, 0.0D, 8.0D), 0.8F));
        rays.add(SonarReturn.at(SonarSignal.BIOLOGICAL, new Vec3(3.0D, 0.0D, 8.0D), 0.8F));

        assertEquals(2, SonarSphere.cluster(rays, 24).size(),
                "a shoal is six fish, not one; bodies merge far more tightly than caves do");
    }

    @Test
    public void theContactListIsCappedAtItsStrongest() {
        List<SonarReturn> rays = new ArrayList<>();

        for (int index = 0; index < 40; index++) {
            rays.add(SonarReturn.at(SonarSignal.MINERAL, new Vec3(index * 6.0D, 0.0D, 0.0D), index / 40.0F));
        }

        List<SonarReturn> contacts = SonarSphere.cluster(rays, 24);
        assertEquals(24, contacts.size());

        for (int index = 1; index < contacts.size(); index++) {
            assertTrue(contacts.get(index - 1).strength() >= contacts.get(index).strength(),
                    "the list has to be strongest first, or the cap drops the wrong contacts");
        }
    }

    // ------------------------------------------------------------------
    // the hollows
    // ------------------------------------------------------------------

    /**
     * The one rule that decides whether the instrument finds caves or finds the water it is already
     * floating in.
     *
     * <p>
     * A pulse that arrives at a wall goes <em>into</em> it. Probing back out along the ray that
     * arrived instead looks like the same thing and is not: a ray meeting a wall at a glancing angle
     * steps sideways out of the rock within half a block and lands in the water it came from, so
     * every shallow hit on a distant wall reads as a hollow. That was the bug, and this is the pin.
     */
    @Test
    public void theCavityProbeGoesIntoTheFaceAndNotBackOutOfIt() {
        BlockPos face = new BlockPos(4, 60, -7);

        for (Direction struck : Direction.values()) {
            List<BlockPos> probes = SonarCavity.probes(face, struck);
            assertEquals(SonarCavity.DEPTH, probes.size());

            assertEquals(face.relative(struck.getOpposite(), 1), probes.get(0),
                    "the probe has to go in through " + struck + ", not back out of it");
            assertNotEquals(face.relative(struck, 1), probes.get(0),
                    "probing back along the ray finds the water the pulse came from");
            assertEquals(face.relative(struck.getOpposite(), SonarCavity.DEPTH),
                    probes.get(probes.size() - 1), "the probe has to reach its full depth");
        }
    }

    @Test
    public void aPulseArrivingFromAboveLooksDownwards() {
        // The common case by far: a ping over a sea floor, looking for the caves threaded under it.
        BlockPos floor = new BlockPos(0, 62, 0);
        List<BlockPos> probes = SonarCavity.probes(floor, Direction.UP);

        for (int step = 0; step < probes.size(); step++) {
            assertEquals(62 - (step + 1), probes.get(step).getY(),
                    "a pulse that came down through the top of a block is looking underneath it");
            assertEquals(0, probes.get(step).getX());
            assertEquals(0, probes.get(step).getZ());
        }
    }

    // ------------------------------------------------------------------
    // the dial
    // ------------------------------------------------------------------

    @Test
    public void theDialFacesTheDiver() {
        // Vanilla yaw zero faces +Z; a contact dead ahead of it has to plot up the screen.
        Vec3 ahead = new Vec3(0.0D, 0.0D, 10.0D);
        assertEquals(0.0F, SonarPlot.bearing(ahead, 0.0F), 1.0E-4F);
        // Half the range plots at half the dial's radius, and dead ahead is straight up it.
        assertEquals(100.0F - 15.0F, SonarPlot.plotY(ahead, 0.0F, 100.0F, 30.0F), 1.0E-3F);
        assertEquals(100.0F, SonarPlot.plotX(ahead, 0.0F, 100.0F, 30.0F), 1.0E-3F);

        // Facing north instead, the same contact is behind the diver and plots down the screen.
        assertEquals(180.0F, Math.abs(SonarPlot.bearing(ahead, 180.0F)), 1.0E-3F);
        assertTrue(SonarPlot.plotY(ahead, 180.0F, 100.0F, 30.0F) > 100.0F);

        // And a contact on the diver's right plots to the right, whichever way they face.
        Vec3 east = new Vec3(10.0D, 0.0D, 0.0D);
        assertTrue(SonarPlot.bearing(east, 0.0F) < 0.0F, "facing south, east is over the left shoulder");
        assertEquals(0.0F, SonarPlot.bearing(east, 270.0F), 0.5F, "facing east, east is dead ahead");
    }

    @Test
    public void theRadiusIsGroundDistanceAndTheStemCarriesHeight() {
        Vec3 below = new Vec3(0.0D, -12.0D, 6.0D);
        assertEquals(0.3F, SonarPlot.rangeFraction(below), 1.0E-4F, "height must not inflate the radius");
        assertTrue(SonarPlot.stem(below, 30.0F) < 0.0F, "something below the diver stems downwards");

        Vec3 above = new Vec3(0.0D, 12.0D, 6.0D);
        assertEquals(SonarPlot.stem(below, 30.0F), -SonarPlot.stem(above, 30.0F), 1.0E-4F);

        // A contact under the transducer plots at the middle, and the stem is what says so.
        assertEquals(0.0F, SonarPlot.rangeFraction(new Vec3(0.0D, -9.0D, 0.0D)), 1.0E-6F);
    }

    @Test
    public void theHeadingFollowsVanillaYaw() {
        assertEquals(180.0F, SonarPlot.heading(0.0F), 1.0E-3F, "yaw zero faces south");
        assertEquals(0.0F, SonarPlot.heading(180.0F), 1.0E-3F, "yaw 180 faces north");
        assertEquals(270.0F, SonarPlot.heading(90.0F), 1.0E-3F, "yaw 90 faces west");
        assertEquals("N", SonarPlot.compassPoint(0.0F));
        assertEquals("E", SonarPlot.compassPoint(90.0F));
        assertEquals("SW", SonarPlot.compassPoint(225.0F));
        assertEquals("N", SonarPlot.compassPoint(359.0F));
    }

    // ------------------------------------------------------------------
    // the voices
    // ------------------------------------------------------------------

    @Test
    public void aVoiceSurvivesTheWire() {
        for (SonarSignal signal : SonarSignal.values()) {
            assertEquals(signal, SonarSignal.byOrdinal(signal.ordinal()));
        }

        assertEquals(SonarSignal.BIOLOGICAL, SonarSignal.byOrdinal(-1), "an unknown ordinal must not crash");
        assertEquals(SonarSignal.BIOLOGICAL, SonarSignal.byOrdinal(99), "an unknown ordinal must not crash");
    }

    @Test
    public void aContactIsAlwaysUsable() {
        SonarReturn contact = new SonarReturn(null, null, 4.0F, 0);
        assertNotNull(contact.signal());
        assertNotNull(contact.offset());
        assertEquals(1.0F, contact.strength(), 1.0E-6F, "strength is a fraction");
        assertEquals(1, contact.count(), "a contact is at least one ray");
        assertEquals(0.0D, contact.distance(), 1.0E-9D);
    }

    // ------------------------------------------------------------------
    // the files that have to agree
    // ------------------------------------------------------------------

    @Test
    public void everyVoiceIsSoundedInBothLocales() throws IOException {
        JsonObject sounds = readJson(ASSETS.resolve("sounds.json"));

        for (SonarSignal signal : SonarSignal.values()) {
            assertTrue(sounds.has(signal.soundName()),
                    "no sound event called " + signal.soundName() + "; the voice would be silent");
            assertTrue(sounds.getAsJsonObject(signal.soundName()).has("subtitle"),
                    signal.soundName() + " has no subtitle, so it cannot be turned off by name");

            for (String locale : new String[] { "en_us.json", "zh_cn.json" }) {
                JsonObject lang = readJson(ASSETS.resolve("lang").resolve(locale));

                assertTrue(lang.has("subtitles.aquanaut." + signal.soundName()),
                        "missing subtitle for " + signal.soundName() + " in " + locale);
                assertTrue(lang.has("hud.aquanaut.sonar.title"), "missing the scope's title in " + locale);
                assertTrue(lang.has("hud.aquanaut.sonar.range"), "missing the scope's range label in " + locale);
            }
        }

        assertTrue(sounds.has("sonar_ping"), "the pulse itself has to have a sound");
    }

    /**
     * The instrument must never say what it found. A colour and a sound are a voice the diver learns
     * to read; a name is an answer the instrument has no way of knowing the truth of, and a figure
     * for the distance is the same mistake in numbers. This pins the absence, because a screen that
     * quietly grew a label back would be the easiest thing in the mod to miss in review.
     */
    @Test
    public void noVoiceIsEverNamedToThePlayer() throws IOException {
        // Not merely unused: there is no way to ask a voice for a name at all.
        assertThrows(NoSuchMethodException.class, () -> SonarSignal.class.getMethod("translationKey"),
                "the voice table has grown a way to name itself");

        for (String locale : new String[] { "en_us.json", "zh_cn.json" }) {
            JsonObject lang = readJson(ASSETS.resolve("lang").resolve(locale));

            for (String key : lang.keySet()) {
                assertFalse(key.startsWith("hud.aquanaut.sonar.signal."),
                        "the scope has been given a name for a voice in " + locale + ": " + key);
            }

            assertFalse(lang.has("hud.aquanaut.sonar.metres"),
                    "the scope has been given a numeric readout in " + locale);
            assertFalse(lang.has("hud.aquanaut.sonar.empty"),
                    "the scope has been given an empty-state caption in " + locale);
            assertFalse(lang.has("message.aquanaut.portable_sonar.depth"),
                    "the sonar has been given a depth readout in " + locale);
        }
    }

    @Test
    public void theScansTagsExistAsFiles() throws IOException {
        Path minerals = DATA.resolve("tags/block/sonar_minerals.json");
        Path abyssal = DATA.resolve("tags/entity_type/sonar_abyssal.json");

        assertTrue(Files.isRegularFile(minerals), "missing " + minerals);
        assertTrue(Files.isRegularFile(abyssal), "missing " + abyssal);
        assertFalse(readJson(minerals).getAsJsonArray("values").isEmpty(),
                "an empty mineral tag would make the sonar blind to every ore in the pack");
        assertFalse(readJson(abyssal).getAsJsonArray("values").isEmpty(),
                "an empty abyssal tag would leave the long echo with nothing to say");
    }

    @Test
    public void theScopeFaceIsBakedForTheDialItIsBlittedAt() throws IOException {
        Path scope = ASSETS.resolve("textures/gui/sprites/hud/sonar_scope.png");
        Path white = ASSETS.resolve("textures/entity/sonar_white.png");

        assertTrue(Files.isRegularFile(scope), "missing " + scope);
        assertTrue(Files.isRegularFile(white), "missing " + white);

        java.awt.image.BufferedImage dial = javax.imageio.ImageIO.read(scope.toFile());
        assertNotNull(dial, "unreadable " + scope);
        // Square, and the size SonarScope blits it at: a dial that is scaled stops being pixel art.
        assertEquals(dial.getWidth(), dial.getHeight(), "the dial has to be square");
        assertEquals(72, dial.getWidth(), "SonarScope blits the dial at seventy-two pixels");

        // The corners are outside the bezel and must stay clear, or the panel shows through square.
        assertEquals(0, dial.getRGB(0, 0) >>> 24, "the corner of the dial has to be transparent");
        assertTrue((dial.getRGB(dial.getWidth() / 2, dial.getHeight() / 2) >>> 24) == 255,
                "the middle of the dial is the transducer and has to be drawn");
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing asset: " + path);
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
