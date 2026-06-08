package com.dexer.aquanaut.common.inventory.aquarium;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AquariumModelFootprint {

    private static final Map<String, Optional<Footprint>> CACHE = new HashMap<>();

    private AquariumModelFootprint() {
    }

    public static Optional<Footprint> resolve(String namespace, String path) {
        String cacheKey = namespace + ":" + path;
        Optional<Footprint> cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Optional<Footprint> resolved = load(namespace, path);
        CACHE.put(cacheKey, resolved);
        return resolved;
    }

    private static Optional<Footprint> load(String namespace, String path) {
        String resourcePath = "assets/" + namespace + "/" + path;
        try (InputStream stream = AquariumModelFootprint.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Optional.empty();
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            float[] bounds = new float[]{
                    Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE
            };

            if (root.has("minecraft:geometry")) {
                for (JsonElement geometry : root.getAsJsonArray("minecraft:geometry")) {
                    if (!(geometry instanceof JsonObject geometryObject) || !geometryObject.has("bones")) {
                        continue;
                    }

                    Map<String, Bone> bones = parseBones(geometryObject.getAsJsonArray("bones"));
                    Map<String, Matrix4f> transforms = new HashMap<>();
                    for (Bone bone : bones.values()) {
                        Matrix4f boneTransform = boneTransform(bone, bones, transforms);
                        for (Cube cube : bone.cubes()) {
                            extendBounds(bounds, cube, boneTransform);
                        }
                    }
                }
            }

            if (bounds[0] == Float.MAX_VALUE) {
                return Optional.empty();
            }

            float modelWidth = bounds[3] - bounds[0];
            float modelHeight = bounds[4] - bounds[1];
            float modelLength = bounds[5] - bounds[2];
            int gridWidth = Math.max(1, Math.round(modelLength / 16.0F));
            int gridHeight = Math.max(1, Math.round(modelHeight / 16.0F));
            return Optional.of(new Footprint(modelLength, modelWidth, modelHeight, gridWidth, gridHeight));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Map<String, Bone> parseBones(JsonArray boneArray) {
        Map<String, Bone> bones = new HashMap<>();
        for (JsonElement boneElement : boneArray) {
            if (!(boneElement instanceof JsonObject boneObject) || !boneObject.has("name")) {
                continue;
            }

            String name = boneObject.get("name").getAsString();
            String parent = boneObject.has("parent") ? boneObject.get("parent").getAsString() : null;
            Vector3f pivot = getVector(boneObject, "pivot");
            Vector3f rotation = getVector(boneObject, "rotation");
            List<Cube> cubes = new ArrayList<>();
            if (boneObject.has("cubes")) {
                for (JsonElement cubeElement : boneObject.getAsJsonArray("cubes")) {
                    if (!(cubeElement instanceof JsonObject cubeObject)) {
                        continue;
                    }
                    Vector3f origin = getVector(cubeObject, "origin");
                    Vector3f size = getVector(cubeObject, "size");
                    Vector3f cubePivot = cubeObject.has("pivot") ? getVector(cubeObject, "pivot") : null;
                    Vector3f cubeRotation = cubeObject.has("rotation") ? getVector(cubeObject, "rotation") : null;
                    cubes.add(new Cube(origin, size, cubePivot, cubeRotation));
                }
            }

            bones.put(name, new Bone(name, parent, pivot, rotation, cubes));
        }
        return bones;
    }

    private static Matrix4f boneTransform(Bone bone, Map<String, Bone> bones, Map<String, Matrix4f> transforms) {
        Matrix4f cached = transforms.get(bone.name());
        if (cached != null) {
            return new Matrix4f(cached);
        }

        Matrix4f transform = bone.parentName() == null
                ? new Matrix4f()
                : boneTransform(bones.get(bone.parentName()), bones, transforms);
        transform = rotateAroundPivot(transform, bone.pivot(), bone.rotation());
        transforms.put(bone.name(), new Matrix4f(transform));
        return transform;
    }

    private static Matrix4f rotateAroundPivot(Matrix4f parentTransform, Vector3f pivot, Vector3f rotationDegrees) {
        if (rotationDegrees == null || (rotationDegrees.x == 0.0F && rotationDegrees.y == 0.0F && rotationDegrees.z == 0.0F)) {
            return new Matrix4f(parentTransform);
        }

        return new Matrix4f(parentTransform)
                .translate(pivot)
                .rotateXYZ(
                        (float) Math.toRadians(rotationDegrees.x),
                        (float) Math.toRadians(rotationDegrees.y),
                        (float) Math.toRadians(rotationDegrees.z))
                .translate(-pivot.x, -pivot.y, -pivot.z);
    }

    private static void extendBounds(float[] bounds, Cube cube, Matrix4f boneTransform) {
        Matrix4f cubeTransform = cube.rotation() == null || cube.pivot() == null
                ? new Matrix4f(boneTransform)
                : rotateAroundPivot(boneTransform, cube.pivot(), cube.rotation());

        float[] xs = new float[]{cube.origin().x, cube.origin().x + cube.size().x};
        float[] ys = new float[]{cube.origin().y, cube.origin().y + cube.size().y};
        float[] zs = new float[]{cube.origin().z, cube.origin().z + cube.size().z};

        for (float x : xs) {
            for (float y : ys) {
                for (float z : zs) {
                    Vector3f point = cubeTransform.transformPosition(new Vector3f(x, y, z));
                    bounds[0] = Math.min(bounds[0], point.x);
                    bounds[1] = Math.min(bounds[1], point.y);
                    bounds[2] = Math.min(bounds[2], point.z);
                    bounds[3] = Math.max(bounds[3], point.x);
                    bounds[4] = Math.max(bounds[4], point.y);
                    bounds[5] = Math.max(bounds[5], point.z);
                }
            }
        }
    }

    private static Vector3f getVector(JsonObject object, String key) {
        if (!object.has(key)) {
            return new Vector3f();
        }
        JsonArray values = object.getAsJsonArray(key);
        return new Vector3f(
                getFloat(values, 0),
                getFloat(values, 1),
                getFloat(values, 2));
    }

    private static float getFloat(JsonArray values, int index) {
        return index < values.size() ? values.get(index).getAsFloat() : 0.0F;
    }

    public record Footprint(float length, float width, float height, int gridWidth, int gridHeight) {
    }

    private record Bone(String name, String parentName, Vector3f pivot, Vector3f rotation, List<Cube> cubes) {
    }

    private record Cube(Vector3f origin, Vector3f size, Vector3f pivot, Vector3f rotation) {
    }
}
