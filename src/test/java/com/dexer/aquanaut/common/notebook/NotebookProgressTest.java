package com.dexer.aquanaut.common.notebook;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NotebookProgressTest {

    @Test
    void promotesStagesAndCanonicalizesAliases() throws IOException {
        loadCatalog();

        ResourceLocation canonical = ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine");
        ResourceLocation alias = ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine_variant");

        NotebookProgress progress = NotebookProgress.EMPTY
                .withStage(alias, NotebookResearchStage.ENCOUNTERED)
                .withStage(canonical, NotebookResearchStage.DEFEATED);

        assertEquals(NotebookResearchStage.DEFEATED, progress.stageFor(alias), "alias stage promotion");
        assertEquals(NotebookResearchStage.DEFEATED, progress.stageFor(canonical), "canonical stage promotion");
        assertTrue(progress.speciesStages().containsKey(canonical), "canonical key should be retained");
        assertTrue(!progress.speciesStages().containsKey(alias), "alias key should be normalized away");
    }

    private void loadCatalog() throws IOException {
        Path tempDir = Files.createTempDirectory("aquanaut-notebook-progress");
        Files.writeString(tempDir.resolve("sardine.json"), """
                {
                  "species_id": "aquanaut:sardine",
                  "entity_id": "aquanaut:sardine",
                  "category": "friendly",
                  "diet": "carnivorous",
                  "aliases": ["aquanaut:sardine_variant"],
                  "blocks": []
                }
                """);
        Files.writeString(tempDir.resolve("catfish.json"), """
                {
                  "species_id": "aquanaut:catfish",
                  "entity_id": "aquanaut:catfish",
                  "category": "friendly",
                  "diet": "carnivorous",
                  "aliases": [],
                  "blocks": []
                }
                """);
        NotebookCatalog.reloadFromDirectory(tempDir);
    }

}
