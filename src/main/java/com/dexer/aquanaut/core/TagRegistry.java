package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Tags the mod reads by hand rather than through a material property.
 *
 * <p>{@link #SULFURIC_ACID} exists because vanilla only recognises water, lava and powder
 * snow as fluids a camera can be inside — a custom liquid reports no {@code FogType} at all.
 * Tagging is what lets another mod's corrosive liquid be drawn as acid by the fog authority
 * without any further wiring on either side.</p>
 *
 * <p>{@link #CRYSTAL_GROWTH_SUPPORT} is the rock crystals may grow from (晶巢岩). Crystals
 * never root in legacy rock, druse crusts or partial blocks — only whole nest stone carries
 * them — and the tag is how a sibling mod can put its own stone on that short list.</p>
 */
public final class TagRegistry {
    public static final TagKey<Fluid> SULFURIC_ACID = TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "sulfuric_acid"));

    public static final TagKey<Block> CRYSTAL_GROWTH_SUPPORT = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "crystal_growth_support"));

    private TagRegistry() {
    }
}