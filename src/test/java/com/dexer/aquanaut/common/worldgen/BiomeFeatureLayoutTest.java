package com.dexer.aquanaut.common.worldgen;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class BiomeFeatureLayoutTest {
    @Test
    void coralForestKeepsItsCustomPillarFeature() {
        assertTrue(loadFeatureStages("coral_forest").stream()
                        .flatMap(List::stream)
                        .anyMatch("aquanaut:coral_forest_pillar"::equals),
                "coral forest should keep its custom stone pillar feature");
    }

    @Test
    void middleLevelOceanDoesNotGenerateCoralForestPillars() {
        assertFalse(loadFeatureStages("middle_level_ocean").stream()
                        .flatMap(List::stream)
                        .anyMatch("aquanaut:coral_forest_pillar"::equals),
                "middle level ocean should not decorate its open water with coral forest pillars");
    }

    @Test
    void customOceanBiomesKeepTheSameFeatureStageCount() {
        assertEquals(loadFeatureStages("middle_level_ocean").size(), loadFeatureStages("coral_forest").size(),
                "custom ocean biomes should keep the same number of generation stages");
    }

    private List<List<String>> loadFeatureStages(String biomeName) {
        String path = "data/aquanaut/worldgen/biome/" + biomeName + ".json";
        try (InputStream stream = BiomeFeatureLayoutTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("missing biome resource " + path);
            }

            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            int featuresKey = json.indexOf("\"features\"");
            if (featuresKey < 0) {
                throw new AssertionError("missing features array in " + path);
            }

            int arrayStart = json.indexOf('[', featuresKey);
            if (arrayStart < 0) {
                throw new AssertionError("missing features array body in " + path);
            }

            int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
            String featuresBody = json.substring(arrayStart + 1, arrayEnd);
            List<List<String>> stages = new ArrayList<>();
            int index = 0;
            while (index < featuresBody.length()) {
                int stageStart = featuresBody.indexOf('[', index);
                if (stageStart < 0) {
                    break;
                }
                int stageEnd = findMatchingBracket(featuresBody, stageStart, '[', ']');
                stages.add(parseStringArray(featuresBody.substring(stageStart + 1, stageEnd)));
                index = stageEnd + 1;
            }
            return stages;
        } catch (Exception e) {
            throw new AssertionError("failed to load biome resource " + path, e);
        }
    }

    private int findMatchingBracket(String text, int startIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == startIndex || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new AssertionError("unterminated array while parsing biome features");
    }

    private List<String> parseStringArray(String text) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                if (inString) {
                    values.add(current.toString());
                    current.setLength(0);
                }
                inString = !inString;
            } else if (inString) {
                current.append(c);
            }
        }
        return values;
    }
}
