package com.dexer.aquanaut.common.investigation;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** A discoverable card on the investigation board. Coordinates use a 1000 x 620 design grid. */
public record InvestigationNode(
        ResourceLocation id,
        String titleKey,
        String summaryKey,
        String questionKey,
        int x,
        int y,
        int maxStars,
        Tone tone,
        List<ResourceLocation> prerequisites) {

    public InvestigationNode {
        if (id == null || titleKey == null || summaryKey == null || questionKey == null || tone == null) {
            throw new IllegalArgumentException("investigation node fields cannot be null");
        }
        if (x < 0 || x > 1000 || y < 0 || y > 620) {
            throw new IllegalArgumentException("investigation node must be inside the 1000 x 620 design grid: " + id);
        }
        if (maxStars < 1 || maxStars > 5) {
            throw new IllegalArgumentException("investigation nodes support between one and five stars: " + id);
        }
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    }

    public enum Tone {
        PAPER,
        BLUEPRINT,
        WARNING
    }
}
