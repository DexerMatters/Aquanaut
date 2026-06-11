package com.dexer.aquanaut.common.notebook;

public enum NotebookResearchStage {
    UNSEEN,
    ENCOUNTERED,
    CAPTURED,
    DEFEATED;

    public boolean isAtLeast(NotebookResearchStage other) {
        return this.ordinal() >= other.ordinal();
    }

    public String serializedName() {
        return name().toLowerCase();
    }

    public String translationKey() {
        return "gui.aquanaut.notebook.stage." + serializedName();
    }

    public static NotebookResearchStage fromSerializedName(String serializedName) {
        for (NotebookResearchStage stage : values()) {
            if (stage.serializedName().equals(serializedName)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown notebook research stage: " + serializedName);
    }
}
