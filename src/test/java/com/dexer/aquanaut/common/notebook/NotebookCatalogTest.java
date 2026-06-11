package com.dexer.aquanaut.common.notebook;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NotebookCatalogTest {

    @Test
    void reloadsSpeciesDataAndResolvesAliasesToCanonicalSpecies() throws IOException {
        Path tempDir = Files.createTempDirectory("aquanaut-notebook-catalog");
        writeSpecies(tempDir, "sardine", """
                {
                  "species_id": "aquanaut:sardine",
                  "entity_id": "aquanaut:sardine",
                  "category": "friendly",
                  "diet": "carnivorous",
                  "aliases": ["aquanaut:sardine_variant"],
                  "blocks": [
                    {
                      "type": "paragraph",
                      "stage": "encountered",
                      "text": "A tidy school of silver shadows, quick to scatter and quick to return."
                    }
                  ]
                }
                """);
        writeSpecies(tempDir, "catfish", """
                {
                  "species_id": "aquanaut:catfish",
                  "entity_id": "aquanaut:catfish",
                  "category": "friendly",
                  "diet": "carnivorous",
                  "aliases": [],
                  "blocks": [
                    {
                      "type": "paragraph",
                      "stage": "encountered",
                      "text": "Cat-fish keep their distance until they learn a diver is harmless."
                    }
                  ]
                }
                """);
        writeSpecies(tempDir, "anglerfish", """
                {
                  "species_id": "aquanaut:anglerfish",
                  "entity_id": "aquanaut:anglerfish",
                  "category": "threatening",
                  "diet": "carnivorous",
                  "aliases": ["aquanaut:anglerfish_variant"],
                  "blocks": [
                    {
                      "type": "paragraph",
                      "stage": "encountered",
                      "text": "A lure and a jaw. Nothing else is needed."
                    }
                  ]
                }
                """);

        NotebookCatalog.reloadFromDirectory(tempDir);

        assertEquals(3, NotebookCatalog.all().size(), "catalog entry count");
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine"),
                NotebookCatalog.canonicalId(ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine_variant")),
                "alias resolution");
        assertTrue(NotebookCatalog.resolve(ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine_variant"))
                .isPresent(), "alias should resolve to a canonical species");

        List<NotebookSpecies> friendly = NotebookCatalog.byCategory(NotebookCategory.FRIENDLY);
        assertEquals(2, friendly.size(), "friendly category size");
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "catfish"), friendly.get(0).id(),
                "friendly category ordering");
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine"), friendly.get(1).id(),
                "friendly category ordering");
    }

    @Test
    void sortsVisibleEntriesByCanonicalSpeciesIdWithinEachCategory() throws IOException {
        Path tempDir = Files.createTempDirectory("aquanaut-notebook-visible");
        writeSpecies(tempDir, "algae_eel", """
                {
                  "species_id": "aquanaut:algae_eel",
                  "entity_id": "aquanaut:algae_eel",
                  "category": "friendly",
                  "diet": "herbivorous",
                  "aliases": [],
                  "blocks": [
                    {
                      "type": "paragraph",
                      "stage": "encountered",
                      "text": "A calm eel that browses the kelp."
                    }
                  ]
                }
                """);
        writeSpecies(tempDir, "brine_darter", """
                {
                  "species_id": "aquanaut:brine_darter",
                  "entity_id": "aquanaut:brine_darter",
                  "category": "friendly",
                  "diet": "carnivorous",
                  "aliases": [],
                  "blocks": [
                    {
                      "type": "paragraph",
                      "stage": "encountered",
                      "text": "A darting little predator."
                    }
                  ]
                }
                """);
        writeSpecies(tempDir, "abyss_hunter", """
                {
                  "species_id": "aquanaut:abyss_hunter",
                  "entity_id": "aquanaut:abyss_hunter",
                  "category": "threatening",
                  "diet": "energevorous",
                  "aliases": [],
                  "blocks": [
                    {
                      "type": "paragraph",
                      "stage": "encountered",
                      "text": "It feeds on lingering warmth and static."
                    }
                  ]
                }
                """);

        NotebookCatalog.reloadFromDirectory(tempDir);

        NotebookProgress progress = NotebookProgress.EMPTY
                .withStage(ResourceLocation.fromNamespaceAndPath("aquanaut", "brine_darter"),
                        NotebookResearchStage.ENCOUNTERED)
                .withStage(ResourceLocation.fromNamespaceAndPath("aquanaut", "abyss_hunter"),
                        NotebookResearchStage.CAPTURED);

        List<NotebookSpecies> friendlyVisible = NotebookCatalog.visibleByCategory(NotebookCategory.FRIENDLY, progress);
        assertEquals(List.of(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "brine_darter")),
                friendlyVisible.stream().map(NotebookSpecies::id).toList(),
                "friendly visible ordering");

        List<NotebookSpecies> threateningVisible = NotebookCatalog.visibleByCategory(NotebookCategory.THREATENING,
                progress);
        assertEquals(List.of(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "abyss_hunter")),
                threateningVisible.stream().map(NotebookSpecies::id).toList(),
                "threatening visible ordering");
    }

    private void writeSpecies(Path directory, String fileName, String json) throws IOException {
        Files.writeString(directory.resolve(fileName + ".json"), json);
    }

}
