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
 * Layout contracts of the Brine Mirror Gorge enrichment: the new evaporite structures
 * and decorations all generate, the original suite survives, and the stage count stays
 * in parity with the other custom oceans.
 */
public final class BrineGorgeFeatureLayoutTest {
    private static final List<String> ENRICHMENT_FEATURES = List.of(
            "aquanaut:salt_diapir",
            "aquanaut:crystal_grotto",
            "aquanaut:salt_arch",
            "aquanaut:druse_vein",
            "aquanaut:hopper_garden",
            "aquanaut:gypsum_garden",
            "aquanaut:salt_cascade");

    @Test
    void brineGorgeCarriesItsWholeEnrichmentSuite() {
        List<String> features = flatten(loadStages("brine_mirror_gorge"));
        for (String feature : ENRICHMENT_FEATURES) {
            assertTrue(features.contains(feature), "brine gorge should generate " + feature);
        }
    }

    @Test
    void brineGorgeKeepsItsOriginalFormations() {
        List<String> features = flatten(loadStages("brine_mirror_gorge"));
        for (String feature : List.of("aquanaut:brine_terraces", "aquanaut:brine_mirrors",
                "aquanaut:halite_organ", "aquanaut:calcite_quill_field", "aquanaut:salt_fringe_curtain")) {
            assertTrue(features.contains(feature), "brine gorge should keep " + feature);
        }
    }

    @Test
    void enrichmentKeepsTheCustomOceanStageCount() {
        assertEquals(loadStages("middle_level_ocean").size(), loadStages("brine_mirror_gorge").size(),
                "custom ocean biomes should keep the same number of generation stages");
    }

    private static List<String> flatten(JsonArray stages) {
        List<String> features = new ArrayList<>();
        stages.forEach(stage -> stage.getAsJsonArray().forEach(entry -> features.add(entry.getAsString())));
        return features;
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
