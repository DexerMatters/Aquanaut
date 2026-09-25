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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CrystalNestFeatureLayoutTest {

    @Test
    void crystalNestKeepsTheCustomOceanFeatureStageCount() {
        assertEquals(loadStages("middle_level_ocean").size(), loadStages("crystal_nest").size(),
                "crystal nest should keep the same number of generation stages");
        assertEquals(11, loadStages("crystal_nest").size());
    }

    @Test
    void theGeodeLatticeSuppliesItsOwnCrystals() {
        List<String> features = flatten(loadStages("crystal_nest"));
        assertFalse(features.contains("minecraft:amethyst_geode"),
                "the skinned cores bring their own chambers; vanilla geodes would clash");
    }

    @Test
    void theCrystalNestStartsWithoutAnyWildlife() {
        JsonObject biome = parse("src/main/resources/data/aquanaut/worldgen/biome/crystal_nest.json");
        JsonObject spawners = biome.getAsJsonObject("spawners");
        for (String category : new String[]{"monster", "creature", "ambient", "axolotls",
                "underground_water_creature", "water_ambient", "water_creature", "misc"}) {
            JsonArray entries = spawners.getAsJsonArray(category);
            assertEquals(0, entries.size(), "no wildlife in the crystal nest yet: " + category);
        }
    }

    @Test
    void everyNewBlockLootsItself() {
        for (String name : List.of("crystal_nest_stone", "crystal_druse", "crystal_column",
                "white_crystal_cluster", "rose_crystal_cluster", "amethyst_crystal_cluster",
                "aqua_crystal_cluster", "smoky_crystal_cluster", "resonant_crystal_cluster",
                "life_gem_cluster", "algae_mat", "algae_tuft", "crystal_sprout",
                "crystal_fringe")) {
            JsonObject loot = parse("src/main/resources/data/aquanaut/loot_table/blocks/" + name + ".json");
            String drops = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject()
                    .get("name").getAsString();
            assertEquals("aquanaut:" + name, drops, name + " drops itself");
            assertTrue(Files.exists(Path.of("src/main/resources/assets/aquanaut/blockstates/" + name + ".json")),
                    name + " has a blockstate");
            assertTrue(Files.exists(Path.of("src/main/resources/assets/aquanaut/models/item/" + name + ".json")),
                    name + " has an item model");
        }
    }

    @Test
    void theGlowingPairShipsGlowmasksAndTheQuietCrystalsDoNot() {
        for (String name : List.of("resonant_crystal_cluster", "life_gem_cluster")) {
            assertTrue(Files.exists(Path.of(
                            "src/main/resources/assets/aquanaut/textures/block/" + name + "_glowmask.png")),
                    name + " glows through a glowmask");
        }
        for (String name : List.of("white_crystal_cluster", "rose_crystal_cluster",
                "amethyst_crystal_cluster", "aqua_crystal_cluster", "smoky_crystal_cluster")) {
            assertFalse(Files.exists(Path.of(
                            "src/main/resources/assets/aquanaut/textures/block/" + name + "_glowmask.png")),
                    name + " must stay unlit on the open surface");
        }
    }

    private static JsonArray loadStages(String biomeName) {
        return parse("src/main/resources/data/aquanaut/worldgen/biome/" + biomeName + ".json")
                .getAsJsonArray("features");
    }

    private static List<String> flatten(JsonArray stages) {
        List<String> features = new ArrayList<>();
        stages.forEach(stage -> stage.getAsJsonArray().forEach(feature -> features.add(feature.getAsString())));
        return features;
    }

    private static JsonObject parse(String path) {
        try {
            return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("failed to load " + path, e);
        }
    }
}
