package com.dexer.aquanaut;

import com.dexer.aquanaut.common.entity.DetectorGeometry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the biological detector's shipped assets against the numbers the code deploys it with.
 *
 * <p>
 * The detector exists twice: as an exported Blockbench model and as an entity that plays its clips,
 * matches its hitbox to it and drops its own item. Nothing in the game runtime joins the two, so a
 * re-export that moved the ball, renamed a clip or lost the bio-green collar would fail in play
 * rather than at build time. This test reads the shipped files and asserts the contract
 * {@link DetectorGeometry} describes:
 *
 * <ul>
 * <li>the ball is half a block across and stands on its own origin;</li>
 * <li>the animation file contains the clips the entity queues, in the right order and the right
 * loop modes, and animates only bones the geometry has;</li>
 * <li>the release ends at the working loop's hover height, so the handover has nowhere to jump;</li>
 * <li>both textures ship at the geometry's resolution, and the glowmask really lights the collar;</li>
 * <li>the item points at its own sprite, and that sprite carries the same bio-green tell.</li>
 * </ul>
 */
final class BiologicalDetectorAssetTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path GEO = ASSETS.resolve("geo/biological_detector.geo.json");
    private static final Path ANIMATION = ASSETS.resolve("animations/biological_detector.animation.json");
    private static final Path TEXTURE = ASSETS.resolve("textures/entity/biological_detector.png");
    private static final Path GLOWMASK = ASSETS.resolve("textures/entity/biological_detector_glowmask.png");
    private static final Path ITEM_MODEL = ASSETS.resolve("models/item/biological_detector.json");
    private static final Path ITEM_TEXTURE = ASSETS.resolve("textures/item/biological_detector.png");
    private static final Path HOLOGRAM_SAMPLER = ASSETS.resolve("textures/entity/detector_hologram_white.png");

    // ------------------------------------------------------------------
    // geometry
    // ------------------------------------------------------------------

    @Test
    void theShippedBallIsHalfABlockAcrossAndStandsOnItsOrigin() throws IOException {
        JsonArray bones = geometry().getAsJsonArray("bones");
        List<JsonObject> cubes = new ArrayList<>();
        collectCubes(bones, cubes);
        assertFalse(cubes.isEmpty(), "the geometry has no cubes");

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (JsonObject cube : cubes) {
            minX = Math.min(minX, axis(cube, "origin", 0));
            maxX = Math.max(maxX, axis(cube, "origin", 0) + axis(cube, "size", 0));
            minY = Math.min(minY, axis(cube, "origin", 1));
            maxY = Math.max(maxY, axis(cube, "origin", 1) + axis(cube, "size", 1));
            minZ = Math.min(minZ, axis(cube, "origin", 2));
            maxZ = Math.max(maxZ, axis(cube, "origin", 2) + axis(cube, "size", 2));
        }

        double ball = DetectorGeometry.BALL_UNITS;
        assertEquals(ball, maxX - minX, 1.0E-6D, "the ball is not half a block across in X");
        assertEquals(ball, maxZ - minZ, 1.0E-6D, "the ball is not half a block across in Z");
        assertEquals(ball, maxY - minY, 1.0E-6D, "the folded ball is not half a block tall");
        assertEquals(0.0D, minY, 1.0E-9D, "a model whose base is not at y=0 would float or sink");
        assertEquals(DetectorGeometry.FOLDED_HEIGHT, ball / DetectorGeometry.UNITS_PER_BLOCK, 1.0E-6F);
    }

    @Test
    void theGeometryCarriesTheBonesTheCodeAndTheClipsUse() throws IOException {
        List<String> names = new ArrayList<>();
        for (JsonElement element : geometry().getAsJsonArray("bones")) {
            names.add(element.getAsJsonObject().get("name").getAsString());
        }
        assertTrue(names.contains("group_body"), "the hull bone is missing: " + names);
        assertTrue(names.contains("group_core"), "the core bone is missing: " + names);
        assertTrue(names.contains("group_shell_upper"), "the opening shell bone is missing: " + names);
    }

    @Test
    void theTexturesShipAtTheGeometryResolution() throws IOException {
        JsonObject description = geometry().getAsJsonObject("description");
        int width = description.get("texture_width").getAsInt();
        int height = description.get("texture_height").getAsInt();

        BufferedImage diffuse = read(TEXTURE);
        BufferedImage glowmask = read(GLOWMASK);
        assertEquals(width, diffuse.getWidth(), "the diffuse atlas is not the geometry resolution");
        assertEquals(height, diffuse.getHeight(), "the diffuse atlas is not the geometry resolution");
        assertEquals(width, glowmask.getWidth(), "the glowmask must match the diffuse atlas");
        assertEquals(height, glowmask.getHeight(), "the glowmask must match the diffuse atlas");
    }

    /**
     * The whole point of the collar is that it reads as bio-green, in the atlas and in the emissive
     * mask: a re-export that flattened it to steel would quietly lose the "biological" tell.
     */
    @Test
    void theCollarIsBioGreenInBothTextures() throws IOException {
        assertTrue(greenPixels(read(TEXTURE)) > 0, "the diffuse atlas has no bio-green collar");
        assertTrue(greenPixels(read(GLOWMASK)) > 0, "the glowmask does not light the collar");
    }

    /**
     * The hologram is drawn with vanilla's block-breaking program, which is a textured one, so every
     * fragment of the projection is multiplied by this single texel. It has to be white and opaque:
     * anything else would tint the hologram through a channel nothing in the code can compensate
     * for, and a transparent texel would discard the whole projection.
     */
    @Test
    void theHologramSamplesAFlatWhiteTexel() throws IOException {
        BufferedImage sampler = read(HOLOGRAM_SAMPLER);
        assertEquals(1, sampler.getWidth(), "the hologram sampler should be a single texel");
        assertEquals(1, sampler.getHeight(), "the hologram sampler should be a single texel");

        int argb = sampler.getRGB(0, 0);
        assertEquals(255, (argb >>> 24) & 0xFF, "a transparent sampler would discard the hologram");
        assertEquals(0xFFFFFF, argb & 0xFFFFFF, "anything but white would tint the hologram");
    }

    // ------------------------------------------------------------------
    // animation
    // ------------------------------------------------------------------

    @Test
    void theShippedClipsAreTheOnesTheEntityQueues() throws IOException {
        JsonObject animations = animation().getAsJsonObject("animations");
        assertNotNull(animations.get(DetectorGeometry.RELEASE_ANIMATION),
                "the entity plays '" + DetectorGeometry.RELEASE_ANIMATION + "', which is not shipped");
        assertNotNull(animations.get(DetectorGeometry.WORKING_ANIMATION),
                "the entity loops '" + DetectorGeometry.WORKING_ANIMATION + "', which is not shipped");

        JsonObject release = animations.getAsJsonObject(DetectorGeometry.RELEASE_ANIMATION);
        JsonObject working = animations.getAsJsonObject(DetectorGeometry.WORKING_ANIMATION);

        // The release is played once and then handed over: it must not loop.
        assertFalse(release.has("loop") && release.get("loop").getAsBoolean(),
                "the release clip loops; the buoy would never stop deploying");
        assertTrue(working.has("loop") && working.get("loop").getAsBoolean(),
                "the working clip does not loop; the buoy would stop scanning");
        assertEquals(DetectorGeometry.RELEASE_SECONDS,
                release.get("animation_length").getAsFloat(), 1.0E-6F);
    }

    @Test
    void everyAnimatedBoneExistsInTheGeometry() throws IOException {
        List<String> bones = new ArrayList<>();
        for (JsonElement element : geometry().getAsJsonArray("bones")) {
            bones.add(element.getAsJsonObject().get("name").getAsString());
        }
        for (String clip : new String[] { DetectorGeometry.RELEASE_ANIMATION,
                DetectorGeometry.WORKING_ANIMATION }) {
            JsonObject tracks = animation().getAsJsonObject("animations").getAsJsonObject(clip)
                    .getAsJsonObject("bones");
            for (String bone : tracks.keySet()) {
                assertTrue(bones.contains(bone), clip + " animates '" + bone + "', which is not in the geometry");
            }
        }
    }

    /**
     * The deployment opens the shell and the working loop starts from there, so the two clips have to
     * agree at the seam: a lift that did not line up would show as a visible pop at the handover.
     */
    @Test
    void theReleaseEndsWhereTheWorkingLoopHovers() throws IOException {
        JsonObject animations = animation().getAsJsonObject("animations");
        JsonObject releaseTrack = animations.getAsJsonObject(DetectorGeometry.RELEASE_ANIMATION)
                .getAsJsonObject("bones").getAsJsonObject("group_shell_upper")
                .getAsJsonObject("position");
        JsonObject workingTrack = animations.getAsJsonObject(DetectorGeometry.WORKING_ANIMATION)
                .getAsJsonObject("bones").getAsJsonObject("group_shell_upper")
                .getAsJsonObject("position");

        JsonObject first = firstKeyframe(releaseTrack);
        assertEquals(0.0D, lift(first), 1.0E-6D, "a released buoy starts folded shut");

        // The channels carry model units, the same space the geometry is authored in.
        double end = lift(lastKeyframe(releaseTrack));
        assertEquals(DetectorGeometry.OPEN_LIFT_UNITS, end, 1.0E-3D,
                "the release does not stop at the recorded lift; update DetectorGeometry");

        for (var entry : workingTrack.entrySet()) {
            assertTrue(Math.abs(lift(entry.getValue().getAsJsonObject())
                    - DetectorGeometry.OPEN_LIFT_UNITS) < 0.25D,
                    "the working loop hovers " + lift(entry.getValue().getAsJsonObject())
                            + " above the folded pose at t=" + entry.getKey());
        }
    }

    // ------------------------------------------------------------------
    // item
    // ------------------------------------------------------------------

    @Test
    void theItemPointsAtItsOwnSprite() throws IOException {
        JsonObject model = JsonParser.parseString(Files.readString(ITEM_MODEL, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("aquanaut:item/biological_detector",
                model.getAsJsonObject("textures").get("layer0").getAsString());

        BufferedImage icon = read(ITEM_TEXTURE);
        assertEquals(16, icon.getWidth(), "item sprites are 16x16");
        assertEquals(16, icon.getHeight(), "item sprites are 16x16");
        assertTrue(greenPixels(icon) > 0, "the item sprite has no bio-green collar");
    }

    @Test
    void theItemAndTheEntityAreNamedInBothLanguages() throws IOException {
        for (String language : new String[] { "en_us.json", "zh_cn.json" }) {
            JsonObject lang = JsonParser.parseString(Files.readString(
                    ASSETS.resolve("lang").resolve(language), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : new String[] { "item.aquanaut.biological_detector",
                    "entity.aquanaut.biological_detector" }) {
                assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(),
                        language + " does not name " + key);
            }
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static JsonObject geometry() throws IOException {
        return JsonParser.parseString(Files.readString(GEO, StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonArray("minecraft:geometry")
                .get(0)
                .getAsJsonObject();
    }

    private static JsonObject animation() throws IOException {
        return JsonParser.parseString(Files.readString(ANIMATION, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static BufferedImage read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing asset " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, "unreadable image " + path);
        return image;
    }

    /**
     * How many pixels lean green: the detector's collar uses the model's own bio-green, so a
     * green-dominant pixel can only have come from it. Opaque pixels only, since the gutter around
     * the atlas islands is transparent black and the item sprite is transparent everywhere else.
     */
    private static int greenPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (alpha > 0 && green > red + 12 && green >= blue) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The shell's vertical offset out of a position keyframe, whatever shape the codec wrote. */
    private static double lift(JsonObject keyframe) {
        JsonElement post = keyframe.get("post");
        JsonElement vector = post != null && post.isJsonObject() ? post.getAsJsonObject().get("vector") : post;
        if (vector == null) {
            vector = keyframe.get("vector");
        }
        assertNotNull(vector, "a position keyframe carries no vector: " + keyframe);
        JsonArray values = vector.isJsonArray() ? vector.getAsJsonArray() : vector.getAsJsonObject()
                .getAsJsonArray("vector");
        return values.get(1).getAsDouble();
    }

    private static JsonObject firstKeyframe(JsonObject track) {
        JsonObject best = null;
        double bestTime = Double.POSITIVE_INFINITY;
        for (var entry : track.entrySet()) {
            double time = Double.parseDouble(entry.getKey());
            if (time < bestTime) {
                bestTime = time;
                best = entry.getValue().getAsJsonObject();
            }
        }
        assertNotNull(best, "the track has no keyframes");
        return best;
    }

    private static JsonObject lastKeyframe(JsonObject track) {
        JsonObject best = null;
        double bestTime = Double.NEGATIVE_INFINITY;
        for (var entry : track.entrySet()) {
            double time = Double.parseDouble(entry.getKey());
            if (time > bestTime) {
                bestTime = time;
                best = entry.getValue().getAsJsonObject();
            }
        }
        assertNotNull(best, "the track has no keyframes");
        return best;
    }

    private static double axis(JsonObject cube, String member, int index) {
        return cube.getAsJsonArray(member).get(index).getAsDouble();
    }

    private static void collectCubes(JsonArray bones, List<JsonObject> out) {
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            JsonArray cubes = bone.getAsJsonArray("cubes");
            if (cubes != null) {
                for (JsonElement cube : cubes) {
                    out.add(cube.getAsJsonObject());
                }
            }
            JsonArray children = bone.getAsJsonArray("bones");
            if (children != null) {
                collectCubes(children, out);
            }
        }
    }
}
