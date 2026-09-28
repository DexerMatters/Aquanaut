package com.dexer.aquanaut.common.investigation;

import net.minecraft.resources.ResourceLocation;

/** A directed piece of evidence connecting two nodes. */
public record InvestigationLink(
        ResourceLocation id,
        ResourceLocation from,
        ResourceLocation to,
        String titleKey,
        String detailKey) {

    public InvestigationLink {
        if (id == null || from == null || to == null || titleKey == null || detailKey == null) {
            throw new IllegalArgumentException("investigation link fields cannot be null");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("investigation link cannot point to itself: " + id);
        }
    }
}
