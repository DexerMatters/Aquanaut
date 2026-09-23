package com.dexer.aquanaut;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the inaccessible transient source block's resource contract. */
final class SearchlightLightBlockAssetTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/aquanaut");
    private static final Path BLOCKSTATES = ASSETS.resolve("blockstates");

    @Test
    void everyLightStateUsesTheInvisibleAirModel() throws IOException {
        Path blockstate = BLOCKSTATES.resolve("dynamic_light.json");
        assertTrue(Files.isRegularFile(blockstate), "missing dynamic light blockstate");

        JsonObject root = readJson(blockstate);
        assertEquals("minecraft:block/air",
                root.getAsJsonArray("multipart").get(0).getAsJsonObject()
                        .getAsJsonObject("apply").get("model").getAsString());
    }

    @Test
    void theTransientBlockHasNoItemModelOrRecipe() {
        assertFalse(Files.exists(ASSETS.resolve("models/item/dynamic_light.json")));
        assertFalse(Files.exists(Path.of("src/main/resources/data/aquanaut/recipes/dynamic_light.json")));
        assertFalse(Files.exists(BLOCKSTATES.resolve("searchlight_light.json")));
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
