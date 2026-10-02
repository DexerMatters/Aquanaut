package com.dexer.aquanaut.common.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards the complete GeckoLib transform that hangs the board on its occupied wall cells. */
public final class InvestigationBoardPlacementTest {
    private static final Path GEO = Path.of(
            "src/main/resources/assets/aquanaut/geo/investigation_board.geo.json");
    private static final double EPSILON = 1.0E-6D;
    private static final double UNITS_PER_BLOCK = 16.0D;

    @Test
    void shippedGeometryMatchesThePlacementAssumptions() {
        assertArrayEquals(new double[] { -1.0D, 1.0D, 0.0D, 1.5D, -1.0D / 16.0D, 1.0D / 16.0D },
                modelBounds(), EPSILON);
    }

    @Test
    void everyFacingCoversExactlyItsTwoColumnsAndCollisionDepth() {
        assertWorldBounds(Direction.NORTH,
                new double[] { -1.0D, 1.0D, 0.0D, 1.5D, 13.0D / 16.0D, 15.0D / 16.0D });
        assertWorldBounds(Direction.EAST,
                new double[] { 1.0D / 16.0D, 3.0D / 16.0D, 0.0D, 1.5D, -1.0D, 1.0D });
        assertWorldBounds(Direction.SOUTH,
                new double[] { 0.0D, 2.0D, 0.0D, 1.5D, 1.0D / 16.0D, 3.0D / 16.0D });
        assertWorldBounds(Direction.WEST,
                new double[] { 13.0D / 16.0D, 15.0D / 16.0D, 0.0D, 1.5D, 0.0D, 2.0D });
    }

    @Test
    void poseOnlyOffsetsGeckoLibsCentredModel() {
        assertPose(Direction.NORTH, -0.5D, 0.375D);
        assertPose(Direction.EAST, -0.375D, -0.5D);
        assertPose(Direction.SOUTH, 0.5D, -0.375D);
        assertPose(Direction.WEST, 0.375D, 0.5D);
    }

    private static void assertWorldBounds(Direction facing, double[] expected) {
        assertArrayEquals(expected, project(facing, modelBounds()), EPSILON, facing.toString());
    }

    /** Applies translation, GeckoLib's half-block centring, then its FACING rotation. */
    private static double[] project(Direction facing, double[] bounds) {
        InvestigationBoardPlacement.Pose pose = InvestigationBoardPlacement.forFacing(facing);
        double yaw = Math.toRadians(rendererYaw(facing));
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double[] projected = { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE };

        for (double x : new double[] { bounds[0], bounds[1] }) {
            for (double y : new double[] { bounds[2], bounds[3] }) {
                for (double z : new double[] { bounds[4], bounds[5] }) {
                    double worldX = pose.x() + InvestigationBoardPlacement.BLOCK_CENTRE
                            + x * cos + z * sin;
                    double worldZ = pose.z() + InvestigationBoardPlacement.BLOCK_CENTRE
                            - x * sin + z * cos;
                    projected[0] = Math.min(projected[0], worldX);
                    projected[1] = Math.max(projected[1], worldX);
                    projected[2] = Math.min(projected[2], y + pose.y());
                    projected[3] = Math.max(projected[3], y + pose.y());
                    projected[4] = Math.min(projected[4], worldZ);
                    projected[5] = Math.max(projected[5], worldZ);
                }
            }
        }
        return projected;
    }

    /** GeckoLib 4.8's GeoBlockRenderer.rotateBlock mapping for horizontal facings. */
    private static float rendererYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case WEST -> 90.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            // A board hangs on a wall, so it has no renderer yaw for a facing with no horizontal
            // component. The shim grew UP and DOWN for the sonar's cavity probes; nothing here uses
            // them, and saying so is better than inventing an angle.
            case UP, DOWN -> throw new IllegalArgumentException("not a horizontal facing: " + facing);
        };
    }

    private static void assertPose(Direction facing, double x, double z) {
        InvestigationBoardPlacement.Pose pose = InvestigationBoardPlacement.forFacing(facing);
        assertEquals(x, pose.x(), EPSILON, facing + " x offset");
        assertEquals(0.0D, pose.y(), EPSILON, facing + " y offset");
        assertEquals(z, pose.z(), EPSILON, facing + " z offset");
    }

    /** Overall cube bounds of the shipped model, converted from model units to blocks. */
    private static double[] modelBounds() {
        JsonObject geometry = readJson(GEO).getAsJsonArray("minecraft:geometry")
                .get(0).getAsJsonObject();
        double[] bounds = { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE };
        for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
            JsonObject bone = boneElement.getAsJsonObject();
            for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                JsonObject cube = cubeElement.getAsJsonObject();
                double[] origin = components(cube.getAsJsonArray("origin"));
                double[] size = components(cube.getAsJsonArray("size"));
                for (int axis = 0; axis < 3; axis++) {
                    bounds[axis * 2] = Math.min(bounds[axis * 2], origin[axis] / UNITS_PER_BLOCK);
                    bounds[axis * 2 + 1] = Math.max(bounds[axis * 2 + 1],
                            (origin[axis] + size[axis]) / UNITS_PER_BLOCK);
                }
            }
        }
        return bounds;
    }

    private static double[] components(JsonElement array) {
        double[] values = new double[3];
        for (int axis = 0; axis < values.length; axis++) {
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
