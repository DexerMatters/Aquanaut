package com.dexer.aquanaut.common.notebook;

public enum NotebookBlockKind {
    PARAGRAPH,
    COMPONENT;

    public String serializedName() {
        return name().toLowerCase();
    }

    public static NotebookBlockKind fromSerializedName(String serializedName) {
        for (NotebookBlockKind kind : values()) {
            if (kind.serializedName().equals(serializedName)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown notebook block kind: " + serializedName);
    }
}
