package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The island biomes are mod-owned copies of their vanilla counterparts. The copies must
 * keep vanilla flora (trees, flowers, grass) but strip every lava feature and the vanilla
 * carvers, so the island's analytically carved caves stay dry and untouched.
 */
class IslandBiomeResourcesTest {
    private static final String[] ISLAND_BIOMES =
            {"island_plains", "island_hills", "island_beach", "island_stony_shore"};

    @Test
    void islandBiomesCarryNoLavaFeatures() {
        for (String biome : ISLAND_BIOMES) {
            String json = load(biome);
            assertFalse(json.contains("lake_lava"), biome + " still places vanilla lava lakes");
            assertFalse(json.contains("spring_lava"), biome + " still places lava springs");
        }
    }

    @Test
    void islandBiomesStripTheVanillaCarvers() {
        for (String biome : ISLAND_BIOMES) {
            String json = load(biome);
            int carvers = json.indexOf("\"carvers\"");
            assertTrue(carvers >= 0, biome + " has no carvers field");
            int brace = json.indexOf('{', carvers);
            assertTrue(brace >= 0 && json.indexOf('}', brace) == brace + 1,
                    biome + " still runs vanilla carvers");
        }
    }

    @Test
    void islandBiomesKeepVanillaFlora() {
        String plains = load("island_plains");
        assertTrue(plains.contains("minecraft:trees_birch_and_oak"),
                "island plains lost its forest");
        assertTrue(plains.contains("minecraft:patch_sunflower"), "island plains lost its sunflowers");
        assertTrue(plains.contains("minecraft:patch_grass_plain"), "island plains lost its grass");
        String hills = load("island_hills");
        assertTrue(hills.contains("minecraft:trees_windswept_hills"), "island hills lost its trees");
        assertTrue(hills.contains("minecraft:trees_birch_and_oak"),
                "island hills lost its forest");
        assertTrue(hills.contains("minecraft:patch_grass_badlands"), "island hills lost its grass");
        for (String biome : new String[]{"island_beach", "island_stony_shore"}) {
            assertTrue(load(biome).contains("minecraft:patch_grass_badlands"),
                    biome + " lost its grass cover");
        }
    }

    @Test
    void islandBiomesCarryTheirCreatureSpawners() {
        // The island herd pass samples the surface biome's creature list, so these tables
        // are what populates the island: the grassland biomes must keep their vanilla herds.
        String plains = load("island_plains");
        assertTrue(plains.contains("minecraft:sheep") && plains.contains("minecraft:cow")
                        && plains.contains("minecraft:horse"),
                "island plains lost its herds");
        assertTrue(load("island_hills").contains("minecraft:sheep"),
                "island hills lost its herds");
        assertTrue(load("island_beach").contains("minecraft:turtle"),
                "island beach lost its turtles");
    }

    private String load(String biome) {
        String path = "data/aquanaut/worldgen/biome/" + biome + ".json";
        try (InputStream stream = IslandBiomeResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("missing island biome resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError("failed to load island biome resource " + path, e);
        }
    }
}
