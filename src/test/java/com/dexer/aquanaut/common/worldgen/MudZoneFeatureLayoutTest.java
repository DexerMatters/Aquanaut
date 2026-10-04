package com.dexer.aquanaut.common.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout contract of the mud zone: the aquanaut features that build its floor, landforms,
 * cracks and dressing, and its stage parity with the other custom oceans.
 */
public final class MudZoneFeatureLayoutTest {

    @Test
    void mudZoneCarriesItsFeatureSuite() {
        JsonArray stages = loadStages("mud_zone");
        List<String> features = new ArrayList<>();
        stages.forEach(stage -> stage.getAsJsonArray().forEach(entry -> features.add(entry.getAsString())));
        for (String feature : List.of(
                "aquanaut:mud_zone_sediment",
                "aquanaut:mud_mound",
                "aquanaut:fossil_outcrop",
                "aquanaut:mud_crack",
                "aquanaut:mud_flora")) {
            assertTrue(features.contains(feature), "mud zone should generate " + feature);
        }
        assertTrue(features.contains("minecraft:freeze_top_layer"), "top-layer stage stays vanilla");
    }

    @Test
    void mudZoneKeepsStageParityWithTheCustomOceans() {
        assertEquals(loadStages("middle_level_ocean").size(), loadStages("mud_zone").size(),
                "custom ocean biomes should keep the same number of generation stages");
        assertEquals(11, loadStages("mud_zone").size());
    }

    private static JsonArray loadStages(String biomeName) {
        try {
            JsonObject biome = JsonParser.parseString(Files.readString(
                    Path.of("src/main/resources/data/aquanaut/worldgen/biome/" + biomeName + ".json")))
                    .getAsJsonObject();
            return biome.getAsJsonArray("features");
        } catch (Exception e) {
            throw new AssertionError("failed to load biome " + biomeName, e);
        }
    }
}
