package com.dexer.aquanaut.common.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shipped vertical envelope.
 *
 * <p>
 * Two mistakes here are unrecoverable in a live world: a {@code logical_height} that does not
 * match {@code height} silently breaks nether portals and chorus fruit, and any later height change
 * either deletes everything below the new floor or — when both heights share the same
 * {@code ceillog2(height + 1)} bit width — silently offsets every stored heightmap. These
 * assertions are the tripwire for both.
 * </p>
 */
public final class AbyssEnvelopeJsonTest {

    private static final Path MOD_DATA = Path.of("src/main/resources/data");
    private static final Path PACK_ROOT = Path.of("src/main/resources/abyssal_overworld_pack");
    private static final Path PACK_DATA = PACK_ROOT.resolve("data");

    /** Top of the world: sea level and every surface anchor stay where vanilla expects them. */
    private static final int BUILD_LIMIT = 320;
    private static final int ABYSSAL_BOTTOM = -512;
    private static final int ABYSSAL_HEIGHT = 832;

    private static final int MIN_Y_LIMIT = -2032;
    private static final int MAX_Y_LIMIT = 2031;
    private static final int MAX_HEIGHT = 4064;

    private static JsonObject parse(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static List<Path> jsonUnder(Path root, String marker) throws IOException {
        List<Path> found = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> path.toString().replace('\\', '/').contains(marker))
                    .forEach(found::add);
        }
        return found;
    }

    private static List<Path> dimensionTypes() throws IOException {
        List<Path> found = new ArrayList<>(jsonUnder(MOD_DATA, "/dimension_type/"));
        found.addAll(jsonUnder(PACK_DATA, "/dimension_type/"));
        return found;
    }

    private static List<Path> noiseSettings() throws IOException {
        List<Path> found = new ArrayList<>(jsonUnder(MOD_DATA, "/worldgen/noise_settings/"));
        found.addAll(jsonUnder(PACK_DATA, "/worldgen/noise_settings/"));
        return found;
    }

    @Test
    void everyShippedDimensionTypeIsALegalAbyssalEnvelope() throws IOException {
        List<Path> types = dimensionTypes();
        assertEquals(2, types.size(), "expected the mod's abyssal type plus the pack's overworld override");

        for (Path path : types) {
            JsonObject root = parse(path);
            String at = path.toString();
            int minY = root.get("min_y").getAsInt();
            int height = root.get("height").getAsInt();
            int logicalHeight = root.get("logical_height").getAsInt();

            assertEquals(ABYSSAL_BOTTOM, minY, at);
            assertEquals(ABYSSAL_HEIGHT, height, at);
            assertEquals(height, logicalHeight,
                    at + ": logical_height must equal height or portals/chorus break");
            assertEquals(BUILD_LIMIT, minY + height, at + ": the surface must stay where it is");
            assertEquals(0, Math.floorMod(minY, 16), at);
            assertEquals(0, Math.floorMod(height, 16), at);
            assertTrue(minY >= MIN_Y_LIMIT && height <= MAX_HEIGHT, at + ": inside the engine limits");
            assertTrue(minY + height <= MAX_Y_LIMIT + 1, at + ": inside the engine limits");
            assertEquals("minecraft:overworld", root.get("effects").getAsString(), at);
            assertTrue(root.get("has_skylight").getAsBoolean(), at);
            assertFalse(root.get("has_ceiling").getAsBoolean(), at);
        }
    }

    @Test
    void everyShippedNoiseSettingsMatchesTheAbyssalEnvelope() throws IOException {
        List<Path> settings = noiseSettings();
        assertEquals(4, settings.size(),
                "expected aquanaut:water_world plus the overworld/large_biomes/amplified overrides");

        for (Path path : settings) {
            JsonObject root = parse(path);
            String at = path.toString();
            JsonObject noise = root.getAsJsonObject("noise");
            int minY = noise.get("min_y").getAsInt();
            int height = noise.get("height").getAsInt();

            assertEquals(ABYSSAL_BOTTOM, minY, at + ": a shallower generator voids the new depth");
            assertEquals(ABYSSAL_HEIGHT, height, at);
            assertEquals(BUILD_LIMIT, minY + height, at);
            assertEquals(0, Math.floorMod(minY, 16), at);
            assertEquals(0, Math.floorMod(height, 16), at);
            assertEquals(63, root.get("sea_level").getAsInt(),
                    at + ": sea level is part of the untouched surface story");
        }
    }

    @Test
    void waterWorldPresetUsesTheModdedAbyssalDimensionType() throws IOException {
        Path preset = MOD_DATA.resolve("aquanaut/worldgen/world_preset/water_world.json");
        JsonObject overworld = parse(preset).getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld");

        assertEquals("aquanaut:abyssal_overworld", overworld.get("type").getAsString(),
                "the mod's own world must not depend on the disableable vanilla override");
        assertEquals("aquanaut:water_world",
                overworld.getAsJsonObject("generator").get("settings").getAsString());

        assertTrue(Files.exists(MOD_DATA.resolve("aquanaut/dimension_type/abyssal_overworld.json")),
                "the preset's dimension type must be shipped by the always-on pack");
        assertTrue(Files.exists(MOD_DATA.resolve("aquanaut/worldgen/noise_settings/water_world.json")),
                "the preset's noise settings must be shipped by the always-on pack");
    }

    @Test
    void packStringsExistInBothLanguages() throws IOException {
        for (String lang : new String[] {"en_us", "zh_cn"}) {
            JsonObject translations = parse(Path.of("src/main/resources/assets/aquanaut/lang/" + lang + ".json"));
            for (String key : new String[] {"pack.aquanaut.abyssal_overworld",
                    "pack.aquanaut.abyssal_overworld.desc"}) {
                assertTrue(translations.has(key) && !translations.get(key).getAsString().isBlank(),
                        lang + " is missing " + key + ", which the pack screen and pack.mcmeta resolve");
            }
        }
    }

    @Test
    void vanillaOverrideTravelsInTheDisableablePackOnly() throws IOException {
        assertFalse(Files.exists(MOD_DATA.resolve("minecraft/dimension_type/overworld.json")),
                "the vanilla overworld override must stay disableable, not ride the always-on pack");

        Path meta = PACK_ROOT.resolve("pack.mcmeta");
        assertTrue(Files.exists(meta), "the override pack needs a pack.mcmeta");
        assertEquals(48, parse(meta).getAsJsonObject("pack").get("pack_format").getAsInt());
        assertTrue(Files.exists(PACK_DATA.resolve("minecraft/dimension_type/overworld.json")));
        for (String name : new String[] {"overworld", "large_biomes", "amplified"}) {
            assertTrue(Files.exists(PACK_DATA.resolve("minecraft/worldgen/noise_settings/" + name + ".json")),
                    "preset settings " + name + " must be deepened alongside the dimension type");
        }
    }
}
