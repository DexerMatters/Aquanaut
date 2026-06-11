package com.dexer.aquanaut.common.notebook;

public enum NotebookComponentKind {
    SPRITE,
    MODEL;

    public String serializedName() {
        return name().toLowerCase();
    }

    public static NotebookComponentKind fromSerializedName(String serializedName) {
        for (NotebookComponentKind kind : values()) {
            if (kind.serializedName().equals(serializedName)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown notebook component kind: " + serializedName);
    }
}
