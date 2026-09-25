package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.layers.OceanLayer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout contracts of Brimstone Caldera: its feature set, the biome's stage parity with
 * the other custom oceans, and its slot in the middle-sea mix of the layer stack.
 */
public final class BrimstoneFeatureLayoutTest {
    private static final List<String> BRIMSTONE_FEATURES = List.of(
            "aquanaut:hot_spring",
            "aquanaut:smoker_cluster",
            "aquanaut:fumarole_field",
            "aquanaut:sulfur_veins",
            "aquanaut:brimstone_garden",
            "aquanaut:ash_drifts",
            "aquanaut:acid_lake",
            "aquanaut:vent_flora");

    @Test
    void brimstoneCalderaCarriesItsWholeThermalSuite() {
        JsonArray stages = loadStages("brimstone_caldera");
        List<String> features = new ArrayList<>();
        stages.forEach(stage -> stage.getAsJsonArray().forEach(entry -> features.add(entry.getAsString())));
        for (String feature : BRIMSTONE_FEATURES) {
            assertTrue(features.contains(feature), "brimstone caldera should generate " + feature);
        }
        assertFalse(features.contains("aquanaut:jelly_jungle_bulge"), "no jelly in the caldera");
        assertFalse(features.contains("aquanaut:brine_terraces"), "no brine terraces in the caldera");
        assertFalse(features.contains("aquanaut:coral_forest_pillar"), "no coral pillars in the caldera");
    }

    @Test
    void brimstoneCalderaKeepsTheCustomOceanStageCount() {
        assertEquals(loadStages("middle_level_ocean").size(), loadStages("brimstone_caldera").size(),
                "custom ocean biomes should keep the same number of generation stages");
        assertEquals(11, loadStages("brimstone_caldera").size());
    }

    @Test
    void middleSeaMixCarriesTheCalderaAlongsideTheOtherFloorBiomes() throws Exception {
        JsonObject stack = parse("src/main/resources/data/aquanaut/worldgen/ocean_layer_stack/default_deep_stack.json");
        JsonObject middleSea = null;
        for (var element : stack.getAsJsonArray("layers")) {
            JsonObject layer = element.getAsJsonObject();
            if ("aquanaut:middle_sea".equals(layer.get("id").getAsString())) {
                middleSea = layer;
            }
        }
        assertTrue(middleSea != null, "default stack keeps the middle sea layer");
        JsonArray entries = middleSea.getAsJsonObject("biomes").getAsJsonArray("entries");
        List<String> biomes = new ArrayList<>();
        entries.forEach(entry -> biomes.add(entry.getAsJsonObject().get("biome").getAsString()));
        assertEquals(4, biomes.size(), "middle sea mixes MLO, the gorge, the caldera and the crystal nest");
        assertTrue(biomes.contains("aquanaut:brimstone_caldera"));
        assertTrue(biomes.contains("aquanaut:crystal_nest"));
    }

    @Test
    void javaLayerMirrorMatchesTheDataStack() {
        OceanLayer middleSea = OceanLayer.middleSea();
        List<String> biomes = middleSea.mix().entries().stream().map(entry -> entry.biome().toString()).toList();
        assertEquals(4, biomes.size());
        assertTrue(biomes.contains("aquanaut:crystal_nest"),
                "the built-in stack must carry the crystal nest like the JSON does");
        assertTrue(biomes.contains(BrimstoneCalderaPlacement.location().toString()),
                "the built-in stack must carry the caldera like the JSON does");
    }

    private static JsonArray loadStages(String biomeName) {
        try {
            JsonObject biome = parse("src/main/resources/data/aquanaut/worldgen/biome/" + biomeName + ".json");
            return biome.getAsJsonArray("features");
        } catch (Exception e) {
            throw new AssertionError("failed to load biome " + biomeName, e);
        }
    }

    private static JsonObject parse(String path) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }
}
