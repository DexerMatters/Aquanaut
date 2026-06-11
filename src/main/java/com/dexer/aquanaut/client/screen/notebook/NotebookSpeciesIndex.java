package com.dexer.aquanaut.client.screen.notebook;

import com.dexer.aquanaut.common.notebook.NotebookBlock;
import com.dexer.aquanaut.common.notebook.NotebookProgress;
import com.dexer.aquanaut.common.notebook.NotebookResearchStage;
import com.dexer.aquanaut.common.notebook.NotebookSpecies;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class NotebookSpeciesIndex {

    private NotebookSpeciesIndex() {
    }

    public static List<NotebookSpecies> visibleSpecies(List<NotebookSpecies> species, NotebookProgress progress,
            Function<ResourceLocation, ResourceLocation> canonicalizer) {
        LinkedHashMap<ResourceLocation, NotebookSpecies> visible = new LinkedHashMap<>();
        for (NotebookSpecies entry : species) {
            ResourceLocation canonicalId = canonicalizer.apply(entry.id());
            if (!canonicalId.equals(entry.id())) {
                continue;
            }
            if (!progress.stageFor(entry.id()).isAtLeast(NotebookResearchStage.ENCOUNTERED)) {
                continue;
            }
            visible.putIfAbsent(entry.id(), entry);
        }
        return List.copyOf(visible.values());
    }

    public static Optional<NotebookSpecies> selectedSpecies(List<NotebookSpecies> species, ResourceLocation selectedId,
            NotebookProgress progress, Function<ResourceLocation, ResourceLocation> canonicalizer) {
        if (selectedId != null) {
            ResourceLocation canonicalId = canonicalizer.apply(selectedId);
            for (NotebookSpecies entry : species) {
                if (!entry.id().equals(canonicalId)) {
                    continue;
                }
                if (progress.stageFor(entry.id()).isAtLeast(NotebookResearchStage.ENCOUNTERED)) {
                    return Optional.of(entry);
                }
                break;
            }
        }

        List<NotebookSpecies> visible = visibleSpecies(species, progress, canonicalizer);
        if (visible.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(visible.getFirst());
    }

    public static List<NotebookBlock> visibleBlocks(NotebookSpecies species, NotebookProgress progress) {
        List<NotebookBlock> blocks = new ArrayList<>();
        NotebookResearchStage stage = progress.stageFor(species.id());
        for (NotebookBlock block : species.blocks()) {
            if (stage.isAtLeast(block.stage())) {
                blocks.add(block);
            }
        }
        return List.copyOf(blocks);
    }
}
