package com.dexer.aquanaut.common.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the pose that puts a merged dissection bench on its footprint.
 *
 * <p>
 * A bench whose pose is wrong still looks plausible as a single table and is obviously off the moment
 * two are merged, so the arithmetic is checked against the shipped geometry rather than by eye: for
 * every layout the projected model bounds have to land exactly on the group's footprint, and the
 * extents the placement assumes have to match the models that actually ship.
 */
public final class DissectionTablePlacementTest {

    private static final Path GEO = Path.of("src/main/resources/assets/aquanaut/geo");
    private static final double EPSILON = 1.0E-6D;
    private static final double UNITS_PER_BLOCK = 16.0D;

    @Test
    void everyLayoutLandsExactlyOnItsFootprint() {
        assertPose("", 1, 1);
        assertPose("_x2", 2, 1);
        assertPose("_x2", 1, 2);
        assertPose("_x4", 2, 2);
    }

    @Test
    void aTurnedBenchActuallyTurns() {
        DissectionTablePlacement.Pose alongX = pose(2, 1);
        DissectionTablePlacement.Pose alongZ = pose(1, 2);

        assertEquals(0.0F, alongX.yawDegrees(), EPSILON, "an east-west bench is not turned");
        assertEquals(-90.0F, alongZ.yawDegrees(), EPSILON,
                "a north-south bench is the same model turned a quarter turn clockwise");
        assertEquals(alongX.y(), alongZ.y(), EPSILON, "both orientations stand on the same floor");
        // The long bench is slid half a block east to cover two cells; the turned bench is one block
        // deep, so it only has to be slid half a block south.
        assertEquals(0.5D, alongX.x(), EPSILON);
        assertEquals(0.0D, alongX.z(), EPSILON);
        assertEquals(1.0D, alongZ.x(), EPSILON);
        assertEquals(0.5D, alongZ.z(), EPSILON);
    }

    @Test
    void thePlacementMatchesTheShippedModels() {
        for (DissectionTableMultiblock.Group group : new DissectionTableMultiblock.Group[] {
                group(1, 1), group(2, 1), group(1, 2), group(2, 2) }) {
            String suffix = group.modelSuffix();
            DissectionTablePlacement.Span shipped = spanOf("dissection_table" + suffix);
            DissectionTablePlacement.Span assumed = DissectionTablePlacement.MODEL_SPANS.get(suffix);

            assertEquals(assumed, shipped,
                    "the geometry of dissection_table" + suffix + " no longer matches the pose maths");
        }
    }

    /** Applies a pose the way the renderer does and returns the model bounds in cell-relative blocks. */
    private static double[] project(DissectionTablePlacement.Pose pose, DissectionTablePlacement.Span span) {
        double yaw = Math.toRadians(pose.yawDegrees());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (double x : new double[] { span.minX(), span.maxX() }) {
            for (double y : new double[] { span.minY(), span.maxY() }) {
                for (double z : new double[] { span.minZ(), span.maxZ() }) {
                    // GeckoLib shifts the model to the block centre before the renderer's own pose.
                    double localX = x + DissectionTablePlacement.BLOCK_CENTRE;
                    double localZ = z + DissectionTablePlacement.BLOCK_CENTRE;
                    // Axis.YP.rotationDegrees: the model's +X turns to the north, so a point maps to
                    // (x cos + z sin, -x sin + z cos) and the pose translate is applied afterwards.
                    double worldX = localX * cos + localZ * sin + pose.x();
                    double worldZ = -localX * sin + localZ * cos + pose.z();
                    minX = Math.min(minX, worldX);
                    maxX = Math.max(maxX, worldX);
                    minY = Math.min(minY, y + pose.y());
                    minZ = Math.min(minZ, worldZ);
                    maxZ = Math.max(maxZ, worldZ);
                }
            }
        }
        return new double[] { minX, maxX, minY, minZ, maxZ };
    }

    private static void assertPose(String suffix, int widthX, int depthZ) {
        DissectionTablePlacement.Pose pose = pose(widthX, depthZ);
        DissectionTablePlacement.Span span = DissectionTablePlacement.MODEL_SPANS.get(suffix);
        double[] bounds = project(pose, span);

        assertEquals(0.0D, bounds[0], EPSILON, suffix + " model must start on the group's west edge");
        assertEquals(widthX, bounds[1], EPSILON, suffix + " model must end on the group's east edge");
        assertEquals(0.0D, bounds[2], EPSILON, suffix + " model must stand on the block floor");
        assertEquals(0.0D, bounds[3], EPSILON, suffix + " model must start on the group's north edge");
        assertEquals(depthZ, bounds[4], EPSILON, suffix + " model must end on the group's south edge");
    }

    private static DissectionTablePlacement.Pose pose(int widthX, int depthZ) {
        return DissectionTablePlacement.forGroup(group(widthX, depthZ));
    }

    private static DissectionTableMultiblock.Group group(int widthX, int depthZ) {
        return new DissectionTableMultiblock.Group(new BlockPos(0, 0, 0), widthX, depthZ);
    }

    /** The outer bounds of a shipped geometry, converted from model units to blocks. */
    private static DissectionTablePlacement.Span spanOf(String model) {
        JsonObject geometry = readJson(GEO.resolve(model + ".geo.json"))
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        double[] bounds = { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE,
                Double.MAX_VALUE, -Double.MAX_VALUE };
        collect(geometry.getAsJsonArray("bones"), bounds);
        return new DissectionTablePlacement.Span(
                bounds[0] / UNITS_PER_BLOCK, bounds[1] / UNITS_PER_BLOCK,
                bounds[2] / UNITS_PER_BLOCK, bounds[3] / UNITS_PER_BLOCK,
                bounds[4] / UNITS_PER_BLOCK, bounds[5] / UNITS_PER_BLOCK);
    }

    private static void collect(JsonElement bones, double[] bounds) {
        for (JsonElement element : bones.getAsJsonArray()) {
            JsonObject bone = element.getAsJsonObject();
            if (bone.has("cubes")) {
                for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = cubeElement.getAsJsonObject();
                    double[] origin = components(cube.getAsJsonArray("origin"));
                    double[] size = components(cube.getAsJsonArray("size"));
                    for (int axis = 0; axis < 3; axis++) {
                        bounds[axis * 2] = Math.min(bounds[axis * 2], origin[axis]);
                        bounds[axis * 2 + 1] = Math.max(bounds[axis * 2 + 1], origin[axis] + size[axis]);
                    }
                }
            }
            if (bone.has("children")) {
                collect(bone.getAsJsonArray("children"), bounds);
            }
        }
    }

    private static double[] components(JsonElement array) {
        double[] values = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            values[axis] = array.getAsJsonArray().get(axis).getAsDouble();
        }
        return values;
    }

    private static JsonObject readJson(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException error) {
            throw new AssertionError("failed to read " + path, error);
        }
    }
}
