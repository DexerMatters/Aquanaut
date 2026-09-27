package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Tags the mod reads by hand rather than through a material property.
 *
 * <p>{@link #CRYSTAL_GROWTH_SUPPORT} is the rock crystals may grow from (晶巢岩). Crystals
 * never root in legacy rock, druse crusts or partial blocks — only whole nest stone carries
 * them — and the tag is how a sibling mod can put its own stone on that short list.</p>
 */
public final class TagRegistry {
    public static final TagKey<Block> CRYSTAL_GROWTH_SUPPORT = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "crystal_growth_support"));

    private TagRegistry() {
    }
}