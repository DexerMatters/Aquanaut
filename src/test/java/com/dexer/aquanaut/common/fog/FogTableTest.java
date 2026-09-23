package com.dexer.aquanaut.common.fog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fog table: a complete default, partial files that override only what they name, and
 * the contract that every Aquanaut ocean declares how far it can be seen through.
 */
public final class FogTableTest {
    private static final Path BIOME_DIRECTORY =
            Path.of("src/main/resources/data/aquanaut/worldgen/biome");
    private static final Path PROFILE = Path.of("src/main/resources/assets/aquanaut/fog_profile.json");

    @Test
    void defaultsAreCompleteAndUsable() {
        FogTable table = FogTable.defaults();
        assertTrue(table.biomeIds().size() >= 5, "every Aquanaut ocean is described");
        assertNotNull(table.acid(), "the acid medium exists");
        assertTrue(table.fallback().farPlane() > FogTable.defaults().fallback().nearPlane());
        assertTrue(table.acid().visibility().farPlane() < 64.0F,
                "acid is drawn in much tighter than open water");
        assertEquals(0x6E7A2A, table.acid().rgb());
    }

    @Test
    void aPointBetweenOceansGetsTheBlendOfBoth() {
        ResourceLocation clear = id("brine_mirror_gorge");
        ResourceLocation murky = id("jelly_jungle");
        FogVisibility clearProfile = FogTable.defaults().visibility(clear);
        FogVisibility murkyProfile = FogTable.defaults().visibility(murky);

        FogVisibility onlyClear = FogTable.defaults().blendedVisibility(List.of(
                new FogTable.WeightedBiome(clear, 1.0D)));
        assertEquals(clearProfile.farPlane(), onlyClear.farPlane(), 1e-6F,
                "a camera inside one ocean sees exactly that ocean's profile");

        FogVisibility half = FogTable.defaults().blendedVisibility(List.of(
                new FogTable.WeightedBiome(clear, 1.0D),
                new FogTable.WeightedBiome(murky, 1.0D)));
        assertEquals((clearProfile.farPlane() + murkyProfile.farPlane()) / 2.0F, half.farPlane(), 1e-6F);
        assertTrue(half.farPlane() < clearProfile.farPlane()
                        && half.farPlane() > murkyProfile.farPlane(),
                "a boundary sits between the two oceans, not on one of them");

        FogVisibility leaning = FogTable.defaults().blendedVisibility(List.of(
                new FogTable.WeightedBiome(clear, 3.0D),
                new FogTable.WeightedBiome(murky, 1.0D)));
        assertTrue(leaning.farPlane() > half.farPlane(), "weight decides which way it leans");

        assertEquals(FogTable.defaults().fallback(),
                FogTable.defaults().blendedVisibility(List.of()), "no samples means the fallback");
        assertEquals(FogTable.defaults().fallback(),
                FogTable.defaults().blendedVisibility(List.of(
                        new FogTable.WeightedBiome(clear, 0.0D))),
                "samples that carry no weight leave nothing to blend, so the fallback stands");
    }

    @Test
    void unknownBiomesFallBackInsteadOfVanishing() {
        FogTable table = FogTable.parse(new JsonObject());
        FogVisibility unknown = table.visibility(ResourceLocation.parse("someothermod:deep_trench"));
        assertEquals(table.fallback(), unknown);
        assertFalse(table.declares(ResourceLocation.parse("someothermod:deep_trench")));
    }

    @Test
    void partialFileOverridesOnlyWhatItNames() {
        JsonObject json = JsonParser.parseString("""
                {
                  "biomes": {
                    "aquanaut:brimstone_caldera": {"far": 40.0, "cast": 0.9}
                  }
                }
                """).getAsJsonObject();
        FogTable table = FogTable.parse(json);
        FogVisibility brimstone = table.visibility(id("brimstone_caldera"));
        assertEquals(40.0F, brimstone.farPlane(), 1e-6F, "the named field is taken");
        assertEquals(0.9F, brimstone.castStrength(), 1e-6F);
        assertEquals(FogTable.defaults().visibility(id("brimstone_caldera")).nearPlane(),
                brimstone.nearPlane(), 1e-6F, "fields the file leaves out keep their value");
        assertEquals(128.0F, table.visibility(id("middle_level_ocean")).farPlane(), 1e-6F,
                "a biome the file does not mention keeps its own profile");
    }

