package com.dexer.aquanaut.common.notebook;

import net.minecraft.resources.ResourceLocation;

public record NotebookBlock(
        NotebookBlockKind kind,
        NotebookResearchStage stage,
        String text,
        NotebookComponentKind componentKind,
        ResourceLocation resource,
        String caption) {

    public NotebookBlock {
        text = text == null ? "" : text;
        caption = caption == null ? "" : caption;
    }

    public static NotebookBlock paragraph(NotebookResearchStage stage, String text) {
        return new NotebookBlock(NotebookBlockKind.PARAGRAPH, stage, text, null, null, "");
    }

    public static NotebookBlock component(NotebookResearchStage stage, NotebookComponentKind componentKind,
            ResourceLocation resource, String caption) {
        return new NotebookBlock(NotebookBlockKind.COMPONENT, stage, "", componentKind, resource, caption);
    }

    public boolean isParagraph() {
        return kind == NotebookBlockKind.PARAGRAPH;
    }

    public boolean isComponent() {
        return kind == NotebookBlockKind.COMPONENT;
    }
}
