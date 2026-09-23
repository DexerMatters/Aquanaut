package com.dexer.aquanaut;

import com.dexer.aquanaut.common.entity.DroneGeometry;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the drone's model-derived numbers.
 *
 * <p>
 * The pilot's camera rides the drone's nose sensor, and the entity's eye height is computed from
 * {@link DroneGeometry} rather than typed in by hand. This test re-derives that sensor from the
 * shipped {@code submarine_drone.geo.json}: re-export the model with the pod somewhere else and the
 * test fails instead of the feed quietly looking out of the roof of the hull for another release.
 */
final class DroneGeometryTest {

    private static final Path GEO = Path.of("src/main/resources/assets/aquanaut/geo/submarine_drone.geo.json");

    @Test
    void theEyeHeightIsTheMiddleOfTheModelsNoseSensor() throws IOException {
        JsonObject sensor = frontMostCube();

        assertEquals(DroneGeometry.NOSE_SENSOR_MIN_Y, y(sensor, "origin"), 1.0E-9D,
                "the nose sensor moved; update DroneGeometry");
        assertEquals(DroneGeometry.NOSE_SENSOR_MAX_Y,
                y(sensor, "origin") + y(sensor, "size"), 1.0E-9D,
                "the nose sensor changed height; update DroneGeometry");

        // Four units up, at sixteen units to the block.
        assertEquals(0.25F, DroneGeometry.EYE_HEIGHT, 1.0E-6F);
    }

    /**
     * The camera has to be inside the drone, not above it: that is the whole point of deriving the
     * height from the model. The hull and its beacon are what the drone looks like from outside.
     */
    @Test
    void theEyeHeightIsInsideTheHull() throws IOException {
        List<JsonObject> cubes = new ArrayList<>();
        collectCubes(geometry().getAsJsonArray("bones"), cubes);
        assertFalse(cubes.isEmpty(), "submarine_drone.geo.json has no cubes");

        double hullBottom = Double.POSITIVE_INFINITY;
        double hullTop = Double.NEGATIVE_INFINITY;
        for (JsonObject cube : cubes) {
            hullBottom = Math.min(hullBottom, y(cube, "origin"));
            hullTop = Math.max(hullTop, y(cube, "origin") + y(cube, "size"));
        }

        double eyeUnits = DroneGeometry.EYE_HEIGHT * DroneGeometry.UNITS_PER_BLOCK;
        assertTrue(eyeUnits > hullBottom && eyeUnits < hullTop,
                "the eye height is outside the drone: " + eyeUnits + " units against a hull of "
                        + hullBottom + ".." + hullTop);
    }

    /** The drone's model is authored standing on its origin, like every other entity in the pack. */
    @Test
    void theModelStandsOnItsOrigin() throws IOException {
        List<JsonObject> cubes = new ArrayList<>();
        collectCubes(geometry().getAsJsonArray("bones"), cubes);
        double bottom = Double.POSITIVE_INFINITY;
        for (JsonObject cube : cubes) {
            bottom = Math.min(bottom, y(cube, "origin"));
        }
        assertEquals(0.0D, bottom, 1.0E-9D,
                "a model whose feet are not at y=0 would float or sink by the difference");
    }

    private static JsonObject geometry() throws IOException {
        return JsonParser.parseString(Files.readString(GEO, StandardCharsets.UTF_8))
                .getAsJsonObject()
                .getAsJsonArray("minecraft:geometry")
                .get(0)
                .getAsJsonObject();
    }

    /**
     * The front-most cube of the hull: the smallest {@code z} reach of any cube. On this model that is
     * the nose sensor, which is also what the renderer lights through the glowmask.
     */
    private static JsonObject frontMostCube() throws IOException {
        List<JsonObject> cubes = new ArrayList<>();
        collectCubes(geometry().getAsJsonArray("bones"), cubes);
        if (cubes.isEmpty()) {
            throw new IllegalStateException("submarine_drone.geo.json has no cubes");
        }

        JsonObject best = null;
        double bestFront = Double.POSITIVE_INFINITY;
        for (JsonObject cube : cubes) {
            double front = y(cube, "origin", 2);
            if (front < bestFront) {
                bestFront = front;
                best = cube;
            }
        }
        return best;
    }

    private static double y(JsonObject cube, String member) {
        return y(cube, member, 1);
    }

    private static double y(JsonObject cube, String member, int axis) {
        return cube.getAsJsonArray(member).get(axis).getAsDouble();
    }

    private static void collectCubes(JsonArray bones, List<JsonObject> out) {
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
                collectCubes(children, out);
            }
        }
    }
}
