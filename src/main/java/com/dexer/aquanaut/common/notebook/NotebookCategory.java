package com.dexer.aquanaut.common.notebook;

import net.minecraft.resources.ResourceLocation;

public enum NotebookCategory {
    FRIENDLY("friendly", ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full")),
    THREATENING("threatening", ResourceLocation.withDefaultNamespace("hud/heart/full")),
    TITAN("titan", ResourceLocation.withDefaultNamespace("hud/heart/withered_full"));

    private final String serializedName;
    private final ResourceLocation iconSpriteId;

    NotebookCategory(String serializedName, ResourceLocation iconSpriteId) {
        this.serializedName = serializedName;
        this.iconSpriteId = iconSpriteId;
    }

    public String serializedName() {
        return serializedName;
    }

    public ResourceLocation iconSpriteId() {
        return iconSpriteId;
    }

    public String translationKey() {
        return "gui.aquanaut.notebook.category." + serializedName;
    }

    public static NotebookCategory fromSerializedName(String serializedName) {
        for (NotebookCategory category : values()) {
            if (category.serializedName.equals(serializedName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown notebook category: " + serializedName);
    }
}
