package com.dexer.aquanaut.common.notebook;

public enum NotebookDiet {
    HERBIVOROUS,
    CARNIVOROUS,
    ENERGEVOROUS;

    public String serializedName() {
        return name().toLowerCase();
    }

    public String translationKey() {
        return "gui.aquanaut.notebook.diet." + serializedName();
    }

    public static NotebookDiet fromSerializedName(String serializedName) {
        for (NotebookDiet diet : values()) {
            if (diet.serializedName().equals(serializedName)) {
                return diet;
            }
        }
        throw new IllegalArgumentException("Unknown notebook diet: " + serializedName);
    }
}
