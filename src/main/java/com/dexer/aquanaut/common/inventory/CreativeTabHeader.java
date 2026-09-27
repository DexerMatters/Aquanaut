package com.dexer.aquanaut.common.inventory;

import com.dexer.aquanaut.Aquanaut;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * A banner row inside a creative tab.
 *
 * <p>
 * A header is not an item: it is a full row of the tab's item grid that the client draws as one
 * horizontal banner instead of nine slots. Items accepted after it through
 * {@link SectionedTabOutput#header} are laid out below it (see {@link CreativeTabLayout}). The
 * banner is painted from {@link #background()}, a plain GUI texture; the client stretches it across
 * the whole row and writes {@link #title()} on top of it.
 *
 * @param title      the name written on the banner
 * @param background the banner image, drawn 1:1 across one full grid row
 */
public record CreativeTabHeader(Component title, ResourceLocation background) {

    /**
     * The banner of a biome: the biome's display name, over the biome's header image.
     *
     * <p>
     * The image follows the naming convention the header textures are generated under --
     * {@code aquanaut:textures/gui/creative_tab/<biome path>.png} -- so a biome header needs no
     * wiring beyond its texture existing.
     */
    public static CreativeTabHeader biome(ResourceKey<Biome> biome) {
        return new CreativeTabHeader(
                Component.translatable(Util.makeDescriptionId("biome", biome.location())),
                ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
                        "textures/gui/creative_tab/" + biome.location().getPath() + ".png"));
    }

    /**
     * A header of the mod's own naming: the title is
     * {@code gui.aquanaut.creative_header.<name>} and the banner is
     * {@code aquanaut:textures/gui/creative_tab/<name>.png}, the same convention {@link #biome}
     * follows for biomes.
     */
    public static CreativeTabHeader of(String name) {
        return new CreativeTabHeader(
                Component.translatable("gui." + Aquanaut.MODID + ".creative_header." + name),
                ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID,
                        "textures/gui/creative_tab/" + name + ".png"));
    }
}
