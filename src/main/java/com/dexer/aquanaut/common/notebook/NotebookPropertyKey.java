package com.dexer.aquanaut.common.notebook;

public enum NotebookPropertyKey {
    MAX_HEALTH("max_health", NotebookResearchStage.ENCOUNTERED, false),
    EXCREMENT("excrement", NotebookResearchStage.ENCOUNTERED, true),
    DIET("diet", NotebookResearchStage.CAPTURED, false),
    SIZE("size", NotebookResearchStage.CAPTURED, false),
    VARIANT_COUNT("variant_count", NotebookResearchStage.DEFEATED, true);

    private final String serializedName;
    private final NotebookResearchStage unlockStage;
    private final boolean placeholder;

    NotebookPropertyKey(String serializedName, NotebookResearchStage unlockStage, boolean placeholder) {
        this.serializedName = serializedName;
        this.unlockStage = unlockStage;
        this.placeholder = placeholder;
    }

    public String serializedName() {
        return serializedName;
    }

    public NotebookResearchStage unlockStage() {
        return unlockStage;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public String translationKey() {
        return "gui.aquanaut.notebook.property." + serializedName;
    }
}
