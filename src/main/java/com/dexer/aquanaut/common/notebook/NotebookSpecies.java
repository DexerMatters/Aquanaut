package com.dexer.aquanaut.common.notebook;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record NotebookSpecies(
        ResourceLocation id,
        ResourceLocation entityId,
        NotebookCategory category,
        NotebookDiet diet,
        List<ResourceLocation> aliases,
        List<NotebookBlock> blocks) {

    public NotebookSpecies {
        entityId = entityId == null ? id : entityId;
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
    }

    public String translationKey() {
        return entityId.toLanguageKey("entity");
    }
}
