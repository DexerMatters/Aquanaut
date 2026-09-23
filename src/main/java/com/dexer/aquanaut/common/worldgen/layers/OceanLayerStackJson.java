package com.dexer.aquanaut.common.worldgen.layers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gson codec for ocean layer stacks. Used for jar defaults and datapack overrides
 * (datapacks may drop {@code data/<ns>/ocean_layer_stack/<path>.json}).
 */
public final class OceanLayerStackJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private OceanLayerStackJson() {
    }

    public static OceanLayerStack fromJson(String json) {
        return parse(JsonParser.parseString(json).getAsJsonObject());
    }

    public static OceanLayerStack fromReader(Reader reader) {
        return parse(JsonParser.parseReader(reader).getAsJsonObject());
    }

    public static OceanLayerStack fromStream(InputStream stream) {
        return fromReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    public static OceanLayerStack fromPath(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return fromReader(reader);
        }
    }

    public static String toJson(OceanLayerStack stack) {
        return GSON.toJson(write(stack));
    }

    public static OceanLayerStack parse(JsonObject root) {
        ResourceLocation id = ResourceLocation.parse(root.get("id").getAsString());
        List<OceanLayer> layers = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("layers")) {
            layers.add(parseLayer(element.getAsJsonObject()));
        }
        double verticalBlend = optDouble(root, "vertical_blend_quarts", 1.0D);
        double edgeFade = optDouble(root, "region_edge_fade_blocks", 16.0D);
        int minOpen = (int) optDouble(root, "min_open_water_columns", 16.0D);
        List<ResourceLocation> parents = new ArrayList<>();
        if (root.has("parent_biomes")) {
            for (JsonElement element : root.getAsJsonArray("parent_biomes")) {
                parents.add(ResourceLocation.parse(element.getAsString()));
            }
        } else {
            parents.add(ResourceLocation.withDefaultNamespace("deep_ocean"));
            parents.add(ResourceLocation.withDefaultNamespace("deep_cold_ocean"));
            parents.add(ResourceLocation.withDefaultNamespace("deep_lukewarm_ocean"));
            parents.add(ResourceLocation.withDefaultNamespace("deep_frozen_ocean"));
        }
        return new OceanLayerStack(id, layers, verticalBlend, edgeFade, minOpen, parents);
    }

    private static OceanLayer parseLayer(JsonObject root) {
        ResourceLocation id = ResourceLocation.parse(root.get("id").getAsString());
        JsonObject band = root.getAsJsonObject("band");
        DepthBand depthBand = new DepthBand(
                band.get("min_y").getAsInt(),
                band.get("max_y").getAsInt(),
                optDouble(band, "blend_down", 0.0D),
                optDouble(band, "blend_up", 0.0D));

        JsonObject mixRoot = root.getAsJsonObject("biomes");
        List<MixEntry> entries = new ArrayList<>();
        for (JsonElement element : mixRoot.getAsJsonArray("entries")) {
            entries.add(parseEntry(element.getAsJsonObject()));
        }
        BiomeMix mix = new BiomeMix(entries, optDouble(mixRoot, "horizontal_blend_quarts", 2.0D));

        ResourceLocation terrain = root.has("terrain")
                ? ResourceLocation.parse(root.get("terrain").getAsString())
                : ResourceLocation.withDefaultNamespace("none");
        boolean carve = optBool(root, "carve", false);
        return new OceanLayer(id, depthBand, mix, terrain, carve);
    }

    private static MixEntry parseEntry(JsonObject root) {
        ResourceLocation biome = ResourceLocation.parse(root.get("biome").getAsString());
        double weight = optDouble(root, "weight", 1.0D);
        int noiseScale = (int) optDouble(root, "noise_scale", 32.0D);
        int offsetX = (int) optDouble(root, "noise_offset_x", 0.0D);
        int offsetZ = (int) optDouble(root, "noise_offset_z", 0.0D);
        long salt = (long) optDouble(root, "noise_salt", 2.0D);
        boolean inherit = optBool(root, "inherit_surface", false);
        return new MixEntry(biome, weight, noiseScale, offsetX, offsetZ, salt, inherit);
    }

    public static JsonObject write(OceanLayerStack stack) {
        JsonObject root = new JsonObject();
        root.addProperty("id", stack.id().toString());
        root.addProperty("vertical_blend_quarts", stack.verticalBlendQuarts());
        root.addProperty("region_edge_fade_blocks", stack.regionEdgeFadeBlocks());
        root.addProperty("min_open_water_columns", stack.minOpenWaterColumns());
        JsonArray parents = new JsonArray();
        for (ResourceLocation parent : stack.parentBiomes()) {
            parents.add(parent.toString());
        }
        root.add("parent_biomes", parents);
        JsonArray layers = new JsonArray();
        for (OceanLayer layer : stack.layers()) {
            layers.add(writeLayer(layer));
        }
        root.add("layers", layers);
        return root;
    }

    private static JsonObject writeLayer(OceanLayer layer) {
        JsonObject root = new JsonObject();
        root.addProperty("id", layer.id().toString());
        JsonObject band = new JsonObject();
        band.addProperty("min_y", layer.band().minY());
        band.addProperty("max_y", layer.band().maxY());
        band.addProperty("blend_down", layer.band().blendDown());
        band.addProperty("blend_up", layer.band().blendUp());
        root.add("band", band);

        JsonObject mix = new JsonObject();
        mix.addProperty("horizontal_blend_quarts", layer.mix().horizontalBlendQuarts());
        JsonArray entries = new JsonArray();
        for (MixEntry entry : layer.mix().entries()) {
            JsonObject entryJson = new JsonObject();
            entryJson.addProperty("biome", entry.biome().toString());
            entryJson.addProperty("weight", entry.weight());
            entryJson.addProperty("noise_scale", entry.noiseScaleQuarts());
            entryJson.addProperty("noise_offset_x", entry.noiseOffsetX());
            entryJson.addProperty("noise_offset_z", entry.noiseOffsetZ());
            entryJson.addProperty("noise_salt", entry.noiseSalt());
            entryJson.addProperty("inherit_surface", entry.inheritSurface());
            entries.add(entryJson);
        }
        mix.add("entries", entries);
        root.add("biomes", mix);
        root.addProperty("terrain", layer.terrain().toString());
        root.addProperty("carve", layer.carve());
        return root;
    }

    public static TerrainModule parseTerrain(JsonObject root) {
        return new TerrainModule(
                (int) optDouble(root, "cap_top_min_y", 35),
                (int) optDouble(root, "cap_top_max_y", 39),
                (int) optDouble(root, "min_cap_thickness", 4),
                (int) optDouble(root, "cap_thickness_variants", 5),
                (int) optDouble(root, "min_cavity_depth", 58),
                (int) optDouble(root, "cavity_depth_variants", 9),
                (int) optDouble(root, "min_floor_margin", 12),
                optDouble(root, "pillar_chance", 0.05D),
                optDouble(root, "pillar_connected_chance", 0.20D),
                optDouble(root, "pillar_height_min_ratio", 0.20D),
                optDouble(root, "pillar_height_max_ratio", 0.40D),
                (int) optDouble(root, "pillar_base_extra", 4),
                (int) optDouble(root, "crack_cell_size", 80),
                optDouble(root, "crack_threshold", 0.56D),
                optDouble(root, "crack_detail_threshold", 0.44D),
                optDouble(root, "crack_open_edge", 0.32D),
                optDouble(root, "pillar_edge", 0.45D),
                optDouble(root, "coral_tree_edge", 0.58D),
                optDouble(root, "coral_tree_chance", 0.06D),
                (int) optDouble(root, "top_water_y", 62),
                optDouble(root, "wall_intrusion", 0.15D),
                (int) optDouble(root, "wall_cell_size", 64));
    }

    public static void registerTerrainJson(ResourceLocation id, String json) {
        DefaultOceanLayerStacks.registerTerrain(id, parseTerrain(JsonParser.parseString(json).getAsJsonObject()));
    }

    public static void registerTerrainJson(ResourceLocation id, JsonObject json) {
        DefaultOceanLayerStacks.registerTerrain(id, parseTerrain(json));
    }

    private static double optDouble(JsonObject root, String key, double fallback) {
        return root.has(key) ? root.get(key).getAsDouble() : fallback;
    }

    private static boolean optBool(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }
}
