package com.dexer.aquanaut.client.screen;

import com.dexer.aquanaut.common.notebook.NotebookCatalog;
import com.dexer.aquanaut.common.notebook.NotebookCategory;
import com.dexer.aquanaut.common.notebook.NotebookProgress;
import com.dexer.aquanaut.common.notebook.NotebookResearchStage;
import com.dexer.aquanaut.common.notebook.NotebookSpecies;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class NotebookLayoutTest {

    @Test
    void categoryTabsAreEvenlySpacedAcrossTheBook() {
        List<NotebookLayout.TabBounds> tabs = NotebookLayout.categoryTabs(320, 240);
        assertEquals(3, tabs.size(), "tab count");

        int bookLeft = NotebookLayout.bookLeft(320);
        assertEquals(bookLeft + 20, tabs.get(0).x(), "friendly tab x");
        assertEquals(tabs.get(0).x() + NotebookLayout.CATEGORY_TAB_WIDTH + NotebookLayout.CATEGORY_TAB_GAP,
                tabs.get(1).x(), "threatening tab spacing");
        assertEquals(tabs.get(1).x() + NotebookLayout.CATEGORY_TAB_WIDTH + NotebookLayout.CATEGORY_TAB_GAP,
                tabs.get(2).x(), "titan tab spacing");
        assertEquals(NotebookCategory.FRIENDLY, tabs.getFirst().category(), "friendly tab order");
        assertEquals(NotebookCategory.THREATENING, tabs.get(1).category(), "threatening tab order");
        assertEquals(NotebookCategory.TITAN, tabs.get(2).category(), "titan tab order");
    }

    @Test
    void bookPositionClampsInsideSmallScreens() {
        assertEquals(0, NotebookLayout.bookLeft(200), "book left clamp");
        assertEquals(0, NotebookLayout.bookTop(160), "book top clamp");
    }

    @Test
    void resolveSelectionFallsBackToTheFirstVisibleEntry() throws IOException {
        loadCatalog();

        NotebookProgress progress = NotebookProgress.EMPTY
                .withStage(ResourceLocation.fromNamespaceAndPath("aquanaut", "catfish"),
                        NotebookResearchStage.ENCOUNTERED);

        NotebookSpecies selected = NotebookLayout.resolveSelection(
                NotebookCategory.FRIENDLY,
                ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine_variant"),
                progress);
        assertEquals(ResourceLocation.fromNamespaceAndPath("aquanaut", "catfish"), selected.id(),
                "fallback selection");

        NotebookSpecies empty = NotebookLayout.resolveSelection(
                NotebookCategory.THREATENING,
                null,
                NotebookProgress.EMPTY);
        assertEquals(null, empty, "empty category fallback");
    }

    @Test
    void visibleSpeciesWindowFollowsCanonicalSortOrder() throws IOException {
        loadCatalog();

        NotebookProgress progress = NotebookProgress.EMPTY
                .withStage(ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine"),
                        NotebookResearchStage.ENCOUNTERED)
                .withStage(ResourceLocation.fromNamespaceAndPath("aquanaut", "catfish"),
                        NotebookResearchStage.CAPTURED);

        List<NotebookSpecies> visible = NotebookLayout.visibleSpeciesWindow(NotebookCategory.FRIENDLY, progress, 0);
        assertEquals(List.of(
                ResourceLocation.fromNamespaceAndPath("aquanaut", "catfish"),
                ResourceLocation.fromNamespaceAndPath("aquanaut", "sardine")),
                visible.stream().map(NotebookSpecies::id).toList(),
                "visible index order");
    }

    private void loadCatalog() throws IOException {
        Path tempDir = Files.createTempDirectory("aquanaut-notebook-layout");
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
        Files.writeString(tempDir.resolve("anglerfish.json"), """
                {
                  "species_id": "aquanaut:anglerfish",
                  "entity_id": "aquanaut:anglerfish",
                  "category": "threatening",
                  "diet": "carnivorous",
                  "aliases": [],
                  "blocks": []
                }
                """);
        NotebookCatalog.reloadFromDirectory(tempDir);
    }

}
