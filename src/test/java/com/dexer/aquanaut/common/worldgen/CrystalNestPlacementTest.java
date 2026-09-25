package com.dexer.aquanaut.common.worldgen;

import com.dexer.aquanaut.common.worldgen.layers.OceanLayer;
import com.dexer.aquanaut.common.worldgen.layers.OceanLayerStackJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CrystalNestPlacementTest {

    @Test
    void biomeIdAndHiddenHolderAnchorStayStable() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "crystal_nest"),
                CrystalNestPlacement.location(), "biome id");
        assertEquals(2, CrystalNestPlacement.regionWeight(), "region weight");
        assertEquals(4.5F, CrystalNestPlacement.holderAnchorParameter(), 0.0001F, "hidden holder anchor");
        assertEquals(2.25F, CrystalNestPlacement.holderAnchorOffset(), 0.0001F, "hidden holder offset");
    }

    @Test
    void crystalNestIsCarriedByTheMiddleSeaMixOnItsOwnChannel() {
        OceanLayer middleSea = OceanLayer.middleSea();
        Long nestSalt = null;
        Set<Long> otherSalts = new HashSet<>();
        for (var entry : middleSea.mix().entries()) {
            if (entry.biome().equals(CrystalNestPlacement.location())) {
                nestSalt = entry.noiseSalt();
            } else {
                otherSalts.add(entry.noiseSalt());
            }
        }
        assertTrue(nestSalt != null, "the crystal nest joins the middle-sea floor biomes");
        assertFalse(otherSalts.contains(nestSalt),
                "the crystal nest rides its own noise channel so its patches stay coherent");
    }

    @Test
    void defaultStackJsonMirrorsTheJavaLayer() throws Exception {
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
        assertTrue(biomes.contains("aquanaut:crystal_nest"), "bundled stack carries the crystal nest");
        OceanLayerStackJson.fromReader(Files.newBufferedReader(
                Path.of("src/main/resources/data/aquanaut/worldgen/ocean_layer_stack/default_deep_stack.json")));
    }

    @Test
    void theLatticeIsTheOnlyDecoratedMiddleSeaBiome() {
        OceanLayer middleSea = OceanLayer.middleSea();
        assertEquals("aquanaut:middle_cavity", middleSea.terrain().toString(),
                "the crystal nest grows inside the shared middle-sea chamber");
        assertTrue(middleSea.carve(), "the middle sea still carves its cavity");
    }

    @Test
    void surfaceBiomeSamplingCanReachTheCrystalNest() {
        var mix = OceanLayer.middleSea().mix();
        Set<ResourceLocation> seen = new HashSet<>();
        for (int x = -400; x <= 400; x += 25) {
            for (int z = -400; z <= 400; z += 25) {
                seen.add(mix.dominantBiomeAt(x, z));
            }
        }
        assertTrue(seen.contains(CrystalNestPlacement.location()),
                "large-scale sampling should produce crystal nest districts");
        assertTrue(seen.size() >= 3, "the middle sea keeps several distinct biomes");
    }

    private static JsonObject parse(String path) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }
}
