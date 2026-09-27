package com.dexer.aquanaut.common.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * A creative tab output that can add things below a header.
 *
 * <p>
 * This is the tab-side of the header API: it is a plain {@link CreativeModeTab.Output}, so a tab's
 * contents generator accepts items through it exactly as before, but every {@link #header} call
 * opens a new section and the items accepted afterwards are recorded as belonging under it. The
 * client later lays those items out below their header (see {@link CreativeTabLayout}).
 *
 * <p>
 * Usage, inside a tab's contents generator:
 *
 * <pre>{@code
 * output.header(CreativeTabHeader.biome(BiomeRegistry.CORAL_FOREST));
 * output.accept(RED_CORAL_BLOCK.get());
 * ...
 * }</pre>
 */
public final class SectionedTabOutput implements CreativeModeTab.Output {

    /**
     * The recorder of each tab's last contents build, by tab id. A build re-runs the generator
     * from scratch, so a new recorder replaces the old one instead of accumulating.
     */
    private static final Map<ResourceLocation, SectionedTabOutput> RECORDERS = new HashMap<>();

    private final CreativeModeTab.Output delegate;
    private final List<CreativeTabLayout.Section> sections = new ArrayList<>();
    private List<ItemStack> currentSection;

    private SectionedTabOutput(CreativeModeTab.Output delegate) {
        this.delegate = delegate;
    }

    /**
     * Wraps the output handed to a tab's contents generator and starts recording it under the
     * tab's id, so {@link #layoutFor} can find the tab's sections afterwards.
     */
    public static SectionedTabOutput of(ResourceLocation tabId, CreativeModeTab.Output output) {
        SectionedTabOutput recorder = new SectionedTabOutput(output);
        RECORDERS.put(tabId, recorder);
        return recorder;
    }

    /**
     * Opens a section: everything accepted after this call is displayed below {@code header}.
     * Items accepted before the first header belong to no section and are laid out as they come.
     */
    public void header(CreativeTabHeader header) {
        this.currentSection = new ArrayList<>();
        this.sections.add(new CreativeTabLayout.Section(header, this.currentSection));
    }

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility tabVisibility) {
        this.delegate.accept(stack, tabVisibility);
        // A search-tab-only stack never reaches the tab's own grid, so it cannot sit under a header.
        if (this.currentSection != null
                && tabVisibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
                && !stack.isEmpty()) {
            this.currentSection.add(stack);
        }
    }

    /**
     * The recorded sections of a tab, or empty when the tab was not built through {@link #of} (a
     * vanilla tab, say) and therefore shows its items without headers.
     */
    public static Optional<CreativeTabLayout> layoutFor(@Nullable CreativeModeTab tab) {
        if (tab == null) {
            return Optional.empty();
        }
        ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        SectionedTabOutput recorder = tabId == null ? null : RECORDERS.get(tabId);
        return recorder == null ? Optional.empty() : Optional.of(recorder.layout());
    }

    private CreativeTabLayout layout() {
        List<CreativeTabLayout.Section> copy = new ArrayList<>(this.sections.size());
        for (CreativeTabLayout.Section section : this.sections) {
            copy.add(new CreativeTabLayout.Section(section.header(), List.copyOf(section.items())));
        }
        return new CreativeTabLayout(List.copyOf(copy));
    }
}