    @Test
    void laterFilesLayerRatherThanReplace() {
        FogTable first = FogTable.parse(JsonParser.parseString("""
                {"biomes": {"aquanaut:coral_forest": {"far": 190.0}, "aquanaut:jelly_jungle": {"far": 20.0}}}
                """).getAsJsonObject());
        FogTable second = first.withOverrides(JsonParser.parseString("""
                {"biomes": {"aquanaut:jelly_jungle": {"far": 33.0}}}
                """).getAsJsonObject());
        assertEquals(190.0F, second.visibility(id("coral_forest")).farPlane(), 1e-6F,
                "the second file must not wipe the first");
        assertEquals(33.0F, second.visibility(id("jelly_jungle")).farPlane(), 1e-6F,
                "and must be able to override it");
    }

    @Test
    void coloursAcceptEveryWrittenFormAndFallBackOnNonsense() {
        JsonObject json = JsonParser.parseString("""
                {
                  "mediums": {
                    "acid": {"color": "#123456"},
                    "sludge": {"color": "abcdef", "far": 30.0},
                    "broken": {"color": "not-a-colour"},
                    "colourless": {"far": 12.0}
                  }
                }
                """).getAsJsonObject();
        FogTable table = FogTable.parse(json);
        assertEquals(0x123456, table.acid().rgb());
        assertEquals(0xABCDEF, table.medium("sludge").rgb());
        assertEquals(30.0F, table.medium("sludge").visibility().farPlane(), 1e-6F);
        assertEquals(FogTable.defaults().acid().rgb(), table.medium("broken").rgb(),
                "an unreadable colour falls back to the acid's own rather than to white");
        assertNull(table.medium("colourless"),
                "a medium with neither a colour nor a known identity is not introduced at all");
    }

    @Test
    void acidColourChannelsReadCorrectly() {
        FogMediumProfile acid = FogTable.defaults().acid();
        assertEquals(0x6E / 255.0F, acid.red(), 1e-6F);
        assertEquals(0x7A / 255.0F, acid.green(), 1e-6F);
        assertEquals(0x2A / 255.0F, acid.blue(), 1e-6F);
    }

    @Test
    void everyAquanautBiomeDeclaresItsVisibility() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.list(BIOME_DIRECTORY)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String name = file.getFileName().toString().replace(".json", "");
                if (!loadedTable().declares(id(name))) {
                    missing.add(name);
                }
            }
        }
        assertTrue(missing.isEmpty(), "oceans without a fog profile: " + missing);
    }

    @Test
    void theProfileOnlyNamesRealBiomes() throws IOException {
        List<String> invented = new ArrayList<>();
        for (ResourceLocation declared : loadedTable().biomeIds()) {
            if (!"aquanaut".equals(declared.getNamespace())) {
                continue;
            }
            Path file = BIOME_DIRECTORY.resolve(declared.getPath() + ".json");
            if (!Files.isRegularFile(file)) {
                invented.add(declared.toString());
            }
        }
        assertTrue(invented.isEmpty(), "fog profiles for biomes that do not exist: " + invented);
    }

    @Test
    void shippedProfileParsesAndMatchesItsDefaults() throws IOException {
        FogTable shipped = loadedTable();
        assertEquals(128.0F, shipped.visibility(id("middle_level_ocean")).farPlane(), 1e-6F);
        assertEquals(0x6E7A2A, shipped.acid().rgb());
        assertTrue(shipped.visibility(id("brine_mirror_gorge")).farPlane()
                        > shipped.visibility(id("jelly_jungle")).farPlane(),
                "the mirror gorge is the clear one and the jungle the murky one");
    }

    private static FogTable loadedTable() throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(PROFILE, StandardCharsets.UTF_8))
                .getAsJsonObject();
        return FogTable.parse(json);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", path);
    }
}