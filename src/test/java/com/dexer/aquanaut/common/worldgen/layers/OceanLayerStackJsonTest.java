package com.dexer.aquanaut.common.worldgen.layers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OceanLayerStackJsonTest {

    @Test
    void jarDefaultStackParses() throws Exception {
        Path path = Path.of("src/main/resources/data/aquanaut/worldgen/ocean_layer_stack/default_deep_stack.json");
        OceanLayerStack stack = OceanLayerStackJson.fromPath(path);
        assertEquals(4, stack.layers().size());
        assertTrue(stack.layers().get(1).rewritesBiome());
        assertEquals(3, stack.layers().get(1).mix().entries().size());
        // The deep sea is the last layer and its single placeholder biome fills the abyss.
        assertEquals("aquanaut:deep_sea", stack.layers().get(3).id().toString());
        assertEquals(1, stack.layers().get(3).mix().entries().size());
    }

    @Test
    void terrainModulesParse() throws Exception {
        Path path = Path.of("src/main/resources/data/aquanaut/worldgen/ocean_terrain/reef_cap.json");
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        TerrainModule module = OceanLayerStackJson.parseTerrain(json);
        assertTrue(module.enabled());
        assertEquals(62, module.topWaterY());
        assertEquals(58, module.minCavityDepth());
    }

    @Test
    void customFourLayerStackParses() {
        String json = """
                {
                  "id": "aquanaut:deep_stack_v2",
                  "layers": [
                    {
                      "id": "aquanaut:surface_ocean",
                      "band": { "min_y": 40, "max_y": 320 },
                      "biomes": { "entries": [ { "biome": "minecraft:deep_ocean", "inherit_surface": true } ] }
                    },
                    {
                      "id": "aquanaut:twilight",
                      "band": { "min_y": 24, "max_y": 31, "blend_down": 2, "blend_up": 2 },
                      "biomes": {
                        "horizontal_blend_quarts": 2,
                        "entries": [
                          { "biome": "aquanaut:coral_forest", "noise_scale": 24 },
                          { "biome": "aquanaut:jelly_jungle", "noise_scale": 24 },
                          { "biome": "aquanaut:middle_level_ocean", "noise_scale": 24, "noise_salt": 9 }
                        ]
                      },
                      "carve": true
                    },
                    {
                      "id": "aquanaut:middle_sea",
                      "band": { "min_y": -64, "max_y": 23, "blend_up": 2 },
                      "biomes": { "entries": [ { "biome": "aquanaut:middle_level_ocean" } ] },
                      "carve": true
                    }
                  ]
                }
                """;
        OceanLayerStack stack = OceanLayerStackJson.fromJson(json);
        assertEquals(3, stack.layers().size());
        assertEquals(3, stack.layers().get(1).mix().entries().size());
        double[] weights = stack.layers().get(1).mix().weightsAt(0, 0);
        assertEquals(1.0, weights[0] + weights[1] + weights[2], 1e-6);
    }
}
