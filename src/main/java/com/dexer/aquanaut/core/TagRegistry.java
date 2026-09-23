package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Fluid tags the mod reads by hand rather than through a material property.
 *
 * <p>{@link #SULFURIC_ACID} exists because vanilla only recognises water, lava and powder
 * snow as fluids a camera can be inside — a custom liquid reports no {@code FogType} at all.
 * Tagging is what lets another mod's corrosive liquid be drawn as acid by the fog authority
 * without any further wiring on either side.</p>
 */
public final class TagRegistry {
    public static final TagKey<Fluid> SULFURIC_ACID = TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "sulfuric_acid"));

    private TagRegistry() {
    }
}