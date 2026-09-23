package com.dexer.aquanaut;

import com.dexer.aquanaut.common.entity.CursorGeometry;
import com.dexer.aquanaut.common.item.SpawnAnchor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cursor's model-derived numbers and its textures.
 *
 * <p>
 * The entity's dimensions are computed from {@link CursorGeometry#MODEL_BOUNDS} rather than typed in
 * by hand, and this test re-derives that box from the shipped {@code cursor.geo.json}. Re-export the
 * model with a different silhouette and the test fails instead of the entity quietly keeping a
 * hitbox that no longer wraps it. The glowmask is pinned to the same model the same way.
 */
final class CursorHitboxTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path GEO = ASSETS.resolve("geo/cursor.geo.json");
    private static final Path ANIMATION = ASSETS.resolve("animations/cursor.animation.json");
    private static final Path TEXTURE = ASSETS.resolve("textures/entity/cursor.png");
    private static final Path GLOWMASK = ASSETS.resolve("textures/entity/cursor_glowmask.png");

    @Test
    void declaredModelBoundsAreExactlyTheModelsBounds() throws IOException {
        List<CursorGeometry.Bounds> cubes = new ArrayList<>();
        collectCubes(geometry().getAsJsonArray("bones"), cubes);
        assertFalse(cubes.isEmpty(), "cursor.geo.json has no cubes");

        CursorGeometry.Bounds union = CursorGeometry.union(cubes);
        assertEquals(CursorGeometry.MODEL_BOUNDS, union,
                "the cursor hitbox no longer matches cursor.geo.json; update CursorGeometry.MODEL_BOUNDS");
    }

    @Test
    void hitboxIsTheModelSizedInBlocks() {
        // 7 units wide by 29 tall, at 16 units to the block.
        assertEquals(7.0D, CursorGeometry.MODEL_BOUNDS.sizeX(), 1.0E-9D);
        assertEquals(29.0D, CursorGeometry.MODEL_BOUNDS.sizeY(), 1.0E-9D);
        assertEquals(7.0D, CursorGeometry.MODEL_BOUNDS.sizeZ(), 1.0E-9D);

        assertEquals(0.4375F, CursorGeometry.HITBOX_WIDTH, 1.0E-6F);
        assertEquals(1.8125F, CursorGeometry.HITBOX_HEIGHT, 1.0E-6F);
    }

    @Test
    void theModelStandsOnItsOriginSoTheHitboxAndThePlacementAgree() {
        // EntityType.sized() measures the box upward from the entity position, so a model whose
        // feet are not at y=0 would float or sink by the difference.
        assertEquals(0.0D, CursorGeometry.MODEL_BOUNDS.minY(), 1.0E-9D);
    }

    @Test
    void theEntityShipsEveryAssetItNames() throws IOException {
        assertTrue(Files.isRegularFile(GEO), "missing geometry");
        assertTrue(Files.isRegularFile(TEXTURE), "missing entity texture");
        assertTrue(Files.isRegularFile(ANIMATION), "missing animation");

        JsonObject animations = JsonParser
                .parseString(Files.readString(ANIMATION, StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonObject("animations");
        assertTrue(animations.has("float"),
                "CursorEntity plays the 'float' clip; the animation file no longer defines it");
    }

    /**
     * An aimed placement has to put the crosshair through the middle of the model. The offset is
     * derived from the same hitbox the entity is built with, so the two cannot disagree.
     */
    @Test
    void anAimedPlacementPutsTheCrosshairThroughTheMiddleOfTheModel() {
        double middleOfModel = CursorGeometry.MODEL_BOUNDS.sizeY()
                / CursorGeometry.UNITS_PER_BLOCK / 2.0D;
        double offset = SpawnAnchor.CENTER.originOffset(CursorGeometry.HITBOX_HEIGHT);

        assertEquals(middleOfModel, offset, 1.0E-9D,
                "the centred anchor must drop the origin by exactly half the model's height");

        // Block placement keeps standing the pin on the surface that was clicked.
        assertEquals(0.0D, SpawnAnchor.BOTTOM.originOffset(CursorGeometry.HITBOX_HEIGHT), 1.0E-9D);
    }

    @Test
    void theAnchorRulesAreWhatTheNamesSay() {
        for (double height : new double[] { 0.0D, 0.5D, 1.8125D, 7.0D }) {
            assertEquals(0.0D, SpawnAnchor.BOTTOM.originOffset(height), 1.0E-9D);
            assertEquals(height / 2.0D, SpawnAnchor.CENTER.originOffset(height), 1.0E-9D);
        }
    }

    @Test
    void thePlacementItemHasASprite() {
        assertTrue(Files.isRegularFile(ASSETS.resolve("textures/item/cursor.png")),
                "missing cursor item sprite");
        assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/cursor.json")),
                "missing cursor item model");
    }

    /**
     * The glowmask must light the beacon and nothing else. If the model is re-exported and the
     * beacon moves to a different part of the sheet, this fails rather than the glow silently
     * sliding onto the hull.
     */
    @Test
    void theGlowmaskLightsExactlyTheBeaconOnTopOfTheAntenna() throws IOException {
        assertTrue(Files.isRegularFile(GLOWMASK), "missing cursor glowmask");

        BufferedImage glow = ImageIO.read(GLOWMASK.toFile());
        BufferedImage base = ImageIO.read(TEXTURE.toFile());
        assertEquals(base.getWidth(), glow.getWidth(), "glowmask width must match the entity sheet");
        assertEquals(base.getHeight(), glow.getHeight(), "glowmask height must match the entity sheet");

        int[] box = topmostCubeBoxUv();
        int u = box[0];
        int v = box[1];
        int width = box[2];
        int height = box[3];

        int lit = 0;
        for (int y = 0; y < glow.getHeight(); y++) {
            for (int x = 0; x < glow.getWidth(); x++) {
                boolean glowing = (glow.getRGB(x, y) >>> 24) != 0;
                boolean insideBeacon = x >= u && x < u + width && y >= v && y < v + height;
                if (glowing) {
                    lit++;
                    assertTrue(insideBeacon,
                            "glowmask lights (" + x + ", " + y + "), outside the beacon block at ("
                                    + u + ", " + v + ") sized " + width + "x" + height);
                }
            }
        }
        assertEquals(width * height, lit,
                "every pixel of the beacon block should glow; the mask is incomplete");
    }

    private static JsonObject geometry() throws IOException {
        return JsonParser.parseString(Files.readString(GEO, StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonArray("minecraft:geometry")
                .get(0)
                .getAsJsonObject();
    }

    /** Box-UV rectangle of the highest cube, as {@code [u, v, width, height]}. */
    private static int[] topmostCubeBoxUv() throws IOException {
        JsonObject best = null;
        double bestTop = Double.NEGATIVE_INFINITY;
        List<JsonObject> cubes = new ArrayList<>();
        collectCubeObjects(geometry().getAsJsonArray("bones"), cubes);
        for (JsonObject cube : cubes) {
            double top = cube.getAsJsonArray("origin").get(1).getAsDouble()
                    + cube.getAsJsonArray("size").get(1).getAsDouble();
            if (top > bestTop) {
                bestTop = top;
                best = cube;
            }
        }
        if (best == null) {
            throw new IllegalStateException("cursor.geo.json has no cubes");
        }
        JsonArray uv = best.getAsJsonArray("uv");
        int width = best.getAsJsonArray("size").get(0).getAsInt();
        int height = best.getAsJsonArray("size").get(1).getAsInt();
        int depth = best.getAsJsonArray("size").get(2).getAsInt();
        // Bedrock box UV: (d + w + d + w) wide and (d + h) tall, from (u, v).
        return new int[] { uv.get(0).getAsInt(), uv.get(1).getAsInt(), 2 * (width + depth), depth + height };
    }

    private static void collectCubes(JsonArray bones, List<CursorGeometry.Bounds> out) {
        List<JsonObject> objects = new ArrayList<>();
        collectCubeObjects(bones, objects);
        for (JsonObject cube : objects) {
            out.add(CursorGeometry.Bounds.ofCube(
                    get(cube, "origin", 0), get(cube, "origin", 1), get(cube, "origin", 2),
                    get(cube, "size", 0), get(cube, "size", 1), get(cube, "size", 2)));
        }
    }

    private static void collectCubeObjects(JsonArray bones, List<JsonObject> out) {
        for (var element : bones) {
            JsonObject bone = element.getAsJsonObject();
            JsonArray cubes = bone.getAsJsonArray("cubes");
            if (cubes != null) {
                for (var cubeElement : cubes) {
                    out.add(cubeElement.getAsJsonObject());
                }
            }
            JsonArray children = bone.getAsJsonArray("bones");
            if (children != null) {
                collectCubeObjects(children, out);
            }
        }
    }

    private static double get(JsonObject cube, String member, int axis) {
        return cube.getAsJsonArray(member).get(axis).getAsDouble();
    }
}
