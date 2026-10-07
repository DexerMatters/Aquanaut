package com.dexer.aquanaut.common.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring invariants of the water world preset that the island generation silently depends on:
 * the island biomes must be declared in the climate source, or vanilla's biome decoration
 * drops them (it intersects section palettes with the source's possible biomes), and the
 * surface rule must leave painting to the mod, or the vanilla surface system repaints the
 * island's stone and lava basin with grass.
 */
class PresetWiringTest {
    private static final String[] ISLAND_BIOMES =
            {"island_plains", "island_hills", "island_beach", "island_stony_shore"};

    @Test
    void presetDeclaresTheIslandBiomesAsPossibleBiomes() {
        JsonArray biomes = load("data/aquanaut/worldgen/world_preset/water_world.json")
                .getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld")
                .getAsJsonObject("generator")
                .getAsJsonObject("biome_source")
                .getAsJsonArray("biomes");

        Set<String> declared = new LinkedHashSet<>();
        List<String> unreachable = new ArrayList<>();
        for (JsonElement element : biomes) {
            JsonObject entry = element.getAsJsonObject();
            String id = entry.get("biome").getAsString();
            declared.add(id);
            if (id.startsWith("aquanaut:island_")
                    && entry.getAsJsonObject("parameters").get("offset").getAsDouble() == 1.0D) {
                unreachable.add(id);
            }
        }
        for (String island : ISLAND_BIOMES) {
            assertTrue(declared.contains("aquanaut:" + island),
                    "preset does not declare aquanaut:" + island
                            + " - vanilla decoration would drop every island biome from its"
                            + " feature scheduling and the island would carry no vegetation");
        }
        assertTrue(unreachable.size() >= ISLAND_BIOMES.length,
                "island biomes must sit on unreachable anchors (offset 1.0) so the climate"
                        + " never actually picks them: " + unreachable);
    }

    @Test
    void waterWorldSurfaceRuleLeavesPaintingToTheMod() {
        JsonObject surfaceRule = load("data/aquanaut/worldgen/noise_settings/water_world.json")
                .getAsJsonObject("surface_rule");

        Set<String> materials = new LinkedHashSet<>();
        collectMaterials(surfaceRule, materials);
        for (String material : materials) {
            assertTrue("minecraft:bedrock".equals(material) || "minecraft:deepslate".equals(material),
                    "the water world surface rule still paints " + material
                            + " - the vanilla surface system would repaint the island's"
                            + " painted ground (grass over the quarry and the lava basin)");
        }
        assertTrue(materials.contains("minecraft:bedrock"),
                "the water world must keep its bedrock floor");
    }

    private static void collectMaterials(JsonElement element, Set<String> materials) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectMaterials(child, materials);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        JsonElement resultState = object.get("result_state");
        if (resultState != null) {
            materials.add(resultState.getAsJsonObject().get("Name").getAsString());
        }
        for (String key : List.of("sequence", "then_run", "if_true", "fallback")) {
            JsonElement child = object.get(key);
            if (child != null) {
                collectMaterials(child, materials);
            }
        }
    }

    private static JsonObject load(String path) {
        try (InputStream stream = PresetWiringTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("missing resource " + path);
            }
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("failed to load " + path, e);
        }
    }
}
